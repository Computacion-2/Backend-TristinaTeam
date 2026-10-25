# Módulo MVC de CuidaPet

CuidaPet incluye un módulo de administración de usuarios, roles y permisos con Spring Boot, Thymeleaf y Spring Security. Sus integrantes son Juan Pablo Pino, Valentina Martínez y Juan Gordillo.

## Arquitectura

Las peticiones pasan por Spring Security y llegan a controladores con `@Controller`. Los controladores reciben los formularios y llaman a los servicios; estos aplican las reglas de negocio y acceden a Oracle mediante repositorios JPA.

```text
Navegador → Spring Security → Controller → Service → Repository → Oracle
Controller → Model → Thymeleaf → HTML
```

Los GET cargan datos en `Model` y devuelven una plantilla. Los POST llaman al servicio y redirigen a un GET, con mensajes de éxito o error mediante `RedirectAttributes`. Las vistas usan `/css/styles.css` y `th:action` para enviar los formularios con CSRF.

La API REST se conserva en `/users`, `/api/roles` y `/api/permissions`, con las mismas restricciones que las pantallas. Sus rutas están en [API.md](API.md).

## Autenticación y autorización

`AuthController` muestra login y registro. `AuthService` registra cuentas ACTIVE con OWNER; el visitante no puede escoger ADMIN ni cambiar su estado. `AppUserService` cifra las contraseñas con el `PasswordEncoder` BCrypt de `PasswordConfig`. El registro y los formularios MVC de usuarios solicitan al menos 8 caracteres, con un máximo de 72 bytes UTF-8 para BCrypt.

`AppUserDetailsService` implementa `UserDetailsService`. `loadUserByUsername` busca por correo, recupera los roles activos y sus permisos y los convierte en `GrantedAuthority`. Los roles reciben el prefijo `ROLE_` y los permisos conservan su código. Las cuentas distintas de ACTIVE no pueden iniciar sesión. Los cambios de roles y permisos se reflejan al cerrar y volver a iniciar sesión.

`SecurityConfig` define la `SecurityFilterChain`:

| Rutas | Acceso |
|---|---|
| `/login`, `/register`, `/css/**`, `/error` | Público |
| `/usuarios`, `/usuarios/**`, `/users`, `/users/**` | MANAGE_USERS |
| `/roles`, `/roles/**`, `/permisos`, `/permisos/**` | MANAGE_ROLES |
| `/api/roles`, `/api/roles/**`, `/api/permissions`, `/api/permissions/**` | MANAGE_ROLES |
| Resto | Sesión iniciada |

CSRF permanece habilitado, incluso en login y registro. El cierre de sesión usa `POST /logout`. El inicio muestra la cuenta, sus autoridades y los enlaces de administración según sus permisos. Ocultar enlaces complementa las restricciones del servidor.

## Gestión de usuarios

`UserMvcController` utiliza `AppUserService` para listar, crear, editar y eliminar usuarios. Los correos son únicos y la eliminación es lógica mediante el estado DELETED. Al editar, una contraseña vacía conserva la actual; una nueva se cifra antes de guardar. Las vistas no muestran `passwordHash` y la API lo excluye de sus respuestas.

| Operación | Ruta |
|---|---|
| Listar | `GET /usuarios` |
| Formulario de creación | `GET /usuarios/nuevo` |
| Crear | `POST /usuarios/crear` |
| Consultar y guardar edición | `GET/POST /usuarios/{id}/editar` |
| Eliminar lógicamente | `POST /usuarios/{id}/eliminar` |
| Consultar roles | `GET /usuarios/{id}/roles` |
| Asignar rol | `POST /usuarios/{id}/roles/asignar`, parámetro `roleId` |
| Quitar rol | `POST /usuarios/{id}/roles/{roleId}/quitar` |

Cada usuario debe tener al menos un rol. Al crear se seleccionan los roles iniciales; en la pantalla de roles se consultan y cambian las asignaciones. No se permiten asignaciones duplicadas, roles sin permisos ni quitar el último rol, aunque se envíe el POST directamente.

## Gestión de roles y permisos

`RoleMvcController` y `PermissionMvcController` delegan en `AppRoleService` y `AppPermissionService`. Los tres servicios de gestión ofrecen `findAll`, `findById`, `create`, `update` y `delete`.

