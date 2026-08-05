# Informe de Menús Popup — VisorImagenes V2

Se han identificado **9 menús popup** distintos en el programa.

---

## 1. Menú contextual del árbol de Tags (Modo DATOS)

**Clase:** [DataBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/DataBuilder.java#L150-L193)  
**Componente:** `JTree allTagsTree`  
**Trigger:** Clic derecho sobre un nodo del árbol de etiquetas  
**Construido con:** `PopupMenuBuilder.buildPopupMenuWithNestedMenus()`

### Opciones:
| Opción 	| Comando 							| Habilitado si... 			|
|--------	|---------							|-----------------			|
| Nuevo 	| `CMD_DATOS_TAG_NUEVO` 		| Siempre 					|
| Renombrar | `CMD_DATOS_TAG_RENOMBRAR` 	| Tag no es `readOnly` 	|
| Borrar 	| `CMD_DATOS_TAG_BORRAR` 		| Tag no es `readOnly` 	|

---

## 2. Menú contextual de lista "Selección" (Modo PROYECTO)

**Clase:** [ProjectBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ProjectBuilder.java#L208-L213)  
**Componente:** `JList projectFileList` (registro: `list.proyecto.nombres`)  
**Trigger:** Clic derecho sobre un elemento de la lista de selección  
**Construido con:** `createContextMenuListener()` interno de ProjectBuilder

### Opciones:
| Opción 				| Acción 									|
|--------				|--------									|
| Mover a descartes 	| `CMD_PROYECTO_MOVER_A_DESCARTES` 	|
| *(separador)* 		| 											|
| Localizar archivo 	| `CMD_PROYECTO_LOCALIZAR_ARCHIVO` 	|
| *(separador)* 		| 											|
| Añadir archivos 		| `CMD_PROYECTO_ANADIR_ARCHIVOS` 		|
| *(separador)* 		| 											|
| Asignar Etiqueta... 	| Abre `TagAssignmentDialog` 			|

---

## 3. Menú contextual de lista "Descartes" (Modo PROYECTO)

**Clase:** [ProjectBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ProjectBuilder.java#L257-L260)  
**Componente:** `JList descartesList` (registro: `list.proyecto.descartes`)  
**Trigger:** Clic derecho sobre un elemento de la lista de descartes  
**Construido con:** `createContextMenuListener()` interno de ProjectBuilder

### Opciones:
| Opción 					| Acción 											|
|--------					|--------											|
| Restaurar de descartes 	| `CMD_PROYECTO_RESTAURAR_DE_DESCARTES` 	|
| *(separador)* 			| 													|
| Localizar archivo 		| `CMD_PROYECTO_LOCALIZAR_ARCHIVO` 			|
| *(separador)* 			| 													|
| Vaciar descartes 			| `CMD_PROYECTO_VACIAR_DESCARTES` 			|
| *(separador)* 			| 													|
| Añadir archivos 			| `CMD_PROYECTO_ANADIR_ARCHIVOS` 				|
| *(separador)* 			| 													|
| Borrar imagen 			| `CMD_PROYECTO_ELIMINAR_PERMANENTEMENTE` 	|

---

## 4. Menú contextual del visor principal (Modo PROYECTO — dinámico)

**Clase:** [ProjectUIManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ui/ProjectUIManager.java#L207-L281)  
**Componentes:** `ImageDisplayPanel`, `PolaroidDisplayPanel`, `GridDisplayPanel` (los paneles de visualización compartidos en modo PROYECTO)  
**Trigger:** Clic derecho sobre el visor de imágenes o la rejilla de miniaturas  
**Método:** `crearMenuContextualVisorManualmente()` — menú construido dinámicamente según el contexto

### Opciones (dinámicas según estado):

**Si hay imagen seleccionada (`hasSelection = true`):**

| Estado del visor 							| Opciones añadidas 															|
|-----------------							|-------------------															|
| `VIEW_SELECTION` / `VIEW_EXPORT` 	| Mover a descartes, Localizar archivo 											|
| `VIEW_DISCARDS` 						| Restaurar de descartes, Localizar archivo, *(sep)*, Eliminar permanentemente 	|


**Siempre (independientemente del contexto):**

| Opción 						| Detalle 																|
|--------						|---------																|
| *(separador)* 				| 																		|
| [✓] Activar Zoom Manual 		| `CMD_ZOOM_MANUAL_TOGGLE` — CheckBox, desactivado en Grid 		|
| Restablecer Zoom 				| `CMD_ZOOM_RESET` — desactivado en Grid 							|
| *(separador)* 				| 																		|
| Asignar Etiqueta... 			| Solo si `dataManager != null` — abre `TagAssignmentDialog` 	|
| *(separador)* 				| Solo si `dataManager != null` 									|
| Añadir archivos al proyecto 	| `CMD_PROYECTO_ANADIR_ARCHIVOS` — siempre visible 				|

---

## 5. Menú contextual de la tabla de exportación (Modo PROYECTO)

**Clase:** [ProjectUIManager.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/controlador/ui/ProjectUIManager.java#L284-L325)  
**Componente:** `JTable tablaExportacion` (registro: `tabla.exportacion`)  
**Trigger:** Clic derecho sobre una fila de la tabla de exportación  
**Método:** `configurarContextMenuTablaExportacion()`  
**Nota:** Las acciones son `ContextSensitiveAction`, su estado se actualiza al abrir el menú.

### Opciones:
| Opción 						| Comando 										|
|--------						|---------										|
| Asignar archivo comprimido 	| `CMD_EXPORT_ASIGNAR_ARCHIVO` 			|
| Quitar de la cola 			| `CMD_EXPORT_QUITAR_DE_COLA` 			|
| *(separador)* 				| 												|
| Ignorar comprimido 			| `CMD_EXPORT_IGNORAR_COMPRIMIDO` 		|
| Relocalizar imagen 			| `CMD_EXPORT_RELOCALIZAR_IMAGEN` 		|
| Limpiar no encontrados 		| `CMD_EXPORT_LIMPIAR_NO_ENCONTRADOS` 	|
| *(separador)* 				| 												|
| Abrir ubicación 				| `CMD_EXPORT_ABRIR_UBICACION` 			|

---

## 6. Menú contextual de tablas de cliente — Proyecto y Cliente (Modo CLIENTE)

> Hay **dos instancias** de este popup prácticamente idénticas, construidas en dos métodos distintos:
> - `createProjectTable()` (línea 266) → tablas `ProyectoClienteTableModel`
> - `createClientTable()` (línea 501) → tablas `ClienteTableModel`

**Clase:** [ClientBuilder.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/builders/ClientBuilder.java#L266-L327)  
**Componente:** `JTable` (tabla de selección o de descartes de cliente)  
**Trigger:** Clic derecho sobre una fila  
**Nota:** Si el proyecto está en modo cliente cerrado (`isClientModeClosed()`), no se muestra.

### Opciones según modo edición y tabla:

**Si edición NO activa:**

| Opción 							| Estado	 					|
|--------							|--------						|
| Activar edición para modificar 	| Deshabilitado (informativo) 	|


**Si edición activa + tabla de Selección (`isSeleccion = true`):**

| Opción 			| Acción 													|
|--------			|--------													|
| Mover a descartes | Marca imagen como `DISCARDED` + actualiza checkboxes 	|

**Si edición activa + tabla de Descartes:**


*`createProjectTable()` (ProyectoClienteTableModel):*

| Opción 				| Acción 																					|
|--------				|--------																					|
| Mover a selección 	| Marca imagen como `UNDEFINED` + restaura checkboxes 									|
| *(separador)* 		| 																							|
| Borrar imagen 		| Elimina del proyecto (`projectManager.eliminarDeProyecto`) — pide confirmación 	|
| *(separador)* 		| 																							|
| Localizar archivo 	| Abre explorador en la ubicación del archivo 												|


*`createClientTable()` (ClienteTableModel):*

| Opción 							| Acción 																|
|--------							|--------																|
| Mover a selección 				| Cambia `estadoCliente` a `UNDEFINED` + restaura checkboxes 		|
| *(separador)* 					| 																		|
| Mover a descartes del proyecto 	| Saca imagen de `enSeleccionProyecto` conservando estado cliente 	|
| *(separador)* 					| 																		|
| Borrar imagen 					| Saca de selección y pone `UNDEFINED` — pide confirmación 			|
| *(separador)* 					| 																		|
| Localizar archivo 				| Abre explorador en la ubicación del archivo 							|

---

## 7. Menú contextual del editor de checkboxes (Modo CLIENTE — Panel de edición)

**Clase:** [CheckboxEditorMouseHandler.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/CheckboxEditorMouseHandler.java#L319-L393)  
**Componente:** Panel de imagen con overlays de checkbox (`ImageDisplayPanel`)  
**Trigger:** Clic derecho sobre el panel de imagen en modo edición  

### Opciones (dinámicas según posición del clic):

**Siempre disponibles:**

| Opción 						| Acción 										|
|--------						|--------										|
| Añadir Checkbox 				| Añade overlay en la posición del clic 		|
| *(separador)* 				|	 											|
| Añadir Comentario Global... 	| Añade `CommentOverlay` sobre la imagen 	|


**Si el clic es sobre un checkbox existente (`hitIdx >= 0`):**

| Opción 										| Acción 												|
|--------										|--------												|
| *(separador)* 								| 														|
| Borrar Checkbox 								| Elimina el overlay y recalcula estado de la imagen 	|
| Poner PVP... 									| Abre diálogo para asignar precio al checkbox 			|
| *(separador)* 								| 														|
| Añadir mensaje al modelo... 					| Añade mensaje al `CommentThread` del checkbox 		|
| Ver mensajes del modelo... *(si los hay)* 	| Abre `MsgPopupDialog` para ver el hilo 			|
| Borrar mensaje del modelo *(si los hay)* 		| Borra el mensaje del hilo 							|


**Si el clic es sobre el comentario global (`overComment = true`):**

| Opción 				| Acción 											|
|--------				|--------											|
| *(separador)* 		| 													|
| Editar comentario... 	| Abre diálogo de edición del `CommentOverlay` 	|
| Borrar comentario 	| Elimina el `CommentOverlay` de la imagen 		|

---

## 8. Popup de selección de alineación/distribución (Editor de Render)

**Clase:** [EditorComponentBar.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/panels/render/EditorComponentBar.java#L440-L474)  
**Componente:** Botón `▼` en la barra del editor de render  
**Trigger:** Clic sobre el botón combo de alineación o distribución  
**Nota:** No es un menú contextual de clic derecho, sino un dropdown visual con un `GridLayout(2, 3)` de iconos.

### Contenido (para el botón de alineación):
Grid 2×3 de iconos de alineación (izq/centro/der, arriba/centro/abajo)

### Contenido (para el botón de distribución):
Grid 2×3 de iconos de distribución (bordes y centros horizontal/vertical)

---

## 9. Popup de sugerencias IntelliSense de Tags

**Clase:** [TagIntelliSenseField.java](file:///d:/Programacion/Eclipse/Workspace%202024-12R/VisorImagenes/src/vista/components/TagIntelliSenseField.java#L78-L82)  
**Componente:** Campo de texto `JTextField` extendido  
**Trigger:** Al escribir en el campo, aparece automáticamente una lista de sugerencias  
**Nota:** No es un menú contextual, sino un `JPopupMenu` usado como contenedor dropdown de un `JList` de sugerencias de rutas de tags. Navegable con ↑/↓/TAB/Enter.

---

## Resumen

pendiente de revisar la clase MenuPopupManager.java

| # 	| Menú 											| Clase 								| Modo 		| Trigger 					|
|---	|------											|-------								|------		|---------					|
| 2 	| Lista Selección Proyecto 						| `ProjectBuilder` 					| PROYECTO 	| Clic derecho lista 		|
| 3 	| Lista Descartes Proyecto 						| `ProjectBuilder` 					| PROYECTO 	| Clic derecho lista 		|
| 4 	| Visor principal (dinámico) 					| `ProjectUIManager` 				| PROYECTO 	| Clic derecho imagen/grid 	|
| 5 	| Tabla Exportación 							| `ProjectUIManager` 				| PROYECTO 	| Clic derecho tabla 		|
| 6a 	| Tabla Cliente (ProyectoClienteTableModel) 	| `ClientBuilder` 					| CLIENTE 	| Clic derecho tabla 		|
| 6b 	| Tabla Cliente (ClienteTableModel) 			| `ClientBuilder` 					| CLIENTE 	| Clic derecho tabla 		|
| 7 	| Editor de Checkboxes 							| `CheckboxEditorMouseHandler` 	| CLIENTE 	| Clic derecho imagen 		|
| 8 	| Botón alineación/distribución 				| `EditorComponentBar` 				| RENDER 	| Clic botón ▼ 				|
| 9 	| IntelliSense de Tags 							| `TagIntelliSenseField` 			| DATOS 	| Al escribir 				|
| 1 	| Árbol de Tags 								| `DataBuilder` 						| DATOS 	| Clic derecho árbol 		|
