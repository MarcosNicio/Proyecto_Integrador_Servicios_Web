package com.veterinaria.inventario.repository;

import com.veterinaria.inventario.entity.MovimientoInventario;
import com.veterinaria.inventario.entity.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// Repositorio Spring Data JPA para la bitacora de movimientos de inventario (MOVIMIENTOS_INVENTARIO).
// Aprovecha el indice idx_movimientos_producto (id_producto, fecha_movimiento).
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {

    // Consulta el historial de movimientos de un producto ordenado del mas reciente al mas antiguo
    List<MovimientoInventario> findByProductoIdProductoOrderByFechaMovimientoDescIdMovimientoDesc(Integer idProducto);

    // Cuenta el total de movimientos registrados para un producto especifico
    long countByProductoIdProducto(Integer idProducto);

    // Consulta el Kardex general o filtrado por producto, tipo de movimiento y rango de fechas
    // Carga producto y usuario en la misma consulta para evitar consultas N+1 al armar los DTOs
    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.producto p
            JOIN FETCH m.usuario u
            WHERE (:idProducto IS NULL OR p.idProducto = :idProducto)
              AND (:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento)
              AND (:desde IS NULL OR m.fechaMovimiento >= :desde)
              AND (:hasta IS NULL OR m.fechaMovimiento <= :hasta)
            ORDER BY m.fechaMovimiento DESC, m.idMovimiento DESC
            """)
    List<MovimientoInventario> buscarKardex(
            @Param("idProducto") Integer idProducto,
            @Param("tipoMovimiento") TipoMovimiento tipoMovimiento,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
