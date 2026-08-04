# Documento de Especificación de Requisitos de Software (SRS)

## Modo Cliente del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo CLIENTE (Revisión, exportación de catálogos y sincronización) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Cliente**
(`WorkMode.CLIENTE`) del Visor de Imágenes V2, un sistema de gestión de activos digitales (DAM)
de escritorio para bibliotecas masivas (>40.000 activos) de imágenes y modelos 3D.

El Modo Cliente es el subsistema que permite al operador **compartir una selección del proyecto
con el cliente final**, revisar y **exportar catálogos** (HTML interactivo único, carpeta web y
presupuesto PDF) y, al recibir la respuesta del cliente, **importarla y sincronizarla** con el
proyecto. Es el cierre del flujo comercial: selección → revisión → entrega → respuesta →
reconciliación.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia para
el cliente y de base para futuras evoluciones del sistema.

### 1.2 Alcance

El alcance cubre:

- Acceso y ciclo de vida del modo (entrada, salida, activación, reparentado de paneles).
- Compartición del proyecto con el cliente (primera vez e iteraciones) y formato `.prjcl`.
- Revisión del cliente: tablas de proyecto y de cliente, estado tristate por imagen y checkbox.
- Panel de edición de checkboxes superpuestos sobre la imagen (añadir, reposicionar, precios,
  comentarios).
- Exportación del **catálogo HTML único** (miniaturas base64, CSS/JS inline, galería, modal).
- Exportación del **catálogo web** a carpeta.
- Generación del **presupuesto PDF** del cliente.
- Importación de la **respuesta del cliente** (JSON) y sus estadísticas.
- Mensajes y comentarios (hilo de mensajes por imagen y por checkbox, comentarios superpuestos).
- Cierre y sincronización de la revisión con el proyecto.
- Comandos, acciones y menús contextuales del modo.

Queda **fuera del alcance** de este documento:

- El Modo Visor (especificado en `SRS-ModoVisor.md`), el Modo Render (`SRS-ModoRender.md`), el
  Modo Datos (`SRS-ModoDatos.md`) y el Modo Proyecto (`SRS-ModoProyecto.md`).
- La gestión de la cola de exportación de archivos del Modo Proyecto, salvo la integración de
  entrada (compartir) y salida (importar/sincronizar).

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **Compartir** | Acción que convierte una selección de proyecto en un catálogo revisable, generando códigos de catálogo y una copia `.prjcl`. |
| **Iteración** | Envío del catálogo al cliente; cada envío incrementa el contador de iteraciones. |
| `SelectionState` | Estado tristate: `SELECTED`, `DISCARDED`, `UNDEFINED`. |
| **Checkbox overlay** | Casilla superpuesta en una imagen con posición, estado, código, precio y tamaño. |
| **Comentario overlay** | Texto superpuesto sobre la imagen con posición y contenido. |
| **Hilo de mensajes** | Conversación (taller/cliente) por imagen o checkbox (`CommentThread`). |
| **Estado del hilo** | `estadoPr`/`estadoHtml`: `0` sin mensajes, `1` leído, `2` contestado, `3` nuevo. |
| **Catálogo HTML único** | Archivo `.html` autocontenido con miniaturas base64 y CSS/JS inline. |
| **Respuesta del cliente** | JSON devuelto por el cliente con los estados y comentarios de cada imagen. |
| **Sincronizar** | Al cerrar la revisión, ajustar la selección del proyecto a los estados del cliente. |
| **Collapse** | Plegado de los checkboxes de una imagen en la tabla (NONE/SMART/FULL). |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |

### 1.4 Referencias

- `resources/help/cliente.html` — página de ayuda del Modo Cliente.
- `AGENTS.md` — Guía general del proyecto.
- `docs/SRS-ModoProyecto.md`, `docs/SRS-ModoVisor.md`, `docs/SRS-ModoDatos.md` y
  `docs/SRS-ModoRender.md` — Especificaciones de otros modos.

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 presenta la descripción general del producto; la
sección 3 enumera los requisitos funcionales agrupados por módulos; la sección 4 describe los casos
de uso principales; la sección 5 especifica los requisitos no funcionales; la sección 6 describe el
modelo de datos; y la sección 7 incluye apéndices con trazabilidad al código fuente.

---

## 2. Descripción general

### 2.1 Perspectiva del producto

El Modo Cliente es uno de los modos de trabajo del Visor de Imágenes V2. El acceso se realiza desde
la barra de modos principal (botón **Cliente**). El resto de modos son VISUALIZADOR, PROYECTO,
DATOS, CARRUSEL y RENDER.

El modo se integra con el resto del sistema mediante:

- `ClientController` — controlador principal del modo (1.444 líneas).
- `ClientBuilder` — construcción del panel del modo y registro de componentes.
- `AppModeService` / `GeneralController` — ciclo de vida del modo, pre-validación de entrada y
  reparentado de paneles de visualización compartidos.
- `ProjectManager` / `ProjectModel` — modelo del proyecto y persistencia `.prj` / `.prjcl`.
- `ClientSyncService` — sincronización al cerrar la revisión.
- `ClientResponseImporter` — importación de la respuesta del cliente.
- `WebCatalogExporter` — exportación del catálogo HTML único y de la carpeta web.
- `PDFGeneratorService` — generación del presupuesto PDF (`crearPresupuestoCliente`).
- `ExportPreflightService` — validación previa de asignación de códigos.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Compartir** el proyecto con el cliente, asignando códigos de catálogo correlativos.
2. **Entrar** en el modo de revisión con las cuatro tablas (proyecto y cliente).
3. **Revisar** cada imagen con estado tristate por imagen y por checkbox interno.
4. **Editar** la selección (activar/desactivar edición), añadir y reposicionar checkboxes y
   comentarios superpuestos, asignar precios.
5. **Exportar un catálogo HTML único** autocontenido para el cliente.
6. **Exportar un catálogo web** a una carpeta.
7. **Generar el presupuesto PDF** del cliente.
8. **Importar la respuesta del cliente** (JSON) y mostrarla en el modelo.
9. **Conversar** con el cliente mediante hilos de mensajes por imagen y por checkbox.
10. **Cerrar y sincronizar** la revisión, ajustando la selección del proyecto.

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Prepara y comparte el catálogo, revisa la respuesta del cliente y sincroniza el proyecto. |
| **Cliente final** | Recibe el catálogo HTML/PDF y responde con un JSON de selecciones y comentarios. |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; Swing con FlatLaf.
- **Base de datos:** SQLite embebida + JDBI (catálogo de imágenes).
- **PDF:** Apache PDFBox para el presupuesto del cliente.

### 2.5 Restricciones de diseño e implementación

- El estado de revisión del cliente vive en **`ProjectImage`** (`estadoCliente`,
  `estadoClienteOriginal`, `checkboxes`, `commentOverlay`, `commentThread`) dentro de la lista
  maestra del proyecto.
- Las claves compuestas `codigoImagen_codigoCheckbox` (p. ej. `C001_cb01`) asocian cada checkbox
  a su imagen; los códigos de imagen se generan correlativos (`C001`, `C002`, …) al compartir.
- Por defecto **no se puede modificar la selección** en el modo Cliente: hay que activar el modo
  edición (con confirmación) mediante «Editar selección».
- El modo Cliente **fuerza `DisplayMode.SINGLE_IMAGE`** al entrar y reparenta el contenedor de
  visualización compartido.
- En modo Cliente quedan **deshabilitadas** las operaciones de exportación del Modo Proyecto
  (copiar/mover archivos) y compartir; el botón de Modo Cliente se bloquea si el proyecto
  compartido tiene imágenes sin código.
- Al **cerrar y sincronizar**, las imágenes en `UNDEFINED` se tratan como descartadas y se genera
  un backup previo en `.prj`.
- El gotcha del `%` en text blocks: si el JS embebido usara `String.formatted()` o se inyectara
  `%` dentro de un text block con formato, el operador módulo debe duplicarse como `%%` (la
  implementación actual usa concatenación de cadenas y no requiere el escape).
