# Taller Backend: reparto y avance

## Alcance y estado inicial

Fuente: Taller Backend.pdf, entrega indicada: 8 de noviembre de 2026.
El alcance confirmado comprende **las 23 entidades** del proyecto.
Los archivos de claves compuestas (terminados en Id) no son entidades adicionales.

La base recibida (commit 94fd1ab) tiene modelos JPA y repositorios para las 23 entidades,
el módulo MVC de usuarios, roles y permisos, autenticación con sesión y empaquetado WAR.
Hay cinco servicios: AppUserService, AppRoleService, AppPermissionService, AuthService
y AppUserDetailsService. Los tres controladores REST existentes trabajan con entidades
y rutas heredadas; todavía necesitan adaptarse al nuevo contrato.
La existencia de un modelo o repositorio no significa que su API REST esté terminada.

El nuevo taller exige:
- CRUD REST para cada entidad: consultar todos, consultar por clave, crear, actualizar y eliminar.
- DTOs de entrada/salida y MapStruct con entityToDto y dtoToEntity.
- Validaciones y respuestas HTTP apropiadas.
- Autenticación JWT con información del usuario, roles, fecha de emisión y vencimiento.
- Filtro JWT y una cadena de seguridad para REST, manteniendo MVC/Thymeleaf.
- Colección Postman para todos los controladores, con variables y ejecución secuencial.
- Swagger para todos los métodos REST.
- Documentación de ejecución, pruebas, videos y despliegue real en Tomcat.

La rúbrica asigna 45% a REST, 30% a seguridad, 20% a Postman y 5% a Swagger;
el despliegue afecta la nota. Estas cifras son criterios de evaluación, no porcentajes
de avance por cantidad de commits.

## Reparto de nueve commits

Cada fila representa un commit funcional, revisable y probado. Juan Pablo realiza los
primeros tres; después Valentina y finalmente Gordillo. El reparto busca equilibrar
complejidad: Juan tiene menos entidades porque implementa también la seguridad y la
infraestructura compartida. No equivale a un 33% exacto por persona ni por contar commits.

| Orden | Responsable | Entrega |
| --- | --- | --- |
| 1 | Juan Pablo | Configurar MapStruct; DTOs y mappers de las cinco entidades de seguridad; pruebas de mapeo/validación; este plan. |
| 2 | Juan Pablo | Autenticación REST con JWT; filtro y dos cadenas de seguridad aisladas; pruebas de token, 401/403 y MVC; configuración Swagger; base de Postman y entorno. |
| 3 | Juan Pablo | CRUD REST de AppUser, AppRole, AppPermission, UserRole y RolePermission; errores comunes; autorizaciones; Swagger y pruebas/Postman del bloque. |
| 4 | Valentina | CRUD completo de UserProfile, PetSpecies y Pet. |
| 5 | Valentina | CRUD completo de PetPhoto, ServiceType y CaregiverProfile. |
| 6 | Valentina | CRUD completo de CaregiverAvailability, CaregiverService y FavoriteCaregiver. |
| 7 | Gordillo | CRUD completo de BookingStatus, CareRequest y CareApplication. |
| 8 | Gordillo | CRUD completo de CareBooking, CareUpdate y CareUpdatePhoto. |
| 9 | Gordillo | CRUD completo de AppNotification, IncidentReport y UserReview. |

**Completo**, en cada bloque de entidades, incluye DTOs, MapStruct, servicios, validación
de relaciones y reglas del negocio, permisos/propiedad, cinco operaciones REST, manejo
de errores, pruebas automatizadas, documentación Swagger y su carpeta Postman.
Las pruebas y documentación se hacen con cada bloque, no se dejan para el último integrante.

### Inventario y rutas propuestas

Las cinco rutas de Juan Pablo están implementadas desde su tercer commit.
Las rutas user-profiles, pet-species y pets están implementadas en el primer
bloque de Valentina. Su segundo bloque implementa pet-photos, service-types y
caregiver-profiles. El tercero completa caregiver-availabilities, caregiver-services
y favorite-caregivers. El primer bloque de Gordillo implementa booking-statuses,
care-requests y care-applications.
Las rutas existentes del MVC se conservan.

