package com.veterinaria.clientes.service;

import com.veterinaria.clientes.dto.AntecedenteClinicoRequestDTO;
import com.veterinaria.clientes.dto.AntecedenteClinicoResponseDTO;
import com.veterinaria.clientes.dto.MascotaRequestDTO;
import com.veterinaria.clientes.dto.MascotaResponseDTO;
import com.veterinaria.clientes.entity.AntecedenteClinico;
import com.veterinaria.clientes.entity.Cliente;
import com.veterinaria.clientes.entity.Mascota;
import com.veterinaria.clientes.entity.TipoAntecedente;
import com.veterinaria.clientes.repository.AntecedenteClinicoRepository;
import com.veterinaria.clientes.repository.ClienteRepository;
import com.veterinaria.clientes.repository.MascotaRepository;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {

    // Prefijo estándar para los folios clínicos de pacientes (Opción B)
    private static final String PREFIJO_CODIGO_MASCOTA = "PAC-";

    // Repositorio principal de mascotas
    private final MascotaRepository mascotaRepository;

    // Repositorio de antecedentes clínicos de las mascotas
    private final AntecedenteClinicoRepository antecedenteClinicoRepository;

    // Repositorio de clientes para validar la existencia del propietario
    private final ClienteRepository clienteRepository;

    // Validador de pertenencia para operaciones realizadas por usuarios con rol CLIENTE
    private final ValidadorAccesoCliente validadorAccesoCliente;

    // MÉTODOS PÚBLICOS DE NEGOCIO - MASCOTAS

    @Transactional
    public MascotaResponseDTO crearMascota(MascotaRequestDTO dto) {
        // Verifica que el cliente propietario exista y pertenezca al usuario si es CLIENTE
        Cliente propietario = buscarClientePropietarioOError(dto.getIdCliente());
        validadorAccesoCliente.validarAccesoACliente(propietario);

        // Instancia la nueva mascota activa por defecto
        Mascota nuevaMascota = Mascota.builder()
                .activo(true)
                .build();

        // Aplica los datos del DTO a la entidad utilizando la función auxiliar
        aplicarDatosMascota(nuevaMascota, dto, propietario);

        // Guarda en base de datos y retorna el DTO de respuesta con su folio PAC-XXXX
        Mascota mascotaGuardada = mascotaRepository.save(nuevaMascota);
        return mapearAMascotaResponseDTO(mascotaGuardada);
    }

    @Transactional(readOnly = true)
    public MascotaResponseDTO obtenerPorId(Integer idMascota) {
        // Busca la mascota por su ID numérico y valida pertenencia si es CLIENTE
        Mascota mascota = buscarMascotaPorIdOError(idMascota);
        validadorAccesoCliente.validarAccesoAMascota(mascota);
        return mapearAMascotaResponseDTO(mascota);
    }

    @Transactional(readOnly = true)
    public MascotaResponseDTO obtenerPorCodigo(String codigoMascota) {
        // Convierte el folio de negocio (ej. PAC-0001) a su ID interno y obtiene la
        // mascota
        Integer idMascota = extraerIdDesdeCodigoMascota(codigoMascota);
        return obtenerPorId(idMascota);
    }

    @Transactional(readOnly = true)
    public List<MascotaResponseDTO> listarPorCliente(Integer idCliente) {
        // Verifica que el cliente exista y pertenezca al usuario si es CLIENTE
        Cliente propietario = buscarClientePropietarioOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(propietario);

        // Obtiene únicamente las mascotas activas del cliente
        List<Mascota> mascotasActivas = mascotaRepository.findByClienteIdClienteAndActivoTrue(idCliente);
        return mapearListaMascotasADTO(mascotasActivas);
    }

    @Transactional(readOnly = true)
    public List<MascotaResponseDTO> listarMascotasActivas(String nombreOCodigo) {
        // Si no se envía filtro, lista todas las mascotas activas
        if (nombreOCodigo == null || nombreOCodigo.isBlank()) {
            return mapearListaMascotasADTO(mascotaRepository.findByActivoTrue());
        }

        String termino = nombreOCodigo.trim();

        // Si el usuario busca por folio clínico (ej. PAC-0001), resuelve directamente
        // por código
        if (termino.toUpperCase().startsWith(PREFIJO_CODIGO_MASCOTA)) {
            Integer idMascota = extraerIdDesdeCodigoMascota(termino);
            return mascotaRepository.findById(idMascota)
                    .filter(Mascota::getActivo)
                    .map(m -> List.of(mapearAMascotaResponseDTO(m)))
                    .orElse(List.of());
        }

        // De lo contrario, busca coincidencias por nombre de mascota
        List<Mascota> mascotas = mascotaRepository.findByNombreContainingIgnoreCaseAndActivoTrue(termino);
        return mapearListaMascotasADTO(mascotas);
    }

    @Transactional
    public MascotaResponseDTO actualizarMascota(Integer idMascota, MascotaRequestDTO dto) {
        // Obtiene la mascota existente y valida pertenencia si es CLIENTE
        Mascota mascotaExistente = buscarMascotaPorIdOError(idMascota);
        validadorAccesoCliente.validarAccesoAMascota(mascotaExistente);

        // Valida al cliente propietario indicado en el DTO (un CLIENTE no puede transferir su mascota a otro cliente)
        Cliente propietario = buscarClientePropietarioOError(dto.getIdCliente());
        validadorAccesoCliente.validarAccesoACliente(propietario);

        // Actualiza los campos de la mascota mediante la función auxiliar reutilizable
        aplicarDatosMascota(mascotaExistente, dto, propietario);

        Mascota mascotaActualizada = mascotaRepository.save(mascotaExistente);
        return mapearAMascotaResponseDTO(mascotaActualizada);
    }

    @Transactional
    public void desactivarMascota(Integer idMascota) {
        // Aplica baja lógica cambiando el estado activo a false para conservar
        // integridad histórica
        Mascota mascota = buscarMascotaPorIdOError(idMascota);
        mascota.setActivo(false);
        mascotaRepository.save(mascota);
    }

    @Transactional
    public MascotaResponseDTO reactivarMascota(Integer idMascota) {
        // Reactiva una mascota previamente dada de baja lógica (activo = true)
        Mascota mascota = buscarMascotaPorIdOError(idMascota);
        mascota.setActivo(true);
        Mascota mascotaReactivada = mascotaRepository.save(mascota);
        return mapearAMascotaResponseDTO(mascotaReactivada);
    }

    // MÉTODOS PÚBLICOS DE NEGOCIO - ANTECEDENTES CLÍNICOS

    @Transactional
    public AntecedenteClinicoResponseDTO agregarAntecedente(Integer idMascota, AntecedenteClinicoRequestDTO dto) {
        // Verifica que la mascota exista antes de registrar el antecedente
        Mascota mascota = buscarMascotaPorIdOError(idMascota);

        // Crea el nuevo antecedente y aplica los datos usando la función auxiliar común
        AntecedenteClinico nuevoAntecedente = new AntecedenteClinico();
        aplicarDatosAntecedente(nuevoAntecedente, dto, mascota);

        AntecedenteClinico antecedenteGuardado = antecedenteClinicoRepository.save(nuevoAntecedente);
        return mapearAAntecedenteResponseDTO(antecedenteGuardado);
    }

    @Transactional(readOnly = true)
    public List<AntecedenteClinicoResponseDTO> listarAntecedentes(Integer idMascota, TipoAntecedente tipo) {
        // Verifica que la mascota exista y pertenezca al usuario si es CLIENTE
        Mascota mascota = buscarMascotaPorIdOError(idMascota);
        validadorAccesoCliente.validarAccesoAMascota(mascota);

        // Si se proporciona un tipo (ej. ALERGIA), filtra por tipo; si no, trae todo el
        // historial ordenado por fecha
        List<AntecedenteClinico> antecedentes = (tipo != null)
                ? antecedenteClinicoRepository.findByMascotaIdMascotaAndTipo(idMascota, tipo)
                : antecedenteClinicoRepository.findByMascotaIdMascotaOrderByFechaRegistroDesc(idMascota);

        return antecedentes.stream()
                .map(this::mapearAAntecedenteResponseDTO)
                .toList();
    }

    @Transactional
    public AntecedenteClinicoResponseDTO actualizarAntecedente(
            Integer idMascota,
            Integer idAntecedente,
            AntecedenteClinicoRequestDTO dto) {
        // Verifica que el antecedente exista y pertenezca a la mascota indicada
        AntecedenteClinico antecedenteExistente = buscarAntecedenteDeMascotaOError(idMascota, idAntecedente);

        // Actualiza los datos del antecedente reutilizando la función auxiliar
        aplicarDatosAntecedente(antecedenteExistente, dto, antecedenteExistente.getMascota());

        AntecedenteClinico antecedenteActualizado = antecedenteClinicoRepository.save(antecedenteExistente);
        return mapearAAntecedenteResponseDTO(antecedenteActualizado);
    }

    @Transactional
    public void eliminarAntecedente(Integer idMascota, Integer idAntecedente) {
        // Verifica que el antecedente exista y pertenezca estrictamente a la mascota
        // indicada
        AntecedenteClinico antecedente = buscarAntecedenteDeMascotaOError(idMascota, idAntecedente);
        antecedenteClinicoRepository.delete(antecedente);
    }

    // FUNCIONES AUXILIARES REUTILIZABLES

    // Convierte una entidad Mascota a su DTO de respuesta incluyendo su folio
    // clínico (PAC-XXXX)
    public MascotaResponseDTO mapearAMascotaResponseDTO(Mascota mascota) {
        // Mapea la lista de antecedentes clínicos de la mascota
        List<AntecedenteClinicoResponseDTO> antecedentesDTO = (mascota.getAntecedentesClinicos() != null)
                ? mascota.getAntecedentesClinicos().stream()
                        .map(this::mapearAAntecedenteResponseDTO)
                        .toList()
                : List.of();

        return MascotaResponseDTO.builder()
                .idMascota(mascota.getIdMascota())
                .codigoMascota(generarCodigoMascota(mascota.getIdMascota()))
                .idCliente(mascota.getCliente().getIdCliente())
                .nombreCliente(construirNombreCompletoCliente(mascota.getCliente()))
                .nombre(mascota.getNombre())
                .especie(mascota.getEspecie())
                .raza(mascota.getRaza())
                .fechaNacimiento(mascota.getFechaNacimiento())
                .pesoActualKg(mascota.getPesoActualKg())
                .sexo(mascota.getSexo())
                .esterilizado(mascota.getEsterilizado())
                .fotoUrl(mascota.getFotoUrl())
                .activo(mascota.getActivo())
                .antecedentesClinicos(antecedentesDTO)
                .build();
    }

    // Genera el código de negocio legible del paciente (ej. 1 -> PAC-0001)
    private String generarCodigoMascota(Integer idMascota) {
        return (idMascota != null) ? String.format("%s%04d", PREFIJO_CODIGO_MASCOTA, idMascota) : null;
    }

    // Extrae el ID numérico a partir de un folio de mascota (ej. PAC-0001 -> 1)
    private Integer extraerIdDesdeCodigoMascota(String codigoMascota) {
        if (codigoMascota == null || codigoMascota.isBlank()) {
            throw new BusinessException("El código de la mascota no puede estar vacío");
        }
        String limpio = codigoMascota.trim().toUpperCase();
        String parteNumerica = limpio.startsWith(PREFIJO_CODIGO_MASCOTA)
                ? limpio.substring(PREFIJO_CODIGO_MASCOTA.length())
                : limpio;
        try {
            return Integer.parseInt(parteNumerica);
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "Formato de código de mascota inválido: '" + codigoMascota + "'. Ejemplo válido: PAC-0001");
        }
    }

    // Busca una mascota por su ID o lanza excepción 404 estandarizada
    private Mascota buscarMascotaPorIdOError(Integer idMascota) {
        return mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + idMascota));
    }

    // Busca al cliente propietario por su ID o lanza excepción 404 estandarizada
    private Cliente buscarClientePropietarioOError(Integer idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente propietario no encontrado con ID: " + idCliente));
    }

    // Busca un antecedente clínico validando que pertenezca a la mascota indicada o
    // lanza 404
    private AntecedenteClinico buscarAntecedenteDeMascotaOError(Integer idMascota, Integer idAntecedente) {
        return antecedenteClinicoRepository
                .findByIdAntecedenteAndMascotaIdMascota(idAntecedente, idMascota)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el antecedente con ID " + idAntecedente + " para la mascota con ID "
                                + idMascota));
    }

    // Asigna y limpia los campos provenientes del DTO hacia la entidad Mascota
    private void aplicarDatosMascota(Mascota mascota, MascotaRequestDTO dto, Cliente propietario) {
        mascota.setCliente(propietario);
        mascota.setNombre(dto.getNombre().trim());
        mascota.setEspecie(dto.getEspecie().trim());
        mascota.setRaza(limpiarTextoOpcional(dto.getRaza()));
        mascota.setFechaNacimiento(dto.getFechaNacimiento());
        mascota.setPesoActualKg(dto.getPesoActualKg());
        mascota.setSexo(dto.getSexo());
        mascota.setEsterilizado(Boolean.TRUE.equals(dto.getEsterilizado()));
        mascota.setFotoUrl(limpiarTextoOpcional(dto.getFotoUrl()));
    }

    // Asigna y limpia los campos del DTO hacia la entidad AntecedenteClinico
    private void aplicarDatosAntecedente(
            AntecedenteClinico antecedente,
            AntecedenteClinicoRequestDTO dto,
            Mascota mascota) {
        LocalDate fechaRegistro = (dto.getFechaRegistro() != null)
                ? dto.getFechaRegistro()
                : LocalDate.now();
        antecedente.setMascota(mascota);
        antecedente.setTipo(dto.getTipo());
        antecedente.setDescripcion(dto.getDescripcion().trim());
        antecedente.setFechaRegistro(fechaRegistro);
    }

    // Convierte una lista de entidades Mascota a una lista de DTOs de respuesta
    private List<MascotaResponseDTO> mapearListaMascotasADTO(List<Mascota> mascotas) {
        return mascotas.stream()
                .map(this::mapearAMascotaResponseDTO)
                .toList();
    }

    // Convierte una entidad AntecedenteClinico a su DTO de respuesta
    private AntecedenteClinicoResponseDTO mapearAAntecedenteResponseDTO(AntecedenteClinico antecedente) {
        return AntecedenteClinicoResponseDTO.builder()
                .idAntecedente(antecedente.getIdAntecedente())
                .idMascota(antecedente.getMascota().getIdMascota())
                .tipo(antecedente.getTipo())
                .descripcion(antecedente.getDescripcion())
                .fechaRegistro(antecedente.getFechaRegistro())
                .build();
    }

    // Construye el nombre completo del cliente propietario
    private String construirNombreCompletoCliente(Cliente cliente) {
        return cliente.getNombre() + " " + cliente.getApellidos();
    }

    // Limpia espacios en blanco de textos opcionales o devuelve null si están
    // vacíos
    private String limpiarTextoOpcional(String texto) {
        return (texto != null && !texto.isBlank()) ? texto.trim() : null;
    }
}
