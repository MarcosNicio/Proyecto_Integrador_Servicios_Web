package com.veterinaria.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI / Swagger UI.
 * <p>
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * <br>
 * Especificación JSON: http://localhost:8080/v3/api-docs
 * <p>
 * Define el esquema de autenticación Bearer (JWT) para que, desde el botón
 * "Authorize" de Swagger UI, se pueda enviar el token en el header
 * {@code Authorization: Bearer <token>} a los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI veterinariaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema Integrado de Gestión Veterinaria")
                        .description("Servicios REST para la gestión de citas, expediente clínico, inventario y caja.")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el token JWT obtenido en el login (sin el prefijo 'Bearer').")));
    }
}
