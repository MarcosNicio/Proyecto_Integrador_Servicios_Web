package com.veterinaria.servicios.controller;

import com.veterinaria.servicios.entity.CategoriaServicio;
import com.veterinaria.servicios.entity.Servicio;
import com.veterinaria.servicios.repository.CategoriaServicioRepository;
import com.veterinaria.servicios.repository.ServicioRepository;
import com.veterinaria.servicios.repository.TarifaServicioRepository;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de las tarifas por especie y peso, y de la cotización, de {@link ServicioController}
 * contra la base real.
 * <p>
 * Los rangos de peso incluyen su límite inferior pero no el superior: {@code [mín, máx)}. Por eso
 * 0-10 y 10-25 no se traslapan y un peso de exactamente 10 kg cuenta en el segundo.
 * <p>
 * {@code @WithMockUser} simula al usuario que hace la petición (por defecto un ADMIN).
 * {@code @Transactional} deshace al final de cada prueba lo creado. Cada prueba parte de un
 * servicio con precio base de $300.50.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class TarifaServicioControllerTest {

    private static final String PRECIO_BASE = "300.5";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaServicioRepository categoriaRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TarifaServicioRepository tarifaRepository;

    private Servicio vacuna;
    private String urlTarifas;

    @BeforeEach
    void crearServicioDePrueba() {
        CategoriaServicio categoria = categoriaRepository.save(CategoriaServicio.builder().nombre("Vacunas Prueba").build());
        vacuna = servicioRepository.save(Servicio.builder().categoria(categoria).nombre("Vacuna Prueba")
                .duracionMinutos(15).precioBase(new BigDecimal("300.50")).build());
        urlTarifas = "/api/servicios/" + vacuna.getIdServicio() + "/tarifas";
    }

    // ---------- Crear tarifas ----------

    @Test
    void creaUnaTarifaConLaEspecieNormalizada() throws Exception {
        enviar(post(urlTarifas), tarifa("  perro ", "0", "10", "210"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTarifa").isNumber())
                .andExpect(jsonPath("$.idServicio").value(vacuna.getIdServicio()))
                .andExpect(jsonPath("$.especie").value("PERRO"))
                .andExpect(jsonPath("$.pesoMinKg").value(0.0))
                .andExpect(jsonPath("$.pesoMaxKg").value(10.0))
                .andExpect(jsonPath("$.precioCalculado").value(210.0));
    }

    @Test
    void losRangosContiguosNoSeTraslapanElLimiteSuperiorEsDelRangoSiguiente() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Perro", "10", "25", "350")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Perro", "25", "100", "500")).andExpect(status().isCreated());
    }

    @Test
    void unRangoQueSeEncimaConOtroDeLaMismaEspecieDevuelve409() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());

        enviar(post(urlTarifas), tarifa("PERRO", "5", "15", "1"))        // empieza dentro
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("se traslapa")));
        enviar(post(urlTarifas), tarifa("perro", "-0", "5", "1"))        // termina dentro
                .andExpect(status().isConflict());
        enviar(post(urlTarifas), tarifa("Perro", "2", "8", "1"))         // queda dentro
                .andExpect(status().isConflict());
        enviar(post(urlTarifas), tarifa("Perro", "0", "30", "1"))        // lo cubre por completo
                .andExpect(status().isConflict());
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "1"))        // es el mismo rango
                .andExpect(status().isConflict());
    }

    @Test
    void elMismoRangoEnOtraEspecieOEnOtroServicioSiSePermite() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Gato", "0", "10", "180")).andExpect(status().isCreated());

        Servicio otro = servicioRepository.save(Servicio.builder().categoria(vacuna.getCategoria())
                .nombre("Otra Vacuna Prueba").duracionMinutos(15).precioBase(BigDecimal.TEN).build());
        enviar(post("/api/servicios/" + otro.getIdServicio() + "/tarifas"), tarifa("Perro", "0", "10", "99"))
                .andExpect(status().isCreated());
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "5", "5", "100"))              // máximo igual al mínimo
                .andExpect(status().isBadRequest());
        enviar(post(urlTarifas), tarifa("Perro", "8", "2", "100"))              // máximo menor al mínimo
                .andExpect(status().isBadRequest());
        enviar(post(urlTarifas), tarifa("Perro", "-1", "2", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pesoMinKg").value("El peso mínimo no puede ser negativo"));
        enviar(post(urlTarifas), tarifa("Perro", "0", "2", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precioCalculado").value("El precio calculado no puede ser negativo"));
        enviar(post(urlTarifas), tarifa("Perro", "0", "2.123", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pesoMaxKg").exists());
        enviar(post(urlTarifas), tarifa("   ", "0", "2", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.especie").value("La especie es obligatoria"));
    }

    @Test
    void servicioInexistenteDevuelve404() throws Exception {
        enviar(post("/api/servicios/999999/tarifas"), tarifa("Perro", "0", "10", "210")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/servicios/999999/tarifas")).andExpect(status().isNotFound());
    }

    // ---------- Consultar, editar y borrar ----------

    @Test
    void listaLasTarifasOrdenadasYFiltraPorEspecieSinImportarMayusculas() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "10", "25", "350")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Gato", "0", "10", "180")).andExpect(status().isCreated());

        mockMvc.perform(get(urlTarifas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].especie", contains("GATO", "PERRO", "PERRO")))
                .andExpect(jsonPath("$[1].pesoMinKg").value(0.0))
                .andExpect(jsonPath("$[2].pesoMinKg").value(10.0));
        mockMvc.perform(get(urlTarifas).param("especie", "perro"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].pesoMinKg").value(0.0));
        mockMvc.perform(get("/api/servicios/" + vacuna.getIdServicio()))
                .andExpect(jsonPath("$.tarifas", hasSize(3)));
    }

    @Test
    void editaUnaTarifaSinChocarConsigoMismaPeroNoHaciaOtraTarifa() throws Exception {
        Integer id = idDe(enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")));
        enviar(post(urlTarifas), tarifa("Perro", "10", "25", "350")).andExpect(status().isCreated());
        String url = urlTarifas + "/" + id;

        enviar(put(url), tarifa("Perro", "0", "10", "230"))             // solo cambia el precio
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioCalculado").value(230.0));
        enviar(put(url), tarifa("Perro", "0", "9", "230"))              // se acorta: sigue sin chocar
                .andExpect(status().isOk());
        enviar(put(url), tarifa("Perro", "0", "12", "230"))             // invade la de 10-25
                .andExpect(status().isConflict());
    }

    @Test
    void editarOBorrarUnaTarifaDeOtroServicioODeUnaTarifaInexistenteDevuelve404() throws Exception {
        Servicio otro = servicioRepository.save(Servicio.builder().categoria(vacuna.getCategoria())
                .nombre("Otra Vacuna Prueba").duracionMinutos(15).precioBase(BigDecimal.TEN).build());
        Integer idDeOtro = idDe(enviar(post("/api/servicios/" + otro.getIdServicio() + "/tarifas"),
                tarifa("Perro", "0", "10", "99")));

        enviar(put(urlTarifas + "/" + idDeOtro), tarifa("Perro", "0", "10", "1")).andExpect(status().isNotFound());
        mockMvc.perform(delete(urlTarifas + "/" + idDeOtro)).andExpect(status().isNotFound());
        mockMvc.perform(delete(urlTarifas + "/999999")).andExpect(status().isNotFound());
        assertThat(tarifaRepository.findById(idDeOtro)).isPresent();
    }

    @Test
    void borraUnaTarifaYYaNoSeAplica() throws Exception {
        Integer id = idDe(enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")));

        mockMvc.perform(delete(urlTarifas + "/" + id)).andExpect(status().isNoContent());
        assertThat(tarifaRepository.findById(id)).isEmpty();
        mockMvc.perform(delete(urlTarifas + "/" + id)).andExpect(status().isNotFound());
        cotizar("Perro", "5").andExpect(jsonPath("$.tarifaAplicada").value(false));
    }

    // ---------- Cotización ----------

    @Test
    void cotizaConLaTarifaQueCorrespondeAlPesoYUsaElPrecioBaseSiNingunaLoCubre() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());
        Integer idSegunda = idDe(enviar(post(urlTarifas), tarifa("Perro", "10", "25", "350")));

        cotizar("Perro", "5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioFinal").value(210.0))
                .andExpect(jsonPath("$.tarifaAplicada").value(true))
                .andExpect(jsonPath("$.precioBase").value(300.5))
                .andExpect(jsonPath("$.especie").value("PERRO"))
                .andExpect(jsonPath("$.nombreServicio").value("Vacuna Prueba"));
        cotizar("Perro", "15")
                .andExpect(jsonPath("$.precioFinal").value(350.0))
                .andExpect(jsonPath("$.idTarifaAplicada").value(idSegunda))
                .andExpect(jsonPath("$.rangoAplicado", containsString("25")));
        cotizar("Perro", "40")          // ningún rango: precio base
                .andExpect(jsonPath("$.precioFinal").value(300.5))
                .andExpect(jsonPath("$.tarifaAplicada").value(false))
                .andExpect(jsonPath("$.idTarifaAplicada").doesNotExist())
                .andExpect(jsonPath("$.rangoAplicado").doesNotExist());
        cotizar("Hámster", "1")         // especie sin tarifas: precio base
                .andExpect(jsonPath("$.precioFinal").value(300.5))
                .andExpect(jsonPath("$.tarifaAplicada").value(false));
    }

    @Test
    void enElLimiteDeDosRangosElPesoCuentaEnElDeArribaYElMaximoDelUltimoRangoNoEntra() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());
        enviar(post(urlTarifas), tarifa("Perro", "10", "25", "350")).andExpect(status().isCreated());

        cotizar("Perro", "0.01").andExpect(jsonPath("$.precioFinal").value(210.0));
        cotizar("Perro", "9.99").andExpect(jsonPath("$.precioFinal").value(210.0));
        cotizar("Perro", "9.999").andExpect(jsonPath("$.precioFinal").value(210.0));
        cotizar("Perro", "10").andExpect(jsonPath("$.precioFinal").value(350.0));        // el 10 es del segundo
        cotizar("Perro", "10.00").andExpect(jsonPath("$.precioFinal").value(350.0));
        cotizar("Perro", "24.99").andExpect(jsonPath("$.precioFinal").value(350.0));
        cotizar("Perro", "25").andExpect(jsonPath("$.precioFinal").value(300.5));        // el 25 ya no entra
    }

    @Test
    void laEspecieSeComparaSinImportarMayusculasNiEspacios() throws Exception {
        enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")).andExpect(status().isCreated());

        cotizar("perro", "5").andExpect(jsonPath("$.precioFinal").value(210.0));
        cotizar("PERRO", "5").andExpect(jsonPath("$.precioFinal").value(210.0));
        cotizar(" Perro ", "5").andExpect(jsonPath("$.precioFinal").value(210.0));
    }

    @Test
    void unPesoCeroNegativoOUnaEspecieFaltanteDevuelven400() throws Exception {
        cotizar("Perro", "0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El peso en kilogramos debe ser mayor a 0 para calcular el precio"));
        cotizar("Perro", "-3").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/servicios/" + vacuna.getIdServicio() + "/calcular-precio").param("pesoKg", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'especie'"));
        cotizar("   ", "5").andExpect(status().isBadRequest());
    }

    @Test
    void noSeCotizaUnServicioInexistenteNiUnoDesactivado() throws Exception {
        mockMvc.perform(get("/api/servicios/999999/calcular-precio").param("especie", "Perro").param("pesoKg", "5"))
                .andExpect(status().isNotFound());

        vacuna.setActivo(false);
        servicioRepository.saveAndFlush(vacuna);
        cotizar("Perro", "5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("se encuentra inactivo")));
    }

    // ---------- Permisos ----------

    @Test
    void cualquierRolConSesionPuedeConsultarYCotizarPeroSinSesionDa401() throws Exception {
        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            mockMvc.perform(get(urlTarifas).with(usuario)).andExpect(status().isOk());
            mockMvc.perform(get("/api/servicios/" + vacuna.getIdServicio() + "/calcular-precio")
                            .param("especie", "Perro").param("pesoKg", "5").with(usuario))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get(urlTarifas).with(anonymous())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/servicios/" + vacuna.getIdServicio() + "/calcular-precio")
                        .param("especie", "Perro").param("pesoKg", "5").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElAdminPuedeModificarLasTarifas() throws Exception {
        Integer id = idDe(enviar(post(urlTarifas), tarifa("Perro", "0", "10", "210")));

        for (String rol : List.of("VETERINARIO", "CAJERO", "CLIENTE")) {
            var usuario = user("alguien@correo.com").roles(rol);
            mockMvc.perform(post(urlTarifas).with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content(tarifa("Gato", "0", "10", "1")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(urlTarifas + "/" + id).with(usuario).contentType(MediaType.APPLICATION_JSON)
                            .content(tarifa("Perro", "0", "10", "1")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(urlTarifas + "/" + id).with(usuario)).andExpect(status().isForbidden());
        }
        assertThat(tarifaRepository.findById(id)).isPresent();
    }

    // ---------- Ayudantes ----------

    private ResultActions cotizar(String especie, String peso) throws Exception {
        return mockMvc.perform(get("/api/servicios/" + vacuna.getIdServicio() + "/calcular-precio")
                .param("especie", especie).param("pesoKg", peso));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String tarifa(String especie, String min, String max, String precio) {
        return "{\"especie\":\"%s\",\"pesoMinKg\":%s,\"pesoMaxKg\":%s,\"precioCalculado\":%s}"
                .formatted(especie, min, max, precio);
    }

    private Integer idDe(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Integer.valueOf(json.replaceAll(".*\"idTarifa\":(\\d+).*", "$1"));
    }
}
