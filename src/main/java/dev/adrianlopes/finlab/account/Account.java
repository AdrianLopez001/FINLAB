package dev.adrianlopes.finlab.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * The account row in the database. The balance only changes through
 * deposit() and withdraw(), which keep it valid.
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance;

    protected Account() {
        // required by JPA
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(balance) > 0) {
            throw new OperationException("Insufficient balance.");
        }
        balance = balance.subtract(amount);
    }
}
