package com.veterinaria.auth.repository;

import com.veterinaria.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a la tabla {@code USUARIOS}.
 * <p>
 * {@link JpaRepository} ya incluye {@code save}, {@code findById}, {@code findAll},
 * {@code deleteById}, etc. Los métodos de abajo los implementa Spring Data JPA
 * automáticamente a partir de su nombre (no hay que escribir SQL).
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su correo. Se usará en el login.
     * Equivale a: {@code SELECT * FROM usuarios WHERE correo = ?}
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Indica si ya existe un usuario con ese correo. Se usará en el registro
     * para no permitir correos repetidos.
     */
    boolean existsByCorreo(String correo);
}
