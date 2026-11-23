package com.veterinaria.clientes.dto;

import com.veterinaria.clientes.entity.TipoAntecedente;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de un antecedente clínico registrado")
public class AntecedenteClinicoResponseDTO {

    // Identificador único del antecedente clínico
    @Schema(description = "Identificador único del antecedente clínico", example = "1")
    private Integer idAntecedente;

    // Identificador de la mascota a la que pertenece el antecedente
    @Schema(description = "Identificador de la mascota a la que pertenece", example = "1")
    private Integer idMascota;

    // Tipo de antecedente (ALERGIA, ENFERMEDAD_CRONICA, CIRUGIA_PREVIA)
    @Schema(description = "Tipo de antecedente clínico", example = "ALERGIA")
    private TipoAntecedente tipo;

    // Descripción clínica detallada
    @Schema(description = "Descripción clínica detallada", example = "Alergia severa a la penicilina y amoxicilina")
    private String descripcion;

    // Fecha de registro del antecedente
    @Schema(description = "Fecha de registro del antecedente", example = "2025-10-12")
    private LocalDate fechaRegistro;
}
