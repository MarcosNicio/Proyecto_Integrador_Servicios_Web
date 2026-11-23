package com.veterinaria.common.exception;

/**
 * Se lanza cuando la operación choca con datos que ya existen (responde <b>409 Conflict</b>).
 * <p>
 * Ejemplos: registrar un correo que ya está en uso, reservar un horario ya ocupado.
 * <pre>
 * if (usuarioRepository.existsByCorreo(correo)) {
 *     throw new ConflictException("El correo " + correo + " ya está registrado");
 * }
 * </pre>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
