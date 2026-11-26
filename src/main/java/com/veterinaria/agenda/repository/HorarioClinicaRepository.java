package com.veterinaria.agenda.repository;

import com.veterinaria.agenda.entity.HorarioClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code HORARIOS_CLINICA} (un registro por día de la semana).
 */
public interface HorarioClinicaRepository extends JpaRepository<HorarioClinica, Integer> {

    /** Horario de un día (0 = domingo … 6 = sábado). */
    Optional<HorarioClinica> findByDiaSemana(Integer diaSemana);

    /** La semana completa, de domingo a sábado. */
    List<HorarioClinica> findAllByOrderByDiaSemanaAsc();
}
