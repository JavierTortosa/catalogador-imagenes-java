# Documento de Especificación de Requisitos de Software (SRS)

## Modo Visor del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo VISUALIZADOR (Visor de imágenes) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Visor**
(`WorkMode.VISUALIZADOR`) del Visor de Imágenes V2, un sistema de gestión de activos digitales
(DAM) de escritorio para bibliotecas masivas (>40.000 activos) de imágenes y modelos 3D.

El Modo Visor es el modo de trabajo **por defecto** de la aplicación y su **núcleo funcional**:
proporciona la exploración, búsqueda, navegación, visualización y previsualización de la
biblioteca de imágenes. Todos los demás modos (PROYECTO, CLIENTE, DATOS, CARRUSEL y RENDER) se
alimentan o sincronizan con el contexto que se gestiona aquí.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia
para el cliente y de base para futuras evoluciones del sistema.

### 1.2 Alcance

El alcance cubre:

- Carga y navegación de carpetas (raíz inicial, árbol de carpetas, drill-down con historial).
- Gestión de la lista de imágenes (carga, ordenación, selección múltiple).
- Búsqueda y filtrado (búsqueda puntual, filtro en vivo «Tornado», filtros permanentes).
- Filtrado por etiquetas (resolución en base de datos).
- Visor principal en sus tres modos de vista: Imagen Única, Cuadrícula (Grid) y Polaroid.
- Zoom y paneo (8 modos de zoom, zoom al cursor, paneo con modificadores).
- Navegación por teclado y ratón.
- Miniaturas (tira inferior, grid, caché y generación perezosa).
- Modo carrusel (presentación automática con velocidad, shuffle y sincronización).
- Edición básica de imagen (rotación, volteo, recorte, renombrado, eliminación).
- Gestión de proyecto desde el visor (marcado de imágenes, ciclo de vida del proyecto).
- Personalización de la interfaz (pantalla completa, siempre encima, paneles ocultables).
- Sincronización con el modo DATOS (etiquetas).

Queda **fuera del alcance** de este documento:

- El modo PROYECTO (gestión de selección, verificación de integridad y exportación de archivos),
  especificado en `SRS-ModoProyecto.md`.
- El modo CLIENTE (revisión, catálogos HTML/web y presupuesto PDF, importación de respuesta),
  especificado en `SRS-ModoCliente.md`.
- El modo RENDER (generación de previews 3D), especificado en `SRS-ModoRender.md`.
- El modo EDITOR (edición de composiciones por capas y documentos `.edoc`), especificado en
  `SRS-ModoEditor.md`.
- La gestión de etiquetas en sí (modo DATOS), especificada en `SRS-ModoDatos.md`; en este
  documento solo se documenta su integración con el visor.

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **Clave de imagen** | Identificador de una imagen en una lista; habitualmente la ruta relativa (o nombre de archivo) dentro de la carpeta raíz. |
| **Lista maestra** | Lista completa de imágenes cargada para el modo activo; sobre ella se aplican filtros. |
| **Contexto de lista** | Estado por modo (`ListContext`): lista, carpeta raíz, selección, historial de navegación, modo de visualización. |
| **Tornado** | Filtro en vivo activable por botón: filtra la lista conforme se teclea. |
| **Filtro permanente** | Criterio acumulable (texto, carpeta o etiqueta) persistente mientras dure la sesión de exploración. |
| **DisplayMode** | Modo de visualización del área principal: `SINGLE_IMAGE`, `GRID`, `POLAROID`. |
| **SortDirection** | Dirección de ordenación: `NONE`, `ASCENDING`, `DESCENDING`. |
| **Shuffle** | Reproducción aleatoria en el carrusel. |
| **EXIF** | Metadatos estándar de las imágenes que incluyen orientación. |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |
| **BD** | Base de datos (SQLite + JDBI) que cataloga imágenes y etiquetas. |

### 1.4 Referencias

- `resources/help/visualizador.html` y documentos de ayuda del visor (`visualizador_ui.html`,
  `visualizador_panel_izquierdo.html`, `visualizador_area_principal.html`,
  `visualizador_barras_info.html`, `visualizador_controles.html`, `visualizador_zoom.html`,
  `visualizador_menus.html`, `conceptos_zoom.html`, `conceptos_modos_vista.html`,
  `atajos.html`).
- `AGENTS.md` — Guía general del proyecto.
- `docs/SRS-ModoRender.md` — Especificación del Modo Render.
- `docs/SRS-ModoDatos.md` — Especificación del Modo Datos.
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

El Modo Visor es el modo de trabajo **por defecto** (`VisorModel` arranca en
`VISUALIZADOR`). El acceso se realiza desde la barra de modos principal o directamente al
iniciar la aplicación. El resto de modos son PROYECTO, CLIENTE, DATOS, CARRUSEL, RENDER y EDITOR.

El modo se integra con el resto del sistema mediante:

- `GeneralController` — router por modo; delega la navegación, zoom y carga al controlador
  correspondiente.
- `VisorController` — controlador principal del visualizador (carga de imágenes, navegación,
  restauración de UI).
- `AppModeService` — orquestación de cambios de modo y reparentado de paneles compartidos.
- `AppInitializer` — arranque en 3 fases, carga de datos iniciales y restauración de sesión.
- `ConfigurationManager` / `ConfigKeys` — parámetros de comportamiento y zoom.
- `ComponentRegistry` — registro de componentes UI por clave (DI manual).
- `ImagenDAO` / `TagDAO` — catálogo de imágenes y etiquetas en base de datos.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Explorar carpetas** del sistema mediante árbol de carpetas o navegación jerárquica.
2. **Navegar** por la lista de imágenes (anterior/siguiente/primera/última, bloques, rueda,
   teclado) con opción de navegación circular.
