package com.veterinaria.auth.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.security.SecurityErrorWriter;
import com.veterinaria.common.util.JwtUtil;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.PersonalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@code GET /api/auth/me} con tokens reales, que pasan por el filtro JWT igual que
 * las peticiones del frontend. {@code @Transactional} deshace las cuentas creadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthMeTest {

    private static final String CLIENTE = "cliente.me@correo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Test
    void clienteRecibeSuPerfilDeClienteDespuesDelLogin() throws Exception {
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"correo":"%s","password":"MiClave123","nombre":"Ana","apellidos":"López",
                         "telefono":"5551234567"}
                        """.formatted(CLIENTE)))
                .andExpect(status().isCreated());

        // Como haría el frontend otro día: inicia sesión y pide sus datos con el token del login
        String login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + CLIENTE + "\",\"password\":\"MiClave123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = login.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        Integer idUsuario = usuarioRepository.findByCorreo(CLIENTE).orElseThrow().getIdUsuario();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(idUsuario))
                .andExpect(jsonPath("$.correo").value(CLIENTE))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.cliente.idCliente").isNumber())
                .andExpect(jsonPath("$.cliente.idUsuario").value(idUsuario))
                .andExpect(jsonPath("$.cliente.nombre").value("Ana"))
                .andExpect(jsonPath("$.personal").isEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void veterinarioRecibeSuPerfilDePersonal() throws Exception {
        Usuario usuario = guardarUsuario("vet.me@veterinaria.com", Rol.VETERINARIO, true);
        Integer idPersonal = personalRepository.save(Personal.builder()
                .usuario(usuario).nombre("Laura").apellidos("Zamora").telefono("5551112222")
                .cedulaProfesional("CED-1").build()).getIdPersonal();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("VETERINARIO"))
                .andExpect(jsonPath("$.personal.idPersonal").value(idPersonal))
                .andExpect(jsonPath("$.personal.cedulaProfesional").value("CED-1"))
                .andExpect(jsonPath("$.cliente").isEmpty());
    }

    @Test
    void cajeroRecibeSuPerfilDePersonal() throws Exception {
        Usuario usuario = guardarUsuario("caja.me@veterinaria.com", Rol.CAJERO, true);
        personalRepository.save(Personal.builder()
                .usuario(usuario).nombre("Pedro").apellidos("Alvarez").telefono("5553334444").build());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CAJERO"))
                .andExpect(jsonPath("$.personal.nombre").value("Pedro"));
    }

    @Test
    void adminRecibeSoloSuCuentaSinPerfiles() throws Exception {
        Usuario admin = usuarioRepository.findByCorreo("admin@veterinaria.com").orElseThrow();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@veterinaria.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.cliente").isEmpty())
                .andExpect(jsonPath("$.personal").isEmpty());
    }

    @Test
    void cuentaSinPerfilDevuelveElPerfilEnNull() throws Exception {
        // Un CLIENTE sin fila en CLIENTES (ej. su perfil se borró): no debe fallar
        Usuario usuario = guardarUsuario("sin.perfil.me@correo.com", Rol.CLIENTE, true);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.cliente").isEmpty());
    }

    @Test
    void sinTokenDevuelve401AunqueEsteDentroDeApiAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_SIN_TOKEN));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-inventado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_TOKEN_INVALIDO));
    }

    @Test
    void cuentaDesactivadaDevuelve401() throws Exception {
        Usuario usuario = guardarUsuario("inactivo.me@veterinaria.com", Rol.CAJERO, false);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorWriter.MENSAJE_USUARIO_INACTIVO));
    }

    @Test
    void loginYRegistroSiguenSiendoPublicos() throws Exception {
        // La excepción de /me no debe afectar a las demás rutas de /api/auth
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@veterinaria.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Ayudantes ----------

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtUtil.generarToken(usuario.getCorreo(), usuario.getRol().name());
    }

    private Usuario guardarUsuario(String correo, Rol rol, boolean activo) {
        return usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(rol)
                .activo(activo)
                .build());
    }
}
