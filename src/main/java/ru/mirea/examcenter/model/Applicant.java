package ru.mirea.examcenter.model;

public final class Applicant {
    private final long id;
    private final String fullName;
    private final String email;
    private final String phone;

    public Applicant(long id, String fullName, String email, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
