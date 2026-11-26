package com.veterinaria.personal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Datos de un turno de trabajo semanal. Se usa para agregarlo y para editarlo.
 * <p>
 * Que {@code horaFin} sea posterior a {@code horaInicio} y que no se encime con otro turno lo
 * revisa el servicio, porque depende de los dos campos y de los turnos ya guardados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Turno de trabajo semanal")
public class HorarioPersonalRequestDTO {

    // Día de la semana: 0 = domingo ... 6 = sábado (misma numeración que HORARIOS_CLINICA)
    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 0, message = "El día de la semana debe estar entre 0 (domingo) y 6 (sábado)")
    @Max(value = 6, message = "El día de la semana debe estar entre 0 (domingo) y 6 (sábado)")
    @Schema(description = "Día de la semana: 0 = domingo, 1 = lunes ... 6 = sábado", example = "1")
    private Integer diaSemana;

    // Hora de inicio en formato HH:mm (si llegan segundos, se ignoran)
    @NotNull(message = "La hora de inicio es obligatoria")
    @Schema(description = "Hora de inicio (HH:mm)", type = "string", example = "08:00")
    private LocalTime horaInicio;

    // Hora de fin en formato HH:mm; debe ser posterior a la de inicio (el turno no cruza la medianoche)
    @NotNull(message = "La hora de fin es obligatoria")
    @Schema(description = "Hora de fin (HH:mm), posterior a la de inicio", type = "string", example = "14:00")
    private LocalTime horaFin;
}
