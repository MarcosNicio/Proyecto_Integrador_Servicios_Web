package com.veterinaria.auth.dto;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.clientes.dto.ClienteResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de un registro correcto: los mismos datos que el login (el cliente queda con la
 * sesión iniciada) más su perfil de cliente recién creado, con su folio {@code CLI-XXXX}.
 * Nunca incluye la contraseña ni su hash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Token de acceso y perfil del cliente recién registrado")
public class RegistroResponse {

    // Token JWT: el cliente queda con la sesión iniciada sin tener que hacer login
    @Schema(description = "Token JWT para el header Authorization", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    // Tipo de token: siempre "Bearer"
    @Schema(description = "Tipo de token; se antepone al token en el header Authorization", example = "Bearer")
    private String tipo;

    // Duración del token en milisegundos desde que se emitió
    @Schema(description = "Milisegundos que dura el token desde que se emitió", example = "86400000")
    private long expiraEnMs;

    // Correo con el que se registró (en minúsculas)
    @Schema(description = "Correo del usuario", example = "ana.lopez@correo.com")
    private String correo;

    // Rol asignado: siempre CLIENTE en el registro
    @Schema(description = "Rol del usuario", example = "CLIENTE")
    private Rol rol;

    // Perfil de cliente creado con el módulo de clientes (incluye el folio CLI-XXXX)
    @Schema(description = "Perfil de cliente creado, con su folio")
    private ClienteResponseDTO cliente;
}
