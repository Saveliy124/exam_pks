package ru.mirea.examcenter;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.service.*;

class RulesTest {
  static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-16T09:00:00Z"), ZoneId.of("Europe/Moscow"));
  static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "a"})
  void rejectsShortRequiredName(String input) {
    assertThatThrownBy(() -> Rules.text("fullName", input, "ФИО", 2, 120))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("ФИО");
  }

  @Test
  void trimsAndHonorsLengthBoundary() {
    assertThat(Rules.text("name", " Иван ", "Имя", 2, 120)).isEqualTo("Иван");
    assertThat(Rules.text("name", "я".repeat(120), "Имя", 2, 120)).hasSize(120);
    assertThatThrownBy(() -> Rules.text("name", "я".repeat(121), "Имя", 2, 120))
        .isInstanceOf(ValidationException.class);
    assertThat(Rules.text("phone", null, "Телефон", 0, 30)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"a@b", "a b@c.ru", "@example.org", "a@@b.ru", "a@.ru", "a@b."})
  void rejectsMalformedEmail(String input) {
    assertThatThrownBy(() -> Rules.email(input)).isInstanceOf(ValidationException.class);
  }

  @Test
  void normalizesEmail() {
    assertThat(Rules.email(" User@Example.ORG ")).isEqualTo("user@example.org");
  }

  @Test
  void dateMustBeStrictlyInFuture() {
    assertThatThrownBy(() -> Rules.future(null, CLOCK)).isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> Rules.future(NOW, CLOCK)).isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> Rules.future(NOW.minusSeconds(1), CLOCK))
        .isInstanceOf(ValidationException.class);
    assertThatCode(() -> Rules.future(NOW.plusNanos(1), CLOCK)).doesNotThrowAnyException();
  }

  static Stream<Arguments> transitions() {
    return Stream.of(ApplicationStatus.values())
        .flatMap(
            from ->
                Stream.of(ApplicationStatus.values())
                    .map(
                        to ->
                            Arguments.of(
                                from,
                                to,
                                (from == ApplicationStatus.NEW
                                        && (to == ApplicationStatus.APPROVED
                                            || to == ApplicationStatus.CANCELLED))
                                    || (from == ApplicationStatus.APPROVED
                                        && (to == ApplicationStatus.COMPLETED
                                            || to == ApplicationStatus.CANCELLED)))));
  }

  @ParameterizedTest
  @MethodSource("transitions")
  void validatesEveryStatusTransition(
      ApplicationStatus from, ApplicationStatus to, boolean allowed) {
    var call =
        (org.assertj.core.api.ThrowableAssert.ThrowingCallable)
            () ->
                Rules.transition(
                    from,
                    to,
                    NOW.minusDays(1),
                    to == ApplicationStatus.COMPLETED ? 50 : null,
                    CLOCK);
    if (allowed) assertThatCode(call).doesNotThrowAnyException();
    else assertThatThrownBy(call).isInstanceOf(ValidationException.class);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {-1, 101})
  void rejectsInvalidScore(Integer score) {
    assertThatThrownBy(
            () ->
                Rules.transition(
                    ApplicationStatus.APPROVED, ApplicationStatus.COMPLETED, NOW, score, CLOCK))
        .isInstanceOf(ValidationException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100})
  void acceptsScoreBoundary(int score) {
    assertThatCode(
            () ->
                Rules.transition(
                    ApplicationStatus.APPROVED, ApplicationStatus.COMPLETED, NOW, score, CLOCK))
        .doesNotThrowAnyException();
  }

  @Test
  void refusesEarlyCompletionAndScoreForOtherStatus() {
    assertThatThrownBy(
            () ->
                Rules.transition(
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.COMPLETED,
                    NOW.plusMinutes(1),
                    90,
                    CLOCK))
        .hasMessageContaining("до назначенного");
    assertThatThrownBy(
            () ->
                Rules.transition(ApplicationStatus.NEW, ApplicationStatus.APPROVED, NOW, 0, CLOCK))
        .hasMessageContaining("только для завершённого");
  }
}
