package dev.adrianlopes.finlab.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests: Money has no Spring and no state, so it's tested directly.
 */
class MoneyTest {

    @Test
    void parsesValidAmounts() {
        assertThat(Money.parse("10.50")).isEqualByComparingTo("10.50");
        assertThat(Money.parse(" 7 ")).isEqualByComparingTo("7");
        assertThat(Money.parse("10.5000")).isEqualByComparingTo("10.50");
        assertThat(Money.parse("1000000.00")).isEqualByComparingTo("1000000");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NaN        | Enter a valid amount",
            "Infinity   | Enter a valid amount",
            "abc        | Enter a valid amount",
            "10,50      | Enter a valid amount",
            "0          | greater than zero",
            "-5         | greater than zero",
            "10.005     | 2 decimal places",
            "1e308      | at most",
            "1000000.01 | at most"
    })
    void rejectsInvalidAmountsWithTheReason(String text, String reason) {
        assertThatThrownBy(() -> Money.parse(text))
                .isInstanceOf(OperationException.class)
                .hasMessageContaining(reason);
    }

    @Test
    void rejectsMissingAmount() {
        assertThatThrownBy(() -> Money.parse(null)).isInstanceOf(OperationException.class);
    }

    @Test
    void formatsAsBrazilianCurrency() {
        assertThat(Money.format(new BigDecimal("1234.5")).replace(' ', ' ')).isEqualTo("R$ 1.234,50");
    }
}
