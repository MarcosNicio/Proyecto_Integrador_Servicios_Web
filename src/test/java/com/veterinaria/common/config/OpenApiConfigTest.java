package com.veterinaria.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que la documentación de la API (Swagger / OpenAPI) definida en
 * {@link OpenApiConfig} se genera y se publica correctamente.
 * <p>
 * {@code @SpringBootTest} levanta la aplicación completa (incluida la conexión a la BD) y
 * {@code @AutoConfigureMockMvc} permite simular peticiones HTTP sin abrir un puerto real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    /** Cliente HTTP simulado para hacer peticiones a la aplicación dentro del test. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Pide la especificación OpenAPI en JSON ({@code /v3/api-docs}) y verifica que:
     * <ul>
     *   <li>responde 200 OK;</li>
     *   <li>incluye el título de la API configurado en {@link OpenApiConfig};</li>
     *   <li>declara el esquema de autenticación {@code bearerAuth} de tipo Bearer con formato JWT,
     *       que es el que habilita el botón "Authorize" en Swagger UI.</li>
     * </ul>
     */
    @Test
    void apiDocsIncluyeInfoYEsquemaBearer() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API - Sistema Integrado de Gestión Veterinaria"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    /**
     * Verifica que la página de Swagger UI carga (200 OK).
     * <p>
     * Se usa {@code /swagger-ui/index.html} porque {@code /swagger-ui.html} (la ruta configurada
     * en application.properties) solo redirige a esta página.
     */
    @Test
    void swaggerUiEstaDisponible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
