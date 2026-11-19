package ru.mirea.examcenter.kr3.repository;
import ru.mirea.examcenter.kr3.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ApplicantRepository extends JpaRepository<Applicant,Long>, JpaSpecificationExecutor<Applicant> {
 Optional<Applicant> findByPublicId(UUID id);
 Optional<Applicant> findByEmailIgnoreCase(String email);
}
