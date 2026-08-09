# Documento de Especificación de Requisitos de Software (SRS)

## Modo Editor del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo EDITOR + Editor Avanzado |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Editor** del
Visor de Imágenes V2 y del **Editor Avanzado** que lo sustenta. El Modo Editor es un modo de
trabajo dedicado (como Modo Proyecto o Modo Datos) que abre un editor de imágenes por capas a
pantalla completa y gestiona el trabajo como **documentos propios** con extensión `.edoc`
(guardado, cambios pendientes y recuperación automática).

El editor se usa en dos contextos con **documentos independientes**: como editor embebido dentro
del Modo Render (composición en memoria, sin archivo) y como Modo Editor (documento `.edoc`).
Este documento especifica ambos contextos y los requisitos comunes del editor.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia
para el cliente y de base para futuras evoluciones.

### 1.2 Alcance

El alcance cubre:

- El modo de trabajo EDITOR (`WorkMode.EDITOR`): activación, pantalla completa y orquestación.
- La gestión de **documento del editor** (`.edoc`): nuevo, abrir, guardar, guardar como,
  documento temporal, cambios pendientes y recuperación de sesión.
- Los **documentos independientes** RENDER / EDITOR y el portapapeles entre contextos.
- El **Editor Avanzado**: canvas, barra de herramientas, barra de opciones y panel de capas.
- Las **herramientas** de selección, pintura, recorte, texto y formas.
- La **selección de píxeles** con máscaras y booleanas.
- La **transformación** con gizmo y la rotación no destructiva.
- El **panel de capas** y sus operaciones.
- La **disposición** (alinear, distribuir, espaciar) y el sistema **Auto Layout**.
- Las **Smart Guides** de ajuste durante el arrastre.
- El **historial de deshacer/rehacer** (50 pasos).
- La **configuración** del subsistema (`editor.*`) y los atajos de teclado.

Queda **fuera del alcance** de este documento:

- El Modo RENDER (escaneo, renderizado 3D y asignación de previews), especificado en
  `SRS-ModoRender.md`.
- El resto de modos de trabajo, especificados en sus SRS modulares.

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **Modo Editor** | Modo de trabajo dedicado (`WorkMode.EDITOR`) que abre el editor a pantalla completa con un documento `.edoc`. |
| **Editor Avanzado** | Editor de imágenes por capas (canvas, herramientas, capas, texto, formas, transformación). |
| **Documento `.edoc`** | Archivo JSON (Gson) que guarda lienzo y capas; se acompaña de una carpeta `<archivo>_capas/` con los PNG de las capas sin origen externo. |
| **Capa** | Elemento apilado sobre el lienzo: imagen (`ImageLayer`), texto (`TextLayer`) o forma. |
| **Lienzo** | Área de trabajo (`CanvasModel`): dimensiones, color de fondo y transparencia. |
| **Gizmo** | Conjunto de tiradores visuales (`TransformGizmo`) para mover, escalar y rotar capas. |
| **Máscara** | Forma real e irregular de la zona seleccionada (selección de píxeles). |
| **Smart Guides** | Guías inteligentes de ajuste al mover capas (bordes y centros). |
| **Auto Layout** | Sistema de composición automática de las capas seleccionadas (13 herramientas). |
| **TextRun** | Fragmento de texto con formato propio (fuente, color, subrayado, tachado). |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |

### 1.4 Referencias

- `resources/help/modo_editor.html` — Ayuda del Modo Editor.
- `resources/help/editor/*.html` — Ayudas del Editor Avanzado (visión general, herramientas,
  selección, texto, edición/transformar, disposición, smart guides, capas).
- `docs/SRS-ModoRender.md` — Especificación del Modo Render (integración del editor embebido).
- `docs/SRS-General.md` — SRS raíz con el mapa de trazabilidad.
- `AGENTS.md` — Guía general del proyecto.

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 presenta una descripción general del producto;
la sección 3 enumera los requisitos funcionales agrupados por módulos; la sección 4 describe los
casos de uso principales; la sección 5 especifica los requisitos no funcionales; la sección 6
describe el modelo de datos; y la sección 7 incluye apéndices con trazabilidad al código fuente.

---

## 2. Descripción general

### 2.1 Perspectiva del producto

El Modo Editor es uno de los modos de trabajo del Visor de Imágenes V2. El acceso se realiza
desde la barra de modos principal (botón **Modo Editor**, atajo **Ctrl+7**). Internamente el modo
se identifica como `WorkMode.EDITOR`.

El modo se integra con el resto del sistema mediante:

- `AppModeService` — gestión del cambio de modo, entrada/salida y sincronización de botones.
- `GeneralController` — validación previa (recuperación de documento) antes de entrar en el modo.
- `RenderController` — activación/desactivación del modo y gestión del documento del editor.
- `EditorDocumentManager` — ciclo de vida del documento `.edoc` (espejo de `ProjectManager`).
- `ActionFactory` / `AppActionCommands` — acciones del editor (`CMD_EDITOR_*`, `CMD_ADVANCED_EDITOR_*`).
- `ComponentRegistry` — registro de los paneles del editor.
- `ConfigurationManager` / `ConfigKeys` — parámetros de configuración (`editor.*`).

