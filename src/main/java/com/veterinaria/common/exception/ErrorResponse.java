package com.veterinaria.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de las respuestas de error de toda la API.
 * <p>
 * Ejemplo de respuesta:
 * <pre>
 * {
 *   "timestamp": "2026-11-23T10:15:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Mascota no encontrada con id 999",
 *   "path": "/api/mascotas/999"
 * }
 * </pre>
 * En errores de validación se agrega {@code errors}, con el mensaje de cada campo inválido,
 * para que el frontend pueda mostrar cada mensaje debajo de su campo:
 * <pre>
 * "errors": { "telefono": "El teléfono es obligatorio" }
 * </pre>
 *
 * @param timestamp momento en que ocurrió el error
 * @param status    código HTTP (404, 400, 409, 500...)
 * @param error     nombre del código HTTP ("Not Found", "Bad Request"...)
 * @param message   explicación del error, pensada para mostrarse al usuario
 * @param path      ruta que se pidió
 * @param errors    errores por campo (solo en validaciones; si es null no se envía)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> errors
) {
}
