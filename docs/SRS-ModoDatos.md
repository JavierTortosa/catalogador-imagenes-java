# Documento de Especificación de Requisitos de Software (SRS)

## Modo Datos del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo DATOS (Gestión de etiquetas y catalogación) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Datos**
(`WorkMode.DATOS`) del Visor de Imágenes V2, un sistema de gestión de activos digitales (DAM)
de escritorio para bibliotecas masivas (>40.000 activos) de imágenes y modelos 3D.

El Modo Datos es el subsistema de **catalogación y organización por etiquetas (tags)** de la
biblioteca de activos. Combina una red de etiquetas de **sistema** (derivadas automáticamente de
la estructura de carpetas, «carpetas autodescriptivas») con etiquetas de **usuario** libres,
permitiendo buscar, clasificar, asignar y mantener las etiquetas de las imágenes de forma
intuitiva y masiva.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia
para el cliente y de base para futuras evoluciones del sistema.

### 1.2 Alcance

El alcance cubre:

- Acceso y ciclo de vida del modo (entrada, salida, inicialización).
- Biblioteca de etiquetas en dos vistas: árbol jerárquico y lista plana.
- Búsqueda IntelliSense de etiquetas (4 modos de entrada y autocompletado).
- CRUD de etiquetas: crear, renombrar, mover y borrar (incluida la notación de puntos).
- Carga de imágenes asociadas a una etiqueta (incluyendo descendientes).
- Asignación y desasignación de etiquetas a imágenes (individual y masiva).
- Tornados de filtrado (archivos, etiquetas y etiquetas asignadas).
- Unidades de disco catalogadas (detección por número de serie).
- Visor de imágenes del modo (vistas única, cuadrícula y polaroid) y marcado de proyecto.
- Base de datos embebida (esquema, migraciones, indexación de carpetas a etiquetas).
- Sincronización bidireccional con el Modo Visor.
- Comandos, acciones y menús contextuales del modo.

Queda **fuera del alcance** de este documento:

- El Modo VISUALIZADOR (especificado en `SRS-ModoVisor.md`).
- El Modo RENDER (especificado en `SRS-ModoRender.md`).
- El Modo PROYECTO (selección, verificación de integridad y exportación de archivos),
  especificado en `SRS-ModoProyecto.md`.
- El Modo CLIENTE (revisión y exportación de catálogos HTML), especificado en
  `SRS-ModoCliente.md`.

  
### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **Etiqueta (tag)** | Nodo de clasificación jerárquica con nombre único dentro de su nivel. |
| **Etiqueta de sistema** | Etiqueta de solo lectura (`read_only=1`) generada automáticamente a partir de las carpetas del disco durante la indexación. |
| **Etiqueta de usuario** | Etiqueta libre creada por el usuario (`read_only=0`). |
| **Red de carpetas autodescriptivas** | Jerarquía de etiquetas de sistema que refleja la estructura de carpetas del disco. |
| **Notación de puntos** | Ruta de etiqueta expresada como `padre.hijo.nieto`. |
| **IntelliSense** | Campo de texto con autocompletado, navegación por teclado y creación de jerarquías. |
| **Tornado** | Filtro en vivo que filtra la lista conforme se teclea, con debounce. |
| **Disco catalogado** | Volumen con número de serie (VSN) registrado en la base de datos. |
| **Disco conectado** | Volumen presente en el sistema en el momento actual. |
| **VSN** | *Volume Serial Number*; número de serie del volumen (atributo `volume:vsn` de Windows). |
| **Herencia** | Modo de asignación que aplica una etiqueta a las imágenes incluyendo la lógica de descendencia. |
| **BD** | Base de datos (SQLite embebida + JDBI). |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |
| **CTE** | *Common Table Expression*; consulta SQL recursiva. |

### 1.4 Referencias

- `resources/help/modo_datos/` — páginas de ayuda del Modo Datos:
  - `modo_datos.html` (rol del modo gestor de activos).
  - `tags_sistema.html` (etiquetas de sistema y asignación).
  - `paneles_modo_datos.html` (grid con bordes de estado, panel de exportación, propiedades).
  - `intellisense_modo_datos.html` (modos de entrada del IntelliSense).
- `AGENTS.md` — Guía general del proyecto.
- `docs/SRS-ModoVisor.md` — Especificación del Modo Visor.
- `docs/SRS-ModoRender.md` — Especificación del Modo Render.
- `docs/SRS-ModoProyecto.md` — Especificación del Modo Proyecto.
- `docs/SRS-ModoCliente.md` — Especificación del Modo Cliente.
- `docs/SRS-ModoEditor.md` — Especificación del Modo Editor y el Editor Avanzado.

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 presenta la descripción general del producto;
la sección 3 enumera los requisitos funcionales agrupados por módulos; la sección 4 describe los
casos de uso principales; la sección 5 especifica los requisitos no funcionales; la sección 6
describe el modelo de datos; y la sección 7 incluye apéndices con trazabilidad al código fuente.

---

## 2. Descripción general

### 2.1 Perspectiva del producto

