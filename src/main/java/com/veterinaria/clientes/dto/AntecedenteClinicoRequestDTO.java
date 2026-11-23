package com.veterinaria.clientes.dto;

import com.veterinaria.clientes.entity.TipoAntecedente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
@Schema(description = "Datos para registrar un antecedente clínico en el expediente de la mascota")
public class AntecedenteClinicoRequestDTO {

    // Tipo de antecedente clínico (ALERGIA, ENFERMEDAD_CRONICA, CIRUGIA_PREVIA)
    @NotNull(message = "El tipo de antecedente clínico es obligatorio")
    @Schema(description = "Categoría del antecedente clínico", example = "ALERGIA", allowableValues = {"ALERGIA", "ENFERMEDAD_CRONICA", "CIRUGIA_PREVIA"})
    private TipoAntecedente tipo;

    // Descripción detallada del antecedente médico
    @NotBlank(message = "La descripción del antecedente clínico es obligatoria")
    @Schema(description = "Descripción clínica detallada del antecedente", example = "Alergia severa a la penicilina y amoxicilina")
    private String descripcion;

    // Fecha en la que se registró o diagnosticó el antecedente (si no se envía, se puede usar la fecha actual)
    @PastOrPresent(message = "La fecha de registro no puede ser una fecha futura")
    @Schema(description = "Fecha de diagnóstico o registro del antecedente (formato YYYY-MM-DD)", example = "2025-10-12")
    private LocalDate fechaRegistro;
}
