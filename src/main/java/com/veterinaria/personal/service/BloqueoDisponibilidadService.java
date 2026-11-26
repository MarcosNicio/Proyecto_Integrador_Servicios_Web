package com.veterinaria.personal.service;

import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.personal.dto.BloqueoRequestDTO;
import com.veterinaria.personal.dto.BloqueoResponseDTO;
import com.veterinaria.personal.entity.BloqueoDisponibilidad;
import com.veterinaria.personal.entity.MotivoBloqueo;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.BloqueoDisponibilidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lógica de los bloqueos de agenda del personal ({@code BLOQUEOS_DISPONIBILIDAD}).
 * <p>
 * Reglas: el fin va después del inicio y en el futuro, los bloqueos de una misma persona no se
 * enciman (seguidos sí) y un bloqueo que ya terminó queda como historial: no se edita ni se borra.
 */
@Service
@RequiredArgsConstructor
public class BloqueoDisponibilidadService {

    public static final String MENSAJE_RANGO_INVALIDO = "La fecha de fin debe ser posterior a la fecha de inicio";
    public static final String MENSAJE_YA_TERMINO = "El bloqueo ya terminó: la fecha de fin debe ser futura";
    public static final String MENSAJE_HISTORIAL = "Un bloqueo que ya terminó no se puede editar ni borrar";

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BloqueoDisponibilidadRepository bloqueoRepository;
    private final PersonalService personalService;

    /**
     * Registra un bloqueo al empleado.
     *
     * @throws ResourceNotFoundException si el empleado no existe (404)
     * @throws BusinessException         si el rango es inválido o ya terminó (400)
     * @throws ConflictException         si se encima con otro de sus bloqueos (409)
     */
    @Transactional
    public BloqueoResponseDTO registrar(Integer idPersonal, BloqueoRequestDTO dto) {
        Personal personal = personalService.buscarOError(idPersonal);
        LocalDateTime inicio = sinSegundos(dto.getFechaInicio());
        LocalDateTime fin = sinSegundos(dto.getFechaFin());
        validarFechas(inicio, fin);
        validarQueNoSeEncime(idPersonal, inicio, fin, null);

        BloqueoDisponibilidad bloqueo = bloqueoRepository.save(BloqueoDisponibilidad.builder()
                .personal(personal)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo(MotivoBloqueo.valueOf(dto.getMotivo()))
                .notas(textoOpcional(dto.getNotas()))
                .build());
        return mapear(bloqueo);
    }

    /**
     * Bloqueos del empleado ordenados por fecha de inicio.
     *
     * @param incluirPasados {@code false}: solo vigentes y futuros; {@code true}: también los que ya terminaron
     * @throws ResourceNotFoundException si el empleado no existe (404)
     */
    @Transactional(readOnly = true)
    public List<BloqueoResponseDTO> listar(Integer idPersonal, boolean incluirPasados) {
        personalService.buscarOError(idPersonal);
        List<BloqueoDisponibilidad> bloqueos = incluirPasados
                ? bloqueoRepository.findByPersonalIdPersonalOrderByFechaInicioAsc(idPersonal)
                : bloqueoRepository.findByPersonalIdPersonalAndFechaFinAfterOrderByFechaInicioAsc(
                        idPersonal, LocalDateTime.now());
        return bloqueos.stream().map(this::mapear).toList();
    }

    /**
     * Bloqueos de quien inició sesión, a partir del correo de su token.
     *
     * @throws ResourceNotFoundException si su cuenta no tiene perfil de personal (404)
     */
    @Transactional(readOnly = true)
    public List<BloqueoResponseDTO> listarMisBloqueos(String correo, boolean incluirPasados) {
        Personal personal = personalService.buscarPorCorreoOError(correo);
        return listar(personal.getIdPersonal(), incluirPasados);
    }

