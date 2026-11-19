package ru.mirea.examcenter.kr3.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
@Entity
@Table(name="applicants")
public class Applicant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="public_id",nullable=false,unique=true)
    private UUID publicId;
    @Version
    private long version;
    @Column(name="full_name",nullable=false,length=120)
    private String fullName;
    @Column(nullable=false,length=254)
    private String email;
    @Column(nullable=false,length=30)
    private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12)
    private ApplicantStatus status;
    public Applicant() { }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public void setPublicId(UUID value) { this.publicId = value; }
    public long getVersion() { return version; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { this.fullName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public ApplicantStatus getStatus() { return status; }
    public void setStatus(ApplicantStatus value) { this.status = value; }

}
