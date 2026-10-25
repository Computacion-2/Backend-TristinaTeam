# CuidaPet Icesi

Proyecto desarrollado para la asignatura Computación en Internet II. Incluye el backend de una plataforma de cuidado de mascotas y el módulo MVC de gestión de usuarios, roles y permisos, protegido con Spring Security y construido con vistas Thymeleaf.

Integrantes: Juan Pablo Pino, Valentina Martínez y Juan Gordillo.

El módulo permite registrarse, iniciar y cerrar sesión, gestionar usuarios, roles y permisos, asignar roles a usuarios y asociar permisos a roles. La [guía MVC](docs/TALLER_MVC.md) explica su funcionamiento y el recorrido para demostrarlo.

## 1. Tecnologías utilizadas

- Java 21.
- Spring Boot 4.1.1.
- Spring Data JPA y Hibernate.
- Spring Security, BCrypt y Thymeleaf.
- Oracle Database XE 21c.
- Maven.
- JUnit, Mockito y MockMvc.
- JaCoCo para la cobertura de pruebas.

## 2. Estructura del proyecto

- `src/main/java`: entidades JPA, repositorios, servicios y controladores.
- `src/main/resources/templates`: vistas Thymeleaf.
- `src/main/resources/static/css/styles.css`: estilos de las pantallas.
- `src/main/resources/sql`: scripts de creación de la base de datos y datos iniciales.
- `src/test/java`: pruebas automatizadas.
- `docs/API.md`: documentación de los endpoints.
- `pom.xml`: dependencias y configuración de Maven.

## 3. Configuración de la base de datos

La aplicación utiliza Oracle XE y el servicio `XEPDB1`.

Para preparar la base de datos, se debe crear un usuario con permisos para conectarse y crear tablas, además de asignarle una cuota sobre el tablespace `USERS`.

Una vez creado el usuario, ejecutar los siguientes archivos desde Oracle SQL Developer, utilizando F5 y respetando este orden:

1. `src/main/resources/sql/01_creacion.sql`.
2. `src/main/resources/sql/02_datos_iniciales.sql`.

El primer archivo crea las 23 tablas, sus relaciones, restricciones e índices. El segundo carga los registros iniciales de usuarios, roles, permisos, especies, servicios y estados de reserva.

## 4. Configuración de Spring Boot

La conexión principal se configura en `src/main/resources/application.properties`.

Para las pruebas con el esquema `CUIDAPET_TEST`, se utiliza `src/main/resources/application-test.properties`, con la configuración correspondiente a Oracle.

La conexión local utiliza la siguiente dirección:

`jdbc:oracle:thin:@localhost:1521/XEPDB1`

La conexión se configura con `DB_URL`, `DB_USER` y `DB_PASSWORD`; la contraseña es obligatoria y no tiene un valor guardado en las propiedades. Para el perfil `test` se utilizan `TEST_DB_URL`, `TEST_DB_USER` y `TEST_DB_PASSWORD`. Los pasos para definirlas están en [ejecución y despliegue](docs/DESPLIEGUE.md).

## 5. Ejecución del proyecto

Abrir el proyecto en IntelliJ IDEA y comprobar que esté configurado para utilizar Java 21.

Desde una terminal ubicada en la raíz del proyecto, definir primero las variables de Oracle siguiendo [la guía de ejecución](docs/DESPLIEGUE.md) y ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

Cuando la aplicación termine de iniciar, los endpoints estarán disponibles en:

`http://localhost:8080`

## 6. Pruebas automatizadas

Para compilar el proyecto, ejecutar las pruebas y generar el reporte de cobertura:

```powershell
.\mvnw.cmd clean verify
```

Para ejecutar las pruebas con el perfil de Oracle de prueba:

```powershell
.\mvnw.cmd clean verify "-Dspring.profiles.active=test"
```

El reporte de JaCoCo se genera en:

`target/site/jacoco/index.html`

