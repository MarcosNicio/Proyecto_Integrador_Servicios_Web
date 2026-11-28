package com.veterinaria.inventario.repository;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.inventario.entity.InventarioProducto;
import com.veterinaria.inventario.entity.MovimientoInventario;
import com.veterinaria.inventario.entity.TipoMovimiento;
import com.veterinaria.inventario.entity.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pruebas de las entidades y repositorios del modulo inventario contra la base PostgreSQL real.
// Cada prueba corre dentro de una transaccion que se revierte al finalizar para no dejar datos residuales.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventarioRepositoryTest {

    @Autowired
    private InventarioProductoRepository productoRepository;

    @Autowired
    private MovimientoInventarioRepository movimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioPrueba;
    private InventarioProducto vacuna;
    private InventarioProducto jeringa;
    private InventarioProducto collarInactivo;

    @BeforeEach
    void prepararDatos() {
        usuarioPrueba = usuarioRepository.saveAndFlush(Usuario.builder()
                .correo("inv.repo@veterinaria.com")
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(Rol.ADMIN)
                .build());

        vacuna = productoRepository.saveAndFlush(InventarioProducto.builder()
                .sku("REPO-VAC-001")
                .nombre("Vacuna Antirrabica Repo")
                .tipo(TipoProducto.MEDICAMENTO)
                .unidadMedida("dosis")
                .precioUnitario(new BigDecimal("220.00"))
                .stockActual(3)
                .stockMinimo(5)
                .activo(true)
                .build());

        jeringa = productoRepository.saveAndFlush(InventarioProducto.builder()
                .sku("REPO-INS-002")
                .nombre("Jeringa 5ml Repo")
                .tipo(TipoProducto.INSUMO_CLINICO)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("8.50"))
                .stockActual(25)
                .stockMinimo(10)
                .activo(true)
                .build());

        collarInactivo = productoRepository.saveAndFlush(InventarioProducto.builder()
                .sku("REPO-TND-003")
                .nombre("Collar Rojo Repo")
                .tipo(TipoProducto.TIENDA)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("150.00"))
                .stockActual(1)
                .stockMinimo(5)
                .activo(false)
                .build());
    }

    // ---------- INVENTARIO_PRODUCTOS ----------

    @Test
    void guardaProductoConDefaultsYValidaUnicidadDeSku() {
        InventarioProducto conDefaults = productoRepository.saveAndFlush(InventarioProducto.builder()
                .sku("REPO-DEF-004")
                .nombre("Gasas Esteriles Repo")
                .tipo(TipoProducto.INSUMO_CLINICO)
                .unidadMedida("paquete")
                .precioUnitario(new BigDecimal("35.00"))
                .stockActual(null)
                .stockMinimo(null)
                .activo(null)
                .build());

        assertThat(conDefaults.getIdProducto()).isNotNull();
        assertThat(conDefaults.getStockActual()).isZero();
        assertThat(conDefaults.getStockMinimo()).isEqualTo(5);
        assertThat(conDefaults.getActivo()).isTrue();
        assertThat(productoRepository.existsBySkuIgnoreCase("repo-def-004")).isTrue();
        assertThat(productoRepository.findBySkuIgnoreCase("repo-def-004")).isPresent();

        InventarioProducto duplicado = InventarioProducto.builder()
                .sku("REPO-VAC-001")
                .nombre("Duplicado")
                .tipo(TipoProducto.MEDICAMENTO)
                .unidadMedida("dosis")
                .precioUnitario(BigDecimal.TEN)
                .build();

        assertThatThrownBy(() -> productoRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaPrecioNegativo() {
        InventarioProducto precioNegativo = InventarioProducto.builder()
                .sku("REPO-ERR-1")
                .nombre("Precio Negativo")
                .tipo(TipoProducto.TIENDA)
                .unidadMedida("pieza")
                .precioUnitario(new BigDecimal("-1.00"))
                .build();

        assertThatThrownBy(() -> productoRepository.saveAndFlush(precioNegativo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaStockNegativo() {
        InventarioProducto stockNegativo = InventarioProducto.builder()
                .sku("REPO-ERR-2")
                .nombre("Stock Negativo")
                .tipo(TipoProducto.TIENDA)
                .unidadMedida("pieza")
                .precioUnitario(BigDecimal.TEN)
                .stockActual(-1)
                .build();

        assertThatThrownBy(() -> productoRepository.saveAndFlush(stockNegativo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtraProductosPorEstadoTipoYBusquedaEnNombreOSku() {
        List<InventarioProducto> soloActivos = productoRepository.buscarConFiltros(false, null, "%repo%");
        assertThat(soloActivos).extracting(InventarioProducto::getSku)
                .contains("REPO-VAC-001", "REPO-INS-002")
                .doesNotContain("REPO-TND-003");

        List<InventarioProducto> conInactivos = productoRepository.buscarConFiltros(true, null, "%repo%");
        assertThat(conInactivos).extracting(InventarioProducto::getSku)
                .contains("REPO-VAC-001", "REPO-INS-002", "REPO-TND-003");

        List<InventarioProducto> porTipo = productoRepository.buscarConFiltros(false, TipoProducto.MEDICAMENTO, "%repo%");
        assertThat(porTipo).extracting(InventarioProducto::getSku)
                .containsExactly("REPO-VAC-001");

        List<InventarioProducto> porSku = productoRepository.buscarConFiltros(false, null, "%ins-002%");
        assertThat(porSku).extracting(InventarioProducto::getSku)
                .containsExactly("REPO-INS-002");
    }

    @Test
    void encuentraProductosActivosConStockBajoExcluyendoInactivosYStockSuficiente() {
        InventarioProducto enElLimite = productoRepository.saveAndFlush(InventarioProducto.builder()
                .sku("REPO-LIM-005")
                .nombre("Alcohol 70 Repo")
                .tipo(TipoProducto.INSUMO_CLINICO)
                .unidadMedida("frasco")
                .precioUnitario(new BigDecimal("45.00"))
                .stockActual(5)
                .stockMinimo(5)
                .activo(true)
                .build());

        List<InventarioProducto> alertas = productoRepository.findProductosConStockBajo();

        assertThat(alertas).extracting(InventarioProducto::getIdProducto)
                .contains(vacuna.getIdProducto(), enElLimite.getIdProducto())
                .doesNotContain(jeringa.getIdProducto(), collarInactivo.getIdProducto());

        // Vacuna (stock 3) debe aparecer antes que Alcohol (stock 5)
        List<Integer> ids = alertas.stream().map(InventarioProducto::getIdProducto).toList();
        assertThat(ids.indexOf(vacuna.getIdProducto())).isLessThan(ids.indexOf(enElLimite.getIdProducto()));
    }

    @Test
    void findByIdForUpdateDevuelveElProductoConBloqueoPesimista() {
        InventarioProducto bloqueado = productoRepository.findByIdForUpdate(vacuna.getIdProducto()).orElseThrow();
        assertThat(bloqueado.getSku()).isEqualTo("REPO-VAC-001");
    }

    // ---------- MOVIMIENTOS_INVENTARIO ----------

    @Test
    void guardaMovimientosYConsultaKardexConFiltrosYOrdenDescendente() {
        LocalDateTime fecha1 = LocalDateTime.of(2026, 11, 10, 9, 0);
        LocalDateTime fecha2 = LocalDateTime.of(2026, 11, 15, 12, 0);
        LocalDateTime fecha3 = LocalDateTime.of(2026, 11, 20, 18, 0);

        MovimientoInventario m1 = guardarMovimiento(vacuna, TipoMovimiento.ENTRADA, 10, "Compra inicial", fecha1);
        MovimientoInventario m2 = guardarMovimiento(vacuna, TipoMovimiento.SALIDA_CONSULTA, 2, "Consulta #10", fecha2);
        MovimientoInventario m3 = guardarMovimiento(jeringa, TipoMovimiento.SALIDA_VENTA, 3, "Venta #5", fecha3);

        List<MovimientoInventario> historialVacuna = movimientoRepository
                .findByProductoIdProductoOrderByFechaMovimientoDescIdMovimientoDesc(vacuna.getIdProducto());
        assertThat(historialVacuna).extracting(MovimientoInventario::getIdMovimiento)
                .containsExactly(m2.getIdMovimiento(), m1.getIdMovimiento());
        assertThat(movimientoRepository.countByProductoIdProducto(vacuna.getIdProducto())).isEqualTo(2);

        List<MovimientoInventario> kardexFiltradoTipo = movimientoRepository.buscarKardex(
                vacuna.getIdProducto(), TipoMovimiento.SALIDA_CONSULTA, null, null);
        assertThat(kardexFiltradoTipo).extracting(MovimientoInventario::getIdMovimiento)
                .containsExactly(m2.getIdMovimiento());

        List<MovimientoInventario> kardexRango = movimientoRepository.buscarKardex(
                null, null, LocalDateTime.of(2026, 11, 12, 0, 0), LocalDateTime.of(2026, 11, 25, 0, 0));
        assertThat(kardexRango).extracting(MovimientoInventario::getIdMovimiento)
                .contains(m3.getIdMovimiento(), m2.getIdMovimiento())
                .doesNotContain(m1.getIdMovimiento());
    }

    @Test
    void laBaseRechazaMovimientoConCantidadCero() {
        assertThatThrownBy(() -> guardarMovimiento(
                vacuna, TipoMovimiento.AJUSTE, 0, "Invalido", LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- Ayudantes ----------

    private MovimientoInventario guardarMovimiento(
            InventarioProducto producto,
            TipoMovimiento tipo,
            int cantidad,
            String referencia,
            LocalDateTime fecha
    ) {
        return movimientoRepository.saveAndFlush(MovimientoInventario.builder()
                .producto(producto)
                .usuario(usuarioPrueba)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .referencia(referencia)
                .fechaMovimiento(fecha)
                .build());
    }
}
