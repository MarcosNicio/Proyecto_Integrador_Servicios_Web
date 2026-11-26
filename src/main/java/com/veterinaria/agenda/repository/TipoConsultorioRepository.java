package com.veterinaria.agenda.repository;

import com.veterinaria.agenda.entity.TipoConsultorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code TIPOS_CONSULTORIO}.
 */
public interface TipoConsultorioRepository extends JpaRepository<TipoConsultorio, Integer> {

    /** Todos los tipos, ordenados por nombre. */
    List<TipoConsultorio> findAllByOrderByNombreAsc();

    /** Solo los tipos activos, ordenados por nombre. */
    List<TipoConsultorio> findByActivoTrueOrderByNombreAsc();

    /** Busca por nombre sin importar mayúsculas (para evitar "Cirugía" y "cirugía"). */
    Optional<TipoConsultorio> findByNombreIgnoreCase(String nombre);
}
