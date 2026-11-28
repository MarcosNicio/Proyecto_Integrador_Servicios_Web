package com.veterinaria.inventario.service;

import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.inventario.dto.ProductoRequestDTO;
import com.veterinaria.inventario.dto.ProductoResponseDTO;
import com.veterinaria.inventario.dto.ProductoUpdateDTO;
import com.veterinaria.inventario.entity.InventarioProducto;
import com.veterinaria.inventario.entity.MovimientoInventario;
import com.veterinaria.inventario.entity.TipoMovimiento;
import com.veterinaria.inventario.entity.TipoProducto;
import com.veterinaria.inventario.repository.InventarioProductoRepository;
import com.veterinaria.inventario.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// Servicio de logica de negocio para el catalogo de productos (INVENTARIO_PRODUCTOS)
// y la bitacora auditable de existencias (MOVIMIENTOS_INVENTARIO).
@Service
@RequiredArgsConstructor
public class InventarioService {

    // Valor por defecto para el stock minimo cuando no se especifica en el alta
    private static final int STOCK_MINIMO_POR_DEFECTO = 5;

    // Referencia estandar registrada en el Kardex cuando un producto nace con stock inicial mayor a 0
    private static final String REFERENCIA_STOCK_INICIAL = "Stock inicial";

    // Repositorio JPA del catalogo de productos
    private final InventarioProductoRepository productoRepository;

    // Repositorio JPA de movimientos auditable (Kardex)
    private final MovimientoInventarioRepository movimientoRepository;

    // Repositorio JPA de usuarios para resolver al responsable autenticado desde el JWT
    private final UsuarioRepository usuarioRepository;


    // METODOS PUBLICOS DE NEGOCIO - CATALOGO DE PRODUCTOS Y ALERTAS

