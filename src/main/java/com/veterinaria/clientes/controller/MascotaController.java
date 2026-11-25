package com.veterinaria.clientes.controller;

import com.veterinaria.clientes.dto.AntecedenteClinicoRequestDTO;
import com.veterinaria.clientes.dto.AntecedenteClinicoResponseDTO;
import com.veterinaria.clientes.dto.MascotaRequestDTO;
import com.veterinaria.clientes.dto.MascotaResponseDTO;
import com.veterinaria.clientes.entity.TipoAntecedente;
import com.veterinaria.clientes.service.MascotaService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
@Tag(name = "Mascotas", description = "Gestión de pacientes (mascotas), carnet digital y antecedentes clínicos")
public class MascotaController {

    // Servicio de lógica de negocio para mascotas y antecedentes clínicos
    private final MascotaService mascotaService;

    // ENDPOINTS DE MASCOTAS (PACIENTES)

    //Registrar una nueva mascota
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Registrar una nueva mascota", description = "Crea el perfil de una mascota vinculada a un cliente y genera su folio clínico (ej. PAC-0001)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mascota registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de la mascota inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para registrar mascotas en otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente propietario no encontrado")
    })
    public ResponseEntity<MascotaResponseDTO> crearMascota(@Valid @RequestBody MascotaRequestDTO dto) {
        // Registra la nueva mascota y devuelve código HTTP 201 Created
        MascotaResponseDTO respuesta = mascotaService.crearMascota(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    //Listar o buscar mascotas activas
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO')")
    @Operation(summary = "Directorio de mascotas activas", description = "Lista todas las mascotas activas o busca por nombre o folio del carnet digital (PAC-XXXX)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mascotas obtenida correctamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar el directorio global de mascotas")
    })
    public ResponseEntity<List<MascotaResponseDTO>> listarMascotasActivas(
            @Parameter(description = "Nombre de la mascota o folio clínico (ej. Firulais o PAC-0001)", example = "Firulais")
            @RequestParam(required = false) String busqueda) {
        // Devuelve el listado de mascotas activas filtrando por nombre o folio si se envía búsqueda
        return ResponseEntity.ok(mascotaService.listarMascotasActivas(busqueda));
    }

    //Obtener mascota por ID
    @GetMapping("/{idMascota}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Obtener mascota por ID", description = "Consulta el perfil completo de la mascota y sus antecedentes clínicos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar mascotas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<MascotaResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador numérico de la mascota", example = "1")
            @PathVariable Integer idMascota) {
        // Obtiene el expediente base de la mascota por su ID numérico
        return ResponseEntity.ok(mascotaService.obtenerPorId(idMascota));
    }

    //Obtener mascota por folio clínico
    @GetMapping("/codigo/{codigoMascota}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Obtener mascota por folio clínico (Carnet Digital)", description = "Consulta el perfil de la mascota usando el folio de su carnet digital (ej. PAC-0001)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota encontrada"),
            @ApiResponse(responseCode = "400", description = "Formato de folio inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar mascotas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<MascotaResponseDTO> obtenerPorCodigo(
            @Parameter(description = "Folio clínico de la mascota", example = "PAC-0001")
            @PathVariable String codigoMascota) {
        // Busca la mascota mediante su folio PAC-XXXX
        return ResponseEntity.ok(mascotaService.obtenerPorCodigo(codigoMascota));
    }

    //Listar mascotas activas de un cliente
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Listar mascotas activas de un cliente", description = "Obtiene todas las mascotas activas asociadas a un cliente específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mascotas del cliente obtenida"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar las mascotas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente propietario no encontrado")
    })
    public ResponseEntity<List<MascotaResponseDTO>> listarPorCliente(
            @Parameter(description = "Identificador del cliente propietario", example = "1")
            @PathVariable Integer idCliente) {
        // Lista únicamente las mascotas activas del cliente indicado
        return ResponseEntity.ok(mascotaService.listarPorCliente(idCliente));
    }

    //Actualizar perfil de una mascota
    @PutMapping("/{idMascota}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Actualizar perfil de una mascota", description = "Modifica datos biométricos, peso actual, estado de esterilización o fotografía de la mascota")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para modificar mascotas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Mascota o cliente propietario no encontrado")
    })
    public ResponseEntity<MascotaResponseDTO> actualizarMascota(
            @Parameter(description = "Identificador numérico de la mascota", example = "1")
            @PathVariable Integer idMascota,
            @Valid @RequestBody MascotaRequestDTO dto) {
        // Actualiza los datos de la mascota y retorna el perfil actualizado
        return ResponseEntity.ok(mascotaService.actualizarMascota(idMascota, dto));
    }

    //Desactivar una mascota
    @PatchMapping("/{idMascota}/desactivar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Dar de baja lógica a una mascota", description = "Marca la mascota como inactiva (activo = false) conservando su historial clínico y de citas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mascota dada de baja correctamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para dar de baja expedientes de mascotas"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<Void> desactivarMascota(
            @Parameter(description = "Identificador numérico de la mascota", example = "1")
            @PathVariable Integer idMascota) {
        // Ejecuta la baja lógica de la mascota y responde 204 No Content
        mascotaService.desactivarMascota(idMascota);
        return ResponseEntity.noContent().build();
    }

    //Reactivar una mascota
    @PatchMapping("/{idMascota}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Reactivar una mascota dada de baja", description = "Restaura el estado activo (activo = true) de una mascota previamente desactivada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mascota reactivada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para reactivar expedientes de mascotas"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<MascotaResponseDTO> reactivarMascota(
            @Parameter(description = "Identificador numérico de la mascota", example = "1")
            @PathVariable Integer idMascota) {
        // Reactiva la mascota y devuelve su perfil actualizado
        return ResponseEntity.ok(mascotaService.reactivarMascota(idMascota));
    }

    // ENDPOINTS DE ANTECEDENTES CLÍNICOS

    //Registrar antecedente clínico
    @PostMapping("/{idMascota}/antecedentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Registrar antecedente clínico", description = "Agrega una alergia, enfermedad crónica o cirugía previa al expediente de la mascota")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Antecedente clínico registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos del antecedente inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN o VETERINARIO pueden registrar antecedentes clínicos"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<AntecedenteClinicoResponseDTO> agregarAntecedente(
            @Parameter(description = "Identificador de la mascota", example = "1")
            @PathVariable Integer idMascota,
            @Valid @RequestBody AntecedenteClinicoRequestDTO dto) {
        // Registra el antecedente clínico y devuelve código HTTP 201 Created
        AntecedenteClinicoResponseDTO respuesta = mascotaService.agregarAntecedente(idMascota, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    //Listar antecedentes clínicos de una mascota
    @GetMapping("/{idMascota}/antecedentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'VETERINARIO', 'CLIENTE')")
    @Operation(summary = "Listar antecedentes clínicos de una mascota", description = "Consulta el historial de antecedentes ordenado por fecha o filtrado por tipo (ej. ALERGIA)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de antecedentes obtenida correctamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar antecedentes de mascotas de otro cliente"),
            @ApiResponse(responseCode = "404", description = "Mascota no encontrada")
    })
    public ResponseEntity<List<AntecedenteClinicoResponseDTO>> listarAntecedentes(
            @Parameter(description = "Identificador de la mascota", example = "1")
            @PathVariable Integer idMascota,
            @Parameter(description = "Filtro opcional por tipo de antecedente (ALERGIA, ENFERMEDAD_CRONICA, CIRUGIA_PREVIA)", example = "ALERGIA")
            @RequestParam(required = false) TipoAntecedente tipo) {
        // Lista los antecedentes de la mascota con filtro opcional por categoría
        return ResponseEntity.ok(mascotaService.listarAntecedentes(idMascota, tipo));
    }

    //Actualizar antecedente clínico
    @PutMapping("/{idMascota}/antecedentes/{idAntecedente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Actualizar antecedente clínico", description = "Modifica el tipo, descripción o fecha de registro de un antecedente clínico de la mascota")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Antecedente actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN o VETERINARIO pueden modificar antecedentes clínicos"),
            @ApiResponse(responseCode = "404", description = "Antecedente o mascota no encontrada")
    })
    public ResponseEntity<AntecedenteClinicoResponseDTO> actualizarAntecedente(
            @Parameter(description = "Identificador de la mascota", example = "1")
            @PathVariable Integer idMascota,
            @Parameter(description = "Identificador del antecedente clínico", example = "1")
            @PathVariable Integer idAntecedente,
            @Valid @RequestBody AntecedenteClinicoRequestDTO dto) {
        // Actualiza el antecedente clínico validando que pertenezca a la mascota
        return ResponseEntity.ok(mascotaService.actualizarAntecedente(idMascota, idAntecedente, dto));
    }

    //Eliminar antecedente clínico
    @DeleteMapping("/{idMascota}/antecedentes/{idAntecedente}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
    @Operation(summary = "Eliminar antecedente clínico", description = "Elimina un antecedente clínico registrado en la mascota")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Antecedente eliminado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN o VETERINARIO pueden eliminar antecedentes clínicos"),
            @ApiResponse(responseCode = "404", description = "Antecedente o mascota no encontrada")
    })
    public ResponseEntity<Void> eliminarAntecedente(
            @Parameter(description = "Identificador de la mascota", example = "1")
            @PathVariable Integer idMascota,
            @Parameter(description = "Identificador del antecedente clínico", example = "1")
            @PathVariable Integer idAntecedente) {
        // Elimina el antecedente clínico y responde 204 No Content
        mascotaService.eliminarAntecedente(idMascota, idAntecedente);
        return ResponseEntity.noContent().build();
    }
}
