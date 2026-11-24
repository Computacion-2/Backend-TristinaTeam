# Frontend de CuidaPet Icesi

Desde esta carpeta, con Node.js 20.19+ (rama 20), 22.13+ (rama 22), o 24+:

```bash
npm ci
npm run dev
```

Abre la URL que muestra Vite. La base incluye las rutas `/` y `/como-funciona` y una página para direcciones desconocidas.

## API local

Por defecto, Vite reenvía las peticiones a `/api/v1` a Spring Boot en `http://localhost:8080`. Para configurar otra dirección, copia `.env.example` a `.env.local` y ajusta `API_PROXY_TARGET`. Si el backend está en el contexto `/cuidapet`, configura `VITE_API_BASE_URL=/cuidapet/api/v1`. El proxy se usa en desarrollo. El build de producción se servirá con la configuración de Tomcat.

Arranca Spring Boot según la [guía del backend](../docs/DESPLIEGUE.md) cuando necesites utilizar la API.

## Inicio de sesión

- Abre `/cuenta` sin sesión. La aplicación te lleva a `/login` y te devuelve a la página solicitada al autenticarte.
- Inicia sesión con una cuenta activa del backend. La SPA usa `POST /api/v1/auth/login` y `GET /api/v1/auth/me`. Guarda el JWT en el almacenamiento de sesión de la pestaña y vuelve a consultar `/auth/me` al recargar.
- Abre `/administracion` para comprobar el acceso. La ruta requiere el rol `ADMIN` y los permisos `MANAGE_USERS` y `MANAGE_ROLES`. Spring Boot aplica la autorización de las operaciones del API.
- Cierra sesión desde la navegación. Al vencer el token, la SPA también descarta la sesión y solicita un nuevo inicio de sesión para entrar a una ruta privada.

## Verificación

```bash
npm run lint
npm run build
```
