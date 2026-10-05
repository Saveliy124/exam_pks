package ru.mirea.examcenter.repository;

import ru.mirea.examcenter.model.ApplicationStatus;
import ru.mirea.examcenter.model.ExamApplication;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC и SQL для заявок на экзамен. */
public final class ExamApplicationRepository {
    private static final String APPLICATION_SELECT =
            "SELECT e.id,e.applicant_id,a.full_name,a.email,e.exam_name," +
            "e.scheduled_at,e.status,e.score,e.created_at " +
            "FROM exam_applications e JOIN applicants a ON a.id=e.applicant_id ";

    public void checkConnection() throws SQLException {
        try (Connection ignored = Database.connect()) {
            // Ошибка подключения будет передана вызывающему коду.
        }
    }

    public long createApplication(long applicantId, String exam, LocalDateTime date) throws SQLException {
        String sql = "INSERT INTO exam_applications(applicant_id,exam_name,scheduled_at,status) " +
                "VALUES(?,?,?,'NEW')";
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, applicantId);
            s.setString(2, exam);
            s.setTimestamp(3, Timestamp.valueOf(date));
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("База данных не вернула ID заявки.");
    }

    public Optional<ExamApplication> findApplication(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(APPLICATION_SELECT + "WHERE e.id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(application(r)) : Optional.empty();
            }
        }
    }

    public List<ExamApplication> allApplications() throws SQLException {
        List<ExamApplication> result = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(APPLICATION_SELECT + "ORDER BY e.id");
             ResultSet r = s.executeQuery()) {
            while (r.next()) result.add(application(r));
        }
        return result;
    }

    public boolean hasActiveDuplicate(long applicantId, String exam, LocalDateTime date, long excludeId)
            throws SQLException {
        String sql = "SELECT 1 FROM exam_applications WHERE applicant_id=? " +
                "AND lower(exam_name)=lower(?) AND scheduled_at=? " +
                "AND status IN ('NEW','APPROVED') AND id<>?";
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, applicantId);
            s.setString(2, exam);
            s.setTimestamp(3, Timestamp.valueOf(date));
            s.setLong(4, excludeId);
            try (ResultSet r = s.executeQuery()) {
                return r.next();
            }
        }
    }

    public void updateApplication(long id, long applicantId, String exam, LocalDateTime date)
            throws SQLException {
        String sql = "UPDATE exam_applications SET applicant_id=?,exam_name=?,scheduled_at=? WHERE id=?";
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, applicantId);
            s.setString(2, exam);
            s.setTimestamp(3, Timestamp.valueOf(date));
            s.setLong(4, id);
            s.executeUpdate();
        }
    }

    public void updateStatus(long id, ApplicationStatus status, Integer score) throws SQLException {
        String sql = "UPDATE exam_applications SET status=?,score=? WHERE id=?";
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status.name());
            if (score == null) s.setNull(2, java.sql.Types.INTEGER);
            else s.setInt(2, score);
            s.setLong(3, id);
            s.executeUpdate();
        }
    }

    public void deleteApplication(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement("DELETE FROM exam_applications WHERE id=?")) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    private ExamApplication application(ResultSet r) throws SQLException {
        int rawScore = r.getInt("score");
        Integer score = r.wasNull() ? null : rawScore;
        return new ExamApplication(r.getLong("id"), r.getLong("applicant_id"),
                r.getString("full_name"), r.getString("email"), r.getString("exam_name"),
                r.getTimestamp("scheduled_at").toLocalDateTime(),
                ApplicationStatus.valueOf(r.getString("status")), score,
                r.getTimestamp("created_at").toLocalDateTime());
    }

    public List<String> databaseTables() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection c = Database.connect()) {
            DatabaseMetaData meta = c.getMetaData();
            try (ResultSet tables = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String name = tables.getString("TABLE_NAME");
                    result.add(name);
                    try (ResultSet columns = meta.getColumns(null, "public", name, "%")) {
                        while (columns.next())
                            result.add("    " + columns.getString("COLUMN_NAME") + " : " +
                                    columns.getString("TYPE_NAME"));
                    }
                }
            }
        }
        return result;
    }
}
