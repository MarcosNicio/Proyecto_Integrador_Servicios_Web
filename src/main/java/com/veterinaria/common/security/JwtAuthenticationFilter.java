package com.veterinaria.common.security;

import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro que identifica al usuario a partir del token JWT. Se ejecuta una vez por petición,
 * antes de las reglas de {@link SecurityConfig}.
 * <p>
 * Pasos:
 * <ol>
 *   <li>Lee el header {@code Authorization: Bearer <token>}. Si no viene, no hace nada.</li>
 *   <li>Valida la firma y la caducidad del token con {@link JwtUtil}.</li>
 *   <li>Busca al usuario en la base y comprueba que siga <b>activo</b>. Así, al desactivar a
 *       alguien pierde el acceso de inmediato, sin esperar a que caduque su token.</li>
 *   <li>Registra al usuario en el contexto de seguridad con su rol ({@code ADMIN} pasa a ser
 *       la autoridad {@code ROLE_ADMIN}). El rol se toma de la base, no del token, para que un
 *       cambio de rol aplique de inmediato.</li>
 * </ol>
 * Si algo falla, el filtro <b>no lanza errores</b>: deja la petición sin identificar y guarda el
 * motivo en la petición. Si la ruta es privada, {@link JwtAuthenticationEntryPoint} responde 401
 * con ese motivo; si es pública, la petición sigue normal.
 * <p>
 * No se registra como {@code @Component} a propósito: Spring Boot agregaría todos los filtros
 * que son beans a la cadena general de la aplicación, además de la de seguridad.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            autenticar(header.substring(PREFIJO_BEARER.length()).trim(), request);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        if (!jwtUtil.esTokenValido(token)) {
            request.setAttribute(SecurityErrorWriter.ATRIBUTO_MOTIVO, SecurityErrorWriter.MENSAJE_TOKEN_INVALIDO);
            return;
        }

        Optional<Usuario> usuario = usuarioRepository.findByCorreo(jwtUtil.obtenerCorreo(token))
                .filter(u -> Boolean.TRUE.equals(u.getActivo()));

        if (usuario.isEmpty()) {
            request.setAttribute(SecurityErrorWriter.ATRIBUTO_MOTIVO, SecurityErrorWriter.MENSAJE_USUARIO_INACTIVO);
            return;
        }

        Usuario u = usuario.get();
        UsernamePasswordAuthenticationToken autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                u.getCorreo(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
