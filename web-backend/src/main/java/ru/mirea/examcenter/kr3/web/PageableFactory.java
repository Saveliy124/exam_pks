package ru.mirea.examcenter.kr3.web;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import ru.mirea.examcenter.kr3.error.BadRequestException;
import java.util.*;
@Component
public class PageableFactory {
 private Pageable make(int page,int size,String sort,Map<String,String> allowed) {
  if(page<0 || size<1 || size>100)throw new BadRequestException("Страница от 0, размер от 1 до 100");
  String[] parts=sort.split(",",-1);
  if(parts.length!=2 || !allowed.containsKey(parts[0]) || !Set.of("asc","desc").contains(parts[1]))throw new BadRequestException("Недопустимая сортировка");
  return PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(parts[1]),allowed.get(parts[0])).and(Sort.by("id")));
 }
 public Pageable applications(int p,int s,String sort) {return make(p,s,sort,Map.of("externalId","externalId","scheduledAt","scheduledAt","status","status","preparationPercent","preparationPercent","createdAt","createdAt","updatedAt","updatedAt","applicantName","applicant.fullName","disciplineName","discipline.name"));}
 public Pageable users(int p,int s,String sort) {return make(p,s,sort,Map.of("email","email","displayName","displayName","createdAt","createdAt"));}
 public Pageable audit(int p,int s,String sort) {return make(p,s,sort,Map.of("occurred","occurredAt","eventType","eventType","actorEmail","actorEmail"));}
 public Pageable people(int p,int s,String sort) {return make(p,s,sort,Map.of("fullName","fullName","email","email","status","status"));}
}
