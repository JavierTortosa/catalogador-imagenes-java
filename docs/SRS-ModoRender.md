# Documento de Especificación de Requisitos de Software (SRS)

## Modo Render del Visor de Imágenes V2

| Campo | Valor |
|-------|-------|
| **Proyecto** | Visor de Imágenes V2 (DAM) |
| **Componente** | Modo RENDER (Zip2Png) |
| **Versión del documento** | 1.0 |
| **Estado** | Aprobado para revisión |
| **Audiencia** | Cliente, equipo de desarrollo, QA |

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales del **Modo Render** del
Visor de Imágenes V2, un sistema de gestión de activos digitales (DAM) de escritorio para
bibliotecas masivas (>40.000 activos) de modelos 3D.

El Modo Render automatiza la generación de imágenes de previsualización (PNG) a partir de
archivos de modelado 3D (principalmente STL) que se distribuyen comprimidos en ZIP, RAR o 7z.
Estas imágenes de renderizado se asocian a cada modelo y alimentan el flujo de visualización y
selección del sistema.

El documento describe **lo que se ha construido**, sirviendo de especificación de referencia
para el cliente y de base para futuras evoluciones (nuevos motores de renderizado, persistencia
del editor, etc.).

### 1.2 Alcance

El alcance cubre:

- Escaneo de carpetas en busca de modelos 3D sin imagen de previsualización asociada.
- Inspección del contenido de archivos comprimidos.
- Renderizado por software de mallas 3D a imágenes PNG.
- Visor 3D interactivo (JavaFX) y visor 2D.
- Ajustes de imagen y fondo del renderizado.
- Asignación de previews a los archivos de origen.
- Exportación/descarga de previews.
- **Editor avanzado** de composición por capas (integración desde el Modo Render; la
  especificación completa del editor vive en `SRS-ModoEditor.md`).
- Configuración del subsistema de renderizado.

Queda **fuera del alcance** de este documento:

- El renderizado mediante motores externos OpenSCAD y Blender (configurables pero no
  implementados funcionalmente).
- El modo VISUALIZADOR (navegación y previsualización de la biblioteca), especificado en
  `SRS-ModoVisor.md`.
- El modo DATOS (catalogación y etiquetas en base de datos SQLite), especificado en
  `SRS-ModoDatos.md`.
- El modo PROYECTO (selección, verificación de integridad y exportación de archivos),
  especificado en `SRS-ModoProyecto.md`.
- El modo CLIENTE (revisión y exportación de catálogos HTML), especificado en
  `SRS-ModoCliente.md`.

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---------|-----------|
| **STL** | Formato de archivo de malla 3D (binario o ASCII) basado en triángulos. |
| **OBJ / 3MF** | Otros formatos de malla 3D detectados por el escáner. |
| **Candidato** | Modelo 3D o comprimido que no dispone de imagen asociada y, por tanto, es susceptible de renderizarse. |
| **Multivolumen** | Archivo comprimido partido en varios volúmenes (`nombre.part01.rar`, `nombre.7z.001`, etc.). |
| **Preview** | Imagen PNG generada o extraída que representa visualmente al modelo 3D. |
| **Filmstrip** | Tira horizontal de miniaturas bajo el visor. |
| **SSAO** | *Screen-Space Ambient Occlusion*; sombreado aproximado de oclusión ambiental. |
| **DAM** | *Digital Asset Management*; gestión de activos digitales. |
| **EDT** | *Event Dispatch Thread*; hilo de interfaz de Swing. |

### 1.4 Referencias

- `resources/help/render.html` — Ayuda del Modo Render.
- `resources/help/herramientas.html` — Ayuda de herramientas del Modo Render.
- `AGENTS.md` — Guía general del proyecto.
- `README` / documentación del Visor de Imágenes V2.
- `docs/SRS-ModoVisor.md` — Especificación del Modo Visor.
- `docs/SRS-ModoDatos.md` — Especificación del Modo Datos.
- `docs/SRS-ModoProyecto.md` — Especificación del Modo Proyecto.
- `docs/SRS-ModoCliente.md` — Especificación del Modo Cliente.
- `docs/SRS-ModoEditor.md` — Especificación del Modo Editor y el Editor Avanzado.

