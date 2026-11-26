package com.veterinaria.agenda.entity;

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

/**
 * Entidad que representa la tabla {@code CONSULTORIOS}: los espacios físicos de la clínica
 * (Consultorio 1, Quirófano…). La agenda (Sprint 3) asigna cada cita a uno de ellos.
 * <p>
 * Un consultorio desactivado no se borra (las citas pasadas lo siguen referenciando), solo deja
 * de ofrecerse para citas nuevas.
 */
@Entity
@Table(name = "CONSULTORIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "tipoConsultorio")
public class Consultorio {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consultorio")
    private Integer idConsultorio;

    /** Tipo de espacio (Consulta, Cirugía, Estética…). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_consultorio", nullable = false)
    private TipoConsultorio tipoConsultorio;

    /** Nombre visible (ej. "Consultorio 1"). */
    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    /** Si es {@code false}, el consultorio no se ofrece para citas nuevas. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
