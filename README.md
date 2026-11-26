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
[INFO] Tests run: 176, Failures: 0, Errors: 0, Skipped: 0
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
| `auth.controller.AuthMeTest` | 9 | `GET /api/auth/me` con tokens reales: perfil de cliente después del login, perfil de personal (veterinario y cajero), admin sin perfil, cuenta sin perfil, sin token, token inválido y cuenta desactivada (401), y login y registro siguen públicos |
| `auth.controller.AuthRegistroTest` | 13 | Registro: crea usuario y cliente vinculados, contraseña cifrada, token, login posterior, correo repetido y contraseñas débiles |
| `auth.service.AuthRegistroTransaccionTest` | 1 | Si falla la creación del cliente, tampoco se guarda el usuario |
| `personal.controller.PersonalControllerTest` | 19 | Personal: alta de veterinario y cajero, login del empleado nuevo, correo normalizado y repetido, cédula obligatoria, roles no permitidos, filtros, edición, desactivar/reactivar y permisos por rol |
| `agenda.controller.HorarioClinicaControllerTest` | 19 | Horario de la clínica: consulta por cualquier rol con sesión, 401 sin sesión, guardado sin segundos, reenvío tal cual del `GET`, semana incompleta o con día repetido, día fuera de rango, campos faltantes, cierre antes de la apertura, hora inválida, turnos que quedarían fuera (409, no se guarda nada, lista de varios, más de diez) y permisos por rol |
| `agenda.repository.ConfigClinicaRepositoryTest` | 12 | Horario de la clínica, tipos de consultorio y consultorios contra la base: datos iniciales, búsquedas sin importar mayúsculas, filtros por tipo y activos, conteo de consultorios activos y reglas de la base (día repetido, cierre antes de la apertura, nombre de tipo repetido, consultorio sin tipo) |
| `personal.controller.BloqueoDisponibilidadControllerTest` | 18 | Bloqueos: alta con fechas sin segundos, ausencia que ya empezó, lista sin terminados salvo `incluirPasados`, motivo inválido, fin antes del inicio, bloqueo ya terminado, bloqueos encimados (409), seguidos permitidos, edición, borrado, historial intocable, bloqueo de otra persona (404) y permisos por rol |
| `personal.controller.HorarioPersonalControllerTest` | 17 | Turnos: alta con nombre del día y formato `HH:mm`, orden, segundos descartados, día fuera de rango, fin antes del inicio, turnos encimados (409), turnos seguidos permitidos, edición sin chocar consigo mismo, borrado, turno de otra persona (404) y permisos por rol |
| `personal.service.PersonalAltaTransaccionTest` | 1 | Si falla el perfil de personal, tampoco se guarda la cuenta |
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

#### Personal (alta y gestión)

Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1.

```powershell
# 16. Alta de una veterinaria → 201
$vet = @{ correo = "laura.prueba@veterinaria.com"; password = "Inicial123"; rol = "VETERINARIO"; nombre = "Laura"; apellidos = "Zamora"; telefono = "5551112222"; cedulaProfesional = "12345678"; especialidadEspecies = "Perros, Gatos" }
$alta = Invoke-Api POST "/personal" $vet $token
$alta.Codigo
$idVet = $alta.Respuesta.idPersonal

# 17. El mismo correo otra vez → 409
(Invoke-Api POST "/personal" $vet $token).Codigo

# 18. Veterinario sin cédula → 400 con el mensaje
$sinCedula = $vet.Clone(); $sinCedula.correo = "otro.vet@veterinaria.com"; $sinCedula.Remove("cedulaProfesional")
(Invoke-Api POST "/personal" $sinCedula $token).Respuesta.message

# 19. Rol no permitido → 400 con el error del campo
$rolMalo = $vet.Clone(); $rolMalo.correo = "otro.vet@veterinaria.com"; $rolMalo.rol = "ADMIN"
(Invoke-Api POST "/personal" $rolMalo $token).Respuesta.errors

# 20. Alta de un cajero (sin cédula) → 201
(Invoke-Api POST "/personal" @{ correo = "pedro.prueba@veterinaria.com"; password = "Inicial123"; rol = "CAJERO"; nombre = "Pedro"; apellidos = "Alvarez"; telefono = "5553334444" } $token).Codigo

# 21. Lista filtrada: solo veterinarios → solo aparece Laura
(Invoke-Api GET "/personal?rol=VETERINARIO" -Token $token).Respuesta | Select-Object idPersonal, correo, rol, activo

# 22. Editar el perfil → 200 con el teléfono nuevo
(Invoke-Api PUT "/personal/$idVet" @{ nombre = "Laura"; apellidos = "Zamora"; telefono = "5559998888"; cedulaProfesional = "12345678" } $token).Respuesta.telefono

# 23. La veterinaria inicia sesión y consulta su perfil → 200
$tVet = (Invoke-Api POST "/auth/login" @{ correo = "laura.prueba@veterinaria.com"; password = "Inicial123" }).Respuesta.token
(Invoke-Api GET "/personal/mi-perfil" -Token $tVet).Respuesta.correo

# 24. La veterinaria intenta ver la lista del personal → 403
(Invoke-Api GET "/personal" -Token $tVet).Codigo

# 25. Desactivar → 204; su token deja de servir (401) y ya no puede iniciar sesión (403)
(Invoke-Api PATCH "/personal/$idVet/desactivar" -Token $token).Codigo
(Invoke-Api GET "/personal/mi-perfil" -Token $tVet).Codigo
(Invoke-Api POST "/auth/login" @{ correo = "laura.prueba@veterinaria.com"; password = "Inicial123" }).Codigo

# 26. Reactivar → 200 y vuelve a iniciar sesión
(Invoke-Api PATCH "/personal/$idVet/reactivar" -Token $token).Respuesta.activo
(Invoke-Api POST "/auth/login" @{ correo = "laura.prueba@veterinaria.com"; password = "Inicial123" }).Codigo
```

