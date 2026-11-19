package ru.mirea.examcenter.client;

import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.ExaminerStatus;

public final class ExaminerClient {
  private final ApiClient api;

  public ExaminerClient(ApiClient api) {
    this.api = api;
  }

  public long createExaminer(String name, String email, String subject, ExaminerStatus status) {
    return api.call(
            "POST",
            "/api/examiners",
            new ExaminerInput(name, email, subject, status),
            Created.class)
        .id();
  }

  public void updateExaminer(
      long id, String name, String email, String subject, ExaminerStatus status) {
    api.call(
        "PUT", "/api/examiners/" + id, new ExaminerInput(name, email, subject, status), Void.class);
  }

  public void deleteExaminer(long id) {
    api.call("DELETE", "/api/examiners/" + id, null, Void.class);
  }
}
