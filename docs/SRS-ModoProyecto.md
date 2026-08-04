# Documento de Especificación de Requisitos de Software (SRS)

## Modo Proyecto del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo PROYECTO (Selección, verificación de integridad y exportación) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Proyecto**
(`WorkMode.PROYECTO`) del Visor de Imágenes V2, un sistema de gestión de activos digitales
(DAM) de escritorio para bibliotecas masivas (>40.000 activos) de imágenes y modelos 3D.

El Modo Proyecto es el subsistema que permite al operador **marcar y organizar las imágenes** del
proyecto comercial (selección frente a descartes), **verificar la integridad** de los archivos
comprimidos asociados (STL en ZIP/RAR/7Z) a las imágenes seleccionadas y **exportar** dicho
conjunto a una carpeta de destino, así como **generar un catálogo en PDF** técnico para su
entrega. Es el punto de paso obligado entre la exploración de la biblioteca y la entrega al
cliente.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia
para el cliente y de base para futuras evoluciones del sistema.

### 1.2 Alcance

El alcance cubre:

- Ciclo de vida completo del proyecto: crear, abrir, guardar, guardar como y eliminar.
- Persistencia del proyecto en fichero `.prj` (JSON con Gson) y formato legado TXT.
- Modelo de datos del proyecto: lista maestra (esquema v2) y retrocompatibilidad con v1.
- Marcado/desmarcado de imágenes, movimiento a descartes, restauración, vaciado y eliminación.
- Asignación de etiquetas por imagen.
- Listas del modo (Selección Actual y Descartes), estados de vista y coordinador de navegación.
- Preparación y gestión de la **cola de exportación** con sus estados e integridad.
- Verificación de integridad de archivos: existencia, legibilidad y análisis de contenido con 7z.
- Detección de imágenes huérfanas y auto-relocalización.
- Exportación de archivos a carpeta destino (copiar/mover), con preflight y resolución de
  conflictos y sobrescritura.
- Generación del **catálogo PDF** (códigos de catálogo, análisis técnico, previsualización).
- Compartir la selección con el modo CLIENTE (códigos de catálogo y redirección).
- Comandos, acciones, atajos y menús contextuales del modo.

Queda **fuera del alcance** de este documento:

- El Modo Visor (especificado en `SRS-ModoVisor.md`).
- El Modo Render (especificado en `SRS-ModoRender.md`).
- El Modo Datos (especificado en `SRS-ModoDatos.md`).
- La exportación de catálogos **HTML** del cliente (`WebCatalogExporter`) y la revisión/importación
  de la respuesta del cliente, que corresponden al **Modo Cliente** (se especifican en su propio
  documento `SRS-ModoCliente.md`). Aquí solo se documenta la **integración de salida** hacia ese
  modo.

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **Proyecto** | Conjunto persistido de imágenes marcadas para entrega comercial, con sus configuraciones de exportación y estado compartido. |
| **Lista maestra** | Mapa de imágenes del proyecto (`masterImages`), clave = ruta normalizada. |
| **Esquema v1** | Formato legado con `selectedImages`, `discardedImages` y `ClientSelection` separados. |
| **Esquema v2** | Formato actual basado en `masterImages` (`Map<String, ProjectImage>`); cada imagen lleva su estado de proyecto y de cliente. |
| **Selección** | Lista de imágenes marcadas `enSeleccionProyecto = true`. |
| **Descartes** | Lista de imágenes descartadas (`enSeleccionProyecto = false`). |
| **Cola de exportación** | Lista de `ExportItem` derivada de la Selección que se exportará a disco. |
| **Archivo asociado** | Archivo comprimido (ZIP/RAR/7Z) u otro recurso vinculado a una imagen. |
| `ExportStatus` | Estado por fila de la cola (PENDIENTE, ENCONTRADO_OK, NO_ENCONTRADO, …). |
| **Dry/preflight** | Revisión previa que comprueba condiciones sin ejecutar la operación. |
| **Auto-heal** | Relocalización automática de imágenes cuya ruta ya no existe en disco. |
| **Catálogo PDF** | Documento PDF técnico (PDFBox) con códigos e imagen de cada ítem. |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |
| **BD** | Base de datos (SQLite + JDBI) que cachea los metadatos de archivos analizados. |

### 1.4 Referencias

- `resources/help/proyecto.html`, `proyecto_controles.html`, `proyecto_componentes.html` y
  `proyecto_exportar.html` — páginas de ayuda del Modo Proyecto.
- `AGENTS.md` — Guía general del proyecto.
- `docs/SRS-ModoVisor.md` — Especificación del Modo Visor.
- `docs/SRS-ModoDatos.md` — Especificación del Modo Datos.
- `docs/SRS-ModoRender.md` — Especificación del Modo Render.
- `docs/SRS-ModoCliente.md` — Especificación del Modo Cliente (salida de la compartición).

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 presenta la descripción general del producto;
la sección 3 enumera los requisitos funcionales agrupados por módulos; la sección 4 describe los
casos de uso principales; la sección 5 especifica los requisitos no funcionales; la sección 6
describe el modelo de datos; y la sección 7 incluye apéndices con trazabilidad al código fuente.

---

## 2. Descripción general

### 2.1 Perspectiva del producto

El Modo Proyecto es uno de los modos de trabajo del Visor de Imágenes V2. El acceso se realiza
desde la barra de modos principal (botón **Proyecto**). El resto de modos son VISUALIZADOR,
CLIENTE, DATOS, CARRUSEL y RENDER.

El modo se integra con el resto del sistema mediante:

- `ProjectController` — controlador principal del modo (3.204 líneas) con la lógica de la UI y de
  negocio del proyecto.
