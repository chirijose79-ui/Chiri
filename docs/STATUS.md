# Chiri Platform v1.0 — Estado del Proyecto

> Fuente de seguimiento: GitHub Issues + GitHub Projects.
> La documentación en `docs/` define la arquitectura y especificación.
> Los commits representan los cambios realizados en el código.
> Este archivo resume el estado consolidado para evitar repetir revisiones ya realizadas.

## Estado general

**Fase actual:** Implementación incremental

**Último bloque funcional cerrado:** UC-001 — Autenticarse

**UC-002:** Diseño funcional aprobado

**Bloque actual:** HOME v1.0

---

## Completado

### Arquitectura

- [x] Principios del proyecto
- [x] Arquitectura general
- [x] Decisiones arquitectónicas principales

### Base de Datos

- [x] Modelo de identidad
- [x] `identity.user`
- [x] `security.session`
- [x] `security.refresh_token`
- [x] Documentación de Base de Datos v1.0

### Backend

- [x] Login
- [x] Contrato de login mediante `identifier` + `password`
- [x] Access Token JWT RS256
- [x] Claims JWT
- [x] Refresh Token
- [x] Rotación de Refresh Token
- [x] Detección de reutilización de Refresh Token
- [x] Revocación de sesión
- [x] Logout
- [x] `/auth/me`
- [x] Gestión de sesiones
- [x] Protección mediante Bearer Token
- [x] Refresh Token rechazado después de revocación
- [x] Pruebas de autenticación
- [x] Flujo de autenticación Backend validado

### Android

- [x] Auth API
- [x] Auth Models
- [x] Auth Interceptor
- [x] Authenticator
- [x] SessionManager
- [x] Session Storage
- [x] Secure Session Storage
- [x] Login Use Case
- [x] Logout Use Case
- [x] Validate Session Use Case
- [x] Auth Repository
- [x] Login ViewModel
- [x] Integración de `AuthInterceptor` en `OkHttpClient`
- [x] Integración de `AuthAuthenticator` en `OkHttpClient`
- [x] Uso compartido de `SecureSessionStorage`
- [x] Persistencia segura de Access Token
- [x] Persistencia segura de Refresh Token
- [x] Flujo de refresh automático implementado
- [x] Rotación de tokens implementada
- [x] Retry de solicitud implementado
- [x] Validación de sesión mediante `/auth/me`

---

## Integración E2E de autenticación

### Implementación ya realizada

- [x] Persistencia de Access Token
- [x] Persistencia de Refresh Token
- [x] Request autenticado mediante Bearer Token
- [x] Manejo de respuesta `401`
- [x] Refresh automático
- [x] Rotación del Refresh Token
- [x] Retry de la solicitud original
- [x] Sincronización del refresh mediante `AuthAuthenticator`
- [x] Limpieza de sesión ante fallo de refresh
- [x] Logout implementado
- [x] Validación de sesión mediante `/auth/me`

### Validación E2E

- [x] Ejecutar flujo Android contra Backend real
- [x] Validar Login desde la aplicación
- [x] Validar persistencia de sesión después del Login
- [x] Validar request autenticado desde Android
- [x] Validar 401 desde Android
- [x] Validar refresh automático desde Android
- [x] Validar rotación de Refresh Token desde Android
- [x] Validar construcción del retry de la solicitud original desde Android
- [x] Validar resultado 200 después del retry de la solicitud original
- [x] Validar múltiples requests simultáneos con 401
- [x] Validar comportamiento ante refresh fallido
- [x] Validar Logout desde Android
- [x] Validar sesión revocada después del Logout
- [ ] Ejecutar pruebas E2E automatizadas — diferido como deuda técnica futura
- [x] Realizar revisión final de seguridad
- [x] Actualizar Issue #2 con resultados
- [x] Cerrar Issue #2 al cumplir los criterios del bloque

> Las pruebas E2E automatizadas completas quedan fuera del alcance de UC-001 y se mantienen como deuda técnica futura. La validación funcional E2E de autenticación fue realizada manualmente contra el Backend real, complementada con pruebas unitarias e instrumentadas Android.

---

## Despliegue e infraestructura

### Backend

