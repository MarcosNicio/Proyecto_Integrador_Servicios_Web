package com.veterinaria.servicios.service;

import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.servicios.dto.ServicioRequestDTO;
import com.veterinaria.servicios.dto.ServicioResponseDTO;
import com.veterinaria.servicios.dto.TarifaServicioResponseDTO;
import com.veterinaria.servicios.entity.CategoriaServicio;
import com.veterinaria.servicios.entity.Servicio;
import com.veterinaria.servicios.entity.TarifaServicio;
import com.veterinaria.servicios.repository.CategoriaServicioRepository;
import com.veterinaria.servicios.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// Servicio de lógica de negocio para el catálogo de servicios veterinarios (SERVICIOS).
// Gestiona el CRUD de servicios, la validación de categorías activas, la verificación
// desacoplada de tipos de consultorio y el soporte de baja lógica/reactivación.
@Service
@RequiredArgsConstructor
public class ServicioService {

    // Repositorio JPA para operaciones sobre el catálogo de servicios
    private final ServicioRepository servicioRepository;

    // Repositorio JPA para validar la existencia y estado de categorías de servicio
    private final CategoriaServicioRepository categoriaServicioRepository;

    // MÉTODOS PÚBLICOS DE NEGOCIO - CATÁLOGO DE SERVICIOS

    // Registra un nuevo servicio en el catálogo validando que la categoría exista y esté activa, que el tipo de consultorio (si se especifica) exista y esté activo, y que el nombre no se repita dentro de la misma categoría.
    // Recibe dto con los datos del servicio a registrar.
    // Retorna el DTO con la información del servicio creado.
    // Lanza ResourceNotFoundException si la categoría o el tipo de consultorio no existen, BusinessException si la categoría asignada está inactiva y ConflictException si ya existe un servicio con el mismo nombre en la categoría.
    @Transactional
    public ServicioResponseDTO crearServicio(ServicioRequestDTO dto) {
        CategoriaServicio categoria = buscarCategoriaActivaOError(dto.getIdCategoria());
        validarTipoConsultorioSiAplica(dto.getIdTipoConsultorio());

        String nombreNormalizado = dto.getNombre().trim();
        validarUnicidadNombreEnCategoria(nombreNormalizado, categoria.getIdCategoria(), null);

        Servicio nuevoServicio = new Servicio();
        aplicarDatosServicio(nuevoServicio, dto, categoria, nombreNormalizado);
        nuevoServicio.setActivo(true);

        Servicio servicioGuardado = servicioRepository.save(nuevoServicio);
        return mapearAServicioResponseDTO(servicioGuardado);
    }

    // Obtiene el detalle completo de un servicio por su identificador único, incluyendo las tarifas dinámicas asociadas.
    // Recibe idServicio con el identificador del servicio a consultar.
    // Retorna el DTO con el detalle del servicio y sus tarifas.
    // Lanza ResourceNotFoundException si no existe un servicio con ese ID.
    @Transactional(readOnly = true)
    public ServicioResponseDTO obtenerPorId(Integer idServicio) {
        Servicio servicio = buscarServicioPorIdOError(idServicio);
        return mapearAServicioResponseDTO(servicio);
    }

    // Lista los servicios del catálogo con soporte opcional para incluir inactivos, filtrar por categoría y buscar por coincidencia parcial en el nombre.
    // Recibe incluirInactivos (true para incluir desactivados; false para solo activos), idCategoria (identificador opcional de categoría) y nombre (texto opcional para filtrar por coincidencia parcial).
    // Retorna la lista de servicios que cumplen los criterios especificados.
    // Lanza ResourceNotFoundException si se especifica un idCategoria inexistente.
    @Transactional(readOnly = true)
    public List<ServicioResponseDTO> listarServicios(
            boolean incluirInactivos,
            Integer idCategoria,
            String nombre
    ) {
        String termino = normalizarTextoOpcional(nombre);
        List<Servicio> servicios;

        if (idCategoria != null) {
            if (!categoriaServicioRepository.existsById(idCategoria)) {
                throw new ResourceNotFoundException(
                        "No se encontró la categoría de servicio con ID: " + idCategoria);
            }
            servicios = incluirInactivos
                    ? servicioRepository.findByCategoriaIdCategoria(idCategoria)
                    : servicioRepository.findByCategoriaIdCategoriaAndActivoTrue(idCategoria);

            if (termino != null) {
                String terminoLower = termino.toLowerCase(Locale.ROOT);
                servicios = servicios.stream()
                        .filter(s -> s.getNombre().toLowerCase(Locale.ROOT).contains(terminoLower))
                        .toList();
            }
        } else if (termino != null) {
            if (incluirInactivos) {
                String terminoLower = termino.toLowerCase(Locale.ROOT);
                servicios = servicioRepository.findAll().stream()
                        .filter(s -> s.getNombre().toLowerCase(Locale.ROOT).contains(terminoLower))
                        .toList();
            } else {
                servicios = servicioRepository.findByNombreContainingIgnoreCaseAndActivoTrue(termino);
            }
        } else {
            servicios = incluirInactivos
                    ? servicioRepository.findAll()
                    : servicioRepository.findByActivoTrue();
        }

        return servicios.stream()
                .map(this::mapearAServicioResponseDTO)
                .toList();
    }