    /**
     * Edita un bloqueo que aún no termina. Al revisar choques no se compara consigo mismo.
     *
     * @throws ResourceNotFoundException si el bloqueo no existe o es de otro empleado (404)
     * @throws BusinessException         si ya terminó, o si el nuevo rango es inválido o ya terminó (400)
     * @throws ConflictException         si se encima con otro de sus bloqueos (409)
     */
    @Transactional
    public BloqueoResponseDTO actualizar(Integer idPersonal, Integer idBloqueo, BloqueoRequestDTO dto) {
        BloqueoDisponibilidad bloqueo = buscarVigenteOError(idPersonal, idBloqueo);
        LocalDateTime inicio = sinSegundos(dto.getFechaInicio());
        LocalDateTime fin = sinSegundos(dto.getFechaFin());
        validarFechas(inicio, fin);
        validarQueNoSeEncime(idPersonal, inicio, fin, idBloqueo);

        bloqueo.setFechaInicio(inicio);
        bloqueo.setFechaFin(fin);
        bloqueo.setMotivo(MotivoBloqueo.valueOf(dto.getMotivo()));
        bloqueo.setNotas(textoOpcional(dto.getNotas()));
        return mapear(bloqueo);
    }

    /**
     * Cancela (borra) un bloqueo que aún no termina.
     *
     * @throws ResourceNotFoundException si el bloqueo no existe o es de otro empleado (404)
     * @throws BusinessException         si ya terminó (400)
     */
    @Transactional
    public void eliminar(Integer idPersonal, Integer idBloqueo) {
        bloqueoRepository.delete(buscarVigenteOError(idPersonal, idBloqueo));
    }

    // ---------- Ayudantes ----------

    /** El bloqueo debe existir, ser del empleado de la ruta y no haber terminado. */
    private BloqueoDisponibilidad buscarVigenteOError(Integer idPersonal, Integer idBloqueo) {
        personalService.buscarOError(idPersonal);
        BloqueoDisponibilidad bloqueo = bloqueoRepository.findByIdBloqueoAndPersonalIdPersonal(idBloqueo, idPersonal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bloqueo no encontrado con id " + idBloqueo + " para el personal " + idPersonal));
        if (!bloqueo.getFechaFin().isAfter(LocalDateTime.now())) {
            throw new BusinessException(MENSAJE_HISTORIAL);
        }
        return bloqueo;
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new BusinessException(MENSAJE_RANGO_INVALIDO);
        }
        if (!fin.isAfter(LocalDateTime.now())) {
            throw new BusinessException(MENSAJE_YA_TERMINO);
        }
    }

    /**
     * Usa {@code buscarEncimados}: dos rangos se enciman si cada uno empieza antes de que termine
     * el otro. Si uno termina justo cuando empieza el otro, no se enciman.
     *
     * @param idBloqueoExcluido el bloqueo que se está editando, para no compararlo consigo mismo
     */
    private void validarQueNoSeEncime(Integer idPersonal, LocalDateTime inicio, LocalDateTime fin,
                                      Integer idBloqueoExcluido) {
        bloqueoRepository.buscarEncimados(idPersonal, inicio, fin).stream()
                .filter(otro -> !otro.getIdBloqueo().equals(idBloqueoExcluido))
                .findFirst()
                .ifPresent(otro -> {
                    throw new ConflictException("El bloqueo se encima con otro bloqueo (" + otro.getMotivo()
                            + ") del " + otro.getFechaInicio().format(FORMATO)
                            + " al " + otro.getFechaFin().format(FORMATO));
                });
    }

    /** Las fechas se manejan en minutos exactos: si llegan segundos, se descartan. */
    private LocalDateTime sinSegundos(LocalDateTime fecha) {
        return fecha.truncatedTo(ChronoUnit.MINUTES);
    }

    /** Quita espacios; un texto vacío se guarda como {@code null}. */
    private String textoOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private BloqueoResponseDTO mapear(BloqueoDisponibilidad bloqueo) {
        return BloqueoResponseDTO.builder()
                .idBloqueo(bloqueo.getIdBloqueo())
                .idPersonal(bloqueo.getPersonal().getIdPersonal())
                .fechaInicio(bloqueo.getFechaInicio())
                .fechaFin(bloqueo.getFechaFin())
                .motivo(bloqueo.getMotivo())
                .notas(bloqueo.getNotas())
                .build();
    }
}
