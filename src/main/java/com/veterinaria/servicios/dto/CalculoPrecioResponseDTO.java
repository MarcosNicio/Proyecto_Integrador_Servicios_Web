package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


 // Indica si se aplicó una tarifa diferenciada o si se utilizó el precio base como fallback.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resultado del cálculo dinámico de precio para un servicio según especie y peso")
public class CalculoPrecioResponseDTO {

    // Identificador del servicio cotizado
    @Schema(description = "Identificador del servicio cotizado", example = "1")
    private Integer idServicio;

    // Nombre del servicio cotizado
    @Schema(description = "Nombre del servicio cotizado", example = "Baño y corte de pelo")
    private String nombreServicio;

    // Especie consultada (normalizada)
    @Schema(description = "Especie consultada para la cotización", example = "PERRO")
    private String especie;

    // Peso consultado en kilogramos
    @Schema(description = "Peso del paciente utilizado para el cálculo en kilogramos", example = "12.50")
    private BigDecimal pesoKg;

    // Precio base configurado en el servicio
    @Schema(description = "Precio base original del servicio", example = "350.00")
    private BigDecimal precioBase;

    // Precio final resultante (tarifa dinámica o precio base en fallback)
    @Schema(description = "Precio final cotizado para el servicio", example = "480.00")
    private BigDecimal precioFinal;

    // Bandera que indica si aplicó una tarifa dinámica (true) o el precio base como fallback (false)
    @Schema(description = "Indica si se aplicó una tarifa diferenciada (true) o el precio base por defecto (false)", example = "true")
    private Boolean tarifaAplicada;

    // Identificador de la tarifa aplicada (null cuando tarifaAplicada es false)
    @Schema(description = "Identificador de la regla tarifaria aplicada (null si se usó precio base)", example = "12")
    private Integer idTarifaAplicada;

    // Representación legible del rango aplicado (null cuando se usó precio base)
    @Schema(description = "Rango de peso de la tarifa aplicada (null si se usó precio base)", example = "10.01 kg - 25.00 kg")
    private String rangoAplicado;
}
