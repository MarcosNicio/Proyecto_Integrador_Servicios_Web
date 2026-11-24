package com.veterinaria.auth.dto;

import com.veterinaria.auth.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un inicio de sesión correcto.
 * <p>
 * El frontend guarda {@code token} y lo envía en cada petición como
 * {@code Authorization: Bearer <token>}. Con {@code rol} decide qué menú mostrar.
 * Nunca incluye la contraseña ni su hash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Token de acceso y datos básicos del usuario que inició sesión")
public class AuthResponse {

    // Token JWT firmado por el servidor
    @Schema(description = "Token JWT para el header Authorization", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    // Tipo de token: siempre "Bearer"
    @Schema(description = "Tipo de token; se antepone al token en el header Authorization", example = "Bearer")
    private String tipo;

    // Duración del token en milisegundos desde que se emitió
    @Schema(description = "Milisegundos que dura el token desde que se emitió", example = "86400000")
    private long expiraEnMs;

    // Correo del usuario que inició sesión
    @Schema(description = "Correo del usuario", example = "admin@veterinaria.com")
    private String correo;

    // Rol del usuario, para que el frontend muestre las opciones que le corresponden
    @Schema(description = "Rol del usuario", example = "ADMIN")
    private Rol rol;
}
