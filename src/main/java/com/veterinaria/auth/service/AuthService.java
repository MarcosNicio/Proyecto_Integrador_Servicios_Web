package com.veterinaria.auth.service;

import com.veterinaria.auth.dto.AuthResponse;
import com.veterinaria.auth.dto.LoginRequest;
import com.veterinaria.auth.dto.MeResponse;
import com.veterinaria.auth.dto.RegistroRequest;
import com.veterinaria.auth.dto.RegistroResponse;
import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.clientes.dto.ClienteRequestDTO;
import com.veterinaria.clientes.dto.ClienteResponseDTO;
import com.veterinaria.clientes.repository.ClienteRepository;
import com.veterinaria.clientes.service.ClienteService;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ForbiddenException;
import com.veterinaria.common.exception.UnauthorizedException;
import com.veterinaria.common.security.SecurityErrorWriter;
import com.veterinaria.common.util.JwtUtil;
import com.veterinaria.personal.dto.PersonalResponseDTO;
import com.veterinaria.personal.service.PersonalService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Lógica de autenticación: inicio de sesión y registro de clientes.
 */
@Service
public class AuthService {

    public static final String MENSAJE_CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    public static final String MENSAJE_CUENTA_DESACTIVADA = "Tu cuenta está desactivada. Contacta al administrador";
    public static final String MENSAJE_CORREO_REGISTRADO = "El correo ya está registrado";

    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ClienteService clienteService;
    private final ClienteRepository clienteRepository;
    private final PersonalService personalService;

    /**
     * Hash BCrypt de un valor aleatorio, generado al arrancar. Se compara contra él cuando el
     * correo no existe, para que el login tarde lo mismo en ambos casos (ver {@link #login}).
     */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, ClienteService clienteService,
                       ClienteRepository clienteRepository, PersonalService personalService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.clienteService = clienteService;
        this.clienteRepository = clienteRepository;
        this.personalService = personalService;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Datos de quien inició sesión, a partir del correo de su token: su cuenta y, según su rol,
     * su perfil de cliente (CLIENTE) o de personal (VETERINARIO, CAJERO). El ADMIN no tiene perfil.
     * <p>
     * Sirve para que el frontend, justo después del login, obtenga el {@code idCliente} o el
     * {@code idPersonal} que necesitan los demás endpoints. Si la cuenta no tiene perfil (por
     * ejemplo, un cliente cuyo perfil se borró), ese campo llega en {@code null}.
     * <p>
     * El filtro JWT ya garantiza que la cuenta existe y está activa; si no, la petición recibe
     * 401 antes de llegar aquí.
     */
    @Transactional(readOnly = true)
    public MeResponse obtenerUsuarioActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UnauthorizedException(SecurityErrorWriter.MENSAJE_USUARIO_INACTIVO));

        ClienteResponseDTO cliente = null;
        PersonalResponseDTO personal = null;
        if (usuario.getRol() == Rol.CLIENTE && clienteRepository.existsByIdUsuario(usuario.getIdUsuario())) {
            cliente = clienteService.obtenerPorIdUsuario(usuario.getIdUsuario());
        } else if (usuario.getRol() == Rol.VETERINARIO || usuario.getRol() == Rol.CAJERO) {
            personal = personalService.buscarPerfilPorIdUsuario(usuario.getIdUsuario()).orElse(null);
        }

        return MeResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .cliente(cliente)
                .personal(personal)
                .build();
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
        String correo = normalizarCorreo(request.getCorreo());
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

    /**
     * Crea la cuenta de un cliente: su usuario (rol CLIENTE) y su perfil en {@code CLIENTES},
     * vinculados por {@code id_usuario}. Devuelve un token para que quede con la sesión iniciada.
     * <p>
     * Todo ocurre en <b>una sola transacción</b>: si falla la creación del perfil, también se
     * deshace la del usuario. Así nunca queda una cuenta sin cliente, ni al revés.
     * <p>
     * El perfil se crea con {@link ClienteService#crearCliente} del módulo de clientes, para
     * que el folio {@code CLI-XXXX} y sus reglas se apliquen igual que en recepción.
     *
     * @throws ConflictException si el correo ya está registrado (409)
     */
    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        String correo = normalizarCorreo(request.getCorreo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException(MENSAJE_CORREO_REGISTRADO);
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .activo(true)
                .build());

        ClienteResponseDTO cliente = clienteService.crearCliente(ClienteRequestDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .build());

        return RegistroResponse.builder()
                .token(jwtUtil.generarToken(usuario.getCorreo(), usuario.getRol().name()))
                .tipo(TIPO_TOKEN)
                .expiraEnMs(jwtUtil.getExpirationMs())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .cliente(cliente)
                .build();
    }

    /** Los correos se guardan y se comparan sin espacios y en minúsculas. También lo usa el módulo de personal. */
    public static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
