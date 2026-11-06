# Valentina: primer bloque REST

Este commit implementa UserProfile, PetSpecies y Pet. Añade 15 operaciones REST,
DTOs validados, tres mappers MapStruct, servicios, Swagger y pruebas/Postman.
No incluye todavía fotos, tipos de servicio ni perfiles de cuidadores.

## Rutas y permisos

| Colección | Ruta individual | Acceso |
| --- | --- | --- |
| /api/v1/user-profiles | /api/v1/user-profiles/{id} | Propietario del perfil o rol ADMIN |
| /api/v1/pet-species | /api/v1/pet-species/{id} | Lectura autenticada; escritura solo ADMIN |
| /api/v1/pets | /api/v1/pets/{id} | Propietario de la mascota o rol ADMIN |

En perfiles, id es el identificador de AppUser; no se genera otro ID.
Cada colección admite GET y POST; cada ruta individual GET, PUT y DELETE.
GET y PUT devuelven 200; POST devuelve 201 con Location y DTO; DELETE devuelve 204.
Errores: 400 datos inválidos, 401 token inválido/ausente, 403 acceso ajeno o rol
insuficiente, 404 recurso inexistente, 409 duplicado/conflicto de relaciones.

Los listados de perfiles y mascotas devuelven únicamente recursos propios para
usuarios sin ADMIN. ADMIN puede consultar todos y operar en nombre de un usuario.
El rol ADMIN se obtiene de las autoridades actuales cargadas durante la validación
JWT. MANAGE_USERS por sí solo no concede acceso administrativo a este bloque.

ResourceAccess concentra estas comprobaciones para poder reutilizarlas en los
siguientes bloques. ApiExceptionHandler devuelve 403 cuando un servicio deniega
acceso; las comprobaciones de propiedad no dependen de que el cliente envíe un email.

## Perfiles de usuario

POST /api/v1/user-profiles:
```json
{
  "userId": 1,
  "fullName": "Persona de ejemplo",
  "photoUrl": "https://example.com/perfil.jpg",
  "phoneNumber": "3001234567",
  "city": "Cali",
  "academicProgram": "Ingeniería",
  "emergencyContact": "Contacto de ejemplo",
  "emergencyPhone": "3007654321"
}
```

Usar IDs reales. El usuario debe existir y estar ACTIVE; solo puede tener un perfil.
PUT recibe los mismos campos descriptivos, sin userId. El ID de la URL se conserva.
fullName es obligatorio; los demás campos admiten null y respetan las longitudes
de Oracle. profileUpdatedAt lo genera el servidor.

DELETE elimina únicamente el perfil; no elimina la cuenta de usuario.
La respuesta incluye userId y los campos del perfil, sin entidades ni contraseñas.

## Especies

POST /api/v1/pet-species:
```json
{"speciesName": "Conejo"}
```

PUT /api/v1/pet-species/{id}:
```json
{"speciesName": "Conejo", "activeFlag": "Y"}
```

Solo ADMIN puede crear, actualizar o eliminar. El nombre se recorta y no puede
duplicarse aunque cambie entre mayúsculas y minúsculas. activeFlag admite Y o N;
las creaciones empiezan en Y. Una especie utilizada por mascotas no se puede
eliminar: devuelve 409. Se puede desactivar para impedir nuevas asignaciones.

## Mascotas

POST /api/v1/pets:
```json
{
  "ownerId": 1,
  "speciesId": 1,
  "petName": "Luna",
  "breed": "Mestiza",
  "birthDate": "2024-01-10",
  "sex": "FEMALE",
  "weightKg": 4.25,
  "temperament": "Tranquila",
  "careInstructions": "Paseo diario",
  "medicalAlerts": "Ninguna"
}
```

El propietario debe existir y estar ACTIVE; debe ser el usuario autenticado,
salvo que quien opera tenga ADMIN. La especie debe existir y estar activa.

PUT recibe los campos anteriores sin ownerId, y exige petStatus
(ACTIVE, ARCHIVED o DELETED). No transfiere la mascota a otra persona.
Si se cambia la especie, la nueva debe estar activa. Si la especie original fue
desactivada, aún se permite editar la mascota conservando esa misma especie.

La fecha de nacimiento no puede ser futura. sex admite FEMALE, MALE, UNKNOWN
o null. weightKg admite null o un valor mayor que cero, con hasta cuatro cifras
enteras y dos decimales. Los textos respetan las longitudes del esquema.
La creación usa ACTIVE y el servidor genera createdAt/updatedAt.

PUT reemplaza los datos editables: enviar los valores opcionales que se deseen
conservar, pues los omitidos quedan null. Esto también aplica a los perfiles.

DELETE elimina físicamente la mascota. Las solicitudes que la referencien impiden
el borrado y generan 409. El esquema existente elimina sus fotos por ON DELETE CASCADE.
Cambiar petStatus a ARCHIVED permite conservarla sin borrarla.
Las respuestas contienen ownerId y speciesId, no los objetos JPA completos.

## Swagger, Postman y pruebas

Swagger muestra las 15 operaciones nuevas en /swagger-ui/index.html.
Importar la colección y el entorno de docs/postman. Para ejecutar todo se necesita
una cuenta ACTIVE con rol ADMIN y permisos MANAGE_USERS/MANAGE_ROLES.

Al cerrar este primer bloque, la colección sumaba 69 peticiones. El
[segundo bloque](VALENTINA_2.md) la amplía a 97. El recorrido del primer bloque es:
1. Los bloques de Juan Pablo crean permisos, roles y un usuario de prueba.
2. Las carpetas 07, 08 y 09 crean y editan perfil, especie y mascota.
3. El login del propietario guarda ownerToken por separado de token (administrador).
4. Se verifica el filtrado por propietario y que OWNER reciba 403 al crear especies.
5. La carpeta final de limpieza elimina mascota, perfil y especie; luego limpia los recursos
   del bloque anterior. El usuario queda DELETED según la regla del módulo de usuarios.

Los IDs se guardan automáticamente y cada ejecución usa nombres/correo distintos.
No exportar credenciales ni tokens reales. Si se interrumpe el flujo, revisar sus
variables antes de limpiar. Se mantienen pendientes las entidades de los próximos bloques.

Pruebas integradas (necesitan Oracle y DB_PASSWORD):
```powershell
mvn "-Dtest=ProfilePetIntegrationTest" test
mvn verify
```

Las pruebas usan repositorios/servicios y JWT reales, simulan la identidad autenticada
y revierten todos sus datos al finalizar. Cubren los tres CRUD, propiedad en lectura
y escritura, filtrado de listados, límites de campos, especies inactivas/duplicadas,
relaciones inexistentes, claves compartidas de perfiles y documentación Swagger.

## Siguiente trabajo de Valentina

Verificación del 6 de noviembre de 2026: `mvn -o verify` terminó con BUILD SUCCESS,
260 pruebas, cero fallos, cero errores y cero omitidas, y generó
`target/cuidapet.war`. Las 10 pruebas nuevas del bloque pasaron contra Oracle.
Se validaron el JSON y la sintaxis de los scripts de las 69 peticiones Postman;
la colección no se ejecutó desde Postman en esta verificación.

- Segundo commit: PetPhoto, ServiceType y CaregiverProfile.
- Tercer commit: CaregiverAvailability, CaregiverService y FavoriteCaregiver.

Cada uno debe incluir sus DTOs, mappers, reglas, autorización, pruebas, Swagger y Postman.
