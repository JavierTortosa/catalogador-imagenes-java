# Informe de Auditoría de Principios SOLID
**Proyecto:** Visor de Imágenes V2 (DAM Desktop System)  
**Evaluador:** Experto en Arquitectura de Software y Principios SOLID  
**Fecha:** Julio 2026  

---

## 1. Resumen Ejecutivo

Se ha realizado una revisión integral de la arquitectura y el código fuente del proyecto **VisorImagenes**. El sistema implementa un sistema DAM (Digital Asset Management) de escritorio complejo y funcional usando MVC plano en Java Swing y JavaFX.

Sin embargo, a nivel arquitectónico, la aplicación presenta una alta concentración de responsabilidades, acoplamiento fuerte a implementaciones concretas y violaciones sistemáticas de los 5 principios **SOLID**.

---

## 2. Análisis Detallado por Principio SOLID

### S — Single Responsibility Principle (SRP) / Principio de Responsabilidad Única
> *"Una clase debe tener una, y solo una, razón para cambiar."*

#### Incumplimientos Detectados:

1. **Clases "God Object" en la capa de controladores (`controlador`)**
   - **[DataController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/DataController.java)** (~126 KB, >3.000 líneas): Concentra la lógica de árbol de directorios, cuadrícula de datos, sincronización con SQLite/base de datos, etiquetado de usuario/sistema, renombrado de archivos, filtrado y escucha de eventos de ratón/teclado.
   - **[ProjectController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ProjectController.java)** (~135 KB): Gestiona la persistencia de proyectos (`.prj`), validación e integridad de archivos compresos (ZIP/7Z/RAR), exportación e importación de catálogos de clientes, cálculo de estadísticas, binding con tablas Swing y renderizado de overlays.
   - **[VisorController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/VisorController.java)** (~113 KB): Controla la vista principal, cálculo de zooms y desplazamientos (pan), pases de diapositivas (carrusel), modo polaroid, cálculo de grid de miniaturas y gestión de atajos de teclado.
   - **[RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java)** (~90 KB): Mezcla la carga asíncrona de miniaturas en disco, la conversión/malla 3D de modelos STL con JavaFX 3D, gestión de cachés de memoria y sincronización de hilos EDT/Background.

2. **Acoplamiento de Tareas en Servicios (`servicios.cliente`)**
   - **[WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java)** (~104 KB, 1.510 líneas): Mezcla 5 responsabilidades totalmente distintas en una sola clase:
     1. Procesamiento y reescalado de imágenes (Thumbnailator / Graphics2D).
     2. Codificación de imágenes a Base64.
     3. Construcción y serialización JSON.
     4. Generación de código de plantilla Web (bloques de texto HTML/CSS/JavaScript embebidos).
     5. Operaciones de I/O en sistema de archivos (`Files.writeString`).

3. **Modelo Único Monolítico (`modelo`)**
   - **[VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java)** (~25 KB): Mantiene el estado global de 6 modos de trabajo diferentes (`VISUALIZADOR`, `PROYECTO`, `DATOS`, `CLIENTE`, `CARROUSEL`, `RENDER`), administrando múltiples `ListContext`, `ZoomContext`, estados de temporizadores, parámetros de UI y visibilidad de paneles.

---

### O — Open/Closed Principle (OCP) / Principio de Abierto/Cerrado
> *"Las entidades de software deben estar abiertas para la extensión, pero cerradas para la modificación."*

#### Incumplimientos Detectados:

