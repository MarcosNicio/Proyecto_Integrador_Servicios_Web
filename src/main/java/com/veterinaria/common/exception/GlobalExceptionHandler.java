package com.veterinaria.common.exception;

import com.veterinaria.common.security.SecurityErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones de la API.
 * <p>
 * {@code @RestControllerAdvice} le indica a Spring que esta clase atrapa los errores que
 * lanzan <b>todos</b> los controladores. Cada método {@code @ExceptionHandler} se encarga de
 * un tipo de error y lo convierte en un {@link ErrorResponse} con el código HTTP correcto.
 * <p>
 * Así, los servicios y controladores de cada módulo solo lanzan la excepción
 * (por ejemplo {@code throw new ResourceNotFoundException("...")}) sin preocuparse
 * de cómo se responde, y el frontend recibe siempre el mismo formato de error.
 * <p>
 * Resumen de lo que maneja:
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404</li>
 *   <li>{@link BusinessException} → 400</li>
 *   <li>{@link ConflictException} → 409</li>
 *   <li>Validaciones ({@code @Valid}, {@code @NotBlank}, {@code @Email}...) → 400 con errores por campo</li>
 *   <li>JSON mal formado, parámetros faltantes o de tipo incorrecto → 400</li>
 *   <li>Ruta inexistente → 404 / método HTTP no permitido → 405</li>
 *   <li>Datos que la base de datos rechaza (duplicados, llaves foráneas) → 409</li>
 *   <li>Cualquier otro error inesperado → 500 con mensaje genérico (el detalle va al log)</li>
 * </ul>
 * <b>Nota:</b> los errores de seguridad (401 sin token / 403 sin permiso) se agregarán con el módulo auth.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------
    // Excepciones propias del sistema
    // ------------------------------------------------------------------

    /** Registro buscado que no existe. Ej.: mascota con id 999. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** Regla de negocio incumplida. Ej.: cita en fecha pasada. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** Choque con datos existentes. Ej.: correo ya registrado. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ------------------------------------------------------------------
    // Errores en los datos que envía el cliente
    // ------------------------------------------------------------------

    /**
     * Falla la validación del cuerpo JSON de un DTO marcado con {@code @Valid}.
     * Devuelve en {@code errors} el mensaje de cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", request, errors);
    }

    /**
     * Falla la validación de parámetros sueltos (ej. {@code @RequestParam @Min(1) int page})
     * en controladores marcados con {@code @Validated}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.putIfAbsent(field, v.getMessage());
        });
        return build(HttpStatus.BAD_REQUEST, "Datos inválidos", request, errors);
    }

    /** El cuerpo de la petición no es un JSON válido o tiene un valor con formato incorrecto. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido o está mal formado", request, null);
    }

    /** Un parámetro de la URL tiene un tipo incorrecto. Ej.: /api/mascotas/abc cuando se espera un número. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue();
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /** Falta un parámetro obligatorio de la URL. Ej.: /api/citas?fecha= sin enviar la fecha. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "Falta el parámetro obligatorio '" + ex.getParameterName() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    // ------------------------------------------------------------------
    // Errores de ruta / método HTTP
    // ------------------------------------------------------------------

    /** La ruta pedida no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request, null);
    }

    /** La ruta existe pero no acepta ese método. Ej.: DELETE donde solo hay GET. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = "El método " + ex.getMethod() + " no está permitido en esta ruta";
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request, null);
    }

    // ------------------------------------------------------------------
    // Errores de permisos
    // ------------------------------------------------------------------

    /**
     * Un {@code @PreAuthorize} negó el acceso: el usuario está identificado, pero su rol no
     * tiene permiso. Sin este método, el manejador general lo convertiría en un 500.
     * <p>
     * Responde lo mismo que {@code common/security/JwtAccessDeniedHandler}, que cubre las reglas
     * por ruta de {@code SecurityConfig}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, SecurityErrorWriter.MENSAJE_SIN_PERMISO, request, null);
    }

    // ------------------------------------------------------------------
    // Errores de base de datos
    // ------------------------------------------------------------------

    /**
     * La base de datos rechazó la operación: valor duplicado en una columna UNIQUE,
     * llave foránea inexistente, CHECK incumplido, etc.
     * El detalle técnico solo se registra en el log para no exponer la estructura de la BD.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violación de integridad en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operación no se pudo completar porque entra en conflicto con datos existentes",
                request, null);
    }

    // ------------------------------------------------------------------
    // Cualquier otro error
    // ------------------------------------------------------------------

    /**
     * Red de seguridad para errores no previstos (bugs, fallas de conexión, etc.).
     * <p>
     * Primero revisa si es una excepción de Spring que ya trae su propio código HTTP
     * (por ejemplo {@code ResponseStatusException} o un {@code Content-Type} no soportado, 415):
     * en ese caso se respeta ese código en lugar de responder 500.
     * <p>
     * Si no, se responde un 500 con mensaje genérico y el error completo se registra en el log
     * del servidor, porque mostrar el detalle podría revelar información interna del sistema.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
            if (status != null && !status.is5xxServerError()) {
                String detail = springError.getBody().getDetail();
                return build(status, detail != null ? detail : status.getReasonPhrase(), request, null);
            }
        }
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intenta de nuevo más tarde",
                request, null);
    }

    /** Arma la respuesta con el formato único {@link ErrorResponse}. */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, Map<String, String> errors) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors);
        return ResponseEntity.status(status).body(body);
    }
}