El modo comparte la vista `VISTA_RENDER` con el Modo Render: al entrar, el sistema carga el
documento del editor en el panel (swap del slot RENDER al slot EDITOR) y activa el **fullscreen**
del editor, ocultando los paneles laterales del Modo Render (previsión 3D y archivos). Al salir,
se restauran los paneles.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Editar composiciones por capas** sobre un lienzo con zoom y paneo.
2. **Trabajar con documentos `.edoc`**: nuevo, abrir, guardar, guardar como y temporal.
3. **Recuperar** el documento no guardado al cerrar la aplicación o al entrar en el modo.
4. **Mantener documentos independientes** RENDER y EDITOR con el portapapeles como puente.
5. **Seleccionar píxeles** con máscaras (marco, capa, varita) y booleanas (unión, intersección, resta).
6. **Pintar** (bote de pintura, degradado) y **capturar colores** (cuentagotas).
7. **Recortar** a nueva capa o eliminando la selección.
8. **Añadir y editar texto** con formato por carácter (rich text).
9. **Crear formas** (rectángulo, elipse, triángulo, línea, flecha).
10. **Transformar** capas con el gizmo (mover, escalar, rotar de forma no destructiva).
11. **Gestionar capas** (panel de capas: añadir, duplicar, combinar, borrar, reordenar, opacidad).
12. **Disponer la composición** con alinear, distribuir, espaciar y Auto Layout.
13. **Asistir el arrastre** con Smart Guides.
14. **Deshacer/rehacer** hasta 50 pasos con historial navegable.
15. **Configurar** el subsistema desde Configuración Avanzada.

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Compone y retoca renders de modelos 3D; necesita capas, texto y disposición rápida de elementos. |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; interfaz Swing + FlatLaf.
- **Interfaz de usuario:** Swing; el canvas es un panel personalizado con `Graphics2D`.
- **Almacenamiento de documentos:** carpeta base configurable (por defecto `~/.miVisorImagenesApp/.editor_docs`).

### 2.5 Restricciones de diseño e implementación

- El documento `.edoc` es un **JSON con Gson** (`disableHtmlEscaping`, pretty-printing).
- Las capas **sin origen externo** (pintadas, degradados, fusiones, formas) se exportan como PNG
  en la carpeta sidecar `<archivo>_capas/` junto al archivo.
- El historial guarda **snapshots inmutables** del documento (capas con imagen clonada en
  profundidad), con un máximo de **50 pasos**.