- Gson requiere que `module-info.java` abra `modelo.proyecto` para deserializar enums anidados.

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/cliente.html`.

### 2.7 Suposiciones y dependencias

- El proyecto debe estar **compartido** (o contener datos de cliente) para entrar en el modo.
- El JSON de respuesta del cliente debe incluir el campo `respuestas`.
- El cliente final dispone de un navegador moderno para el catálogo HTML único.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de clase
entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Acceso y ciclo de vida del modo (`AppModeService`, `GeneralController`, `ClientController`)

| ID | Requisito |
|----|-----------|
| **RF-001** | Al entrar en el Modo Cliente, el sistema debe **pre-validar** el estado del proyecto: si está compartido y hay imágenes sin código de catálogo, debe **bloquear la entrada** («Modo Cliente No Disponible»). |
| **RF-002** | Si no hay datos de cliente ni proyecto compartido, el sistema debe ofrecer **abrir un `.prj`** antes de entrar. |
| **RF-003** | Si hay datos de proyecto pero no está compartido, el sistema debe **avisar de que hay que compartirlo** antes de entrar. |
| **RF-004** | Al entrar, el sistema debe **forzar `DisplayMode.SINGLE_IMAGE`**, reparentar el contenedor de visualización compartido y mostrar la tarjeta `VISTA_CLIENTE`. |
| **RF-005** | Al entrar, el sistema debe **activar la vista de cliente** (cargar la primera imagen) y **marcar el proyecto como guardado**. |
| **RF-006** | Al salir del modo, el sistema debe **guardar el estado** del cliente (última imagen activa). |
| **RF-007** | El sistema debe permitir **alternar la visibilidad de la selección** con el botón «Editar selección», pidiendo confirmación la primera vez y guardando el flag `clientModeClosed`. |

### 3.2 Módulo B — Compartir con el cliente (`ClientController`, `ProjectModel`, `ExportPreflightService`)

| ID | Requisito |
|----|-----------|
| **RF-008** | El sistema debe permitir **compartir el proyecto por primera vez** (`compartirConCliente`), siempre que haya imágenes seleccionadas. |
| **RF-009** | Al compartir, el sistema debe **construir la lista maestra** en memoria preservando los datos existentes (seleccionados, descartes y estados de cliente previos). |
| **RF-010** | Al compartir, el sistema debe **asignar códigos de catálogo correlativos** (`C001`, `C002`, …) con número de dígitos ajustado al total. |
| **RF-011** | Al compartir, el sistema debe **validar las asignaciones** (`ExportPreflightService.validarAsignaciones`) y **abortar sin commit** si alguna imagen seleccionada no tiene código. |
| **RF-012** | Al confirmar, el sistema debe fijar `sharedWithClient`, la marca de tiempo, `sharedIteration = 1` y el esquema v2, **guardar una copia `.prjcl`** y **cambiar al Modo Cliente**. |
| **RF-013** | El sistema debe permitir **compartir una nueva iteración** (`compartirIteracion`): incorporar imágenes nuevas de la selección/descartes, asignar códigos continuando desde el máximo existente e **incrementar la iteración**. |
| **RF-014** | El sistema debe poder **abrir un fichero `.prjcl`**, rechazando los `.prj` con un mensaje que indica usar el Modo Cliente. |
| **RF-015** | El sistema debe poder **guardar, guardar como y crear nuevo** `.prjcl`. |

### 3.3 Módulo C — Tablas del modo (`ClientBuilder`, `ClienteTableModel`, `ProyectoClienteTableModel`, `ClientTableConfig`)

| ID | Requisito |
|----|-----------|
| **RF-016** | El modo debe presentar **cuatro tablas**: Selección y Descartes del Proyecto (tablas de proyecto) y Selección y Descartes del Cliente (tablas de cliente). |
| **RF-017** | La tabla de proyecto debe mostrar **código de catálogo** y **nombre** de archivo, con ordenación por columnas. |
| **RF-018** | La tabla de cliente debe mostrar columnas: **colapsar (collapse)**, **estado**, **código de imagen**, **código de checkbox**, **PVP** y **mensaje**. |
| **RF-019** | La tabla de cliente debe presentar **filas padre** (imagen) y **filas hija** (checkboxes internos), manteniéndolas juntas al ordenar. |
| **RF-020** | El sistema debe permitir **plegar/desplegar los checkboxes** de cada imagen (collapso `NONE`, `SMART`, `FULL`) pulsando en la columna de colapso. |
| **RF-021** | El **estado tristate** debe editarse con un editor que cicla `UNDEFINED → SELECTED → DISCARDED → UNDEFINED`. |
| **RF-022** | Al editar el estado o el precio de una **imagen padre**, el sistema debe **propagar la cascada a todos sus checkboxes**; al editar una hija, debe **recalcular el estado derivado** de la imagen. |
| **RF-023** | El **PVP** debe editarse en la columna correspondiente (formato `%.2f`) y persistirse. |
| **RF-024** | La **ordenación** de la tabla de cliente debe agrupar imágenes con sus checkboxes y soportar orden por nombre, estado y peso de mensajes. |
| **RF-025** | El **doble clic en la columna de mensaje** debe abrir el diálogo de mensajes (`MsgPopupDialog`) de la imagen o checkbox. |
| **RF-026** | La **selección de fila** (clic, teclado o rueda) debe **navegar la imagen** correspondiente en el visor. |
| **RF-027** | El menú contextual de las tablas debe ofrecer: mover a descartes/selección, mover a descartes del proyecto, borrar imagen y localizar archivo. |

### 3.4 Módulo D — Editor de checkboxes (`CheckboxEditorPanel`, `CheckboxEditorMouseHandler`)

| ID | Requisito |
|----|-----------|
| **RF-028** | El sistema debe ofrecer un **editor de checkboxes** superpuestos sobre la imagen (`panel.cliente.editor`), activable con la acción de edición. |
| **RF-029** | El editor debe mostrar la **imagen con zoom** ajustable por rueda del ratón (factor 0.1–10) y permitir el **paneo** arrastrando el fondo. |
| **RF-030** | El sistema debe permitir **añadir un checkbox** en una posición de la imagen (código correlativo `cb01`, `cb02`, …, estado `UNDEFINED`). |
| **RF-031** | El **clic sobre un checkbox** debe **ciclar su estado tristate** y recalcular el estado derivado de la imagen. |
| **RF-032** | El sistema debe permitir **arrastrar un checkbox** para reposicionarlo; las coordenadas se almacenan **relativas a la imagen original** (transformadas con `ImageDisplayPanel.getCurrentImageTransform`). |
| **RF-033** | El sistema debe permitir **arrastrar un comentario overlay** para reposicionarlo. |
| **RF-034** | El sistema debe permitir **asignar un PVP** a un checkbox mediante un diálogo. |
| **RF-035** | El sistema debe permitir **añadir y editar comentarios globales** de la imagen y **gestionar mensajes** de un checkbox (diálogo de mensajes). |
| **RF-036** | Al terminar la edición, el sistema debe **actualizar las cuatro tablas** y notificar el cambio al proyecto. |

### 3.5 Módulo E — Estado tristate y derivación (`SelectionState`, `ProjectModel`, `ClientController`)

| ID | Requisito |
|----|-----------|
| **RF-037** | El estado de una imagen o checkbox debe ser uno de `SELECTED`, `DISCARDED`, `UNDEFINED`. |
| **RF-038** | El estado derivado de una imagen se calcula de sus checkboxes: **al menos un `SELECTED` → `SELECTED`**; ningún `SELECTED` y al menos un `UNDEFINED` → `UNDEFINED`; todos `DISCARDED` → `DISCARDED`. |
| **RF-039** | El sistema debe **refrescar el estado derivado** tras editar un checkbox o importar una respuesta. |
| **RF-040** | El sistema debe **visualizar los checkboxes sobre la imagen** en el visor (`clienteCheckboxVisible`) y ocultarlos cuando se activa el editor. |

### 3.6 Módulo F — Catálogo HTML único (`WebCatalogExporter`)

| ID | Requisito |
|----|-----------|
| **RF-041** | El sistema debe exportar un **único archivo HTML autocontenido** con miniaturas en base64 y CSS/JS inline. |
| **RF-042** | La **calidad de las miniaturas** debe ser **adaptativa**: ≤20 imágenes → 800 px / 85 %, ≤50 → 600 px / 75 %, >50 → 400 px / 60 %. |
| **RF-043** | El HTML debe **incrustar los datos** en `CATALOG_DATA` (id, código, nombre, estado, precio, comentario, dimensiones, miniatura, comentario overlay y checkboxes) y una **galería estática** de tarjetas prerenderizadas. |
| **RF-044** | Cada **tarjeta** debe mostrar: indicador tristate, código, nombre, precio, icono de mensaje y último comentario con etiqueta `TALLER:`/`TÚ:`. |
| **RF-045** | El **JS de la galería** debe **ciclar el estado tristate** (`SELECTED → DISCARDED → UNDEFINED`) al pulsar la tarjeta, propagando el estado a sus checkboxes, sin abrir el modal. |
| **RF-046** | El **modal** debe mostrar la imagen con **zoom, pan y pinch** (escala 0.3–6, anclada al cursor), con los **overlays posicionados** sobre la imagen según coordenadas relativas. |
| **RF-047** | El modal debe incluir un **chat lateral** para enviar comentarios (hilo con `de: 'cliente'`) y un **checkbox principal tristate** que afecta a la imagen. |
| **RF-048** | El sistema debe exponer en el JS la función **`buildResponse()`** que genera la respuesta con `respuestas[]`, cada una con `id`, `codigo`, `estado`, `comentario` y `checkboxes[]` (`codigo`, `estado`, `comentario`). |
| **RF-049** | El HTML debe ofrecer botones de **compartir** (Web Share API con fallback a copiar) y **descargar** el JSON de respuesta con el nombre `respuesta_<proyecto>_iteracion<N>.json`. |
| **RF-050** | El nombre de archivo de salida debe ser `<proyecto>_iteracion<N>.html` y el usuario debe poder elegir destino (filtro `*.html`, confirmación de sobrescritura). |
| **RF-051** | Si se emplearan text blocks con `String.formatted()` para el JS, el operador **módulo `%` debe escribirse como `%%`** (gotcha documentado; la implementación actual usa concatenación y no requiere el escape). |

### 3.7 Módulo G — Importación de la respuesta (`ClientResponseImporter`)

| ID | Requisito |
|----|-----------|
| **RF-052** | El sistema debe **importar la respuesta del cliente** desde un fichero JSON/TXT (`import`) o desde un `String` (`importarDesdeString`). |
| **RF-053** | El formato esperado debe tener el campo **`respuestas`** (obligatorio) y opcionalmente `iteracion` y `fechaRespuesta`. |
| **RF-054** | Cada respuesta debe localizarse **por código de catálogo** (`codigo`) en la lista maestra; las no localizadas se **cuentan como ignoradas**. |
| **RF-055** | Antes de sobrescribir, el sistema debe **preservar el estado previo** en `estadoClienteOriginal` y actualizar `estadoCliente`. |
| **RF-056** | La importación debe **actualizar los comentarios e hilos** de imagen y de cada checkbox, y el estado derivado de la imagen. |
| **RF-057** | El sistema debe **actualizar la fecha de respuesta** (`fechaRespuesta`) y **la iteración** (`iteracion + 1`) del proyecto. |
| **RF-058** | La importación debe devolver un **informe** con los contadores de seleccionadas, descartadas, indefinidas e ignoradas. |

### 3.8 Módulo H — Catálogo web y presupuesto PDF (`WebCatalogExporter`, `PDFGeneratorService`)

| ID | Requisito |
|----|-----------|
| **RF-059** | El sistema debe permitir **exportar el catálogo web** a una carpeta (`exportarParaCliente`), generando las miniaturas con diálogo de progreso. |
| **RF-060** | El sistema debe generar el **presupuesto PDF** (`crearPresupuestoCliente`) con las imágenes `SELECTED`, en un grid **2×2** (A4). |
| **RF-061** | Cada celda del presupuesto debe mostrar **código + nombre**, **PVP**, la **imagen** escalada y los **checkboxes** con su estado (`V` / `X` / `—`). |
| **RF-062** | El presupuesto debe incluir una **página de resumen** con el logo, la fecha, los datos (Código/Nombre/PVP), el **total** y las **notas del proyecto**. |

### 3.9 Módulo I — Mensajes y comentarios (`CommentThread`, `Mensaje`, `CommentOverlay`, `MsgPopupDialog`)

| ID | Requisito |
|----|-----------|
| **RF-063** | Cada imagen y cada checkbox debe poder tener un **hilo de mensajes** (`CommentThread`) entre el taller (`nosotros`) y el cliente (`cliente`). |
| **RF-064** | Cada mensaje debe registrar **autor**, **texto**, **iteración** y **flag de leído** (`Mensaje` record). |
| **RF-065** | El hilo debe mantener un **estado** (`estadoPr`/`estadoHtml`: `0` ninguno, `1` leído, `2` contestado, `3` nuevo) que alimenta los iconos de las tablas y el HTML. |
| **RF-066** | El sistema debe permitir **borrar mensajes propios** solo si no están compartidos (el cliente o lo ya compartido no se elimina). |
| **RF-067** | El sistema debe permitir **comentarios superpuestos** (`CommentOverlay`) con posición y texto sobre la imagen, visibles en el editor y en el HTML. |
| **RF-068** | El diálogo de mensajes debe mostrar el hilo y permitir **añadir y borrar** mensajes del taller. |

### 3.10 Módulo J — Cierre y sincronización (`ClientController`, `ClientSyncService`)

| ID | Requisito |
|----|-----------|
| **RF-069** | El sistema debe ofrecer la acción **«Cerrar y sincronizar»** (`cerrarCliente`), con confirmación previa que avisa de que las imágenes `UNDEFINED` se tratarán como descartadas. |
| **RF-070** | Antes del cierre, el sistema debe **guardar un backup** `.prj` con sufijo `_precierre_<fecha>.prj`. |
| **RF-071** | Al cerrar, el sistema debe **convertir `UNDEFINED → DISCARDED`** en imagen y checkboxes. |
| **RF-072** | Al cerrar, el sistema debe **sincronizar la selección del proyecto** (`closeAndSync`): una imagen se conserva en la selección solo si tiene algún checkbox `SELECTED` o su estado es `SELECTED`. |
| **RF-073** | Al cerrar, el sistema debe fijar `clientModeClosed = true`, **ocultar overlays y editor**, y **guardar el proyecto**. |
| **RF-074** | El sistema debe **deshabilitar** al cerrar las acciones de revisión: cargar respuesta, exportar web/HTML, cerrar y sincronizar, editar y ocultar descartes. |
| **RF-075** | El sistema debe representar **conflictos proyecto vs cliente** mediante `ConflictEntry` (estado de proyecto, estado de cliente y estado resuelto) para su posterior resolución. |

### 3.11 Módulo K — Comandos, acciones y barra de estado (`AppActionCommands`, `ActionFactory`, `ClientBuilder`)

| ID | Requisito |
|----|-----------|
| **RF-076** | El sistema debe exponer **comandos canónicos** del modo: abrir/guardar `.prjcl`, actualizar, exportar web, exportar HTML, cargar respuesta, cerrar y sincronizar, añadir checkbox (y con etiqueta), fin de edición, vista visor/editor, editar, exportar PDF y ocultar descartes. |
| **RF-077** | El sistema debe mostrar una **barra de estado** con la iteración y los **contadores** de `SELECTED` (✓), `UNDEFINED` (○) y `DISCARDED` (✗). |
| **RF-078** | El sistema debe mostrar un **aviso visual** (`⚠ DESCARTES OCULTOS EN EXPORTACIÓN ⚠`) cuando la opción de ocultar descartes está activa. |
| **RF-079** | El sistema debe **ocultar los descartes en las exportaciones** cuando la opción esté activa (`incluirDescartes = !ocultarDescartes`), sin ocultarlos en la tabla. |
| **RF-080** | Las acciones no implementadas aún (añadir checkbox por teclado, fin de edición, actualizar proyecto) deben exponerse como **funcionalidad pendiente** deshabilitada. |

---

## 4. Casos de uso

### CU-01 Compartir el proyecto con el cliente

1. El usuario entra en el Modo Proyecto y marca las imágenes deseadas.
2. Pulsa «Compartir con el cliente» (`CMD_PROYECTO_COMPARTIR_CLIENTE`).
3. El sistema valida que haya selección y asigna códigos correlativos.
4. Valida las asignaciones; si todo es correcto, fija el estado compartido, guarda `.prjcl` y
   cambia al Modo Cliente.

### CU-02 Entrar en el Modo Cliente

1. El usuario pulsa el botón «Cliente».
2. El sistema pre-valida el proyecto (bloquea si hay imágenes sin código; sugiere abrir `.prj` o
   compartir si procede).
3. El sistema reparenta el visor, fuerza imagen única y muestra las cuatro tablas.

### CU-03 Revisar el catálogo

1. El usuario navega por las tablas de cliente; al seleccionar una fila, la imagen se muestra en
   el visor con sus checkboxes superpuestos.
2. Pulsa sobre un checkbox para ciclar su estado; el estado derivado de la imagen se recalcula.
3. Los contadores de la barra de estado se actualizan.

### CU-04 Editar la selección

1. El usuario pulsa «Editar selección» y confirma la activación del modo edición.
2. Abre el editor de checkboxes: añade checkboxes, los arrastra, asigna precios y añade
   comentarios superpuestos.
3. Al terminar, actualiza las tablas y desactiva la edición.

### CU-05 Exportar el catálogo HTML único

1. El usuario pulsa «Exportar HTML único» (`CMD_CLIENTE_EXPORTAR_HTML`).
2. Indica la iteración y el destino; el sistema genera el HTML con calidad adaptativa.
3. El cliente recibe el archivo y puede marcar las imágenes (tristate) y responder.

### CU-06 Importar la respuesta del cliente

1. El usuario pulsa «Cargar respuesta» (`CMD_CLIENTE_CARGAR_RESPUESTA`).
2. Selecciona el JSON devuelto por el cliente.
3. El sistema localiza cada imagen por código, actualiza estados, comentarios y checkboxes, y
   muestra el informe de la importación.

### CU-07 Generar el presupuesto PDF

1. El usuario pulsa «Crear PDF» (`CMD_CLIENTE_EXPORTAR_PDF`).
2. El sistema filtra las imágenes `SELECTED` y genera el presupuesto con grid 2×2 y página de
   resumen con total.
3. Guarda el PDF en el destino elegido.

### CU-08 Cerrar y sincronizar

1. El usuario pulsa «Cerrar y sincronizar» (`CMD_CLIENTE_CERRAR_SINCRONIZAR`).
2. Confirma que las imágenes sin marcar se tratarán como descartadas.
3. El sistema hace backup, convierte `UNDEFINED → DISCARDED`, sincroniza la selección del
   proyecto, cierra la revisión y guarda.

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNF-001** | La exportación del catálogo HTML y web debe ejecutarse en **segundo plano** (SwingWorker) con diálogo de progreso. |
| **RNF-002** | Las miniaturas del HTML deben generarse con **calidad adaptativa** al volumen para acotar el tamaño del archivo. |
| **RNF-003** | El renderizado de las tablas debe **colapsar los checkboxes** por defecto para evitar filas innecesarias. |
| **RNF-004** | La carga de la respuesta del cliente no debe bloquear la EDT (procesamiento ligero sobre la lista maestra). |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNF-005** | Los estados tristate deben representarse con **símbolos y colores** (✓ verde, ✗ rojo, ○ gris) y tooltips. |
| **RNF-006** | La navegación por las tablas debe poder realizarse con **teclado y rueda del ratón**. |
| **RNF-007** | El editor de checkboxes debe permitir **arrastre directo** y **zoom con la rueda**. |
| **RNF-008** | Las operaciones que cambian la selección o cierran la revisión deben **pedir confirmación**. |
| **RNF-009** | El catálogo HTML debe ser **usable en móvil** (viewport, pinch y zoom). |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNF-010** | La importación de la respuesta debe **preservar el estado previo** (`estadoClienteOriginal`) y no romper con respuestas parciales (ignora imágenes no localizadas). |
| **RNF-011** | El **cierre de la revisión debe crear un backup** antes de convertir estados. |
| **RNF-012** | El borrado de mensajes debe respetar la **compartición**: no se eliminan mensajes del cliente ni los ya compartidos. |
| **RNF-013** | El HTML generado debe **escapar correctamente** los nombres y comentarios (XSS) y los datos JSON. |
| **RNF-014** | La entrada al modo debe **bloquearse** si el proyecto no está en un estado coherente para revisión. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNF-015** | El código debe seguir el patrón MVC plano del proyecto. |
| **RNF-016** | La exportación HTML, la importación de respuesta y la sincronización deben residir en **servicios** (`servicios/cliente`). |
| **RNF-017** | Los comandos y claves de configuración deben centralizarse en `AppActionCommands` y `ConfigKeys`. |
| **RNF-018** | El modelo del cliente debe persistir en el **`ProjectModel`** (lista maestra), versionado por esquema. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNF-019** | El catálogo HTML debe funcionar sin dependencias externas (un solo archivo). |
| **RNF-020** | La generación de PDF debe **deshabilitar el desmapeo de memoria de PDFBox** en Windows. |
| **RNF-021** | El JSON de respuesta debe ser **estable** y reutilizable entre iteraciones (búsqueda por código). |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNF-022** | No deben registrarse credenciales ni datos sensibles en los logs. |
| **RNF-023** | Los datos incrustados en el HTML deben **escaparse** para evitar inyección de scripts. |
| **RNF-024** | Las decisiones de estado del cliente deben reflejarse en el proyecto **solo mediante la sincronización explícita** de cierre. |

---

## 6. Modelo de datos

### 6.1 Modelos de dominio (`modelo/proyecto`)

| Clase | Campos principales | Descripción |
|-------|--------------------|-------------|
| `SelectionState` | enum `SELECTED`, `DISCARDED`, `UNDEFINED` | Estado tristate de la revisión del cliente. |
| `ProjectImage` | `rutaImagen`, `enSeleccionProyecto`, `etiqueta`, `price`, `estadoCliente`, `estadoClienteOriginal`, `codigoCatalogo`, `exportConfig`, `checkboxes`, `commentOverlay`, `comment`, `commentThread` | Imagen de la lista maestra con su estado de cliente. |
| `ImageCheckboxOverlay` | `imageX`, `imageY`, `state`, `label`, `checkboxCode`, `comment`, `commentThread`, `price`, `size` | Checkbox superpuesto con posición relativa a la imagen original. |
| `CommentOverlay` | `imageX`, `imageY`, `text` | Comentario superpuesto con posición. |
| `CommentThread` | `estadoPr`, `estadoHtml`, `mensajes` | Hilo de mensajes de imagen o checkbox con estados para tabla/HTML. |
| `Mensaje` | record `(de, texto, iteracion, leido)` | Mensaje del hilo; `isCompartido(iter)` compara con la iteración compartida. |
| `ClientSelection` | (deprecado) `images`, `comments`, `commentOverlays`, `imageCheckboxes`, `clientNotes`, `fechaRespuesta` | Contenedor legado v1 de datos del cliente. |

### 6.2 Estado compartido del proyecto (`ProjectModel`)

| Campo | Descripción |
|-------|-------------|
| `sharedWithClient` | Indica que el proyecto está compartido con el cliente. |
| `clientModeClosed` | Indica que la revisión del cliente está cerrada. |
| `sharedTimestamp` | Marca de tiempo del último envío. |
| `sharedIteration` | Número de iteración actual. |
| `fechaRespuesta` | Fecha de la última respuesta importada. |
| `imageCodes` | Mapa clave→código de catálogo. |
| `masterImages` | Lista maestra (esquema v2) con el estado del cliente por imagen. |

### 6.3 Formato de respuesta del cliente

```json
{
  "projectName": "...",
  "iteracion": 1,
  "fechaEnvio": "...",
  "fechaRespuesta": "...",
  "respuestas": [
    {
      "id": "...",
      "codigo": "C001",
      "estado": "SELECTED",
      "comentario": { "estadoPr": 2, "estadoHtml": 3, "hilo": [ { "de": "cliente", "texto": "..." } ] },
      "checkboxes": [ { "codigo": "cb01", "estado": "UNDEFINED", "comentario": {} } ]
    }
  ]
}
```

### 6.4 Registros del `ComponentRegistry` (selección)

| Clave | Componente |
|-------|------------|
| `view.panel.cliente` | Panel raíz del modo. |
| `splitpane.cliente.main` | Split horizontal principal. |
| `panel.cliente.display` / `label.cliente.imagen` | Visor de imagen única. |
| `panel.display.grid.cliente` / `list.grid.cliente` | Grid compartido. |
| `panel.cliente.display.polaroid` / `label.cliente.polaroid.imagen` | Polaroid compartido. |
| `container.displaymodes.cliente` / `placeholder.display.cliente` | CardLayout de modos de display. |
| `container.displaymodes.cliente.wrapper` | CardLayout `DISPLAY_NORMAL` / `DISPLAY_EDITOR`. |
| `panel.cliente.editor` | `CheckboxEditorPanel`. |
| `table.cliente.proyecto.seleccion` / `.descartes` | Tablas de proyecto. |
| `table.cliente.cliente.seleccion` / `.descartes` | Tablas de cliente. |
| `panel.table.cliente.cliente.descartes` | Panel de aviso de descartes ocultos. |
| `label.cliente.estado.iteracion` / `.sel` / `.und` / `.dis` / `.info` | Barra de estado. |
| `dialog.cliente.editor` | Diálogo del editor (opcional). |
| `button.cliente.editar` | Toggle de edición de selección. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Controlador | `controlador/ClientController.java` |
| Ciclo de vida | `controlador/services/AppModeService.java`, `controlador/GeneralController.java` |
| Compartición | `controlador/services/proyecto/ExportPreflightService.java`, `servicios/ProjectManager.java` |
| UI | `vista/builders/ClientBuilder.java`, `vista/models/ClienteTableModel.java`, `vista/models/ProyectoClienteTableModel.java`, `vista/config/ClientTableConfig.java` |
| Renderers/editors | `vista/renderers/TristateCellRenderer.java`, `TristateCellEditor.java`, `CodeCellRenderer.java`, `CommentCellRenderer.java`, `ClientListCellRenderer.java` |
| Editor de checkboxes | `vista/panels/CheckboxEditorPanel.java`, `vista/panels/CheckboxEditorMouseHandler.java` |
| Exportación HTML/web | `servicios/cliente/WebCatalogExporter.java` |
| Importación | `servicios/cliente/ClientResponseImporter.java` |
| Sincronización | `servicios/cliente/ClientSyncService.java` |
| PDF | `modelo/export/pdf/PDFGeneratorService.java` |
| Modelo | `modelo/proyecto/SelectionState.java`, `ProjectModel.java`, `ProjectImage.java`, `ImageCheckboxOverlay.java`, `CommentOverlay.java`, `CommentThread.java`, `Mensaje.java` |
| Comandos/acciones | `controlador/commands/AppActionCommands.java`, `controlador/factory/ActionFactory.java`, `controlador/actions/cliente/*` |
| Ayuda | `resources/help/cliente.html` |

### 7.2 Apéndice B — Regla de derivación del estado de la imagen

| Condición (sobre los checkboxes de la imagen) | Estado derivado |
|-----------------------------------------------|-----------------|
| Al menos un `SELECTED` | `SELECTED` |
| Ningún `SELECTED` y al menos un `UNDEFINED` | `UNDEFINED` |
| Todos `DISCARDED` | `DISCARDED` |

### 7.3 Apéndice C — Glosario técnico

- **Tristate:** estado de tres valores (`SELECTED`, `DISCARDED`, `UNDEFINED`).
- **Iteración:** envío del catálogo; incrementada en cada compartición y tras importar respuesta.
- **`.prjcl`:** fichero de proyecto compartido con el cliente, usado en el Modo Cliente.
- **Overlay:** elemento superpuesto a la imagen (checkbox o comentario) con coordenadas relativas.
- **Hilo (thread):** secuencia de mensajes taller↔cliente con estados de lectura/contestación.

---

*Fin del documento.*