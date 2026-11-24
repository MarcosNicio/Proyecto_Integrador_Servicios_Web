# Sistema Integrado de Gestión Veterinaria

Backend en Spring Boot para la gestión de citas, expediente clínico, inventario y caja de una veterinaria.

## Requisitos

- Java 21
- PostgreSQL 16 o superior
- Git

No es necesario instalar Maven: el proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Crear la base de datos en otra PC

El script con las 24 tablas y los datos iniciales está en [`database/veterinaria_db.sql`](database/veterinaria_db.sql).

### Opción 1: desde la terminal (psql)

1. Abre una terminal en la carpeta raíz del proyecto.

2. Si `psql` no se reconoce como comando, agrégalo al PATH o usa la ruta completa. En Windows suele estar en:

   ```
   C:\Program Files\PostgreSQL\<version>\bin\psql.exe
   ```

3. Crea la base de datos (te pedirá la contraseña del usuario `postgres`):

   ```bash
   psql -U postgres -h localhost -p 5433 -c "CREATE DATABASE veterinaria_db ENCODING 'UTF8';"
   ```

4. Ejecuta el script para crear las tablas y cargar los datos iniciales:

   ```bash
   psql -U postgres -h localhost -p 5433 -d veterinaria_db -f database/veterinaria_db.sql
   ```

   En Windows, si los acentos se ven mal, ejecuta antes en PowerShell:

   ```powershell
   $env:PGCLIENTENCODING = "UTF8"
   ```

