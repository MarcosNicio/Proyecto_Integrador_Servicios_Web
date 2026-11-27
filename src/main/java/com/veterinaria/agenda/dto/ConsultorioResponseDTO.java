package com.veterinaria.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Consultorio que devuelve la API, con el nombre de su tipo para que el frontend no tenga que
 * consultarlo aparte.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Consultorio o espacio físico de la clínica")
public class ConsultorioResponseDTO {

    @Schema(description = "Identificador del consultorio", example = "5")
    private Integer idConsultorio;

    @Schema(description = "Nombre del consultorio", example = "Consultorio 3")
    private String nombre;

    @Schema(description = "Si es false, no se ofrece para citas nuevas", example = "true")
    private Boolean activo;

    @Schema(description = "Identificador de su tipo", example = "1")
    private Integer idTipoConsultorio;

    @Schema(description = "Nombre de su tipo", example = "Consulta")
    private String nombreTipo;
}
