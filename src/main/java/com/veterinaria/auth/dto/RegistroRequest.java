package com.veterinaria.auth.dto;

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
 * Datos que envía el frontend para que un cliente cree su cuenta: los de acceso (correo y
 * contraseña) y los de su perfil de cliente.
 * <p>
 * Los límites de nombre, apellidos, teléfono y dirección son los mismos que usa
 * {@code clientes/dto/ClienteRequestDTO}, porque el perfil se crea con ese módulo.
 * <p>
 * {@code @ToString} excluye la contraseña para que nunca aparezca en el log. No lleva
 * {@code @AllArgsConstructor}: Jackson 3 lo usaría y se saltaría {@link #setCorreo}.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
@Schema(description = "Datos para que un cliente cree su cuenta")
public class RegistroRequest {

    // Correo de acceso (obligatorio, único en el sistema; columna de 120 caracteres)
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 120, message = "El correo no puede exceder los 120 caracteres")
    @Schema(description = "Correo con el que iniciará sesión", example = "ana.lopez@correo.com")
    private String correo;

    // Contraseña: 8 a 72 caracteres (BCrypt solo toma en cuenta los primeros 72 bytes)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$",
            message = "La contraseña debe incluir al menos una letra y un número")
    @Schema(description = "Contraseña: de 8 a 72 caracteres, con al menos una letra y un número", example = "MiClave123")
    private String password;

    // Nombre(s) del cliente (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder los 80 caracteres")
    @Schema(description = "Nombre(s) del cliente", example = "Ana")
    private String nombre;

    // Apellidos del cliente (obligatorio, máximo 100 caracteres)
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder los 100 caracteres")
    @Schema(description = "Apellidos del cliente", example = "López Pérez")
    private String apellidos;

    // Teléfono de contacto (obligatorio, máximo 20 caracteres)
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
    @Schema(description = "Teléfono de contacto", example = "5551234567")
    private String telefono;

    // Dirección del domicilio (opcional, máximo 255 caracteres)
    @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
    @Schema(description = "Dirección del domicilio (opcional)", example = "Av. Juárez 123, Centro", nullable = true)
    private String direccion;

    /** Quita los espacios al inicio y al final del correo antes de validarlo. */
    public void setCorreo(String correo) {
        this.correo = correo != null ? correo.trim() : null;
    }
}