> **Puerto:** el proyecto está configurado con el puerto **`5433`** porque la base se creó en PostgreSQL 18, que en la PC de desarrollo convive con PostgreSQL 16 (este último ocupa el `5432`). Cada versión instalada usa un puerto distinto: la primera toma el `5432` (el puerto por defecto), la siguiente el `5433`, etc. Si en tu PC PostgreSQL está en otro puerto (lo normal con una sola versión es `5432`), cambia `5433` por tu puerto en estos comandos y en la configuración del proyecto (ver [Configurar la conexión](#configurar-la-conexión)). Puedes confirmar el puerto en el valor de `port` del archivo `postgresql.conf` de tu versión.

### Opción 2: desde pgAdmin

1. Abre pgAdmin y conéctate a tu servidor.
2. Clic derecho en **Databases** → **Create** → **Database...**
3. Nombre: `veterinaria_db`, Encoding: `UTF8`, y guarda.
4. Selecciona `veterinaria_db` y abre **Tools** → **Query Tool**.
5. Abre el archivo `database/veterinaria_db.sql` (ícono de carpeta) y ejecútalo con **F5**.

### Verificar la instalación

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"
```

El resultado debe ser **24**.

### Volver a crear la base desde cero

El script falla si las tablas ya existen. Para reiniciar la base, bórrala y repite los pasos anteriores:

```bash
psql -U postgres -h localhost -p 5433 -c "DROP DATABASE IF EXISTS veterinaria_db;"
```

>  Esto elimina todos los datos de la base.

## Datos iniciales

El script carga:

- 3 tipos de consultorio (Consulta, Cirugía, Estética)
- 5 categorías de servicio
- Horario de la clínica para los 7 días de la semana
- Usuario administrador para **desarrollo**:

  | Correo | Contraseña |
  |---|---|
  | `admin@veterinaria.com` | `admin123` |

  > Cámbiala antes de usar el sistema en producción.

### Si tu base se creó antes del 23/11/2026

La versión anterior del script traía al admin con un hash de ejemplo cuya contraseña nadie conocía, así que no se podía iniciar sesión. Para no recrear la base, actualiza solo ese dato:

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "UPDATE usuarios SET password_hash = '\$2a\$10\$kYmg.vd6XLls/qcLRqYQaOvINqml8sOgqi3U5gIVWBrrocxTvqv9S' WHERE correo = 'admin@veterinaria.com';"
```

> En pgAdmin (Query Tool) escribe el hash **sin** las diagonales invertidas: `'$2a$10$kYmg.vd6XLls/qcLRqYQaOvINqml8sOgqi3U5gIVWBrrocxTvqv9S'`.

## Configurar la conexión

La conexión está en `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/veterinaria_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:}
```

La contraseña **no** se guarda en el repositorio: se lee de la variable de entorno `DB_PASSWORD`.

Si tu PostgreSQL usa otro puerto (por ejemplo `5432`), tienes dos opciones:

- **Sin tocar el código (recomendado):** define la variable `DB_URL` con tu puerto antes de ejecutar.
- **Cambiando el archivo:** reemplaza `5433` por tu puerto en `application.properties` (no subas ese cambio al repositorio).

Ejemplo en PowerShell:

```powershell
$env:DB_PASSWORD = "tu_contraseña"
$env:DB_URL = "jdbc:postgresql://localhost:5432/veterinaria_db"   # solo si tu puerto no es 5433
```

En Linux / macOS:

```bash
export DB_PASSWORD="tu_contraseña"
export DB_URL="jdbc:postgresql://localhost:5432/veterinaria_db"   # solo si tu puerto no es 5433
```

## Ejecutar el proyecto

Primero define `DB_PASSWORD` (y `DB_URL` si aplica) como se indica arriba, y luego:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

La aplicación se levanta en `http://localhost:8080`.

## Documentación de la API (Swagger UI)

Con la aplicación en ejecución:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Especificación OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Para probar endpoints protegidos:

1. Ejecuta `POST /api/auth/login` (sección **Autenticación**) con `admin@veterinaria.com` / `admin123`.
2. Copia el valor de `token` de la respuesta.
3. Haz clic en **Authorize**, pega el token (sin el prefijo `Bearer`) y acepta. Desde ese momento, Swagger lo envía en cada petición.

## CORS (conexión con el frontend en React)

El navegador bloquea las peticiones de una página a un servidor con distinto origen (protocolo + dominio + puerto). Como el frontend en React corre en otro puerto que el backend (`8080`), la API indica qué orígenes tienen permiso mediante CORS.

La configuración está en `src/main/java/com/veterinaria/common/config/CorsConfig.java` y aplica a todas las rutas `/api/**`:

| Regla | Valor |
|---|---|
| Orígenes permitidos | Los definidos en `app.cors.allowed-origins` |
| Métodos | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Headers | `Authorization` (token JWT), `Content-Type`, `Accept` |
| Credenciales | Permitidas |
| Cache del preflight | 1 hora |

Orígenes permitidos por defecto (`application.properties`):

- `http://localhost:5173` → React con **Vite** (puerto por defecto)
- `http://localhost:3000` → React con **Create React App**

Si el frontend usa otro puerto o se publica en internet, cambia la lista sin tocar el código con la variable de entorno `CORS_ALLOWED_ORIGINS` (separada por comas):

```powershell
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,https://mi-frontend.com"
```

> No uses `*` como origen: permitiría que cualquier sitio web consuma la API.

Si en la consola del navegador aparece `has been blocked by CORS policy`, revisa que la URL exacta del frontend (incluido el puerto) esté en la lista.

## Manejo de errores

Todos los errores de la API se responden con el mismo formato, gracias al manejador global `common/exception/GlobalExceptionHandler.java`:

```json
{
  "timestamp": "2026-11-23T10:15:00",
  "status": 404,
  "error": "Not Found",
  "message": "Mascota no encontrada con id 999",
  "path": "/api/mascotas/999"
}
```

En errores de validación se agrega `errors` con el mensaje de cada campo:

```json
{
  "status": 400,
  "message": "Datos inválidos",
  "errors": {
    "nombre": "El nombre es obligatorio",
    "correo": "Formato de correo inválido"
  }
}
```

### Excepciones disponibles para los módulos

En los servicios basta con lanzar la excepción adecuada; el manejador arma la respuesta:

| Excepción | Código | Cuándo usarla |
|---|---|---|
| `ResourceNotFoundException` | 404 | El registro buscado no existe |
| `BusinessException` | 400 | Se rompe una regla del negocio (ej. cita en fecha pasada) |
| `ConflictException` | 409 | Choca con datos existentes (ej. correo ya registrado) |
| `UnauthorizedException` | 401 | No se pudo comprobar quién es el usuario (ej. contraseña incorrecta en el login) |
| `ForbiddenException` | 403 | Se sabe quién es, pero no puede hacerlo (ej. un cliente que consulta la mascota de otro) |

```java
Mascota mascota = mascotaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con id " + id));
```

### Otros errores que se manejan automáticamente

| Situación | Código |
|---|---|
| DTO con `@Valid` que no cumple sus validaciones | 400 (con `errors`) |
| JSON mal formado, parámetro faltante o de tipo incorrecto | 400 |
| Ruta inexistente | 404 |
| Método HTTP no permitido en la ruta | 405 |
| La base de datos rechaza el dato (duplicado, llave foránea) | 409 |
| Error inesperado | 500 con mensaje genérico (el detalle solo se registra en el log) |

## JWT (tokens de autenticación)

La utilidad `common/util/JwtUtil.java` crea y verifica los tokens JWT que el frontend envía en cada petición (`Authorization: Bearer <token>`). Será usada por el módulo `auth` (login y filtro de seguridad).

| Método | Qué hace |
|---|---|
| `generarToken(correo, rol)` | Crea un token firmado con el correo, el rol y la fecha de caducidad |
| `esTokenValido(token)` | Verifica la firma y que no haya caducado |
| `obtenerCorreo(token)` | Devuelve el correo guardado en el token |
| `obtenerRol(token)` | Devuelve el rol guardado en el token |

### Configuración

| Propiedad | Variable de entorno | Por defecto |
|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | Clave de desarrollo (**cambiar en producción**) |
| `app.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24 horas) |

La clave secreta firma los tokens: quien la conozca puede fabricar tokens válidos, así que en producción debe definirse con `JWT_SECRET` y **nunca subirse al repositorio**. Debe tener al menos 32 caracteres; si es más corta, la aplicación no arranca.

```powershell
$env:JWT_SECRET = "una-clave-larga-y-aleatoria-de-al-menos-32-caracteres"
```

> Los datos del token no van cifrados (cualquiera puede leerlos): nunca se guardan contraseñas ni datos sensibles en él.

## Módulo de autenticación (`auth`)

Gestiona las cuentas de acceso al sistema (tabla `USUARIOS`).

### Entidad y roles

- **`Usuario`** (`auth/entity/Usuario.java`): representa una fila de `USUARIOS` (correo, contraseña cifrada, rol, activo y fecha de creación). La contraseña **nunca** se guarda en texto plano.
- **`Rol`** (`auth/entity/Rol.java`): valores permitidos para el rol de un usuario.

| Rol | Acceso principal |
|---|---|
| `ADMIN` | Personal, configuración de la clínica y catálogos |
| `VETERINARIO` | Consultas, expediente clínico y recetas |
| `CAJERO` | Cobros, pagos y recibos |
| `CLIENTE` | Sus mascotas, citas e información personal |

### Repositorio

`UsuarioRepository` (`auth/repository/UsuarioRepository.java`):

| Método | Uso |
|---|---|
| `findByCorreo(correo)` | Buscar al usuario en el login |
| `existsByCorreo(correo)` | Evitar correos repetidos en el registro |

### Iniciar sesión: `POST /api/auth/login`

Endpoint público (no pide token). Recibe:

```json
{ "correo": "admin@veterinaria.com", "password": "admin123" }
```

Si las credenciales son correctas, responde **200**:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "expiraEnMs": 86400000,
  "correo": "admin@veterinaria.com",
  "rol": "ADMIN"
}
```

El frontend guarda `token` y lo envía en cada petición como `Authorization: Bearer <token>`. Con `rol` decide qué opciones mostrar, y con `expiraEnMs` sabe cuándo vence la sesión (24 horas por defecto).

| Caso | Código | `message` |
|---|---|---|
| Credenciales correctas | **200** | — |
| Falta el correo o la contraseña, o el correo tiene un formato inválido | **400** | `Datos inválidos` (con `errors` por campo) |
| El correo no existe **o** la contraseña está mal | **401** | `Correo o contraseña incorrectos` |
| Credenciales correctas, pero la cuenta está desactivada | **403** | `Tu cuenta está desactivada. Contacta al administrador` |

**Medidas de seguridad** (en `auth/service/AuthService.java`):

- **Mismo mensaje** si el correo no existe o si la contraseña está mal, para no revelar qué correos están registrados.
- **Mismo tiempo de respuesta** en ambos casos: si el correo no existe, se compara la contraseña contra un hash ficticio, porque BCrypt tarda a propósito y una respuesta más rápida delataría lo mismo que el mensaje.
- La cuenta desactivada se revisa **después** de la contraseña, para no revelar ese dato a quien no la conoce.
- El correo se acepta sin importar mayúsculas ni espacios al inicio o al final.

Ejemplo desde PowerShell:

```powershell
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
     -ContentType "application/json" -Body '{"correo":"admin@veterinaria.com","password":"admin123"}'
Invoke-RestMethod http://localhost:8080/api/clientes -Headers @{ Authorization = "Bearer $($r.token)" }
```

### Seguridad (Spring Security)

La configuración está en `common/security/SecurityConfig.java`. Spring Security revisa cada petición **antes** de que llegue a los controladores:

| Regla | Detalle |
|---|---|
| Rutas públicas (sin token) | `/api/auth/**` (login y registro) y Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) |
| Resto de la API | Requiere un token JWT válido |
| Sesiones | No se usan (*stateless*): cada petición trae su token JWT |
| CSRF | Desactivado: solo aplica a sesiones con cookies, y aquí el token va en el header |
| CORS | Activado con `http.cors(...)`, que usa las reglas de `CorsConfig` |

#### Filtro JWT

`common/security/JwtAuthenticationFilter.java` revisa cada petición antes de las reglas anteriores:

1. Lee el header `Authorization: Bearer <token>`.
2. Valida la firma y la caducidad del token con `JwtUtil`.
3. Busca al usuario en `USUARIOS` y comprueba que siga **activo**. Si se desactiva a alguien, pierde el acceso de inmediato, sin esperar a que caduque su token.
4. Registra al usuario con su rol. El rol se toma de la base, no del token, para que un cambio de rol aplique de inmediato.

Si el token falta o falla, el filtro no lanza errores: la petición sigue sin identificar. Si la ruta es pública, funciona normal; si es privada, responde 401.

#### Respuestas 401 y 403

Usan el mismo formato de [Manejo de errores](#manejo-de-errores):

| Código | Significa | Cuándo | `message` |
|---|---|---|---|
| **401** | "No sé quién eres" | No se envió token (o falta el prefijo `Bearer `) | `Se requiere iniciar sesión para acceder a este recurso` |
| **401** | | El token está mal formado, alterado o vencido | `El token es inválido o ha expirado` |
| **401** | | El usuario del token ya no existe o está desactivado | `El usuario no existe o está desactivado` |
| **403** | "Sé quién eres, pero no puedes" | El rol del usuario no tiene permiso | `No tienes permiso para acceder a este recurso` |

```json
{
  "timestamp": "2026-11-23T10:15:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "El token es inválido o ha expirado",
  "path": "/api/clientes"
}
```

> **Para el frontend:** ante un **401**, borra el token guardado y manda al usuario al login. Ante un **403**, el usuario sí inició sesión, pero no tiene acceso a esa función.

#### Proteger endpoints por rol

Cada módulo protege sus endpoints con `@PreAuthorize` (activado con `@EnableMethodSecurity`):

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Integer id) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO')")
@GetMapping("/{id}/expediente")
public ExpedienteDTO expediente(@PathVariable Integer id) { ... }
```

Sin `@PreAuthorize`, el endpoint queda disponible para **cualquier usuario que haya iniciado sesión**, con cualquier rol.

Para saber quién hace la petición (por ejemplo, para que un cliente solo vea sus mascotas), recibe `Authentication` en el método del controlador. `authentication.getName()` devuelve el correo del usuario.

#### Contraseñas con BCrypt

Las contraseñas se cifran con **BCrypt** (bean `PasswordEncoder`) antes de guardarse en `password_hash`:

- **Es de un solo sentido:** del hash no se puede obtener la contraseña original.
- **Usa una "sal" aleatoria:** la misma contraseña genera un hash distinto cada vez.
- **En el login no se descifra nada:** `passwordEncoder.matches(contraseñaEscrita, hashGuardado)` compara la contraseña escrita con el hash guardado.

#### Pruebas con rutas protegidas

En los tests de endpoints privados, `@WithMockUser` simula un usuario autenticado, sin necesidad de un token real:

```java
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser                       // cualquier usuario identificado
class MiControladorTest { ... }

@Test
@WithMockUser(roles = "ADMIN")      // usuario con rol ADMIN
void adminPuedeEliminar() { ... }
```
