package ru.mirea.examcenter.model;

import java.time.LocalDateTime;

public final class ExamApplication {
    private long id;
    private long applicantId;
    private String applicantName;
    private String applicantEmail;
    private String examName;
    private LocalDateTime scheduledAt;
    private ApplicationStatus status;
    private Integer score;
    private LocalDateTime createdAt;

    public ExamApplication(long id, long applicantId, String applicantName, String applicantEmail,
                           String examName, LocalDateTime scheduledAt, ApplicationStatus status,
                           Integer score, LocalDateTime createdAt) {
        this.id = id;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.examName = examName;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.score = score;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getApplicantId() { return applicantId; }
    public void setApplicantId(long applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