### 1.5 Resumen

El documento se organiza como sigue: la sección 2 presenta una descripción general del producto;
la sección 3 enumera los requisitos funcionales agrupados por módulos; la sección 4 describe los
casos de uso principales; la sección 5 especifica los requisitos no funcionales; la sección 6
describe el modelo de datos; y la sección 7 incluye apéndices con trazabilidad al código fuente.

---

## 2. Descripción general

### 2.1 Perspectiva del producto

El Modo Render es uno de los modos de trabajo del Visor de Imágenes V2. El resto de modos son
VISUALIZADOR, PROYECTO, CLIENTE, DATOS, CARRUSEL y EDITOR. El acceso se realiza desde la barra de
modos principal (botón **Render**, atajo **Ctrl+6**). Internamente el modo se identifica como
`WorkMode.RENDER`.

El modo se integra con el resto del sistema mediante:

- `AppModeService` — gestión del cambio de modo, entrada/salida y sincronización de botones.
- `ViewBuilder` — construcción del panel `RenderPanel` y registro en el `ComponentRegistry`.
- `ActionFactory` — acciones de botones (`CMD_MODO_RENDER`, etc.).
- `ConfigurationManager` / `ConfigKeys` — parámetros de configuración (`zip2png.*`).
- `ExternalToolsManager` — localización de `7z.exe` para extracción y listado.
- `SceneController` — carga de mallas 3D en el visor.

### 2.2 Funciones del producto

El producto ofrece las siguientes capacidades de alto nivel:

1. **Escanear carpetas** para detectar modelos 3D y comprimidos sin imagen asociada.
2. **Clasificar candidatos** en «Sin renderizar» (contienen STL) y «Con imagen» (contienen
   imágenes embebidas).
3. **Inspeccionar** el contenido de los comprimidos (STLs e imágenes con sus tamaños).
4. **Previsualizar** modelos en 3D de forma interactiva (órbita, zoom, pan, iluminación).
5. **Ajustar** brillo, contraste, fondo y rotación del render.
6. **Procesar** candidatos en lote para generar PNG por software.
7. **Asignar previews** a los archivos de origen (o al editor avanzado).
8. **Descargar/guardar** previews en disco.
9. **Editar composiciones** por capas mediante el editor avanzado.
10. **Configurar** motor, rutas, límites y carpeta temporal.

### 2.3 Clases y características de usuarios

| Usuario | Características |
|---------|-----------------|
| **Operador DAM** | Gestiona bibliotecas masivas de modelos 3D; necesita generar y asignar previews rápidamente. |
| **Cliente final** | Recibe catálogos interactivos (fuera del alcance de este documento). |

### 2.4 Entorno operativo

- **Sistema operativo:** Windows (desarrollo y despliegue).
- **Plataforma Java:** Java 21; JavaFX 21 (classifier `win`) para el visor 3D.
- **Herramienta externa requerida:** `7z.exe` para listar y extraer ZIP/RAR/7z.
- **Interfaz de usuario:** Swing + FlatLaf, con `JFXPanel` embebido para la escena 3D.
- **Almacenamiento:** carpeta temporal configurable (por defecto `%TEMP%/visor_zip2png`).

### 2.5 Restricciones de diseño e implementación

- El renderizado actual emplea un **motor por software** (AWT/Swing) con resolución fija de
  **512×512 píxeles**.
- El escaneo es deliberadamente **rápido**: no abre los comprimidos, solo compara nombres de
  archivo.
- La extracción y el listado de comprimidos requieren `7z.exe`.
- El límite de tamaño de candidato es configurable (por defecto 512 MB).
- Los motores OpenSCAD y Blender están configurados en la UI pero **no implementados** como
  alternativa de renderizado.

### 2.6 Documentación de usuario

- Ayuda en línea: `resources/help/render.html` y `resources/help/herramientas.html`.

### 2.7 Suposiciones y dependencias

- Cada comprimido contiene al menos un STL; si contiene varios, se renderiza **el de mayor
  tamaño**.
