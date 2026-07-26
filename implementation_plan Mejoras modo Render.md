# Plan Paso a Paso — Optimización y Refactorización del Modo RENDER

Este plan establece los pasos estructurados para corregir la fuga de memoria RAM, las condiciones de carrera en hilos de background, la gestión insegura de archivos temporales y la elevada complejidad del **Modo RENDER** (`RenderController`).

---

## Revisión Requerida por el Usuario

> [!IMPORTANT]
> **Garantía de Estabilidad y Reglas del Proyecto**:
> Todas las modificaciones mantendrán la compatibilidad con Swing y JavaFX 3D. Siguiendo las normas de `AGENTS.md`, **no se compilará ni se realizará ningún commit** sin autorización previa explícita del usuario.

---

## Pasos del Plan de Mejora

### Paso 1: Solución a la Fuga de Memoria RAM (`triangleCache`)

* **Objetivo:** Eliminar la retención indefinida de mallas 3D en la memoria RAM que provoca fallos `OutOfMemoryError` al previsualizar múltiples archivos STL.
* **Problema que arregla:** [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java#L74) mantiene mallas `List<Triangle>` en un `HashMap` ilimitado.
* **Cambios a realizar:**
  * Implementar almacenamiento de mallas mediante `SoftReference<List<Triangle>>` en `triangleCache`.
  * Permitir que el recolector de basura (GC) de Java libere automáticamente mallas 3D inactivas cuando la memoria RAM entre en presión.
* **Archivos a modificar:**
  * [MODIFY] [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java)

---

### Paso 2: Eliminación de Condiciones de Carrera y Cancelación de Hilos Background

* **Objetivo:** Prevenir que respuestas lentas de la carga 3D o extracción de comprimidos anteriores sobrescriban la selección actual del usuario al hacer clics rápidos.
* **Problema que arregla:** `cargarTriangulosAsync()` y otros flujos asíncronos en `RenderController` lanzan `SwingWorker` sin cancelar las tareas previas en curso.
* **Cambios a realizar:**
  * Guardar una referencia activa al trabajador actual y llamar a `cancel(true)` antes de instanciar un nuevo hilo de procesamiento.
  * Comprobar `isCancelled()` dentro de los bloques `done()` de los `SwingWorker`.
* **Archivos a modificar:**
  * [MODIFY] [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java)

---

### Paso 3: Gestión Segura de Archivos Temporales y Logging de Excepciones

* **Objetivo:** Garantizar la limpieza efectiva del directorio temporal `%TEMP%\visor_zip2png` y evitar la supresión silenciosa de errores de E/S.
* **Problema que arregla:** Bloques `catch (Exception ex) {}` vacíos en `limpiarOutputDir()` que ignoran archivos temporales bloqueados.
* **Cambios a realizar:**
  * Reemplazar los bloques `catch` vacíos por `logger.warn(...)` detallando la ruta y el motivo del fallo de borrado.
  * Forzar el cierre de descriptores de lectura antes de intentar eliminar carpetas o archivos temporales extraídos.
* **Archivos a modificar:**
  * [MODIFY] [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java)
  * [MODIFY] [ZipExtractor.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/ZipExtractor.java)

---

### Paso 4: Modularización y Separación de Responsabilidades (SRP)

* **Objetivo:** Reducir la complejidad ciclomática de `RenderController` (~90 KB, >2.000 líneas) descomponiéndola en controladores delegados de responsabilidad única.
* **Problema que arregla:** Acoplamiento excesivo en una sola clase (E/S, manipulación de escena 3D, eventos de UI, capas 2D).
* **Cambios a realizar:**
  * [NEW] [RenderTempFileManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/servicios/renderer/RenderTempFileManager.java): Encargado exclusivamente de la creación, ubicación y limpieza segura de carpetas temporales.
  * [NEW] [RenderSceneController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/managers/RenderSceneController.java): Encargado exclusivamente de la interacción con la escena JavaFX 3D, luces, cámara y fondos en [PreviewPanel3DFX.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/render/PreviewPanel3DFX.java).
* **Archivos a modificar:**
  * [MODIFY] [RenderController.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/RenderController.java)

---

## Plan de Verificación

### Pruebas Automatizadas (Maven)
- Comprobar que la sintaxis y tipos son correctos:
  ```bash
  mvn clean compile
  ```

### Pruebas Funcionales Manuales
1. **Prueba de Memoria RAM:** Cargar secuencialmente 30 modelos STL pesados y verificar con el administrador de tareas de Windows que la memoria RAM se estabiliza y no crece indefinidamente.
2. **Prueba de Concurrencia:** Hacer clics muy rápidos entre diferentes modelos STL y paquetes ZIP en la lista de candidatos para confirmar que la vista 3D muestra exactamente el elemento seleccionado sin parpadeos desincronizados.
3. **Prueba de Limpieza de Temporales:** Cerrar el modo RENDER o la aplicación y verificar que la carpeta temporal en `%TEMP%\visor_zip2png` se vacía correctamente.

---
*Plan paso a paso elaborado para la optimización del Modo RENDER.*
