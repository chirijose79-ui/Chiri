# Chiri Platform

**Documento:** 030_Backend.md

**Versión:** 1.0

**Estado:** Borrador

---

# 1. Introducción

El Backend de Chiri Platform constituye el núcleo central de la plataforma.

Su responsabilidad principal es proporcionar una capa de integración, coordinación y control entre los clientes de Chiri y los diferentes servicios que forman parte del ecosistema.

El Backend no reemplaza las funcionalidades propias de los servicios integrados.

Su función es:

* centralizar el acceso,
* aplicar reglas de negocio,
* gestionar seguridad,
* transformar información,
* coordinar servicios,
* proporcionar una experiencia uniforme a los clientes.

---

# 1.1 Objetivo del Backend

El objetivo del Backend es proporcionar una API estable y segura que permita a los diferentes clientes interactuar con la plataforma sin conocer la implementación interna de los servicios integrados.

El Backend debe ocultar la complejidad del ecosistema y ofrecer una interfaz coherente para:

* domótica,
* multimedia,
* inteligencia artificial,
* servicios personales.

---

# 1.2 Responsabilidad Principal

El Backend será responsable de:

* Exponer la API de Chiri.
* Gestionar autenticación y autorización.
* Ejecutar lógica de negocio.
* Coordinar módulos internos.
* Integrar servicios externos.
* Gestionar datos propios de la plataforma.
* Registrar eventos importantes.
* Mantener contratos estables con los clientes.

---

# 1.3 Lo que NO hará el Backend

El Backend no será responsable de:

* Reproducir música directamente.
* Administrar dispositivos físicos directamente.
* Gestionar bibliotecas multimedia propias.
* Sustituir servicios especializados.
* Ejecutar lógica perteneciente a plataformas externas.

Ejemplos:

La reproducción musical pertenece a Music Assistant.

La gestión de dispositivos inteligentes pertenece a Home Assistant.

La gestión multimedia pertenece a Jellyfin.

Chiri coordina estas capacidades.

---

# 1.4 Tecnología Base

La tecnología definida para el Backend es:

* Lenguaje: Python.
* Framework API: FastAPI.
* Base de datos: PostgreSQL.
* Contenedores: Docker.

El diseño deberá aprovechar las capacidades del ecosistema Python manteniendo una arquitectura limpia y mantenible.

---

# 1.5 Principio Rector

El Backend deberá seguir el siguiente principio:

> El Backend de Chiri no debe ser un conjunto de integraciones conectadas; debe ser una plataforma organizada que utiliza integraciones como capacidades.

---

# 1.6 Relación con la Arquitectura General

El Backend implementa la capa central definida en:

`020_Arquitectura.md`

Su posición dentro del sistema es:

```mermaid id="c8g4ks"
flowchart TB

    Clientes["Clientes Chiri"]

    API["API FastAPI"]

    Backend["Backend Chiri"]

    Integraciones["Módulos de Integración"]

    Servicios["Servicios Externos"]

    Clientes --> API
    API --> Backend
    Backend --> Integraciones
    Integraciones --> Servicios
```

---

# 1.7 Estado del Documento

Este documento definirá las reglas internas del Backend y será la referencia para la implementación posterior.

No contiene código.

Define diseño, responsabilidades y organización.

# 2. Arquitectura Interna del Backend

El Backend de Chiri Platform estará organizado mediante una arquitectura por capas, donde cada capa tendrá responsabilidades específicas y límites claramente definidos.

El objetivo es mantener:

* bajo acoplamiento,
* alta cohesión,
* facilidad de mantenimiento,
* posibilidad de evolución futura.

La estructura interna seguirá el principio:

> Cada capa conoce únicamente las capas necesarias para cumplir su responsabilidad.

---

# 2.1 Vista General de Capas

La arquitectura interna del Backend será:

```mermaid id="8p7s3m"
flowchart TB

    API["API Layer<br/>FastAPI"]

    Application["Application Layer<br/>Casos de Uso"]

    Domain["Domain Layer<br/>Reglas de Negocio"]

    Integration["Integration Layer<br/>Adaptadores Externos"]

    Data["Data Layer<br/>Persistencia"]

    Infrastructure["Infrastructure Layer<br/>Configuración y Servicios Técnicos"]


    API --> Application
    Application --> Domain
    Application --> Integration
    Application --> Data
    Infrastructure --> Data
    Infrastructure --> Integration
```

---

# 2.2 API Layer

## Responsabilidad

La capa API representa el punto de entrada del Backend.

Su función es recibir solicitudes externas y devolver respuestas mediante contratos definidos.

## Responsabilidades

* Definir endpoints.
* Validar solicitudes.
* Gestionar modelos de entrada y salida.
* Controlar códigos de respuesta.
* Gestionar dependencias de autenticación.

## No debe contener:

* Lógica de negocio.
* Consultas complejas a base de datos.
* Comunicación directa con servicios externos.

---

# 2.3 Application Layer

## Responsabilidad

La capa de aplicación coordina las operaciones principales de Chiri.

Representa los casos de uso del sistema.

Ejemplos:

* Encender dispositivo.
* Reproducir música.
* Consultar estado del hogar.
* Gestionar preferencias.

## Responsabilidades

* Coordinar procesos.
* Ejecutar casos de uso.
* Utilizar servicios internos.
* Coordinar integraciones.

## No debe contener:

* Detalles técnicos de APIs externas.
* Código específico de base de datos.

---

# 2.4 Domain Layer

## Responsabilidad

La capa de dominio contiene las reglas propias de Chiri.

Representa la lógica que pertenece a la plataforma y no a servicios externos.

Ejemplos:

* permisos.
* usuarios.
* preferencias.
* reglas internas.

## Principio

El dominio de Chiri debe ser independiente de tecnologías externas.

---

# 2.5 Integration Layer

## Responsabilidad

La capa de integración contiene los adaptadores hacia servicios externos.

Ejemplos:

* Home Assistant.
* Music Assistant.
* Navidrome.
* Jellyfin.
* Servicios IA.

## Responsabilidades

* Comunicación externa.
* Transformación de datos.
* Manejo de errores externos.
* Ocultar detalles del servicio.

## Ejemplo conceptual:

```mermaid id="l8n5q2"
flowchart LR

    UseCase["Caso de Uso Chiri"]

    Adapter["Home Assistant Adapter"]

    External["Home Assistant API"]

    UseCase --> Adapter
    Adapter --> External
```

---

# 2.6 Data Layer

