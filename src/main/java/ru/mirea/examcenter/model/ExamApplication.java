package ru.mirea.examcenter.model;

import java.time.LocalDateTime;

public final class ExamApplication {
    private final long id;
    private final long applicantId;
    private final String applicantName;
    private final String applicantEmail;
    private final String examName;
    private final LocalDateTime scheduledAt;
    private final ApplicationStatus status;
    private final Integer score;
    private final LocalDateTime createdAt;

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
    public long getApplicantId() { return applicantId; }
    public String getApplicantName() { return applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public String getExamName() { return examName; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public ApplicationStatus getStatus() { return status; }
    public Integer getScore() { return score; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