- La imagen asociada a un modelo se identifica por **igual nombre base en la misma carpeta**
  (p. ej. `pieza.stl` ↔ `pieza.png`).
- Si existe una imagen con el mismo nombre base que el comprimido, el candidato **se excluye**
  del escaneo (ya está previsualizado).
- 7z.exe debe estar accesible para el procesamiento de comprimidos.

---

## 3. Requisitos funcionales

Los requisitos se numeran como **RF-XXX** y se agrupan por módulos. Los identificadores de
clase entre paréntesis permiten la trazabilidad al código fuente (apéndice A).

### 3.1 Módulo A — Escaneo de carpetas (`Zip2PngScanner`)

| ID | Requisito |
|----|-----------|
| **RF-000** | El sistema considerará que un activo está representado cuando exista una imagen con el mismo nombre base que el archivo original. La generación automática de dicha representación constituye el objetivo principal del Modo Render. |
| **RF-001** | El sistema debe escanear una carpeta seleccionada por el usuario a profundidad máxima de 2 niveles, buscando archivos 3D y comprimidos. |
| **RF-002** | Se consideran archivos 3D: `.stl`, `.obj`, `.3mf`. |
| **RF-003** | Se consideran comprimidos: `.zip`, `.rar`, `.7z`. |
| **RF-004** | El escaneo debe detectar las imágenes ya existentes en la carpeta (`.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.webp`) y su nombre base. |
| **RF-005** | Un archivo 3D o comprimido cuyo nombre base coincida con una imagen existente **no** debe aparecer como candidato. |
| **RF-006** | Los comprimidos multivolumen (`.partN.rar`, `.7z.NNN`, `.rNN`, `.zNN`) deben agruparse en un único candidato. |
| **RF-007** | Para cada candidato se debe calcular el tamaño total acumulado (suma de volúmenes) y marcar si supera el límite configurado. |
| **RF-008** | El escáner no debe abrir los comprimidos durante el escaneo; solo compara nombres y tamaños. |
| **RF-009** | El escaneo debe notificar cada candidato encontrado mediante un callback sin bloquear el hilo de escaneo. |
| **RF-010** | Cada candidato debe exponer: ruta, nombre base, tamaño en bytes, si es comprimido, si excede el límite y la lista de volúmenes agrupados. |

### 3.2 Módulo B — Inspección de comprimidos (`ZipExtractor`)

| ID | Requisito |
|----|-----------|
| **RF-011** | El sistema debe listar el contenido de un comprimido sin extraerlo usando 7z (`7z l`). |
| **RF-012** | Debe poder listar únicamente los STL internos de un comprimido, con su ruta interna y tamaño, mediante 7z con formato etiquetado (`-slt`). |
| **RF-013** | Debe poder listar únicamente las imágenes internas (`.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`, `.webp`, `.tiff`, `.tif`) con su tamaño. |
| **RF-014** | Debe poder extraer el contenido completo de un comprimido a un directorio temporal. |
| **RF-015** | Debe poder extraer un único archivo interno de un comprimido preservando la estructura de subdirectorios. |
| **RF-016** | Toda operación de 7z debe tener un tiempo máximo de espera de 120 segundos; ante timeout, el proceso debe terminarse y notificarse el error. |
| **RF-017** | Debe poder comprobar si un archivo es un comprimido válido. |
| **RF-018** | El sistema debe poder eliminar directorios temporales de forma recursiva y segura. |

### 3.3 Módulo C — Parseo de mallas (`StlParser`)

| ID | Requisito |
|----|-----------|
| **RF-019** | El sistema debe detectar automáticamente si un STL es binario o ASCII inspeccionando los primeros 8 KB en busca de la marca `facet normal`. |
| **RF-020** | Debe parsear STL binario y ASCII convirtiéndolos en una lista de triángulos (`Triangle`). |
| **RF-021** | Debe rechazar archivos cuyo tamaño supere un límite máximo (500 MB) para evitar consumos excesivos de memoria. |

### 3.4 Módulo D — Renderizado por software (`AwtModelRenderer`)

