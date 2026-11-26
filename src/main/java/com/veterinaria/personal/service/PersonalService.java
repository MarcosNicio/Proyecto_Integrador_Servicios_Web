package com.veterinaria.personal.service;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.auth.service.AuthService;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.personal.dto.PersonalRequestDTO;
import com.veterinaria.personal.dto.PersonalResponseDTO;
import com.veterinaria.personal.dto.PersonalUpdateDTO;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica del personal de la clínica: alta, consulta, edición y activación de veterinarios y
 * cajeros.
 * <p>
 * Cada empleado tiene una cuenta en {@code USUARIOS} y un perfil en {@code PERSONAL}. El alta
 * crea las dos en una sola transacción, y desactivar a un empleado es desactivar su cuenta
 * (el filtro JWT rechaza desde ese momento sus peticiones).
 */
@Service
@RequiredArgsConstructor
public class PersonalService {

    public static final String MENSAJE_CEDULA_OBLIGATORIA = "La cédula profesional es obligatoria para veterinarios";
    public static final String MENSAJE_SIN_PERFIL = "Tu cuenta no tiene un perfil de personal";

    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Da de alta a un empleado: crea su cuenta (contraseña cifrada con BCrypt) y su perfil.
     * <p>
     * Es <b>todo o nada</b>: si falla el perfil, también se deshace la cuenta.
     *
     * @throws ConflictException si el correo ya está registrado (409)
     * @throws BusinessException si es VETERINARIO y no trae cédula profesional (400)
     */
    @Transactional
    public PersonalResponseDTO crear(PersonalRequestDTO dto) {
        Rol rol = Rol.valueOf(dto.getRol());
        String cedula = textoOpcional(dto.getCedulaProfesional());
        validarCedula(rol, cedula);

        String correo = AuthService.normalizarCorreo(dto.getCorreo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictException(AuthService.MENSAJE_CORREO_REGISTRADO);
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .activo(true)
                .build());

        Personal personal = personalRepository.save(Personal.builder()
                .usuario(usuario)
                .nombre(dto.getNombre().trim())
                .apellidos(dto.getApellidos().trim())
                .telefono(dto.getTelefono().trim())
                .cedulaProfesional(cedula)
                .especialidadEspecies(textoOpcional(dto.getEspecialidadEspecies()))
                .build());

        return mapear(personal);
    }

    /**
     * Lista al personal ordenado por apellidos. Los filtros son opcionales y se combinan.
     *
     * @param rol    solo empleados con ese rol ({@code null} = todos)
     * @param activo solo cuentas activas ({@code true}) o desactivadas ({@code false}); {@code null} = ambas
     */
    @Transactional(readOnly = true)
    public List<PersonalResponseDTO> listar(Rol rol, Boolean activo) {
        List<Personal> personal = (rol == null)
                ? personalRepository.findAllByOrderByApellidosAscNombreAsc()
                : personalRepository.findByUsuarioRolOrderByApellidosAscNombreAsc(rol);

        return personal.stream()
                .filter(p -> activo == null || activo.equals(p.getUsuario().getActivo()))
                .map(this::mapear)
                .toList();
    }

    /** @throws ResourceNotFoundException si no existe (404) */
    @Transactional(readOnly = true)
    public PersonalResponseDTO obtenerPorId(Integer idPersonal) {
        return mapear(buscarOError(idPersonal));
    }

    /**
     * Perfil de quien inició sesión, a partir del correo de su token.
     *
     * @throws ResourceNotFoundException si su cuenta no tiene perfil de personal (404)
     */
    @Transactional(readOnly = true)
    public PersonalResponseDTO obtenerMiPerfil(String correo) {
        return mapear(buscarPorCorreoOError(correo));
    }

    /**
     * Edita los datos del perfil. El correo, el rol y la contraseña no cambian.
     *
     * @throws ResourceNotFoundException si no existe (404)
     * @throws BusinessException         si es VETERINARIO y se intenta quitar la cédula (400)
     */
    @Transactional
    public PersonalResponseDTO actualizar(Integer idPersonal, PersonalUpdateDTO dto) {
        Personal personal = buscarOError(idPersonal);
        String cedula = textoOpcional(dto.getCedulaProfesional());
        validarCedula(personal.getUsuario().getRol(), cedula);

        personal.setNombre(dto.getNombre().trim());
        personal.setApellidos(dto.getApellidos().trim());
        personal.setTelefono(dto.getTelefono().trim());
        personal.setCedulaProfesional(cedula);
        personal.setEspecialidadEspecies(textoOpcional(dto.getEspecialidadEspecies()));

        return mapear(personal);
    }

    /**
     * Desactiva la cuenta del empleado: no puede iniciar sesión y su token deja de servir.
     * Sus datos, turnos y bloqueos se conservan. Si ya estaba desactivada, no pasa nada.
     *
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public void desactivar(Integer idPersonal) {
        buscarOError(idPersonal).getUsuario().setActivo(false);
    }

    /**
     * Reactiva la cuenta del empleado. Si ya estaba activa, no pasa nada.
     *
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public PersonalResponseDTO reactivar(Integer idPersonal) {
        Personal personal = buscarOError(idPersonal);
        personal.getUsuario().setActivo(true);
        return mapear(personal);
    }

    /**
     * Busca un empleado o lanza 404. Es pública porque la usarán los servicios de horarios y
     * bloqueos del mismo módulo.
     */
    @Transactional(readOnly = true)
    public Personal buscarOError(Integer idPersonal) {
        return personalRepository.findById(idPersonal)
                .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado con id " + idPersonal));
    }

    /**
     * Busca el perfil de quien inició sesión (por el correo de su token) o lanza 404. La usan
     * los endpoints {@code /mi-perfil} del módulo.
     */
    @Transactional(readOnly = true)
    public Personal buscarPorCorreoOError(String correo) {
        return personalRepository.findByUsuarioCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException(MENSAJE_SIN_PERFIL));
    }

    // ---------- Ayudantes ----------

    /** La cédula identifica legalmente al médico (saldrá en las recetas): un veterinario debe tenerla. */
    private void validarCedula(Rol rol, String cedula) {
        if (rol == Rol.VETERINARIO && cedula == null) {
            throw new BusinessException(MENSAJE_CEDULA_OBLIGATORIA);
        }
    }

    /** Quita espacios; un texto vacío o solo con espacios se guarda como {@code null}. */
    private String textoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private PersonalResponseDTO mapear(Personal personal) {
        Usuario usuario = personal.getUsuario();
        return PersonalResponseDTO.builder()
                .idPersonal(personal.getIdPersonal())
                .idUsuario(usuario.getIdUsuario())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .nombre(personal.getNombre())
                .apellidos(personal.getApellidos())
                .telefono(personal.getTelefono())
                .cedulaProfesional(personal.getCedulaProfesional())
                .especialidadEspecies(personal.getEspecialidadEspecies())
                .build();
    }
}
