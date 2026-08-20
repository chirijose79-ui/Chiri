# Chiri Platform

**Chiri Platform v1.0**

Plataforma personal de integración de servicios, diseñada para centralizar el acceso y la interacción con diferentes capacidades del entorno Chiri.

---

## 1. Estado del proyecto

**Estado:** En desarrollo

La arquitectura y las decisiones fundamentales de Chiri Platform v1.0 han sido definidas y aprobadas.

La implementación de software se encuentra en preparación.

### Documentación arquitectónica

| Documento | Estado |
|---|---|
| `000_Principios.md` | ✅ Aprobado |
| `010_Proyecto.md` | ✅ Aprobado |
| `020_Arquitectura.md` | ✅ Aprobado |
| `030_Backend.md` | ✅ Aprobado |
| `040_Android.md` | ✅ Aprobado |
| `050_BaseDatos.md` | ✅ Aprobado |
| `060_API.md` | ✅ Aprobado |
| `070_Seguridad.md` | ✅ Aprobado |
| `080_Despliegue.md` | ✅ Aprobado |
| `090_GuiaProgramacion.md` | ✅ Aprobado |
| `100_DecisionesArquitectura.md` | ✅ Aprobado |

Estos documentos constituyen la base arquitectónica oficial de Chiri Platform v1.0.

---

# 2. Arquitectura

Chiri Platform utiliza una arquitectura basada en separación de responsabilidades.

```text
                    ┌─────────────────────┐
                    │   Android Chiri     │
                    │      Cliente        │
                    └──────────┬──────────┘
                               │
                              HTTPS
                               │
                               ▼
                    ┌─────────────────────┐
                    │      API Chiri      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Backend Chiri    │
                    │  Lógica de negocio │
                    └───────┬─────┬───────┘
                            │     │
                            │     └──────────────────┐
                            │                        │
                            ▼                        ▼
                  ┌─────────────────┐      ┌────────────────────┐
                  │   PostgreSQL    │      │ Servicios externos │
                  │   Persistencia  │      │ / integraciones    │
                  └─────────────────┘      └────────────────────┘
````

El cliente Android no accede directamente a PostgreSQL ni a los servicios internos.

El Backend es responsable de la lógica de negocio y constituye el punto de acceso a la persistencia y a las integraciones.

---

# 3. Componentes principales

## Android

Aplicación cliente oficial de Chiri Platform.

Tecnologías principales:

* Kotlin
* Jetpack Compose
* Material 3
* MVVM
* Gradle

Paquete Android:

```text
com.chirihome.platform
```

El proyecto Android se encuentra en:

```text
source/android/
```

---

## API

Capa de comunicación entre los clientes y el Backend.

Características principales:

* REST
* JSON
* HTTPS
* Versionamiento de API
* OpenAPI
* Autenticación y autorización
* Validación de solicitudes
* Manejo consistente de errores

La API utiliza el espacio:

```text
/api/v1/
```

---

## Backend

Núcleo de la plataforma.

Responsabilidades:

* lógica de negocio;
* validación;
* seguridad;
* acceso a PostgreSQL;
* integración con servicios externos;
* auditoría;
* coordinación de operaciones.

Tecnología principal:

```text
Python
FastAPI
```

---

## PostgreSQL

PostgreSQL es el sistema de persistencia oficial de Chiri Platform v1.0.

El acceso a PostgreSQL se realiza exclusivamente desde el Backend.

```text
Backend
   │
   ▼
PostgreSQL
```

Chiri Platform no accede directamente a las bases de datos internas de los servicios integrados.

---

## Servicios integrados

El Backend puede integrarse con diferentes servicios del entorno Chiri.

Entre ellos pueden encontrarse:

* Home Assistant
* Music Assistant
* Navidrome
* Jellyfin
* servicios de inteligencia artificial

Las integraciones son responsabilidad del Backend.

Los clientes no acceden directamente a estos servicios.

---

# 4. Estructura del repositorio

La estructura principal del repositorio es:

```text
Chiri/
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
│   └── 100_DecisionesArquitectura.md
│
├── source/
│   └── android/
│
└── README.md
```

La implementación de Backend, API, base de datos e integraciones se incorporará progresivamente durante el desarrollo.

---

# 5. Documentación

## Arquitectura y fundamentos

La documentación oficial de la arquitectura se encuentra en:

```text
docs/
```

### Documentos aprobados

```text
000_Principios.md
010_Proyecto.md
020_Arquitectura.md
030_Backend.md
040_Android.md
050_BaseDatos.md
060_API.md
070_Seguridad.md
080_Despliegue.md
090_GuiaProgramacion.md
100_DecisionesArquitectura.md
```

Estos documentos definen:

* principios del proyecto;
* arquitectura;
* Backend;
* Android;
* PostgreSQL;
* API;
* seguridad;
* despliegue;
* reglas de programación;
* decisiones arquitectónicas.

---

# 6. Implementación

La implementación de Chiri Platform v1.0 se realizará progresivamente a partir de la arquitectura aprobada.

Orden general previsto:

```text
Arquitectura aprobada
        │
        ▼