| ID | Requisito |
|----|-----------|
| **RF-022** | El motor de software debe renderizar una lista de triángulos a una imagen `BufferedImage`. |
| **RF-023** | El renderizado por defecto debe producir una imagen de **512×512** píxeles con margen de 24 px. |
| **RF-024** | El modelo debe iluminarse con luz ambiental (20 %) y 3 luces direccionales para lograr volumen. |
| **RF-025** | Debe aplicar un rango de color normalizado (210) para el sombreado. |
| **RF-026** | Debe admitir la **rotación** del modelo sobre los ejes X e Y antes de renderizar. |
| **RF-027** | Debe ofrecer una variante de renderizado **interactivo** (sin SSAO) para previsualización en tiempo real. |
| **RF-028** | Debe ofrecer una variante de renderizado **con SSAO** (ambient occlusion) para la imagen final. |
| **RF-029** | Debe admitir **ajustes de brillo y contraste** sobre la imagen resultante. |
| **RF-030** | Debe admitir distintos **fondos**: sólido, degradado (dos colores), imagen con escala, y transparente (canal alfa). |
| **RF-031** | La imagen renderizada debe poder escribirse en disco en formato PNG. |

### 3.5 Módulo E — Visor 3D interactivo (`PreviewPanel3DFX`, `StlMeshBuilder`)

| ID | Requisito |
|----|-----------|
| **RF-032** | El visor debe mostrar una escena JavaFX 3D embebida (`JFXPanel`) con cámara en perspectiva. |
| **RF-033** | Debe construir un `TriangleMesh` de JavaFX a partir de la lista de triángulos, con escalado automático y centrado. |
| **RF-034** | El usuario debe poder **orbitar** el modelo arrastrando el ratón. |
| **RF-035** | El usuario debe poder hacer **zoom** (rueda/gesto) y **pan** sobre la escena. |
| **RF-036** | El visor debe admitir cambios de **brillo y contraste** de la escena. |
| **RF-037** | El visor debe admitir los mismos **modos de fondo** que el renderizador (sólido, degradado, imagen, transparente). |
| **RF-038** | El visor debe admitir una **imagen de fondo** con factor de escala ajustable. |
| **RF-039** | El visor debe mostrar una **crosshair** (reticula) opcional y un fondo **damero** (checkerboard) opcional para transparencias. |
| **RF-040** | El modelo debe poder **resetearse** a su orientación inicial. |

### 3.6 Módulo F — Flujo de trabajo principal (`RenderController`, `RenderPanel`)

| ID | Requisito |
|----|-----------|
| **RF-041** | El modo debe presentar dos pestañas de candidatos: **«Sin renderizar»** y **«Con imagen»**. |
| **RF-042** | Al seleccionar un candidato comprimido «Sin renderizar», el panel inferior debe listar sus STL internos **ordenados por tamaño descendente**, previsualizando automáticamente el primero en 3D. |
| **RF-043** | Al seleccionar un candidato «Con imagen», el panel inferior debe listar las imágenes internas del comprimido. |
| **RF-044** | El doble clic sobre un STL debe extraerlo a temporal, parsearlo (en segundo plano vía `SwingWorker`), cachear sus triángulos y cargarlo en el visor 3D. |
| **RF-045** | El doble clic sobre una imagen interna debe extraerla y mostrarla en el visor 2D. |
| **RF-046** | El sistema debe recordar la última selección de cada pestaña y restaurarla al alternar entre «Sin renderizar» y «Con imagen». |
| **RF-047** | La **Galería ZIP** debe mostrar, en un filmstrip horizontal, miniaturas de todos los STLs o imágenes del comprimido seleccionado. |
| **RF-048** | Los STLs del filmstrip deben mostrarse como placeholders y renderizarse **en segundo plano**, con una barra de progreso indicando el avance. |
| **RF-049** | La acción **Escanear carpeta** debe abrir un selector de carpeta, ejecutar el escaneo y clasificar los resultados en las dos pestañas. |
| **RF-050** | La acción **Procesar seleccionados** debe renderizar todos los candidatos en lote, generando un PNG por candidato y extrayendo las imágenes embebidas de los que las contengan. |
| **RF-051** | La acción **Procesar archivo** debe renderizar únicamente el candidato actualmente seleccionado. |
| **RF-052** | La acción **Abrir carpeta temporal** debe abrir el explorador en la carpeta temporal de renders. |
| **RF-053** | La acción **Descargar preview** debe abrir un selector de carpeta (empezando en la carpeta del comprimido original) y guardar el preview actual como PNG. |
| **RF-054** | Durante el procesado en lote, los renders generados deben aparecer en el grid de renders (thumbnail 180 px) y las imágenes extraídas en el grid de imágenes. |
| **RF-055** | Al finalizar el procesado, el sistema debe seleccionar automáticamente el primer candidato de «Sin renderizar». |

