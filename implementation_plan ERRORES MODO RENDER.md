# Plan de Implementación — Modo RENDER

## Descripción

El Modo RENDER es un creador de imágenes descriptivas para archivos comprimidos (ZIP/RAR/7Z) que
contienen modelos 3D. Su objetivo es que cada archivo comprimido tenga una imagen representativa
(PNG con el mismo nombre base) que puede obtenerse de dos fuentes: imágenes ya embebidas dentro
del propio comprimido, o renderizado de uno de los STL que contiene.

---

## Análisis del Estado Actual

### ✅ Implementado y funcionando

| Funcionalidad | Clase/Método |
|---|---|
| Escaneo de carpeta y cribado imagen/no imagen | `Zip2PngScanner.scanFolder()` |
| Detección de imágenes embebidas en ZIPs | `Zip2PngScanner.detectarImagenesEnArchivos()` |
| Distribución en 2 pestañas (Sin renderizar / Con imagen) | `RenderPanel` – `candidateTabs` |
| Panel inferior split: lista STLs ó lista imágenes | `bottomCardPanel` (CardLayout `stl`/`img`) |
| Doble clic en STL → preview 3D | `RenderController.onStlDoubleClick()` |
| Doble clic en imagen → preview 2D | `RenderController.onImageDoubleClick()` |
| Generación masiva: renderiza STL más pesado de cada ZIP | `Zip2PngWorker.doInBackground()` |
| Generación masiva: extrae primera imagen embebida de cada ZIP | `Zip2PngWorker` – bucle `tieneImagenesDentro()` |
| Grid central con thumbnails (renders + imágenes extraídas) | `RenderController.refreshThumbnails()` |
| Clic en thumbnail del grid → carga en preview | `RenderController.showPreview()` |
| Ajustes de orientación 3D (órbita mouse, zoom) | `PreviewPanel3DFX` |
| Ajustes brillo, contraste, antialiasing, fondo | `RenderController.applyAdjustments()` |
| Fondos: sólido, degradado, imagen de fondo, transparente | `RenderPanel` + `RenderController.syncBackgroundToPreview()` |
| Escala de imagen de fondo | `bgImageScaleSlider` |
| Modo collage (capas de imagen) | `RenderController.toggleCollageMode()` |
| Gestión de capas: añadir, eliminar, reordenar, visibilidad | `RenderController.capaAlFrente/Fondo/SubirBajar()` |
| Filmstrip / galería de contenido del ZIP | `RenderController.loadGalleryContent()` |
| Galería renderiza STLs progresivamente (placeholder → render real) | `galleryWorker` con `SwingWorker` |
| Asignar preview al grid central | `RenderController.asignarPreviewAlArchivo()` |
| Copiar PNGs generados a carpeta destino | `RenderController.copiarArchivos()` |
| Descargar preview como PNG | `RenderController.descargarPreview()` |
| Caché de triángulos con SoftReference | `triangleCache` |
| Cancelación de worker anterior al cambiar selección | `currentTriangleWorker.cancel(true)` |

---

## Gaps Detectados — Lo Que Falta Implementar

> [!IMPORTANT]
> Las siguientes funcionalidades están descritas en el spec pero **no están implementadas** o están
> **parcialmente implementadas**. Se ordenan por prioridad de mayor a menor impacto.

---

### GAP 1 — Fase 1: el escaneo no busca imágenes FUERA del ZIP con mismo nombre base

**Problema:** `Zip2PngScanner.scanFolder()` busca archivos comprimidos sin imagen asociada. Compara
el `nombreBase` del comprimido con imágenes en la misma carpeta. Sin embargo, la comparación es
`imagenesEncontradas.contains(base)` usando sólo el primer punto como delimitador (`nombreBase`
toma `substring(0, indexOf('.'))`). Si el archivo es `modelo.parte1.rar`, su base sería `modelo` y
nunca encontraría `modelo.png`. Casos multivolumen pueden fallar en este sentido.

