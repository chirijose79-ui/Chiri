# Chiri Platform

**Documento:** 000_Principios.md

**Versión:** 1.0

**Estado:** Aprobado

---

# 1. Propósito

Este documento establece los principios fundamentales que gobiernan el diseño, desarrollo y evolución de Chiri Platform.

Su objetivo es garantizar que todas las decisiones técnicas del proyecto sean coherentes con una arquitectura modular, mantenible, segura y escalable.

Los principios definidos aquí tienen prioridad sobre cualquier decisión de implementación.

---

# 2. Visión

Chiri Platform es una plataforma personal de integración de servicios que centraliza el acceso a sistemas de domótica, multimedia, inteligencia artificial y servicios personales mediante una arquitectura basada en APIs.

Chiri no reemplaza los sistemas especializados existentes, sino que los integra bajo una única capa de acceso.

---

# 3. Objetivo

Diseñar, desarrollar y mantener una plataforma personal, modular y escalable que permita administrar diversos servicios desde una única aplicación, ejecutándose principalmente sobre un servidor doméstico y ofreciendo acceso seguro desde dispositivos autorizados.

Este objetivo permanecerá estable durante todo el ciclo de vida del proyecto.

---

# 4. Filosofía

Los siguientes principios representan la filosofía del proyecto.

## 4.1 Chiri es una plataforma

Chiri no es una aplicación.

La aplicación Android es únicamente uno de los clientes oficiales de la plataforma.

En el futuro podrán existir otros clientes sin modificar la arquitectura del sistema.

Ejemplos:

* Aplicación Android
* Aplicación Web
* Cliente de escritorio
* Integraciones externas
* Asistentes de voz
* Automatizaciones

Todos consumirán la misma API.

---

## 4.2 La API es el centro del sistema

Ningún cliente accederá directamente a Home Assistant, Music Assistant, Navidrome, Jellyfin u otros servicios.

Toda comunicación deberá realizarse mediante la API de Chiri.

Esto permite:

* mayor seguridad
* control de permisos
* desacoplamiento
* auditoría
* evolución independiente de los servicios

---

## 4.3 Integración antes que reemplazo

Chiri aprovechará soluciones maduras existentes.

No desarrollará funcionalidades que ya resuelven adecuadamente otras plataformas.

Su responsabilidad será integrarlas de forma uniforme.

---

## 4.4 Modularidad

Cada módulo deberá tener una única responsabilidad.

Los módulos podrán evolucionar de forma independiente siempre que respeten los contratos definidos por la API.

---

## 4.5 Simplicidad

Toda solución deberá ser lo más simple posible.

La complejidad solo se aceptará cuando aporte un beneficio técnico claro.

---

## 4.6 Documentación como fuente de verdad

La documentación representa el comportamiento esperado del sistema.

El código deberá implementarla.

Si existe una diferencia entre ambos, primero se revisará la documentación antes de modificar el software.

---

# 5. Principios de Arquitectura

Toda decisión deberá respetar los siguientes principios.

## Arquitectura antes que código

No se implementará una funcionalidad sin haber sido diseñada previamente.

## Documentación antes de implementación

Todo componente importante deberá documentarse antes de desarrollarse.

## API First

Toda funcionalidad deberá exponerse mediante una API bien definida.

## Bajo acoplamiento

Los módulos dependerán de interfaces y contratos, no de implementaciones concretas.

## Alta cohesión

Cada componente deberá encargarse de una responsabilidad específica.

## Reutilización

Siempre que sea posible se reutilizarán componentes existentes.

## Escalabilidad

La arquitectura deberá permitir incorporar nuevos servicios sin rediseñar el sistema completo.

## Seguridad por defecto

La configuración inicial del sistema deberá priorizar la seguridad.

---

# 6. Principios de Desarrollo

Todo desarrollo deberá cumplir las siguientes reglas.

* Código legible.
* Código mantenible.
* Código reutilizable.
* Código documentado.
* Funciones pequeñas.
* Clases con una responsabilidad.
* Evitar duplicación.
* Evitar dependencias innecesarias.
* Preferir soluciones estándar.
* Minimizar deuda técnica.

---

# 7. Principios de Organización

La estructura del proyecto responde a responsabilidades claramente definidas.

Cada carpeta tendrá un único propósito.

Cada documento describirá un único tema.

Cada módulo será independiente siempre que sea posible.

---

# 8. Alcance

Chiri integrará plataformas existentes, entre ellas:

* Home Assistant
* Music Assistant
* Navidrome
* Jellyfin
* PostgreSQL
* Servicios de Inteligencia Artificial
* Servicios propios de Chiri

---

# 9. Fuera de Alcance

No forman parte de los objetivos del proyecto:

* desarrollar un servidor multimedia
* desarrollar un servidor de domótica
* reemplazar Home Assistant
* reemplazar Music Assistant
* reemplazar Navidrome
* reemplazar Jellyfin
* duplicar funcionalidades existentes en esos sistemas

---

# 10. Metodología

Todo desarrollo seguirá obligatoriamente el siguiente ciclo:

1. Documentar.
2. Diseñar.
3. Programar.
4. Probar.
5. Desplegar.

No se alterará este orden salvo casos excepcionales debidamente justificados.

---

# 11. Criterio para Nuevas Funcionalidades

Antes de iniciar cualquier desarrollo deberá responderse la siguiente pregunta:

> ¿Esta funcionalidad contribuye directamente al objetivo principal de Chiri Platform?

Si la respuesta es negativa, la funcionalidad no formará parte del proyecto.

---

# 12. Estabilidad de la Arquitectura

La arquitectura de Chiri Platform v1.0 se considera estable.

Las tecnologías, organización del repositorio y decisiones arquitectónicas solo podrán modificarse cuando exista una justificación técnica basada en:

* seguridad
* rendimiento
* mantenibilidad
* escalabilidad

No se realizarán cambios motivados únicamente por tendencias o preferencias tecnológicas.

---

# 13. Principio Rector

Toda decisión del proyecto deberá contribuir a construir una plataforma personal robusta, coherente y sostenible en el tiempo.

Este principio prevalece sobre cualquier decisión de implementación particular.
