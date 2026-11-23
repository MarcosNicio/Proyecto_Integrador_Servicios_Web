package com.veterinaria.clientes.entity;

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

import java.time.LocalDate;

@Entity
@Table(name = "ANTECEDENTES_CLINICOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntecedenteClinico {

    // Identificador único del antecedente clínico (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_antecedente")
    private Integer idAntecedente;

    // Mascota a la que pertenece el antecedente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mascota", nullable = false)
    private Mascota mascota;

    // Clasificación del antecedente (ALERGIA, ENFERMEDAD_CRONICA, CIRUGIA_PREVIA)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoAntecedente tipo;

    // Detalle clínico descriptivo del antecedente
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // Fecha en que se registró el antecedente
    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;
}