### 3.7 Módulo G — Ajustes de renderizado

| ID | Requisito |
|----|-----------|
| **RF-056** | Los controles de **brillo** y **contraste** deben estar sincronizados entre una barra deslizadora y un campo numérico. |
| **RF-057** | El sistema debe ofrecer una acción para **restablecer** los ajustes de previsualización a sus valores por defecto. |
| **RF-058** | El modo de fondo debe seleccionarse mediante opciones excluyentes: sólido, degradado, imagen y transparente. |
| **RF-059** | Para el fondo sólido, el usuario debe poder elegir el color (selector de color). |
| **RF-060** | Para el fondo degradado, el usuario debe poder elegir los colores de inicio y fin. |
| **RF-061** | Para el fondo imagen, el usuario debe poder elegir el archivo y ajustar la escala de la imagen. |
| **RF-062** | Los cambios de fondo deben aplicarse simultáneamente al visor 3D y al renderizador por software. |

### 3.8 Módulo H — Asignación de previews y gestión de resultados

| ID | Requisito |
|----|-----------|
| **RF-063** | La acción **Asignar preview** debe comportarse según el contexto: |
|     | **a)** Si el editor avanzado está activo, añadir la imagen actual como nueva capa del canvas. |
|     | **b)** Si el visor 2D muestra una imagen, usar esa imagen. |
|     | **c)** En modo 3D, re-renderizar el STL con los ajustes actuales de imagen y fondo. |
| **RF-064** | Si el canvas del editor está vacío, se debe redimensionar al tamaño de la imagen asignada. |
| **RF-065** | El usuario debe poder marcar renders como **aprobados** en el grid. |
| **RF-066** | Al cerrar el modo con renders aprobados pendientes, el sistema debe preguntar si se copian a la carpeta de destino de los archivos de origen. |
| **RF-067** | El sistema debe poder **limpiar** el directorio temporal de renders. |
| **RF-068** | El usuario debe poder **borrar la previsualización** actual. |

### 3.9 Módulo I — Integración del Editor Avanzado en el Modo Render (`AdvanceEditPanel`, `RenderController`)

| ID | Requisito |
|----|-----------|
| **RF-069** | El modo debe permitir activar y desactivar el **modo edición avanzada** (collage de capas) desde el botón **Editor Avanzado** de la barra de previsualización. |
| **RF-070** | El botón **Editor Avanzado** debe ser un toggle **independiente** de Grid 3D y Grid 2D (que sí son excluyentes entre sí); al activarlo sustituye al modo Collage. |
| **RF-071** | El editor embebido del Modo Render debe trabajar sobre el **documento RENDER** (lienzo en memoria, sin archivo asociado); la composición debe conservarse al salir y volver a este contexto. |
| **RF-072** | La activación del editor avanzado dentro del Modo Render **no** debe desactivar ni tocar el documento del Modo Editor (`editorDocumentoCargadoEnPanel`). |

> **Nota:** la especificación completa del Editor Avanzado (canvas, herramientas, selección de
> píxeles, texto, transformación y gizmo, panel de capas, disposición y Auto Layout, Smart Guides
> e historial) y del Modo Editor (documento `.edoc`, guardado y recuperación) se detalla en
> `SRS-ModoEditor.md`. El detalle que este SRS contenía en RF-070 a RF-082 queda absorbido por
> dicho documento, que lo trata como requisitos propios del editor.

### 3.10 Módulo J — Configuración (`Zip2PngConfigPanel`, `ConfigKeys`)

