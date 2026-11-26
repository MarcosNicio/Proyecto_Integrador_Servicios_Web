package com.veterinaria.agenda.repository;

import com.veterinaria.agenda.entity.Consultorio;
import com.veterinaria.agenda.entity.HorarioClinica;
import com.veterinaria.agenda.entity.TipoConsultorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de las entidades de configuración de la clínica (horario, tipos de consultorio y
 * consultorios) y sus repositorios contra la base real.
 * <p>
 * Que la clase arranque ya confirma que {@link HorarioClinica}, {@link TipoConsultorio} y
 * {@link Consultorio} coinciden con sus tablas ({@code ddl-auto=validate}). Usa los datos que
 * carga el script inicial. Cada prueba se deshace al terminar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConfigClinicaRepositoryTest {

    @Autowired
    private HorarioClinicaRepository horarioRepository;

    @Autowired
    private TipoConsultorioRepository tipoRepository;

    @Autowired
    private ConsultorioRepository consultorioRepository;

    // ---------- HORARIOS_CLINICA ----------

    @Test
    void laSemanaInicialTieneLosSieteDiasEnOrden() {
        List<HorarioClinica> semana = horarioRepository.findAllByOrderByDiaSemanaAsc();

        assertThat(semana).extracting(HorarioClinica::getDiaSemana).containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(semana).allSatisfy(dia -> assertThat(dia.getHoraCierre()).isAfter(dia.getHoraApertura()));
    }

    @Test
    void buscaElHorarioDeUnDia() {
        HorarioClinica domingo = horarioRepository.findByDiaSemana(0).orElseThrow();

        assertThat(domingo.getHoraApertura()).isEqualTo(LocalTime.of(9, 0));
        assertThat(domingo.getHoraCierre()).isEqualTo(LocalTime.of(14, 0));
        assertThat(horarioRepository.findByDiaSemana(7)).isEmpty();
    }

    @Test
    void laBaseRechazaUnDiaRepetido() {
        HorarioClinica otroLunes = HorarioClinica.builder()
                .diaSemana(1).horaApertura(LocalTime.of(9, 0)).horaCierre(LocalTime.of(10, 0)).build();

        assertThatThrownBy(() -> horarioRepository.saveAndFlush(otroLunes))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaUnCierreAntesDeLaApertura() {
        HorarioClinica lunes = horarioRepository.findByDiaSemana(1).orElseThrow();
        lunes.setHoraCierre(LocalTime.of(7, 0));

        assertThatThrownBy(() -> horarioRepository.saveAndFlush(lunes))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unDiaPuedeMarcarseComoNoLaborable() {
        HorarioClinica domingo = horarioRepository.findByDiaSemana(0).orElseThrow();
        domingo.setLaborable(false);
        horarioRepository.saveAndFlush(domingo);

        assertThat(horarioRepository.findByDiaSemana(0).orElseThrow().getLaborable()).isFalse();
    }

    // ---------- TIPOS_CONSULTORIO ----------

    @Test
    void losTiposInicialesExistenYSeBuscanSinImportarMayusculas() {
        assertThat(tipoRepository.findAllByOrderByNombreAsc())
                .extracting(TipoConsultorio::getNombre)
                .contains("Cirugía", "Consulta", "Estética");

        assertThat(tipoRepository.findByNombreIgnoreCase("CONSULTA")).isPresent();
        assertThat(tipoRepository.findByNombreIgnoreCase("Rayos X")).isEmpty();
    }

    @Test
    void separaLosTiposActivosDeLosDesactivados() {
        TipoConsultorio rayos = tipoRepository.saveAndFlush(TipoConsultorio.builder()
                .nombre("Rayos X Prueba").activo(false).build());

        assertThat(tipoRepository.findByActivoTrueOrderByNombreAsc())
                .extracting(TipoConsultorio::getIdTipoConsultorio)
                .doesNotContain(rayos.getIdTipoConsultorio());
        assertThat(tipoRepository.findAllByOrderByNombreAsc())
                .extracting(TipoConsultorio::getIdTipoConsultorio)
                .contains(rayos.getIdTipoConsultorio());
    }

    @Test
    void laBaseRechazaUnNombreDeTipoRepetido() {
        TipoConsultorio repetido = TipoConsultorio.builder().nombre("Consulta").build();

        assertThatThrownBy(() -> tipoRepository.saveAndFlush(repetido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- CONSULTORIOS ----------

    @Test
    void losConsultoriosInicialesTienenSuTipo() {
        Consultorio quirofano = consultorioRepository.findByNombreIgnoreCase("quirófano").orElseThrow();

        assertThat(quirofano.getTipoConsultorio().getNombre()).isEqualTo("Cirugía");
        assertThat(consultorioRepository.findAllByOrderByNombreAsc())
                .extracting(Consultorio::getNombre)
                .contains("Consultorio 1", "Consultorio 2", "Quirófano", "Sala de Estética");
    }

    @Test
    void filtraPorTipoYPorActivos() {
        TipoConsultorio consulta = tipoRepository.findByNombreIgnoreCase("Consulta").orElseThrow();
        Integer idConsulta = consulta.getIdTipoConsultorio();
        Consultorio cerrado = consultorioRepository.saveAndFlush(Consultorio.builder()
                .tipoConsultorio(consulta).nombre("Consultorio Prueba Cerrado").activo(false).build());

        List<Consultorio> deConsulta = consultorioRepository.findByTipoConsultorioIdTipoConsultorioOrderByNombreAsc(idConsulta);
        List<Consultorio> activosDeConsulta =
                consultorioRepository.findByTipoConsultorioIdTipoConsultorioAndActivoTrueOrderByNombreAsc(idConsulta);

        assertThat(deConsulta).extracting(Consultorio::getNombre)
                .contains("Consultorio 1", "Consultorio 2", "Consultorio Prueba Cerrado")
                .doesNotContain("Quirófano");
        assertThat(activosDeConsulta).extracting(Consultorio::getIdConsultorio)
                .doesNotContain(cerrado.getIdConsultorio());
        assertThat(consultorioRepository.findByActivoTrueOrderByNombreAsc()).extracting(Consultorio::getIdConsultorio)
                .doesNotContain(cerrado.getIdConsultorio());
    }

    @Test
    void cuentaLosConsultoriosActivosDeUnTipo() {
        TipoConsultorio cirugia = tipoRepository.findByNombreIgnoreCase("Cirugía").orElseThrow();
        Integer id = cirugia.getIdTipoConsultorio();
        long antes = consultorioRepository.countByTipoConsultorioIdTipoConsultorioAndActivoTrue(id);

        consultorioRepository.saveAndFlush(Consultorio.builder().tipoConsultorio(cirugia).nombre("Quirófano 2 Prueba").build());
        consultorioRepository.saveAndFlush(Consultorio.builder().tipoConsultorio(cirugia).nombre("Quirófano 3 Prueba")
                .activo(false).build());

        assertThat(antes).isGreaterThanOrEqualTo(1);
        assertThat(consultorioRepository.countByTipoConsultorioIdTipoConsultorioAndActivoTrue(id)).isEqualTo(antes + 1);
    }

    @Test
    void laBaseRechazaUnConsultorioSinTipo() {
        Consultorio sinTipo = Consultorio.builder().nombre("Sin tipo").build();

        assertThatThrownBy(() -> consultorioRepository.saveAndFlush(sinTipo))
                .isInstanceOf(Exception.class);
    }
}
