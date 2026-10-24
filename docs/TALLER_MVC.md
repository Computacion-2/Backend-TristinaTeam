# Continuación del taller MVC

## Reparto y orden de trabajo

El taller anterior ya implementa los CRUD y pruebas de usuarios, roles y permisos. No hay que volver a escribirlos. Cada integrante debe desarrollar, probar y explicar su parte. Tres commits sirven para organizar el trabajo; el equilibrio se evalúa por funcionalidades y esfuerzo, no por número de archivos.

| Orden | Integrante | Commit | Alcance |
|---|---|---|---|
| 1 | Juan | 1. Autenticación y contraseñas seguras | Dependencias, BCrypt, registro con OWNER, UserDetailsService y pruebas de servicios. |
| 1 | Juan | 2. Reglas de acceso | SecurityFilterChain: login/registro públicos, módulos protegidos por permisos, CSRF y pruebas de autorización. |
| 1 | Juan | 3. Vistas de autenticación | Controlador MVC, login, registro, inicio, CSS, pruebas de formularios y esta guía. |
| 2 | Valentina | 1. Gestión MVC de usuarios | Crear UserMvcController en /usuarios y vistas usuarios/lista.html y usuarios/formulario.html. Listar, crear, editar y eliminar lógicamente; reutilizar AppUserService. Añadir pruebas. |
| 2 | Valentina | 2. Roles de un usuario | Vista usuarios/roles.html para consultar, asignar y quitar roles; usar assignRole/removeRole. Probar duplicados, usuario inexistente y último rol. Añadir enlace de usuarios al inicio solo si tiene MANAGE_USERS. |
| 2 | Valentina | 3. Ejecución y despliegue | Preparar WAR para la versión real de Tomcat de IAsLab, configuración de conexión por variables y guía de ejecución/despliegue. Revisar pruebas de AppUserService y documentar demostración de usuarios. El despliegue definitivo se repite al integrar a Gordillo. |
| 3 | Gordillo | 1. Gestión MVC de permisos | PermissionMvcController en /permisos, listado y formulario con crear/editar/eliminar. Reutilizar AppPermissionService y probar las reglas de permisos en uso. |
| 3 | Gordillo | 2. Gestión MVC de roles | RoleMvcController en /roles: listar, crear con permisos, editar y eliminar. Mover la API REST anterior de /roles a /api/roles y actualizar sus pruebas y documentación. Añadir /api/roles y /api/roles/** a la regla MANAGE_ROLES de SecurityConfig antes de moverla. |
| 3 | Gordillo | 3. Permisos de un rol e integración | Vista roles/permisos.html para asignar y quitar permisos; pruebas, enlaces de roles/permisos según MANAGE_ROLES, documentación de pantallas y verificación final con los tres integrantes. |

La carga de Juan es mayor en seguridad; Valentina combina usuarios con preparación del despliegue; Gordillo concentra roles y permisos. Cada integrante añade sus pruebas en cada commit, en lugar de dejarle todas las pruebas a una persona.

## Contrato para continuar sin conflictos

- Mantener las entidades, repositorios y servicios del taller anterior. Los controladores MVC nuevos llevan @Controller; la API anterior usa @RestController.
- GET carga datos en Model y devuelve el nombre de la plantilla. POST llama al servicio y devuelve redirect:, incluso si ocurre un error de validación; usar RedirectAttributes para el mensaje.
- Todos los formularios POST usan th:action, incluyendo eliminar, asignar, quitar y cerrar sesión. No desactivar CSRF.
- Todas las vistas usan /css/styles.css. Pueden ampliar este archivo para tablas y formularios.
- /users y /usuarios requieren MANAGE_USERS. /roles, /permissions y /permisos requieren MANAGE_ROLES, incluyendo subrutas. Si agregan otra ruta administrativa, añadir su regla explícita y una prueba de rechazo sin permiso.
- Para ocultar un enlace, enviar al Model un booleano obtenido de Authentication.getAuthorities(); usar th:if. Esto complementa las reglas del servidor, nunca las reemplaza.
- La API de usuarios mantiene passwordHash como nombre del campo por compatibilidad, pero recibe la contraseña nueva sin codificar; AppUserService aplica BCrypt. En update, null o vacío conserva la contraseña existente. No enviar de vuelta el hash ni precodificarlo.
- El registro público no recibe IDs de roles ni estado. Siempre crea una cuenta ACTIVE con OWNER. No permitir autoasignarse ADMIN.
- Los roles inactivos no aportan autoridades. Los usuarios que no estén ACTIVE no pueden iniciar sesión.
- Los cambios de roles/permisos se reflejan al cerrar y volver a iniciar sesión: las autoridades se cargan durante el login.
- Las reglas previas se mantienen: al menos un rol por usuario y al menos un permiso por rol. Mostrar errores de negocio en la vista; no eliminar estas validaciones para facilitar un formulario.
- No añadir enlaces a pantallas aún sin implementar. El inicio actual muestra la cuenta y sus autoridades; cada compañero agrega sus enlaces cuando tenga listas las vistas.

## Cómo ejecutar la parte de Juan

1. Usar Java 21. Si Maven utiliza otra versión, ajustar JAVA_HOME en esa terminal. Comprobar con mvn -version.
2. Tener Oracle disponible y ejecutar los scripts existentes 01_creacion.sql y 02_datos_iniciales.sql en el orden indicado en README. Revisarlos antes de ejecutarlos sobre una base con datos que se deban conservar.
3. Verificar la conexión en application.properties. Deben existir OWNER activo y sus permisos iniciales. Los usuarios del script tienen contraseñas de ejemplo que no sirven para iniciar sesión.
4. Ejecutar .\mvnw.cmd spring-boot:run (o mvn spring-boot:run si Maven ya está instalado).
5. Abrir http://localhost:8080/register. Registrar un correo nuevo y contraseña de al menos 8 caracteres (hasta 72 bytes UTF-8 por el límite de BCrypt).
6. Iniciar sesión en /login. Se mostrará la cuenta y ROLE_OWNER con sus permisos. Cerrar sesión desde el botón.

### Si Maven muestra release version 21 not supported

Este error indica que Maven está usando un JDK anterior a Java 21. En PowerShell, desde la carpeta del proyecto, seleccionar el JDK 21 instalado y comprobar la versión que realmente usa Maven:

```powershell
# Ruta comprobada en el equipo de Juan; en otro equipo ajustar a su instalación.
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\mvnw.cmd -version
```

La salida debe indicar Java 21. Luego ejecutar ` .\mvnw.cmd spring-boot:run ` en la misma terminal. El cambio de JAVA_HOME solo aplica a esa sesión de PowerShell; al abrir otra terminal hay que repetirlo si la configuración general sigue apuntando a otro JDK.

## Preparar una cuenta de administración para la demostración

Registrar primero una cuenta propia mediante /register. Luego, el responsable de la base de datos puede asignarle el rol ADMIN con esta consulta, reemplazando el correo por el que registró. No existe una contraseña de administrador incorporada en el código y esta consulta no modifica contraseñas.

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

Los datos iniciales de ADMIN incluyen MANAGE_USERS y MANAGE_ROLES. Cerrar sesión e iniciar de nuevo después de asignar el rol. Durante esta primera etapa /users, /roles y /permissions todavía son respuestas JSON del taller anterior, no pantallas MVC.

## Pruebas y comprobación manual

```powershell
# Servicios y autenticación MVC sin necesitar Oracle:
.\mvnw.cmd test "-Dtest=*ServiceTest,AppUserDetailsServiceTest,SecurityConfigTest,AuthControllerSecurityTest"

# Suite completa y cobertura; contextLoads necesita Oracle:
.\mvnw.cmd clean verify
```

Maven carga Mockito como agente al iniciar Java para evitar la conexión dinámica de agentes en entornos restringidos. La configuración está en maven-surefire-plugin dentro de pom.xml.

Comprobar manualmente:

1. Sin sesión, / debe redirigir a /login; login, registro y CSS deben abrir.
2. Registrar una cuenta válida; revisar que PASSWORD_HASH contiene un hash BCrypt, nunca la contraseña original.
3. Repetir el correo, usar correo inválido o contraseña corta: mensaje de error y redirección a registro, sin conservar la contraseña en el formulario.
4. Probar contraseña equivocada y cuenta DELETED: no deben iniciar sesión.
5. Con OWNER, abrir /users, /roles o /permissions: debe negar acceso.
6. Con permisos de administración, las API actuales permiten consultar; las futuras pantallas deben respetar las mismas reglas.
7. Cerrar sesión y volver a una ruta privada: debe solicitar login otra vez.
8. Al integrar todo, probar crear y eliminar roles, asignar y quitar roles de usuarios, asociar permisos y ejecutar todas las pruebas.

## Primera parte de gestión MVC de usuarios

Esta parte corresponde al primer bloque asignado a Valentina. Incluye `UserMvcController`, `UserForm`, las plantillas `usuarios/lista.html` y `usuarios/formulario.html`, estilos de tabla y pruebas MVC. Reutiliza los servicios existentes; no implementa todavía la pantalla para asignar y quitar roles a usuarios existentes.

Para probarla, iniciar sesión con una cuenta que tenga MANAGE_USERS y abrir el enlace Gestionar usuarios del inicio:

1. `/usuarios`: listar cuentas y estados, sin mostrar contraseñas.
2. `/usuarios/nuevo`: crear una cuenta con correo, contraseña, estado y al menos un rol inicial con permisos.
3. `/usuarios/{id}/editar`: actualizar correo, estado y opcionalmente contraseña. Vacío conserva la contraseña anterior. Este formulario no modifica roles.
4. El botón Eliminar envía un POST y usa la eliminación lógica existente: el usuario sigue listado con estado DELETED.
5. Probar correo duplicado, datos inválidos, cuenta inexistente y acceso con OWNER: deben mostrarse errores o denegarse el acceso según corresponda.

Las pruebas nuevas están en `UserMvcControllerTest`; ejecutar `mvn test -Dtest=UserMvcControllerTest` para esa clase o `mvn verify` para la suite completa. Todos los formularios usan `th:action` y las operaciones POST redirigen. Los roles iniciales se eligen al crear para cumplir la regla previa de al menos un rol; la gestión posterior de esos roles queda para el segundo bloque.

## Segunda parte de gestión MVC de usuarios

El segundo bloque de Valentina añade el enlace Roles de cada fila del listado y la vista `usuarios/roles.html`. Con MANAGE_USERS se puede abrir `/usuarios/{id}/roles`, consultar las asignaciones, elegir un rol no asignado y quitar uno existente. Los controladores delegan en `findRoles`, `assignRole` y `removeRole` de AppUserService.

Los POST `/usuarios/{id}/roles/asignar` (parámetro roleId) y `/usuarios/{id}/roles/{roleId}/quitar` requieren CSRF y redirigen a la pantalla de roles con un mensaje. Si la cuenta no existe, la consulta regresa al listado. Se conserva la regla del servicio de no quitar el último rol, aunque se envíe el POST directamente; además, el botón queda deshabilitado cuando solo hay uno. Los roles duplicados o sin permisos producen el mensaje de la regla existente.

Comprobación manual:

1. Entrar con una cuenta que tenga MANAGE_USERS y abrir Usuarios → Roles de una cuenta de prueba.
2. Asignar un rol con permisos que todavía no tenga. Comprobar que aparece en la tabla y desaparece del selector.
3. Con dos roles asignados, quitar uno. Comprobar que vuelve a estar disponible en el selector.
4. Comprobar que no se puede quitar el último. Los cambios se reflejan en el acceso de la cuenta afectada cuando cierre y vuelva a iniciar sesión.
5. Iniciar sesión como OWNER sin MANAGE_USERS e intentar abrir la misma ruta: debe denegar acceso.

Las pruebas de pantalla y seguridad están en UserMvcControllerTest; las pruebas de consulta y las reglas de asignar/quitar están en AppUserServiceTest. Ejecutar `mvn verify` para verificar toda la aplicación. Este bloque no modifica las pantallas de creación de roles o permisos ni prepara el despliegue.

## Tercer bloque de Valentina

El proyecto ahora genera `target/cuidapet.war`, incorpora el inicializador para Tomcat y obtiene las credenciales de Oracle mediante variables de entorno. Antes de ejecutar o probar, definir DB_PASSWORD (o TEST_DB_PASSWORD con el perfil test), siguiendo [la guía de ejecución y despliegue](DESPLIEGUE.md). Los comandos anteriores de esta guía requieren esa configuración.

El perfil iaslab exige DB_URL, DB_USER y DB_PASSWORD. El WAR está preparado para verificar y desplegar con Java 21 y un contenedor compatible con Servlet 6.1, como Tomcat 11. La versión del servidor de IAsLab todavía no está confirmada: no se considera desplegado hasta comprobarla, cargar el WAR y probar la URL real. Las pantallas pendientes de Gordillo deben integrarse y volver a probarse antes de la entrega final.

## Entrega final pendiente de los tres integrantes

La parte de Juan no representa el taller completo. Faltan las pantallas de gestión y el despliegue efectivo. La entrega requiere el código en el repositorio de Classroom, commits reales de los tres, documentación detallada, pruebas de todos los servicios (incluyendo AuthService y AppUserDetailsService) y la aplicación funcionando en Tomcat de IAsLab. Confirmar versión de Tomcat/Jakarta y recursos del servidor antes de elegir la configuración WAR.

Trabajar en orden: Juan termina y publica sus commits; Valentina hace git pull y desarrolla sus tres commits; Gordillo hace git pull y desarrolla los suyos. No crear commits con el nombre de otro integrante. Cada persona verifica su configuración de Git y entiende el código que entrega.


## Verificación de la primera etapa

El 22 de octubre de 2026 se ejecutó `mvn -o verify` con Java 21 y la base Oracle local configurada: 129 pruebas, 0 fallos, 0 errores y 0 omitidas; BUILD SUCCESS. Incluye el arranque del contexto con Oracle, los servicios del taller anterior, la autenticación, los formularios Thymeleaf, CSRF y las reglas de autorización. También se generaron el JAR y el reporte JaCoCo. Esta comprobación no equivale al despliegue en IAsLab ni sustituye las pruebas de las futuras pantallas de gestión.
