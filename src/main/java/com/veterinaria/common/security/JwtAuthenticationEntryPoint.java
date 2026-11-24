package com.veterinaria.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde <b>401 Unauthorized</b> ("no sé quién eres") cuando una ruta privada recibe una
 * petición sin identificar: sin token, con un token inválido o vencido, o de un usuario
 * desactivado.
 * <p>
 * El mensaje explica el motivo que dejó {@link JwtAuthenticationFilter}. Así el frontend sabe,
 * por ejemplo, que debe mandar al usuario a iniciar sesión otra vez.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object motivo = request.getAttribute(SecurityErrorWriter.ATRIBUTO_MOTIVO);
        String mensaje = motivo != null ? motivo.toString() : SecurityErrorWriter.MENSAJE_SIN_TOKEN;
        errorWriter.escribir(request, response, HttpStatus.UNAUTHORIZED, mensaje);
    }
}
