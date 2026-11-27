package com.veterinaria.servicios.service;

import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.servicios.dto.CategoriaServicioRequestDTO;
import com.veterinaria.servicios.dto.CategoriaServicioResponseDTO;
import com.veterinaria.servicios.entity.CategoriaServicio;
import com.veterinaria.servicios.repository.CategoriaServicioRepository;
import com.veterinaria.servicios.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio de lógica de negocio para la gestión de categorías de servicio (CATEGORIAS_SERVICIO).
// Controla la unicidad de nombres, normalización de datos y el ciclo de vida mediante baja lógica.
@Service
@RequiredArgsConstructor
public class CategoriaServicioService {

    // Repositorio JPA para operaciones CRUD y consultas sobre categorías de servicio
    private final CategoriaServicioRepository categoriaServicioRepository;

    // Repositorio de servicios, para saber si una categoría todavía tiene servicios activos
    private final ServicioRepository servicioRepository;

    // MÉTODOS PÚBLICOS DE NEGOCIO - CATEGORÍAS DE SERVICIO

    // Registra una nueva categoría de servicio validando que su nombre no esté duplicado.
    // Recibe dto con los datos de la categoría a registrar.
    // Retorna el DTO con la información de la categoría creada.
    // Lanza ConflictException si ya existe una categoría con el mismo nombre (sin distinguir mayúsculas/minúsculas).
    @Transactional
    public CategoriaServicioResponseDTO crearCategoria(CategoriaServicioRequestDTO dto) {
        String nombreNormalizado = dto.getNombre().trim();
        validarUnicidadNombre(nombreNormalizado, null);

        CategoriaServicio nuevaCategoria = CategoriaServicio.builder()
                .nombre(nombreNormalizado)
                .descripcion(normalizarTextoOpcional(dto.getDescripcion()))
                .activo(true)
                .build();

        CategoriaServicio categoriaGuardada = categoriaServicioRepository.save(nuevaCategoria);
        return mapearACategoriaResponseDTO(categoriaGuardada);
    }

    // Obtiene el detalle de una categoría de servicio por su identificador único.
    // Recibe idCategoria con el identificador de la categoría a consultar.
    // Retorna el DTO con la información de la categoría encontrada.
    // Lanza ResourceNotFoundException si no existe una categoría con ese ID.
    @Transactional(readOnly = true)
    public CategoriaServicioResponseDTO obtenerPorId(Integer idCategoria) {
        CategoriaServicio categoria = buscarCategoriaPorIdOError(idCategoria);
        return mapearACategoriaResponseDTO(categoria);
    }

    // Lista las categorías de servicio registradas.
    // Recibe incluirInactivas (true para retornar todas las categorías; false para solo las activas).
    // Retorna la lista de categorías mapeadas a DTO de respuesta.
    @Transactional(readOnly = true)
    public List<CategoriaServicioResponseDTO> listarCategorias(boolean incluirInactivas) {
        List<CategoriaServicio> categorias = incluirInactivas
                ? categoriaServicioRepository.findAll()
                : categoriaServicioRepository.findByActivoTrue();

        return categorias.stream()
                .map(this::mapearACategoriaResponseDTO)
                .toList();
    }

    // Actualiza el nombre y descripción de una categoría existente validando que el nuevo nombre no colisione con otra categoría distinta.
    // Recibe idCategoria (identificador de la categoría a actualizar) y dto (nuevos datos de la categoría).
    // Retorna el DTO con la categoría actualizada.
    // Lanza ResourceNotFoundException si la categoría no existe y ConflictException si el nuevo nombre ya está en uso por otra categoría.
    @Transactional
    public CategoriaServicioResponseDTO actualizarCategoria(Integer idCategoria, CategoriaServicioRequestDTO dto) {
        CategoriaServicio categoriaExistente = buscarCategoriaPorIdOError(idCategoria);

        String nombreNormalizado = dto.getNombre().trim();
        validarUnicidadNombre(nombreNormalizado, idCategoria);

        categoriaExistente.setNombre(nombreNormalizado);
        categoriaExistente.setDescripcion(normalizarTextoOpcional(dto.getDescripcion()));

        CategoriaServicio categoriaActualizada = categoriaServicioRepository.save(categoriaExistente);
        return mapearACategoriaResponseDTO(categoriaActualizada);
    }

