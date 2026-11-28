package com.veterinaria.inventario.controller;

import com.veterinaria.inventario.dto.ProductoRequestDTO;
import com.veterinaria.inventario.dto.ProductoResponseDTO;
import com.veterinaria.inventario.dto.ProductoUpdateDTO;
import com.veterinaria.inventario.entity.TipoProducto;
import com.veterinaria.inventario.service.InventarioService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST para el modulo de gestion de inventario (/api/inventario).
// Expone la administracion del catalogo de productos, alertas de stock minimo y el Kardex de movimientos.
// Todo el modulo esta restringido al personal interno (ADMIN, VETERINARIO y CAJERO); el rol CLIENTE recibe 403.
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventario", description = "Catalogo de medicamentos, insumos clinicos y productos de tienda, alertas de stock minimo y Kardex de movimientos")
public class InventarioController {

    // Servicio de logica de negocio para catalogo de productos y movimientos de inventario
    private final InventarioService inventarioService;

    // =========================================================================
    // ENDPOINTS DEL CATALOGO DE PRODUCTOS Y ALERTAS
    // =========================================================================

    // Da de alta un nuevo producto en el catalogo de inventario (solo rol ADMIN).
    // Si se indica un stockInicial mayor a 0, registra automaticamente una ENTRADA en el Kardex con el usuario del token.
    @PostMapping("/productos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto de inventario", description = "Registra un nuevo producto normalizando el SKU y validando unicidad. Si stockInicial > 0 genera automaticamente un movimiento ENTRADA. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos en el cuerpo de la peticion"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "Ya existe un producto registrado con ese SKU")
    })
    public ResponseEntity<ProductoResponseDTO> crearProducto(
            @Valid @RequestBody ProductoRequestDTO dto,
            Authentication authentication
    ) {
        ProductoResponseDTO creado = inventarioService.crearProducto(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Lista los productos del inventario con filtros opcionales por estado, tipo y busqueda parcial en nombre o SKU.
    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'CAJERO')")
    @Operation(summary = "Listar o filtrar productos", description = "Lista productos activos o todos, con filtros opcionales por tipo y coincidencia parcial en nombre o SKU. Accesible para ADMIN, VETERINARIO y CAJERO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida correctamente"),
            @ApiResponse(responseCode = "400", description = "Valor de filtro invalido"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: restringido a personal de la clinica")
    })
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @Parameter(description = "Si es true incluye productos desactivados; por defecto solo activos", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @Parameter(description = "Filtro opcional por tipo de producto (MEDICAMENTO, INSUMO_CLINICO, TIENDA)", example = "MEDICAMENTO")
            @RequestParam(required = false) TipoProducto tipo,
            @Parameter(description = "Texto opcional para buscar coincidencias parciales en nombre o SKU", example = "vacuna")
            @RequestParam(required = false) String busqueda
    ) {
        return ResponseEntity.ok(inventarioService.listarProductos(incluirInactivos, tipo, busqueda));
    }

    // Lista los productos activos cuyo stock actual es menor o igual a su stock minimo configurado.
    @GetMapping("/productos/stock-bajo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'CAJERO')")
    @Operation(summary = "Listar alertas de stock bajo", description = "Obtiene todos los productos activos cuyo stockActual es menor o igual a su stockMinimo, ordenados de menor a mayor existencia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de productos con stock bajo obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: restringido a personal de la clinica")
    })
    public ResponseEntity<List<ProductoResponseDTO>> listarProductosConStockBajo() {
        return ResponseEntity.ok(inventarioService.listarProductosConStockBajo());
    }

    // Obtiene el detalle completo de un producto del inventario por su ID.
    @GetMapping("/productos/{idProducto}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'CAJERO')")
    @Operation(summary = "Obtener producto por ID", description = "Consulta el detalle completo de un producto de inventario incluyendo existencias e indicador de stock bajo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: restringido a personal de la clinica"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductoResponseDTO> obtenerProductoPorId(
            @Parameter(description = "Identificador unico del producto", example = "1")
            @PathVariable Integer idProducto
    ) {
        return ResponseEntity.ok(inventarioService.obtenerProductoPorId(idProducto));
    }

    // Actualiza los datos de catalogo de un producto sin modificar su stockActual directamente (solo rol ADMIN).
    @PutMapping("/productos/{idProducto}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Modifica SKU, nombre, tipo, unidad de medida, precio unitario y stock minimo sin alterar stockActual directamente. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos en el cuerpo de la peticion"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro producto con ese SKU")
    })
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @Parameter(description = "Identificador unico del producto", example = "1")
            @PathVariable Integer idProducto,
            @Valid @RequestBody ProductoUpdateDTO dto
    ) {
        return ResponseEntity.ok(inventarioService.actualizarProducto(idProducto, dto));
    }

    // Realiza la baja logica de un producto preservando su historial en Kardex, vacunas y ordenes (solo rol ADMIN).
    @DeleteMapping("/productos/{idProducto}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactivar producto", description = "Realiza la baja logica del producto (activo = false) preservando la integridad historica. Operacion idempotente. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto desactivado correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<Void> desactivarProducto(
            @Parameter(description = "Identificador unico del producto", example = "1")
            @PathVariable Integer idProducto
    ) {
        inventarioService.desactivarProducto(idProducto);
        return ResponseEntity.noContent().build();
    }

    // Reactiva un producto previamente dado de baja logica (solo rol ADMIN).
    @PatchMapping("/productos/{idProducto}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivar producto", description = "Habilita nuevamente un producto desactivado (activo = true). Operacion idempotente. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto reactivado correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    public ResponseEntity<ProductoResponseDTO> reactivarProducto(
            @Parameter(description = "Identificador unico del producto", example = "1")
            @PathVariable Integer idProducto
    ) {
        return ResponseEntity.ok(inventarioService.reactivarProducto(idProducto));
    }
}
