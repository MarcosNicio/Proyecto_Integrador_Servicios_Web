package com.veterinaria.agenda.service;

import com.veterinaria.agenda.dto.TipoConsultorioRequestDTO;
import com.veterinaria.agenda.dto.TipoConsultorioResponseDTO;
import com.veterinaria.agenda.entity.TipoConsultorio;
import com.veterinaria.agenda.repository.ConsultorioRepository;
import com.veterinaria.agenda.repository.TipoConsultorioRepository;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lógica de los tipos de consultorio ({@code TIPOS_CONSULTORIO}).
 * <p>
 * Los tipos no se borran: se desactivan. El módulo de servicios solo acepta tipos activos, así
 * que desactivar uno impide usarlo en servicios nuevos. Un tipo con consultorios activos no se
 * puede desactivar, para que nunca haya un consultorio activo de un tipo desactivado.
 */
@Service
@RequiredArgsConstructor
public class TipoConsultorioService {

    private final TipoConsultorioRepository tipoRepository;
    private final ConsultorioRepository consultorioRepository;

    /**
     * Lista los tipos ordenados por nombre.
     *
     * @param activo {@code true}: solo activos; {@code false}: solo desactivados; {@code null}: todos
     */
    @Transactional(readOnly = true)
    public List<TipoConsultorioResponseDTO> listar(Boolean activo) {
        return tipoRepository.findAllByOrderByNombreAsc().stream()
                .filter(tipo -> activo == null || activo.equals(tipo.getActivo()))
                .map(this::mapear)
                .toList();
    }

    /**
     * Crea un tipo activo.
     *
     * @throws ConflictException si ya existe un tipo con ese nombre, sin importar mayúsculas (409)
     */
    @Transactional
    public TipoConsultorioResponseDTO crear(TipoConsultorioRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, null);

        TipoConsultorio tipo = tipoRepository.save(TipoConsultorio.builder()
                .nombre(nombre)
                .descripcion(textoOpcional(dto.getDescripcion()))
                .activo(true)
                .build());
        return mapear(tipo);
    }

    /**
     * Edita el nombre y la descripción. El tipo puede conservar su propio nombre.
     *
     * @throws ResourceNotFoundException si no existe (404)
     * @throws ConflictException         si otro tipo ya tiene ese nombre (409)
     */
    @Transactional
    public TipoConsultorioResponseDTO actualizar(Integer idTipo, TipoConsultorioRequestDTO dto) {
        TipoConsultorio tipo = buscarOError(idTipo);
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, idTipo);

        tipo.setNombre(nombre);
        tipo.setDescripcion(textoOpcional(dto.getDescripcion()));
        return mapear(tipo);
    }

    /**
     * Desactiva el tipo. Si ya estaba desactivado, no pasa nada.
     * <p>
     * No se puede desactivar mientras esté en uso: ni con consultorios activos, ni si lo requieren
     * servicios activos (se revisan primero los consultorios). Así no queda un consultorio o un
     * servicio activo apuntando a un tipo desactivado.
     *
     * @throws ResourceNotFoundException si no existe (404)
     * @throws ConflictException         si tiene consultorios activos o lo usan servicios activos (409)
     */
    @Transactional
    public void desactivar(Integer idTipo) {
        TipoConsultorio tipo = buscarOError(idTipo);
        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            return;
        }
        long consultorios = consultorioRepository.countByTipoConsultorioIdTipoConsultorioAndActivoTrue(idTipo);
        if (consultorios > 0) {
            throw new ConflictException("No se puede desactivar el tipo '" + tipo.getNombre() + "': tiene "
                    + consultorios + " consultorio(s) activo(s). Desactiva primero sus consultorios");
        }
        long servicios = tipoRepository.contarServiciosActivos(idTipo);
        if (servicios > 0) {
            throw new ConflictException("No se puede desactivar el tipo '" + tipo.getNombre() + "': lo usan "
                    + servicios + " servicio(s) activo(s). Desactiva o cambia primero esos servicios");
        }
        tipo.setActivo(false);
    }

    /**
     * Reactiva el tipo. Si ya estaba activo, no pasa nada.
     *
     * @throws ResourceNotFoundException si no existe (404)
     */
    @Transactional
    public TipoConsultorioResponseDTO reactivar(Integer idTipo) {
        TipoConsultorio tipo = buscarOError(idTipo);
        tipo.setActivo(true);
        return mapear(tipo);
    }

    /** Busca un tipo o lanza 404. Es pública porque la usa el servicio de consultorios. */
    @Transactional(readOnly = true)
    public TipoConsultorio buscarOError(Integer idTipo) {
        return tipoRepository.findById(idTipo)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de consultorio no encontrado con id " + idTipo));
    }

    // ---------- Ayudantes ----------

    /** El nombre no puede estar en uso por otro tipo (sin importar mayúsculas). */
    private void validarNombreLibre(String nombre, Integer idTipoExcluido) {
        tipoRepository.findByNombreIgnoreCase(nombre)
                .filter(otro -> !otro.getIdTipoConsultorio().equals(idTipoExcluido))
                .ifPresent(otro -> {
                    throw new ConflictException("Ya existe un tipo de consultorio con el nombre '" + nombre + "'");
                });
    }

    /** Quita espacios; un texto vacío se guarda como {@code null}. */
    private String textoOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private TipoConsultorioResponseDTO mapear(TipoConsultorio tipo) {
        return TipoConsultorioResponseDTO.builder()
                .idTipoConsultorio(tipo.getIdTipoConsultorio())
                .nombre(tipo.getNombre())
                .descripcion(tipo.getDescripcion())
                .activo(tipo.getActivo())
                .consultoriosActivos(consultorioRepository
                        .countByTipoConsultorioIdTipoConsultorioAndActivoTrue(tipo.getIdTipoConsultorio()))
                .build();
    }
}
