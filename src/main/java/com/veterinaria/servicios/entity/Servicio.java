package com.veterinaria.servicios.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

//Entidad JPA que representa la tabla SERVICIOS.

 /* Define el catálogo de servicios veterinarios con su categoría, tipo de consultorio
 * desacoplado, duración estimada, precio base y reglas tarifarias dinámicas asociadas.
 */
@Entity
@Table(name = "SERVICIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    // Identificador único del servicio (llave primaria)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    // Categoría a la que pertenece el servicio
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaServicio categoria;

    // Referencia opcional al ID de tipo de consultorio requerido (desacoplado de TipoConsultorio)
    @Column(name = "id_tipo_consultorio")
    private Integer idTipoConsultorio;

    // Nombre descriptivo del servicio
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Descripción detallada opcional del servicio
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Duración estimada del servicio en minutos (debe ser mayor a 0)
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // Precio base utilizado como tarifa por defecto o fallback
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    // Bandera de estado para baja lógica (true = activo)
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // Lista de tarifas dinámicas diferenciadas por especie y rango de peso
    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TarifaServicio> tarifas = new ArrayList<>();

    /**
     * Asocia una tarifa dinámica al servicio manteniendo sincronizada
     * la relación bidireccional entre Servicio y TarifaServicio.
     * @param tarifa tarifa diferenciada a agregar al servicio
     */
    public void agregarTarifa(TarifaServicio tarifa) {
        tarifas.add(tarifa);
        tarifa.setServicio(this);
    }

    /**
     * Desvincula una tarifa dinámica del servicio manteniendo sincronizada
     * la relación bidireccional para habilitar orphanRemoval.
     * @param tarifa tarifa diferenciada a remover del servicio
     */
    public void eliminarTarifa(TarifaServicio tarifa) {
        tarifas.remove(tarifa);
        tarifa.setServicio(null);
    }
}