Las pruebas cubren los servicios de usuarios, roles, permisos, registro y carga de credenciales, además de reglas de acceso y formularios MVC. Consultar el reporte generado para ver la cobertura actual. La prueba contextLoads requiere la base Oracle configurada.

## 7. Funcionalidades implementadas

El módulo se usa desde vistas Thymeleaf. `/login`, `/register`, `/css/**` y `/error` son públicos; las demás rutas exigen sesión; la gestión de usuarios exige `MANAGE_USERS` y la de roles y permisos exige `MANAGE_ROLES`. El registro asigna OWNER y guarda una contraseña BCrypt. Solo las cuentas ACTIVE pueden iniciar sesión y los roles inactivos no conceden autoridades.

| Funcionalidad del taller | Ruta | Plantilla |
|---|---|---|
| Iniciar sesión | `GET /login` | `auth/login.html` |
| Registrar usuario | `GET/POST /register` | `auth/register.html` |
| Listar usuarios | `GET /usuarios` | `usuarios/lista.html` |
| Asociar roles a un usuario | `GET /usuarios/{id}/roles`, `POST /usuarios/{id}/roles/asignar` | `usuarios/roles.html` |
| Quitar roles a un usuario | `POST /usuarios/{id}/roles/{roleId}/quitar` | `usuarios/roles.html` |
| Crear roles | `GET /roles/nuevo`, `POST /roles/crear` | `roles/formulario.html` |
| Eliminar roles | `POST /roles/{id}/eliminar` | `roles/lista.html` |
| Asociar permisos a un rol | `GET /roles/{id}/permisos`, `POST /roles/{id}/permisos/asignar` | `roles/permisos.html` |

Además, las pantallas permiten crear, editar y eliminar usuarios y permisos, editar roles y quitar permisos de un rol. Todos los formularios usan `th:action`, por lo que envían el token CSRF, y cada POST redirige a una vista GET con un mensaje.

La API REST anterior se conserva en `/users`, `/api/roles` y `/api/permissions`, con las mismas reglas de autorización.

Las reglas de negocio impiden crear usuarios sin roles, crear roles sin permisos y retirar el último rol o permiso asignado. Los códigos de permiso son únicos y no se modifican después de crearse. Los roles ADMIN y OWNER no se pueden renombrar, desactivar ni eliminar, ADMIN conserva MANAGE_USERS y MANAGE_ROLES, y esos dos permisos no se pueden eliminar; así el módulo no se queda sin administración y el registro público siempre tiene el rol OWNER.

La eliminación de usuarios es lógica: el registro permanece en la base de datos con el estado `DELETED`.

También se implementaron validaciones de entrada, manejo de errores HTTP para la API REST (las vistas muestran los errores como mensajes) y anotaciones para evitar referencias circulares durante la serialización JSON.

## 8. Documentación de la API

Las rutas, los parámetros, los ejemplos de peticiones y los códigos HTTP se encuentran documentados en `docs/API.md`.

## 9. Consideraciones

Los usuarios cargados mediante los scripts SQL son registros de prueba y no cuentan con credenciales válidas para autenticación. Crear una cuenta desde /register; la guía MVC explica cómo asignarle ADMIN desde la base para la demostración.

Las pantallas y la API REST corresponden a la administración de usuarios, roles y permisos. Las demás entidades forman parte del modelo de persistencia JPA.

El proyecto genera `target/cuidapet.war` con `clean verify` y dispone del perfil `iaslab`. Tomcat conserva el alcance `provided` y la aplicación incluye el inicializador para un contenedor externo. La [guía de ejecución y empaquetado](docs/DESPLIEGUE.md) explica cómo ejecutar el WAR localmente. Esta entrega no requiere un despliegue físico en IAsLab.

## Video de funcionamiento
[Ver demostración de CuidaPet](https://drive.google.com/file/d/1CuUJEpwXpszjL59R-BCdbdwqss7HutJX/view?usp=sharing)
