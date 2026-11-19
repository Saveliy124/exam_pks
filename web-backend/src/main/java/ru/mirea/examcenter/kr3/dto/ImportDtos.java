package ru.mirea.examcenter.kr3.dto;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.ApplicationRequest;
public final class ImportDtos {
 private ImportDtos() { }
 public enum ImportMode { CREATE_ONLY, UPSERT }
 public record RowIssue(long row,String message) { }
 public record ImportRow(long row,UUID targetId,ApplicationRequest request) { }
 public record ImportPreview(UUID previewId,int validRows,int errorRows,List<RowIssue> errors,Instant expiresAt,String fileHash) { }
 public record ApplyRequest(@NotNull UUID previewId) { }
 public record ImportResult(int created,int updated,int failed,List<RowIssue> errors) { }
}
