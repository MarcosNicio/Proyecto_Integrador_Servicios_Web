package com.veterinaria.personal.controller;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.personal.dto.PersonalRequestDTO;
import com.veterinaria.personal.dto.PersonalResponseDTO;
import com.veterinaria.personal.dto.PersonalUpdateDTO;
import com.veterinaria.personal.service.PersonalService;
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
 * Endpoints del personal de la clínica (veterinarios y cajeros).
 * <p>
 * La gestión es solo para ADMIN. Cada empleado puede consultar su propio perfil en
 * {@code /mi-perfil}.
 */
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
@Tag(name = "Personal", description = "Alta y gestión de veterinarios y cajeros")
public class PersonalController {

    private final PersonalService personalService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de alta a un empleado",
            description = "Crea la cuenta de acceso y el perfil de un veterinario o cajero en una sola operación. "
                    + "La cédula profesional es obligatoria para veterinarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empleado dado de alta"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, rol no permitido o veterinario sin cédula"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede dar de alta personal"),
            @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    })
    public ResponseEntity<PersonalResponseDTO> crear(@Valid @RequestBody PersonalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personalService.crear(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar personal", description = "Lista al personal ordenado por apellidos. Los filtros son opcionales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida"),
            @ApiResponse(responseCode = "400", description = "Valor de filtro inválido"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede consultar al personal")
    })
    public ResponseEntity<List<PersonalResponseDTO>> listar(
            @Parameter(description = "Solo empleados con este rol", example = "VETERINARIO")
            @RequestParam(required = false) Rol rol,
            @Parameter(description = "true = solo cuentas activas, false = solo desactivadas", example = "true")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(personalService.listar(rol, activo));
    }

    @GetMapping("/mi-perfil")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'CAJERO')")
    @Operation(summary = "Consultar mi perfil", description = "Devuelve el perfil del empleado que inició sesión, según su token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "403", description = "Solo para veterinarios y cajeros"),
            @ApiResponse(responseCode = "404", description = "La cuenta no tiene perfil de personal")
    })
    public ResponseEntity<PersonalResponseDTO> miPerfil(Authentication authentication) {
        return ResponseEntity.ok(personalService.obtenerMiPerfil(authentication.getName()));
    }

    @GetMapping("/{idPersonal}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener un empleado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado encontrado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede consultar al personal"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<PersonalResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal) {
        return ResponseEntity.ok(personalService.obtenerPorId(idPersonal));
    }

    @PutMapping("/{idPersonal}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar el perfil de un empleado",
            description = "Edita nombre, apellidos, teléfono, cédula y especies. El correo, el rol y la contraseña no cambian.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o veterinario sin cédula"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede editar al personal"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<PersonalResponseDTO> actualizar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal,
            @Valid @RequestBody PersonalUpdateDTO dto) {
        return ResponseEntity.ok(personalService.actualizar(idPersonal, dto));
    }

    @PatchMapping("/{idPersonal}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar a un empleado",
            description = "Desactiva su cuenta: no puede iniciar sesión y su token deja de servir. Sus datos se conservan.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empleado desactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede desactivar personal"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal) {
        personalService.desactivar(idPersonal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idPersonal}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar a un empleado", description = "Vuelve a activar su cuenta para que pueda iniciar sesión.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado reactivado"),
            @ApiResponse(responseCode = "403", description = "Solo el administrador puede reactivar personal"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<PersonalResponseDTO> reactivar(
            @Parameter(description = "Identificador del empleado", example = "1") @PathVariable Integer idPersonal) {
        return ResponseEntity.ok(personalService.reactivar(idPersonal));
    }
}
