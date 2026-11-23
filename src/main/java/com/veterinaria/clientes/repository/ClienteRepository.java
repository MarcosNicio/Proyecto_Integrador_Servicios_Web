package com.veterinaria.clientes.repository;

import com.veterinaria.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Busca el perfil de cliente vinculado a un ID de usuario del sistema
    Optional<Cliente> findByIdUsuario(Integer idUsuario);

    // Verifica si ya existe un cliente asociado a ese ID de usuario
    boolean existsByIdUsuario(Integer idUsuario);

    // Busca clientes cuyo teléfono coincida parcial o totalmente
    List<Cliente> findByTelefonoContaining(String telefono);

    // Busca clientes por coincidencia en nombre o apellidos (sin importar mayúsculas/minúsculas)
    List<Cliente> findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(String nombre, String apellidos);
}
