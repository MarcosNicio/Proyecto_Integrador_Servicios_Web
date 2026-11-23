package com.veterinaria.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link JwtUtil}.
 * <p>
 * Son pruebas unitarias: crean {@code JwtUtil} directamente con una clave de prueba,
 * sin levantar Spring ni conectarse a la base de datos, por eso son muy rápidas.
 */
class JwtUtilTest {

    private static final String CLAVE = "clave-de-prueba-para-tests-de-jwt-1234567890";
    private static final String OTRA_CLAVE = "otra-clave-distinta-para-tests-de-jwt-0987654321";
    private static final long UNA_HORA_MS = 3_600_000;

    private final JwtUtil jwtUtil = new JwtUtil(CLAVE, UNA_HORA_MS);

    /** Un token recién generado es válido y devuelve el correo y el rol con los que se creó. */
    @Test
    void tokenGeneradoEsValidoYContieneCorreoYRol() {
        String token = jwtUtil.generarToken("admin@veterinaria.com", "ADMIN");

        assertTrue(jwtUtil.esTokenValido(token));
        assertEquals("admin@veterinaria.com", jwtUtil.obtenerCorreo(token));
        assertEquals("ADMIN", jwtUtil.obtenerRol(token));
    }

    /** Si se altera el contenido del token (ej. para cambiar el rol), la firma ya no coincide. */
    @Test
    void tokenModificadoEsRechazado() {
        String token = jwtUtil.generarToken("cliente@correo.com", "CLIENTE");
        String[] partes = token.split("\\.");
        // Se reemplaza la parte de datos por otra con rol ADMIN, conservando la firma original
        String datosFalsos = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"cliente@correo.com\",\"rol\":\"ADMIN\"}".getBytes());
        String tokenAlterado = partes[0] + "." + datosFalsos + "." + partes[2];

        assertFalse(jwtUtil.esTokenValido(tokenAlterado));
    }

    /** Un token caducado se rechaza. Se simula creando tokens con duración negativa. */
    @Test
    void tokenCaducadoEsRechazado() {
        JwtUtil jwtUtilCaducado = new JwtUtil(CLAVE, -1000);
        String token = jwtUtilCaducado.generarToken("admin@veterinaria.com", "ADMIN");

        assertFalse(jwtUtil.esTokenValido(token));
    }

    /** Un token firmado con otra clave (fabricado por alguien externo) se rechaza. */
    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        String tokenAjeno = new JwtUtil(OTRA_CLAVE, UNA_HORA_MS).generarToken("admin@veterinaria.com", "ADMIN");

        assertFalse(jwtUtil.esTokenValido(tokenAjeno));
    }

    /** Textos que no son un JWT (basura, vacío o null) se rechazan sin lanzar error. */
    @Test
    void textoQueNoEsTokenEsRechazado() {
        assertFalse(jwtUtil.esTokenValido("esto-no-es-un-token"));
        assertFalse(jwtUtil.esTokenValido(""));
        assertFalse(jwtUtil.esTokenValido(null));
    }

    /** Con una clave de menos de 32 caracteres no se puede crear JwtUtil (la app no arrancaría). */
    @Test
    void claveCortaNoEsPermitida() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("clave-corta", UNA_HORA_MS));
    }
}
