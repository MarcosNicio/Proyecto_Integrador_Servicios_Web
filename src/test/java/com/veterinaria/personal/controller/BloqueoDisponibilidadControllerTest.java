package com.veterinaria.personal.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.entity.BloqueoDisponibilidad;
import com.veterinaria.personal.entity.MotivoBloqueo;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.BloqueoDisponibilidadRepository;
import com.veterinaria.personal.repository.PersonalRepository;
import com.veterinaria.personal.service.BloqueoDisponibilidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link BloqueoDisponibilidadController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba el personal y los bloqueos creados.
 * Los bloqueos "ya terminados" se guardan directo con el repositorio, porque la API no deja crearlos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class BloqueoDisponibilidadControllerTest {

    private static final String VET = "vet.bloqueos@veterinaria.com";
    private static final String DIC_22 = "2030-12-22T00:00";
    private static final String DIC_27 = "2030-12-27T00:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private BloqueoDisponibilidadRepository bloqueoRepository;

    private Personal vet;
    private Personal cajero;

    @BeforeEach
    void crearPersonal() {
        vet = guardarPersonal(VET, Rol.VETERINARIO);
        cajero = guardarPersonal("caja.bloqueos@veterinaria.com", Rol.CAJERO);
    }

    // ---------- Registrar y listar ----------

    @Test
    void registraUnBloqueoConFechasEnFormatoSinSegundos() throws Exception {
        registrar(vet.getIdPersonal(), "2030-12-22T09:30:45", DIC_27, "PERSONAL", "Vacaciones")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBloqueo").isNumber())
                .andExpect(jsonPath("$.idPersonal").value(vet.getIdPersonal()))
                .andExpect(jsonPath("$.fechaInicio").value("2030-12-22T09:30"))
                .andExpect(jsonPath("$.fechaFin").value(DIC_27))
                .andExpect(jsonPath("$.motivo").value("PERSONAL"))
                .andExpect(jsonPath("$.notas").value("Vacaciones"));
    }

    @Test
    void permiteUnaAusenciaQueYaEmpezoSiTodaviaNoTermina() throws Exception {
        LocalDateTime haceUnaHora = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime enCuatroHoras = haceUnaHora.plusHours(5);

        registrar(vet.getIdPersonal(), haceUnaHora.toString(), enCuatroHoras.toString(), "DIA_LIBRE", null)
                .andExpect(status().isCreated());
    }

    @Test
    void laListaOcultaLosTerminadosSalvoQueSePidan() throws Exception {
        guardarTerminado(vet);
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isCreated());
        registrar(vet.getIdPersonal(), "2030-06-01T08:00", "2030-06-01T14:00", "CIRUGIA", null)
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/personal/" + vet.getIdPersonal() + "/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].motivo", contains("CIRUGIA", "PERSONAL")));

        mockMvc.perform(get("/api/personal/" + vet.getIdPersonal() + "/bloqueos").param("incluirPasados", "true"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].motivo").value("DIA_LIBRE"));
    }

    // ---------- Validaciones ----------

    @Test
    void motivoInvalidoYCamposFaltantesDevuelven400() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "VACACIONES", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.motivo").value("El motivo debe ser PERSONAL, CIRUGIA o DIA_LIBRE"));

        mockMvc.perform(post("/api/personal/" + vet.getIdPersonal() + "/bloqueos")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fechaInicio").exists())
                .andExpect(jsonPath("$.errors.fechaFin").exists())
                .andExpect(jsonPath("$.errors.motivo").exists());
    }

    @Test
    void finAntesOIgualAlInicioDevuelve400() throws Exception {
        registrar(vet.getIdPersonal(), DIC_27, DIC_22, "PERSONAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(BloqueoDisponibilidadService.MENSAJE_RANGO_INVALIDO));
        registrar(vet.getIdPersonal(), DIC_22, DIC_22, "PERSONAL", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void bloqueoQueYaTerminoDevuelve400() throws Exception {
        registrar(vet.getIdPersonal(), "2020-01-01T08:00", "2020-01-02T08:00", "PERSONAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(BloqueoDisponibilidadService.MENSAJE_YA_TERMINO));
    }

    @Test
    void bloqueoEncimadoDevuelve409ConElQueChoca() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isCreated());

        registrar(vet.getIdPersonal(), "2030-12-26T10:00", "2030-12-28T10:00", "CIRUGIA", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("El bloqueo se encima con otro bloqueo (PERSONAL) del 2030-12-22 00:00 al 2030-12-27 00:00"));
    }

    @Test
    void bloqueosSeguidosODeOtraPersonaSiSePermiten() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isCreated());

        registrar(vet.getIdPersonal(), DIC_27, "2030-12-28T00:00", "DIA_LIBRE", null)   // empieza justo al terminar
                .andExpect(status().isCreated());
        registrar(cajero.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null)            // otra persona
                .andExpect(status().isCreated());
    }

    @Test
    void empleadoInexistenteDevuelve404() throws Exception {
        registrar(999999, DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/personal/999999/bloqueos")).andExpect(status().isNotFound());
    }

    // ---------- Editar y borrar ----------

    @Test
    void editarNoChocaConsigoMismo() throws Exception {
        Integer id = idDe(registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null));

        editar(vet.getIdPersonal(), id, DIC_22, "2030-12-29T00:00", "PERSONAL", "Se alargan")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaFin").value("2030-12-29T00:00"))
                .andExpect(jsonPath("$.notas").value("Se alargan"));
    }

    @Test
    void editarHaciaUnChoqueDevuelve409() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isCreated());
        Integer id = idDe(registrar(vet.getIdPersonal(), "2031-01-05T00:00", "2031-01-06T00:00", "DIA_LIBRE", null));

        editar(vet.getIdPersonal(), id, "2030-12-26T00:00", "2031-01-06T00:00", "DIA_LIBRE", null)
                .andExpect(status().isConflict());
    }

    @Test
    void borraUnBloqueoVigente() throws Exception {
        Integer id = idDe(registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null));

        mockMvc.perform(delete("/api/personal/" + vet.getIdPersonal() + "/bloqueos/" + id))
                .andExpect(status().isNoContent());
        assertThat(bloqueoRepository.findById(id)).isEmpty();
    }

    @Test
    void unBloqueoTerminadoNoSeEditaNiSeBorra() throws Exception {
        Integer id = guardarTerminado(vet).getIdBloqueo();

        editar(vet.getIdPersonal(), id, DIC_22, DIC_27, "PERSONAL", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(BloqueoDisponibilidadService.MENSAJE_HISTORIAL));
        mockMvc.perform(delete("/api/personal/" + vet.getIdPersonal() + "/bloqueos/" + id))
                .andExpect(status().isBadRequest());
        assertThat(bloqueoRepository.findById(id)).isPresent();
    }

    @Test
    void bloqueoDeOtraPersonaDevuelve404() throws Exception {
        Integer delCajero = idDe(registrar(cajero.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null));

        editar(vet.getIdPersonal(), delCajero, DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/personal/" + vet.getIdPersonal() + "/bloqueos/" + delCajero))
                .andExpect(status().isNotFound());
        assertThat(bloqueoRepository.findById(delCajero)).isPresent();
    }

    // ---------- Permisos ----------

    @Test
    @WithMockUser(roles = "VETERINARIO")
    void veterinarioNoPuedeGestionarBloqueos() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "CIRUGIA", null).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/personal/" + vet.getIdPersonal() + "/bloqueos")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeGestionarBloqueos() throws Exception {
        registrar(cajero.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeVerBloqueosNiUsarMisBloqueos() throws Exception {
        mockMvc.perform(get("/api/personal/" + vet.getIdPersonal() + "/bloqueos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/personal/mi-perfil/bloqueos")).andExpect(status().isForbidden());
    }

    @Test
    void elEmpleadoConsultaSusPropiosBloqueos() throws Exception {
        registrar(vet.getIdPersonal(), DIC_22, DIC_27, "PERSONAL", null).andExpect(status().isCreated());
        registrar(cajero.getIdPersonal(), DIC_22, DIC_27, "DIA_LIBRE", null).andExpect(status().isCreated());
        guardarTerminado(vet);

        mockMvc.perform(get("/api/personal/mi-perfil/bloqueos").with(user(VET).roles("VETERINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].motivo", contains("PERSONAL")));

        mockMvc.perform(get("/api/personal/mi-perfil/bloqueos").param("incluirPasados", "true")
                        .with(user(VET).roles("VETERINARIO")))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ---------- Ayudantes ----------

    private ResultActions registrar(Integer idPersonal, String inicio, String fin, String motivo, String notas)
            throws Exception {
        return mockMvc.perform(post("/api/personal/" + idPersonal + "/bloqueos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(inicio, fin, motivo, notas)));
    }

    private ResultActions editar(Integer idPersonal, Integer idBloqueo, String inicio, String fin, String motivo,
                                 String notas) throws Exception {
        return mockMvc.perform(put("/api/personal/" + idPersonal + "/bloqueos/" + idBloqueo)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(inicio, fin, motivo, notas)));
    }

    private String cuerpo(String inicio, String fin, String motivo, String notas) {
        String notasJson = notas == null ? "null" : "\"" + notas + "\"";
        return "{\"fechaInicio\":\"%s\",\"fechaFin\":\"%s\",\"motivo\":\"%s\",\"notas\":%s}"
                .formatted(inicio, fin, motivo, notasJson);
    }

    private Integer idDe(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.valueOf(json.replaceAll(".*\"idBloqueo\":(\\d+).*", "$1"));
    }

    /** Un bloqueo de hace 10 días, guardado directo (la API no permite registrar bloqueos terminados). */
    private BloqueoDisponibilidad guardarTerminado(Personal personal) {
        LocalDateTime inicio = LocalDateTime.now().minusDays(10).truncatedTo(ChronoUnit.HOURS);
        return bloqueoRepository.save(BloqueoDisponibilidad.builder()
                .personal(personal)
                .fechaInicio(inicio)
                .fechaFin(inicio.plusDays(1))
                .motivo(MotivoBloqueo.DIA_LIBRE)
                .build());
    }

    private Personal guardarPersonal(String correo, Rol rol) {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(rol)
                .build());
        return personalRepository.save(Personal.builder()
                .usuario(usuario)
                .nombre("Prueba")
                .apellidos("Bloqueos")
                .telefono("5551112222")
                .cedulaProfesional(rol == Rol.VETERINARIO ? "CED-1" : null)
                .build());
    }
}
