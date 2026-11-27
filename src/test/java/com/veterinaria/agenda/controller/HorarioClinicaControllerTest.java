package com.veterinaria.agenda.controller;

import com.veterinaria.agenda.service.HorarioClinicaService;
import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.entity.HorarioPersonal;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.HorarioPersonalRepository;
import com.veterinaria.personal.repository.PersonalRepository;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link HorarioClinicaController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba los cambios al horario y los turnos creados.
 * Los turnos de prueba se guardan con los repositorios para poder dejarlos en cualquier hora.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class HorarioClinicaControllerTest {

    private static final String URL = "/api/config/horarios-clinica";
    private static final int DOMINGO = 0;
    private static final int LUNES = 1;
    private static final int SABADO = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private HorarioPersonalRepository horarioPersonalRepository;

    private Personal laura;

    @BeforeEach
    void crearVeterinaria() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo("laura.clinica@veterinaria.com")
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(Rol.VETERINARIO)
                .build());
        laura = personalRepository.save(Personal.builder()
                .usuario(usuario).nombre("Laura").apellidos("Zamora").telefono("5551112222")
                .cedulaProfesional("CED-1").build());
    }

    // ---------- Consultar ----------

    @Test
    void devuelveLaSemanaCompletaOrdenadaConElNombreDelDia() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].diaSemana", contains(0, 1, 2, 3, 4, 5, 6)))
                .andExpect(jsonPath("$[0].nombreDia").value("Domingo"))
                .andExpect(jsonPath("$[6].nombreDia").value("Sábado"))
                .andExpect(jsonPath("$[1].horaApertura").value("08:00"))
                .andExpect(jsonPath("$[1].laborable").value(true));
    }

    @Test
    void cualquierRolConSesionPuedeConsultar() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(get(URL).with(user("alguien@correo.com").roles(rol)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(7)));
        }
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        mockMvc.perform(get(URL).with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Modificar ----------

    @Test
    void guardaElNuevoHorarioYLaConsultaLoReflejaConHorasSinSegundos() throws Exception {
        String semana = semana(Map.of(
                SABADO, dia(SABADO, "09:00:30", "13:00:59", true),
                DOMINGO, dia(DOMINGO, "09:00", "14:00", false)));

        enviar(semana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[6].horaApertura").value("09:00"))
                .andExpect(jsonPath("$[6].horaCierre").value("13:00"))
                .andExpect(jsonPath("$[0].laborable").value(false))
                .andExpect(jsonPath("$[0].horaApertura").value("09:00"));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[6].horaCierre").value("13:00"))
                .andExpect(jsonPath("$[0].laborable").value(false));
    }

    @Test
    void guardarElMismoHorarioDosVecesNoDaError() throws Exception {
        enviar(semana(Map.of())).andExpect(status().isOk());
        enviar(semana(Map.of())).andExpect(status().isOk());
    }

    @Test
    void elFrontendPuedeReenviarTalCualLoQueDevolvioElGet() throws Exception {
        String actual = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        enviar(actual).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)));
    }

    // ---------- Validaciones ----------

    @Test
    void menosDeSieteDiasDevuelve400() throws Exception {
        String seisDias = "[" + String.join(",", List.of(
                dia(0, "08:00", "20:00", true), dia(1, "08:00", "20:00", true), dia(2, "08:00", "20:00", true),
                dia(3, "08:00", "20:00", true), dia(4, "08:00", "20:00", true), dia(5, "08:00", "20:00", true))) + "]";

        enviar(seisDias)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(HorarioClinicaService.MENSAJE_SEMANA_INCOMPLETA));
        enviar("[]").andExpect(status().isBadRequest());
    }

    @Test
    void unDiaRepetidoDevuelve400() throws Exception {
        // Siete elementos, pero el lunes viene dos veces y falta el martes
        String semana = semana(Map.of(2, dia(LUNES, "08:00", "20:00", true)));

        enviar(semana)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(HorarioClinicaService.MENSAJE_SEMANA_INCOMPLETA));
    }

    @Test
    void unDiaFueraDeRangoDevuelve400ConElElementoAfectado() throws Exception {
        enviar(semana(Map.of(3, dia(7, "08:00", "20:00", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Elemento 4 de la lista: El día de la semana debe estar entre 0 (domingo) y 6 (sábado)"));
    }

    @Test
    void camposFaltantesDevuelven400() throws Exception {
        enviar(semana(Map.of(2, "{\"diaSemana\":2,\"horaApertura\":\"08:00\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Elemento 3 de la lista:")))
                .andExpect(jsonPath("$.message", containsString("La hora de cierre es obligatoria")))
                .andExpect(jsonPath("$.message", containsString("Indica si el día es laborable")));
    }

    @Test
    void cierreAntesDeLaAperturaDevuelve400AunqueElDiaNoSeaLaborable() throws Exception {
        enviar(semana(Map.of(LUNES, dia(LUNES, "20:00", "08:00", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El día Lunes: la hora de cierre debe ser posterior a la hora de apertura"));

        // La base exige el rango válido también en un día cerrado
        enviar(semana(Map.of(DOMINGO, dia(DOMINGO, "14:00", "14:00", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El día Domingo: la hora de cierre debe ser posterior a la hora de apertura"));
    }

    @Test
    void unCuerpoQueNoEsUnaListaOUnaHoraInvalidaDevuelve400() throws Exception {
        enviar("{}").andExpect(status().isBadRequest());
        enviar(semana(Map.of(LUNES, dia(LUNES, "25:00", "20:00", true)))).andExpect(status().isBadRequest());
    }

    // ---------- Turnos que quedarían fuera ----------

    @Test
    void unTurnoQueSeSalePorElCierreBloqueaElCambioYNoSeGuardaNada() throws Exception {
        enviar(semana(Map.of())).andExpect(status().isOk()); // base conocida: todos los días 08:00-20:00
        guardarTurno(SABADO, "10:00", "18:00");

        // Cambia el lunes (válido) y el sábado (deja fuera el turno): no debe guardarse ninguno de los dos
        enviar(semana(Map.of(
                LUNES, dia(LUNES, "09:00", "19:00", true),
                SABADO, dia(SABADO, "08:00", "14:00", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se guardó el horario: 1 turno quedaría fuera del horario "
                        + "de la clínica: Laura Zamora, Sábado 10:00-18:00. Ajusta o borra esos turnos y vuelve a intentarlo."));

        mockMvc.perform(get(URL))
                .andExpect(jsonPath("$[1].horaApertura").value("08:00"))
                .andExpect(jsonPath("$[6].horaCierre").value("20:00"));
    }

    @Test
    void unTurnoQueSeSalePorLaAperturaBloqueaElCambio() throws Exception {
        guardarTurno(LUNES, "08:00", "12:00");

        enviar(semana(Map.of(LUNES, dia(LUNES, "09:00", "20:00", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Laura Zamora, Lunes 08:00-12:00")));
    }

    @Test
    void unDiaQuePasaANoLaborableBloqueaElCambioSiTieneTurnos() throws Exception {
        guardarTurno(DOMINGO, "09:00", "12:00");

        enviar(semana(Map.of(DOMINGO, dia(DOMINGO, "09:00", "14:00", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Laura Zamora, Domingo 09:00-12:00")));
    }

    @Test
    void listaTodosLosTurnosAfectadosDeVariosEmpleados() throws Exception {
        guardarTurno(SABADO, "10:00", "18:00");
        guardarTurno(LUNES, "07:00", "12:00");

        enviar(semana(Map.of(
                SABADO, dia(SABADO, "08:00", "14:00", true),
                LUNES, dia(LUNES, "08:00", "20:00", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("2 turnos quedarían fuera")))
                .andExpect(jsonPath("$.message", containsString("Laura Zamora, Lunes 07:00-12:00")))
                .andExpect(jsonPath("$.message", containsString("Laura Zamora, Sábado 10:00-18:00")));
    }

    @Test
    void conMasDeDiezTurnosAfectadosListaLosPrimerosDiezYCuentaElResto() throws Exception {
        for (int i = 0; i < 12; i++) {
            guardarTurno(SABADO, "10:00", "18:00");
        }

        enviar(semana(Map.of(SABADO, dia(SABADO, "08:00", "14:00", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("12 turnos quedarían fuera")))
                .andExpect(jsonPath("$.message", containsString("y 2 más")));
    }

    @Test
    void losTurnosQueSiCabenNoBloqueanElCambio() throws Exception {
        guardarTurno(LUNES, "09:00", "12:00");
        guardarTurno(SABADO, "08:00", "14:00"); // coincide exactamente con el nuevo horario: cabe

        enviar(semana(Map.of(SABADO, dia(SABADO, "08:00", "14:00", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[6].horaCierre").value("14:00"));
    }

    // ---------- Permisos ----------

    @Test
    void soloElAdminPuedeModificarElHorario() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(put(URL).with(user("alguien@correo.com").roles(rol))
                            .contentType(MediaType.APPLICATION_JSON).content(semana(Map.of())))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(String cuerpo) throws Exception {
        return mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    /** Semana de lunes a domingo con todos los días de 08:00 a 20:00, salvo los que se indiquen. */
    private String semana(Map<Integer, String> cambios) {
        List<String> dias = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            dias.add(cambios.getOrDefault(d, dia(d, "08:00", "20:00", true)));
        }
        return "[" + String.join(",", dias) + "]";
    }

    private String dia(int diaSemana, String apertura, String cierre, boolean laborable) {
        return "{\"diaSemana\":%d,\"horaApertura\":\"%s\",\"horaCierre\":\"%s\",\"laborable\":%s}"
                .formatted(diaSemana, apertura, cierre, laborable);
    }

    private void guardarTurno(int diaSemana, String inicio, String fin) {
        horarioPersonalRepository.save(HorarioPersonal.builder()
                .personal(laura)
                .diaSemana(diaSemana)
                .horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin))
                .build());
    }
}
