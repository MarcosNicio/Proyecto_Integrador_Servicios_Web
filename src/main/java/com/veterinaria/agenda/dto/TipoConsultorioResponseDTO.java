package com.veterinaria.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tipo de consultorio que devuelve la API, con el número de consultorios activos que tiene
 * (el frontend lo usa para saber si se puede desactivar).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tipo de consultorio")
public class TipoConsultorioResponseDTO {

    @Schema(description = "Identificador del tipo", example = "1")
    private Integer idTipoConsultorio;

    @Schema(description = "Nombre del tipo", example = "Consulta")
    private String nombre;

    @Schema(description = "Descripción", example = "Consultorio estándar para revisión general y vacunación", nullable = true)
    private String descripcion;

    @Schema(description = "Si es false, el tipo está desactivado y no se usa en servicios ni consultorios nuevos", example = "true")
    private Boolean activo;

    @Schema(description = "Cuántos consultorios activos tiene. Un tipo con consultorios activos no se puede desactivar", example = "2")
    private Long consultoriosActivos;
}