| Caso | Resultado esperado |
|---|---|
| 16. Alta de veterinaria | `201` |
| 17. Correo repetido | `409` |
| 18. Veterinario sin cédula | `La cédula profesional es obligatoria para veterinarios` |
| 19. Rol `ADMIN` | Columna `rol`: `El rol debe ser VETERINARIO o CAJERO` |
| 20. Alta de cajero sin cédula | `201` |
| 21. Filtro por rol | Solo `laura.prueba@veterinaria.com`, con rol `VETERINARIO` |
| 22. Edición | `5559998888` |
| 23. Su propio perfil | `laura.prueba@veterinaria.com` |
| 24. Veterinaria en la lista del personal | `403` |
| 25. Desactivar | `204`, `401`, `403` |
| 26. Reactivar | `True`, `200` |

#### Horarios del personal

Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1. Primero da de alta a un veterinario para asignarle turnos:

```powershell
# 27. Alta de un veterinario para las pruebas → 201
$alta = Invoke-Api POST "/personal" @{ correo = "mario.horarios@veterinaria.com"; password = "Inicial123"; rol = "VETERINARIO"; nombre = "Mario"; apellidos = "Horarios"; telefono = "5556667777"; cedulaProfesional = "87654321" } $token
$alta.Codigo
$idVet = $alta.Respuesta.idPersonal

# 28. Turno del lunes de 08:00 a 14:00 → 201 con nombreDia "Lunes"
$turno = Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 1; horaInicio = "08:00"; horaFin = "14:00" } $token
$turno.Codigo
$turno.Respuesta.nombreDia
$idTurno = $turno.Respuesta.idHorario

# 29. Turno que se encima (12:00-18:00) → 409 con el turno que choca
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 1; horaInicio = "12:00"; horaFin = "18:00" } $token).Respuesta.message

# 30. Turno seguido (14:00-18:00) → 201
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 1; horaInicio = "14:00"; horaFin = "18:00" } $token).Codigo

# 31. Fin antes del inicio → 400
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 2; horaInicio = "14:00"; horaFin = "08:00" } $token).Codigo

# 32. Día 7 → 400 con el error del campo
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 7; horaInicio = "08:00"; horaFin = "14:00" } $token).Respuesta.errors

# 33. Turno del sábado y lista de la semana → ordenada por día y hora
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 6; horaInicio = "09:00"; horaFin = "13:00" } $token).Codigo
(Invoke-Api GET "/personal/$idVet/horarios" -Token $token).Respuesta | Select-Object nombreDia, horaInicio, horaFin

# 34. Editar el turno del lunes para que termine a las 13:00 → 200
(Invoke-Api PUT "/personal/$idVet/horarios/$idTurno" @{ diaSemana = 1; horaInicio = "08:00"; horaFin = "13:00" } $token).Respuesta.horaFin

# 35. El veterinario consulta sus turnos → 3 turnos
$tVet = (Invoke-Api POST "/auth/login" @{ correo = "mario.horarios@veterinaria.com"; password = "Inicial123" }).Respuesta.token
(Invoke-Api GET "/personal/mi-perfil/horarios" -Token $tVet).Respuesta.Count

# 36. El veterinario intenta agregarse un turno → 403
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 3; horaInicio = "08:00"; horaFin = "14:00" } $tVet).Codigo

# 37. Borrar el turno del lunes → 204
(Invoke-Api DELETE "/personal/$idVet/horarios/$idTurno" -Token $token).Codigo
```

| Caso | Resultado esperado |
|---|---|
| 27. Alta del veterinario | `201` |
| 28. Turno del lunes | `201` y `Lunes` |
| 29. Turno encimado | `El turno se encima con el turno del Lunes de 08:00 a 14:00` |
| 30. Turno seguido | `201` |
| 31. Fin antes del inicio | `400` |
| 32. Día 7 | Columna `diaSemana`: `El día de la semana debe estar entre 0 (domingo) y 6 (sábado)` |
| 33. Lista de la semana | `Lunes 08:00-14:00`, `Lunes 14:00-18:00`, `Sábado 09:00-13:00` |
| 34. Edición | `13:00` |
| 35. Sus turnos | `3` |
| 36. El veterinario se agrega un turno | `403` |
| 37. Borrar | `204` |

#### Bloqueos de agenda

Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1. Primero da de alta a un veterinario:

