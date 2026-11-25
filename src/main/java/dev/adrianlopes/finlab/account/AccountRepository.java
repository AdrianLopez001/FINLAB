package dev.adrianlopes.finlab.account;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Where the data lives. For now it's in memory; the rest of the app
 * only talks to these methods, so the storage can change later.
 * Not thread-safe on its own: AccountService serializes the calls.
 */
@Repository
public class AccountRepository {

    private BigDecimal balance = BigDecimal.ZERO;
    private final Set<String> processedRequests = new HashSet<>();

    public BigDecimal getBalance() {
        return balance;
    }

    public void saveBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }

    public boolean isProcessed(String requestId) {
        return processedRequests.contains(requestId);
    }

    public void markProcessed(String requestId) {
        processedRequests.add(requestId);
    }
}
