package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.client.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.repository.Database;
import ru.mirea.examcenter.service.*;

class RestApiIT {
  static TestServer server;
  static final ApiClient api = new ApiClient();
  static final HttpClient http = HttpClient.newHttpClient();
  static final ObjectMapper json = new ObjectMapper();

  @BeforeAll
  static void start() throws Exception {
    TestDatabase.configure();
    server = new TestServer();
    System.setProperty("exam.api.url", server.url());
  }

  @BeforeEach
  void fixture() throws Exception {
    TestDatabase.fixture();
  }

  @AfterAll
  static void finish() throws Exception {
    if (server != null) server.close();
    TestDatabase.clear();
    System.clearProperty("exam.api.url");
    System.clearProperty("exam.db.url");
  }

  HttpResponse<String> request(String method, String path, String body) throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create(server.url() + path))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15));
    return http.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void completeCrudThroughHttpAndPersistentDatabase() throws Exception {
    var people = new ApplicantClient(api);
    var exams = new ExaminerClient(api);
    var apps = new ApplicationClient(api);
    long person =
        people.createApplicant("API Кандидат", " API@example.org ", "123", ApplicantStatus.ACTIVE);
    assertThat(api.snapshot().applicants())
        .anyMatch(p -> p.getId() == person && p.getEmail().equals("api@example.org"));
    people.updateApplicant(person, "Новое имя", "api@example.org", "456", ApplicantStatus.ACTIVE);
    assertThat(api.call("GET", "/api/applicants/" + person, null, Applicant.class).getFullName())
        .isEqualTo("Новое имя");
    long examiner =
        exams.createExaminer(
            "API Экзаменатор", "api.exam@example.org", "Физика", ExaminerStatus.ACTIVE);
    exams.updateExaminer(
        examiner, "API Экзаменатор", "api.exam@example.org", "Математика", ExaminerStatus.INACTIVE);
    assertThat(api.call("GET", "/api/examiners/" + examiner, null, Examiner.class).getStatus())
        .isEqualTo(ExaminerStatus.INACTIVE);
    long application =
        apps.createApplication(person, "API Экзамен", LocalDateTime.now().plusDays(2).withNano(0));
    apps.updateApplication(
        application, person, "API Экзамен новый", LocalDateTime.now().plusDays(3).withNano(0));
    apps.changeStatus(application, ApplicationStatus.APPROVED, null);
    assertThat(
            api.call("GET", "/api/applications/" + application, null, ExamApplication.class)
                .getStatus())
        .isEqualTo(ApplicationStatus.APPROVED);
    assertThatThrownBy(() -> people.deleteApplicant(person)).hasMessageContaining("заявки");
    apps.changeStatus(application, ApplicationStatus.CANCELLED, null);
    apps.deleteApplication(application);
    people.deleteApplicant(person);
    exams.deleteExaminer(examiner);
    assertThat(api.snapshot().applicants()).hasSize(6);
    assertThat(api.snapshot().applications()).hasSize(10);
    try (var c = Database.connect();
        var s = c.createStatement();
        var rs = s.executeQuery("SELECT count(*) FROM applicants WHERE email='api@example.org'")) {
      rs.next();
      assertThat(rs.getInt(1)).isZero();
    }
  }

  @Test
  void serverValidatesBypassedForms() throws Exception {
    assertThat(
            request(
                    "POST",
                    "/api/applicants",
                    "{\"fullName\":\"\",\"email\":\"bad\",\"status\":\"ACTIVE\"}")
                .statusCode())
        .isEqualTo(400);
    var people = new ApplicantClient(api);
    var apps = new ApplicationClient(api);
    assertThatThrownBy(() -> people.createApplicant("Имя", "bad", "", ApplicantStatus.ACTIVE))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(
            () ->
                people.createApplicant("Имя", "ANNA.DEMO@example.org", "", ApplicantStatus.ACTIVE))
        .hasMessageContaining("используется");
    long blocked =
        api.snapshot().applicants().stream()
            .filter(p -> p.getStatus() == ApplicantStatus.BLOCKED)
            .findFirst()
            .orElseThrow()
            .getId();
    assertThatThrownBy(
            () -> apps.createApplication(blocked, "Экзамен", LocalDateTime.now().plusDays(1)))
        .hasMessageContaining("Заблокированный");
    long active =
        api.snapshot().applicants().stream()
            .filter(p -> p.getStatus() == ApplicantStatus.ACTIVE)
            .findFirst()
            .orElseThrow()
            .getId();
    assertThatThrownBy(
            () -> apps.createApplication(active, "Экзамен", LocalDateTime.now().minusDays(1)))
        .hasMessageContaining("будущем");
    var time = LocalDateTime.now().plusDays(1).withNano(0);
    apps.createApplication(active, "Физика API", time);
    assertThatThrownBy(() -> apps.createApplication(active, "физика api", time))
        .hasMessageContaining("активная");
  }

  @Test
  void zeroScoreAndTransitionsSurviveJsonRoundTrip() {
    var app =
        api.snapshot().applications().stream()
            .filter(
                a ->
                    a.getStatus() == ApplicationStatus.APPROVED
                        && a.getScheduledAt().isBefore(LocalDateTime.now()))
            .findFirst()
            .orElseThrow();
    var apps = new ApplicationClient(api);
    assertThatThrownBy(() -> apps.changeStatus(app.getId(), ApplicationStatus.COMPLETED, 101))
        .hasMessageContaining("0 до 100");
    apps.changeStatus(app.getId(), ApplicationStatus.COMPLETED, 0);
    assertThat(
            api.call("GET", "/api/applications/" + app.getId(), null, ExamApplication.class)
                .getScore())
        .isZero();
    assertThatThrownBy(() -> apps.changeStatus(app.getId(), ApplicationStatus.CANCELLED, null))
        .hasMessageContaining("запрещён");
    assertThat(api.snapshot().applications()).anyMatch(a -> a.getScore() == null);
  }

  @Test
  void malformedJsonTypesUnknownFieldsAndEnumsAreRejected() throws Exception {
    for (String payload :
        List.of(
            "{",
            "null",
            "{\"applicantId\":1,\"examName\":\"Экзамен\",\"scheduledAt\":\"bad\"}",
            "{\"applicantId\":1,\"examName\":\"Экзамен\",\"scheduledAt\":null,\"id\":7}"))
      assertThat(request("POST", "/api/applications", payload).statusCode()).isEqualTo(400);
    assertThat(
            request(
                    "PATCH",
                    "/api/applications/1/status",
                    "{\"status\":\"COMPLETED\",\"score\":1.5}")
                .statusCode())
        .isEqualTo(400);
    assertThat(request("PATCH", "/api/applications/1/status", "{\"status\":\"HACK\"}").statusCode())
        .isEqualTo(400);
    assertThat(request("GET", "/api/statistics?from=wrong", null).statusCode()).isEqualTo(400);
    assertThat(request("GET", "/api/applications/not-a-number", null).statusCode()).isEqualTo(400);
  }

  @Test
  void notFoundMethodsAndHealthHaveStableContract() throws Exception {
    var response = request("GET", "/api/applicants/999999", null);
    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(json.readTree(response.body()).has("message")).isTrue();
    assertThat(response.body()).doesNotContain("SQLException", "stackTrace", "password");
    assertThat(request("GET", "/actuator/health", null).body()).contains("UP");
    assertThat(request("GET", "/api/unknown", null).statusCode()).isEqualTo(404);
    assertThat(request("PATCH", "/api/applicants/1", "{}").statusCode()).isEqualTo(405);
  }

  @Test
  void filtersStatisticsAndEmptyPeriodsAreCalculatedOnServer() throws Exception {
    var response = request("GET", "/api/applications?status=COMPLETED&search=demo", null);
    assertThat(json.readTree(response.body()).size()).isEqualTo(3);
    var all = api.statistics(null, null);
    assertThat(all.total()).isEqualTo(10);
    assertThat(all.minimum()).isZero();
    assertThat(all.maximum()).isEqualTo(100);
    assertThat(all.average()).isCloseTo(64.0, within(0.001));
    assertThat(all.completedPercent()).isEqualTo(30.0);
    var empty = api.statistics(LocalDate.now().plusYears(1), null);
    assertThat(empty.total()).isZero();
    assertThat(empty.average()).isNull();
    assertThat(request("GET", "/api/applications?from=2030-01-02&to=2030-01-01", null).statusCode())
        .isEqualTo(400);
  }

  @Test
  void concurrentDuplicateEmailCannotProduceTwoRows() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      String payload =
          "{\"fullName\":\"Параллельный\",\"email\":\"race@example.org\",\"phone\":\"\",\"status\":\"ACTIVE\"}";
      var futures =
          List.of(
              executor.submit(() -> request("POST", "/api/applicants", payload).statusCode()),
              executor.submit(() -> request("POST", "/api/applicants", payload).statusCode()));
      var statuses = List.of(futures.get(0).get(), futures.get(1).get());
      assertThat(statuses).contains(201);
      assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
      assertThat(statuses).allMatch(s -> s == 201 || s == 400 || s == 409);
      assertThat(
              api.snapshot().applicants().stream()
                  .filter(p -> p.getEmail().equals("race@example.org"))
                  .count())
          .isEqualTo(1);
    }
  }

  @Test
  void springTransactionRollsBackRepositoryWrite() {
    var service = server.context.getBean(ApplicantService.class);
    var tx = new TransactionTemplate(server.context.getBean(PlatformTransactionManager.class));
    assertThatThrownBy(
            () ->
                tx.execute(
                    status -> {
                      try {
                        service.createApplicant(
                            "Откат", "rollback@example.org", "", ApplicantStatus.ACTIVE);
                      } catch (SQLException e) {
                        throw new RuntimeException(e);
                      }
                      throw new IllegalStateException("rollback");
                    }))
        .hasMessage("rollback");
    assertThat(api.snapshot().applicants())
        .noneMatch(p -> p.getEmail().equals("rollback@example.org"));
  }
}
