# Chiri Platform v1.0 — Estado del Proyecto

> Fuente de seguimiento: GitHub Issues + GitHub Projects.
> La documentación en `docs/` define la arquitectura y especificación.
> Los commits representan los cambios realizados en el código.

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
- [x] Access Token JWT RS256
- [x] Refresh Token
- [x] Rotación de Refresh Token
- [x] Logout
- [x] `/auth/me`
- [x] Gestión de sesiones
- [x] Protección mediante Bearer Token
- [x] Pruebas de autenticación

### Android

- [x] Auth API
- [x] Auth Interceptor
- [x] Authenticator
- [x] SessionManager
- [x] Session Storage
- [x] Login Use Case
- [x] Logout Use Case
- [x] Validate Session Use Case
- [x] Auth Repository
- [x] Login ViewModel

---

## En progreso

### Integración E2E de autenticación

- [ ] Login Android → Backend
- [ ] Persistencia de sesión
- [ ] Request autenticado
- [ ] Respuesta `401`
- [ ] Refresh automático
- [ ] Rotación del Refresh Token
- [ ] Retry de la solicitud
- [ ] Manejo de concurrencia
- [ ] Logout
- [ ] Sesión revocada
- [ ] Pruebas E2E
- [ ] Revisión de seguridad

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

| ID | Caso de uso | Estado |
|---|---|---|
| UC-001 | Autenticarse | ✅ Completado |
| UC-002 | Consultar funcionalidades disponibles | ⬜ Pendiente |
| UC-003 | Ejecutar funcionalidad | ⬜ Pendiente |
| UC-004 | Gestionar usuarios | ⬜ Pendiente |
| UC-005 | Gestionar permisos | ⬜ Pendiente |
| UC-006 | Administrar configuración | ⬜ Pendiente |

---

## Entidades todavía no implementadas

Las siguientes entidades están definidas para v1.0 pero **no deben implementarse anticipadamente**:

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