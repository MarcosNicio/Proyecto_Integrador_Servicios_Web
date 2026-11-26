package com.veterinaria.agenda.controller;

import com.veterinaria.agenda.dto.HorarioClinicaRequestDTO;
import com.veterinaria.agenda.dto.HorarioClinicaResponseDTO;
import com.veterinaria.agenda.service.HorarioClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints del horario de atención de la clínica.
 * <p>
 * Cualquier usuario con sesión lo consulta (el cliente también necesita saber cuándo abre la
 * clínica); solo el ADMIN lo modifica.
 */
@RestController
@RequestMapping("/api/config/horarios-clinica")
@RequiredArgsConstructor
@Tag(name = "Configuración de la clínica", description = "Horario de atención, tipos de consultorio y consultorios")
public class HorarioClinicaController {

    private final HorarioClinicaService horarioService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar el horario de la clínica",
            description = "La semana completa, de domingo (0) a sábado (6).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario obtenido"),
            @ApiResponse(responseCode = "401", description = "Sin sesión")
    })
    public ResponseEntity<List<HorarioClinicaResponseDTO>> obtener() {
        return ResponseEntity.ok(horarioService.obtenerSemana());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modificar el horario de la clínica",
            description = "Reemplaza el horario de los 7 días en una sola petición (cada día una vez). "
                    + "Es todo o nada: si algún turno del personal quedara fuera del nuevo horario, no se guarda "
                    + "nada y la respuesta 409 lista los turnos afectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario guardado; devuelve la semana completa"),
            @ApiResponse(responseCode = "400", description = "Faltan días o datos, un día se repite, o un cierre no es posterior a su apertura"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador modifica el horario"),
            @ApiResponse(responseCode = "409", description = "Algún turno del personal quedaría fuera del nuevo horario")
    })
    public ResponseEntity<List<HorarioClinicaResponseDTO>> actualizar(
            @RequestBody List<HorarioClinicaRequestDTO> semana) {
        return ResponseEntity.ok(horarioService.actualizarSemana(semana));
    }
}