    // Realiza la baja lógica de una categoría marcando su estado como inactivo (activo = false).
    // Recibe idCategoria con el identificador de la categoría a desactivar. No retorna valor.
    // Si la categoría ya estaba desactivada no hace nada.
    // Lanza ResourceNotFoundException si la categoría no existe y ConflictException si todavía tiene servicios activos
    // (así nunca queda un servicio activo dentro de una categoría desactivada; primero se desactivan sus servicios).
    @Transactional
    public void desactivarCategoria(Integer idCategoria) {
        CategoriaServicio categoria = buscarCategoriaPorIdOError(idCategoria);
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            return;
        }

        long serviciosActivos = servicioRepository.countByCategoriaIdCategoriaAndActivoTrue(idCategoria);
        if (serviciosActivos > 0) {
            throw new ConflictException("No se puede desactivar la categoría '" + categoria.getNombre()
                    + "': tiene " + serviciosActivos + " servicio(s) activo(s). Desactiva primero sus servicios");
        }

        categoria.setActivo(false);
        categoriaServicioRepository.save(categoria);
    }

    // Reactiva una categoría previamente desactivada (activo = true).
    // Recibe idCategoria con el identificador de la categoría a reactivar.
    // Retorna el DTO con la categoría reactivada.
    // Lanza ResourceNotFoundException si la categoría no existe.
    @Transactional
    public CategoriaServicioResponseDTO reactivarCategoria(Integer idCategoria) {
        CategoriaServicio categoria = buscarCategoriaPorIdOError(idCategoria);
        categoria.setActivo(true);
        CategoriaServicio categoriaReactivada = categoriaServicioRepository.save(categoria);
        return mapearACategoriaResponseDTO(categoriaReactivada);
    }

    // MÉTODOS AUXILIARES PRIVADOS

    // Busca una categoría por su ID o lanza error si no existe.
    // Recibe idCategoria con el identificador de la categoría.
    // Retorna la entidad CategoriaServicio encontrada.
    // Lanza ResourceNotFoundException si no se encuentra la categoría.
    private CategoriaServicio buscarCategoriaPorIdOError(Integer idCategoria) {
        return categoriaServicioRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la categoría de servicio con ID: " + idCategoria));
    }

    // Valida que el nombre de categoría no esté registrado en la base de datos; en actualizaciones excluye el ID de la propia categoría editada.
    // Recibe nombre (nombre normalizado a validar) e idCategoriaExcluir (identificador a excluir, o null al crear). No retorna valor.
    // Lanza ConflictException si existe un duplicado.
    private void validarUnicidadNombre(String nombre, Integer idCategoriaExcluir) {
        boolean existeDuplicado = (idCategoriaExcluir == null)
                ? categoriaServicioRepository.existsByNombreIgnoreCase(nombre)
                : categoriaServicioRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(nombre, idCategoriaExcluir);

        if (existeDuplicado) {
            throw new ConflictException(
                    "Ya existe una categoría de servicio registrada con el nombre: " + nombre);
        }
    }

    // Limpia espacios sobrantes de un texto opcional.
    // Recibe texto (cadena de entrada posiblemente nula o vacía).
    // Retorna la cadena recortada o null si está vacía.
    private String normalizarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    // Convierte una entidad CategoriaServicio a su representación DTO de respuesta.
    // Recibe categoria (entidad JPA de categoría).
    // Retorna el DTO de respuesta con los campos de la categoría.
    private CategoriaServicioResponseDTO mapearACategoriaResponseDTO(CategoriaServicio categoria) {
        return CategoriaServicioResponseDTO.builder()
                .idCategoria(categoria.getIdCategoria())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .activo(categoria.getActivo())
                .build();
    }
}
