package com.veterinaria.personal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code BLOQUEOS_DISPONIBILIDAD}: periodos en los que una
 * persona del personal no está disponible aunque esté dentro de su turno (vacaciones,
 * cirugías, días libres).
 * <p>
 * La agenda (Sprint 3) excluye estos periodos al calcular los horarios libres. La base
 * garantiza que {@code fechaFin} sea posterior a {@code fechaInicio} ({@code chk_bloqueo_rango}).
 */
@Entity
@Table(name = "BLOQUEOS_DISPONIBILIDAD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "personal")
public class BloqueoDisponibilidad {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Integer idBloqueo;

    /** Persona cuya agenda se bloquea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal personal;

    /** Fecha y hora en que empieza el bloqueo. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /** Fecha y hora en que termina el bloqueo. */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    /** Motivo del bloqueo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 30)
    private MotivoBloqueo motivo;

    /** Detalle opcional (ej. "Congreso de cirugía en Guadalajara"). */
    @Column(name = "notas", length = 255)
    private String notas;
}
