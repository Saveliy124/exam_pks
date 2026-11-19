package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.repository.*;
import ru.mirea.examcenter.kr3.error.*;
import ru.mirea.examcenter.kr3.filter.ApplicationSpecifications;
import java.util.*;
@Service
@Transactional
public class DirectoryService {
 private final ApplicantRepository applicants;private final ExaminerRepository examiners;private final ExamDisciplineRepository disciplines;private final AuditService audit;
 public DirectoryService(ApplicantRepository a,ExaminerRepository e,ExamDisciplineRepository d,AuditService audit){this.applicants=a;this.examiners=e;this.disciplines=d;this.audit=audit;}
 public static void version(Long expected,long actual){if(expected==null)throw new BadRequestException("Укажите версию записи");if(expected!=actual)throw new ConflictException("Запись уже изменена. Обновите данные");}
 public static String text(String s,int min){String t=s==null?"":s.trim();if(t.length()<min)throw new BadRequestException("Поле не должно быть пустым или слишком коротким");return t;}
 @Transactional(readOnly=true) public Page<ApplicantResponse> applicants(String q,Pageable p){String like="%"+ApplicationSpecifications.escape(q==null?"":q.trim())+"%";return applicants.findAll((r,c,b)->b.or(b.like(b.lower(r.get("fullName")),like,'\\'),b.like(b.lower(r.get("email")),like,'\\')),p).map(ExamMapper::applicant);}
 public ApplicantResponse saveApplicant(UUID id,ApplicantRequest r){Applicant a=id==null?new Applicant():requireApplicant(id);if(id!=null)version(r.version(),a.getVersion());a.setPublicId(id==null?UUID.randomUUID():id);a.setFullName(text(r.fullName(),2));a.setEmail(r.email().trim().toLowerCase(Locale.ROOT));a.setPhone(r.phone()==null?"":r.phone().trim());a.setStatus(r.status());applicants.saveAndFlush(a);audit.record(id==null?"APPLICANT_CREATED":"APPLICANT_UPDATED","APPLICANT",a.getPublicId(),"Сохранен кандидат",Map.of());return ExamMapper.applicant(a);}
 @Transactional(readOnly=true) public Applicant requireApplicant(UUID id){return applicants.findByPublicId(id).orElseThrow(()->new NotFoundException("Кандидат не найден"));}
 @Transactional(readOnly=true) public Page<ExaminerResponse> examiners(String q,Pageable p){String like="%"+ApplicationSpecifications.escape(q==null?"":q.trim())+"%";return examiners.findAll((r,c,b)->b.or(b.like(b.lower(r.get("fullName")),like,'\\'),b.like(b.lower(r.get("subject")),like,'\\')),p).map(ExamMapper::examiner);}
 public ExaminerResponse saveExaminer(UUID id,ExaminerRequest r){Examiner a=id==null?new Examiner():examiners.findByPublicId(id).orElseThrow(()->new NotFoundException("Экзаменатор не найден"));if(id!=null)version(r.version(),a.getVersion());a.setPublicId(id==null?UUID.randomUUID():id);a.setFullName(text(r.fullName(),2));a.setEmail(r.email().trim().toLowerCase(Locale.ROOT));a.setSubject(text(r.subject(),2));a.setStatus(r.status());examiners.saveAndFlush(a);audit.record(id==null?"EXAMINER_CREATED":"EXAMINER_UPDATED","EXAMINER",a.getPublicId(),"Сохранен экзаменатор",Map.of());return ExamMapper.examiner(a);}
 @Transactional(readOnly=true) public List<DisciplineResponse> disciplines(boolean activeOnly){return disciplines.findAll(Sort.by("code")).stream().filter(d->!activeOnly||d.getActive()).map(ExamMapper::discipline).toList();}
 public DisciplineResponse saveDiscipline(UUID id,DisciplineRequest r){ExamDiscipline d=id==null?new ExamDiscipline():requireDiscipline(id);if(id!=null)version(r.version(),d.getVersion());d.setId(id==null?UUID.randomUUID():id);d.setCode(text(r.code(),1).toUpperCase(Locale.ROOT));d.setName(text(r.name(),2));d.setActive(r.active());disciplines.saveAndFlush(d);audit.record(id==null?"DISCIPLINE_CREATED":"DISCIPLINE_UPDATED","DISCIPLINE",d.getId(),"Сохранена дисциплина "+d.getCode(),Map.of("active",d.getActive()));return ExamMapper.discipline(d);}
 public DisciplineResponse active(UUID id,boolean active,long version){ExamDiscipline d=requireDiscipline(id);return saveDiscipline(id,new DisciplineRequest(d.getCode(),d.getName(),active,version));}
 @Transactional(readOnly=true) public ExamDiscipline requireDiscipline(UUID id){return disciplines.findById(id).orElseThrow(()->new NotFoundException("Дисциплина не найдена"));}
}
