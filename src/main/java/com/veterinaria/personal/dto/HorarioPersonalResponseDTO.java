package com.veterinaria.personal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Turno de trabajo que devuelve la API. Las horas salen siempre como {@code "HH:mm"} e incluye
 * el nombre del día para que el frontend no tenga que convertir el número.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Turno de trabajo semanal de un empleado")
public class HorarioPersonalResponseDTO {

    @Schema(description = "Identificador del turno", example = "5")
    private Integer idHorario;

    @Schema(description = "Empleado al que pertenece", example = "1")
    private Integer idPersonal;

    @Schema(description = "Día de la semana: 0 = domingo ... 6 = sábado", example = "1")
    private Integer diaSemana;

    @Schema(description = "Nombre del día", example = "Lunes")
    private String nombreDia;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de inicio", type = "string", example = "08:00")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de fin", type = "string", example = "14:00")
    private LocalTime horaFin;
}
