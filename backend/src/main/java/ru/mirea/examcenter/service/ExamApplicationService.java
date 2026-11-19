package ru.mirea.examcenter.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicantStatus;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.repository.ExamApplicationRepository;

/** Правила создания и изменения заявок на экзамен. */
public final class ExamApplicationService {
  private final ExamApplicationRepository repository;
  private final ApplicantService applicants;
  private final Clock clock;

  /** Ошибка правила предметной области с сообщением для пользователя. */
  public static final class ApplicationException extends ValidationException {
    public ApplicationException(String message) {
      super(message);
    }
  }

  public ExamApplicationService(ExamApplicationRepository repository, ApplicantService applicants) {
    this(repository, applicants, Clock.systemDefaultZone());
  }

  public ExamApplicationService(
      ExamApplicationRepository repository, ApplicantService applicants, Clock clock) {
    this.repository = repository;
    this.applicants = applicants;
    this.clock = clock;
  }

  public long createApplication(long applicantId, String exam, LocalDateTime date)
      throws SQLException {
    exam = checkApplicationDetails(applicantId, exam, date, 0);
    return repository.createApplication(applicantId, exam, date);
  }

  public ExamApplication getApplication(long id) throws SQLException {
    return repository
        .findApplication(id)
        .orElseThrow(() -> new ApplicationException("Заявка с ID " + id + " не найдена."));
  }

  public List<ExamApplication> allApplications() throws SQLException {
    return repository.allApplications();
  }

  public void updateApplication(long id, long applicantId, String exam, LocalDateTime date)
      throws SQLException {
    ExamApplication current = getApplication(id);
    if (current.getStatus() == ApplicationStatus.COMPLETED
        || current.getStatus() == ApplicationStatus.CANCELLED)
      throw new ApplicationException("Завершенную или отмененную заявку нельзя изменять.");
    exam = checkApplicationDetails(applicantId, exam, date, id);
    repository.updateApplication(id, applicantId, exam, date);
  }

  public void deleteApplication(long id) throws SQLException {
    getApplication(id);
    repository.deleteApplication(id);
  }

  public void changeStatus(long id, ApplicationStatus target, Integer score) throws SQLException {
    ExamApplication current = getApplication(id);
    ApplicationStatus from = current.getStatus();
    Rules.transition(from, target, current.getScheduledAt(), score, clock);
    repository.updateStatus(id, from, target, score, LocalDateTime.now(clock));
  }

  private String checkApplicationDetails(
      long applicantId, String exam, LocalDateTime date, long excludeId) throws SQLException {
    Applicant applicant = applicants.getApplicant(applicantId);
    if (applicant.getStatus() == ApplicantStatus.BLOCKED)
      throw new ValidationException(
          "applicant", "Заблокированный кандидат не может быть записан на экзамен.");
    exam = Rules.text("exam", exam, "Название экзамена", 2, 120);
    Rules.future(date, clock);
    if (repository.hasActiveDuplicate(applicantId, exam, date, excludeId))
      throw new ValidationException(
          "exam", "У кандидата уже есть активная заявка на этот экзамен и время.");
    return exam;
  }

  public List<ExamApplication> searchByExam(String query) throws SQLException {
    String value = searchText(query);
    return allApplications().stream()
        .filter(x -> x.getExamName().toLowerCase(Locale.ROOT).contains(value))
        .collect(Collectors.toList());
  }

  public List<ExamApplication> searchByApplicant(String query) throws SQLException {
    String value = searchText(query);
    return allApplications().stream()
        .filter(x -> x.getApplicantName().toLowerCase(Locale.ROOT).contains(value))
        .collect(Collectors.toList());
  }

  private String searchText(String query) {
    String value = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (value.isEmpty()) throw new ApplicationException("Поисковая строка не должна быть пустой.");
    return value;
  }

  public List<ExamApplication> filterByStatus(ApplicationStatus status) throws SQLException {
    return allApplications().stream()
        .filter(x -> x.getStatus() == status)
        .collect(Collectors.toList());
  }

  public List<ExamApplication> filterByDate(LocalDateTime from, LocalDateTime to)
      throws SQLException {
    if (from == null || to == null || from.isAfter(to))
      throw new ApplicationException("Укажите корректный диапазон дат.");
    return allApplications().stream()
        .filter(x -> !x.getScheduledAt().isBefore(from) && !x.getScheduledAt().isAfter(to))
        .collect(Collectors.toList());
  }

  public List<ExamApplication> sortByDate(boolean ascending) throws SQLException {
    Comparator<ExamApplication> order =
        Comparator.comparing(ExamApplication::getScheduledAt)
            .thenComparingLong(ExamApplication::getId);
    return allApplications().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  public List<ExamApplication> sortByApplicant(boolean ascending) throws SQLException {
    Comparator<ExamApplication> order =
        Comparator.comparing(ExamApplication::getApplicantName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(ExamApplication::getId);
    return allApplications().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  public Map<ApplicationStatus, Long> countsByStatus() throws SQLException {
    Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
    for (ApplicationStatus status : ApplicationStatus.values()) counts.put(status, 0L);
    for (ExamApplication application : allApplications())
      counts.put(application.getStatus(), counts.get(application.getStatus()) + 1);
    return counts;
  }

  public long upcomingCount() throws SQLException {
    LocalDateTime now = LocalDateTime.now(clock);
    return allApplications().stream()
        .filter(
            x ->
                x.getScheduledAt().isAfter(now)
                    && (x.getStatus() == ApplicationStatus.NEW
                        || x.getStatus() == ApplicationStatus.APPROVED))
        .count();
  }

  public List<String> databaseTables() throws SQLException {
    return repository.databaseTables();
  }
}
