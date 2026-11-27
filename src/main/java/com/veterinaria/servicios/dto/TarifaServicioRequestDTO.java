package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de petición para registrar o actualizar una tarifa dinámica por especie y rango de peso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos requeridos para registrar o actualizar una tarifa dinámica de servicio")
public class TarifaServicioRequestDTO {

    // Especie animal a la que aplica la tarifa (obligatorio, máximo 50 caracteres)
    @NotBlank(message = "La especie es obligatoria")
    @Size(max = 50, message = "La especie no puede exceder los 50 caracteres")
    @Schema(description = "Especie animal a la que aplica la tarifa (se normaliza en servidor)", example = "PERRO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String especie;

    // Límite inferior del rango de peso en kilogramos (peso_min_kg >= 0.00)
    @NotNull(message = "El peso mínimo en kg es obligatorio")
    @DecimalMin(value = "0.00", message = "El peso mínimo no puede ser negativo")
    @Digits(integer = 3, fraction = 2, message = "El peso mínimo debe tener máximo 3 dígitos enteros y 2 decimales")
    @Schema(description = "Límite inferior inclusivo del rango de peso en kilogramos", example = "0.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal pesoMinKg;

    // Límite superior del rango de peso en kilogramos (debe ser estrictamente mayor a pesoMinKg)
    @NotNull(message = "El peso máximo en kg es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso máximo debe ser mayor a 0 kg")
    @Digits(integer = 3, fraction = 2, message = "El peso máximo debe tener máximo 3 dígitos enteros y 2 decimales")
    @Schema(description = "Límite superior inclusivo del rango de peso en kilogramos", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal pesoMaxKg;

    // Precio calculado para el rango de peso y especie indicados (mayor o igual a 0.00)
    @NotNull(message = "El precio calculado es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio calculado no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio calculado debe tener máximo 8 dígitos enteros y 2 decimales")
    @Schema(description = "Precio diferenciado aplicable para este rango y especie", example = "420.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precioCalculado;

    /**
     * Valida a nivel de DTO que el límite superior de peso sea estrictamente mayor al límite inferior.
     * Si alguno de los dos campos es nulo, retorna true para delegar la notificación a {@code @NotNull}
     * retorna true si el rango de peso es válido o si alguno de los valores aún está ausente
     */
    @AssertTrue(message = "El peso máximo debe ser estrictamente mayor al peso mínimo")
    @Schema(hidden = true)
    public boolean isRangoPesoValido() {
        if (pesoMinKg == null || pesoMaxKg == null) {
            return true;
        }
        return pesoMaxKg.compareTo(pesoMinKg) > 0;
    }
}
