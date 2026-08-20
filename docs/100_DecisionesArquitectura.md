# 100_DecisionesArquitectura.md

# Decisiones Arquitectónicas Chiri Platform v1.0

## 1. Objetivo

Registrar las decisiones arquitectónicas importantes tomadas durante el diseño de Chiri Platform v1.0.

Este documento permite:

* Conocer el motivo detrás de cada decisión.
* Mantener consistencia durante la implementación.
* Evitar cambios arquitectónicos no evaluados.
* Facilitar la evolución futura de la plataforma.

---

# 2. Formato de Decisiones

Cada decisión contiene:

* Identificador.
* Descripción.
* Contexto.
* Decisión tomada.
* Justificación.
* Impacto.

---

# ADR-001

## Separación por Capas de la Arquitectura

### Contexto

El sistema requiere mantener separación clara entre:

* Aplicación Android.
* API.
* Backend.
* Base de Datos.

### Decisión

Se adopta una arquitectura por capas:

```mermaid
flowchart TD
    Android --> API
    API --> Backend
    Backend --> BaseDatos

    Android["Aplicación Android"]
    API["API Chiri Platform"]
    Backend["Backend"]
    BaseDatos["Base de Datos"]
```

### Justificación

Permite:

* Independencia entre componentes.
* Mantenimiento simplificado.
* Evolución tecnológica.
* Mejor control de responsabilidades.

### Impacto

Positivo:

* Mayor escalabilidad.
* Código organizado.

Sí, **así está correcto**. ✅

Ahora ADR-002 queda coherente con `090_GuiaProgramacion.md`:

* No obliga a usar Controller/Service/Repository.
* Permite esos patrones cuando sean apropiados.
* Mantiene separadas las responsabilidades.
* No contradice `020_Arquitectura.md` ni `030_Backend.md`.

Solo haría **un pequeño ajuste de precisión** en el diagrama: `BaseDatos` debería llamarse **PostgreSQL**, porque ya es una decisión confirmada para Chiri Platform.

Te recomiendo dejarlo así:

````markdown
---

# ADR-002

## Separación de Responsabilidades Backend

### Contexto

El Backend deberá mantener separadas las responsabilidades de entrada, lógica de negocio y acceso a datos.

La implementación podrá utilizar patrones como Controller, Service y Repository cuando sean apropiados.

### Decisión

Se establece la separación de responsabilidades:

```mermaid
flowchart TD

    Controller --> Service
    Service --> Repository
    Repository --> PostgreSQL

    Controller["Entrada / Controller"]
    Service["Reglas de Negocio"]
    Repository["Acceso a Datos"]
    PostgreSQL["PostgreSQL"]
````

La utilización de Controller, Service y Repository será una decisión de implementación y no una obligación estructural para todos los módulos.

### Justificación

Cada componente mantiene una responsabilidad definida.

La separación permite evolucionar la implementación sin modificar las responsabilidades principales del Backend.

### Impacto

Facilita:

* Pruebas.
* Mantenimiento.
* Reutilización.
* Evolución del sistema.
* Separación de responsabilidades.

---

# ADR-003

## API como Punto Único de Comunicación

### Contexto

Los clientes externos no deben acceder directamente a la lógica interna.

### Decisión

Toda comunicación externa pasa por la API.

```mermaid
flowchart TD
    Cliente --> API
    API --> Backend

    Cliente["Aplicación Cliente"]
    API["API"]
    Backend["Backend"]
```

### Justificación

Permite:

* Seguridad.
* Control de acceso.
* Validación centralizada.

### Impacto

Mayor control sobre integraciones futuras.

---

# ADR-004

## Base de Datos como Capa Persistente Independiente

### Contexto

La información debe mantenerse independiente de los componentes consumidores.

### Decisión

La persistencia de Chiri Platform v1.0 utilizará PostgreSQL.

El acceso a PostgreSQL se realizará exclusivamente desde el Backend.

```mermaid
flowchart TD

    Backend --> PostgreSQL

    Backend["Backend"]
    PostgreSQL["PostgreSQL"]
```

### Justificación

Evita el acceso directo desde los clientes y mantiene la persistencia bajo el control del Backend.

### Impacto

* Mayor seguridad.
* Mayor integridad de los datos.
* Separación de responsabilidades.
* Facilita la evolución de la Base de Datos.

---

# ADR-005

## Seguridad Integrada desde Arquitectura

### Contexto

La seguridad debe formar parte del diseño inicial.

### Decisión

Se incorporan:

* Autenticación.
* Autorización.
* Tokens.
* Auditoría.
* Validaciones.

```mermaid
flowchart TD
    Usuario --> Autenticacion
    Autenticacion --> Autorizacion
    Autorizacion --> Sistema

    Usuario["Usuario"]
    Autenticacion["Identidad"]
    Autorizacion["Permisos"]
    Sistema["Chiri Platform"]
```

### Justificación

Evita implementar seguridad como una capa posterior.

### Impacto

Mayor protección del sistema.

---

# ADR-006

## Mermaid como Estándar de Diagramación Arquitectónica

### Contexto

La documentación necesita diagramas versionables y mantenibles.

### Decisión

Todos los diagramas arquitectónicos utilizarán Mermaid.

Ejemplo:

```mermaid
flowchart TD
    A --> B
```

### Justificación

Permite:

* Versionamiento junto al Markdown.
* Fácil mantenimiento.
* Visualización independiente del editor.

### Impacto

Toda documentación arquitectónica seguirá un estándar único.

---

# ADR-007

## Separación entre Arquitectura y Diseño UX/UI

### Contexto

La arquitectura debe permanecer independiente de decisiones visuales.

### Decisión

Los documentos arquitectónicos no incluyen:

* Pantallas.
* Mockups.
* Diseños visuales.

### Justificación

Permite evolucionar la experiencia de usuario sin modificar la arquitectura base.

### Impacto

Mayor flexibilidad del proyecto.

---

# ADR-008

## Guía de Programación como Estándar de Desarrollo

### Contexto

El crecimiento del proyecto requiere consistencia técnica.

### Decisión

Se establece:

`090_GuiaProgramacion.md`

como referencia obligatoria para desarrollo.

### Justificación

Mantiene:

* Convenciones.
* Organización.
* Calidad del código.

### Impacto

Mayor mantenibilidad.

---

# ADR-009

## Arquitectura de Despliegue Flexible

### Contexto

La plataforma debe permitir diferentes ambientes.

### Decisión

El despliegue se mantiene independiente de infraestructura específica.

### Justificación

Permite evolucionar hacia:

* Servidores tradicionales.
* Contenedores.
* Cloud.
* Alta disponibilidad.

### Impacto

Mayor capacidad de adaptación.

---

Versión:

```text
Chiri Platform v1.0
```

Estado:

```text
APROBADO
```
