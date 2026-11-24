package com.veterinaria.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Datos que envía el frontend para iniciar sesión.
 * <p>
 * {@code @ToString} excluye la contraseña para que nunca aparezca en el log.
 * <p>
 * No lleva {@code @AllArgsConstructor}: si existiera, Jackson 3 armaría el objeto con ese
 * constructor y se saltaría {@link #setCorreo}, que limpia los espacios del correo.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
@Schema(description = "Credenciales para iniciar sesión")
public class LoginRequest {

    // Correo con el que el usuario se registró (obligatorio)
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Schema(description = "Correo del usuario", example = "admin@veterinaria.com")
    private String correo;

    // Contraseña en texto; se compara con el hash BCrypt y nunca se guarda
    @NotBlank(message = "La contraseña es obligatoria")
    @Schema(description = "Contraseña del usuario", example = "admin123")
    private String password;

    /**
     * Quita los espacios al inicio y al final del correo (comunes al copiar y pegar) antes de
     * validarlo. La contraseña no se toca: los espacios pueden ser parte de ella.
     */
    public void setCorreo(String correo) {
        this.correo = correo != null ? correo.trim() : null;
    }
}
