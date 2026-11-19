package ru.mirea.examcenter.client;

import java.time.LocalDateTime;
import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.ApplicationStatus;

public final class ApplicationClient {
  private final ApiClient api;

  public ApplicationClient(ApiClient api) {
    this.api = api;
  }

  public long createApplication(long person, String exam, LocalDateTime time) {
    return api.call(
            "POST", "/api/applications", new ApplicationInput(person, exam, time), Created.class)
        .id();
  }

  public void updateApplication(long id, long person, String exam, LocalDateTime time) {
    api.call(
        "PUT", "/api/applications/" + id, new ApplicationInput(person, exam, time), Void.class);
  }

  public void deleteApplication(long id) {
    api.call("DELETE", "/api/applications/" + id, null, Void.class);
  }

  public void changeStatus(long id, ApplicationStatus target, Integer score) {
    api.call(
        "PATCH", "/api/applications/" + id + "/status", new StatusInput(target, score), Void.class);
  }
}
