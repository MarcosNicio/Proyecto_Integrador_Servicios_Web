package com.veterinaria.inventario.controller;

import com.veterinaria.inventario.entity.InventarioProducto;
import com.veterinaria.inventario.entity.MovimientoInventario;
import com.veterinaria.inventario.entity.TipoMovimiento;
import com.veterinaria.inventario.entity.TipoProducto;
import com.veterinaria.inventario.repository.InventarioProductoRepository;
import com.veterinaria.inventario.repository.MovimientoInventarioRepository;
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

// Pruebas de integracion del catalogo de productos y alertas de stock bajo de InventarioController.
// Ejecutan contra la base PostgreSQL real con rollback automatico por prueba (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin@veterinaria.com", roles = "ADMIN")
class InventarioProductoControllerTest {

    private static final String URL = "/api/inventario/productos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventarioProductoRepository productoRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    private InventarioProducto vacunaActiva;
    private InventarioProducto jeringaStockAlto;
    private InventarioProducto collarInactivo;

    @BeforeEach
    void prepararDatos() {
        vacunaActiva = productoRepository.save(InventarioProducto.builder()
                .sku("TST-VAC-001")
                .nombre("Vacuna Triple Prueba")
                .tipo(TipoProducto.MEDICAMENTO)
                .unidadMedida("dosis")
                .precioUnitario(new BigDecimal("250.00"))
                .stockActual(3)
                .stockMinimo(5)
                .activo(true)
                .build());

        jeringaStockAlto = productoRepository.save(InventarioProducto.builder()
                .sku("TST-INS-002")
                .nombre("Jeringa 3ml Prueba")
                .tipo(TipoProducto.INSUMO_CLINICO)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("6.50"))
                .stockActual(30)
                .stockMinimo(10)
                .activo(true)
                .build());

