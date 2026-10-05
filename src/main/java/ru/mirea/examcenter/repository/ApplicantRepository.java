package ru.mirea.examcenter.repository;

import ru.mirea.examcenter.model.Applicant;
import ru.mirea.examcenter.model.ApplicantStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ApplicantRepository {
    public long create(String name, String email, String phone, ApplicantStatus status) throws SQLException {
        String sql = "INSERT INTO applicants(full_name,email,phone,status) VALUES(?,?,?,?)";
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, phone);
            s.setString(4, status.name());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("База не вернула ID кандидата.");
    }

    public Optional<Applicant> find(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT id,full_name,email,phone,status FROM applicants WHERE id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(map(r)) : Optional.empty();
            }
        }
    }

    public List<Applicant> all() throws SQLException {
        List<Applicant> result = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT id,full_name,email,phone,status FROM applicants ORDER BY id");
             ResultSet r = s.executeQuery()) {
            while (r.next()) result.add(map(r));
        }
        return result;
    }

    public void update(long id, String name, String email, String phone, ApplicantStatus status)
            throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE applicants SET full_name=?,email=?,phone=?,status=? WHERE id=?")) {
            s.setString(1, name);
            s.setString(2, email);
            s.setString(3, phone);
            s.setString(4, status.name());
            s.setLong(5, id);
            s.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement("DELETE FROM applicants WHERE id=?")) {
            s.setLong(1, id);
            s.executeUpdate();
        }
    }

    public boolean emailExists(String email, long excludeId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT 1 FROM applicants WHERE lower(email)=lower(?) AND id<>?")) {
            s.setString(1, email);
            s.setLong(2, excludeId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    public boolean hasApplications(long id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement s = c.prepareStatement(
                     "SELECT 1 FROM exam_applications WHERE applicant_id=?")) {
            s.setLong(1, id);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    private Applicant map(ResultSet r) throws SQLException {
        return new Applicant(r.getLong("id"), r.getString("full_name"),
                r.getString("email"), r.getString("phone"),
                ApplicantStatus.valueOf(r.getString("status")));
    }
}
