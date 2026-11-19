package ru.mirea.examcenter.server;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mirea.examcenter.api.ApiContracts.*;
import ru.mirea.examcenter.model.*;
import ru.mirea.examcenter.service.Filters;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final ExamCenterFacade service;
  private final javax.sql.DataSource source;

  public ApiController(ExamCenterFacade service, javax.sql.DataSource source) {
    this.service = service;
    this.source = source;
  }

  @GetMapping("/info")
  public java.util.Map<String, String> info() throws SQLException {
    try (var c = source.getConnection();
        var s = c.createStatement();
        var rs = s.executeQuery("SELECT current_database()")) {
      rs.next();
      return java.util.Map.of(
          "database",
          rs.getString(1),
          "version",
          "3.0.0",
          "timezone",
          java.time.ZoneId.systemDefault().getId());
    }
  }

  @GetMapping("/snapshot")
  public CenterSnapshot snapshot() throws SQLException {
    return service.snapshot();
  }

  @GetMapping("/applicants")
  public List<Applicant> applicants() throws SQLException {
    return service.applicants();
  }

  @GetMapping("/applicants/{id}")
  public Applicant applicant(@PathVariable long id) throws SQLException {
    return service.applicant(id);
  }

  @PostMapping("/applicants")
  public ResponseEntity<Created> createApplicant(@RequestBody ApplicantInput p)
      throws SQLException {
    return created("applicants", service.create(p));
  }

  @PutMapping("/applicants/{id}")
  public ResponseEntity<Void> updateApplicant(@PathVariable long id, @RequestBody ApplicantInput p)
      throws SQLException {
    service.update(id, p);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/applicants/{id}")
  public ResponseEntity<Void> deleteApplicant(@PathVariable long id) throws SQLException {
    service.deleteApplicant(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/examiners")
  public List<Examiner> examiners() throws SQLException {
    return service.examiners();
  }

  @GetMapping("/examiners/{id}")
  public Examiner examiner(@PathVariable long id) throws SQLException {
    return service.examiner(id);
  }

  @PostMapping("/examiners")
  public ResponseEntity<Created> createExaminer(@RequestBody ExaminerInput p) throws SQLException {
    return created("examiners", service.create(p));
  }

  @PutMapping("/examiners/{id}")
  public ResponseEntity<Void> updateExaminer(@PathVariable long id, @RequestBody ExaminerInput p)
      throws SQLException {
    service.update(id, p);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/examiners/{id}")
  public ResponseEntity<Void> deleteExaminer(@PathVariable long id) throws SQLException {
    service.deleteExaminer(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/applications")
  public List<ExamApplication> applications(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) ApplicationStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) Long applicantId)
      throws SQLException {
    return service.applications().stream()
        .filter(Filters.applications(search, status, from, to, applicantId))
        .toList();
  }

  @GetMapping("/applications/{id}")
  public ExamApplication application(@PathVariable long id) throws SQLException {
    return service.application(id);
  }

  @PostMapping("/applications")
  public ResponseEntity<Created> createApplication(@RequestBody ApplicationInput p)
      throws SQLException {
    return created("applications", service.create(p));
  }

  @PutMapping("/applications/{id}")
  public ResponseEntity<Void> updateApplication(
      @PathVariable long id, @RequestBody ApplicationInput p) throws SQLException {
    service.update(id, p);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/applications/{id}")
  public ResponseEntity<Void> deleteApplication(@PathVariable long id) throws SQLException {
    service.deleteApplication(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/applications/{id}/status")
  public ResponseEntity<Void> status(@PathVariable long id, @RequestBody StatusInput p)
      throws SQLException {
    service.status(id, p);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/statistics")
  public Statistics statistics(
      @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to)
      throws SQLException {
    return service.statistics(from, to);
  }

  private ResponseEntity<Created> created(String type, long id) {
    return ResponseEntity.created(URI.create("/api/" + type + "/" + id)).body(new Created(id));
  }
}
