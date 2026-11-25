# Frontend de CuidaPet Icesi

Desde esta carpeta, con Node.js 20.19+ (rama 20), 22.13+ (rama 22), o 24+:

```bash
npm ci
npm run dev
```

Abre la URL que muestra Vite. La base incluye las rutas `/` y `/como-funciona` y una página para direcciones desconocidas.

## API local

Por defecto, Vite reenvía las peticiones a `/api/v1` a Spring Boot en `http://localhost:8080`. Para configurar otra dirección, copia `.env.example` a `.env.development.local` y ajusta `API_PROXY_TARGET`. Si el backend está en el contexto `/cuidapet`, configura `VITE_API_BASE_URL=/cuidapet/api/v1` en ese archivo. El proxy se usa en desarrollo.

Arranca Spring Boot según la [guía del backend](../docs/DESPLIEGUE.md) cuando necesites utilizar la API.

## Inicio de sesión

- Abre `/cuenta` sin sesión. La aplicación te lleva a `/login` y te devuelve a la página solicitada al autenticarte.
- Inicia sesión con una cuenta activa del backend. La SPA usa `POST /api/v1/auth/login` y `GET /api/v1/auth/me`. Guarda el JWT en el almacenamiento de sesión de la pestaña y vuelve a consultar `/auth/me` al recargar.
- Abre `/administracion` para comprobar el acceso. La ruta requiere el rol `ADMIN` y los permisos `MANAGE_USERS` y `MANAGE_ROLES`. Spring Boot aplica la autorización de las operaciones del API.
- Cierra sesión desde la navegación. Al vencer el token, la SPA también descarta la sesión y solicita un nuevo inicio de sesión para entrar a una ruta privada.

## Administración

Desde **Mi cuenta**, abre **Administración** con una cuenta autorizada. Las cinco secciones consumen el CRUD de [seguridad del backend](../docs/API_SEGURIDAD.md). En **Usuarios** y **Roles** se escogen las asociaciones iniciales al crear un registro. En **Roles de usuarios** y **Permisos de roles** se pueden añadir, reemplazar y quitar asociaciones existentes.

La edición de un usuario conserva su contraseña si el campo queda vacío. Eliminar un usuario cambia su estado a **Eliminado**. Si el servidor rechaza una operación, el panel muestra su mensaje y mantiene el formulario abierto.

## Componentes compartidos

- `FormField` enlaza la etiqueta y la ayuda con el control. Usa `as="select"` o `as="textarea"` para reutilizarlo con otros campos nativos.
- `Button` ofrece variantes primaria, secundaria y de peligro. Usa `busy` y `busyLabel` durante una operación.
- `Feedback` muestra mensajes informativos, de carga, éxito y error. `Confirmation` permite confirmar o cancelar una acción y mostrar el error si falla.
- Los colores, espaciados y bordes compartidos están en las variables de `src/styles.css`.

## Perfil y mascotas

En **Mi cuenta** puedes crear, editar y eliminar tu perfil. **Mis mascotas** lista las mascotas de tu cuenta, permite registrarlas con una especie activa y abre su ficha para editar, archivar o eliminar. En **Fotos**, añade una URL HTTP o HTTPS y puedes marcarla como principal. La galería se actualiza con la selección devuelta por el backend.

Estas pantallas requieren el backend actualizado. El frontend obtiene el identificador de la cuenta desde `GET /api/v1/auth/me`, que devuelve `userId` junto con los datos de sesión. Spring Boot verifica la propiedad de los recursos.

## Verificación

```bash
npm run lint
npm run build
```

## Despliegue en Tomcat

Desde la raíz del repositorio, con Java 21 y Node.js configurados, genera las dos aplicaciones:

```powershell
mvn -DskipTests package
npm --prefix frontend ci
npm --prefix frontend run build
jar --create --file target/cuidapet-front.war -C frontend/dist .
```

Despliega `target/cuidapet.war` y `target/cuidapet-front.war` en el directorio `webapps` de la misma instancia de Tomcat 11. Conserva los nombres de los WAR para que correspondan a los contextos `/cuidapet` y `/cuidapet-front`. En IAsLab, configura el proceso de Tomcat con el perfil `iaslab`, las variables de Oracle y `JWT_SECRET` según la [guía del backend](../docs/DESPLIEGUE.md).

Abre `http://<servidor>:<puerto>/cuidapet-front/`. El build llama a `/cuidapet/api/v1` en el mismo origen, así que el navegador puede usar el API sin una política CORS adicional. Si creaste un archivo `.env.local` para cambiar el prefijo en desarrollo, mueve ese ajuste a `.env.development.local` antes de generar el WAR.

1. Abre `/cuidapet-front/cuenta` sin sesión y comprueba la redirección al login dentro del mismo contexto.
2. Inicia sesión con una cuenta activa. Confirma que vuelves a **Mi cuenta** y que recargar la página conserva la sesión.
3. Con un administrador, abre y recarga `/cuidapet-front/administracion/usuarios`. Comprueba que aparezcan los datos de Spring Boot. Con un cuidador, la misma ruta debe mostrar **Acceso denegado**.

La configuración de reescritura del WAR reenvía las rutas de navegación a React. Tomcat sirve los archivos estáticos directamente y mantiene el 404 para un recurso inexistente.
