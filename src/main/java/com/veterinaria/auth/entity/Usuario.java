package com.veterinaria.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla {@code USUARIOS}: las cuentas con acceso al sistema.
 * <p>
 * Cada objeto {@code Usuario} equivale a una fila de la tabla. Como el proyecto usa
 * {@code spring.jpa.hibernate.ddl-auto=validate}, al arrancar Hibernate comprueba que
 * los nombres y tipos de estas columnas coincidan con el script {@code database/veterinaria_db.sql}.
 * <p>
 * Un usuario puede estar vinculado a un perfil de {@code CLIENTES} (rol CLIENTE) o de
 * {@code PERSONAL} (ADMIN, VETERINARIO, CAJERO); esos perfiles guardan el {@code id_usuario}.
 */
@Entity
@Table(name = "USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class Usuario {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    /** Correo con el que inicia sesión; único en todo el sistema. */
    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    /**
     * Contraseña cifrada con BCrypt. <b>Nunca</b> se guarda la contraseña en texto plano.
     * Se excluye de {@code toString()} para que no aparezca en los logs.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Rol que define a qué partes del sistema tiene acceso. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    /** Si es {@code false}, la cuenta está desactivada y no puede iniciar sesión. */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** Fecha y hora de creación de la cuenta; no se modifica después. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Se ejecuta automáticamente antes de insertar el registro.
     * Asigna la fecha de creación y el estado activo si no se indicaron, porque Hibernate
     * envía todas las columnas en el INSERT y los DEFAULT de la tabla no se aplicarían.
     */
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
    }
}
