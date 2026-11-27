package com.veterinaria.personal.repository;

import com.veterinaria.personal.entity.BloqueoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code BLOQUEOS_DISPONIBILIDAD}. Usa el índice
 * {@code idx_bloqueos_personal_fecha (id_personal, fecha_inicio)}.
 */
public interface BloqueoDisponibilidadRepository extends JpaRepository<BloqueoDisponibilidad, Integer> {

    /** Todos los bloqueos de una persona, del más antiguo al más reciente. */
    List<BloqueoDisponibilidad> findByPersonalIdPersonalOrderByFechaInicioAsc(Integer idPersonal);

    /** Bloqueos que aún no terminan (vigentes o futuros), del más próximo al más lejano. */
    List<BloqueoDisponibilidad> findByPersonalIdPersonalAndFechaFinAfterOrderByFechaInicioAsc(
            Integer idPersonal, LocalDateTime desde);

    /** Un bloqueo concreto, solo si pertenece a esa persona. */
    Optional<BloqueoDisponibilidad> findByIdBloqueoAndPersonalIdPersonal(Integer idBloqueo, Integer idPersonal);

    /**
     * Bloqueos de una persona que se enciman con el rango {@code [inicio, fin)}.
     * <p>
     * Dos rangos se enciman si cada uno empieza antes de que termine el otro. Si un bloqueo
     * termina justo cuando empieza el otro (ej. 10:00-12:00 y 12:00-14:00) no se enciman.
     * Lo usarán la validación de nuevos bloqueos y la agenda del Sprint 3.
     */
    @Query("""
            SELECT b FROM BloqueoDisponibilidad b
            WHERE b.personal.idPersonal = :idPersonal
              AND b.fechaInicio < :fin
              AND b.fechaFin > :inicio
            ORDER BY b.fechaInicio
            """)
    List<BloqueoDisponibilidad> buscarEncimados(@Param("idPersonal") Integer idPersonal,
                                                @Param("inicio") LocalDateTime inicio,
                                                @Param("fin") LocalDateTime fin);
}
