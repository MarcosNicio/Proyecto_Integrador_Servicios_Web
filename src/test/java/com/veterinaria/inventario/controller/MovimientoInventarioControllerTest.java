package com.veterinaria.inventario.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.inventario.entity.InventarioProducto;
import com.veterinaria.inventario.entity.TipoProducto;
import com.veterinaria.inventario.repository.InventarioProductoRepository;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de integracion de movimientos de stock y consulta del Kardex de InventarioController.
// Verifica reglas de negocio, control de existencias, filtros del Kardex y permisos por endpoint separado.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin@veterinaria.com", roles = "ADMIN")
class MovimientoInventarioControllerTest {

    private static final String URL_MOVIMIENTOS = "/api/inventario/movimientos";
    private static final String URL_PRODUCTOS = "/api/inventario/productos";

    private static final String CORREO_VET = "vet.inv.test@veterinaria.com";
    private static final String CORREO_CAJERO = "cajero.inv.test@veterinaria.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventarioProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private InventarioProducto productoActivo;
    private InventarioProducto productoInactivo;

    @BeforeEach
    void prepararDatos() {
        usuarioRepository.save(Usuario.builder()
                .correo(CORREO_VET)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(Rol.VETERINARIO)
                .build());

        usuarioRepository.save(Usuario.builder()
                .correo(CORREO_CAJERO)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(Rol.CAJERO)
                .build());

        productoActivo = productoRepository.save(InventarioProducto.builder()
                .sku("MOV-VAC-001")
                .nombre("Vacuna Quíntuple Prueba")
                .tipo(TipoProducto.MEDICAMENTO)
                .unidadMedida("dosis")
                .precioUnitario(new BigDecimal("310.00"))
                .stockActual(10)
                .stockMinimo(5)
                .activo(true)
                .build());

        productoInactivo = productoRepository.save(InventarioProducto.builder()
                .sku("MOV-INA-002")
                .nombre("Producto Descontinuado Prueba")
                .tipo(TipoProducto.TIENDA)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("95.00"))
                .stockActual(8)
                .stockMinimo(2)
                .activo(false)
                .build());
    }

    // ---------- Entradas y ajustes (ADMIN) ----------

    @Test
    void adminRegistraEntradaEIncrementaStockConAuditoria() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(productoActivo.getIdProducto(), 15, "Factura #450"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMovimiento").isNumber())
                .andExpect(jsonPath("$.idProducto").value(productoActivo.getIdProducto()))
                .andExpect(jsonPath("$.skuProducto").value("MOV-VAC-001"))
                .andExpect(jsonPath("$.correoUsuario").value("admin@veterinaria.com"))
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$.cantidad").value(15))
                .andExpect(jsonPath("$.referencia").value("Factura #450"))
                .andExpect(jsonPath("$.stockActualProducto").value(25));

        assertThat(productoRepository.findById(productoActivo.getIdProducto()).orElseThrow().getStockActual())
                .isEqualTo(25);
    }

    @Test
    void adminRegistraAjustePositivoYNegativo() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/ajustes"), movimiento(productoActivo.getIdProducto(), 4, "Sobrante conteo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("AJUSTE"))
                .andExpect(jsonPath("$.cantidad").value(4))
                .andExpect(jsonPath("$.stockActualProducto").value(14));

        enviar(post(URL_MOVIMIENTOS + "/ajustes"), movimiento(productoActivo.getIdProducto(), -6, "Merma por caducidad"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("AJUSTE"))
                .andExpect(jsonPath("$.cantidad").value(-6))
                .andExpect(jsonPath("$.stockActualProducto").value(8));

        assertThat(productoRepository.findById(productoActivo.getIdProducto()).orElseThrow().getStockActual())
                .isEqualTo(8);
    }

    // ---------- Salidas por consulta y venta ----------

    @Test
    void veterinarioYAdminRegistranSalidaConsultaYDescuentanStock() throws Exception {
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-consulta")
                        .with(user(CORREO_VET).roles("VETERINARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(productoActivo.getIdProducto(), 2, "Consulta #101")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA_CONSULTA"))
                .andExpect(jsonPath("$.correoUsuario").value(CORREO_VET))
                .andExpect(jsonPath("$.stockActualProducto").value(8));

        enviar(post(URL_MOVIMIENTOS + "/salidas-consulta"),
                movimiento(productoActivo.getIdProducto(), 3, "Consulta #102"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoUsuario").value("admin@veterinaria.com"))
                .andExpect(jsonPath("$.stockActualProducto").value(5));
    }

    @Test
    void cajeroYAdminRegistranSalidaVentaYDescuentanStock() throws Exception {
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-venta")
                        .with(user(CORREO_CAJERO).roles("CAJERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento(productoActivo.getIdProducto(), 4, "Orden #55")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA_VENTA"))
                .andExpect(jsonPath("$.correoUsuario").value(CORREO_CAJERO))
                .andExpect(jsonPath("$.stockActualProducto").value(6));

        enviar(post(URL_MOVIMIENTOS + "/salidas-venta"),
                movimiento(productoActivo.getIdProducto(), 1, "Orden #56"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockActualProducto").value(5));
    }

    // ---------- Validaciones de negocio y stock ----------

    @Test
    void rechazaCantidadCeroONegativaEnEntradasYSalidasCon400() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(productoActivo.getIdProducto(), 0, "Cero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La cantidad del movimiento debe ser distinta de 0"));

        enviar(post(URL_MOVIMIENTOS + "/ajustes"), movimiento(productoActivo.getIdProducto(), 0, "Cero"))
                .andExpect(status().isBadRequest());

        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(productoActivo.getIdProducto(), -5, "Negativa"))
                .andExpect(status().isBadRequest());

        enviar(post(URL_MOVIMIENTOS + "/salidas-consulta"), movimiento(productoActivo.getIdProducto(), -2, "Negativa"))
                .andExpect(status().isBadRequest());

        enviar(post(URL_MOVIMIENTOS + "/salidas-venta"), movimiento(productoActivo.getIdProducto(), -1, "Negativa"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaDescuentoQueDejariaStockNegativoCon409YSinAlterarExistencias() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/salidas-consulta"), movimiento(productoActivo.getIdProducto(), 11, "Exceso"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Stock insuficiente")));

        enviar(post(URL_MOVIMIENTOS + "/salidas-venta"), movimiento(productoActivo.getIdProducto(), 15, "Exceso"))
                .andExpect(status().isConflict());

        enviar(post(URL_MOVIMIENTOS + "/ajustes"), movimiento(productoActivo.getIdProducto(), -11, "Merma excesiva"))
                .andExpect(status().isConflict());

        assertThat(productoRepository.findById(productoActivo.getIdProducto()).orElseThrow().getStockActual())
                .isEqualTo(10);
    }

    @Test
    void rechazaMovimientoEnProductoInactivoCon400YEnInexistenteCon404() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(productoInactivo.getIdProducto(), 5, "Reabasto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("producto inactivo")));

        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(999999, 5, "No existe"))
                .andExpect(status().isNotFound());
    }

    // ---------- Consulta de Kardex ----------

    @Test
    void consultaKardexGeneralYPorProductoConFiltrosYValidaFechas() throws Exception {
        enviar(post(URL_MOVIMIENTOS + "/entradas"), movimiento(productoActivo.getIdProducto(), 5, "Compra 1"))
                .andExpect(status().isCreated());
        enviar(post(URL_MOVIMIENTOS + "/salidas-consulta"), movimiento(productoActivo.getIdProducto(), 2, "Consulta 1"))
                .andExpect(status().isCreated());

        // Kardex del producto: mas reciente primero
        mockMvc.perform(get(URL_PRODUCTOS + "/" + productoActivo.getIdProducto() + "/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tipoMovimiento", contains("SALIDA_CONSULTA", "ENTRADA")));

        // Filtrado por tipo
        mockMvc.perform(get(URL_PRODUCTOS + "/" + productoActivo.getIdProducto() + "/movimientos")
                        .param("tipoMovimiento", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referencia").value("Compra 1"));

        // Kardex general filtrado por producto y rango de fechas valido
        LocalDateTime ahora = LocalDateTime.now();
        mockMvc.perform(get(URL_MOVIMIENTOS)
                        .param("idProducto", productoActivo.getIdProducto().toString())
                        .param("desde", ahora.minusMinutes(5).toString())
                        .param("hasta", ahora.plusMinutes(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Rango invalido (desde posterior a hasta) -> 400
        mockMvc.perform(get(URL_MOVIMIENTOS)
                        .param("desde", ahora.plusHours(1).toString())
                        .param("hasta", ahora.minusHours(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la fecha final"));

        // Producto inexistente -> 404
        mockMvc.perform(get(URL_PRODUCTOS + "/999999/movimientos")).andExpect(status().isNotFound());
        mockMvc.perform(get(URL_MOVIMIENTOS).param("idProducto", "999999")).andExpect(status().isNotFound());
    }

    // ---------- Permisos por rol en endpoints separados ----------

    @Test
    void cadaRolSoloAccedeASusEndpointsDeMovimientosYClienteRecibe403() throws Exception {
        String cuerpo = movimiento(productoActivo.getIdProducto(), 1, "Prueba permisos");

        // VETERINARIO: no puede entradas, ajustes ni salidas-venta; si puede consultar Kardex
        var vet = user(CORREO_VET).roles("VETERINARIO");
        mockMvc.perform(post(URL_MOVIMIENTOS + "/entradas").with(vet).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/ajustes").with(vet).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-venta").with(vet).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL_MOVIMIENTOS).with(vet)).andExpect(status().isOk());
        mockMvc.perform(get(URL_PRODUCTOS + "/" + productoActivo.getIdProducto() + "/movimientos").with(vet))
                .andExpect(status().isOk());

        // CAJERO: no puede entradas, ajustes ni salidas-consulta; si puede consultar Kardex
        var cajero = user(CORREO_CAJERO).roles("CAJERO");
        mockMvc.perform(post(URL_MOVIMIENTOS + "/entradas").with(cajero).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/ajustes").with(cajero).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-consulta").with(cajero).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL_MOVIMIENTOS).with(cajero)).andExpect(status().isOk());
        mockMvc.perform(get(URL_PRODUCTOS + "/" + productoActivo.getIdProducto() + "/movimientos").with(cajero))
                .andExpect(status().isOk());

        // CLIENTE: bloqueado en todo
        var cliente = user("cliente@correo.com").roles("CLIENTE");
        mockMvc.perform(post(URL_MOVIMIENTOS + "/entradas").with(cliente).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/ajustes").with(cliente).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-consulta").with(cliente).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL_MOVIMIENTOS + "/salidas-venta").with(cliente).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL_MOVIMIENTOS).with(cliente)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL_PRODUCTOS + "/" + productoActivo.getIdProducto() + "/movimientos").with(cliente))
                .andExpect(status().isForbidden());

        // Sin sesion -> 401
        mockMvc.perform(get(URL_MOVIMIENTOS).with(anonymous())).andExpect(status().isUnauthorized());
    }

    // ---------- Ayudantes ----------

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String cuerpo) throws Exception {
        return mockMvc.perform(metodo.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String movimiento(Integer idProducto, int cantidad, String referencia) {
        return "{\"idProducto\":%d,\"cantidad\":%d,\"referencia\":\"%s\"}"
                .formatted(idProducto, cantidad, referencia);
    }
}
