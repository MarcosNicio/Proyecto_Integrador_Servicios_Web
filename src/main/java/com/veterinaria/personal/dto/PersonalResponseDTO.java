package com.veterinaria.personal.dto;

import com.veterinaria.auth.entity.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos de un empleado que devuelve la API: su perfil más los datos de su cuenta (correo,
 * rol y si está activa). Nunca incluye la contraseña.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de un empleado de la clínica")
public class PersonalResponseDTO {

    @Schema(description = "Identificador del perfil de personal", example = "1")
    private Integer idPersonal;

    @Schema(description = "Identificador de su cuenta de usuario", example = "2")
    private Integer idUsuario;

    @Schema(description = "Correo con el que inicia sesión", example = "laura.zamora@veterinaria.com")
    private String correo;

    @Schema(description = "Rol del empleado", example = "VETERINARIO")
    private Rol rol;

    @Schema(description = "Si es false, la cuenta está desactivada y no puede iniciar sesión", example = "true")
    private Boolean activo;

    @Schema(description = "Nombre(s)", example = "Laura")
    private String nombre;

    @Schema(description = "Apellidos", example = "Zamora Ruiz")
    private String apellidos;

    @Schema(description = "Teléfono de contacto", example = "5551112222")
    private String telefono;

    @Schema(description = "Cédula profesional", example = "12345678", nullable = true)
    private String cedulaProfesional;

    @Schema(description = "Especies que atiende", example = "Perros, Gatos", nullable = true)
    private String especialidadEspecies;
}
