# Valentina: tercer bloque REST

Completa CaregiverAvailability, CaregiverService y FavoriteCaregiver. Incluye
15 operaciones REST, DTOs, MapStruct, reglas de acceso, Swagger y Postman.
Con este bloque quedan implementadas las nueve entidades asignadas a Valentina.

## Rutas

Todas requieren JWT. POST devuelve 201 con Location, GET y PUT devuelven 200,
DELETE devuelve 204. Los errores usan 400 (datos), 401 (token), 403 (permiso),
404 (inexistente) y 409 (duplicado o relación inactiva).

| Recurso | GET lista / POST | GET, PUT y DELETE individual |
| --- | --- | --- |
| Disponibilidad | /api/v1/caregiver-availabilities | /api/v1/caregiver-availabilities/{id} |
| Servicio ofrecido | /api/v1/caregiver-services | /api/v1/caregiver-services/{caregiverId}/{serviceTypeId} |
| Favorito | /api/v1/favorite-caregivers | /api/v1/favorite-caregivers/{ownerId}/{caregiverId} |

## Disponibilidad

Crear:

```json
{
  "caregiverId": 10,
  "dayOfWeek": 1,
  "startTime": "08:00",
  "endTime": "12:00",
  "validFrom": "2027-01-01",
  "validUntil": "2027-12-31"
}
```

Los IDs son ejemplos; usar los de la base. dayOfWeek va de 1 (lunes) a 7
(domingo). Las horas usan HH:mm, entre 00:00 y 23:59. La hora inicial debe ser
anterior a la final dentro del mismo día; no se admiten franjas que crucen
la medianoche. Las fechas son opcionales y, si se envían ambas,
validUntil no puede ser anterior a validFrom.

PUT recibe los mismos campos de horario y fechas, más activeFlag (Y/N),
sin caregiverId: no permite trasladar un horario a otra persona.
POST asigna activeFlag=Y. DELETE elimina la franja.

Se pueden registrar varias franjas por día. Este CRUD no detecta solapamientos
ni determina si una reserva cabe en un horario; esas validaciones corresponden
al flujo de solicitudes/reservas. GET devuelve las franjas configuradas,
incluidas las de vigencia pasada o futura, para que quien consulta interprete
validFrom/validUntil.

## Servicios ofrecidos

Crear:

```json
{"caregiverId":10,"serviceTypeId":3,"baseRate":25000.50}
```

El perfil debe estar disponible y el tipo de servicio activo. No puede repetirse
la pareja cuidador/tipo de servicio. La tarifa debe ser positiva, con máximo
10 dígitos enteros y 2 decimales.

PUT conserva ambos IDs y modifica la tarifa y el estado:

```json
{"baseRate":30000,"activeFlag":"Y"}
```

Para ofrecer un tipo diferente se crea otra asociación. Se puede desactivar
o eliminar un servicio propio aunque el catálogo o el perfil ya estén inactivos.
Para reactivarlo deben estar activos.

## Acceso a horarios y servicios

Solo el propio cuidador o ADMIN pueden escribir. Para crear o activar se exige
perfil activo, cuenta ACTIVE y rol CAREGIVER activo, consultados en la base.
Tener solo el nombre del rol en un token antiguo no habilita un perfil.

Otros usuarios autenticados pueden consultar horarios y servicios activos de
perfiles disponibles. Un servicio también requiere su tipo de catálogo activo.
GET lista filtra los recursos que no se pueden consultar; GET individual devuelve
403 cuando el recurso existe pero está oculto. El dueño y ADMIN pueden ver
sus recursos inactivos. No se devuelven credenciales ni objetos JPA anidados.

CaregiverAccess reúne estas verificaciones para los dos recursos. El servicio
de ofertas se llama CaregiverOfferingService para distinguirlo de la entidad
JPA CaregiverService.

## Favoritos

Crear:

```json
{"ownerId":20,"caregiverId":10}
```

La cuenta del propietario y el perfil elegido deben estar activos. Los favoritos
son privados: solo su propietario y ADMIN pueden leerlos o modificarlos.
La lista de un usuario nunca incluye los favoritos de otros.

PUT permite reemplazar el cuidador:

```json
{"caregiverId":11}
```

La ruta contiene el propietario y el cuidador anterior. El propietario no cambia.
Se valida el nuevo perfil y que no esté repetido, se inserta la nueva asociación
y se elimina la anterior en una sola transacción. Se conserva createdAt.
Si falla una validación, el favorito anterior permanece. Si se envía el mismo
cuidador disponible, se devuelve la asociación existente.

El esquema impide agregar el propio perfil a favoritos (ownerId = caregiverId);
la API valida esta regla y devuelve 409. Cuando un cuidador deja de estar
disponible, su favorito sigue visible para el propietario para que pueda
quitarlo; el DTO solo incluye IDs y fecha, no expone datos privados del perfil.

## Postman y Swagger

La colección completa contiene **136 peticiones en 16 carpetas**.
Las carpetas 13, 14 y 15 agregan horarios, servicios ofrecidos y favoritos.
La 16 elimina primero favoritos, horarios y ofertas antes de sus perfiles
y tipos de servicio. Se crean dos usuarios adicionales con perfil CAREGIVER para
probar el reemplazo de favoritos; ambos se eliminan lógicamente al final.

Importar la colección y el entorno local de docs/postman. Configurar baseUrl,
email y password de un ADMIN con MANAGE_USERS y MANAGE_ROLES. Deben existir
OWNER y CAREGIVER activos con permisos. Ejecutar toda la colección en orden,
incluida la limpieza. Los usuarios de prueba quedan DELETED por el borrado
lógico; no se eliminan cuentas ajenas. No guardar secretos en los archivos.

Swagger está en /swagger-ui/index.html y documenta las 15 operaciones nuevas.

## Pruebas

```powershell
.\mvnw.cmd test "-Dtest=CaregiverScheduleIntegrationTest"
.\mvnw.cmd verify
```

Requieren Java 21, Oracle y DB_PASSWORD, según DESPLIEGUE.md. Las 13 pruebas nuevas
usan repositorios/servicios reales y JWT firmado con identidades simuladas.
Sus datos se revierten al finalizar. Comprueban CRUD, claves inmutables,
horarios y tarifas inválidos, privacidad, perfiles/roles inactivos, duplicados,
reemplazo de favoritos, permisos de ADMIN y documentación de Swagger.

El JSON de Postman y la sintaxis de sus scripts fueron validados; la colección
no se ha ejecutado en Postman como parte de esta verificación.

Verificación del 7 de noviembre de 2026: `mvn -o verify` terminó con BUILD SUCCESS,
284 pruebas, cero fallos, cero errores y cero omitidas. Generó `target/cuidapet.war`.
Las 13 pruebas nuevas del bloque pasaron contra Oracle.

## Siguiente integrante

A Gordillo le corresponden BookingStatus, CareRequest y CareApplication en su
primer commit; CareBooking, CareUpdate y CareUpdatePhoto en el segundo;
AppNotification, IncidentReport y UserReview en el tercero. Siguen pendientes
la ejecución final conjunta de Postman, el despliegue real y los videos.
