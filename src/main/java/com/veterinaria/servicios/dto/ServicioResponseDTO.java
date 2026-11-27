package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta con la información completa de un servicio y sus tarifas dinámicas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información completa de un servicio veterinario y sus reglas tarifarias")
public class ServicioResponseDTO {

    // Identificador único del servicio
    @Schema(description = "Identificador único del servicio", example = "1")
    private Integer idServicio;

    // Identificador de la categoría asociada
    @Schema(description = "Identificador de la categoría a la que pertenece", example = "3")
    private Integer idCategoria;

    // Nombre descriptivo de la categoría asociada
    @Schema(description = "Nombre de la categoría asociada", example = "Estética")
    private String nombreCategoria;

    // Identificador opcional del tipo de consultorio requerido
    @Schema(description = "Identificador del tipo de consultorio requerido (puede ser null)", example = "3")
    private Integer idTipoConsultorio;

    // Nombre del servicio
    @Schema(description = "Nombre del servicio", example = "Baño y corte de pelo")
    private String nombre;

    // Descripción detallada del servicio
    @Schema(description = "Descripción detallada del servicio", example = "Incluye baño, secado, corte de uñas y limpieza de oídos")
    private String descripcion;

    // Duración estimada en minutos
    @Schema(description = "Duración estimada del servicio en minutos", example = "45")
    private Integer duracionMinutos;

    // Precio base del servicio
    @Schema(description = "Precio base del servicio (utilizado como fallback)", example = "350.00")
    private BigDecimal precioBase;

    // Estado del servicio en el catálogo (true = activo, false = baja lógica)
    @Schema(description = "Indica si el servicio está activo en el catálogo", example = "true")
    private Boolean activo;

    // Lista de tarifas dinámicas asociadas al servicio
    @Schema(description = "Lista de tarifas diferenciadas por especie y rango de peso")
    private List<TarifaServicioResponseDTO> tarifas;
}
