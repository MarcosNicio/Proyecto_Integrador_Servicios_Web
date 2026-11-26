package com.veterinaria.auth.dto;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.clientes.dto.ClienteResponseDTO;
import com.veterinaria.personal.dto.PersonalResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos del usuario que inició sesión ({@code GET /api/auth/me}): su cuenta y, según su rol,
 * su perfil de cliente o de personal. Nunca incluye la contraseña.
 * <p>
 * Solo uno de los perfiles viene lleno: {@code cliente} para CLIENTE, {@code personal} para
 * VETERINARIO y CAJERO. Para ADMIN ambos son {@code null}.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Usuario que inició sesión, con su perfil según el rol")
public class MeResponse {

    @Schema(description = "Identificador de la cuenta", example = "5")
    private Integer idUsuario;

    @Schema(description = "Correo con el que inició sesión", example = "ana.lopez@correo.com")
    private String correo;

    @Schema(description = "Rol de la cuenta", example = "CLIENTE")
    private Rol rol;

    @Schema(description = "Si la cuenta está activa (siempre true: una cuenta desactivada recibe 401)", example = "true")
    private Boolean activo;

    @Schema(description = "Perfil de cliente (solo rol CLIENTE; null en los demás)", nullable = true)
    private ClienteResponseDTO cliente;

    @Schema(description = "Perfil de personal (solo VETERINARIO y CAJERO; null en los demás)", nullable = true)
    private PersonalResponseDTO personal;
}
