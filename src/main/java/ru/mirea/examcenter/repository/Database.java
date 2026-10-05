package ru.mirea.examcenter.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Общие параметры подключения; SQL каждой сущности находится в её репозитории. */
public final class Database {
    private Database() { }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                value("EXAM_DB_URL", "jdbc:postgresql://localhost:5432/exam_center"),
                value("EXAM_DB_USER", "postgres"), value("EXAM_DB_PASSWORD", ""));
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
