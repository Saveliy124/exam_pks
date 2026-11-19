package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote,UUID>, JpaSpecificationExecutor<ApplicationNote> {
 List<ApplicationNote> findByApplicationPublicIdOrderByPinnedDescCreatedAtDesc(UUID applicationId);
}
