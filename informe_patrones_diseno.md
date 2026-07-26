# Informe de Auditoría de Patrones de Diseño (GoF)
**Proyecto:** Visor de Imágenes V2 (DAM Desktop System)  
**Rol:** Experto en Patrones de Diseño y Arquitectura de Software  
**Fecha:** Julio 2026  

---

## 1. Patrones de Diseño Actualmente Implementados (y sus Defectos)

### 1. Modelo-Vista-Controlador (MVC)
* **Ubicación:** 
  * Modelo: `modelo.*` ([VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java), `modelo.proyecto.*`)
  * Vista: `vista.*` (`vista.panels.*`, `vista.builders.*`)
  * Controlador: `controlador.*` ([DataController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/DataController.java), [ProjectController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ProjectController.java), [VisorController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/VisorController.java))
* **Errores / Defectos de Implementación:**
  * **Violación de MVC Puro:** Los controladores contienen lógica de renderizado gráfico directa y referencias rígidas a componentes Swing/JavaFX.
  * **Contaminación del Modelo:** `VisorModel` almacena estados de la interfaz visual (ej. `isProjectExportPanelVisible`, `clienteCheckboxVisible`), en lugar de limitar el modelo a los datos del dominio.
  * **By-pass de Vista a Modelo:** Handlers de eventos visuales (ej. `CheckboxEditorMouseHandler`) manipulan directamente el estado interno del modelo sin pasar por el controlador.

---

### 2. Service Locator (Implementación Anti-patrón)
* **Ubicación:** [ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java)
* **Errores / Defectos de Implementación:**
  * **Anti-patrón Service Locator:** Oculta las dependencias de las clases. En lugar de exigir los componentes requeridos en sus constructores, las clases solicitan componentes a un mapa global estático en tiempo de ejecución.
  * **Falta de Seguridad de Tipos (Type-Safety Violation):** El método genérico `<T extends Component> T get(String name)` realiza un casting inseguro `(T) components.get(name)` mediante `@SuppressWarnings("unchecked")`. Si la clave `String` devuelve un tipo inesperado, la aplicación falla en tiempo de ejecución con `ClassCastException`.

---

### 3. Abstract Factory / Factory Method (Implementación Monolítica / Degenerada)
* **Ubicación:** [ActionFactory.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionFactory.java) (~142 KB)
* **Errores / Defectos de Implementación:**
  * **Fábrica Dios Monolítica:** En lugar de definir una interfaz de fábrica extensible o utilizar subclases polimórficas por módulo, `ActionFactory` es una clase concreta gigantesca con decenas de métodos estáticos que instancian clases anónimas `AbstractAction`.
  * **Violación de Acoplamiento:** Cada método de la fábrica manipula directamente el estado interno de múltiples controllers y singletons globales.

---

### 4. Observer (Observador)
* **Ubicación:** 
  * Interfaces: [MasterListChangeListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/MasterListChangeListener.java), `MasterSelectionChangeListener`, [ProjectStateListener.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/ProjectStateListener.java).
  * Sujetos: `VisorModel`, `ProjectModel`.
* **Errores / Defectos de Implementación:**
  * **Falta de Aislamiento de Contexto:** Los observadores notifican eventos sin comprobar el modo de trabajo activo (`WorkMode`). Esto provocó que el cambio de selección en el modo DATOS sobrescribiera el contexto activo del VISUALIZADOR (corrección documentada en *Gotchas*).
  * **Listeners no segregados:** Las interfaces obligan a los subscriptores a implementar múltiples callbacks aunque solo les interese un evento específico.

---

### 5. Pseudo-Builder (Falso Builder)
* **Ubicación:** `vista.builders.*` ([ViewBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ViewBuilder.java), [ToolbarBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ToolbarBuilder.java), `ClientBuilder.java`, `DataBuilder.java`).
* **Errores / Defectos de Implementación:**
  * **Solo nombre de Builder:** Ninguna de estas clases implementa el patrón GoF **Builder** (interfaz fluida `new Builder().withX().build()`). Son clases de utilidad procedimentales con métodos estáticos que instancian paneles preconfigurados y los registran en `ComponentRegistry`.

---

### 6. Singleton / Estado Global Estático
* **Ubicación:** [ConfigurationManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/ConfigurationManager.java), `HelpRegistry`, `ExternalToolsManager`.
* **Errores / Defectos de Implementación:**
  * **Acceso Estático Global Directo:** Las clases acceden a configuraciones mediante llamadas estáticas directas sin permitir la inyección de configuraciones mockeadas para pruebas unitarias.

---