| Entidad | Responsable | Colección REST propuesta |
| --- | --- | --- |
| AppUser | Juan Pablo | /api/v1/users |
| AppRole | Juan Pablo | /api/v1/roles |
| AppPermission | Juan Pablo | /api/v1/permissions |
| UserRole | Juan Pablo | /api/v1/user-roles |
| RolePermission | Juan Pablo | /api/v1/role-permissions |
| UserProfile | Valentina | /api/v1/user-profiles |
| PetSpecies | Valentina | /api/v1/pet-species |
| Pet | Valentina | /api/v1/pets |
| PetPhoto | Valentina | /api/v1/pet-photos |
| ServiceType | Valentina | /api/v1/service-types |
| CaregiverProfile | Valentina | /api/v1/caregiver-profiles |
| CaregiverAvailability | Valentina | /api/v1/caregiver-availabilities |
| CaregiverService | Valentina | /api/v1/caregiver-services |
| FavoriteCaregiver | Valentina | /api/v1/favorite-caregivers |
| BookingStatus | Gordillo | /api/v1/booking-statuses |
| CareRequest | Gordillo | /api/v1/care-requests |
| CareApplication | Gordillo | /api/v1/care-applications |
| CareBooking | Gordillo | /api/v1/care-bookings |
| CareUpdate | Gordillo | /api/v1/care-updates |
| CareUpdatePhoto | Gordillo | /api/v1/care-update-photos |
| AppNotification | Gordillo | /api/v1/notifications |
| IncidentReport | Gordillo | /api/v1/incident-reports |
| UserReview | Gordillo | /api/v1/reviews |

## Acuerdos para los siguientes commits

- GET colección y GET /{id}: 200; POST: 201 con Location; PUT /{id}: 200;
  DELETE /{id}: 204. Errores: 400 datos inválidos, 401 sin autenticación válida,
  403 sin autorización, 404 recurso inexistente y 409 duplicado/conflicto de relaciones.
- Para claves compuestas, usar ambos identificadores en la ruta y documentarlos.
  UserRole y RolePermission también requieren las cinco operaciones. Su PUT debe
  sustituir la asociación de forma transaccional cuando cambien las claves, validando
  duplicados y mínimos de roles/permisos; no editar fechas de auditoría como contenido.
- Resolver los IDs relacionados mediante repositorios. Verificar existencia,
  propiedad y reglas antes de guardar. No permitir que cualquier usuario gestione
  roles, vea perfiles privados o modifique recursos ajenos por conocer su ID.
