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

### Actualizar una base existente (migraciones)

Cuando el script cambia, no hace falta borrar la base: en `database/migraciones/` hay un archivo por cada cambio, que actualiza una base ya creada **sin borrar datos**. Una base creada desde cero con el script actual ya incluye todas las migraciones.

| Migración | Qué agrega | ¿Quién la corre? |
|---|---|---|
| `001_catalogos_checks_indices.sql` | Catálogo de consultorios, validaciones `CHECK` e índices | Quien haya creado su base antes del 23/11/2026 |

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -f database/migraciones/001_catalogos_checks_indices.sql
```

> Cada migración se corre **una sola vez**. Es todo o nada: si falla, no aplica ningún cambio. Si dice que una restricción "ya existe", esa base ya estaba actualizada.

### Validaciones e índices

Además de las llaves y los `UNIQUE`, la base rechaza datos imposibles aunque el backend no los valide. Por ejemplo:

| Validación | Ejemplo de lo que se rechaza |
|---|---|
| Rangos de horas y fechas | Una cita o un horario que termina antes de empezar |
| Precios, montos y costos | Valores negativos; pagos de 0 |
| Duraciones, pesos y cantidades | Un servicio de 0 minutos, una tarifa con peso máximo menor al mínimo, una venta de 0 piezas |
| Stock | Stock negativo |
| Renglones de una orden de cobro | Un renglón de tipo `SERVICIO` sin servicio, o de producto sin producto |

Si se rompe una validación, PostgreSQL rechaza el dato y la API responde **409** con el formato de [Manejo de errores](#manejo-de-errores). Aun así, cada módulo debe validar sus DTOs para devolver un mensaje claro (400).

También hay **23 índices** en las columnas que más se consultan (las mascotas de un cliente, las citas de un veterinario en un día, las notificaciones pendientes, etc.), porque PostgreSQL no indexa por sí solo las llaves foráneas. Todos se llaman `idx_*`, y las validaciones `chk_*`.

## Datos iniciales

El script carga:

- 3 tipos de consultorio (Consulta, Cirugía, Estética)
- 4 consultorios: Consultorio 1, Consultorio 2, Quirófano y Sala de Estética
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

## Pruebas

Hay dos tipos: las **automáticas** (JUnit, se corren con Maven y comprueban todo en unos segundos) y las **manuales** (con la aplicación encendida, desde Swagger o PowerShell).

### Pruebas automáticas

**Requisitos:** la base `veterinaria_db` creada (con sus [migraciones](#actualizar-una-base-existente-migraciones) al día) y PostgreSQL encendido. Las pruebas usan la base real porque Hibernate valida que las entidades coincidan con las tablas.

```powershell
# Windows (PowerShell)
$env:DB_PASSWORD = "tu_contraseña"

.\mvnw.cmd test                                                            # todas
.\mvnw.cmd test "-Dtest=AuthRegistroTest"                                  # una clase
.\mvnw.cmd test "-Dtest=AuthControllerTest,AuthRegistroTest"               # varias clases
.\mvnw.cmd test "-Dtest=AuthRegistroTest#correoRepetidoDevuelve409AunqueCambienLasMayusculas"   # una sola prueba
```

```bash
# Linux / macOS
export DB_PASSWORD="tu_contraseña"
./mvnw test
./mvnw test -Dtest=AuthRegistroTest
```

> En PowerShell, `-Dtest=...` va **entre comillas**; sin ellas, PowerShell corta el parámetro.

Si todo sale bien, al final aparece:

```
[INFO] Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**No dejan basura en la base:** las pruebas que crean usuarios o clientes deshacen sus cambios al terminar. La única consecuencia visible es que los folios (`CLI-XXXX`) pueden saltarse números.

#### Qué prueba cada clase

