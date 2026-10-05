package ru.mirea.examcenter.ui;

import ru.mirea.examcenter.export.DataExporter;
import ru.mirea.examcenter.export.XlsxExporter;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicantStatus;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.model.Examiner;
import ru.mirea.examcenter.model.ExaminerStatus;
import ru.mirea.examcenter.service.ApplicantService;
import ru.mirea.examcenter.service.ApplicantService.ApplicantException;
import ru.mirea.examcenter.service.ExamApplicationService;
import ru.mirea.examcenter.service.ExamApplicationService.ApplicationException;
import ru.mirea.examcenter.service.ExaminerService;
import ru.mirea.examcenter.service.ExaminerService.ExaminerException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public final class ConsoleUi {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ApplicantService applicants;
    private final ExamApplicationService applications;
    private final ExaminerService examiners;
    private final DataExporter exporter = new XlsxExporter();
    private final Scanner input = new Scanner(System.in, "UTF-8");

    public ConsoleUi(ApplicantService applicants, ExamApplicationService applications,
                     ExaminerService examiners) {
        this.applicants = applicants;
        this.applications = applications;
        this.examiners = examiners;
    }

    private interface MenuAction { void execute(int choice) throws SQLException, IOException; }

    public void run() {
        try {
            menu("ЭКЗАМЕНАЦИОННЫЙ ЦЕНТР",
                    "1 — Кандидаты; 2 — Заявки; 3 — Экзаменаторы; 4 — Статистика; " +
                    "5 — Экспорт Excel; 6 — Таблицы БД; 0 — Выход", choice -> {
                switch (choice) {
                    case 1: applicantsMenu(); break;
                    case 2: applicationsMenu(); break;
                    case 3: examinersMenu(); break;
                    case 4: statistics(); break;
                    case 5: export(); break;
                    case 6: tables(); break;
                    default: unknown();
                }
            });
        } catch (EndOfInput ignored) {
            // Ввод закончился, например при запуске из скрипта.
        }
        System.out.println("Работа завершена.");
    }

    private void menu(String title, String options, MenuAction action) {
        while (true) {
            System.out.println("\n========== " + title + " ==========");
            System.out.println(options.replace("; ", "\n"));
            int choice = menuChoice();
            if (choice == 0) return;
            try {
                action.execute(choice);
            } catch (ApplicantException | ApplicationException | ExaminerException |
                     IllegalArgumentException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            } catch (SQLException ex) {
                System.out.println("Ошибка базы данных (SQLState " + ex.getSQLState() + "): " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Ошибка экспорта: " + ex.getMessage());
            }
        }
    }

    private void applicantsMenu() {
        menu("КАНДИДАТЫ", "1 — Создать; 2 — Список; 3 — По ID; 4 — Изменить; " +
                "5 — Удалить; 6 — Поиск по ФИО; 7 — Фильтр по статусу; " +
                "8 — Фильтр по домену email; 9 — Сортировка по ФИО; " +
                "10 — Сортировка по email; 0 — Назад", choice -> {
            switch (choice) {
                case 1:
                    long id = applicants.createApplicant(line("ФИО: "), line("Email: "),
                            line("Телефон: "), applicantStatus());
                    System.out.println("Кандидат создан, ID: " + id); break;
                case 2: printApplicants(applicants.allApplicants()); break;
                case 3: printApplicants(Collections.singletonList(applicants.getApplicant(positiveId()))); break;
                case 4: updateApplicant(); break;
                case 5: applicants.deleteApplicant(positiveId()); System.out.println("Кандидат удалён."); break;
                case 6: printApplicants(applicants.searchByName(line("Фрагмент ФИО: "))); break;
                case 7: printApplicants(applicants.filterByStatus(applicantStatus())); break;
                case 8: printApplicants(applicants.filterByEmailDomain(line("Домен после @: "))); break;
                case 9: printApplicants(applicants.sortByName(ascending())); break;
                case 10: printApplicants(applicants.sortByEmail(ascending())); break;
                default: unknown();
            }
        });
    }

    private void updateApplicant() throws SQLException {
        long id = positiveId();
        Applicant old = applicants.getApplicant(id);
        String name = optional("ФИО (Enter — оставить): ", old.getFullName());
        String email = optional("Email (Enter — оставить): ", old.getEmail());
        String phone = optional("Телефон (Enter — оставить): ", old.getPhone());
        String value = line("Статус ACTIVE/BLOCKED (Enter — оставить): ").trim();
        ApplicantStatus status = value.isEmpty() ? old.getStatus() : parseEnum(value, ApplicantStatus.class);
        applicants.updateApplicant(id, name, email, phone, status);
        System.out.println("Кандидат изменён.");
    }

    private void applicationsMenu() {
        menu("ЗАЯВКИ", "1 — Создать; 2 — Список; 3 — По ID; 4 — Изменить; " +
                "5 — Удалить; 6 — Согласовать; 7 — Завершить; 8 — Отменить; " +
                "9 — Поиск по экзамену; 10 — Поиск по кандидату; " +
                "11 — Фильтр по статусу; 12 — Фильтр по датам; " +
                "13 — Сортировка по дате; 14 — Сортировка по ФИО; 0 — Назад", choice -> {
            switch (choice) {
                case 1:
                    long id = applications.createApplication(positiveId(), line("Экзамен: "),
                            dateTime("Дата и время (дд.мм.гггг чч:мм): "));
                    System.out.println("Заявка создана, ID: " + id); break;
                case 2: printApplications(applications.allApplications()); break;
                case 3: printApplications(Collections.singletonList(applications.getApplication(positiveId()))); break;
                case 4: updateApplication(); break;
                case 5: applications.deleteApplication(positiveId()); System.out.println("Заявка удалена."); break;
                case 6: applications.changeStatus(positiveId(), ApplicationStatus.APPROVED, null);
                        System.out.println("Заявка согласована."); break;
                case 7: applications.changeStatus(positiveId(), ApplicationStatus.COMPLETED,
                            integer("Балл (0–100): "));
                        System.out.println("Экзамен завершён."); break;
                case 8: applications.changeStatus(positiveId(), ApplicationStatus.CANCELLED, null);
                        System.out.println("Заявка отменена."); break;
                case 9: printApplications(applications.searchByExam(line("Фрагмент экзамена: "))); break;
                case 10: printApplications(applications.searchByApplicant(line("Фрагмент ФИО: "))); break;
                case 11: printApplications(applications.filterByStatus(applicationStatus())); break;
                case 12:
                    LocalDate from = date("С даты (дд.мм.гггг): ");
                    LocalDate to = date("По дату (дд.мм.гггг): ");
                    printApplications(applications.filterByDate(from.atStartOfDay(), to.atTime(LocalTime.MAX)));
                    break;
                case 13: printApplications(applications.sortByDate(ascending())); break;
                case 14: printApplications(applications.sortByApplicant(ascending())); break;
                default: unknown();
            }
        });
    }

    private void updateApplication() throws SQLException {
        long id = positiveId();
        ExamApplication old = applications.getApplication(id);
        System.out.println("Текущая заявка:");
        printApplications(Collections.singletonList(old));
        String applicantText = line("ID кандидата (Enter — оставить): ").trim();
        long applicantId = applicantText.isEmpty() ? old.getApplicantId() : parseLong(applicantText);
        String exam = optional("Экзамен (Enter — оставить): ", old.getExamName());
        String dateText = line("Дата дд.мм.гггг чч:мм (Enter — оставить): ").trim();
        LocalDateTime time = dateText.isEmpty() ? old.getScheduledAt() : parseDateTime(dateText);
        applications.updateApplication(id, applicantId, exam, time);
        System.out.println("Заявка изменена.");
    }

    private void examinersMenu() {
        menu("ЭКЗАМЕНАТОРЫ", "1 — Создать; 2 — Список; 3 — По ID; 4 — Изменить; " +
                "5 — Удалить; 6 — Поиск по ФИО; 7 — Фильтр по статусу; " +
                "8 — Фильтр по предмету; 9 — Сортировка по ФИО; " +
                "10 — Сортировка по предмету; 0 — Назад", choice -> {
            switch (choice) {
                case 1:
                    long id = examiners.createExaminer(line("ФИО: "), line("Email: "),
                            line("Предмет: "), examinerStatus());
                    System.out.println("Экзаменатор создан, ID: " + id); break;
                case 2: printExaminers(examiners.allExaminers()); break;
                case 3: printExaminers(Collections.singletonList(examiners.getExaminer(positiveId()))); break;
                case 4: updateExaminer(); break;
                case 5: examiners.deleteExaminer(positiveId()); System.out.println("Экзаменатор удалён."); break;
                case 6: printExaminers(examiners.searchByName(line("Фрагмент ФИО: "))); break;
                case 7: printExaminers(examiners.filterByStatus(examinerStatus())); break;
                case 8: printExaminers(examiners.filterBySubject(line("Фрагмент предмета: "))); break;
                case 9: printExaminers(examiners.sortByName(ascending())); break;
                case 10: printExaminers(examiners.sortBySubject(ascending())); break;
                default: unknown();
            }
        });
    }

    private void updateExaminer() throws SQLException {
        long id = positiveId();
        Examiner old = examiners.getExaminer(id);
        String name = optional("ФИО (Enter — оставить): ", old.getFullName());
        String email = optional("Email (Enter — оставить): ", old.getEmail());
        String subject = optional("Предмет (Enter — оставить): ", old.getSubject());
        String value = line("Статус ACTIVE/INACTIVE (Enter — оставить): ").trim();
        ExaminerStatus status = value.isEmpty() ? old.getStatus() : parseEnum(value, ExaminerStatus.class);
        examiners.updateExaminer(id, name, email, subject, status);
        System.out.println("Экзаменатор изменён.");
    }

    private void statistics() throws SQLException {
        Map<ApplicationStatus, Long> counts = applications.countsByStatus();
        System.out.println("Кандидатов: " + applicants.allApplicants().size());
        System.out.println("Экзаменаторов: " + examiners.allExaminers().size());
        System.out.println("Заявок: " + applications.allApplications().size());
        for (ApplicationStatus status : ApplicationStatus.values())
            System.out.println(status + ": " + counts.get(status));
        System.out.println("Предстоящих активных экзаменов: " + applications.upcomingCount());
    }

    private void export() throws SQLException, IOException {
        String name = line("Путь к .xlsx (Enter — export/exam_center.xlsx): ").trim();
        Path file = Paths.get(name.isEmpty() ? "export/exam_center.xlsx" : name);
        if (!file.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new IllegalArgumentException("Нужен файл с расширением .xlsx.");
        exporter.export(file, applicants.allApplicants(), applications.allApplications(),
                examiners.allExaminers());
        System.out.println("Экспортировано: " + file.toAbsolutePath());
    }

    private void tables() throws SQLException {
        System.out.println("Таблицы схемы public:");
        for (String line : applications.databaseTables()) System.out.println(line);
        System.out.println("Кандидаты:"); printApplicants(applicants.allApplicants());
        System.out.println("Заявки:"); printApplications(applications.allApplications());
        System.out.println("Экзаменаторы:"); printExaminers(examiners.allExaminers());
    }

    private void printApplicants(List<Applicant> items) {
        if (items.isEmpty()) { System.out.println("Кандидаты не найдены."); return; }
        for (Applicant x : items) System.out.printf("%d | %s | %s | %s | %s%n",
                x.getId(), x.getFullName(), x.getEmail(), x.getPhone(), x.getStatus());
    }

    private void printApplications(List<ExamApplication> items) {
        if (items.isEmpty()) { System.out.println("Заявки не найдены."); return; }
        for (ExamApplication x : items) System.out.printf("%d | %s (#%d) | %s | %s | %s | балл: %s%n",
                x.getId(), x.getApplicantName(), x.getApplicantId(), x.getExamName(),
                x.getScheduledAt().format(DATE_TIME), x.getStatus(),
                x.getScore() == null ? "—" : x.getScore().toString());
    }

    private void printExaminers(List<Examiner> items) {
        if (items.isEmpty()) { System.out.println("Экзаменаторы не найдены."); return; }
        for (Examiner x : items) System.out.printf("%d | %s | %s | %s | %s%n",
                x.getId(), x.getFullName(), x.getEmail(), x.getSubject(), x.getStatus());
    }

    private int menuChoice() {
        while (true) {
            try { return integer("Действие: "); }
            catch (IllegalArgumentException ex) { System.out.println("Ошибка: " + ex.getMessage()); }
        }
    }

    private boolean ascending() {
        int choice = integer("Порядок: 1 — прямой, 2 — обратный: ");
        if (choice != 1 && choice != 2) throw new IllegalArgumentException("Введите 1 или 2.");
        return choice == 1;
    }

    private ApplicantStatus applicantStatus() {
        return parseEnum(line("Статус ACTIVE/BLOCKED: "), ApplicantStatus.class);
    }

    private ApplicationStatus applicationStatus() {
        return parseEnum(line("Статус NEW/APPROVED/COMPLETED/CANCELLED: "), ApplicationStatus.class);
    }

    private ExaminerStatus examinerStatus() {
        return parseEnum(line("Статус ACTIVE/INACTIVE: "), ExaminerStatus.class);
    }

    private <E extends Enum<E>> E parseEnum(String text, Class<E> type) {
        try { return Enum.valueOf(type, text.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Неизвестный статус."); }
    }

    private String optional(String prompt, String old) {
        String value = line(prompt).trim();
        return value.isEmpty() ? old : value;
    }

    private String line(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) throw new EndOfInput();
        return input.nextLine();
    }

    private int integer(String prompt) {
        try { return Integer.parseInt(line(prompt).trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Введите целое число."); }
    }

    private long positiveId() {
        long id = parseLong(line("ID: ").trim());
        if (id <= 0) throw new IllegalArgumentException("ID должен быть положительным.");
        return id;
    }

    private long parseLong(String text) {
        try { return Long.parseLong(text); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("ID должен быть целым числом."); }
    }

    private LocalDateTime dateTime(String prompt) { return parseDateTime(line(prompt).trim()); }

    private LocalDateTime parseDateTime(String text) {
        try { return LocalDateTime.parse(text, DATE_TIME); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Дата: дд.мм.гггг чч:мм."); }
    }

    private LocalDate date(String prompt) {
        try { return LocalDate.parse(line(prompt).trim(), DATE); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Дата: дд.мм.гггг."); }
    }

    private void unknown() { System.out.println("Нет такого пункта меню."); }

    private static final class EndOfInput extends RuntimeException { }
}
