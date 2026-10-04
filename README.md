# CuidaPet Icesi

Proyecto desarrollado para la asignatura Computación en Internet II. Consiste en un backend para una plataforma de cuidado de mascotas, implementado con Spring Boot, Spring Data JPA y Oracle.

## 1. Tecnologías utilizadas

- Java 21.
- Spring Boot 4.1.1.
- Spring Data JPA y Hibernate.
- Oracle Database XE 21c.
- Maven.
- JUnit, Mockito y MockMvc.
- JaCoCo para la cobertura de pruebas.

## 2. Estructura del proyecto

- `src/main/java`: entidades JPA, repositorios, servicios y controladores.
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

Es necesario ajustar el usuario y la contraseña según la instalación de Oracle.

## 5. Ejecución del proyecto

Abrir el proyecto en IntelliJ IDEA y comprobar que esté configurado para utilizar Java 21.

Desde una terminal ubicada en la raíz del proyecto, ejecutar:

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

Los tres servicios principales cuentan con el 100 % de cobertura de instrucciones y ramas.

## 7. Funcionalidades implementadas

El backend permite administrar usuarios, roles y permisos mediante sus respectivos controladores REST.

Las reglas de negocio impiden crear usuarios sin roles, crear roles sin permisos y retirar el último rol o permiso asignado.

La eliminación de usuarios es lógica: el registro permanece en la base de datos con el estado `DELETED`.

También se implementaron validaciones de entrada, manejo de errores HTTP y anotaciones para evitar referencias circulares durante la serialización JSON.

## 8. Documentación de la API

Las rutas, los parámetros, los ejemplos de peticiones y los códigos HTTP se encuentran documentados en `docs/API.md`.

## 9. Consideraciones

Los usuarios cargados mediante los scripts SQL son registros de prueba y no cuentan con credenciales válidas para autenticación.

Las funcionalidades REST implementadas corresponden a la administración de usuarios, roles y permisos. Las demás entidades forman parte del modelo de persistencia JPA.

El despliegue en IAsLab debe configurarse según los recursos y las instrucciones proporcionados para el taller.