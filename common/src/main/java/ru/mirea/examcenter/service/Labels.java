package ru.mirea.examcenter.service;

import ru.mirea.examcenter.model.*;

public final class Labels {
  private Labels() {}

  public static String status(Enum<?> value) {
    if (value == null) return "Все статусы";
    if (value instanceof ApplicationStatus s)
      return switch (s) {
        case NEW -> "Новая";
        case APPROVED -> "Согласована";
        case COMPLETED -> "Завершена";
        case CANCELLED -> "Отменена";
      };
    if (value instanceof ApplicantStatus s)
      return s == ApplicantStatus.ACTIVE ? "Активен" : "Заблокирован";
    if (value instanceof ExaminerStatus s)
      return s == ExaminerStatus.ACTIVE ? "Активен" : "Неактивен";
    return value.toString();
  }
}
