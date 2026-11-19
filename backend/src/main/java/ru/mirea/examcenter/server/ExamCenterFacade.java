package ru.mirea.examcenter.server;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class ExamCenterFacade {
  private final ApplicantService applicants;
  private final ExaminerService examiners;
  private final ExamApplicationService applications;
  private final CenterService center;
  private final StatisticsService statistics;

  public ExamCenterFacade(
      ApplicantService a,
      ExaminerService e,
      ExamApplicationService x,
      CenterService c,
      StatisticsService s) {
    applicants = a;
    examiners = e;
    applications = x;
    center = c;
    statistics = s;
  }

  // CenterRepository opens one explicit repeatable-read snapshot on a separate read-only
  // connection.
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public CenterSnapshot snapshot() throws SQLException {
    return center.load();
  }

  public List<Applicant> applicants() throws SQLException {
    return applicants.allApplicants();
  }

  public Applicant applicant(long id) throws SQLException {
    ensureApplicant(id);
    return applicants.getApplicant(id);
  }

  public long create(ApplicantInput p) throws SQLException {
    return applicants.createApplicant(p.fullName(), p.email(), p.phone(), p.status());
  }

  public void update(long id, ApplicantInput p) throws SQLException {
    ensureApplicant(id);
    applicants.updateApplicant(id, p.fullName(), p.email(), p.phone(), p.status());
  }

  public void deleteApplicant(long id) throws SQLException {
    ensureApplicant(id);
    applicants.deleteApplicant(id);
  }

  public List<Examiner> examiners() throws SQLException {
    return examiners.allExaminers();
  }

  public Examiner examiner(long id) throws SQLException {
    ensureExaminer(id);
    return examiners.getExaminer(id);
  }

  public long create(ExaminerInput p) throws SQLException {
    return examiners.createExaminer(p.fullName(), p.email(), p.subject(), p.status());
  }

  public void update(long id, ExaminerInput p) throws SQLException {
    ensureExaminer(id);
    examiners.updateExaminer(id, p.fullName(), p.email(), p.subject(), p.status());
  }

  public void deleteExaminer(long id) throws SQLException {
    ensureExaminer(id);
    examiners.deleteExaminer(id);
  }

  public List<ExamApplication> applications() throws SQLException {
    return applications.allApplications();
  }

  public ExamApplication application(long id) throws SQLException {
    ensureApplication(id);
    return applications.getApplication(id);
  }

  public long create(ApplicationInput p) throws SQLException {
    Rules.required("applicant", p.applicantId(), "Выберите кандидата.");
    return applications.createApplication(p.applicantId(), p.examName(), p.scheduledAt());
  }

  public void update(long id, ApplicationInput p) throws SQLException {
    ensureApplication(id);
    Rules.required("applicant", p.applicantId(), "Выберите кандидата.");
    applications.updateApplication(id, p.applicantId(), p.examName(), p.scheduledAt());
  }

  public void deleteApplication(long id) throws SQLException {
    ensureApplication(id);
    applications.deleteApplication(id);
  }

  public void status(long id, StatusInput p) throws SQLException {
    ensureApplication(id);
    Rules.required("status", p.status(), "Выберите статус.");
    applications.changeStatus(id, p.status(), p.score());
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public Statistics statistics(LocalDate from, LocalDate to) throws SQLException {
    var s = statistics.calculate(snapshot(), from, to);
    return new Statistics(
        s.applicants(),
        s.activeExaminers(),
        s.total(),
        s.byStatus(),
        s.upcoming(),
        s.average().isPresent() ? s.average().getAsDouble() : null,
        s.minimum().isPresent() ? s.minimum().getAsInt() : null,
        s.maximum().isPresent() ? s.maximum().getAsInt() : null,
        s.completedPercent());
  }

  private void ensureApplicant(long id) throws SQLException {
    try {
      applicants.getApplicant(id);
    } catch (ApplicantService.ApplicantException missing) {
      throw new NotFoundException();
    }
  }

  private void ensureExaminer(long id) throws SQLException {
    try {
      examiners.getExaminer(id);
    } catch (ExaminerService.ExaminerException missing) {
      throw new NotFoundException();
    }
  }

  private void ensureApplication(long id) throws SQLException {
    try {
      applications.getApplication(id);
    } catch (ExamApplicationService.ApplicationException missing) {
      throw new NotFoundException();
    }
  }
}