| ID | Requisito |
|----|-----------|
| **RF-083** | El panel de configuración debe permitir seleccionar el **motor por defecto**: `awt`, `openscad` o `blender`. |
| **RF-084** | Debe permitir introducir la **ruta de OpenSCAD** (opcional). |
| **RF-085** | Debe permitir introducir la **ruta de Blender** (opcional). |
| **RF-086** | Debe permitir configurar el **límite de tamaño** en MB (rango 1–99999, incremento de 64). |
| **RF-087** | Debe permitir configurar la **carpeta temporal** de renders. |
| **RF-088** | La configuración debe cargarse al abrir el diálogo y guardarse únicamente si hubo cambios, persistida vía `ConfigurationManager`. |
| **RF-089** | Las claves de configuración deben estar centralizadas en `ConfigKeys` (`zip2png.motor`, `zip2png.limite_mb`, `zip2png.carpeta_temp`, `zip2png.ruta_openscad`, `zip2png.ruta_blender`). |

### 3.11 Módulo K — Integración de modo (`AppModeService`, `ViewBuilder`, `ActionFactory`)

| ID | Requisito |
|----|-----------|
| **RF-090** | El botón **Render** de la barra de modos debe activar el `WorkMode.RENDER`. |
| **RF-091** | Al entrar en el modo Render, el sistema debe mostrar la vista `VISTA_RENDER` y limpiar la barra de información de imagen. |
| **RF-092** | El panel `RenderPanel` debe registrarse en el `ComponentRegistry` con las claves `panel.workmode.render`, `panel.render.grid`, `panel.render.right` y `panel.render.viewer`. |
| **RF-093** | El modo Render debe seguir el esquema de sincronización de botones de la barra de modos común a todos los modos. |

---

## 4. Casos de uso

### CU-01 Escanear carpeta

1. El usuario activa el Modo Render.
2. Pulsa **Escanear carpeta** y elige una carpeta.
3. El sistema escanea (máx. 2 niveles) e identifica candidatos sin imagen asociada.
4. Los candidatos comprimidos con imágenes internas se clasifican en **«Con imagen»**; el resto en **«Sin renderizar»**.
5. La lista de candidatos se muestra en el panel izquierdo con su tamaño y estado.

**Precondiciones:** carpeta accesible. **Postcondiciones:** lista de candidatos poblada.

### CU-02 Inspeccionar un comprimido

1. El usuario selecciona un candidato en la pestaña **«Sin renderizar»**.
2. El panel inferior lista los STL internos ordenados por tamaño descendente.
3. El primer STL se previsualiza automáticamente en el visor 3D.
4. El usuario hace doble clic en otro STL para cargarlo en el visor.

**Alternativa:** si el candidato está en **«Con imagen»**, el panel lista las imágenes internas; el doble clic las muestra en el visor 2D.

### CU-03 Procesar candidatos

1. El usuario pulsa **Procesar seleccionados** (o **Procesar archivo** para uno solo).
2. Aparece un diálogo de progreso (`TaskProgressDialog`).
3. Para cada candidato comprimido: se extrae, se localiza el STL de mayor tamaño, se parsea y se renderiza a PNG.
4. Se extraen las imágenes embebidas de los candidatos que las contienen.
5. Los PNG y las imágenes aparecen en los grids de resultados.

**Postcondiciones:** renders generados en la carpeta temporal; primer candidato de «Sin renderizar» seleccionado.

### CU-04 Ajustar y guardar un render

1. El usuario carga un modelo en el visor 3D.
2. Ajusta brillo, contraste, rotación y fondo.
3. Pulsa **Descargar preview**: el sistema re-renderiza con los ajustes actuales y abre un selector para guardar el PNG.

**Alternativa:** con el visor 2D activo, se captura la imagen mostrada.

### CU-05 Asignar preview

1. El usuario carga un modelo y ajusta el render.
2. Pulsa **Asignar preview**.
3. Según el contexto, el sistema añade la imagen al editor avanzado, la asigna al grid o re-renderiza con los ajustes actuales.

### CU-06 Editar composición con capas

