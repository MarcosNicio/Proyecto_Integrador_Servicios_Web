package com.veterinaria.servicios.controller;

import com.veterinaria.servicios.dto.CategoriaServicioRequestDTO;
import com.veterinaria.servicios.dto.CategoriaServicioResponseDTO;
import com.veterinaria.servicios.service.CategoriaServicioService;
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

// Controlador REST para la administración y consulta de categorías de servicio (/api/categorias-servicio).
// Todos los endpoints requieren token JWT válido; las operaciones de escritura están restringidas al rol ADMIN.
@RestController
@RequestMapping("/api/categorias-servicio")
@RequiredArgsConstructor
@Tag(name = "Categorías de Servicio", description = "Gestión y consulta del catálogo de categorías de servicios veterinarios")
public class CategoriaServicioController {

    // Servicio de lógica de negocio para categorías de servicio
    private final CategoriaServicioService categoriaServicioService;

    // Registra una nueva categoría de servicio en el catálogo (solo rol ADMIN).
    // Recibe dto con el nombre único y descripción opcional de la categoría.
    // Retorna ResponseEntity con código HTTP 201 Created y la categoría registrada.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear categoría de servicio", description = "Registra una nueva categoría de servicio validando unicidad de nombre. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con el mismo nombre")
    })
    public ResponseEntity<CategoriaServicioResponseDTO> crearCategoria(
            @Valid @RequestBody CategoriaServicioRequestDTO dto) {
        CategoriaServicioResponseDTO creada = categoriaServicioService.crearCategoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Lista las categorías de servicio registradas, permitiendo elegir si se incluyen las inactivas.
    // Recibe incluirInactivas (bandera opcional, false por defecto para listar solo activas).
    // Retorna ResponseEntity con código HTTP 200 OK y la lista de categorías.
    @GetMapping
    @Operation(summary = "Listar categorías de servicio", description = "Obtiene el catálogo de categorías activas o todas incluyendo inactivas. Accesible para cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido")
    })
    public ResponseEntity<List<CategoriaServicioResponseDTO>> listarCategorias(
            @Parameter(description = "Si es true incluye categorías desactivadas; por defecto solo retorna activas", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return ResponseEntity.ok(categoriaServicioService.listarCategorias(incluirInactivas));
    }

    // Obtiene el detalle de una categoría de servicio específica por su identificador.
    // Recibe idCategoria con el identificador numérico de la categoría.
    // Retorna ResponseEntity con código HTTP 200 OK y los datos de la categoría.
    @GetMapping("/{idCategoria}")
    @Operation(summary = "Obtener categoría por ID", description = "Consulta el detalle de una categoría de servicio mediante su identificador único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<CategoriaServicioResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador único de la categoría", example = "1")
            @PathVariable Integer idCategoria) {
        return ResponseEntity.ok(categoriaServicioService.obtenerPorId(idCategoria));
    }

    // Actualiza el nombre y descripción de una categoría existente (solo rol ADMIN).
    // Recibe idCategoria (identificador de la categoría) y dto (nuevos datos validados).
    // Retorna ResponseEntity con código HTTP 200 OK y la categoría actualizada.
    @PutMapping("/{idCategoria}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar categoría de servicio", description = "Modifica el nombre y descripción de una categoría existente. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "El nombre indicado ya está en uso por otra categoría")
    })
    public ResponseEntity<CategoriaServicioResponseDTO> actualizarCategoria(
            @Parameter(description = "Identificador único de la categoría", example = "1")
            @PathVariable Integer idCategoria,
            @Valid @RequestBody CategoriaServicioRequestDTO dto) {
        return ResponseEntity.ok(categoriaServicioService.actualizarCategoria(idCategoria, dto));
    }

    // Realiza la baja lógica de una categoría de servicio marcándola como inactiva (solo rol ADMIN).
    // Recibe idCategoria con el identificador de la categoría a desactivar.
    // Retorna ResponseEntity vacío con código HTTP 204 No Content.
    @DeleteMapping("/{idCategoria}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar categoría de servicio", description = "Realiza la baja lógica de la categoría (activo = false) preservando el historial. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría desactivada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<Void> desactivarCategoria(
            @Parameter(description = "Identificador único de la categoría", example = "1")
            @PathVariable Integer idCategoria) {
        categoriaServicioService.desactivarCategoria(idCategoria);
        return ResponseEntity.noContent().build();
    }

    // Reactiva una categoría previamente desactivada marcándola como activa (solo rol ADMIN).
    // Recibe idCategoria con el identificador de la categoría a reactivar.
    // Retorna ResponseEntity con código HTTP 200 OK y la categoría reactivada.
    @PatchMapping("/{idCategoria}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar categoría de servicio", description = "Habilita nuevamente una categoría dada de baja lógica (activo = true). Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría reactivada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    public ResponseEntity<CategoriaServicioResponseDTO> reactivarCategoria(
            @Parameter(description = "Identificador único de la categoría", example = "1")
            @PathVariable Integer idCategoria) {
        return ResponseEntity.ok(categoriaServicioService.reactivarCategoria(idCategoria));
    }
}
