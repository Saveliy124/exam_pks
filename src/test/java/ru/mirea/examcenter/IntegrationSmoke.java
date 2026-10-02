package ru.mirea.examcenter;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.examcenter.export.DataExporter;
import ru.mirea.examcenter.export.XlsxExporter;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.repository.ExamCenterRepository;
import ru.mirea.examcenter.service.ExamCenterService.BusinessException;
import ru.mirea.examcenter.service.ExamCenterService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

/** Интеграционная проверка реальной PostgreSQL; временные записи удаляются после запуска. */
public final class IntegrationSmoke {
    private IntegrationSmoke() { }

    private interface CheckedAction { void run() throws Exception; }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void mustReject(CheckedAction action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (BusinessException expected) {
            System.out.println("OK: " + expected.getMessage());
        }
    }

    private static Connection testConnection() throws Exception {
        String url = System.getenv().getOrDefault("EXAM_DB_URL", "jdbc:postgresql://localhost:5432/exam_center");
        String user = System.getenv().getOrDefault("EXAM_DB_USER", "postgres");
        String password = System.getenv().getOrDefault("EXAM_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) throws Exception {
        ExamCenterService service = new ExamCenterService(new ExamCenterRepository());
        String email = "smoke-" + UUID.randomUUID() + "@example.org";
        long applicantId = service.createApplicant("Тестовый Кандидат", email, "+7 900 000-00-00");
        long applicationId = 0;
        try {
            require(service.getApplicant(applicantId).getEmail().equals(email), "create/get applicant");
            mustReject(() -> service.createApplicant("Еще Кандидат", email, ""), "duplicate email accepted");
            mustReject(() -> service.createApplication(99999999L, "Математика", LocalDateTime.now().plusDays(1)),
                    "missing applicant accepted");
            mustReject(() -> service.createApplication(applicantId, " ", LocalDateTime.now().plusDays(1)),
                    "empty exam accepted");
            mustReject(() -> service.createApplication(applicantId, "Математика", LocalDateTime.now().minusDays(1)),
                    "past date accepted");

            LocalDateTime slot = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
            applicationId = service.createApplication(applicantId, "Математика", slot);
            final long id = applicationId;
            require(service.getApplication(id).getStatus() == ApplicationStatus.NEW, "create/get application");
            require(service.searchByExam("матем").stream().anyMatch(x -> x.getId() == id), "search exam");
            require(service.searchByApplicant("тестовый").stream().anyMatch(x -> x.getId() == id), "search applicant");
            require(service.filterByStatus(ApplicationStatus.NEW).stream().anyMatch(x -> x.getId() == id),
                    "filter status");
            require(service.filterByDate(slot.minusDays(1), slot.plusDays(1)).stream()
                    .anyMatch(x -> x.getId() == id), "filter date");
            require(!service.sortByDate().isEmpty() && !service.sortByApplicant().isEmpty(), "sort");
            mustReject(() -> service.createApplication(applicantId, "Математика", slot),
                    "duplicate slot accepted");

            LocalDateTime changed = slot.plusDays(1);
            service.updateApplication(id, applicantId, "Физика", changed);
            require(service.getApplication(id).getExamName().equals("Физика"), "update application");
            service.changeStatus(id, ApplicationStatus.APPROVED, null);
            mustReject(() -> service.changeStatus(id, ApplicationStatus.NEW, null),
                    "invalid transition accepted");
            mustReject(() -> service.changeStatus(id, ApplicationStatus.COMPLETED, 90),
                    "early completion accepted");
            mustReject(() -> service.changeStatus(id, ApplicationStatus.CANCELLED, 90),
                    "score on cancellation accepted");

            try (Connection c = testConnection();
                 PreparedStatement s = c.prepareStatement(
                         "UPDATE exam_applications SET scheduled_at=? WHERE id=?")) {
                s.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                s.setLong(2, id);
                require(s.executeUpdate() == 1, "test date setup");
            }
            mustReject(() -> service.changeStatus(id, ApplicationStatus.COMPLETED, 101),
                    "invalid score accepted");
            service.changeStatus(id, ApplicationStatus.COMPLETED, 90);
            require(service.getApplication(id).getScore() == 90, "completion score");
            require(service.countsByStatus().get(ApplicationStatus.COMPLETED) >= 1, "statistics");
            require(!service.databaseTables().isEmpty(), "database table view");
            mustReject(() -> service.updateApplication(id, applicantId, "Химия", slot),
                    "completed application edited");

            Path file = Paths.get("target/integration-export.xlsx");
            DataExporter exporter = new XlsxExporter();
            exporter.export(file, service.allApplicants(), service.allApplications());
            try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
                require(workbook.getNumberOfSheets() == 2, "Excel sheet count");
                require(workbook.getSheet("Кандидаты").getLastRowNum() >= 1, "Excel applicants");
                require(workbook.getSheet("Заявки").getLastRowNum() >= 1, "Excel applications");
                require(workbook.getSheet("Заявки").getRow(1).getCell(5).getCellType() == CellType.NUMERIC,
                        "Excel date cell type");
                require(DateUtil.isCellDateFormatted(workbook.getSheet("Заявки").getRow(1).getCell(5)),
                        "Excel date format");
            }

            service.deleteApplication(id);
            applicationId = 0;
            mustReject(() -> service.getApplication(id), "deleted application found");
            System.out.println("INTEGRATION SMOKE PASSED");
        } finally {
            if (applicationId != 0) {
                try { service.deleteApplication(applicationId); } catch (Exception ignored) { }
            }
            try (Connection c = testConnection();
                 PreparedStatement s = c.prepareStatement("DELETE FROM applicants WHERE id=?")) {
                s.setLong(1, applicantId);
                s.executeUpdate();
            }
        }
    }
}
