package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void computesConvertedAmountAndKeepsAllData() {
        Conversion conversion = Conversion.create(
                new BigDecimal("100"), USD, PEN, rate("3.70"), NOW);

        assertThat(conversion.getAmount()).isEqualByComparingTo("100");
        assertThat(conversion.getSourceCurrency()).isEqualTo("USD");
        assertThat(conversion.getTargetCurrency()).isEqualTo("PEN");
        assertThat(conversion.getExchangeRate()).isEqualByComparingTo("3.70");
        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("370.00");
        assertThat(conversion.getOperationDate()).isEqualTo(NOW);
    }

    @Test
    void roundsConvertedAmountToTwoDecimalsHalfUp() {
        Conversion conversion = Conversion.create(
                new BigDecimal("1"), USD, PEN, rate("3.705"), NOW);

        assertThat(conversion.getConvertedAmount()).isEqualTo(new BigDecimal("3.71"));
    }

    @Test
    void roundsDownBelowHalf() {
        Conversion conversion = Conversion.create(
                new BigDecimal("1"), USD, PEN, rate("3.704"), NOW);

        assertThat(conversion.getConvertedAmount()).isEqualTo(new BigDecimal("3.70"));
    }

    @Test
    void allowsSameCurrencyConversion() {
        Conversion conversion = Conversion.create(
                new BigDecimal("50"), USD, USD, rate("1"), NOW);

        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> Conversion.create(null, USD, PEN, rate("3.70"), NOW))
                .isInstanceOf(InvalidAmountException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void rejectsNonPositiveAmount(String raw) {
        assertThatThrownBy(() -> Conversion.create(new BigDecimal(raw), USD, PEN, rate("3.70"), NOW))
                .isInstanceOf(InvalidAmountException.class);
    }

    private static ExchangeRate rate(String value) {
        return new ExchangeRate(new BigDecimal(value));
    }
}