- `ProjectManager` / `IProjectManager` — lógica de negocio y persistencia del proyecto.
- `ProjectLifecycleService` — orquestación del ciclo de vida (nuevo/abrir/guardar/eliminar/cierre).
- `ProjectListCoordinator` — coordinación de las listas del modo y el grid.
- `ProjectViewState` — estados de vista (selección, descartes, exportación).
- `ExportQueueManager` — construcción y mantenimiento de la cola de exportación.
- `ProjectExportService` / `ExportPreflightService` / `ExportWorker` — exportación a disco.
- `ProjectIntegrityService` / `ValidationService` / `ArchiveAnalysisService` — verificación de
  integridad y análisis técnico.
- `PdfWorkflowService` / `PDFGeneratorService` — catálogo PDF.
- `ProjectFileManagementService` — gestión de archivos asociados y relocalización.
- `ProjectSyncService` / `ProjectStateListener` — sincronización modelo→UI.
- `GeneralController` / `AppModeService` / `AppInitializer` — integración de modo y sesión.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Crear, abrir, guardar y eliminar** proyectos persistentes (fichero `.prj`).
2. **Marcar/desmarcar** imágenes para el proyecto desde el visor y desde este modo.
3. **Organizar** la selección en dos listas: Selección Actual y Descartes.
4. **Mover, restaurar, vaciar y eliminar** imágenes del proyecto (sin tocar el disco).
5. **Etiquetar** cada imagen seleccionada.
6. **Verificar la integridad** de los archivos comprimidos asociados (existencia y análisis).
7. **Autorelocalizar** imágenes huérfanas por nombre desde la base de datos.
8. **Preparar la cola de exportación** y resolver conflictos de nombre.
9. **Exportar** los archivos a una carpeta destino (copiar o mover, con preflight).
10. **Generar un catálogo PDF** técnico con códigos de catálogo.
11. **Asociar** archivos de forma manual y relocalizar archivos o imágenes perdidas.
12. **Compartir** la selección con el Modo Cliente y recuperar la sesión tras un cierre.

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Marca y organiza los activos comerciales; verifica integridad y exporta al cliente. |
| **Cliente final** | Recibe los ficheros exportados y el catálogo PDF (flujo cubierto por otros modos). |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; Swing con FlatLaf.
- **Base de datos:** SQLite embebida + JDBI (catálogo y metadatos técnicos).
- **7-Zip:** binario `7z.exe` necesario para la verificación de integridad (extraído del JAR en
  `lib/bin/7z/` con fallback al PATH del sistema).
- **PDF:** Apache PDFBox para la generación del catálogo.

### 2.5 Restricciones de diseño e implementación

- La verdad del modelo reside en el **mapa `masterImages`** (esquema v2); el esquema v1 se
  conserva solo para compatibilidad/migración.
- Todas las claves de imagen se **normalizan a barras inclinadas hacia adelante** (`/`) mediante
  `ProjectModel.normalizarClaveImagen`, independientemente del SO.
