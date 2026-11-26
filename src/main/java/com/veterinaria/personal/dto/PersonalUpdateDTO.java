package com.veterinaria.personal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos editables del perfil de un empleado.
 * <p>
 * No incluye correo, rol ni contraseña: esos datos pertenecen a la cuenta de acceso y no se
 * cambian desde el perfil.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos editables del perfil de un empleado")
public class PersonalUpdateDTO {

    // Nombre(s) (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder los 80 caracteres")
    @Schema(description = "Nombre(s)", example = "Laura")
    private String nombre;

    // Apellidos (obligatorio, máximo 100 caracteres)
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder los 100 caracteres")
    @Schema(description = "Apellidos", example = "Zamora Ruiz")
    private String apellidos;

    // Teléfono (obligatorio, máximo 20 caracteres)
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
    @Schema(description = "Teléfono de contacto", example = "5551112222")
    private String telefono;

    // Cédula profesional: un VETERINARIO no puede quedarse sin ella (lo revisa el servicio)
    @Size(max = 50, message = "La cédula profesional no puede exceder los 50 caracteres")
    @Schema(description = "Cédula profesional. Obligatoria para VETERINARIO", example = "12345678", nullable = true)
    private String cedulaProfesional;

    // Especies que atiende (opcional, máximo 150 caracteres)
    @Size(max = 150, message = "Las especies no pueden exceder los 150 caracteres")
    @Schema(description = "Especies que atiende (opcional)", example = "Perros, Gatos, Exóticos", nullable = true)
    private String especialidadEspecies;
}
