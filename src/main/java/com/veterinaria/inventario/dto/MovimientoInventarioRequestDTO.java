package com.veterinaria.inventario.dto;

import com.veterinaria.inventario.entity.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO de peticion para registrar un movimiento auditable de inventario (entrada, salida o ajuste)
// El usuario responsable se resuelve desde el token JWT y no se recibe en el cuerpo de la peticion
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos requeridos para registrar un movimiento de stock en el Kardex")
public class MovimientoInventarioRequestDTO {

    // Identificador del producto afectado por el movimiento (obligatorio, positivo)
    @NotNull(message = "El ID del producto es obligatorio")
    @Positive(message = "El ID del producto debe ser un numero positivo")
    @Schema(description = "Identificador del producto de inventario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idProducto;

    // Tipo de movimiento a registrar (ENTRADA, SALIDA_CONSULTA, SALIDA_VENTA o AJUSTE)
    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Schema(description = "Tipo de movimiento: ENTRADA, SALIDA_CONSULTA, SALIDA_VENTA o AJUSTE", example = "ENTRADA", requiredMode = Schema.RequiredMode.REQUIRED)
    private TipoMovimiento tipoMovimiento;

    // Cantidad a aplicar (siempre distinta de 0; mayor a 0 en entradas/salidas y con signo en ajustes)
    @NotNull(message = "La cantidad es obligatoria")
    @Schema(description = "Cantidad del movimiento (mayor a 0 para ENTRADA, SALIDA_CONSULTA y SALIDA_VENTA; positiva o negativa distinta de 0 para AJUSTE)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer cantidad;

    // Referencia o motivo opcional del movimiento (maximo 120 caracteres)
    @Size(max = 120, message = "La referencia no puede exceder los 120 caracteres")
    @Schema(description = "Referencia o motivo opcional del movimiento", example = "Compra proveedor factura #450")
    private String referencia;
}
