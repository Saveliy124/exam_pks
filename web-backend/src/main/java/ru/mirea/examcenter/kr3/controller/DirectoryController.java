package ru.mirea.examcenter.kr3.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mirea.examcenter.kr3.dto.PageResponse;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.service.DirectoryService;
import ru.mirea.examcenter.kr3.web.PageableFactory;
import java.util.*;
@RestController @RequestMapping("/api") @SecurityRequirement(name="bearerAuth")
public class DirectoryController {
 private final DirectoryService dirs;private final PageableFactory pages;
 public DirectoryController(DirectoryService d,PageableFactory p){dirs=d;pages=p;}
 @GetMapping("/disciplines") public List<DisciplineResponse> disciplines(@RequestParam(defaultValue="false") boolean activeOnly){return dirs.disciplines(activeOnly);}
 @PostMapping("/disciplines") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<DisciplineResponse> create(@Valid @RequestBody DisciplineRequest r){return ResponseEntity.status(201).body(dirs.saveDiscipline(null,r));}
 @PutMapping("/disciplines/{id}") @PreAuthorize("hasRole('ADMIN')") public DisciplineResponse update(@PathVariable UUID id,@Valid @RequestBody DisciplineRequest r){return dirs.saveDiscipline(id,r);}
 @PatchMapping("/disciplines/{id}/active") @PreAuthorize("hasRole('ADMIN')") public DisciplineResponse active(@PathVariable UUID id,@RequestParam boolean active,@RequestParam long version){return dirs.active(id,active,version);}
 @GetMapping("/applicants") public PageResponse<ApplicantResponse> applicants(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="fullName,asc") String sort){return PageResponse.from(dirs.applicants(q,pages.people(page,size,sort)));}
 @PostMapping("/applicants") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ResponseEntity<ApplicantResponse> create(@Valid @RequestBody ApplicantRequest r){return ResponseEntity.status(201).body(dirs.saveApplicant(null,r));}
 @PutMapping("/applicants/{id}") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ApplicantResponse update(@PathVariable UUID id,@Valid @RequestBody ApplicantRequest r){return dirs.saveApplicant(id,r);}
 @GetMapping("/examiners") public PageResponse<ExaminerResponse> examiners(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="fullName,asc") String sort){return PageResponse.from(dirs.examiners(q,pages.people(page,size,sort)));}
 @PostMapping("/examiners") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<ExaminerResponse> create(@Valid @RequestBody ExaminerRequest r){return ResponseEntity.status(201).body(dirs.saveExaminer(null,r));}
 @PutMapping("/examiners/{id}") @PreAuthorize("hasRole('ADMIN')") public ExaminerResponse update(@PathVariable UUID id,@Valid @RequestBody ExaminerRequest r){return dirs.saveExaminer(id,r);}
}
