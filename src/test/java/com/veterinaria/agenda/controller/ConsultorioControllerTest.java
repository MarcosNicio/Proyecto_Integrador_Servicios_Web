package com.veterinaria.agenda.controller;

import com.veterinaria.agenda.entity.Consultorio;
import com.veterinaria.agenda.entity.TipoConsultorio;
import com.veterinaria.agenda.repository.ConsultorioRepository;
import com.veterinaria.agenda.repository.TipoConsultorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link ConsultorioController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba lo creado. Cada prueba parte de dos
 * tipos propios: "Rayos X Prueba" (activo, con un consultorio) y "Laboratorio Prueba" (desactivado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ConsultorioControllerTest {

    private static final String URL = "/api/consultorios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipoConsultorioRepository tipoRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    private TipoConsultorio rayosX;
    private TipoConsultorio laboratorio;
    private Consultorio sala1;

    @BeforeEach
    void crearDatosDePrueba() {
        rayosX = tipoRepository.save(TipoConsultorio.builder().nombre("Rayos X Prueba").build());
        laboratorio = tipoRepository.save(TipoConsultorio.builder().nombre("Laboratorio Prueba").activo(false).build());
        sala1 = consultorioRepository.save(Consultorio.builder().tipoConsultorio(rayosX).nombre("Sala RX 1 Prueba").build());
    }

    // ---------- Consultar ----------

    @Test
    void listaLosConsultoriosInicialesYFiltraPorTipoYEstado() throws Exception {
        consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(rayosX).nombre("Sala RX 2 Prueba").activo(false).build());
        Integer idTipo = rayosX.getIdTipoConsultorio();

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Consultorio 1")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Quirófano")));

        mockMvc.perform(get(URL).param("idTipo", idTipo.toString()))
                .andExpect(jsonPath("$[*].nombre", contains("Sala RX 1 Prueba", "Sala RX 2 Prueba")));
        mockMvc.perform(get(URL).param("idTipo", idTipo.toString()).param("activo", "true"))
                .andExpect(jsonPath("$[*].nombre", contains("Sala RX 1 Prueba")));
        mockMvc.perform(get(URL).param("idTipo", idTipo.toString()).param("activo", "false"))
                .andExpect(jsonPath("$[*].nombre", contains("Sala RX 2 Prueba")));
        mockMvc.perform(get(URL).param("activo", "false"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Sala RX 2 Prueba")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Sala RX 1 Prueba"))));
    }

    @Test
    void obtienePorIdConElNombreDeSuTipoYDa404SiNoExiste() throws Exception {
        mockMvc.perform(get(URL + "/" + sala1.getIdConsultorio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sala RX 1 Prueba"))
                .andExpect(jsonPath("$.idTipoConsultorio").value(rayosX.getIdTipoConsultorio()))
                .andExpect(jsonPath("$.nombreTipo").value("Rayos X Prueba"))
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Consultorio no encontrado con id 999999"));
    }

    @Test
    void cualquierRolConSesionPuedeConsultarYSinSesionDa401() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(get(URL).with(user("alguien@correo.com").roles(rol))).andExpect(status().isOk());
            mockMvc.perform(get(URL + "/" + sala1.getIdConsultorio()).with(user("alguien@correo.com").roles(rol)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Crear ----------

    @Test
    void creaUnConsultorioActivoConElNombreDeSuTipo() throws Exception {
        enviar(post(URL), cuerpo("  Sala RX 3 Prueba ", rayosX.getIdTipoConsultorio()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idConsultorio").isNumber())
                .andExpect(jsonPath("$.nombre").value("Sala RX 3 Prueba"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.nombreTipo").value("Rayos X Prueba"));
    }

    @Test
    void nombreRepetidoSinImportarMayusculasNiEspaciosDevuelve409() throws Exception {
        enviar(post(URL), cuerpo("  SALA rx 1 prueba ", rayosX.getIdTipoConsultorio()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un consultorio con el nombre 'SALA rx 1 prueba'"));
    }

    @Test
    void tipoInexistenteDevuelve404YTipoDesactivadoDevuelve400() throws Exception {
        enviar(post(URL), cuerpo("Sala Nueva Prueba", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tipo de consultorio no encontrado con id 999999"));
        enviar(post(URL), cuerpo("Sala Nueva Prueba", laboratorio.getIdTipoConsultorio()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tipo de consultorio 'Laboratorio Prueba' está desactivado"));
    }

    @Test
    void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
        enviar(post(URL), "{\"nombre\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.idTipoConsultorio").value("El tipo de consultorio es obligatorio"));
        enviar(post(URL), cuerpo("x".repeat(61), rayosX.getIdTipoConsultorio()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre no puede exceder los 60 caracteres"));
    }

    // ---------- Editar ----------

    @Test
    void editaElNombreYCambiaDeTipoYPuedeConservarSuPropioNombre() throws Exception {
        String url = URL + "/" + sala1.getIdConsultorio();
        Integer idConsulta = tipoRepository.findByNombreIgnoreCase("Consulta").orElseThrow().getIdTipoConsultorio();

        enviar(put(url), cuerpo("sala rx 1 prueba", rayosX.getIdTipoConsultorio()))   // mismo nombre, otra capitalización
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("sala rx 1 prueba"));
        enviar(put(url), cuerpo("Sala Movida Prueba", idConsulta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sala Movida Prueba"))
                .andExpect(jsonPath("$.nombreTipo").value("Consulta"));
    }

    @Test
    void editarHaciaElNombreDeOtroConsultorioDevuelve409() throws Exception {
        enviar(put(URL + "/" + sala1.getIdConsultorio()), cuerpo("consultorio 1", rayosX.getIdTipoConsultorio()))
                .andExpect(status().isConflict());
    }

    @Test
    void moverAUnTipoDesactivadoDevuelve400PeroEditarElNombreDentroDeSuTipoDesactivadoSiSePuede() throws Exception {
        enviar(put(URL + "/" + sala1.getIdConsultorio()), cuerpo("Sala RX 1 Prueba", laboratorio.getIdTipoConsultorio()))
                .andExpect(status().isBadRequest());

        // Un consultorio desactivado dentro de un tipo desactivado conserva su tipo al editar el nombre
        Consultorio vieja = consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(laboratorio).nombre("Sala Vieja Prueba").activo(false).build());
        enviar(put(URL + "/" + vieja.getIdConsultorio()), cuerpo("Sala Renombrada Prueba", laboratorio.getIdTipoConsultorio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sala Renombrada Prueba"));
    }

    @Test
    void editarUnConsultorioOUnTipoInexistenteDevuelve404() throws Exception {
        enviar(put(URL + "/999999"), cuerpo("Lo que sea", rayosX.getIdTipoConsultorio()))
                .andExpect(status().isNotFound());
        enviar(put(URL + "/" + sala1.getIdConsultorio()), cuerpo("Lo que sea", 999999))
                .andExpect(status().isNotFound());
    }

    // ---------- Desactivar y reactivar ----------

    @Test
    void desactivarYReactivarCambiaElEstadoYNoDaErrorSiSeRepite() throws Exception {
        String url = URL + "/" + sala1.getIdConsultorio();

        mockMvc.perform(patch(url + "/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(patch(url + "/desactivar")).andExpect(status().isNoContent());
        assertThat(consultorioRepository.findById(sala1.getIdConsultorio()).orElseThrow().getActivo()).isFalse();

        mockMvc.perform(patch(url + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(patch(url + "/reactivar")).andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/999999/desactivar")).andExpect(status().isNotFound());
    }

    @Test
    void noSeReactivaUnConsultorioCuyoTipoEstaDesactivado() throws Exception {
        Consultorio vieja = consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(laboratorio).nombre("Sala Vieja Prueba").activo(false).build());

        mockMvc.perform(patch(URL + "/" + vieja.getIdConsultorio() + "/reactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede reactivar el consultorio 'Sala Vieja Prueba': su tipo "
                        + "'Laboratorio Prueba' está desactivado. Reactiva primero el tipo"));
        assertThat(consultorioRepository.findById(vieja.getIdConsultorio()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void desactivarLosConsultoriosPermiteDesactivarSuTipo() throws Exception {
        mockMvc.perform(patch(URL + "/tipos/" + rayosX.getIdTipoConsultorio() + "/desactivar"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(URL + "/" + sala1.getIdConsultorio() + "/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(patch(URL + "/tipos/" + rayosX.getIdTipoConsultorio() + "/desactivar"))
                .andExpect(status().isNoContent());
        // Con el tipo desactivado ya no se puede reactivar el consultorio
        mockMvc.perform(patch(URL + "/" + sala1.getIdConsultorio() + "/reactivar")).andExpect(status().isConflict());
    }

    // ---------- Permisos ----------

    @Test
    void soloElAdminPuedeModificarLosConsultorios() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            String cuerpo = cuerpo("Sala Nueva Prueba", rayosX.getIdTipoConsultorio());
            mockMvc.perform(post(URL).with(usuario).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(URL + "/" + sala1.getIdConsultorio()).with(usuario)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/" + sala1.getIdConsultorio() + "/desactivar").with(usuario))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/" + sala1.getIdConsultorio() + "/reactivar").with(usuario))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String cuerpo(String nombre, Integer idTipo) {
        return "{\"nombre\":\"%s\",\"idTipoConsultorio\":%d}".formatted(nombre, idTipo);
    }
}
