package dev.adrianlopes.finlab.account;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Business rules: validates every operation and changes the balance.
 * Methods are synchronized so only one operation runs at a time.
 */
@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public synchronized BigDecimal getBalance() {
        return repository.getBalance();
    }

    public synchronized BigDecimal deposit(String requestId, String amountText) {
        BigDecimal amount = checkOperation(requestId, amountText);
        repository.saveBalance(repository.getBalance().add(amount));
        repository.markProcessed(requestId);
        return amount;
    }

    public synchronized BigDecimal withdraw(String requestId, String amountText) {
        BigDecimal amount = checkOperation(requestId, amountText);
        BigDecimal balance = repository.getBalance();
        if (amount.compareTo(balance) > 0) {
            throw new OperationException("Insufficient balance.");
        }
        repository.saveBalance(balance.subtract(amount));
        repository.markProcessed(requestId);
        return amount;
    }

    /** Checks that the request is new and the amount is valid; returns the amount. */
    private BigDecimal checkOperation(String requestId, String amountText) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new OperationException("Invalid request. Please try again.");
        }
        if (repository.isProcessed(requestId)) {
            throw new DuplicateOperationException();
        }
        return Money.parse(amountText);
    }
}
