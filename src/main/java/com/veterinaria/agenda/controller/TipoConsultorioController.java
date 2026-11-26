package com.veterinaria.agenda.controller;

import com.veterinaria.agenda.dto.TipoConsultorioRequestDTO;
import com.veterinaria.agenda.dto.TipoConsultorioResponseDTO;
import com.veterinaria.agenda.service.TipoConsultorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de los tipos de consultorio (Consulta, Cirugía, Estética…).
 * <p>
 * Cualquier usuario con sesión los consulta; solo el ADMIN los modifica. No hay {@code DELETE}:
 * los tipos se desactivan.
 */
@RestController
@RequestMapping("/api/consultorios/tipos")
@RequiredArgsConstructor
@Tag(name = "Tipos de consultorio", description = "Clases de espacio físico de la clínica")
public class TipoConsultorioController {

    private final TipoConsultorioService tipoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar los tipos de consultorio",
            description = "Ordenados por nombre. Cada tipo trae cuántos consultorios activos tiene.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipos obtenidos"),
            @ApiResponse(responseCode = "401", description = "Sin sesión")
    })
    public ResponseEntity<List<TipoConsultorioResponseDTO>> listar(
            @Parameter(description = "true = solo activos, false = solo desactivados", example = "true")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(tipoService.listar(activo));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un tipo de consultorio")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tipo creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los tipos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un tipo con ese nombre")
    })
    public ResponseEntity<TipoConsultorioResponseDTO> crear(@Valid @RequestBody TipoConsultorioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoService.crear(dto));
    }

    @PutMapping("/{idTipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar un tipo de consultorio", description = "Edita el nombre y la descripción.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los tipos"),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Otro tipo ya tiene ese nombre")
    })
    public ResponseEntity<TipoConsultorioResponseDTO> actualizar(
            @Parameter(description = "Identificador del tipo", example = "1") @PathVariable Integer idTipo,
            @Valid @RequestBody TipoConsultorioRequestDTO dto) {
        return ResponseEntity.ok(tipoService.actualizar(idTipo, dto));
    }

    @PatchMapping("/{idTipo}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar un tipo de consultorio",
            description = "El tipo deja de aceptarse en servicios y consultorios nuevos. No se puede desactivar "
                    + "si todavía tiene consultorios activos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tipo desactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los tipos"),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El tipo tiene consultorios activos")
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Identificador del tipo", example = "1") @PathVariable Integer idTipo) {
        tipoService.desactivar(idTipo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idTipo}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar un tipo de consultorio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipo reactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los tipos"),
            @ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    })
    public ResponseEntity<TipoConsultorioResponseDTO> reactivar(
            @Parameter(description = "Identificador del tipo", example = "1") @PathVariable Integer idTipo) {
        return ResponseEntity.ok(tipoService.reactivar(idTipo));
    }
}
