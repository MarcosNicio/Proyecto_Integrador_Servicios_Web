package com.veterinaria.personal.repository;

import com.veterinaria.auth.entity.Rol;
import com.veterinaria.auth.entity.Usuario;
import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.entity.BloqueoDisponibilidad;
import com.veterinaria.personal.entity.HorarioPersonal;
import com.veterinaria.personal.entity.MotivoBloqueo;
import com.veterinaria.personal.entity.Personal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de las entidades del módulo {@code personal} y sus repositorios contra la base real.
 * <p>
 * Que la clase arranque ya confirma que {@link Personal}, {@link HorarioPersonal} y
 * {@link BloqueoDisponibilidad} coinciden con sus tablas ({@code ddl-auto=validate}).
 * Cada prueba se deshace al terminar, así que no deja datos en la base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonalRepositoryTest {

    private static final LocalDateTime LUNES_9 = LocalDateTime.of(2030, 1, 7, 9, 0);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonalRepository personalRepository;

    @Autowired
    private HorarioPersonalRepository horarioRepository;

    @Autowired
    private BloqueoDisponibilidadRepository bloqueoRepository;

    private Personal veterinaria;
    private Personal cajero;

    @BeforeEach
    void crearPersonal() {
        veterinaria = guardarPersonal("vet.repo@veterinaria.com", Rol.VETERINARIO, "Laura", "Zamora", "CED-12345");
        cajero = guardarPersonal("caja.repo@veterinaria.com", Rol.CAJERO, "Pedro", "Alvarez", null);
    }

    // ---------- PERSONAL ----------

    @Test
    void guardarPersonalGeneraIdYQuedaVinculadoASuUsuario() {
        assertThat(veterinaria.getIdPersonal()).isNotNull();

        Personal encontrado = personalRepository.findByUsuarioCorreo("vet.repo@veterinaria.com").orElseThrow();
        assertThat(encontrado.getIdPersonal()).isEqualTo(veterinaria.getIdPersonal());
        assertThat(encontrado.getCedulaProfesional()).isEqualTo("CED-12345");
        assertThat(encontrado.getUsuario().getRol()).isEqualTo(Rol.VETERINARIO);

        Integer idUsuario = encontrado.getUsuario().getIdUsuario();
        assertThat(personalRepository.existsByUsuarioIdUsuario(idUsuario)).isTrue();
        assertThat(personalRepository.findByUsuarioIdUsuario(idUsuario)).isPresent();
    }

    @Test
    void filtraPersonalPorRol() {
        List<Personal> veterinarios = personalRepository.findByUsuarioRolOrderByApellidosAscNombreAsc(Rol.VETERINARIO);
        List<Personal> cajeros = personalRepository.findByUsuarioRolOrderByApellidosAscNombreAsc(Rol.CAJERO);

        assertThat(veterinarios).extracting(Personal::getIdPersonal)
                .contains(veterinaria.getIdPersonal())
                .doesNotContain(cajero.getIdPersonal());
        assertThat(cajeros).extracting(Personal::getIdPersonal)
                .contains(cajero.getIdPersonal())
                .doesNotContain(veterinaria.getIdPersonal());
    }

    @Test
    void listaTodoElPersonalOrdenadoPorApellidos() {
        List<Personal> todos = personalRepository.findAllByOrderByApellidosAscNombreAsc();

        // "Alvarez" va antes que "Zamora"
        List<Integer> ids = todos.stream().map(Personal::getIdPersonal).toList();
        assertThat(ids.indexOf(cajero.getIdPersonal())).isLessThan(ids.indexOf(veterinaria.getIdPersonal()));
    }

    @Test
    void unUsuarioNoPuedeTenerDosPerfilesDePersonal() {
        Personal duplicado = Personal.builder()
                .usuario(veterinaria.getUsuario())
                .nombre("Otra").apellidos("Persona").telefono("5550000000")
                .build();

        assertThatThrownBy(() -> personalRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- HORARIOS_PERSONAL ----------

    @Test
    void guardaYOrdenaLosTurnosPorDiaYHora() {
        guardarHorario(veterinaria, 3, "16:00", "20:00");
        guardarHorario(veterinaria, 1, "08:00", "14:00");
        guardarHorario(veterinaria, 3, "08:00", "13:00");
        guardarHorario(cajero, 1, "09:00", "17:00");

        List<HorarioPersonal> turnos =
                horarioRepository.findByPersonalIdPersonalOrderByDiaSemanaAscHoraInicioAsc(veterinaria.getIdPersonal());

        assertThat(turnos).hasSize(3);
        assertThat(turnos).extracting(HorarioPersonal::getDiaSemana).containsExactly(1, 3, 3);
        assertThat(turnos).extracting(HorarioPersonal::getHoraInicio)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(8, 0), LocalTime.of(16, 0));

        List<HorarioPersonal> miercoles =
                horarioRepository.findByPersonalIdPersonalAndDiaSemanaOrderByHoraInicioAsc(veterinaria.getIdPersonal(), 3);
        assertThat(miercoles).extracting(HorarioPersonal::getHoraInicio)
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(16, 0));
    }

    @Test
    void unTurnoSoloSeEncuentraConLaPersonaALaQuePertenece() {
        HorarioPersonal turno = guardarHorario(veterinaria, 2, "08:00", "14:00");

        assertThat(horarioRepository.findByIdHorarioPersonalAndPersonalIdPersonal(
                turno.getIdHorarioPersonal(), veterinaria.getIdPersonal())).isPresent();
        assertThat(horarioRepository.findByIdHorarioPersonalAndPersonalIdPersonal(
                turno.getIdHorarioPersonal(), cajero.getIdPersonal())).isEmpty();
    }

    @Test
    void laBaseRechazaUnTurnoQueTerminaAntesDeEmpezar() {
        assertThatThrownBy(() -> guardarHorario(veterinaria, 1, "14:00", "08:00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laBaseRechazaUnDiaDeLaSemanaFueraDeRango() {
        assertThatThrownBy(() -> guardarHorario(veterinaria, 7, "08:00", "14:00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- BLOQUEOS_DISPONIBILIDAD ----------

    @Test
    void guardaUnBloqueoConSuMotivo() {
        BloqueoDisponibilidad bloqueo = guardarBloqueo(veterinaria, LUNES_9, LUNES_9.plusHours(3), MotivoBloqueo.CIRUGIA);

        BloqueoDisponibilidad encontrado = bloqueoRepository
                .findByIdBloqueoAndPersonalIdPersonal(bloqueo.getIdBloqueo(), veterinaria.getIdPersonal())
                .orElseThrow();
        assertThat(encontrado.getMotivo()).isEqualTo(MotivoBloqueo.CIRUGIA);
        assertThat(encontrado.getNotas()).isEqualTo("Prueba");
        assertThat(bloqueoRepository.findByIdBloqueoAndPersonalIdPersonal(
                bloqueo.getIdBloqueo(), cajero.getIdPersonal())).isEmpty();
    }

    @Test
    void detectaBloqueosQueSeEnciman() {
        // Bloqueo existente: lunes de 09:00 a 12:00
        guardarBloqueo(veterinaria, LUNES_9, LUNES_9.plusHours(3), MotivoBloqueo.CIRUGIA);
        Integer id = veterinaria.getIdPersonal();

        // Se enciman: empieza dentro, termina dentro, o lo cubre por completo
        assertThat(bloqueoRepository.buscarEncimados(id, LUNES_9.plusHours(2), LUNES_9.plusHours(5))).hasSize(1);
        assertThat(bloqueoRepository.buscarEncimados(id, LUNES_9.minusHours(1), LUNES_9.plusHours(1))).hasSize(1);
        assertThat(bloqueoRepository.buscarEncimados(id, LUNES_9.minusDays(1), LUNES_9.plusDays(1))).hasSize(1);

        // No se enciman: termina justo cuando empieza el otro, o empieza justo cuando el otro termina
        assertThat(bloqueoRepository.buscarEncimados(id, LUNES_9.minusHours(2), LUNES_9)).isEmpty();
        assertThat(bloqueoRepository.buscarEncimados(id, LUNES_9.plusHours(3), LUNES_9.plusHours(5))).isEmpty();

        // Los bloqueos de otra persona no cuentan
        assertThat(bloqueoRepository.buscarEncimados(cajero.getIdPersonal(), LUNES_9, LUNES_9.plusHours(3))).isEmpty();
    }

    @Test
    void separaLosBloqueosQueYaTerminaronDeLosVigentes() {
        guardarBloqueo(veterinaria, LUNES_9.minusDays(10), LUNES_9.minusDays(9), MotivoBloqueo.DIA_LIBRE);
        guardarBloqueo(veterinaria, LUNES_9, LUNES_9.plusDays(5), MotivoBloqueo.PERSONAL);

        assertThat(bloqueoRepository.findByPersonalIdPersonalOrderByFechaInicioAsc(veterinaria.getIdPersonal()))
                .hasSize(2);
        assertThat(bloqueoRepository.findByPersonalIdPersonalAndFechaFinAfterOrderByFechaInicioAsc(
                veterinaria.getIdPersonal(), LUNES_9.minusDays(1)))
                .extracting(BloqueoDisponibilidad::getMotivo)
                .containsExactly(MotivoBloqueo.PERSONAL);
    }

    @Test
    void laBaseRechazaUnBloqueoQueTerminaAntesDeEmpezar() {
        assertThatThrownBy(() -> guardarBloqueo(veterinaria, LUNES_9, LUNES_9.minusHours(1), MotivoBloqueo.PERSONAL))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- Ayudantes ----------

    private Personal guardarPersonal(String correo, Rol rol, String nombre, String apellidos, String cedula) {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .correo(correo)
                .passwordHash("$2a$10$hashDePruebaNoSeUsaParaIniciarSesion000000000000000")
                .rol(rol)
                .build());
        return personalRepository.saveAndFlush(Personal.builder()
                .usuario(usuario)
                .nombre(nombre)
                .apellidos(apellidos)
                .telefono("5551112222")
                .cedulaProfesional(cedula)
                .build());
    }

    private HorarioPersonal guardarHorario(Personal personal, int dia, String inicio, String fin) {
        return horarioRepository.saveAndFlush(HorarioPersonal.builder()
                .personal(personal)
                .diaSemana(dia)
                .horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin))
                .build());
    }

    private BloqueoDisponibilidad guardarBloqueo(Personal personal, LocalDateTime inicio, LocalDateTime fin,
                                                 MotivoBloqueo motivo) {
        return bloqueoRepository.saveAndFlush(BloqueoDisponibilidad.builder()
                .personal(personal)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .motivo(motivo)
                .notas("Prueba")
                .build());
    }
}
