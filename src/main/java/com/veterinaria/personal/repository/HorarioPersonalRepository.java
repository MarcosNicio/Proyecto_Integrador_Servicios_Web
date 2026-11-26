package com.veterinaria.personal.repository;

import com.veterinaria.personal.entity.HorarioPersonal;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
