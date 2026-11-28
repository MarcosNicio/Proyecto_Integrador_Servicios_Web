package com.veterinaria.inventario.dto;

import com.veterinaria.inventario.entity.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// DTO de respuesta con el detalle completo de un producto del inventario y su indicador de stock bajo
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Informacion completa de un producto del inventario")
public class ProductoResponseDTO {

    // Identificador unico del producto
    @Schema(description = "Identificador unico del producto", example = "1")
    private Integer idProducto;

    // Codigo SKU normalizado del producto
    @Schema(description = "Codigo SKU unico del articulo", example = "VAC-RAB-001")
    private String sku;

    // Nombre descriptivo del producto
    @Schema(description = "Nombre descriptivo del producto", example = "Vacuna antirrabica 1 ml")
    private String nombre;

    // Clasificacion del producto
    @Schema(description = "Tipo de producto", example = "MEDICAMENTO")
    private TipoProducto tipo;

    // Unidad de medida del producto
    @Schema(description = "Unidad de medida del producto", example = "dosis")
    private String unidadMedida;

    // Precio unitario vigente
    @Schema(description = "Precio unitario vigente del producto", example = "220.00")
    private BigDecimal precioUnitario;

    // Existencias actuales disponibles
    @Schema(description = "Existencias disponibles actuales", example = "20")
    private Integer stockActual;

    // Umbral minimo configurado para alertas
    @Schema(description = "Stock minimo configurado para alertas de reposicion", example = "5")
    private Integer stockMinimo;

    // Indica si el stock actual es menor o igual al stock minimo configurado
    @Schema(description = "True cuando stockActual es menor o igual a stockMinimo", example = "false")
    private Boolean bajoStock;

    // Estado del producto en el catalogo (true = activo, false = baja logica)
    @Schema(description = "Indica si el producto esta activo en el catalogo", example = "true")
    private Boolean activo;
}
