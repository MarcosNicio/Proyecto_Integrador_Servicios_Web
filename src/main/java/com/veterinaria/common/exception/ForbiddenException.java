package com.veterinaria.common.exception;

/**
 * Se lanza cuando se sabe quién es el usuario, pero no puede hacer la operación
 * (responde <b>403 Forbidden</b>).
 * <p>
 * Ejemplos: una cuenta desactivada que intenta iniciar sesión, o un cliente que intenta ver
 * la mascota de otro cliente.
 * <pre>
 * if (!mascota.getCliente().getIdUsuario().equals(idUsuarioActual)) {
 *     throw new ForbiddenException("No puedes consultar mascotas de otro cliente");
 * }
 * </pre>
 * Para limitar un endpoint completo por rol es más simple usar
 * {@code @PreAuthorize("hasRole('ADMIN')")}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