## Responsabilidad

La capa de datos administra la persistencia propia de Chiri.

## Responsabilidades

* Acceso a PostgreSQL.
* Modelos persistentes.
* Consultas.
* Migraciones.

## Principio

La capa de datos no debe contener reglas de negocio.

---

# 2.7 Infrastructure Layer

## Responsabilidad

Contiene elementos técnicos necesarios para ejecutar la plataforma.

Ejemplos:

* configuración.
* variables de entorno.
* logs.
* clientes HTTP.
* seguridad técnica.
* utilidades compartidas.

---

# 2.8 Regla de Dependencias

Las dependencias deberán seguir una dirección controlada:

```mermaid id="5f7n0q"
flowchart LR

    API --> Application

    Application --> Domain

    Application --> Integration

    Application --> Data

    Infrastructure --> Integration

    Infrastructure --> Data
```

Las capas internas no deberán depender de detalles externos.

---

# 2.9 Beneficios de esta Organización

Esta arquitectura permite:

* cambiar una integración sin afectar el dominio.
* cambiar PostgreSQL sin modificar reglas de negocio.
* crear nuevos clientes sin modificar lógica interna.
* realizar pruebas más simples.
* mantener código organizado.

---

# 2.10 Principio Arquitectónico

La implementación del Backend deberá priorizar:

> La lógica de Chiri debe permanecer independiente de las tecnologías utilizadas para ejecutarla.

# 3. Estructura del Proyecto Backend

La estructura del Backend de Chiri Platform estará organizada para reflejar las responsabilidades definidas en la arquitectura interna.

La organización deberá favorecer:

* separación de responsabilidades,
* facilidad de navegación,
* mantenimiento a largo plazo,
* incorporación de nuevos módulos.

---

# 3.1 Estructura General

La carpeta del Backend será:

```
server/
│
├── app/
│
├── tests/
│
├── migrations/
│
├── scripts/
│
├── Dockerfile
│
├── requirements.txt
│
├── docker-compose.yml
│
└── README.md
```

---

# 3.2 Carpeta Principal `app/`

Contendrá todo el código fuente de Chiri Backend.

Estructura:

```
app/

├── api/
│
├── application/
│
├── domain/
│
├── integrations/
│
├── infrastructure/
│
├── database/
│
├── security/
│
├── config/
│
└── main.py
```

---

# 3.3 `api/`

## Responsabilidad

Contiene la capa de exposición HTTP mediante FastAPI.

Ejemplo:

```
api/

├── routes/
├── schemas/
└── dependencies/
```

Contendrá:

* endpoints.
* modelos de entrada.
* modelos de salida.
* dependencias HTTP.

No contendrá:

* reglas de negocio.
* consultas directas.
* llamadas directas a servicios externos.

---

# 3.4 `application/`

## Responsabilidad

Contiene los casos de uso de Chiri.

Ejemplo:

```
application/

├── services/
├── use_cases/
└── dto/
```

Contendrá:

* coordinación de operaciones.
* flujo de procesos.
* lógica de aplicación.

---

# 3.5 `domain/`

## Responsabilidad

Contiene las reglas propias de Chiri.

Ejemplo:

```
domain/

├── models/
├── entities/
├── value_objects/
└── exceptions/
```

Contendrá:

* entidades.
* reglas de negocio.
* conceptos centrales.

No dependerá de:

* FastAPI.
* PostgreSQL.
* APIs externas.

---

# 3.6 `integrations/`

## Responsabilidad

Contiene adaptadores para servicios externos.

Ejemplo:

```
integrations/

├── home_assistant/
├── music_assistant/
├── navidrome/
├── jellyfin/
└── ai/
```

Cada integración deberá ser independiente.

---

# 3.7 `database/`

## Responsabilidad

Gestiona la persistencia de Chiri.

Ejemplo:

```
database/

├── models/
├── repositories/
└── migrations/
```

Contendrá:

* modelos ORM.
* repositorios.
* conexión a PostgreSQL.

---

# 3.8 `security/`

## Responsabilidad

Agrupa componentes relacionados con seguridad.

Ejemplo:

```
security/

├── authentication/
├── authorization/
└── tokens/
```

---

# 3.9 `config/`

## Responsabilidad

Gestiona configuración del sistema.

Ejemplo:

```
config/

├── settings.py
└── constants.py
```

---

# 3.10 `main.py`

Será el punto de entrada del Backend.

Responsabilidades:

* crear aplicación FastAPI.
* cargar configuración.
* registrar rutas.
* inicializar componentes necesarios.

No contendrá lógica de negocio.

---

# 3.11 Pruebas

La carpeta:

```
tests/
```

contendrá pruebas organizadas según capas:

```
tests/

├── api/
├── application/
├── domain/
└── integrations/
```

---

# 3.12 Principio de Organización

La estructura del código deberá reflejar la arquitectura.

No se crearán carpetas por comodidad temporal.

Cada nuevo componente deberá ubicarse según su responsabilidad.

---

# 3.13 Regla Arquitectónica

Antes de crear un nuevo archivo o módulo deberá responderse:

> ¿A qué responsabilidad pertenece este componente?

Si no existe una ubicación clara, primero deberá revisarse el diseño antes de programar.

# 4. Módulos Funcionales del Backend

El Backend de Chiri Platform estará organizado mediante módulos funcionales independientes.

Cada módulo representa una capacidad específica de la plataforma y deberá mantener una responsabilidad claramente definida.

Los módulos funcionales utilizarán las capas internas definidas anteriormente:

* API Layer.
* Application Layer.
* Domain Layer.
* Integration Layer.
* Data Layer.

---

# 4.1 Módulo de Usuarios

## Responsabilidad

Gestionar los usuarios que interactúan con Chiri Platform.

## Funciones principales

* Creación de usuarios.
* Gestión de perfiles.
* Administración de estados.
* Preferencias personales.
* Asociación de permisos.

## No será responsable de:

* Autenticación técnica.
* Comunicación con servicios externos.

---

# 4.2 Módulo de Autenticación y Autorización

## Responsabilidad

Controlar el acceso seguro a la plataforma.

## Funciones principales

* Inicio de sesión.
* Validación de identidad.
* Gestión de sesiones.
* Control de permisos.
* Protección de recursos.

## Integración

Este módulo estará relacionado con:

* Base de datos PostgreSQL.
* Sistema de seguridad.
* API de Chiri.

---

# 4.3 Módulo de Domótica

## Responsabilidad

