package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface CsvImportPreviewRepository extends JpaRepository<CsvImportPreview,UUID>, JpaSpecificationExecutor<CsvImportPreview> {
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select p from CsvImportPreview p where p.id=:id") Optional<CsvImportPreview> lockById(UUID id);
}
