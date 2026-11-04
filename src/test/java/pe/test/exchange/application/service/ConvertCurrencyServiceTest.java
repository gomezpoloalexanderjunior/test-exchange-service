package pe.test.exchange.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import pe.test.exchange.domain.service.CurrencyConversionService;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvertCurrencyServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");

    @Mock
    private CurrencyConversionService conversionService;

    @Mock
    private ConversionRepository repository;

    private ConvertCurrencyService service;

    @BeforeEach
    void setUp() {
        service = new ConvertCurrencyService(conversionService, repository, new ConversionMapper());
    }

    @Test
    void delegatesToTheDomainServiceSavesAndMapsTheResponse() {
        Conversion conversion = Conversion.create(new BigDecimal("100"), USD, PEN,
                new ExchangeRate(new BigDecimal("3.70")), NOW);
        when(conversionService.convert(new BigDecimal("100"), USD, PEN)).thenReturn(conversion);
        when(repository.save(conversion)).thenReturn(conversion);

        ConversionResponse response = service.convert(
                new ConversionRequest(new BigDecimal("100"), "usd", " pen "));

        verify(repository).save(conversion);
        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualByComparingTo("370.00");
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.70");
    }

    @Test
    void rejectsInvalidCurrencyBeforeCallingTheDomainService() {
        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.TEN, "US", "PEN")))
                .isInstanceOf(InvalidCurrencyException.class);

        verify(conversionService, never()).convert(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void doesNotSaveWhenTheDomainServiceRejectsTheAmount() {
        when(conversionService.convert(any(), any(), any())).thenThrow(new InvalidAmountException("monto"));

        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.ZERO, "USD", "PEN")))
                .isInstanceOf(InvalidAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void doesNotSaveWhenTheRateIsUnavailable() {
        when(conversionService.convert(any(), any(), any()))
                .thenThrow(new ExchangeRateUnavailableException("caído"));

        assertThatThrownBy(() -> service.convert(new ConversionRequest(BigDecimal.TEN, "USD", "PEN")))
                .isInstanceOf(ExchangeRateUnavailableException.class);

        verify(repository, never()).save(any());
    }
}