```powershell
# 38. Alta de un veterinario para las pruebas → 201
$alta = Invoke-Api POST "/personal" @{ correo = "sofia.bloqueos@veterinaria.com"; password = "Inicial123"; rol = "VETERINARIO"; nombre = "Sofía"; apellidos = "Bloqueos"; telefono = "5558889999"; cedulaProfesional = "11223344" } $token
$alta.Codigo
$idVet = $alta.Respuesta.idPersonal

# 39. Vacaciones del 22 al 27 de diciembre de 2030 → 201
$vac = Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2030-12-22T00:00"; fechaFin = "2030-12-27T00:00"; motivo = "PERSONAL"; notas = "Vacaciones" } $token
$vac.Codigo
$idVac = $vac.Respuesta.idBloqueo

# 40. Cirugía que se encima con las vacaciones → 409 con el bloqueo que choca
(Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2030-12-26T10:00"; fechaFin = "2030-12-26T14:00"; motivo = "CIRUGIA" } $token).Respuesta.message

# 41. Día libre justo después de las vacaciones → 201
(Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2030-12-27T00:00"; fechaFin = "2030-12-28T00:00"; motivo = "DIA_LIBRE" } $token).Codigo

# 42. Bloqueo que ya terminó → 400
(Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2020-01-01T08:00"; fechaFin = "2020-01-02T08:00"; motivo = "PERSONAL" } $token).Respuesta.message

# 43. Motivo inválido → 400 con el error del campo
(Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2030-11-01T08:00"; fechaFin = "2030-11-01T14:00"; motivo = "VACACIONES" } $token).Respuesta.errors

# 44. Lista de bloqueos → ordenada por fecha
(Invoke-Api GET "/personal/$idVet/bloqueos" -Token $token).Respuesta | Select-Object motivo, fechaInicio, fechaFin

# 45. Alargar las vacaciones hasta el 29 → choca con el día libre del 27 → 409
(Invoke-Api PUT "/personal/$idVet/bloqueos/$idVac" @{ fechaInicio = "2030-12-22T00:00"; fechaFin = "2030-12-29T00:00"; motivo = "PERSONAL" } $token).Codigo

# 46. Adelantar el inicio de las vacaciones al 20 → 200
(Invoke-Api PUT "/personal/$idVet/bloqueos/$idVac" @{ fechaInicio = "2030-12-20T00:00"; fechaFin = "2030-12-27T00:00"; motivo = "PERSONAL"; notas = "Vacaciones" } $token).Respuesta.fechaInicio

# 47. La veterinaria consulta sus bloqueos → 2
$tVet = (Invoke-Api POST "/auth/login" @{ correo = "sofia.bloqueos@veterinaria.com"; password = "Inicial123" }).Respuesta.token
(Invoke-Api GET "/personal/mi-perfil/bloqueos" -Token $tVet).Respuesta.Count

# 48. La veterinaria intenta registrarse un bloqueo → 403
(Invoke-Api POST "/personal/$idVet/bloqueos" @{ fechaInicio = "2030-10-01T08:00"; fechaFin = "2030-10-01T14:00"; motivo = "CIRUGIA" } $tVet).Codigo

# 49. Cancelar las vacaciones → 204
(Invoke-Api DELETE "/personal/$idVet/bloqueos/$idVac" -Token $token).Codigo
```

| Caso | Resultado esperado |
|---|---|
| 38. Alta del veterinario | `201` |
| 39. Vacaciones | `201` |
| 40. Cirugía encimada | `El bloqueo se encima con otro bloqueo (PERSONAL) del 2030-12-22 00:00 al 2030-12-27 00:00` |
| 41. Día libre seguido | `201` |
| 42. Bloqueo ya terminado | `El bloqueo ya terminó: la fecha de fin debe ser futura` |
| 43. Motivo inválido | Columna `motivo`: `El motivo debe ser PERSONAL, CIRUGIA o DIA_LIBRE` |
| 44. Lista | `PERSONAL 2030-12-22T00:00 → 2030-12-27T00:00` y `DIA_LIBRE 2030-12-27T00:00 → 2030-12-28T00:00` |
| 45. Alargar hasta chocar | `409` |
| 46. Adelantar el inicio | `2030-12-20T00:00` |
| 47. Sus bloqueos | `2` |
| 48. La veterinaria se registra un bloqueo | `403` |
| 49. Cancelar | `204` |

#### Usuario que inició sesión (`/me`)

Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1.

```powershell
# 50. Admin → 200, rol ADMIN y sin perfiles
$me = Invoke-Api GET "/auth/me" -Token $token
$me.Codigo
$me.Respuesta | Select-Object correo, rol, cliente, personal

# 51. Sin token → 401 (aunque esté dentro de /api/auth)
(Invoke-Api GET "/auth/me").Codigo

# 52. Un cliente se registra, vuelve a iniciar sesión y obtiene su idCliente → número
$null = Invoke-Api POST "/auth/registro" @{ correo = "ana.me@correo.com"; password = "MiClave123"; nombre = "Ana"; apellidos = "Me"; telefono = "5551230000" }
$tCliente = (Invoke-Api POST "/auth/login" @{ correo = "ana.me@correo.com"; password = "MiClave123" }).Respuesta.token
$meCliente = (Invoke-Api GET "/auth/me" -Token $tCliente).Respuesta
$meCliente.cliente.idCliente

# 53. Con ese idCliente consulta sus mascotas → 200
(Invoke-Api GET "/mascotas/cliente/$($meCliente.cliente.idCliente)" -Token $tCliente).Codigo

# 54. Un cajero obtiene su idPersonal → número
$null = Invoke-Api POST "/personal" @{ correo = "caja.me@veterinaria.com"; password = "Inicial123"; rol = "CAJERO"; nombre = "Pedro"; apellidos = "Me"; telefono = "5553330000" } $token
$tCajero = (Invoke-Api POST "/auth/login" @{ correo = "caja.me@veterinaria.com"; password = "Inicial123" }).Respuesta.token
(Invoke-Api GET "/auth/me" -Token $tCajero).Respuesta.personal.idPersonal
```

