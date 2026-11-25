package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta con la información de una categoría de servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información detallada de una categoría de servicio")
public class CategoriaServicioResponseDTO {

    // Identificador único de la categoría
    @Schema(description = "Identificador único de la categoría de servicio", example = "1")
    private Integer idCategoria;

    // Nombre único de la categoría
    @Schema(description = "Nombre de la categoría de servicio", example = "Consulta General")
    private String nombre;

    // Descripción de la categoría
    @Schema(description = "Descripción de la categoría de servicio", example = "Valoraciones médicas generales y revisiones preventivas")
    private String descripcion;

    // Estado de la categoría en el sistema (true = activa, false = inactiva)
    @Schema(description = "Indica si la categoría se encuentra activa", example = "true")
    private Boolean activo;
}