### 7. Adapter (Adaptador) — *Buena Implementación*
* **Ubicación:** `StlMeshBuilder` (adapta listas de triángulos 3D de archivos STL al formato `TriangleMesh` de JavaFX) e `ImageUtils` (adapta decodificadores de TwelveMonkeys a `BufferedImage` de AWT).
* **Estado:** Correctamente aplicado.

---

## 2. Patrones de Diseño que se DEBERÍAN Implementar

| Patrón | Ubicación Propuesta | Problema que Resuelve y Beneficio |
| :--- | :--- | :--- |
| **Strategy (Estrategia)** | `controlador.strategies.*` (`WorkModeStrategy`) | **Problema:** Múltiples bloques `switch(currentWorkMode)` dispersos en controladores.<br>**Beneficio:** Encapsula el comportamiento de cada modo (`VISUALIZADOR`, `DATOS`, `PROYECTO`, `CLIENTE`) en clases independientes. Permitirá añadir nuevos modos sin tocar el código existente. |
| **Strategy (Estrategia de Exportación)** | `servicios.cliente.export.*` (`CatalogExporterStrategy`) | **Problema:** En [WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java), la generación HTML está acoplada al código Java.<br>**Beneficio:** Permite alternar entre exportación HTML interactiva, exportación PDF estática o exportación JSON/ZIP mediante algoritmos intercambiables. |
| **Command (Comando)** | `controlador.commands.*` (`UndoableCommand`) | **Problema:** Atajos de teclado y acciones de menú en `ActionFactory` ejecutan mutaciones directas e irreversibles sobre el modelo.<br>**Beneficio:** Permite implementar un historial de **Deshacer/Rehacer (Undo/Redo)** para operaciones como etiquetado, selección de cliente o renombrado de activos. |
| **State (Estado)** | `modelo.proyecto.SelectionState` | **Problema:** El estado tristate (`SELECTED`, `DISCARDED`, `UNDEFINED`) conmuta mediante cadenas de condicionales `if-else` / `switch` en la interfaz Swing y en el catálogo Web.<br>**Beneficio:** Transiciones de estado encapsuladas en objetos (`SelectedState`, `DiscardedState`, `UndefinedState`) que conocen su color, icono y siguiente transición. |
| **Template Method (Método Plantilla)** | [AbstractListCoordinator.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/AbstractListCoordinator.java) | **Problema:** Los coordinadores de listas (`ListCoordinator`, `ProjectListCoordinator`, `GridCoordinator`) duplican la estructura del algoritmo de carga y ordenación de elementos.<br>**Beneficio:** Define el esqueleto de filtrado, ordenación y actualización en la clase base, permitiendo que las subclases solo personalicen la representación visual. |
| **Chain of Responsibility (Cadena de Responsabilidad)** | `controlador.managers.filter.*` (`ImageFilterStep`) | **Problema:** `FilterManager` aplica filtros combinados (texto, carpeta, etiquetas, selección) en un único método monolítico con alta complejidad ciclomática.<br>**Beneficio:** Encadena manejadores de filtro independientes (ej. `TextFilterStep` $\rightarrow$ `TagFilterStep` $\rightarrow$ `FolderFilterStep`). |
| **Builder (GoF Real)** | `vista.builders.dialogs.*` (`ProjectDialogBuilder`) | **Problema:** Creación compleja de cuadros de diálogo y vistas configurables mediante código procedimental espagueti.<br>**Beneficio:** Construcción paso a paso de vistas y diálogos mediante interfaz fluida (`new DialogBuilder().withTitle(...).withButtons(...).build()`). |
| **Inyección de Dependencias (DI / IoC)** | `controlador.AppInitializer` y constructores | **Problema:** El antipatrón Service Locator ([ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java)) acopla la aplicación a claves de texto estáticas.<br>**Beneficio:** Las dependencias se pasan explícitamente a los constructores, garantizando tipado estático y facilitando tests unitarios aislados. |

---

## 3. Resumen de Errores y Antipatrones Detectados

1. **Service Locator Antipattern:** `ComponentRegistry` oculta dependencias y carece de tipado estático seguro.
2. **God Factory Antipattern:** `ActionFactory` (142 KB) desacata la modularidad de fábricas GoF.
3. **Model Contamination (MVC Corrompido):** `VisorModel` mezcla datos del dominio con banderas de la interfaz de usuario.
4. **Pseudo-Builders:** La denominación `*Builder` en `vista.builders` confunde el patrón procedural de montaje de GUI con el patrón GoF Builder.
5. **Context Bleed en Observer:** `VisorModel` notifica a observadores globales sin filtrar el ámbito o modo de trabajo activo.

---
*Informe arquitectónico de Patrones de Diseño finalizado.*
