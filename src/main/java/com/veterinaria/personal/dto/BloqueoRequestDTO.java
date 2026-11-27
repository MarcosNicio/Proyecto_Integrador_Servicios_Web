package com.veterinaria.personal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Datos de un bloqueo de agenda. Se usa para registrarlo y para editarlo.
 * <p>
 * Las reglas que dependen de las fechas (fin posterior al inicio, que no haya terminado y que
 * no se encime con otro bloqueo) las revisa el servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Periodo en que un empleado no está disponible")
public class BloqueoRequestDTO {

    // Inicio del bloqueo (yyyy-MM-ddTHH:mm); puede ser de hace un rato (ausencia que ya empezó)
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(description = "Inicio del bloqueo (yyyy-MM-ddTHH:mm)", type = "string", example = "2026-12-22T00:00")
    private LocalDateTime fechaInicio;

    // Fin del bloqueo: posterior al inicio y en el futuro
    @NotNull(message = "La fecha de fin es obligatoria")
    @Schema(description = "Fin del bloqueo (yyyy-MM-ddTHH:mm), posterior al inicio y futuro",
            type = "string", example = "2026-12-27T00:00")
    private LocalDateTime fechaFin;

    // Motivo: los mismos valores que permite la columna BLOQUEOS_DISPONIBILIDAD.motivo
    @NotBlank(message = "El motivo es obligatorio")
    @Pattern(regexp = "^(PERSONAL|CIRUGIA|DIA_LIBRE)$", message = "El motivo debe ser PERSONAL, CIRUGIA o DIA_LIBRE")
    @Schema(description = "Motivo del bloqueo", example = "PERSONAL", allowableValues = {"PERSONAL", "CIRUGIA", "DIA_LIBRE"})
    private String motivo;

    // Detalle opcional (columna de 255 caracteres)
    @Size(max = 255, message = "Las notas no pueden exceder los 255 caracteres")
    @Schema(description = "Detalle opcional", example = "Vacaciones de fin de año", nullable = true)
    private String notas;
}
