package ru.mirea.examcenter.api;

import java.time.LocalDateTime;
import java.util.Map;
import ru.mirea.examcenter.model.*;

/** Explicit request/response contracts. Clients cannot set generated IDs or creation times. */
public final class ApiContracts {
  private ApiContracts() {}

  public record ApplicantInput(
      String fullName, String email, String phone, ApplicantStatus status) {}

  public record ExaminerInput(
      String fullName, String email, String subject, ExaminerStatus status) {}

  public record ApplicationInput(Long applicantId, String examName, LocalDateTime scheduledAt) {}

  public record StatusInput(ApplicationStatus status, Integer score) {}

  public record Created(long id) {}

  public record ApiError(String code, String field, String message) {}

  public record Statistics(
      long applicants,
      long activeExaminers,
      long total,
      Map<ApplicationStatus, Long> byStatus,
      long upcoming,
      Double average,
      Integer minimum,
      Integer maximum,
      double completedPercent) {}
}
