package pe.test.exchange.application.dto;

import java.time.Instant;

public record ErrorResponse(
        String codigo,
        String mensaje,
        Instant fecha
) {
}
