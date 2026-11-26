package com.veterinaria.agenda.service;

import com.veterinaria.agenda.dto.HorarioClinicaRequestDTO;
import com.veterinaria.agenda.dto.HorarioClinicaResponseDTO;
import com.veterinaria.agenda.entity.HorarioClinica;
import com.veterinaria.agenda.repository.HorarioClinicaRepository;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.personal.entity.HorarioPersonal;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.HorarioPersonalRepository;
import com.veterinaria.personal.service.HorarioPersonalService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lógica del horario de atención de la clínica ({@code HORARIOS_CLINICA}).
 * <p>
 * El horario se modifica <b>completo</b> (los 7 días a la vez) y nunca puede dejar fuera de
 * horario un turno del personal: si pasara, no se guarda nada y se devuelve la lista de turnos
 * afectados, para que el administrador los ajuste primero.
 */
@Service
@RequiredArgsConstructor
public class HorarioClinicaService {

    public static final String MENSAJE_SEMANA_INCOMPLETA =
            "Debes enviar el horario de los 7 días de la semana (0 = domingo … 6 = sábado), cada uno una sola vez";

    private static final int DIAS_DE_LA_SEMANA = 7;
    private static final int MAX_TURNOS_EN_MENSAJE = 10;

    private final HorarioClinicaRepository horarioRepository;
    private final HorarioPersonalRepository horarioPersonalRepository;
    private final Validator validator;

    /** Horario de un día ya validado y sin segundos. */
    private record Dia(int diaSemana, LocalTime apertura, LocalTime cierre, boolean laborable) {
    }

    /** La semana completa, de domingo a sábado. */
    @Transactional(readOnly = true)
    public List<HorarioClinicaResponseDTO> obtenerSemana() {
        return horarioRepository.findAllByOrderByDiaSemanaAsc().stream().map(this::mapear).toList();
    }

    /**
     * Reemplaza el horario de la clínica con el de los 7 días. Es todo o nada.
     *
     * @throws BusinessException si no vienen los 7 días una sola vez, falta un dato, o algún cierre
     *                           no es posterior a su apertura (400)
     * @throws ConflictException si algún turno del personal quedaría fuera del nuevo horario (409)
     */
    @Transactional
    public List<HorarioClinicaResponseDTO> actualizarSemana(List<HorarioClinicaRequestDTO> semana) {
        Map<Integer, Dia> nuevoHorario = validarSemana(semana);
        validarQueNingunTurnoQuedeFuera(nuevoHorario);

        for (Dia dia : nuevoHorario.values()) {
            HorarioClinica horario = horarioRepository.findByDiaSemana(dia.diaSemana())
                    .orElseGet(() -> HorarioClinica.builder().diaSemana(dia.diaSemana()).build());
            horario.setHoraApertura(dia.apertura());
            horario.setHoraCierre(dia.cierre());
            horario.setLaborable(dia.laborable());
            horarioRepository.save(horario);
        }
        return obtenerSemana();
    }

    /**
     * Indica si un turno queda fuera del horario de la clínica de ese día: la clínica no abre,
     * o el turno empieza antes de la apertura o termina después del cierre. Un turno que
     * coincide exactamente con la apertura y el cierre sí cabe.
     */
    public static boolean turnoFueraDeHorario(boolean laborable, LocalTime apertura, LocalTime cierre,
                                              LocalTime inicioTurno, LocalTime finTurno) {
        return !laborable || inicioTurno.isBefore(apertura) || finTurno.isAfter(cierre);
    }

    // ---------- Ayudantes ----------

    /** Revisa que vengan los 7 días, completos y con cierre posterior a la apertura. */
    private Map<Integer, Dia> validarSemana(List<HorarioClinicaRequestDTO> semana) {
        if (semana == null || semana.size() != DIAS_DE_LA_SEMANA || semana.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(MENSAJE_SEMANA_INCOMPLETA);
        }
        for (int i = 0; i < semana.size(); i++) {
            String errores = validator.validate(semana.get(i)).stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            if (!errores.isEmpty()) {
                throw new BusinessException("Elemento " + (i + 1) + " de la lista: " + errores);
            }
        }
        if (semana.stream().map(HorarioClinicaRequestDTO::getDiaSemana).distinct().count() != DIAS_DE_LA_SEMANA) {
            throw new BusinessException(MENSAJE_SEMANA_INCOMPLETA);
        }

        Map<Integer, Dia> nuevoHorario = new HashMap<>();
        for (HorarioClinicaRequestDTO dto : semana) {
            LocalTime apertura = sinSegundos(dto.getHoraApertura());
            LocalTime cierre = sinSegundos(dto.getHoraCierre());
            if (!cierre.isAfter(apertura)) {
                throw new BusinessException("El día " + HorarioPersonalService.nombreDia(dto.getDiaSemana())
                        + ": la hora de cierre debe ser posterior a la hora de apertura");
            }
            nuevoHorario.put(dto.getDiaSemana(), new Dia(dto.getDiaSemana(), apertura, cierre, dto.getLaborable()));
        }
        return nuevoHorario;
    }

    /**
     * Revisa los turnos de todo el personal (también de cuentas desactivadas, para que al
     * reactivar a alguien su turno no quede fuera) contra el nuevo horario.
     */
    private void validarQueNingunTurnoQuedeFuera(Map<Integer, Dia> nuevoHorario) {
        List<String> afectados = horarioPersonalRepository.findAllConPersonal().stream()
                .filter(turno -> {
                    Dia dia = nuevoHorario.get(turno.getDiaSemana());
                    return turnoFueraDeHorario(dia.laborable(), dia.apertura(), dia.cierre(),
                            turno.getHoraInicio(), turno.getHoraFin());
                })
                .map(this::describir)
                .toList();
        if (afectados.isEmpty()) {
            return;
        }

        int total = afectados.size();
        String lista = String.join("; ", afectados.subList(0, Math.min(total, MAX_TURNOS_EN_MENSAJE)));
        if (total > MAX_TURNOS_EN_MENSAJE) {
            lista += "; y " + (total - MAX_TURNOS_EN_MENSAJE) + " más";
        }
        throw new ConflictException("No se guardó el horario: " + total
                + (total == 1 ? " turno quedaría" : " turnos quedarían")
                + " fuera del horario de la clínica: " + lista
                + ". Ajusta o borra esos turnos y vuelve a intentarlo.");
    }

    /** "Laura Zamora, Sábado 10:00-18:00". */
    private String describir(HorarioPersonal turno) {
        Personal personal = turno.getPersonal();
        return personal.getNombre() + " " + personal.getApellidos() + ", "
                + HorarioPersonalService.nombreDia(turno.getDiaSemana()) + " "
                + turno.getHoraInicio() + "-" + turno.getHoraFin();
    }

    /** Las horas se manejan en minutos exactos: si llegan segundos, se descartan. */
    private LocalTime sinSegundos(LocalTime hora) {
        return hora.truncatedTo(ChronoUnit.MINUTES);
    }

    private HorarioClinicaResponseDTO mapear(HorarioClinica horario) {
        return HorarioClinicaResponseDTO.builder()
                .diaSemana(horario.getDiaSemana())
                .nombreDia(HorarioPersonalService.nombreDia(horario.getDiaSemana()))
                .horaApertura(horario.getHoraApertura())
                .horaCierre(horario.getHoraCierre())
                .laborable(horario.getLaborable())
                .build();
    }
}
