package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Component;
import ru.mirea.examcenter.kr3.domain.*;
@Component
public class RiskCalculator {
 public RiskLevel calculate(ApplicationStatus status,int preparation,boolean verified) {
  if(status==ApplicationStatus.COMPLETED || status==ApplicationStatus.CANCELLED) return RiskLevel.NOT_APPLICABLE;
  if(!verified && preparation<30) return RiskLevel.CRITICAL;
  if(!verified || preparation<50) return RiskLevel.HIGH;
  return preparation<80?RiskLevel.MEDIUM:RiskLevel.LOW;
 }
}