    // Actualiza la información de un servicio existente validando su categoría, tipo de consultorio opcional y unicidad de nombre dentro de la categoría.
    // Recibe idServicio (identificador del servicio a modificar) y dto (nuevos datos del servicio).
    // Retorna el DTO con la información actualizada del servicio.
    // Lanza ResourceNotFoundException si el servicio, la categoría o el tipo de consultorio no existen, BusinessException si la categoría seleccionada está inactiva y ConflictException si el nombre colisiona con otro servicio en la misma categoría.
    @Transactional
    public ServicioResponseDTO actualizarServicio(Integer idServicio, ServicioRequestDTO dto) {
        Servicio servicioExistente = buscarServicioPorIdOError(idServicio);
        CategoriaServicio categoria = buscarCategoriaActivaOError(dto.getIdCategoria());
        validarTipoConsultorioSiAplica(dto.getIdTipoConsultorio());

        String nombreNormalizado = dto.getNombre().trim();
        validarUnicidadNombreEnCategoria(nombreNormalizado, categoria.getIdCategoria(), idServicio);

        aplicarDatosServicio(servicioExistente, dto, categoria, nombreNormalizado);

        Servicio servicioActualizado = servicioRepository.save(servicioExistente);
        return mapearAServicioResponseDTO(servicioActualizado);
    }

