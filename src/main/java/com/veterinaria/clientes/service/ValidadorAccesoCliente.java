package com.veterinaria.clientes.service;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.clientes.entity.Cliente;
import com.veterinaria.clientes.entity.Mascota;
import com.veterinaria.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Valida la pertenencia de recursos del módulo de clientes cuando el usuario autenticado
 * tiene rol {@link Rol#CLIENTE}.
 * <p>
 * DESCRIPCION DE ESTE ARCHIVO :V :
 * El personal de la clínica ({@code ADMIN}, {@code CAJERO}, {@code VETERINARIO}) puede operar
 * sobre cualquier cliente o mascota permitido por {@code @PreAuthorize}; en cambio, un usuario
 * con rol {@code CLIENTE} solo puede consultar o modificar su propio perfil, sus contactos de
 * emergencia y sus propias mascotas.
 */
@Component
@RequiredArgsConstructor
public class ValidadorAccesoCliente {

    private static final String AUTORIDAD_CLIENTE = "ROLE_" + Rol.CLIENTE.name();

    private final UsuarioRepository usuarioRepository;

    // Indica si la petición actual pertenece a un usuario autenticado con rol CLIENTE.
    public boolean esClienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> AUTORIDAD_CLIENTE.equals(authority.getAuthority()));
    }

    // Obtiene el {@code idUsuario} del usuario autenticado cuando tiene rol CLIENTE.
    // Si el usuario pertenece al personal de la clínica (o no hay rol CLIENTE), devuelve {@code null}.
    public Integer obtenerIdUsuarioClienteAutenticado() {
        if (!esClienteAutenticado()) {
            return null;
        }
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByCorreo(correo)
                .map(Usuario::getIdUsuario)
                .orElseThrow(() -> new ForbiddenException("No se pudo verificar la identidad del cliente autenticado"));
    }

    
     //Si el usuario autenticado es CLIENTE, verifica que el perfil de cliente objetivo le pertenezca.
    public void validarAccesoACliente(Cliente cliente) {
        Integer idUsuarioActual = obtenerIdUsuarioClienteAutenticado();
        if (idUsuarioActual == null) {
            return;
        }
        if (cliente == null || cliente.getIdUsuario() == null || !idUsuarioActual.equals(cliente.getIdUsuario())) {
            throw new ForbiddenException("No tienes permiso para acceder o modificar la información de otro cliente");
        }
    }

    
    // Si el usuario autenticado es CLIENTE, verifica que el {@code idUsuario} consultado sea el suyo.
    public void validarAccesoPorIdUsuario(Integer idUsuarioObjetivo) {
        Integer idUsuarioActual = obtenerIdUsuarioClienteAutenticado();
        if (idUsuarioActual == null) {
            return;
        }
        if (!idUsuarioActual.equals(idUsuarioObjetivo)) {
            throw new ForbiddenException("No tienes permiso para consultar el perfil de otro usuario");
        }
    }

    // Si el usuario autenticado es CLIENTE, verifica que la mascota pertenezca a su perfil.
    public void validarAccesoAMascota(Mascota mascota) {
        Integer idUsuarioActual = obtenerIdUsuarioClienteAutenticado();
        if (idUsuarioActual == null) {
            return;
        }
        Cliente propietario = (mascota != null) ? mascota.getCliente() : null;
        if (propietario == null || propietario.getIdUsuario() == null
                || !idUsuarioActual.equals(propietario.getIdUsuario())) {
            throw new ForbiddenException("No tienes permiso para acceder o modificar mascotas de otro cliente");
        }
    }
}
