# Documento de Especificación de Requisitos de Software (SRS)

## Visor de Imágenes V2 — SRS General

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Sistema completo (documento raíz de la serie de SRS) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento es el **SRS general** del Visor de Imágenes V2, un sistema de gestión de activos
digitales (DAM) de escritorio para bibliotecas masivas (>40.000 activos) de imágenes y modelos 3D
(STL comprimidos en ZIP/RAR/7Z) con sus renders asociados.

El objetivo del documento es **describir el sistema en su conjunto** — qué es, cómo se organiza,
cómo funciona su flujo de trabajo y cómo se distribuye el software — y **servir de raíz** de la
serie de SRS modulares que especifican en detalle cada modo de trabajo. No repite los requisitos
funcionales de cada modo; los delega en los documentos especializados (sección 1.4).

### 1.2 Alcance

Este documento cubre:

- Visión general del producto y del dominio DAM.
- Descripción de los **modos de trabajo** y su orquestación.
- Arquitectura de software (MVC plano, inyección de dependencias, managers y servicios).
- Arranque de la aplicación, contexto global y restauración de sesión.
- Configuración global y persistencia.
- Flujo de trabajo de extremo a extremo entre modos.
- Requisitos no funcionales transversales.
- Mapa de trazabilidad entre paquetes/clases y los SRS modulares.

Los **requisitos funcionales detallados de cada modo** se especifican en los SRS modulares:

- `SRS-ModoVisor.md` — VISUALIZADOR y CARRUSEL.
- `SRS-ModoRender.md` — RENDER (generación de previews 3D).
- `SRS-ModoDatos.md` — DATOS (etiquetas y catalogación).
- `SRS-ModoProyecto.md` — PROYECTO (selección, integridad y exportación).
- `SRS-ModoCliente.md` — CLIENTE (revisión, catálogos y sincronización).

Este documento **no** incluye la especificación detallada de dichos modos; solo su visión general
y la integración transversal que los une.

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **Activo** | Imagen de renderizado de un modelo 3D y su archivo comprimido asociado (STL/ZIP/RAR/7Z). |
| **Modo de trabajo** | Entorno operativo de la aplicación (`WorkMode`): VISUALIZADOR, PROYECTO, CLIENTE, DATOS, CARRUSEL, RENDER. |
| **DisplayMode** | Modo de visualización del área central: `SINGLE_IMAGE`, `GRID`, `POLAROID`. |
| **Contexto de lista** | Estado por modo (`ListContext`): lista maestra, selección, mapa clave→ruta e historial. |
| **Lista maestra** | Lista completa de imágenes cargada para el modo activo; sobre ella se aplican filtros. |
| **Manager** | Clase de `controlador.managers` que gestiona un sub-sistema o estado concreto. |
| **Builder** | Clase de `vista.builders` que construye paneles y barras de la UI. |
| **ComponentRegistry** | Registro central de componentes UI por clave (DI manual). |
| **Action** | Comando ejecutable (`javax.swing.Action`) con un identificador canónico (`CMD_*`). |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |
| **BD** | Base de datos (SQLite embebida + JDBI). |
| **SRS** | *Software Requirements Specification*; documento de especificación de requisitos. |

### 1.4 Referencias

- `AGENTS.md` — Guía general del proyecto (arquitectura, gotchas y reglas).
- `docs/SRS-ModoVisor.md` — Especificación del Modo Visor (incluye carrusel).
- `docs/SRS-ModoRender.md` — Especificación del Modo Render.
- `docs/SRS-ModoDatos.md` — Especificación del Modo Datos.
- `docs/SRS-ModoProyecto.md` — Especificación del Modo Proyecto.
- `docs/SRS-ModoCliente.md` — Especificación del Modo Cliente.
- `resources/help/*.html` — Ayudas en línea por modo.

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 describe el producto y su dominio; la sección 3
presenta los modos de trabajo; la sección 4 detalla la arquitectura de software; la sección 5
describe el estado y contexto compartido; la sección 6 presenta el flujo de trabajo de extremo a
extremo; la sección 7 enumera los requisitos no funcionales transversales; la sección 8 describe
la persistencia global; la sección 9 incluye el mapa de trazabilidad a los SRS modulares; y la
sección 10 cierra con el glosario.