        collarInactivo = productoRepository.save(InventarioProducto.builder()
                .sku("TST-TND-003")
                .nombre("Collar Azul Prueba")
                .tipo(TipoProducto.TIENDA)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("180.00"))
                .stockActual(0)
                .stockMinimo(5)
                .activo(false)
                .build());
    }

    // ---------- Crear ----------

    @Test
    void creaProductoSinStockInicialConDefaultsYSinGenerarMovimiento() throws Exception {
        String cuerpo = """
                {
                  "sku": "  tst-med-010  ",
                  "nombre": "  Antibiotico Oral Prueba ",
                  "tipo": "MEDICAMENTO",
                  "unidadMedida": "  frasco ",
                  "precioUnitario": 145.50
                }
                """;

        enviar(post(URL), cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProducto").isNumber())
                .andExpect(jsonPath("$.sku").value("TST-MED-010"))
                .andExpect(jsonPath("$.nombre").value("Antibiotico Oral Prueba"))
                .andExpect(jsonPath("$.tipo").value("MEDICAMENTO"))
                .andExpect(jsonPath("$.unidadMedida").value("frasco"))
                .andExpect(jsonPath("$.precioUnitario").value(145.50))
                .andExpect(jsonPath("$.stockActual").value(0))
                .andExpect(jsonPath("$.stockMinimo").value(5))
                .andExpect(jsonPath("$.bajoStock").value(true))
                .andExpect(jsonPath("$.activo").value(true));

        InventarioProducto guardado = productoRepository.findBySkuIgnoreCase("TST-MED-010").orElseThrow();
        assertThat(movimientoRepository.countByProductoIdProducto(guardado.getIdProducto())).isZero();
    }

    @Test
    void creaProductoConStockInicialYRegistraEntradaAutomaticaEnKardex() throws Exception {
        String cuerpo = """
                {
                  "sku": "tst-tnd-020",
                  "nombre": "Croquetas Premium 5kg Prueba",
                  "tipo": "TIENDA",
                  "unidadMedida": "bolsa",
                  "precioUnitario": 620.00,
                  "stockInicial": 12,
                  "stockMinimo": 4
                }
                """;

        enviar(post(URL), cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("TST-TND-020"))
                .andExpect(jsonPath("$.stockActual").value(12))
                .andExpect(jsonPath("$.stockMinimo").value(4))
                .andExpect(jsonPath("$.bajoStock").value(false));

        InventarioProducto guardado = productoRepository.findBySkuIgnoreCase("TST-TND-020").orElseThrow();
        List<MovimientoInventario> movimientos = movimientoRepository
                .findByProductoIdProductoOrderByFechaMovimientoDescIdMovimientoDesc(guardado.getIdProducto());

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).getTipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movimientos.get(0).getCantidad()).isEqualTo(12);
        assertThat(movimientos.get(0).getReferencia()).isEqualTo("Stock inicial");
        assertThat(movimientos.get(0).getUsuario().getCorreo()).isEqualTo("admin@veterinaria.com");
    }

    @Test
    void skuRepetidoDevuelve409AunqueCambienMayusculasOEspacios() throws Exception {
        String cuerpo = """
                {
                  "sku": "  tst-vac-001 ",
                  "nombre": "Otra Vacuna",
                  "tipo": "MEDICAMENTO",
                  "unidadMedida": "dosis",
                  "precioUnitario": 200.00
                }
                """;

        enviar(post(URL), cuerpo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un producto registrado con el SKU: TST-VAC-001"));
    }

    @Test
    void datosInvalidosAlCrearDevuelven400ConErroresPorCampo() throws Exception {
        String cuerpoInvalido = """
                {
                  "sku": " ",
                  "nombre": "",
                  "unidadMedida": "",
                  "precioUnitario": -10.555,
                  "stockInicial": -2,
                  "stockMinimo": -1
                }
                """;

        enviar(post(URL), cuerpoInvalido)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.tipo").exists())
                .andExpect(jsonPath("$.errors.unidadMedida").exists())
                .andExpect(jsonPath("$.errors.precioUnitario").exists())
                .andExpect(jsonPath("$.errors.stockInicial").exists())
                .andExpect(jsonPath("$.errors.stockMinimo").exists());
    }

    // ---------- Consultar y alertas ----------

    @Test
    void listaProductosConFiltrosPorEstadoTipoYBusqueda() throws Exception {
        mockMvc.perform(get(URL).param("busqueda", "Prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem("TST-VAC-001")))
                .andExpect(jsonPath("$[*].sku", hasItem("TST-INS-002")))
                .andExpect(jsonPath("$[*].sku", not(hasItem("TST-TND-003"))));

        mockMvc.perform(get(URL).param("busqueda", "Prueba").param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem("TST-TND-003")));

        mockMvc.perform(get(URL).param("tipo", "INSUMO_CLINICO").param("busqueda", "TST-INS-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", contains("TST-INS-002")));
    }

    @Test
    void listaAlertasDeStockBajoExcluyendoInactivosYStockSuficiente() throws Exception {
        mockMvc.perform(get(URL + "/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem("TST-VAC-001")))
                .andExpect(jsonPath("$[*].sku", not(hasItem("TST-INS-002"))))
                .andExpect(jsonPath("$[*].sku", not(hasItem("TST-TND-003"))));
    }

    @Test
    void obtieneProductoPorIdYDevuelve404SiNoExiste() throws Exception {
        mockMvc.perform(get(URL + "/" + vacunaActiva.getIdProducto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TST-VAC-001"))
                .andExpect(jsonPath("$.bajoStock").value(true));

        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el producto de inventario con ID: 999999"));
    }

    // ---------- Actualizar ----------

    @Test
    void actualizaProductoConservandoSuSkuYSinAlterarStockActual() throws Exception {
        String cuerpo = """
                {
                  "sku": " tst-vac-001 ",
                  "nombre": "Vacuna Triple Felina Prueba",
                  "tipo": "MEDICAMENTO",
                  "unidadMedida": "frasco",
                  "precioUnitario": 280.00,
                  "stockMinimo": 2
                }
                """;

        enviar(put(URL + "/" + vacunaActiva.getIdProducto()), cuerpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TST-VAC-001"))
                .andExpect(jsonPath("$.nombre").value("Vacuna Triple Felina Prueba"))
                .andExpect(jsonPath("$.precioUnitario").value(280.00))
                .andExpect(jsonPath("$.stockActual").value(3))
                .andExpect(jsonPath("$.stockMinimo").value(2))
                .andExpect(jsonPath("$.bajoStock").value(false));
    }

    @Test
    void actualizarHaciaSkuDeOtroProductoDevuelve409YProductoInexistente404() throws Exception {
        String cuerpoConSkuAjeno = """
                {
                  "sku": "TST-INS-002",
                  "nombre": "Choque SKU",
                  "tipo": "MEDICAMENTO",
                  "unidadMedida": "dosis",
                  "precioUnitario": 100.00,
                  "stockMinimo": 5
                }
                """;

        enviar(put(URL + "/" + vacunaActiva.getIdProducto()), cuerpoConSkuAjeno)
                .andExpect(status().isConflict());

        enviar(put(URL + "/999999"), cuerpoConSkuAjeno)
                .andExpect(status().isNotFound());
    }

    // ---------- Desactivar y reactivar ----------

    @Test
    void desactivarYReactivarSonIdempotentes() throws Exception {
        String urlProducto = URL + "/" + jeringaStockAlto.getIdProducto();

        mockMvc.perform(delete(urlProducto)).andExpect(status().isNoContent());
        mockMvc.perform(delete(urlProducto)).andExpect(status().isNoContent());
        assertThat(productoRepository.findById(jeringaStockAlto.getIdProducto()).orElseThrow().getActivo()).isFalse();

        mockMvc.perform(patch(urlProducto + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(patch(urlProducto + "/reactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(delete(URL + "/999999")).andExpect(status().isNotFound());
        mockMvc.perform(patch(URL + "/999999/reactivar")).andExpect(status().isNotFound());
    }

    // ---------- Permisos por rol ----------

    @Test
    void veterinarioYCajeroConsultanCatalogoPeroNoModificanYClienteRecibe403EnTodo() throws Exception {
        String cuerpoAlta = """
                {
                  "sku": "TST-PERM-999",
                  "nombre": "Producto Permiso",
                  "tipo": "TIENDA",
                  "unidadMedida": "pieza",
                  "precioUnitario": 50.00
                }
                """;
        String cuerpoEdicion = """
                {
                  "sku": "TST-VAC-001",
                  "nombre": "Editado",
                  "tipo": "MEDICAMENTO",
                  "unidadMedida": "dosis",
                  "precioUnitario": 250.00,
                  "stockMinimo": 5
                }
                """;

        for (String rol : List.of("VETERINARIO", "CAJERO")) {
            var empleado = user("empleado@veterinaria.com").roles(rol);
            mockMvc.perform(get(URL).with(empleado)).andExpect(status().isOk());
            mockMvc.perform(get(URL + "/stock-bajo").with(empleado)).andExpect(status().isOk());
            mockMvc.perform(get(URL + "/" + vacunaActiva.getIdProducto()).with(empleado)).andExpect(status().isOk());

            mockMvc.perform(post(URL).with(empleado).contentType(MediaType.APPLICATION_JSON).content(cuerpoAlta))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(URL + "/" + vacunaActiva.getIdProducto()).with(empleado)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoEdicion))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(URL + "/" + vacunaActiva.getIdProducto()).with(empleado))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch(URL + "/" + vacunaActiva.getIdProducto() + "/reactivar").with(empleado))
                    .andExpect(status().isForbidden());
        }

        var cliente = user("cliente@correo.com").roles("CLIENTE");
        mockMvc.perform(get(URL).with(cliente)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/stock-bajo").with(cliente)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/" + vacunaActiva.getIdProducto()).with(cliente)).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(cliente).contentType(MediaType.APPLICATION_JSON).content(cuerpoAlta))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
