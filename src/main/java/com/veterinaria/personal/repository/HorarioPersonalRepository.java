package com.veterinaria.personal.repository;

import com.veterinaria.personal.entity.HorarioPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code HORARIOS_PERSONAL}. Usa el índice
 * {@code idx_horarios_personal_dia (id_personal, dia_semana)}.
 */
public interface HorarioPersonalRepository extends JpaRepository<HorarioPersonal, Integer> {

    /** Todos los turnos de una persona, de domingo a sábado y por hora de inicio. */
    List<HorarioPersonal> findByPersonalIdPersonalOrderByDiaSemanaAscHoraInicioAsc(Integer idPersonal);

    /** Turnos de una persona en un día de la semana (para revisar que no se encimen). */
    List<HorarioPersonal> findByPersonalIdPersonalAndDiaSemanaOrderByHoraInicioAsc(Integer idPersonal, Integer diaSemana);

    /** Un turno concreto, solo si pertenece a esa persona. */
    Optional<HorarioPersonal> findByIdHorarioPersonalAndPersonalIdPersonal(Integer idHorarioPersonal, Integer idPersonal);

    /**
     * Los turnos de todo el personal, con su empleado ya cargado (para nombrarlos sin consultas
     * extra), ordenados por apellidos, nombre, día y hora. Lo usa la revisión del horario de la clínica.
     */
    @Query("""
            SELECT h FROM HorarioPersonal h JOIN FETCH h.personal p
            ORDER BY p.apellidos, p.nombre, h.diaSemana, h.horaInicio
            """)
    List<HorarioPersonal> findAllConPersonal();
}