---

## 2. Descripción general del sistema

### 2.1 Perspectiva del producto

El Visor de Imágenes V2 es un **DAM de escritorio** orientado a un taller de modelado 3D. Gestiona
bibliotecas masivas de imágenes de renderizado, cada una asociada a un modelo 3D empaquetado en un
archivo comprimido. El flujo comercial típico es: el operador explora la biblioteca, selecciona
activos para un proyecto, verifica su integridad, exporta los archivos y entrega un catálogo al
cliente, que devuelve sus preferencias.

El sistema se ejecuta en **Windows** sobre **Java 21**, con interfaz **Swing + FlatLaf** y
**JavaFX** para la previsualización 3D interactiva.

### 2.2 Stack tecnológico

| Tecnología | Uso |
|------------|-----|
| Java 21 | Plataforma. |
| Swing + FlatLaf | Interfaz de escritorio. |
| JavaFX 21 | Visor 3D interactivo (previews STL) y carrusel. |
| SQLite + JDBI | Base de datos embebida (catálogo y metadatos). |
| Apache PDFBox | Generación de catálogos/presupuestos PDF. |
| 7-Zip (binario externo) | Verificación y análisis de archivos comprimidos. |
| Thumbnailator | Generación de miniaturas. |
| TwelveMonkeys | Soporte de formatos de imagen (CMYK, TIFF, PSD, WebP, etc.). |
| SLF4J + Logback | Logging. |

### 2.3 Funciones del producto a alto nivel

1. **Explorar** la biblioteca de imágenes (VISUALIZADOR).
2. **Previsualizar** modelos 3D y generar previews (RENDER).
3. **Catalogar** los activos con etiquetas y discos (DATOS).
4. **Seleccionar** y verificar la integridad de los activos de un proyecto (PROYECTO).
5. **Compartir** catálogos con el cliente e importar su respuesta (CLIENTE).
6. **Configurar** la aplicación (temas, zoom, toolbars, atajos).

---

## 3. Modos de trabajo

### 3.1 Modos (`WorkMode`)

| Modo | Rol | Entrada (barra de modos) | SRS |
|------|-----|--------------------------|-----|
| **VISUALIZADOR** | Exploración, búsqueda, navegación y visualización; núcleo de la app. | Por defecto al arrancar | `SRS-ModoVisor.md` |
| **CARRUSEL** | Presentación automática de la colección. | Barra de modos | `SRS-ModoVisor.md` |
| **RENDER** | Escaneo y renderizado de modelos 3D a previews. | Barra de modos | `SRS-ModoRender.md` |
| **DATOS** | Etiquetas, discos y catalogación. | Barra de modos | `SRS-ModoDatos.md` |
| **PROYECTO** | Selección, integridad y exportación de archivos. | Barra de modos | `SRS-ModoProyecto.md` |
| **CLIENTE** | Revisión, catálogos HTML/PDF y sincronización. | Barra de modos | `SRS-ModoCliente.md` |

### 3.2 Orquestación de modos (`AppModeService`, `GeneralController`)

- El cambio de modo lo orquesta `GeneralController` (router por modo) y `AppModeService`
  (ciclo de vida: validación → salida → cambio → entrada → título).
- Al cambiar, el sistema ejecuta `salirModo` (guarda el estado del modo anterior), fija el
  `WorkMode` activo y ejecuta `entrarModo` (reparenta los paneles de visualización compartidos,
  ajusta el `DisplayMode` y restaura la UI del modo).
- El contenedor de visualización compartido (`container.displaymodes`, que aloja los paneles
  `ImageDisplayPanel`, `GridDisplayPanel` y `PolaroidDisplayPanel`) se **reparenta** en el
  placeholder del modo activo. Los modos DATOS y CARRUSEL **no participan** en el reparentado y
  usan su propia estructura.
