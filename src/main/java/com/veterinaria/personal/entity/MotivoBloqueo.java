package com.veterinaria.personal.entity;

/**
 * Motivos por los que se bloquea la agenda de una persona del personal.
 * Son los mismos valores que permite la columna {@code BLOQUEOS_DISPONIBILIDAD.motivo}.
 */
public enum MotivoBloqueo {
    PERSONAL,   // Asunto personal, permiso o vacaciones
    CIRUGIA,    // Cirugía programada: el veterinario no atiende consultas
    DIA_LIBRE   // Día de descanso fuera de su turno habitual
}
