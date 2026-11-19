package ru.mirea.examcenter.service;

import java.util.*;
import ru.mirea.examcenter.export.*;
import ru.mirea.examcenter.model.*;

public final class ExportService {
  private ExportService() {}

  public static ExportTable applicants(List<Applicant> items) {
    return new ExportTable(
        "Кандидаты",
        List.of("ФИО", "Email", "Телефон", "Статус"),
        items.stream()
            .map(
                a ->
                    Arrays.<Object>asList(
                        a.getFullName(), a.getEmail(), a.getPhone(), Labels.status(a.getStatus())))
            .toList());
  }

  public static ExportTable examiners(List<Examiner> items) {
    return new ExportTable(
        "Экзаменаторы",
        List.of("ФИО", "Email", "Предмет", "Статус"),
        items.stream()
            .map(
                e ->
                    Arrays.<Object>asList(
                        e.getFullName(),
                        e.getEmail(),
                        e.getSubject(),
                        Labels.status(e.getStatus())))
            .toList());
  }

  public static ExportTable applications(List<ExamApplication> items) {
    return new ExportTable(
        "Заявки",
        List.of("Кандидат", "Email", "Экзамен", "Дата экзамена", "Статус", "Балл", "Создана"),
        items.stream()
            .map(
                a ->
                    Arrays.<Object>asList(
                        a.getApplicantName(),
                        a.getApplicantEmail(),
                        a.getExamName(),
                        a.getScheduledAt(),
                        Labels.status(a.getStatus()),
                        a.getScore(),
                        a.getCreatedAt()))
            .toList());
  }
}
