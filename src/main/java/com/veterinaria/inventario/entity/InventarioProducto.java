package com.veterinaria.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Entidad JPA que representa la tabla INVENTARIO_PRODUCTOS.

// Modela el catalogo maestro de medicamentos, insumos clinicos y articulos de tienda con control de stock minimo.
// Los productos se manejan con baja logica (activo = false) para conservar el historial de movimientos, vacunas y cobros.
@Entity
@Table(name = "INVENTARIO_PRODUCTOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioProducto {

    // Identificador unico del producto (llave primaria autoincremental)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    // Codigo SKU unico del articulo, normalizado en mayusculas
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    // Nombre descriptivo del producto
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    // Clasificacion del producto (MEDICAMENTO, INSUMO_CLINICO o TIENDA)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoProducto tipo;

    // Unidad de medida para el control de existencias (pieza, dosis, frasco, caja, ml)
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private String unidadMedida;

    // Precio unitario vigente del producto (mayor o igual a 0)
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // Existencias disponibles actuales (mayor o igual a 0); solo se modifica mediante movimientos
    @Column(name = "stock_actual", nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    // Umbral minimo de existencias para disparar alertas de reposicion (mayor o igual a 0)
    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 5;

    // Bandera de estado para baja logica (true = activo)
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Asigna valores por defecto antes de insertar porque Hibernate envia todas las columnas en el INSERT
    @PrePersist
    void prePersist() {
        if (stockActual == null) {
            stockActual = 0;
        }
        if (stockMinimo == null) {
            stockMinimo = 5;
        }
        if (activo == null) {
            activo = true;
        }
    }
}
