package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Component;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.error.BadRequestException;
import java.time.*;
@Component
public class ExamRules {
 private final Clock clock;
 public ExamRules(Clock clock) {this.clock=clock;}
 public void score(ApplicationStatus state,Integer score,LocalDateTime date) {
  if(state==ApplicationStatus.COMPLETED) {
   if(score==null || score<0 || score>100) throw new BadRequestException("Для завершенной заявки нужен целый балл 0–100");
   if(date.isAfter(LocalDateTime.now(clock))) throw new BadRequestException("Завершить экзамен можно после назначенного времени");
  } else if(score!=null) throw new BadRequestException("Балл указывается только для завершенной заявки");
 }
 public void transition(ApplicationStatus from,ApplicationStatus to) {
  if(from==to)return;
  boolean allowed=(from==ApplicationStatus.NEW && (to==ApplicationStatus.APPROVED || to==ApplicationStatus.CANCELLED))
   || (from==ApplicationStatus.APPROVED && (to==ApplicationStatus.COMPLETED || to==ApplicationStatus.CANCELLED));
  if(!allowed)throw new BadRequestException("Недопустимый переход статуса: "+from+" → "+to);
 }
 public void future(LocalDateTime date) {
  if(!date.isAfter(LocalDateTime.now(clock)))throw new BadRequestException("Дата нового или перенесенного экзамена должна быть в будущем");
 }
}
