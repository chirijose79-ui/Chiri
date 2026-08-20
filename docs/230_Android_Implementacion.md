# 230_Android_Implementacion.md

# Implementación Aplicación Android Chiri Platform v1.0

**Versión:** 1.0

**Estado:** Aprobado

---

# 1. Objetivo

Definir la implementación técnica de la aplicación Android de Chiri Platform v1.0 como cliente oficial de la plataforma.

Este documento transforma la arquitectura Android definida en `040_Android.md` y las decisiones arquitectónicas definidas en `100_DecisionesArquitectura.md` en una estructura concreta de implementación.

Define:

- estructura del proyecto;
- organización interna;
- capas de aplicación;
- comunicación con la API;
- gestión de datos;
- gestión de sesión;
- configuración;
- seguridad;
- pruebas;
- reglas de implementación.

Este documento no modifica la arquitectura definida en los documentos arquitectónicos.

---

# 2. Alcance

La aplicación Android será responsable de:

- interfaz de usuario;
- interacción con el usuario;
- navegación;
- gestión del estado de las pantallas;
- gestión de sesión del cliente;
- consumo de la API Chiri;
- almacenamiento local necesario;
- presentación de información;
- experiencia de usuario.

La aplicación Android no será responsable de:

- implementar las reglas principales de negocio;
- acceder directamente a PostgreSQL;
- acceder directamente a las bases de datos de servicios integrados;
- acceder directamente a Home Assistant;
- acceder directamente a Music Assistant;
- acceder directamente a Navidrome;
- acceder directamente a Jellyfin;
- ejecutar procesos internos del Backend.

El flujo de comunicación será:

```text
Android
   ↓
HTTPS
   ↓
API Chiri
   ↓
Backend
   ↓
Servicios / PostgreSQL
````

---

# 3. Tecnologías

La aplicación utilizará:

## Lenguaje

* Kotlin

## Interfaz

* Jetpack Compose
* Material Design

## Arquitectura

* MVVM
* separación por capas

## Construcción

* Gradle

## Comunicación

* API REST
* JSON
* HTTPS

## Pruebas

* pruebas unitarias;
* pruebas de integración;
* pruebas de interfaz cuando corresponda.

Las versiones concretas de las dependencias se gestionarán mediante la configuración Gradle del proyecto.

---

# 4. Identidad de la aplicación

La aplicación utilizará el dominio oficial de Chiri como base de su identidad.

Dominio:

```text
chirihome.com
```

Namespace:

```text
com.chirihome.platform
```

Application ID:

```text
com.chirihome.platform
```

El namespace y Application ID forman parte de la identidad técnica de la aplicación y no deberán modificarse sin una decisión técnica que justifique el cambio.

---

# 5. Ubicación del Proyecto

El código Android se encuentra en:

```text
source/android/
```

Estructura general:

```text
source/android/

├── app/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

El módulo principal de aplicación es:

```text
source/android/app/
```

---

# 6. Arquitectura Android

La aplicación seguirá una arquitectura por capas basada en MVVM.

Flujo principal:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Data Source
 ├── Remote API
 └── Local Storage
```

La comunicación con el sistema será:

```text
Android
   ↓
API Chiri
   ↓
Backend
```

La interfaz no accederá directamente a fuentes de datos.

---

# 7. Capa UI

La capa UI será implementada utilizando Jetpack Compose.

Responsabilidades:

* representar el estado de la aplicación;
* mostrar pantallas;
* capturar eventos del usuario;
* mostrar estados de carga;
* mostrar errores;
* mostrar información;
* ejecutar navegación.

La UI no deberá contener:

* llamadas HTTP;
* acceso directo a almacenamiento;
* acceso directo a repositorios;
* reglas principales de negocio;
* lógica compleja de procesamiento.

Estructura prevista:

```text
ui/

├── screens/
├── components/
├── theme/
└── icons/
```

---

# 8. ViewModel

Los ViewModels administrarán el estado de las pantallas y coordinarán las acciones iniciadas desde la UI.

Responsabilidades:

* recibir eventos de UI;
* ejecutar casos de uso;
* administrar estado;
* exponer estado observable;
* gestionar estados de carga;
* gestionar errores;
* coordinar acciones.

El ViewModel no deberá realizar directamente llamadas HTTP.

Flujo:

```text
Usuario
   ↓
UI
   ↓
ViewModel
   ↓
Use Case
```

---

# 9. Domain y Use Cases

La capa de dominio representará las operaciones que la aplicación puede realizar.

Los Use Cases expresarán acciones de la aplicación.

Ejemplos:

```text
Obtener estado del hogar
Consultar biblioteca multimedia
Reproducir contenido
Consultar información
Gestionar sesión
```

Los Use Cases no deberán depender de componentes visuales.

Estructura conceptual:

```text
domain/

├── model/
├── repository/
└── usecase/
```

Los contratos de Repository podrán definirse en la capa de dominio.

---

# 10. Repository

Los Repository abstraerán el origen de los datos.

Responsabilidades:

* proporcionar datos al dominio;
* coordinar fuentes remotas y locales;
* ocultar detalles de implementación;
* decidir cuándo utilizar API o almacenamiento local;
* transformar información cuando corresponda.

Flujo:

```text
ViewModel
   ↓
