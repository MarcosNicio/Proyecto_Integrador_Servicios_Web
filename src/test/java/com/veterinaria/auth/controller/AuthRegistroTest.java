package com.veterinaria.auth.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.auth.service.AuthService;
import com.veterinaria.clientes.entity.Cliente;
import com.veterinaria.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba {@code POST /api/auth/registro} de punta a punta: crea el usuario y el cliente en
 * la base real, y luego usa el token y las credenciales.
 * <p>
 * {@code @Transactional} deshace al terminar cada prueba todo lo que se crea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegistroTest {

    private static final String URL_REGISTRO = "/api/auth/registro";
    private static final String CORREO = "ana.registro@correo.com";
    private static final String PASSWORD = "MiClave123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registroCorrectoDevuelve201ConTokenYPerfilDelCliente() throws Exception {
        mockMvc.perform(registro(CORREO, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.correo").value(CORREO))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.cliente.idCliente").isNumber())
                .andExpect(jsonPath("$.cliente.codigoCliente").value(matchesPattern("CLI-\\d{4,}")))
                .andExpect(jsonPath("$.cliente.nombre").value("Ana"))
                .andExpect(jsonPath("$.cliente.apellidos").value("López Pérez"))
                .andExpect(jsonPath("$.cliente.telefono").value("5551234567"));
    }

    @Test
    void enLaBaseQuedanElUsuarioClienteYSuPerfilVinculado() throws Exception {
        mockMvc.perform(registro(CORREO, PASSWORD)).andExpect(status().isCreated());

        Usuario usuario = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        assertThat(usuario.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(usuario.getActivo()).isTrue();

        Cliente cliente = clienteRepository.findByIdUsuario(usuario.getIdUsuario()).orElseThrow();
        assertThat(cliente.getNombre()).isEqualTo("Ana");
        assertThat(cliente.getDireccion()).isEqualTo("Av. Juárez 123");
    }

    @Test
    void laContrasenaSeGuardaCifrada() throws Exception {
        mockMvc.perform(registro(CORREO, PASSWORD)).andExpect(status().isCreated());

        String hash = usuarioRepository.findByCorreo(CORREO).orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo(PASSWORD).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void elTokenDelRegistroDaAccesoAUnaRutaPrivada() throws Exception {
        String token = extraerToken(mockMvc.perform(registro(CORREO, PASSWORD)).andReturn());
        Integer idUsuario = usuarioRepository.findByCorreo(CORREO).orElseThrow().getIdUsuario();

        mockMvc.perform(get("/api/clientes/usuario/" + idUsuario)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(idUsuario));
    }

    @Test
    void despuesDeRegistrarsePuedeIniciarSesion() throws Exception {
        mockMvc.perform(registro(CORREO, PASSWORD)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"" + CORREO + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void elCorreoSeGuardaEnMinusculasYSinEspacios() throws Exception {
        mockMvc.perform(registro("  Ana.Registro@Correo.COM ", PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value(CORREO));

        assertThat(usuarioRepository.existsByCorreo(CORREO)).isTrue();
    }

    @Test
    void correoRepetidoDevuelve409AunqueCambienLasMayusculas() throws Exception {
        mockMvc.perform(registro(CORREO, PASSWORD)).andExpect(status().isCreated());

        mockMvc.perform(registro("ANA.REGISTRO@correo.com", "OtraClave456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(AuthService.MENSAJE_CORREO_REGISTRADO))
                .andExpect(jsonPath("$.path").value(URL_REGISTRO));
    }

    @Test
    void elCorreoDelAdminTambienCuentaComoRegistrado() throws Exception {
        mockMvc.perform(registro("admin@veterinaria.com", PASSWORD))
                .andExpect(status().isConflict());
    }

    @Test
    void contrasenaCortaDevuelve400() throws Exception {
        mockMvc.perform(registro(CORREO, "Ab1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    void contrasenaSinNumeroDevuelve400() throws Exception {
        mockMvc.perform(registro(CORREO, "SoloLetras"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("La contraseña debe incluir al menos una letra y un número"));
    }

    @Test
    void contrasenaSinLetraDevuelve400() throws Exception {
        mockMvc.perform(registro(CORREO, "12345678"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("La contraseña debe incluir al menos una letra y un número"));
    }

    @Test
    void camposObligatoriosVaciosDevuelven400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post(URL_REGISTRO).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").value("El correo es obligatorio"))
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.apellidos").value("Los apellidos son obligatorios"))
                .andExpect(jsonPath("$.errors.telefono").value("El teléfono es obligatorio"));
    }

    @Test
    void laRespuestaNuncaIncluyeLaContrasenaNiSuHash() throws Exception {
        String cuerpo = mockMvc.perform(registro(CORREO, PASSWORD))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(PASSWORD).doesNotContain("$2a$").doesNotContainIgnoringCase("password");
    }

    private RequestBuilder registro(String correo, String password) {
        String json = """
                {
                  "correo": "%s",
                  "password": "%s",
                  "nombre": "Ana",
                  "apellidos": "López Pérez",
                  "telefono": "5551234567",
                  "direccion": "Av. Juárez 123"
                }
                """.formatted(correo, password);
        return post(URL_REGISTRO).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String extraerToken(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        int inicio = cuerpo.indexOf("\"token\":\"") + "\"token\":\"".length();
        return cuerpo.substring(inicio, cuerpo.indexOf('"', inicio));
    }
}