| Caso | Resultado esperado |
|---|---|
| 50. Admin | `200`, `ADMIN`, `cliente` y `personal` vacíos |
| 51. Sin token | `401` |
| 52. Cliente después del login | Un número (su `idCliente`) |
| 53. Sus mascotas con ese id | `200` |
| 54. Cajero | Un número (su `idPersonal`) |

#### Horario de la clínica

Usa la misma función `Invoke-Api` y el `$token` del admin del caso 1. **Al terminar, el horario original se restablece** (caso 64).

```powershell
# 55. Consultar el horario → 7 días, de domingo a sábado. Se guarda una copia para restablecerlo al final
$original = (Invoke-Api GET "/config/horarios-clinica" -Token $token).Respuesta
$original | Select-Object nombreDia, horaApertura, horaCierre, laborable

# 56. Cerrar el sábado a las 14:00 (todavía no hay turnos) → 200
$nueva = $original | ForEach-Object { $d = $_.PSObject.Copy(); if ($d.diaSemana -eq 6) { $d.horaCierre = "14:00" }; $d }
$r = Invoke-Api PUT "/config/horarios-clinica" $nueva $token
$r.Codigo
$r.Respuesta[6].horaCierre

# 57. Alta de una veterinaria con un turno el sábado (10:00-13:00) y otro el domingo (09:00-12:00) → 201, 201, 201
$alta = Invoke-Api POST "/personal" @{ correo = "marta.clinica@veterinaria.com"; password = "Inicial123"; rol = "VETERINARIO"; nombre = "Marta"; apellidos = "Clínica"; telefono = "5554443333"; cedulaProfesional = "55667788" } $token
$alta.Codigo
$idVet = $alta.Respuesta.idPersonal
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 6; horaInicio = "10:00"; horaFin = "13:00" } $token).Codigo
(Invoke-Api POST "/personal/$idVet/horarios" @{ diaSemana = 0; horaInicio = "09:00"; horaFin = "12:00" } $token).Codigo

# 58. Cerrar el sábado a las 12:00 dejaría fuera el turno de Marta → 409 con el turno afectado
$recorte = $nueva | ForEach-Object { $d = $_.PSObject.Copy(); if ($d.diaSemana -eq 6) { $d.horaCierre = "12:00" }; $d }
(Invoke-Api PUT "/config/horarios-clinica" $recorte $token).Respuesta.message

# 59. Dejar el domingo como no laborable también dejaría fuera su turno → 409
$domingoCerrado = $nueva | ForEach-Object { $d = $_.PSObject.Copy(); if ($d.diaSemana -eq 0) { $d.laborable = $false }; $d }
(Invoke-Api PUT "/config/horarios-clinica" $domingoCerrado $token).Respuesta.message

# 60. No se guardó nada de los dos intentos → domingo sigue laborable y el sábado cierra a las 14:00
(Invoke-Api GET "/config/horarios-clinica" -Token $token).Respuesta | Where-Object diaSemana -in 0, 6 | Select-Object nombreDia, horaCierre, laborable

# 61. Cierre antes de la apertura → 400 con el día afectado
$malo = $nueva | ForEach-Object { $d = $_.PSObject.Copy(); if ($d.diaSemana -eq 1) { $d.horaApertura = "20:00"; $d.horaCierre = "08:00" }; $d }
(Invoke-Api PUT "/config/horarios-clinica" $malo $token).Respuesta.message

# 62. Menos de 7 días → 400
(Invoke-Api PUT "/config/horarios-clinica" ($original | Select-Object -First 6) $token).Respuesta.message

# 63. La veterinaria consulta el horario → 200, pero no puede modificarlo → 403
$tVet = (Invoke-Api POST "/auth/login" @{ correo = "marta.clinica@veterinaria.com"; password = "Inicial123" }).Respuesta.token
(Invoke-Api GET "/config/horarios-clinica" -Token $tVet).Codigo
(Invoke-Api PUT "/config/horarios-clinica" $original $tVet).Codigo

# 64. Restablecer el horario original → 200 (sus turnos caben en el horario original)
(Invoke-Api PUT "/config/horarios-clinica" $original $token).Codigo
(Invoke-Api GET "/config/horarios-clinica" -Token $token).Respuesta | Select-Object nombreDia, horaApertura, horaCierre, laborable
```

| Caso | Resultado esperado |
|---|---|
| 55. Consultar | Los 7 días, de `Domingo` a `Sábado` |
| 56. Cerrar el sábado a las 14:00 | `200` y `14:00` |
| 57. Alta y turnos | `201`, `201`, `201` |
| 58. Sábado cierra a las 12:00 | `No se guardó el horario: 1 turno quedaría fuera del horario de la clínica: Marta Clínica, Sábado 10:00-13:00. Ajusta o borra esos turnos y vuelve a intentarlo.` |
| 59. Domingo no laborable | El mismo mensaje, con `Marta Clínica, Domingo 09:00-12:00` |
| 60. Nada se guardó | Domingo `14:00` y `True`; sábado `14:00` y `True` |
| 61. Cierre antes de la apertura | `El día Lunes: la hora de cierre debe ser posterior a la hora de apertura` |
| 62. Menos de 7 días | `Debes enviar el horario de los 7 días de la semana (0 = domingo … 6 = sábado), cada uno una sola vez` |
| 63. La veterinaria | `200` al consultar, `403` al modificar |
| 64. Restablecer | `200` y el horario como estaba al inicio |

