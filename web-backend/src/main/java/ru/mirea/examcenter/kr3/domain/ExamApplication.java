package ru.mirea.examcenter.kr3.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity
@Table(name="exam_applications")
public class ExamApplication {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="public_id",nullable=false,unique=true)
    private UUID publicId;
    @Version
    private long version;
    @Column(name="external_id",nullable=false,length=100)
    private String externalId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="applicant_id",nullable=false)
    private Applicant applicant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="discipline_id",nullable=false)
    private ExamDiscipline discipline;
    @Column(name="exam_name",nullable=false,length=120)
    private String examName;
    @Column(name="scheduled_at",nullable=false)
    private LocalDateTime scheduledAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12)
    private ApplicationStatus status;
    @Column
    private Short score;
    @Column(name="preparation_percent",nullable=false)
    private int preparationPercent;
    @Column(name="documents_verified",nullable=false)
    private boolean documentsVerified;
    @Enumerated(EnumType.STRING) @Column(name="risk_level",nullable=false,length=32)
    private RiskLevel riskLevel;
    @Column(nullable=false)
    private boolean archived;
    @Column(name="archived_at")
    private Instant archivedAt;
    @Column(name="created_at",nullable=false,updatable=false)
    private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false)
    private Instant updatedAt;
    @ElementCollection @CollectionTable(name="exam_application_tag",joinColumns=@JoinColumn(name="application_id")) @Column(name="tag",length=50,nullable=false)
    private Set<String> tags;
    public ExamApplication() { }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public void setPublicId(UUID value) { this.publicId = value; }
    public long getVersion() { return version; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String value) { this.externalId = value; }
    public Applicant getApplicant() { return applicant; }
    public void setApplicant(Applicant value) { this.applicant = value; }
    public ExamDiscipline getDiscipline() { return discipline; }
    public void setDiscipline(ExamDiscipline value) { this.discipline = value; }
    public String getExamName() { return examName; }
    public void setExamName(String value) { this.examName = value; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime value) { this.scheduledAt = value; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus value) { this.status = value; }
    public Short getScore() { return score; }
    public void setScore(Short value) { this.score = value; }
    public int getPreparationPercent() { return preparationPercent; }
    public void setPreparationPercent(int value) { this.preparationPercent = value; }
    public boolean getDocumentsVerified() { return documentsVerified; }
    public void setDocumentsVerified(boolean value) { this.documentsVerified = value; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel value) { this.riskLevel = value; }
    public boolean getArchived() { return archived; }
    public void setArchived(boolean value) { this.archived = value; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant value) { this.archivedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> value) { this.tags = value; }

    @PrePersist void createTimes() { if (publicId==null) publicId=UUID.randomUUID(); if (createdAt==null) createdAt=LocalDateTime.now(); updatedAt=Instant.now(); if(tags==null) tags=new LinkedHashSet<>(); }
    @PreUpdate void updateTime() { updatedAt=Instant.now(); }

}