Use Case
   ↓
Repository
   ├── Remote Data Source
   └── Local Data Source
```

Los Repository no deberán contener lógica visual.

---

# 11. Data Layer

La capa de datos implementará los mecanismos concretos de acceso a información.

Estructura prevista:

```text
data/

├── remote/
├── local/
├── mapper/
└── repository/
```

Responsabilidades:

* comunicación con API;
* almacenamiento local;
* conversión de datos;
* implementación de Repository;
* manejo técnico de fuentes de datos.

---

# 12. Network

La comunicación con Chiri se realizará mediante HTTPS.

Estructura prevista:

```text
network/

├── ApiService.kt
├── ApiClient.kt
└── interceptor/
```

La capa Network será responsable de:

* configurar el cliente HTTP;
* realizar solicitudes a la API;
* procesar respuestas HTTP;
* gestionar headers técnicos;
* gestionar autenticación técnica;
* manejar errores de comunicación.

La capa Network no implementará reglas de negocio.

La aplicación no se comunicará directamente con PostgreSQL ni con los servicios internos.

---

# 13. API

La aplicación Android consumirá exclusivamente las capacidades expuestas por la API Chiri.

La API utilizará el espacio:

```text
/api/v1/
```

El contrato de API se definirá en la documentación correspondiente.

Referencia:

```text
docs/060_API.md
docs/140_EspecificacionAPI.md
```

El cliente Android deberá respetar los contratos definidos por la API.

---

# 14. Modelos

Los modelos deberán mantener separación entre los diferentes niveles de representación de datos.

Flujo:

```text
DTO API
   ↓
Modelo de dominio
   ↓
Estado de UI
```

Los DTO representan el contrato externo de la API.

Los modelos de dominio representan información utilizada por la lógica de la aplicación.

Los modelos de UI representan el estado necesario para presentar información al usuario.

No se deberá utilizar directamente un DTO de API como estado visual cuando exista una transformación necesaria.

---

# 15. Manejo de Estado

La aplicación utilizará estados explícitos.

Como mínimo se deberán contemplar:

```text
LOADING
SUCCESS
ERROR
EMPTY
```

Ejemplo conceptual:

```kotlin
data class ScreenState<T>(
    val loading: Boolean,
    val data: T?,
    val error: String?
)
```

La implementación concreta podrá evolucionar hacia modelos de estado más específicos cuando la complejidad de cada pantalla lo requiera.

La UI deberá reaccionar al estado expuesto por el ViewModel.

---

# 16. Navegación

La navegación será responsabilidad de una capa específica.

Estructura prevista:

```text
navigation/

├── Routes.kt
└── NavGraph.kt
```

La navegación deberá:

* definir destinos;
* controlar el flujo entre pantallas;
* manejar argumentos de navegación;
* mantener separada la navegación de la lógica de negocio.

No deberá contener acceso directo a API ni PostgreSQL.

---

# 17. Almacenamiento Local

El almacenamiento local se utilizará únicamente cuando sea necesario para la aplicación Android.

Puede utilizarse para:

* preferencias;
* configuración local;
* información temporal;
* caché;
* información necesaria para mantener la experiencia del usuario.

No deberá utilizarse para almacenar:

* contraseñas;
* secretos del servidor;
* credenciales en texto plano;
* datos críticos que deban permanecer exclusivamente en Backend/PostgreSQL.

La implementación concreta del almacenamiento se seleccionará según las necesidades reales de cada funcionalidad.

---

# 18. Gestión de Sesión

La aplicación deberá soportar:

* inicio de sesión;
* mantenimiento de sesión;
* almacenamiento seguro de credenciales de sesión;
* renovación cuando corresponda;
* cierre de sesión;
* detección de sesión inválida.

Flujo conceptual:

```text
Usuario
   ↓
Login
   ↓
API
   ↓
Sesión
   ↓
