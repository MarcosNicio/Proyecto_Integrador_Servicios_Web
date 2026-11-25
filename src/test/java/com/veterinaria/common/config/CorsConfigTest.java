package com.veterinaria.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simula las peticiones que hace el navegador desde el frontend en React
 * para comprobar que las reglas de {@link CorsConfig} se aplican.
 * <p>
 * {@code @WithMockUser} simula un usuario autenticado: la ruta de prueba es privada y aquí
 * solo interesa comprobar CORS, no la seguridad (eso lo cubre {@code SecurityConfigTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import(CorsConfigTest.PingController.class)
class CorsConfigTest {

    private static final String REACT_VITE = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    /** Endpoint de prueba bajo /api, solo existe durante los tests. */
    @TestConfiguration
    @RestController
    static class PingController {
        @GetMapping("/api/test/ping")
        String ping() {
            return "pong";
        }
    }

    @Test
    void preflightDesdeReactEsPermitido() throws Exception {
        mockMvc.perform(options("/api/test/ping")
                        .header(HttpHeaders.ORIGIN, REACT_VITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REACT_VITE))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void peticionRealDesdeReactIncluyeHeaderCors() throws Exception {
        mockMvc.perform(get("/api/test/ping").header(HttpHeaders.ORIGIN, REACT_VITE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, REACT_VITE));
    }

    @Test
    void origenNoPermitidoEsRechazado() throws Exception {
        mockMvc.perform(options("/api/test/ping")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
