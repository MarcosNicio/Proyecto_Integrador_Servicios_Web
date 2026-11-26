package com.veterinaria.personal.controller;

import com.veterinaria.personal.dto.HorarioPersonalRequestDTO;
import com.veterinaria.personal.dto.HorarioPersonalResponseDTO;
import com.veterinaria.personal.service.HorarioPersonalService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de los turnos de trabajo semanales del personal.
 * <p>
 * La gestión es solo para ADMIN. Cada empleado puede consultar sus propios turnos en
 * {@code /mi-perfil/horarios}.
 */
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
@Tag(name = "Horarios del personal", description = "Turnos de trabajo semanales de veterinarios y cajeros")
public class HorarioPersonalController {

    private final HorarioPersonalService horarioService;

    @PostMapping("/{idPersonal}/horarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agregar un turno",
            description = "Agrega un turno semanal al empleado. No puede encimarse con otro de sus turnos ese día; "
                    + "dos turnos seguidos (08:00-13:00 y 13:00-17:00) sí se permiten.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Turno agregado"),
            @ApiResponse(responseCode = "400", description = "Día fuera de rango o la hora de fin no es posterior a la de inicio"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los turnos"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado"),
            @ApiResponse(responseCode = "409", description = "El turno se encima con otro del mismo día")
    })
    public ResponseEntity<HorarioPersonalResponseDTO> agregar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Valid @RequestBody HorarioPersonalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioService.agregar(idPersonal, dto));
    }

    @GetMapping("/{idPersonal}/horarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar los turnos de un empleado", description = "De domingo a sábado, ordenados por hora de inicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turnos obtenidos"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador consulta los turnos de otros"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<List<HorarioPersonalResponseDTO>> listar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal) {
        return ResponseEntity.ok(horarioService.listar(idPersonal));
    }

    @GetMapping("/mi-perfil/horarios")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'CAJERO')")
    @Operation(summary = "Consultar mis turnos", description = "Turnos del empleado que inició sesión, según su token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turnos obtenidos"),
            @ApiResponse(responseCode = "403", description = "Solo para veterinarios y cajeros"),
            @ApiResponse(responseCode = "404", description = "La cuenta no tiene perfil de personal")
    })
    public ResponseEntity<List<HorarioPersonalResponseDTO>> misHorarios(Authentication authentication) {
        return ResponseEntity.ok(horarioService.listarMisHorarios(authentication.getName()));
    }

    @PutMapping("/{idPersonal}/horarios/{idHorario}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar un turno")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turno actualizado"),
            @ApiResponse(responseCode = "400", description = "Día fuera de rango o la hora de fin no es posterior a la de inicio"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los turnos"),
            @ApiResponse(responseCode = "404", description = "Empleado o turno no encontrado (o el turno es de otro empleado)"),
            @ApiResponse(responseCode = "409", description = "El turno se encima con otro del mismo día")
    })
    public ResponseEntity<HorarioPersonalResponseDTO> actualizar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Parameter(description = "Identificador del turno", example = "5") @PathVariable Integer idHorario,
            @Valid @RequestBody HorarioPersonalRequestDTO dto) {
        return ResponseEntity.ok(horarioService.actualizar(idPersonal, idHorario, dto));
    }

    @DeleteMapping("/{idPersonal}/horarios/{idHorario}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Borrar un turno")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Turno borrado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador gestiona los turnos"),
            @ApiResponse(responseCode = "404", description = "Empleado o turno no encontrado (o el turno es de otro empleado)")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Parameter(description = "Identificador del turno", example = "5") @PathVariable Integer idHorario) {
        horarioService.eliminar(idPersonal, idHorario);
        return ResponseEntity.noContent().build();
    }
}