- Cada tarjeta del `CardLayout` maestro (`container.workmodes`) aloja la vista raíz de cada modo
  (`VISTA_VISUALIZADOR`, `VISTA_PROYECTOS`, `VISTA_DATOS`, `VISTA_CLIENTE`, `VISTA_CARROUSEL`,
  `VISTA_RENDER`).

---

## 4. Arquitectura de software

### 4.1 MVC plano

El sistema sigue un **MVC plano** (sin frameworks de inversión de control):

```
controlador/  → lógica de UI y de negocio (controllers, managers, services, commands, factory)
modelo/       → estado y dominio (VisorModel, contextos, modelo de proyecto, datos)
vista/        → presentación (builders, panels, models, renderers, dialogs, theme)
servicios/    → servicios transversales (DB, imagen, cliente, renderer, cache, zoom)
utils/        → utilidades puras
principal/    → entrypoint (VisorV2)
```

### 4.2 Componentes clave

| Clase/Paquete | Responsabilidad |
|---------------|-----------------|
| `principal.VisorV2` | Entrypoint; programa el arranque en el EDT. |
| `VisorController` | Controlador raíz; delega la inicialización en `AppInitializer`. |
| `AppInitializer` | Arranque en 3 fases (instanciación, cableado, inicialización de UI). |
| `GeneralController` | Router por modo, recuperación de sesión, título de ventana. |
| `AppModeService` | Ciclo de vida de modos y reparentado de paneles compartidos. |
| `ComponentRegistry` | DI manual: registro y consulta de componentes por clave String. |
| `ActionFactory` | Fábrica de `Action`; genera el mapa de acciones canónicas. |
| `AppActionCommands` | Constantes `CMD_*` (≈320) que identifican los comandos. |
| `ConfigurationManager` | Carga/persistencia de `config.cfg`. |
| `ConfigKeys` | Constantes de claves de configuración. |
| Managers (`controlador.managers`) | Estado y sub-sistemas (zoom, filtros, carrusel, toolbars, atajos, etc.). |
| Services (`controlador.services`) | Servicios auxiliares (navegación, filtro, ciclo de proyecto). |
| Workers (`controlador.worker`) | Tareas en segundo plano (`SwingWorker`). |

### 4.3 Inyección de dependencias (`ComponentRegistry`)

El sistema usa **DI manual**: `ComponentRegistry` guarda instancias por clave `String`
(p. ej. `frame.principal`, `container.workmodes`, `list.miniaturas`, `list.grid.proyecto`) y
permite registrarlas, consultarlas (tipadas) y darlas de baja. También registra *beans* no-Swing
(p. ej. `"dataController"`). Es el mecanismo que conecta builders y controladores sin acoplarlos.

### 4.4 Comandos y acciones

Toda acción de la UI se modela como un `javax.swing.Action` creado por `ActionFactory`, identificada
por una constante `CMD_*` de `AppActionCommands` (convención `cmd.<area>.<accion>`). Las acciones se
organizan en categorías (archivo, navegación, zoom, proyecto, cliente, datos, render, vista, etc.) y
pueden ser **sensibles al contexto** (se habilitan/deshabilitan según el estado del modelo). Los
atajos de teclado se registran en `KeyboardShortcutManager`.

---

## 5. Estado y contexto compartido

### 5.1 `VisorModel`

`VisorModel` es el **modelo global** de la aplicación. Conserva:

- `WorkMode` activo (inicia `VISUALIZADOR`) y `DisplayMode` activo (inicia `SINGLE_IMAGE`).
- Un **contexto de lista por modo** (`ListContext`): visualizador, proyecto, datos y carrusel.
- Un **contexto de zoom por modo** (`ZoomContext`): factor, modo, offsets de pan y opciones.
- Estado de navegación (imagen actual, miniaturas, salto de bloque, navegación circular).
- Estado del carrusel, del cliente (checkboxes visibles, editor visible, panel de exportación) y de
  ordenación/filtrado.
