package com.veterinaria.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde <b>403 Forbidden</b> ("sé quién eres, pero no puedes") cuando el usuario sí está
 * identificado pero su rol no tiene permiso para la ruta, según las reglas de
 * {@link SecurityConfig}.
 * <p>
 * Los {@code @PreAuthorize} de los controladores se resuelven en {@code GlobalExceptionHandler},
 * que responde el mismo 403 con el mismo mensaje.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    public JwtAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errorWriter.escribir(request, response, HttpStatus.FORBIDDEN, SecurityErrorWriter.MENSAJE_SIN_PERMISO);
    }
}
