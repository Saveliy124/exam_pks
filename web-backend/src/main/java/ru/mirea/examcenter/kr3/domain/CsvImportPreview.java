package ru.mirea.examcenter.kr3.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity
@Table(name="csv_import_preview")
public class CsvImportPreview {
    @Id
    private UUID id;
    @Column(name="owner_id",nullable=false)
    private UUID ownerId;
    @Column(name="file_hash",nullable=false,length=64)
    private String fileHash;
    @Column(nullable=false,length=20)
    private String mode;
    @Column(name="expires_at",nullable=false)
    private Instant expiresAt;
    @Column(name="applied_at")
    private Instant appliedAt;
    @Column(nullable=false,columnDefinition="TEXT")
    private String content;
    public CsvImportPreview() { }
    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID value) { this.ownerId = value; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String value) { this.fileHash = value; }
    public String getMode() { return mode; }
    public void setMode(String value) { this.mode = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant value) { this.appliedAt = value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content = value; }

}
