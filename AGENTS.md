# Visor de Imágenes V2

Aplicación Swing (Java 21) para visualización, catalogación y exportación de imágenes con modo cliente.

## Quick start

```bash
mvn compile          # compilar
mvn package          # empaquetar fat JAR en D:/Descargas/VisorV2/VisorV2.jar
```

**Entrypoint:** `principal.VisorV2.main()` → programa `AppInitializer` en EDT.

## Arquitectura

- **MVC plano** (no Spring): `controlador/` → `modelo/` → `vista/`, más `servicios/`
- `controlador.managers.*` → managers de estado y sub-sistemas
- `controlador.services.*` → servicios auxiliares
- `controlador.factory.ActionFactory` → fábrica de Actions (botones, menús)
- `controlador.utils.ComponentRegistry` → DI manual (singletons por clave String: `"frame.principal"`)
- `vista.builders.*Builder` → construyen paneles y toolbars

## Modos de trabajo

`VisorModel.WorkMode` enum: `VISUALIZADOR`, `PROYECTO`, `CLIENTE`, `DATOS`, `CARRUSEL`

## Paquetes clave

| Paquete | Rol |
|---------|-----|
| `modelo.proyecto` | `ProjectModel` + `ClientSelection` (estados, checkboxes, comentarios) |
| `servicios.cliente` | Exportación HTML (`WebCatalogExporter`), importación respuesta, sincronización |
| `controlador` | `ClientController`, `ProjectController`, `GeneralController` |
| `vista.panels` | `CheckboxEditorMouseHandler`, `CheckboxEditorPanel`, `ClientReviewPanel` |

## Modelo Cliente

- `SelectionState` enum: `SELECTED`, `DISCARDED`, `UNDEFINED` (tristate)
- `ImageCheckboxOverlay` → checkbox con posición (x,y), estado, código, precio, comentario, tamaño
- `CommentOverlay` → comentario superpuesto con texto + posición (x,y)
- `ClientSelection` inner class en `ProjectModel` → mapas de estados, comentarios, overlays
- **Claves compuestas:** `imgCode + "_" + cbCode` (ej. `C001_cb01`) para asociar checkbox a imagen

## Exportación HTML cliente (nuevo)

- `WebCatalogExporter.exportarHtmlCliente()` genera HTML único con miniaturas base64 + JS/CSS inline
- Calidad adaptativa: ≤20 imgs → 800px/85%, ≤50 → 600px/75%, >50 → 400px/60%
- Galería JS: cada tarjeta muestra **código + nombre + tristate clickeable** → **comentario centrado** → **imagen (180px cover)**
- El JS tristate en la galería cicla `SELECTED → DISCARDED → UNDEFINED` sin abrir modal
- Overlays en modal posicionados sobre la imagen con zoom/pan/pinch
- El HTML se guarda como archivo único descargable por el cliente
- El JS `buildResponse()` genera `checkboxes[]` con `codigo` + `estado` por cada imagen
- `ClientResponseImporter.importarDesdeString()` ahora procesa `checkboxes[]` y actualiza los `ImageCheckboxOverlay` correspondientes (busca por `codigo`, fallback por índice)

## Gotchas

- **JS `%` en text blocks:** el operador módulo JS (ej. `(i+1) % length`) debe escaparse como `%%` porque Java `.formatted()` lo interpreta como formato `%e`. Ver `getClientJs()`.
- **Gson + enums anidados:** Gson deserializa `Map<String, SelectionState>` correctamente si el campo tiene tipo genérico completo. El `module-info.java` debe `opens modelo.proyecto to com.google.gson`.
- **ProjectManager.gson** usa `disableHtmlEscaping()` para preservar barras `\` en rutas Windows.
- `.proyectos/*.prj` y `config.cfg` están en `.gitignore` (datos locales).
- `config.cfg` tiene 246+ claves definidas en `ConfigKeys.java`.
- **FlatLaf temas:** los temas personalizados se guardan en `.temas_personalizados/`.

## Herramientas

- **Thumbnailator** 0.4.20 → generar miniaturas con `Thumbnails.of(...).crop(Positions.CENTER)`
- **TwelveMonkeys** 3.10.1 → soporte JPEG CMYK, TIFF, PSD, WebP, TGA, PCX, BMP
- **SLF4J + Logback** → logging (salida a `log.txt` por configuración)
- **SQLite + JDBI** → base de datos embebida (futuro catalogador)

## Reglas de formato y documentación Java

### Documentación

- Generar JavaDoc para todas las clases y métodos **públicos**.
- Para clases y métodos **no públicos**, añadir un comentario descriptivo antes de la declaración explicando su propósito.
- Evitar comentarios redundantes que simplemente repitan el nombre del método o clase.

### Espaciado

- Exactamente **2 líneas en blanco** entre métodos de una misma clase.
- Una línea en blanco entre bloques lógicos dentro de un método para mejorar legibilidad.

### Comentarios de cierre

- Al cierre de cada método: `} // --- Fin del metodo <nombreMetodo> ---`
- Al cierre de cada clase: `} // --- Fin de la clase <nombreClase> ---`

### Imports

- Eliminar imports no utilizados.
- No generar imports redundantes o innecesarios.

### Legibilidad

- Priorizar claridad y mantenibilidad del código.

## Commits

Estilo descriptivo en español, con prefijo de fase o área cuando aplica. Ej:
```
Exportación HTML único cliente + tristate overlays + galería reestructurada
Fase 0: Fundación modelo de datos + toolbars cliente
refactor: Renombrado WorkMode.EDICION a CLIENTE (Modo Cliente)
```
