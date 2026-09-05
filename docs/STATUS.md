# Chiri Platform v1.0 — Estado del Proyecto

> Fuente de seguimiento: GitHub Issues + GitHub Projects.
>
> La documentación en `docs/` define la arquitectura y especificación.
>
> Los commits representan los cambios realizados en el código.
>
> Este archivo resume el estado consolidado para evitar repetir revisiones ya realizadas.

## Estado general

**Fase actual:** Implementación incremental

**Último bloque funcional cerrado:** Home v1.0

**Casos de uso completados:**
- UC-001 — Autenticarse
- Home v1.0 — Implementado y validado E2E

**Siguiente bloque:** Integración funcional de módulos desde Inicio

---

## Completado

### Arquitectura

- [x] Principios del proyecto
- [x] Arquitectura general
- [x] Decisiones arquitectónicas principales
- [x] Decisión arquitectónica de Home como resumen y punto de entrada

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
- [x] Rechazo de Access Token expirado
- [x] Pruebas de autenticación
- [x] Flujo de autenticación Backend validado
- [x] Endpoint `GET /home`
- [x] Protección de `/home`
- [x] Respuesta Home v1.0
- [x] Pruebas Backend de Home

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
- [x] Home API
- [x] Home Models
- [x] Home Repository
- [x] Home Use Case
- [x] Home ViewModel
- [x] Home Screen
- [x] Estados de carga, información y error
- [x] Logout desde Home
- [x] Acciones rápidas de Música y Multimedia preparadas como puntos de entrada

---

## Home v1.0

### Backend

- [x] Endpoint `GET /home`
- [x] Autenticación mediante Bearer Token
- [x] Información del usuario
- [x] Estado general del hogar
- [x] Acciones rápidas
- [x] Información de conectividad y servidor
- [x] Pruebas unitarias/API
- [x] Integración con autenticación existente

### Android

- [x] Consumo de `/api/home`
- [x] Presentación de bienvenida
- [x] Presentación del estado del hogar
- [x] Presentación de acciones rápidas
- [x] Presentación de conectividad y servidor
- [x] Manejo de carga
- [x] Manejo de error
- [x] Logout
- [x] Validación en dispositivo real

### Integración E2E

- [x] Backend desplegado en Raspberry Pi
- [x] API pública disponible mediante HTTPS
- [x] Android conectado a API real
- [x] Login desde Android
- [x] Carga de Home desde Backend real
- [x] Logout desde Android
- [x] Validación funcional en dispositivo real

### Pendiente

- [ ] Navegación real desde Inicio hacia Música
- [ ] Navegación real desde Inicio hacia Multimedia

---

## Validaciones ya realizadas

### Backend

- [x] Suite completa de pruebas
- [x] `52 passed`
- [x] Access Token expirado rechazado correctamente
- [x] Sesiones revocadas rechazadas
- [x] Refresh Token revocado rechazado
- [x] Home protegido mediante autenticación

### Servidor

- [x] `chiri-backend.service` activo y habilitado
- [x] Backend ejecutándose mediante systemd
- [x] API pública operativa
- [x] `GET /api/health` responde `200`
- [x] PostgreSQL operativo
- [x] PostgreSQL limitado a `127.0.0.1:5432`
- [x] PostgreSQL no expuesto directamente a la red LAN
- [x] SSH mediante clave pública
- [x] Autenticación SSH por contraseña deshabilitada
- [x] Login SSH de root deshabilitado
- [x] `AllowUsers jose`
- [x] Acceso SSH externo mediante Cloudflare Tunnel + Access
- [x] Acceso SSH probado desde una red externa
- [x] No existe exposición directa del puerto 22 mediante port forwarding

### Android

- [x] `./gradlew test`
- [x] `./gradlew assembleDebug`
- [x] APK instalado en dispositivo real
- [x] Login validado
- [x] Home validado
- [x] Logout validado

---

## Configuración de servidor

### Completado

- [x] Backend desplegado en Raspberry Pi
- [x] Backend administrado mediante systemd
- [x] Caddy operativo
- [x] Cloudflare Tunnel para servicios web/API
- [x] Cloudflare Tunnel dedicado para SSH
- [x] Cloudflare Access para SSH
- [x] SSH endurecido
- [x] PostgreSQL limitado a localhost
- [x] Servicios principales verificados
- [x] Sin unidades systemd fallidas

### Pendiente

- [ ] Rotación de credenciales/tokens expuestos durante la configuración

---

## Documentación

- [x] `000_Principios.md`
- [x] `010_Proyecto.md`
- [x] `020_Arquitectura.md`
- [x] `030_Backend.md`
- [x] `040_Android.md`
- [x] `050_BaseDatos.md`
- [x] `060_API.md`
- [x] `070_Seguridad.md`
- [x] `080_Despliegue.md`
- [x] `090_GuiaProgramacion.md`
- [x] `100_DecisionesArquitectura.md`
- [x] `docs/STATUS.md`

---

## Próximo bloque funcional

### Integración de módulos desde Inicio

Pendiente:

- [ ] Definir navegación hacia Música
- [ ] Implementar entrada al módulo Música
- [ ] Definir navegación hacia Multimedia
- [ ] Implementar entrada al módulo Multimedia
- [ ] Validar navegación desde dispositivo real

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