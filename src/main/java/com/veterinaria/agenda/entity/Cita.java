package com.veterinaria.agenda.entity;

import com.veterinaria.clientes.entity.Mascota;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.servicios.entity.Servicio;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Entidad JPA que representa la tabla CITAS: 
// reservas agendadas para la atención de una mascota
// con un veterinario, un servicio y (opcionalmente) un consultorio físico asignado.
// Comparte modelo entre el motor de disponibilidad/agendamiento y la gestión diaria en recepción.
// La base garantiza que fechaHoraFin sea posterior a fechaHoraInicio (chk_cita_rango)
// y que costoEstimado sea mayor o igual a 0 (chk_cita_costo).
@Entity
@Table(name = "CITAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"mascota", "servicio", "veterinario", "consultorio"})
public class Cita {

    // Identificador único de la cita (llave primaria autoincremental)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    // Mascota que recibirá la atención
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mascota", nullable = false)
    private Mascota mascota;

    // Servicio agendado (consulta, vacunación, cirugía, estética, etc.)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    // Veterinario asignado para atender la cita
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_veterinario", nullable = false)
    private Personal veterinario;

    // Consultorio físico asignado (opcional al agendar; puede asignarse o cambiarse en recepción)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consultorio")
    private Consultorio consultorio;

    // Fecha y hora de inicio programada para la cita
    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    // Fecha y hora de fin programada para la cita
    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    // Costo estimado calculado al momento de agendar la cita
    @Column(name = "costo_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    // Estado actual de la cita en el flujo de recepción y atención
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoCita estado = EstadoCita.PENDIENTE;

    // Notas u observaciones operativas (motivo de cancelación, indicaciones de llegada, etc.)
    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    // Fecha y hora de registro de la cita (no se modifica después de crearse)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Se ejecuta automáticamente antes de insertar el registro.
    // Asigna el estado inicial y la fecha de creación si no se indicaron, porque Hibernate envía
    // todas las columnas en el INSERT y los DEFAULT de la tabla no se aplicarían.
    @PrePersist
    void prePersist() {
        if (estado == null) {
            estado = EstadoCita.PENDIENTE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