**Impacto:** Archivos que ya tienen imagen pueden aparecer como "sin renderizar".

**Clases a modificar:**
- [`Zip2PngScanner.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/Zip2PngScanner.java) — método `nombreBase()` y `claveGrupo()`

---

### GAP 2 — Fase 1: no hay selección de archivo individual para escanear (solo carpeta)

**Problema:** `onScan()` en `RenderController` sólo abre un `JFileChooser` en modo
`DIRECTORIES_ONLY`. El spec indica que se puede escanear "carpeta/archivo". No hay opción para
seleccionar un único archivo comprimido.

**Clases a modificar:**
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — `onScan()` (añadir modo `FILES_AND_DIRECTORIES`)
- [`Zip2PngScanner.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/Zip2PngScanner.java) — `scanFolder()` → añadir `scanFile(Path)` para un único archivo

---

### GAP 3 — Fase 2: el grid central NO muestra simultáneamente render + primera imagen interna

**Problema:** El spec indica que la Fase 2 (generación masiva) debe generar dos resultados por
candidato: la primera imagen de cada ZIP **y** el render del STL más pesado. El `Zip2PngWorker`
sí genera ambos, pero la rejilla de thumbnails los separa en dos paneles (`rendersGrid` e
`imagenesGrid`) gestionados por una pestaña. El usuario no puede ver render e imagen del mismo
candidato lado a lado; debe cambiar entre las pestañas "Sin renderizar" / "Con imagen".

**Impacto:** Dificulta comparar render vs. imagen interna para elegir cuál usar.

**Opciones de solución:**
- A) Mostrar ambos en la misma tarjeta del grid (render arriba, imagen abajo)
- B) Mantener separados pero añadir botón "Ver comparativa" que los muestre en split

**Clases a modificar:**
- [`RenderPanel.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/render/RenderPanel.java) — diseño del `gridCardPanel`
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — `refreshThumbnails()`

---

### GAP 4 — Fase 3: no existe un flujo explícito de "sustitución de imagen" en el grid

**Problema:** El spec dice que en la Fase 3, una vez terminada la edición, se puede "sustituir la
imagen del panel de preview por la imagen correspondiente del grid central". Existe
`asignarPreviewAlArchivo()` que sobrescribe el PNG del candidato en `outputDir`, pero:

1. No hay feedback visual claro de qué thumbnail del grid queda "asignado" como definitivo.
2. No hay distinción entre un PNG provisional (recién generado) y uno aprobado (editado y guardado).
3. No se actualiza el estado del candidato (sigue apareciendo en "Sin renderizar" aunque ya tenga
   un render bueno asignado).

**Clases a modificar:**
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — `asignarPreviewAlArchivo()` + nuevo concepto de estado "aprobado"
- [`RenderCandidate`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/Zip2PngScanner.java) — añadir campo `aprobado: boolean` o similar
- [`RenderPanel.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/render/RenderPanel.java) — `RenderListCellRenderer` para mostrar badge/check en candidatos aprobados

---

### GAP 5 — Fase 1: panel inferior no muestra lista de STLs en la pestaña "Con imagen"

**Problema:** Cuando se selecciona un candidato en la pestaña "Con imagen", el panel inferior
muestra la lista de imágenes del ZIP (`contentImageList`). Sin embargo, el spec dice:
> "si el archivo comprimido tiene imágenes, en la parte de abajo se muestran todas las imágenes
> (es muy habitual encontrar archivos comprimidos que tienen una imagen para cada archivo STL, por ej)"

Esto está implementado. Sin embargo, el spec también indica que haciendo doble clic en un elemento
de la lista de abajo se muestra en el preview. Para la pestaña "Con imagen" esto funciona con
`onImageDoubleClick()`. Para la pestaña "Sin renderizar" funciona con `onStlDoubleClick()`. 
**Lo que falta** es que en la pestaña "Sin renderizar", el panel inferior muestra los STLs pero
el usuario no tiene contexto de qué STL corresponde a qué render del grid (sin correlación visual
nombre→thumbnail).

