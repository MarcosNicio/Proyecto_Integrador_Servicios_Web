package com.veterinaria.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de un consultorio. Se usa para crearlo y para editarlo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Consultorio o espacio físico de la clínica")
public class ConsultorioRequestDTO {

    // Nombre visible, sin repetirse (obligatorio, máximo 60 caracteres: la columna es de 60)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede exceder los 60 caracteres")
    @Schema(description = "Nombre del consultorio; no se repite", example = "Consultorio 3")
    private String nombre;

    // Tipo de consultorio: debe existir y estar activo
    @NotNull(message = "El tipo de consultorio es obligatorio")
    @Schema(description = "Identificador del tipo de consultorio (debe existir y estar activo)", example = "1")
    private Integer idTipoConsultorio;
}
