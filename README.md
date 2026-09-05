# Chiri Platform v1.0

Plataforma personal para integrar servicios del hogar, multimedia, inteligencia artificial y servicios personales desde una única aplicación.

## Estado del proyecto

**Versión:** v1.0
**Fase:** Implementación incremental
**Plataforma principal:** Android
**Servidor:** Raspberry Pi 4B 8 GB
**Backend:** FastAPI
**Base de datos:** PostgreSQL 17
**Branch principal:** `master`

> El proyecto se desarrolla de forma incremental. Cada funcionalidad se diseña, implementa, prueba y valida antes de continuar con el siguiente bloque.

---

## Objetivo

Chiri busca proporcionar una plataforma centralizada para interactuar con los servicios personales y del hogar mediante una arquitectura modular.

La aplicación Android actúa como cliente principal y se comunica con el Backend de Chiri mediante una API HTTPS.

```text
┌──────────────────────┐
│      Android App     │
└──────────┬───────────┘
           │ HTTPS
           ▼
┌──────────────────────┐
│      Cloudflare      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│        Caddy         │
│    Reverse Proxy     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│    Chiri Backend     │
│       FastAPI        │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│      PostgreSQL      │
└──────────────────────┘
```

---

## Módulos funcionales

La plataforma está organizada en cinco áreas principales:

```text
Chiri
│
├── Home
├── Multimedia
├── Inteligencia Artificial
├── Personal
└── Configuración
```

### Home

Centro de acceso y resumen de Chiri.

Incluye inicialmente:

* Bienvenida al usuario.
* Estado general del hogar.
* Acciones rápidas.
* Información básica de conectividad y servidor.

### Multimedia

Área destinada a:

* Música.
* Videos.
* Fotos.

### Inteligencia Artificial

Área destinada a las funcionalidades de inteligencia artificial de Chiri.

### Personal

Área destinada a servicios y funcionalidades personales.

### Configuración

Área destinada a la configuración y administración de Chiri.

> El alcance de cada módulo se implementa únicamente cuando la funcionalidad correspondiente sea necesaria y esté definida.

---

## Arquitectura

El proyecto sigue una arquitectura cliente-servidor:

```text
Android
   │
   │ HTTPS + Bearer Token
   ▼
Cloudflare
   │
   ▼
Caddy
   │
   ▼
FastAPI
   │
   ├── Application
   ├── Domain
   └── Infrastructure
          │
          ▼
      PostgreSQL
```

Los servicios externos, cuando corresponda, se integran mediante sus propias APIs o mecanismos de comunicación.

Android **no accede directamente a PostgreSQL**.

---

## Autenticación

La autenticación de v1.0 está implementada.

Incluye:

* Login.
* Access Token JWT.
* Firma RS256.
* RSA 3072.
* Refresh Token.
* Rotación de Refresh Token.
* Detección de reutilización de Refresh Token.
* Sesiones persistentes en PostgreSQL.
* Revocación de sesión.
* Logout.
* Validación mediante `/auth/me`.
* Renovación automática desde Android.
* Retry de solicitudes después del refresh.

Endpoints principales:

```text
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/me
POST /api/auth/logout
```

---

## API

La API pública se encuentra detrás de HTTPS:

```text
https://api.chirihome.com/api/
```

Endpoint de salud:

```text
GET /api/health
```

Respuesta esperada:

```json
{
  "status": "ok"
}
```

La API requiere autenticación para los recursos protegidos.

---

## Despliegue

El servidor principal utiliza:

```text
Raspberry Pi 4B
├── Debian GNU/Linux
├── Docker
├── PostgreSQL
├── Caddy
├── Cloudflare Tunnel
└── Chiri Backend
```

La infraestructura utiliza dos túneles Cloudflare separados:

```text
chiri-web
└── WEB

chiri-home
└── API
```

La separación permite mantener diferenciados el acceso web y el acceso al Backend.

---

## Seguridad

Las principales medidas implementadas incluyen:

* HTTPS.
* JWT firmado mediante RS256.
* Claves RSA protegidas.
* Password hashing mediante Argon2.
* Sesiones persistentes.
* Revocación de sesiones.
* Rotación de Refresh Token.
* Detección de reutilización de Refresh Token.
* Bearer Token.
* Protección del servicio Backend mediante systemd.
* Ejecución del Backend con usuario dedicado.
* Restricciones de systemd.
* Android utiliza almacenamiento seguro para los tokens.

La documentación detallada de seguridad se encuentra en:

```text
docs/070_Seguridad.md
```

---

## Estado actual

### Completado

