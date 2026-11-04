package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

public class CurrencyConversionService {

    private final ExchangeRateProvider rateProvider;
    private final Clock clock;

    public CurrencyConversionService(ExchangeRateProvider rateProvider, Clock clock) {
        this.rateProvider = rateProvider;
        this.clock = clock;
    }

    public Conversion convert(BigDecimal amount, CurrencyCode source, CurrencyCode target) {
        ExchangeRate rate = rateProvider.getRate(source, target);
        return Conversion.create(amount, source, target, rate, Instant.now(clock));
    }
}