- Listeners de cambios de lista maestra y de selección.

`getCurrentListContext()` devuelve el contexto del modo activo (en CLIENTE usa el del proyecto).

### 5.2 `ListContext`

Cada `ListContext` guarda:

- `modeloLista` (`DefaultListModel<String>`) — la lista maestra de claves.
- `rutaCompletaMap` (clave → `Path`) — la correspondencia real clave→ruta.
- `selectedImageKey` — la clave de la imagen seleccionada.
- `carpetaRaizContexto` y `mostrarSoloCarpetaActual`.
- En el modo proyecto: `nombreListaActiva`, `seleccionListKey`, `descartesListKey`.
- En el modo datos: `datosSelectedTag`.
- `displayMode` propio y **historial de navegación** (pila de estados para retroceder).

### 5.3 Sincronización entre modos

- Al **salir de un modo**, el controlador guarda en su contexto la selección y las claves de estado
  (p. ej. DATOS guarda el tag seleccionado y la clave de imagen; CLIENTE guarda la última imagen).
- Al **entrar en un modo**, el sistema restaura la UI desde el contexto.
- El modo DATOS recibe la imagen pendiente del VISUALIZADOR para seleccionar el tag más profundo;
  a su vez, al seleccionar una imagen bajo la raíz del visor, fija su clave en el contexto del
  visualizador (gotcha documentado en `AGENTS.md`).

---

## 6. Flujo de trabajo de extremo a extremo

1. **Explorar** — el operador navega y busca en la biblioteca (VISUALIZADOR; CU del
   `SRS-ModoVisor.md`).
2. **Generar renders** (opcional) — si faltan previews, se renderizan los modelos 3D (RENDER;
   CU del `SRS-ModoRender.md`).
3. **Catalogar** (opcional) — se organizan los activos con etiquetas y discos (DATOS; CU del
   `SRS-ModoDatos.md`).
4. **Seleccionar** — se marcan los activos del proyecto y se gestionan selección/descartes
   (PROYECTO; CU del `SRS-ModoProyecto.md`).
5. **Verificar y exportar** — se comprueba la integridad de los comprimidos y se exportan los
   archivos a la carpeta de destino (PROYECTO).
6. **Compartir** — se comparte el proyecto con el cliente (códigos de catálogo y copia `.prjcl`)
   y se pasa al Modo Cliente.
7. **Entregar catálogos** — se exporta el HTML único, el catálogo web o el presupuesto PDF
   (CLIENTE; CU del `SRS-ModoCliente.md`).
8. **Importar respuesta** — se carga el JSON del cliente y se sincroniza la selección
   (CLIENTE).

Este flujo puede repetirse en **iteraciones**: cada envío al cliente incrementa la iteración y los
comentarios del cliente se incorporan al modelo.

---

## 7. Requisitos no funcionales transversales

### 7.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNG-001** | La inicialización pesada y las operaciones de larga duración deben ejecutarse en **hilos de fondo** sin bloquear la EDT. |
| **RNG-002** | La aplicación debe gestionar bibliotecas de **>40.000 activos** manteniendo una UI fluida (carga perezosa de miniaturas y datos). |
| **RNG-003** | Las tareas en segundo plano deben usar el **executor compartido** del `VisorController` (pool de hilos dimensionado al nº de procesadores). |

### 7.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNG-004** | La aplicación debe ofrecer **atajos de teclado** y navegación completa por teclado. |
| **RNG-005** | La interfaz debe ser **personalizable** (toolbars, barras, temas, pantalla completa). |
| **RNG-006** | Los estados de los datos (marcado, etiquetas, estados de exportación) deben comunicarse **visualmente** (color, iconos, tooltips). |

### 7.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNG-007** | El sistema debe **preservar el estado** entre sesiones (carpeta e imagen de inicio, proyecto abierto, recuperación de sesión). |
| **RNG-008** | La configuración debe persistirse de forma **atómica** (escritura temporal con renombrado). |
| **RNG-009** | Las operaciones destructivas deben pedir **confirmación** y nunca eliminar archivos físicos sin consentimiento explícito. |
| **RNG-010** | El cierre de la aplicación debe **validar** tareas pendientes (renders) y guardar la sesión si procede. |

