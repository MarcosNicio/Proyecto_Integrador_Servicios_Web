package com.veterinaria.common.security;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba {@link JwtAuthenticationFilter} con tokens reales y usuarios guardados en la base.
 * <p>
 * {@code @Transactional} deshace al final de cada prueba los usuarios que se crean, así que la
 * base queda como estaba. MockMvc corre en el mismo hilo que la prueba, por eso el filtro sí ve
 * esos usuarios aunque no se hayan confirmado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(JwtAuthenticationFilterTest.RutasDePruebaController.class)
class JwtAuthenticationFilterTest {

    private static final String ADMIN = "admin.prueba@veterinaria.com";
    private static final String CLIENTE = "cliente.prueba@veterinaria.com";
    private static final String INACTIVO = "inactivo.prueba@veterinaria.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.jwt.secret}")
    private String secret;

    /** Controlador de prueba: solo existe durante los tests. */
    @TestConfiguration
    @RestController
    @RequestMapping("/api/test/jwt")
    static class RutasDePruebaController {

        /** Devuelve quién hizo la petición, según el filtro. */
        @GetMapping("/perfil")
        String perfil(Authentication authentication) {
            return authentication.getName() + " " + authentication.getAuthorities();
        }

        @GetMapping("/solo-admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok admin";
        }
    }

    @BeforeEach
    void crearUsuarios() {
        guardarUsuario(ADMIN, Rol.ADMIN, true);
        guardarUsuario(CLIENTE, Rol.CLIENTE, true);
        guardarUsuario(INACTIVO, Rol.VETERINARIO, false);
    }

    @Test
    void tokenValidoIdentificaAlUsuarioConSuRol() throws Exception {
        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(ADMIN + " [ROLE_ADMIN]"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/test/jwt/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_SIN_TOKEN));
    }

    @Test
    void headerSinPrefijoBearerSeIgnora() throws Exception {
        String token = jwtUtil.generarToken(ADMIN, "ADMIN");

        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_SIN_TOKEN));
    }

    @Test
    void tokenMalFormadoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_TOKEN_INVALIDO));
    }

    @Test
    void tokenFirmadoConOtraClaveDevuelve401() throws Exception {
        // Simula un token falsificado: alguien que no conoce la clave secreta del servidor.
        JwtUtil otraClave = new JwtUtil("otra-clave-secreta-distinta-de-al-menos-32-caracteres", 3_600_000);
        String falsificado = otraClave.generarToken(ADMIN, "ADMIN");

        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + falsificado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_TOKEN_INVALIDO));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        // Mismo secreto que el servidor, pero con una duración negativa: nace ya vencido.
        JwtUtil vencido = new JwtUtil(secret, -60_000);
        String token = vencido.generarToken(ADMIN, "ADMIN");

        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_TOKEN_INVALIDO));
    }

    @Test
    void usuarioDesactivadoDevuelve401AunqueSuTokenSeaValido() throws Exception {
        mockMvc.perform(get("/api/test/jwt/perfil").header(HttpHeaders.AUTHORIZATION, bearer(INACTIVO, "VETERINARIO")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_USUARIO_INACTIVO));
    }

    @Test
    void usuarioQueYaNoExisteDevuelve401() throws Exception {
        mockMvc.perform(get("/api/test/jwt/perfil")
                        .header(HttpHeaders.AUTHORIZATION, bearer("no.existe@veterinaria.com", "ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_USUARIO_INACTIVO));
    }

    @Test
    void rolConPermisoEntra() throws Exception {
        mockMvc.perform(get("/api/test/jwt/solo-admin").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok admin"));
    }

    @Test
    void rolSinPermisoDevuelve403ConFormatoDeLaApi() throws Exception {
        mockMvc.perform(get("/api/test/jwt/solo-admin").header(HttpHeaders.AUTHORIZATION, bearer(CLIENTE, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_SIN_PERMISO))
                .andExpect(jsonPath("$.path").value("/api/test/jwt/solo-admin"));
    }

    @Test
    void elRolSeTomaDeLaBaseYNoDelToken() throws Exception {
        // El token dice ADMIN, pero en la base el usuario es CLIENTE: manda la base.
        mockMvc.perform(get("/api/test/jwt/solo-admin").header(HttpHeaders.AUTHORIZATION, bearer(CLIENTE, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaPublicaFuncionaAunqueElTokenSeaInvalido() throws Exception {
        // Un token viejo guardado en el navegador no debe impedir entrar al login.
        mockMvc.perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, "Bearer token-viejo-invalido"))
                .andExpect(status().isOk());
    }

    @Test
    void accessDeniedHandlerRespondeConFormatoDeLaApi() throws Exception {
        // Cubre las futuras reglas de rol por ruta en SecurityConfig.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/personal");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("sin permiso"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"status\":403")
                .contains(SecurityErrorWriter.MENSAJE_SIN_PERMISO)
                .contains("\"path\":\"/api/personal\"");
    }

    private String bearer(String correo, String rol) {
        return "Bearer " + jwtUtil.generarToken(correo, rol);
    }

    private void guardarUsuario(String correo, Rol rol, boolean activo) {
        usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(rol)
                .activo(activo)
                .build());
    }
}
