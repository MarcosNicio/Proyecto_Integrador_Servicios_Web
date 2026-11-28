package com.veterinaria.inventario.dto;

import com.veterinaria.inventario.entity.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// DTO de respuesta que representa un registro del Kardex de inventario junto con su contexto de producto y usuario
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro auditable de un movimiento de inventario (Kardex)")
public class MovimientoInventarioResponseDTO {

    // Identificador unico del movimiento
    @Schema(description = "Identificador unico del movimiento", example = "15")
    private Integer idMovimiento;

    // Identificador del producto afectado
    @Schema(description = "Identificador del producto afectado", example = "1")
    private Integer idProducto;

    // Codigo SKU del producto afectado
    @Schema(description = "Codigo SKU del producto", example = "VAC-RAB-001")
    private String skuProducto;

    // Nombre del producto afectado
    @Schema(description = "Nombre descriptivo del producto", example = "Vacuna antirrabica 1 ml")
    private String nombreProducto;

    // Identificador del usuario responsable del movimiento
    @Schema(description = "Identificador del usuario que registro el movimiento", example = "1")
    private Integer idUsuario;

    // Correo del usuario responsable del movimiento
    @Schema(description = "Correo del usuario que registro el movimiento", example = "admin@veterinaria.com")
    private String correoUsuario;

    // Tipo de movimiento ejecutado
    @Schema(description = "Tipo de movimiento ejecutado", example = "ENTRADA")
    private TipoMovimiento tipoMovimiento;

    // Cantidad registrada en el movimiento
    @Schema(description = "Cantidad aplicada en el movimiento", example = "10")
    private Integer cantidad;

    // Referencia o motivo opcional del movimiento
    @Schema(description = "Referencia o motivo del movimiento", example = "Compra proveedor factura #450")
    private String referencia;

    // Fecha y hora en que se registro el movimiento
    @Schema(description = "Fecha y hora exacta del movimiento", example = "2026-11-27T18:30:00")
    private LocalDateTime fechaMovimiento;

    // Stock actual del producto en el catalogo al momento de consultar o registrar el movimiento
    @Schema(description = "Existencias actuales del producto tras el movimiento", example = "30")
    private Integer stockActualProducto;
}
