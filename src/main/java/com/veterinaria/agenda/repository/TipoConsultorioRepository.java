package com.veterinaria.agenda.repository;

import com.veterinaria.agenda.entity.TipoConsultorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Cuántos servicios activos requieren este tipo de consultorio ({@code SERVICIOS.id_tipo_consultorio}).
     * <p>
     * Es una consulta directa a la tabla para no acoplar este módulo al código del módulo de
     * servicios (de la misma forma que ese módulo consulta {@code TIPOS_CONSULTORIO}).
     */
    @Query(value = "SELECT COUNT(*) FROM SERVICIOS WHERE id_tipo_consultorio = :idTipo AND activo = TRUE",
            nativeQuery = true)
    long contarServiciosActivos(@Param("idTipo") Integer idTipo);
}
