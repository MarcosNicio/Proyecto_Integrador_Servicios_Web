package com.veterinaria.clientes.service;

import com.veterinaria.clientes.dto.ClienteRequestDTO;
import com.veterinaria.clientes.dto.ClienteResponseDTO;
import com.veterinaria.clientes.dto.MascotaResponseDTO;
import com.veterinaria.clientes.dto.ResponsableExtraRequestDTO;
import com.veterinaria.clientes.dto.ResponsableExtraResponseDTO;
import com.veterinaria.clientes.entity.Cliente;
import com.veterinaria.clientes.entity.Mascota;
import com.veterinaria.clientes.entity.ResponsableExtra;
import com.veterinaria.clientes.repository.ClienteRepository;
import com.veterinaria.clientes.repository.ResponsableExtraRepository;
import com.veterinaria.common.exception.BusinessException;
import com.veterinaria.common.exception.ConflictException;
import com.veterinaria.common.exception.ForbiddenException;
import com.veterinaria.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClienteService {

    // Prefijo estándar para los folios de negocio de clientes (Opción B)
    private static final String PREFIJO_CODIGO_CLIENTE = "CLI-";

    // Repositorio principal para operaciones sobre la tabla CLIENTES
    private final ClienteRepository clienteRepository;

    // Repositorio para operaciones sobre la tabla RESPONSABLES_EXTRA
    private final ResponsableExtraRepository responsableExtraRepository;

    // Servicio de mascotas reutilizado para mapear mascotas sin duplicar lógica
    private final MascotaService mascotaService;

    // Validador de pertenencia para operaciones realizadas por usuarios con rol CLIENTE
    private final ValidadorAccesoCliente validadorAccesoCliente;

    // =========================================================================
    // MÉTODOS PÚBLICOS DE NEGOCIO - CLIENTES
    // =========================================================================

    @Transactional
    public ClienteResponseDTO crearCliente(ClienteRequestDTO dto) {
        // Verifica que el ID de usuario (si fue proporcionado) no esté vinculado a otro
        // cliente
        validarDisponibilidadUsuario(dto.getIdUsuario(), null);

        // Crea la instancia del cliente y le asigna los datos normalizados
        Cliente nuevoCliente = new Cliente();
        aplicarDatosCliente(nuevoCliente, dto);

        // Persiste el cliente y devuelve el DTO de respuesta con su folio CLI-XXXX
        Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
        return mapearAClienteResponseDTO(clienteGuardado);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorId(Integer idCliente) {
        // Busca el cliente por su llave primaria o lanza excepción 404
        Cliente cliente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(cliente);
        return mapearAClienteResponseDTO(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorCodigo(String codigoCliente) {
        // Convierte el folio de negocio (ej. CLI-0001) a su ID interno y obtiene el
        // cliente
        Integer idCliente = extraerIdDesdeCodigoCliente(codigoCliente);
        return obtenerPorId(idCliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorIdUsuario(Integer idUsuario) {
        // Si es CLIENTE, verifica primero que solo consulte su propio ID de usuario
        validadorAccesoCliente.validarAccesoPorIdUsuario(idUsuario);

        // Busca el perfil de cliente asociado al ID de usuario autenticado
        Cliente cliente = clienteRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un perfil de cliente vinculado al usuario con ID: " + idUsuario));
        return mapearAClienteResponseDTO(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarClientes(String filtro) {
        // Si no hay filtro de búsqueda, retorna todos los clientes registrados
        if (filtro == null || filtro.isBlank()) {
            return mapearListaClientesADTO(clienteRepository.findAll());
        }

        // Si hay filtro, busca coincidencias por folio (CLI-XXXX), nombre/apellidos o
        // teléfono
        String termino = filtro.trim();
        List<Cliente> clientesFiltrados = buscarClientesPorTermino(termino);
        return mapearListaClientesADTO(clientesFiltrados);
    }

    @Transactional
    public ClienteResponseDTO actualizarCliente(Integer idCliente, ClienteRequestDTO dto) {
        // Verifica que el cliente exista y que el usuario autenticado tenga permiso sobre él
        Cliente clienteExistente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(clienteExistente);

        Integer idUsuarioFinal = dto.getIdUsuario();
        if (validadorAccesoCliente.esClienteAutenticado()) {
            // Un CLIENTE no puede reasignar ni desvincular el idUsuario de su propio perfil
            if (dto.getIdUsuario() != null && !dto.getIdUsuario().equals(clienteExistente.getIdUsuario())) {
                throw new ForbiddenException("No tienes permiso para cambiar la cuenta de usuario vinculada");
            }
            idUsuarioFinal = clienteExistente.getIdUsuario();
        } else {
            validarDisponibilidadUsuario(idUsuarioFinal, idCliente);
        }

        // Actualiza los datos utilizando la función auxiliar común
        aplicarDatosCliente(clienteExistente, dto);
        clienteExistente.setIdUsuario(idUsuarioFinal);

        Cliente clienteActualizado = clienteRepository.save(clienteExistente);
        return mapearAClienteResponseDTO(clienteActualizado);
    }

    // MÉTODOS PÚBLICOS DE NEGOCIO - RESPONSABLES EXTRA (CONTACTOS DE EMERGENCIA)
    @Transactional
    public ResponsableExtraResponseDTO agregarResponsableExtra(Integer idCliente, ResponsableExtraRequestDTO dto) {
        // Valida que el cliente titular exista y pertenezca al usuario si es CLIENTE
        Cliente cliente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(cliente);

        // Crea el nuevo contacto y aplica los datos mediante función auxiliar
        // reutilizable
        ResponsableExtra nuevoResponsable = new ResponsableExtra();
        aplicarDatosResponsable(nuevoResponsable, dto, cliente);

        ResponsableExtra responsableGuardado = responsableExtraRepository.save(nuevoResponsable);
        return mapearAResponsableResponseDTO(responsableGuardado);
    }

    @Transactional(readOnly = true)
    public List<ResponsableExtraResponseDTO> listarResponsablesExtra(Integer idCliente) {
        // Valida que el cliente titular exista y pertenezca al usuario si es CLIENTE
        Cliente cliente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(cliente);

        return responsableExtraRepository.findByClienteIdCliente(idCliente).stream()
                .map(this::mapearAResponsableResponseDTO)
                .toList();
    }

    @Transactional
    public ResponsableExtraResponseDTO actualizarResponsableExtra(
            Integer idCliente,
            Integer idResponsable,
            ResponsableExtraRequestDTO dto) {
        // Valida que el cliente titular exista y pertenezca al usuario si es CLIENTE
        Cliente cliente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(cliente);

        // Verifica que el contacto exista y pertenezca al cliente titular
        ResponsableExtra responsableExistente = buscarResponsableDeClienteOError(idCliente, idResponsable);

        // Actualiza los datos del contacto adicional reutilizando la función auxiliar
        aplicarDatosResponsable(responsableExistente, dto, responsableExistente.getCliente());

        ResponsableExtra responsableActualizado = responsableExtraRepository.save(responsableExistente);
        return mapearAResponsableResponseDTO(responsableActualizado);
    }

    @Transactional
    public void eliminarResponsableExtra(Integer idCliente, Integer idResponsable) {
        // Valida que el cliente titular exista y pertenezca al usuario si es CLIENTE
        Cliente cliente = buscarClientePorIdOError(idCliente);
        validadorAccesoCliente.validarAccesoACliente(cliente);

        // Busca el responsable verificando que pertenezca estrictamente al cliente
        // indicado
        ResponsableExtra responsable = buscarResponsableDeClienteOError(idCliente, idResponsable);
        responsableExtraRepository.delete(responsable);
    }

    // FUNCIONES AUXILIARES PRIVADAS REUTILIZABLES

    // Genera el código o folio de negocio legible del cliente (ej. 1 -> CLI-0001)
    private String generarCodigoCliente(Integer idCliente) {
        return (idCliente != null) ? String.format("%s%04d", PREFIJO_CODIGO_CLIENTE, idCliente) : null;
    }

    // Extrae el ID numérico a partir de un folio de cliente (ej. CLI-0001 -> 1)
    private Integer extraerIdDesdeCodigoCliente(String codigoCliente) {
        if (codigoCliente == null || codigoCliente.isBlank()) {
            throw new BusinessException("El código del cliente no puede estar vacío");
        }
        String limpio = codigoCliente.trim().toUpperCase();
        String parteNumerica = limpio.startsWith(PREFIJO_CODIGO_CLIENTE)
                ? limpio.substring(PREFIJO_CODIGO_CLIENTE.length())
                : limpio;
        try {
            return Integer.parseInt(parteNumerica);
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "Formato de código de cliente inválido: '" + codigoCliente + "'. Ejemplo válido: CLI-0001");
        }
    }

    // Busca un cliente por ID o lanza excepción 404 estandarizada
    private Cliente buscarClientePorIdOError(Integer idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));
    }

    // Busca un responsable extra validando que pertenezca al cliente titular o
    // lanza 404
    private ResponsableExtra buscarResponsableDeClienteOError(Integer idCliente, Integer idResponsable) {
        return responsableExtraRepository
                .findByIdResponsableAndClienteIdCliente(idResponsable, idCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el responsable extra con ID " + idResponsable + " para el cliente con ID "
                                + idCliente));
    }

    // Valida que un idUsuario no esté ocupado por un cliente diferente
    private void validarDisponibilidadUsuario(Integer idUsuario, Integer idClienteActual) {
        if (idUsuario == null) {
            return;
        }
        clienteRepository.findByIdUsuario(idUsuario).ifPresent(clienteConUsuario -> {
            if (!clienteConUsuario.getIdCliente().equals(idClienteActual)) {
                throw new ConflictException(
                        "El usuario con ID " + idUsuario + " ya tiene un perfil de cliente asignado");
            }
        });
    }

    // Asigna y limpia los campos del DTO hacia la entidad Cliente
    private void aplicarDatosCliente(Cliente cliente, ClienteRequestDTO dto) {
        cliente.setIdUsuario(dto.getIdUsuario());
        cliente.setNombre(dto.getNombre().trim());
        cliente.setApellidos(dto.getApellidos().trim());
        cliente.setTelefono(dto.getTelefono().trim());
        cliente.setDireccion(limpiarTextoOpcional(dto.getDireccion()));
        cliente.setResponsivaUrl(limpiarTextoOpcional(dto.getResponsivaUrl()));
    }

    // Asigna y limpia los campos del DTO hacia la entidad ResponsableExtra
    private void aplicarDatosResponsable(
            ResponsableExtra responsable,
            ResponsableExtraRequestDTO dto,
            Cliente cliente) {
        responsable.setCliente(cliente);
        responsable.setNombreCompleto(dto.getNombreCompleto().trim());
        responsable.setTelefono(dto.getTelefono().trim());
        responsable.setParentesco(limpiarTextoOpcional(dto.getParentesco()));
    }

    // Unifica la búsqueda por folio (CLI-XXXX), nombre, apellidos y teléfono
    // evitando elementos duplicados
    private List<Cliente> buscarClientesPorTermino(String termino) {
        Map<Integer, Cliente> resultadosUnicos = new LinkedHashMap<>();

        // Si el término empieza con CLI-, busca también por el folio de cliente
        if (termino.toUpperCase().startsWith(PREFIJO_CODIGO_CLIENTE)) {
            try {
                Integer idPorCodigo = extraerIdDesdeCodigoCliente(termino);
                clienteRepository.findById(idPorCodigo)
                        .ifPresent(c -> resultadosUnicos.put(c.getIdCliente(), c));
            } catch (BusinessException ignored) {
                // Si el texto después de CLI- no es numérico, continúa con la búsqueda normal
            }
        }

        clienteRepository.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(termino, termino)
                .forEach(c -> resultadosUnicos.put(c.getIdCliente(), c));

        clienteRepository.findByTelefonoContaining(termino)
                .forEach(c -> resultadosUnicos.put(c.getIdCliente(), c));

        return List.copyOf(resultadosUnicos.values());
    }

    // Convierte una entidad Cliente a ClienteResponseDTO incluyendo su folio
    // CLI-XXXX, responsables y mascotas activas
    private ClienteResponseDTO mapearAClienteResponseDTO(Cliente cliente) {
        List<ResponsableExtraResponseDTO> responsablesDTO = (cliente.getResponsablesExtra() != null)
                ? cliente.getResponsablesExtra().stream()
                        .map(this::mapearAResponsableResponseDTO)
                        .toList()
                : List.of();

        List<MascotaResponseDTO> mascotasActivasDTO = (cliente.getMascotas() != null)
                ? cliente.getMascotas().stream()
                        .filter(Mascota::getActivo)
                        .map(mascotaService::mapearAMascotaResponseDTO)
                        .toList()
                : List.of();

        return ClienteResponseDTO.builder()
                .idCliente(cliente.getIdCliente())
                .codigoCliente(generarCodigoCliente(cliente.getIdCliente()))
                .idUsuario(cliente.getIdUsuario())
                .nombre(cliente.getNombre())
                .apellidos(cliente.getApellidos())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .responsivaUrl(cliente.getResponsivaUrl())
                .responsablesExtra(responsablesDTO)
                .mascotas(mascotasActivasDTO)
                .build();
    }

    // Convierte una lista de entidades Cliente a una lista de DTOs de respuesta
    private List<ClienteResponseDTO> mapearListaClientesADTO(List<Cliente> clientes) {
        return clientes.stream()
                .map(this::mapearAClienteResponseDTO)
                .toList();
    }

    // Convierte una entidad ResponsableExtra a su DTO de respuesta
    private ResponsableExtraResponseDTO mapearAResponsableResponseDTO(ResponsableExtra responsable) {
        return ResponsableExtraResponseDTO.builder()
                .idResponsable(responsable.getIdResponsable())
                .idCliente(responsable.getCliente().getIdCliente())
                .nombreCompleto(responsable.getNombreCompleto())
                .telefono(responsable.getTelefono())
                .parentesco(responsable.getParentesco())
                .build();
    }

    // Limpia espacios de textos opcionales o devuelve null si están vacíos
    private String limpiarTextoOpcional(String texto) {
        return (texto != null && !texto.isBlank()) ? texto.trim() : null;
    }
}
