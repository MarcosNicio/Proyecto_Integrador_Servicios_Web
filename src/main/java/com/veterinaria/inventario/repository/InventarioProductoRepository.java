package com.veterinaria.inventario.repository;

import com.veterinaria.inventario.entity.InventarioProducto;
import com.veterinaria.inventario.entity.TipoProducto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Repositorio Spring Data JPA para el catalogo de productos (INVENTARIO_PRODUCTOS).
public interface InventarioProductoRepository extends JpaRepository<InventarioProducto, Integer> {

    // Verifica si ya existe un producto con el SKU indicado sin distinguir mayusculas
    boolean existsBySkuIgnoreCase(String sku);

    // Verifica si existe otro producto con el mismo SKU excluyendo el ID del producto en edicion
    boolean existsBySkuIgnoreCaseAndIdProductoNot(String sku, Integer idProducto);

    // Busca un producto por su codigo SKU sin distinguir mayusculas
    Optional<InventarioProducto> findBySkuIgnoreCase(String sku);

    // Lista todos los productos activos ordenados alfabeticamente por nombre
    List<InventarioProducto> findByActivoTrueOrderByNombreAsc();

    // Lista todos los productos (activos e inactivos) ordenados alfabeticamente por nombre
    List<InventarioProducto> findAllByOrderByNombreAsc();

    // Busca productos aplicando filtros opcionales de estado, tipo y patron de busqueda sobre nombre o SKU
    // El parametro patron debe enviarse en minusculas con comodines (por ejemplo "%vacuna%") o null si no aplica
    @Query("""
            SELECT p FROM InventarioProducto p
            WHERE (:incluirInactivos = true OR p.activo = true)
              AND (:tipo IS NULL OR p.tipo = :tipo)
              AND (:patron IS NULL OR LOWER(p.nombre) LIKE :patron OR LOWER(p.sku) LIKE :patron)
            ORDER BY p.nombre ASC, p.idProducto ASC
            """)
    List<InventarioProducto> buscarConFiltros(
            @Param("incluirInactivos") boolean incluirInactivos,
            @Param("tipo") TipoProducto tipo,
            @Param("patron") String patron
    );

    // Lista los productos activos cuyo stock actual es menor o igual a su stock minimo configurado
    @Query("""
            SELECT p FROM InventarioProducto p
            WHERE p.activo = true
              AND p.stockActual <= p.stockMinimo
            ORDER BY p.stockActual ASC, p.nombre ASC
            """)
    List<InventarioProducto> findProductosConStockBajo();

    // Obtiene un producto aplicando bloqueo pesimista de escritura para actualizar stock sin condiciones de carrera
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InventarioProducto p WHERE p.idProducto = :id")
    Optional<InventarioProducto> findByIdForUpdate(@Param("id") Integer id);
}
