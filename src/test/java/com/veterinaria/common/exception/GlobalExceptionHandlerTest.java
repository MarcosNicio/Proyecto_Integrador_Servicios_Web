package com.veterinaria.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que {@link GlobalExceptionHandler} convierte cada tipo de error en una respuesta
 * con el código HTTP correcto y el formato único {@link ErrorResponse}.
 * <p>
 * Usa un controlador de prueba ({@link ErroresController}) que solo existe durante los tests
 * y cuyos endpoints lanzan a propósito cada tipo de error.
 * <p>
 * {@code @WithMockUser} simula un usuario autenticado: las rutas de prueba son privadas y aquí
 * solo interesa el formato de los errores, no la seguridad (eso lo cubre {@code SecurityConfigTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import(GlobalExceptionHandlerTest.ErroresController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /** DTO de prueba con validaciones, como los que usarán los módulos. */
    record ClienteDePrueba(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @Email(message = "Formato de correo inválido") String correo) {
    }

    /** Controlador de prueba: cada endpoint provoca un tipo de error distinto. */
    @TestConfiguration
    @RestController
    @RequestMapping("/api/test/errores")
    static class ErroresController {

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new ResourceNotFoundException("Mascota no encontrada con id 999");
        }

        @GetMapping("/negocio")
        void negocio() {
            throw new BusinessException("No se puede agendar una cita en una fecha pasada");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictException("El correo ya está registrado");
        }

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody ClienteDePrueba cliente) {
        }

        @GetMapping("/numero/{id}")
        void numero(@PathVariable Integer id) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam String fecha) {
        }

        @GetMapping("/bd")
        void baseDeDatos() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"usuarios_correo_key\"");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe verse");
        }
    }

    /** ResourceNotFoundException → 404 con el mensaje y el formato completo. */
    @Test
    void recursoNoEncontradoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/test/errores/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Mascota no encontrada con id 999"))
                .andExpect(jsonPath("$.path").value("/api/test/errores/no-encontrado"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    /** BusinessException → 400. */
    @Test
    void reglaDeNegocioDevuelve400() throws Exception {
        mockMvc.perform(get("/api/test/errores/negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede agendar una cita en una fecha pasada"));
    }

    /** ConflictException → 409. */
    @Test
    void conflictoDevuelve409() throws Exception {
        mockMvc.perform(get("/api/test/errores/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado"));
    }

    /** DTO inválido → 400 con el mensaje de cada campo en "errors". */
    @Test
    void validacionDevuelve400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/test/errores/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"correo\": \"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.correo").value("Formato de correo inválido"));
    }

    /** JSON mal formado → 400. */
    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/test/errores/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es válido o está mal formado"));
    }

    /** Parámetro con tipo incorrecto (texto donde va un número) → 400. */
    @Test
    void tipoDeParametroIncorrectoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/test/errores/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un valor inválido: abc"));
    }

    /** Falta un parámetro obligatorio → 400. */
    @Test
    void parametroFaltanteDevuelve400() throws Exception {
        mockMvc.perform(get("/api/test/errores/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'fecha'"));
    }

    /** Ruta que no existe → 404. */
    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"));
    }

    /** Método HTTP no permitido en la ruta → 405. */
    @Test
    void metodoNoPermitidoDevuelve405() throws Exception {
        mockMvc.perform(delete("/api/test/errores/negocio"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("El método DELETE no está permitido en esta ruta"));
    }

    /** Error de integridad de la BD → 409 sin revelar el detalle técnico. */
    @Test
    void errorDeBaseDeDatosDevuelve409SinDetalleTecnico() throws Exception {
        mockMvc.perform(get("/api/test/errores/bd"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("La operación no se pudo completar porque entra en conflicto con datos existentes"));
    }

    /** Error inesperado → 500 con mensaje genérico, sin mostrar el detalle interno. */
    @Test
    void errorInesperadoDevuelve500Generico() throws Exception {
        mockMvc.perform(get("/api/test/errores/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Intenta de nuevo más tarde"));
    }
}