3. **Buscar y filtrar** imágenes por texto, carpeta y etiquetas, con filtros permanentes
   acumulables.
4. **Visualizar** imágenes a pantalla completa del área central en tres modos: imagen única,
   cuadrícula y polaroid.
5. **Hacer zoom y paneo** con 8 modos de zoom, zoom al cursor y paneo direccional.
6. **Previsualizar** con una tira de miniaturas de carga perezosa y caché.
7. **Presentar** automáticamente la colección con el carrusel (velocidad y shuffle).
8. **Editar** imágenes de forma básica (rotar, voltear, recortar, renombrar, eliminar).
9. **Marcar imágenes para proyecto** y gestionar el ciclo de vida del proyecto.
10. **Personalizar** la interfaz (pantalla completa, paneles ocultables, fondo a cuadros).
11. **Sincronizar** la selección con el modo DATOS para etiquetar imágenes.

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Gestiona bibliotecas masivas; necesita explorar, filtrar y previsualizar imágenes rápidamente. |
| **Cliente final** | Recibe catálogos interactivos (flujo cubierto por otros modos). |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; Swing con FlatLaf; JavaFX 21 para el carrusel/3D.
- **Base de datos:** SQLite embebida + JDBI (catálogo de imágenes y etiquetas).
- **Formatos de imagen:** JPG, JPEG, PNG, GIF, BMP, TIFF, PSD, WebP, TGA, PCX mediante
  ImageIO + TwelveMonkeys 3.10.1; corrección de orientación EXIF con metadata-extractor.

### 2.5 Restricciones de diseño e implementación

- La lista completa de imágenes de una carpeta se **carga en memoria** (`DefaultListModel`);
  no hay paginación de la lista. La carga perezosa aplica a las **miniaturas**.
- El filtrado en vivo y el filtrado permanente se ejecutan en **hilos de fondo**
  (`SwingWorker`) sobre una copia de la lista maestra para no bloquear la EDT.
- La navegación y el zoom mantienen **contextos independientes por modo** (`ListContext`,
  `ZoomContext`).
