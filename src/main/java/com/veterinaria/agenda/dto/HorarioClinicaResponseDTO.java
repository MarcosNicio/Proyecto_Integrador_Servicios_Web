package com.veterinaria.agenda.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Horario de la clínica de un día que devuelve la API. Las horas salen siempre como
 * {@code "HH:mm"} e incluye el nombre del día para que el frontend no tenga que convertir el número.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Horario de atención de la clínica en un día de la semana")
public class HorarioClinicaResponseDTO {

    @Schema(description = "Día de la semana: 0 = domingo ... 6 = sábado", example = "1")
    private Integer diaSemana;

    @Schema(description = "Nombre del día", example = "Lunes")
    private String nombreDia;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de apertura", type = "string", example = "08:00")
    private LocalTime horaApertura;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de cierre", type = "string", example = "20:00")
    private LocalTime horaCierre;

    @Schema(description = "Si es false, la clínica no abre ese día", example = "true")
    private Boolean laborable;
}
