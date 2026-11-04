package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.UnsupportedCurrencyException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;
import pe.test.exchange.domain.service.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;

@Slf4j
@Component
public class FeignExchangeRateProvider implements ExchangeRateProvider {

    private static final String SUCCESS = "success";

    private final ExchangeRateApiClient client;
    private final String apiKey;

    public FeignExchangeRateProvider(ExchangeRateApiClient client,
                                     @Value("${exchange-rate.api.key}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public ExchangeRate getRate(CurrencyCode source, CurrencyCode target) {
        ExchangeRateApiResponse response = fetchRates(source);
        requireSuccess(response);
        return new ExchangeRate(findRate(response, target));
    }

    private ExchangeRateApiResponse fetchRates(CurrencyCode source) {
        try {
            return client.getLatestRates(apiKey, source.value());
        } catch (FeignException.NotFound e) {
            throw new UnsupportedCurrencyException(
                    "La moneda de origen " + source.value() + " no es soportada");
        } catch (FeignException e) {
            log.warn("Exchange rate API call failed with status {}", e.status());
            throw new ExchangeRateUnavailableException(
                    "El servicio de tipo de cambio no está disponible");
        }
    }

    private void requireSuccess(ExchangeRateApiResponse response) {
        if (response == null || !SUCCESS.equals(response.result()) || response.conversionRates() == null) {
            throw new ExchangeRateUnavailableException(
                    "El servicio de tipo de cambio devolvió una respuesta inválida");
        }
    }

    private BigDecimal findRate(ExchangeRateApiResponse response, CurrencyCode target) {
        BigDecimal rate = response.conversionRates().get(target.value());
        if (rate == null) {
            throw new UnsupportedCurrencyException(
                    "La moneda de destino " + target.value() + " no es soportada");
        }
        return rate;
    }
}
