# Caso de Estudio: ModelTag (Digital Asset Management & Workflow Automation)

**Francisco Javier Tortosa Ors** — Especialista en Automatización de Procesos y Desarrollo de Software Interno

Tel: 681 818 240 | Email: fjaviertortosa@gmail.com | LinkedIn: [linkedin.com/in/fco-javier-tortosa](https://linkedin.com/in/fco-javier-tortosa)

---

## 1. Resumen del Proyecto y Reto de Negocio

**Contexto:** Gestión de bibliotecas masivas de modelado 3D (activos STL) con un volumen actual que supera los 40.000 activos y sus correspondientes archivos de renderizado/previsualización en disco.

**El Problema:** El explorador de archivos estándar de los sistemas operativos no permite traspasar de forma eficiente el bloqueo de carpetas anidadas ni cruzar metadatos complejos. Realizar selecciones, comprobar la integridad física de archivos comprimidos relacionados (ZIP, RAR, 7Z) y validar catálogos para clientes requería procesos manuales propensos a errores que consumían múltiples jornadas de trabajo.

**La Solución:** Un sistema DAM (Digital Asset Management) a medida que unifica la visualización recursiva de archivos, la gestión relacional de metadatos mediante una base de datos local embebida, la automatización del control de integridad de archivos y la exportación de catálogos interactivos para el cliente final.

**Impacto Cuantificable:** El tiempo requerido para seleccionar, validar y organizar unas 100 imágenes específicas dentro de una biblioteca de más de 40.000 activos se redujo de varios días a aproximadamente dos horas, garantizando una manipulación segura y no destructiva de los archivos del sistema.

## 2. Arquitectura de Software y Stack Tecnológico

El desarrollo de ModelTag se ha regido por el principio de maximizar el rendimiento local, la predictibilidad del ciclo de vida de los componentes y la minimización del uso de recursos.

```
┌────────────────────────────────────────────────────────┐
│                      CAPA DE VISTA                     │
│    Swing Puro (FlatLaf) • Renderizado por Demanda      │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                   CAPA DE CONTROLADOR                  │
│   Patrón Command (90+ Acciones) • SwingWorkers • DI    │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                     CAPA DE MODELO                     │
│  Servicios • GSON Persistencia • SQLite (WAL / JDBC)   │
└────────────────────────────────────────────────────────┘
```

### Stack Principal

- **Entorno de Ejecución:** Java 21 (construido mediante Maven).
- **Interfaz de Usuario (UI):** Swing puro (sin dependencias web o JavaFX pesadas), utilizando componentes estándar personalizados (JFrame, JList, JTable). La coherencia visual se maneja mediante la librería FlatLaf 3.4.1 (soportando múltiples temas y extensiones personalizadas).
- **Base de Datos Local:** SQLite embebido accedido mediante JDBC con persistencia optimizada.
- **Serialización:** GSON 2.10.1 con adaptadores personalizados de tipo (TypeAdapter) y configuraciones específicas (disableHtmlEscaping) para asegurar la compatibilidad con rutas de archivos de Windows.
- **Procesamiento de Miniaturas:** Thumbnailator 0.4.20 para generación adaptativa y escalado eficiente, y TwelveMonkeys 3.10.1 bajo el estándar ImageIO para extender el soporte a formatos avanzados de imagen (CMYK, PSD, WebP, TGA, etc.).
- **Gestión de Caché:** Caffeine 3.1.8 para la gestión en memoria de miniaturas con políticas de desalojo automatizadas.
- **Generación de Reportes:** Apache PDFBox 3.0.5 para la exportación de catálogos técnicos.

### Patrones de Diseño Aplicados

- **MVC Plano Desacoplado:** Arquitectura limpia dividida en modelo, vista, controlador y servicios, gestionando la inyección de dependencias mediante un registro manual de componentes (ComponentRegistry).
- **Command Pattern:** Encapsulación de la lógica de negocio en más de 90 clases de acción independientes coordinadas por un ActionFactory, lo que facilita la escalabilidad y mantenibilidad del software.
- **Inicialización en 3 Fases:** Ciclo de vida predecible del inicio de la aplicación en la clase AppInitializer (instantiateComponents → wireDependencies → initializeApplication).

## 3. Soluciones de Ingeniería a Desafíos Complejos

### Desafío A: Renderizado en Tiempo Real de 40.000+ Imágenes en la UI sin Bloqueo de Memoria

**Problema:** Cargar decenas de miles de imágenes de alta resolución en componentes Swing saturaría la memoria JVM en pocos segundos.

**Solución (Sliding Window & Caché):** Se diseñó un GridCoordinator que implementa una ventana deslizante virtual. El modelo de Swing solo almacena strings de rutas en memoria. Solo las imágenes visibles en pantalla (y un margen de tres filas de pre-carga anterior y posterior) son solicitadas al servicio de miniaturas. Las imágenes se procesan de forma asíncrona mediante un hilo dedicado (ThumbnailGeneratorThread) y se almacenan en una caché de Caffeine con un límite estricto de 5.000 entradas. Las rutas de archivos corruptos o inexistentes se registran en un set sincronizado para evitar reintentos de lectura innecesarios.

### Desafío B: Representación Jerárquica (Relación 1:N) en una Interfaz de Tabla Plana

**Problema:** En el módulo "Modo Cliente", se requiere visualizar y editar de forma interactiva una imagen padre que contiene múltiples variantes o archivos de componentes internos (hijos), permitiendo ediciones de estado de selección ternaria (SELECTED, DISCARDED, UNDEFINED). El uso de componentes complejos como JTreeTable suele sobrecargar el desarrollo y restar flexibilidad de ordenamiento.

**Solución (ClienteTableModel):** Se desarrolló un modelo de tabla jerárquico personalizado sobre un JTable convencional mediante dos estructuras de datos paralelas: una lista de referencias directas al modelo de imagen (parentImages) y una lista de índices de checkbox (checkboxIndices).

```
Índice en JTable:   0       1        2        3       4       5
parentImages:    [Img_A] [Img_A]  [Img_A]  [Img_B] [Img_B] [Img_C]
checkboxIndices: [  -1 ] [   0 ]  [   1 ]  [  -1 ] [   0 ] [  -1 ]  <-- (-1 indica fila padre)
```

Esta abstracción permite:

- **Colapso dinámico progresivo:** Soporte de tres modos de colapso (NONE, SMART y FULL) que reconstruyen el modelo de tabla sobre la marcha.
- **Edición Tristate en Cascada:** Al editar un padre, el estado se propaga inmediatamente a los checkboxes hijos; al editar un hijo, el estado del padre se recalcula en base a las reglas de negocio del modelo.
- **Ordenación Preservando la Jerarquía:** Implementación de un algoritmo de ordenamiento específico (sortGroups) que preserva la cohesión del bloque "padre-hijos", superando las limitaciones de los clasificadores de tabla predeterminados de la API de Java.

### Desafío C: Automatización e Integridad de Datos en Entornos Distribuidos

**Problema:** Al trabajar con volúmenes masivos de datos en sistemas locales, las desconexiones de discos extraíbles, las inconsistencias en nombres de archivos o los fallos de escritura de configuración pueden corromper el estado del proyecto.

**Soluciones:**

- **Auto-relocalización Asíncrona:** El ProjectIntegrityService repara de forma autónoma rutas físicas rotas buscando de forma recursiva en el sistema por firma de nombre de archivo.
- **Escritura Atómica:** La persistencia del archivo de configuración global se realiza escribiendo primero sobre un archivo temporal que, una vez validado, reemplaza de forma atómica al archivo original, eliminando el riesgo de archivos de configuración corruptos por cierres inesperados de la aplicación.
- **Pre-vuelo de Exportación (Preflight):** Mediante ExportPreflightService se realizan comprobaciones previas antes de iniciar escrituras pesadas: validación de códigos de catálogo, existencia física, espacio disponible en disco y análisis estructural interno de los contenedores .7z para verificar la presencia de los ficheros STL requeridos.

## 4. Arquitectura de Base de Datos (Esquema SQLite)

El almacenamiento del catálogo se centraliza en un motor SQLite local. Se optimizó el rendimiento transaccional aplicando los modos PRAGMA journal_mode=WAL (Write-Ahead Logging), sincronización normal, almacenamiento temporal en memoria y un límite de caché de 64MB.

```
┌──────────────────────┐
│        discos        │
├──────────────────────┤
│ PK  id               │
│     numero_serie (U) │
└──────────┬───────────┘
           │ 1
           │
           │ N
┌──────────▼───────────┐         ┌──────────────────────┐
│       imagenes       │         │         tags         │
├──────────────────────┤         ├──────────────────────┤
│ PK  id               │         │ PK  id               │
│     ruta_completa (U)│         │     nombre           │
│ FK  disco_id         │         │ FK  parent_id (Tree) │
└──────────┬───────────┘         └──────────┬───────────┘
           │ 1                              │ 1
           │                                │
           └───────────────┬────────────────┘
                           │ N:M (imagen_tags)
                           ▼
                ┌──────────────────────┐
                │     imagen_tags      │
                ├──────────────────────┤
                │ PK,FK imagen_id      │
                │ PK,FK tag_id         │
                └──────────────────────┘
```

### Aspectos de Diseño Clave

- **Estructura Jerárquica N-aria en Tags:** La tabla tags implementa una relación autorreferencial (parent_id) para estructurar árboles lógicos de categorías. El sistema permite la consulta recursiva de elementos (ej. ver imágenes de toda la rama descendiente de un tag principal de forma directa).
- **Integridad Estricta:** Uso de restricciones de clave foránea (FOREIGN KEYS = ON) y borrados en cascada (ON DELETE CASCADE) para evitar registros huérfanos en la tabla de unión de muchos a muchos imagen_tags.
- **Migración de Esquema Robusta:** Implementación de un mecanismo de migración automática (upgradeSchema) que detecta y actualiza la estructura de la base de datos de los usuarios de forma progresiva sin alteración de sus datos.

## 5. Flujos de Trabajo Destacados (De la Biblioteca al Cliente)

El flujo de trabajo cubre el ciclo completo desde la catalogación hasta la entrega de propuestas interactivas:

1. **Catalogación y Clasificación (Modo Datos):** Asignación de etiquetas personalizadas organizadas de forma arbórea y mapeo automático de la estructura física del disco como etiquetas de sistema no editables.
2. **Filtrado Avanzado (Modo Visor):** Búsqueda instantánea de tipo "tornado" asíncrona que cruza de forma aditiva o sustractiva textos de ruta, nombres y etiquetas personalizadas.
3. **Consolidación (Modo Proyecto):** Asociación semiautomática de imágenes con sus respectivos ficheros de geometría 3D, validando la integridad del contenido técnico del archivo y permitiendo la corrección de inconsistencias.
4. **Validación del Cliente (Modo Cliente):** Exportación dinámica del catálogo consolidado a un entorno HTML estático interactivo. El cliente puede revisar de forma remota, realizar selecciones o descartes, y generar notas de comunicación de vuelta que el taller puede reimportar y procesar mediante el ClienteTableModel.

## 6. Conclusiones de Rendimiento y Desarrollo

- **Optimización de Procesos:** La aplicación ha eliminado los cuellos de botella de renderizado locales asociados a las limitaciones de los exploradores de archivos genéricos del sistema operativo.
- **Seguridad y Robustez:** Diseñada bajo un esquema no destructivo (operaciones de solo lectura sobre los activos de entrada salvo orden explícita del usuario), garantizando la total preservación de los archivos de origen.
- **Mantenibilidad:** El uso estricto de patrones como Command y la modularización en servicios especializados ha permitido añadir funciones complejas (como la exportación HTML y el análisis de archivos comprimidos) de forma limpia y desacoplada del resto del sistema.
