package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

class FiltersStatisticsTest {
  private final LocalDateTime time = RulesTest.NOW;

  private ExamApplication application(
      long id, ApplicationStatus status, Integer score, LocalDateTime at) {
    return new ExamApplication(
        id,
        10,
        "Иван Петров",
        "ivan@example.org",
        "Математика",
        at,
        status,
        score,
        time.minusDays(2));
  }

  @Test
  void searchesAllSemanticFieldsAndCombinesFilters() {
    var row = application(1, ApplicationStatus.NEW, null, time);
    for (String query : List.of(" ИВАН ", "example.org", "матем"))
      assertThat(
              Filters.applications(
                      query, ApplicationStatus.NEW, time.toLocalDate(), time.toLocalDate(), 10L)
                  .test(row))
          .isTrue();
    assertThat(Filters.applications("физика", null, null, null, null).test(row)).isFalse();
    assertThat(Filters.applications("иван", ApplicationStatus.APPROVED, null, null, null).test(row))
        .isFalse();
    assertThat(Filters.applications("", null, null, null, 99L).test(row)).isFalse();
  }

  @Test
  void dateRangeIncludesEntireLastDayAndOpenBounds() {
    var date = time.toLocalDate();
    assertThat(Filters.inPeriod(date.atTime(LocalTime.MAX), date, date)).isTrue();
    assertThat(Filters.inPeriod(date.atStartOfDay(), date, date)).isTrue();
    assertThat(Filters.inPeriod(date.plusDays(1).atStartOfDay(), null, date)).isFalse();
    assertThat(Filters.inPeriod(date.minusDays(1).atStartOfDay(), date, null)).isFalse();
    assertThatThrownBy(() -> Filters.period(date.plusDays(1), date))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void directoryFiltersUseWholeEmailDomainAndSubjectSubstring() {
    var applicant =
        new Applicant(1, "Ирина", "irina@example.org", "123456", ApplicantStatus.ACTIVE);
    assertThat(Filters.applicants("345", ApplicantStatus.ACTIVE, "@EXAMPLE.org").test(applicant))
        .isTrue();
    assertThat(Filters.applicants("", null, "ample.org").test(applicant)).isFalse();
    assertThat(Filters.applicants("", ApplicantStatus.BLOCKED, "").test(applicant)).isFalse();
    var examiner =
        new Examiner(1, "Олег", "oleg@example.org", "Высшая математика", ExaminerStatus.ACTIVE);
    assertThat(Filters.examiners("@example", ExaminerStatus.ACTIVE, "МАТЕМ").test(examiner))
        .isTrue();
    assertThat(Filters.examiners("", ExaminerStatus.INACTIVE, "матем").test(examiner)).isFalse();
  }

  @Test
  void statisticsHaveExplicitDenominatorAndCountZeroScore() {
    var rows =
        List.of(
            application(1, ApplicationStatus.COMPLETED, 0, time.minusDays(1)),
            application(2, ApplicationStatus.COMPLETED, 100, time.minusDays(1)),
            application(3, ApplicationStatus.NEW, null, time.plusDays(1)),
            application(4, ApplicationStatus.APPROVED, null, time.plusDays(2)),
            application(5, ApplicationStatus.CANCELLED, null, time.plusDays(3)));
    var snapshot =
        new CenterSnapshot(
            List.of(new Applicant(1, "Иван", "a@b.ru", "", ApplicantStatus.ACTIVE)),
            rows,
            List.of(
                new Examiner(1, "Олег", "b@b.ru", "Физика", ExaminerStatus.ACTIVE),
                new Examiner(2, "Анна", "c@b.ru", "Химия", ExaminerStatus.INACTIVE)));
    var service = new StatisticsService(RulesTest.CLOCK);
    var stats = service.calculate(snapshot, null, null);
    assertThat(stats.total()).isEqualTo(5);
    assertThat(stats.upcoming()).isEqualTo(2);
    assertThat(stats.average()).hasValue(50);
    assertThat(stats.minimum()).hasValue(0);
    assertThat(stats.maximum()).hasValue(100);
    assertThat(stats.completedPercent()).isEqualTo(40);
    assertThat(stats.byStatus().get(ApplicationStatus.COMPLETED)).isEqualTo(2);
    assertThat(stats.applicants()).isEqualTo(1);
    assertThat(stats.activeExaminers()).isEqualTo(1);
    var future = service.calculate(snapshot, time.toLocalDate(), null);
    assertThat(future.total()).isEqualTo(3);
    assertThat(future.average()).isEmpty();
    assertThat(future.minimum()).isEmpty();
    assertThat(future.maximum()).isEmpty();
    assertThat(future.applicants()).isEqualTo(1);
  }

  @Test
  void emptyStatisticsAreDefined() {
    var stats =
        new StatisticsService(RulesTest.CLOCK).calculate(CenterSnapshot.empty(), null, null);
    assertThat(stats.total()).isZero();
    assertThat(stats.completedPercent()).isZero();
    assertThat(stats.average()).isEmpty();
    assertThat(stats.byStatus().values()).containsOnly(0L);
  }
}
