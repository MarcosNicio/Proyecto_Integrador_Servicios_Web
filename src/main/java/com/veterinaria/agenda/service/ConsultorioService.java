package com.veterinaria.agenda.service;

import com.veterinaria.agenda.dto.ConsultorioRequestDTO;
import com.veterinaria.agenda.dto.ConsultorioResponseDTO;
import com.veterinaria.agenda.entity.Consultorio;
import com.veterinaria.agenda.entity.TipoConsultorio;
import com.veterinaria.agenda.repository.ConsultorioRepository;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de los consultorios ({@code CONSULTORIOS}).
 * <p>
 * Los consultorios no se borran (las citas pasadas los referencian): se desactivan para que
 * dejen de ofrecerse en citas nuevas. Un consultorio solo puede pertenecer a un tipo activo.
 */
@Service
@RequiredArgsConstructor
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final TipoConsultorioService tipoService;

    /**
     * Lista los consultorios ordenados por nombre. Los filtros son opcionales y se combinan.
     *
     * @param idTipo solo los de ese tipo ({@code null} = todos)
     * @param activo {@code true}: solo activos; {@code false}: solo desactivados; {@code null}: todos
     */
    @Transactional(readOnly = true)
    public List<ConsultorioResponseDTO> listar(Integer idTipo, Boolean activo) {
        List<Consultorio> consultorios = (idTipo == null)
                ? consultorioRepository.findAllByOrderByNombreAsc()
                : consultorioRepository.findByTipoConsultorioIdTipoConsultorioOrderByNombreAsc(idTipo);

        return consultorios.stream()
                .filter(c -> activo == null || activo.equals(c.getActivo()))
                .map(this::mapear)
                .toList();
    }

    /** @throws ResourceNotFoundException si no existe (404) */
    @Transactional(readOnly = true)
    public ConsultorioResponseDTO obtenerPorId(Integer idConsultorio) {
        return mapear(buscarOError(idConsultorio));
    }

    /**
     * Crea un consultorio activo.
     *
     * @throws ResourceNotFoundException si el tipo no existe (404)
     * @throws BusinessException         si el tipo está desactivado (400)
     * @throws ConflictException         si ya existe un consultorio con ese nombre (409)
     */
    @Transactional
    public ConsultorioResponseDTO crear(ConsultorioRequestDTO dto) {
        TipoConsultorio tipo = tipoService.buscarOError(dto.getIdTipoConsultorio());
        validarTipoActivo(tipo);
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, null);

        Consultorio consultorio = consultorioRepository.save(Consultorio.builder()
                .tipoConsultorio(tipo)
                .nombre(nombre)
                .activo(true)
                .build());
        return mapear(consultorio);
    }

    /**
     * Edita el nombre y el tipo. El consultorio puede conservar su propio nombre y su tipo
     * actual aunque ese tipo esté desactivado; el tipo nuevo, si cambia, debe estar activo.
     *
     * @throws ResourceNotFoundException si el consultorio o el tipo no existen (404)
     * @throws BusinessException         si se asigna un tipo desactivado (400)
     * @throws ConflictException         si otro consultorio ya tiene ese nombre (409)
     */
    @Transactional
    public ConsultorioResponseDTO actualizar(Integer idConsultorio, ConsultorioRequestDTO dto) {
        Consultorio consultorio = buscarOError(idConsultorio);
        TipoConsultorio tipo = tipoService.buscarOError(dto.getIdTipoConsultorio());
        boolean cambiaDeTipo = !tipo.getIdTipoConsultorio()
                .equals(consultorio.getTipoConsultorio().getIdTipoConsultorio());
        if (cambiaDeTipo) {
            validarTipoActivo(tipo);
        }
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, idConsultorio);

        consultorio.setNombre(nombre);
        consultorio.setTipoConsultorio(tipo);
        return mapear(consultorio);
    }

    /**
     * Desactiva el consultorio: deja de ofrecerse para citas nuevas. Si ya estaba desactivado,
     * no pasa nada.
     *
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public void desactivar(Integer idConsultorio) {
        buscarOError(idConsultorio).setActivo(false);
    }

    /**
     * Reactiva el consultorio. Si ya estaba activo, no pasa nada.
     *
     * @throws ResourceNotFoundException si no existe (404)
     * @throws ConflictException         si su tipo está desactivado: primero hay que reactivar el tipo (409)
     */
    @Transactional
    public ConsultorioResponseDTO reactivar(Integer idConsultorio) {
        Consultorio consultorio = buscarOError(idConsultorio);
        TipoConsultorio tipo = consultorio.getTipoConsultorio();
        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new ConflictException("No se puede reactivar el consultorio '" + consultorio.getNombre()
                    + "': su tipo '" + tipo.getNombre() + "' está desactivado. Reactiva primero el tipo");
        }
        consultorio.setActivo(true);
        return mapear(consultorio);
    }

    // ---------- Ayudantes ----------

    private Consultorio buscarOError(Integer idConsultorio) {
        return consultorioRepository.findById(idConsultorio)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado con id " + idConsultorio));
    }

    private void validarTipoActivo(TipoConsultorio tipo) {
        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new BusinessException("El tipo de consultorio '" + tipo.getNombre() + "' está desactivado");
        }
    }

    /** El nombre no puede estar en uso por otro consultorio (sin importar mayúsculas). */
    private void validarNombreLibre(String nombre, Integer idConsultorioExcluido) {
        consultorioRepository.findByNombreIgnoreCase(nombre)
                .filter(otro -> !otro.getIdConsultorio().equals(idConsultorioExcluido))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe un consultorio con el nombre '" + nombre + "'");
                });
    }

    private ConsultorioResponseDTO mapear(Consultorio consultorio) {
        TipoConsultorio tipo = consultorio.getTipoConsultorio();
        return ConsultorioResponseDTO.builder()
                .idConsultorio(consultorio.getIdConsultorio())
                .nombre(consultorio.getNombre())
                .activo(consultorio.getActivo())
                .idTipoConsultorio(tipo.getIdTipoConsultorio())
                .nombreTipo(tipo.getNombre())
                .build();
    }
}