| Clase | Pruebas | Qué comprueba |
|---|---|---|
| `VeterinariaApplicationTests` | 1 | La aplicación arranca y todas las entidades coinciden con la base |
| `common.config.CorsConfigTest` | 3 | El frontend en React (5173 / 3000) puede consumir la API y otros orígenes no |
| `common.config.OpenApiConfigTest` | 2 | Swagger UI y la especificación OpenAPI responden |
| `common.exception.GlobalExceptionHandlerTest` | 11 | Cada tipo de error responde con su código y el formato `ErrorResponse` |
| `common.util.JwtUtilTest` | 6 | Los tokens se generan, se validan y se rechazan si están alterados o vencidos |
| `common.security.SecurityConfigTest` | 6 | Rutas públicas y privadas, Swagger público y cifrado BCrypt |
| `common.security.JwtAuthenticationFilterTest` | 13 | El filtro JWT: token válido, ausente, inventado, falsificado o vencido, usuario desactivado, roles (401/403) |
| `auth.repository.UsuarioRepositoryTest` | 3 | El repositorio de usuarios contra la base |
| `auth.controller.AuthControllerTest` | 10 | Login: credenciales correctas e incorrectas, cuenta desactivada, validaciones y uso del token |
| `auth.controller.AuthRegistroTest` | 13 | Registro: crea usuario y cliente vinculados, contraseña cifrada, token, login posterior, correo repetido y contraseñas débiles |
| `auth.service.AuthRegistroTransaccionTest` | 1 | Si falla la creación del cliente, tampoco se guarda el usuario |
| `personal.repository.PersonalRepositoryTest` | 12 | Entidades de personal, turnos y bloqueos contra la base: vínculo con el usuario, filtros por rol, orden, bloqueos encimados y reglas de la base |

#### Si algo falla

