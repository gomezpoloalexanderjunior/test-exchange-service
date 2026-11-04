package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.UnsupportedCurrencyException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignExchangeRateProviderTest {

    private static final String API_KEY = "test-key";
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");

    @Mock
    private ExchangeRateApiClient client;

    private FeignExchangeRateProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FeignExchangeRateProvider(client, API_KEY);
    }

    @Test
    void returnsTheRateOfTheTargetCurrency() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(successResponse());

        ExchangeRate rate = provider.getRate(USD, PEN);

        assertThat(rate.value()).isEqualByComparingTo("3.7041");
    }

    @Test
    void failsWhenTheTargetCurrencyIsNotSupported() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(successResponse());

        assertThatThrownBy(() -> provider.getRate(USD, CurrencyCode.of("XXX")))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void failsWhenTheBaseCurrencyIsUnknownToTheApi() {
        when(client.getLatestRates(API_KEY, "ZZZ")).thenThrow(feignError(404));

        assertThatThrownBy(() -> provider.getRate(CurrencyCode.of("ZZZ"), PEN))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void translatesApiFailuresIntoUnavailable() {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(feignError(503));

        assertThatThrownBy(() -> provider.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void failsWhenTheResultIsNotSuccess() {
        when(client.getLatestRates(API_KEY, "USD"))
                .thenReturn(new ExchangeRateApiResponse("error", "USD", Map.of()));

        assertThatThrownBy(() -> provider.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void failsWhenTheResponseIsEmpty() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(null);

        assertThatThrownBy(() -> provider.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void failsWhenTheRatesAreMissing() {
        when(client.getLatestRates(API_KEY, "USD"))
                .thenReturn(new ExchangeRateApiResponse("success", "USD", null));

        assertThatThrownBy(() -> provider.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private static ExchangeRateApiResponse successResponse() {
        return new ExchangeRateApiResponse("success", "USD",
                Map.of("USD", BigDecimal.ONE, "PEN", new BigDecimal("3.7041")));
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "https://api.test/v6/key/latest/USD",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("getLatestRates",
                feign.Response.builder().status(status).reason("error").request(request)
                        .headers(Map.of()).build());
    }
}
