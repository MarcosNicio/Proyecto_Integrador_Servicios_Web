package com.veterinaria.auth.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.auth.service.AuthService;
import com.veterinaria.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba {@code POST /api/auth/login} de punta a punta: controlador, servicio, BCrypt,
 * emisión del token y su uso en una ruta privada.
 * <p>
 * Crea sus propios usuarios con contraseñas conocidas (no depende del admin del script) y
 * {@code @Transactional} los deshace al terminar cada prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    private static final String URL_LOGIN = "/api/auth/login";
    private static final String VETERINARIO = "vet.login@veterinaria.com";
    private static final String INACTIVO = "inactivo.login@veterinaria.com";
    private static final String PASSWORD = "Clave-Prueba-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void crearUsuarios() {
        guardarUsuario(VETERINARIO, Rol.VETERINARIO, true);
        guardarUsuario(INACTIVO, Rol.CAJERO, false);
    }

    @Test
    void loginCorrectoDevuelveTokenYDatosDelUsuario() throws Exception {
        MvcResult resultado = mockMvc.perform(login(VETERINARIO, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnMs").value(jwtUtil.getExpirationMs()))
                .andExpect(jsonPath("$.correo").value(VETERINARIO))
                .andExpect(jsonPath("$.rol").value("VETERINARIO"))
                .andReturn();

        String token = extraerToken(resultado);
        assertThat(jwtUtil.esTokenValido(token)).isTrue();
        assertThat(jwtUtil.obtenerCorreo(token)).isEqualTo(VETERINARIO);
        assertThat(jwtUtil.obtenerRol(token)).isEqualTo("VETERINARIO");
    }

    @Test
    void elTokenDelLoginDaAccesoAUnaRutaPrivada() throws Exception {
        // Ciclo completo: sin token se rechaza, con el token del login entra.
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());

        String token = extraerToken(mockMvc.perform(login(VETERINARIO, PASSWORD)).andReturn());

        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void correoConMayusculasYEspaciosTambienEntra() throws Exception {
        mockMvc.perform(login("  Vet.Login@Veterinaria.com ", PASSWORD))                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(VETERINARIO));
    }

    @Test
    void passwordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(login(VETERINARIO, "otra-clave"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(AuthService.MENSAJE_CREDENCIALES_INCORRECTAS))
                .andExpect(jsonPath("$.path").value(URL_LOGIN));
    }

    @Test
    void correoInexistenteDevuelveElMismo401() throws Exception {
        // Mismo mensaje que con contraseña incorrecta: no revela qué correos existen.
        mockMvc.perform(login("no.existe@veterinaria.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthService.MENSAJE_CREDENCIALES_INCORRECTAS));
    }

    @Test
    void cuentaDesactivadaConPasswordCorrectaDevuelve403() throws Exception {
        mockMvc.perform(login(INACTIVO, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AuthService.MENSAJE_CUENTA_DESACTIVADA));
    }

    @Test
    void cuentaDesactivadaConPasswordIncorrectaNoRevelaQueEstaDesactivada() throws Exception {
        mockMvc.perform(login(INACTIVO, "otra-clave"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthService.MENSAJE_CREDENCIALES_INCORRECTAS));
    }

    @Test
    void camposVaciosDevuelven400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post(URL_LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"\", \"password\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").exists())
                .andExpect(jsonPath("$.errors.password").value("La contraseña es obligatoria"));
    }

    @Test
    void correoConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(login("esto-no-es-un-correo", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").value("Formato de correo inválido"));
    }

    @Test
    void laRespuestaNuncaIncluyeLaContrasenaNiSuHash() throws Exception {
        String cuerpo = mockMvc.perform(login(VETERINARIO, PASSWORD))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(PASSWORD).doesNotContain("$2a$").doesNotContainIgnoringCase("password");
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String correo, String password) {
        String json = "{\"correo\": \"" + correo + "\", \"password\": \"" + password + "\"}";
        return post(URL_LOGIN).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String extraerToken(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        int inicio = cuerpo.indexOf("\"token\":\"") + "\"token\":\"".length();
        return cuerpo.substring(inicio, cuerpo.indexOf('"', inicio));
    }

    private void guardarUsuario(String correo, Rol rol, boolean activo) {
        usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .rol(rol)
                .activo(activo)
                .build());
    }
}
