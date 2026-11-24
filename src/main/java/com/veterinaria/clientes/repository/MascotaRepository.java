package com.veterinaria.clientes.repository;

import com.veterinaria.clientes.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Integer> {

    // Lista todas las mascotas de un cliente (incluyendo activas e inactivas)
    List<Mascota> findByClienteIdCliente(Integer idCliente);

    // Lista únicamente las mascotas activas de un cliente
    List<Mascota> findByClienteIdClienteAndActivoTrue(Integer idCliente);

    // Lista todas las mascotas activas registradas en la clínica
    List<Mascota> findByActivoTrue();

    // Busca mascotas activas por coincidencia en su nombre
    List<Mascota> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
}
