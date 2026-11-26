package com.veterinaria.personal.repository;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.personal.entity.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code PERSONAL}. Las búsquedas por rol o correo navegan a la cuenta
 * vinculada en {@code USUARIOS}.
 */
public interface PersonalRepository extends JpaRepository<Personal, Integer> {

    /** Perfil de personal vinculado a una cuenta de usuario. */
    Optional<Personal> findByUsuarioIdUsuario(Integer idUsuario);

    /** Perfil de personal de quien inició sesión (el token trae su correo). */
    Optional<Personal> findByUsuarioCorreo(String correo);

    /** Si la cuenta de usuario ya tiene un perfil de personal. */
    boolean existsByUsuarioIdUsuario(Integer idUsuario);

    /** Todo el personal, ordenado por apellidos y nombre. */
    List<Personal> findAllByOrderByApellidosAscNombreAsc();

    /** Personal con un rol (ej. solo veterinarios), ordenado por apellidos y nombre. */
    List<Personal> findByUsuarioRolOrderByApellidosAscNombreAsc(Rol rol);
}
