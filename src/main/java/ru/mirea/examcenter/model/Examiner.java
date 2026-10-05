package ru.mirea.examcenter.model;

public final class Examiner {
    private long id;
    private String fullName;
    private String email;
    private String subject;
    private ExaminerStatus status;

    public Examiner(long id, String fullName, String email, String subject, ExaminerStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subject = subject;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public ExaminerStatus getStatus() { return status; }
    public void setStatus(ExaminerStatus status) { this.status = status; }
}