Proporcionar una capa de integración con sistemas de automatización del hogar.

## Integración principal

* Home Assistant.

## Funciones posibles

* Consultar estados.
* Ejecutar acciones.
* Obtener información de dispositivos.
* Integrar eventos relevantes.

## No será responsable de:

* Administrar dispositivos directamente.
* Crear un motor propio de automatización.

---

# 4.4 Módulo Multimedia

## Responsabilidad

Centralizar las capacidades multimedia disponibles en Chiri.

## Integraciones previstas

* Music Assistant.
* Navidrome.
* Jellyfin.

## Funciones posibles

* Consultar contenido.
* Gestionar reproducción.
* Mostrar información multimedia.
* Coordinar experiencias entre servicios.

## No será responsable de:

* Servir contenido multimedia.
* Administrar bibliotecas externas.

---

# 4.5 Módulo de Inteligencia Artificial

## Responsabilidad

Proporcionar capacidades inteligentes a la plataforma.

## Funciones futuras posibles

* Asistente conversacional.
* Interpretación de lenguaje natural.
* Automatizaciones inteligentes.
* Análisis de información.
* Interacción por voz.

## Principio

El módulo deberá permitir integrar diferentes proveedores de IA sin acoplar el sistema a uno específico.

---

# 4.6 Módulo de Configuración

## Responsabilidad

Gestionar la configuración propia de Chiri.

## Funciones principales

* Preferencias del sistema.
* Configuración de módulos.
* Parámetros generales.
* Estado de componentes.

## Principio

La configuración de Chiri será independiente de la configuración interna de servicios externos.

---

# 4.7 Módulo de Auditoría

## Responsabilidad

Registrar eventos importantes de la plataforma.

## Funciones principales

* Registro de acciones.
* Seguimiento de cambios.
* Historial administrativo.
* Diagnóstico.

Ejemplos:

* Usuario inició sesión.
* Configuración modificada.
* Acción ejecutada sobre un dispositivo.

---

# 4.8 Módulo de Notificaciones

## Responsabilidad

Centralizar eventos relevantes para los usuarios.

## Funciones futuras posibles

* Alertas del hogar.
* Avisos del sistema.
* Eventos multimedia.
* Mensajes de servicios integrados.

---

# 4.9 Relación entre Módulos

Los módulos deberán comunicarse mediante interfaces internas definidas.

```mermaid id="4q3n2h"
flowchart TB

    API["Chiri API"]

    Users["Usuarios"]
    Security["Seguridad"]
    Home["Domótica"]
    Media["Multimedia"]
    AI["IA"]
    Config["Configuración"]
    Audit["Auditoría"]
    Notify["Notificaciones"]

    API --> Users
    API --> Security
    API --> Home
    API --> Media
    API --> AI
    API --> Config

    Users --> Audit
    Security --> Audit
    Home --> Notify
    Media --> Notify
    AI --> Notify
```

---

# 4.10 Regla de Creación de Módulos

Un nuevo módulo deberá crearse únicamente cuando:

* tenga una responsabilidad clara.
* represente una capacidad real de Chiri.
* tenga límites definidos.
* aporte valor a la plataforma.

No se crearán módulos solamente para agrupar código sin una responsabilidad funcional.

---

# 4.11 Principio Arquitectónico

Los módulos de Chiri deben representar capacidades del sistema, no tecnologías específicas.

Ejemplo:

Correcto:

```
Módulo Multimedia
        |
        +-- Music Assistant
        +-- Jellyfin
```

Incorrecto:

```
Módulo Music Assistant
Módulo Jellyfin
```

La primera opción representa una capacidad del usuario.

La segunda representa detalles internos de implementación.

# 5. Flujo Interno de una Solicitud

El Backend de Chiri Platform seguirá un flujo controlado para procesar cualquier solicitud recibida desde un cliente.

El objetivo es mantener una separación clara entre:

* entrada externa,
* lógica de aplicación,
* reglas de negocio,
* integraciones,
* persistencia,
* respuesta al cliente.

---

# 5.1 Flujo General

El flujo interno será:

```mermaid id="6q8d3v"
flowchart TB

    Client["Cliente Chiri"]

    API["API Layer"]

    Auth["Autenticación<br/>Autorización"]

    UseCase["Application Layer<br/>Caso de Uso"]

    Domain["Domain Layer<br/>Reglas de Negocio"]

    Adapter["Integration Layer<br/>Adaptador"]

    External["Servicio Externo"]

    Repository["Data Layer<br/>Repositorio"]

    DB["PostgreSQL"]


    Client --> API
    API --> Auth
    Auth --> UseCase
    UseCase --> Domain
    UseCase --> Adapter
    UseCase --> Repository

    Adapter --> External

    Repository --> DB
```

---

# 5.2 Recepción de Solicitud

La solicitud inicia desde un cliente autorizado.

Ejemplo:

Usuario solicita:

> "Apagar la luz del salón"

El cliente envía una petición a la API de Chiri.

La API recibe:

* endpoint solicitado.
* parámetros.
* información de autenticación.

---

# 5.3 Capa API

La API es responsable de:

* recibir la petición.
* validar formato.
* validar datos básicos.
* identificar usuario.
* enviar la operación al caso de uso correspondiente.

La API no decide:

* qué debe ocurrir.
* cómo ejecutar una acción.
* cómo comunicarse con servicios externos.

---

# 5.4 Capa de Seguridad

Antes de ejecutar una operación, Chiri deberá validar:

* identidad del usuario.
* permisos necesarios.
* disponibilidad del recurso.

Ejemplo:

Un usuario puede consultar temperatura, pero no necesariamente modificar configuraciones del hogar.

---

# 5.5 Capa Application

El caso de uso coordina la operación.

Ejemplo:

```text
Apagar luz salón

        |
        v

Caso de Uso Domótica

        |
        v

Validar operación

        |
        v

Solicitar acción a Home Assistant
```

Esta capa decide el flujo, pero no conoce detalles técnicos del servicio externo.

---

# 5.6 Capa Domain

El dominio contiene las reglas propias de Chiri.

Ejemplo:

Reglas:

* El usuario debe tener permiso.
* El dispositivo debe estar autorizado.
* La acción debe ser válida.

Estas reglas pertenecen a Chiri, no a Home Assistant.

---

# 5.7 Capa de Integración

El adaptador traduce la operación de Chiri al formato requerido por el servicio externo.

Ejemplo:

Chiri:

```json
{
 "action": "turn_off",
 "device": "living_room_light"
}
```

