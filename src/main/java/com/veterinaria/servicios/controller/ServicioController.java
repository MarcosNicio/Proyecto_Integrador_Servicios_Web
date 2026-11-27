package com.veterinaria.servicios.controller;

import com.veterinaria.servicios.dto.CalculoPrecioResponseDTO;
import com.veterinaria.servicios.dto.ServicioRequestDTO;
import com.veterinaria.servicios.dto.ServicioResponseDTO;
import com.veterinaria.servicios.dto.TarifaServicioRequestDTO;
import com.veterinaria.servicios.dto.TarifaServicioResponseDTO;
import com.veterinaria.servicios.service.ServicioService;
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

import java.math.BigDecimal;
import java.util.List;

// Controlador REST para el catálogo de servicios veterinarios, tarifas dinámicas y cálculo de precios (/api/servicios).
// Todos los endpoints requieren token JWT válido; las operaciones de escritura están restringidas al rol ADMIN.
@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios y Tarifas", description = "Catálogo de servicios veterinarios, reglas tarifarias dinámicas y cotización por especie y peso")
public class ServicioController {

    // Servicio de lógica de negocio para catálogo de servicios, tarifas dinámicas y cotización
    private final ServicioService servicioService;

    // ENDPOINTS DEL CATÁLOGO DE SERVICIOS