    // Realiza la baja lógica de un servicio marcándolo como inactivo (activo = false), preservando el historial de citas y órdenes previas.
    // Recibe idServicio con el identificador del servicio a desactivar. No retorna valor.
    // Lanza ResourceNotFoundException si el servicio no existe.
    @Transactional
    public void desactivarServicio(Integer idServicio) {
        Servicio servicio = buscarServicioPorIdOError(idServicio);
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    // Reactiva un servicio previamente dado de baja lógica (activo = true), verificando que su categoría asociada se encuentre activa.
    // Recibe idServicio con el identificador del servicio a reactivar.
    // Retorna el DTO con la información del servicio reactivado.
    // Lanza ResourceNotFoundException si el servicio no existe y BusinessException si la categoría asociada al servicio está inactiva.
    @Transactional
    public ServicioResponseDTO reactivarServicio(Integer idServicio) {
        Servicio servicio = buscarServicioPorIdOError(idServicio);

        if (!Boolean.TRUE.equals(servicio.getCategoria().getActivo())) {
            throw new BusinessException(
                    "No se puede reactivar el servicio porque su categoría '"
                            + servicio.getCategoria().getNombre() + "' se encuentra inactiva");
        }

        servicio.setActivo(true);
        Servicio servicioReactivado = servicioRepository.save(servicio);
        return mapearAServicioResponseDTO(servicioReactivado);
    }

    // MÉTODOS AUXILIARES PRIVADOS

    // Busca un servicio por su ID o lanza error si no existe.
    // Recibe idServicio con el identificador del servicio.
    // Retorna la entidad Servicio encontrada.
    // Lanza ResourceNotFoundException si no existe el servicio.
    private Servicio buscarServicioPorIdOError(Integer idServicio) {
        return servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el servicio con ID: " + idServicio));
    }

    // Busca una categoría por su ID y verifica que se encuentre activa.
    // Recibe idCategoria con el identificador de la categoría.
    // Retorna la entidad CategoriaServicio activa.
    // Lanza ResourceNotFoundException si la categoría no existe y BusinessException si la categoría está inactiva.
    private CategoriaServicio buscarCategoriaActivaOError(Integer idCategoria) {
        CategoriaServicio categoria = categoriaServicioRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la categoría de servicio con ID: " + idCategoria));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new BusinessException(
                    "La categoría de servicio con ID " + idCategoria + " se encuentra inactiva");
        }
        return categoria;
    }

    // Valida la existencia de un tipo de consultorio activo en TIPOS_CONSULTORIO únicamente cuando el identificador es proporcionado (desacoplado de Dev A).
    // Recibe idTipoConsultorio con el identificador opcional del tipo de consultorio. No retorna valor.
    // Lanza ResourceNotFoundException si el ID no existe o no está activo en TIPOS_CONSULTORIO.
    private void validarTipoConsultorioSiAplica(Integer idTipoConsultorio) {
        if (idTipoConsultorio != null && !servicioRepository.existeTipoConsultorioActivo(idTipoConsultorio)) {
            throw new ResourceNotFoundException(
                    "No se encontró un tipo de consultorio activo con ID: " + idTipoConsultorio);
        }
    }

    // Verifica que no exista otro servicio con el mismo nombre dentro de la misma categoría.
    // Recibe nombre (nombre normalizado del servicio), idCategoria (identificador de la categoría) e idServicioExcluir (identificador del servicio a excluir, o null al crear). No retorna valor.
    // Lanza ConflictException si existe duplicado en la misma categoría.
    private void validarUnicidadNombreEnCategoria(
            String nombre,
            Integer idCategoria,
            Integer idServicioExcluir
    ) {
        boolean existeDuplicado = (idServicioExcluir == null)
                ? servicioRepository.existsByNombreIgnoreCaseAndCategoriaIdCategoria(nombre, idCategoria)
                : servicioRepository.existsByNombreIgnoreCaseAndCategoriaIdCategoriaAndIdServicioNot(
                        nombre, idCategoria, idServicioExcluir);

        if (existeDuplicado) {
            throw new ConflictException(
                    "Ya existe un servicio con el nombre '" + nombre + "' dentro de la categoría indicada");
        }
    }

    // Asigna los atributos normalizados del DTO a la entidad Servicio.
    // Recibe servicio (entidad destino a poblar), dto (datos validados de entrada), categoria (categoría activa previamente validada) y nombreNormalizado (nombre del servicio recortado). No retorna valor.
    private void aplicarDatosServicio(
            Servicio servicio,
            ServicioRequestDTO dto,
            CategoriaServicio categoria,
            String nombreNormalizado
    ) {
        servicio.setCategoria(categoria);
        servicio.setIdTipoConsultorio(dto.getIdTipoConsultorio());
        servicio.setNombre(nombreNormalizado);
        servicio.setDescripcion(normalizarTextoOpcional(dto.getDescripcion()));
        servicio.setDuracionMinutos(dto.getDuracionMinutos());
        servicio.setPrecioBase(dto.getPrecioBase());
    }

    // Limpia espacios sobrantes de un texto opcional.
    // Recibe texto (texto de entrada).
    // Retorna el texto recortado o null si está en blanco.
    private String normalizarTextoOpcional(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    // Mapea una entidad Servicio y sus tarifas dinámicas a un ServicioResponseDTO.
    // Recibe servicio (entidad JPA de servicio).
    // Retorna el DTO de respuesta completo del servicio.
    private ServicioResponseDTO mapearAServicioResponseDTO(Servicio servicio) {
        List<TarifaServicioResponseDTO> tarifasDTO = servicio.getTarifas().stream()
                .map(this::mapearATarifaResponseDTO)
                .toList();

        return ServicioResponseDTO.builder()
                .idServicio(servicio.getIdServicio())
                .idCategoria(servicio.getCategoria().getIdCategoria())
                .nombreCategoria(servicio.getCategoria().getNombre())
                .idTipoConsultorio(servicio.getIdTipoConsultorio())
                .nombre(servicio.getNombre())
                .descripcion(servicio.getDescripcion())
                .duracionMinutos(servicio.getDuracionMinutos())
                .precioBase(servicio.getPrecioBase())
                .activo(servicio.getActivo())
                .tarifas(tarifasDTO)
                .build();
    }

    // Mapea una entidad TarifaServicio a su representación DTO de respuesta.
    // Recibe tarifa (entidad JPA de tarifa).
    // Retorna el DTO de respuesta de la tarifa.
    private TarifaServicioResponseDTO mapearATarifaResponseDTO(TarifaServicio tarifa) {
        return TarifaServicioResponseDTO.builder()
                .idTarifa(tarifa.getIdTarifa())
                .idServicio(tarifa.getServicio().getIdServicio())
                .especie(tarifa.getEspecie())
                .pesoMinKg(tarifa.getPesoMinKg())
                .pesoMaxKg(tarifa.getPesoMaxKg())
                .precioCalculado(tarifa.getPrecioCalculado())
                .build();
    }
}
