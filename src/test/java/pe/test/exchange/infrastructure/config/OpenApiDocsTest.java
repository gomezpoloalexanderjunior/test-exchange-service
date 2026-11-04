package pe.test.exchange.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTheConversionEndpointInTheOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/conversion'].post").exists())
                .andExpect(jsonPath("$.paths['/api/conversion'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/conversion'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/conversion'].post.responses['502']").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }
}
