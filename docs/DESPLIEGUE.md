# Ejecución y empaquetado de CuidaPet

Esta guía explica la configuración de Oracle, la ejecución local, las pruebas y la generación de cuidapet.war. El proyecto conserva el empaquetado WAR y el inicializador para un contenedor externo. La entrega del módulo MVC no requiere un despliegue físico en IAsLab.

## Requisitos

- JDK 21 para compilar y ejecutar este proyecto (pom.xml define java.version=21).
- Maven mediante mvnw.cmd en Windows o ./mvnw en Linux; alternativamente, Maven instalado.
- Oracle accesible y esquema preparado con los scripts existentes, sin volver a ejecutar scripts sobre datos que se deban conservar sin revisarlos.
- Para el WAR externo: Tomcat 11 con Servlet 6.1, compatible con Spring Boot 4.1.

Referencias oficiales: [despliegue WAR de Spring Boot](https://docs.spring.io/spring-boot/how-to/deployment/traditional-deployment.html) y [requisitos de contenedores](https://docs.spring.io/spring-boot/system-requirements.html). El proyecto conserva Spring Boot 4.1.1.

## Variables de entorno

| Variable | Uso | Valor predeterminado local |
|---|---|---|
| DB_URL | URL JDBC de Oracle | jdbc:oracle:thin:@localhost:1521/XEPDB1 |
| DB_USER | Usuario de Oracle | cuidapet |
| DB_PASSWORD | Contraseña de Oracle | Obligatoria, sin valor guardado |
| PORT | Puerto del servidor integrado | 8080 |
| SHOW_SQL | Mostrar SQL durante desarrollo | false |
| SPRING_PROFILES_ACTIVE | Perfil opcional del servidor | Usar iaslab al desplegar |
| TEST_DB_URL | Oracle del perfil test | jdbc:oracle:thin:@localhost:1521/XEPDB1 |
| TEST_DB_USER | Usuario del perfil test | CUIDAPET_TEST |
| TEST_DB_PASSWORD | Contraseña del perfil test | Obligatoria al activar test |

Con el perfil iaslab, DB_URL, DB_USER y DB_PASSWORD son obligatorias: no se utilizan los valores locales por omisión. La configuración mantiene ddl-auto=validate, por lo que la aplicación valida el esquema existente y no crea ni elimina tablas. Un archivo .env no se carga automáticamente: estas variables deben estar en el proceso que inicia Java o Tomcat.

## Ejecutar localmente en PowerShell

Desde la carpeta del repositorio:

```powershell
# JAVA_HOME debe apuntar al JDK 21 instalado en el equipo.
.\mvnw.cmd -version
$env:DB_URL = 'jdbc:oracle:thin:@localhost:1521/XEPDB1'
$env:DB_USER = 'cuidapet'
$claveOracle = Read-Host 'Contraseña de Oracle' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $claveOracle).Password
$claveOracle.Dispose()
.\mvnw.cmd spring-boot:run
```

Abrir http://localhost:8080/login. Para detener la aplicación, usar Ctrl+C. Las variables definidas con $env: pertenecen a esta terminal; hay que volver a definirlas al abrir otra. En IntelliJ se pueden configurar en las variables de entorno de la configuración de ejecución, sin compartir el archivo de configuración personal.

## Probar y generar el WAR

En la misma terminal con Oracle configurado y el esquema preparado:

```powershell
.\mvnw.cmd clean verify
```

El resultado es `target/cuidapet.war`; el informe de cobertura queda en `target/site/jacoco/index.html`. Las pruebas de servicios y controladores utilizan dobles de prueba; contextLoads sí abre una conexión a Oracle para validar el esquema. Para ejecutar contra el esquema de pruebas, definir TEST_DB_URL y TEST_DB_USER y solicitar su contraseña:

```powershell
$clavePruebas = Read-Host 'Contraseña de Oracle de pruebas' -AsSecureString
$env:TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $clavePruebas).Password
$clavePruebas.Dispose()
.\mvnw.cmd clean verify '-Dspring.profiles.active=test'
```

El esquema de pruebas debe existir y estar preparado. Para verificar solamente los servicios y controladores sin Oracle:

```powershell
.\mvnw.cmd test '-Dtest=*Test'
```

Esta selección incluye las clases terminadas en Test, pero no CuidaPetProyectApplicationTests, que es la prueba de arranque con Oracle. No usarla como sustituto de clean verify para la entrega final.

## Ejecutar el WAR localmente

Con DB_PASSWORD configurada, ejecutar:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/cuidapet.war --server.port=8081 --server.servlet.context-path=/cuidapet
```

Abrir http://localhost:8081/cuidapet/login. El WAR también se puede ejecutar con su servidor integrado. El paquete separa las dependencias del servidor integrado en WEB-INF/lib-provided para permitir ambas formas de ejecución.

Comprobar:

1. Sin sesión, /cuidapet/usuarios redirige a /cuidapet/login.
2. Login, registro y estilos usan el prefijo /cuidapet.
3. Registrar una cuenta de prueba e iniciar sesión. Los usuarios de los scripts no tienen contraseñas válidas para login; consultar la guía MVC para asignar ADMIN a una cuenta registrada.
4. Con MANAGE_USERS, crear/editar un usuario, asignar un segundo rol y quitarlo. No se debe poder retirar el último rol.
5. Con MANAGE_ROLES, crear un permiso y un rol, asignar y quitar permisos del rol y eliminar el rol.
6. Con OWNER, el acceso a /usuarios, /roles y /permisos debe denegarse.
7. Cerrar sesión y confirmar que las rutas privadas vuelven a pedir login.

## Configuración para un contenedor externo

El `pom.xml` conserva `packaging=war`, `finalName=cuidapet` y Tomcat con alcance `provided`. `CuidaPetProyectApplication` extiende `SpringBootServletInitializer` e implementa `configure`.

El perfil `iaslab` permite obtener DB_URL, DB_USER y DB_PASSWORD del proceso que ejecuta el contenedor. En un Tomcat externo compatible, el nombre `cuidapet.war` determina por defecto el contexto `/cuidapet`; el puerto lo define Tomcat. PORT solo aplica al servidor integrado. Esta configuración forma parte del proyecto y el despliegue externo es opcional para esta entrega.

## Problemas frecuentes

- `release version 21 not supported`: Maven utiliza otro JDK; revisar JAVA_HOME y mvnw.cmd -version.
- Error de DB_PASSWORD o autenticación Oracle: definir la contraseña correcta en el proceso de ejecución. Con test se usa TEST_DB_PASSWORD.
- ORA-17820 o conexión rechazada: revisar listener, host, puerto, servicio Oracle y acceso de red.
- Validación de esquema fallida: comprobar que se seleccionó el esquema correcto y que contiene las tablas del proyecto; no cambiar a create o update para ocultar el problema.
- 404 en el servidor: comprobar nombre del WAR y contexto; /login puede ser /cuidapet/login.
- 403: revisar permisos del usuario y enviar los formularios desde la página para incluir CSRF. No desactivar la protección.
- Error de versión de clases o Servlet: comprobar Java 21 y compatibilidad Servlet 6.1 antes de cambiar código.
