package com.veterinaria.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing).
 * <p>
 * El navegador bloquea por defecto las peticiones de una página a un servidor con
 * distinto origen (protocolo + dominio + puerto). Como el frontend en React corre en
 * otro puerto (Vite: 5173, Create React App: 3000) y este backend en el 8080, sin
 * esta configuración el navegador rechazaría las respuestas de la API.
 * <p>
 * Reglas que se aplican a todas las rutas {@code /api/**}:
 * <ul>
 *   <li><b>Orígenes:</b> solo los definidos en {@code app.cors.allowed-origins}
 *       (application.properties). Nunca usar "*" para no exponer la API a cualquier sitio.</li>
 *   <li><b>Métodos:</b> GET, POST, PUT, PATCH, DELETE y OPTIONS (OPTIONS es la petición
 *       previa o "preflight" que hace el navegador antes de la petición real).</li>
 *   <li><b>Headers:</b> {@code Authorization} (token JWT) y {@code Content-Type} (JSON).</li>
 *   <li><b>Credenciales:</b> permitidas, para que el navegador pueda enviar datos de autenticación.</li>
 *   <li><b>Cache del preflight:</b> 1 hora, para no repetir la consulta previa en cada petición.</li>
 * </ul>
 * <b>Nota:</b> Spring Security aplica estas mismas reglas gracias a {@code http.cors(...)}
 * en {@code common/security/SecurityConfig}. Si se quita, el navegador bloquearía al frontend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Rutas de la API a las que aplican las reglas de CORS. */
    private static final String API_PATH = "/api/**";

    /** Tiempo (en segundos) que el navegador recuerda la respuesta del preflight. */
    private static final long PREFLIGHT_MAX_AGE_SECONDS = 3600;

    /** Orígenes del frontend permitidos, separados por coma en application.properties. */
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API_PATH)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(PREFLIGHT_MAX_AGE_SECONDS);
    }
}