| Operación | Ruta |
|---|---|
| Listar roles | `GET /roles` |
| Formulario de rol | `GET /roles/nuevo` |
| Crear rol con permisos | `POST /roles/crear` |
| Consultar y guardar edición de rol | `GET/POST /roles/{id}/editar` |
| Eliminar rol | `POST /roles/{id}/eliminar` |
| Consultar permisos del rol | `GET /roles/{id}/permisos` |
| Asignar permiso | `POST /roles/{id}/permisos/asignar`, parámetro `permissionId` |
| Quitar permiso | `POST /roles/{id}/permisos/{permissionId}/quitar` |
| Listar permisos | `GET /permisos` |
| Formulario de permiso | `GET /permisos/nuevo` |
| Crear permiso | `POST /permisos/crear` |
| Consultar y guardar edición de permiso | `GET/POST /permisos/{id}/editar` |
| Eliminar permiso | `POST /permisos/{id}/eliminar` |

Un rol se crea con al menos un permiso. Los nombres de roles y códigos de permisos se normalizan a mayúsculas, empiezan por una letra y admiten letras, números y guion bajo. No se repiten nombres de roles, códigos ni nombres de permisos. El código de un permiso no cambia al editarlo.

No se puede quitar el último permiso ni eliminar un permiso si eso deja un rol sin permisos. Eliminar un rol exige que no esté asignado a usuarios. ADMIN y OWNER no se pueden renombrar, desactivar ni eliminar. ADMIN conserva MANAGE_USERS y MANAGE_ROLES; estos permisos tampoco se pueden eliminar.

## Ejecución y cuenta de administración

Usar Java 21, preparar Oracle con `01_creacion.sql` y después `02_datos_iniciales.sql` y definir las variables de conexión descritas en [DESPLIEGUE.md](DESPLIEGUE.md). Desde la raíz:

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir `http://localhost:8080/register`. Los usuarios del script tienen hashes ficticios para los datos de ejemplo; las cuentas para iniciar sesión se crean desde el registro.

Para preparar la primera cuenta de administración, registrar una cuenta propia. Desde una conexión autorizada al esquema, ejecutar esta consulta sustituyendo el correo por el registrado:

```sql
INSERT INTO USER_ROLE (USER_ID, ROLE_ID, ASSIGNED_AT)
SELECT u.USER_ID, r.ROLE_ID, SYSTIMESTAMP
FROM APP_USER u CROSS JOIN APP_ROLE r
WHERE u.EMAIL = 'correo-registrado@icesi.edu.co'
  AND r.ROLE_NAME = 'ADMIN'
  AND r.ACTIVE_FLAG = 'Y'
  AND NOT EXISTS (
      SELECT 1 FROM USER_ROLE ur
      WHERE ur.USER_ID = u.USER_ID AND ur.ROLE_ID = r.ROLE_ID
  );
COMMIT;
```

Cerrar sesión y volver a entrar. ADMIN recibe MANAGE_USERS y MANAGE_ROLES de los datos iniciales. Las demás asignaciones se administran desde las pantallas.

## Pruebas y demostración

La suite incluye `AppUserServiceTest`, `AppRoleServiceTest`, `AppPermissionServiceTest`, `AuthServiceTest` y `AppUserDetailsServiceTest`. Comprueba CRUD, asignaciones, duplicados, contraseñas y autoridades. Las pruebas MVC y de seguridad verifican plantillas, mensajes, redirecciones, ausencia de hashes en las respuestas, permisos y rechazo de POST sin CSRF. `CuidaPetProyectApplicationTests` valida el arranque con Oracle.

Con la conexión Oracle configurada:

```powershell
.\mvnw.cmd clean verify
```

Maven genera `target/cuidapet.war` y `target/site/jacoco/index.html`. El empaquetado WAR se conserva como configuración del proyecto; esta entrega no exige desplegarlo físicamente en IAsLab.

Recorrido de demostración:

1. Sin sesión, abrir una ruta privada y comprobar la redirección al login. Registrar una cuenta y verificar en Oracle que PASSWORD_HASH contiene BCrypt.
2. Iniciar sesión como OWNER y comprobar que no puede acceder a `/usuarios`, `/roles` ni `/permisos`. Probar también credenciales incorrectas y una cuenta inactiva.
3. Entrar con permisos administrativos. En `/usuarios`, crear y editar una cuenta; asignarle un segundo rol, quitarlo y comprobar que no se puede quitar el último. Eliminar una cuenta de prueba y comprobar el estado DELETED.
4. En `/permisos`, crear y editar permisos de prueba. En `/roles`, crear un rol con un permiso, editarlo, asignarle otro permiso y quitarlo. Comprobar que no se puede retirar el último.
5. Intentar eliminar un rol asignado: debe rechazarse. Quitar su asignación conservando otro rol para el usuario y eliminarlo. Eliminar el permiso de prueba cuando ningún rol quede sin permisos.
6. Comprobar que ADMIN conserva sus permisos esenciales. Cerrar sesión y abrir otra ruta protegida: debe solicitar login nuevamente.
