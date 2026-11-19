package ru.mirea.examcenter.kr3.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.dto.ImportDtos.*;
import ru.mirea.examcenter.kr3.error.*;
import ru.mirea.examcenter.kr3.filter.*;
import ru.mirea.examcenter.kr3.repository.*;
import ru.mirea.examcenter.kr3.security.CurrentUserService;
import ru.mirea.examcenter.kr3.config.AppProperties;
import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;
@Service
public class CsvApplicationService {
 public static final String[] HEADERS={"externalId","applicantEmail","disciplineCode","scheduledAt","status","score","preparationPercent","documentsVerified","tags"};
 private final ExamApplicationRepository repo;private final ApplicantRepository people;private final ExamDisciplineRepository disciplines;
 private final CsvImportPreviewRepository previews;private final ApplicationService apps;private final CurrentUserService user;
 private final ObjectMapper json;private final Validator validator;private final AppProperties props;private final ExamRules rules;private final TransactionTemplate tx;
 public CsvApplicationService(ExamApplicationRepository repo,ApplicantRepository people,ExamDisciplineRepository disciplines,CsvImportPreviewRepository previews,ApplicationService apps,CurrentUserService user,ObjectMapper json,Validator validator,AppProperties props,ExamRules rules,PlatformTransactionManager tm){this.repo=repo;this.people=people;this.disciplines=disciplines;this.previews=previews;this.apps=apps;this.user=user;this.json=json;this.validator=validator;this.props=props;this.rules=rules;this.tx=new TransactionTemplate(tm);}
 public ImportPreview preview(MultipartFile file,ImportMode mode){
  if(file.isEmpty())throw new BadRequestException("CSV-файл пуст");if(file.getSize()>2*1024*1024)throw new BadRequestException("CSV-файл больше 2 МБ");
  try {
   byte[] bytes=file.getBytes();String source=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();if(source.startsWith("\uFEFF"))source=source.substring(1);
   List<ImportRow> rows=new ArrayList<>();List<RowIssue> errors=new ArrayList<>();Set<String> codes=new HashSet<>();Set<String> slots=new HashSet<>();
   try(CSVParser parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get().parse(new StringReader(source))){
    if(!parser.getHeaderNames().equals(List.of(HEADERS)))throw new BadRequestException("Неверные заголовки CSV. Скачайте образец или экспорт");
    int seen=0;for(CSVRecord rec:parser){long line=rec.getRecordNumber()+1;if(++seen>props.importLimits().maxRows())throw new BadRequestException("Не более "+props.importLimits().maxRows()+" строк CSV");
     try {
      if(!rec.isConsistent())throw new BadRequestException("Количество столбцов не совпадает с заголовком");
      String external=cell(rec,"externalId");if(!codes.add(external))throw new BadRequestException("Код повторяется в CSV");
      Applicant person=people.findByEmailIgnoreCase(cell(rec,"applicantEmail")).orElseThrow(()->new BadRequestException("Кандидат с таким email не найден"));
      ExamDiscipline d=disciplines.findByCodeIgnoreCase(cell(rec,"disciplineCode")).orElseThrow(()->new BadRequestException("Код дисциплины не найден"));
      ExamApplication old=repo.findByExternalId(external).orElse(null);if(old!=null&&mode==ImportMode.CREATE_ONLY)throw new BadRequestException("Код уже существует (режим только новые)");
      String flag=cell(rec,"documentsVerified");if(!Set.of("true","false").contains(flag))throw new BadRequestException("documentsVerified: только true или false");
      String score=cell(rec,"score");String tag=cell(rec,"tags");
      ApplicationRequest req=new ApplicationRequest(external,person.getPublicId(),d.getId(),LocalDateTime.parse(cell(rec,"scheduledAt")),ApplicationStatus.valueOf(cell(rec,"status")),score.isEmpty()?null:Integer.valueOf(score),Integer.valueOf(cell(rec,"preparationPercent")),Boolean.valueOf(flag),tag.isEmpty()?Set.of():new LinkedHashSet<>(Arrays.asList(tag.split("\\|",-1))),old==null?null:old.getVersion());
      var violations=validator.validate(req);if(!violations.isEmpty())throw new BadRequestException(violations.iterator().next().getPropertyPath()+": "+violations.iterator().next().getMessage());
      if(person.getStatus()!=ApplicantStatus.ACTIVE)throw new BadRequestException("Кандидат заблокирован");if(!d.getActive())throw new BadRequestException("Дисциплина отключена");
      ApplicationService.tags(req.tags());rules.score(req.status(),req.score(),req.scheduledAt());
      if(old==null){if(req.status()!=ApplicationStatus.NEW)throw new BadRequestException("Новая заявка создается со статусом NEW");rules.future(req.scheduledAt());}
      else {if(old.getArchived()||Set.of(ApplicationStatus.COMPLETED,ApplicationStatus.CANCELLED).contains(old.getStatus()))throw new BadRequestException("Архивную или закрытую заявку нельзя обновить импортом");rules.transition(old.getStatus(),req.status());if(!old.getScheduledAt().equals(req.scheduledAt()))rules.future(req.scheduledAt());}
      if(Set.of(ApplicationStatus.NEW,ApplicationStatus.APPROVED).contains(req.status())) {
       final Long oldId=old==null?null:old.getId();
       if(repo.findByApplicantIdAndDisciplineIdAndScheduledAtAndArchivedFalseAndStatusIn(person.getId(),d.getId(),req.scheduledAt(),List.of(ApplicationStatus.NEW,ApplicationStatus.APPROVED)).stream().anyMatch(other->!other.getId().equals(oldId)))throw new BadRequestException("У кандидата уже есть активная заявка на эту дисциплину и время");
       String slot=person.getPublicId()+"/"+d.getId()+"/"+req.scheduledAt();if(!slots.add(slot))throw new BadRequestException("В CSV повторяется кандидат, дисциплина и время");
      }
      rows.add(new ImportRow(line,old==null?null:old.getPublicId(),req));
     }catch(BadRequestException e){errors.add(new RowIssue(line,e.getMessage()));}catch(IllegalArgumentException e){errors.add(new RowIssue(line,"Проверьте дату ISO, статус и целые числовые значения"));}
    }
   }
   if(rows.isEmpty()&&errors.isEmpty())throw new BadRequestException("В CSV нет строк данных");
   CsvImportPreview p=new CsvImportPreview();p.setId(UUID.randomUUID());p.setOwnerId(user.requirePrincipal().id());p.setMode(mode.name());p.setFileHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));p.setExpiresAt(Instant.now().plusSeconds(900));p.setContent(json.writeValueAsString(rows));previews.saveAndFlush(p);
   return new ImportPreview(p.getId(),rows.size(),errors.size(),List.copyOf(errors),p.getExpiresAt(),p.getFileHash());
  }catch(BadRequestException e){throw e;}catch(Exception e){throw new BadRequestException("Не удалось прочитать CSV: требуется корректный UTF-8 и формат образца");}
 }
 public ImportResult apply(UUID id){
  String content=tx.execute(t->{CsvImportPreview p=previews.lockById(id).orElseThrow(()->new NotFoundException("Предварительная проверка не найдена"));
   if(!p.getOwnerId().equals(user.requirePrincipal().id()))throw new org.springframework.security.access.AccessDeniedException("Чужая проверка CSV");
   if(p.getAppliedAt()!=null)throw new ConflictException("Этот импорт уже применен");if(!p.getExpiresAt().isAfter(Instant.now()))throw new BadRequestException("Проверка устарела. Загрузите файл заново");
   if(p.getContent().equals("[]"))throw new BadRequestException("Нет корректных строк для импорта");p.setAppliedAt(Instant.now());previews.flush();return p.getContent();});
  List<ImportRow> rows;try{rows=json.readValue(content,new TypeReference<List<ImportRow>>(){});}catch(Exception e){throw new IllegalStateException("Stored preview is invalid",e);}
  int created=0,updated=0;List<RowIssue> errors=new ArrayList<>();
  for(ImportRow row:rows){try{if(row.targetId()==null){apps.create(row.request());created++;}else{apps.update(row.targetId(),row.request());updated++;}}
   catch(BadRequestException|ConflictException|NotFoundException e){errors.add(new RowIssue(row.row(),e.getMessage()));}
   catch(DataIntegrityViolationException|org.springframework.orm.ObjectOptimisticLockingFailureException e){errors.add(new RowIssue(row.row(),"Конфликт данных или версии. Выполните проверку заново"));}}
  return new ImportResult(created,updated,errors.size(),List.copyOf(errors));
 }
 @org.springframework.transaction.annotation.Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public void export(ApplicationFilter filter,OutputStream out)throws IOException{
  Writer writer=new OutputStreamWriter(out,StandardCharsets.UTF_8);writer.write('\uFEFF');
  try(CSVPrinter csv=new CSVPrinter(writer,CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())){
   int page=0;Page<ExamApplication> batch;do{batch=repo.findAll(ApplicationSpecifications.from(filter),PageRequest.of(page++,100,Sort.by("id")));
    for(ExamApplication a:batch)csv.printRecord(safe(a.getExternalId()),safe(a.getApplicant().getEmail()),safe(a.getDiscipline().getCode()),a.getScheduledAt(),a.getStatus(),a.getScore()==null?"":a.getScore(),a.getPreparationPercent(),a.getDocumentsVerified(),safe(String.join("|",new TreeSet<>(a.getTags()))));
   }while(batch.hasNext());
  }
 }
 private static String cell(CSVRecord r,String key){String s=r.get(key).trim();if(s.startsWith("''")||(s.length()>1&&s.charAt(0)=='\''&&"=+-@".indexOf(s.charAt(1))>=0))return s.substring(1);return s;}
 public static String safe(String s){if(!s.isEmpty()&&("=+-@'\t\r\n".indexOf(s.charAt(0))>=0))return "'"+s;return s;}
}
