package com.veterinaria.clientes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información detallada de un cliente registrado")
public class ClienteResponseDTO {

    // Identificador numérico interno del cliente
    @Schema(description = "Identificador único del cliente", example = "1")
    private Integer idCliente;

    // Folio o código legible de negocio del cliente (Opción B: ej. CLI-0001)
    @Schema(description = "Folio o código de negocio legible del cliente", example = "CLI-0001")
    private String codigoCliente;

    // Identificador del usuario vinculado (si tiene cuenta de acceso)
    @Schema(description = "Identificador del usuario vinculado", example = "1", nullable = true)
    private Integer idUsuario;

    // Nombre(s) del cliente
    @Schema(description = "Nombre(s) del cliente", example = "Carlos Alberto")
    private String nombre;

    // Apellidos del cliente
    @Schema(description = "Apellidos del cliente", example = "Gómez Pérez")
    private String apellidos;

    // Teléfono principal de contacto
    @Schema(description = "Teléfono principal de contacto", example = "5512345678")
    private String telefono;

    // Dirección del domicilio
    @Schema(description = "Dirección del domicilio", example = "Av. Insurgentes Sur 123, Col. Roma, CDMX")
    private String direccion;

    // URL del documento de responsiva firmado
    @Schema(description = "URL del documento de responsiva firmado", example = "https://storage.veterinaria.com/responsivas/cliente_1.pdf")
    private String responsivaUrl;

    // Lista de contactos adicionales o de emergencia del cliente
    @Schema(description = "Lista de contactos adicionales o de emergencia del cliente")
    private List<ResponsableExtraResponseDTO> responsablesExtra;

    // Lista de mascotas registradas del cliente
    @Schema(description = "Lista de mascotas registradas del cliente")
    private List<MascotaResponseDTO> mascotas;
}
