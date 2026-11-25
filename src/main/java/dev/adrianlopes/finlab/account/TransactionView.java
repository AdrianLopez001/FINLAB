package dev.adrianlopes.finlab.account;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A transaction already formatted for the page (dates and money as text).
 */
public record TransactionView(String dateTime, String type, String amount, String balanceAfter) {

    private static final ZoneId BRAZIL_TIME = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static TransactionView from(Transaction transaction) {
        boolean deposit = transaction.getType() == TransactionType.DEPOSIT;
        return new TransactionView(
                transaction.getCreatedAt().atZoneSameInstant(BRAZIL_TIME).format(DATE_TIME),
                deposit ? "Deposit" : "Withdrawal",
                (deposit ? "+ " : "- ") + Money.format(transaction.getAmount()),
                Money.format(transaction.getBalanceAfter())
        );
    }
}