Adaptador:

```text
Convertir solicitud
        |
        v
Home Assistant API
```

---

# 5.8 Capa de Datos

Cuando una operación requiere información propia de Chiri, se utilizará la capa de datos.

Ejemplos:

* permisos.
* preferencias.
* configuraciones.
* auditoría.

La lógica de negocio no realizará consultas directas a PostgreSQL.

---

# 5.9 Procesamiento de Respuesta

La respuesta seguirá el camino inverso:

```mermaid id="v8c3s1"
flowchart LR

    External["Servicio Externo"]

    Adapter["Adaptador"]

    UseCase["Caso de Uso"]

    API["API"]

    Client["Cliente"]


    External --> Adapter
    Adapter --> UseCase
    UseCase --> API
    API --> Client
```

---

# 5.10 Manejo de Errores

Cada capa deberá manejar únicamente los errores correspondientes a su responsabilidad.

Ejemplo:

Error externo:

```text
Home Assistant no disponible
```

Será transformado en:

```json
{
 "error": "HOME_SERVICE_UNAVAILABLE",
 "message": "El servicio de hogar inteligente no está disponible"
}
```

El cliente no deberá conocer detalles internos.

---

# 5.11 Auditoría

Las operaciones relevantes podrán generar registros de auditoría.

Ejemplo:

```text
Usuario:
José

Acción:
Apagar luz salón

Resultado:
Correcto

Fecha:
2026-08-06
```

---

# 5.12 Principio Arquitectónico

Toda solicitud dentro de Chiri deberá respetar:

> La API recibe, la aplicación coordina, el dominio decide, las integraciones ejecutan y los datos persisten.

# 6. Integraciones del Backend

Las integraciones del Backend permiten que Chiri Platform utilice servicios externos manteniendo independencia respecto a sus implementaciones internas.

Cada integración deberá encapsular la comunicación con un servicio específico mediante un adaptador independiente.

---

# 6.1 Principio de Integración

El Backend no deberá comunicarse directamente con servicios externos desde los casos de uso.

Flujo correcto:

```mermaid id="2w6j9r"
flowchart LR

    UseCase["Caso de Uso Chiri"]

    Interface["Interfaz de Integración"]

    Adapter["Adaptador"]

    Service["Servicio Externo"]


    UseCase --> Interface
    Interface --> Adapter
    Adapter --> Service
```

---

# 6.2 Adaptadores de Integración

Cada servicio externo tendrá un adaptador responsable de:

* autenticación contra el servicio.
* comunicación mediante API.
* transformación de datos.
* manejo de errores.
* control de disponibilidad.

Ejemplo:

```text
integrations/

├── home_assistant/
│
├── music_assistant/
│
├── navidrome/
│
├── jellyfin/
│
└── ai/
```

---

# 6.3 Integración Home Assistant

## Objetivo

Permitir que Chiri interactúe con la plataforma de automatización del hogar.

## Responsabilidad del adaptador

El adaptador Home Assistant será responsable de:

* consultar estados.
* enviar comandos.
* recibir información disponible.
* transformar entidades externas.

---

## Ejemplo conceptual

Home Assistant:

```json
{
 "entity_id": "light.salon",
 "state": "on"
}
```

Chiri:

```json
{
 "device": "salon",
 "type": "light",
 "status": "active"
}
```

Chiri no deberá depender del formato interno de Home Assistant.

---

# 6.4 Integración Music Assistant

## Objetivo

Integrar capacidades musicales dentro de Chiri.

## Responsabilidad del adaptador

Permitirá:

* consultar biblioteca.
* controlar reproducción.
* obtener estado del reproductor.
* gestionar información musical.

---

El adaptador ocultará:

* endpoints internos.
* estructura de datos.
* detalles del servidor musical.

---

# 6.5 Integración Navidrome

## Objetivo

Integrar la biblioteca musical personal.

## Responsabilidad del adaptador

Permitirá:

* consultar artistas.
* consultar álbumes.
* consultar canciones.
* obtener información multimedia.

---

Chiri no administrará:

* archivos musicales.
* organización física de biblioteca.
* indexación.

Estas responsabilidades pertenecen a Navidrome.

---

# 6.6 Integración Jellyfin

## Objetivo

Integrar contenido multimedia.

## Responsabilidad del adaptador

Permitirá:

* consultar películas.
* consultar series.
* obtener información multimedia.
* integrar estados cuando sea necesario.

---

Chiri no realizará:

* transcodificación.
* gestión de almacenamiento multimedia.
* administración del servidor Jellyfin.

---

# 6.7 Integración de Inteligencia Artificial

## Objetivo

Permitir capacidades inteligentes dentro de Chiri.

La arquitectura deberá permitir múltiples proveedores.

Ejemplo:

```mermaid id="4y3v0a"
flowchart LR

    ChiriAI["Chiri AI Adapter"]

    Provider1["Proveedor IA Cloud"]

    Provider2["Modelo Local"]

    Provider3["Proveedor Futuro"]


    ChiriAI --> Provider1
    ChiriAI --> Provider2
    ChiriAI --> Provider3
```

---

# 6.8 Manejo de Fallos de Integración

Una integración externa puede fallar.

Ejemplos:

* servicio apagado.
* red no disponible.
* credenciales inválidas.
* API modificada.

El adaptador deberá:

* detectar el error.
* registrar información técnica.
* devolver una respuesta controlada.

---

# 6.9 Disponibilidad de Servicios

Chiri deberá conocer el estado de sus integraciones.

Ejemplo:

```json
{
 "service": "music",
 "status": "available"
}
```

o:

```json
{
 "service": "home",
 "status": "unavailable"
}
```

Esto permitirá:

* diagnóstico.
* interfaz más inteligente.
* mantenimiento.

---

# 6.10 Regla de Aislamiento

Un fallo en una integración no deberá comprometer toda la plataforma.

Ejemplo:

Si Jellyfin está detenido:

* Domótica continúa funcionando.
* Usuarios continúan accediendo.
* Otros módulos permanecen disponibles.

---

# 6.11 Principio Arquitectónico

Toda integración externa deberá cumplir:

> Chiri debe conocer las capacidades del servicio, pero no depender de sus detalles internos.

# 7. Base de Datos del Backend

La base de datos del Backend de Chiri Platform será utilizada exclusivamente para almacenar información propia de la plataforma.

El Backend utilizará PostgreSQL como sistema principal de persistencia.

La base de datos deberá mantener independencia respecto a los servicios externos integrados.