#### Borrar las cuentas de prueba

Las pruebas manuales **sí guardan datos**. Para borrar una cuenta de prueba (primero su perfil de cliente, después el usuario):

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM clientes WHERE id_usuario = (SELECT id_usuario FROM usuarios WHERE correo = 'prueba.manual@correo.com'); DELETE FROM usuarios WHERE correo = 'prueba.manual@correo.com';"
```

Para las pruebas de roles (Ana, Beto y el cajero) hay que borrar también sus mascotas y antecedentes, en este orden:

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM antecedentes_clinicos WHERE id_mascota IN (SELECT m.id_mascota FROM mascotas m JOIN clientes c ON c.id_cliente = m.id_cliente JOIN usuarios u ON u.id_usuario = c.id_usuario WHERE u.correo LIKE '%.roles@correo.com'); DELETE FROM mascotas WHERE id_cliente IN (SELECT c.id_cliente FROM clientes c JOIN usuarios u ON u.id_usuario = c.id_usuario WHERE u.correo LIKE '%.roles@correo.com'); DELETE FROM clientes WHERE id_usuario IN (SELECT id_usuario FROM usuarios WHERE correo LIKE '%.roles@correo.com'); DELETE FROM usuarios WHERE correo LIKE '%.roles@correo.com';"
```

Para el cliente de la prueba de `/me`:

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM clientes WHERE id_usuario = (SELECT id_usuario FROM usuarios WHERE correo = 'ana.me@correo.com'); DELETE FROM usuarios WHERE correo = 'ana.me@correo.com';"
```

Para el personal de prueba basta con borrar sus usuarios: su perfil, turnos y bloqueos se borran solos (`ON DELETE CASCADE`):

```bash
psql -U postgres -h localhost -p 5433 -d veterinaria_db -c "DELETE FROM usuarios WHERE correo IN ('laura.prueba@veterinaria.com', 'pedro.prueba@veterinaria.com', 'mario.horarios@veterinaria.com', 'sofia.bloqueos@veterinaria.com', 'caja.me@veterinaria.com', 'marta.clinica@veterinaria.com');"
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

### Usuario que inició sesión: `GET /api/auth/me`

Devuelve la cuenta del token y su perfil según el rol. El frontend lo llama **justo después del login**: el login solo devuelve el correo y el rol, y aquí obtiene el `idCliente` o el `idPersonal` que necesitan los demás endpoints (por ejemplo, `GET /api/mascotas/cliente/{idCliente}` para "Mis mascotas").

```http
GET /api/auth/me
Authorization: Bearer <token>
```

Respuesta `200` de un **cliente**:

```json
{
  "idUsuario": 5,
  "correo": "ana.lopez@correo.com",
  "rol": "CLIENTE",
  "activo": true,
  "cliente": {
    "idCliente": 3,
    "codigoCliente": "CLI-0003",
    "idUsuario": 5,
    "nombre": "Ana",
    "apellidos": "López Pérez",
    "telefono": "5551234567",
    "direccion": null,
    "responsivaUrl": null,
    "responsablesExtra": [],
    "mascotas": []
  },
  "personal": null
}
```

| Rol | `cliente` | `personal` |
|---|---|---|
| `CLIENTE` | Su perfil de cliente (el mismo formato que `GET /api/clientes/{id}`) | `null` |
| `VETERINARIO`, `CAJERO` | `null` | Su perfil de personal (el mismo formato que `GET /api/personal/{id}`) |
| `ADMIN` | `null` | `null` |

- Si la cuenta no tiene perfil (por ejemplo, un cliente cuyo perfil se borró), ese campo llega en `null` en lugar de dar error.
- Nunca incluye la contraseña.

| Situación | Respuesta |
|---|---|
| Token válido de una cuenta activa | `200` |
| Sin token | `401` `Se requiere iniciar sesión para acceder a este recurso` |
| Token alterado o vencido | `401` `El token es inválido o ha expirado` |
| La cuenta se desactivó después del login | `401` `El usuario no existe o está desactivado` |

### Seguridad (Spring Security)

La configuración está en `common/security/SecurityConfig.java`. Spring Security revisa cada petición **antes** de que llegue a los controladores:

| Regla | Detalle |
|---|---|
| Rutas públicas (sin token) | `/api/auth/**` (login y registro) y Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) |
| Excepción | `/api/auth/me` **sí** requiere token. La regla va antes que las rutas públicas, porque Spring aplica la primera que coincide |
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

> **Estado:** módulo completo: entidades y repositorios, alta y gestión del personal, horarios y bloqueos de agenda.

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

### Endpoints del personal

| Método y ruta | Quién | Qué hace | Respuesta |
|---|---|---|---|
| `POST /api/personal` | ADMIN | Da de alta a un veterinario o cajero: crea su cuenta y su perfil en una sola operación | `201` |
| `GET /api/personal` | ADMIN | Lista al personal ordenado por apellidos. Filtros opcionales: `?rol=VETERINARIO` o `CAJERO`, `?activo=true` o `false` | `200` |
| `GET /api/personal/{id}` | ADMIN | Detalle de un empleado | `200` / `404` |
| `PUT /api/personal/{id}` | ADMIN | Edita nombre, apellidos, teléfono, cédula y especies | `200` |
| `PATCH /api/personal/{id}/desactivar` | ADMIN | Desactiva su cuenta: no puede iniciar sesión y su token deja de servir. Sus datos se conservan | `204` |
| `PATCH /api/personal/{id}/reactivar` | ADMIN | Vuelve a activar su cuenta | `200` |
| `GET /api/personal/mi-perfil` | VETERINARIO, CAJERO | El empleado consulta **su propio** perfil, según su token | `200` / `404` |