Preparación del entorno
        │
        ▼
Backend
        │
        ▼
PostgreSQL
        │
        ▼
API
        │
        ▼
Android
        │
        ▼
Integraciones
        │
        ▼
Funcionalidades de Chiri
```

Los componentes se desarrollarán de forma incremental y verificable.

---

# 7. Desarrollo Android

El proyecto Android existente se encuentra en:

```text
source/android/
```

Configuración principal:

```text
Namespace:
com.chirihome.platform

Application ID:
com.chirihome.platform
```

La arquitectura Android seguirá:

```text
UI
 │
 ▼
ViewModel
 │
 ▼
Use Case
 │
 ▼
Repository
 │
 ▼
Data
 ├── API
 └── almacenamiento local
```

La aplicación consumirá exclusivamente las capacidades expuestas por la API de Chiri.

---

# 8. Seguridad

La seguridad es un principio transversal de la plataforma.

Se consideran, entre otros:

* autenticación;
* autorización;
* HTTPS;
* protección de credenciales;
* gestión de secretos;
* validación de entradas;
* protección de API;
* auditoría;
* actualización controlada;
* protección de datos.

Los secretos y credenciales no deben almacenarse en el repositorio.

---

# 9. Base de datos

Chiri Platform v1.0 utiliza:

```text
PostgreSQL
```

El acceso se realiza exclusivamente mediante el Backend.

```text
Android
   │
   ▼
API
   │
   ▼
Backend
   │
   ▼
PostgreSQL
```

Los servicios integrados mantienen sus propios datos y bases de datos cuando corresponda.

Chiri no debe duplicar innecesariamente esos datos.

---

# 10. Ambientes

La plataforma contempla diferentes ambientes:

```text
DESARROLLO
PRUEBAS
PRODUCCIÓN
```

Cada ambiente deberá mantener su propia configuración.

La configuración sensible no debe formar parte del código fuente.

---

# 11. Git

El proyecto utiliza Git para control de versiones.

Los commits siguen Conventional Commits.

Ejemplos:

```text
feat: agregar nueva funcionalidad
fix: corregir error
docs: actualizar documentación
chore: actualizar configuración
```

Los cambios que modifiquen decisiones arquitectónicas deben registrarse en:

```text
docs/100_DecisionesArquitectura.md
```

---

# 12. Principios de desarrollo

El desarrollo debe mantener:

* separación de responsabilidades;
* código simple;
* mantenibilidad;
* seguridad;
* pruebas;
* validación;
* trazabilidad;
* documentación de decisiones importantes.

La implementación debe respetar la arquitectura aprobada.

No se deben introducir tecnologías o patrones que contradigan las decisiones arquitectónicas sin registrar primero la correspondiente decisión.

---

# 13. Estado actual

Actualmente:

```text
Documentación arquitectónica    ✅ Aprobada
Proyecto Android                🟡 Base inicial creada
Backend                         ⏳ Pendiente de implementación
API                             ⏳ Pendiente de implementación
PostgreSQL                      ⏳ Pendiente de implementación
Integraciones                   ⏳ Pendiente de implementación
Funcionalidades                 ⏳ Pendientes
```

El siguiente objetivo de desarrollo es establecer la base técnica ejecutable de Chiri Platform v1.0.

---

# 14. Próximos pasos

El desarrollo continuará de forma incremental:

1. Alinear la implementación Android con la arquitectura aprobada.
2. Preparar la estructura inicial del Backend.
3. Preparar PostgreSQL.
4. Implementar la API base.
5. Implementar pruebas iniciales.
6. Conectar Android con la API.
7. Implementar las primeras capacidades funcionales.
8. Incorporar las integraciones de servicios.

---

# 15. Proyecto

**Chiri Platform v1.0**

Repositorio:

```text
chirijose79-ui/Chiri
```

Dominio:

```text
chirihome.com
```

Android Application ID:

```text
com.chirihome.platform
```