- [x] Backend FastAPI desplegado en Raspberry Pi
- [x] PostgreSQL operativo
- [x] Servicio `chiri-backend` configurado mediante systemd
- [x] Reinicio automático del Backend configurado
- [x] Validar Backend después de reiniciar Raspberry Pi

### Reverse Proxy

- [x] Caddy operativo
- [x] Caddy conectado con Backend
- [x] API publicada mediante Caddy

### Cloudflare

- [x] Tunnel `chiri-web` configurado para WEB
- [x] Tunnel `chiri-home` configurado para API
- [x] Separación WEB / API validada
- [x] `api.chirihome.com` operativo
- [x] API HTTPS validada

### Android

- [x] Android configurado para utilizar API HTTPS pública
- [x] Login Android contra API HTTPS
- [x] Persistencia de sesión contra API HTTPS
- [x] Logout contra API HTTPS
- [x] Re-login contra API HTTPS

### Validación final de despliegue

- [x] `GET /api/health` responde `200`
- [x] API accesible mediante HTTPS
- [x] Backend vuelve automáticamente después de reinicio
- [x] Android puede comunicarse con el Backend mediante HTTPS
- [x] Infraestructura de despliegue v1.0 validada

---

## UC-002 — Consultar funcionalidades disponibles

### Diseño

- [x] Definir módulos principales de Chiri
- [x] Definir funcionalidades principales
- [x] Definir estructura de Home
- [x] Definir separación entre módulos
- [x] Aprobar alcance funcional inicial
- [x] Mantener el alcance puntual para v1.0

### Estado

- [x] Diseño funcional aprobado
- [ ] Implementación Backend
- [ ] Implementación Android
- [ ] Pruebas
- [ ] Validación E2E

> UC-002 permanece abierto hasta completar su implementación y validación. El diseño funcional aprobado permite continuar con la construcción incremental de Home.

---

# HOME v1.0

## Diseño

- [x] Definir objetivo y alcance de Home
- [x] Definir estructura funcional
- [x] Definir bloques de la pantalla
- [x] Definir bienvenida
- [x] Definir estado del hogar
- [x] Definir acciones rápidas
- [x] Definir información básica
- [x] Definir acciones rápidas iniciales: `music`
- [x] Definir acciones rápidas iniciales: `multimedia`
- [x] Definir contrato `GET /api/home`
- [x] Definir modelo de respuesta
- [x] Definir reglas de negocio
- [x] Definir autenticación
- [x] Definir respuestas de error
- [x] Determinar que Home no requiere nuevas tablas
- [x] Determinar que Home no accede directamente a PostgreSQL
- [x] Mantener Home como resumen y punto de acceso a otros módulos

## Backend

- [ ] Crear modelos Pydantic de Home
- [ ] Crear servicio de Home
- [ ] Integrar usuario autenticado
- [ ] Implementar estado del hogar
- [ ] Implementar acciones rápidas
- [ ] Implementar información básica
- [ ] Implementar `GET /api/home`
- [ ] Validar respuesta `200`
- [ ] Validar respuestas de error
- [ ] Revisar seguridad del endpoint

## Pruebas Backend

- [ ] Probar `GET /api/home` con sesión válida
- [ ] Probar acceso sin token
- [ ] Probar token inválido
- [ ] Probar token expirado
- [ ] Probar usuario no disponible
- [ ] Validar estructura de respuesta
- [ ] Validar `display_name`
- [ ] Validar `home.status`
- [ ] Validar `quick_actions`
- [ ] Validar `information`
- [ ] Ejecutar suite completa de pruebas

## Android

- [ ] Crear modelo de respuesta Home
- [ ] Crear endpoint Retrofit
- [ ] Crear repositorio Home
- [ ] Crear ViewModel
- [ ] Crear pantalla Home
- [ ] Mostrar bienvenida
- [ ] Mostrar estado del hogar
- [ ] Mostrar acciones rápidas
- [ ] Mostrar información básica
- [ ] Manejar estado de carga
- [ ] Manejar errores
- [ ] Mantener navegación hacia los módulos correspondientes

## Integración

