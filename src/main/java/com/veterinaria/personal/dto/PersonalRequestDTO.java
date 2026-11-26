package com.veterinaria.personal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Datos para dar de alta a un empleado: los de su cuenta (correo, contraseña y rol) y los de
 * su perfil en {@code PERSONAL}.
 * <p>
 * La contraseña sigue las mismas reglas que el registro de clientes. {@code @ToString} la
 * excluye para que nunca aparezca en el log. No lleva {@code @AllArgsConstructor}: Jackson 3
 * lo usaría y se saltaría {@link #setCorreo}.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
@Schema(description = "Datos para dar de alta a un veterinario o cajero")
public class PersonalRequestDTO {

    // Correo de acceso (obligatorio, único en el sistema; columna de 120 caracteres)
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 120, message = "El correo no puede exceder los 120 caracteres")
    @Schema(description = "Correo con el que iniciará sesión", example = "laura.zamora@veterinaria.com")
    private String correo;

    // Contraseña inicial que define el administrador: 8 a 72 caracteres, con letra y número
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "La contraseña debe incluir al menos una letra y un número")
    @Schema(description = "Contraseña inicial: de 8 a 72 caracteres, con al menos una letra y un número",
            example = "Inicial123")
    private String password;

    // Rol del empleado: solo VETERINARIO o CAJERO (los clientes se registran solos y el ADMIN ya existe)
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "^(VETERINARIO|CAJERO)$", message = "El rol debe ser VETERINARIO o CAJERO")
    @Schema(description = "Rol del empleado", example = "VETERINARIO", allowableValues = {"VETERINARIO", "CAJERO"})
    private String rol;

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

    // Cédula profesional: obligatoria para VETERINARIO (lo revisa el servicio), opcional para CAJERO
    @Size(max = 50, message = "La cédula profesional no puede exceder los 50 caracteres")
    @Schema(description = "Cédula profesional. Obligatoria para VETERINARIO", example = "12345678", nullable = true)
    private String cedulaProfesional;

    // Especies que atiende (opcional, máximo 150 caracteres)
    @Size(max = 150, message = "Las especies no pueden exceder los 150 caracteres")
    @Schema(description = "Especies que atiende (opcional)", example = "Perros, Gatos", nullable = true)
    private String especialidadEspecies;

    /** Quita los espacios al inicio y al final del correo antes de validarlo. */
    public void setCorreo(String correo) {
        this.correo = correo != null ? correo.trim() : null;
    }
}