---

# 7.1 Responsabilidad de PostgreSQL

PostgreSQL será responsable de almacenar información necesaria para el funcionamiento interno de Chiri.

Ejemplos:

* usuarios.
* permisos.
* configuraciones.
* preferencias.
* auditoría.
* relaciones internas.
* información propia de la plataforma.

---

# 7.2 Datos que NO pertenecen a Chiri

Chiri no almacenará información que ya sea responsabilidad de servicios especializados.

Ejemplos:

## Música

No almacenará:

* archivos musicales.
* biblioteca completa.
* metadatos musicales originales.

Responsabilidad:

* Navidrome.
* Music Assistant.

---

## Domótica

No almacenará:

* dispositivos físicos.
* estados en tiempo real.
* automatizaciones.

Responsabilidad:

* Home Assistant.

---

## Multimedia

No almacenará:

* películas.
* series.
* archivos multimedia.

Responsabilidad:

* Jellyfin.

---

# 7.3 Principio de Fuente Única de Verdad

Cada dato deberá tener un único sistema responsable.

Ejemplo:

```mermaid id="m8c7q4"
flowchart LR

    Users["Usuarios"]
    ChiriDB["PostgreSQL Chiri"]

    Music["Biblioteca Musical"]
    Nav["Navidrome"]

    Devices["Dispositivos Hogar"]
    HA["Home Assistant"]


    Users --> ChiriDB

    Music --> Nav

    Devices --> HA
```

Chiri podrá consultar información externa, pero no duplicará responsabilidades.

---

# 7.4 Capa de Persistencia

El acceso a PostgreSQL estará encapsulado mediante la capa de datos.

Flujo:

```mermaid id="8x0qkg"
flowchart LR

    UseCase["Caso de Uso"]

    Repository["Repositorio"]

    ORM["Modelo Persistencia"]

    DB["PostgreSQL"]


    UseCase --> Repository
    Repository --> ORM
    ORM --> DB
```

---

# 7.5 Repositorios

Los repositorios serán responsables de:

* consultar datos.
* guardar información.
* actualizar registros.
* eliminar información cuando corresponda.

No deberán contener:

* reglas de negocio.
* lógica de aplicación.

---

# 7.6 Modelos de Datos

Los modelos de base de datos deberán representar conceptos propios de Chiri.

Ejemplos futuros:

* User.
* Role.
* Permission.
* Setting.
* Integration.
* AuditEvent.

No deberán representar directamente entidades externas.

Incorrecto:

```text
MusicAssistantSong
HomeAssistantLight
JellyfinMovie
```

Correcto:

```text
UserPreference
IntegrationStatus
AuditEvent
```

---

# 7.7 Migraciones

Todos los cambios estructurales de base de datos deberán gestionarse mediante migraciones.

Las migraciones permitirán:

* evolución controlada.
* reproducibilidad.
* recuperación.
* despliegues seguros.

No se realizarán cambios manuales directamente en producción.

---

# 7.8 Integridad de Datos

El Backend deberá garantizar:

* validación de información.
* relaciones consistentes.
* restricciones adecuadas.
* eliminación controlada.

---

# 7.9 Configuración de Base de Datos

La conexión a PostgreSQL deberá gestionarse mediante configuración externa.

Nunca deberá incluirse:

* usuario.
* contraseña.
* cadena de conexión.

dentro del código fuente.

---

# 7.10 Escalabilidad Futura

La arquitectura permitirá evolucionar hacia:

* separación de módulos.
* optimización de consultas.
* replicación.
* nuevas estrategias de almacenamiento.

Sin modificar la lógica principal de Chiri.

---

# 7.11 Regla Arquitectónica

Toda información almacenada en PostgreSQL deberá responder:

> ¿Este dato pertenece realmente a Chiri o pertenece a un servicio especializado?

Si pertenece a otro sistema, Chiri deberá integrarlo, no duplicarlo.

# 8. Configuración y Variables de Entorno

La configuración del Backend de Chiri Platform deberá estar separada del código fuente.

El objetivo es permitir ejecutar la misma aplicación en diferentes ambientes sin modificar archivos internos.

---

# 8.1 Principio de Configuración Externa

Los valores variables del sistema no deberán estar definidos directamente en el código.

Ejemplos:

* conexiones de base de datos.
* claves de servicios externos.
* puertos.
* URLs.
* parámetros del sistema.

Estos valores deberán administrarse mediante configuración externa.

---

# 8.2 Ambientes del Sistema

Chiri Platform tendrá inicialmente dos ambientes principales:

## Desarrollo

Ejecutado en:

* PC Windows 11.
* Docker Desktop.

Objetivo:

* programación.
* pruebas.
* validación.

---

## Producción

Ejecutado en:

* Raspberry Pi 4B.
* Docker Compose.

Objetivo:

* servicio real del hogar.
* integración con servicios existentes.

---

# 8.3 Estructura de Configuración

La configuración seguirá una estructura similar:

```text id="k8m2da"
server/

├── .env.example

├── config/

│   └── settings.py

└── docker-compose.yml
```

---

# 8.4 Variables de Entorno

Las variables de entorno podrán contener:

## Aplicación

Ejemplo:

```text id="3kqf1h"
APP_NAME=Chiri
APP_ENV=production
APP_PORT=8000
```

---

## Base de Datos

Ejemplo:

```text id="m2r5fz"
DATABASE_HOST=postgres
DATABASE_PORT=5432
DATABASE_NAME=chiri
```

---

## Servicios Externos

Ejemplo conceptual:

```text id="w7j4az"
HOME_ASSISTANT_URL=
MUSIC_ASSISTANT_URL=
JELLYFIN_URL=
```

---

# 8.5 Protección de Secretos

Nunca deberán almacenarse secretos dentro del código.

Ejemplos:

* contraseñas.
* tokens.
* claves API.
* credenciales.

No permitido:

```python
API_KEY="valor_secreto"
```

Permitido:

```python
API_KEY=os.getenv("API_KEY")
```

---

# 8.6 Clase de Configuración

La configuración será centralizada mediante un componente único.

Ejemplo conceptual:

```mermaid id="h3g8q1"
flowchart TB

    ENV["Variables de Entorno"]

    Settings["Settings Chiri"]

    App["Aplicación Backend"]


    ENV --> Settings
    Settings --> App
```

---

# 8.7 Configuración por Ambiente

Cada ambiente podrá tener valores diferentes.

Ejemplo:

Desarrollo:

