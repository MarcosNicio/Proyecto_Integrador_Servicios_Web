package com.veterinaria.servicios.controller;

import com.veterinaria.agenda.entity.TipoConsultorio;
import com.veterinaria.agenda.repository.TipoConsultorioRepository;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
 * Pruebas del catálogo de servicios de {@link ServicioController} contra la base real
 * (las tarifas y la cotización están en {@code TarifaServicioControllerTest}).
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba lo creado. Cada prueba parte de una
 * categoría activa, "Laboratorio Prueba", con un servicio, y de un tipo de consultorio desactivado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ServicioControllerTest {

    private static final String URL = "/api/servicios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaServicioRepository categoriaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TipoConsultorioRepository tipoRepository;

    private CategoriaServicio laboratorio;
    private Servicio hemograma;
    private TipoConsultorio tipoDesactivado;

    @BeforeEach
    void crearDatosDePrueba() {
        laboratorio = categoriaRepository.save(CategoriaServicio.builder().nombre("Laboratorio Prueba").build());
        hemograma = servicioRepository.save(Servicio.builder()
                .categoria(laboratorio).nombre("Hemograma Prueba").duracionMinutos(15)
                .precioBase(new BigDecimal("180.00")).build());
        tipoDesactivado = tipoRepository.saveAndFlush(TipoConsultorio.builder()
                .nombre("Tipo Desactivado Prueba").activo(false).build());
    }

    // ---------- Crear ----------

    @Test
    void creaUnServicioActivoSinTarifas() throws Exception {
        enviar(post(URL), servicio("  Perfil Químico Prueba ", laboratorio.getIdCategoria(), 30, "250.50", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idServicio").isNumber())
                .andExpect(jsonPath("$.nombre").value("Perfil Químico Prueba"))
                .andExpect(jsonPath("$.nombreCategoria").value("Laboratorio Prueba"))
                .andExpect(jsonPath("$.duracionMinutos").value(30))
                .andExpect(jsonPath("$.precioBase").value(250.50))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.tarifas", hasSize(0)));
    }

    @Test
    void aceptaUnTipoDeConsultorioActivoYRechazaUnoInexistenteODesactivado() throws Exception {
        Integer idConsulta = tipoRepository.findByNombreIgnoreCase("Consulta").orElseThrow().getIdTipoConsultorio();
        Integer idCategoria = laboratorio.getIdCategoria();

        enviar(post(URL), servicio("Con tipo Prueba", idCategoria, 30, "100", idConsulta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoConsultorio").value(idConsulta));
        enviar(post(URL), servicio("Tipo inexistente Prueba", idCategoria, 30, "100", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró un tipo de consultorio activo con ID: 999999"));
        enviar(post(URL), servicio("Tipo desactivado Prueba", idCategoria, 30, "100", tipoDesactivado.getIdTipoConsultorio()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("tipo de consultorio activo")));
    }

    @Test
    void nombreRepetidoEnLaMismaCategoriaDevuelve409PeroEnOtraCategoriaSiSePermite() throws Exception {
        enviar(post(URL), servicio("  HEMOGRAMA prueba ", laboratorio.getIdCategoria(), 15, "180", null))
                .andExpect(status().isConflict());

        CategoriaServicio otra = categoriaRepository.save(CategoriaServicio.builder().nombre("Otra Categoría Prueba").build());
        enviar(post(URL), servicio("Hemograma Prueba", otra.getIdCategoria(), 15, "180", null))
                .andExpect(status().isCreated());
    }

    @Test
    void categoriaInexistenteDevuelve404YCategoriaDesactivadaDevuelve400() throws Exception {
        enviar(post(URL), servicio("Nuevo Prueba", 999999, 30, "100", null))
                .andExpect(status().isNotFound());

        CategoriaServicio inactiva = categoriaRepository.save(
                CategoriaServicio.builder().nombre("Inactiva Prueba").activo(false).build());
        enviar(post(URL), servicio("Nuevo Prueba", inactiva.getIdCategoria(), 30, "100", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La categoría de servicio con ID " + inactiva.getIdCategoria() + " se encuentra inactiva"));
    }

    @Test
    void datosInvalidosDevuelven400ConErrorPorCampo() throws Exception {
        Integer idCategoria = laboratorio.getIdCategoria();

        enviar(post(URL), servicio("Duración cero Prueba", idCategoria, 0, "100", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.duracionMinutos").value("La duración debe ser mayor a 0 minutos"));
        enviar(post(URL), servicio("Duración larga Prueba", idCategoria, 1441, "100", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.duracionMinutos").value("La duración no puede exceder los 1440 minutos"));
        enviar(post(URL), servicio("Precio negativo Prueba", idCategoria, 30, "-5", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precioBase").value("El precio base no puede ser negativo"));
        enviar(post(URL), servicio("Tres decimales Prueba", idCategoria, 30, "10.123", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precioBase").exists());
        enviar(post(URL), "{\"duracionMinutos\":30,\"precioBase\":100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.idCategoria").exists());
    }

    // ---------- Consultar ----------

    @Test
    void listaConFiltrosPorCategoriaNombreEInactivos() throws Exception {
        servicioRepository.save(Servicio.builder().categoria(laboratorio).nombre("Coproparasitoscópico Prueba")
                .duracionMinutos(20).precioBase(new BigDecimal("120")).activo(false).build());
        String idCategoria = laboratorio.getIdCategoria().toString();

        mockMvc.perform(get(URL).param("idCategoria", idCategoria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("Hemograma Prueba")));
        mockMvc.perform(get(URL).param("idCategoria", idCategoria).param("incluirInactivos", "true"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(URL).param("nombre", "hemograma"))
                .andExpect(jsonPath("$[*].nombre", contains("Hemograma Prueba")));
        mockMvc.perform(get(URL).param("idCategoria", "999999")).andExpect(status().isNotFound());
    }

    @Test
    void obtienePorIdYDa404SiNoExiste() throws Exception {
        mockMvc.perform(get(URL + "/" + hemograma.getIdServicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hemograma Prueba"))
                .andExpect(jsonPath("$.nombreCategoria").value("Laboratorio Prueba"));
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el servicio con ID: 999999"));
    }

    @Test
    void cualquierRolConSesionPuedeConsultarYSinSesionDa401() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            mockMvc.perform(get(URL).with(user("alguien@correo.com").roles(rol))).andExpect(status().isOk());
            mockMvc.perform(get(URL + "/" + hemograma.getIdServicio()).with(user("alguien@correo.com").roles(rol)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Editar ----------

    @Test
    void editaUnServicioYPuedeConservarSuNombre() throws Exception {
        String url = URL + "/" + hemograma.getIdServicio();

        enviar(put(url), servicio("hemograma prueba", laboratorio.getIdCategoria(), 25, "199.99", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("hemograma prueba"))
                .andExpect(jsonPath("$.duracionMinutos").value(25))
                .andExpect(jsonPath("$.precioBase").value(199.99));
    }

    @Test
    void editarHaciaElNombreDeOtroServicioODeUnTipoDesactivadoFalla() throws Exception {
        servicioRepository.save(Servicio.builder().categoria(laboratorio).nombre("Urianálisis Prueba")
                .duracionMinutos(10).precioBase(new BigDecimal("90")).build());
        String url = URL + "/" + hemograma.getIdServicio();

        enviar(put(url), servicio("Urianálisis Prueba", laboratorio.getIdCategoria(), 15, "180", null))
                .andExpect(status().isConflict());
        enviar(put(url), servicio("Hemograma Prueba", laboratorio.getIdCategoria(), 15, "180",
                tipoDesactivado.getIdTipoConsultorio()))
                .andExpect(status().isNotFound());
        enviar(put(URL + "/999999"), servicio("Lo que sea Prueba", laboratorio.getIdCategoria(), 15, "180", null))
                .andExpect(status().isNotFound());
    }

    // ---------- Desactivar y reactivar ----------

    @Test
    void desactivarYReactivarCambiaElEstadoYNoDaErrorSiSeRepite() throws Exception {
        String url = URL + "/" + hemograma.getIdServicio();

        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        mockMvc.perform(delete(url)).andExpect(status().isNoContent());
        assertThat(servicioRepository.findById(hemograma.getIdServicio()).orElseThrow().getActivo()).isFalse();

        mockMvc.perform(patch(url + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(delete(URL + "/999999")).andExpect(status().isNotFound());
    }

    @Test
    void noSeReactivaUnServicioCuyaCategoriaEstaInactiva() throws Exception {
        Servicio viejo = servicioRepository.save(Servicio.builder()
                .categoria(categoriaRepository.save(CategoriaServicio.builder().nombre("Cerrada Prueba").activo(false).build()))
                .nombre("Servicio cerrado Prueba").duracionMinutos(10).precioBase(BigDecimal.TEN).activo(false).build());

        mockMvc.perform(patch(URL + "/" + viejo.getIdServicio() + "/reactivar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede reactivar el servicio porque su categoría "
                        + "'Cerrada Prueba' se encuentra inactiva"));
    }

    // ---------- Permisos ----------

    @Test
    void soloElAdminPuedeModificarElCatalogo() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            String cuerpo = servicio("Nuevo Prueba", laboratorio.getIdCategoria(), 30, "100", null);
            mockMvc.perform(post(URL).with(usuario).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(URL + "/" + hemograma.getIdServicio()).with(usuario)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(URL + "/" + hemograma.getIdServicio()).with(usuario)).andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/" + hemograma.getIdServicio() + "/reactivar").with(usuario))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String servicio(String nombre, Integer idCategoria, int duracion, String precio, Integer idTipoConsultorio) {
        String tipo = idTipoConsultorio == null ? "null" : idTipoConsultorio.toString();
        return "{\"idCategoria\":%d,\"nombre\":\"%s\",\"duracionMinutos\":%d,\"precioBase\":%s,\"idTipoConsultorio\":%s}"
                .formatted(idCategoria, nombre, duracion, precio, tipo);
    }
}