    // Registra un nuevo servicio en el catálogo veterinario (solo rol ADMIN).
    // Recibe dto con la categoría, tipo de consultorio opcional, nombre, descripción, duración y precio base.
    // Retorna ResponseEntity con código HTTP 201 Created y el servicio registrado.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear servicio", description = "Registra un nuevo servicio validando categoría activa, tipo de consultorio opcional y unicidad de nombre en la categoría. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría inactiva"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Categoría o tipo de consultorio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un servicio con el mismo nombre en la categoría")
    })
    public ResponseEntity<ServicioResponseDTO> crearServicio(@Valid @RequestBody ServicioRequestDTO dto) {
        ServicioResponseDTO creado = servicioService.crearServicio(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Lista los servicios del catálogo con filtros opcionales por estado activo, categoría y coincidencia parcial en el nombre.
    // Recibe incluirInactivos (false por defecto), idCategoria (opcional) y nombre (opcional).
    // Retorna ResponseEntity con código HTTP 200 OK y la lista de servicios coincidentes.
    @GetMapping
    @Operation(summary = "Listar o filtrar servicios", description = "Lista servicios activos o todos, con soporte opcional para filtrar por categoría y buscar por nombre. Accesible para cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "La categoría especificada en el filtro no existe")
    })
    public ResponseEntity<List<ServicioResponseDTO>> listarServicios(
            @Parameter(description = "Si es true incluye servicios desactivados; por defecto solo activos", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @Parameter(description = "Identificador opcional de categoría para filtrar servicios", example = "1")
            @RequestParam(required = false) Integer idCategoria,
            @Parameter(description = "Texto opcional para buscar servicios por coincidencia parcial en el nombre", example = "Baño")
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(servicioService.listarServicios(incluirInactivos, idCategoria, nombre));
    }

    // Obtiene el detalle completo de un servicio y sus tarifas dinámicas asociadas por ID.
    // Recibe idServicio con el identificador numérico del servicio.
    // Retorna ResponseEntity con código HTTP 200 OK y el detalle del servicio.
    @GetMapping("/{idServicio}")
    @Operation(summary = "Obtener servicio por ID", description = "Consulta el detalle completo de un servicio junto con sus reglas tarifarias configuradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<ServicioResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio) {
        return ResponseEntity.ok(servicioService.obtenerPorId(idServicio));
    }

    // Actualiza la información de un servicio existente en el catálogo (solo rol ADMIN).
    // Recibe idServicio (identificador del servicio) y dto (nuevos datos validados).
    // Retorna ResponseEntity con código HTTP 200 OK y el servicio actualizado.
    @PutMapping("/{idServicio}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar servicio", description = "Modifica los datos de un servicio existente validando categoría, consultorio y unicidad. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría inactiva"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio, categoría o tipo de consultorio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro servicio con ese nombre en la categoría")
    })
    public ResponseEntity<ServicioResponseDTO> actualizarServicio(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio,
            @Valid @RequestBody ServicioRequestDTO dto) {
        return ResponseEntity.ok(servicioService.actualizarServicio(idServicio, dto));
    }

    // Realiza la baja lógica de un servicio marcándolo como inactivo (solo rol ADMIN).
    // Recibe idServicio con el identificador del servicio a desactivar.
    // Retorna ResponseEntity vacío con código HTTP 204 No Content.
    @DeleteMapping("/{idServicio}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar servicio", description = "Realiza la baja lógica del servicio (activo = false) preservando historial de citas y cobros. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Servicio desactivado correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<Void> desactivarServicio(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio) {
        servicioService.desactivarServicio(idServicio);
        return ResponseEntity.noContent().build();
    }

    // Reactiva un servicio previamente dado de baja lógica verificando que su categoría esté activa (solo rol ADMIN).
    // Recibe idServicio con el identificador del servicio a reactivar.
    // Retorna ResponseEntity con código HTTP 200 OK y el servicio reactivado.
    @PatchMapping("/{idServicio}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar servicio", description = "Habilita nuevamente un servicio desactivado (activo = true) si su categoría permanece activa. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio reactivado correctamente"),
            @ApiResponse(responseCode = "400", description = "La categoría asociada al servicio está inactiva"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<ServicioResponseDTO> reactivarServicio(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio) {
        return ResponseEntity.ok(servicioService.reactivarServicio(idServicio));
    }

    // ENDPOINTS DE TARIFAS DINÁMICAS (SUB-RECURSO /api/servicios/{idServicio}/tarifas)

    // Registra una nueva regla tarifaria por especie y rango de peso para un servicio (solo rol ADMIN).
    // Recibe idServicio (identificador del servicio padre) y dto (especie, rango de peso y precio calculado).
    // Retorna ResponseEntity con código HTTP 201 Created y la tarifa registrada.
    @PostMapping("/{idServicio}/tarifas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear tarifa dinámica para un servicio", description = "Agrega una regla tarifaria por especie y rango de peso validando consistencia y ausencia de traslapes. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarifa creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos o rango de pesos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
            @ApiResponse(responseCode = "409", description = "El rango de peso se traslapa con una tarifa existente para esa especie")
    })
    public ResponseEntity<TarifaServicioResponseDTO> crearTarifa(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio,
            @Valid @RequestBody TarifaServicioRequestDTO dto) {
        TarifaServicioResponseDTO creada = servicioService.crearTarifa(idServicio, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Lista las tarifas dinámicas configuradas para un servicio, con filtro opcional por especie.
    // Recibe idServicio (identificador del servicio) y especie (opcional).
    // Retorna ResponseEntity con código HTTP 200 OK y la lista ordenada de tarifas.
    @GetMapping("/{idServicio}/tarifas")
    @Operation(summary = "Listar tarifas de un servicio", description = "Obtiene todas las reglas tarifarias de un servicio ordenadas por especie y peso mínimo, con filtro opcional por especie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarifas obtenidas correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<List<TarifaServicioResponseDTO>> listarTarifas(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio,
            @Parameter(description = "Especie opcional para filtrar tarifas (insensible a mayúsculas)", example = "PERRO")
            @RequestParam(required = false) String especie) {
        return ResponseEntity.ok(servicioService.listarTarifasPorServicio(idServicio, especie));
    }

    // Actualiza una regla tarifaria existente perteneciente a un servicio (solo rol ADMIN).
    // Recibe idServicio (identificador del servicio), idTarifa (identificador de la tarifa) y dto (nuevos datos).
    // Retorna ResponseEntity con código HTTP 200 OK y la tarifa actualizada.
    @PutMapping("/{idServicio}/tarifas/{idTarifa}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar tarifa dinámica", description = "Modifica una regla tarifaria verificando pertenencia al servicio, consistencia de pesos y ausencia de traslapes. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarifa actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos o rango de pesos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio o tarifa no encontrados o no vinculados"),
            @ApiResponse(responseCode = "409", description = "El nuevo rango se traslapa con otra tarifa existente")
    })
    public ResponseEntity<TarifaServicioResponseDTO> actualizarTarifa(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio,
            @Parameter(description = "Identificador único de la tarifa", example = "10")
            @PathVariable Integer idTarifa,
            @Valid @RequestBody TarifaServicioRequestDTO dto) {
        return ResponseEntity.ok(servicioService.actualizarTarifa(idServicio, idTarifa, dto));
    }

    // Elimina una regla tarifaria asociada a un servicio (solo rol ADMIN).
    // Recibe idServicio (identificador del servicio padre) e idTarifa (identificador de la tarifa a eliminar).
    // Retorna ResponseEntity vacío con código HTTP 204 No Content.
    @DeleteMapping("/{idServicio}/tarifas/{idTarifa}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar tarifa dinámica", description = "Elimina permanentemente una regla tarifaria de un servicio. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tarifa eliminada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Servicio o tarifa no encontrados o no vinculados")
    })
    public ResponseEntity<Void> eliminarTarifa(
            @Parameter(description = "Identificador único del servicio", example = "1")
            @PathVariable Integer idServicio,
            @Parameter(description = "Identificador único de la tarifa", example = "10")
            @PathVariable Integer idTarifa) {
        servicioService.eliminarTarifa(idServicio, idTarifa);
        return ResponseEntity.noContent().build();
    }

    // ENDPOINT DE COTIZACIÓN DINÁMICA

    // Calcula el precio final de un servicio activo para una especie y peso determinados con fallback al precio base.
    // Recibe idServicio (identificador del servicio), especie (especie de la mascota) y pesoKg (peso en kilogramos).
    // Retorna ResponseEntity con código HTTP 200 OK y el desglose del cálculo en CalculoPrecioResponseDTO.
    @GetMapping("/{idServicio}/calcular-precio")
    @Operation(summary = "Calcular precio dinámico de un servicio", description = "Cotiza el precio final según especie y peso en kg; si no existe una tarifa aplicable, retorna el precio base con tarifaAplicada = false. Accesible para cualquier usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Precio calculado correctamente"),
            @ApiResponse(responseCode = "400", description = "Servicio inactivo, especie vacía o peso inválido (<= 0)"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<CalculoPrecioResponseDTO> calcularPrecio(
            @Parameter(description = "Identificador único del servicio a cotizar", example = "1")
            @PathVariable Integer idServicio,
            @Parameter(description = "Especie del paciente (insensible a mayúsculas)", example = "Perro", required = true)
            @RequestParam String especie,
            @Parameter(description = "Peso del paciente en kilogramos (mayor a 0)", example = "12.50", required = true)
            @RequestParam BigDecimal pesoKg) {
        return ResponseEntity.ok(servicioService.calcularPrecio(idServicio, especie, pesoKg));
    }
}
