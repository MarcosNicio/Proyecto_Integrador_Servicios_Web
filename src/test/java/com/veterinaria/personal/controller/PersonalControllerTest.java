package com.veterinaria.personal.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.service.PersonalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link PersonalController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba los empleados creados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class PersonalControllerTest {

    private static final String VET = "laura.vet@veterinaria.com";
    private static final String CAJERO = "pedro.caja@veterinaria.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Alta ----------

    @Test
    void altaDeVeterinarioCreaCuentaYPerfil() throws Exception {
        crear(VET, "VETERINARIO", "CED-123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPersonal").isNumber())
                .andExpect(jsonPath("$.idUsuario").isNumber())
                .andExpect(jsonPath("$.correo").value(VET))
                .andExpect(jsonPath("$.rol").value("VETERINARIO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.nombre").value("Laura"))
                .andExpect(jsonPath("$.cedulaProfesional").value("CED-123"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Usuario usuario = usuarioRepository.findByCorreo(VET).orElseThrow();
        assertThat(usuario.getRol()).isEqualTo(Rol.VETERINARIO);
        assertThat(usuario.getPasswordHash()).isNotEqualTo("Inicial123");
        assertThat(passwordEncoder.matches("Inicial123", usuario.getPasswordHash())).isTrue();
    }

    @Test
    void altaDeCajeroNoRequiereCedula() throws Exception {
        crear(CAJERO, "CAJERO", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CAJERO"))
                .andExpect(jsonPath("$.cedulaProfesional").doesNotExist());
    }

    @Test
    void elEmpleadoNuevoPuedeIniciarSesion() throws Exception {
        crear(VET, "VETERINARIO", "CED-123").andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + VET + "\",\"password\":\"Inicial123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("VETERINARIO"));
    }

    @Test
    void correoConMayusculasYEspaciosSeGuardaNormalizado() throws Exception {
        crear("  Laura.VET@Veterinaria.com ", "VETERINARIO", "CED-123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value(VET));
    }

    @Test
    void correoRepetidoDevuelve409() throws Exception {
        crear(VET, "VETERINARIO", "CED-123").andExpect(status().isCreated());

        crear(VET.toUpperCase(), "CAJERO", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado"));
    }

    @Test
    void veterinarioSinCedulaDevuelve400() throws Exception {
        crear(VET, "VETERINARIO", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PersonalService.MENSAJE_CEDULA_OBLIGATORIA));

        assertThat(usuarioRepository.existsByCorreo(VET)).isFalse();
    }

    @Test
    void rolClienteOAdminDevuelve400() throws Exception {
        crear(VET, "CLIENTE", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rol").value("El rol debe ser VETERINARIO o CAJERO"));
        crear(VET, "ADMIN", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rol").value("El rol debe ser VETERINARIO o CAJERO"));
    }

    @Test
    void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
        mockMvc.perform(post("/api/personal").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"no-es-correo","password":"corta1","rol":"CAJERO",
                                 "nombre":"","apellidos":"Ruiz","telefono":"5551112222"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.nombre").exists());
    }

    // ---------- Consultas ----------

    @Test
    void listaYFiltraPorRolYEstado() throws Exception {
        crear(VET, "VETERINARIO", "CED-123").andExpect(status().isCreated());
        Integer idCajero = idDe(crear(CAJERO, "CAJERO", null));
        mockMvc.perform(patch("/api/personal/" + idCajero + "/desactivar")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].correo", hasItem(VET)))
                .andExpect(jsonPath("$[*].correo", hasItem(CAJERO)));

        mockMvc.perform(get("/api/personal").param("rol", "VETERINARIO"))
                .andExpect(jsonPath("$[*].correo", hasItem(VET)))
                .andExpect(jsonPath("$[*].correo", not(hasItem(CAJERO))));

        mockMvc.perform(get("/api/personal").param("activo", "false"))
                .andExpect(jsonPath("$[*].correo", hasItem(CAJERO)))
                .andExpect(jsonPath("$[*].correo", not(hasItem(VET))));
    }

    @Test
    void filtroDeRolInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/personal").param("rol", "JEFE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtienePorIdY404SiNoExiste() throws Exception {
        Integer id = idDe(crear(VET, "VETERINARIO", "CED-123"));

        mockMvc.perform(get("/api/personal/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(VET));

        mockMvc.perform(get("/api/personal/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Personal no encontrado con id 999999"));
    }

    // ---------- Edición y estado ----------

    @Test
    void editaElPerfilSinTocarCorreoNiRol() throws Exception {
        Integer id = idDe(crear(VET, "VETERINARIO", "CED-123"));

        mockMvc.perform(put("/api/personal/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Laura Elena","apellidos":"Zamora","telefono":"5559998888",
                                 "cedulaProfesional":"CED-999","especialidadEspecies":"Exóticos"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura Elena"))
                .andExpect(jsonPath("$.telefono").value("5559998888"))
                .andExpect(jsonPath("$.cedulaProfesional").value("CED-999"))
                .andExpect(jsonPath("$.especialidadEspecies").value("Exóticos"))
                .andExpect(jsonPath("$.correo").value(VET))
                .andExpect(jsonPath("$.rol").value("VETERINARIO"));
    }

    @Test
    void noSePuedeQuitarLaCedulaAUnVeterinario() throws Exception {
        Integer id = idDe(crear(VET, "VETERINARIO", "CED-123"));

        mockMvc.perform(put("/api/personal/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Laura\",\"apellidos\":\"Zamora\",\"telefono\":\"5551112222\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PersonalService.MENSAJE_CEDULA_OBLIGATORIA));
    }

    @Test
    void desactivarImpideIniciarSesionYReactivarLoPermite() throws Exception {
        Integer id = idDe(crear(VET, "VETERINARIO", "CED-123"));
        String login = "{\"correo\":\"" + VET + "\",\"password\":\"Inicial123\"}";

        mockMvc.perform(patch("/api/personal/" + id + "/desactivar")).andExpect(status().isNoContent());
        assertThat(usuarioRepository.findByCorreo(VET).orElseThrow().getActivo()).isFalse();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/personal/" + id + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk());
    }

    // ---------- Permisos ----------

    @Test
    @WithMockUser(roles = "VETERINARIO")
    void veterinarioNoPuedeGestionarPersonal() throws Exception {
        mockMvc.perform(get("/api/personal")).andExpect(status().isForbidden());
        crear(CAJERO, "CAJERO", null).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeGestionarPersonal() throws Exception {
        mockMvc.perform(get("/api/personal")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/personal/1/desactivar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeGestionarPersonalNiUsarMiPerfil() throws Exception {
        mockMvc.perform(get("/api/personal")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/personal/mi-perfil")).andExpect(status().isForbidden());
    }

    @Test
    void elEmpleadoConsultaSuPropioPerfil() throws Exception {
        crear(VET, "VETERINARIO", "CED-123").andExpect(status().isCreated());

        mockMvc.perform(get("/api/personal/mi-perfil")
                        .with(user(VET).roles("VETERINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(VET))
                .andExpect(jsonPath("$.cedulaProfesional").value("CED-123"));
    }

    @Test
    @WithMockUser(username = "sin.perfil@veterinaria.com", roles = "CAJERO")
    void miPerfilSinPerfilDePersonalDevuelve404() throws Exception {
        mockMvc.perform(get("/api/personal/mi-perfil"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(PersonalService.MENSAJE_SIN_PERFIL));
    }

    // ---------- Ayudantes ----------

    private ResultActions crear(String correo, String rol, String cedula) throws Exception {
        String cedulaJson = cedula == null ? "null" : "\"" + cedula + "\"";
        return mockMvc.perform(post("/api/personal").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"correo":"%s","password":"Inicial123","rol":"%s","nombre":"Laura",
                         "apellidos":"Zamora","telefono":"5551112222","cedulaProfesional":%s,
                         "especialidadEspecies":"Perros, Gatos"}
                        """.formatted(correo, rol, cedulaJson)));
    }

    private Integer idDe(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.valueOf(json.replaceAll(".*\"idPersonal\":(\\d+).*", "$1"));
    }
}
