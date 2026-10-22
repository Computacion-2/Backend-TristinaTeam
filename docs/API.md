# CuidaPet – Documentación de la API REST

## 1. Descripción general

Desde la primera etapa MVC, todas las rutas de esta API requieren sesión iniciada mediante `/login`. Usuarios exige `MANAGE_USERS`; roles y permisos exigen `MANAGE_ROLES`. Las operaciones POST también requieren el token CSRF de la misma sesión (encabezado `X-CSRF-TOKEN` para JSON). Los formularios Thymeleaf lo envían automáticamente con `th:action`.

El campo `passwordHash` se conserva por compatibilidad en las peticiones de usuarios, pero ahora se envía la contraseña nueva sin codificar y el servicio guarda BCrypt. En una actualización, omitirlo o enviarlo vacío conserva la contraseña actual. Nunca enviar un hash como si fuera la contraseña nueva.

El backend de CuidaPet está desarrollado con Spring Boot, Spring Data JPA y Oracle. Permite administrar usuarios, roles y permisos, respetando las relaciones entre estas entidades y las reglas de negocio implementadas en los servicios.

La API utiliza los métodos HTTP `GET` para las consultas y `POST` para las operaciones de creación, actualización, eliminación y asignación de relaciones.

**Dirección local:** `http://localhost:8080`

Las peticiones que contienen información deben enviarse en formato JSON, utilizando el encabezado `Content-Type: application/json`.

## 2. Endpoints de permisos

Controlador: `AppPermissionController`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/permissions` | Consultar todos los permisos. |
| GET | `/permissions/{id}` | Consultar un permiso por su identificador. |
| POST | `/permissions/create` | Crear un permiso. |
| POST | `/permissions/update/{id}` | Actualizar un permiso. |
| POST | `/permissions/delete/{id}` | Eliminar un permiso. |

### Crear un permiso

**Petición:** `POST /permissions/create`

```json
{
  "permissionCode": "CREATE_REQUEST",
  "permissionName": "Crear solicitudes",
  "description": "Permite crear solicitudes de cuidado"
}
```

El código y el nombre son obligatorios. El identificador y la fecha de creación se gestionan mediante la aplicación y Oracle.

### Actualizar un permiso

**Petición:** `POST /permissions/update/1`

```json
{
  "permissionName": "Permiso actualizado",
  "description": "Descripción actualizada"
}
```

El servicio actualiza el nombre y la descripción; el código del permiso no se modifica mediante esta operación.

## 3. Endpoints de roles

Controlador: `AppRoleController`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/roles` | Consultar todos los roles. |
| GET | `/roles/{id}` | Consultar un rol por identificador. |
| POST | `/roles/create?permissionIds=1,2` | Crear un rol con permisos. |
| POST | `/roles/update/{id}` | Actualizar los datos de un rol. |
| POST | `/roles/delete/{id}` | Eliminar un rol. |
| POST | `/roles/{roleId}/assign-permission/{permissionId}` | Asignar un permiso a un rol. |
| POST | `/roles/{roleId}/remove-permission/{permissionId}` | Retirar un permiso de un rol. |

### Crear un rol

**Petición:** `POST /roles/create?permissionIds=1,2`

```json
{
  "roleName": "CAREGIVER",
  "description": "Cuidador de mascotas",
  "activeFlag": "Y"
}
```

El parámetro `permissionIds` contiene los identificadores de los permisos que se asignarán al nuevo rol.

**Reglas de negocio:**
- Un rol debe tener al menos un permiso.
- No se permite retirar el último permiso de un rol.
- Los permisos que se asignan deben existir.

### Asignar un permiso

**Petición:** `POST /roles/1/assign-permission/3`

Asigna el permiso con ID 3 al rol con ID 1.

### Retirar un permiso

**Petición:** `POST /roles/1/remove-permission/3`

Retira el permiso indicado, siempre que el rol conserve al menos otro permiso.

## 4. Endpoints de usuarios

