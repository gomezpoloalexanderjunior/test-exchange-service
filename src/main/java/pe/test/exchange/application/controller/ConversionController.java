package pe.test.exchange.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.dto.ErrorResponse;
import pe.test.exchange.application.service.ConvertCurrencyService;

@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
@Tag(name = "Conversión", description = "Conversión de montos entre monedas")
public class ConversionController {

    private final ConvertCurrencyService service;

    @PostMapping
    @Operation(summary = "Convierte un monto de una moneda a otra con el tipo de cambio actual")
    @ApiResponse(responseCode = "200", description = "Conversión realizada")
    @ApiResponse(responseCode = "400",
            description = "Solicitud inválida, cuerpo mal formado o moneda inválida/no soportada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "405", description = "Método HTTP no permitido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type distinto de application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "El servicio externo de tipo de cambio no está disponible",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ConversionResponse convert(@Valid @RequestBody ConversionRequest request) {
        return service.convert(request);
    }
}
