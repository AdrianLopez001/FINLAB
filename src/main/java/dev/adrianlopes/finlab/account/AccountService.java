package dev.adrianlopes.finlab.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business rules: validates every operation, changes the balance and records it
 * in the statement. Each operation runs in one database transaction: it all
 * happens, or none of it does.
 */
@Service
public class AccountService {

    static final long ACCOUNT_ID = 1L;

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public AccountService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance() {
        return accounts.findById(ACCOUNT_ID).orElseThrow().getBalance();
    }

    @Transactional(readOnly = true)
    public List<Transaction> recentTransactions() {
        return transactions.findTop10ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Transaction> allTransactions() {
        return transactions.findAllByOrderByIdDesc();
    }

    @Transactional
    public BigDecimal deposit(String requestId, String amountText) {
        Account account = lockAccountFor(requestId);
        BigDecimal amount = Money.parse(amountText);
        account.deposit(amount);
        transactions.save(new Transaction(requestId, TransactionType.DEPOSIT, amount, account.getBalance()));
        return amount;
    }

    @Transactional
    public BigDecimal withdraw(String requestId, String amountText) {
        Account account = lockAccountFor(requestId);
        BigDecimal amount = Money.parse(amountText);
        account.withdraw(amount);
        transactions.save(new Transaction(requestId, TransactionType.WITHDRAWAL, amount, account.getBalance()));
        return amount;
    }

    /**
     * Checks the request id, locks the account row and makes sure this form
     * wasn't applied before. While the lock is held, no other operation can
     * read or change the balance.
     */
    private Account lockAccountFor(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new OperationException("Invalid request. Please try again.");
        }
        Account account = accounts.findByIdForUpdate(ACCOUNT_ID).orElseThrow();
        if (transactions.existsByRequestId(requestId)) {
            throw new DuplicateOperationException();
        }
        return account;
    }
}
