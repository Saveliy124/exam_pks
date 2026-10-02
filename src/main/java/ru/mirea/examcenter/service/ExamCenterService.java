package ru.mirea.examcenter.service;

import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.repository.ExamCenterRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Все правила работы с кандидатами и заявками сосредоточены здесь. */
public final class ExamCenterService {
    private final ExamCenterRepository repository;

    /** Ошибка правила предметной области с сообщением для пользователя. */
    public static final class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    public ExamCenterService(ExamCenterRepository repository) {
        this.repository = repository;
    }

    public long createApplicant(String name, String email, String phone) throws SQLException {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        phone = phone == null ? "" : phone.trim();
        if (name.length() < 2 || name.length() > 120)
            throw new BusinessException("ФИО должно содержать от 2 до 120 символов.");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new BusinessException("Укажите корректный адрес электронной почты.");
        if (phone.length() > 30)
            throw new BusinessException("Телефон не должен превышать 30 символов.");
        if (repository.applicantEmailExists(email))
            throw new BusinessException("Кандидат с таким адресом уже существует.");
        return repository.createApplicant(name, email, phone);
    }

    public Applicant getApplicant(long id) throws SQLException {
        return repository.findApplicant(id)
                .orElseThrow(() -> new BusinessException("Кандидат с ID " + id + " не найден."));
    }

    public List<Applicant> allApplicants() throws SQLException {
        return repository.allApplicants();
    }

    public long createApplication(long applicantId, String exam, LocalDateTime date) throws SQLException {
        exam = checkApplicationDetails(applicantId, exam, date, 0);
        return repository.createApplication(applicantId, exam, date);
    }

    public ExamApplication getApplication(long id) throws SQLException {
        return repository.findApplication(id)
                .orElseThrow(() -> new BusinessException("Заявка с ID " + id + " не найдена."));
    }

    public List<ExamApplication> allApplications() throws SQLException {
        return repository.allApplications();
    }

    public void updateApplication(long id, long applicantId, String exam, LocalDateTime date)
            throws SQLException {
        ExamApplication current = getApplication(id);
        if (current.getStatus() == ApplicationStatus.COMPLETED ||
                current.getStatus() == ApplicationStatus.CANCELLED)
            throw new BusinessException("Завершенную или отмененную заявку нельзя изменять.");
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
        boolean allowed = (from == ApplicationStatus.NEW &&
                (target == ApplicationStatus.APPROVED || target == ApplicationStatus.CANCELLED)) ||
                (from == ApplicationStatus.APPROVED &&
                (target == ApplicationStatus.COMPLETED || target == ApplicationStatus.CANCELLED));
        if (!allowed)
            throw new BusinessException("Переход " + from + " → " + target + " запрещен.");
        if (target == ApplicationStatus.COMPLETED) {
            if (current.getScheduledAt().isAfter(LocalDateTime.now()))
                throw new BusinessException("Нельзя завершить экзамен до назначенного времени.");
            if (score == null || score < 0 || score > 100)
                throw new BusinessException("Для завершения нужен балл от 0 до 100.");
        } else if (score != null) {
            throw new BusinessException("Балл можно указать только для завершенного экзамена.");
        }
        repository.updateStatus(id, target, score);
    }

    private String checkApplicationDetails(long applicantId, String exam, LocalDateTime date, long excludeId)
            throws SQLException {
        getApplicant(applicantId);
        exam = exam == null ? "" : exam.trim();
        if (exam.length() < 2 || exam.length() > 120)
            throw new BusinessException("Название экзамена должно содержать от 2 до 120 символов.");
        if (date == null || !date.isAfter(LocalDateTime.now()))
            throw new BusinessException("Дата экзамена должна быть в будущем.");
        if (repository.hasActiveDuplicate(applicantId, exam, date, excludeId))
            throw new BusinessException("У кандидата уже есть активная заявка на этот экзамен и время.");
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
        if (value.isEmpty()) throw new BusinessException("Поисковая строка не должна быть пустой.");
        return value;
    }

    public List<ExamApplication> filterByStatus(ApplicationStatus status) throws SQLException {
        return allApplications().stream().filter(x -> x.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<ExamApplication> filterByDate(LocalDateTime from, LocalDateTime to) throws SQLException {
        if (from == null || to == null || from.isAfter(to))
            throw new BusinessException("Укажите корректный диапазон дат.");
        return allApplications().stream()
                .filter(x -> !x.getScheduledAt().isBefore(from) && !x.getScheduledAt().isAfter(to))
                .collect(Collectors.toList());
    }

    public List<ExamApplication> sortByDate() throws SQLException {
        return allApplications().stream()
                .sorted(Comparator.comparing(ExamApplication::getScheduledAt)
                        .thenComparingLong(ExamApplication::getId))
                .collect(Collectors.toList());
    }

    public List<ExamApplication> sortByApplicant() throws SQLException {
        return allApplications().stream()
                .sorted(Comparator.comparing(ExamApplication::getApplicantName,
                        String.CASE_INSENSITIVE_ORDER).thenComparingLong(ExamApplication::getId))
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
        LocalDateTime now = LocalDateTime.now();
        return allApplications().stream()
                .filter(x -> x.getScheduledAt().isAfter(now) &&
                        (x.getStatus() == ApplicationStatus.NEW || x.getStatus() == ApplicationStatus.APPROVED))
                .count();
    }

    public List<String> databaseTables() throws SQLException {
        return repository.databaseTables();
    }
}
