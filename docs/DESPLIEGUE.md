# Ejecución y despliegue de CuidaPet

Este documento cubre el tercer bloque de Valentina: generar un WAR, configurar Oracle por variables de entorno, ejecutar las pruebas y preparar la entrega al servidor. Preparar el WAR no equivale a haber desplegado la aplicación en IAsLab. La versión de Java, la versión de Tomcat, la dirección de Oracle y el mecanismo de carga del laboratorio siguen pendientes de confirmación.

## Requisitos

- JDK 21 para compilar y ejecutar este proyecto (pom.xml define java.version=21).
- Maven mediante mvnw.cmd en Windows o ./mvnw en Linux; alternativamente, Maven instalado.
- Oracle accesible y esquema preparado con los scripts existentes, sin volver a ejecutar scripts sobre datos que se deban conservar sin revisarlos.
- Para el WAR externo: Tomcat 11 con Servlet 6.1, compatible con Spring Boot 4.1. No asumir compatibilidad con Tomcat 9 o 10.1. Si IAsLab tiene otra versión, consultar al docente antes de cambiar Spring Boot: puede requerir una adaptación adicional.

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
# Ajustar la ruta si el JDK 21 está instalado en otra ubicación.
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
.\mvnw.cmd -version
$env:DB_URL = 'jdbc:oracle:thin:@localhost:1521/XEPDB1'
$env:DB_USER = 'cuidapet'
$claveOracle = Read-Host 'Contraseña de Oracle' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $claveOracle).Password
$claveOracle.Dispose()
.\mvnw.cmd spring-boot:run
```

Abrir http://localhost:8080/login. Para detener la aplicación, usar Ctrl+C. Las variables definidas con $env: pertenecen a esta terminal; hay que volver a definirlas al abrir otra. En IntelliJ se pueden configurar en las variables de entorno de la configuración de ejecución, sin compartir el archivo de configuración personal. Las contraseñas que aparecían anteriormente en las propiedades ya no se usan como valores predeterminados; esto no borra su presencia en el historial anterior de Git.

## Probar y generar el WAR

En la misma terminal con Oracle configurado:

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

## Probar el WAR antes de entregarlo

Con DB_PASSWORD configurada, ejecutar:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/cuidapet.war --server.port=8081 --server.servlet.context-path=/cuidapet
```

Abrir http://localhost:8081/cuidapet/login. Esta es una prueba del WAR con su servidor integrado; no demuestra por sí sola el despliegue en un Tomcat externo. El paquete separa las dependencias del servidor integrado en WEB-INF/lib-provided para permitir ambas formas de ejecución.

Comprobar:

1. Sin sesión, /cuidapet/usuarios redirige a /cuidapet/login.
2. Login, registro y estilos usan el prefijo /cuidapet.
3. Registrar una cuenta de prueba e iniciar sesión. Los usuarios de los scripts no tienen contraseñas válidas para login; consultar la guía MVC para asignar ADMIN a una cuenta registrada.
4. Con MANAGE_USERS, crear/editar un usuario, asignar un segundo rol y quitarlo. No se debe poder retirar el último rol.
5. Con OWNER sin MANAGE_USERS, el acceso a usuarios y sus acciones debe denegarse.
6. Cerrar sesión y confirmar que las rutas privadas vuelven a pedir login.

## Desplegar en IAsLab cuando se confirmen los datos

1. Pedir al docente o administrador la versión de Tomcat y Java, URL JDBC, usuario de Oracle, permisos de despliegue y URL final asignada al equipo. No inventar direcciones ni copiar los valores de localhost al servidor: allí localhost se refiere al propio servidor.
2. Preparar el esquema correspondiente según las indicaciones del laboratorio. No ejecutar automáticamente los scripts de creación sobre un esquema existente.
3. El administrador configura SPRING_PROFILES_ACTIVE=iaslab, DB_URL, DB_USER y DB_PASSWORD para el proceso de Tomcat y lo reinicia según las reglas del laboratorio. Si Tomcat es compartido, acordar con el administrador cómo aislar la configuración; no cambiar variables globales que afecten aplicaciones de otros grupos.
4. Generar target/cuidapet.war mediante clean verify. Usar el Tomcat Manager autorizado o el mecanismo de carga que indique IAsLab. Si el procedimiento permite copiar a webapps, el destino corresponde al CATALINA_BASE de ese servidor, no a una ruta supuesta.
5. Si el WAR se llama cuidapet.war y no hay una configuración de contexto diferente, la ruta será /cuidapet. Host, puerto y protocolo los proporciona el laboratorio. En Tomcat externo, el puerto lo define Tomcat; PORT solo aplica al servidor integrado.
6. Revisar los logs y ejecutar la comprobación funcional anterior con la URL real. Registrar la URL, versiones, fecha y resultado en la documentación del equipo. Repetir el empaquetado y despliegue tras integrar las partes de Gordillo.

## Problemas frecuentes

- `release version 21 not supported`: Maven utiliza otro JDK; revisar JAVA_HOME y mvnw.cmd -version.
- Error de DB_PASSWORD o autenticación Oracle: definir la contraseña correcta en el proceso de ejecución. Con test se usa TEST_DB_PASSWORD.
- ORA-17820 o conexión rechazada: revisar listener, host, puerto, servicio Oracle y acceso de red.
- Validación de esquema fallida: comprobar que se seleccionó el esquema correcto y que contiene las tablas del proyecto; no cambiar a create o update para ocultar el problema.
- 404 en el servidor: comprobar nombre del WAR y contexto; /login puede ser /cuidapet/login.
- 403: revisar permisos del usuario y enviar los formularios desde la página para incluir CSRF. No desactivar la protección.
- Error de versión de clases o Servlet: comprobar Java 21 y compatibilidad Servlet 6.1 antes de cambiar código.

## Verificación del tercer bloque

El 24 de octubre de 2026 se ejecutó mvn -o verify con Java 21 y Oracle local: 156 pruebas, 0 fallos, 0 errores y 0 omitidas; BUILD SUCCESS. Se inspeccionó el WAR para confirmar WarLauncher, clases y plantillas, perfil iaslab y Tomcat core/websocket en WEB-INF/lib-provided. Se inició el WAR localmente con el perfil iaslab y el contexto /cuidapet: login, registro y CSS respondieron 200, y usuarios sin sesión respondió 302 hacia /cuidapet/login. El servidor de prueba se detuvo al terminar. No se desplegó en un Tomcat externo ni en IAsLab; esa comprobación sigue pendiente.
