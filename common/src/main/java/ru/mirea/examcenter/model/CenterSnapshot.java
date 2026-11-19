package ru.mirea.examcenter.model;

import java.util.List;

public record CenterSnapshot(
    List<Applicant> applicants, List<ExamApplication> applications, List<Examiner> examiners) {
  public CenterSnapshot {
    applicants = List.copyOf(applicants);
    applications = List.copyOf(applications);
    examiners = List.copyOf(examiners);
  }

  public static CenterSnapshot empty() {
    return new CenterSnapshot(List.of(), List.of(), List.of());
  }
}
