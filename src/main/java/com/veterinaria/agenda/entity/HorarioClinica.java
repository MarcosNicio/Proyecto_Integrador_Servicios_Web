package com.veterinaria.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * Entidad que representa la tabla {@code HORARIOS_CLINICA}: el horario de atención de la
 * clínica, un registro por día de la semana.
 * <p>
 * La base garantiza que cada día aparezca una sola vez ({@code UNIQUE}) y que el cierre sea
 * posterior a la apertura ({@code chk_horario_clinica_rango}), incluso en días no laborables.
 * La agenda (Sprint 3) solo ofrece citas en días laborables y dentro de este horario.
 */
@Entity
@Table(name = "HORARIOS_CLINICA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioClinica {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_clinica")
    private Integer idHorarioClinica;

    /**
     * Día de la semana: 0 = domingo, 1 = lunes … 6 = sábado (misma numeración que
     * {@code HORARIOS_PERSONAL}). Único en la tabla. En la base es {@code SMALLINT}.
     */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dia_semana", nullable = false, unique = true)
    private Integer diaSemana;

    /** Hora en que abre la clínica ese día. */
    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    /** Hora en que cierra la clínica ese día. */
    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    /** Si es {@code false}, la clínica no abre ese día (aunque conserve sus horas). */
    @Column(name = "laborable", nullable = false)
    @Builder.Default
    private Boolean laborable = true;
}
