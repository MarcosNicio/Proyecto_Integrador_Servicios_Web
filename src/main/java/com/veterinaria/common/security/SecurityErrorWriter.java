package com.veterinaria.common.security;

import com.veterinaria.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Escribe las respuestas 401 y 403 de seguridad con el mismo formato {@link ErrorResponse}
 * que usa {@code GlobalExceptionHandler} para el resto de la API.
 * <p>
 * Hace falta porque los errores de seguridad ocurren en los filtros, <b>antes</b> de llegar a
 * los controladores, y por eso {@code GlobalExceptionHandler} no los ve. Usa el mismo
 * {@link JsonMapper} de Spring, así que el JSON (por ejemplo, el formato de la fecha) sale igual.
 */
@Component
public class SecurityErrorWriter {

    /** Atributo de la petición donde el filtro JWT deja el motivo por el que no autenticó. */
    public static final String ATRIBUTO_MOTIVO = "veterinaria.security.motivo";

    public static final String MENSAJE_SIN_TOKEN = "Se requiere iniciar sesión para acceder a este recurso";
    public static final String MENSAJE_TOKEN_INVALIDO = "El token es inválido o ha expirado";
    public static final String MENSAJE_USUARIO_INACTIVO = "El usuario no existe o está desactivado";
    public static final String MENSAJE_SIN_PERMISO = "No tienes permiso para acceder a este recurso";

    private final JsonMapper jsonMapper;

    public SecurityErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String mensaje) throws IOException {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI(),
                null);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
