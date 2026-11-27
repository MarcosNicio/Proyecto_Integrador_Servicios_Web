package com.veterinaria.personal.entity;

import com.veterinaria.auth.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad que representa la tabla {@code PERSONAL}: el perfil de los empleados de la clínica
 * (veterinarios y cajeros).
 * <p>
 * Cada persona del personal tiene exactamente una cuenta en {@code USUARIOS}. De esa cuenta se
 * obtienen su correo, su rol y si está activa: la tabla {@code PERSONAL} no tiene columna
 * {@code activo}, así que dar de baja a alguien es desactivar su usuario.
 */
@Entity
@Table(name = "PERSONAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "usuario")
public class Personal {

    /** Identificador único (llave primaria autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personal")
    private Integer idPersonal;

    /** Cuenta con la que inicia sesión; una cuenta solo puede tener un perfil de personal. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    /** Nombre(s). */
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    /** Apellidos. */
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    /** Teléfono de contacto. */
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    /** Cédula profesional; aplica a veterinarios, es opcional para el resto. */
    @Column(name = "cedula_profesional", length = 50)
    private String cedulaProfesional;

    /** Especies que atiende, en texto libre (ej. "Perros, Gatos"); opcional. */
    @Column(name = "especialidad_especies", length = 150)
    private String especialidadEspecies;
}