El Modo Datos es uno de los modos de trabajo del Visor de Imágenes V2. El acceso se realiza desde
la barra de modos principal (botón **Datos**). El resto de modos son VISUALIZADOR, PROYECTO,
CLIENTE, CARRUSEL, RENDER y EDITOR.

El modo se integra con el resto del sistema mediante:

- `AppModeService` — ciclo de vida del modo (entrar/salir), fuerza de `DisplayMode.GRID` y
  reparentado de paneles compartidos.
- `DataController` — controlador principal (2.823 líneas) con la lógica de la UI y de negocio.
- `DataManager` — lógica de negocio de datos (tags, discos, imágenes, notación de puntos).
- `IndexationService` — generación de etiquetas de sistema desde las carpetas al indexar.
- `TagDAO` / `ImagenDAO` / `DiscoDAO` — acceso a la base de datos.
- `DatabaseManager` — esquema, PRAGMAs y migraciones de la BD.
- `TaggingManager` — puente Visor→Datos (última imagen activa).
- `AppInitializer` — inyección de dependencias y callbacks de CRUD de etiquetas.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Catalogar** la biblioteca con una red jerárquica de etiquetas de sistema y usuario.
2. **Explorar** las etiquetas en árbol (con carga perezosa y contadores) o en lista plana.
3. **Buscar** etiquetas con IntelliSense (texto libre, comodines, rutas y drill-down).
4. **Crear, renombrar, mover y borrar** etiquetas con notación de puntos y protección de sistema.
5. **Cargar imágenes** asociadas a una etiqueta (incluyendo sus descendientes).
6. **Asignar/desasignar** etiquetas a imágenes de forma individual o masiva.
7. **Filtrar** en vivo (tornados) sobre archivos, etiquetas y etiquetas asignadas.
8. **Gestionar unidades** de disco catalogadas (detección por VSN y redetección).
9. **Visualizar** las imágenes de la etiqueta en vistas única, cuadrícula y polaroid.
10. **Marcar** imágenes para proyecto desde el modo.
11. **Sincronizar** la selección con el Modo Visor en ambas direcciones.
12. **Mantener la base de datos** (reindexación, detección de movimientos, purga).

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Catalogador de bibliotecas masivas; organiza y clasifica activos con etiquetas. |
| **Cliente final** | Recibe catálogos interactivos (flujo cubierto por otros modos). |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; Swing con FlatLaf.
- **Base de datos:** SQLite embebida + JDBI (futuro catalogador de la biblioteca completa).
- **Almacenamiento:** base de datos por defecto en `~/.VisorImagenes/visor_collection.db` o ruta
  portable configurable.
- **Identificación de discos:** número de serie de volumen (VSN) mediante atributo de Windows.

### 2.5 Restricciones de diseño e implementación

- El modo DATOS **fuerza `DisplayMode.GRID`** al entrar.
- El modo DATOS **no participa en el reparentado de paneles de visualización compartidos**:
  tiene paneles propios (`panel.datamode.display`, `panel.datamode.grid`,
  `panel.datamode.display.polaroid`).
- Las etiquetas de sistema (`read_only=1`) **no pueden renombrarse, moverse ni borrarse**.
- El nombre de una etiqueta debe ser **único entre sus hermanos** (restricción de BD y de
  negocio) y se normaliza a **minúsculas**.
- Las claves de imagen del modo DATOS son el **nombre de archivo**, con sufijo ` (N)` para
  duplicados; la correspondencia real clave→ruta vive en el `rutaCompletaMap` del contexto de
  datos.
- Solo se muestran imágenes cuyas rutas residan en **discos conectados** actualmente
  (`filterConnectedPaths`).
