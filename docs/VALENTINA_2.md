# Valentina: segundo bloque REST

Este commit implementa PetPhoto, ServiceType y CaregiverProfile: 15 operaciones
adicionales, nueve DTOs, tres mappers MapStruct, servicios, validaciones, Swagger,
Postman y pruebas integradas. No implementa todavía disponibilidad, servicios
ofrecidos por cuidadores ni favoritos: corresponden al tercer commit.

## Rutas y permisos

| Colección | Ruta individual | Acceso |
| --- | --- | --- |
| /api/v1/pet-photos | /api/v1/pet-photos/{id} | Propietario de la mascota o ADMIN |
| /api/v1/service-types | /api/v1/service-types/{id} | Lectura autenticada; escritura ADMIN |
| /api/v1/caregiver-profiles | /api/v1/caregiver-profiles/{id} | Directorio activo; escritura propia o ADMIN |

Cada colección tiene GET y POST; cada recurso tiene GET, PUT y DELETE.
GET/PUT devuelven 200, POST devuelve 201 con Location y DELETE devuelve 204.
Se mantienen 400 para entradas inválidas, 401 sin JWT válido, 403 sin autorización,
404 para recursos inexistentes y 409 para duplicados o relaciones que impiden la operación.

## Fotos de mascotas

POST /api/v1/pet-photos:
```json
{"petId":1,"photoUrl":"https://example.com/luna.jpg","mainPhotoFlag":"Y"}
```

PUT /api/v1/pet-photos/{id}:
```json
{"photoUrl":"https://example.com/luna-nueva.jpg","mainPhotoFlag":"N"}
```

Usar IDs reales. Se registra la URL, no se sube un archivo ni se descarga la imagen.
La URL debe comenzar por http:// o https://, no contener espacios y tener como
máximo 500 caracteres. mainPhotoFlag es obligatorio y admite Y o N.
uploadedAt se genera al crear; PUT no permite cambiar la mascota ni la fecha.

Una mascota no puede repetir la misma URL. Los listados devuelven solo las fotos
de las mascotas propias, salvo ADMIN. Leer o modificar fotos ajenas devuelve 403.

Elegir una foto principal (Y) desmarca las anteriores de esa mascota en la misma
transacción. Se bloquea la fila de la mascota durante las escrituras para coordinar
la elección de foto principal. Se permite no tener foto principal: borrar la
principal o cambiarla a N no elige automáticamente otra.

## Tipos de servicio

POST /api/v1/service-types:
```json
{"serviceName":"Paseo","description":"Paseo de mascotas"}
```

PUT /api/v1/service-types/{id}:
```json
{"serviceName":"Paseo","description":"Descripción actualizada","activeFlag":"Y"}
```

El nombre es obligatorio, se recorta y admite hasta 100 caracteres; la descripción
hasta 400. No se aceptan nombres repetidos aunque difieran en mayúsculas.
Los nuevos tipos empiezan activos (Y), createdAt lo genera el servidor y la
actualización permite activeFlag Y/N.

Solo ADMIN escribe; cualquier usuario autenticado consulta.
No se permite eliminar tipos referenciados por servicios de cuidadores o solicitudes:
se responde 409. Desactivar un tipo permite conservar sus relaciones.

## Perfiles de cuidadores

POST /api/v1/caregiver-profiles:
```json
{
  "caregiverId":1,
  "biography":"Tengo experiencia cuidando mascotas",
  "experienceYears":2,
  "serviceCity":"Cali",
  "serviceZone":"Sur"
}
```

PUT /api/v1/caregiver-profiles/{id}:
```json
{
  "biography":"Biografía actualizada",
  "experienceYears":3,
  "serviceCity":"Cali",
  "serviceZone":"Sur",
  "activeFlag":"Y"
}
```

El ID del perfil es el del usuario. El usuario debe existir, estar ACTIVE y tener
el rol CAREGIVER activo. Esto se comprueba en la base de datos, incluso si ADMIN
crea el perfil. El endpoint no concede roles: un administrador debe asignar
CAREGIVER mediante el bloque de seguridad si el usuario todavía no lo tiene.
No se admiten dos perfiles para el mismo cuidador.

La biografía es obligatoria (máximo 1200 caracteres), experienceYears es obligatorio
y va de 0 a 999, serviceCity es obligatorio (máximo 100) y serviceZone es opcional
(máximo 160). Los nuevos perfiles empiezan en Y y las fechas las genera el servidor.

PUT no cambia el usuario del perfil. Los datos opcionales omitidos quedan null.
Para mantener o activar un perfil en Y, el usuario y su rol CAREGIVER deben seguir
activos. Se permite desactivar el perfil propio aunque se haya retirado ese rol.

El directorio para usuarios autenticados muestra perfiles activos de usuarios
activos con CAREGIVER activo, más el perfil propio aunque esté inactivo.
ADMIN ve todos. La consulta individual aplica la misma regla; nadie puede editar
ni eliminar un perfil ajeno salvo ADMIN. El DTO solo contiene datos profesionales,
sin correo, contraseña ni datos privados del perfil de usuario.

DELETE elimina el perfil, no la cuenta. Las postulaciones, reservas o actualizaciones
que lo referencien pueden impedir el borrado (409).
El esquema elimina en cascada sus servicios, disponibilidades y favoritos.
Para conservar todas las relaciones, desactivar con activeFlag=N en lugar de borrar.

## Postman y Swagger

Swagger: /swagger-ui/index.html. Se documentan las 15 operaciones nuevas.

La colección completa contiene 97 peticiones en 13 carpetas:
- Las carpetas 10, 11 y 12 cubren fotos, tipos de servicio y cuidadores.
- La selección de la foto principal se comprueba leyendo la foto anterior.
- El perfil de cuidador se crea con ownerToken, después de que ADMIN asigne
  CAREGIVER al usuario de prueba.
- La carpeta final limpia primero fotos y perfil de cuidador, retira el rol
  CAREGIVER de prueba y elimina el tipo de servicio; luego limpia los bloques previos.

Importar también el entorno y configurar localmente baseUrl, email y password.
La cuenta de ejecución requiere ADMIN, MANAGE_USERS y MANAGE_ROLES.
Deben existir OWNER y CAREGIVER activos con permisos. El script inicial guarda
sus IDs; la colección no modifica sus definiciones ni crea roles del sistema.
No guardar credenciales, tokens ni claves JWT reales en Git.

## Pruebas

```powershell
mvn "-Dtest=CaregiverPhotoIntegrationTest" test
mvn verify
```

Necesitan Oracle y DB_PASSWORD. Las pruebas usan servicios/repositorios reales,
JWT firmado e identidades simuladas; todos sus datos se revierten al terminar.
Cubren los CRUD, auditoría, foto principal, URLs duplicadas/inválidas, privacidad,
permisos de catálogo, conflictos de servicio en uso, rol CAREGIVER,
experiencia inválida, directorio activo y generación de Swagger.

A Valentina le queda su tercer bloque: CaregiverAvailability, CaregiverService y
FavoriteCaregiver, con pruebas, Swagger y Postman. El despliegue y los videos
siguen formando parte de la entrega compartida.

Verificación del 6 de noviembre de 2026: `mvn -o verify` terminó con BUILD SUCCESS,
271 pruebas, cero fallos, cero errores y cero omitidas. Se generó
`target/cuidapet.war`. Las 11 pruebas nuevas del bloque pasaron contra Oracle.
Se validaron el JSON y la sintaxis de los scripts de las 97 peticiones de Postman;
la colección no se ejecutó desde Postman en esta verificación.
