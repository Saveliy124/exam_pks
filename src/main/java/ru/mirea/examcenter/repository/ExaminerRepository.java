package ru.mirea.examcenter.repository;

import ru.mirea.examcenter.model.Examiner;
import ru.mirea.examcenter.model.ExaminerStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ExaminerRepository {
    public long create(String name, String email, String subject, ExaminerStatus status)
            throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO examiners(full_name,email,subject,status) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, subject);
            s.setString(4, status.name());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("База не вернула ID экзаменатора.");
    }

    public Optional<Examiner> find(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT id,full_name,email,subject,status FROM examiners WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(map(r)) : Optional.empty();
            }
        }
    }

    public List<Examiner> all() throws SQLException {
        List<Examiner> result = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT id,full_name,email,subject,status FROM examiners ORDER BY id");
             ResultSet r = s.executeQuery()) {
            while (r.next()) result.add(map(r));
        }
        return result;
    }

    public void update(long id, String name, String email, String subject, ExaminerStatus status)
            throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE examiners SET full_name=?,email=?,subject=?,status=? WHERE id=?")) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, subject);
            s.setString(4, status.name());
            s.setLong(5, id);
            s.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement("DELETE FROM examiners WHERE id=?")) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    public boolean emailExists(String email, long excludeId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT 1 FROM examiners WHERE lower(email)=lower(?) AND id<>?")) {
            s.setString(1, email);
            s.setLong(2, excludeId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    private Examiner map(ResultSet r) throws SQLException {
        return new Examiner(r.getLong("id"), r.getString("full_name"),
                r.getString("email"), r.getString("subject"),
                ExaminerStatus.valueOf(r.getString("status")));
    }
}