- El índice de zoom se limita al rango `[0.01, 50.0]`.
- El modo DATOS escribe su selección usando su propio contexto de lista, no el del visor
  (ver gotcha en AGENTS.md).

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/visualizador*.html` y documentos de conceptos y atajos.

### 2.7 Suposiciones y dependencias

- La carpeta de inicio y la imagen de inicio se restauran entre sesiones mediante
  `config.inicio.carpeta` / `config.inicio.imagen`.
- Una imagen ya indexada en BD es accesible por su ruta; las imágenes huérfanas (eliminadas
  en disco) se purgan durante la sincronización con el disco.
- El carrusel puede sincronizarse con el visualizador (misma lista) o recordar su propio
  contexto de sesión.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de
clase entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Carga y navegación de carpetas (`FolderNavigationManager`, `FolderTreeManager`)

| ID | Requisito |
|----|-----------|
| **RF-001** | Al iniciar, el sistema debe cargar la carpeta inicial de exploración desde la configuración (`config.inicio.carpeta`) y, si existe, seleccionar la imagen de inicio (`config.inicio.imagen`). |
| **RF-002** | El sistema debe presentar un **árbol de carpetas** con una raíz virtual «Equipo», cuyas hijas son las unidades del sistema y los subdirectorios no ocultos ordenados alfabéticamente. |
| **RF-003** | Al navegar a una carpeta, el árbol debe sincronizarse: expandir, seleccionar y hacer scroll hasta el nodo correspondiente. |
| **RF-004** | La acción **Abrir aquí** sobre un nodo del árbol debe limpiar el historial de navegación y cargar esa carpeta como nueva raíz. |
| **RF-005** | La acción **Entrar aquí** sobre un nodo del árbol debe guardar el estado actual en el historial antes de cargar la carpeta seleccionada. |
| **RF-006** | El sistema debe mantener un **historial de navegación** por modo (`NavigationState`: carpeta padre + clave de imagen seleccionada) con capacidad de volver atrás y restaurar el estado. |
| **RF-007** | La acción **Subir nivel** debe ir a la carpeta padre (priorizando el historial cuando exista). |
| **RF-008** | La acción **Volver a la raíz** debe restaurar el primer estado del historial (raíz de la sesión) y vaciarlo. |
| **RF-009** | El **escaneo de disco debe ser siempre recursivo** (profundidad completa): la opción **"Mostrar subcarpetas"** es un **filtro visual** de presentación. Con "mostrar solo carpeta actual" el listado se limita a las imágenes cuyo padre coincide exactamente con la raíz; desactivada, se muestran las de toda la jerarquía. Alternar el toggle solo recarga la lista desde la BD aplicando el filtro, **sin re-escaneo de disco**. |
| **RF-010** | La carga de una nueva carpeta debe resetear los filtros (permanentes y en vivo), actualizar la raíz, sincronizar el árbol y disparar la recarga desde disco y base de datos. |
| **RF-011** | Al sincronizar con disco, el sistema debe detectar imágenes nuevas, actualizar la BD y **eliminar huérfanos** (archivos que ya no existen). |
| **RF-012** | El escaneo de disco debe omitir directorios no deseados (p. ej. `__MACOSX`). |

### 3.2 Módulo B — Lista de imágenes y ordenación (`ImageListManager`, `SearchSortService`)

| ID | Requisito |
|----|-----------|
| **RF-013** | El sistema debe cargar la lista de imágenes de la carpeta activa desde la base de datos (`ImagenDAO.getImagenesInFolder`). |
| **RF-014** | La lista cargada debe estar ordenada alfabéticamente por defecto (orden de lectura de BD). |
| **RF-015** | La lista debe exponerse a la UI en tres vistas sincronizadas: **lista de nombres**, **tira de miniaturas** y **grid**. |
| **RF-016** | El sistema debe permitir **ordenar** la lista por nombre de archivo en tres estados cíclicos: sin orden, ascendente (A-Z) y descendente (Z-A). |
| **RF-017** | Al cambiar la ordenación, el sistema debe conservar la selección actual, recalculando el índice de la clave seleccionada. |
| **RF-018** | La lista de nombres debe soportar **selección múltiple** (`MULTIPLE_INTERVAL_SELECTION`) para operaciones en lote. |
| **RF-019** | La selección de navegación (imagen principal) debe ser única, aunque conviva con la multiselección. |

### 3.3 Módulo C — Búsqueda y filtros (`FilterManager`, `SearchSortService`, `FilterService`, `FilterCriterion`)

| ID | Requisito |
|----|-----------|
| **RF-020** | El sistema debe ofrecer un campo de búsqueda en la barra de orden de lista. |
| **RF-021** | La **búsqueda puntual** (pulsar Enter sin filtro en vivo) debe localizar la siguiente coincidencia de forma **cíclica**, case-insensitive, sobre el nombre de archivo, partiendo de la selección actual. |
| **RF-022** | Si la búsqueda puntual no encuentra coincidencias, el sistema debe mostrar un mensaje temporal en la barra de estado. |
| **RF-023** | El **filtro en vivo («Tornado»)** debe activarse mediante un botón toggle y filtrar la lista conforme se teclea. |
| **RF-024** | El filtro en vivo debe aplicar un **debounce de 300 ms** antes de ejecutar el filtrado. |
| **RF-025** | El texto del filtro en vivo debe poder contener **múltiples términos separados por comas**, cada uno tratado como un criterio de tipo texto. |
| **RF-026** | El filtrado (en vivo y permanente) debe ejecutarse en un `SwingWorker` sobre una copia de la lista maestra, sin bloquear la EDT. |
| **RF-027** | El sistema debe permitir **añadir filtros permanentes** desde el diálogo de filtros, con los tipos: texto (nombre de archivo), carpeta (ruta del padre) y etiqueta. |
| **RF-028** | Cada filtro permanente debe admitir dos lógicas: **Añadir** (ADD, criterio positivo) y **Excluir** (NOT, criterio negativo). |
| **RF-029** | Los filtros de texto deben comparar contra el **nombre de archivo** de la imagen (contains, case-insensitive). |
| **RF-030** | Los filtros de carpeta deben comparar contra la **ruta del padre** normalizada (separadores `\` y `/` equivalentes, sin barra final). |
| **RF-031** | Los criterios positivos deben combinarse con **AND** (la imagen debe cumplir todos); los negativos excluyen si se cumplen. |
| **RF-032** | El usuario debe poder **convertir el texto del tornado en filtros permanentes** mediante el botón `+`. |
| **RF-033** | El sistema debe mostrar un panel de **Filtros Activos** listando los criterios con su lógica, tipo y valor. |
| **RF-034** | Desde el panel de Filtros Activos, el usuario debe poder **eliminar** un filtro o **alternar su lógica** ADD/NOT en caliente. |
| **RF-035** | La barra de filtros debe ofrecer acciones para añadir, quitar el seleccionado, subir/bajar nivel de carpeta y limpiar todos. |
| **RF-036** | El título de la lista debe reflejar el filtrado con el formato «Archivos (Filtro): total - visibles». |
| **RF-037** | Los filtros permanentes deben operar siempre sobre la **lista maestra absoluta** (sin filtrar), de modo que al desactivarlos se restaure la lista y selección originales. |
| **RF-038** | Al cargar una nueva carpeta se debe **resetear el estado de filtros** persistentes. |

### 3.4 Módulo D — Filtrado por etiquetas (`TagDAO`, `FilterManager`)

| ID | Requisito |
|----|-----------|
| **RF-039** | El sistema debe permitir filtrar por **etiquetas** almacenadas en la base de datos. |
| **RF-040** | Los filtros de etiqueta positivos deben exigir que la imagen tenga **todas** las etiquetas seleccionadas (intersección de conjuntos de rutas). |
| **RF-041** | Los filtros de etiqueta negativos deben excluir las imágenes que tengan cualquiera de las etiquetas indicadas (unión de rutas a excluir). |
| **RF-042** | Los filtros de etiqueta deben resolverse **antes** que los de memoria (texto/carpeta), usándose su resultado como base del resto de filtros. |
| **RF-043** | El diálogo de filtro debe ofrecer selección de etiqueta con autocompletado (`TagIntelliSenseField`). |

### 3.5 Módulo E — Visor principal (`ImageDisplayPanel`, `GridDisplayPanel`, `PolaroidDisplayPanel`, `DisplayModeManager`)

| ID | Requisito |
|----|-----------|
| **RF-044** | El área central debe soportar **tres modos de visualización** intercambiables: Imagen Única, Cuadrícula y Polaroid. |
| **RF-045** | En modo Imagen Única, la imagen debe dibujarse aplicando el factor de zoom, el centrado y los offsets de paneo del modelo. |
| **RF-046** | El modo de zoom «Rellenar Pantalla» (FILL) debe escalar **deformando** la imagen (sin mantener la proporción); el resto de modos deben mantener la proporción. |
| **RF-047** | El panel debe soportar **fondo a cuadros** (checkerboard) alternativo al fondo sólido. |
| **RF-048** | La carga de la imagen principal debe realizarse **en segundo plano**, cancelando la carga anterior si la selección cambia rápidamente. |
| **RF-049** | Tras la carga, el sistema debe **corregir la orientación EXIF** de la imagen. |
| **RF-050** | Durante la carga, el sistema debe mostrar un mensaje «Cargando…»; ante error de lectura, un mensaje de error o un placeholder «sin imagen» para formatos no soportados. |
| **RF-051** | El panel debe poder mostrar un **placeholder de archivo sin imagen** (p. ej. para STL) y un mensaje para carpetas vacías. |
| **RF-052** | El visor debe poder mostrar **flechas de navegación superpuestas** (anterior/siguiente), activables desde la configuración. |
| **RF-053** | La imagen activa debe poder marcarse con un **marco** de color (verde para marcada, rojo para «sin imagen»). |
| **RF-054** | El modo **Polaroid** debe mostrar la imagen junto a un panel lateral de metadatos (nombre, tipo/formato, tamaño, fecha, ubicación, índice, dimensiones y etiquetas). |
| **RF-055** | En modo **Cuadrícula**, las imágenes deben presentarse en una rejilla con celdas de tamaño fijo, selección múltiple y miniaturas cacheadas. |
| **RF-056** | El doble clic sobre una miniatura del grid o de la tira debe abrir una **previsualización ampliada** (diálogo modal con zoom y paneo). |
| **RF-057** | El panel de imagen activo debe poder **reparentarse** entre contenedores al cambiar de modo, reutilizando el mismo componente. |

### 3.6 Módulo F — Zoom y paneo (`ZoomManager`, `ZoomContext`, `GlobalInputManager`)

| ID | Requisito |
|----|-----------|
| **RF-058** | El sistema debe soportar los modos de zoom: Ajustar a Pantalla, Tamaño Real (100%), Ajustar a Alto, Ajustar a Ancho, Rellenar Pantalla, Mantener Zoom Actual, Porcentaje Personalizado y Ajuste Inteligente. |
| **RF-059** | El factor de zoom debe calcularse comparando las dimensiones de la imagen con las del panel según el modo seleccionado. |
| **RF-060** | El factor de zoom debe estar limitado al rango `[0.01, 50.0]`. |
| **RF-061** | La **rueda del ratón** debe acercar/alejar en pasos de ×1.1 y ÷1.1. |
| **RF-062** | Con **zoom al cursor activado**, la rueda debe anclar el punto de la imagen bajo el cursor durante el zoom. |
| **RF-063** | El **paneo** debe estar disponible únicamente cuando el zoom manual está habilitado. |
| **RF-064** | La rueda con modificadores debe comportarse: Ctrl+Shift = zoom; Shift = pan horizontal; Ctrl = pan vertical. |
| **RF-065** | El usuario debe poder solicitar un **porcentaje de zoom personalizado**, que se guarda en modelo y configuración. |
| **RF-066** | El sistema debe exponer un **D-Pad de paneo** (arriba/abajo/izquierda/derecha, bordes e incremental) para desplazarse en imágenes ampliadas. |
| **RF-067** | El zoom debe disponer de una acción de **reset** que reaplica el modo de zoom actual y resincroniza la UI. |
| **RF-068** | El estado de zoom debe mantenerse por **contexto independiente** por modo de trabajo (visor, proyecto, datos, carrusel). |
| **RF-069** | El porcentaje de zoom debe mostrarse en la barra de estado con el formato «Z: XX%». |
| **RF-070** | La rueda con **zoom manual activo** debe hacer zoom sobre la imagen; sin zoom manual, debe navegar anterior/siguiente. |

### 3.7 Módulo G — Navegación por teclado y ratón (`GlobalInputManager`, `KeyboardShortcutManager`, `ListCoordinator`, `NavigationService`)

| ID | Requisito |
|----|-----------|
| **RF-071** | Las teclas de navegación (↑/↓/←/→, Inicio, Fin, Re Pág, Av Pág) deben navegar por la lista de imágenes, desactivándose cuando el foco está en un campo de texto, tabla o árbol. |
| **RF-072** | El sistema debe soportar navegar a la **primera, anterior, siguiente, última** imagen, y por **bloques** (salto configurable, por defecto 10). |
| **RF-073** | La navegación debe poder ser **circular** (al llegar al final se vuelve al principio) según configuración. |
| **RF-074** | La **rueda del ratón** debe navegar anterior/siguiente al pasar sobre la lista de nombres, el grid o la tira de miniaturas. |
| **RF-075** | El teclado numérico (NUMPAD 1-9, 0) debe activar los modos de zoom, paneo manual, zoom al cursor y reset. |
| **RF-076** | Los dígitos 0-9 sobre la lista de nombres deben seleccionar rápidamente la primera imagen cuyo nombre comience por ese dígito. |
| **RF-077** | El sistema debe soportar la **selección aleatoria** de una imagen (usada por el carrusel con shuffle). |
| **RF-078** | La selección debe sincronizarse entre las tres vistas (nombres, miniaturas, grid) mediante un coordinador único que evita bucles de eventos. |
| **RF-079** | La selección de imagen debe notificarse a los listeners de selección y refrescar las acciones sensibles al contexto. |
| **RF-080** | Los atajos globales deben incluir: **F5** refrescar, **F11** pantalla completa, **Ctrl+L** localizar archivo. |

### 3.8 Módulo H — Miniaturas (`ThumbnailService`, `MiniaturaListCellRenderer`, `GridCellRenderer`, `GridCoordinator`)

| ID | Requisito |
|----|-----------|
| **RF-081** | El sistema debe generar miniaturas **bajo demanda** y mantenerlas en una **caché** con tamaño máximo configurable (por defecto 5000 entradas). |
| **RF-082** | La generación de miniaturas debe realizarse **de forma asíncrona** en un pool de hilos daemon, notificando al renderer cuando la miniatura está lista. |
| **RF-083** | Las miniaturas deben generarse con **Thumbnailator** (escalado con o sin mantener proporción) y con corrección de orientación EXIF previa. |
| **RF-084** | Las rutas que fallan al leer imagen deben registrarse en una **lista negra** para no reintentar, mostrando un icono de imagen rota. |
| **RF-085** | La **tira de miniaturas** debe mostrar una **ventana dinámica** alrededor de la imagen seleccionada (número configurable antes/después), precalentando la caché de esas posiciones. |
| **RF-086** | El **grid** debe precargar miniaturas por ventana de scroll (3 filas por delante) y precachear de forma asíncrona. |
| **RF-087** | Las miniaturas pueden mostrar **nombres de archivo** opcionalmente (`mostrar nombres en miniaturas`). |
| **RF-088** | La tira de miniaturas y el grid deben permitir **selección múltiple** y sincronizarse con la lista de nombres. |
| **RF-089** | Las miniaturas marcadas para proyecto deben mostrarse con un **borde distintivo** (verde). |

### 3.9 Módulo I — Carrusel (`CarouselManager`, acciones de carrusel)

| ID | Requisito |
|----|-----------|
| **RF-090** | El sistema debe ofrecer un modo **carrusel** con controles de reproducción: Play, Pausa y Stop. |
| **RF-091** | En Play, el sistema debe avanzar una imagen cada intervalo de tiempo configurable (por defecto 3000 ms, rango `[500, 30000]` ms). |
| **RF-092** | El **signo** del intervalo debe indicar la dirección: positivo avanza, negativo retrocede. |
| **RF-093** | El sistema debe mostrar una **cuenta atrás** MM:SS superpuesta en el visor durante la reproducción. |
| **RF-094** | En Pausa, la tira de miniaturas debe mostrarse de nuevo; en Play, ocultarse. |
| **RF-095** | El usuario debe poder ajustar la **velocidad** mediante botones +/−/reset (paso de 500 ms), aplicables en caliente sin reiniciar la reproducción. |
| **RF-096** | El usuario debe poder activar el **shuffle**, que selecciona imágenes aleatoriamente. |
| **RF-097** | El sistema debe ofrecer **avance/retroceso rápido** (5 imágenes/s) con botones de pulsación sostenida, pausando el carrusel durante la pulsación y reanudándolo al soltar. |
| **RF-098** | El carrusel debe recordar su **contexto propio** (carpeta y última imagen) entre sesiones. |
| **RF-099** | La **sincronización Visor⇄Carrusel** debe poder activarse: al cambiar entre ambos modos, el contexto de lista se clona entre uno y otro. |
| **RF-100** | Al salir del carrusel sin sincronización, el sistema debe restaurar el contexto recordado. |
| **RF-101** | El visor del carrusel debe mostrar el indicador de estado de reproducción (play/pausa) y el overlay de cuenta atrás en un `JLayeredPane`. |

### 3.10 Módulo J — Edición de imagen (`RotateLeftAction`, `RotateRightAction`, `FlipHorizontalAction`, `FlipVerticalAction`, `CropAction`, `RenameImageAction`, `DeleteAction`, `ImagePropertiesAction`)

| ID | Requisito |
|----|-----------|
| **RF-102** | El sistema debe permitir **rotar** la imagen actual 90° a la izquierda y a la derecha. |
| **RF-103** | El sistema debe permitir **voltear** la imagen horizontal y verticalmente. |
| **RF-104** | El sistema debe permitir **recortar** la imagen (acción de recorte). |
| **RF-105** | El sistema debe permitir **renombrar** el archivo de la imagen actual. |
| **RF-106** | El sistema debe permitir **eliminar** la imagen actual (a la papelera del sistema). |
| **RF-107** | El sistema debe permitir **localizar** el archivo en el explorador del sistema (`explorer.exe /select` con fallback). |
| **RF-108** | El sistema debe permitir establecer la imagen como **fondo de escritorio** o **fondo de bloqueo** del sistema. |
| **RF-109** | El sistema debe permitir consultar las **propiedades** de la imagen actual. |
| **RF-110** | El sistema debe permitir **abrir la imagen con una aplicación externa** seleccionada por el usuario. |
| **RF-111** | El sistema debe permitir **recargar la imagen** actual desde disco. |

### 3.11 Módulo K — Proyecto desde el visor (`VisorController`, `ProjectLifecycleService`, `ProjectController`)

| ID | Requisito |
|----|-----------|
| **RF-112** | El usuario debe poder **marcar/desmarcar** la imagen actual (o la selección múltiple) para el proyecto, con indicador visual en los tres modos de vista y en la barra de estado. |
| **RF-113** | Si el proyecto está **compartido con el cliente**, el sistema debe solicitar confirmación antes de desmarcar una imagen. |
| **RF-114** | El sistema debe permitir **crear un nuevo proyecto** desde el visor. |
| **RF-115** | El sistema debe permitir **abrir un proyecto** guardado en disco (`.prj`). |
| **RF-116** | El sistema debe permitir **guardar** y **guardar como** el proyecto activo. |
| **RF-117** | El sistema debe permitir **eliminar** un proyecto, con confirmación de borrado irreversible. |
| **RF-118** | Al salir de la aplicación con cambios sin guardar, el sistema debe preguntar si se guardan o se **crea una sesión de recuperación**. |
| **RF-119** | Al cerrar la aplicación, el sistema debe persistir la carpeta y la imagen activas para restaurarlas en el siguiente arranque. |

### 3.12 Módulo L — Vista y personalización de la UI (`ToggleFullScreenAction`, toggles de panel)

| ID | Requisito |
|----|-----------|
| **RF-120** | El sistema debe soportar **pantalla completa** (F11) y **siempre encima**. |
| **RF-121** | El usuario debe poder **mostrar/ocultar** cada panel de la UI: barra de menús, barra de herramientas, lista de archivos, tira de miniaturas, barra de estado y barra de información. |
| **RF-122** | El usuario debe poder alternar el **fondo a cuadros** del visor. |
| **RF-123** | El usuario debe poder alternar la **visualización de nombres en miniaturas**. |
| **RF-124** | El usuario debe poder alternar la **visualización de flechas de navegación** sobre la imagen. |
| **RF-125** | El usuario debe poder **agrandar/reducir el tamaño de las miniaturas**. |
| **RF-126** | Los cambios de configuración visual deben persistirse en el archivo de configuración. |
| **RF-127** | El sistema debe mostrar un **diálogo de ayuda de atajos** de teclado accesible desde la UI. |

### 3.13 Módulo M — Integración y modo DATOS (`DataController`, `TaggingManager`, `AppModeService`)

| ID | Requisito |
|----|-----------|
| **RF-128** | El sistema debe recordar la **última imagen activa** del visor para que el modo DATOS recupere el contexto al entrar. |
| **RF-129** | Al entrar en el modo DATOS, el sistema debe procesar la **sincronización pendiente** desde el visor y seleccionar el tag del sistema más profundo de la imagen activa. |
| **RF-130** | Al seleccionar una imagen en el modo DATOS cuya ruta esté bajo la raíz del visor, el sistema debe **sincronizar la selección con el visor** (fijando su clave en el contexto del visualizador). |
| **RF-131** | El modo DATOS debe escribir su selección usando su **propio contexto de lista**, sin pisar la clave de la imagen del visualizador. |
| **RF-132** | El cambio de modo debe mantener la coherencia de la UI (estado de botones, display mode, ordenación, barra de estado) al volver al visor. |

---

## 4. Casos de uso

### CU-01 Abrir una carpeta y navegar por ella

1. El usuario entra en el Modo Visor.
2. Selecciona una carpeta mediante el árbol de carpetas («Abrir aquí») o la acción de abrir carpeta.
3. El sistema carga la lista de imágenes desde BD, ordenada alfabéticamente, y sincroniza el árbol.
4. El usuario navega con las flechas del teclado, la rueda o los botones de navegación.
5. La imagen principal, la lista de nombres y la tira de miniaturas se actualizan sincronizadas.

**Precondiciones:** carpeta accesible. **Postcondiciones:** lista cargada y primera imagen visible.

### CU-02 Buscar y filtrar imágenes

1. El usuario teclea un término en el campo de búsqueda.
2. Pulsa Enter: el sistema salta a la siguiente coincidencia (búsqueda cíclica).
3. (Alternativa) Activa el **Tornado** y escribe: la lista se filtra en vivo con debounce de 300 ms.
4. (Alternativa) Añade filtros permanentes por texto, carpeta o etiqueta mediante el diálogo de filtros.
5. La lista visible y el título «Archivos (Filtro): total - visibles» se actualizan.

### CU-03 Navegar entre imágenes

1. El usuario utiliza teclado (flechas, Inicio/Fin, Re Pág/Av Pág), rueda del ratón o botones.
2. El coordinador de selección actualiza la imagen principal, la lista y la tira de miniaturas.
3. Si la navegación circular está activa, al llegar al final se vuelve al principio.

### CU-04 Ajustar zoom y paneo

1. El usuario selecciona un modo de zoom (Ajustar, 100%, Ancho, Alto, Rellenar, Personalizado…).
2. Hace zoom con la rueda (×1.1), con zoom al cursor si está activado.
3. Con zoom manual, arrastra o usa el D-Pad para paneo; con Shift/Ctrl desplaza horizontal/vertical.
4. El porcentaje se muestra como «Z: XX%» en la barra de estado.

### CU-05 Inspeccionar en cuadrícula y polaroid

1. El usuario cambia el modo de vista a Cuadrícula o Polaroid.
2. En Cuadrícula, explora miniaturas en rejilla; el doble clic abre una previsualización ampliada.
3. En Polaroid, la imagen se muestra junto a sus metadatos (formato, tamaño, fecha, dimensiones).

### CU-06 Presentar la colección con el carrusel

1. El usuario entra en el modo Carrusel.
2. Pulsa Play: las imágenes avanzan cada intervalo configurado con cuenta atrás MM:SS.
3. Ajusta la velocidad (+/−) o activa el shuffle.
4. Usa avance/retroceso rápido con los botones de pulsación sostenida.
5. Pulsa Pausa o Stop para detener.

### CU-07 Editar una imagen

1. El usuario selecciona una imagen y accede al menú Imagen.
2. Rota, voltea, recorta, renombra o elimina la imagen.
3. (Alternativa) La establece como fondo de escritorio/bloqueo, abre su ubicación o consulta sus propiedades.

### CU-08 Marcar imágenes para proyecto

1. El usuario selecciona una o varias imágenes en la lista.
2. Pulsa «Marcar para proyecto»: las imágenes se marcan con borde verde y se reflejan en la barra de estado.
3. Gestiona el proyecto (nuevo, abrir, guardar, guardar como, eliminar).

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNF-001** | La carga de la lista completa debe proceder de BD y ser rápida (consulta por prefijo de carpeta, sin paginación). |
| **RNF-002** | La carga de la imagen principal debe realizarse en un hilo de fondo, cancelando la carga anterior ante selecciones rápidas. |
| **RNF-003** | El filtrado en vivo y el filtrado permanente deben ejecutarse en `SwingWorker` sobre una copia de la lista, sin bloquear la EDT (apto para listas de ~12.000 elementos). |
| **RNF-004** | Las miniaturas deben generarse de forma perezosa y asíncrona, con caché de 5000 entradas por defecto. |
| **RNF-005** | La tira de miniaturas debe precargar solo una ventana dinámica alrededor de la selección. |
| **RNF-006** | El grid debe precargar miniaturas por ventana de scroll (3 filas por delante). |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNF-007** | La navegación debe ser posible por teclado, ratón y botones, con atajos globales documentados. |
| **RNF-008** | La UI debe mantener sincronizadas las tres vistas (nombres, miniaturas, grid) y la barra de estado. |
| **RNF-009** | Los paneles de la UI deben poder ocultarse/mostrarse para adaptarse al espacio de trabajo. |
| **RNF-010** | El filtro en vivo debe responder en menos de 300 ms (debounce) sin degradación perceptible. |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNF-011** | Los errores de lectura de imagen deben capturarse y mostrar un placeholder/mensaje sin bloquear la UI. |
| **RNF-012** | La sincronización con disco debe purgar imágenes huérfanas de BD de forma segura. |
| **RNF-013** | La gestión de cambios sin guardar del proyecto debe ofrecer la opción de guardar o crear sesión de recuperación. |
| **RNF-014** | El estado de navegación y zoom debe conservarse por contexto de modo y restaurarse al volver. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNF-015** | El código debe seguir el patrón MVC plano del proyecto (`controlador` → `modelo` → `vista` → `servicios`). |
| **RNF-016** | Los managers de dominio (filtros, navegación, zoom, miniaturas) deben ser componentes independientes y reutilizables. |
| **RNF-017** | Las constantes de comandos deben centralizarse en `AppActionCommands` y las de configuración en `ConfigKeys`. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNF-018** | Deben soportarse los formatos JPG, JPEG, PNG, GIF, BMP, TIFF, PSD, WebP, TGA y PCX mediante TwelveMonkeys 3.10.1. |
| **RNF-019** | La orientación EXIF debe corregirse al cargar imágenes y al generar miniaturas. |
| **RNF-020** | La localización de archivos en el explorador debe usar mecanismo específico de Windows con fallback genérico. |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNF-021** | No deben registrarse credenciales ni datos sensibles en los logs. |
| **RNF-022** | La eliminación de imágenes debe dirigirse a la papelera del sistema, no a borrado definitivo. |

---

## 6. Modelo de datos

### 6.1 Modelos de estado (`modelo`)

| Clase | Campos principales | Descripción |
|-------|--------------------|-------------|
| `VisorModel` | `workMode`, `displayMode`, `sortDirection`, `navegacionCircular`, `saltoDeBloque`, `carouselDelay`, 4 `ListContext`, 4 `ZoomContext` | Modelo central del estado de la aplicación. |
| `WorkMode` | `VISUALIZADOR, PROYECTO, DATOS, CLIENTE, CARROUSEL, RENDER, EDITOR` | Modos de trabajo. |
| `DisplayMode` | `SINGLE_IMAGE, GRID, POLAROID` | Modos de vista del área central. |
| `SortDirection` | `NONE, ASCENDING, DESCENDING` | Direcciones de ordenación. |
| `ListContext` | `modeloLista`, `carpetaRaizContexto`, `rutaCompletaMap`, `selectedImageKey`, `mostrarSoloCarpetaActual`, `displayMode`, `historialNavegacion`, `seleccionListKey`, `descartesListKey`, `datosSelectedTag` | Estado de lista por modo de trabajo. |
| `NavigationState` | `carpetaPadre`, `claveImagenSeleccionada` | Entrada del historial de navegación. |
| `ZoomContext` | `zoomFactor`, `zoomMode`, `imageOffsetX/Y`, `zoomHabilitado`, `mantenerProporcion`, `zoomToCursorEnabled` | Estado de zoom por modo de trabajo. |

### 6.2 Criterio de filtro (`controlador/managers/filter/FilterCriterion`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `FilterSource` | `FILENAME`, `FOLDER_PATH`, `DATABASE_TAGS` | Origen del filtro. |
| `FilterType` | `CONTAINS`, `DOES_NOT_CONTAIN` | Tipo de comparación. |
| `Logic` | `ADD`, `NOT` | Lógica de combinación (positivo/negativo). |
| `SourceType` | `TEXT`, `FOLDER`, `TAG` | Tipo mostrado en la UI. |

### 6.3 Claves de configuración relevantes (`ConfigKeys`)

| Clave | Por defecto | Descripción |
|-------|-------------|-------------|
| `config.inicio.carpeta` | *(vacío)* | Carpeta inicial de exploración. |
| `config.inicio.imagen` | *(vacío)* | Imagen inicial seleccionada. |
| `comportamiento.zoom.modo_inicial` | `FIT_TO_SCREEN` | Modo de zoom inicial. |
| `comportamiento.zoom.manual_inicial` | `false` | Zoom manual inicial. |
| `comportamiento.zoom.porcentaje_personalizado` | *(valor)* | Porcentaje de zoom personalizado. |
| `comportamiento.navegacion_circular` | `false` | Navegación circular. |
| `comportamiento.cargar_subcarpetas` | *(valor)* | Estado del toggle **Mostrar subcarpetas** (filtro visual de presentación). |
| `comportamiento.zoom_al_cursor_activado` | `false` | Zoom al cursor. |
| `comportamiento.navegacion_salto_bloque` | `10` | Salto de bloque de navegación. |
| `carousel.delay_ms` | `3000` | Intervalo del carrusel (signo = dirección). |
| `comportamiento.sync_visor_carrusel` | `false` | Sincronización Visor⇄Carrusel. |
| `miniaturas.cache.max_size` | `5000` | Tamaño máximo de la caché de miniaturas. |

### 6.4 Registros del `ComponentRegistry` (selección)

| Clave | Componente |
|-------|------------|
| `panel.display.imagen` | Visor de imagen única (compartido). |
| `panel.display.grid` | Visor de cuadrícula (compartido). |
| `panel.display.polaroid.image` | Visor polaroid (compartido). |
| `panel.display.carousel` | Visor del carrusel. |
| `label.carousel.timer.overlay` | Cuenta atrás del carrusel. |
| `label.carousel.status.indicator` | Indicador play/pausa del carrusel. |
| `list.nombresArchivo` | Lista de nombres de archivo. |
| `list.miniaturas` / `scroll.miniaturas` | Tira de miniaturas del visor. |
| `list.miniaturas.carousel` | Tira de miniaturas del carrusel. |
| `list.grid` | Lista del grid. |
| `list.filtrosActivos` | Panel de filtros activos. |
| `label.control.zoomPorcentaje` | Etiqueta «Z: XX%». |
| `label.velocidad.carrusel` | Etiqueta de velocidad del carrusel. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Modelo de estado | `modelo/VisorModel.java`, `modelo/ListContext.java`, `modelo/ZoomContext.java` |
| Carga de listas | `controlador/managers/ImageListManager.java`, `servicios/db/ImagenDAO.java` |
| Navegación de carpetas | `controlador/managers/FolderNavigationManager.java`, `controlador/managers/tree/FolderTreeManager.java`, `controlador/managers/tree/FileSystemTreeModel.java` |
| Filtros | `controlador/managers/FilterManager.java`, `controlador/managers/filter/FilterCriterion.java`, `controlador/services/SearchSortService.java`, `controlador/services/FilterService.java`, `vista/dialogos/FilterDialog.java`, `vista/renderers/FilterCriterionCellRenderer.java` |
| Etiquetas | `servicios/db/TagDAO.java`, `controlador/managers/TaggingManager.java` |
| Visor principal | `vista/panels/ImageDisplayPanel.java`, `vista/panels/GridDisplayPanel.java`, `vista/panels/PolaroidDisplayPanel.java`, `controlador/managers/DisplayModeManager.java` |
| Zoom y paneo | `controlador/managers/ZoomManager.java`, `servicios/zoom/ZoomModeEnum.java`, `controlador/actions/zoom/*.java` |
| Entrada global | `controlador/managers/GlobalInputManager.java`, `controlador/managers/KeyboardShortcutManager.java` |
| Coordinación de selección | `controlador/ListCoordinator.java`, `controlador/GridCoordinator.java` |
| Miniaturas | `servicios/image/ThumbnailService.java`, `vista/renderers/MiniaturaListCellRenderer.java`, `vista/renderers/GridCellRenderer.java`, `vista/util/ThumbnailPreviewer.java` |
| Carrusel | `controlador/managers/CarouselManager.java`, `controlador/actions/carousel/*.java`, `vista/builders/ViewBuilder.java` |
| Edición de imagen | `controlador/actions/edicion/*.java`, `controlador/actions/archivo/*.java`, `controlador/actions/imagen/*.java` |
| Proyecto | `controlador/VisorController.java`, `controlador/ProjectController.java`, `servicios/ProjectLifecycleService.java` |
| Comandos y acciones | `controlador/commands/AppActionCommands.java`, `controlador/factory/ActionFactory.java` |
| Barras y menús | `vista/config/UIDefinitionService.java`, `vista/builders/ViewBuilder.java` |
| Controlador general | `controlador/GeneralController.java`, `controlador/VisorController.java`, `controlador/services/AppModeService.java` |
| Arranque | `controlador/AppInitializer.java` |
| Utilidades de imagen | `utils/ImageUtils.java`, `controlador/utils/DesktopUtils.java`, `utils/ImageFileUtils.java` |
| Ayuda | `resources/help/visualizador*.html`, `resources/help/visor/*.html`, `resources/help/atajos.html` |

### 7.2 Apéndice B — Atajos de teclado principales

| Atajo | Acción |
|-------|--------|
| ↑ / ↓ / ← / → | Navegar entre imágenes. |
| Inicio / Fin | Primera / última imagen. |
| Re Pág / Av Pág | Salto de bloque (10). |
| F5 | Refrescar lista. |
| F11 | Pantalla completa. |
| Ctrl+L | Localizar archivo en el explorador. |
| 1-7 | Modos de zoom. |
| 0 | Reset de zoom. |
| NUMPAD 1-9, 0 | Modos de zoom, paneo manual y zoom al cursor. |
| Shift / Ctrl + rueda | Pan horizontal / vertical. |
| Ctrl+Shift + rueda | Zoom. |

### 7.3 Apéndice C — Glosario técnico

- **Tornado:** filtro en vivo que filtra la lista conforme se teclea, con debounce.
- **Filtro permanente:** criterio acumulable (texto/carpeta/etiqueta) con lógica ADD/NOT que
  sobrevive a los cambios de lista dentro de la sesión de exploración.
- **Ventana dinámica de miniaturas:** rango `antes/después` alrededor de la selección cuyas
  miniaturas se precargan en caché.
- **D-Pad:** control de paneo direccional (arriba/abajo/izquierda/derecha) para imágenes
  ampliadas.
- **Contexto por modo:** cada modo mantiene su propia lista, zoom e historial para preservar
  la sesión al alternar de modo.

---

*Fin del documento.*