#### Alta de un empleado

```http
POST /api/personal
Authorization: Bearer <token del ADMIN>
Content-Type: application/json
```

```json
{
  "correo": "laura.zamora@veterinaria.com",
  "password": "Inicial123",
  "rol": "VETERINARIO",
  "nombre": "Laura",
  "apellidos": "Zamora Ruiz",
  "telefono": "5551112222",
  "cedulaProfesional": "12345678",
  "especialidadEspecies": "Perros, Gatos"
}
```

Respuesta `201`. El mismo formato sale en la lista, el detalle, la edición, la reactivación y `mi-perfil`:

```json
{
  "idPersonal": 1,
  "idUsuario": 2,
  "correo": "laura.zamora@veterinaria.com",
  "rol": "VETERINARIO",
  "activo": true,
  "nombre": "Laura",
  "apellidos": "Zamora Ruiz",
  "telefono": "5551112222",
  "cedulaProfesional": "12345678",
  "especialidadEspecies": "Perros, Gatos"
}
```

La respuesta **nunca** incluye la contraseña. Con esa contraseña inicial, el empleado ya puede iniciar sesión en `POST /api/auth/login`.

#### Reglas

| Regla | Si no se cumple |
|---|---|
| `rol` solo puede ser `VETERINARIO` o `CAJERO`: los clientes se registran solos y el ADMIN ya existe | `400`, `errors.rol` |
| La contraseña tiene de 8 a 72 caracteres, con al menos una letra y un número (igual que el registro). Se guarda cifrada con BCrypt | `400`, `errors.password` |
| El correo se guarda sin espacios y en minúsculas, y no puede estar registrado | `409` `El correo ya está registrado` |
| Un **veterinario** debe tener cédula profesional, al darlo de alta y al editarlo: identifica legalmente al médico y saldrá en las recetas. Para un cajero es opcional | `400` `La cédula profesional es obligatoria para veterinarios` |
| Nombre, apellidos y teléfono son obligatorios. La cédula y las especies son opcionales; si llegan vacías se guardan como `null` | `400` con el error de cada campo |
| El alta es **todo o nada**: si falla el perfil, tampoco se guarda la cuenta | |
| La edición no cambia el correo, el rol ni la contraseña | |
| Desactivar o reactivar dos veces seguidas no da error | |

Para el frontend: el `idPersonal` es el que usarán los turnos, los bloqueos y, en el Sprint 3, las citas (`id_veterinario`).

### Horarios del personal

Turnos de trabajo semanales de cada empleado (por ejemplo, "lunes de 08:00 a 14:00"). En el Sprint 3 la agenda los cruzará con el horario de la clínica y los bloqueos para calcular los espacios libres de cada veterinario.

| Método y ruta | Quién | Qué hace | Respuesta |
|---|---|---|---|
| `POST /api/personal/{id}/horarios` | ADMIN | Agrega un turno | `201` |
| `GET /api/personal/{id}/horarios` | ADMIN | Turnos de la semana, de domingo a sábado y por hora de inicio | `200` |
| `PUT /api/personal/{id}/horarios/{idHorario}` | ADMIN | Edita un turno | `200` |
| `DELETE /api/personal/{id}/horarios/{idHorario}` | ADMIN | Borra un turno | `204` |
| `GET /api/personal/mi-perfil/horarios` | VETERINARIO, CAJERO | El empleado consulta **sus** turnos, según su token | `200` |

#### Agregar un turno

```http
POST /api/personal/1/horarios
Authorization: Bearer <token del ADMIN>
Content-Type: application/json
```

```json
{ "diaSemana": 1, "horaInicio": "08:00", "horaFin": "14:00" }
```

Respuesta `201` (mismo formato en la lista, la edición y `mi-perfil/horarios`):

```json
{
  "idHorario": 5,
  "idPersonal": 1,
  "diaSemana": 1,
  "nombreDia": "Lunes",
  "horaInicio": "08:00",
  "horaFin": "14:00"
}
```

- `diaSemana`: `0` = domingo, `1` = lunes … `6` = sábado. `nombreDia` viene ya convertido para el frontend.
- Las horas viajan como `"HH:mm"`. Si llegan segundos (`"08:00:45"`), se descartan.

#### Reglas

| Regla | Si no se cumple |
|---|---|
| `diaSemana` va de 0 a 6 | `400`, `errors.diaSemana` |
| `horaFin` es posterior a `horaInicio`: un turno no cruza la medianoche | `400` `La hora de fin debe ser posterior a la hora de inicio (el turno no puede cruzar la medianoche)` |
| Los turnos de una persona en un mismo día **no se enciman**: con 08:00-14:00 no se puede agregar 12:00-18:00 | `409` `El turno se encima con el turno del Lunes de 08:00 a 14:00` |
| Turnos seguidos sí se permiten (08:00-13:00 y 13:00-17:00), igual que varios turnos el mismo día (08:00-13:00 y 16:00-20:00) | |
| Al editar, el turno no se compara consigo mismo (se puede alargar o mover sin conflicto) | |
| El turno debe pertenecer al empleado de la ruta: `/personal/1/horarios/9` con un turno de otra persona responde 404 | `404` |
| El empleado debe existir | `404` |

