package com.veterinaria.personal.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.HorarioPersonalRepository;
import com.veterinaria.personal.repository.PersonalRepository;
import com.veterinaria.personal.service.HorarioPersonalService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link HorarioPersonalController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba el personal y los turnos creados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class HorarioPersonalControllerTest {

    private static final String VET = "vet.horarios@veterinaria.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private HorarioPersonalRepository horarioRepository;

    private Integer idVet;
    private Integer idCajero;

    @BeforeEach
    void crearPersonal() {
        idVet = guardarPersonal(VET, Rol.VETERINARIO);
        idCajero = guardarPersonal("caja.horarios@veterinaria.com", Rol.CAJERO);
    }

    // ---------- Agregar y listar ----------

    @Test
    void agregaUnTurnoConNombreDelDiaYHorasEnFormatoHHmm() throws Exception {
        agregar(idVet, 1, "08:00", "14:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHorario").isNumber())
                .andExpect(jsonPath("$.idPersonal").value(idVet))
                .andExpect(jsonPath("$.diaSemana").value(1))
                .andExpect(jsonPath("$.nombreDia").value("Lunes"))
                .andExpect(jsonPath("$.horaInicio").value("08:00"))
                .andExpect(jsonPath("$.horaFin").value("14:00"));
    }

    @Test
    void listaLosTurnosOrdenadosPorDiaYHora() throws Exception {
        agregar(idVet, 3, "16:00", "20:00").andExpect(status().isCreated());
        agregar(idVet, 0, "09:00", "14:00").andExpect(status().isCreated());
        agregar(idVet, 3, "08:00", "13:00").andExpect(status().isCreated());
        agregar(idCajero, 1, "09:00", "17:00").andExpect(status().isCreated());

        mockMvc.perform(get("/api/personal/" + idVet + "/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreDia", contains("Domingo", "Miércoles", "Miércoles")))
                .andExpect(jsonPath("$[*].horaInicio", contains("09:00", "08:00", "16:00")));
    }

    @Test
    void losSegundosSeDescartan() throws Exception {
        agregar(idVet, 2, "08:00:45", "14:00:59")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").value("08:00"))
                .andExpect(jsonPath("$.horaFin").value("14:00"));
    }

    // ---------- Validaciones ----------

    @Test
    void diaFueraDeRangoDevuelve400() throws Exception {
        agregar(idVet, 7, "08:00", "14:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.diaSemana").value("El día de la semana debe estar entre 0 (domingo) y 6 (sábado)"));
    }

    @Test
    void finAntesOIgualAlInicioDevuelve400() throws Exception {
        agregar(idVet, 1, "14:00", "08:00")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(HorarioPersonalService.MENSAJE_RANGO_INVALIDO));
        agregar(idVet, 1, "08:00", "08:00")
                .andExpect(status().isBadRequest());
    }

    @Test
    void camposFaltantesDevuelven400() throws Exception {
        mockMvc.perform(post("/api/personal/" + idVet + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.diaSemana").exists())
                .andExpect(jsonPath("$.errors.horaInicio").exists())
                .andExpect(jsonPath("$.errors.horaFin").exists());
    }

    @Test
    void turnoEncimadoDevuelve409ConElTurnoQueChoca() throws Exception {
        agregar(idVet, 1, "08:00", "14:00").andExpect(status().isCreated());

        agregar(idVet, 1, "12:00", "18:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El turno se encima con el turno del Lunes de 08:00 a 14:00"));
        // Uno que lo cubre por completo también choca
        agregar(idVet, 1, "07:00", "15:00").andExpect(status().isConflict());
    }

    @Test
    void turnosSeguidosOEnOtroDiaOdeOtraPersonaSiSePermiten() throws Exception {
        agregar(idVet, 1, "08:00", "13:00").andExpect(status().isCreated());

        agregar(idVet, 1, "13:00", "17:00").andExpect(status().isCreated());   // empieza justo al terminar
        agregar(idVet, 1, "18:00", "20:00").andExpect(status().isCreated());   // segundo turno del día
        agregar(idVet, 2, "08:00", "13:00").andExpect(status().isCreated());   // otro día
        agregar(idCajero, 1, "08:00", "13:00").andExpect(status().isCreated()); // otra persona
    }

    @Test
    void empleadoInexistenteDevuelve404() throws Exception {
        agregar(999999, 1, "08:00", "14:00").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/personal/999999/horarios")).andExpect(status().isNotFound());
    }

    // ---------- Editar y borrar ----------

    @Test
    void editarNoChocaConsigoMismo() throws Exception {
        Integer id = idDe(agregar(idVet, 1, "08:00", "14:00"));

        // Lo alarga una hora: se enciman consigo mismo, pero eso no cuenta
        editar(idVet, id, 1, "08:00", "15:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaFin").value("15:00"));
        // Lo cambia de día
        editar(idVet, id, 5, "08:00", "15:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreDia").value("Viernes"));
    }

    @Test
    void editarHaciaUnChoqueDevuelve409() throws Exception {
        agregar(idVet, 1, "08:00", "13:00").andExpect(status().isCreated());
        Integer tarde = idDe(agregar(idVet, 1, "16:00", "20:00"));

        editar(idVet, tarde, 1, "12:00", "20:00").andExpect(status().isConflict());
    }

    @Test
    void borraUnTurno() throws Exception {
        Integer id = idDe(agregar(idVet, 1, "08:00", "14:00"));

        mockMvc.perform(delete("/api/personal/" + idVet + "/horarios/" + id))
                .andExpect(status().isNoContent());
        assertThat(horarioRepository.findById(id)).isEmpty();
    }

    @Test
    void turnoDeOtraPersonaDevuelve404() throws Exception {
        Integer turnoDelCajero = idDe(agregar(idCajero, 1, "09:00", "17:00"));

        editar(idVet, turnoDelCajero, 1, "09:00", "18:00").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/personal/" + idVet + "/horarios/" + turnoDelCajero))
                .andExpect(status().isNotFound());
        assertThat(horarioRepository.findById(turnoDelCajero)).isPresent();
    }

    // ---------- Permisos ----------

    @Test
    @WithMockUser(roles = "VETERINARIO")
    void veterinarioNoPuedeGestionarTurnos() throws Exception {
        agregar(idVet, 1, "08:00", "14:00").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/personal/" + idVet + "/horarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeGestionarTurnos() throws Exception {
        agregar(idCajero, 1, "08:00", "14:00").andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeVerTurnosNiUsarMisHorarios() throws Exception {
        mockMvc.perform(get("/api/personal/" + idVet + "/horarios")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/personal/mi-perfil/horarios")).andExpect(status().isForbidden());
    }

    @Test
    void elEmpleadoConsultaSusPropiosTurnos() throws Exception {
        agregar(idVet, 1, "08:00", "14:00").andExpect(status().isCreated());
        agregar(idCajero, 2, "09:00", "17:00").andExpect(status().isCreated());

        mockMvc.perform(get("/api/personal/mi-perfil/horarios").with(user(VET).roles("VETERINARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreDia", contains("Lunes")))
                .andExpect(jsonPath("$[0].idPersonal").value(idVet));
    }

    // ---------- Ayudantes ----------

    private ResultActions agregar(Integer idPersonal, int dia, String inicio, String fin) throws Exception {
        return mockMvc.perform(post("/api/personal/" + idPersonal + "/horarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(dia, inicio, fin)));
    }

    private ResultActions editar(Integer idPersonal, Integer idHorario, int dia, String inicio, String fin)
            throws Exception {
        return mockMvc.perform(put("/api/personal/" + idPersonal + "/horarios/" + idHorario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(dia, inicio, fin)));
    }

    private String cuerpo(int dia, String inicio, String fin) {
        return "{\"diaSemana\":%d,\"horaInicio\":\"%s\",\"horaFin\":\"%s\"}".formatted(dia, inicio, fin);
    }

    private Integer idDe(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.valueOf(json.replaceAll(".*\"idHorario\":(\\d+).*", "$1"));
    }

    private Integer guardarPersonal(String correo, Rol rol) {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(rol)
                .build());
        return personalRepository.save(Personal.builder()
                .usuario(usuario)
                .nombre("Prueba")
                .apellidos("Horarios")
                .telefono("5551112222")
                .cedulaProfesional(rol == Rol.VETERINARIO ? "CED-1" : null)
                .build()).getIdPersonal();
    }
}
