package com.veterinaria.clientes.repository;

import com.veterinaria.clientes.entity.ResponsableExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResponsableExtraRepository extends JpaRepository<ResponsableExtra, Integer> {

    // Obtiene todos los contactos adicionales o de emergencia de un cliente
    List<ResponsableExtra> findByClienteIdCliente(Integer idCliente);

    // Busca un responsable extra específico validando que pertenezca al cliente indicado
    Optional<ResponsableExtra> findByIdResponsableAndClienteIdCliente(Integer idResponsable, Integer idCliente);
}
