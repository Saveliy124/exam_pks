package ru.mirea.examcenter.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import ru.mirea.examcenter.model.ApplicationStatus;

/** Pure validation, also used by forms before submitting JDBC work. */
public final class Rules {
  private Rules() {}

  public static String text(String field, String value, String label, int min, int max) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.length() < min || normalized.length() > max)
      throw new ValidationException(field, label + ": от " + min + " до " + max + " символов.");
    return normalized;
  }

  public static String email(String input) {
    String email = text("email", input, "Email", 3, 254).toLowerCase(Locale.ROOT);
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
      throw new ValidationException("email", "Укажите email в формате name@example.org.");
    return email;
  }

  public static <T> T required(String field, T value, String message) {
    if (value == null) throw new ValidationException(field, message);
    return value;
  }

  public static void future(LocalDateTime date, Clock clock) {
    if (date == null || !date.isAfter(LocalDateTime.now(clock)))
      throw new ValidationException("date", "Дата и время экзамена должны быть в будущем.");
  }

  public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
    return from == ApplicationStatus.NEW
            && (to == ApplicationStatus.APPROVED || to == ApplicationStatus.CANCELLED)
        || from == ApplicationStatus.APPROVED
            && (to == ApplicationStatus.COMPLETED || to == ApplicationStatus.CANCELLED);
  }

  public static void transition(
      ApplicationStatus from,
      ApplicationStatus to,
      LocalDateTime scheduled,
      Integer score,
      Clock clock) {
    if (!canTransition(from, to))
      throw new ValidationException("status", "Этот переход статуса запрещён.");
    if (to == ApplicationStatus.COMPLETED) {
      if (scheduled.isAfter(LocalDateTime.now(clock)))
        throw new ValidationException("date", "Нельзя завершить экзамен до назначенного времени.");
      if (score == null || score < 0 || score > 100)
        throw new ValidationException("score", "Укажите целый балл от 0 до 100.");
    } else if (score != null)
      throw new ValidationException("score", "Балл разрешён только для завершённого экзамена.");
  }
}
