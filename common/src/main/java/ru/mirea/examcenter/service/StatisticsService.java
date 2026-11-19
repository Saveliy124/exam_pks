package ru.mirea.examcenter.service;

import java.time.*;
import java.util.*;
import ru.mirea.examcenter.model.*;

public final class StatisticsService {
  public record Statistics(
      long applicants,
      long activeExaminers,
      long total,
      Map<ApplicationStatus, Long> byStatus,
      long upcoming,
      OptionalDouble average,
      OptionalInt minimum,
      OptionalInt maximum,
      double completedPercent) {}

  private final Clock clock;

  public StatisticsService(Clock clock) {
    this.clock = clock;
  }

  public Statistics calculate(CenterSnapshot snapshot, LocalDate from, LocalDate to) {
    Filters.period(from, to);
    var selected =
        snapshot.applications().stream()
            .filter(Filters.applications("", null, from, to, null))
            .toList();
    Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
    for (var status : ApplicationStatus.values()) counts.put(status, 0L);
    for (var a : selected) counts.merge(a.getStatus(), 1L, Long::sum);
    var scores =
        selected.stream()
            .filter(a -> a.getStatus() == ApplicationStatus.COMPLETED)
            .map(ExamApplication::getScore)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .summaryStatistics();
    LocalDateTime now = LocalDateTime.now(clock);
    long upcoming =
        selected.stream()
            .filter(
                a ->
                    a.getScheduledAt().isAfter(now)
                        && (a.getStatus() == ApplicationStatus.NEW
                            || a.getStatus() == ApplicationStatus.APPROVED))
            .count();
    return new Statistics(
        snapshot.applicants().size(),
        snapshot.examiners().stream().filter(e -> e.getStatus() == ExaminerStatus.ACTIVE).count(),
        selected.size(),
        Map.copyOf(counts),
        upcoming,
        scores.getCount() == 0 ? OptionalDouble.empty() : OptionalDouble.of(scores.getAverage()),
        scores.getCount() == 0 ? OptionalInt.empty() : OptionalInt.of(scores.getMin()),
        scores.getCount() == 0 ? OptionalInt.empty() : OptionalInt.of(scores.getMax()),
        selected.isEmpty() ? 0 : 100.0 * counts.get(ApplicationStatus.COMPLETED) / selected.size());
  }
}
