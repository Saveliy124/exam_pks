package ru.mirea.examcenter.kr3.filter;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.examcenter.kr3.domain.ExamApplication;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
public final class ApplicationSpecifications {
 private ApplicationSpecifications() { }
 public static String escape(String value) {return value.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
 public static Specification<ExamApplication> from(ApplicationFilter f) {
  return (r,q,cb)-> {
   List<Predicate> p=new ArrayList<>();
   if(f.archive()!=ApplicationFilter.ArchiveMode.ALL)p.add(cb.equal(r.get("archived"),f.archive()==ApplicationFilter.ArchiveMode.ARCHIVED));
   if(f.disciplineId()!=null)p.add(cb.equal(r.get("discipline").get("id"),f.disciplineId()));
   if(f.status()!=null)p.add(cb.equal(r.get("status"),f.status()));
   if(f.documentsVerified()!=null)p.add(cb.equal(r.get("documentsVerified"),f.documentsVerified()));
   if(f.risk()!=null)p.add(cb.equal(r.get("riskLevel"),f.risk()));
   if(f.tag()!=null&&!f.tag().isBlank())p.add(cb.isMember(f.tag().trim().toLowerCase(Locale.ROOT),r.get("tags")));
   if(f.q()!=null&&!f.q().isBlank()) {
    String s="%"+escape(f.q().trim())+"%";
    p.add(cb.or(cb.like(cb.lower(r.get("externalId")),s,'\\'),cb.like(cb.lower(r.get("examName")),s,'\\'),
     cb.like(cb.lower(r.get("applicant").get("fullName")),s,'\\'),cb.like(cb.lower(r.get("applicant").get("email")),s,'\\')));
   }
   if(f.preparationOp()!=null && f.preparationValue()!=null) p.add(switch(f.preparationOp()) {
    case LT->cb.lt(r.get("preparationPercent"),f.preparationValue());case LTE->cb.le(r.get("preparationPercent"),f.preparationValue());
    case EQ->cb.equal(r.get("preparationPercent"),f.preparationValue());case GTE->cb.ge(r.get("preparationPercent"),f.preparationValue());
    case GT->cb.gt(r.get("preparationPercent"),f.preparationValue());
   });
   return cb.and(p.toArray(Predicate[]::new));
  };
 }
}
