# API JWT: ejecución y pruebas

Este bloque corresponde al segundo commit de Juan Pablo. Agrega autenticación JWT,
dos cadenas de seguridad, Swagger y la colección inicial de Postman. El tercer
commit agrega el [CRUD de usuarios, roles, permisos y asociaciones](API_SEGURIDAD.md).

## Configurar y ejecutar

Usar Java 21, Maven y las variables Oracle descritas en README.
Configurar además JWT_SECRET: una clave aleatoria Base64 de al menos 32 bytes.
En PowerShell se puede generar una clave para la sesión actual así:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtGenerator.GetBytes($jwtBytes)
$jwtGenerator.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
mvn spring-boot:run
```

El servidor rechaza una clave ausente, inválida o demasiado corta. Para Tomcat,
configurar JWT_SECRET en el entorno del proceso junto con DB_URL, DB_USER y
DB_PASSWORD. Conservar la misma clave entre reinicios si se desea mantener los tokens.
Cambiar la clave invalida los tokens anteriores. No guardarla en Git.

JWT_EXPIRATION_MINUTES es opcional, vale 30 por defecto y admite de 1 a 1440 minutos.
No se implementan refresh tokens: al vencer, hay que iniciar sesión de nuevo.
El cierre de sesión MVC no revoca los tokens REST; el cliente debe descartarlos.
La cuenta y sus permisos se comprueban en la base de datos en cada petición.

## Endpoints disponibles

| Método y ruta | Acceso | Respuesta |
| --- | --- | --- |
| POST /api/v1/auth/login | Público | 200 con accessToken, tokenType, expiresAt, email y roles |
| GET /api/v1/auth/me | Bearer JWT | 200 con email, roles y permissions actuales |

Login recibe JSON:
```json
{"email": "usuario@ejemplo.com", "password": "contraseña de la cuenta"}
```

Usar una cuenta ACTIVE existente con su contraseña original. El registro público
continúa disponible en la pantalla MVC /register y asigna el rol OWNER.
Las respuestas y el JWT no incluyen contraseñas ni hashes.

Para consultar /me y los próximos CRUD, enviar:
```text
Authorization: Bearer <accessToken>
```

- 400: datos de entrada inválidos o JSON mal formado.
- 401: credenciales incorrectas/cuenta inactiva, token ausente, alterado o vencido.
- 403: usuario autenticado sin el permiso exigido.
- Las respuestas de seguridad son JSON; la API no redirige al formulario de login.

La firma usa HS256. El token contiene sub/email, roles, permissions, iss, iat y exp.
El filtro BearerTokenAuthenticationFilter de Spring Security verifica la firma,
el emisor y las fechas. La validación temporal estándar admite un desfase de reloj
de 60 segundos. Se exigen además sujeto, emisión y vencimiento.
AppUserDetailsService vuelve a cargar el estado y las autoridades del usuario:
retirar un permiso o desactivar una cuenta afecta a las siguientes peticiones,
aunque todavía exista un token emitido anteriormente.

## Separación de seguridad

ApiSecurityConfig, con prioridad 1, atiende /api/v1/**, sin sesión ni CSRF.
SecurityConfig, con prioridad 2, mantiene el login y CSRF del MVC.
Una cookie de sesión MVC no autentica la API y un token JWT no inicia sesión MVC.

Las próximas rutas /api/v1/users y /api/v1/user-roles exigen MANAGE_USERS;
roles, permissions y role-permissions exigen MANAGE_ROLES.
Otros endpoints requieren autenticación y deberán agregar sus reglas de propiedad
y negocio al implementarse. Los cinco controladores de seguridad ya existen bajo /api/v1.
Las rutas REST heredadas siguen funcionando con las reglas MVC durante esta etapa.

## Swagger

Abrir /swagger-ui/index.html; la definición está en /v3/api-docs.
Se documentan únicamente las rutas /api/v1/**.
Ejecutar login, copiar accessToken y pegarlo en Authorize, sin escribir el prefijo Bearer.
Login es público; /me y los futuros endpoints usan el esquema bearerAuth.

En Tomcat, anteponer el contexto: por ejemplo
http://servidor:8080/cuidapet/swagger-ui/index.html.

## Postman

Importar los dos JSON de docs/postman y seleccionar el entorno CuidaPet local.
Configurar localmente:
- baseUrl: http://localhost:8080, sin barra final; en Tomcat incluir /cuidapet.
- email y password: credenciales de una cuenta existente.
- token: lo asigna automáticamente el script del login.

Ejecutar la colección completa en orden: autenticación, permisos, roles, usuarios,
asociaciones y limpieza. El login limpia cualquier token anterior.
Los JSON versionados no contienen credenciales ni tokens reales.
El tercer commit amplía la colección a 44 peticiones para las cinco entidades de
seguridad; requiere una cuenta con MANAGE_USERS y MANAGE_ROLES. Cada integrante
añadirá sus carpetas CRUD al desarrollar su bloque. Todavía no cubre las 23 entidades.

## Pruebas automatizadas

```powershell
mvn "-Dtest=JwtValidationTest,ApiAuthenticationTest" test
mvn verify
```

ApiAuthenticationTest usa la configuración real de seguridad, firma JWT real y
usuarios simulados; necesita Oracle para cargar el contexto de la aplicación.
Comprueba login, errores, cambios de permisos, cuentas desactivadas, separación
MVC/API y la generación real de Swagger. JwtValidationTest no necesita Oracle:
comprueba vencimiento, firma, emisor, claims obligatorios y configuración inválida.
Las claves fijas de las pruebas se usan únicamente mediante propiedades de prueba;
no se incluyen como valor predeterminado de la aplicación.

Verificación del segundo bloque (5 de noviembre de 2026): `mvn -o verify`
terminó con BUILD SUCCESS, 241 pruebas, cero fallos, cero errores y cero omitidas.
Se generó `target/cuidapet.war`. Los JSON de Postman se validaron como archivos JSON;
la ejecución manual de la colección requiere configurar una cuenta local y arrancar
el servidor. El despliegue en IAsLab continúa pendiente.

Referencias:
- [JWT en Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
- [Springdoc y compatibilidad con Spring Boot](https://springdoc.org/).
