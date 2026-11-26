package com.veterinaria.personal.service;

import com.veterinaria.agenda.entity.HorarioClinica;
import com.veterinaria.agenda.repository.HorarioClinicaRepository;
import com.veterinaria.agenda.service.HorarioClinicaService;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import com.veterinaria.personal.dto.HorarioPersonalRequestDTO;
import com.veterinaria.personal.dto.HorarioPersonalResponseDTO;
import com.veterinaria.personal.entity.HorarioPersonal;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.HorarioPersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lógica de los turnos de trabajo semanales del personal ({@code HORARIOS_PERSONAL}).
 * <p>
 * Reglas: el fin va después del inicio (sin cruzar la medianoche), el turno cabe en el horario
 * de la clínica de ese día, y los turnos de una misma persona en un mismo día no se enciman.
 * Dos turnos seguidos (08:00-13:00 y 13:00-17:00) sí se permiten.
 * <p>
 * Junto con la revisión de {@link HorarioClinicaService} al cambiar el horario de la clínica,
 * garantiza que ningún turno quede fuera del horario de atención.
 */
@Service
@RequiredArgsConstructor
public class HorarioPersonalService {

    public static final String MENSAJE_RANGO_INVALIDO =
            "La hora de fin debe ser posterior a la hora de inicio (el turno no puede cruzar la medianoche)";

