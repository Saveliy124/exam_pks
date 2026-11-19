package ru.mirea.examcenter.kr3.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.service.NoteService;
import java.util.*;
@RestController @RequestMapping("/api/applications/{applicationId}/notes") @SecurityRequirement(name="bearerAuth")
public class NoteController {
 private final NoteService notes;public NoteController(NoteService n){notes=n;}
 @GetMapping public List<NoteResponse> list(@PathVariable UUID applicationId){return notes.list(applicationId);}
 @PostMapping @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ResponseEntity<NoteResponse> create(@PathVariable UUID applicationId,@Valid @RequestBody NoteRequest r){return ResponseEntity.status(201).body(notes.create(applicationId,r));}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public NoteResponse update(@PathVariable UUID applicationId,@PathVariable UUID id,@Valid @RequestBody NoteRequest r){return notes.update(applicationId,id,r);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ResponseEntity<Void> delete(@PathVariable UUID applicationId,@PathVariable UUID id,@RequestParam long version){notes.delete(applicationId,id,version);return ResponseEntity.noContent().build();}
}
