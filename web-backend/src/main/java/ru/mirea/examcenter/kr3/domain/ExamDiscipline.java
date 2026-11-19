package ru.mirea.examcenter.kr3.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity
@Table(name="exam_discipline")
public class ExamDiscipline extends AuditedVersionedEntity {
    @Id
    private UUID id;
    @Column(nullable=false,length=50)
    private String code;
    @Column(nullable=false,length=120)
    private String name;
    @Column(nullable=false)
    private boolean active;
    public ExamDiscipline() { }
    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { this.active = value; }

}
