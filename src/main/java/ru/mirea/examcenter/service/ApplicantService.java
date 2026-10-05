package ru.mirea.examcenter.service;

import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicantStatus;
import ru.mirea.examcenter.repository.ApplicantRepository;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ApplicantService {
    public static final class ApplicantException extends RuntimeException {
        public ApplicantException(String message) { super(message); }
    }

    private final ApplicantRepository repository;

    public ApplicantService(ApplicantRepository repository) { this.repository = repository; }

    public long createApplicant(String name, String email, String phone, ApplicantStatus status)
            throws SQLException {
        String[] values = validate(name, email, phone, status, 0);
        return repository.create(values[0], values[1], values[2], status);
    }

    public Applicant getApplicant(long id) throws SQLException {
        return repository.find(id).orElseThrow(() ->
                new ApplicantException("Кандидат с ID " + id + " не найден."));
    }

    public List<Applicant> allApplicants() throws SQLException { return repository.all(); }

    public void updateApplicant(long id, String name, String email, String phone,
                                ApplicantStatus status) throws SQLException {
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

    private String[] validate(String name, String email, String phone, ApplicantStatus status,
                              long excludeId) throws SQLException {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        phone = phone == null ? "" : phone.trim();
        if (name.length() < 2 || name.length() > 120)
            throw new ApplicantException("ФИО: от 2 до 120 символов.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new ApplicantException("Укажите корректный email.");
        if (phone.length() > 30)
            throw new ApplicantException("Телефон: не более 30 символов.");
        if (status == null) throw new ApplicantException("Укажите статус кандидата.");
        if (repository.emailExists(email, excludeId))
            throw new ApplicantException("Кандидат с таким email уже существует.");
        return new String[]{name, email, phone};
    }

    public List<Applicant> searchByName(String text) throws SQLException {
        String query = required(text).toLowerCase(Locale.ROOT);
        return allApplicants().stream()
                .filter(x -> x.getFullName().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    public List<Applicant> filterByStatus(ApplicantStatus status) throws SQLException {
        if (status == null) throw new ApplicantException("Укажите статус кандидата.");
        return allApplicants().stream().filter(x -> x.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Applicant> filterByEmailDomain(String domain) throws SQLException {
        String suffix = "@" + required(domain).toLowerCase(Locale.ROOT).replaceFirst("^@", "");
        return allApplicants().stream().filter(x -> x.getEmail().endsWith(suffix))
                .collect(Collectors.toList());
    }

    public List<Applicant> sortByName(boolean ascending) throws SQLException {
        Comparator<Applicant> order = Comparator.comparing(Applicant::getFullName,
                String.CASE_INSENSITIVE_ORDER).thenComparingLong(Applicant::getId);
        return allApplicants().stream().sorted(ascending ? order : order.reversed())
                .collect(Collectors.toList());
    }

    public List<Applicant> sortByEmail(boolean ascending) throws SQLException {
        Comparator<Applicant> order = Comparator.comparing(Applicant::getEmail,
                String.CASE_INSENSITIVE_ORDER).thenComparingLong(Applicant::getId);
        return allApplicants().stream().sorted(ascending ? order : order.reversed())
                .collect(Collectors.toList());
    }

    private String required(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) throw new ApplicantException("Строка поиска или фильтра не может быть пустой.");
        return value;
    }
}
