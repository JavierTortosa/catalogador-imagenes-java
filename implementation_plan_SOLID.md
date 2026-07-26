# Plan de Reparación Exhaustivo — Aplicación de Principios SOLID

Este plan detalla los pasos estructurados por fases para resolver los incumplimientos de los principios SOLID identificados en la auditoría de **VisorImagenes V2** ([informe_principios_solid.md](file:///C:/Users/ameri/.gemini/antigravity-ide/brain/baac0a83-098a-4c4d-874f-ffc1ba829302/informe_principios_solid.md)).

---

## Revisión Requerida por el Usuario

> [!IMPORTANT]
> **Estrategia de Refactorización Progresiva (Sin Romper la Aplicación)**:
> Debido a que la base de código sobrepasa las 40.000 líneas y se trata de un sistema de producción, las refactorizaciones se organizan en **4 Fases Incrementales**. Cada fase mantiene la aplicación en estado totalmente funcional y ejecutable en todo momento.
> No se realizará ningún commit ni compilación sin la aprobación explícita previa del usuario (siguiendo las reglas de `AGENTS.md`).

---

## Fases y Cambios Propuestos

---

### FASE 1: Inversión de Dependencias (DIP) y Registro Seguro (LSP)

**Objetivo:** Eliminar el acoplamiento directo a implementaciones concretas y arreglar la inseguridad de tipos en el Service Locator estático.

#### Problema que soluciona:
- **DIP:** Eliminación del antipatrón Service Locator sin inyección explícita y acoplamiento directo a servicios concretos (`WebCatalogExporter`, `ProjectManager`, `IndexationService`, `ArchiveAnalysisService`).
- **LSP:** Eliminación de los casts `@SuppressWarnings("unchecked")` inseguros por claves String en `ComponentRegistry`.

---

#### [NEW] [IServices.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/interfaces/IProjectService.java)
- Crear interfaces bien definidas para los servicios del sistema:
  - `IProjectService` (abstrae `ProjectManager`)
  - `ICatalogExporter` (abstrae `WebCatalogExporter`)
  - `IArchiveValidator` (abstrae `ArchiveAnalysisService`)
  - `IIndexationService` (abstrae `IndexationService`)

#### [MODIFY] [ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java)
- Introducir registro fuertemente tipado mediante `ComponentKey<T>` o métodos sobrecargados `register(Class<T> type, T instance)` y `get(Class<T> type)` para garantizar la seguridad de tipos en tiempo de compilación y evitar `ClassCastException` (solución LSP).
- Mantener compatibilidad previa mediante getters tipados explícitos (`getFramePrincipal()`, `getPanelCentral()`).

#### [MODIFY] [AppInitializer.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/AppInitializer.java)
- Inyectar las instancias de servicios abstractos en los controladores principales durante el arranque mediante Constructor Injection en lugar de instanciarlos internamente con `new`.

---

### FASE 2: Descomposición de Clases "God Object" (SRP)

**Objetivo:** Dividir los controladores gigantes y servicios monolíticos en clases delegadas de responsabilidad única.

#### Problema que soluciona:
- **SRP:** `DataController` (~126 KB), `ProjectController` (~135 KB), `VisorController` (~113 KB), `RenderController` (~90 KB) y `WebCatalogExporter` (~104 KB) acumulan múltiples razones para cambiar (E/S, renderizado, eventos de UI, lógica de negocio).

---

#### [MODIFY] [WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java)
#### [NEW] [CatalogImageProcessor.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/CatalogImageProcessor.java)
#### [NEW] [HtmlTemplateRenderer.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/HtmlTemplateRenderer.java)
#### [NEW] [CatalogFileWriter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/CatalogFileWriter.java)
- Extraer la lógica de escalado/procesamiento de imágenes a `CatalogImageProcessor`.
- Extraer la generación de plantilla HTML/CSS/JS a `HtmlTemplateRenderer`.
- Extraer la escritura de archivos al sistema de archivos a `CatalogFileWriter`.
- `WebCatalogExporter` actuará como un orquestador ligero de alto nivel.

#### [MODIFY] [DataController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/DataController.java)
#### [NEW] [FolderTreeHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/handlers/FolderTreeHandler.java)
#### [NEW] [TaggingHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/handlers/TaggingHandler.java)
#### [NEW] [DatabaseSyncHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/handlers/DatabaseSyncHandler.java)
- Separar la manipulación del árbol de directorios a `FolderTreeHandler`.
- Separar la gestión de etiquetas del usuario/sistema a `TaggingHandler`.
- Separar la sincronización con SQLite a `DatabaseSyncHandler`.

#### [MODIFY] [ProjectController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ProjectController.java)
#### [NEW] [ArchiveIntegrityHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/handlers/ArchiveIntegrityHandler.java)
#### [NEW] [ProjectExportImportHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/handlers/ProjectExportImportHandler.java)
- Delegar la comprobación de archivos ZIP/7Z/RAR a `ArchiveIntegrityHandler`.
- Delegar la importación/exportación de respuestas de clientes a `ProjectExportImportHandler`.

#### [MODIFY] [VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java)
#### [NEW] [ClientModeState.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/estados/ClientModeState.java)
#### [NEW] [DataModeState.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/estados/DataModeState.java)
#### [NEW] [CarouselModeState.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/estados/CarouselModeState.java)
- Descomponer el modelo monolítico extrayendo submódulos de estado especializados para los modos CLIENTE, DATOS y CARRUSEL.

---

### FASE 3: Apertura a la Extensión y Patrón Estrategia (OCP)

**Objetivo:** Eliminar la rigidez del código provocada por sentencias condicionales `switch` sobre Enums (`WorkMode`, `SelectionState`, `DisplayMode`).

#### Problema que soluciona:
- **OCP:** Modificar o extender modos de trabajo, estados de selección o formatos de exportación actualmente requiere tocar múltiples clases en todo el proyecto.

---

#### [NEW] [WorkModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/WorkModeStrategy.java)
- Definir la interfaz `WorkModeStrategy` con los métodos del ciclo de vida del modo: `activar()`, `desactivar()`, `configurarToolbar()`, `obtenerPanelPrincipal()`.

#### [NEW] [VisualizadorModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/VisualizadorModeStrategy.java)
#### [NEW] [ProyectoModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/ProyectoModeStrategy.java)
#### [NEW] [DatosModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/DatosModeStrategy.java)
#### [NEW] [ClienteModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/ClienteModeStrategy.java)
- Implementar estrategias concretas para cada uno de los modos de trabajo existentes.

#### [MODIFY] [GeneralController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/GeneralController.java)
#### [MODIFY] [ToolbarManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/ToolbarManager.java)
#### [MODIFY] [ViewManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/ViewManager.java)
- Reemplazar las cadenas `switch(currentWorkMode)` por delegación polimórfica a la instancia de `WorkModeStrategy` activa.

#### [MODIFY] [ActionFactory.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionFactory.java)
#### [NEW] [ActionRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionRegistry.java)
- Convertir la fábrica rígida en un registro de acciones dinámico y extensible donde los componentes pueden registrar nuevas `Action` sin modificar la fábrica.

---

### FASE 4: Segregación de Interfaces (ISP)

**Objetivo:** Dividir interfaces monolíticas de listeners y restringir las vistas expuestas a cada cliente.

#### Problema que soluciona:
- **ISP:** Clientes obligados a implementar métodos vacíos de eventos que no utilizan.

---

#### [MODIFY] [MasterListChangeListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/MasterListChangeListener.java)
#### [NEW] [ItemListSelectionListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/listeners/ItemListSelectionListener.java)
#### [NEW] [ListContentsChangeListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/listeners/ListContentsChangeListener.java)
- Segregar `MasterListChangeListener` en interfaces funcionales independientes para cambios de selección y cambios de contenido.

#### [MODIFY] [ProjectStateListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/ProjectStateListener.java)
- Dividir en `ProjectSaveListener`, `ProjectLoadListener` e `IntegrityCheckListener`.

---

## Plan de Verificación

### Pruebas Automatizadas (Maven)
- Ejecutar compilación del proyecto para verificar sintaxis y contratos:
  ```bash
  mvn clean compile
  ```
- Ejecutar empaquetado Fat JAR y verificar la correcta inclusión de dependencias JavaFX/TwelveMonkeys:
  ```bash
  mvn package
  ```

### Verificación Manual de Flujos de Usuario
1. **Modo VISUALIZADOR:** Verificar la carga rápida de miniaturas, navegación, atajos de teclado y funcionamiento del carrusel.
2. **Modo DATOS:** Comprobar la selección en árbol de carpetas y asignación de etiquetas sin afectar al contexto del visualizador.
3. **Modo PROYECTO:** Crear un nuevo proyecto, ejecutar análisis de integridad de compresos (ZIP/7Z/RAR) y verificar overlays de tristate.
4. **Modo CLIENTE:** Generar exportación HTML (`WebCatalogExporter`), comprobar interactividad del catálogo Web de cliente en navegador e importar de vuelta las respuestas del cliente (`ClientResponseImporter`).

---
*Plan de reparación redactado respetando la norma de no compilar ni realizar commits sin autorización explícita.*
