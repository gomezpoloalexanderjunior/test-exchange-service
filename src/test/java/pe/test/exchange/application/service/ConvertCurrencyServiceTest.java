package pe.test.exchange.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidAmountException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ExchangeRateProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertCurrencyServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private ExchangeRateProvider rateProvider;

    @Mock
    private ConversionRepository repository;

    private ConvertCurrencyService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ConvertCurrencyService(rateProvider, repository, fixedClock, new ConversionMapper());
    }

    @Test
    void convertsPersistsAndReturnsTheResponse() {
        when(rateProvider.getRate(CurrencyCode.of("USD"), CurrencyCode.of("PEN")))
                .thenReturn(new ExchangeRate(new BigDecimal("3.70")));
        when(repository.save(any(Conversion.class))).thenAnswer(call -> call.getArgument(0));

        ConversionResponse response = service.convert(
                new ConversionRequest(new BigDecimal("100"), "usd", " pen "));

        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualByComparingTo("370.00");
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.70");
    }

    @Test
    void persistsTheConversionWithTheClockDate() {
        when(rateProvider.getRate(any(), any())).thenReturn(new ExchangeRate(new BigDecimal("3.70")));
        when(repository.save(any(Conversion.class))).thenAnswer(call -> call.getArgument(0));

        service.convert(new ConversionRequest(new BigDecimal("100"), "USD", "PEN"));

        ArgumentCaptor<Conversion> saved = ArgumentCaptor.forClass(Conversion.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOperationDate()).isEqualTo(NOW);
        assertThat(saved.getValue().getConvertedAmount()).isEqualByComparingTo("370.00");
    }

    @Test
    void rejectsInvalidCurrencyBeforeCallingTheProvider() {
        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.TEN, "US", "PEN")))
                .isInstanceOf(InvalidCurrencyException.class);

        verify(rateProvider, never()).getRate(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotPersistWhenTheAmountIsInvalid() {
        when(rateProvider.getRate(any(), any())).thenReturn(new ExchangeRate(new BigDecimal("3.70")));

        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.ZERO, "USD", "PEN")))
                .isInstanceOf(InvalidAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void doesNotPersistWhenTheProviderFails() {
        when(rateProvider.getRate(any(), any()))
                .thenThrow(new ExchangeRateUnavailableException("caído"));

        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.TEN, "USD", "PEN")))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(repository, never()).save(any());
    }
}
