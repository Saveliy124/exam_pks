package ru.mirea.examcenter;

import ru.mirea.examcenter.repository.ApplicantRepository;
import ru.mirea.examcenter.repository.ExamApplicationRepository;
import ru.mirea.examcenter.repository.ExaminerRepository;
import ru.mirea.examcenter.service.ApplicantService;
import ru.mirea.examcenter.service.ExamApplicationService;
import ru.mirea.examcenter.service.ExaminerService;
import ru.mirea.examcenter.ui.ConsoleUi;
import java.sql.SQLException;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            ApplicantService applicants = new ApplicantService(new ApplicantRepository());
            ExamApplicationRepository repository = new ExamApplicationRepository();
            repository.checkConnection();
            new ConsoleUi(applicants, new ExamApplicationService(repository, applicants),
                    new ExaminerService(new ExaminerRepository())).run();
        } catch (SQLException ex) {
            System.err.println("Не удалось подключиться к PostgreSQL: " + ex.getMessage());
            System.err.println("Проверьте EXAM_DB_URL, EXAM_DB_USER, EXAM_DB_PASSWORD и инструкцию README.md.");
        }
    }
}
