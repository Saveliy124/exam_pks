package ru.mirea.examcenter.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** JDBC configuration. No UI dependencies and no password in the source. */
public final class Database {
  private Database() {}

  public static Connection connect() throws SQLException {
    Properties config = configuration();
    Properties connection = new Properties();
    connection.setProperty("user", value("user", config));
    connection.setProperty("password", value("password", config));
    connection.setProperty("connectTimeout", config.getProperty("db.connectTimeout", "5"));
    connection.setProperty("socketTimeout", config.getProperty("db.socketTimeout", "15"));
    connection.setProperty("ApplicationName", "Exam Center KR2");
    return DriverManager.getConnection(value("url", config), connection);
  }

  public static String configuredUrl() throws SQLException {
    return value("url", configuration());
  }

  private static String value(String key, Properties config) {
    String explicit = System.getProperty("exam.db." + key);
    if (explicit != null) return explicit;
    String environment = System.getenv("EXAM_DB_" + key.toUpperCase(java.util.Locale.ROOT));
    return environment != null ? environment : config.getProperty("db." + key, "");
  }

  private static Properties configuration() throws SQLException {
    Properties p = new Properties();
    try (InputStream input = Database.class.getResourceAsStream("/database.properties")) {
      if (input != null) p.load(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
      String custom = System.getProperty("exam.config", System.getenv("EXAM_DB_CONFIG"));
      Path local = custom == null ? Path.of("config", "database.properties") : Path.of(custom);
      if (Files.isRegularFile(local)) {
        try (var reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
          p.load(reader);
        }
      } else if (custom != null) {
        throw new IOException("Файл настроек подключения не найден.");
      }
      return p;
    } catch (IOException | IllegalArgumentException ex) {
      throw new SQLException("Не удалось прочитать настройки подключения к БД.", "08001", ex);
    }
  }
}