- La inicialización pesada se ejecuta en un **hilo de fondo** (`"DataController-init"`).

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/modo_datos/*.html`.

### 2.7 Suposiciones y dependencias

- La base de datos se crea/actualiza automáticamente al arrancar (migraciones M1–M5).
- Cada imagen catalogada tiene una `ruta_completa` única en BD.
- Los discos se identifican de forma estable por número de serie (VSN), de modo que un volumen
  puede cambiar de letra sin perder la catalogación.
- El estado del modo (tag seleccionado, imagen seleccionada, mapa de rutas) se conserva en el
  contexto de lista del modo durante la sesión.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de
clase entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Acceso y ciclo de vida del modo (`DataController`, `AppModeService`)

| ID | Requisito |
|----|-----------|
| **RF-001** | Al iniciar la aplicación, el controlador del modo debe inicializarse una única vez (`initialize()`), registrando listeners y lanzando la carga pesada (árbol, lista plana y unidades) en un hilo de fondo. |
| **RF-002** | La carga pesada inicial no debe bloquear la EDT; al terminar, debe seleccionarse la primera fila del árbol si no hay selección. |
| **RF-003** | Al entrar en el modo, el sistema debe forzar el modo de visualización **Cuadrícula (GRID)**. |
| **RF-004** | Al entrar en el modo, el sistema debe refrescar el árbol de etiquetas (preservando expansión y selección), la lista plana, las unidades y las etiquetas disponibles. |
| **RF-005** | El modo debe mostrar su propio panel de visualización sin reparentar paneles compartidos de otros modos. |
| **RF-006** | Al salir del modo, el sistema debe guardar en el contexto el **tag seleccionado** (por nombre, o `"Biblioteca"` para la raíz) y la **clave de imagen** seleccionada. |
| **RF-007** | Al volver a entrar sin imagen pendiente, el sistema debe restaurar el tag guardado y la selección de imagen del contexto. |

### 3.2 Módulo B — Biblioteca de etiquetas (`TagTreeModel`, `TagTreeCellRenderer`, lista plana)

| ID | Requisito |
|----|-----------|
| **RF-008** | El sistema debe presentar la biblioteca de etiquetas como un **árbol jerárquico** con raíz virtual «Biblioteca». |
| **RF-009** | El árbol debe cargar los nodos de forma **perezosa**: los hijos se consultan a BD al expandir cada nodo. |
| **RF-010** | Cada etiqueta debe mostrarse con su nombre y sus contadores en formato `nombre (disponibles/total)`, siendo `total` las imágenes asociadas (recursivo) y `disponibles` las que residen en discos conectados. |
| **RF-011** | Las etiquetas de sistema deben distinguirse tipográficamente (fuente normal con color de acento) de las de usuario (cursiva). |
| **RF-012** | El sistema debe permitir alternar entre la vista **árbol** y la vista **lista plana** mediante un botón toggle, conservando la etiqueta seleccionada en el cambio. |
| **RF-013** | La lista plana debe poder **ordenarse** cíclicamente: A-Z, Z-A y orden original. |
| **RF-014** | Al seleccionar una etiqueta en el árbol o en la lista, el sistema debe cargar sus imágenes (ver módulo E). |
| **RF-015** | Al seleccionar la raíz «Biblioteca», el sistema debe cargar todas las imágenes catalogadas. |
| **RF-016** | El árbol debe permitir refrescarse conservando el estado de **expansión** y la **selección** actual. |

### 3.3 Módulo C — IntelliSense de etiquetas (`TagIntelliSenseField`, `TagSearchEngine`)

| ID | Requisito |
|----|-----------|
| **RF-017** | El campo IntelliSense debe ofrecer **autocompletado** de etiquetas con un listado de sugerencias (máximo 8 visibles). |
| **RF-018** | Debe soportar **cuatro modos de entrada**: texto plano (`foto`), comodín (`.escu`), ruta (`armas.escu`) y drill-down (`.armas.`). |
| **RF-019** | La tecla `.` debe activar el modo drill-down mostrando los hijos del segmento actual. |
| **RF-020** | Las teclas ↑/↓ deben navegar las sugerencias; **Tab** debe aplicar la sugerencia; **Enter** debe confirmar y, si la ruta no existe, ofrecer **crear la jerarquía faltante**; **Escape** debe cerrar el popup. |
| **RF-021** | El campo debe admitir **notación de puntos** completa para expresar rutas de etiqueta. |
| **RF-022** | Al confirmar una ruta inexistente, el sistema debe preguntar si se crean las etiquetas faltantes y crearlas si se acepta. |
| **RF-023** | Ante una **ambigüedad** (varios tags coincidentes en distintas ramas), el sistema debe mostrar un diálogo con las rutas completas para que el usuario elija. |
| **RF-024** | El campo debe indicar los estados «(no existe)», «(sin hijos)» y «Seleccionar…» en el listado de sugerencias. |

### 3.4 Módulo D — CRUD de etiquetas (`DataManager`, `TagDAO`)

| ID | Requisito |
|----|-----------|
| **RF-025** | El sistema debe permitir **crear** una etiqueta nueva, bien por nombre simple, bien por **notación de puntos** creando toda la jerarquía de niveles. |
| **RF-026** | Si el texto del campo coincide con una etiqueta existente, el sistema debe avisar y no crear duplicados. |
| **RF-027** | Los nombres de etiqueta deben normalizarse a minúsculas y ser **únicos entre hermanos** (validación de BD y de negocio). |
| **RF-028** | El sistema debe permitir **renombrar** una etiqueta, rechazando nombres duplicados en el mismo nivel. |
| **RF-029** | El sistema debe permitir **mover** una etiqueta a otro padre (o a la raíz), validando: el nuevo padre no debe ser descendiente de la etiqueta (anti-bucle), la etiqueta no debe ser de sistema y el nombre debe ser único en el destino. |
| **RF-030** | El sistema debe permitir **borrar** una etiqueta. Si tiene hijos, debe ofrecer dos opciones: borrar solo la etiqueta (re-hijando los hijos al abuelo) o borrar toda la rama. |
| **RF-031** | Al borrar, el sistema debe informar del número de imágenes que pierden la etiqueta (las imágenes no se eliminan). |
| **RF-032** | Las etiquetas de sistema (`read_only=1`) deben estar **protegidas**: no permitir renombrar, mover ni borrar. |
| **RF-033** | Tras cualquier cambio estructural, el sistema debe refrescar el árbol, la lista plana, las etiquetas disponibles, el IntelliSense y recargar las imágenes del tag activo. |
| **RF-034** | El menú contextual del árbol debe ofrecer las acciones Nuevo, Renombrar y Borrar, deshabilitando Renombrar/Borrar sobre etiquetas de sistema. |

### 3.5 Módulo E — Carga de imágenes por etiqueta (`DataManager`, `ImagenDAO`, `TagDAO`)

| ID | Requisito |
|----|-----------|
| **RF-035** | El sistema debe cargar las imágenes asociadas a la etiqueta seleccionada **incluyendo las de sus descendientes** (consulta CTE recursiva). |
| **RF-036** | El sistema debe cargar las imágenes de todas las etiquetas con un **nombre dado** en cualquier rama (usado por la vista lista plana). |
| **RF-037** | El sistema debe poder cargar **todas las imágenes catalogadas** (raíz «Biblioteca»). |
| **RF-038** | Las rutas resultantes deben filtrarse para mostrar **solo las de discos conectados** actualmente. |
| **RF-039** | Las imágenes deben presentarse en dos listas sincronizadas: el **grid** (`list.datamode.grid`) y la **lista de nombres** (`list.datamode.filenames`). |
| **RF-040** | Las claves de imagen deben ser el **nombre de archivo**; ante nombres repetidos, el sistema debe añadir un sufijo de desambiguación ` (N)` (p. ej. `imagen.jpg (3)`). |
| **RF-041** | El mapa clave→ruta resultante debe poblarse en el `rutaCompletaMap` del contexto de datos del modo. |
| **RF-042** | Al terminar la carga, el sistema debe restaurar la selección pendiente de sincronización (si existe) o seleccionar el primer elemento. |
| **RF-043** | Si el tornado central está activo, la lista de nombres no debe sobrescribirse con la lista completa; el filtro seguirá aplicándose. |

### 3.6 Módulo F — Asignación y desasignación de etiquetas (`DataManager`, `TagManagementPanel`)

| ID | Requisito |
|----|-----------|
| **RF-044** | El usuario debe poder **asignar** una etiqueta (expresable en notación de puntos) a una o varias imágenes seleccionadas, creando previamente la jerarquía si es necesario. |
| **RF-045** | La asignación debe aplicar **cada etiqueta de la ruta** que la imagen no tenga ya (asignación idempotente). |
| **RF-046** | El usuario debe poder **desasignar** una etiqueta de las imágenes seleccionadas. |
| **RF-047** | El panel «Etiquetas de la Imagen Seleccionada» debe mostrar las etiquetas de la imagen activa como chips. |
| **RF-048** | Con varias imágenes seleccionadas, el panel debe mostrar la **intersección de etiquetas comunes**. |
| **RF-049** | El botón de asignación debe habilitarse/deshabilitarse según exista selección en el grid. |
| **RF-050** | El menú contextual del grid debe ofrecer «Añadir Etiqueta…» con un diálogo IntelliSense y un selector avanzado de etiquetas. |
| **RF-051** | El sistema debe permitir **vincular un archivo 3D** a una imagen desde el menú contextual (asociación de recurso). |
| **RF-052** | El panel de etiquetas debe admitir su propio **tornado de filtrado** y búsqueda de la siguiente coincidencia con salto circular. |

### 3.7 Módulo G — Tornados de filtrado (`TornadoFilterController`, `DataController`)

| ID | Requisito |
|----|-----------|
| **RF-053** | El **tornado central** (archivos) debe filtrar en vivo la lista de nombres por coincidencia case-insensitive, con debounce de 300 ms. |
| **RF-054** | El tornado central debe mostrar un **contador** de resultados filtrados. |
| **RF-055** | Con el tornado central apagado, la tecla Enter debe buscar la **siguiente coincidencia** con salto circular (wrap-around). |
| **RF-056** | Al apagar el tornado central, debe restaurarse la lista completa y la selección si sobrevive. |
| **RF-057** | El **tornado de etiquetas** (izquierda) debe filtrar la biblioteca: en vista árbol fuerza la vista lista mientras esté activo y restaura árbol y expansión al desactivarse. |
| **RF-058** | El **tornado de etiquetas asignadas** (derecha) debe filtrar las etiquetas de la imagen seleccionada en el panel de chips. |
| **RF-059** | El patrón tornado debe ser reutilizable (`TornadoFilterController`): toggle ON = filtro en vivo; toggle OFF = Enter busca siguiente coincidencia; al apagar se invoca el callback de restauración. |

### 3.8 Módulo H — Unidades de disco (`DataManager`, `DriveListPanel`, `VolumeService`, `DiscoDAO`)

| ID | Requisito |
|----|-----------|
| **RF-060** | El sistema debe **detectar** las unidades conectadas y su **número de serie (VSN)** para identificar cada volumen de forma estable. |
| **RF-061** | El sistema debe **registrar** automáticamente los discos nuevos en la base de datos al detectarlos. |
| **RF-062** | El panel «Unidades Catalogadas» debe listar los discos con su nombre, raíz y **estado CONECTADO/DESCONECTADO**. |
| **RF-063** | El panel debe mostrar el **número de imágenes** catalogado por disco con formato abreviado (p. ej. `1.0k imgs`). |
| **RF-064** | El usuario debe poder **redetectar** las unidades mediante un botón. |
| **RF-065** | Si la letra de unidad de un disco cambia, el sistema debe actualizar su `ultima_ruta_conocida`. |
| **RF-066** | Los contadores de imágenes de las etiquetas deben calcularse solo sobre los discos conectados (`disponibles`) y sobre el total (`total`). |

### 3.9 Módulo I — Visor y navegación del modo (`ImageDisplayPanel`, `GridDisplayPanel`, `PolaroidDisplayPanel`)

| ID | Requisito |
|----|-----------|
| **RF-067** | El modo debe presentar las imágenes de la etiqueta en tres vistas: **Imagen Única**, **Cuadrícula** y **Polaroid**. |
| **RF-068** | Al entrar, el modo debe forzar la vista **Cuadrícula**. |
| **RF-069** | La cuadrícula debe permitir **selección múltiple** para operaciones en lote (asignación, marcado, desasignación). |
| **RF-070** | La selección del grid debe sincronizarse con la lista de nombres y viceversa, sin bucles de eventos. |
| **RF-071** | Al seleccionar una imagen en el grid, el sistema debe: fijar su clave en el **contexto de datos**, sincronizar la selección con el visor, cargar la imagen en el visor y actualizar el panel de etiquetas. |
| **RF-072** | El visor debe permitir **navegar** por las imágenes (primera, anterior, siguiente, última y por bloques de 10), respetando la configuración de navegación circular. |
| **RF-073** | El modo debe aplicar el **zoom y el paneo** del contexto de zoom propio del modo. |
| **RF-074** | La imagen activa debe poder **marcarse/desmarcarse para el proyecto**, con indicador visual (marco) en las vistas única, polaroid y en el grid. |
| **RF-075** | Al marcar/desmarcar una selección múltiple, el sistema debe aplicar la operación en grupo (si todas están marcadas, desmarca; si alguna no, marca todas). |
| **RF-076** | Si el proyecto está **compartido con el cliente**, el sistema debe solicitar confirmación antes de desmarcar. |
| **RF-077** | El grid debe ofrecer un menú contextual con: Añadir/Quitar del proyecto, Localizar archivo, Añadir etiqueta y Vincular archivo 3D. |

### 3.10 Módulo J — Base de datos (`DatabaseManager`, `TagDAO`, `ImagenDAO`, `DiscoDAO`, `IndexationService`)

| ID | Requisito |
|----|-----------|
| **RF-078** | El sistema debe gestionar una **base de datos embebida** con esquema versionado y **migraciones automáticas** (M1–M5). |
| **RF-079** | El esquema debe incluir tablas para: discos, imágenes, etiquetas, relación imagen-etiqueta, metadatos de archivos comprimidos y asociaciones de recursos. |
| **RF-080** | La tabla de etiquetas debe garantizar la unicidad de nombre **entre hermanos** (`UNIQUE(parent_id, nombre)`). |
| **RF-081** | La relación imagen-etiqueta debe ser una clave primaria compuesta con borrado en cascada. |
| **RF-082** | El sistema debe aplicar PRAGMAs de rendimiento: foreign keys activas, modo WAL, sincronización NORMAL, almacenamiento temporal en memoria y caché ampliada. |
| **RF-083** | La base de datos debe ubicarse por defecto en `~/.VisorImagenes/visor_collection.db`, o en una ruta portable configurable. |
| **RF-084** | El sistema debe poder **reindexar** la base de datos (REINDEX + VACUUM). |
| **RF-085** | El sistema debe mantener funcionalidades de **limpieza**: etiquetas sin uso, ramas vacías y fusión de etiquetas. |
| **RF-086** | Durante la indexación de una imagen, el sistema debe **crear una etiqueta de sistema por cada carpeta** de su ruta absoluta (omitir carpetas configuradas, p. ej. «ARCHIVOS 3D») y asignarlas a la imagen. |
| **RF-087** | La indexación debe manejar tres escenarios: imagen ya catalogada, imagen **movida** (localizada por nombre+tamaño) e imagen **nueva**. |

### 3.11 Módulo K — Sincronización con el Modo Visor (`TaggingManager`, `DataController`, `AppModeService`)

| ID | Requisito |
|----|-----------|
| **RF-088** | El Modo Visor debe **registrar la imagen activa** al cambiar de selección. |
| **RF-089** | Al entrar en el Modo Datos, el sistema debe recibir la imagen pendiente del visor y seleccionar el **tag de sistema más profundo** de esa imagen. |
| **RF-090** | Al cargar las imágenes de una etiqueta, el sistema debe **restaurar la selección** en el grid de la imagen pendiente (por ruta absoluta). |
| **RF-091** | Al seleccionar una imagen en el Modo Datos cuya ruta esté **bajo la raíz del visor**, el sistema debe fijar su clave en el contexto del visualizador para que, al volver, la imagen sea la misma. |
| **RF-092** | El Modo Datos debe escribir su selección usando **su propio contexto de lista**, sin pisar la clave de la imagen del visualizador (gotcha documentado). |

### 3.12 Módulo L — Comandos, acciones y menús (`AppActionCommands`, `ActionFactory`, `DataBuilder`)

| ID | Requisito |
|----|-----------|
| **RF-093** | El sistema debe exponer comandos canónicos del modo: cambiar vista árbol/lista, ordenar etiquetas, filtrar etiquetas, crear/renombrar/borrar etiqueta, toggle de tornado asignado, mantenimiento de BD y orden cíclico de la lista de archivos. |
| **RF-094** | La barra de la biblioteca debe ofrecer: toggle árbol↔lista, ordenación y botones de CRUD (crear, editar, borrar) con iconos. |
| **RF-095** | La barra de filtrado central debe ofrecer: tornado, marcado para proyecto, ordenación cíclica y contador de resultados. |
| **RF-096** | El panel de asignación debe ofrecer: tornado de etiquetas asignadas, campo IntelliSense, asignar, editar, desasignar y toggle de herencia. |
| **RF-097** | El menú contextual del árbol debe ofrecer crear/renombrar/borrar etiqueta, con el prefijo de ruta del padre en notación de puntos al crear. |

---

## 4. Casos de uso

### CU-01 Explorar la biblioteca de etiquetas

1. El usuario entra en el Modo Datos.
2. Se muestra el árbol de etiquetas con contadores `(disponibles/total)`.
3. El usuario expande ramas (carga perezosa) o cambia a la vista lista plana.
4. Al seleccionar una etiqueta, el grid muestra sus imágenes (incluyendo descendientes).

**Precondiciones:** BD catalogada. **Postcondiciones:** imágenes de la etiqueta visibles.

### CU-02 Crear una etiqueta nueva

1. El usuario escribe `armas.escu.espada` en el campo IntelliSense.
2. El autocompletado le ayuda con la ruta; pulsa Enter.
3. El sistema crea la jerarquía de tres niveles y refresca la biblioteca.
4. (Alternativa) Si la ruta coincide con una existente, se avisa y no se crea duplicado.

### CU-03 Renombrar, mover o borrar una etiqueta

1. El usuario selecciona una etiqueta y pulsa Editar.
2. Elige Renombrar (rechaza duplicados entre hermanos) o Mover (a otro padre, con anti-bucle).
3. (Alternativa) Pulsa Borrar: si tiene hijos, elige «borrar solo» (re-hija al abuelo) o «borrar rama».
4. El sistema informa del impacto sobre imágenes y refresca la biblioteca.

### CU-04 Asignar etiquetas a imágenes

1. El usuario selecciona una o varias imágenes en el grid.
2. Escribe la etiqueta (o ruta) en el campo de asignación y pulsa Asignar.
3. El sistema crea la jerarquía si falta y asigna cada etiqueta de la ruta que falte.
4. El panel de chips muestra las etiquetas de la imagen seleccionada (o las comunes de la selección).

### CU-05 Buscar imágenes por etiqueta

1. El usuario selecciona una etiqueta en el árbol o lista plana.
2. El grid carga las imágenes de la etiqueta y sus descendientes, solo en discos conectados.
3. El usuario navega por el grid (cuadrícula), usando zoom y vista única/polaroid.

### CU-06 Filtrar con tornados

1. El usuario activa el tornado central y teclea: la lista de archivos se filtra en vivo con debounce.
2. (Alternativa) Activa el tornado de etiquetas: la biblioteca se filtra (forzando vista lista en modo árbol).
3. (Alternativa) Activa el tornado de etiquetas asignadas: filtra los chips de la imagen seleccionada.
4. Con el tornado apagado, Enter busca la siguiente coincidencia con salto circular.

### CU-07 Gestionar unidades de disco

1. El usuario entra en el Modo Datos; el panel «Unidades Catalogadas» lista los discos.
2. Se muestran los discos conectados (verde) y desconectados (gris) con su conteo de imágenes.
3. El usuario pulsa «Redetectar Unidades» para registrar volúmenes nuevos.

### CU-08 Marcar imágenes para proyecto

1. El usuario selecciona una o varias imágenes en el grid.
2. Pulsa el botón de marcado: la operación se aplica en grupo.
3. Si el proyecto está compartido con el cliente, se pide confirmación antes de desmarcar.
4. Las imágenes marcadas se reflejan con marco visual.

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNF-001** | La carga pesada inicial (árbol, lista plana, unidades) debe ejecutarse en un hilo de fondo sin bloquear la EDT. |
| **RNF-002** | El árbol debe usar **carga perezosa**: los hijos de cada nodo solo se consultan a BD al expandirlo. |
| **RNF-003** | Las consultas de descendientes y conteos recursivos deben usar **CTE recursivas** eficientes. |
| **RNF-004** | Los conteos masivos de etiquetas deben poder calcularse en **lote** (consulta por rama, mapa de resultados). |
| **RNF-005** | El filtrado en vivo (tornados) debe aplicar **debounce de 300 ms** y ejecutarse sin bloquear la UI. |
| **RNF-006** | La carga de imágenes en el visor debe realizarse en `SwingWorker` (lectura `ImageIO.read` fuera de la EDT). |
| **RNF-007** | La base de datos debe configurarse con PRAGMAs de rendimiento (WAL, caché ampliada, temp en memoria). |
| **RNF-008** | Las etiquetas disponibles para autocompletado deben consultarse con caché (`DataManager.invalidateTagCache`). |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNF-009** | La biblioteca debe presentarse en dos vistas alternativas (árbol y lista) con toggle de un solo clic. |
| **RNF-010** | El IntelliSense debe permitir crear jerarquías sin diálogos adicionales (confirmación integrada). |
| **RNF-011** | Las operaciones en lote (asignación, marcado, desasignación) deben soportar selección múltiple. |
| **RNF-012** | Los estados de los discos (conectado/desconectado) deben mostrarse visualmente. |
| **RNF-013** | El teclado debe estar soportado en el IntelliSense (↑/↓, Tab, Enter, Escape, punto). |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNF-014** | La unicidad de etiquetas debe protegerse en BD (restricción `UNIQUE(parent_id, nombre)`) y en la capa de negocio. |
| **RNF-015** | El borrado de etiquetas no debe eliminar imágenes; solo disocia la relación. |
| **RNF-016** | El movimiento de etiquetas debe impedir **bucles** (el nuevo padre no puede ser descendiente). |
| **RNF-017** | Las etiquetas de sistema deben estar protegidas de edición y borrado en todas las vías (UI y DAO). |
| **RNF-018** | Los errores de lectura de imagen deben capturarse y mostrarse sin bloquear la UI. |
| **RNF-019** | La BD debe actualizarse por migraciones automáticas sin pérdida de datos. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNF-020** | El código debe seguir el patrón MVC plano del proyecto (`controlador` → `modelo` → `vista` → `servicios`). |
| **RNF-021** | La lógica de negocio de datos debe residir en `DataManager` y el acceso a BD en los DAOs, independientes del controlador. |
| **RNF-022** | Los comandos canónicos deben centralizarse en `AppActionCommands` y las claves de configuración en `ConfigKeys`. |
| **RNF-023** | El patrón de filtrado en vivo debe reutilizarse mediante `TornadoFilterController`. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNF-024** | El sistema debe identificar discos por **número de serie (VSN)**, estable ante cambios de letra de unidad. |
| **RNF-025** | El acceso a la BD debe ser portable (ruta configurable). |
| **RNF-026** | La base de datos debe admitir esquemas con etiquetado de sistema (`read_only`) introducido por migración. |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNF-027** | No deben registrarse credenciales ni datos sensibles en los logs. |
| **RNF-028** | Las sentencias SQL de los DAOs deben parametrizarse para evitar inyecciones. |
| **RNF-029** | El borrado de etiquetas debe confirmarse explícitamente, informando del impacto. |

---

## 6. Modelo de datos

### 6.1 Esquema de la base de datos (`DatabaseManager`)

| Tabla | Columnas principales | Descripción |
|-------|----------------------|-------------|
| `discos` | `id`, `numero_serie` (único), `nombre_etiqueta`, `ultima_ruta_conocida` | Unidades catalogadas identificadas por VSN. |
| `imagenes` | `id`, `ruta_completa` (única), `nombre_archivo`, `fecha_modificacion`, `tamano_bytes`, `disco_id`, `ruta_relativa`, `fecha_adicion` | Catálogo de imágenes. |
| `tags` | `id`, `nombre`, `parent_id`, `read_only` — `UNIQUE(parent_id, nombre)` | Jerarquía de etiquetas (sistema y usuario). |
| `imagen_tags` | `imagen_id`, `tag_id` (PK compuesta, FK con CASCADE) | Asociación imagen-etiqueta. |
| `archives_metadata` | *(metadatos de comprimidos)* | Metadatos de archivos comprimidos. |
| `resource_associations` | *(asociación de recursos)* | Vínculo imagen ↔ archivo 3D. |

### 6.2 Modelos de dominio (`modelo/datos`)

| Clase | Campos | Descripción |
|-------|--------|-------------|
| `Tag` | `id`, `nombre`, `parentId`, `readOnly` | Etiqueta de la biblioteca; `isReadOnly()` distingue sistema/usuario. |
| `Disco` | `id`, `numeroSerie`, `nombreEtiqueta`, `ultimaRutaConocida`, `cantidadImagenes` | Unidad catalogada. |

### 6.3 Estado del modo (`ListContext`, `VisorModel`)

| Elemento | Descripción |
|----------|-------------|
| `datosListContext` | Contexto de lista del modo: modelo, mapa clave→ruta, selección, tag seleccionado. |
| `datosZoomContext` | Contexto de zoom independiente del modo. |
| `datosSelectedTag` | Nombre del tag seleccionado (`"Biblioteca"` = raíz), persistido al salir del modo. |
| `rutaCompletaMap` | Mapa clave (nombre con sufijo `(N)`) → ruta absoluta. |

### 6.4 Claves de configuración relevantes (`ConfigKeys`)

| Clave | Descripción |
|-------|-------------|
| `config.database.path` | Ruta portable de la base de datos. |
| `indexacion.excluir_carpetas` | Carpetas que no generan etiquetas de sistema (p. ej. «ARCHIVOS 3D»). |
| `indexacion.omitir_directorios` | Directorios omitidos en la indexación. |
| `comportamiento.mostrar_flechas` | Visibilidad de las flechas de navegación del visor (heredado). |

### 6.5 Registros del `ComponentRegistry` (selección)

| Clave | Componente |
|-------|------------|
| `panel.workmode.datos` | Panel principal del modo DATOS. |
| `tree.datamode.alltags` | Árbol de etiquetas. |
| `list.datamode.alltags.flat` | Lista plana de etiquetas. |
| `panel.datamode.treelist.container` | Contenedor CardLayout árbol↔lista. |
| `list.datamode.filenames` | Lista de nombres de archivo (columna central). |
| `list.datamode.grid` | Grid de imágenes. |
| `panel.datamode.display` / `.display.polaroid(.image)` | Visores de imagen única y polaroid. |
| `panel.datamode.tagmanagement` | Panel de etiquetas de la imagen seleccionada. |
| `panel.datamode.drives` / `list.datamode.drives` | Panel y lista de unidades. |
| `textfield.datamode.tornado` / `toggle.datamode.tornado` / `label.datamode.tornado.count` | Tornado central. |
| `textfield.datamode.tag.intellisense(.create)` | Campos IntelliSense de asignación y creación. |
| `toggle.datamode.tag.tornado` / `toggle.datamode.tag.assigned.tornado` | Tornados de etiquetas y asignadas. |
| `toggle.datamode.mark` | Toggle de marcado para proyecto. |
| `btn.datamode.tag.*` | Botones de CRUD y asignación de etiquetas. |
| `combo.datamode.img.newtag` | Combo de nueva etiqueta de imagen. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Controlador | `controlador/DataController.java`, `controlador/AppInitializer.java` |
| Lógica de negocio | `controlador/managers/DataManager.java`, `controlador/managers/TaggingManager.java` |
| Base de datos | `servicios/db/DatabaseManager.java`, `servicios/db/TagDAO.java`, `servicios/db/ImagenDAO.java`, `servicios/db/DiscoDAO.java` |
| Indexación | `servicios/IndexationService.java`, `servicios/VolumeService.java` |
| UI | `vista/builders/DataBuilder.java`, `vista/tree/TagTreeModel.java`, `vista/tree/TagTreeCellRenderer.java` |
| IntelliSense | `vista/components/TagIntelliSenseField.java`, `vista/components/TagSearchEngine.java` |
| Paneles | `vista/panels/TagManagementPanel.java`, `vista/panels/DriveListPanel.java`, `vista/panels/GridDisplayPanel.java`, `vista/panels/ImageDisplayPanel.java`, `vista/panels/PolaroidDisplayPanel.java` |
| Diálogos | `vista/dialogos/TagSelectionDialog.java`, `vista/dialogos/TagAssignmentDialog.java` |
| Integración de modo | `controlador/services/AppModeService.java`, `controlador/GeneralController.java` |
| Comandos | `controlador/commands/AppActionCommands.java`, `controlador/factory/ActionFactory.java` |
| Patrón tornado | `controlador/utils/TornadoFilterController.java` |
| Modelo | `modelo/VisorModel.java`, `modelo/ListContext.java`, `modelo/datos/Tag.java`, `modelo/datos/Disco.java` |
| Marcado de proyecto | `controlador/managers/interfaces/IProjectManager.java` |
| Ayuda | `resources/help/modo_datos/*.html` |

### 7.2 Apéndice B — Modos de entrada del IntelliSense

| Modo | Ejemplo | Comportamiento |
|------|---------|----------------|
| PLAIN | `foto` | Búsqueda por prefijo/subcadena del nombre. |
| WILDCARD | `.escu` | Búsqueda con comodín (punto inicial). |
| PATH | `armas.escu` | Ruta completa separada por puntos. |
| DRILL_DOWN | `.armas.` | Explora los hijos de `armas` (punto final). |

### 7.3 Apéndice C — Glosario técnico

- **Etiqueta de sistema:** etiqueta `read_only` generada de cada carpeta de la ruta absoluta al
  indexar, formando la «red de carpetas autodescriptivas».
- **Notación de puntos:** ruta de etiqueta `padre.hijo.nieto`; el IntelliSense la interpreta y
  puede crearla.
- **Re-hijado:** al borrar una etiqueta con hijos, sus hijos pasan al abuelo (etiqueta superior).
- **VSN:** número de serie de volumen de Windows, usado como identificador estable de disco.
- **Tornado:** filtro en vivo con debounce implementado por `TornadoFilterController`.

---

*Fin del documento.*
