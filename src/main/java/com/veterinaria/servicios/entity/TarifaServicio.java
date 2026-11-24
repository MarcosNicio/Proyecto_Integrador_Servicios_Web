package com.veterinaria.servicios.entity;

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

import java.math.BigDecimal;

//Entidad JPA que representa la tabla TARIFAS_SERVICIO.
 
/* Define reglas de precio diferenciado para un servicio en función
 * de la especie del paciente y un rango de peso en kilogramos.
 */
@Entity
@Table(name = "TARIFAS_SERVICIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaServicio {

    // Identificador único de la regla tarifaria (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarifa")
    private Integer idTarifa;

    // Servicio al que pertenece esta tarifa dinámica
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Especie animal a la que aplica la tarifa (almacenada de forma normalizada)
    @Column(name = "especie", nullable = false, length = 50)
    private String especie;

    // Límite inferior del rango de peso en kilogramos (peso_min_kg >= 0)
    @Column(name = "peso_min_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoMinKg;

    // Límite superior del rango de peso en kilogramos (peso_max_kg > peso_min_kg)
    @Column(name = "peso_max_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoMaxKg;

    // Precio calculado aplicable cuando la especie y el peso coinciden con la regla
    @Column(name = "precio_calculado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCalculado;
}