- El formato `.prj` es **JSON** con Gson (`setPrettyPrinting()` + `disableHtmlEscaping()` para
  preservar las barras `\` de rutas Windows).
- Los formatos se distinguen al abrir: la primera línea `{` indica JSON; si no, se trata como
  TXT legado y se **migra a JSON** automáticamente guardando.
- Un proyecto **compartido con el cliente** se guarda además como `.prjcl`; al abrir un `.prj`
  compartido se lanza la concordante `/PRJ` y se pregunta al usuario si quiere entrar en Modo
  Cliente.
- El modo CLIENTE **desactiva las acciones de exportar/compartir** del proyecto; el botón de Modo
  Cliente se bloquea si el proyecto compartido tiene imágenes sin código de catálogo.
- La preparación de la cola y las operaciones de integridad y análisis técnico se delegan para no
  bloquear la EDT (§5).
- El modo PROYECTO **no participa** en el reparentado de los paneles compartidos de visualización
  de otros modos; los reparenta según su propio layout.

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/proyecto*.html`.

### 2.7 Suposiciones y dependencias

- Existe un binario de 7-Zip accesible para la verificación de integridad.
- La base de datos embebida cataloga de forma opcional las imágenes y sus rutas para permitir la
  auto-relocalización por nombre.
- El proyecto es un **conjunto de referencias** a los activos; operaciones como "eliminar" o
  "limpiar" nunca borran archivos del disco.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de clase
entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Ciclo de vida del proyecto (`ProjectLifecycleService`, `ProjectController`, `ProjectManager`)

| ID | Requisito |
|----|-----------|
| **RF-001** | El sistema debe permitir **crear un proyecto nuevo** (`handleNewProject`), previo prompt de guardado si hay cambios; al crear, se resetea el modelo activo, se limpia la vista, la tabla de exportación y se permanece en Modo Proyecto. |
| **RF-002** | El sistema debe permitir **abrir un proyecto** desde un `JFileChooser` filtrado a `*.prj` partiendo de la carpeta base de proyectos. |
| **RF-003** | Al abrir, el sistema debe detectar el formato (JSON o TXT legado) y **migrar a JSON** automáticamente en el caso legado, así como migrar `schemaVersion` v1→v2 si procede. |
| **RF-004** | Si la ruta no es legible, el sistema debe lanzar un error controlado **sin bloquear la aplicación**. |
| **RF-005** | Si el proyecto abierto está **compartido con el cliente** (`isSharedWithClient()`), el sistema debe ofrecer entrar en Modo Cliente en lugar de cargarlo en Modo Proyecto. |
| **RF-006** | El sistema debe permitir **guardar** el proyecto activo; si no hay archivo activo, debe derivar a *Guardar Como*. |
| **RF-007** | **Guardar Como** debe permitir elegir nombre y destino (filtro `*.prj`, nombre por defecto `MiProyecto.prj`) con confirmación de sobrescritura. |
| **RF-008** | El sistema debe permitir **eliminar** el fichero `.prj` del disco con confirmación fuerte; si es el proyecto activo, se crea uno nuevo y se cambia al Modo Visor. |
| **RF-009** | Antes de cualquier operación destructiva o de cambio, el sistema debe consultar si se **guardar**, **no guardar** o **cancelar** cuando hay cambios sin guardar (`promptToSaveChangesIfNecessary`). |
| **RF-010** | Al **cerrar la aplicación**, el sistema debe gestionar los cambios sin guardar y, si el usuario los descarta, **guardar una sesión de recuperación** (fichero temporal y clave `PROYECTO_RECUPERACION_PENDIENTE`). |
| **RF-011** | Al arrancar (y de nuevo al entrar en Modo Proyecto/Cliente), el sistema debe detectar una recuperación pendiente y ofrecer **Restaurar Sesión / Abrir Proyecto / Cancelar**. |
| **RF-012** | Al restaurar la sesión, el sistema debe recuperar el proyecto temporal con su `originalProjectPath`, marcar el proyecto como **modificado** y limpiar el fichero de recuperación. |

### 3.2 Módulo B — Modelo de proyecto y persistencia (`ProjectModel`, `ProjectImage`, `ProjectManager`)

| ID | Requisito |
|----|-----------|
| **RF-013** | El modelo del proyecto debe mantener una **lista maestra** (`masterImages`) que mapee ruta de imagen (clave) a `ProjectImage`. |
| **RF-014** | Cada `ProjectImage` debe guardar estado de proyecto (`enSeleccionProyecto`), etiqueta, precio, estado de cliente (`SelectionState`), código de catálogo, `ExportConfig`, checkboxes internos, comentarios e hilo de comentarios. |
| **RF-015** | El sistema debe mantener la compatibilidad con el **esquema v1** (`selectedImages`, `discardedImages`, `ClientSelection`) y migrar todos los datos al esquema v2 sin pérdidas. |
| **RF-016** | El sistema debe **normalizar las claves de imagen** a barras inclinadas hacia adelante al leer/escribir y al operar sobre el modelo. |
| **RF-017** | La serialización a JSON debe ser **legible** (pretty-printing) y **preservar las rutas de Windows** (sin escapar `\`, `disableHtmlEscaping`). |
| **RF-018** | Al guardar un proyecto en esquema v2, el sistema debe **omitir temporalmente los campos v1** del JSON (no serializarlos) y restaurarlos en memoria. |
| **RF-019** | El sistema debe soportar dos extensiones: `.prj` (proyecto) y `.prjcl` (copia compartida con el cliente). |
| **RF-020** | El sistema debe detectar si hay **cambios sin guardar** (`isProjectDirty`) comparando selección, descartes, exportConfigs, descripción y lista maestra, y **notificar el cambio de estado** para actualizar el título de la ventana. |

### 3.3 Módulo C — Selección, descartes y marcado (`ProjectManager`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-021** | El sistema debe permitir **marcar y desmarcar** una imagen para el proyecto, y **alternar** su marca. |
| **RF-022** | El sistema debe permitir **mover la selección actual a descartes** (una o varias imágenes). |
| **RF-023** | Al mover a descartes **estando el proyecto compartido** con el cliente, el sistema debe solicitar **confirmación** antes de operar. |
| **RF-024** | El sistema debe permitir **restaurar imágenes** desde los descartes a la Selección, y pasar a la vista de selección si los descartes quedan vacíos. |
| **RF-025** | El sistema debe permitir **vaciar los descartes** con confirmación. |
| **RF-026** | El sistema debe permitir **eliminar imágenes del proyecto** (solo del modelo, nunca del disco) con confirmación. |
| **RF-027** | El sistema debe poder **limpiar las imágenes no encontradas** (huérfanas) detectadas en el proyecto, conservando los archivos en disco. |
| **RF-028** | El sistema debe permitir **asignar y borrar una etiqueta** a la imagen activa en selección (esquema v2: `ProjectImage.setEtiqueta`). |
| **RF-029** | El sistema debe permitir **vincular etiquetas de sistema** del Modo Datos desde el menú contextual mediante `TagAssignmentDialog`. |

### 3.4 Módulo D — Listas del modo y navegación (`ProjectListCoordinator`, `ProjectViewState`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-030** | El modo debe presentar **dos listas**: Selección Actual y Descartes, con su contexto de lista compartido (`proyectoListContext`). |
| **RF-031** | El contexto debe conservar la **lista activa** (`seleccion`/`descartes`), las últimas claves seleccionadas, el mapa clave→ruta y el modelo de lista. |
| **RF-032** | El modo debe soportar **tres estados de vista**: `VIEW_SELECTION`, `VIEW_DISCARDS` y `VIEW_EXPORT` (el grid muestra la selección con bordes de estado). |
| **RF-033** | Al cambiar de estado de vista, el sistema debe **redirigir la fuente de datos del grid** a la lista activa y, en `VIEW_EXPORT`, preparar la cola de exportación. |
| **RF-034** | El coordinador debe permitir **navegación**: siguiente, anterior, primera, última, bloques de N (configurable, por defecto 10), aleatorio y con la rueda del ratón. |
| **RF-035** | La navegación debe respetar la configuración de **navegación circular** y evitar **bucles de sincronización** (anti-reentrada). |
| **RF-036** | El sistema debe **sincronizar la selección** entre la lista activa y el grid/visor, aplicando la clave al visor y la fila al grid. |
| **RF-037** | La selección del grid debe **sincronizarse con la tabla de exportación** cuando el estado de vista lo requiera. |
| **RF-038** | El coordinador debe permitir seleccionar por **clave o índice**, buscando en las dos listas, y notificar a los listeners de cambio de selección maestra. |

### 3.5 Módulo E — Cola de exportación (`ExportQueueManager`, `ExportItem`, `ProjectExportService`)

| ID | Requisito |
|----|-----------|
| **RF-039** | El sistema debe **preparar la cola de exportación** a partir de la Selección, respetando el estado en memoria de los ítems ya presentes. |
| **RF-040** | Para un ítem nuevo, el sistema debe aplicar **prioridad de carga**: (1) config guardado en el `.prj`, (2) búsqueda en disco si no hay config. |
| **RF-041** | El sistema debe **buscar el archivo comprimido asociado**: escanear el directorio de la imagen filtrando por nombre base y extensión comprimida (`.zip`, `.rar`, `.7z`). |
| **RF-042** | Si hay **varios candidatos** válidos, el sistema debe asignarlos todos como asociados en orden (multipart) y marcar el estado correspondiente. |
| **RF-043** | Cada ítem debe exponer un **estado de archivo** (`ExportStatus`) con texto, tooltip y color (neutral / OK / aviso / error). |
| **RF-044** | El sistema debe **detectar conflictos de nombre** dentro de la cola: imágenes duplicadas y archivos asociados duplicados (que provocarían sobrescritura). |
| **RF-045** | El sistema debe permitir **forzar el reescaneo** en disco (`refresh`) solo sobre ítems no asignados manualmente, no ignorados y no perdidos. |
| **RF-046** | El sistema debe conservar (al refrescar y guardar/restaurar) las **sobrescrituras de análisis** (`lycheeOverride`, `chituboxOverride`, `totalSizeMb`) de cada `ExportItem`. |

### 3.6 Módulo F — Verificación de integridad y análisis técnico (`ValidationService`, `ProjectIntegrityService`, `ArchiveAnalysisService`, `ArchiveMetadataDAO`)

| ID | Requisito |
|----|-----------|
| **RF-047** | El sistema debe **verificar la integridad** de la imagen y sus archivos asociados antes de considerarla exportable (`isValidForExport`). |
| **RF-048** | La verificación debe contemplar: imagen existente, `ExportConfig` normalizado, ignorar comprimido explícito, verificación de todos los asociados, o búsqueda de un comprimido asociado. |
| **RF-049** | El sistema debe realizar el **análisis de contenido** de un comprimido mediante **7-Zip** (`7z l -slt`), con timeouts y terminación forzada del proceso. |
| **RF-050** | El análisis debe contener el **conteo de piezas** (STL/OBJ/3MF), piezas **con y sin soporte**, detector de **Lychee** (`.lys`) y **Chitubox** (`.ctb`) y tamaño total. |
| **RF-051** | El análisis debe **procesar comprimidos anidados** recursivamente, extrayendo a un directorio temporal descartable. |
| **RF-052** | El sistema debe **cachear los metadatos** de análisis en la base de datos (tabla `archives_metadata`, clave = ruta del archivo). |
| **RF-053** | El sistema debe **detectar imágenes huérfanas** (cuya ruta ya no existe en disco). |
| **RF-054** | Ante un huérfano, el sistema debe **auto-relocalizarlo** por nombre de archivo buscando en la base de datos (`findPathByFileName`), migrar su `ExportConfig` y notificar el cambio. |
| **RF-055** | Al refrescar la vista del proyecto debe ejecutarse la **auto-relocalización** de huérfanos automáticamente. |
| **RF-056** | El sistema debe permitir **limpiar los huérfanos no relocalizados** del modelo (sin borrar archivos del disco). |
| **RF-057** | El flujo de PDF deberá **comprobar prerrequisitos** (librerías XZ/JunRar y accesibilidad de archivos) reportando advertencias no bloqueantes. |

### 3.7 Módulo G — Exportación de archivos (`ProjectExportService`, `ExportWorker`, `ExportPanel`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-058** | El sistema debe permitir **elegir la carpeta de destino** de exportación (diálogo `DIRECTORIES_ONLY`) y persistirla en el modelo. |
| **RF-059** | Antes de exportar, el sistema debe ejecutar un **preflight** que valide la carpeta destino (existe, es directorio, es escribible) y **liste los archivos en conflicto** existentes. |
| **RF-060** | Si existen conflictos con el destino, el sistema debe ofrecer cuatro opciones: **Sincronizar**, **Sobrescribir**, **Limpiar Carpeta** o **Cancelar**. |
| **RF-061** | El sistema debe soportar operación de **copiar** o **mover**; la de mover se indica con un toggle visual de peligro y requiere confirmación. |
| **RF-062** | Si un ítem tiene **varios archivos asociados**, la exportación debe agruparlos en una **subcarpeta** con el nombre base de la imagen. |
| **RF-063** | La operación debe ejecutarse en segundo plano (worker) mostrando **progreso** preciso (1 paso por imagen más 1 por cada asociado) y permitiendo **cancelación**. |
| **RF-064** | Al terminar, el sistema debe mostrar un **resumen** y ofrecer **abrir la carpeta de destino**. |

### 3.8 Módulo H — Catálogo PDF (`PdfWorkflowService`, `PDFGeneratorService`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-065** | El sistema debe **asignar códigos de catálogo** correlativos (`C001`, `C002`, …) a los ítems seleccionados, con número de dígitos ajustado al tamaño del lote. |
| **RF-066** | Antes de generar el PDF, el sistema debe **asegurar el análisis técnico** de los ítems (con umbral opcional para omitir el análisis profundo en grandes lotes). |
| **RF-067** | El sistema debe **previsualizar el PDF** en un diálogo antes del guardado final. |
| **RF-068** | El sistema debe generar el PDF con **PDFBox** (A4, grid 2×2): código, imagen y datos técnicos (piezas C/S y S/S, Lychee, Chitubox, tamaño, LVL, PVP, notas). |
| **RF-069** | Las **notas del proyecto** deben añadirse como páginas finales del PDF si no están en blanco. |
| **RF-070** | Al confirmar, el sistema debe **persistir** los datos del catálogo (código, piezas, LVL, PVP, notas) en el modelo (`ExportConfig`/`ProjectImage`). |
| **RF-071** | El guardado del PDF debe partir de la **carpeta de destino** configurada por defecto. |

### 3.9 Módulo I — Asociación de archivos y relocalización (`ProjectFileManagementService`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-072** | El usuario debe poder **añadir manualmente** un archivo asociado a una imagen de la cola. |
| **RF-073** | El usuario debe poder **quitar** un archivo asociado de un ítem. |
| **RF-074** | El usuario debe poder **localizar manualmente** el archivo comprimido de un ítem (asignación manual). |
| **RF-075** | El usuario debe poder **relocalizar** una imagen perdida de la tabla de exportación o del visor. |
| **RF-076** | Al relocalizar, el sistema debe **migrar** los metadatos clave (etiqueta, descartes, `ExportConfig`) de la ruta antigua a la nueva. |
| **RF-077** | El usuario debe poder **ignorar el comprimido** (solo exportar la imagen) o **quitar de la cola** un ítem. |
| **RF-078** | El usuario debe poder **abrir la ubicación** de la imagen en el explorador. |

### 3.10 Módulo J — Sincronización modelo↔UI (`ProjectSyncService`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-079** | El sistema debe **sincronizar la UI desde el modelo** al refrescar (relleno de listas, tabla, grid) sin pisar la selección. |
| **RF-080** | El sistema debe gestionar el **caso de lista vacía** entre selección y descartes evitando estados inconsistentes. |
| **RF-081** | El sistema debe conservar y mostrar los **metadatos del proyecto** (nombre y descripción) en el panel de propiedades. |
| **RF-082** | El sistema debe **actualizar los contadores** de los títulos de las listas (Selección Actual: N, Descartes: N) al cambiar el estado. |
| **RF-083** | El sistema debe **notificar a los listeners** de estado del proyecto (título de ventana) cuando cambia el flag de cambios sin guardar. |

### 3.11 Módulo K — Compartir e integrar con el cliente (`ClientController`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-084** | El sistema debe permitir **compartir el proyecto con el cliente** (acción Compartir) validando que haya imágenes y código de catálogo. |
| **RF-085** | Al compartir, el sistema debe generar **códigos correlativos**, marcar el estado compartido (marca de tiempo e iteración), guardar una copia `.prjcl`, y cambiar al Modo Cliente. |
| **RF-086** | El sistema debe **validar previa** (preflight de asignaciones) que todas las imágenes seleccionadas tengan código de catálogo antes de compartir. |
| **RF-087** | Al abrir un `.prj` compartido, el sistema debe **redirigir al Modo Cliente** (ofreciendo entrar) para continuar la revisión. |
| **RF-088** | Cuando la respuesta del cliente se recibe, el importador debe actualizar `estadoCliente`, `estadoClienteOriginal`, comentarios y `ImageCheckboxOverlay`s, **preservando `enSeleccionProyecto`**. |
| **RF-089** | El sistema debe **habilitar/deshabilitar cruzada**: en Modo Cliente se desactiva exportar/compartir; fuera de él, «Compartir» se habilita si no hay conflictos y sin imágenes sin código; las exportaciones HTML/Web solo si el proyecto está compartido y completo. |

### 3.12 Módulo L — Menús, comandos y atajos (`AppActionCommands`, `ActionFactory`, `KeyboardShortcutManager`, `ProjectBuilder`)

| ID | Requisito |
|----|-----------|
| **RF-090** | El sistema debe exponer **comandos canónicos** del modo (`CMD_PROYECTO_*` y `CMD_EXPORT_*`) para ciclo de vida, listas, compartir y exportación. |
| **RF-091** | El sistema debe registrar **atajos de teclado**: `Ctrl+N` (nuevo), `Ctrl+O` (abrir), `Ctrl+S` (guardar), `Ctrl+Shift+S` (guardar como), `Ctrl+L` (localizar), `F5` (refrescar) y `F11` (pantalla completa). |
| **RF-092** | En las listas del modo, el sistema debe soportar **navegación con las flechas ←/→** y **búsqueda rápida por dígitos**. |
| **RF-093** | El menú contextual de la **tabla de exportación** debe ofrecer: asignar archivo, añadir/quitar asociado, localizar asociado, abrir ubicación, ignorar comprimido, quitar de cola y relocalizar imagen. |
| **RF-094** | El menú contextual del visor del proyecto debe ofrecer acciones análogas más **Añadir Etiqueta**. |
| **RF-095** | El sistema debe ofrecer dos **layouts** del modo (`DEFAULT` y `ASSIGNMENT`) con toggle y reset a DEFAULT. |
| **RF-096** | Las **acciones sensibles al contexto** deben actualizar su estado habilitado/deshabilitado según el modelo (selección, proyecto compartido, estado de exportación). |

---

## 4. Casos de uso

### CU-01 Crear un proyecto nuevo

1. El usuario selecciona «Proyecto» en la barra de modos.
2. Si hay cambios sin guardar, se le pregunta si quiere guardar.
3. El sistema crea un modelo vacío y muestra las listas vacías.

**Precondiciones:** usuario en la app. **Postcondiciones:** proyecto vacío activo.

### CU-02 Abrir un proyecto existente

1. El usuario pulsa «Abrir» (`Ctrl+O`).
2. Selecciona un fichero `*.prj`; el sistema detecta el formato y migra si es legado.
3. Si el proyecto está compartido, se le ofrece pasar al Modo Cliente.
4. Las listas y el panel de exportación se rellenan.

### CU-03 Guardar / Guardar como

1. El usuario pulsa «Guardar» (`Ctrl+S`); sin archivo activo deriva a «Guardar Como».
2. Elige nombre y carpeta de destino y confirma sobrescritura si aplica.
3. El sistema serializa a JSON y actualiza el título de la ventana.

### CU-04 Marcar imágenes y mover a descartes

1. El operador marca imágenes desde el visor.
2. En el Modo Proyecto, selecciona varias en la lista de Selección y pulsa «Mover a Descartes».
3. (Alternativa) El sistema pregunta confirmación si el proyecto está compartido.
4. Las imágenes pasan a la lista de Descartes y se actualizan los contadores.

### CU-05 Restaurar y vaciar descartes

1. El usuario selecciona imágenes de Descartes y pulsa «Restaurar».
2. Al quedar Descartes vacías, el sistema regresa a la vista de Selección.
3. (Alternativa) «Vaciar Descartes» restaura todas con confirmación.

### CU-06 Etiquetar una imagen

1. El usuario selecciona una imagen en el grid activo.
2. Asigna una etiqueta a la imagen o la quita desde el menú contextual o el diálogo de etiquetas.
3. La etiqueta se persiste en el `ProjectImage`.

### CU-07 Preparar la cola de exportación

1. El usuario activa la vista de Exportación (`VIEW_EXPORT`).
2. El sistema prepara la cola desde la Selección, buscando los archivos asociados.
3. Cada fila muestra código, imagen, estado y archivos asignados.

### CU-08 Resolver integridad y relocalizar

1. Con estado «NO ZIP» o «IMG. PERDIDA», el usuario usa el menú de la tabla.
2. Localiza manualmente el comprimido o relocaliza la imagen perdida; o bien el sistema
   auto-relocaliza el huérfano durante el refresco.

### CU-09 Exportar a carpeta destino

1. El usuario elige la carpeta de destino y pulsa «Exportar».
2. El preflight valida el destino y muestra los conflictos si los hay.
3. Elige Sincronizar / Sobrescribir / Limpiar Carpeta / Cancelar y confirma mover si aplica.
4. El worker copia/mueve archivos con progreso; al final ofrece abrir la carpeta.

### CU-10 Generar catálogo PDF

1. El usuario pulsa «Generar PDF».
2. Se asegura el análisis técnico, se asignan códigos y se muestra el preflight del PDF.
3. Se muestra la previsualización y el usuario confirma el guardado final.

### CU-11 Compartir con el cliente

1. El usuario pulsa «Compartir con el cliente».
2. El sistema valida códigos, genera la copia `.prjcl` y pasa al Modo Cliente.
3. (Alternativa) abre una `.prj` compartida → se ofrece entrar en Modo Cliente.

### CU-12 Recuperar la sesión

1. El usuario cierra la app con los cambios descartados; se guarda `session_recovery.prj`.
2. Al arrancar, el sistema ofrece Restaurar Sesión.
3. Se restaura el proyecto marcado como modificado y se limpia la recuperación.

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNG-001** | La preparación de la cola, el análisis de archivos y la exportación deben ejecutarse en **segundo plano** sin bloquear la EDT. |
| **RNG-002** | El análisis de un archivo con `7z` debe aplicar **timeouts** (60 s listado, 120 s extracción) y terminar el proceso por la fuerza. |
| **RNG-003** | Los metadatos de análisis deben **cachearse** en la base de datos para evitar re-analizar archivos repetidos. |
| **RNG-004** | La exportación debe informar de un **progreso preciso** computado como imagen + asociados. |
| **RNG-005** | El guardado/restauración del proyecto debe preservar la **prioridad de carga** de los `ExportItem` en memoria para evitar recomputo. |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNG-006** | La interfaz debe distinguir claramente los **tres estados de vista** y las dos listas. |
| **RNG-007** | Los **estados de la cola** deben comunicarse con color, texto corto y tooltip. |
| **RNG-008** | Las operaciones destructivas (guardar sobre fichero, mover, limpiar carpeta, vaciar descartes, eliminar) deben **solicitar confirmación**. |
| **RNG-009** | El teclado debe dar soporte a los atajos documentados y a la navegación por teclado de las listas. |
| **RNG-010** | Debe ofrecer la opción de **abrir la carpeta de destino** tras la exportación y **abrir la ubicación** de una imagen. |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNG-011** | Las operaciones sobre el proyecto deben ser todos **no destructivos** respecto a los archivos físicos (eliminar, limpiar, vaciar nunca borran del disco). |
| **RNG-012** | Debe controlarse la **coherencia de rutas** mediante normalización (`/`) para evitar duplicados por backslash. |
| **RNG-013** | El fichero `.prj` debe ser **atómico** en su escritura y tolerante a la migración de versiones. |
| **RNG-014** | La detección de conflictos de nombre debe ser **proactiva** para prevenir sobrescrituras no deseadas. |
| **RNG-015** | Los procesos externos (`7z`) deben limpiarse (destruir) si superan timeout para evitar procesos zombis. |
| **RNG-016** | Los datos del proyecto compartido deben conservar un **historial** (timestamp e iteración) para auditoría. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNG-017** | El código debe seguir el patrón MVC plano del proyecto. |
| **RNG-018** | La lógica de exportación, integridad y PDF debe residir en **servicios desacoplados** del controlador. |
| **RNG-019** | Los comandos y claves de config se centralizarren en `AppActionCommands` y `ConfigKeys`. |
| **RNG-020** | Los formatos de persistencia deben ser **versionados** y con rutas de migración. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNG-021** | El sistema debe soportar formatos comprimidos ZIP, RAR y 7Z. |
| **RNG-022** | La referencia al binario de 7-Zip debe **buscar en el JAR** y fallback al `PATH` del sistema. |
| **RNG-023** | Las rutas deben tratarse de forma agnóstica al separador del SO. |
| **RNG-024** | La generación de PDF debe deshabilitar el **desmapeo de memoria de PDFBox** en Windows. |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNG-025** | No deben registrarse credenciales ni datos sensibles. |
| **RNG-026** | La operación de **Mover** (que elimina del origen) debe **pedir confirmación** explícita. |
| **RNG-027** | Los ficheros de proyecto deben poder contener **códigos de catálogo** protegidos de divulgación accidental. |

---

## 6. Modelo de datos

### 6.1 Modelos de dominio (`modelo/proyecto`)

| Clase | Campos principales | Descripción |
|-------|--------------------|-------------|
| `ProjectModel` | `schemaVersion`, `masterImages`, `selectedImages` (v1), `discardedImages` (v1), `projectName`, `projectDescription`, `creationDate`, `lastModifiedDate`, `exportConfigs`, `exportDestinationFolder`, `sharedWithClient`, `sharedTimestamp`, `sharedIteration`, `imageCodes`, `originalProjectPath`, `lastWorkMode`, `clientSelection` (v1, deprecado) | Raíz del proyecto; claves normalizadas. Métodos de migración v1→v2. |
| `ProjectImage` | `rutaImagen`, `etiqueta`, `enSeleccionProyecto`, `price`, `estadoCliente`, `estadoClienteOriginal`, `codigoCatalogo`, `exportConfig`, `checkboxes` (`List<ImageCheckboxOverlay>`), `commentOverlay`, `comment`, `commentThread` | Imagen dentro de la lista maestra con estado de proyecto y de cliente. |
| `ExportConfig` | `exportEnabled`, `ignoreCompressed`, `status`, `codigoCatalogo`, `piezas`, `piezasConSoporte`, `piezasSinSoporte`, `lvl`, `pvp`, `notas`, `hasLychee`, `hasChitubox`, `totalSizeMb`, `associatedFiles` | Configuración persistida de exportación por imagen. |
| `ExportItem` | `rutaImagen`, `miniatura`, `metadata`, `rutasArchivosAsociados`, `candidatosArchivo`, `estadoArchivoComprimido`, `seleccionadoParaExportar`, `tieneConflictoDeNombre`, `codigoCatalogo`, `piezas`, lvl/pvp/notas, overrides (`lycheeOverride`, `chituboxOverride`, `totalSizeOverride`), tamaños | Ítem de la cola de exportación en memoria. |
| `ExportStatus` | enum | Estados de la cola con `display`, `iconName`, `tooltip` y `color`. |
| `SelectionState` | enum `SELECTED`, `DISCARDED`, `UNDEFINED` | Estado tristado de la revisión del cliente. |

### 6.2 Estados de la cola de exportación (`ExportStatus`)

| Estado | Clasificación | Significado |
|--------|---------------|-------------|
| `PENDIENTE`, `COPIANDO` | Neutral | En espera o copiando. |
| `ENCONTRADO_OK`, `ASIGNADO_MANUAL`, `COPIADO_OK` | OK (verde) | Comprimido encontrado/asignado/copiado. |
| `MULTIPLES_CANDIDATOS`, `IGNORAR_COMPRIMIDO`, `NO_ENCONTRADO` | Aviso (amarillo) | Conflicto para resolver. |
| `IMAGEN_NO_ENCONTRADA`, `ERROR_COPIA`, `NOMBRE_DUPLICADO`, `ASIGNADO_DUPLICADO` | Error (rojo) | Bloquea la exportación. |

### 6.3 Metadatos de análisis en BD (`archives_metadata`)

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `archive_path` | TEXT (PK) | Ruta del archivo comprimido. |
| `stl_count` | INTEGER | Piezas del modelo. |
| `supported_stl_count` | INTEGER | Piezas con soporte. |
| `unsupported_stl_count` | INTEGER | Piezas sin soporte. |
| `is_multipart` | INTEGER | Es multiparte (0/1). |
| `has_lychee` | INTEGER | Contiene `.lys`. |
| `has_chitubox` | INTEGER | Contiene `.ctb`. |
| `total_size_mb` | REAL | Tamaño del contenido. |
| `analysis_date` | INTEGER | Fecha de análisis (epoch milis). |

Nota: la tabla `resource_associations` vincula `imagenes.id` con `archives_metadata.archive_path` (PK compuesta con CASCADE).

### 6.4 Claves de configuración relevantes (`ConfigKeys`)

| Clave | Descripción |
|-------|-------------|
| `proyectos.carpeta_base` | Carpeta base de proyectos (por defecto `proyectos_carpeta_base`). |
| `proyectos.archivo_temporal_nombre` | Nombre del proyecto temporal de sesión. |
| `proyectos.archivo.recuperacion` | Ruta del fichero de recuperación de sesión. |
| `proyectos.estado.lista_activa` | Lista activa (`seleccion`/`descartes`) persistida. |
| `proyectos.estado.ultima_seleccion_key` | Última clave seleccionada en selección. |
| `proyectos.estado.ultima_descartes_key` | Última clave seleccionada en descartes. |
| `proyectos.estado.ultimo_proyecto_abierto` | Último proyecto abierto. |
| `proyectos.estado.recuperacion_pendiente` | Flag de recuperación pendiente. |
| `proyectos.grid.mostrar.estado.state` | Si el grid muestra bordes de estado. |
| `comportamiento.navegacion.salto_bloque` | Tamaño del salto de bloque de navegación (def. 10). |

### 6.5 Registros del `ComponentRegistry` (selección)

| Clave | Componente |
|-------|------------|
| `view.panel.proyectos` | Panel raíz del modo. |
| `splitpane.proyecto.main` / `.left` / `.right` | Splits del layout. |
| `tabs.proyecto.herramientas` / `tabs.proyecto.left.assignment` | Pestañas de herramientas y layout de asignación. |
| `panel.proyecto.seleccion.container` / `.descartes.container` | Paneles de listas (Selección Actual / Descartes). |
| `list.proyecto.nombres` / `list.proyecto.descartes` | Listas. |
| `list.grid.proyecto` / `scroll.grid.proyecto` | Grid compartido. |
| `panel.proyecto.display` / `.grid` / `.polaroid` | Visores compartidos. |
| `panel.proyecto.exportacion.completo` | `ExportPanel`. |
| `tabla.exportacion` | JTable de exportación. |
| `panel.proyecto.exportacion.detalles` | `ExportDetailPanel`. |
| `panel.proyecto.propiedades` | `ProjectMetadataPanel`. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Controlador | `controlador/ProjectController.java`, `controlador/ProjectLayout.java`, `controlador/ProjectViewState.java`, `controlador/ProjectListCoordinator.java`, `controlador/GridCoordinator.java` |
| Ciclo de vida | `controlador/services/ProjectLifecycleService.java` |
| Persistencia | `servicios/ProjectManager.java`, `servicios/ProjectStateListener.java`, `controlador/managers/interfaces/IProjectManager.java` |
| Cola de exportación | `controlador/managers/ExportQueueManager.java` |
| Exportación | `controlador/services/proyecto/ProjectExportService.java`, `ExportWorker.java`, `ExportPreflightService.java`, `ExportPreflightReport.java`, `ExportStatusReport.java` |
| Integridad/análisis | `servicios/ValidationService.java`, `controlador/services/proyecto/ProjectIntegrityService.java`, `servicios/ArchiveAnalysisService.java`, `servicios/db/ArchiveMetadataDAO.java`, `servicios/db/DatabaseManager.java` |
| PDF | `controlador/services/proyecto/PdfWorkflowService.java`, `modelo/export/pdf/PDFGeneratorService.java`, `modelo/export/pdf/PDFExportPreflightService.java` |
| Gestión de ficheros | `controlador/services/proyecto/ProjectFileManagementService.java` |
| Sincronización | `controlador/services/proyecto/ProjectSyncService.java` |
| UI | `vista/builders/ProjectBuilder.java`, `vista/panels/export/*.pdf` (ExportPanel, ExportTableModel, rendereres/editores) |
| Comandos | `controlador/commands/AppActionCommands.java`, `controlador/factory/ActionFactory.java` |
| Atajos | `controlador/managers/KeyboardShortcutManager.java` |
| Worker | `controlador/worker/ExportWorker.java`, `controlador/worker/ArchiveAnalysisWorker.java` |
| Modelo | `modelo/proyecto/*.java`, `modelo/datos/ArchiveMetadata.java` |
| Integración cliente | `controlador/ClientController.java`, `servicios/cliente/ClientResponseImporter.java` |
| Ayuda | `resources/help/proyecto*.html` |

### 7.2 Apéndice B — Prioridad de carga de la cola de exportación

1. **ExportConfig** guardado en `.prj`: restaura selección, código, piezas, LVL/PVP/notas, uso de
   Lychee/Chitubox, tamaño, archivos asociados; aplica `IGNORAR_COMPRIMIDO` o `ASIGNADO_MANUAL`
   según corresponda.
2. **Sin config:** si la imagen existe, busca el comprimido asociado en disco; si no, posición
   `IMAGEN_NO_ENCONTRADA`.
3. Si ya estaba en la cola, se conserva el `ExportItem` en memoria (metadatos, candidatos,
   sobreescritura de análisis).

### 7.3 Apéndice C — Resolución de conflictos de exportación

| Opción | Comportamiento |
|--------| ---------------|
| **Sincronizar** | Solo añade los archivos que faltan (`soloModificados = true`, respeta los existentes en destino). |
| **Sobrescribir** | Copia con `REPLACE_EXISTING`. |
| **Limpiar Carpeta** | Vacía completamente el destino antes de copiar. |
| **Cancelar** | Aborta la operación. |

### 7.4 Apéndice D — Glosario técnico

- **Auto-heal:** relocalización automática de una imagen huérfana mediante búsqueda por nombre en
  la base de datos.
- **Prioridad de carga:** orden con el que `ExportQueueManager` reconstituye un `ExportItem`.
- **Preflight:** validación previa del destino y de los prerrequisitos de una operación.
- **Multipart:** un ítem con varios archivos asociados que se exportan agrupados en una subcarpeta.

---

*Fin del documento.*