package com.veterinaria.clientes.dto;

import com.veterinaria.clientes.entity.SexoMascota;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos requeridos para registrar o actualizar una mascota")
public class MascotaRequestDTO {

    // Identificador del cliente propietario de la mascota
    @NotNull(message = "El ID del cliente propietario es obligatorio")
    @Schema(description = "Identificador del cliente propietario de la mascota", example = "1")
    private Integer idCliente;

    // Nombre de la mascota (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(max = 80, message = "El nombre de la mascota no puede exceder los 80 caracteres")
    @Schema(description = "Nombre de la mascota", example = "Firulais")
    private String nombre;

    // Especie animal, ej. Perro, Gato (obligatorio, máximo 50 caracteres)
    @NotBlank(message = "La especie de la mascota es obligatoria")
    @Size(max = 50, message = "La especie no puede exceder los 50 caracteres")
    @Schema(description = "Especie animal", example = "Perro")
    private String especie;

    // Raza de la mascota (opcional, máximo 80 caracteres)
    @Size(max = 80, message = "La raza no puede exceder los 80 caracteres")
    @Schema(description = "Raza de la mascota", example = "Golden Retriever")
    private String raza;

    // Fecha de nacimiento de la mascota (no puede ser una fecha futura)
    @PastOrPresent(message = "La fecha de nacimiento no puede ser una fecha futura")
    @Schema(description = "Fecha de nacimiento de la mascota (formato YYYY-MM-DD)", example = "2023-05-15")
    private LocalDate fechaNacimiento;

    // Peso actual en kilogramos (debe ser mayor a 0 y máximo 999.99)
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0 kg")
    @Digits(integer = 3, fraction = 2, message = "El peso debe tener máximo 3 dígitos enteros y 2 decimales")
    @Schema(description = "Peso actual en kilogramos", example = "24.50")
    private BigDecimal pesoActualKg;

    // Sexo biológico de la mascota (MACHO o HEMBRA)
    @NotNull(message = "El sexo de la mascota es obligatorio")
    @Schema(description = "Sexo biológico de la mascota", example = "MACHO", allowableValues = {"MACHO", "HEMBRA"})
    private SexoMascota sexo;

    // Indica si la mascota se encuentra esterilizada
    @Schema(description = "Indica si la mascota se encuentra esterilizada", example = "true")
    private Boolean esterilizado;

    // URL de la fotografía de la mascota (opcional, máximo 255 caracteres)
    @Size(max = 255, message = "La URL de la foto no puede exceder los 255 caracteres")
    @Schema(description = "URL de la fotografía de perfil de la mascota", example = "https://storage.veterinaria.com/mascotas/firulais.jpg")
    private String fotoUrl;
}
