package ru.mirea.examcenter;

import java.nio.file.*;
import java.sql.*;
import ru.mirea.examcenter.repository.Database;

/** Интеграционные тесты имеют право очищать только явно указанную БД *_test. */
public final class TestDatabase {
  private TestDatabase() {}

  public static void configure() throws Exception {
    String url = System.getenv("EXAM_TEST_DB_URL");
    if (url == null || !url.matches("jdbc:postgresql://[^/]+/[a-zA-Z0-9_]+_test"))
      throw new IllegalStateException(
          "EXAM_TEST_DB_URL должен явно указывать отдельную базу с суффиксом _test. Запустите"
              + " scripts/test.sh.");
    System.setProperty("exam.db.url", url);
    try (var connection = Database.connect();
        var statement = connection.createStatement()) {
      try (var result = statement.executeQuery("SELECT current_database()")) {
        result.next();
        if (!result.getString(1).endsWith("_test"))
          throw new IllegalStateException("Отказ: рабочую БД очищать нельзя");
      }
      statement.execute(Files.readString(Path.of("../sql/schema.sql")));
    }
  }

  public static void clear() throws SQLException {
    try (var connection = Database.connect();
        var statement = connection.createStatement()) {
      try (var result = statement.executeQuery("SELECT current_database()")) {
        result.next();
        if (!result.getString(1).endsWith("_test"))
          throw new IllegalStateException("Отказ очищать рабочую БД");
      }
      statement.execute("TRUNCATE exam_applications, applicants, examiners RESTART IDENTITY");
    }
  }

  public static void fixture() throws Exception {
    clear();
    try (var connection = Database.connect();
        var statement = connection.createStatement()) {
      statement.execute(Files.readString(Path.of("../sql/demo.sql")));
    }
  }
}
