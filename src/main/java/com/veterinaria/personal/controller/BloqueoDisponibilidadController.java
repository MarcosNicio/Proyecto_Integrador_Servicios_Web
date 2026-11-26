package com.veterinaria.personal.controller;

import com.veterinaria.personal.dto.BloqueoRequestDTO;
import com.veterinaria.personal.dto.BloqueoResponseDTO;
import com.veterinaria.personal.service.BloqueoDisponibilidadService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de los bloqueos de agenda del personal (vacaciones, cirugías, días libres).
 * <p>
 * La gestión es solo para ADMIN. Cada empleado puede consultar sus propios bloqueos en
 * {@code /mi-perfil/bloqueos}.
 */
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
@Tag(name = "Bloqueos de agenda", description = "Periodos en que un empleado no está disponible")
public class BloqueoDisponibilidadController {

    private final BloqueoDisponibilidadService bloqueoService;

    @PostMapping("/{idPersonal}/bloqueos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un bloqueo",
            description = "El fin debe ser posterior al inicio y futuro. No puede encimarse con otro bloqueo del "
                    + "empleado; bloqueos seguidos sí se permiten.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bloqueo registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, fin antes del inicio o bloqueo ya terminado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los bloqueos"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
            @ApiResponse(responseCode = "409", description = "El bloqueo se encima con otro")
    })
    public ResponseEntity<BloqueoResponseDTO> registrar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Valid @RequestBody BloqueoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueoService.registrar(idPersonal, dto));
    }

    @GetMapping("/{idPersonal}/bloqueos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar los bloqueos de un empleado",
            description = "Por defecto solo los vigentes y futuros, ordenados por fecha de inicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bloqueos obtenidos"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador consulta los bloqueos de otros"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<List<BloqueoResponseDTO>> listar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Parameter(description = "true = incluye también los bloqueos que ya terminaron", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirPasados) {
        return ResponseEntity.ok(bloqueoService.listar(idPersonal, incluirPasados));
    }

    @GetMapping("/mi-perfil/bloqueos")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'CAJERO')")
    @Operation(summary = "Consultar mis bloqueos", description = "Bloqueos del empleado que inició sesión, según su token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bloqueos obtenidos"),
            @ApiResponse(responseCode = "403", description = "Solo para veterinarios y cajeros"),
            @ApiResponse(responseCode = "404", description = "La cuenta no tiene perfil de personal")
    })
    public ResponseEntity<List<BloqueoResponseDTO>> misBloqueos(
            Authentication authentication,
            @Parameter(description = "true = incluye también los bloqueos que ya terminaron", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirPasados) {
        return ResponseEntity.ok(bloqueoService.listarMisBloqueos(authentication.getName(), incluirPasados));
    }

    @PutMapping("/{idPersonal}/bloqueos/{idBloqueo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar un bloqueo", description = "Solo se pueden editar bloqueos que aún no terminan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bloqueo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o el bloqueo ya terminó"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los bloqueos"),
            @ApiResponse(responseCode = "404", description = "Empleado o bloqueo no encontrado (o el bloqueo es de otro empleado)"),
            @ApiResponse(responseCode = "409", description = "El bloqueo se encima con otro")
    })
    public ResponseEntity<BloqueoResponseDTO> actualizar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Parameter(description = "Identificador del bloqueo", example = "3") @PathVariable Integer idBloqueo,
            @Valid @RequestBody BloqueoRequestDTO dto) {
        return ResponseEntity.ok(bloqueoService.actualizar(idPersonal, idBloqueo, dto));
    }

    @DeleteMapping("/{idPersonal}/bloqueos/{idBloqueo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancelar un bloqueo", description = "Solo se pueden borrar bloqueos que aún no terminan.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bloqueo cancelado"),
            @ApiResponse(responseCode = "400", description = "El bloqueo ya terminó (queda como historial)"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los bloqueos"),
            @ApiResponse(responseCode = "404", description = "Empleado o bloqueo no encontrado (o el bloqueo es de otro empleado)")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Parameter(description = "Identificador del bloqueo", example = "3") @PathVariable Integer idBloqueo) {
        bloqueoService.eliminar(idPersonal, idBloqueo);
        return ResponseEntity.noContent().build();
    }
}
