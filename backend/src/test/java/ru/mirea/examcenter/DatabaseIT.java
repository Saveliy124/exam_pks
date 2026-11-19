package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import org.junit.jupiter.api.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.repository.*;
import ru.mirea.examcenter.service.*;

class DatabaseIT {
  private final ApplicantService applicants = new ApplicantService(new ApplicantRepository());
  private final ExaminerService examiners = new ExaminerService(new ExaminerRepository());
  private final ExamApplicationRepository repository = new ExamApplicationRepository();
  private final ExamApplicationService applications =
      new ExamApplicationService(
          repository,
          applicants,
          java.time.Clock.fixed(
              java.time.Instant.parse("2026-10-16T09:00:00Z"),
              java.time.ZoneId.of("Europe/Moscow")));

  @BeforeAll
  static void prepare() throws Exception {
    TestDatabase.configure();
  }

  @BeforeEach
  void clear() throws Exception {
    TestDatabase.clear();
  }

  @AfterAll
  static void clean() throws Exception {
    TestDatabase.clear();
    System.clearProperty("exam.db.url");
  }

  @Test
  void fullCrudAndBusinessLifecycle() throws Exception {
    long person =
        applicants.createApplicant(
            "Тест Кандидат", "CANDIDATE@example.org", "123", ApplicantStatus.ACTIVE);
    assertThat(applicants.getApplicant(person).getEmail()).isEqualTo("candidate@example.org");
    long examiner =
        examiners.createExaminer(
            "Экзаменатор", "examiner@example.org", "Физика", ExaminerStatus.ACTIVE);
    examiners.updateExaminer(
        examiner, "Экзаменатор", "examiner@example.org", "Математика", ExaminerStatus.INACTIVE);
    assertThat(examiners.getExaminer(examiner).getSubject()).isEqualTo("Математика");
    long application =
        applications.createApplication(
            person, "Физика", java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(1));
    assertThat(applications.getApplication(application).getApplicantName())
        .isEqualTo("Тест Кандидат");
    applications.updateApplication(
        application,
        person,
        "Математика",
        java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(2));
    assertThat(applications.getApplication(application).getExamName()).isEqualTo("Математика");
    assertThatThrownBy(
            () ->
                applications.createApplication(
                    person,
                    "математика",
                    java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(2)))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> applicants.deleteApplicant(person))
        .isInstanceOf(ValidationException.class);
    applications.changeStatus(application, ApplicationStatus.APPROVED, null);
    assertThatThrownBy(
            () -> applications.changeStatus(application, ApplicationStatus.COMPLETED, 80))
        .isInstanceOf(ValidationException.class);
    try (var c = Database.connect();
        var s = c.prepareStatement("UPDATE exam_applications SET scheduled_at=? WHERE id=?")) {
      s.setTimestamp(
          1, Timestamp.valueOf(java.time.LocalDateTime.of(2026, 10, 16, 12, 0).minusMinutes(1)));
      s.setLong(2, application);
      s.executeUpdate();
    }
    applications.changeStatus(application, ApplicationStatus.COMPLETED, 0);
    assertThat(applications.getApplication(application).getScore()).isZero();
    assertThatThrownBy(
            () ->
                applications.updateApplication(
                    application,
                    person,
                    "Физика",
                    java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(1)))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(
            () -> applications.changeStatus(application, ApplicationStatus.CANCELLED, null))
        .isInstanceOf(ValidationException.class);
    var snapshot = new CenterRepository().load();
    assertThat(snapshot.applicants()).hasSize(1);
    assertThat(snapshot.applications()).hasSize(1);
    assertThat(snapshot.examiners()).hasSize(1);
    applications.deleteApplication(application);
    applicants.deleteApplicant(person);
    examiners.deleteExaminer(examiner);
    assertThat(new CenterRepository().load().applications()).isEmpty();
  }

  @Test
  void checksBlockedMissingAndUniquePeople() throws Exception {
    long person =
        applicants.createApplicant("Кандидат", "one@example.org", "", ApplicantStatus.BLOCKED);
    assertThatThrownBy(
            () ->
                applications.createApplication(
                    person, "Физика", java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(1)))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(
            () ->
                applications.createApplication(
                    99999, "Физика", java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(1)))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(
            () ->
                applicants.createApplicant(
                    "Второй", " ONE@EXAMPLE.ORG ", "", ApplicantStatus.ACTIVE))
        .hasMessageContaining("email");
    assertThatThrownBy(
            () ->
                new ApplicantRepository()
                    .create("Второй", "ONE@example.org", "", ApplicantStatus.ACTIVE))
        .isInstanceOf(SQLException.class)
        .extracting(e -> ((SQLException) e).getSQLState())
        .isEqualTo("23505");
    applicants.updateApplicant(person, "Кандидат", "one@example.org", "", ApplicantStatus.ACTIVE);
    assertThat(applicants.getApplicant(person).getStatus()).isEqualTo(ApplicantStatus.ACTIVE);
  }

  @Test
  void databaseGuardsDuplicateSlotAndStaleStatus() throws Exception {
    long person =
        applicants.createApplicant("Кандидат", "one@example.org", "", ApplicantStatus.ACTIVE);
    var time = java.time.LocalDateTime.of(2026, 10, 16, 12, 0).plusDays(1);
    long id = applications.createApplication(person, "Физика", time);
    assertThatThrownBy(() -> repository.createApplication(person, "физика", time))
        .isInstanceOf(SQLException.class)
        .extracting(e -> ((SQLException) e).getSQLState())
        .isEqualTo("23505");
    applications.changeStatus(id, ApplicationStatus.APPROVED, null);
    assertThatThrownBy(
            () ->
                repository.updateStatus(
                    id,
                    ApplicationStatus.NEW,
                    ApplicationStatus.CANCELLED,
                    null,
                    java.time.LocalDateTime.of(2026, 10, 16, 12, 0)))
        .isInstanceOf(SQLException.class)
        .extracting(e -> ((SQLException) e).getSQLState())
        .isEqualTo("40001");
    assertThat(applications.getApplication(id).getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    applications.changeStatus(id, ApplicationStatus.CANCELLED, null);
    assertThat(applications.createApplication(person, "Физика", time)).isNotEqualTo(id);
  }

  @Test
  void preparedStatementsTreatSqlLikeNameAsData() throws Exception {
    String text = "О'Брайен'); DROP TABLE applicants; --";
    long person = applicants.createApplicant(text, "name@example.org", "", ApplicantStatus.ACTIVE);
    assertThat(applicants.getApplicant(person).getFullName()).isEqualTo(text);
    assertThat(applicants.allApplicants()).hasSize(1);
  }
}
