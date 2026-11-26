package com.veterinaria.personal.service;

import com.veterinaria.auth.repository.UsuarioRepository;
import com.veterinaria.personal.entity.Personal;
import com.veterinaria.personal.repository.PersonalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que el alta de personal es <b>todo o nada</b>: si falla el guardado del perfil,
 * tampoco queda guardada la cuenta de usuario.
 * <p>
 * A propósito <b>no</b> usa {@code @Transactional}: así la transacción real del alta se
 * confirma o se deshace igual que en producción. {@code PersonalRepository} se reemplaza por
 * un simulacro que siempre falla al guardar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class PersonalAltaTransaccionTest {

    private static final String CORREO = "rollback.personal@veterinaria.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PersonalRepository personalRepository;

    @AfterEach
    void limpiar() {
        // Por si la prueba fallara y el usuario sí quedara guardado: no ensuciar la base.
        usuarioRepository.findByCorreo(CORREO).ifPresent(usuarioRepository::delete);
    }

    @Test
    void siFallaElPerfilNoQuedaGuardadaLaCuenta() throws Exception {
        when(personalRepository.save(any(Personal.class)))
                .thenThrow(new IllegalStateException("Falla simulada al guardar el perfil"));

        mockMvc.perform(post("/api/personal").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "correo": "%s",
                          "password": "Inicial123",
                          "rol": "CAJERO",
                          "nombre": "Prueba",
                          "apellidos": "Rollback",
                          "telefono": "5550000000"
                        }
                        """.formatted(CORREO)))
                .andExpect(status().isInternalServerError());

        assertThat(usuarioRepository.existsByCorreo(CORREO)).isFalse();
    }
}
