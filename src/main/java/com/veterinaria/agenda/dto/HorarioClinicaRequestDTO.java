package com.veterinaria.agenda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * Horario de la clínica de un día de la semana. {@code PUT /api/config/horarios-clinica} recibe
 * una lista con los 7 días.
 * <p>
 * Ignora los campos desconocidos, así el frontend puede reenviar tal cual lo que devolvió el
 * {@code GET} (que incluye {@code nombreDia}). Como la lista no se valida automáticamente,
 * el servicio aplica estas anotaciones con el validador y luego revisa las reglas entre días.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Horario de atención de la clínica en un día de la semana")
public class HorarioClinicaRequestDTO {

    // Día de la semana: 0 = domingo ... 6 = sábado (misma numeración que los turnos del personal)
    @NotNull(message = "El día de la semana es obligatorio")
    @Min(value = 0, message = "El día de la semana debe estar entre 0 (domingo) y 6 (sábado)")
    @Max(value = 6, message = "El día de la semana debe estar entre 0 (domingo) y 6 (sábado)")
    @Schema(description = "Día de la semana: 0 = domingo, 1 = lunes ... 6 = sábado", example = "1")
    private Integer diaSemana;

    // Hora de apertura en formato HH:mm (si llegan segundos, se ignoran)
    @NotNull(message = "La hora de apertura es obligatoria")
    @Schema(description = "Hora de apertura (HH:mm)", type = "string", example = "08:00")
    private LocalTime horaApertura;

    // Hora de cierre en formato HH:mm; posterior a la apertura, incluso en un día no laborable
    @NotNull(message = "La hora de cierre es obligatoria")
    @Schema(description = "Hora de cierre (HH:mm), posterior a la apertura. También en un día no laborable",
            type = "string", example = "20:00")
    private LocalTime horaCierre;

    // Si es false, la clínica no abre ese día (conserva sus horas)
    @NotNull(message = "Indica si el día es laborable")
    @Schema(description = "Si es false, la clínica no abre ese día", example = "true")
    private Boolean laborable;
}
