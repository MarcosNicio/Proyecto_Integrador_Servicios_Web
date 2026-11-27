package com.veterinaria.servicios.controller;

import com.veterinaria.servicios.entity.CategoriaServicio;
import com.veterinaria.servicios.entity.Servicio;
import com.veterinaria.servicios.repository.CategoriaServicioRepository;
import com.veterinaria.servicios.repository.ServicioRepository;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de {@link CategoriaServicioController} contra la base real.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba lo creado. Cada prueba parte de una
 * categoría propia, "Radiografías Prueba", con un servicio activo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class CategoriaServicioControllerTest {

    private static final String URL = "/api/categorias-servicio";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaServicioRepository categoriaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    private CategoriaServicio radiografias;
    private Servicio servicio;

    @BeforeEach
    void crearDatosDePrueba() {
        radiografias = categoriaRepository.save(CategoriaServicio.builder().nombre("Radiografías Prueba").build());
        servicio = servicioRepository.save(Servicio.builder()
                .categoria(radiografias).nombre("Radiografía simple Prueba")
                .duracionMinutos(20).precioBase(new BigDecimal("400.00")).build());
    }

    // ---------- Consultar ----------

    @Test
    void listaLasCategoriasActivasYSoloIncluyeLasInactivasSiSePide() throws Exception {
        categoriaRepository.save(CategoriaServicio.builder().nombre("Archivada Prueba").activo(false).build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Consulta General")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Radiografías Prueba")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Archivada Prueba"))));
        mockMvc.perform(get(URL).param("incluirInactivas", "true"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Archivada Prueba")));
    }

    @Test
    void obtienePorIdYDa404SiNoExiste() throws Exception {
        mockMvc.perform(get(URL + "/" + radiografias.getIdCategoria()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Radiografías Prueba"))
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró la categoría de servicio con ID: 999999"));
    }

    @Test
    void cualquierRolConSesionPuedeConsultarYSinSesionDa401() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(get(URL).with(user("alguien@correo.com").roles(rol))).andExpect(status().isOk());
        }
        mockMvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Crear y editar ----------

    @Test
    void creaUnaCategoriaActivaConElNombreSinEspacios() throws Exception {
        enviar(post(URL), "{\"nombre\":\"  Hospitalización Prueba \",\"descripcion\":\"  Internamiento \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCategoria").isNumber())
                .andExpect(jsonPath("$.nombre").value("Hospitalización Prueba"))
                .andExpect(jsonPath("$.descripcion").value("Internamiento"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void nombreRepetidoSinImportarMayusculasNiEspaciosDevuelve409() throws Exception {
        enviar(post(URL), "{\"nombre\":\"  RADIOGRAFÍAS prueba \"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("RADIOGRAFÍAS prueba")));
    }

    @Test
    void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
        enviar(post(URL), "{\"nombre\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre de la categoría es obligatorio"));
        enviar(post(URL), "{\"nombre\":\"" + "x".repeat(81) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre de la categoría no puede exceder los 80 caracteres"));
        enviar(post(URL), "{\"nombre\":\"Valida\",\"descripcion\":\"" + "x".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.descripcion").exists());
    }

    @Test
    void editaYPuedeConservarSuPropioNombreYNoTomarElDeOtra() throws Exception {
        String url = URL + "/" + radiografias.getIdCategoria();

        enviar(put(url), "{\"nombre\":\"radiografías prueba\",\"descripcion\":\"Imagen\"}")   // mismo nombre, otra capitalización
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("radiografías prueba"))
                .andExpect(jsonPath("$.descripcion").value("Imagen"));
        enviar(put(url), "{\"nombre\":\"Cirugía\"}").andExpect(status().isConflict());
        enviar(put(URL + "/999999"), "{\"nombre\":\"Lo que sea\"}").andExpect(status().isNotFound());
    }

    // ---------- Desactivar y reactivar ----------

    @Test
    void noSeDesactivaUnaCategoriaConServiciosActivosHastaDesactivarSusServicios() throws Exception {
        String url = URL + "/" + radiografias.getIdCategoria();

        mockMvc.perform(delete(url))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede desactivar la categoría 'Radiografías Prueba': "
                        + "tiene 1 servicio(s) activo(s). Desactiva primero sus servicios"));
        assertThat(categoriaRepository.findById(radiografias.getIdCategoria()).orElseThrow().getActivo()).isTrue();

        servicio.setActivo(false);
        servicioRepository.saveAndFlush(servicio);
        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        assertThat(categoriaRepository.findById(radiografias.getIdCategoria()).orElseThrow().getActivo()).isFalse();
    }

    @Test
    void losServiciosInactivosNoImpidenDesactivarLaCategoria() throws Exception {
        CategoriaServicio nueva = categoriaRepository.save(CategoriaServicio.builder().nombre("Solo Inactivos Prueba").build());
        servicioRepository.save(Servicio.builder().categoria(nueva).nombre("Servicio viejo Prueba")
                .duracionMinutos(10).precioBase(BigDecimal.TEN).activo(false).build());

        mockMvc.perform(delete(URL + "/" + nueva.getIdCategoria())).andExpect(status().isNoContent());
    }

    @Test
    void desactivarYReactivarDosVecesSeguidasNoDaError() throws Exception {
        CategoriaServicio vacia = categoriaRepository.save(CategoriaServicio.builder().nombre("Vacía Prueba").build());
        String url = URL + "/" + vacia.getIdCategoria();

        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        mockMvc.perform(patch(url + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(patch(url + "/reactivar")).andExpect(status().isOk());
        mockMvc.perform(delete(URL + "/999999")).andExpect(status().isNotFound());
    }

    // ---------- Permisos ----------

    @Test
    void soloElAdminPuedeModificarLasCategorias() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            mockMvc.perform(post(URL).with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nueva\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(URL + "/1").with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Nueva\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(URL + "/1").with(usuario)).andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/1/reactivar").with(usuario)).andExpect(status().isForbidden());
        }
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
