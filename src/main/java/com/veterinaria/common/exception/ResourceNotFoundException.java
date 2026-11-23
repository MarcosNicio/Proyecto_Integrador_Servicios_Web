package com.veterinaria.common.exception;

/**
 * Se lanza cuando se busca un registro que no existe (responde <b>404 Not Found</b>).
 * <p>
 * Ejemplo de uso en un servicio:
 * <pre>
 * Mascota mascota = mascotaRepository.findById(id)
 *         .orElseThrow(() -&gt; new ResourceNotFoundException("Mascota no encontrada con id " + id));
 * </pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