- [ ] Probar Android contra API HTTPS
- [ ] Validar Home con sesión existente
- [ ] Validar rechazo de sesión inválida
- [ ] Validar carga correcta de información
- [ ] Validar navegación mediante acción `music`
- [ ] Validar navegación mediante acción `multimedia`
- [ ] Probar comportamiento con API no disponible
- [ ] Ejecutar validación E2E de Home

## Documentación

- [ ] Actualizar `060_API.md`
- [ ] Actualizar `040_Android.md`
- [ ] Registrar decisiones específicas de Home
- [ ] Revisar `020_Arquitectura.md` únicamente si fuera necesario
- [ ] Revisar `080_Despliegue.md` únicamente si fuera necesario

## Git

- [ ] Commit Backend Home
- [ ] Commit pruebas Backend Home
- [ ] Commit Android Home
- [ ] Commit integración Home
- [ ] Commit documentación Home

---

## Validaciones ya realizadas

### Conectividad

- [x] Windows → `192.168.1.88:8000`
- [x] TCP puerto `8000` accesible
- [x] FastAPI `/docs` responde HTTP `200`
- [x] Backend accesible desde la red LAN
- [x] API pública HTTPS responde
- [x] `GET /api/health` responde `200`

### Autenticación Backend

- [x] `POST /auth/login`
- [x] `GET /auth/me`
- [x] `POST /auth/refresh`
- [x] `POST /auth/logout`
- [x] Creación de sesión
- [x] Access Token RS256
- [x] Refresh Token
- [x] Rotación de Refresh Token
- [x] Revocación de sesión
- [x] Rechazo posterior de tokens/sesiones revocadas

### Pruebas Backend

- [x] Tests de autenticación
- [x] Tests de sesión
- [x] Tests de logout
- [x] Tests de refresh
- [x] Tests de revocación
- [x] Suite existente validada

---

## Casos de Uso

| ID     | Caso de uso                           | Estado               |
| ------ | ------------------------------------- | -------------------- |
| UC-001 | Autenticarse                          | ✅ Completado         |
| UC-002 | Consultar funcionalidades disponibles | 🔄 En implementación |
| UC-003 | Ejecutar funcionalidad                | ⬜ Pendiente          |
| UC-004 | Gestionar usuarios                    | ⬜ Pendiente          |
| UC-005 | Gestionar permisos                    | ⬜ Pendiente          |
| UC-006 | Administrar configuración             | ⬜ Pendiente          |

---

## Entidades todavía no implementadas

Las siguientes entidades están definidas para v1.0 pero **no deben implementarse anticipadamente**.

### Authorization

- [ ] `authorization.role`
- [ ] `authorization.permission`
- [ ] `authorization.user_role`
- [ ] `authorization.role_permission`

### Platform

- [ ] `platform.module`
- [ ] `platform.functionality`

> Estas entidades no deben implementarse únicamente por estar definidas. Se implementarán cuando exista una funcionalidad que realmente las necesite.

---

## Reglas de trabajo

1. No reimplementar componentes marcados como completados.
2. No crear componentes únicamente para completar una estructura teórica.
3. No modificar PostgreSQL manualmente.
4. Los cambios de Base de Datos se realizan mediante Alembic.
5. Android no accede directamente a PostgreSQL.
6. No duplicar innecesariamente datos de servicios externos.
7. Todo bloque funcional debe tener pruebas.
8. Todo cambio importante debe quedar registrado mediante commit.
9. Los cambios arquitectónicos requieren revisión de las decisiones arquitectónicas.
10. El estado operativo del trabajo se controla mediante GitHub Issues y Projects.
11. Antes de implementar un bloque, revisar `docs/STATUS.md` para evitar repetir trabajo ya completado.
12. El código real del repositorio y los commits son la referencia para determinar qué está implementado.
13. Una funcionalidad no se considera E2E completada solamente porque sus componentes existan; debe existir una validación del flujo completo.
14. No marcar como pendiente nuevamente una funcionalidad que ya haya sido validada y registrada como completada.
15. Mantener cada bloque funcional dentro del alcance aprobado para v1.0.
16. No implementar infraestructura adicional si la infraestructura existente ya resuelve la necesidad.
17. No agregar nuevas tablas de Base de Datos si el bloque funcional no requiere persistencia.