Controlador: `AppUserController`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/users` | Consultar todos los usuarios. |
| GET | `/users/{id}` | Consultar un usuario por identificador. |
| POST | `/users/create?roleIds=1,2` | Crear un usuario con roles. |
| POST | `/users/update/{id}` | Actualizar un usuario. |
| POST | `/users/delete/{id}` | Eliminar lógicamente un usuario. |
| POST | `/users/{userId}/assign-role/{roleId}` | Asignar un rol a un usuario. |
| POST | `/users/{userId}/remove-role/{roleId}` | Retirar un rol de un usuario. |

### Crear un usuario

**Petición:** `POST /users/create?roleIds=1`

```json
{
  "email": "usuario@cuidapet.test",
  "passwordHash": "HASH_GENERADO_PREVIAMENTE",
  "accountStatus": "ACTIVE"
}
```

El parámetro `roleIds` indica los roles que se asignarán al usuario.

El campo `passwordHash` debe contener un hash seguro generado previamente. El endpoint no debe utilizarse para recibir contraseñas en texto plano.

### Actualizar un usuario

**Petición:** `POST /users/update/1`

```json
{
  "email": "usuario.actualizado@cuidapet.test",
  "accountStatus": "ACTIVE"
}
```

Los campos efectivamente modificados dependen de la implementación de `AppUserService.update()`.

### Eliminar un usuario

**Petición:** `POST /users/delete/1`

La eliminación es lógica. El registro permanece en Oracle, pero su estado cambia a `DELETED`.

**Reglas de negocio:**
- Un usuario debe tener al menos un rol.
- No se permite retirar el último rol.
- Los roles que se asignan deben existir.

## 5. Códigos HTTP

| Código | Significado |
|---|---|
| 200 | La operación se ejecutó correctamente. |
| 400 | La petición contiene datos inválidos, un JSON incorrecto o carece de un parámetro obligatorio. |
| 404 | No se encontró el registro solicitado. |
| 409 | La operación incumple una regla de negocio o entra en conflicto con datos existentes. |
| 500 | Ocurrió un error interno no controlado. |

Ejemplo de respuesta de error:

```json
{
  "error": "Usuario no encontrado"
}
```

Las excepciones se gestionan mediante `GlobalExceptionHandler`.

## 6. Seguridad y serialización JSON

El atributo `passwordHash` está configurado para no aparecer en las respuestas JSON.

Las relaciones entre usuarios, roles y permisos utilizan anotaciones de Jackson para evitar referencias circulares durante la serialización.

Estas medidas no sustituyen la autenticación, la autorización ni el procesamiento seguro de contraseñas.

## 7. Pruebas automatizadas

El proyecto cuenta con pruebas unitarias realizadas mediante JUnit y Mockito, además de pruebas HTTP con MockMvc.

Las pruebas principales están organizadas en las siguientes clases:

- `AppPermissionServiceTest`
- `AppRoleServiceTest`
- `AppUserServiceTest`
- `AppPermissionControllerTest`
- `AppRoleControllerTest`
- `AppUserControllerTest`
- `GlobalExceptionHandlerTest`
- `AppPermissionControllerHttpTest`
- `AppRoleControllerHttpTest`
- `AppUserControllerHttpTest`

Para ejecutar todas las pruebas y generar el reporte de JaCoCo:

```powershell
.\mvnw.cmd clean verify
```

El reporte se genera en:

```text
target/site/jacoco/index.html
```

El objetivo de cobertura del proyecto es mantener el 100 % de instrucciones y ramas en los tres servicios.

## 8. Configuración de Oracle y datos iniciales

La aplicación utiliza Oracle XE y se conecta a la base de datos mediante la configuración de Spring Boot.

El proyecto incluye el archivo:

```text
src/main/resources/sql/02_datos_iniciales.sql
```

Este script carga los permisos, roles, usuarios de prueba, especies de mascotas y tipos de servicios, además de establecer las relaciones correspondientes.

Debe ejecutarse después del script de creación del esquema, desde una conexión de Oracle SQL Developer con el usuario de la aplicación.

Los registros de usuarios del script tienen hashes ficticios y solo sirven para verificar la persistencia y las relaciones entre las entidades.


## Video de funcionamiento
[Ver demostración de CuidaPet](https://drive.google.com/file/d/1CuUJEpwXpszjL59R-BCdbdwqss7HutJX/view?usp=sharing)
