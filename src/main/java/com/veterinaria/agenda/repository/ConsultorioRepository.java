package com.veterinaria.agenda.repository;

import com.veterinaria.agenda.entity.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code CONSULTORIOS}. Las búsquedas por tipo usan el índice
 * {@code idx_consultorios_tipo}.
 */
public interface ConsultorioRepository extends JpaRepository<Consultorio, Integer> {

    /** Todos los consultorios, ordenados por nombre. */
    List<Consultorio> findAllByOrderByNombreAsc();

    /** Solo los activos, ordenados por nombre. */
    List<Consultorio> findByActivoTrueOrderByNombreAsc();

    /** Los de un tipo, ordenados por nombre. */
    List<Consultorio> findByTipoConsultorioIdTipoConsultorioOrderByNombreAsc(Integer idTipoConsultorio);

    /** Los activos de un tipo (ej. quirófanos disponibles para una cirugía), ordenados por nombre. */
    List<Consultorio> findByTipoConsultorioIdTipoConsultorioAndActivoTrueOrderByNombreAsc(Integer idTipoConsultorio);

    /** Busca por nombre sin importar mayúsculas (la base no lo impide; lo valida el servicio). */
    Optional<Consultorio> findByNombreIgnoreCase(String nombre);

    /** Cuántos consultorios activos tiene un tipo (no se desactiva un tipo en uso). */
    long countByTipoConsultorioIdTipoConsultorioAndActivoTrue(Integer idTipoConsultorio);
}
