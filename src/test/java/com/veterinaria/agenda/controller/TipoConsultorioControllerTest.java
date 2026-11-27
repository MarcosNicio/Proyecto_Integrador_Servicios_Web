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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Pruebas de {@link TipoConsultorioController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba los tipos y consultorios creados.
 * Cada prueba parte de un tipo propio, "Rayos X Prueba", con un consultorio activo y otro desactivado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class TipoConsultorioControllerTest {

    private static final String URL = "/api/consultorios/tipos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipoConsultorioRepository tipoRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TipoConsultorio rayosX;
    private Consultorio salaActiva;

    @BeforeEach
    void crearTipoDePrueba() {
        rayosX = tipoRepository.save(TipoConsultorio.builder().nombre("Rayos X Prueba").build());
        salaActiva = consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(rayosX).nombre("Sala RX 1 Prueba").build());
        consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(rayosX).nombre("Sala RX 2 Prueba").activo(false).build());
    }

    // ---------- Consultar ----------

    @Test
    void listaLosTiposConSuConteoDeConsultoriosActivos() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Consulta")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Cirugía")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Estética")))
                // 2 consultorios, pero solo 1 activo
                .andExpect(jsonPath("$[?(@.nombre=='Rayos X Prueba')].consultoriosActivos").value(1))
                .andExpect(jsonPath("$[?(@.nombre=='Rayos X Prueba')].activo").value(true));
    }

    @Test
    void filtraPorEstadoActivoODesactivado() throws Exception {
        TipoConsultorio viejo = tipoRepository.save(TipoConsultorio.builder()
                .nombre("Laboratorio Prueba").activo(false).build());

        mockMvc.perform(get(URL).param("activo", "true"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Rayos X Prueba")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem(viejo.getNombre()))));
        mockMvc.perform(get(URL).param("activo", "false"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Laboratorio Prueba")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Rayos X Prueba"))));
    }

    @Test
    void cualquierRolConSesionPuedeConsultarYSinSesionDa401() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(get(URL).with(user("alguien@correo.com").roles(rol)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Crear y editar ----------

    @Test
    void creaUnTipoActivoSinConsultorios() throws Exception {
        enviar(post(URL), "{\"nombre\":\"  Hospitalización Prueba \",\"descripcion\":\"Sala de recuperación\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoConsultorio").isNumber())
                .andExpect(jsonPath("$.nombre").value("Hospitalización Prueba"))
                .andExpect(jsonPath("$.descripcion").value("Sala de recuperación"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.consultoriosActivos").value(0));
    }

    @Test
    void nombreRepetidoSinImportarMayusculasNiEspaciosDevuelve409() throws Exception {
        enviar(post(URL), "{\"nombre\":\"  CIRUGÍA \"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un tipo de consultorio con el nombre 'CIRUGÍA'"));
    }

    @Test
    void nombreVacioOMuyLargoDevuelve400() throws Exception {
        enviar(post(URL), "{\"nombre\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"));
        enviar(post(URL), "{\"nombre\":\"" + "x".repeat(81) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre no puede exceder los 80 caracteres"));
    }

    @Test
    void editaNombreYDescripcionYPuedeConservarSuPropioNombre() throws Exception {
        String url = URL + "/" + rayosX.getIdTipoConsultorio();

        enviar(put(url), "{\"nombre\":\"rayos x prueba\",\"descripcion\":\"Diagnóstico por imagen\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("rayos x prueba"))
                .andExpect(jsonPath("$.descripcion").value("Diagnóstico por imagen"));
        enviar(put(url), "{\"nombre\":\"Imagenología Prueba\",\"descripcion\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Imagenología Prueba"))
                .andExpect(jsonPath("$.descripcion").doesNotExist());
    }

    @Test
    void editarHaciaElNombreDeOtroTipoDevuelve409YUnTipoInexistenteDa404() throws Exception {
        enviar(put(URL + "/" + rayosX.getIdTipoConsultorio()), "{\"nombre\":\"Estética\"}")
                .andExpect(status().isConflict());
        enviar(put(URL + "/999999"), "{\"nombre\":\"Lo que sea\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tipo de consultorio no encontrado con id 999999"));
    }

    // ---------- Desactivar y reactivar ----------

    @Test
    void unTipoConConsultoriosActivosNoSeDesactivaHastaDesactivarSusConsultorios() throws Exception {
        String url = URL + "/" + rayosX.getIdTipoConsultorio();

        mockMvc.perform(patch(url + "/desactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede desactivar el tipo 'Rayos X Prueba': tiene 1 "
                        + "consultorio(s) activo(s). Desactiva primero sus consultorios"));
        assertThat(tipoRepository.findById(rayosX.getIdTipoConsultorio()).orElseThrow().getActivo()).isTrue();

        salaActiva.setActivo(false);
        consultorioRepository.saveAndFlush(salaActiva);
        mockMvc.perform(patch(url + "/desactivar")).andExpect(status().isNoContent());
        assertThat(tipoRepository.findById(rayosX.getIdTipoConsultorio()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void desactivarYReactivarDosVecesSeguidasNoDaError() throws Exception {
        TipoConsultorio sinConsultorios = tipoRepository.save(TipoConsultorio.builder().nombre("Vacío Prueba").build());
        String url = URL + "/" + sinConsultorios.getIdTipoConsultorio();

        mockMvc.perform(patch(url + "/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(patch(url + "/desactivar")).andExpect(status().isNoContent());
        mockMvc.perform(patch(url + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(patch(url + "/reactivar")).andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/999999/desactivar")).andExpect(status().isNotFound());
    }

    @Test
    void unTipoDesactivadoYaNoPasaLaConsultaDelModuloDeServicios() throws Exception {
        // La misma consulta que usa el módulo de servicios para aceptar un tipo en un servicio
        String consulta = "SELECT EXISTS(SELECT 1 FROM TIPOS_CONSULTORIO WHERE id_tipo_consultorio = ? AND activo = TRUE)";
        Integer id = rayosX.getIdTipoConsultorio();
        salaActiva.setActivo(false);
        consultorioRepository.saveAndFlush(salaActiva);

        assertThat(jdbcTemplate.queryForObject(consulta, Boolean.class, id)).isTrue();

        mockMvc.perform(patch(URL + "/" + id + "/desactivar")).andExpect(status().isNoContent());
        tipoRepository.flush();
        assertThat(jdbcTemplate.queryForObject(consulta, Boolean.class, id)).isFalse();

        mockMvc.perform(patch(URL + "/" + id + "/reactivar")).andExpect(status().isOk());
        tipoRepository.flush();
        assertThat(jdbcTemplate.queryForObject(consulta, Boolean.class, id)).isTrue();
    }

    // ---------- Permisos ----------

    @Test
    void soloElAdminPuedeModificarLosTipos() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            mockMvc.perform(post(URL).with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nuevo\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(URL + "/1").with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nuevo\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/1/desactivar").with(usuario)).andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/1/reactivar").with(usuario)).andExpect(status().isForbidden());
        }
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder metodo,
                                 String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
