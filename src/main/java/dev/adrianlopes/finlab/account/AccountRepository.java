package dev.adrianlopes.finlab.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Database access for the account. Spring Data writes the implementation.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Reads the account and locks its row until the transaction ends
     * (SELECT ... FOR UPDATE). Any other request that wants the same row waits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