> **Pendiente para el módulo `config-clinica`:** validar que el turno quede dentro del horario de atención de la clínica (`HORARIOS_CLINICA`). Se agregará cuando exista esa entidad.

### Bloqueos de agenda

Periodos en que un empleado **no está disponible** aunque le toque turno: vacaciones, una cirugía programada o un día libre. En el Sprint 3 la agenda no ofrecerá citas en esos periodos.

| Método y ruta | Quién | Qué hace | Respuesta |
|---|---|---|---|
| `POST /api/personal/{id}/bloqueos` | ADMIN | Registra un bloqueo | `201` |
| `GET /api/personal/{id}/bloqueos` | ADMIN | Bloqueos vigentes y futuros, ordenados por fecha de inicio. Con `?incluirPasados=true` incluye también los que ya terminaron | `200` |
| `PUT /api/personal/{id}/bloqueos/{idBloqueo}` | ADMIN | Edita un bloqueo que aún no termina | `200` |
| `DELETE /api/personal/{id}/bloqueos/{idBloqueo}` | ADMIN | Cancela un bloqueo que aún no termina | `204` |
| `GET /api/personal/mi-perfil/bloqueos` | VETERINARIO, CAJERO | El empleado consulta **sus** bloqueos (acepta `?incluirPasados=true`) | `200` |

#### Registrar un bloqueo

```http
POST /api/personal/1/bloqueos
Authorization: Bearer <token del ADMIN>
Content-Type: application/json
```

```json
{
  "fechaInicio": "2026-12-22T00:00",
  "fechaFin": "2026-12-27T00:00",
  "motivo": "PERSONAL",
  "notas": "Vacaciones de fin de año"
}
```

Respuesta `201` (mismo formato en la lista, la edición y `mi-perfil/bloqueos`):

```json
{
  "idBloqueo": 3,
  "idPersonal": 1,
  "fechaInicio": "2026-12-22T00:00",
  "fechaFin": "2026-12-27T00:00",
  "motivo": "PERSONAL",
  "notas": "Vacaciones de fin de año"
}
```

- `motivo`: `PERSONAL` (permiso o vacaciones), `CIRUGIA` (cirugía programada) o `DIA_LIBRE`.
- Las fechas viajan como `"yyyy-MM-ddTHH:mm"`. Si llegan segundos, se descartan.

#### Reglas

| Regla | Si no se cumple |
|---|---|
| `motivo` es `PERSONAL`, `CIRUGIA` o `DIA_LIBRE` | `400`, `errors.motivo` |
| `fechaFin` es posterior a `fechaInicio` | `400` `La fecha de fin debe ser posterior a la fecha de inicio` |
| `fechaFin` es **futura**: no se registran bloqueos que ya terminaron. `fechaInicio` sí puede ser de hace un rato (una ausencia que empezó en la mañana) | `400` `El bloqueo ya terminó: la fecha de fin debe ser futura` |
| Los bloqueos de una persona **no se enciman**. Bloqueos seguidos sí se permiten (uno termina el día 27 a las 00:00 y otro empieza a esa hora) | `409` `El bloqueo se encima con otro bloqueo (PERSONAL) del 2026-12-22 00:00 al 2026-12-27 00:00` |
| Un bloqueo que **ya terminó** queda como historial: no se edita ni se borra | `400` `Un bloqueo que ya terminó no se puede editar ni borrar` |
| Al editar, el bloqueo no se compara consigo mismo | |
| `notas` es opcional, de máximo 255 caracteres | `400`, `errors.notas` |
| El bloqueo debe pertenecer al empleado de la ruta, y el empleado debe existir | `404` |

> **Pendiente para el Sprint 3 (agenda):** cuando existan las citas, decidir qué pasa si se bloquea un periodo en el que el veterinario ya tiene citas agendadas (avisar o impedirlo).

## Configuración de la clínica (`agenda`)

Horario de atención de la clínica y espacios físicos (tipos de consultorio y consultorios). Las clases están en el paquete `agenda` porque el motor de citas del Sprint 3 las usará: solo ofrecerá citas en días laborables, dentro del horario y en un consultorio libre del tipo que pide el servicio.

> **Estado:** entidades y repositorios, y horario de la clínica listos. Los endpoints de tipos de consultorio y consultorios llegan en las siguientes partes.

### Entidades

| Entidad | Tabla | Qué guarda |
|---|---|---|
| `HorarioClinica` | `HORARIOS_CLINICA` | Un registro por día de la semana: hora de apertura, hora de cierre y si es laborable |
| `TipoConsultorio` | `TIPOS_CONSULTORIO` | Clase de espacio (Consulta, Cirugía, Estética): nombre, descripción y si está activo |
| `Consultorio` | `CONSULTORIOS` | Espacio físico (Consultorio 1, Quirófano…): su tipo, nombre y si está activo |

- **Días de la semana:** `0` = domingo … `6` = sábado, igual que los turnos del personal.
- **Datos iniciales** (los carga `veterinaria_db.sql`): lunes a viernes de 08:00 a 20:00, sábado de 08:00 a 18:00 y domingo de 09:00 a 14:00; los tipos Consulta, Cirugía y Estética; y los consultorios Consultorio 1, Consultorio 2, Quirófano y Sala de Estética.
- **Reglas que ya cuida la base:** cada día aparece una sola vez, el cierre es posterior a la apertura (también en días no laborables), el nombre de un tipo no se repite y todo consultorio tiene tipo.
- **Relación con el módulo de servicios:** cada servicio guarda el `idTipoConsultorio` que necesita y solo acepta tipos **activos**. Desactivar un tipo impide usarlo en servicios nuevos.
- **Desactivar en lugar de borrar:** los consultorios y tipos no se borran, porque las citas pasadas los siguen referenciando. Se desactivan para que no se usen en citas nuevas.

