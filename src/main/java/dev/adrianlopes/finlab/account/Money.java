package dev.adrianlopes.finlab.account;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Everything about turning text into money and money into text.
 */
public final class Money {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    private Money() {
        // utility class: only static methods
    }

    /** Converts the typed text into a valid amount, or throws with the reason. */
    public static BigDecimal parse(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim());
        } catch (NullPointerException | NumberFormatException e) {
            throw new OperationException("Enter a valid amount, like 10.50.");
        }

        if (value.signum() <= 0) {
            throw new OperationException("Amount must be greater than zero.");
        } else if (value.stripTrailingZeros().scale() > 2) {
            throw new OperationException("Amount must have at most 2 decimal places.");
        } else if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new OperationException("Amount must be at most " + format(MAX_AMOUNT) + ".");
        }
        return value;
    }

    public static String format(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(value);
    }
}
