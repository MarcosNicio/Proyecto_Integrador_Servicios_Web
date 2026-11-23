package com.veterinaria.clientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Datos requeridos para registrar o actualizar un cliente")
public class ClienteRequestDTO {

    // ID opcional del usuario asociado a la cuenta del cliente
    @Schema(description = "ID opcional del usuario asociado a la cuenta del cliente", example = "1", nullable = true)
    private Integer idUsuario;

    // Nombre(s) del cliente (obligatorio, máximo 80 caracteres)
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder los 80 caracteres")
    @Schema(description = "Nombre(s) del cliente", example = "Carlos Alberto")
    private String nombre;

    // Apellidos del cliente (obligatorio, máximo 100 caracteres)
    @NotBlank(message = "Los apellidos del cliente son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder los 100 caracteres")
    @Schema(description = "Apellidos del cliente", example = "Gómez Pérez")
    private String apellidos;

    // Teléfono de contacto (obligatorio, máximo 20 caracteres)
    @NotBlank(message = "El teléfono del cliente es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder los 20 caracteres")
    @Schema(description = "Teléfono principal de contacto", example = "5512345678")
    private String telefono;

    // Dirección física del cliente (opcional, máximo 255 caracteres)
    @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
    @Schema(description = "Dirección física del domicilio", example = "Av. Insurgentes Sur 123, Col. Roma, CDMX")
    private String direccion;

    // URL del documento de responsiva firmado (opcional, máximo 255 caracteres)
    @Size(max = 255, message = "La URL de la responsiva no puede exceder los 255 caracteres")
    @Schema(description = "URL del documento de carta responsiva firmada", example = "https://storage.veterinaria.com/responsivas/cliente_1.pdf")
    private String responsivaUrl;
}
