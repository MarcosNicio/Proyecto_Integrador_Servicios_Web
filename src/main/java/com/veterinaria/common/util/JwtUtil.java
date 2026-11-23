package com.veterinaria.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para crear y verificar tokens JWT (JSON Web Token).
 * <p>
 * Un JWT es un "gafete firmado" que el backend entrega al usuario al hacer login y que el
 * frontend envía en cada petición en el header {@code Authorization: Bearer <token>}.
 * Tiene tres partes: encabezado, datos (claims) y firma.
 * <p>
 * Datos que guarda cada token generado aquí:
 * <ul>
 *   <li>{@code sub}: correo del usuario (quién es).</li>
 *   <li>{@code rol}: ADMIN, VETERINARIO, CAJERO o CLIENTE.</li>
 *   <li>{@code iat}: fecha de creación.</li>
 *   <li>{@code exp}: fecha de caducidad.</li>
 * </ul>
 * Los datos del token <b>no van cifrados</b> (cualquiera puede leerlos), por eso nunca se guardan
 * contraseñas. Lo que lo hace seguro es la <b>firma</b> (HMAC-SHA): se calcula con la clave secreta
 * {@code app.jwt.secret}, así que si alguien modifica el token o lo fabrica sin la clave, la firma
 * no coincide y el token se rechaza.
 * <p>
 * Uso previsto (módulo auth, Sprint 1):
 * <ul>
 *   <li>El login llamará a {@link #generarToken(String, String)} para entregar el token.</li>
 *   <li>El filtro JWT llamará a {@link #esTokenValido(String)} y {@link #obtenerCorreo(String)}
 *       en cada petición para saber quién la hace.</li>
 * </ul>
 */
@Component
public class JwtUtil {

    /** Longitud mínima de la clave para HMAC-SHA256: 256 bits = 32 bytes. */
    private static final int MIN_SECRET_BYTES = 32;

    /** Nombre del dato (claim) donde se guarda el rol del usuario. */
    private static final String CLAIM_ROL = "rol";

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Lee la clave secreta y la duración del token desde application.properties.
     *
     * @param secret       clave secreta ({@code app.jwt.secret}, variable de entorno {@code JWT_SECRET})
     * @param expirationMs duración del token en milisegundos ({@code app.jwt.expiration-ms})
     * @throws IllegalStateException si la clave mide menos de 32 bytes; así la aplicación no arranca
     *                               con una clave débil por descuido
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "La clave app.jwt.secret (JWT_SECRET) debe tener al menos " + MIN_SECRET_BYTES + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Crea un token firmado para el usuario.
     *
     * @param correo correo del usuario, se guarda como {@code sub}
     * @param rol    rol del usuario (ADMIN, VETERINARIO, CAJERO o CLIENTE)
     * @return el token JWT en texto, listo para enviarse al frontend
     */
    public String generarToken(String correo, String rol) {
        Date ahora = new Date();
        Date caducidad = new Date(ahora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_ROL, rol)
                .issuedAt(ahora)
                .expiration(caducidad)
                .signWith(key)
                .compact();
    }

    /**
     * Indica si el token es válido: firma correcta (no fue modificado ni fabricado
     * con otra clave), formato correcto y no ha caducado.
     *
     * @param token token JWT sin el prefijo "Bearer "
     * @return {@code true} si se puede confiar en el token
     */
    public boolean esTokenValido(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene el correo del usuario guardado en el token.
     *
     * @throws JwtException si el token no es válido (verificar antes con {@link #esTokenValido(String)})
     */
    public String obtenerCorreo(String token) {
        return obtenerClaims(token).getSubject();
    }

    /**
     * Obtiene el rol del usuario guardado en el token.
     *
     * @throws JwtException si el token no es válido (verificar antes con {@link #esTokenValido(String)})
     */
    public String obtenerRol(String token) {
        return obtenerClaims(token).get(CLAIM_ROL, String.class);
    }

    /**
     * Verifica la firma y la caducidad del token y devuelve sus datos.
     * Lanza {@link JwtException} si el token fue modificado, está caducado o tiene mal formato.
     */
    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