1. El usuario activa el editor avanzado.
2. Asigna una o varias imágenes como capas del canvas.
3. Reordena, elimina o añade capas mediante el panel de capas.
4. Añade texto y configura tipografía, alineación y orientación.
5. Ajusta colores frontal/fondo y exporta el resultado.

### CU-07 Configurar el subsistema

1. El usuario abre el diálogo de configuración (pestaña **Renderizado**).
2. Elige motor, rutas externas, límite y carpeta temporal.
3. Guarda; los cambios se persisten solo si hubo modificaciones.

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento

| ID | Requisito |
|----|-----------|
| **RNF-001** | El escaneo de carpetas no debe abrir los comprimidos; debe completarse en tiempo proporcional al número de archivos (comparación de nombres y tamaños). |
| **RNF-002** | El renderizado de un STL debe ejecutarse en un hilo de fondo (`SwingWorker`), sin bloquear la EDT. |
| **RNF-003** | El renderizado por software produce imágenes de 512×512; el coste por modelo debe ser acotado (tipo de imagen ARGB, un solo pase con SSAO opcional). |
| **RNF-004** | Las operaciones de 7z deben tener timeout de 120 s para evitar bloqueos indefinidos. |
| **RNF-005** | El listado de contenido de un comprimido debe usar 7z sin extracción previa (eficiente en disco). |

### 5.2 Usabilidad

| ID | Requisito |
|----|-----------|
| **RNF-006** | La interfaz debe presentar pestañas claras («Sin renderizar» / «Con imagen») y barras de herramientas con iconos. |
| **RNF-007** | El progreso de las operaciones largas (escaneo, renderizado, galería) debe indicarse mediante barras o diálogos de progreso. |
| **RNF-008** | Los controles numéricos y los deslizadores deben mantenerse sincronizados. |
| **RNF-009** | La interacción 3D (órbita, zoom, pan) debe ser directa con el ratón. |

### 5.3 Fiabilidad y robustez

| ID | Requisito |
|----|-----------|
| **RNF-010** | Los errores de 7z (códigos de salida distintos de 0, timeout) deben capturarse y notificarse sin abortar el resto del lote. |
| **RNF-011** | Si un comprimido no contiene STL, el sistema debe informarlo y continuar con el siguiente candidato. |
| **RNF-012** | La limpieza de temporales debe ser tolerante a archivos bloqueados (advertencias en log, no fallos). |
| **RNF-013** | La carga de mallas debe realizarse en segundo plano; los errores de parseo deben mostrarse sin bloquear la UI. |

### 5.4 Mantenibilidad

| ID | Requisito |
|----|-----------|
| **RNF-014** | El código debe seguir el patrón MVC plano del proyecto (`controlador` → `modelo` → `vista` → `servicios`). |
| **RNF-015** | Los servicios de renderizado deben residir en `servicios.renderer` y ser reutilizables de forma independiente del controlador. |
| **RNF-016** | Las claves de configuración deben centralizarse en `ConfigKeys`. |

### 5.5 Compatibilidad y portabilidad

| ID | Requisito |
|----|-----------|
| **RNF-017** | El visor 3D requiere JavaFX 21; el empaquetado debe incluir los JARs con classifier `win`. |
| **RNF-018** | El subsistema depende de `7z.exe` accesible vía `ExternalToolsManager`. |

### 5.6 Seguridad

| ID | Requisito |
|----|-----------|
| **RNF-019** | Los archivos extraídos se escriben únicamente en la carpeta temporal configurada y se eliminan al finalizar. |
| **RNF-020** | No se deben registrar credenciales ni datos sensibles en los logs. |

---

## 6. Modelo de datos

### 6.1 Modelos de renderizado (`modelo.renderer`)

| Clase | Campos principales | Descripción |
|-------|--------------------|-------------|
| `Triangle` | vértices (3×3 flotantes), normal | Triángulo individual de la malla. |
| `StlEntry` | `filename`, `sizeBytes` | Archivo STL interno de un comprimido. |
| `ImageEntry` | `filename`, `sizeBytes` | Imagen interna de un comprimido. |
| `ImageLayer` | imagen, posición, texto/estilo, visibilidad | Capa del editor avanzado. |
| `StlMeshBuilder` | — | Utilidad: `TriangleMesh`, escala, caja delimitadora, centro, centroide. |

