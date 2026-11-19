package ru.mirea.examcenter.kr3.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity
@Table(name="exam_application_note")
public class ApplicationNote extends AuditedVersionedEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false)
    private ExamApplication application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id",nullable=false)
    private UserAccount author;
    @Column(nullable=false,length=4000)
    private String body;
    @Column(nullable=false)
    private boolean pinned;
    public ApplicationNote() { }
    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }
    public ExamApplication getApplication() { return application; }
    public void setApplication(ExamApplication value) { this.application = value; }
    public UserAccount getAuthor() { return author; }
    public void setAuthor(UserAccount value) { this.author = value; }
    public String getBody() { return body; }
    public void setBody(String value) { this.body = value; }
    public boolean getPinned() { return pinned; }
    public void setPinned(boolean value) { this.pinned = value; }

}
