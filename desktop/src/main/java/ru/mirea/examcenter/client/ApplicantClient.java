package ru.mirea.examcenter.client;

import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.ApplicantStatus;

public final class ApplicantClient {
  private final ApiClient api;

  public ApplicantClient(ApiClient api) {
    this.api = api;
  }

  public long createApplicant(String name, String email, String phone, ApplicantStatus status) {
    return api.call(
            "POST",
            "/api/applicants",
            new ApplicantInput(name, email, phone, status),
            Created.class)
        .id();
  }

  public void updateApplicant(
      long id, String name, String email, String phone, ApplicantStatus status) {
    api.call(
        "PUT", "/api/applicants/" + id, new ApplicantInput(name, email, phone, status), Void.class);
  }

  public void deleteApplicant(long id) {
    api.call("DELETE", "/api/applicants/" + id, null, Void.class);
  }
}
