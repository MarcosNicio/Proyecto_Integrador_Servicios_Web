package com.veterinaria.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba las reglas de {@link SecurityConfig}: qué rutas son públicas, cuáles exigen
 * autenticación y que BCrypt cifre y reconozca contraseñas.
 * <p>
 * Usa un controlador de prueba con una ruta pública ({@code /api/auth/...}) y una privada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTest.RutasDePruebaController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Controlador de prueba: solo existe durante los tests. */
    @TestConfiguration
    @RestController
    static class RutasDePruebaController {

        @GetMapping("/api/auth/test-publica")
        String publica() {
            return "publica";
        }

        @GetMapping("/api/test/privada")
        String privada() {
            return "privada";
        }
    }

    @Test
    void rutaDeAuthEsPublicaSinToken() throws Exception {
        mockMvc.perform(get("/api/auth/test-publica"))
                .andExpect(status().isOk())
                .andExpect(content().string("publica"));
    }

    @Test
    void rutaPrivadaSinTokenDevuelve401ConFormatoDeLaApi() throws Exception {
        mockMvc.perform(get("/api/test/privada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_SIN_TOKEN))
                .andExpect(jsonPath("$.path").value("/api/test/privada"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void endpointsDeModulosRequierenAutenticacion() throws Exception {
        // Las rutas reales de los módulos (ej. clientes) también quedan protegidas.
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerEsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void bcryptCifraYReconoceLaContrasena() {
        String hash = passwordEncoder.encode("MiClave123");

        assertThat(hash).isNotEqualTo("MiClave123").startsWith("$2a$");
        assertThat(passwordEncoder.matches("MiClave123", hash)).isTrue();
        assertThat(passwordEncoder.matches("OtraClave", hash)).isFalse();
    }

    @Test
    void bcryptGeneraHashDistintoParaLaMismaContrasena() {
        // Por la "sal" aleatoria, dos usuarios con la misma contraseña tienen hashes distintos.
        String hash1 = passwordEncoder.encode("MiClave123");
        String hash2 = passwordEncoder.encode("MiClave123");

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches("MiClave123", hash1)).isTrue();
        assertThat(passwordEncoder.matches("MiClave123", hash2)).isTrue();
    }
}
