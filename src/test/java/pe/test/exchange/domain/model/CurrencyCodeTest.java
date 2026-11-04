package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.InvalidCurrencyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyCodeTest {

    @Test
    void normalizesToUpperCaseAndTrims() {
        assertThat(CurrencyCode.of("  usd ").value()).isEqualTo("USD");
    }

    @Test
    void equalCodesAreEqualValueObjects() {
        assertThat(CurrencyCode.of("pen")).isEqualTo(CurrencyCode.of("PEN"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "US", "USDD", "U5D", "12 ", "US$"})
    void rejectsMalformedCodes(String raw) {
        assertThatThrownBy(() -> CurrencyCode.of(raw))
                .isInstanceOf(InvalidCurrencyException.class);
    }
}
