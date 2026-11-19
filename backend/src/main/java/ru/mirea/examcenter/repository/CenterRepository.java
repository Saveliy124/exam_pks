package ru.mirea.examcenter.repository;

import java.sql.Connection;
import java.sql.SQLException;
import ru.mirea.examcenter.model.CenterSnapshot;

/** One consistent snapshot for tables and statistics; no joins multiplying counts. */
public final class CenterRepository {
  private final javax.sql.DataSource source;

  public CenterRepository() {
    this(null);
  }

  public CenterRepository(javax.sql.DataSource source) {
    this.source = source;
  }

  private java.sql.Connection connection() throws java.sql.SQLException {
    return source == null ? Database.connect() : source.getConnection();
  }

  public CenterSnapshot load() throws SQLException {
    try (Connection c = connection()) {
      c.setReadOnly(true);
      c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      c.setAutoCommit(false);
      try {
        CenterSnapshot result =
            new CenterSnapshot(
                new ApplicantRepository().all(c),
                new ExamApplicationRepository().allApplications(c),
                new ExaminerRepository().all(c));
        c.commit();
        return result;
      } catch (SQLException | RuntimeException ex) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          ex.addSuppressed(rollback);
        }
        throw ex;
      }
    }
  }
}