| Error | Causa probable |
|---|---|
| `Connection refused` / `Connection to localhost:5433 refused` | PostgreSQL apagado o en otro puerto: define `DB_URL` (ver [Configurar la conexión](#configurar-la-conexión)) |
| `password authentication failed for user "postgres"` | `DB_PASSWORD` no está definida o es incorrecta |
| `Schema-validation: missing table` / `missing column` | La base no está al día: corre las migraciones o vuelve a crear la base |
| Errores raros después de cambiar de rama | Quedaron clases viejas compiladas: usa `.\mvnw.cmd clean test` |

### Pruebas manuales

Arranca la aplicación (`.\mvnw.cmd spring-boot:run`) y deja esa terminal abierta.

#### Desde Swagger

1. Abre http://localhost:8080/swagger-ui.html
2. Sección **Autenticación** → `POST /api/auth/login` → **Try it out** → cuerpo `{"correo": "admin@veterinaria.com", "password": "admin123"}` → **Execute**. Copia el `token` de la respuesta.
3. Botón **Authorize** (arriba a la derecha) → pega el token sin la palabra `Bearer` → **Authorize**.
4. Prueba cualquier endpoint, por ejemplo `GET /api/clientes`: debe responder **200**. Sin el paso 3 responde **401**.
5. Para el registro: `POST /api/auth/registro` con los datos de ejemplo que muestra Swagger. Debe responder **201** con el token y el folio del cliente.

#### Desde PowerShell

En **otra** terminal de PowerShell, pega primero esta función. Muestra el código HTTP y la respuesta de cada petición, aunque sea un error:

```powershell
$api = "http://localhost:8080/api"
function Invoke-Api($Metodo, $Ruta, $Cuerpo = $null, $Token = $null) {
    $p = @{ Method = $Metodo; Uri = "$api$Ruta"; ContentType = "application/json"; UseBasicParsing = $true }
    if ($Cuerpo) { $p.Body = $Cuerpo | ConvertTo-Json }
    if ($Token)  { $p.Headers = @{ Authorization = "Bearer $Token" } }
    try   { $r = Invoke-WebRequest @p; [pscustomobject]@{ Codigo = [int]$r.StatusCode; Respuesta = $r.Content | ConvertFrom-Json } }
    catch { [pscustomobject]@{ Codigo = [int]$_.Exception.Response.StatusCode; Respuesta = $_.ErrorDetails.Message | ConvertFrom-Json } }
}
```

Luego prueba cada caso:

```powershell
# 1. Login del admin → 200
$login = Invoke-Api POST "/auth/login" @{ correo = "admin@veterinaria.com"; password = "admin123" }
$login.Codigo
$token = $login.Respuesta.token

# 2. Ruta privada sin token → 401, con token → 200
(Invoke-Api GET "/clientes").Codigo
(Invoke-Api GET "/clientes" -Token $token).Codigo

# 3. Contraseña incorrecta → 401 "Correo o contraseña incorrectos"
(Invoke-Api POST "/auth/login" @{ correo = "admin@veterinaria.com"; password = "mala" }).Respuesta.message

# 4. Registro de un cliente → 201 con folio CLI-XXXX
$cliente = @{ correo = "prueba.manual@correo.com"; password = "MiClave123"; nombre = "Prueba"; apellidos = "Manual"; telefono = "5550001111" }
$reg = Invoke-Api POST "/auth/registro" $cliente
$reg.Codigo
$reg.Respuesta.cliente.codigoCliente

# 5. El mismo correo otra vez → 409 "El correo ya está registrado"
(Invoke-Api POST "/auth/registro" $cliente).Respuesta.message

# 6. Contraseña débil → 400 con el error del campo
$debil = $cliente.Clone(); $debil.correo = "otro@correo.com"; $debil.password = "Abc1"
(Invoke-Api POST "/auth/registro" $debil).Respuesta.errors

# 7. El cliente registrado inicia sesión → 200 con rol CLIENTE
(Invoke-Api POST "/auth/login" @{ correo = "prueba.manual@correo.com"; password = "MiClave123" }).Respuesta.rol
```

| Caso | Resultado esperado |
|---|---|
| 1. Login del admin | `200` |
| 2. Ruta privada | `401` sin token, `200` con token |
| 3. Contraseña incorrecta | `Correo o contraseña incorrectos` |
| 4. Registro | `201` y un folio como `CLI-0001` |
| 5. Correo repetido | `El correo ya está registrado` |
| 6. Contraseña débil | `password : La contraseña debe tener entre 8 y 72 caracteres` |
| 7. Login del cliente nuevo | `CLIENTE` |

#### Roles y pertenencia (módulo de clientes)

Comprueba los [permisos del módulo de clientes](#permisos-del-módulo-de-clientes): un CLIENTE solo puede ver y modificar **lo suyo**, y cada rol del personal solo lo que le corresponde. Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1.

Primero crea a los participantes: dos clientes (Ana y Beto), una mascota para cada uno y un cajero.

```powershell
# Dos clientes registrados → cada uno con su token y su perfil
$ana  = (Invoke-Api POST "/auth/registro" @{ correo = "ana.roles@correo.com";  password = "MiClave123"; nombre = "Ana";  apellidos = "Roles"; telefono = "5550002222" }).Respuesta
$beto = (Invoke-Api POST "/auth/registro" @{ correo = "beto.roles@correo.com"; password = "MiClave123"; nombre = "Beto"; apellidos = "Roles"; telefono = "5550003333" }).Respuesta
$tAna = $ana.token
$idAna = $ana.cliente.idCliente;  $idBeto = $beto.cliente.idCliente

# Una mascota para cada uno (Ana registra la suya, el admin la de Beto)
$mAna  = (Invoke-Api POST "/mascotas" @{ idCliente = $idAna;  nombre = "Firulais"; especie = "Perro"; sexo = "MACHO" } $tAna).Respuesta.idMascota
$mBeto = (Invoke-Api POST "/mascotas" @{ idCliente = $idBeto; nombre = "Michi";    especie = "Gato";  sexo = "HEMBRA" } $token).Respuesta.idMascota
```

El registro público solo crea clientes. El cajero se crea directo en la base, con la misma contraseña que el admin (`admin123`). Esta línea va en la terminal de **psql**, no en PowerShell:

```sql
INSERT INTO usuarios (correo, password_hash, rol, activo)
SELECT 'cajero.roles@correo.com', password_hash, 'CAJERO', true FROM usuarios WHERE correo = 'admin@veterinaria.com';
```

```powershell
$tCajero = (Invoke-Api POST "/auth/login" @{ correo = "cajero.roles@correo.com"; password = "admin123" }).Respuesta.token
```

Ahora prueba cada caso. Todos muestran solo el código HTTP:

```powershell
# 8. Ana consulta lo suyo → 200
(Invoke-Api GET "/clientes/$idAna" -Token $tAna).Codigo
(Invoke-Api GET "/clientes/usuario/$($ana.cliente.idUsuario)" -Token $tAna).Codigo
(Invoke-Api GET "/mascotas/$mAna" -Token $tAna).Codigo
(Invoke-Api GET "/mascotas/$mAna/antecedentes" -Token $tAna).Codigo

# 9. Ana intenta ver o modificar lo de Beto → 403
(Invoke-Api GET "/clientes/$idBeto" -Token $tAna).Codigo
(Invoke-Api GET "/clientes/codigo/$($beto.cliente.codigoCliente)" -Token $tAna).Codigo
(Invoke-Api PUT "/clientes/$idBeto" @{ nombre = "X"; apellidos = "X"; telefono = "5550000000" } $tAna).Codigo
(Invoke-Api GET "/mascotas/$mBeto" -Token $tAna).Codigo
(Invoke-Api GET "/mascotas/cliente/$idBeto" -Token $tAna).Codigo

# 10. Ana intenta pasarle su mascota a Beto → 403
(Invoke-Api PUT "/mascotas/$mAna" @{ idCliente = $idBeto; nombre = "Firulais"; especie = "Perro"; sexo = "MACHO" } $tAna).Codigo

# 11. Ana intenta quedarse con la cuenta de Beto (cambiando el idUsuario de su perfil) → 403
(Invoke-Api PUT "/clientes/$idAna" @{ idUsuario = $beto.cliente.idUsuario; nombre = "Ana"; apellidos = "Roles"; telefono = "5550002222" } $tAna).Codigo

# 12. Ana en funciones del personal → 403
(Invoke-Api GET "/clientes" -Token $tAna).Codigo
(Invoke-Api PATCH "/mascotas/$mAna/desactivar" -Token $tAna).Codigo
(Invoke-Api POST "/mascotas/$mAna/antecedentes" @{ tipo = "ALERGIA"; descripcion = "Polen" } $tAna).Codigo

# 13. El cajero ve a cualquier cliente → 200, pero no toca lo clínico → 403
(Invoke-Api GET "/clientes" -Token $tCajero).Codigo
(Invoke-Api GET "/clientes/$idBeto" -Token $tCajero).Codigo
(Invoke-Api POST "/mascotas/$mBeto/antecedentes" @{ tipo = "ALERGIA"; descripcion = "Polen" } $tCajero).Codigo
(Invoke-Api PATCH "/mascotas/$mBeto/desactivar" -Token $tCajero).Codigo

# 14. El admin registra el antecedente → 201
(Invoke-Api POST "/mascotas/$mBeto/antecedentes" @{ tipo = "ALERGIA"; descripcion = "Penicilina" } $token).Codigo

# 15. Mensaje del 403
(Invoke-Api GET "/clientes/$idBeto" -Token $tAna).Respuesta.message
```

| Caso | Resultado esperado |
|---|---|
| 8. Ana consulta lo suyo | `200` en las 4 |
| 9. Ana con lo de Beto | `403` en las 5 |
| 10. Pasar su mascota a otro cliente | `403` |
| 11. Cambiar la cuenta vinculada a su perfil | `403` |
| 12. Ana en funciones del personal | `403` en las 3 |
| 13. Cajero | `200`, `200`, `403`, `403` |
| 14. Admin crea un antecedente | `201` |
| 15. Mensaje | `No tienes permiso para acceder o modificar la información de otro cliente` |

#### Borrar las cuentas de prueba

Las pruebas manuales **sí guardan datos**. Para borrar una cuenta de prueba (primero su perfil de cliente, después el usuario):

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM clientes WHERE id_usuario = (SELECT id_usuario FROM usuarios WHERE correo = 'prueba.manual@correo.com'); DELETE FROM usuarios WHERE correo = 'prueba.manual@correo.com';"
```

Para las pruebas de roles (Ana, Beto y el cajero) hay que borrar también sus mascotas y antecedentes, en este orden:

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM antecedentes_clinicos WHERE id_mascota IN (SELECT m.id_mascota FROM mascotas m JOIN clientes c ON c.id_cliente = m.id_cliente JOIN usuarios u ON u.id_usuario = c.id_usuario WHERE u.correo LIKE '%.roles@correo.com'); DELETE FROM mascotas WHERE id_cliente IN (SELECT c.id_cliente FROM clientes c JOIN usuarios u ON u.id_usuario = c.id_usuario WHERE u.correo LIKE '%.roles@correo.com'); DELETE FROM clientes WHERE id_usuario IN (SELECT id_usuario FROM usuarios WHERE correo LIKE '%.roles@correo.com'); DELETE FROM usuarios WHERE correo LIKE '%.roles@correo.com';"
```

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

Gestiona las cuentas de acceso al sistema (tabla `USUARIOS`): inicio de sesión, registro de clientes y protección de la API con JWT.

| Endpoint | Acceso | Qué hace |
|---|---|---|
| `POST /api/auth/login` | Público | Inicia sesión y devuelve un token |
| `POST /api/auth/registro` | Público | Crea la cuenta de un cliente y devuelve un token |

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

### Registro de clientes: `POST /api/auth/registro`

Endpoint público para que el **dueño de la mascota cree su propia cuenta** desde el frontend. El personal de la clínica (veterinarios, cajeros) no se registra aquí: lo da de alta el administrador.

```json
{
  "correo": "ana.lopez@correo.com",
  "password": "MiClave123",
  "nombre": "Ana",
  "apellidos": "López Pérez",
  "telefono": "5551234567",
  "direccion": "Av. Juárez 123, Centro"
}
```

`direccion` es opcional. En **una sola transacción**:

1. Crea el usuario con rol `CLIENTE` (correo en minúsculas, contraseña cifrada con BCrypt).
2. Crea su perfil en `CLIENTES` con `ClienteService` del módulo de clientes, vinculado por `id_usuario`, con su folio `CLI-XXXX`.
3. Genera un token, así el cliente queda con la sesión iniciada.

Si falla cualquier paso, no se guarda nada: nunca queda una cuenta sin perfil de cliente, ni al revés.

Responde **201**:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "expiraEnMs": 86400000,
  "correo": "ana.lopez@correo.com",
  "rol": "CLIENTE",
  "cliente": {
    "idCliente": 1,
    "codigoCliente": "CLI-0001",
    "idUsuario": 2,
    "nombre": "Ana",
    "apellidos": "López Pérez",
    "telefono": "5551234567",
    "direccion": "Av. Juárez 123, Centro",
    "responsablesExtra": [],
    "mascotas": []
  }
}
```

| Campo | Regla |
|---|---|
| `correo` | Obligatorio, formato de correo, máximo 120 caracteres. Se guarda en minúsculas y sin espacios |
| `password` | Obligatoria, **de 8 a 72 caracteres, con al menos una letra y un número**. El máximo es 72 porque BCrypt ignora lo que pase de 72 bytes |
| `nombre`, `apellidos`, `telefono` | Obligatorios; máximo 80, 100 y 20 caracteres (los mismos límites del módulo de clientes) |
| `direccion` | Opcional, máximo 255 caracteres |

| Caso | Código | `message` |
|---|---|---|
| Cuenta creada | **201** | — |
| Datos inválidos | **400** | `Datos inválidos` (con `errors` por campo) |
| El correo ya existe (sin importar mayúsculas) | **409** | `El correo ya está registrado` |

> **Pendiente:** si recepción ya dio de alta a un cliente sin cuenta (`POST /api/clientes`) y después ese cliente se registra, quedarían dos perfiles. Más adelante habrá que vincular la cuenta al perfil existente (por ejemplo, buscando por teléfono).

> **Folios:** los números de folio pueden saltarse (por ejemplo, de `CLI-0003` a `CLI-0009`). Es normal: PostgreSQL no reutiliza los ids de registros que se deshicieron, como los de las pruebas automáticas.

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

#### Permisos del módulo de clientes

El módulo de clientes y mascotas aplica dos controles:

1. **Por rol**, con `@PreAuthorize` en cada endpoint:

| Endpoints | ADMIN | VETERINARIO | CAJERO | CLIENTE |
|---|:-:|:-:|:-:|:-:|
| `GET /api/clientes`, `POST /api/clientes`, `GET /api/mascotas` (directorios y alta en mostrador) | ✅ | ✅ | ✅ | ❌ |
| Perfil del cliente, sus responsables, sus mascotas y consulta de antecedentes | ✅ | ✅ | ✅ | ✅ solo lo suyo |
| `PATCH .../desactivar`, `PATCH .../reactivar`, crear/editar/borrar antecedentes | ✅ | ✅ | ❌ | ❌ |

2. **Por pertenencia**, con `ValidadorAccesoCliente`: si quien llama es CLIENTE, el servicio comprueba que el perfil o la mascota sean suyos. Si no, responde **403** con `No tienes permiso para acceder o modificar la información de otro cliente` (o `... mascotas de otro cliente`). Además, un CLIENTE no puede cambiar la cuenta de usuario vinculada a su perfil ni pasar su mascota a otro cliente. El personal no tiene esta restricción.

Un CLIENTE consulta su propio perfil con `GET /api/clientes/usuario/{idUsuario}`. El `idUsuario` viene en la respuesta del registro, en `cliente.idUsuario`. Las pruebas manuales de estos permisos están en [Roles y pertenencia](#roles-y-pertenencia-módulo-de-clientes).

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

## Módulo de personal (`personal`)

Gestiona a los empleados de la clínica (veterinarios y cajeros), sus turnos semanales y los bloqueos de su agenda. La agenda del Sprint 3 usará estos datos para calcular los horarios libres de cada veterinario.

> **Estado:** Parte 1 lista (entidades y repositorios). Los endpoints (`/api/personal`, horarios y bloqueos) llegan en las siguientes partes.

### Entidades

| Entidad | Tabla | Qué guarda |
|---|---|---|
| `Personal` | `PERSONAL` | Perfil del empleado: nombre, apellidos, teléfono, cédula profesional y especies que atiende. Está vinculado a **una** cuenta de `USUARIOS` |
| `HorarioPersonal` | `HORARIOS_PERSONAL` | Un turno de trabajo: día de la semana, hora de inicio y hora de fin. Una persona puede tener varios turnos el mismo día |
| `BloqueoDisponibilidad` | `BLOQUEOS_DISPONIBILIDAD` | Un periodo en que la persona no está disponible: fecha y hora de inicio y de fin, motivo y notas |
| `MotivoBloqueo` (enum) | | `PERSONAL`, `CIRUGIA`, `DIA_LIBRE` |

- **Días de la semana:** `0` = domingo, `1` = lunes … `6` = sábado, la misma numeración que `HORARIOS_CLINICA`.
- **Correo, rol y estado activo:** vienen de la cuenta en `USUARIOS`, porque `PERSONAL` no tiene columna `activo`. Dar de baja a un empleado es desactivar su usuario, y el filtro JWT ya le impide iniciar sesión.
- **Reglas que ya cuida la base:** un usuario solo puede tener un perfil de personal, el fin de un turno o bloqueo debe ser posterior a su inicio, y el día debe estar entre 0 y 6.

### Repositorios

| Repositorio | Búsquedas |
|---|---|
| `PersonalRepository` | Por cuenta de usuario (`findByUsuarioIdUsuario`), por correo de quien inició sesión (`findByUsuarioCorreo`), si una cuenta ya tiene perfil (`existsByUsuarioIdUsuario`), todo el personal o solo un rol, ordenados por apellidos |
| `HorarioPersonalRepository` | Turnos de una persona (toda la semana o un día) y un turno solo si pertenece a esa persona |
| `BloqueoDisponibilidadRepository` | Bloqueos de una persona (todos o solo los que aún no terminan), un bloqueo solo si pertenece a esa persona, y `buscarEncimados` |

`buscarEncimados(idPersonal, inicio, fin)` devuelve los bloqueos de esa persona que chocan con el rango indicado. Dos rangos chocan si cada uno empieza antes de que termine el otro. Si uno termina justo cuando empieza el otro (10:00-12:00 y 12:00-14:00), **no** chocan. Lo usarán la validación de bloqueos y la agenda.