### 7.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNG-011** | El código debe mantener la separación **MVC plano** y la DI por `ComponentRegistry`. |
| **RNG-012** | Los comandos deben centralizarse en `AppActionCommands` y las claves de configuración en `ConfigKeys`. |
| **RNG-013** | La documentación de ayuda debe residir en `resources/help/*.html` por modo. |

### 7.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNG-014** | La aplicación debe ejecutarse en **Windows** con Java 21 (Swing + JavaFX). |
| **RNG-015** | La ruta de la base de datos y de los proyectos debe ser **configurable**. |
| **RNG-016** | Las rutas de Windows deben tratarse de forma **agnóstica al separador** (normalización `\` ↔ `/`). |

### 7.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNG-017** | No deben registrarse credenciales ni datos sensibles en los logs. |
| **RNG-018** | Los datos exportados (catálogos HTML) deben **escaparse** para evitar inyecciones. |
| **RNG-019** | Los datos locales (`config.cfg`, `.proyectos`, BD) deben mantenerse fuera del repositorio (`.gitignore`). |

---

## 8. Persistencia global

| Recurso | Ruta/ubicación | Descripción |
|---------|----------------|-------------|
| `config.cfg` | directorio de trabajo | Configuración de la aplicación (carga, validación y guardado atómico). |
| `.proyectos/` | `user.dir\.proyectos` | Proyectos (`.prj`), copias compartidas (`.prjcl`), temporal de sesión y recuperación. |
| `.temas_personalizados/` | relativa | Temas FlatLaf personalizados del usuario. |
| BD SQLite | configurable | Catálogo de imágenes, etiquetas, discos y metadatos de comprimidos. |
| `log.txt` / consola | según logback | Salida de logs SLF4J/Logback. |

---

## 9. Mapa de trazabilidad a los SRS modulares

| Área | Paquete/Clase principal | SRS |
|------|--------------------------|-----|
| Visor y carrusel | `controlador/VisorController`, `controlador/managers/CarouselManager`, `vista/builders/ViewBuilder` | `SRS-ModoVisor.md` |
| Render | `controlador/RenderController`, `vista/panels/render/PreviewPanel3DFX`, `servicios/renderer/*` | `SRS-ModoRender.md` |
| Datos | `controlador/DataController`, `controlador/managers/DataManager`, `servicios/db/*` | `SRS-ModoDatos.md` |
| Proyecto | `controlador/ProjectController`, `servicios/ProjectManager`, `controlador/services/proyecto/*` | `SRS-ModoProyecto.md` |
| Cliente | `controlador/ClientController`, `servicios/cliente/*`, `vista/builders/ClientBuilder` | `SRS-ModoCliente.md` |
| Global | `principal/VisorV2`, `controlador/AppInitializer`, `controlador/services/AppModeService`, `controlador/GeneralController`, `modelo/VisorModel`, `modelo/ListContext`, `controlador/utils/ComponentRegistry`, `controlador/factory/ActionFactory`, `controlador/commands/AppActionCommands`, `servicios/ConfigurationManager`, `servicios/ConfigKeys` | **Este documento** |

---

## 10. Glosario

- **Activo:** imagen de render + comprimido 3D asociado.
- **Modo:** entorno de trabajo del sistema (`WorkMode`).
- **Lista maestra:** lista de claves de imágenes del modo activo.
- **Contexto de lista:** estado persistente de cada lista por modo.
- **Manager / Builder / Service:** capas de lógica (managers), construcción de UI (builders) y
  utilidades (services).
- **Comando (`CMD_*`):** identificador canónico de una acción.
- **`.prj` / `.prjcl`:** proyecto persistente / copia compartida con el cliente.
- **Recuperación de sesión:** restauración del proyecto temporal tras un cierre sin guardar.

---

*Fin del documento.*