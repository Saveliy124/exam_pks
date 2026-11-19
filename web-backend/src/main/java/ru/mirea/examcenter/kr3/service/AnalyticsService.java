package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.examcenter.kr3.repository.ExamApplicationRepository;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import java.util.*;
@Service
public class AnalyticsService {
 private final ExamApplicationRepository apps;
 public AnalyticsService(ExamApplicationRepository apps){this.apps=apps;}
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public AnalyticsResponse summary(){return new AnalyticsResponse(apps.count(),apps.countByArchived(false),apps.countByArchived(true),apps.countByDocumentsVerifiedTrue(),apps.averagePreparation(),counts(apps.byDiscipline()),counts(apps.byStatus()),counts(apps.byRisk()));}
 private List<NamedCount> counts(List<Object[]> rows){return rows.stream().map(r->new NamedCount(r[0].toString(),((Number)r[1]).longValue())).sorted(Comparator.comparing(NamedCount::name)).toList();}
}
