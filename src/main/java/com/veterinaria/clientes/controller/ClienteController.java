package com.veterinaria.clientes.controller;

import com.veterinaria.clientes.dto.ClienteRequestDTO;
import com.veterinaria.clientes.dto.ClienteResponseDTO;
import com.veterinaria.clientes.dto.ResponsableExtraRequestDTO;
import com.veterinaria.clientes.dto.ResponsableExtraResponseDTO;
import com.veterinaria.clientes.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de perfiles de clientes y contactos adicionales de emergencia")
public class ClienteController {

    // Servicio de lógica de negocio para clientes y responsables extra
    private final ClienteService clienteService;

    // ENDPOINTS DE CLIENTES

    //Registrar un nuevo cliente
    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el perfil de un cliente y genera su folio de negocio (ej. CLI-0001)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "El ID de usuario ya está vinculado a otro cliente")
    })
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        // Registra el nuevo cliente y responde con código HTTP 201 Created
        ClienteResponseDTO respuesta = clienteService.crearCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    //Listar o buscar clientes
    @GetMapping
    @Operation(summary = "Listar o buscar clientes", description = "Lista todos los clientes o filtra por folio (CLI-XXXX), nombre, apellidos o teléfono")
    @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida correctamente")
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes(
            @Parameter(description = "Término opcional de búsqueda (folio CLI-0001, nombre, apellido o teléfono)", example = "Gómez")
            @RequestParam(required = false) String filtro) {
        // Obtiene la lista de clientes aplicando el filtro si fue proporcionado
        return ResponseEntity.ok(clienteService.listarClientes(filtro));
    }

    //Obtener cliente por ID
    @GetMapping("/{idCliente}")
    @Operation(summary = "Obtener cliente por ID", description = "Consulta el detalle de un cliente, sus contactos de emergencia y mascotas activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(
            @Parameter(description = "Identificador numérico del cliente", example = "1")
            @PathVariable Integer idCliente) {
        // Busca el cliente por su ID numérico primario
        return ResponseEntity.ok(clienteService.obtenerPorId(idCliente));
    }

    //Obtener cliente por folio de negocio
    @GetMapping("/codigo/{codigoCliente}")
    @Operation(summary = "Obtener cliente por folio de negocio", description = "Consulta el perfil de un cliente mediante su folio legible (ej. CLI-0001)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "400", description = "Formato de folio inválido"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ClienteResponseDTO> obtenerPorCodigo(
            @Parameter(description = "Folio de negocio del cliente", example = "CLI-0001")
            @PathVariable String codigoCliente) {
        // Busca el cliente a partir de su folio CLI-XXXX
        return ResponseEntity.ok(clienteService.obtenerPorCodigo(codigoCliente));
    }

    //Obtener cliente por ID de usuario
    @GetMapping("/usuario/{idUsuario}")
    @Operation(summary = "Obtener cliente por ID de usuario", description = "Obtiene el perfil del cliente vinculado a una cuenta de usuario del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil de cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe perfil de cliente para el usuario indicado")
    })
    public ResponseEntity<ClienteResponseDTO> obtenerPorIdUsuario(
            @Parameter(description = "Identificador de la cuenta de usuario", example = "1")
            @PathVariable Integer idUsuario) {
        // Obtiene el perfil asociado al usuario autenticado
        return ResponseEntity.ok(clienteService.obtenerPorIdUsuario(idUsuario));
    }

    //Actualizar datos de un cliente
    @PutMapping("/{idCliente}")
    @Operation(summary = "Actualizar datos de un cliente", description = "Modifica la información de contacto, dirección o carta responsiva de un cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "409", description = "El ID de usuario ya pertenece a otro cliente")
    })
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @Parameter(description = "Identificador numérico del cliente", example = "1")
            @PathVariable Integer idCliente,
            @Valid @RequestBody ClienteRequestDTO dto) {
        // Actualiza los datos del cliente y devuelve el perfil modificado
        return ResponseEntity.ok(clienteService.actualizarCliente(idCliente, dto));
    }

    // ENDPOINTS DE RESPONSABLES EXTRA (CONTACTOS DE EMERGENCIA)

    //Registrar un responsable extra
    @PostMapping("/{idCliente}/responsables")
    @Operation(summary = "Agregar contacto de emergencia", description = "Registra un responsable adicional o contacto de emergencia asociado al cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contacto registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos del contacto inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente titular no encontrado")
    })
    public ResponseEntity<ResponsableExtraResponseDTO> agregarResponsableExtra(
            @Parameter(description = "Identificador del cliente titular", example = "1")
            @PathVariable Integer idCliente,
            @Valid @RequestBody ResponsableExtraRequestDTO dto) {
        // Asocia un nuevo contacto adicional al cliente titular
        ResponsableExtraResponseDTO respuesta = clienteService.agregarResponsableExtra(idCliente, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    //Listar responsables extra
    @GetMapping("/{idCliente}/responsables")
    @Operation(summary = "Listar contactos de emergencia del cliente", description = "Obtiene todos los responsables adicionales registrados para un cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de contactos obtenida"),
            @ApiResponse(responseCode = "404", description = "Cliente titular no encontrado")
    })
    public ResponseEntity<List<ResponsableExtraResponseDTO>> listarResponsablesExtra(
            @Parameter(description = "Identificador del cliente titular", example = "1")
            @PathVariable Integer idCliente) {
        // Devuelve los contactos adicionales registrados del cliente
        return ResponseEntity.ok(clienteService.listarResponsablesExtra(idCliente));
    }

    //Actualizar contacto de emergencia
    @PutMapping("/{idCliente}/responsables/{idResponsable}")
    @Operation(summary = "Actualizar contacto de emergencia", description = "Modifica el nombre, teléfono o parentesco de un contacto adicional del cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacto actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Contacto o cliente no encontrado")
    })
    public ResponseEntity<ResponsableExtraResponseDTO> actualizarResponsableExtra(
            @Parameter(description = "Identificador del cliente titular", example = "1")
            @PathVariable Integer idCliente,
            @Parameter(description = "Identificador del responsable extra", example = "1")
            @PathVariable Integer idResponsable,
            @Valid @RequestBody ResponsableExtraRequestDTO dto) {
        // Actualiza los datos del responsable extra validando pertenencia al cliente
        return ResponseEntity.ok(clienteService.actualizarResponsableExtra(idCliente, idResponsable, dto));
    }

    //Eliminar contacto de emergencia
    @DeleteMapping("/{idCliente}/responsables/{idResponsable}")
    @Operation(summary = "Eliminar contacto de emergencia", description = "Elimina un contacto adicional asociado al cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contacto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Contacto o cliente no encontrado")
    })
    public ResponseEntity<Void> eliminarResponsableExtra(
            @Parameter(description = "Identificador del cliente titular", example = "1")
            @PathVariable Integer idCliente,
            @Parameter(description = "Identificador del responsable extra", example = "1")
            @PathVariable Integer idResponsable) {
        // Elimina el contacto adicional y responde 204 No Content
        clienteService.eliminarResponsableExtra(idCliente, idResponsable);
        return ResponseEntity.noContent().build();
    }
}