```text
APP_ENV=development
DATABASE_HOST=localhost
```

Producción:

```text
APP_ENV=production
DATABASE_HOST=postgres
```

El código será el mismo.

---

# 8.8 Configuración de Docker

Docker será responsable de inyectar configuración al contenedor.

Ejemplo conceptual:

```yaml
services:

  chiri-backend:
    environment:
      APP_ENV: production
      DATABASE_HOST: postgres
```

---

# 8.9 Validación de Configuración

Al iniciar el Backend deberá validar:

* variables obligatorias existentes.
* formatos correctos.
* disponibilidad de servicios críticos.

Si la configuración es incorrecta, el servicio deberá informar claramente el problema.

---

# 8.10 Registro de Configuración

La aplicación podrá mostrar información general del ambiente:

Permitido:

```text
Environment: production
Version: 1.0
```

No permitido:

```text
Database Password: xxxx
API Token: xxxx
```

---

# 8.11 Principio Arquitectónico

La configuración pertenece al entorno de ejecución, no al código fuente.

El mismo Backend debe poder funcionar en diferentes ambientes únicamente cambiando su configuración.


# 9. Logging, Monitoreo y Diagnóstico

Chiri Platform deberá incorporar mecanismos de registro y diagnóstico que permitan conocer el estado interno de la plataforma y facilitar la resolución de problemas.

El sistema deberá proporcionar información suficiente para:

* detectar errores.
* analizar comportamientos.
* verificar integraciones.
* mantener estabilidad operacional.

---

# 9.1 Principio de Observabilidad

La observabilidad de Chiri estará basada en tres elementos:

```mermaid id="2t4m9x"
flowchart TB

    Logs["Logs<br/>Eventos del sistema"]

    Metrics["Métricas<br/>Estado y rendimiento"]

    Health["Health Checks<br/>Disponibilidad"]


    Observability["Observabilidad Chiri"]

    Logs --> Observability
    Metrics --> Observability
    Health --> Observability
```

---

# 9.2 Sistema de Logs

El Backend deberá generar registros estructurados.

Los logs deberán permitir identificar:

* fecha y hora.
* componente.
* nivel del evento.
* operación realizada.
* resultado.

Ejemplo conceptual:

```text id="5m8r9p"
2026-08-06 10:30:15
INFO
MusicIntegration
Playback request completed
```

---

# 9.3 Niveles de Log

Se utilizarán niveles estándar:

## DEBUG

Información detallada para desarrollo.

Ejemplo:

* solicitudes internas.
* datos técnicos.

---

## INFO

Eventos normales del sistema.

Ejemplo:

* servicio iniciado.
* usuario autenticado.
* integración disponible.

---

## WARNING

Situaciones que requieren atención.

Ejemplo:

* servicio lento.
* reintento de conexión.

---

## ERROR

Fallos que afectan una operación.

Ejemplo:

* integración no disponible.
* error de comunicación.

---

## CRITICAL

Problemas graves del sistema.

Ejemplo:

* Backend no puede iniciar.
* pérdida de conexión crítica.

---

# 9.4 Separación de Información

Los logs deberán diferenciar:

## Información técnica

Para administración y desarrollo.

Ejemplo:

```text id="w3nq2k"
Connection timeout Home Assistant API
```

---

## Información de usuario

Para mostrar en clientes.

Ejemplo:

```json id="a5c8x7"
{
 "error": "HOME_SERVICE_UNAVAILABLE"
}
```

Los detalles internos nunca deberán exponerse al usuario.

---

# 9.5 Health Checks

El Backend deberá proporcionar mecanismos para conocer su estado.

Ejemplo:

```text id="6j9k0p"
GET /health
```

Respuesta:

```json id="4g8s2n"
{
 "status": "healthy"
}
```

---

# 9.6 Estado de Integraciones

Chiri deberá poder consultar el estado de servicios externos.

Ejemplo:

```mermaid id="8r2c5v"
flowchart LR

    Backend["Chiri Backend"]

    HA["Home Assistant"]
    MA["Music Assistant"]
    JF["Jellyfin"]

    Backend --> HA
    Backend --> MA
    Backend --> JF
```

Resultado:

```json id="j2n7pv"
{
 "services": {
   "home_assistant": "online",
   "music": "online",
   "jellyfin": "offline"
 }
}
```

---

# 9.7 Diagnóstico de Errores

Cuando ocurre un problema, Chiri deberá permitir identificar:

* dónde ocurrió.
* qué componente falló.
* qué operación estaba ejecutándose.
* qué servicio estaba involucrado.

Ejemplo:

```text id="8o5n3j"
Solicitud:
Play Music

Componente:
Music Adapter

Error:
Servicio no disponible
```

---

# 9.8 Auditoría vs Logs

Ambos conceptos son diferentes.

## Logs

Orientados a:

* técnicos.
* diagnóstico.
* funcionamiento interno.

---

## Auditoría

Orientada a:

* acciones importantes.
* usuarios.
* seguridad.

Ejemplo:

Auditoría:

```text id="5g0s7h"
Usuario José ejecutó:
Apagar luz salón
```

---

# 9.9 Monitoreo Futuro

La arquitectura permitirá incorporar herramientas externas de monitoreo.

Ejemplos futuros:

* métricas del sistema.
* paneles de estado.
* alertas.
* análisis histórico.

Sin modificar la arquitectura principal.

---

# 9.10 Principio Arquitectónico

Toda funcionalidad crítica de Chiri deberá poder responder:

> ¿Qué ocurrió, dónde ocurrió y por qué ocurrió?

Si un componente no permite diagnóstico suficiente, deberá mejorarse antes de considerarse estable.

# 10. Pruebas y Calidad del Código

La calidad del Backend de Chiri Platform será garantizada mediante prácticas de desarrollo que permitan detectar errores antes de llegar al entorno de producción.

Las pruebas forman parte del proceso normal de desarrollo y no serán una actividad posterior.

El flujo definido será:

```mermaid id="7c4m9p"
flowchart LR

    Code["Código"]

    Test["Pruebas"]

    Review["Revisión"]

    Deploy["Despliegue"]


    Code --> Test
    Test --> Review
    Review --> Deploy
```

---

# 10.1 Principios de Calidad

El desarrollo del Backend deberá cumplir:

* Código legible.
* Código mantenible.
* Código documentado cuando sea necesario.
* Bajo acoplamiento.
* Responsabilidades claras.
* Pruebas antes de cambios importantes.

---

