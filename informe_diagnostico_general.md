# Informe Técnico de Diagnóstico General, Errores Silenciosos y Riesgos Futuros
**Proyecto:** Visor de Imágenes V2 (DAM Desktop System)  
**Evaluador:** Programador Senior & Arquitecto de Software  
**Fecha:** Julio 2026  

---

## 1. Problemas Generales de la Aplicación

### 1.1 Escalabilidad y Gestión de Memoria RAM (>40.000 Activos)
* **Cachés Ilimitados sin Evicción (Riesgo `OutOfMemoryError`):**
  * En [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java#L74), la caché 3D `triangleCache` se almacena como `Map<Path, List<Triangle>> triangleCache = new HashMap<>()` sin límite de tamaño ni uso de `SoftReference` / `WeakReference`.
  * **Consecuencia:** Al explorar miles de archivos 3D STL, la memoria RAM consumida por la geometría de triángulos crece de forma indefinida hasta provocar la caída de la aplicación por `java.lang.OutOfMemoryError`. La clase [LruCache.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/cache/LruCache.java) fue marcada como "obsoleta" sin reemplazo.
* **Carga Masiva síncrona en Swing `DefaultListModel`:**
  * Al indexar bibliotecas de más de 40.000 elementos, el modelo de lista Swing ([ListContext.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/modelo/ListContext.java)) carga miles de objetos `String` e `Icon` directamente en memoria RAM. Swing no utiliza virtualización de listas (paginación o lazy loading), provocando congelamientos severos de la interfaz de usuario (UI freezing).

### 1.2 Complejidad en la Concurrencia y Sincronización de Hilos (Swing EDT vs JavaFX)
* **Entrelazamiento de EDT y JavaFX Thread:**
  * La previsualización 3D integra JavaFX (`JFXPanel`) dentro de Swing. En [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java) y `PreviewPanel3DFX`, existen llamadas anidadas cruzadas `Platform.runLater()` dentro de `SwingUtilities.invokeLater()`.
  * **Riesgo:** Si el usuario cambia rápidamente de carpeta o selecciona modelos 3D a gran velocidad, se generan condiciones de carrera (*race conditions*) e hilos huérfanos que compiten por el contexto gráfico OpenGL/DirectX de la GPU, bloqueando la interfaz.

### 1.3 Acoplamiento Estricto al Sistema Operativo Windows
* **Rutas Absolutas Codificadas en Duro:**
  * En el script de compilación y lanzamiento (`AGENTS.md`) y clases de servicios, existen rutas fijas como `D:/Descargas/VisorV2/VisorV2.jar` y separadores de carpeta Windows (`\`).
  * **Problema:** Imposibilita la ejecución portable del sistema en otros entornos o unidades (ej. Linux, macOS, o instalaciones en unidades de disco `C:` o `E:`).

---

## 2. Errores Silenciosos (Swallowed Exceptions y Fallos No Detectados)

### 2.1 Supresión Sistemática de Excepciones (`catch (Exception e) {}`)
Se han localizado **más de 25 bloques de captura de excepción vacíos** en operaciones críticas de la aplicación. Ejemplos destacados:

1. **E/S y Eliminación de Archivos Temporales:**
   * En [ArchiveAnalysisService.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/ArchiveAnalysisService.java#L142) y `RenderController.java:L867`:
     ```java
     .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception e) {} });
     ```
   * **Consecuencia:** Si los archivos temporales de extracción 3D están bloqueados por el sistema, el fallo se ignora silenciosamente. La carpeta `%TEMP%` se llena de gigabytes de archivos residuales sin que el usuario ni el sistema lo detecten.

2. **Rollback de Transacciones SQLite:**
   * En `TagDAO.java:L899` e `ImagenDAO.java:L312`:
     ```java
     try { connection.rollback(); } catch (SQLException ex) {}
     ```
   * **Consecuencia:** Si la transacción de base de datos falla y el `rollback()` posterior también lanza una excepción, el estado de la conexión SQLite queda corrupto o desincronizado sin dejar rastro en el archivo de logs.

3. **Carga y Parsing de Imágenes/Miniaturas:**
   * En `RenderController.java:L582`:
     ```java
     } catch (Exception ignored) {}
     ```
   * **Consecuencia:** Si una imagen está corrupta o utiliza un formato no soportado, se ignora el error en lugar de registrar la causa en `log.txt` o mostrar un indicador de "Imagen no disponible" en la interfaz.

### 2.2 Inconsistencia de Clave de Imagen Seleccionada al Cambiar de Modo
* **Defecto Detectado:**
  * En `DataController.java`, los listeners del modo DATOS modificaban la clave `selectedImageKey` operando directamente sobre el contexto **activo**.
  * **Efecto Silencioso:** Durante el arranque o al conmutar entre pantallas, se sobrescribía la imagen seleccionada por el usuario en el visualizador sin lanzar ningún error, produciendo un comportamiento errático invisible para el programador hasta que el usuario perdía su selección.

---

## 3. Previsiones de Fallos Futuros y Riesgos a Largo Plazo

### 3.1 Colapso Navegador Cliente por HTML Monolítico Gigante
* **Contexto:** `WebCatalogExporter` genera un único archivo `index.html` con las imágenes incrustadas en Base64.
* **Fallo Futuro Previsible:**
  * Si un proyecto contiene más de 100 imágenes en alta resolución, el tamaño del archivo HTML superará los **150 MB**.
  * Al abrir este HTML en el navegador del cliente (Chrome/Firefox/Safari) o en dispositivos móviles/tablets, el motor V8 del navegador agotará la memoria de la pestaña y **colapsará la página Web** (Crash de pestaña por *Out of Memory*).

### 3.2 Corrupción de la Base de Datos SQLite por Accesos Concurrentes
* **Contexto:** SQLite no soporta escrituras concurrentes simultáneas de múltiples hilos de forma nativa sin configuración de modo WAL (Write-Ahead Logging).
* **Fallo Futuro Previsible:**
  * Si `IndexationService` realiza una indexación masiva en segundo plano mientras el usuario asigna etiquetas rápidamente en la UI (`TagDAO`), SQLite devolverá el error `org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked`. Si los bloques `catch` silenciosos tragan este error, **se perderán etiquetas guardadas por el usuario**.

### 3.3 Fragmentación y Degradación de la Configuración (`config.cfg`)
* **Contexto:** `config.cfg` administra más de 246 claves de configuración sin esquema de migración ni versión de configuración.
* **Fallo Futuro Previsible:**
  * Al actualizar la aplicación o agregar nuevos parámetros, instalaciones existentes con archivos `config.cfg` antiguos fallarán con `NullPointerException` o valores por defecto inconsistentes al no encontrar las nuevas claves esperadas por `ConfigKeys.java`.

### 3.4 Fugas de Hilos en Pases de Diapositivas y Renderizado 3D
* **Contexto:** `CarouselManager` y `RenderController` lanzan hilos `Thread` y `ExecutorService` para temporizadores y cargas en background.
* **Fallo Futuro Previsible:**
  * Si el usuario cierra rápidamente el visor o cambia de carpeta mientras se está procesando un renderizado 3D pesado, los hilos de background continúan ejecutándose en segundo plano (hilos zombi). Esto provocará un **uso del 100% de CPU** en background y fuga de descriptores de archivos.

---

## 4. Resumen de Acciones Recomendadas de Mantenimiento

1. **Reemplazar `catch` vacíos por Logging Obligatorio (`logger.error(...)`):** Impedir que cualquier excepción de E/S o SQL se trague sin registro.
2. **Implantar Bounded Cache (`SoftReference` / Evicción LRU):** Limitar `triangleCache` y cachés de imágenes en `RenderController` para evitar caídas de RAM.
3. **Paginación / Paginado Web para Exportación Cliente:** Dividir exportaciones masivas en miniaturas en disco o assets independientes en lugar de un Base64 gigante en un solo HTML.
4. **Activar SQLite WAL Mode y Transacciones Protegidas:** Configurar la base de datos para soportar lecturas UI concurrentes durante la indexación background.
5. **Introducir Manejo Único de Hilos (ExecutorService Centralizado):** Garantizar la cancelación limpia de tareas de background al conmutar de vista.

---
*Informe técnico de diagnóstico y prevención de fallos finalizado.*
