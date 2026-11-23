package com.veterinaria.clientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de un contacto adicional o de emergencia registrado")
public class ResponsableExtraResponseDTO {

    // Identificador único del responsable extra
    @Schema(description = "Identificador único del responsable extra", example = "1")
    private Integer idResponsable;

    // Identificador del cliente titular al que pertenece
    @Schema(description = "Identificador del cliente titular", example = "1")
    private Integer idCliente;

    // Nombre completo del responsable adicional
    @Schema(description = "Nombre completo del responsable adicional", example = "María Fernanda Pérez López")
    private String nombreCompleto;

    // Teléfono de contacto del responsable
    @Schema(description = "Teléfono de contacto del responsable", example = "5598765432")
    private String telefono;

    // Parentesco o relación con el cliente titular
    @Schema(description = "Parentesco o relación con el cliente titular", example = "Esposa")
    private String parentesco;
}
