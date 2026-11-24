package com.veterinaria.common.exception;

/**
 * Se lanza cuando no se puede comprobar quién es el usuario (responde <b>401 Unauthorized</b>).
 * <p>
 * Ejemplo: credenciales incorrectas en el login.
 * <pre>
 * if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
 *     throw new UnauthorizedException("Correo o contraseña incorrectos");
 * }
 * </pre>
 * Las peticiones a rutas privadas sin token no usan esta excepción: las responde
 * {@code common/security/JwtAuthenticationEntryPoint} con el mismo formato.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
