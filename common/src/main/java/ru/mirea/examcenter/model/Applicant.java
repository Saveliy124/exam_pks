package ru.mirea.examcenter.model;

public final class Applicant {
  public Applicant() {}

  private long id;
  private String fullName;
  private String email;
  private String phone;
  private ApplicantStatus status;

  public Applicant(long id, String fullName, String email, String phone, ApplicantStatus status) {
    this.id = id;
    this.fullName = fullName;
    this.email = email;
    this.phone = phone;
    this.status = status;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public ApplicantStatus getStatus() {
    return status;
  }

  public void setStatus(ApplicantStatus status) {
    this.status = status;
  }
}
