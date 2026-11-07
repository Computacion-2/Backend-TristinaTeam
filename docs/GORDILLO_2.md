# Gordillo: segundo bloque REST

Completa CareBooking, CareUpdate y CareUpdatePhoto: 15 operaciones REST con DTOs,
MapStruct, validaciones, permisos con JWT, Swagger, Postman y pruebas. Continúa el
flujo del primer bloque: el propietario acepta una postulación creando la reserva,
el cuidador registra novedades con fotos y ambas partes cierran el servicio.

## Rutas

Todas requieren JWT. POST devuelve 201 con Location, GET y PUT devuelven 200 y
DELETE devuelve 204. Los errores usan 400, 401, 403, 404 y 409 como en los demás bloques.

| Recurso | GET lista / POST | GET, PUT y DELETE individual |
| --- | --- | --- |
| Reserva | /api/v1/care-bookings | /api/v1/care-bookings/{id} |
| Novedad del cuidado | /api/v1/care-updates | /api/v1/care-updates/{id} |
| Foto de una novedad | /api/v1/care-update-photos | /api/v1/care-update-photos/{id} |

Solo los participantes (propietario y cuidador de la reserva) y ADMIN consultan
reservas, novedades y fotos; GET lista filtra el resto.

## Reservas

Crear (acepta una postulación):

```json
{"applicationId":21,"exactAddress":"Calle 5 # 10-20"}
```

Solo el propietario de la solicitud o ADMIN. La postulación debe estar PENDING,
la solicitud OPEN, sin reserva previa y sin haber comenzado, y el cuidador debe
seguir disponible. El servidor copia solicitud, propietario, cuidador y tarifa
(agreedRate = proposedRate) y asigna SCHEDULED. En la misma transacción la
postulación pasa a ACCEPTED, las demás pendientes a REJECTED ("Se aceptó otra
postulación"), la solicitud a ASSIGNED y se notifica al cuidador.

PUT recibe bookingStatusCode, exactAddress y cancellationReason:

```json
{"bookingStatusCode":"IN_PROGRESS","exactAddress":"Calle 5 # 10-20","cancellationReason":null}
```

| Transición | Quién | Efecto |
| --- | --- | --- |
| SCHEDULED a IN_PROGRESS | Cuidador | startedAt |
| IN_PROGRESS a PENDING_CONFIRMATION | Cuidador | caregiverDoneFlag = Y |
| PENDING_CONFIRMATION a COMPLETED | Propietario | ownerConfirmedFlag = Y, completedAt, solicitud CLOSED |
| Cualquier estado activo a CANCELLED | Propietario o cuidador | motivo obligatorio, cancelledAt, solicitud CANCELLED |

ADMIN puede ejecutar cualquiera. Otras transiciones dan 409, igual que modificar una
reserva COMPLETED o CANCELLED. Solo el propietario cambia la dirección y solo antes
de iniciar. Cada cambio de estado notifica a la otra parte. Los indicadores y fechas
los fija el servidor; no se aceptan en el DTO.

DELETE es exclusivo de ADMIN. Si la solicitud seguía ASSIGNED vuelve a OPEN y la
postulación a PENDING; la base elimina en cascada novedades, fotos y reseñas.

## Novedades y fotos

```json
{"bookingId":30,"updateMessage":"Llegamos al parque"}
{"updateId":40,"photoUrl":"https://fotos.example.com/parque.jpg"}
```

Las registra el cuidador de la reserva (o ADMIN) y solo con la reserva IN_PROGRESS
o PENDING_CONFIRMATION. El cuidador se toma de la reserva; el propietario recibe una
notificación por cada novedad. PUT cambia el mensaje o la URL con las mismas reglas.
Las fotos usan URL http(s) de hasta 500 caracteres, no se repiten dentro de la misma
novedad y una novedad admite como máximo 10. Eliminar una novedad elimina sus fotos.

## Postman y Swagger

La colección pasa a **235 peticiones en 22 carpetas**. La 19 crea la reserva desde la
postulación del primer bloque y recorre la máquina de estados con sus 403/409. La 20
registra, edita y elimina novedades y fotos, y termina la reserva como COMPLETED
(base de las reseñas del tercer bloque). La 21 crea una tercera solicitud, la reserva
y la cancela. La limpieza elimina primero las reservas, novedades y fotos.

Swagger documenta las 15 operaciones nuevas.

## Pruebas

```powershell
.\mvnw.cmd -Ph2 test "-Dtest=CareBookingTrackingIntegrationTest"
```

Las 6 pruebas nuevas cubren la aceptación y sus efectos (postulaciones, solicitud,
notificación), la máquina de estados por participante, la dirección y la cancelación,
novedades y fotos con sus límites y permisos, el borrado de ADMIN que reabre la
solicitud, y Swagger.

Verificación del 7 de noviembre de 2026 con `mvn -o -Ph2 verify`: BUILD SUCCESS,
297 pruebas, cero fallos, cero errores y cero omitidas. La colección completa se
ejecutó con Newman: 235 peticiones y 293 aserciones, cero fallos.
