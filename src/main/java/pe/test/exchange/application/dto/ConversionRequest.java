package pe.test.exchange.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConversionRequest(
        @Schema(example = "100")
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto,
        @Schema(example = "USD")
        @NotBlank(message = "La moneda de origen es obligatoria")
        String monedaOrigen,
        @Schema(example = "PEN")
        @NotBlank(message = "La moneda de destino es obligatoria")
        String monedaDestino
) {
}
