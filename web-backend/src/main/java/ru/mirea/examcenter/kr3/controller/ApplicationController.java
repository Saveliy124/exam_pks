package ru.mirea.examcenter.kr3.controller;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.examcenter.kr3.dto.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.dto.ImportDtos.*;
import ru.mirea.examcenter.kr3.filter.ApplicationFilter;
import ru.mirea.examcenter.kr3.service.*;
import ru.mirea.examcenter.kr3.web.PageableFactory;
import java.io.IOException;
import java.util.UUID;
@RestController @RequestMapping("/api/applications") @SecurityRequirement(name="bearerAuth")
public class ApplicationController {
 private final ApplicationService apps;private final CsvApplicationService csv;private final PageableFactory pages;
 public ApplicationController(ApplicationService a,CsvApplicationService c,PageableFactory p){apps=a;csv=c;pages=p;}
 @GetMapping @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
 public PageResponse<ApplicationResponse> search(@Valid @ModelAttribute ApplicationFilter f,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="scheduledAt,asc") String sort){return PageResponse.from(apps.search(f,pages.applications(page,size,sort)));}
 @GetMapping("/{id}") @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')") public ApplicationResponse get(@PathVariable UUID id){return apps.get(id);}
 @PostMapping @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest r){return ResponseEntity.status(201).body(apps.create(r));}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ApplicationResponse update(@PathVariable UUID id,@Valid @RequestBody ApplicationRequest r){return apps.update(id,r);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ApplicationResponse archive(@PathVariable UUID id,@RequestParam long version){return apps.archive(id,version);}
 @PostMapping("/{id}/restore") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ApplicationResponse restore(@PathVariable UUID id,@RequestParam long version){return apps.restore(id,version);}
 @DeleteMapping("/{id}/permanent") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> delete(@PathVariable UUID id,@RequestParam long version,@RequestHeader(name="X-Confirm-Delete",required=false) String confirmation){apps.permanentDelete(id,version,confirmation);return ResponseEntity.noContent().build();}
 @PostMapping("/bulk") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public BulkResult bulk(@Valid @RequestBody BulkRequest r){return apps.bulk(r);}
 @PostMapping(value="/import/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ImportPreview preview(@RequestPart MultipartFile file,@RequestParam(defaultValue="CREATE_ONLY") ImportMode mode){return csv.preview(file,mode);}
 @PostMapping("/import/apply") @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')") public ImportResult apply(@Valid @RequestBody ApplyRequest r){return csv.apply(r.previewId());}
 @GetMapping(value="/export",produces="text/csv;charset=UTF-8") @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')") public void export(@Valid @ModelAttribute ApplicationFilter f,HttpServletResponse res)throws IOException{res.setContentType("text/csv;charset=UTF-8");res.setHeader("Content-Disposition","attachment; filename=exam-applications.csv");csv.export(f,res.getOutputStream());}
}