# 10.2 Tipos de Pruebas

Chiri utilizará diferentes niveles de pruebas.

---

# 10.3 Pruebas Unitarias

## Objetivo

Validar componentes individuales de forma aislada.

Ejemplos:

* reglas de dominio.
* validaciones.
* transformaciones.
* funciones internas.

No deberán depender de:

* servicios externos.
* base de datos real.

---

# 10.4 Pruebas de Integración

## Objetivo

Validar la comunicación entre componentes.

Ejemplos:

* Backend + PostgreSQL.
* Backend + adaptador.
* API + caso de uso.

Permitirán comprobar que las capas trabajan correctamente juntas.

---

# 10.5 Pruebas de Integraciones Externas

## Objetivo

Validar adaptadores contra servicios reales.

Ejemplos:

* Home Assistant.
* Music Assistant.
* Jellyfin.

Estas pruebas deberán considerar:

* disponibilidad del servicio.
* respuestas esperadas.
* manejo de errores.

---

# 10.6 Pruebas de API

La API deberá validar:

* endpoints existentes.
* formatos de respuesta.
* autenticación.
* permisos.
* errores esperados.

Ejemplo:

Solicitud:

```http id="m4p8cz"
GET /api/user/profile
```

Respuesta esperada:

```json id="7k2n1m"
{
 "name": "Usuario",
 "status": "active"
}
```

---

# 10.7 Pruebas Antes de Producción

Antes de desplegar una versión en Raspberry Pi deberán validarse:

* pruebas automáticas.
* inicio correcto del contenedor.
* conexión con PostgreSQL.
* estado de integraciones necesarias.
* funcionamiento básico de API.

---

# 10.8 Calidad del Código

El código deberá seguir reglas:

## Nombres claros

Incorrecto:

```python id="u7w3ak"
def proc():
```

Correcto:

```python id="k8q1mp"
def validate_user_permissions():
```

---

## Funciones pequeñas

Una función deberá realizar una responsabilidad clara.

---

## Evitar duplicación

La lógica común deberá reutilizarse.

---

## Separación de responsabilidades

Cada componente debe estar ubicado donde corresponde.

---

# 10.9 Revisión de Cambios

Antes de incorporar cambios importantes deberá revisarse:

* impacto arquitectónico.
* compatibilidad.
* seguridad.
* pruebas realizadas.

---

# 10.10 Automatización Futura

La arquitectura permitirá incorporar herramientas como:

* ejecución automática de pruebas.
* validación de código.
* integración continua.

Estas herramientas se definirán posteriormente en:

`090_GuiaProgramacion.md`

---

# 10.11 Regla Arquitectónica

Un cambio no estará listo para producción solamente porque funciona.

Debe cumplir:

> Funciona correctamente, mantiene la arquitectura y puede ser mantenido en el futuro.

# 11. Despliegue del Backend

El despliegue del Backend de Chiri Platform estará basado en contenedores Docker, permitiendo mantener consistencia entre ambientes de desarrollo y producción.

El objetivo es que el mismo componente pueda ejecutarse en diferentes infraestructuras mediante configuración externa.

---

# 11.1 Principio de Despliegue

El Backend deberá cumplir:

* instalación reproducible.
* configuración separada.
* actualización controlada.
* aislamiento mediante contenedores.
* facilidad de recuperación.

---

# 11.2 Modelo de Ejecución

La arquitectura de despliegue será:

```mermaid id="9c4k2m"
flowchart TB

    Developer["PC Desarrollo<br/>Windows 11"]

    DockerDev["Docker Desktop"]

    Image["Imagen Docker<br/>Chiri Backend"]

    Registry["Repositorio de Imágenes"]

    Raspberry["Raspberry Pi 4B"]

    DockerProd["Docker Compose"]

    Container["Chiri Backend Container"]


    Developer --> DockerDev
    DockerDev --> Image
    Image --> Registry
    Registry --> Raspberry
    Raspberry --> DockerProd
    DockerProd --> Container
```

---

# 11.3 Contenedor Backend

El Backend será ejecutado dentro de un contenedor independiente.

Responsabilidades del contenedor:

* ejecutar aplicación FastAPI.
* cargar configuración.
* comunicarse con servicios internos.
* exponer API de Chiri.

No deberá contener:

* datos persistentes.
* secretos permanentes.
* configuraciones específicas del servidor.

---

# 11.4 Dockerfile

El proyecto deberá incluir un Dockerfile responsable de definir:

* imagen base.
* dependencias.
* instalación del proyecto.
* comando de inicio.

Ejemplo conceptual:

```text id="j2m6x8"
Dockerfile

Imagen Python
        |
Dependencias
        |
Código Chiri
        |
Servidor FastAPI
```

---

# 11.5 Docker Compose

En producción, Chiri Backend será administrado mediante Docker Compose.

Responsabilidades:

* crear contenedor.
* conectar redes.
* cargar variables.
* montar volúmenes necesarios.

Ejemplo conceptual:

```yaml id="k8n4q7"
services:

  chiri-backend:
    image: chiri/backend
    environment:
      APP_ENV: production
    depends_on:
      - postgres
```

---

# 11.6 Comunicación con Servicios Internos

Los contenedores deberán comunicarse mediante redes Docker.

Ejemplo:

```mermaid id="6y3p9w"
flowchart LR

    Backend["Chiri Backend"]

    PostgreSQL["PostgreSQL"]

    HA["Home Assistant"]

    MA["Music Assistant"]

    JF["Jellyfin"]


    Backend --> PostgreSQL
    Backend --> HA
    Backend --> MA
    Backend --> JF
```

---

# 11.7 Persistencia

Los datos persistentes deberán estar fuera del ciclo de vida del contenedor.

Ejemplos:

Correcto:

```text id="4v7n1k"
Container
    |
    +--> Volume
            |
            +--> Data persistente
```

Incorrecto:

```text id="8p5m2d"
Container
    |
    +--> Datos importantes internos
```

---

# 11.8 Actualización del Backend

Las actualizaciones deberán seguir un proceso controlado:

```mermaid id="q8x5r2"
flowchart LR

    Code["Nuevo Código"]

    Build["Construcción Imagen"]

    Test["Pruebas"]

    Deploy["Actualización Producción"]

    Verify["Verificación"]


    Code --> Build
    Build --> Test
    Test --> Deploy
    Deploy --> Verify
```

---

# 11.9 Recuperación

El sistema deberá permitir:

