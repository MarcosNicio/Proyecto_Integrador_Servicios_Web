package com.veterinaria.auth.entity;

/**
 * Roles de los usuarios del sistema.
 * <p>
 * Coinciden con el {@code CHECK} de la columna {@code USUARIOS.rol} y se guardan como texto
 * ({@code @Enumerated(EnumType.STRING)}), así que el nombre de cada valor no debe cambiarse.
 */
public enum Rol {
    /** Administrador: gestiona personal, configuración de la clínica y catálogos. */
    ADMIN,
    /** Veterinario: atiende consultas, expediente clínico y recetas. */
    VETERINARIO,
    /** Cajero: cobros, pagos y emisión de recibos. */
    CAJERO,
    /** Cliente: dueño de mascotas, agenda citas y consulta su información. */
    CLIENTE
}
