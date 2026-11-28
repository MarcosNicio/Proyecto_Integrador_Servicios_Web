package com.veterinaria.agenda.entity;

// Estados posibles en el ciclo de vida de una cita veterinaria.
// Corresponden a los valores permitidos por la restricción CHECK de la columna CITAS.estado.
public enum EstadoCita {
    PENDIENTE,       // Cita agendada en espera de la llegada del cliente
    EN_SALA_ESPERA,  // Cliente y mascota registrados en recepción (check-in)
    EN_ATENCION,     // Paciente ingresado a consultorio con el veterinario
    COMPLETADA,      // Atención clínica finalizada (cerrada desde el expediente SOAP)
    CANCELADA        // Cita cancelada antes de ser atendida
}
