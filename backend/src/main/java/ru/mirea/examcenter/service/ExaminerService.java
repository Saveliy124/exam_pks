package ru.mirea.examcenter.service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import ru.mirea.examcenter.model.Examiner;
import ru.mirea.examcenter.model.ExaminerStatus;
import ru.mirea.examcenter.repository.ExaminerRepository;

public final class ExaminerService {
  public static final class ExaminerException extends ValidationException {
    public ExaminerException(String message) {
      super(message);
    }
  }

  private final ExaminerRepository repository;

  public ExaminerService(ExaminerRepository repository) {
    this.repository = repository;
  }

  public long createExaminer(String name, String email, String subject, ExaminerStatus status)
      throws SQLException {
    String[] values = validate(name, email, subject, status, 0);
    return repository.create(values[0], values[1], values[2], status);
  }

  public Examiner getExaminer(long id) throws SQLException {
    return repository
        .find(id)
        .orElseThrow(() -> new ExaminerException("Экзаменатор с ID " + id + " не найден."));
  }

  public List<Examiner> allExaminers() throws SQLException {
    return repository.all();
  }

  public void updateExaminer(
      long id, String name, String email, String subject, ExaminerStatus status)
      throws SQLException {
    getExaminer(id);
    String[] values = validate(name, email, subject, status, id);
    repository.update(id, values[0], values[1], values[2], status);
  }

  public void deleteExaminer(long id) throws SQLException {
    getExaminer(id);
    repository.delete(id);
  }

  private String[] validate(
      String name, String email, String subject, ExaminerStatus status, long excludeId)
      throws SQLException {
    name = Rules.text("fullName", name, "ФИО", 2, 120);
    email = Rules.email(email);
    subject = Rules.text("subject", subject, "Предмет", 2, 120);
    Rules.required("status", status, "Выберите статус.");
    if (repository.emailExists(email, excludeId))
      throw new ValidationException("email", "Этот email уже используется.");
    return new String[] {name, email, subject};
  }

  public List<Examiner> searchByName(String text) throws SQLException {
    String query = required(text).toLowerCase(Locale.ROOT);
    return allExaminers().stream()
        .filter(x -> x.getFullName().toLowerCase(Locale.ROOT).contains(query))
        .collect(Collectors.toList());
  }

  public List<Examiner> filterByStatus(ExaminerStatus status) throws SQLException {
    if (status == null) throw new ExaminerException("Укажите статус экзаменатора.");
    return allExaminers().stream()
        .filter(x -> x.getStatus() == status)
        .collect(Collectors.toList());
  }

  public List<Examiner> filterBySubject(String text) throws SQLException {
    String query = required(text).toLowerCase(Locale.ROOT);
    return allExaminers().stream()
        .filter(x -> x.getSubject().toLowerCase(Locale.ROOT).contains(query))
        .collect(Collectors.toList());
  }

  public List<Examiner> sortByName(boolean ascending) throws SQLException {
    Comparator<Examiner> order =
        Comparator.comparing(Examiner::getFullName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(Examiner::getId);
    return allExaminers().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  public List<Examiner> sortBySubject(boolean ascending) throws SQLException {
    Comparator<Examiner> order =
        Comparator.comparing(Examiner::getSubject, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(Examiner::getId);
    return allExaminers().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  private String required(String text) {
    String value = text == null ? "" : text.trim();
    if (value.isEmpty())
      throw new ExaminerException("Строка поиска или фильтра не может быть пустой.");
    return value;
  }
}
