# Gordillo: tercer bloque REST y cierre de la entrega

Completa AppNotification, IncidentReport y UserReview: 15 operaciones REST con DTOs,
MapStruct, validaciones, permisos con JWT, Swagger, Postman y pruebas. Con este bloque
las **23 entidades** tienen CRUD REST: 115 operaciones, más la autenticación.

## Rutas

Todas requieren JWT. POST devuelve 201 con Location, GET y PUT devuelven 200 y
DELETE devuelve 204. Los errores usan 400, 401, 403, 404 y 409 como en los demás bloques.

| Recurso | GET lista / POST | GET, PUT y DELETE individual |
| --- | --- | --- |
| Notificación | /api/v1/notifications | /api/v1/notifications/{id} |
| Reporte de incidente | /api/v1/incident-reports | /api/v1/incident-reports/{id} |
| Reseña | /api/v1/reviews | /api/v1/reviews/{id} |

## Notificaciones

El flujo de cuidado genera notificaciones automáticas: BOOKING_CREATED para el
cuidador aceptado, BOOKING_STATUS para la otra parte en cada cambio de estado,
CARE_UPDATE para el propietario por cada novedad, INCIDENT_STATUS para quien reportó
cuando ADMIN cambia el estado y REVIEW_RECEIVED para la persona reseñada. Solo se
crean para cuentas ACTIVE.

ADMIN también puede enviar una notificación manual:

```json
{"userId":10,"notificationType":"BOOKING_REMINDER","title":"Recordatorio",
 "messageText":"Revisa tu reserva de mañana","referenceEntity":"CARE_BOOKING","referenceId":30}
```

El tipo y la entidad de referencia usan mayúsculas y guion bajo; referenceEntity y
referenceId se envían juntos o ninguno. El destinatario debe existir (404) y estar
ACTIVE (409). Cada usuario ve solo sus notificaciones (ADMIN ve todas), ordenadas de
la más reciente a la más antigua.

PUT recibe `readFlag` (Y/N) y, opcionalmente, `title` y `messageText`. El destinatario
solo marca como leída o no leída (se registra o borra readAt); cambiar el contenido
es exclusivo de ADMIN (403). DELETE corresponde al destinatario o ADMIN.

## Reportes de incidentes

```json
{"reporterId":10,"reportedUserId":11,"requestId":15,"bookingId":30,
 "reportReason":"Llegó tarde","reportDetails":"El cuidador llegó una hora tarde",
 "evidenceUrl":"https://evidencias.example.com/1.jpg"}
```

Quien reporta es el usuario autenticado (o ADMIN en su nombre). No se puede reportar
a uno mismo. bookingId es opcional; si se envía debe pertenecer a la solicitud. Ambas
personas deben participar en el cuidado: con reserva, propietario y cuidador de la
reserva; sin reserva, el propietario de la solicitud y los cuidadores que se postularon.
El servidor asigna reportStatus=OPEN.

PUT recibe motivo, detalles, evidencia, reportStatus (OPEN, IN_REVIEW, RESOLVED,
DISMISSED) y resolutionNotes. Quien reporta solo edita el texto mientras el reporte
sigue OPEN; revisar y resolver es exclusivo de ADMIN. RESOLVED y DISMISSED exigen notas
y registran resolvedBy y resolvedAt. El reporte lo consultan quien lo creó y ADMIN; la
persona reportada no lo ve. Quien reporta puede retirarlo solo mientras está OPEN.

## Reseñas

```json
{"bookingId":30,"reviewerId":10,"rating":5,"reviewComment":"Excelente cuidado"}
```

Solo se reseñan reservas COMPLETED y solo sus participantes: el propietario califica
al cuidador y el cuidador al propietario; el servidor deduce reviewedUserId. Cada
participante reseña una vez cada reserva (409). rating va de 1 a 5. Las reseñas son
públicas para usuarios autenticados porque forman la reputación. PUT cambia
calificación y comentario; PUT y DELETE corresponden al autor o ADMIN.

## Postman

La colección final tiene **282 peticiones en 25 carpetas** y cubre las 23 entidades.
Las carpetas 22 a 24 comprueban las notificaciones automáticas generadas por los
bloques anteriores, envían y leen una notificación manual, recorren el ciclo de un
reporte hasta su resolución y registran las reseñas de la reserva completada.
La carpeta 25 limpia en orden inverso: reseñas, reportes, notificaciones, reservas,
novedades, fotos, postulaciones, solicitudes y luego los datos de los bloques de
Juan Pablo y Valentina.

Para ejecutarla: importar `docs/postman/CuidaPet.postman_collection.json` y el entorno
`CuidaPet-local.postman_environment.json`; configurar baseUrl, email y password de una
cuenta ADMIN con MANAGE_USERS y MANAGE_ROLES; ejecutar la colección completa en orden
con el Collection Runner. Los datos de prueba usan un runId único, por lo que la
colección puede ejecutarse varias veces sobre la misma base.

## Verificación de la entrega conjunta

7 de noviembre de 2026:

- `mvn -o -Ph2 verify`: BUILD SUCCESS, 302 pruebas, cero fallos, errores u omisiones;
  generó `target/cuidapet.war`.
- Newman ejecutó la colección completa contra la aplicación (base H2 en modo Oracle
  cargada con `02_datos_iniciales.sql` y una cuenta ADMIN): 282 peticiones y
  361 aserciones sin fallos. Una segunda ejecución sobre la misma base también pasó.
- Swagger (/v3/api-docs) documenta 115 operaciones CRUD sobre 23 recursos, todas con
  descripción, además de los dos endpoints de autenticación.
- El módulo MVC sigue funcionando: sus pruebas pasan, /login responde 200 y las
  rutas protegidas sin sesión redirigen al login.

Pendiente de la entrega conjunta: desplegar el WAR en el Tomcat de IAsLab con
`SPRING_PROFILES_ACTIVE=iaslab`, `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET`,
comprobar allí la aplicación y grabar los videos de cada integrante.
