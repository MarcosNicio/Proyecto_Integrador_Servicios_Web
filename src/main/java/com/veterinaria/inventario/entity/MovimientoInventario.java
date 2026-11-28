package com.veterinaria.inventario.entity;

import com.veterinaria.auth.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Entidad JPA que representa la tabla MOVIMIENTOS_INVENTARIO.
// Funciona como bitacora inmutable (Kardex) de cada entrada, salida o ajuste aplicado a un producto.
// La base de datos valida mediante chk_movimiento_cantidad que la cantidad sea distinta de 0.
@Entity
@Table(name = "MOVIMIENTOS_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"producto", "usuario"})
public class MovimientoInventario {

    // Identificador unico del movimiento (llave primaria autoincremental)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    // Producto de inventario afectado por el movimiento
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private InventarioProducto producto;

    // Usuario autenticado responsable de registrar el movimiento
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Naturaleza del movimiento (ENTRADA, SALIDA_CONSULTA, SALIDA_VENTA o AJUSTE)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private TipoMovimiento tipoMovimiento;

    // Cantidad del movimiento (siempre distinta de 0); positiva en entradas y salidas, con signo en ajustes
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    // Referencia o motivo opcional del movimiento (por ejemplo: Stock inicial, Consulta #45, Factura #120)
    @Column(name = "referencia", length = 120)
    private String referencia;

    // Fecha y hora exacta en que se registro el movimiento; no se modifica despues de crearse
    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    // Asigna automaticamente la fecha y hora actual antes de guardar si no se indico
    @PrePersist
    void prePersist() {
        if (fechaMovimiento == null) {
            fechaMovimiento = LocalDateTime.now();
        }
    }
}
