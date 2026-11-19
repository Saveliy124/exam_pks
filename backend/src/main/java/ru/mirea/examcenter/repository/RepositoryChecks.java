package ru.mirea.examcenter.repository;

import java.sql.SQLException;

final class RepositoryChecks {
  private RepositoryChecks() {}

  static void changed(int rows) throws SQLException {
    if (rows != 1)
      throw new SQLException(
          "Запись удалена или изменена. Обновите данные и повторите действие.", "40001");
  }
}
