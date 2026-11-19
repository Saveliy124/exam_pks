package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ExamApplicationRepository extends JpaRepository<ExamApplication,Long>, JpaSpecificationExecutor<ExamApplication> {
 Optional<ExamApplication> findByPublicId(UUID id);
 Optional<ExamApplication> findByExternalId(String externalId);
 List<ExamApplication> findByApplicantIdAndDisciplineIdAndScheduledAtAndArchivedFalseAndStatusIn(Long applicantId,UUID disciplineId,java.time.LocalDateTime scheduledAt,Collection<ApplicationStatus> statuses);
 boolean existsByApplicantId(Long id);
 long countByArchived(boolean archived); long countByDocumentsVerifiedTrue();
 @Query("select avg(a.preparationPercent) from ExamApplication a") Double averagePreparation();
 @Query("select d.name,count(a) from ExamApplication a join a.discipline d group by d.name order by d.name") List<Object[]> byDiscipline();
 @Query("select a.status,count(a) from ExamApplication a group by a.status") List<Object[]> byStatus();
 @Query("select a.riskLevel,count(a) from ExamApplication a group by a.riskLevel") List<Object[]> byRisk();
}
