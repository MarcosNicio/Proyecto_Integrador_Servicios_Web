package com.veterinaria.auth.service;

import com.veterinaria.auth.dto.AuthResponse;
import com.veterinaria.auth.dto.LoginRequest;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.exception.ForbiddenException;
import com.veterinaria.common.exception.UnauthorizedException;
import com.veterinaria.common.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Lógica de autenticación: inicio de sesión (y, más adelante, registro de clientes).
 */
@Service
public class AuthService {

    public static final String MENSAJE_CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    public static final String MENSAJE_CUENTA_DESACTIVADA = "Tu cuenta está desactivada. Contacta al administrador";

    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Hash BCrypt de un valor aleatorio, generado al arrancar. Se compara contra él cuando el
     * correo no existe, para que el login tarde lo mismo en ambos casos (ver {@link #login}).
     */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Comprueba las credenciales y, si son correctas, emite un token JWT.
     * <p>
     * Medidas de seguridad:
     * <ul>
     *   <li><b>Mismo mensaje</b> si el correo no existe o si la contraseña está mal. Si fueran
     *       distintos, cualquiera podría averiguar qué correos están registrados.</li>
     *   <li><b>Mismo tiempo de respuesta</b> en los dos casos: BCrypt tarda a propósito, así que
     *       si el correo no existe se compara contra {@code hashFicticio}. Si no, la velocidad
     *       de la respuesta delataría lo mismo que el mensaje.</li>
     *   <li>La cuenta desactivada se revisa <b>después</b> de la contraseña, para no revelar ese
     *       dato a quien no la conoce.</li>
     * </ul>
     *
     * @throws UnauthorizedException si el correo no existe o la contraseña no coincide (401)
     * @throws ForbiddenException    si las credenciales son correctas pero la cuenta está desactivada (403)
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String correo = request.getCorreo().trim().toLowerCase();
        Optional<Usuario> encontrado = usuarioRepository.findByCorreo(correo);

        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashFicticio);
        boolean passwordCorrecta = passwordEncoder.matches(request.getPassword(), hash);

        if (encontrado.isEmpty() || !passwordCorrecta) {
            throw new UnauthorizedException(MENSAJE_CREDENCIALES_INCORRECTAS);
        }

        Usuario usuario = encontrado.get();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ForbiddenException(MENSAJE_CUENTA_DESACTIVADA);
        }

        return AuthResponse.builder()
                .token(jwtUtil.generarToken(usuario.getCorreo(), usuario.getRol().name()))
                .tipo(TIPO_TOKEN)
                .expiraEnMs(jwtUtil.getExpirationMs())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .build();
    }
}
