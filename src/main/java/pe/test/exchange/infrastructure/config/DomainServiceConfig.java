package pe.test.exchange.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.test.exchange.domain.service.CurrencyConversionService;
import pe.test.exchange.domain.service.ExchangeRateProvider;

import java.time.Clock;

@Configuration
public class DomainServiceConfig {

    @Bean
    public CurrencyConversionService currencyConversionService(ExchangeRateProvider rateProvider, Clock clock) {
        return new CurrencyConversionService(rateProvider, clock);
    }
}
