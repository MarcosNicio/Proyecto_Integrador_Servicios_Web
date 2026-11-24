package com.veterinaria.common.security;

import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security: decide qué peticiones pueden llegar a los controladores.
 * <p>
 * Spring Security se coloca <b>antes</b> de los controladores y, al agregarlo, bloquea todo
 * por defecto. Aquí se definen las reglas:
 * <ul>
 *   <li><b>Rutas públicas:</b> {@code /api/auth/**} (login y registro) y la documentación de
 *       Swagger. No se puede pedir un token para obtener el token.</li>
 *   <li><b>Todo lo demás requiere autenticación:</b> {@link JwtAuthenticationFilter} lee el token
 *       del header {@code Authorization} e identifica al usuario con su rol.</li>
 *   <li><b>Errores con el formato de la API:</b> 401 ({@link JwtAuthenticationEntryPoint}) si no
 *       se sabe quién es, y 403 ({@link JwtAccessDeniedHandler}) si su rol no tiene permiso.</li>
 *   <li><b>Roles por endpoint:</b> {@code @EnableMethodSecurity} permite usar
 *       {@code @PreAuthorize("hasRole('ADMIN')")} en los controladores de cada módulo.</li>
 *   <li><b>Sin sesiones (stateless):</b> con JWT el servidor no guarda sesiones; cada
 *       petición trae su token.</li>
 *   <li><b>CSRF desactivado:</b> CSRF protege sesiones basadas en cookies. Con JWT en el
 *       header no aplica.</li>
 *   <li><b>CORS activado:</b> aplica las reglas de {@code common/config/CorsConfig} también
 *       dentro de la cadena de seguridad. Sin esto, el navegador bloquearía al frontend.</li>
 *   <li><b>Sin formulario de login ni autenticación básica:</b> el login será un endpoint
 *       JSON propio ({@code POST /api/auth/login}).</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas que no requieren token. */
    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Página interna a la que Spring redirige los errores; si se bloquea,
            // un error real se convertiría en un 401/403 y ocultaría el problema.
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   UsuarioRepository usuarioRepository,
                                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                                   JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Cifrador de contraseñas con BCrypt.
     * <p>
     * BCrypt es de un solo sentido (del hash no se obtiene la contraseña) y agrega una "sal"
     * aleatoria, así que la misma contraseña genera un hash distinto cada vez. El registro lo
     * usará para cifrar ({@code encode}) y el login para comparar ({@code matches}).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
