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

/**
 * Entidad que representa la tabla {@code TIPOS_CONSULTORIO}: las clases de espacio físico de la
 * clínica (Consulta, Cirugía, Estética…).
 * <p>
 * Cada servicio indica qué tipo de consultorio necesita ({@code SERVICIOS.id_tipo_consultorio},
 * módulo de servicios), y la agenda busca un consultorio libre de ese tipo. El módulo de
 * servicios solo acepta tipos <b>activos</b>, así que desactivar un tipo impide usarlo en
 * servicios nuevos.
 */
@Entity
@Table(name = "TIPOS_CONSULTORIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoConsultorio {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_consultorio")
    private Integer idTipoConsultorio;

    /** Nombre del tipo; único en la tabla. */
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    /** Descripción opcional. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /** Si es {@code false}, el tipo está desactivado y no se usa en servicios ni consultorios nuevos. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
