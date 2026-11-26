package com.veterinaria.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de un tipo de consultorio. Se usa para crearlo y para editarlo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Tipo de consultorio (Consulta, Cirugía, Estética…)")
public class TipoConsultorioRequestDTO {

    // Nombre único del tipo (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder los 80 caracteres")
    @Schema(description = "Nombre del tipo; no se repite", example = "Rayos X")
    private String nombre;

    // Descripción opcional (máximo 255 caracteres)
    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres")
    @Schema(description = "Descripción opcional", example = "Sala de diagnóstico por imagen", nullable = true)
    private String descripcion;
}
