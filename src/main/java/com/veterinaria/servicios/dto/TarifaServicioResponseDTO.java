package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de respuesta con el detalle de una regla tarifaria asociada a un servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de una tarifa diferenciada por especie y rango de peso")
public class TarifaServicioResponseDTO {

    // Identificador único de la tarifa
    @Schema(description = "Identificador único de la regla tarifaria", example = "10")
    private Integer idTarifa;

    // Identificador del servicio padre
    @Schema(description = "Identificador del servicio al que pertenece la tarifa", example = "1")
    private Integer idServicio;

    // Especie animal normalizada
    @Schema(description = "Especie animal a la que aplica la tarifa", example = "PERRO")
    private String especie;

    // Peso mínimo en kilogramos (este peso sí pertenece al rango)
    @Schema(description = "Límite inferior del rango de peso en kilogramos (incluido)", example = "0.00")
    private BigDecimal pesoMinKg;

    // Peso máximo en kilogramos (este peso no pertenece al rango: es el inicio del siguiente)
    @Schema(description = "Límite superior del rango de peso en kilogramos (no incluido)", example = "10.00")
    private BigDecimal pesoMaxKg;

    // Precio calculado para esta regla
    @Schema(description = "Precio aplicable cuando la mascota coincide con la especie y el rango de peso", example = "420.00")
    private BigDecimal precioCalculado;
}
