package com.veterinaria.inventario.dto;

import com.veterinaria.inventario.entity.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// DTO de peticion para editar los datos de catalogo de un producto existente
// No incluye stockActual porque las existencias solo se modifican mediante movimientos auditables
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos permitidos para actualizar un producto existente sin alterar su stock directamente")
public class ProductoUpdateDTO {

    // Codigo SKU unico del producto (obligatorio, maximo 50 caracteres)
    @NotBlank(message = "El SKU del producto es obligatorio")
    @Size(max = 50, message = "El SKU no puede exceder los 50 caracteres")
    @Schema(description = "Codigo SKU unico del articulo (se normaliza en mayusculas)", example = "VAC-RAB-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    // Nombre descriptivo del producto (obligatorio, maximo 120 caracteres)
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 120, message = "El nombre del producto no puede exceder los 120 caracteres")
    @Schema(description = "Nombre descriptivo del producto", example = "Vacuna antirrabica canina 1 ml", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    // Clasificacion del producto (MEDICAMENTO, INSUMO_CLINICO o TIENDA)
    @NotNull(message = "El tipo de producto es obligatorio")
    @Schema(description = "Tipo de producto: MEDICAMENTO, INSUMO_CLINICO o TIENDA", example = "MEDICAMENTO", requiredMode = Schema.RequiredMode.REQUIRED)
    private TipoProducto tipo;

    // Unidad de medida del producto (obligatorio, maximo 30 caracteres)
    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30, message = "La unidad de medida no puede exceder los 30 caracteres")
    @Schema(description = "Unidad de medida del producto (pieza, dosis, frasco, caja, ml)", example = "dosis", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unidadMedida;

    // Precio unitario actualizado del producto (mayor o igual a 0.00)
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio unitario debe tener maximo 8 digitos enteros y 2 decimales")
    @Schema(description = "Precio unitario vigente del producto", example = "240.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precioUnitario;

    // Umbral minimo de existencias para alertas de reposicion (obligatorio, mayor o igual a 0)
    @NotNull(message = "El stock minimo es obligatorio")
    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    @Schema(description = "Umbral minimo de stock para alertas de reposicion", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stockMinimo;
}
