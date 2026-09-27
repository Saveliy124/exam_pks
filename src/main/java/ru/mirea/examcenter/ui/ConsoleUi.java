package ru.mirea.examcenter.ui;

import ru.mirea.examcenter.service.BusinessException;
import ru.mirea.examcenter.export.DataExporter;
import ru.mirea.examcenter.export.XlsxExporter;
import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExamApplication;
import ru.mirea.examcenter.service.ExamCenterService;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class ConsoleUi {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final ExamCenterService service;
    private final DataExporter exporter;
    private final Scanner input = new Scanner(System.in, "UTF-8");

    public ConsoleUi(ExamCenterService service) {
        this.service = service;
        this.exporter = new XlsxExporter();
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n========== ЭКЗАМЕНАЦИОННЫЙ ЦЕНТР ==========");
            System.out.println("1. Кандидаты\n2. Заявки на экзамен\n3. Поиск заявок\n4. Фильтрация заявок");
            System.out.println("5. Сортировка заявок\n6. Статистика\n7. Экспорт в Excel");
            System.out.println("8. Таблицы базы данных\n0. Выход");
            try {
                switch (integer("Выберите действие: ")) {
                    case 1: applicantsMenu(); break;
                    case 2: applicationsMenu(); break;
                    case 3: searchMenu(); break;
                    case 4: filterMenu(); break;
                    case 5: sortMenu(); break;
                    case 6: statistics(); break;
                    case 7: export(); break;
                    case 8: tables(); break;
                    case 0: running = false; break;
                    default: System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | IllegalArgumentException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            } catch (SQLException ex) {
                System.out.println("Ошибка базы данных (SQLState " + ex.getSQLState() + "): " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Ошибка экспорта: " + ex.getMessage());
            } catch (EndOfInput ex) {
                running = false;
            }
        }
        System.out.println("Работа завершена.");
    }

    private void applicantsMenu() throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("\nКАНДИДАТЫ: 1 — добавить, 2 — список, 3 — по ID, 0 — назад");
            try {
                switch (integer("Действие: ")) {
                    case 1:
                        long id = service.createApplicant(line("ФИО: "), line("Email: "), line("Телефон: "));
                        System.out.println("Кандидат создан, ID: " + id); break;
                    case 2: printApplicants(service.allApplicants()); break;
                    case 3: printApplicants(java.util.Collections.singletonList(service.getApplicant(positiveId()))); break;
                    case 0: back = true; break;
                    default: System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | IllegalArgumentException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            } catch (SQLException ex) {
                System.out.println("Ошибка базы данных (SQLState " + ex.getSQLState() + "): " + ex.getMessage());
            }
        }
    }

    private void applicationsMenu() throws SQLException {
        boolean back = false;
        while (!back) {
            System.out.println("\nЗАЯВКИ: 1 — создать, 2 — список, 3 — по ID, 4 — изменить,");
            System.out.println("5 — удалить, 6 — согласовать, 7 — завершить, 8 — отменить, 0 — назад");
            try {
                switch (integer("Действие: ")) {
                    case 1:
                        long id = service.createApplication(positiveId(), line("Название экзамена: "),
                                dateTime("Дата и время (дд.мм.гггг чч:мм): "));
                        System.out.println("Заявка создана, ID: " + id); break;
                    case 2: printApplications(service.allApplications()); break;
                    case 3: printApplications(java.util.Collections.singletonList(service.getApplication(positiveId()))); break;
                    case 4: updateApplication(); break;
                    case 5:
                        service.deleteApplication(positiveId()); System.out.println("Заявка удалена."); break;
                    case 6:
                        service.changeStatus(positiveId(), ApplicationStatus.APPROVED, null);
                        System.out.println("Заявка согласована."); break;
                    case 7:
                        long completedId = positiveId();
                        service.changeStatus(completedId, ApplicationStatus.COMPLETED,
                                integer("Балл (0–100): "));
                        System.out.println("Экзамен завершен."); break;
                    case 8:
                        service.changeStatus(positiveId(), ApplicationStatus.CANCELLED, null);
                        System.out.println("Заявка отменена."); break;
                    case 0: back = true; break;
                    default: System.out.println("Нет такого пункта меню.");
                }
            } catch (BusinessException | IllegalArgumentException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            } catch (SQLException ex) {
                System.out.println("Ошибка базы данных (SQLState " + ex.getSQLState() + "): " + ex.getMessage());
            }
        }
    }

    private void updateApplication() throws SQLException {
        long id = positiveId();
        ExamApplication current = service.getApplication(id);
        System.out.println("Текущая заявка:");
        printApplications(java.util.Collections.singletonList(current));
        String applicantText = line("Новый ID кандидата (Enter — оставить): ").trim();
        long applicantId = applicantText.isEmpty() ? current.getApplicantId() : parseLong(applicantText);
        String exam = line("Новое название экзамена (Enter — оставить): ").trim();
        if (exam.isEmpty()) exam = current.getExamName();
        String dateText = line("Новая дата дд.мм.гггг чч:мм (Enter — оставить): ").trim();
        LocalDateTime date = dateText.isEmpty() ? current.getScheduledAt() : parseDateTime(dateText);
        service.updateApplication(id, applicantId, exam, date);
        System.out.println("Заявка изменена.");
    }

    private void searchMenu() throws SQLException {
        System.out.println("ПОИСК: 1 — по названию экзамена, 2 — по ФИО кандидата");
        int choice = integer("Действие: ");
        if (choice == 1) printApplications(service.searchByExam(line("Фрагмент названия: ")));
        else if (choice == 2) printApplications(service.searchByApplicant(line("Фрагмент ФИО: ")));
        else System.out.println("Нет такого способа поиска.");
    }

    private void filterMenu() throws SQLException {
        System.out.println("ФИЛЬТР: 1 — по статусу, 2 — по диапазону дат");
        int choice = integer("Действие: ");
        if (choice == 1) {
            System.out.println("Статусы: NEW, APPROVED, COMPLETED, CANCELLED");
            String value = line("Статус: ").trim().toUpperCase(java.util.Locale.ROOT);
            try { printApplications(service.filterByStatus(ApplicationStatus.valueOf(value))); }
            catch (IllegalArgumentException ex) { throw new BusinessException("Неизвестный статус."); }
        } else if (choice == 2) {
            LocalDate from = date("С даты (дд.мм.гггг): ");
            LocalDate to = date("По дату (дд.мм.гггг): ");
            printApplications(service.filterByDate(from.atStartOfDay(), to.atTime(LocalTime.MAX)));
        } else System.out.println("Нет такого фильтра.");
    }

    private void sortMenu() throws SQLException {
        System.out.println("СОРТИРОВКА: 1 — по дате экзамена, 2 — по ФИО кандидата");
        int choice = integer("Действие: ");
        if (choice == 1) printApplications(service.sortByDate());
        else if (choice == 2) printApplications(service.sortByApplicant());
        else System.out.println("Нет такого способа сортировки.");
    }

    private void statistics() throws SQLException {
        Map<ApplicationStatus, Long> counts = service.countsByStatus();
        System.out.println("\nСТАТИСТИКА");
        System.out.println("Кандидатов: " + service.allApplicants().size());
        System.out.println("Всего заявок: " + service.allApplications().size());
        for (ApplicationStatus status : ApplicationStatus.values())
            System.out.println(status + ": " + counts.get(status));
        System.out.println("Предстоящих активных экзаменов: " + service.upcomingCount());
    }

    private void export() throws SQLException, IOException {
        String name = line("Путь к .xlsx (Enter — export/exam_center.xlsx): ").trim();
        Path path = Paths.get(name.isEmpty() ? "export/exam_center.xlsx" : name);
        if (!path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))
            throw new BusinessException("Имя файла должно оканчиваться на .xlsx.");
        exporter.export(path, service.allApplicants(), service.allApplications());
        System.out.println("Экспортировано: " + path.toAbsolutePath());
    }

    private void tables() throws SQLException {
        System.out.println("Таблицы схемы public:");
        for (String line : service.databaseTables()) System.out.println(line);
        System.out.println("\nДанные applicants:");
        printApplicants(service.allApplicants());
        System.out.println("\nДанные exam_applications:");
        printApplications(service.allApplications());
    }

    private void printApplicants(List<Applicant> items) {
        if (items.isEmpty()) { System.out.println("Кандидаты не найдены."); return; }
        for (Applicant x : items) System.out.printf("%d | %s | %s | %s%n",
                x.getId(), x.getFullName(), x.getEmail(), x.getPhone());
    }

    private void printApplications(List<ExamApplication> items) {
        if (items.isEmpty()) { System.out.println("Заявки не найдены."); return; }
        for (ExamApplication x : items) System.out.printf("%d | %s (#%d) | %s | %s | %s | балл: %s%n",
                x.getId(), x.getApplicantName(), x.getApplicantId(), x.getExamName(),
                x.getScheduledAt().format(DATE_TIME), x.getStatus(),
                x.getScore() == null ? "—" : x.getScore().toString());
    }

    private String line(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) throw new EndOfInput();
        return input.nextLine();
    }

    private int integer(String prompt) {
        String value = line(prompt).trim();
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new BusinessException("Введите целое число."); }
    }

    private long positiveId() {
        long id = parseLong(line("ID: ").trim());
        if (id <= 0) throw new BusinessException("ID должен быть положительным числом.");
        return id;
    }

    private long parseLong(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException ex) { throw new BusinessException("ID должен быть целым числом."); }
    }

    private LocalDateTime dateTime(String prompt) { return parseDateTime(line(prompt).trim()); }
    private LocalDateTime parseDateTime(String value) {
        try { return LocalDateTime.parse(value, DATE_TIME); }
        catch (DateTimeParseException ex) { throw new BusinessException("Дата и время: дд.мм.гггг чч:мм."); }
    }

    private LocalDate date(String prompt) {
        try { return LocalDate.parse(line(prompt).trim(), DATE); }
        catch (DateTimeParseException ex) { throw new BusinessException("Дата: дд.мм.гггг."); }
    }

    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
