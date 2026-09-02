# Chiri Platform v1.0 — Estado del Proyecto

> Fuente de seguimiento: GitHub Issues + GitHub Projects.
> La documentación en `docs/` define la arquitectura y especificación.
> Los commits representan los cambios realizados en el código.
> Este archivo resume el estado consolidado para evitar repetir revisiones ya realizadas.

## Estado general

**Fase actual:** Implementación incremental

**Último bloque documental cerrado:** Base de Datos

**Caso de uso completado:** UC-001 — Autenticarse

**Siguiente bloque:** Integración E2E Android ↔ Backend de autenticación

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

### Validación E2E pendiente

- [ ] Ejecutar flujo completo desde Android contra Backend real
- [ ] Validar Login desde la aplicación
- [ ] Validar persistencia de sesión después del Login
- [ ] Validar request autenticado desde Android
- [ ] Validar `401` real desde Android
- [ ] Validar refresh automático desde Android
- [ ] Validar rotación de Refresh Token desde Android
- [ ] Validar retry de la solicitud original desde Android
- [ ] Validar múltiples requests simultáneos con `401`
- [ ] Validar comportamiento ante refresh fallido
- [ ] Validar Logout desde Android
- [ ] Validar sesión revocada después del Logout
- [ ] Ejecutar pruebas E2E automatizadas
- [ ] Realizar revisión final de seguridad
- [ ] Actualizar Issue #2 con resultados
- [ ] Cerrar Issue #2 al cumplir todos los criterios

---

## Validaciones ya realizadas

### Conectividad

- [x] Windows → `192.168.1.88:8000`
- [x] TCP puerto `8000` accesible
- [x] FastAPI `/docs` responde HTTP `200`
- [x] Backend accesible desde la red LAN

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

## Próximo bloque funcional

### UC-002 — Consultar funcionalidades disponibles

Pendiente de implementar:

- [ ] Modelo `platform.module`
- [ ] Modelo `platform.functionality`
- [ ] Migración Alembic
- [ ] Application layer
- [ ] API
- [ ] Pruebas Backend
- [ ] Integración Android
- [ ] Pruebas E2E

---

## Casos de Uso

| ID     | Caso de uso                           | Estado       |
| ------ | ------------------------------------- | ------------ |
| UC-001 | Autenticarse                          | ✅ Completado |
| UC-002 | Consultar funcionalidades disponibles | ⬜ Pendiente  |
| UC-003 | Ejecutar funcionalidad                | ⬜ Pendiente  |
| UC-004 | Gestionar usuarios                    | ⬜ Pendiente  |
| UC-005 | Gestionar permisos                    | ⬜ Pendiente  |
| UC-006 | Administrar configuración             | ⬜ Pendiente  |

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
