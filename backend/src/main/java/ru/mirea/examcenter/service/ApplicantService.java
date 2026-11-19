package ru.mirea.examcenter.service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicantStatus;
import ru.mirea.examcenter.repository.ApplicantRepository;

public final class ApplicantService {
  public static final class ApplicantException extends ValidationException {
    public ApplicantException(String message) {
      super(message);
    }
  }

  private final ApplicantRepository repository;

  public ApplicantService(ApplicantRepository repository) {
    this.repository = repository;
  }

  public long createApplicant(String name, String email, String phone, ApplicantStatus status)
      throws SQLException {
    String[] values = validate(name, email, phone, status, 0);
    return repository.create(values[0], values[1], values[2], status);
  }

  public Applicant getApplicant(long id) throws SQLException {
    return repository
        .find(id)
        .orElseThrow(() -> new ApplicantException("Кандидат с ID " + id + " не найден."));
  }

  public List<Applicant> allApplicants() throws SQLException {
    return repository.all();
  }

  public void updateApplicant(
      long id, String name, String email, String phone, ApplicantStatus status)
      throws SQLException {
    getApplicant(id);
    String[] values = validate(name, email, phone, status, id);
    repository.update(id, values[0], values[1], values[2], status);
  }

  public void deleteApplicant(long id) throws SQLException {
    getApplicant(id);
    if (repository.hasApplications(id))
      throw new ApplicantException("Сначала удалите заявки кандидата.");
    repository.delete(id);
  }

  private String[] validate(
      String name, String email, String phone, ApplicantStatus status, long excludeId)
      throws SQLException {
    name = Rules.text("fullName", name, "ФИО", 2, 120);
    email = Rules.email(email);
    phone = Rules.text("phone", phone, "Телефон", 0, 30);
    Rules.required("status", status, "Выберите статус.");
    if (repository.emailExists(email, excludeId))
      throw new ValidationException("email", "Этот email уже используется.");
    return new String[] {name, email, phone};
  }

  public List<Applicant> searchByName(String text) throws SQLException {
    String query = required(text).toLowerCase(Locale.ROOT);
    return allApplicants().stream()
        .filter(x -> x.getFullName().toLowerCase(Locale.ROOT).contains(query))
        .collect(Collectors.toList());
  }

  public List<Applicant> filterByStatus(ApplicantStatus status) throws SQLException {
    if (status == null) throw new ApplicantException("Укажите статус кандидата.");
    return allApplicants().stream()
        .filter(x -> x.getStatus() == status)
        .collect(Collectors.toList());
  }

  public List<Applicant> filterByEmailDomain(String domain) throws SQLException {
    String suffix = "@" + required(domain).toLowerCase(Locale.ROOT).replaceFirst("^@", "");
    return allApplicants().stream()
        .filter(x -> x.getEmail().endsWith(suffix))
        .collect(Collectors.toList());
  }

  public List<Applicant> sortByName(boolean ascending) throws SQLException {
    Comparator<Applicant> order =
        Comparator.comparing(Applicant::getFullName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(Applicant::getId);
    return allApplicants().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  public List<Applicant> sortByEmail(boolean ascending) throws SQLException {
    Comparator<Applicant> order =
        Comparator.comparing(Applicant::getEmail, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(Applicant::getId);
    return allApplicants().stream()
        .sorted(ascending ? order : order.reversed())
        .collect(Collectors.toList());
  }

  private String required(String text) {
    String value = text == null ? "" : text.trim();
    if (value.isEmpty())
      throw new ApplicantException("Строка поиска или фильтра не может быть пустой.");
    return value;
  }
}