    /** Nombre de cada día según su número: 0 = domingo ... 6 = sábado. */
    private static final String[] NOMBRES_DIA =
            {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private final HorarioPersonalRepository horarioRepository;
    private final HorarioClinicaRepository horarioClinicaRepository;
    private final PersonalService personalService;

    /**
     * Agrega un turno al empleado.
     *
     * @throws ResourceNotFoundException si el empleado no existe (404)
     * @throws BusinessException         si el fin no es posterior al inicio (400)
     * @throws ConflictException         si queda fuera del horario de la clínica, o se encima con otro
     *                                   de sus turnos ese día (409)
     */
    @Transactional
    public HorarioPersonalResponseDTO agregar(Integer idPersonal, HorarioPersonalRequestDTO dto) {
        Personal personal = personalService.buscarOError(idPersonal);
        LocalTime inicio = sinSegundos(dto.getHoraInicio());
        LocalTime fin = sinSegundos(dto.getHoraFin());
        validarRango(inicio, fin);
        validarQueCabaEnLaClinica(dto.getDiaSemana(), inicio, fin);
        validarQueNoSeEncime(idPersonal, dto.getDiaSemana(), inicio, fin, null);

        HorarioPersonal horario = horarioRepository.save(HorarioPersonal.builder()
                .personal(personal)
                .diaSemana(dto.getDiaSemana())
                .horaInicio(inicio)
                .horaFin(fin)
                .build());
        return mapear(horario);
    }

    /**
     * Turnos del empleado de domingo a sábado, ordenados por hora de inicio.
     *
     * @throws ResourceNotFoundException si el empleado no existe (404)
     */
    @Transactional(readOnly = true)
    public List<HorarioPersonalResponseDTO> listar(Integer idPersonal) {
        personalService.buscarOError(idPersonal);
        return horarioRepository.findByPersonalIdPersonalOrderByDiaSemanaAscHoraInicioAsc(idPersonal)
                .stream().map(this::mapear).toList();
    }

    /**
     * Turnos de quien inició sesión, a partir del correo de su token.
     *
     * @throws ResourceNotFoundException si su cuenta no tiene perfil de personal (404)
     */
    @Transactional(readOnly = true)
    public List<HorarioPersonalResponseDTO> listarMisHorarios(String correo) {
        Personal personal = personalService.buscarPorCorreoOError(correo);
        return listar(personal.getIdPersonal());
    }

    /**
     * Edita un turno del empleado. Al revisar choques no se compara consigo mismo.
     *
     * @throws ResourceNotFoundException si el turno no existe o es de otro empleado (404)
     * @throws BusinessException         si el fin no es posterior al inicio (400)
     * @throws ConflictException         si queda fuera del horario de la clínica, o se encima con otro
     *                                   de sus turnos ese día (409)
     */
    @Transactional
    public HorarioPersonalResponseDTO actualizar(Integer idPersonal, Integer idHorario, HorarioPersonalRequestDTO dto) {
        HorarioPersonal horario = buscarOError(idPersonal, idHorario);
        LocalTime inicio = sinSegundos(dto.getHoraInicio());
        LocalTime fin = sinSegundos(dto.getHoraFin());
        validarRango(inicio, fin);
        validarQueCabaEnLaClinica(dto.getDiaSemana(), inicio, fin);
        validarQueNoSeEncime(idPersonal, dto.getDiaSemana(), inicio, fin, idHorario);

        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
        return mapear(horario);
    }

    /**
     * Borra un turno del empleado.
     *
     * @throws ResourceNotFoundException si el turno no existe o es de otro empleado (404)
     */
    @Transactional
    public void eliminar(Integer idPersonal, Integer idHorario) {
        horarioRepository.delete(buscarOError(idPersonal, idHorario));
    }

    /** Nombre del día ("Lunes") para su número (1). Lo usará también la agenda. */
    public static String nombreDia(int diaSemana) {
        return NOMBRES_DIA[diaSemana];
    }

    // ---------- Ayudantes ----------

    /** El turno debe existir y ser del empleado de la ruta; si no, 404 (sin revelar de quién es). */
    private HorarioPersonal buscarOError(Integer idPersonal, Integer idHorario) {
        personalService.buscarOError(idPersonal);
        return horarioRepository.findByIdHorarioPersonalAndPersonalIdPersonal(idHorario, idPersonal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Horario no encontrado con id " + idHorario + " para el personal " + idPersonal));
    }

    private void validarRango(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new BusinessException(MENSAJE_RANGO_INVALIDO);
        }
    }

    /**
     * El turno debe caer en un día en que la clínica abre y dentro de su horario. Un turno que
     * coincide exactamente con la apertura y el cierre sí cabe.
     * <p>
     * Si el día no tuviera horario registrado (con los datos iniciales siempre existen los 7),
     * no se bloquea el turno.
     */
    private void validarQueCabaEnLaClinica(Integer diaSemana, LocalTime inicio, LocalTime fin) {
        HorarioClinica clinica = horarioClinicaRepository.findByDiaSemana(diaSemana).orElse(null);
        if (clinica == null) {
            return;
        }
        if (!Boolean.TRUE.equals(clinica.getLaborable())) {
            throw new ConflictException("La clínica no abre el " + nombreDia(diaSemana)
                    + ": no se puede asignar un turno ese día");
        }
        if (HorarioClinicaService.turnoFueraDeHorario(true, clinica.getHoraApertura(), clinica.getHoraCierre(),
                inicio, fin)) {
            throw new ConflictException("El turno queda fuera del horario de la clínica del " + nombreDia(diaSemana)
                    + " (" + clinica.getHoraApertura() + " a " + clinica.getHoraCierre() + ")");
        }
    }

    /**
     * Dos turnos se enciman si cada uno empieza antes de que termine el otro. Si uno termina
     * justo cuando empieza el otro, no se enciman.
     *
     * @param idHorarioExcluido el turno que se está editando, para no compararlo consigo mismo
     */
    private void validarQueNoSeEncime(Integer idPersonal, Integer diaSemana, LocalTime inicio, LocalTime fin,
                                      Integer idHorarioExcluido) {
        horarioRepository.findByPersonalIdPersonalAndDiaSemanaOrderByHoraInicioAsc(idPersonal, diaSemana).stream()
                .filter(otro -> !otro.getIdHorarioPersonal().equals(idHorarioExcluido))
                .filter(otro -> otro.getHoraInicio().isBefore(fin) && otro.getHoraFin().isAfter(inicio))
                .findFirst()
                .ifPresent(otro -> {
                    throw new ConflictException("El turno se encima con el turno del " + nombreDia(diaSemana)
                            + " de " + otro.getHoraInicio() + " a " + otro.getHoraFin());
                });
    }

    /** Las horas se manejan en minutos exactos: si llegan segundos, se descartan. */
    private LocalTime sinSegundos(LocalTime hora) {
        return hora.truncatedTo(ChronoUnit.MINUTES);
    }

    private HorarioPersonalResponseDTO mapear(HorarioPersonal horario) {
        return HorarioPersonalResponseDTO.builder()
                .idHorario(horario.getIdHorarioPersonal())
                .idPersonal(horario.getPersonal().getIdPersonal())
                .diaSemana(horario.getDiaSemana())
                .nombreDia(nombreDia(horario.getDiaSemana()))
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .build();
    }
}
