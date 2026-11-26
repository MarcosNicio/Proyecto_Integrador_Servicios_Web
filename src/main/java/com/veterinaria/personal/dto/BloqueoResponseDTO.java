package com.veterinaria.personal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.veterinaria.personal.entity.MotivoBloqueo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bloqueo de agenda que devuelve la API. Las fechas salen siempre como {@code "yyyy-MM-ddTHH:mm"}.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Periodo en que un empleado no está disponible")
public class BloqueoResponseDTO {

    @Schema(description = "Identificador del bloqueo", example = "3")
    private Integer idBloqueo;

    @Schema(description = "Empleado al que pertenece", example = "1")
    private Integer idPersonal;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Schema(description = "Inicio del bloqueo", type = "string", example = "2026-12-22T00:00")
    private LocalDateTime fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Schema(description = "Fin del bloqueo", type = "string", example = "2026-12-27T00:00")
    private LocalDateTime fechaFin;

    @Schema(description = "Motivo del bloqueo", example = "PERSONAL")
    private MotivoBloqueo motivo;

    @Schema(description = "Detalle opcional", example = "Vacaciones de fin de año", nullable = true)
    private String notas;
}
