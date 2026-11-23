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
- Usuario administrador: `admin@veterinaria.com`

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

Para probar endpoints protegidos, usa el botón **Authorize** y pega el token JWT obtenido en el login (sin el prefijo `Bearer`).

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
