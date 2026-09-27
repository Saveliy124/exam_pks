package ru.mirea.examcenter;

import ru.mirea.examcenter.repository.ExamCenterRepository;
import ru.mirea.examcenter.service.ExamCenterService;
import ru.mirea.examcenter.ui.ConsoleUi;
import java.sql.SQLException;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            ExamCenterRepository repository = new ExamCenterRepository();
            repository.checkConnection();
            new ConsoleUi(new ExamCenterService(repository)).run();
        } catch (SQLException ex) {
            System.err.println("Не удалось подключиться к PostgreSQL: " + ex.getMessage());
            System.err.println("Проверьте EXAM_DB_URL, EXAM_DB_USER, EXAM_DB_PASSWORD и инструкцию README.md.");
        }
    }
}