**Clases a modificar:**
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — `actualizarContenidoInferior()`, añadir sincronización bidirecional con la filmstrip
- [`RenderPanel.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/render/RenderPanel.java) — renderer de la lista STL para resaltar el STL seleccionado en el filmstrip

---

### GAP 6 — Modo collage: no se puede añadir imagen desde cero (imagen en blanco)

**Problema:** El spec dice: "podemos quitar todo y añadir una imagen desde cero para crear un png
nuevo". Existe `cleanAndAddLayer()` que limpia capas y abre selector de archivo. Pero no hay opción
para crear un lienzo en blanco de dimensiones configurables (para montar una imagen corporativa, p.ej.).

**Clases a modificar:**
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — nuevo método `crearLienzoeEnBlanco()`
- Nuevo diálogo de configuración de tamaño/color del lienzo

---

### GAP 7 — No hay integración entre el render generado y la carpeta original de los ZIPs

**Problema:** El flujo termina generando PNGs en `outputDir` (carpeta temporal). Hay `copyToSource()`
que copia el PNG generado a la carpeta del ZIP original, pero:

1. No copia automáticamente al finalizar la edición (es un paso manual).
2. No verifica si ya existe un PNG con ese nombre en la carpeta origen antes de copiar (puede
   sobreescribir sin advertir).
3. No hay ningún indicador en la lista de candidatos de que ese ZIP ya tiene un PNG en la carpeta
   origen (diferente del PNG en `outputDir`).

**Clases a modificar:**
- [`RenderController.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) — `copyToSource()` + `asignarPreviewAlArchivo()`
- [`Zip2PngScanner.java`](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/Zip2PngScanner.java) — enriquecer `RenderCandidate` con info de existencia de PNG en origen

---

## Preguntas Abiertas

> [!IMPORTANT]
> Antes de implementar los gaps, necesito respuesta a las siguientes preguntas de diseño:

1. **GAP 3 — Layout del grid:** ¿Prefieres la opción A (mostrar render+imagen en la misma tarjeta)
   o la opción B (mantener separados con botón de comparativa)? ¿O quieres mantener el diseño
   actual (dos pestañas separadas)?

2. **GAP 4 — Estado "aprobado":** ¿Debe el candidato "aprobado" desaparecer de la lista
   "Sin renderizar" y moverse a "Con imagen"? ¿O simplemente marcarse con un icono/color en
   la misma lista?

3. **GAP 7 — Copia automática:** Al hacer "Asignar preview al archivo", ¿debe copiarse
   automáticamente el PNG a la carpeta del ZIP original, o prefieres que siga siendo un paso
   manual separado?

4. **Nuevo: Tamaño del render generado:** El `AwtModelRenderer` genera imágenes de tamaño fijo.
   ¿Debería ser configurable (p.ej. 512x512, 1024x1024) o dejar el tamaño actual?

5. **Nuevo: Recorrido de carpetas:** `Zip2PngScanner.scanFolder()` hace un walk de profundidad 2
   (`Files.walk(folderPath, 2)`). ¿Es suficiente o debería ser recursivo sin límite de profundidad?

---

## Orden de Implementación Recomendado

Una vez aprobado el plan, los gaps se implementarían en este orden:

1. **GAP 1** — Arreglo del `nombreBase()` (mínimo, no genera UI)
2. **GAP 2** — Escaneo de archivo único (amplía el selector de carpeta)
3. **GAP 5** — Sincronización STL↔filmstrip (mejora UX sin cambios de arquitectura)
4. **GAP 4** — Estado "aprobado" en candidatos (requiere decisión de diseño)
5. **GAP 7** — Integración con carpeta original (requiere decisión de copia automática)
6. **GAP 3** — Layout del grid comparativo (requiere decisión de diseño)
7. **GAP 6** — Lienzo en blanco (nuevo diálogo, más esfuerzo)

