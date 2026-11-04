package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateTest {

    @Test
    void keepsTheValueAsProvided() {
        assertThat(new ExchangeRate(new BigDecimal("3.7041")).value()).isEqualByComparingTo("3.7041");
    }

    @Test
    void rejectsNullRate() {
        assertThatThrownBy(() -> new ExchangeRate(null))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.0001"})
    void rejectsNonPositiveRates(String raw) {
        assertThatThrownBy(() -> new ExchangeRate(new BigDecimal(raw)))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
