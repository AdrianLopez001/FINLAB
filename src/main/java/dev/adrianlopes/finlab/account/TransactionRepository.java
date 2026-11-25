package dev.adrianlopes.finlab.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database access for the statement. Spring Data builds each query from the method name.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByRequestId(String requestId);

    List<Transaction> findTop10ByOrderByIdDesc();

    List<Transaction> findAllByOrderByIdDesc();
}