Android
```

Los tokens y credenciales deberán almacenarse utilizando mecanismos seguros del sistema Android.

La implementación concreta dependerá del mecanismo de autenticación definido por la API.

---

# 19. Seguridad

La aplicación deberá cumplir las reglas de seguridad definidas en:

```text
docs/070_Seguridad.md
```

Principios principales:

* comunicación HTTPS;
* protección de tokens;
* almacenamiento seguro de credenciales;
* no almacenar secretos de servidor;
* no incluir credenciales en el código fuente;
* validación de respuestas;
* manejo seguro de errores;
* protección de información sensible.

Los logs de desarrollo no deberán exponer:

* contraseñas;
* tokens;
* secretos;
* credenciales;
* información sensible innecesaria.

---

# 20. Configuración

La configuración deberá mantenerse separada del código.

Como mínimo se deberá contemplar:

* URL de API;
* ambiente de ejecución;
* parámetros de aplicación;
* configuración específica de desarrollo;
* configuración específica de pruebas;
* configuración de producción.

Ambientes:

```text
DESARROLLO
PRUEBAS
PRODUCCIÓN
```

La configuración sensible no deberá almacenarse directamente en el repositorio.

---

# 21. Inyección de Dependencias

La aplicación deberá mantener las dependencias desacopladas.

Estructura prevista:

```text
di/
```

La inyección de dependencias permitirá administrar componentes como:

* API Client;
* Repository;
* Use Cases;
* ViewModels;
* almacenamiento local.

La tecnología concreta de inyección de dependencias podrá definirse durante la implementación cuando sea necesario.

---

# 22. Pruebas

La aplicación deberá incluir pruebas proporcionales a la complejidad de cada componente.

## 22.1 Pruebas Unitarias

Se consideran principalmente:

* Use Cases;
* ViewModels;
* Repository;
* validaciones;
* mappers.

## 22.2 Pruebas de Integración

Se consideran:

* comunicación con API;
* Repository;
* gestión de sesión;
* almacenamiento local cuando corresponda.

## 22.3 Pruebas de UI

Se consideran:

* navegación;
* estados de pantalla;
* flujos principales;
* interacción del usuario.

---

# 23. Manejo de Errores

Los errores deberán tratarse de forma controlada.

Se deberán diferenciar, cuando corresponda:

```text
Error de red
Error HTTP
Error de autenticación
Error de autorización
Error de validación
Error de datos
Error interno
```

La aplicación no deberá mostrar información técnica sensible al usuario.

Los mensajes presentados al usuario deberán ser comprensibles.

---

# 24. Logging

Los logs deberán utilizarse para diagnóstico y mantenimiento.

No deberán contener:

* contraseñas;
* tokens;
* secretos;
* credenciales;
* información sensible innecesaria.

Los logs de desarrollo podrán ser más detallados que los de producción.

---

# 25. Organización de Código

La organización del código deberá reflejar la arquitectura.

Estructura prevista:

```text
com.chirihome.platform/

├── ui/
│   ├── screens/
│   ├── components/
│   ├── theme/
│   └── icons/
│
├── navigation/
│
├── viewmodel/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── data/
│   ├── remote/
│   ├── local/
│   ├── mapper/
│   └── repository/
│
├── network/
│
├── storage/
│
├── di/
│
└── utils/
```

Esta estructura representa una organización lógica.

No todas las carpetas deben contener código desde el inicio.

Las carpetas y clases se crearán conforme aparezcan necesidades reales de implementación.

---

# 26. Regla de Implementación Incremental

No se deberán crear componentes únicamente para completar una estructura teórica.

Cada componente deberá existir cuando exista una necesidad funcional o técnica real.

Ejemplo:

```text
No crear:

AiViewModel
MediaViewModel
HomeViewModel
```

hasta que las funcionalidades correspondientes estén siendo implementadas.

La arquitectura debe permitir crecimiento sin obligar a implementar módulos inexistentes.

---

# 27. Integración con Backend

El cliente Android no implementará directamente la lógica de negocio.

El flujo será:

```text
Android
   ↓
API Chiri
   ↓
Backend
   ↓
PostgreSQL / Integraciones
```

El Backend será responsable de:

* lógica de negocio;
* validaciones de negocio;
* acceso a PostgreSQL;
* integración con servicios externos;
* seguridad del lado servidor.

---

# 28. Compatibilidad con la Arquitectura General

La implementación Android deberá mantener consistencia con:

```text
docs/020_Arquitectura.md
docs/040_Android.md
docs/050_BaseDatos.md
docs/060_API.md
docs/070_Seguridad.md
docs/080_Despliegue.md
docs/090_GuiaProgramacion.md
docs/100_DecisionesArquitectura.md
```

Cuando una implementación requiera modificar una decisión arquitectónica existente, primero deberá registrarse la decisión correspondiente en:

```text
docs/100_DecisionesArquitectura.md
```

---

# 29. Evolución

La arquitectura Android deberá permitir incorporar progresivamente:

* nuevos módulos;
* nuevas pantallas;
* nuevas capacidades;
* nuevas integraciones expuestas por el Backend;
* nuevas versiones de API;
* nuevos mecanismos de almacenamiento;
* nuevas capacidades de usuario.

La evolución deberá mantener la separación entre presentación, dominio, datos y comunicación.

---

# 30. Estado de Implementación

Este documento define las reglas de implementación de Android.

Su existencia no implica que todas las capas descritas estén actualmente implementadas.

Estado inicial esperado:

```text
Proyecto Android       ✅
Gradle                 ✅
Kotlin                 ✅
Jetpack Compose        ✅
Material 3             ✅

UI Chiri               ⏳
Navigation             ⏳
ViewModel              ⏳
Domain                 ⏳
Use Cases              ⏳
Repository             ⏳
Network/API            ⏳
Storage                ⏳
DI                     ⏳
Autenticación          ⏳
Pruebas                ⏳
```

Las funcionalidades se implementarán progresivamente.

---

# 31. Estado del Documento

Documento:

```text
230_Android_Implementacion.md
```

Versión:

```text
Chiri Platform v1.0
```

Estado:

```text
APROBADO
```