* [x] Arquitectura base.
* [x] Modelo inicial de Base de Datos.
* [x] PostgreSQL.
* [x] Backend FastAPI.
* [x] Sistema de autenticación.
* [x] Gestión de sesiones.
* [x] Refresh Token y rotación.
* [x] Logout.
* [x] Cliente Android de autenticación.
* [x] Persistencia segura de sesión.
* [x] Refresh automático en Android.
* [x] Validación E2E de autenticación.
* [x] Despliegue del Backend en Raspberry Pi.
* [x] Caddy.
* [x] Cloudflare.
* [x] API pública HTTPS.
* [x] Integración Android → API HTTPS.

### En implementación

* [ ] UC-002 — Consultar funcionalidades disponibles.
* [ ] Home v1.0.

### Pendiente

* [ ] Multimedia.
* [ ] Inteligencia Artificial.
* [ ] Personal.
* [ ] Configuración.
* [ ] Funcionalidades adicionales según el alcance aprobado de v1.0.

El estado detallado se mantiene en:

```text
docs/STATUS.md
```

---

## Casos de uso

| ID     | Caso de uso                           | Estado               |
| ------ | ------------------------------------- | -------------------- |
| UC-001 | Autenticarse                          | ✅ Completado         |
| UC-002 | Consultar funcionalidades disponibles | 🔄 En implementación |
| UC-003 | Ejecutar funcionalidad                | ⬜ Pendiente          |
| UC-004 | Gestionar usuarios                    | ⬜ Pendiente          |
| UC-005 | Gestionar permisos                    | ⬜ Pendiente          |
| UC-006 | Administrar configuración             | ⬜ Pendiente          |

---

## Estructura del repositorio

```text
Chiri/
│
├── assets/
│
├── docs/
│   ├── 000_Principios.md
│   ├── 010_Proyecto.md
│   ├── 020_Arquitectura.md
│   ├── 030_Backend.md
│   ├── 040_Android.md
│   ├── 050_BaseDatos.md
│   ├── 060_API.md
│   ├── 070_Seguridad.md
│   ├── 080_Despliegue.md
│   ├── 090_GuiaProgramacion.md
│   ├── 100_DecisionesArquitectura.md
│   └── STATUS.md
│
├── source/
│   ├── android/
│   └── server/
│
├── .gitignore
└── README.md
```

---

## Documentación

La documentación técnica y de arquitectura se encuentra en `docs/`.

| Documento                       | Contenido                  |
| ------------------------------- | -------------------------- |
| `000_Principios.md`             | Principios del proyecto    |
| `010_Proyecto.md`               | Definición general         |
| `020_Arquitectura.md`           | Arquitectura               |
| `030_Backend.md`                | Backend                    |
| `040_Android.md`                | Aplicación Android         |
| `050_BaseDatos.md`              | Base de Datos              |
| `060_API.md`                    | Contratos API              |
| `070_Seguridad.md`              | Seguridad                  |
| `080_Despliegue.md`             | Despliegue                 |
| `090_GuiaProgramacion.md`       | Guía de programación       |
| `100_DecisionesArquitectura.md` | Decisiones arquitectónicas |
| `STATUS.md`                     | Estado y seguimiento       |

---

## Desarrollo

### Entorno

El entorno de desarrollo utiliza:

* Windows 11.
* Android Studio.
* VS Code.
* Docker Desktop.
* Git.
* Python.
* Raspberry Pi como servidor de despliegue.

### Flujo de trabajo

El desarrollo sigue este ciclo:

```text
Definir
   ↓
Diseñar
   ↓
Aprobar
   ↓
Implementar
   ↓
Probar
   ↓
Validar
   ↓
Documentar
   ↓
Commit
```

No se implementan componentes anticipadamente solamente porque estén contemplados en la arquitectura.

---

## Principios de desarrollo

1. Mantener el alcance de v1.0 controlado.
2. Implementar únicamente funcionalidades aprobadas.
3. No reimplementar componentes ya terminados.
4. No crear componentes únicamente por estructura teórica.
5. Los cambios de Base de Datos se realizan mediante Alembic.
6. Android no accede directamente a PostgreSQL.
7. Mantener separación entre módulos.
8. Toda funcionalidad debe tener pruebas.
9. Los cambios importantes deben quedar registrados mediante Git.
10. Los cambios arquitectónicos requieren revisión.
11. La documentación debe mantenerse alineada con el código real.
12. Una funcionalidad no se considera terminada únicamente porque el código exista; debe ser probada y validada.
13. No ampliar el alcance sin una decisión explícita.

---

## Repositorio

Código fuente:

[GitHub — Chiri Platform](https://github.com/chirijose79-ui/Chiri?utm_source=chatgpt.com)

Branch principal:

```text
master
```

---

## Licencia

Proyecto personal.

La licencia y condiciones de distribución se definirán según la evolución del proyecto.
