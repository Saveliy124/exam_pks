package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ExamDisciplineRepository extends JpaRepository<ExamDiscipline,UUID>, JpaSpecificationExecutor<ExamDiscipline> {
 Optional<ExamDiscipline> findByCodeIgnoreCase(String code); Optional<ExamDiscipline> findByNameIgnoreCase(String name);
}
