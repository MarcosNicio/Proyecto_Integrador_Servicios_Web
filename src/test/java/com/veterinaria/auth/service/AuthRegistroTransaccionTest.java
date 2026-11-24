package com.veterinaria.auth.service;

import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.clientes.dto.ClienteRequestDTO;
import com.veterinaria.clientes.service.ClienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que el registro es <b>todo o nada</b>: si falla la creación del perfil de cliente,
 * tampoco queda guardado el usuario.
 * <p>
 * A propósito <b>no</b> usa {@code @Transactional}: así la transacción real del registro se
 * confirma o se deshace igual que en producción. {@code ClienteService} se reemplaza por un
 * simulacro que siempre falla.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistroTransaccionTest {

    private static final String CORREO = "rollback.registro@correo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ClienteService clienteService;

    @AfterEach
    void limpiar() {
        // Por si la prueba fallara y el usuario sí quedara guardado: no ensuciar la base.
        usuarioRepository.findByCorreo(CORREO).ifPresent(usuarioRepository::delete);
    }

    @Test
    void siFallaLaCreacionDelClienteNoQuedaGuardadoElUsuario() throws Exception {
        when(clienteService.crearCliente(any(ClienteRequestDTO.class)))
                .thenThrow(new IllegalStateException("Falla simulada al crear el cliente"));

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "correo": "%s",
                          "password": "MiClave123",
                          "nombre": "Prueba",
                          "apellidos": "Rollback",
                          "telefono": "5550000000"
                        }
                        """.formatted(CORREO)))
                .andExpect(status().isInternalServerError());

        assertThat(usuarioRepository.existsByCorreo(CORREO)).isFalse();
    }
}
