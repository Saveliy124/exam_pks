package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ExaminerRepository extends JpaRepository<Examiner,Long>, JpaSpecificationExecutor<Examiner> {
 Optional<Examiner> findByPublicId(UUID id); Optional<Examiner> findByEmailIgnoreCase(String email);
}
