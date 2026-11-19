package ru.mirea.examcenter.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.function.Predicate;
import ru.mirea.examcenter.model.*;

public final class Filters {
  private Filters() {}

  public static String normalize(String value) {
    return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
  }

  public static void period(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw new ValidationException("period", "Начало периода должно быть не позже окончания.");
  }

  public static boolean inPeriod(LocalDateTime value, LocalDate from, LocalDate to) {
    // Comparing LocalDate includes the whole last day, including fractional seconds.
    return (from == null || !value.toLocalDate().isBefore(from))
        && (to == null || !value.toLocalDate().isAfter(to));
  }

  public static Predicate<ExamApplication> applications(
      String search, ApplicationStatus status, LocalDate from, LocalDate to, Long applicantId) {
    period(from, to);
    String q = normalize(search);
    return a ->
        (q.isEmpty()
                || normalize(a.getExamName()).contains(q)
                || normalize(a.getApplicantName()).contains(q)
                || normalize(a.getApplicantEmail()).contains(q))
            && (status == null || a.getStatus() == status)
            && inPeriod(a.getScheduledAt(), from, to)
            && (applicantId == null || a.getApplicantId() == applicantId);
  }

  public static Predicate<Applicant> applicants(
      String search, ApplicantStatus status, String domain) {
    String q = normalize(search), d = normalize(domain).replaceFirst("^@", "");
    return a ->
        (q.isEmpty()
                || normalize(a.getFullName()).contains(q)
                || normalize(a.getEmail()).contains(q)
                || normalize(a.getPhone()).contains(q))
            && (status == null || a.getStatus() == status)
            && (d.isEmpty() || normalize(a.getEmail()).endsWith("@" + d));
  }

  public static Predicate<Examiner> examiners(
      String search, ExaminerStatus status, String subject) {
    String q = normalize(search), s = normalize(subject);
    return e ->
        (q.isEmpty()
                || normalize(e.getFullName()).contains(q)
                || normalize(e.getEmail()).contains(q)
                || normalize(e.getSubject()).contains(q))
            && (status == null || e.getStatus() == status)
            && normalize(e.getSubject()).contains(s);
  }
}
