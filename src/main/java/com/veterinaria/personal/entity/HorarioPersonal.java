package com.veterinaria.personal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * Entidad que representa la tabla {@code HORARIOS_PERSONAL}: los turnos de trabajo semanales
 * de cada persona del personal.
 * <p>
 * Una persona puede tener varios turnos el mismo día (ej. 08:00-13:00 y 16:00-20:00).
 * La base garantiza que {@code horaFin} sea posterior a {@code horaInicio}
 * ({@code chk_horario_personal_rango}).
 */
@Entity
@Table(name = "HORARIOS_PERSONAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "personal")
public class HorarioPersonal {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_personal")
    private Integer idHorarioPersonal;

    /** Persona a la que pertenece el turno. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal personal;

    /**
     * Día de la semana: 0 = domingo, 1 = lunes … 6 = sábado (misma numeración que
     * {@code HORARIOS_CLINICA}). En la base es {@code SMALLINT}.
     */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    /** Hora en que empieza el turno. */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /** Hora en que termina el turno. */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
