# Chiri Platform

**Documento:** 010_Proyecto.md

**Versión:** 1.0

**Estado:** Borrador

---

# 1. Introducción

Chiri Platform es una plataforma personal diseñada para integrar, centralizar y administrar servicios de domótica, multimedia, inteligencia artificial y herramientas personales mediante una arquitectura modular basada en APIs.

Su propósito es proporcionar una experiencia unificada para el usuario, ocultando la complejidad de los distintos servicios que la componen y ofreciendo un punto único de acceso seguro.

---

# 2. Objetivo General

Construir una plataforma personal que permita administrar múltiples servicios desde una única aplicación y una única API, ejecutándose principalmente en un servidor doméstico y permitiendo el acceso seguro desde dispositivos autorizados.

---

# 3. Objetivos Específicos

La plataforma deberá:

* Centralizar el acceso a todos los servicios.
* Proporcionar una API única para los clientes.
* Integrar servicios existentes sin reemplazarlos.
* Permitir la incorporación de nuevos módulos con un impacto mínimo sobre el resto del sistema.
* Facilitar el mantenimiento y la evolución del proyecto.
* Priorizar la seguridad, la estabilidad y la simplicidad.

---

# 4. Alcance

En su primera versión, Chiri Platform integrará los siguientes componentes:

* Home Assistant
* Music Assistant
* Navidrome
* Jellyfin
* PostgreSQL
* Backend propio
* Aplicación Android

La incorporación de nuevos servicios deberá realizarse respetando la arquitectura definida en el proyecto.

---

# 5. Componentes del Sistema

La plataforma estará compuesta por los siguientes elementos principales:

## Cliente Android

Aplicación oficial para el acceso a la plataforma.

Será responsable de la interacción con el usuario y consumirá exclusivamente la API de Chiri.

---

## Backend Chiri

Núcleo central de la plataforma.

Será responsable de:

* autenticación
* autorización
* integración de servicios
* lógica de negocio
* exposición de la API
* coordinación entre módulos

---

## Servicios Integrados

Cada servicio conservará su responsabilidad original.

Ejemplos:

* Home Assistant para domótica.
* Music Assistant para reproducción musical.
* Navidrome como servidor de biblioteca musical.
* Jellyfin como servidor multimedia.

Chiri actuará como una capa de integración entre ellos.

---

## Base de Datos

PostgreSQL almacenará la información propia de Chiri, incluyendo:

* usuarios
* configuración
* preferencias
* auditoría
* datos internos de la plataforma

No reemplazará las bases de datos utilizadas por los servicios integrados.

---

# 6. Usuarios

La plataforma está diseñada inicialmente para uso personal y familiar.

No se contempla, en esta versión, un entorno multiempresa o de uso comercial.

La arquitectura, sin embargo, permitirá futuras ampliaciones si fueran necesarias.

---

# 7. Casos de Uso Generales

Entre las capacidades esperadas de Chiri se encuentran:

* Controlar dispositivos del hogar.
* Gestionar la reproducción multimedia.
* Consultar información de los servicios integrados.
* Centralizar notificaciones.
* Ejecutar automatizaciones.
* Integrar asistentes de inteligencia artificial.
* Administrar configuraciones desde un único punto.

Estos casos de uso podrán ampliarse sin alterar la arquitectura base.

---

# 8. Principios Funcionales

Toda funcionalidad desarrollada deberá cumplir los siguientes criterios:

* Utilizar la API de Chiri.
* Respetar la separación de responsabilidades.
* No acceder directamente a los servicios desde los clientes.
* Mantener la consistencia de la experiencia de usuario.
* Minimizar el acoplamiento entre módulos.

---

# 9. Restricciones

El proyecto adopta las siguientes restricciones técnicas:

* Backend desarrollado en Python con FastAPI.
* Aplicación Android desarrollada en Kotlin con Jetpack Compose.
* Base de datos PostgreSQL.
* Infraestructura basada en Docker y Docker Compose.
* Entorno de producción sobre Raspberry Pi.

Estas restricciones forman parte de la arquitectura aprobada para la versión 1.0.

---

# 10. Exclusiones

No forman parte del alcance de Chiri Platform:

* Desarrollar un sistema operativo.
* Reemplazar Home Assistant.
* Reemplazar Music Assistant.
* Reemplazar Navidrome.
* Reemplazar Jellyfin.
* Implementar funcionalidades propias de esos servicios cuando ya estén disponibles en ellos.

---

# 11. Criterios de Calidad

La plataforma deberá priorizar:

* Fiabilidad.
* Mantenibilidad.
* Modularidad.
* Seguridad.
* Escalabilidad.
* Simplicidad.
* Bajo acoplamiento.
* Alta cohesión.

---

# 12. Evolución del Proyecto

La evolución de Chiri Platform deberá realizarse mediante la incorporación de nuevos módulos o integraciones, evitando modificaciones que afecten a la arquitectura central.

Las decisiones relevantes serán registradas en el documento de Decisiones de Arquitectura (ADR), garantizando la trazabilidad técnica del proyecto.

---

# 13. Declaración Final

Chiri Platform constituye una plataforma de integración personal cuyo propósito es unificar el acceso a servicios especializados mediante una arquitectura modular, segura y mantenible.

Su evolución se basará en la incorporación ordenada de nuevas capacidades, preservando la estabilidad de la arquitectura y respetando los principios establecidos en `000_Principios.md`.
