package ru.mirea.examcenter;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.examcenter.export.DataExporter;
import ru.mirea.examcenter.export.XlsxExporter;
import ru.mirea.examcenter.model.ApplicantStatus;
import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExaminerStatus;
import ru.mirea.examcenter.repository.ApplicantRepository;
import ru.mirea.examcenter.repository.Database;
import ru.mirea.examcenter.repository.ExamApplicationRepository;
import ru.mirea.examcenter.repository.ExaminerRepository;
import ru.mirea.examcenter.service.ApplicantService;
import ru.mirea.examcenter.service.ApplicantService.ApplicantException;
import ru.mirea.examcenter.service.ExamApplicationService;
import ru.mirea.examcenter.service.ExamApplicationService.ApplicationException;
import ru.mirea.examcenter.service.ExaminerService;
import ru.mirea.examcenter.service.ExaminerService.ExaminerException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Проверка трёх сущностей на работающей PostgreSQL; временные записи удаляются. */
public final class IntegrationSmoke {
    private IntegrationSmoke() { }
    private interface Action { void run() throws Exception; }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void rejects(Action action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (ApplicantException | ApplicationException | ExaminerException expected) {
            System.out.println("OK: " + expected.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        ApplicantService applicants = new ApplicantService(new ApplicantRepository());
        ExamApplicationService applications = new ExamApplicationService(
                new ExamApplicationRepository(), applicants);
        ExaminerService examiners = new ExaminerService(new ExaminerRepository());
        String marker = UUID.randomUUID().toString().substring(0, 8);
        String applicantEmail = "candidate-" + marker + "@example.org";
        String examinerEmail = "examiner-" + marker + "@example.org";
        long applicantId = 0, applicationId = 0, examinerId = 0;
        try {
            applicantId = applicants.createApplicant("Тестовый Кандидат", applicantEmail, "123",
                    ApplicantStatus.ACTIVE);
            examinerId = examiners.createExaminer("Тестовый Экзаменатор", examinerEmail,
                    "Математика", ExaminerStatus.ACTIVE);
            final long aid = applicantId, eid = examinerId;

            check(applicants.getApplicant(aid).getStatus() == ApplicantStatus.ACTIVE, "applicant get");
            applicants.updateApplicant(aid, "Новый Кандидат", applicantEmail, "456",
                    ApplicantStatus.BLOCKED);
            check(applicants.getApplicant(aid).getPhone().equals("456"), "applicant update");
            check(applicants.searchByName("новый").stream().anyMatch(x -> x.getId() == aid),
                    "applicant search");
            check(applicants.filterByStatus(ApplicantStatus.BLOCKED).stream().anyMatch(x -> x.getId() == aid),
                    "applicant status filter");
            check(applicants.filterByEmailDomain("example.org").stream().anyMatch(x -> x.getId() == aid),
                    "applicant email filter");
            check(!applicants.sortByName(true).isEmpty() && !applicants.sortByName(false).isEmpty()
                    && !applicants.sortByEmail(true).isEmpty()
                    && !applicants.sortByEmail(false).isEmpty(), "applicant sorts");
            check(applicants.sortByName(true).get(0).getId() ==
                    applicants.sortByName(false).get(applicants.allApplicants().size() - 1).getId(),
                    "applicant reverse name order");
            check(applicants.sortByEmail(true).get(0).getId() ==
                    applicants.sortByEmail(false).get(applicants.allApplicants().size() - 1).getId(),
                    "applicant reverse email order");
            rejects(() -> applications.createApplication(aid, "Математика",
                    LocalDateTime.now().plusDays(2)), "blocked applicant accepted");
            applicants.updateApplicant(aid, "Новый Кандидат", applicantEmail, "456",
                    ApplicantStatus.ACTIVE);

            check(examiners.getExaminer(eid).getSubject().equals("Математика"), "examiner get");
            examiners.updateExaminer(eid, "Новый Экзаменатор", examinerEmail,
                    "Физика", ExaminerStatus.INACTIVE);
            check(examiners.searchByName("новый").stream().anyMatch(x -> x.getId() == eid),
                    "examiner search");
            check(examiners.filterByStatus(ExaminerStatus.INACTIVE).stream().anyMatch(x -> x.getId() == eid),
                    "examiner status filter");
            check(examiners.filterBySubject("физ").stream().anyMatch(x -> x.getId() == eid),
                    "examiner subject filter");
            check(!examiners.sortByName(true).isEmpty() && !examiners.sortByName(false).isEmpty()
                    && !examiners.sortBySubject(true).isEmpty()
                    && !examiners.sortBySubject(false).isEmpty(), "examiner sorts");
            check(examiners.sortByName(true).get(0).getId() ==
                    examiners.sortByName(false).get(examiners.allExaminers().size() - 1).getId(),
                    "examiner reverse name order");
            check(examiners.sortBySubject(true).get(0).getId() ==
                    examiners.sortBySubject(false).get(examiners.allExaminers().size() - 1).getId(),
                    "examiner reverse subject order");

            LocalDateTime slot = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
            applicationId = applications.createApplication(aid, "Математика", slot);
            final long rid = applicationId;
            check(applications.getApplication(rid).getStatus() == ApplicationStatus.NEW, "application get");
            rejects(() -> applicants.deleteApplicant(aid), "candidate with applications deleted");
            rejects(() -> applications.createApplication(aid, "Математика", slot),
                    "duplicate application accepted");
            check(applications.searchByExam("матем").stream().anyMatch(x -> x.getId() == rid),
                    "application search exam");
            check(applications.searchByApplicant("новый").stream().anyMatch(x -> x.getId() == rid),
                    "application search applicant");
            check(applications.filterByStatus(ApplicationStatus.NEW).stream().anyMatch(x -> x.getId() == rid),
                    "application status filter");
            check(applications.filterByDate(slot.minusDays(1), slot.plusDays(1)).stream()
                    .anyMatch(x -> x.getId() == rid), "application date filter");
            check(!applications.sortByDate(true).isEmpty() && !applications.sortByDate(false).isEmpty()
                    && !applications.sortByApplicant(true).isEmpty()
                    && !applications.sortByApplicant(false).isEmpty(), "application sorts");
            check(applications.sortByDate(true).get(0).getId() ==
                    applications.sortByDate(false).get(applications.allApplications().size() - 1).getId(),
                    "application reverse date order");
            check(applications.sortByApplicant(true).get(0).getId() ==
                    applications.sortByApplicant(false).get(applications.allApplications().size() - 1).getId(),
                    "application reverse applicant order");
            applications.updateApplication(rid, aid, "Физика", slot.plusDays(1));
            check(applications.getApplication(rid).getExamName().equals("Физика"), "application update");
            applications.changeStatus(rid, ApplicationStatus.APPROVED, null);
            rejects(() -> applications.changeStatus(rid, ApplicationStatus.NEW, null),
                    "invalid transition accepted");
            rejects(() -> applications.changeStatus(rid, ApplicationStatus.COMPLETED, 90),
                    "early completion accepted");
            try (Connection c = Database.connect();
                 PreparedStatement s = c.prepareStatement(
                         "UPDATE exam_applications SET scheduled_at=? WHERE id=?")) {
                s.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
                s.setLong(2, rid);
                s.executeUpdate();
            }
            applications.changeStatus(rid, ApplicationStatus.COMPLETED, 90);
            check(applications.getApplication(rid).getScore() == 90, "application completion");
            check(!applications.databaseTables().isEmpty(), "metadata");

            Path file = Paths.get("target/integration-export.xlsx");
            DataExporter exporter = new XlsxExporter();
            exporter.export(file, applicants.allApplicants(), applications.allApplications(),
                    examiners.allExaminers());
            try (Workbook book = new XSSFWorkbook(Files.newInputStream(file))) {
                check(book.getNumberOfSheets() == 3, "Excel sheet count");
                check(book.getSheet("Кандидаты") != null && book.getSheet("Заявки") != null
                        && book.getSheet("Экзаменаторы") != null, "Excel sheet names");
            }

            applications.deleteApplication(rid);
            applicationId = 0;
            rejects(() -> applications.getApplication(rid), "deleted application found");
            applicants.deleteApplicant(aid);
            applicantId = 0;
            rejects(() -> applicants.getApplicant(aid), "deleted applicant found");
            examiners.deleteExaminer(eid);
            examinerId = 0;
            rejects(() -> examiners.getExaminer(eid), "deleted examiner found");
            System.out.println("THREE ENTITY INTEGRATION PASSED");
        } finally {
            if (applicationId != 0) try { applications.deleteApplication(applicationId); }
                catch (Exception ignored) { }
            if (applicantId != 0) try { applicants.deleteApplicant(applicantId); }
                catch (Exception ignored) { }
            if (examinerId != 0) try { examiners.deleteExaminer(examinerId); }
                catch (Exception ignored) { }
        }
    }
}
