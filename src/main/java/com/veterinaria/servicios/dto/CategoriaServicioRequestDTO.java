package com.veterinaria.servicios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de petición para crear o actualizar una categoría de servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos requeridos para registrar o actualizar una categoría de servicio")
public class CategoriaServicioRequestDTO {

    // Nombre único de la categoría de servicio (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre de la categoría no puede exceder los 80 caracteres")
    @Schema(description = "Nombre único de la categoría de servicio", example = "Estética", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    // Descripción opcional del alcance de la categoría (máximo 255 caracteres)
    @Size(max = 255, message = "La descripción de la categoría no puede exceder los 255 caracteres")
    @Schema(description = "Descripción breve de los servicios agrupados en esta categoría", example = "Servicios de baño, corte de pelo y cuidado higiénico")
    private String descripcion;
}
