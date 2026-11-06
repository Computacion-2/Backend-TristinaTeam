# CRUD REST del bloque de seguridad

Tercer commit de Juan Pablo. Implementa las cinco entidades de seguridad bajo
/api/v1, reutilizando los DTOs y MapStruct del primer commit y JWT del segundo.

## Operaciones

Todas las rutas requieren Authorization: Bearer <token>.

| Colección | Ruta individual | Permiso |
| --- | --- | --- |
| /api/v1/users | /api/v1/users/{id} | MANAGE_USERS |
| /api/v1/roles | /api/v1/roles/{id} | MANAGE_ROLES |
| /api/v1/permissions | /api/v1/permissions/{id} | MANAGE_ROLES |
| /api/v1/user-roles | /api/v1/user-roles/{userId}/{roleId} | MANAGE_USERS |
| /api/v1/role-permissions | /api/v1/role-permissions/{roleId}/{permissionId} | MANAGE_ROLES |

Cada colección tiene GET (listar) y POST (crear). Cada ruta individual tiene
GET (consultar), PUT (actualizar) y DELETE (eliminar): **25 operaciones en total**.
Las respuestas son DTOs; nunca se devuelve passwordHash ni un grafo de entidades JPA.

- GET y PUT: 200 con el DTO o la lista.
- POST: 201 con el DTO y Location, incluyendo el contexto de Tomcat.
- DELETE: 204 sin cuerpo.
- 400: JSON, parámetros o campos inválidos.
- 401: token ausente, inválido o usuario no disponible.
- 403: falta el permiso correspondiente.
- 404: recurso o relación referenciada inexistente.
- 409: duplicado, regla del negocio o restricción de integridad.

Los servicios usan ResourceNotFoundException y ConflictException para distinguir
404 y 409 sin depender del texto del mensaje. ApiExceptionHandler cubre errores
de la API nueva, incluidos conflictos de integridad de Oracle. Las validaciones
de DTO y JSON conservan el manejador común existente.

## Cuerpos de solicitudes

### Usuarios

POST /api/v1/users:
```json
{"email":"persona@ejemplo.com","password":"ClaveEjemplo123","roleIds":[1]}
```

PUT /api/v1/users/{id}:
```json
{"email":"persona@ejemplo.com","password":null,"accountStatus":"ACTIVE"}
```

Los IDs de los ejemplos deben sustituirse por IDs reales. password=null conserva
la contraseña; una contraseña nueva se cifra en AppUserService.
La creación exige al menos un rol con permisos. Los estados admitidos son ACTIVE,
INACTIVE, SUSPENDED y DELETED. DELETE es lógico: cambia el estado a DELETED y conserva
las relaciones; las consultas administrativas siguen mostrando el registro.
Eliminarlo de nuevo responde 409. El estado DELETED impide usar su JWT.

### Roles

POST /api/v1/roles:
```json
{"roleName":"SUPPORT","description":"Soporte","permissionIds":[1]}
```

PUT /api/v1/roles/{id}:
```json
{"roleName":"SUPPORT","description":"Descripción nueva","activeFlag":"Y"}
```

Los roles necesitan permisos. ADMIN y OWNER no pueden eliminarse, renombrarse
ni desactivarse. Un rol asignado a usuarios no se puede eliminar. Los nombres
se normalizan a mayúsculas según el servicio existente.

### Permisos

POST /api/v1/permissions:
```json
{"permissionCode":"READ_REPORTS","permissionName":"Consultar reportes","description":"Lectura"}
```

PUT /api/v1/permissions/{id}:
```json
{"permissionName":"Ver reportes","description":"Descripción nueva"}
```

El código no cambia al actualizar. Los permisos de sistema MANAGE_USERS y MANAGE_ROLES
están protegidos. No se permite eliminar el último permiso de un rol.

### Asociaciones

POST /api/v1/user-roles:
```json
{"userId":1,"roleId":2}
```

