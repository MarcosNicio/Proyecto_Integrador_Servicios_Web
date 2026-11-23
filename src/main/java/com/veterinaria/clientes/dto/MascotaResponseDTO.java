package com.veterinaria.clientes.dto;

import com.veterinaria.clientes.entity.SexoMascota;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información completa del perfil de una mascota")
public class MascotaResponseDTO {

    // Identificador único de la mascota
    @Schema(description = "Identificador único de la mascota", example = "1")
    private Integer idMascota;

    // Identificador del cliente propietario
    @Schema(description = "Identificador del cliente propietario", example = "1")
    private Integer idCliente;

    // Nombre completo del cliente propietario
    @Schema(description = "Nombre completo del cliente propietario", example = "Carlos Alberto Gómez Pérez")
    private String nombreCliente;

    // Nombre de la mascota
    @Schema(description = "Nombre de la mascota", example = "Firulais")
    private String nombre;

    // Especie animal
    @Schema(description = "Especie animal", example = "Perro")
    private String especie;

    // Raza de la mascota
    @Schema(description = "Raza de la mascota", example = "Golden Retriever")
    private String raza;

    // Fecha de nacimiento registrada
    @Schema(description = "Fecha de nacimiento registrada", example = "2023-05-15")
    private LocalDate fechaNacimiento;

    // Peso actual en kilogramos
    @Schema(description = "Peso actual en kilogramos", example = "24.50")
    private BigDecimal pesoActualKg;

    // Sexo de la mascota (MACHO o HEMBRA)
    @Schema(description = "Sexo de la mascota", example = "MACHO")
    private SexoMascota sexo;

    // Estado de esterilización
    @Schema(description = "Estado de esterilización", example = "true")
    private Boolean esterilizado;

    // URL de la fotografía de perfil
    @Schema(description = "URL de la fotografía de perfil", example = "https://storage.veterinaria.com/mascotas/firulais.jpg")
    private String fotoUrl;

    // Estado de la mascota en el sistema (true = activa, false = baja lógica)
    @Schema(description = "Estado de la mascota en el sistema (true = activa, false = baja lógica)", example = "true")
    private Boolean activo;

    // Lista de antecedentes clínicos registrados para la mascota
    @Schema(description = "Lista de antecedentes clínicos registrados para la mascota")
    private List<AntecedenteClinicoResponseDTO> antecedentesClinicos;
}