### 6.2 Candidato de escaneo (`servicios.renderer.Zip2PngScanner.RenderCandidate`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `path` | `Path` | Ruta del primer archivo del grupo. |
| `nombreBase` | `String` | Nombre base sin extensiones ni sufijos de volumen. |
| `tamanoBytes` | `long` | Tamaño total acumulado. |
| `esComprimido` | `boolean` | Indica si es ZIP/RAR/7z. |
| `excedeLimite` | `boolean` | Supera el límite configurado. |
| `volumenesAgrupados` | `List<Path>` | Volúmenes del multivolumen. |
| `imagenesInternas` | `List<ImageEntry>` | Imágenes embebidas (poblado bajo demanda). |

### 6.3 Claves de configuración

| Clave | Por defecto | Descripción |
|-------|-------------|-------------|
| `zip2png.motor` | `awt` | Motor de renderizado. |
| `zip2png.limite_mb` | `512` | Límite de tamaño de candidato. |
| `zip2png.carpeta_temp` | `%TEMP%/visor_zip2png` | Carpeta temporal de renders. |
| `zip2png.ruta_openscad` | *(vacío)* | Ruta de OpenSCAD. |
| `zip2png.ruta_blender` | *(vacío)* | Ruta de Blender. |

### 6.4 Constantes del motor AWT (`AwtModelRenderer`)

| Constante | Valor | Descripción |
|-----------|-------|-------------|
| `SIZE` | 512 | Resolución de la imagen. |
| `MARGIN` | 24 | Margen del modelo en la imagen. |
| `AMBIENT` | 0.20 | Intensidad de luz ambiental. |
| `LIGHTS` | 3 direccionales | Luces direccionales. |
| `COLOR_RANGE` | 210 | Rango de color del sombreado. |

---

## 7. Apéndices

### 7.1 Apéndice A — Trazabilidad al código fuente

| Módulo | Clases principales |
|--------|--------------------|
| Escaneo | `servicios/renderer/Zip2PngScanner.java` |
| Extracción/listado | `servicios/renderer/ZipExtractor.java` |
| Parseo STL | `servicios/renderer/StlParser.java` |
| Renderizado software | `servicios/renderer/AwtModelRenderer.java`, `servicios/renderer/ModelRenderer.java` |
| Visor 3D | `vista/panels/render/PreviewPanel3DFX.java`, `modelo/renderer/StlMeshBuilder.java` |
| Panel de trabajo | `vista/panels/render/RenderPanel.java`, `vista/panels/render/RenderListCellRenderer.java` |
| Editor avanzado (integración) | `vista/panels/render/AdvanceEditPanel.java`, `CanvasPanel.java`, `EditorComponentBar.java`, `LayerCardPanel.java`, `LayerCard.java` |
| Editor avanzado (especificación completa) | `SRS-ModoEditor.md` (modelos en `modelo/editor`, documento en `servicios/editor`, herramientas en `controlador/tools`) |
| Worker de procesado | `controlador/worker/Zip2PngWorker.java` |
| Controlador | `controlador/RenderController.java` |
| Configuración | `vista/configuracion/panels/Zip2PngConfigPanel.java`, `servicios/ConfigKeys.java` |
| Integración | `controlador/services/AppModeService.java`, `vista/builders/ViewBuilder.java`, `controlador/services/ActionFactory.java`, `controlador/utils/ComponentRegistry.java` |
| Temporales | `servicios/renderer/RenderTempFileManager.java` |
| Ayuda | `resources/help/render.html`, `resources/help/herramientas.html` |

### 7.2 Apéndice B — Glosario técnico

- **Motor AWT:** renderizador por software implementado sobre AWT/Swing que proyecta triángulos 3D sobre un `BufferedImage` con iluminación y SSAO.
- **SSAO:** técnica de oclusión ambiental por aproximación en espacio de pantalla que oscurece los huecos y aristas.
- **Filmstrip:** tira horizontal de miniaturas para navegar los contenidos de un comprimido.

---

*Fin del documento.*