POST /api/v1/role-permissions:
```json
{"roleId":2,"permissionId":3}
```

Los servicios resuelven los IDs a entidades existentes. assignedBy se obtiene del
usuario autenticado y las fechas de auditoría se generan en el servidor.

PUT usa el mismo cuerpo de POST. La URL contiene la clave anterior y el cuerpo
la nueva. Si ambos son iguales, devuelve la asociación existente sin cambiar
su auditoría. Si cambian, se crea la nueva y se retira la anterior en una misma
transacción. Si cualquier regla falla, se revierte toda la sustitución.
Esto permite reemplazar el único rol de un usuario o el único permiso de un rol,
siempre que el resultado conserve las reglas. Cambiar también de usuario/rol padre
no permite dejar al anterior sin asociaciones.

No se admiten duplicados, roles inactivos en nuevas asignaciones de usuario,
usuarios sin roles, roles sin permisos ni retirar a ADMIN sus permisos de administración.
Las reglas anteriores se reutilizan al eliminar o sustituir asociaciones.

## Swagger y Postman

Swagger: /swagger-ui/index.html. Documenta las 25 operaciones nuevas y las dos
de autenticación, con Bearer como esquema de seguridad.

La colección docs/postman/CuidaPet.postman_collection.json tiene 44 peticiones
en siete carpetas. Importar también el entorno local y configurar baseUrl, email
y password. Se necesita una cuenta ACTIVE con MANAGE_USERS y MANAGE_ROLES,
y el rol OWNER activo con permisos. No usar una cuenta OWNER para ejecutar los CRUD
administrativos: recibiría 403.

Ejecutar toda la colección en orden. El inicio crea un identificador de ejecución
y limpia las variables anteriores. El login guarda token; las creaciones guardan
los IDs reales y las siguientes peticiones los reutilizan.
Se comprueban respuestas correctas y errores 400, 401, 404 y 409.
Las pruebas automatizadas comprueban también 403.

La última carpeta retira las asociaciones de prueba y elimina roles/permisos creados.
El usuario queda DELETED y conserva OWNER por la regla de borrado lógico.
La colección no modifica roles o permisos del sistema. Si se interrumpe a mitad,
revisar los IDs guardados antes de limpiar datos; una ejecución nueva usa nombres
y correo distintos. No exportar credenciales ni tokens reales al repositorio.

## Pruebas

```powershell
mvn "-Dtest=ApiCrudIntegrationTest" test
mvn verify
```

ApiCrudIntegrationTest usa Oracle y los servicios/repositorios reales; simula solamente
la identidad que autentica JWT. Comprueba el recorrido CRUD de cada entidad, auditoría,
errores, permisos y los métodos publicados en Swagger. Las pruebas normales revierten
sus datos. La prueba de reversión de asociaciones usa transacciones independientes
y elimina al finalizar los datos de prueba que creó.

Las rutas REST heredadas (/users, /api/roles y /api/permissions) conservan su contrato
y seguridad con sesión/CSRF por compatibilidad. El taller nuevo, Swagger y Postman
usan exclusivamente /api/v1. Las vistas MVC siguen usando los mismos servicios.

## Trabajo que continúa

Verificación del 6 de noviembre de 2026: `mvn -o verify` terminó con BUILD SUCCESS,
250 pruebas, cero fallos, cero errores y cero omitidas. Se generó
`target/cuidapet.war`. También se validaron el JSON y la sintaxis de los scripts
de las 44 peticiones Postman; la colección no se ejecutó desde Postman en esta
verificación. El despliegue en IAsLab sigue pendiente.

Los tres commits de Juan Pablo cubren las cinco entidades de seguridad y la base JWT.
No completan el taller de 23 entidades. Valentina continúa con sus nueve entidades;
después Gordillo con las nueve restantes. Cada bloque debe ampliar Swagger, Postman
y las pruebas. El despliegue real y los videos siguen pendientes.
