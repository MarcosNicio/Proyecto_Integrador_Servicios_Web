package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de petición para registrar o actualizar un servicio del catálogo veterinario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos requeridos para registrar o actualizar un servicio del catálogo")
public class ServicioRequestDTO {

    // Identificador de la categoría a la que pertenece el servicio (obligatorio, positivo)
    @NotNull(message = "El ID de la categoría es obligatorio")
    @Positive(message = "El ID de la categoría debe ser un número positivo")
    @Schema(description = "Identificador de la categoría del servicio", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idCategoria;

    // Identificador opcional del tipo de consultorio requerido para prestar el servicio
    @Positive(message = "El ID del tipo de consultorio debe ser un número positivo")
    @Schema(description = "Identificador opcional del tipo de consultorio requerido", example = "1")
    private Integer idTipoConsultorio;

    // Nombre descriptivo del servicio (obligatorio, máximo 100 caracteres)
    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 100, message = "El nombre del servicio no puede exceder los 100 caracteres")
    @Schema(description = "Nombre del servicio", example = "Baño y corte de pelo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    // Descripción detallada opcional del servicio (máximo 2000 caracteres)
    @Size(max = 2000, message = "La descripción del servicio no puede exceder los 2000 caracteres")
    @Schema(description = "Descripción detallada del servicio", example = "Incluye baño medicado o cosmético, secado, corte de uñas y limpieza de oídos")
    private String descripcion;

    // Duración estimada en minutos (obligatorio, mayor a 0 y máximo 1440 minutos)
    @NotNull(message = "La duración en minutos es obligatoria")
    @Positive(message = "La duración debe ser mayor a 0 minutos")
    @Max(value = 1440, message = "La duración no puede exceder los 1440 minutos")
    @Schema(description = "Duración estimada del servicio en minutos", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer duracionMinutos;

    // Precio base del servicio utilizado por defecto o como fallback (mayor o igual a 0.00)
    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio base no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio base debe tener máximo 8 dígitos enteros y 2 decimales")
    @Schema(description = "Precio base del servicio en moneda local (fallback si no aplica tarifa dinámica)", example = "350.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal precioBase;
}