* recrear contenedores.
* restaurar configuración.
* recuperar datos persistentes.
* volver a una versión anterior si fuera necesario.

---

# 11.10 Diferencia Desarrollo / Producción

La diferencia entre ambientes deberá estar limitada a:

* configuración.
* datos.
* servicios disponibles.

No deberá existir:

* código diferente.
* lógica diferente.
* comportamiento diferente.

---

# 11.11 Principio Arquitectónico

El despliegue de Chiri deberá responder:

> Si la Raspberry Pi falla, ¿podemos reconstruir el sistema siguiendo un proceso conocido?

Si la respuesta es no, el despliegue necesita mejorar.

# 12. Evolución y Mantenimiento del Backend

El Backend de Chiri Platform deberá estar preparado para evolucionar de forma controlada durante toda la vida del proyecto.

Los cambios deberán realizarse respetando la arquitectura definida y evitando degradar la calidad del sistema.

---

# 12.1 Principio de Evolución

La evolución del Backend deberá seguir:

```mermaid id="4m8q2x"
flowchart LR

    Necesidad["Nueva Necesidad"]

    Analisis["Análisis"]

    Diseño["Diseño"]

    Implementacion["Implementación"]

    Pruebas["Pruebas"]

    Despliegue["Despliegue"]


    Necesidad --> Analisis
    Analisis --> Diseño
    Diseño --> Implementacion
    Implementacion --> Pruebas
    Pruebas --> Despliegue
```

No se realizarán cambios directamente sobre producción sin pasar por el proceso definido.

---

# 12.2 Incorporación de Nuevas Funcionalidades

Una nueva funcionalidad deberá determinar primero:

* si pertenece a un módulo existente.
* si requiere un nuevo módulo.
* si requiere una nueva integración.
* si modifica la arquitectura.

La pregunta principal será:

> ¿Esta capacidad pertenece realmente al núcleo de Chiri?

---

# 12.3 Creación de Nuevos Módulos

Un nuevo módulo solamente deberá crearse cuando:

* tenga una responsabilidad propia.
* tenga reglas claras.
* pueda evolucionar independientemente.
* aporte una capacidad real.

No se crearán módulos para:

* agrupar archivos.
* separar código temporal.
* seguir tendencias técnicas.

---

# 12.4 Compatibilidad

Las modificaciones deberán considerar:

* compatibilidad con clientes existentes.
* compatibilidad con datos existentes.
* compatibilidad con integraciones actuales.

Los cambios que puedan romper contratos existentes deberán planificarse.

---

# 12.5 Versionado de API

La API de Chiri deberá considerar evolución mediante versiones.

Ejemplo conceptual:

```text id="h4p8z2"
API v1

/api/v1/users
/api/v1/media
/api/v1/home
```

Esto permitirá evolucionar sin afectar clientes actuales.

---

# 12.6 Refactorización

La refactorización será una actividad normal del mantenimiento.

Objetivos:

* mejorar claridad.
* eliminar duplicación.
* mejorar rendimiento.
* simplificar código.

La refactorización no deberá cambiar comportamiento esperado sin pruebas.

---

# 12.7 Dependencias Externas

Las dependencias utilizadas por el Backend deberán revisarse periódicamente.

Se deberá evaluar:

* seguridad.
* compatibilidad.
* mantenimiento.
* necesidad real.

No se agregarán librerías solamente por conveniencia temporal.

---

# 12.8 Documentación Continua

La documentación deberá mantenerse junto con la evolución del código.

Cuando exista un cambio importante deberá actualizarse:

* documentación técnica.
* diagramas.
* ADR correspondiente.

---

# 12.9 Regla de Cambios Arquitectónicos

Si un cambio afecta:

* estructura principal.
* tecnologías base.
* comunicación entre componentes.
* modelo de seguridad.

deberá registrarse mediante ADR.

---

# 12.10 Principio de Mantenimiento

El Backend de Chiri deberá priorizar:

* estabilidad antes que velocidad.
* claridad antes que complejidad.
* soluciones simples antes que soluciones sofisticadas.

---

# 12.11 Estado del Backend

La arquitectura del Backend queda definida como una base preparada para:

* integración de servicios.
* crecimiento modular.
* incorporación de nuevos clientes.
* evolución tecnológica controlada.

---

# 12.12 Regla Final del Backend

Toda modificación futura deberá cumplir:

> Mejorar la plataforma sin comprometer la arquitectura, seguridad y mantenibilidad definida.


# 13. Conclusión del Backend

El Backend de Chiri Platform v1.0 queda definido como el núcleo central de coordinación e integración de la plataforma.

Su diseño establece una arquitectura modular, segura y escalable que permite conectar diferentes servicios manteniendo separación de responsabilidades.

---

# 13.1 Arquitectura Definida

El Backend estará construido utilizando:

* Python.
* FastAPI.
* PostgreSQL.
* Docker.
* Docker Compose.

Su arquitectura interna estará basada en:

* API Layer.
* Application Layer.
* Domain Layer.
* Integration Layer.
* Data Layer.
* Infrastructure Layer.

---

# 13.2 Responsabilidad del Backend

El Backend será responsable de:

* Exponer la API de Chiri.
* Gestionar usuarios y permisos.
* Ejecutar lógica propia de la plataforma.
* Coordinar servicios externos.
* Mantener información propia.
* Garantizar seguridad y trazabilidad.

---

# 13.3 Límites Confirmados

El Backend no será responsable de reemplazar:

* Home Assistant.
* Music Assistant.
* Navidrome.
* Jellyfin.
* Servicios externos de IA.

Su función será integrarlos y proporcionar una experiencia unificada.

---

# 13.4 Principios Confirmados

El desarrollo del Backend seguirá:

* Arquitectura antes que código.
* Separación de responsabilidades.
* Código mantenible.
* Seguridad por defecto.
* Documentación como fuente de verdad.
* Integraciones mediante adaptadores.
* Evolución controlada.

---

# 13.5 Estado del Documento

El documento:

```text
030_Backend.md
```

queda definido como la referencia arquitectónica para el desarrollo del Backend de Chiri Platform v1.0.

Cualquier implementación futura deberá respetar las decisiones aquí establecidas.

Los cambios que afecten la arquitectura deberán seguir el proceso ADR definido.

---

# Declaración Final

El Backend de Chiri Platform v1.0 está preparado para pasar de la fase de diseño a la fase de implementación cuando el proyecto lo determine.

La arquitectura proporciona una base estable para construir una plataforma personal modular, integrada y escalable.