1. **Sentencias condicionales masivas sobre Enums (`WorkMode`, `SelectionState`, `DisplayMode`)**
   - El uso de `WorkMode` en [VisorModel.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/VisorModel.java#L23-L30) genera bloques `switch` y cadenas `if-else` en `GeneralController`, `ToolbarManager`, `ViewManager`, `VisorController` y `ActionFactory`.
   - **Consecuencia:** Para añadir un nuevo modo de trabajo (ej. `MODO_ESTADISTICAS`), es necesario modificar manualmente más de 8 clases existentes en lugar de extender una interfaz común mediante un patrón Estrategia (`WorkModeStrategy`).
   - El estado tristate `SelectionState` (`SELECTED`, `DISCARDED`, `UNDEFINED`) en [ClientSelection](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/proyecto/ProjectModel.java) se evalúa mediante `switch` directos en el exportador HTML, importador de respuestas y componentes visuales (`CheckboxEditorPanel`, `ClientReviewPanel`). Añadir un estado adicional exige modificar múltiples clases.

2. **Fábrica Monolítica No Extensible (`controlador.factory`)**
   - **[ActionFactory.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/factory/ActionFactory.java)**: Contiene la instanciación directa de todas las acciones Swing de la aplicación. No permite registrar nuevas acciones o plugins sin modificar el código fuente de la fábrica.

3. **Exportador HTML Rígido sin Plantillas Extensibles**
   - En [WebCatalogExporter.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cliente/WebCatalogExporter.java), el código HTML, CSS y JS está escrito directamente dentro de Java Text Blocks. Cambiar el diseño visual o exportar a otros formatos (ej. PDF, Excel catalog) exige modificar la lógica Java del exportador o duplicar la clase.

---

### L — Liskov Substitution Principle (LSP) / Principio de Sustitución de Liskov
> *"Las clases derivadas deben poder sustituir a sus clases base sin alterar el comportamiento correcto del programa."*

#### Incumplimientos Detectados:

1. **Violación de Tipado y Contrato en Registro de Componentes (`ComponentRegistry`)**
   - En **[ComponentRegistry.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java#L88-L91)**, el método genérico `<T extends Component> T get(String name)` realiza un casting implícito no comprobado `(T) components.get(name)`.
   - **Consecuencia:** Rompe el contrato de seguridad de tipos. Si un componente se registra con una clave (ej. `"panel.central"`) como `JPanel` y más tarde se sustituye o requiere en otro punto del código como `JScrollPane`, se lanzará un `ClassCastException` en tiempo de ejecución en el cliente invocador, ya que el registro enmascara el contrato de tipado.

2. **Jerarquía de Coordinadores de Listas (`AbstractListCoordinator`)**
   - Las subclases de [AbstractListCoordinator](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/AbstractListCoordinator.java) (`ProjectListCoordinator`, `ListCoordinator`, `GridCoordinator`) anulan ciertos métodos del coordinador base dejándolos vacíos o lanzando excepciones cuando ciertas operaciones de selección o filtrado no aplican al contexto específico (ej. en modo DATOS vs modo VISUALIZADOR).

---

### I — Interface Segregation Principle (ISP) / Principio de Segregación de Interfaces
> *"Los clientes no deben estar obligados a depender de interfaces que no utilizan."*

#### Incumplimientos Detectados:

1. **Interfaces de Escucha "Gordas" / Monolíticas**
   - Interfaces como [MasterListChangeListener](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/MasterListChangeListener.java) y [ProjectStateListener](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/ProjectStateListener.java) fuerzan a las clases suscriptoras a implementar callbacks múltiples sobre eventos de la lista/proyecto que a menudo no les interesan.

2. **Acoplamiento de Controladores a una Interfaz de Modelo Excesivamente Grande**
   - `VisorModel` expone una interfaz masiva con decenas de métodos getter/setter para la totalidad de la aplicación. Un controlador que solo gestiona la navegación en modo `VISUALIZADOR` depende implícitamente de métodos dirigidos exclusivamente a la gestión de etiquetas del modo `DATOS` o comentarios del modo `CLIENTE`.

---

### D — Dependency Inversion Principle (DIP) / Principio de Inversión de Dependencias
> *"Los módulos de alto nivel no deben depender de módulos de bajo nivel. Ambos deben depender de abstracciones. Las abstracciones no deben depender de detalles."*

#### Incumplimientos Detectados:

1. **Antipatrón Service Locator (`ComponentRegistry`)**
   - La aplicación utiliza **[ComponentRegistry](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/utils/ComponentRegistry.java)** como un registro global estático basado en claves `String` (`"frame.principal"`, `"panel.info.superior"`).
   - **Consecuencia:** Los controladores acceden directamente a componentes Swing concretos buscando cadenas mágicas globales en lugar de recibir dependencias o interfaces explícitas a través de sus constructores (Constructor Injection). Esto imposibilita las pruebas unitarias aisladas.

2. **Instanciación Directa de Servicios Concretos (`new`)**
   - En controladores como `ProjectController`, `DataController` y `VisorController`, los servicios secundarios (`ProjectManager`, `WebCatalogExporter`, `IndexationService`, `ArchiveAnalysisService`) se instancian directamente usando `new` o métodos estáticos.
   - No existen interfaces para los servicios (ej. `IProjectRepository`, `ICatalogExporter`, `IArchiveValidator`), lo que acopla el control de la aplicación a la implementación en disco y formatos concretos.

3. **Acoplamiento de Lógica de Negocio a Frameworks GUI (Swing y JavaFX)**
   - Clases de servicio y controladores de alto nivel manejan directamente tipos concretos de Swing (`JFrame`, `JPanel`, `JTable`) y JavaFX (`JFXPanel`, `PerspectiveCamera`, `TriangleMesh`) en lugar de abstraer las interacciones con la interfaz mediante patrones Presenter / View interfaces.

---

## 3. Plan de Recomendaciones de Refactorización

| Principio | Acción Recomendada | Beneficio |
| :--- | :--- | :--- |
| **SRP** | Dividir `DataController`, `ProjectController` y `VisorController` en sub-controladores/delegados especializados (ej. `ArchiveValidationHandler`, `CatalogExportService`, `FolderTreeHandler`). Separar `WebCatalogExporter` en procesador de imágenes, generador de plantillas y escritor de archivos. | Código modular, fácil de mantener y probar de forma independiente. |
| **OCP** | Sustituir las evaluaciones por `switch(WorkMode)` por el patrón **Strategy** (`WorkModeStrategy`). Usar motores de plantillas o renderizadores configurables para la exportación HTML. | Permitirá añadir nuevos modos o formatos de exportación sin tocar código existente. |
| **LSP** | Eliminar el uso de claves `String` desprotegidas en `ComponentRegistry` e implantar inyección de dependencias fuertemente tipada. Asegurar que subclases de coordinadores respeten plenamente los contratos de sus clases base. | Prevención de errores de casting y excepciones en tiempo de ejecución. |
| **ISP** | Dividir interfaces monolíticas de listeners en interfaces funcionales o pequeñas (ej. `ItemSelectionListener`, `ListClearedListener`). Ofrecer vistas/interfaces segregadas del modelo según el modo activo. | Componentes con menos dependencias innecesarias y código más limpio. |
| **DIP** | Reemplazar `ComponentRegistry` y llamadas con `new` por Inyección de Dependencias por Constructor. Introducir interfaces para los servicios de infraestructura (`IProjectRepository`, `IImageRenderer`). | Facilita la creación de Mocks y la ejecución de tests unitarios rápidos sin interfaz gráfica. |

---
*Informe generado automáticamente para la revisión arquitectónica de VisorImagenes V2.*
