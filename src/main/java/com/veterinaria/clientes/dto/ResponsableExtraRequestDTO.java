package com.veterinaria.clientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para registrar un contacto adicional o de emergencia del cliente")
public class ResponsableExtraRequestDTO {

    // Nombre completo del contacto adicional o de emergencia (obligatorio, máximo 150 caracteres)
    @NotBlank(message = "El nombre completo del responsable es obligatorio")
    @Size(max = 150, message = "El nombre completo no puede exceder los 150 caracteres")
    @Schema(description = "Nombre completo del contacto adicional", example = "María Fernanda Pérez López")
    private String nombreCompleto;

    // Teléfono de contacto del responsable (obligatorio, máximo 20 caracteres)
    @NotBlank(message = "El teléfono del responsable es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
    @Schema(description = "Teléfono de contacto del responsable", example = "5598765432")
    private String telefono;

    // Parentesco o relación con el cliente titular (opcional, máximo 50 caracteres)
    @Size(max = 50, message = "El parentesco no puede exceder los 50 caracteres")
    @Schema(description = "Parentesco o relación con el cliente titular", example = "Esposa")
    private String parentesco;
}
