package ru.mirea.examcenter.kr3.dto;
import jakarta.validation.constraints.*;
import ru.mirea.examcenter.kr3.domain.*;
import java.time.*;
import java.util.*;
public final class ExamDtos {
 private ExamDtos() { }
 public record ApplicationRequest(
  @NotBlank @Size(max=100) @Pattern(regexp="[A-Za-zА-Яа-яЁё0-9._/-]+",message="Код: буквы, цифры, точка, дефис, слэш") String externalId,
  @NotNull UUID applicantId, @NotNull UUID disciplineId, @NotNull LocalDateTime scheduledAt,
  @NotNull ApplicationStatus status, @Min(0) @Max(100) Integer score,
  @NotNull @Min(0) @Max(100) Integer preparationPercent, @NotNull Boolean documentsVerified,
  @NotNull @Size(max=10) Set<@NotBlank @Size(max=50) String> tags, @PositiveOrZero Long version) { }
 public record ApplicationResponse(UUID id, String externalId, UUID applicantId, String applicantName,
  UUID disciplineId, String disciplineName, String disciplineCode, LocalDateTime scheduledAt,
  ApplicationStatus status, Integer score, int preparationPercent, boolean documentsVerified,
  RiskLevel riskLevel, boolean archived, Instant archivedAt, Set<String> tags,
  LocalDateTime createdAt, Instant updatedAt, long version) { }
 public record ApplicantRequest(@NotBlank @Size(min=2,max=120) String fullName,
  @NotBlank @Email @Size(max=254) String email, @Size(max=30) String phone,
  @NotNull ApplicantStatus status, @PositiveOrZero Long version) { }
 public record ApplicantResponse(UUID id, String fullName, String email, String phone, ApplicantStatus status, long version) { }
 public record ExaminerRequest(@NotBlank @Size(min=2,max=120) String fullName,
  @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=2,max=120) String subject,
  @NotNull ExaminerStatus status, @PositiveOrZero Long version) { }
 public record ExaminerResponse(UUID id, String fullName, String email, String subject, ExaminerStatus status, long version) { }
 public record DisciplineRequest(@NotBlank @Size(max=50) @Pattern(regexp="[A-Za-zА-Яа-яЁё0-9._-]+") String code,
  @NotBlank @Size(min=2,max=120) String name, @NotNull Boolean active, @PositiveOrZero Long version) { }
 public record DisciplineResponse(UUID id,String code,String name,boolean active,Instant createdAt,Instant updatedAt,long version) { }
 public record NoteRequest(@NotBlank @Size(max=4000) String body, @NotNull Boolean pinned,@PositiveOrZero Long version) { }
 public record NoteResponse(UUID id,UUID applicationId,UUID authorId,String authorName,String body,
  boolean pinned,Instant createdAt,Instant updatedAt,long version) { }
 public enum BulkAction { ARCHIVE, RESTORE, STATUS, ADD_TAG, REMOVE_TAG }
 public record BulkRequest(@NotEmpty @Size(max=100) List<@NotNull UUID> ids,@NotNull BulkAction action,
  @NotNull Map<UUID,@NotNull @PositiveOrZero Long> versions, ApplicationStatus status,
  @Min(0) @Max(100) Integer score, @Size(max=50) String tag) { }
 public record BulkFailure(UUID id,int status,String message) { }
 public record BulkResult(List<UUID> successful,List<BulkFailure> errors) { }
 public record NamedCount(String name,long count) { }
 public record AnalyticsResponse(long total,long active,long archived,long documentsVerified,Double averagePreparation,
  List<NamedCount> byDiscipline,List<NamedCount> byStatus,List<NamedCount> byRisk) { }
}