- La **rotación** de capas es no destructiva: se almacena como ángulo y se aplica al pintar.
- El lienzo por defecto es **1920×1080** con fondo blanco y sin transparencia.

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/modo_editor.html` y las 8 subpáginas de
  `resources/help/editor/`.

### 2.7 Suposiciones y dependencias

- El portapapeles del sistema admite imágenes; sirve de puente entre el documento RENDER y el
  EDITOR y con aplicaciones externas.
- La recuperación de sesión usa el archivo `editor_recuperacion.edoc` y la clave de configuración
  `editor.estado.recuperacion_pendiente`.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de clase
entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Integración de modo (`AppModeService`, `GeneralController`, `RenderController`)

| ID | Requisito |
|----|-----------|
| **RF-001** | El botón **Modo Editor** de la barra de modos debe activar el `WorkMode.EDITOR`; la barra muestra el orden Visualizador (Ctrl+1), Proyecto (Ctrl+2), Cliente (Ctrl+3), Datos (Ctrl+4), Carrusel (Ctrl+5), Render (Ctrl+6), Editor (Ctrl+7). |
| **RF-002** | El atajo **Ctrl+7** debe activar el Modo Editor desde cualquier contexto (binding global `WHEN_IN_FOCUSED_WINDOW`). |
| **RF-003** | Al entrar en el modo, el sistema debe inicializar el editor, cargar el documento del editor en el panel (swap del slot RENDER al slot EDITOR), activar el editor avanzado y el **fullscreen** (ocultando los paneles laterales de preview y archivos). |
| **RF-004** | Al salir del modo, el sistema debe restaurar el estado previo del panel y los paneles laterales, sin tocar el documento del editor. |
| **RF-005** | El modo debe compartir la vista `VISTA_RENDER` con el Modo Render en el `CardLayout` maestro. |
| **RF-006** | El cambio de modo debe mantener el trabajo del editor **en memoria** (misma semántica que el Modo Proyecto). |
| **RF-007** | La activación del editor avanzado dentro del Modo Render (toggle de la barra de previsualización) **no** debe desactivar ni tocar el documento del Modo Editor cuando este esté cargado en el panel. |

### 3.2 Módulo B — Documento del editor (`.edoc`) (`EditorDocumentManager`, `EditorDoc`)

| ID | Requisito |
|----|-----------|
| **RF-008** | El editor debe organizar el trabajo como un **documento** con lienzo y capas, persistible en un archivo `.edoc` (JSON con Gson). |
| **RF-009** | Un documento **sin guardar** debe tratarse como temporal (`editor_temporal.edoc`). |
| **RF-010** | El documento debe conservar el **nombre**, el **flag de cambios sin guardar** (sucio) y la **ruta del archivo activo**. |
| **RF-011** | Ante cualquier modificación del lienzo o de las capas, el documento debe marcarse como **con cambios pendientes** y el título de la ventana debe mostrar un **asterisco (\*)** delante del nombre. |
| **RF-012** | La acción **Nuevo documento** debe restablecer el lienzo por defecto (1920×1080, blanco, sin transparencia) y vaciar las capas, reiniciando el historial. |
| **RF-013** | La acción **Abrir documento** debe cargar un `.edoc` existente y aplicarlo a los modelos activos (lienzo y capas). |
| **RF-014** | La acción **Guardar** debe escribir en el archivo activo o, si no hay, en el temporal; al guardar, debe limpiarse el flag de cambios. |
| **RF-015** | La acción **Guardar como** debe permitir elegir nombre y ruta, fijarla como archivo activo y guardar. |
| **RF-016** | Al guardar, las capas **con origen externo** deben referenciarse por su ruta; las capas **sin origen** (pintadas, degradados, fusiones, formas) deben exportarse a PNG en la carpeta sidecar `<archivo>_capas/`. |
| **RF-017** | El esquema del documento debe versionarse (`EditorDoc.VERSION_ACTUAL = 1`) e incluir el lienzo (ancho, alto, fondo ARGB, transparencia) y la lista de capas con sus transformaciones y metadatos de forma/texto. |
| **RF-018** | El documento debe admitir capas de tipo **IMAGE**, **SHAPE** y **TEXT**, con estado común (bounds, rotación, visibilidad, bloqueo, opacidad) y metadatos específicos por tipo. |
| **RF-019** | El texto debe persistirse como una lista de **TextRun** (fuente, color, subrayado, tachado y fragmento de texto) para conservar el formato por carácter. |
| **RF-020** | La **barra de documento** del Modo Editor debe ofrecer: Nuevo, Abrir, Guardar, Guardar como y **Pegar imagen (Ctrl+V)**. |
| **RF-021** | La carpeta base de documentos y el nombre del archivo temporal deben ser configurables (`editor.carpeta_base`, `editor.archivo_temporal_nombre`). |

### 3.3 Módulo C — Documentos independientes RENDER / EDITOR y portapapeles

| ID | Requisito |
|----|-----------|
| **RF-022** | El editor debe mantener **dos documentos separados** en memoria: el documento RENDER (composición del editor embebido del Modo Render, sin archivo) y el documento EDITOR (del Modo Editor, con su gestión `.edoc`). |
| **RF-023** | Al cambiar entre el Modo Render y el Modo Editor, cada contexto debe **conservar intacto su documento** aunque se use el mismo editor. |
| **RF-024** | Las modificaciones sobre el documento RENDER **no** deben ensuciar el documento EDITOR, y viceversa. |
| **RF-025** | El sistema debe usar el **portapapeles del sistema** (imágenes) para mover contenido entre documentos: copiar la imagen del contexto activo y pegarla como nueva capa centrada en el lienzo. |
| **RF-026** | El **copiar imagen** debe capturar según el contexto: en Visualizador/Proyecto/Cliente la imagen seleccionada; en Render lo que se ve en pantalla (si el editor está activo, aplana las capas visibles); en Editor la composición visible aplanada. |
| **RF-027** | El **pegar imagen** (Ctrl+V o botón Pegar) debe crear una capa nueva centrada en el lienzo, activa y lista para mover o redimensionar. |
| **RF-028** | El portapapeles debe permitir también pegar capturas de aplicaciones externas (Paint, WhatsApp, navegador...). |

### 3.4 Módulo D — Recuperación y cierre

| ID | Requisito |
|----|-----------|
| **RF-029** | Al **cerrar la aplicación** con el documento marcado con `*`, el sistema debe mostrar el aviso **Guardar / No Guardar / Cancelar**. |
| **RF-030** | **Guardar** debe guardar el documento (preguntando la ruta si no tiene nombre) y, solo si queda limpio, permitir el cierre. |
| **RF-031** | **No Guardar** debe persistir una **sesión de recuperación** automática (`editor_recuperacion.edoc`) y registrar la clave `editor.estado.recuperacion_pendiente`. |
| **RF-032** | **Cancelar** debe abortar el cierre y conservar la edición. |
| **RF-033** | Al **salir del Modo Editor**, si el documento tiene cambios sin guardar y **no tiene archivo activo**, debe guardarse automáticamente en el temporal `editor_temporal.edoc` para no perderlo. |
| **RF-034** | Al **entrar en el Modo Editor**, si existe una sesión de recuperación pendiente, el sistema debe preguntar **Cargar Documento / Descartar / Cancelar**. |
| **RF-035** | **Cargar** debe restaurar el documento desde la recuperación, conservando el nombre original, marcándolo como **recuperado** (sin archivo activo, con cambios sin guardar) y eliminando la sesión de recuperación. |
| **RF-036** | **Descartar** debe eliminar la sesión de recuperación y empezar con un lienzo limpio. |
| **RF-037** | **Cancelar** debe impedir la entrada en el Modo Editor. |
| **RF-038** | La clave de recuperación pendiente debe limpiarse de la configuración antes de mostrar el aviso, y persistirse de nuevo solo si el usuario elige "No Guardar". |

### 3.5 Módulo E — Editor: lienzo, panel y barra de herramientas (`AdvanceEditPanel`, `CanvasPanel`, `EditorComponentBar`)

| ID | Requisito |
|----|-----------|
| **RF-039** | El editor debe presentar un **canvas** central donde se apilan las capas sobre el lienzo, con zoom y paneo. |
| **RF-040** | El **zoom** debe controlarse con la rueda del ratón, centrando el zoom en la posición del cursor. |
| **RF-041** | El **paneo** debe realizarse con el **botón central** del ratón desde cualquier punto; el área oscura fuera del lienzo también paneea con el botón izquierdo si no hay una capa bajo el cursor. |
| **RF-042** | Las herramientas solo deben actuar **dentro del lienzo**; la zona oscura fuera de él paneea. |
| **RF-043** | El editor debe presentar una **barra de herramientas izquierda** con `JToggleButton` agrupados (solo uno activo a la vez): Edición, Transformar, Selección por marco, Selección por capa, Varita, Recortar, Cuentagotas, Bote de pintura, Degradado, Texto, Formas y Pantalla completa. |
| **RF-044** | El editor debe presentar **controles de color** estilo Photoshop (color frontal y de fondo) con botones de **intercambiar** y **restablecer** colores (frontal blanco, fondo negro por defecto). |
| **RF-045** | El color frontal debe usarse para el bote de pintura, el relleno de formas, el color del texto y el inicio del degradado; el de fondo, para el final del degradado. |
| **RF-046** | La **barra de opciones superior** (`EditorComponentBar`) debe dividirse en dos partes: la **Parte A** con el selector de herramienta, spinners de posición/tamaño/ángulo, historial, inversión de selección de capas/píxeles, checkboxes de opciones (auto-selección, tiradores, proporción, auto-zoom) y combos Alinear/Distribuir; y la **Parte B** con el panel contextual de la herramienta activa. |
| **RF-047** | La **Parte B** debe mostrar opciones específicas por herramienta: auto-selección/tiradores/proporción (Edición), sub-herramientas y proporción (Transformar), desvanecido (Selección), tolerancia y contiguo (Varita), mantener original (Recortar), muestra de color (Cuentagotas), tolerancia (Bote), colores/tipo/opacidad (Degradado), texto (fuentes, estilos, alineación, ángulo) y relleno/borde/grosor (Formas). |
| **RF-048** | El editor debe admitir un **modo Home** inicial con dos pestañas excluyentes: **Lienzo** (nuevo lienzo con ancho, alto, color de fondo y transparencia configurables) e **Imagen** (carga la imagen seleccionada en el visor principal como capa base, manteniendo sus dimensiones). |
| **RF-049** | El editor debe poder entrar en **pantalla completa** y volver al modo normal (botón o tecla **F**); **Esc** nunca debe salir de pantalla completa, solo cancelar la herramienta activa. |
| **RF-050** | El editor debe poder cambiar el **tamaño de los iconos** de la barra de herramientas para adaptarse a la densidad de la interfaz. |

### 3.6 Módulo F — Herramientas de edición (tools de `controlador.tools`)

| ID | Requisito |
|----|-----------|
| **RF-051** | **Edición:** herramienta por defecto. Un clic sobre una capa la activa (si la auto-selección está activa), un clic en vacío deselecciona, el arrastre en zona vacía dibuja un **marco de selección** de capas (Shift añade), y el arrastre mueve las capas seleccionadas. Con "Tiradores" activo muestra el gizmo. |
| **RF-052** | **Transformar:** muestra el gizmo (mover, escalar, rotar); en zona vacía abre un marco de selección, sobre una capa la selecciona (Ctrl alterna, Shift añade) y sobre un tirador transforma **todas las capas seleccionadas juntas**. |
| **RF-053** | **Selección por marco:** define una selección rectangular; **Shift** añade (unión), **Shift+Alt** intersecta y **Alt** resta. El desvanecido (feather) debe ser configurable (0-50 px). |
| **RF-054** | **Selección por capa:** crea una selección ajustada a la forma real del contenido no transparente de la capa; Shift añade. |
| **RF-055** | **Varita mágica:** selecciona el área de color similar con máscara irregular; tolerancia configurable (0-255) y opción **Contiguo**; Shift+clic añade a la selección. |
| **RF-056** | **Recortar:** define el área a conservar; al soltar crea una nueva capa con el recorte. Debe ofrecer "Recortar a nueva capa" y "Recortar eliminando selección", con opción "Mantener original". |
| **RF-057** | **Cuentagotas:** captura el color bajo el cursor como color frontal y lo muestra en el panel de opciones. |
| **RF-058** | **Bote de pintura:** rellena el área de color similar con el color frontal (tolerancia configurable); con una selección de píxeles activa, el relleno debe limitarse a su forma real. |
| **RF-059** | **Degradado:** crea una nueva capa con un degradado lineal definido por el arrastre entre el color de inicio y el de fin, con tipo y opacidad configurables. |
| **RF-060** | **Formas:** crea formas básicas (rectángulo, elipse, triángulo, línea, flecha) como nuevas capas de imagen, con relleno, borde y grosor configurables. |
| **RF-061** | La herramienta activa debe poder cambiarse con las **letras de acceso rápido** (E, V, M, L, W, C, I, G, D, T, U, Z, F), mostradas en un chivato sobre el icono. |
| **RF-062** | Las teclas de herramienta deben ignorarse mientras se escribe texto inline (el campo de edición captura el teclado). |
| **RF-063** | **Esc** debe cancelar la edición de texto inline, el arrastre en curso o deseleccionar la capa activa según el contexto. |

### 3.7 Módulo G — Selección de píxeles (`SelectionModel`)

| ID | Requisito |
|----|-----------|
| **RF-064** | Las herramientas de selección (marco, capa, varita) deben generar una **máscara** (la forma real e irregular de la zona seleccionada), no un simple rectángulo. |
| **RF-065** | Las selecciones deben combinarse con **booleanas**: Shift = unión, Shift+Alt = intersección, Alt = resta; deben funcionar incluso entre formas irregulares. |
| **RF-066** | El contorno de la selección debe mostrarse con **hormigueo** (dashes blancos y negros que marchan) y la zona seleccionada con relleno azul semitransparente. |
| **RF-067** | **Ctrl+A** debe seleccionar todo el lienzo y **Ctrl+D** deseleccionar. |
| **RF-068** | El botón **Invertir selección de píxeles** (y la acción correspondiente) debe invertir la máscara respecto a todo el lienzo. |
| **RF-069** | Con una selección de píxeles activa, **Supr / Retroceso** debe borrar solo el contenido seleccionado de la capa activa de imagen, dejando el resto intacto (estilo Photoshop). |
| **RF-070** | El gizmo con target **marco** (herramienta Transformar) debe mover y escalar la máscara junto con su forma, re-muestreando los píxeles seleccionados. |
| **RF-071** | El **recorte** debe respetar la forma irregular de la selección: "Recortar a nueva capa" extrae solo el objeto y "Recortar eliminando selección" borra solo la zona seleccionada. |
| **RF-072** | El flujo de **eliminación de fondos** debe ser posible: marcar el fondo con la varita (Shift+clic para añadir zonas) y pulsar Supr, o invertir la selección y recortar a nueva capa. |

### 3.8 Módulo H — Herramienta Texto (`TextTool`, `TextLayer`, `TextRun`)

| ID | Requisito |
|----|-----------|
| **RF-073** | La herramienta de texto debe crear **capas de texto editables** (`TextLayer`) que almacenan texto, fuente, color y estilos como propiedades, sin rasterizar. |
| **RF-074** | **Arrastrar** sobre el lienzo debe definir el rectángulo de texto y abrir un editor inline (`JTextPane`) superpuesto; **clic** simple activa el modo **auto-ajuste** (dimensiones calculadas según el contenido). |
| **RF-075** | **Enter / Ctrl+Enter / focus lost / cambio de herramienta** deben confirmar el texto y crear o actualizar la capa, con nombre = primeros 30 caracteres del contenido. |
| **RF-076** | Un **clic** sobre una `TextLayer` existente debe re-editar el texto inline, restaurando el formato rico previo. |
| **RF-077** | El texto debe soportar **formato por carácter** (rich text) mediante `TextRun` (fuente, tamaño, negrita, cursiva, color, subrayado, tachado): los cambios con texto seleccionado se aplican solo a la selección; sin selección, como atributos de entrada por defecto. |
| **RF-078** | La capa debe ofrecer **alineación** (izquierda, centro, derecha, justificada), **orientación** (horizontal/vertical), **flujo** (líneas/columnas), **interlineado** y **auto-ajuste**. |
| **RF-079** | En modo auto-ajuste, la alineación debe usar la **primera línea** como referencia (el texto crece hacia la derecha, se centra o termina en el punto de clic). |
| **RF-080** | El panel de opciones de texto debe ofrecer fuente (todas las del sistema), negrita, cursiva, tamaño (8-72), alineación, vertical, color, ángulo de rotación, subrayado y tachado, con aplicación **en tiempo real**. |
| **RF-081** | El **ángulo** de rotación (-360° a 360°) debe rotar la capa de texto de forma no destructiva, sincronizándose con el asa del gizmo y conservándose al re-editar. |
| **RF-082** | La barra de opciones debe **sincronizarse con la selección** del editor inline: atributos uniformes → valores mostrados; atributos mixtos → controles en blanco/indeterminado; alineación → según el párrafo del cursor. |
| **RF-083** | El **panel derecho de texto** (pestañas Alineamiento y Fuentes) debe ofrecer controles duplicados **completamente sincronizados** con la barra superior (misma lógica `applyTextProperty`). |

### 3.9 Módulo I — Transformación y gizmo (`TransformGizmo`, `TransformTool`)

| ID | Requisito |
|----|-----------|
| **RF-084** | El gizmo debe dibujar un marco azul alrededor de los bounds de la capa, **8 tiradores** (esquinas y puntos medios) y un **asa de rotación** circular en el borde superior. |
| **RF-085** | Arrastrar un tirador debe redimensionar la capa; al redimensionar desde una **esquina**, la esquina opuesta queda **fija**. |
| **RF-086** | Con "Mantener proporción" activo, las esquinas deben mantener la relación ancho/alto y los tiradores laterales redimensionan libremente. |
| **RF-087** | El asa de rotación debe rotar la capa de forma **no destructiva** (ángulo guardado, sin rasterizar); **Shift** ajusta a múltiplos de 15°. |
| **RF-088** | La **rotación no destructiva** debe aplicarse a imágenes, texto y formas: girar y volver a girar no acumula pérdida de calidad, y un texto rotado sigue siendo editable. |
| **RF-089** | El gizmo debe respetar un tamaño mínimo de 4 px y, cuando la capa está rotada, el marco y los tiradores deben dibujarse también rotados. |
| **RF-090** | El gizmo debe exponer targets: **capa** (operar sobre los bounds de la capa o capas) y **marco** (operar sobre los bounds de la selección de píxeles; la rotación no aplica). |
| **RF-091** | Las **flechas del teclado** deben mover la capa o capas seleccionadas 1 px (Shift = 10 px). |
| **RF-092** | **Alt+arrastre** con la herramienta Edición debe duplicar la capa activa y mover la copia en el mismo gesto. |
| **RF-093** | **Ctrl+clic** sobre una capa debe activarla temporalmente sin cambiar la selección permanente; **Ctrl+Shift+I** invierte la selección de capas. |

### 3.10 Módulo J — Panel de capas (`LayerModel`, `LayerCardPanel`, `LayerCard`)

| ID | Requisito |
|----|-----------|
| **RF-094** | La pestaña "Capas" del panel derecho debe mostrar las capas como **tarjetas visuales** apiladas (la superior arriba, estilo Photoshop), con barra de scroll vertical que aparece cuando no caben. |
| **RF-095** | Cada tarjeta debe incluir: **ojo de visibilidad**, **candado de bloqueo** (antes del nombre, para no quedar tapado por el scroll), **miniatura 40×40** y **nombre editable**. |
| **RF-096** | La selección de capas debe soportar: clic (única activa), **Ctrl+clic** (añadir/quitar de la multiselección) y **Shift+clic** (rango continuo); la capa activa debe distinguirse visualmente. |
| **RF-097** | El modelo de capas (`LayerModel`) debe notificar cada mutación (añadir, eliminar, duplicar, mover, selección) y refrescar automáticamente el panel y el canvas. |
| **RF-098** | Las tarjetas deben poder **reordenarse por arrastre**, con una línea azul indicando el destino y **Esc** para cancelar; las capas bloqueadas no se arrastran. |
| **RF-099** | La barra vertical EAST debe ofrecer: **Añadir capa nueva** (vacía, transparente, del tamaño del lienzo), **Combina Capas**, **Duplicar Capa**, **Borrar capa** y las cuatro operaciones de orden Z (Traer al frente, Subir un nivel, Bajar un nivel, Enviar al fondo). |
| **RF-100** | **Combinar capas** debe fusionar las capas seleccionadas **visibles y no bloqueadas** (mínimo dos) en una única capa de imagen con el área unión de sus bounds; las ocultas se descartan y las bloqueadas permanecen intactas. |
| **RF-101** | El resultado de combinar debe colocarse donde estaba la selección más alta, adoptar el nombre de la capa superior y quedar con opacidad completa, visible y sin bloqueo. |
| **RF-102** | **Duplicar capa** debe crear una copia justo encima de la original con el mismo contenido, tamaño y propiedades; las capas bloqueadas no se duplican. |
| **RF-103** | **Borrar capa** debe eliminar la capa o capas seleccionadas (o la activa si no hay selección); el botón de la barra siempre elimina la capa completa. |
| **RF-104** | El **slider de opacidad** bajo las tarjetas debe modificar la opacidad de la capa activa, incluidas las capas de texto. |
| **RF-105** | Las capas **bloqueadas** no deben poder moverse, duplicarse ni combinarse, pero deben seguir siendo visibles. |
| **RF-106** | El botón **Invertir selección de capas** (o Ctrl+Shift+I) debe seleccionar las capas no seleccionadas y deseleccionar las actuales, pasando a activa la de mayor índice. |

### 3.11 Módulo K — Disposición y Auto Layout (`LayerDistributionActions`, `AutoLayoutEngine`, `AutoLayoutAlgorithm`)

| ID | Requisito |
|----|-----------|
| **RF-107** | La sección **Disposición** del panel derecho debe agrupar Alinear, Distribuir, Espacio, Auto Layout y Opciones en un **acordeón exclusivo** (al expandir una sección, las demás se cierran). |
| **RF-108** | **Alinear** debe alinear las capas seleccionadas respecto a la referencia elegida; con varias capas, se alinean como una única capa virtual cuyo contorno es la unión de todas (estilo Photoshop). |
| **RF-109** | La referencia de alineación debe ser configurable: **Lienzo** (bordes y centro), **Selección** (unión de las seleccionadas; por defecto) o **Capa maestra** (fijada con "Usar capa activa como maestra" / "Quitar"). |
| **RF-110** | **Distribuir** debe repartir las capas a intervalos iguales entre los valores extremos (bordes o centros), dejando fijas la primera y la última; debe ser **idempotente** y deshabilitarse con menos de 3 capas efectivas. |
| **RF-111** | **Espacio** debe igualar el hueco entre capas consecutivas sin mover la primera ni la última. |
| **RF-112** | El sistema **Auto Layout** debe actuar **únicamente sobre las capas seleccionadas** (visibles y no bloqueadas); sin selección, no hace nada. |
| **RF-113** | Auto Layout debe ofrecer **13 herramientas**: Grid Uniforme, Grid Conservando Tamaño, Compactar (Estantes/Skyline/Guillotina), Compactar por Filas, Compactar por Columnas, Layout Hero, Espiral, Mosaico, Ajustar al Canvas, Centrar Composición Horizontal/Vertical e Igualar Altura/Anchura. |
| **RF-114** | **Grid Uniforme** debe escalar cada capa (con proporción) para encajar en una celda igual; **Grid Conservando Tamaño** respeta el tamaño natural y solo crece hacia la derecha y abajo. |
| **RF-115** | **Compactar** debe empaquetar las capas sin solaparse minimizando huecos según el algoritmo elegido; **por Filas/Columnas** reparte con equilibrio (12 capas → 4/4/4). |
| **RF-116** | **Layout Hero** debe dar a la capa activa (o a la de mayor área) la superficie principal y distribuir el resto a su lado; **Espiral** coloca las capas en espiral de Arquímedes desde el centro; **Mosaico** usa celdas de tamaño variable limitadas por la variación configurada. |
| **RF-117** | **Ajustar al Canvas** debe escalar la composición (uniforme) para encajar en el lienzo con el margen configurado sin modificar posiciones relativas; **Centrar Composición** traslada al centro sin redimensionar; **Igualar Altura/Anchura** ajusta el tamaño al de la capa activa o primera seleccionada manteniendo proporción. |
| **RF-118** | Ninguna operación de disposición debe modificar la rotación, opacidad, orden Z, nombre ni selección; cada operación debe ser una única entrada de deshacer/rehacer, determinista y con proporción preservada salvo indicación contraria. |
| **RF-119** | Los parámetros de Auto Layout (no seleccionadas: fuera/ignorar, espaciado, margen, margen ajustar, escala hero, variación mosaico y algoritmo de empaquetado) deben configurarse en Configuración Avanzada → Editor. |

### 3.12 Módulo L — Smart Guides

| ID | Requisito |
|----|-----------|
| **RF-120** | Las **Smart Guides** deben asistir el arrastre de capas con la herramienta Edición: los bordes y centros de la capa o capas en movimiento se ajustan a los bordes y centros de las demás capas visibles y del lienzo. |
| **RF-121** | Cuando un borde o centro queda a menos de la **distancia de ajuste** (6 px por defecto), la capa debe alinearse al candidato y dibujarse una **guía de color** a línea completa. |
| **RF-122** | El sistema debe aplicar **histéresis (pegajoso)**: la guía se mantiene mientras el cursor no se aleje más de la **distancia de retención** (3 px), evitando parpadeos. |
| **RF-123** | Mantener **Ctrl** durante el arrastre debe anular temporalmente el ajuste y las guías. |
| **RF-124** | Los parámetros (activación, ajuste a lienzo/capas, distancia de ajuste, retención, color y grosor) deben configurarse en Configuración Avanzada → Editor → Smart Guides. |

### 3.13 Módulo M — Historial de deshacer/rehacer (`EditorHistory`)

| ID | Requisito |
|----|-----------|
| **RF-125** | El editor debe mantener un **historial de hasta 50 pasos** por documento con undo/redo y navegación a cualquier paso. |
| **RF-126** | Cada paso debe guardar un **snapshot inmutable** del documento: lienzo (tamaño, fondo, transparencia) y todas las capas con imagen clonada en profundidad, incluidos los cambios de píxeles (bote, degradado). |
| **RF-127** | Los **gestos de arrastre** de herramienta deben agruparse en un único paso (`beginGesture`/`endGesture`); las operaciones de píxel fuerzan el paso porque mutan la imagen in-place. |
| **RF-128** | Las **operaciones discretas** (alinear, distribuir, espaciar, auto layout, propiedades de texto/forma, pegar imagen, nuevo/redimensionar lienzo, opacidad, eliminar capa) deben registrarse con su propio nombre. |
| **RF-129** | Los atajos deben ser: **Ctrl+Z** deshacer, **Ctrl+Y / Ctrl+Shift+Z** rehacer. |
| **RF-130** | El botón **Historial** debe abrir la lista de pasos (el actual marcado con ▶ y negrita); al pulsar cualquier paso, el sistema debe saltar a él deshaciendo o rehaciendo los pasos necesarios. |
| **RF-131** | El historial debe reiniciarse al crear, abrir o cargar un documento nuevo, y los botones Deshacer/Rehacer deben habilitarse/deshabilitarse según haya pasos. |

### 3.14 Módulo N — Configuración (`ConfigKeys`)

| ID | Requisito |
|----|-----------|
| **RF-132** | Las claves de configuración del editor deben centralizarse en `ConfigKeys` (`editor.*`, `config.editor.*`). |
| **RF-133** | La carpeta base de documentos debe ser configurable (`editor.carpeta_base`), con ruta absoluta o relativa a `~/.miVisorImagenesApp`. |
| **RF-134** | El nombre del archivo temporal (`editor.archivo_temporal_nombre`) y del archivo de recuperación (`editor.archivo.recuperacion`) deben ser configurables. |
| **RF-135** | Las dimensiones por defecto del lienzo deben ser configurables (`config.editor.canvas.default_width` / `_height`). |
| **RF-136** | Los parámetros de Auto Layout y Smart Guides deben persistirse vía `ConfigurationManager`. |

---

## 4. Casos de uso

### CU-01 Crear un documento del editor

1. El usuario entra en el Modo Editor (barra de modos o Ctrl+7).
2. Si hay una sesión de recuperación pendiente, el sistema pregunta Cargar/Descartar/Cancelar.
3. El usuario pulsa **Nuevo** (o crea un lienzo/imagen desde el modo Home).
4. El lienzo por defecto se restablece y el editor queda listo para editar.

**Precondiciones:** no hay sesión de recuperación o el usuario elige Descartar.
**Postcondiciones:** documento limpio listo para editar.

### CU-02 Editar una composición con capas

1. El usuario añade capas (pegar imagen, añadir capa nueva, arrastrar imágenes).
2. Reordena las capas en el panel de capas (arrastre o botones de orden Z).
3. Transforma capas con el gizmo (mover, escalar, rotar).
4. Ajusta la disposición con Alinear/Distribuir/Auto Layout.
5. Guarda el documento (Ctrl+S o Guardar).

### CU-03 Quitar el fondo de una imagen

1. El usuario selecciona la capa con la imagen.
2. Pulsa con la **varita** sobre el fondo; usa Shift+clic para añadir todas las zonas del fondo.
3. Pulsa **Supr** para borrar el fondo (deja transparente), o invierte la selección y usa
   **Recortar a nueva capa** para extraer solo el objeto.

### CU-04 Añadir y formatear texto

1. El usuario activa la herramienta Texto (T) y arrastra sobre el lienzo.
2. Escribe el texto en el editor inline.
3. Selecciona fragmentos y cambia fuente, tamaño, negrita, color, subrayado o tachado.
4. Ajusta alineación, vertical o ángulo; confirma con Enter.

### CU-05 Disposición automática

1. El usuario selecciona varias capas en el panel de capas.
2. Abre la sección Disposición del panel derecho.
3. Elige la referencia de alineación y aplica alinear/distribuir/espaciar o una de las 13
   herramientas de Auto Layout.
4. Deshace (Ctrl+Z) si el resultado no convence.

### CU-06 Recuperar un documento sin guardar

1. El usuario cierra la aplicación con el documento marcado con `*` y elige **No Guardar**.
2. El sistema persiste la sesión de recuperación.
3. Al reabrir y entrar en el Modo Editor, el sistema pregunta **Cargar**.
4. El documento se restaura con su nombre original y marcado como recuperado.

### CU-07 Mover contenido entre el Modo Render y el Modo Editor

1. En el Modo Render, el usuario pulsa *Copiar imagen* sobre la imagen actual.
2. Entra en el Modo Editor (Ctrl+7).
3. Pulsa **Pegar imagen** (Ctrl+V); la imagen se inserta como nueva capa centrada.

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNF-001** | El historial debe acotar la memoria a **50 snapshots**; las capas se clonan en profundidad solo en la captura, no en cada pintado. |
| **RNF-002** | El pintado del canvas debe repintarse solo sobre el área afectada cuando sea posible (repaint incremental), evitando repintados completos innecesarios. |
| **RNF-003** | La carga/lectura de documentos `.edoc` debe ser ligera (JSON simple, capas con ruta de origen o PNG sidecar). |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNF-004** | El editor debe ofrecer atajos de teclado completos (herramientas, undo/redo, selección, modo). |
| **RNF-005** | La interfaz debe comunicar visualmente el estado: capa activa, selección múltiple, candado/visibilidad, selección de píxeles (hormigueo + relleno), cambios pendientes (asterisco). |
| **RNF-006** | Los paneles (barra de opciones, panel derecho de texto, panel de capas) deben mantener **sincronización bidireccional** con el canvas. |
| **RNF-007** | El acordeón de Disposición y el panel de capas deben mantener el espacio compacto. |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNF-008** | Las operaciones de documento deben fallar de forma controlada (log + diálogo de error) sin bloquear la aplicación. |
| **RNF-009** | La **recuperación de sesión** debe ser autocontenida (PNG sidecar junto al archivo de recuperación) y eliminarse tras un uso correcto. |
| **RNF-010** | El historial debe ser **a prueba de errores de clonado** (una capa que falle al clonar se ignora en el snapshot, sin romper el documento). |
| **RNF-011** | La salida de pantalla completa no debe perder el estado del documento ni la herramienta activa. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNF-012** | El código debe seguir el patrón MVC plano del proyecto y la DI por `ComponentRegistry`. |
| **RNF-013** | Los modelos del editor deben residir en `modelo.editor`, la lógica de documento en `servicios.editor` y las herramientas en `controlador.tools`. |
| **RNF-014** | Las acciones deben centralizarse en `AppActionCommands` y las claves de configuración en `ConfigKeys`. |
| **RNF-015** | La documentación de ayuda debe residir en `resources/help/editor/*.html`. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNF-016** | El editor debe ejecutarse en Windows sobre Java 21 (Swing + FlatLaf), sin dependencias externas adicionales. |
| **RNF-017** | Las rutas de los documentos `.edoc` y de los PNG sidecar deben tratarse de forma **agnóstica al separador**. |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNF-018** | No deben registrarse datos sensibles en los logs. |
| **RNF-019** | Los documentos `.edoc` y las carpetas sidecar son datos locales del usuario (fuera del repositorio). |

---

## 6. Modelo de datos

### 6.1 Modelos del editor (`modelo.editor`)

| Clase | Campos principales | Descripción |
|-------|--------------------|-------------|
| `CanvasModel` | `width`, `height`, `backgroundColor`, `transparent` | Lienzo de trabajo. |
| `LayerModel` | lista de `Layer`, capa activa, selección, `changeListener` | Modelo de capas con notificación de cambios. |
| `Layer` | `id`, `name`, `bounds`, `rotation`, `visible`, `locked`, `opacity` | Capa base con transformación y estado. |
| `ImageLayer` | imagen, `type` (IMAGE/SHAPE), `srcPath`, metadatos de forma | Capa de imagen o forma. |
| `TextLayer` | texto, fuente, color, alineación, vertical, interlineado, auto-ajuste, `runs` | Capa de texto editable. |
| `SelectionModel` | máscara, bounds, activo | Selección de píxeles con máscara. |
| `TransformGizmo` | target (capa/marco), tiradores, asa de rotación | Gizmo de transformación. |

### 6.2 Documento `.edoc` (`servicios.editor.EditorDoc`)

| Campo | Descripción |
|-------|-------------|
| `version` | Versión del esquema (`VERSION_ACTUAL = 1`). |
| `canvas` | `width`, `height`, `backgroundColorArgb`, `transparent`. |
| `layers` | Lista de `LayerDTO` con `type` (IMAGE/SHAPE/TEXT), transformación, estado común y metadatos específicos. |
| `nombreDocumento` | Nombre informativo (útil al recuperar). |
| `TextRunDTO` | `fontFamily`, `fontStyle`, `fontSize`, `colorArgb`, `underline`, `strikethrough`, `text`. |

### 6.3 Claves de configuración

| Clave | Por defecto | Descripción |
|-------|-------------|-------------|
| `editor.carpeta_base` | `.editor_docs` | Carpeta base de documentos (relativa a `~/.miVisorImagenesApp`). |
| `editor.archivo_temporal_nombre` | `editor_temporal.edoc` | Documento temporal sin guardar. |
| `editor.archivo.recuperacion` | `editor_recuperacion.edoc` | Sesión de recuperación. |
| `editor.estado.recuperacion_pendiente` | *(vacío)* | Marca de sesión de recuperación pendiente. |
| `config.editor.canvas.default_width` | `1920` | Ancho por defecto del lienzo. |
| `config.editor.canvas.default_height` | `1080` | Alto por defecto del lienzo. |
| `editor.autolayout.margin` | — | Margen general de Auto Layout. |
| `editor.autolayout.spacing` | — | Espaciado entre celdas/capas. |
| `editor.autolayout.fit_margin` | — | Margen de *Ajustar al Canvas*. |
| `editor.autolayout.hero_scale` | — | Proporción de superficie del Layout Hero. |
| `editor.autolayout.mosaic_variance` | — | Variación de tamaño del mosaico. |
| `editor.autolayout.pack_mode` | — | Algoritmo de empaquetado (Estantes/Skyline/Guillotina). |
| `editor.autolayout.no_seleccionadas` | — | Comportamiento de las no seleccionadas (fuera/ignorar). |
| `editor.smart_guides.show` | `true` | Activar Smart Guides. |
| `editor.smart_guides.snap_canvas` | `true` | Ajuste al lienzo. |
| `editor.smart_guides.snap_layers` | `true` | Ajuste a otras capas. |
| `editor.smart_guides.snap_distance` | `6` | Distancia de ajuste (px). |
| `editor.smart_guides.sticky_distance` | `3` | Distancia de retención (px). |
| `editor.smart_guides.color` | — | Color de las guías. |
| `editor.smart_guides.stroke_width` | — | Grosor de las guías. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Integración de modo | `controlador/services/AppModeService.java`, `controlador/GeneralController.java`, `controlador/RenderController.java` |
| Documento `.edoc` | `servicios/editor/EditorDocumentManager.java`, `servicios/editor/EditorDoc.java` |
| Historial | `servicios/editor/EditorHistory.java` |
| Panel del editor | `vista/panels/render/AdvanceEditPanel.java`, `vista/panels/render/CanvasPanel.java`, `vista/panels/render/EditorComponentBar.java` |
| Panel de capas | `vista/panels/render/LayerCardPanel.java`, `vista/panels/render/LayerCard.java` |
| Modelos del editor | `modelo/editor/CanvasModel.java`, `LayerModel.java`, `Layer.java`, `ImageLayer.java`, `TextLayer.java`, `SelectionModel.java` |
| Gizmo | `modelo/gizmo/TransformGizmo.java` |
| Herramientas | `controlador/tools/CanvasController.java`, `controlador/tools/EditTool.java`, `TransformTool.java`, `MarqueeSelectionTool.java`, `LayerSelectionTool.java`, `MagicWandTool.java`, `PaintBucketTool.java`, `ColorPickerTool.java`, `CropTool.java`, `TextTool.java`, `ShapeTool.java`, `GradientTool.java`, `ZoomTool.java` |
| Editores de capa | `controlador/tools/editors/TextLayerEditor.java`, `controlador/tools/editors/ShapeLayerEditor.java` |
| Disposición | `controlador/actions/editoravanzado/LayerDistributionActions.java`, `controlador/actions/editoravanzado/autolayout/*` |
| Atajos | `controlador/managers/KeyboardShortcutManager.java`, `controlador/utils/EditorHotkeys.java` |
| Acciones | `controlador/factory/ActionFactory.java`, `controlador/commands/AppActionCommands.java` |
| Configuración | `servicios/ConfigKeys.java`, `servicios/ConfigurationManager.java` |
| Ayuda | `resources/help/modo_editor.html`, `resources/help/editor/*.html` |

### 7.2 Apéndice B — Glosario técnico

- **Lienzo:** área de trabajo del editor con dimensiones, fondo y transparencia.
- **Capa:** elemento apilado (imagen, forma o texto) con transformación y estado propios.
- **Snapshot:** estado inmutable del documento capturado por el historial.
- **Gizmo:** tiradores visuales para mover, escalar y rotar capas.
- **Máscara:** forma irregular de la selección de píxeles.
- **TextRun:** fragmento de texto con formato propio.
- **Auto Layout:** sistema de composición automática (13 herramientas) sobre las capas seleccionadas.
- **Smart Guides:** guías inteligentes de ajuste al arrastrar capas.
- **`.edoc`:** formato JSON del documento del editor.

---

*Fin del documento.*
