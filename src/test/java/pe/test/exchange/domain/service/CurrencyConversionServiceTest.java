package pe.test.exchange.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidAmountException;
import pe.test.exchange.domain.exception.UnsupportedCurrencyException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");

    @Mock
    private ExchangeRateProvider rateProvider;

    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyConversionService(rateProvider, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void buildsTheConversionWithTheProviderRateAndTheClockDate() {
        when(rateProvider.getRate(USD, PEN)).thenReturn(new ExchangeRate(new BigDecimal("3.70")));

        Conversion conversion = service.convert(new BigDecimal("100"), USD, PEN);

        assertThat(conversion.getId()).isNull();
        assertThat(conversion.getSourceCurrency()).isEqualTo("USD");
        assertThat(conversion.getTargetCurrency()).isEqualTo("PEN");
        assertThat(conversion.getExchangeRate()).isEqualByComparingTo("3.70");
        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("370.00");
        assertThat(conversion.getOperationDate()).isEqualTo(NOW);
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        when(rateProvider.getRate(USD, PEN)).thenReturn(new ExchangeRate(new BigDecimal("0.125")));

        Conversion conversion = service.convert(new BigDecimal("1"), USD, PEN);

        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("0.13");
    }

    @Test
    void propagatesTheProviderFailure() {
        when(rateProvider.getRate(any(), any())).thenThrow(new ExchangeRateUnavailableException("caído"));

        assertThatThrownBy(() -> service.convert(BigDecimal.TEN, USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void propagatesAnUnsupportedTargetCurrency() {
        when(rateProvider.getRate(any(), any())).thenThrow(new UnsupportedCurrencyException("XXX"));

        assertThatThrownBy(() -> service.convert(BigDecimal.TEN, USD, CurrencyCode.of("XXX")))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void rejectsANonPositiveAmount() {
        when(rateProvider.getRate(any(), any())).thenReturn(new ExchangeRate(new BigDecimal("3.70")));

        assertThatThrownBy(() -> service.convert(BigDecimal.ZERO, USD, PEN))
                .isInstanceOf(InvalidAmountException.class);
    }
}
