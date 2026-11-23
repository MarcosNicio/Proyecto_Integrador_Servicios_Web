package com.veterinaria.auth.repository;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la entidad {@link Usuario} y de {@link UsuarioRepository} contra la base real.
 * <p>
 * {@code @DataJpaTest} levanta solo la parte de JPA (entidades y repositorios), no toda la app.
 * {@code @AutoConfigureTestDatabase(replace = NONE)} indica que use PostgreSQL y no una base
 * en memoria. Cada prueba corre dentro de una transacción que se <b>deshace al terminar</b>,
 * así que los usuarios de prueba no quedan guardados en la base.
 * <p>
 * Que esta clase arranque ya confirma que la entidad coincide con la tabla
 * ({@code ddl-auto=validate}).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** El administrador que carga el script inicial se encuentra por su correo. */
    @Test
    void encuentraAlAdministradorInicialPorCorreo() {
        Optional<Usuario> admin = usuarioRepository.findByCorreo("admin@veterinaria.com");

        assertTrue(admin.isPresent());
        assertEquals(Rol.ADMIN, admin.get().getRol());
        assertTrue(admin.get().getActivo());
    }

    /** existsByCorreo distingue correos registrados de los que no existen. */
    @Test
    void existsByCorreoIndicaSiElCorreoEstaRegistrado() {
        assertTrue(usuarioRepository.existsByCorreo("admin@veterinaria.com"));
        assertFalse(usuarioRepository.existsByCorreo("no-existe@correo.com"));
    }

    /** Al guardar un usuario se genera su id y se asignan fecha de creación y estado activo. */
    @Test
    void guardarUsuarioAsignaIdFechaYActivo() {
        Usuario nuevo = Usuario.builder()
                .correo("prueba-repo@correo.com")
                .passwordHash("$2a$10$hashDePruebaNoReal")
                .rol(Rol.CLIENTE)
                .build();

        Usuario guardado = usuarioRepository.saveAndFlush(nuevo);

        assertNotNull(guardado.getIdUsuario());
        assertNotNull(guardado.getCreatedAt());
        assertTrue(guardado.getActivo());
        assertEquals(Rol.CLIENTE, usuarioRepository.findByCorreo("prueba-repo@correo.com").orElseThrow().getRol());
    }
}
