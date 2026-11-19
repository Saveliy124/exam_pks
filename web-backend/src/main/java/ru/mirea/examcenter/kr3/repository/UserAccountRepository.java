package ru.mirea.examcenter.kr3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.examcenter.kr3.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for user accounts. */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserAccount u where u.id=:id")
    Optional<UserAccount> lockById(UUID id);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
