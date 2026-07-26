# Plan Maestro de Patrones de Diseño (GoF) — Reparación e Implementación

Este plan se divide en dos secciones principales basadas en el informe de auditoría de patrones ([informe_patrones_diseno.md](file:///C:/Users/ameri/.gemini/antigravity-ide/brain/baac0a83-098a-4c4d-874f-ffc1ba829302/informe_patrones_diseno.md)):

1. **Sección 1: Reparación Exhaustiva de Errores y Antipatrones Existentes.**
2. **Sección 2: Plan de Implementación de Nuevos Patrones Priorizado por Relevancia.**

---

## Revisión Requerida por el Usuario

> [!IMPORTANT]
> **Norma de Seguridad Arquitectónica**:
> Ningún cambio alterará el comportamiento externo del programa. El sistema mantendrá su funcionalidad completa durante todas las etapas. No se compilará ni se realizará ningún commit sin la confirmación explícita del usuario (`AGENTS.md`).

---

# SECCIÓN 1: Plan de Reparación de Errores y Antipatrones Existentes

### 1.1 Corrección del Antipatrón Service Locator (`ComponentRegistry`)

* **Problema que arregla:** 
  * En [ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java), el uso de un mapa estático global con claves `String` arbitrarias enmascara las dependencias de los controladores y rompe la seguridad de tipos (`ClassCastException` en runtime debido a `@SuppressWarnings("unchecked")`).
* **Clases a Modificar / Crear:**
  * [MODIFY] [ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java): Introducir llaves tipadas `ComponentKey<T>` y mapa `Map<Class<T>, T>`. Eliminar el método genérico inseguro.
  * [MODIFY] [AppInitializer.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/AppInitializer.java): Inyectar referencias explícitas a los componentes GUI requeridos en los controladores en lugar de hacer lecturas estáticas a través del registro.

---

### 1.2 Desacoplamiento de la God Factory (`ActionFactory`)

* **Problema que arregla:** 
  * En [ActionFactory.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionFactory.java) (142 KB), existe una fábrica monolítica rígida que instancian docenas de acciones anónimas acopladas directamente a controladores globales.
* **Clases a Modificar / Crear:**
  * [MODIFY] [ActionFactory.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionFactory.java): Transformar la fábrica monolítica estática en fábricas por módulo (`VisualizadorActionFactory`, `ProjectActionFactory`, `DataActionFactory`).
  * [NEW] [ActionRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionRegistry.java): Registro extensible donde las acciones se registran bajo demanda.

---

### 1.3 Saneamiento de la Violación de MVC en `VisorModel` y Handlers

* **Problema que arregla:** 
  * [VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java) contiene variables de la interfaz gráfica (`isProjectExportPanelVisible`, `clienteCheckboxVisible`). Además, handlers como `CheckboxEditorMouseHandler` modifican directamente el modelo sin pasar por la capa de controladores.
* **Clases a Modificar / Crear:**
  * [MODIFY] [VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java): Extraer banderas de la interfaz visual fuera del modelo de dominio.
  * [MODIFY] [CheckboxEditorMouseHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/CheckboxEditorMouseHandler.java): Reorientar las interacciones de ratón para que invoquen a `ProjectController` / `ClientController` en lugar de mutar el modelo directamente.

---

### 1.4 Aislamiento de Ámbito en Observadores (`Observer`)

* **Problema que arregla:** 
  * `VisorModel` notifica eventos a listeners globales sin validar el modo de trabajo activo (`WorkMode`). Esto causaba que eventos en modo DATOS afectaran el estado del VISUALIZADOR.
* **Clases a Modificar / Crear:**
  * [MODIFY] [VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java): Filtrar la emisión de eventos para notificar únicamente a los listeners pertenecientes al `WorkMode` activo.
  * [MODIFY] [MasterListChangeListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/MasterListChangeListener.java): Añadir el contexto activo como parámetro de la notificación (`onListChanged(ListContext context)`).

---

### 1.5 Corrección de Pseudo-Builders en Capa Vista

* **Problema que arregla:** 
  * Clases en `vista.builders.*` ([ViewBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ViewBuilder.java), [ToolbarBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ToolbarBuilder.java)) utilizan el sufijo `Builder` pero son clases procedimentales con acoplamiento lateral estático.
* **Clases a Modificar / Crear:**
  * [MODIFY] `vista.builders.*`: Convertir los generadores procedimentales estáticos en Component Factories declarativas con inyección de parámetros explícita.

---

# SECCIÓN 2: Plan de Implementación de Nuevos Patrones (Priorizado)

---

### 2.1 Patrón Strategy (Estrategia) para Modos de Trabajo (`WorkModeStrategy`) — **[IMPRESCINDIBLE N°1]**

* **Qué soluciona:** 
  * Elimina todas las sentencias condicionales `switch(currentWorkMode)` en `GeneralController`, `ToolbarManager`, `ViewManager` y `VisorController`. Permite agregar un nuevo modo de trabajo creando una sola clase sin tocar controladores existentes.
* **Ventajas:**
  * Cumplimiento estricto del Principio Open/Closed (OCP).
  * Código altamente modular y extensible.
* **Inconvenientes:**
  * Ligero incremento en el número de clases (`+6` clases de estrategia).
* **Clases a Crear / Modificar:**
  * [NEW] [WorkModeStrategy.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/strategies/WorkModeStrategy.java) (Interfaz base)
  * [NEW] `VisualizadorModeStrategy`, `ProyectoModeStrategy`, `DatosModeStrategy`, `ClienteModeStrategy`, `CarouselModeStrategy`, `RenderModeStrategy`
  * [MODIFY] [GeneralController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/GeneralController.java)
  * [MODIFY] [ToolbarManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/ToolbarManager.java)
  * [MODIFY] [ViewManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/ViewManager.java)

---

### 2.2 Patrón Inyección de Dependencias (DI / Constructor Injection) — **[IMPRESCINDIBLE N°2]**

* **Qué soluciona:** 
  * Desacopla completamente los controladores principales de las implementaciones concretas de servicios e infraestructura (`ProjectManager`, `WebCatalogExporter`, `IndexationService`).
* **Ventajas:**
  * Seguridad de tipos estática en compilación.
  * Permite crear pruebas unitarias rápidas utilizando objetos Mock o Fake.
* **Inconvenientes:**
  * Los constructores de los controladores requieren más parámetros explícitos durante el arranque en `AppInitializer`.
* **Clases a Crear / Modificar:**
  * [NEW] `IProjectService`, `ICatalogExporter`, `IArchiveValidator`, `IIndexationService`
  * [MODIFY] [AppInitializer.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/AppInitializer.java)
  * [MODIFY] [ProjectController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ProjectController.java)
  * [MODIFY] [DataController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/DataController.java)

---

### 2.3 Patrón Strategy para Exportación de Catálogos (`CatalogExporterStrategy`) — **[MUY IMPORTANTE N°3]**

* **Qué soluciona:** 
  * Desacopla la lógica de exportación monolítica en [WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java), permitiendo alternar entre formato HTML interactivo, PDF estático o exportaciones compactas JSON/ZIP.
* **Ventajas:**
  * Aísla la generación de plantillas web y formateo de archivos.
  * Permite añadir nuevos formatos de exportación sin tocar el núcleo del sistema de clientes.
* **Inconvenientes:**
  * Requiere extraer la lógica de renderizado HTML fuera de Java Text Blocks hacia clases formateadoras especializadas.
* **Clases a Crear / Modificar:**
  * [NEW] `ICatalogExporterStrategy.java`
  * [NEW] `HtmlCatalogExporterStrategy.java`, `PdfCatalogExporterStrategy.java`
  * [MODIFY] [WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java)

---

### 2.4 Patrón Command (Comando con Deshacer / Rehacer) — **[RELEVANTE N°4]**

* **Qué soluciona:** 
  * Otorga capacidad de **Deshacer (Undo) / Rehacer (Redo)** en operaciones críticas del usuario (ej. marcado tristate de clientes, asignación masiva de etiquetas, borrado/renombrado de activos).
* **Ventajas:**
  * Mejora drástica en la experiencia de usuario (UX) previniendo errores accidentales.
  * Aísla la ejecución de la acción de su origen UI.
* **Inconvenientes:**
  * Requiere almacenar el estado previo en una pila de deshacer (`CommandHistory`), aumentando ligeramente el uso de memoria RAM.
* **Clases a Crear / Modificar:**
  * [NEW] `ICommand.java`, `UndoableCommand.java`, `CommandHistory.java`
  * [NEW] `TagAssignmentCommand`, `ClientSelectionCommand`, `RenameFileCommand`
  * [MODIFY] [GlobalInputManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/GlobalInputManager.java)
  * [MODIFY] [KeyboardShortcutManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/KeyboardShortcutManager.java)

---

### 2.5 Patrón State (Estado) para Tristate de Selección — **[RELEVANTE N°5]**

* **Qué soluciona:** 
  * Encapsula la lógica de conmutación tristate (`SELECTED` $\rightarrow$ `DISCARDED` $\rightarrow$ `UNDEFINED` $\rightarrow$ `SELECTED`) en objetos de estado en lugar de usar sentencias `switch` dispersas en JavaScript, Java Swing y el serializador JSON.
* **Ventajas:**
  * Cada objeto de estado conoce su propio icono, color CSS/Swing y siguiente transición.
  * Elimina condicionales redundantes.
* **Inconvenientes:**
  * Requiere adaptar el formateador JSON de Gson para guardar el estado simple en disco.
* **Clases a Crear / Modificar:**
  * [NEW] `SelectionStateObject.java`, `SelectedState.java`, `DiscardedState.java`, `UndefinedState.java`
  * [MODIFY] [SelectionState.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/proyecto/SelectionState.java)
  * [MODIFY] [CheckboxEditorPanel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/CheckboxEditorPanel.java)

---

### 2.6 Patrón Chain of Responsibility (Cadena de Filtros de Imagen) — **[UTIL N°6]**

* **Qué soluciona:** 
  * Descompone el método monolítico de filtrado de imágenes en [FilterManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/FilterManager.java) en pasos de cadena reutilizables e independientes.
* **Ventajas:**
  * Facilita añadir nuevos tipos de filtro (ej. por resolución, por fecha, por extensión 3D).
  * Reduce la complejidad ciclomática.
* **Inconvenientes:**
  * Ligera sobrecarga en la iteración de la cadena sobre colecciones grandes (>40.000 imágenes).
* **Clases a Crear / Modificar:**
  * [NEW] `ImageFilterStep.java`, `TextFilterStep`, `TagFilterStep`, `FolderFilterStep`, `ClientSelectionFilterStep`
  * [MODIFY] [FilterManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/FilterManager.java)

---

## Verificación del Plan

- Compilación con Maven para validar que no se rompen contratos ni interfaces:
  ```bash
  mvn clean compile
  ```
- Empaquetado ejecutable y pruebas funcionales de regresión en los 5 modos de trabajo.

---
*Plan de patrones redactado respetando las directivas del sistema.*
