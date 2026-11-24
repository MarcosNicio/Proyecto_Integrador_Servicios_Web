package com.veterinaria.servicios.entity;

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

//Entidad JPA que representa la tabla CATEGORIAS_SERVICIO.

 /* Agrupa lógicamente los servicios clínicos y estéticos de la veterinaria
 * (por ejemplo: Consulta General, Vacunación, Estética, Urgencia, Cirugía).
 */
@Entity
@Table(name = "CATEGORIAS_SERVICIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaServicio {

    // Identificador único de la categoría de servicio (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    // Nombre único de la categoría de servicio
    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    // Descripción opcional del alcance de la categoría
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // Bandera de estado para baja lógica (true = activa)
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