### Repositorios

| Repositorio | Búsquedas |
|---|---|
| `HorarioClinicaRepository` | Horario de un día (`findByDiaSemana`) y la semana completa de domingo a sábado |
| `TipoConsultorioRepository` | Todos o solo los activos (por nombre) y búsqueda por nombre sin importar mayúsculas |
| `ConsultorioRepository` | Todos o solo los activos, los de un tipo (todos o activos), búsqueda por nombre sin importar mayúsculas y cuántos consultorios activos tiene un tipo |

### Horario de la clínica

| Método y ruta | Quién | Qué hace | Respuesta |
|---|---|---|---|
| `GET /api/config/horarios-clinica` | Cualquier usuario con sesión | La semana completa, de domingo a sábado. El cliente también lo necesita para saber cuándo abre la clínica | `200` |
| `PUT /api/config/horarios-clinica` | ADMIN | Reemplaza el horario de **los 7 días** en una sola petición | `200` |

#### Modificar el horario

```http
PUT /api/config/horarios-clinica
Authorization: Bearer <token del ADMIN>
Content-Type: application/json
```

```json
[
  { "diaSemana": 0, "horaApertura": "09:00", "horaCierre": "14:00", "laborable": true },
  { "diaSemana": 1, "horaApertura": "08:00", "horaCierre": "20:00", "laborable": true },
  { "diaSemana": 2, "horaApertura": "08:00", "horaCierre": "20:00", "laborable": true },
  { "diaSemana": 3, "horaApertura": "08:00", "horaCierre": "20:00", "laborable": true },
  { "diaSemana": 4, "horaApertura": "08:00", "horaCierre": "20:00", "laborable": true },
  { "diaSemana": 5, "horaApertura": "08:00", "horaCierre": "20:00", "laborable": true },
  { "diaSemana": 6, "horaApertura": "08:00", "horaCierre": "18:00", "laborable": true }
]
```

La respuesta `200` (y el `GET`) devuelve la misma lista, y cada día trae además `nombreDia` ("Domingo", "Lunes"…) para que el frontend no tenga que convertir el número:

```json
{ "diaSemana": 6, "nombreDia": "Sábado", "horaApertura": "08:00", "horaCierre": "18:00", "laborable": true }
```

- `diaSemana`: `0` = domingo … `6` = sábado, igual que los turnos del personal.
- Las horas viajan como `"HH:mm"`. Si llegan segundos, se descartan.
- **Para el frontend:** se puede reenviar tal cual lo que devolvió el `GET` (el campo `nombreDia` se ignora). Para cerrar un día basta con poner `"laborable": false`, pero **hay que seguir enviando sus horas** (por ejemplo, las que ya tenía), porque la base exige un cierre posterior a la apertura aunque el día esté cerrado.

#### Reglas

| Regla | Si no se cumple |
|---|---|
| Deben venir **los 7 días**, cada uno una sola vez | `400` `Debes enviar el horario de los 7 días de la semana (0 = domingo … 6 = sábado), cada uno una sola vez` |
| `diaSemana` va de 0 a 6; `horaApertura`, `horaCierre` y `laborable` son obligatorios | `400` `Elemento 4 de la lista: El día de la semana debe estar entre 0 (domingo) y 6 (sábado)` (indica qué elemento falla) |
| `horaCierre` es posterior a `horaApertura`, **también en un día no laborable** | `400` `El día Lunes: la hora de cierre debe ser posterior a la hora de apertura` |
| La lista no es un arreglo JSON, o una hora no existe (`"25:00"`) | `400` |
| **Ningún turno del personal puede quedar fuera del nuevo horario** | `409`, **sin guardar nada** |

**Cuándo un turno queda fuera:** el día pasa a no laborable y alguien tiene turno ese día, o el turno empieza antes de la nueva apertura, o termina después del nuevo cierre. Un turno que coincide exactamente con la apertura y el cierre sí cabe. Se revisan los turnos de **todo** el personal, también de cuentas desactivadas, para que al reactivar a alguien su turno no quede fuera.

Ejemplo: el sábado pasa a cerrar a las 14:00 y Laura tiene turno de 10:00 a 18:00. Respuesta `409`:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "No se guardó el horario: 1 turno quedaría fuera del horario de la clínica: Laura Zamora, Sábado 10:00-18:00. Ajusta o borra esos turnos y vuelve a intentarlo."
}
```

- La operación es **todo o nada**: si hay conflicto, no cambia ni un día (aunque otros días del mismo `PUT` fueran válidos).
- El mensaje lista los primeros 10 turnos afectados y, si hay más, dice cuántos faltan (`; y 2 más`).
- Para resolverlo, el ADMIN ajusta o borra primero esos turnos (`PUT` o `DELETE /api/personal/{id}/horarios/{idHorario}`) y vuelve a guardar el horario.
- Guardar el mismo horario dos veces seguidas no da error.

> **Pendiente (siguiente parte):** validar también el sentido contrario: que al agregar o editar un turno del personal quede dentro del horario de la clínica.
> **Pendiente (Sprint 3):** qué hacer con las citas ya agendadas en horas que dejen de ser laborables.
