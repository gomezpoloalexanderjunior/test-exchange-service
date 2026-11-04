package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;

public interface ExchangeRateProvider {

    ExchangeRate getRate(CurrencyCode source, CurrencyCode target);
}
