package ru.mirea.examcenter.kr3.service;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import java.util.*;
public final class ExamMapper {
 private ExamMapper() { }
 public static ApplicationResponse application(ExamApplication a) {return new ApplicationResponse(a.getPublicId(),a.getExternalId(),a.getApplicant().getPublicId(),a.getApplicant().getFullName(),a.getDiscipline().getId(),a.getDiscipline().getName(),a.getDiscipline().getCode(),a.getScheduledAt(),a.getStatus(),a.getScore()==null?null:a.getScore().intValue(),a.getPreparationPercent(),a.getDocumentsVerified(),a.getRiskLevel(),a.getArchived(),a.getArchivedAt(),Set.copyOf(a.getTags()),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion());}
 public static ApplicantResponse applicant(Applicant a) {return new ApplicantResponse(a.getPublicId(),a.getFullName(),a.getEmail(),a.getPhone(),a.getStatus(),a.getVersion());}
 public static ExaminerResponse examiner(Examiner a) {return new ExaminerResponse(a.getPublicId(),a.getFullName(),a.getEmail(),a.getSubject(),a.getStatus(),a.getVersion());}
 public static DisciplineResponse discipline(ExamDiscipline a) {return new DisciplineResponse(a.getId(),a.getCode(),a.getName(),a.getActive(),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion());}
 public static NoteResponse note(ApplicationNote a) {return new NoteResponse(a.getId(),a.getApplication().getPublicId(),a.getAuthor().getId(),a.getAuthor().getDisplayName(),a.getBody(),a.getPinned(),a.getCreatedAt(),a.getUpdatedAt(),a.getVersion());}
}
