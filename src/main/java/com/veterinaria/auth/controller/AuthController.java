package com.veterinaria.auth.controller;

import com.veterinaria.auth.dto.AuthResponse;
import com.veterinaria.auth.dto.LoginRequest;
import com.veterinaria.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación. {@code /api/auth/**} no requiere token
 * (ver {@code common/security/SecurityConfig}).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y obtención del token JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements // Endpoint público: Swagger no le pone el candado del token
    @Operation(summary = "Iniciar sesión",
            description = "Comprueba correo y contraseña y devuelve un token JWT. Envíalo en las demás "
                    + "peticiones como 'Authorization: Bearer <token>' (en Swagger, con el botón Authorize).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inicio de sesión correcto; devuelve el token"),
            @ApiResponse(responseCode = "400", description = "Falta el correo o la contraseña, o el correo tiene un formato inválido"),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos"),
            @ApiResponse(responseCode = "403", description = "La cuenta está desactivada")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
