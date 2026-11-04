package pe.test.exchange.application.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConvertCurrencyService;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.exception.UnsupportedCurrencyException;
import pe.test.exchange.infrastructure.config.ClockConfig;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
@Import(ClockConfig.class)
class ConversionControllerTest {

    private static final String URL = "/api/conversion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvertCurrencyService service;

    @Test
    void returnsTheFlatContractOnSuccess() throws Exception {
        when(service.convert(any(ConversionRequest.class))).thenReturn(new ConversionResponse(
                new BigDecimal("100"), new BigDecimal("370.00"), "USD", "PEN", new BigDecimal("3.70")));

        perform("""
                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100))
                .andExpect(jsonPath("$.montoConvertido").value(370.00))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.70));
    }

    @Test
    void rejectsNegativeAmountWithTheErrorStructure() throws Exception {
        perform("""
                {"monto": -5, "monedaOrigen": "USD", "monedaDestino": "PEN"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingAmount() throws Exception {
        perform("""
                {"monedaOrigen": "USD", "monedaDestino": "PEN"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void rejectsBlankCurrency() throws Exception {
        perform("""
                {"monto": 10, "monedaOrigen": " ", "monedaDestino": "PEN"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void rejectsMalformedBody() throws Exception {
        perform("{ esto no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CUERPO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void mapsInvalidCurrencyTo400() throws Exception {
        when(service.convert(any(ConversionRequest.class)))
                .thenThrow(new InvalidCurrencyException("Moneda inválida"));

        perform(validBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MONEDA_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("Moneda inválida"));
    }

    @Test
    void mapsUnsupportedCurrencyTo400() throws Exception {
        when(service.convert(any(ConversionRequest.class)))
                .thenThrow(new UnsupportedCurrencyException("Moneda no soportada"));

        perform(validBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("MONEDA_NO_SOPORTADA"));
    }

    @Test
    void mapsUnavailableProviderTo502() throws Exception {
        when(service.convert(any(ConversionRequest.class)))
                .thenThrow(new ExchangeRateUnavailableException("Servicio caído"));

        perform(validBody())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_EXTERNO_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value("Servicio caído"))
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void hidesUnexpectedErrorsBehindAGenericMessage() throws Exception {
        when(service.convert(any(ConversionRequest.class)))
                .thenThrow(new IllegalStateException("detalle interno secreto"));

        perform(validBody())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado"));
    }

    @Test
    void mapsWrongHttpMethodTo405() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void mapsUnsupportedContentTypeTo415() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("TIPO_CONTENIDO_NO_SOPORTADO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void mapsUnknownUrlTo404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    private ResultActions perform(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String validBody() {
        return """
                {"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}""";
    }
}
