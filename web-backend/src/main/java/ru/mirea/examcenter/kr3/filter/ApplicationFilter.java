package ru.mirea.examcenter.kr3.filter;
import jakarta.validation.constraints.*;
import ru.mirea.examcenter.kr3.domain.*;
import java.util.UUID;
public record ApplicationFilter(@Size(max=500) String q,UUID disciplineId,ApplicationStatus status,
 Boolean documentsVerified,RiskLevel risk,@Size(max=50) String tag,ArchiveMode archive,
 Comparison preparationOp,@Min(0) @Max(100) Integer preparationValue) {
 public enum ArchiveMode { ACTIVE, ARCHIVED, ALL }
 public enum Comparison { LT,LTE,EQ,GTE,GT }
 @AssertTrue(message="Для сравнения готовности нужны оператор и значение")
 public boolean isComparisonValid() {return (preparationOp==null)==(preparationValue==null);}
}
