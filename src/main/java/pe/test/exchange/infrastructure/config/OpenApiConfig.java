package pe.test.exchange.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("API de Tipo de Cambio")
                .description("Convierte montos entre monedas usando el tipo de cambio de ExchangeRate-API y guarda cada conversión.")
                .version("0.0.1"));
    }
}
