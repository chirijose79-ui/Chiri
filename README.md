# Chiri Platform

## 1. Descripción

Chiri Platform v1.0 es una plataforma personal de integración de servicios.

## 2. Arquitectura

La plataforma está organizada en los siguientes componentes principales:

- Aplicación Android Chiri
- API Chiri
- Backend Chiri
- Base de Datos PostgreSQL
- Servicios integrados

Flujo general:

```text
Aplicación Android
        ↓
     API Chiri
        ↓
   Backend Chiri
      ↓     ↓
PostgreSQL  Servicios Integrados
```

## 3. Documentación

La documentación técnica de Chiri Platform está organizada en los siguientes bloques.

### Fundamentos y arquitectura

- `docs/000_Principios.md`
- `docs/010_Proyecto.md`
- `docs/020_Arquitectura.md`
- `docs/030_Backend.md`
- `docs/040_Android.md`
- `docs/050_BaseDatos.md`
- `docs/060_API.md`
- `docs/070_Seguridad.md`
- `docs/080_Despliegue.md`
- `docs/090_GuiaProgramacion.md`
- `docs/100_DecisionesArquitectura.md`

### Modelo funcional y de dominio

- `docs/110_ModeloDominio.md`
- `docs/120_CasosUso.md`
- `docs/130_ReglasNegocio.md`
- `docs/140_EspecificacionAPI.md`
- `docs/150_DiccionarioDatos.md`

### Implementación técnica

- `docs/200_Backend_Implementacion.md`
- `docs/210_BaseDatos_Implementacion.md`
- `docs/220_API_Implementacion.md`
- `docs/230_Android_Implementacion.md`
- `docs/240_Integracion_Sistema.md`

### Calidad y operación

- `docs/310_Calidad_Codigo.md`
- `docs/320_Monitoreo_Operacion.md`
- `docs/330_Mantenimiento.md`
- `docs/340_Evolucion_Plataforma.md`

## 4. Tecnologías documentadas

La documentación de Chiri Platform establece, entre otros componentes técnicos:

- Android
- API
- Backend
- PostgreSQL
- Docker
- Servicios integrados

## 5. Principios

La plataforma mantiene separación de responsabilidades entre sus componentes, buscando consistencia, seguridad, mantenibilidad y evolución futura.

La documentación establece que la interfaz de usuario no contiene la lógica principal de negocio, la API gestiona la comunicación y validación inicial, el Backend contiene las reglas de negocio y la Base de Datos se encarga de persistencia e integridad.

## 6. Estado

**Chiri Platform v1.0**
