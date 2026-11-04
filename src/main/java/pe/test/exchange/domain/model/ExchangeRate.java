package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;

import java.math.BigDecimal;

public record ExchangeRate(BigDecimal value) {

    public ExchangeRate {
        if (value == null || value.signum() <= 0) {
            throw new ExchangeRateUnavailableException(
                    "El servicio de tipo de cambio devolvió una tasa inválida");
        }
    }
}
