# Gordillo: primer bloque REST

Completa BookingStatus, CareRequest y CareApplication: 15 operaciones REST con DTOs,
MapStruct, validaciones del negocio, permisos con JWT, Swagger, Postman y pruebas.
Es el inicio del flujo de cuidado: el propietario publica una solicitud para su
mascota y los cuidadores se postulan.

## Rutas

Todas requieren JWT. POST devuelve 201 con Location, GET y PUT devuelven 200 y
DELETE devuelve 204. Los errores usan 400 (datos), 401 (token), 403 (permiso),
404 (inexistente) y 409 (duplicado, estado o relación que impide la operación).

| Recurso | GET lista / POST | GET, PUT y DELETE individual |
| --- | --- | --- |
| Estado de reserva | /api/v1/booking-statuses | /api/v1/booking-statuses/{code} |
| Solicitud de cuidado | /api/v1/care-requests | /api/v1/care-requests/{id} |
| Postulación | /api/v1/care-applications | /api/v1/care-applications/{id} |

## Estados de reserva

Es el catálogo de estados de CareBooking. Su clave es el código, restringido por
el modelo a SCHEDULED, IN_PROGRESS, PENDING_CONFIRMATION, COMPLETED y CANCELLED.
Cualquier usuario autenticado puede consultarlo; crear, renombrar y eliminar
requiere ADMIN.

```json
{"statusCode":"CANCELLED","statusName":"Cancelada"}
```

PUT solo cambia el nombre (`{"statusName":"Cancelada por las partes"}`); el código
no se edita porque lo referencian las reservas. Un código fuera del modelo da 400,
un código o nombre repetido da 409 y no se elimina un estado usado por reservas (409).

## Solicitudes de cuidado

Crear:

```json
{
  "ownerId": 10,
  "petId": 4,
  "serviceTypeId": 2,
  "startAt": "2027-01-15T08:00:00",
  "endAt": "2027-01-15T12:00:00",
  "city": "Cali",
  "zoneName": "Sur",
  "addressReference": "Cerca al parque",
  "requestNotes": "Paseo de la tarde",
  "maxBudget": 50000
}
```

Los IDs son ejemplos. Solo el propio propietario o ADMIN crean solicitudes. La cuenta
debe estar ACTIVE, la mascota debe pertenecer al propietario y estar ACTIVE, y el tipo
de servicio debe estar activo. Las fechas deben ser futuras y endAt posterior a startAt.
maxBudget es opcional, positivo y con máximo 10 enteros y 2 decimales. El servidor
asigna requestStatus=OPEN y las fechas de auditoría.

PUT recibe mascota, servicio, fechas, ubicación, notas, presupuesto, requestStatus
y cancellationReason, sin ownerId. Solo se modifican solicitudes OPEN; si cambian las
fechas, la nueva fecha inicial debe ser futura. requestStatus admite OPEN, CANCELLED
y CLOSED: ASSIGNED lo asigna el servidor al crear la reserva. Cancelar exige motivo.
Al cancelar o cerrar, las postulaciones pendientes pasan a REJECTED con la razón.

Visibilidad: ADMIN ve todas; el propietario ve las suyas; un cuidador disponible
(perfil activo, cuenta ACTIVE y rol CAREGIVER activo) ve las solicitudes abiertas y
las que tienen una postulación suya. DELETE corresponde al propietario o ADMIN,
elimina sus postulaciones y se rechaza si ya existe una reserva.

## Postulaciones

Crear:

```json
{"requestId":15,"caregiverId":11,"proposedRate":45000,"applicationMessage":"Disponible"}
```

Solo el propio cuidador o ADMIN. Se valida que el cuidador esté disponible, que la
solicitud siga OPEN y no haya comenzado, que el cuidador no sea el propietario, que
ofrezca activamente ese tipo de servicio (CaregiverService) y que la tarifa no supere
maxBudget. No se permite una segunda postulación del mismo cuidador a la misma
solicitud (409). El servidor asigna applicationStatus=PENDING.

PUT recibe proposedRate, applicationMessage, applicationStatus y responseReason:

- El cuidador (o ADMIN) cambia tarifa y mensaje con PENDING, o retira con WITHDRAWN.
- El propietario de la solicitud (o ADMIN) rechaza con REJECTED y responseReason;
  la tarifa enviada se ignora. El cuidador no puede rechazar (403).
- ACCEPTED no se envía: se asigna al crear la reserva en el segundo bloque.
- Una postulación que ya no está PENDING no se modifica (409).

La consultan el cuidador, el propietario de la solicitud y ADMIN. DELETE corresponde
al cuidador o ADMIN y no aplica a una postulación aceptada.

## Postman y Swagger

La colección pasa a **183 peticiones en 19 carpetas**. La 16 recorre el catálogo de
estados (renombra y restaura CANCELLED, lo elimina y vuelve a crear, y prueba 400,
403 y 409). La 17 crea dos solicitudes con fechas calculadas en el pre-request y
prueba fechas, presupuesto, propiedad e inexistencia. La 18 inicia sesión con el
segundo cuidador, le agrega el servicio solicitado y prueba postulación, duplicado,
presupuesto, rechazo del propietario, retiro, cancelación y sus restricciones.
La limpieza elimina primero postulaciones, solicitudes y el servicio agregado.

La eliminación y recreación de CANCELLED supone que ninguna reserva de la base
usa ese estado; en una base con reservas canceladas esa petición responde 409.

Swagger (/swagger-ui/index.html) documenta las 15 operaciones nuevas.

## Pruebas

```powershell
.\mvnw.cmd test "-Dtest=CareRequestApplicationIntegrationTest"
.\mvnw.cmd -Ph2 test "-Dtest=CareRequestApplicationIntegrationTest"
```

La primera línea usa Oracle como el resto del proyecto. La segunda usa el perfil
Maven `h2`, una base en memoria en modo Oracle que Hibernate crea en cada ejecución:
sirve para ejecutar todas las pruebas sin Oracle ni DB_PASSWORD (`.\mvnw.cmd -Ph2 verify`).

Las 6 pruebas nuevas (clase base CareFixture, datos revertidos al terminar) cubren
el catálogo de estados, el CRUD y la visibilidad de solicitudes, las validaciones de
fechas, presupuesto, mascota y estado, el ciclo de postulaciones y sus permisos,
el cierre de solicitudes y la documentación de Swagger.

Se agregó además una prueba de regresión a ProfilePetIntegrationTest: eliminar un
perfil de usuario cuando la cuenta está cargada con su perfil devolvía 500
(TransientPropertyValueException). UserProfileService ahora desvincula el perfil
antes de borrarlo.

Verificación del 7 de noviembre de 2026 con `mvn -o -Ph2 verify`: BUILD SUCCESS,
291 pruebas, cero fallos, cero errores y cero omitidas. La colección completa de
Postman se ejecutó con Newman contra la aplicación con datos iniciales:
183 peticiones y 219 aserciones, cero fallos.
