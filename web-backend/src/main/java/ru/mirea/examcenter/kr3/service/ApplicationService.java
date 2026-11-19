package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.data.domain.*;
import org.springframework.dao.*;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.filter.*;
import ru.mirea.examcenter.kr3.repository.*;
import ru.mirea.examcenter.kr3.error.*;
import ru.mirea.examcenter.kr3.config.AppProperties;
import java.time.*;
import java.util.*;
@Service
public class ApplicationService {
 private final ExamApplicationRepository apps;private final DirectoryService dirs;private final RiskCalculator risk;private final ExamRules rules;private final AuditService audit;private final AppProperties props;private final TransactionTemplate tx;
 public ApplicationService(ExamApplicationRepository apps,DirectoryService dirs,RiskCalculator risk,ExamRules rules,AuditService audit,AppProperties props,PlatformTransactionManager tm){this.apps=apps;this.dirs=dirs;this.risk=risk;this.rules=rules;this.audit=audit;this.props=props;tx=new TransactionTemplate(tm);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
 @Transactional(readOnly=true) public Page<ApplicationResponse> search(ApplicationFilter f,Pageable p){return apps.findAll(ApplicationSpecifications.from(f),p).map(ExamMapper::application);}
 @Transactional(readOnly=true) public ApplicationResponse get(UUID id){return ExamMapper.application(require(id));}
 @Transactional(readOnly=true) public ExamApplication require(UUID id){return apps.findByPublicId(id).orElseThrow(()->new NotFoundException("Заявка не найдена"));}
 @Transactional public ApplicationResponse create(ApplicationRequest r){
  if(r.status()!=ApplicationStatus.NEW)throw new BadRequestException("Новая заявка создается со статусом NEW");rules.future(r.scheduledAt());
  ExamApplication a=new ExamApplication();a.setPublicId(UUID.randomUUID());a.setTags(new LinkedHashSet<>());assign(a,r,true);apps.saveAndFlush(a);event("APPLICATION_CREATED",a);return ExamMapper.application(a);
 }
 @Transactional public ApplicationResponse update(UUID id,ApplicationRequest r){
  ExamApplication a=require(id);DirectoryService.version(r.version(),a.getVersion());
  if(a.getArchived())throw new BadRequestException("Сначала восстановите заявку из архива");
  boolean terminal=a.getStatus()==ApplicationStatus.COMPLETED||a.getStatus()==ApplicationStatus.CANCELLED;
  if(terminal)throw new BadRequestException("Завершенную или отмененную заявку нельзя редактировать");
  rules.transition(a.getStatus(),r.status());
  if(!a.getScheduledAt().equals(r.scheduledAt()))rules.future(r.scheduledAt());
  assign(a,r,false);apps.flush();event("APPLICATION_UPDATED",a);return ExamMapper.application(a);
 }
 private void assign(ExamApplication a,ApplicationRequest r,boolean fresh){
  Applicant person=dirs.requireApplicant(r.applicantId());ExamDiscipline d=dirs.requireDiscipline(r.disciplineId());
  if(person.getStatus()!=ApplicantStatus.ACTIVE)throw new BadRequestException("Кандидат заблокирован");
  if(!d.getActive()&&(fresh||!d.getId().equals(a.getDiscipline().getId())))throw new BadRequestException("Дисциплина отключена");
  rules.score(r.status(),r.score(),r.scheduledAt());a.setExternalId(DirectoryService.text(r.externalId(),1));a.setApplicant(person);a.setDiscipline(d);a.setExamName(d.getName());a.setScheduledAt(r.scheduledAt());a.setStatus(r.status());a.setScore(r.score()==null?null:r.score().shortValue());a.setPreparationPercent(r.preparationPercent());a.setDocumentsVerified(r.documentsVerified());a.setRiskLevel(risk.calculate(a.getStatus(),a.getPreparationPercent(),a.getDocumentsVerified()));a.setTags(tags(r.tags()));
 }
 public static Set<String> tags(Set<String> values){
  if(values==null||values.size()>10)throw new BadRequestException("Не более 10 тегов");Set<String> result=new LinkedHashSet<>();
  for(String s:values){if(s==null||s.isBlank()||s.trim().length()>50||s.indexOf('|')>=0||s.indexOf(',')>=0||s.chars().anyMatch(Character::isISOControl))throw new BadRequestException("Тег: 1–50 символов, без запятой, | и управляющих символов");result.add(s.trim().toLowerCase(Locale.ROOT));}return result;
 }
 @Transactional public ApplicationResponse archive(UUID id,long version){ExamApplication a=require(id);DirectoryService.version(version,a.getVersion());if(!a.getArchived()){a.setArchived(true);a.setArchivedAt(Instant.now());apps.flush();event("APPLICATION_ARCHIVED",a);}return ExamMapper.application(a);}
 @Transactional public ApplicationResponse restore(UUID id,long version){ExamApplication a=require(id);DirectoryService.version(version,a.getVersion());if(a.getArchived()){a.setArchived(false);a.setArchivedAt(null);apps.flush();event("APPLICATION_RESTORED",a);}return ExamMapper.application(a);}
 @Transactional public void permanentDelete(UUID id,long version,String confirmation){
  if(!props.permanentDeleteEnabled())throw new BadRequestException("Окончательное удаление отключено настройкой");ExamApplication a=require(id);DirectoryService.version(version,a.getVersion());
  if(!a.getArchived())throw new BadRequestException("Окончательно удалить можно только архивную заявку");if(!a.getExternalId().equals(confirmation))throw new BadRequestException("Подтвердите код заявки в X-Confirm-Delete");
  event("APPLICATION_DELETED",a);apps.delete(a);apps.flush();
 }
 public BulkResult bulk(BulkRequest r){
  List<UUID> ok=new ArrayList<>();List<BulkFailure> errors=new ArrayList<>();
  for(UUID id:new LinkedHashSet<>(r.ids())){
   try {tx.executeWithoutResult(t->{Long v=r.versions().get(id);if(v==null)throw new BadRequestException("Не указана версия заявки");
    if(r.action()==BulkAction.ARCHIVE){archive(id,v);return;}if(r.action()==BulkAction.RESTORE){restore(id,v);return;}
    ExamApplication a=require(id);DirectoryService.version(v,a.getVersion());if(a.getArchived())throw new BadRequestException("Заявка в архиве");
    if(r.action()==BulkAction.STATUS){if(r.status()==null)throw new BadRequestException("Укажите статус");rules.transition(a.getStatus(),r.status());rules.score(r.status(),r.score(),a.getScheduledAt());a.setStatus(r.status());a.setScore(r.score()==null?null:r.score().shortValue());a.setRiskLevel(risk.calculate(a.getStatus(),a.getPreparationPercent(),a.getDocumentsVerified()));}
    else {String tag=tags(Collections.singleton(r.tag())).iterator().next();Set<String> tags=new LinkedHashSet<>(a.getTags());if(r.action()==BulkAction.ADD_TAG)tags.add(tag);else tags.remove(tag);a.setTags(tags(tags));a.setUpdatedAt(Instant.now());}
    apps.flush();event("APPLICATION_BULK_"+r.action(),a);
   });ok.add(id);}catch(NotFoundException e){errors.add(new BulkFailure(id,404,e.getMessage()));}catch(BadRequestException e){errors.add(new BulkFailure(id,400,e.getMessage()));}catch(ConflictException|DataIntegrityViolationException|org.springframework.orm.ObjectOptimisticLockingFailureException e){errors.add(new BulkFailure(id,409,"Конфликт данных или версии. Обновите список"));}
  }return new BulkResult(List.copyOf(ok),List.copyOf(errors));
 }
 private void event(String type,ExamApplication a){audit.record(type,"EXAM_APPLICATION",a.getPublicId(),"Заявка "+a.getExternalId(),Map.of("status",a.getStatus(),"archived",a.getArchived()));}
}
