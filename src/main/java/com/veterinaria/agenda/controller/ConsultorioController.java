package com.veterinaria.agenda.controller;

import com.veterinaria.agenda.dto.ConsultorioRequestDTO;
import com.veterinaria.agenda.dto.ConsultorioResponseDTO;
import com.veterinaria.agenda.service.ConsultorioService;
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
 * Endpoints de los consultorios de la clínica.
 * <p>
 * Cualquier usuario con sesión los consulta; solo el ADMIN los modifica. No hay {@code DELETE}:
 * los consultorios se desactivan porque las citas pasadas los referencian. Los tipos de
 * consultorio están en {@code /api/consultorios/tipos}.
 */
@RestController
@RequestMapping("/api/consultorios")
@RequiredArgsConstructor
@Tag(name = "Consultorios", description = "Espacios físicos de la clínica donde se atienden las citas")
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar los consultorios", description = "Ordenados por nombre. Los filtros son opcionales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consultorios obtenidos"),
            @ApiResponse(responseCode = "400", description = "Valor de filtro inválido"),
            @ApiResponse(responseCode = "401", description = "Sin sesión")
    })
    public ResponseEntity<List<ConsultorioResponseDTO>> listar(
            @Parameter(description = "Solo los de este tipo de consultorio", example = "1")
            @RequestParam(required = false) Integer idTipo,
            @Parameter(description = "true = solo activos, false = solo desactivados", example = "true")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(consultorioService.listar(idTipo, activo));
    }

    @GetMapping("/{idConsultorio}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener un consultorio por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consultorio encontrado"),
            @ApiResponse(responseCode = "401", description = "Sin sesión"),
            @ApiResponse(responseCode = "404", description = "Consultorio no encontrado")
    })
    public ResponseEntity<ConsultorioResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador del consultorio", example = "1") @PathVariable Integer idConsultorio) {
        return ResponseEntity.ok(consultorioService.obtenerPorId(idConsultorio));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un consultorio", description = "El tipo debe existir y estar activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consultorio creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tipo desactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los consultorios"),
            @ApiResponse(responseCode = "404", description = "Tipo de consultorio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un consultorio con ese nombre")
    })
    public ResponseEntity<ConsultorioResponseDTO> crear(@Valid @RequestBody ConsultorioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultorioService.crear(dto));
    }

    @PutMapping("/{idConsultorio}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar un consultorio", description = "Edita el nombre y el tipo. Un tipo nuevo debe estar activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consultorio actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tipo desactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los consultorios"),
            @ApiResponse(responseCode = "404", description = "Consultorio o tipo no encontrado"),
            @ApiResponse(responseCode = "409", description = "Otro consultorio ya tiene ese nombre")
    })
    public ResponseEntity<ConsultorioResponseDTO> actualizar(
            @Parameter(description = "Identificador del consultorio", example = "1") @PathVariable Integer idConsultorio,
            @Valid @RequestBody ConsultorioRequestDTO dto) {
        return ResponseEntity.ok(consultorioService.actualizar(idConsultorio, dto));
    }

    @PatchMapping("/{idConsultorio}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar un consultorio", description = "Deja de ofrecerse para citas nuevas. Sus datos se conservan.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Consultorio desactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los consultorios"),
            @ApiResponse(responseCode = "404", description = "Consultorio no encontrado")
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Identificador del consultorio", example = "1") @PathVariable Integer idConsultorio) {
        consultorioService.desactivar(idConsultorio);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idConsultorio}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar un consultorio", description = "Su tipo debe estar activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consultorio reactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los consultorios"),
            @ApiResponse(responseCode = "404", description = "Consultorio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Su tipo está desactivado")
    })
    public ResponseEntity<ConsultorioResponseDTO> reactivar(
            @Parameter(description = "Identificador del consultorio", example = "1") @PathVariable Integer idConsultorio) {
        return ResponseEntity.ok(consultorioService.reactivar(idConsultorio));
    }
}
