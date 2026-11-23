package com.veterinaria.common.exception;

/**
 * Se lanza cuando se rompe una regla del negocio (responde <b>400 Bad Request</b>).
 * <p>
 * Ejemplos: agendar una cita en una fecha pasada, cobrar una orden ya pagada,
 * registrar una salida de inventario mayor al stock disponible.
 * <pre>
 * if (cita.getFecha().isBefore(LocalDate.now())) {
 *     throw new BusinessException("No se puede agendar una cita en una fecha pasada");
 * }
 * </pre>
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