    // Registra un nuevo producto normalizando su SKU y validando unicidad.
    // Si se indica un stock inicial mayor a 0, genera automaticamente un movimiento ENTRADA en el Kardex.
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto, String correoUsuario) {
        String skuNormalizado = normalizarSku(dto.getSku());
        validarUnicidadSku(skuNormalizado, null);

        int stockInicial = dto.getStockInicial() != null ? dto.getStockInicial() : 0;
        int stockMinimo = dto.getStockMinimo() != null ? dto.getStockMinimo() : STOCK_MINIMO_POR_DEFECTO;

        Usuario usuarioResponsable = null;
        if (stockInicial > 0) {
            usuarioResponsable = buscarUsuarioPorCorreoOError(correoUsuario);
        }

        InventarioProducto nuevoProducto = InventarioProducto.builder()
                .sku(skuNormalizado)
                .nombre(dto.getNombre().trim())
                .tipo(dto.getTipo())
                .unidadMedida(dto.getUnidadMedida().trim())
                .precioUnitario(dto.getPrecioUnitario())
                .stockActual(stockInicial)
                .stockMinimo(stockMinimo)
                .activo(true)
                .build();

        InventarioProducto productoGuardado = productoRepository.save(nuevoProducto);

        if (stockInicial > 0) {
            MovimientoInventario movimientoInicial = MovimientoInventario.builder()
                    .producto(productoGuardado)
                    .usuario(usuarioResponsable)
                    .tipoMovimiento(TipoMovimiento.ENTRADA)
                    .cantidad(stockInicial)
                    .referencia(REFERENCIA_STOCK_INICIAL)
                    .build();
            movimientoRepository.save(movimientoInicial);
        }

        return mapearAProductoResponseDTO(productoGuardado);
    }

    // Lista productos del catalogo aplicando filtros opcionales de estado, tipo y texto sobre nombre o SKU
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarProductos(
            boolean incluirInactivos,
            TipoProducto tipo,
            String busqueda
    ) {
        String patron = construirPatronBusqueda(busqueda);
        return productoRepository.buscarConFiltros(incluirInactivos, tipo, patron).stream()
                .map(this::mapearAProductoResponseDTO)
                .toList();
    }

    // Obtiene el detalle completo de un producto por su identificador unico
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerProductoPorId(Integer idProducto) {
        InventarioProducto producto = buscarProductoPorIdOError(idProducto);
        return mapearAProductoResponseDTO(producto);
    }

    // Actualiza los datos de catalogo de un producto existente sin modificar su stockActual directamente
    @Transactional
    public ProductoResponseDTO actualizarProducto(Integer idProducto, ProductoUpdateDTO dto) {
        InventarioProducto producto = buscarProductoPorIdOError(idProducto);

        String skuNormalizado = normalizarSku(dto.getSku());
        validarUnicidadSku(skuNormalizado, idProducto);

        producto.setSku(skuNormalizado);
        producto.setNombre(dto.getNombre().trim());
        producto.setTipo(dto.getTipo());
        producto.setUnidadMedida(dto.getUnidadMedida().trim());
        producto.setPrecioUnitario(dto.getPrecioUnitario());
        producto.setStockMinimo(dto.getStockMinimo());

        InventarioProducto actualizado = productoRepository.save(producto);
        return mapearAProductoResponseDTO(actualizado);
    }

    // Realiza la baja logica de un producto preservando su historial en Kardex, vacunas y cobros
    @Transactional
    public void desactivarProducto(Integer idProducto) {
        InventarioProducto producto = buscarProductoPorIdOError(idProducto);
        if (Boolean.TRUE.equals(producto.getActivo())) {
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    // Reactiva un producto previamente dado de baja logica (operacion idempotente)
    @Transactional
    public ProductoResponseDTO reactivarProducto(Integer idProducto) {
        InventarioProducto producto = buscarProductoPorIdOError(idProducto);
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            producto.setActivo(true);
            producto = productoRepository.save(producto);
        }
        return mapearAProductoResponseDTO(producto);
    }

    // Lista todos los productos activos cuyo stock actual es menor o igual a su stock minimo
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarProductosConStockBajo() {
        return productoRepository.findProductosConStockBajo().stream()
                .map(this::mapearAProductoResponseDTO)
                .toList();
    }


    
    // METODOS AUXILIARES PRIVADOS

    // Busca un producto por ID o lanza ResourceNotFoundException si no existe
    private InventarioProducto buscarProductoPorIdOError(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el producto de inventario con ID: " + idProducto));
    }

    // Resuelve el usuario autenticado a partir de su correo para registrar la auditoria en el Kardex
    private Usuario buscarUsuarioPorCorreoOError(String correoUsuario) {
        return usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado: " + correoUsuario));
    }

    // Valida que no exista otro producto con el mismo SKU sin distinguir mayusculas
    private void validarUnicidadSku(String skuNormalizado, Integer idProductoExcluir) {
        boolean existe = (idProductoExcluir == null)
                ? productoRepository.existsBySkuIgnoreCase(skuNormalizado)
                : productoRepository.existsBySkuIgnoreCaseAndIdProductoNot(skuNormalizado, idProductoExcluir);

        if (existe) {
            throw new ConflictException("Ya existe un producto registrado con el SKU: " + skuNormalizado);
        }
    }

    // Elimina espacios extremos y convierte el SKU a mayusculas para almacenamiento uniforme
    private String normalizarSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    // Construye el patron LIKE en minusculas para busquedas parciales, o null si no se envio texto
    private String construirPatronBusqueda(String busqueda) {
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return null;
        }
        return "%" + busqueda.trim().toLowerCase(Locale.ROOT) + "%";
    }

    // Mapea la entidad InventarioProducto a su DTO de respuesta calculando el indicador de bajo stock
    private ProductoResponseDTO mapearAProductoResponseDTO(InventarioProducto producto) {
        boolean bajoStock = producto.getStockActual() <= producto.getStockMinimo();
        return ProductoResponseDTO.builder()
                .idProducto(producto.getIdProducto())
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .tipo(producto.getTipo())
                .unidadMedida(producto.getUnidadMedida())
                .precioUnitario(producto.getPrecioUnitario())
                .stockActual(producto.getStockActual())
                .stockMinimo(producto.getStockMinimo())
                .bajoStock(bajoStock)
                .activo(producto.getActivo())
                .build();
    }
}