- Separar la cadena JWT sin sesión para /api/v1/** de la cadena MVC con sesión y CSRF.
  Probar que una sesión MVC sola no autentica la API y que se mantiene el login web.
  Revisar las rutas REST heredadas al migrarlas para evitar contratos duplicados.
- JWT no debe contener contraseñas ni hashes. La clave se configura por variable de
  entorno; validar firma y vencimiento. La autorización debe respetar roles, permisos,
  estado del usuario y reglas del negocio.
- Cada carpeta Postman debe guardar IDs reales y comprobar respuestas. Orden global:
  autenticación, permisos/roles/usuarios, catálogos/perfiles/mascotas/cuidadores,
  solicitudes/postulaciones/reservas, seguimiento/notificaciones/incidentes/reseñas.
  Borrar los datos creados en orden inverso cuando las reglas lo permitan.
  No incluir tokens, contraseñas reales ni secretos en Git.
- Las reglas particulares (fechas, estados, importes, disponibilidad, participantes)
  se revisan al implementar cada servicio, además de las restricciones del modelo.

## Qué incluye el primer commit de Juan Pablo

Implementado:
- MapStruct 1.6.3 y su procesador, con lombok-mapstruct-binding 0.2.0.
- ApiMapperConfig compartido: componentes Spring y error de compilación ante campos
  de destino no mapeados.
- 13 DTOs como records de Java, con validaciones Jakarta.
- Cinco mappers: AppUser, AppRole, AppPermission, UserRole y RolePermission.
- Pruebas de respuestas sin contraseñas, claves compuestas, valores iniciales,
  campos administrados por servidor y validaciones de solicitudes.

Los DTOs usan nombres de campos existentes (email, roleName, permissionCode).
UserCreateRequest recibe password y roleIds; RoleCreateRequest recibe permissionIds.
Los IDs de relaciones se entregan por separado al servicio, sin aceptar grafos JPA.

El mapper de usuario coloca la contraseña recibida en passwordHash del objeto de
entrada porque AppUserService ya la cifra. **El mapper no cifra ni guarda**: se debe
pasar siempre por el servicio, nunca guardar ese objeto directamente en el repositorio.
Al actualizar, password=null conserva la contraseña; una cadena vacía no es válida
en este contrato REST. La longitud mínima propuesta es de 8 caracteres; el servicio
también impone el límite de 72 bytes UTF-8 del codificador existente.

Los DTOs de respuesta nunca contienen password/passwordHash ni colecciones JPA.
Los datos de auditoría no se aceptan en solicitudes. El código de un permiso no
cambia al actualizarlo, conforme al servicio actual.

Los mappers de asociaciones construyen claves y referencias que contienen solo IDs.
Los servicios deberán reemplazar esas referencias por entidades existentes y
validadas antes de persistir. assignedBy se obtiene del usuario autenticado.
No se deben usar los mappers para saltarse validaciones del negocio.

Pendiente al terminar el primer commit: conectar estos DTOs a controladores REST,
JWT, nueva cadena, CRUD completo de las 23 entidades, Swagger, Postman,
validación de despliegue y videos. Este primer commit no cambia los endpoints ni el MVC.

## Qué incluye el segundo commit de Juan Pablo

- POST /api/v1/auth/login y GET /api/v1/auth/me.
- JWT firmado, con usuario, roles, permisos, emisión y vencimiento; clave por entorno.
- Filtro Bearer de Spring Security y cadena REST independiente de MVC.
- Respuestas JSON 401/403 y comprobación del estado/permisos actuales de la cuenta.
- Configuración Swagger y documentación de los dos endpoints.
- Colección y entorno Postman iniciales, sin credenciales reales.
- Pruebas de firma, vencimiento, login, autorizaciones y separación de las dos cadenas.

Consultar [API_JWT.md](API_JWT.md) para configurar JWT_SECRET, ejecutar y probar.
El tercer commit agregará los CRUD de las cinco entidades de seguridad; las otras
18 entidades siguen asignadas a Valentina y Gordillo. Swagger y Postman se ampliarán
con cada bloque. El despliegue y los videos siguen pendientes.

## Qué incluye el tercer commit de Juan Pablo

- Los cinco CRUD de seguridad: 25 operaciones REST con DTOs y MapStruct.
- Asociaciones con claves compuestas, auditoría del usuario autenticado y
  sustitución transaccional que conserva los mínimos de roles/permisos.
- Errores tipados 404/409, validación 400 y seguridad JWT 401/403.
- Swagger de todas las operaciones y colección Postman de 44 peticiones.
- Pruebas integradas sobre Oracle, con reversión/limpieza de datos de prueba.

La [guía del CRUD de seguridad](API_SEGURIDAD.md) explica contratos y ejecución.
Quedan las 18 entidades de Valentina/Gordillo, completar la colección global,
comprobar el despliegue y grabar los videos. Tener tres commits no equivale por sí
solo a un porcentaje exacto de la rúbrica.

## Primer commit de Valentina

Implementa UserProfile, PetSpecies y Pet: 15 operaciones, DTOs, MapStruct,
validaciones, control de acceso por propietario/ADMIN, pruebas integradas,
Swagger y ampliación de Postman a 69 peticiones. Consultar [VALENTINA_1.md](VALENTINA_1.md).
Quedan seis entidades de Valentina y nueve de Gordillo, además de finalizar
la entrega conjunta, el despliegue y los videos.

## Segundo commit de Valentina

Implementa PetPhoto, ServiceType y CaregiverProfile: 15 operaciones adicionales,
DTOs, MapStruct, permisos por propietario/ADMIN, validación del rol CAREGIVER,
elección de foto principal y controles de relaciones. Amplía Postman a
97 peticiones. Consultar [VALENTINA_2.md](VALENTINA_2.md).
Quedan tres entidades de Valentina y nueve de Gordillo.

## Tercer commit de Valentina

Implementa CaregiverAvailability, CaregiverService y FavoriteCaregiver: 15 operaciones,
DTOs, MapStruct, validaciones de horarios/tarifas, acceso por cuidador/ADMIN y
favoritos privados con reemplazo transaccional. Postman llega a 136 peticiones.
Consultar [VALENTINA_3.md](VALENTINA_3.md). Quedan completas las nueve entidades de
Valentina y pendientes las nueve de Gordillo; 14 de las 23 entidades tienen CRUD REST.
El despliegue, la ejecución conjunta de Postman y los videos siguen pendientes.

## Primer commit de Gordillo

Implementa BookingStatus, CareRequest y CareApplication: 15 operaciones, DTOs,
MapStruct, catálogo de estados administrado por ADMIN, solicitudes del propietario
con validación de mascota, servicio, fechas y presupuesto, y postulaciones de
cuidadores disponibles que ofrecen el servicio. Corrige el borrado de perfiles de
usuario (500 por la relación inversa con AppUser) y agrega el perfil Maven `h2`
para ejecutar las pruebas sin Oracle. Postman llega a 183 peticiones.
Consultar [GORDILLO_1.md](GORDILLO_1.md). Quedan seis entidades de Gordillo.

## Verificación de los bloques completados

Primer bloque de Gordillo, 7 de noviembre de 2026: `mvn -o -Ph2 verify` pasó con
291 pruebas, sin fallos, errores ni omisiones, y generó el WAR. La colección de
Postman se ejecutó completa con Newman: 183 peticiones, 219 aserciones, sin fallos.

Tercer bloque de Valentina, 7 de noviembre de 2026: `mvn -o verify` pasó con
284 pruebas, sin fallos, errores ni omisiones, y generó el WAR. Las 13 pruebas
nuevas cubren horarios, tarifas, privacidad, claves compuestas, favoritos,
perfiles/roles inactivos y Swagger. Se validaron JSON y scripts de las 136
peticiones de Postman; todavía no se ha ejecutado la colección completa.

Segundo bloque de Valentina, 6 de noviembre de 2026: `mvn -o verify` pasó
con 271 pruebas, sin fallos, errores ni omisiones, y generó el WAR.
Las 11 pruebas nuevas cubren los tres CRUD, foto principal, privacidad,
roles, conflictos de relaciones y Swagger.

Primer bloque de Valentina, 6 de noviembre de 2026: `mvn -o verify` pasó con
260 pruebas, sin fallos, errores ni omisiones. Se generó el WAR. Las 10 pruebas
nuevas verifican los CRUD, propiedad de recursos, validaciones y Swagger.

Tercer bloque, 6 de noviembre de 2026: `mvn -o verify` pasó con 250 pruebas,
sin fallos, errores ni omisiones, y generó el WAR. Las nueve pruebas nuevas
cubren los CRUD, errores, permisos, Swagger y la reversión de asociaciones.

Resultado del primer bloque, 5 de noviembre de 2026: `mvn -o verify` terminó con
BUILD SUCCESS, 225 pruebas ejecutadas, cero fallos, cero errores y cero omitidas
(12 pruebas nuevas). Se generó `target/cuidapet.war`. Esto verifica la compilación
y las pruebas locales; no acredita un despliegue en IAsLab.

Requisitos: JDK 21, Maven y Oracle con el esquema existente para las pruebas integradas.
Configurar DB_URL, DB_USER y DB_PASSWORD en el entorno según README; no guardarlas en Git.

Pruebas nuevas, sin Oracle:
```powershell
mvn "-Dtest=ApiMapperTest,ApiDtoValidationTest" test
```

Verificación completa y generación de target/cuidapet.war:
```powershell
mvn verify
```

Referencia de configuración: [documentación oficial de MapStruct](https://mapstruct.org/documentation/stable/reference/html/).

## Entrega compartida

Juan Pablo documenta autenticación, variables y contrato de errores; Valentina documenta
sus nueve entidades y prepara ejemplos de datos; Gordillo documenta las suyas y el flujo
de reservas. Cada integrante graba y explica su bloque. Entre los tres se ejecuta la
colección completa, se comprueba que MVC sigue funcionando y se despliega el WAR.
La versión de Java/Tomcat de IAsLab sigue pendiente de confirmar antes de desplegar.
No se considera cumplido el despliegue solo por generar un WAR.

Antes de cada commit: revisar git diff, ejecutar las pruebas del bloque y usar la
identidad real de quien trabaja.
