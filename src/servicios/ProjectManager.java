package servicios;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.managers.interfaces.IProjectManager;

public class ProjectManager implements IProjectManager {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    private Path archivoSeleccionActualPath;
    private Set<Path> seleccionActual;
    private Set<Path> seleccionDescartada;
    private ConfigurationManager configManager;

    /**
     * Constructor refactorizado. Ahora es un constructor simple, sin parámetros.
     * Las dependencias se inyectan a través de setters.
     */
    public ProjectManager() {
    	this.seleccionActual = new HashSet<>();
        this.seleccionDescartada = new HashSet<>();
    } // --- Fin del método ProjectManager (constructor) ---

    /**
     * Inicializa el manager después de que las dependencias han sido inyectadas.
     * Debe ser llamado desde AppInitializer.
     */
    public void initialize() {
        if (this.configManager == null) {
            throw new IllegalStateException("ProjectManager no puede inicializarse sin ConfigurationManager.");
        }
        inicializarRutaArchivoSeleccion();
        logger.debug("[ProjectManager] Instancia inicializada. Selección actual desde: " + (this.archivoSeleccionActualPath != null ? this.archivoSeleccionActualPath.toAbsolutePath() : "N/A"));
        logger.debug("  -> Proyecto inicial cargado. Selección: " + this.seleccionActual.size() + ", Descartes: " + this.seleccionDescartada.size());
    } // --- Fin del método initialize ---

    private void inicializarRutaArchivoSeleccion() {
        String carpetaBaseProyectosStr = this.configManager.getString(
            ConfigKeys.PROYECTOS_CARPETA_BASE,
            ".project_selections"
        );
        Path carpetaBaseProyectos = Paths.get(carpetaBaseProyectosStr);

        if (!carpetaBaseProyectos.isAbsolute()) {
            String userHome = System.getProperty("user.home");
            carpetaBaseProyectos = Paths.get(userHome, ".miVisorImagenesApp", carpetaBaseProyectosStr).toAbsolutePath();
        }

        try {
            if (!Files.exists(carpetaBaseProyectos)) {
                Files.createDirectories(carpetaBaseProyectos);
                logger.debug("  [ProjectManager] Directorio base de proyectos creado: " + carpetaBaseProyectos);
            }
        } catch (IOException e) {
            logger.warn("WARN [ProjectManager]: No se pudo crear el directorio base de proyectos: " + carpetaBaseProyectos +
                               ". Usando directorio actual de la aplicación como fallback.");
            carpetaBaseProyectos = Paths.get("").toAbsolutePath();
        }

        String nombreArchivoSeleccion = this.configManager.getString(
            ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL,
            "seleccion_actual_rutas.txt"
        );
        this.archivoSeleccionActualPath = carpetaBaseProyectos.resolve(nombreArchivoSeleccion);
        cargarDesdeArchivo(this.archivoSeleccionActualPath);
    } // --- Fin del método inicializarRutaArchivoSeleccion ---

    
    private void cargarDesdeArchivo(Path rutaArchivo) {
        this.seleccionActual.clear();
        this.seleccionDescartada.clear();

        if (Files.exists(rutaArchivo) && Files.isReadable(rutaArchivo)) {
            String seccionActual = "SELECCION"; // Por defecto, las líneas sin sección van a la selección principal.

            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) {
                        continue; // Ignorar líneas vacías y comentarios
                    }

                    if (linea.equalsIgnoreCase("[SELECCION]")) {
                        seccionActual = "SELECCION";
                        continue;
                    } else if (linea.equalsIgnoreCase("[DESCARTES]")) {
                        seccionActual = "DESCARTES";
                        continue;
                    }

                    try {
                        Path rutaParseada = Paths.get(linea);
                        if ("SELECCION".equals(seccionActual)) {
                            this.seleccionActual.add(rutaParseada);
                        } else if ("DESCARTES".equals(seccionActual)) {
                            this.seleccionDescartada.add(rutaParseada);
                        }
                    } catch (Exception e) {
                        logger.warn("WARN [PM cargar]: Ruta inválida en archivo de proyecto: '" + linea + "' - " + e.getMessage());
                    }
                }
                logger.debug("  [ProjectManager] Proyecto cargado desde " + rutaArchivo +
                                   ". Selección: " + this.seleccionActual.size() +
                                   ", Descartes: " + this.seleccionDescartada.size());

            } catch (IOException e) {
                logger.error("ERROR [ProjectManager]: No se pudo leer el archivo de proyecto: " + rutaArchivo + " - " + e.getMessage());
            }
        } else {
            logger.debug("  [ProjectManager] Archivo de proyecto no encontrado: " + rutaArchivo + ". Se iniciará con proyecto vacío.");
        }
    } // --- Fin del método cargarDesdeArchivo ---
    
    
    private void guardarAArchivo() {
        if (this.archivoSeleccionActualPath == null) {
            logger.error("ERROR [PM guardar]: archivoSeleccionActualPath es null. No se puede guardar.");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(this.archivoSeleccionActualPath,
                                                            StandardOpenOption.CREATE,
                                                            StandardOpenOption.TRUNCATE_EXISTING)) {
            // --- Escribir sección de SELECCION ---
            writer.write("# VisorImagenes - Archivo de Proyecto");
            writer.newLine();
            writer.newLine();
            writer.write("[SELECCION]");
            writer.newLine();
            for (Path ruta : this.seleccionActual) {
                writer.write(ruta.toString().replace("\\", "/"));
                writer.newLine();
            }

            // --- Escribir sección de DESCARTES ---
            writer.newLine();
            writer.write("[DESCARTES]");
            writer.newLine();
            for (Path ruta : this.seleccionDescartada) {
                writer.write(ruta.toString().replace("\\", "/"));
                writer.newLine();
            }

            logger.debug("  [ProjectManager] Proyecto guardado en " + this.archivoSeleccionActualPath +
                               " (Selección: " + this.seleccionActual.size() +
                               ", Descartes: " + this.seleccionDescartada.size() + ").");

        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo guardar el archivo de proyecto: " +
                               this.archivoSeleccionActualPath + " - " + e.getMessage());
        }
    } // --- Fin del método guardarAArchivo ---


    @Override
    public List<Path> getImagenesMarcadas() {
    	List<Path> sortedList = new ArrayList<>(this.seleccionActual);
        Collections.sort(sortedList);
        return sortedList;
    } // --- Fin del método getImagenesMarcadas ---

    @Override
    public void gestionarSeleccionProyecto(Component parentComponent) {
        String message = "Funcionalidad para gestionar la selección de imágenes del proyecto (marcar, ver, guardar, cargar) aún no implementada.\n\n" +
                         "Actualmente se usa: " + (this.archivoSeleccionActualPath != null ? this.archivoSeleccionActualPath.toAbsolutePath() : "Ninguno") +
                         "\nImágenes seleccionadas: " + this.seleccionActual.size() + "\nImágenes descartadas: " + this.seleccionDescartada.size();
        JOptionPane.showMessageDialog(parentComponent,
                                      message,
                                      "Gestión de Selección de Proyecto (Pendiente)",
                                      JOptionPane.INFORMATION_MESSAGE);
        logger.debug("[ProjectManager] Diálogo placeholder 'gestionarSeleccionProyecto' mostrado.");
    } // --- Fin del método gestionarSeleccionProyecto ---
    
    @Override
    public void marcarImagenInterno(Path rutaAbsoluta) {
        if (rutaAbsoluta == null) return;
        if (this.seleccionActual.add(rutaAbsoluta)) {
            guardarAArchivo();
            logger.debug("  [ProjectManager] Imagen marcada (ruta abs): " + rutaAbsoluta);
        }
    } // --- Fin del método marcarImagenInterno ---

    @Override
    public void desmarcarImagenInterno(Path rutaAbsoluta) {
        if (rutaAbsoluta == null) return;
        if (this.seleccionActual.remove(rutaAbsoluta)) {
            guardarAArchivo();
            logger.debug("  [ProjectManager] Imagen desmarcada (ruta abs): " + rutaAbsoluta);
        }
    } // --- Fin del método desmarcarImagenInterno ---
    
    @Override
    public boolean estaMarcada(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        return this.seleccionActual.contains(rutaAbsolutaImagen);
    } // --- Fin del método estaMarcada ---

    @Override
    public boolean alternarMarcaImagen(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        boolean estabaMarcada = estaMarcada(rutaAbsolutaImagen);
        if (estabaMarcada) {
            desmarcarImagenInterno(rutaAbsolutaImagen);
            return false;
        } else {
            marcarImagenInterno(rutaAbsolutaImagen);
            return true;
        }
    } // --- Fin del método alternarMarcaImagen ---

    
    /**
     * Devuelve la lista de imágenes actualmente en la sección de descartes.
     * @return Una lista ordenada de Paths de las imágenes descartadas.
     */
    public List<Path> getImagenesDescartadas() {
        List<Path> sortedList = new ArrayList<>(this.seleccionDescartada);
        Collections.sort(sortedList);
        return sortedList;
    } // --- Fin del método getImagenesDescartadas ---

    /**
     * Mueve una imagen de la selección actual a la lista de descartes.
     * Si la imagen no estaba en la selección, no hace nada.
     * @param rutaAbsolutaImagen La ruta de la imagen a mover.
     */
    public void moverAdescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;
        if (this.seleccionActual.remove(rutaAbsolutaImagen)) {
            this.seleccionDescartada.add(rutaAbsolutaImagen);
            guardarAArchivo();
            logger.debug("  [ProjectManager] Imagen movida a descartes: " + rutaAbsolutaImagen);
        }
    } // --- Fin del método moverAdescartes ---

    /**
     * Mueve una imagen de la lista de descartes de vuelta a la selección actual.
     * Si la imagen no estaba en descartes, no hace nada.
     * @param rutaAbsolutaImagen La ruta de la imagen a restaurar.
     */
    public void restaurarDeDescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;
        if (this.seleccionDescartada.remove(rutaAbsolutaImagen)) {
            this.seleccionActual.add(rutaAbsolutaImagen);
            guardarAArchivo();
            logger.debug("  [ProjectManager] Imagen restaurada desde descartes: " + rutaAbsolutaImagen);
        }
    } // --- Fin del método restaurarDeDescartes ---

    /**
     * Comprueba si una imagen está actualmente en la lista de descartes.
     * @param rutaAbsolutaImagen La ruta de la imagen a comprobar.
     * @return true si está en descartes, false en caso contrario.
     */
    public boolean estaEnDescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        return this.seleccionDescartada.contains(rutaAbsolutaImagen);
    } // --- Fin del método estaEnDescartes ---
    
    /**
     * Elimina permanentemente una imagen del proyecto, quitándola tanto de la
     * lista de selección actual como de la de descartes.
     * Esta acción es irreversible para la sesión actual.
     *
     * @param rutaAbsolutaImagen La ruta de la imagen a eliminar del proyecto.
     */
    public void eliminarDeProyecto(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;

        // Intentar eliminar de ambas listas. El método remove() devuelve true
        // si el elemento existía y fue eliminado.
        boolean removidoDeSeleccion = this.seleccionActual.remove(rutaAbsolutaImagen);
        boolean removidoDeDescartes = this.seleccionDescartada.remove(rutaAbsolutaImagen);

        // Solo guardar el archivo si realmente se hizo un cambio.
        if (removidoDeSeleccion || removidoDeDescartes) {
            guardarAArchivo();
            logger.debug("  [ProjectManager] Imagen eliminada permanentemente del proyecto: " + rutaAbsolutaImagen);
        }
    } // --- Fin del método eliminarDeProyecto ---
    
    
    /**
     * Inyecta el gestor de configuración.
     * @param configManager La instancia de ConfigurationManager.
     */
    public void setConfigManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null en ProjectManager");
    } // --- Fin del método setConfigManager ---

} // --- FIN de la clase ProjectManager ---


    // -------------------------------------------------------------------------
    // --- MEGACOMENTARIO: PLAN DE IMPLEMENTACIÓN SELECCIÓN DE PROYECTO ---
    // -------------------------------------------------------------------------
    /*
     * == Plan Detallado para la Funcionalidad de "Selección de Imágenes para Proyecto" ==
     *
     * OBJETIVO PRINCIPAL:
     * Permitir al usuario marcar imágenes individuales de interés dentro de un directorio
     * grande, guardar esta selección como un "proyecto", y poder ver/cargar estas
     * selecciones posteriormente. Esto es para ayudar en proyectos de impresión 3D
     * donde las imágenes representan los archivos STL.
     *
     * TERMINOLOGÍA:
     * - "Selección de Proyecto" o "Imágenes Marcadas": El conjunto de imágenes que el usuario ha marcado.
     * - "Archivo de Proyecto": El archivo en disco (ej. .prj, .txt) que guarda una Selección de Proyecto.
     * - "Archivo de Selección Temporal": Un archivo por defecto donde se guardan las marcas si no se ha guardado/cargado un proyecto.
     *
     * ============================
     * ITERACIÓN 1: FUNCIONALIDAD BÁSICA (Archivo Temporal Único, Marcar/Desmarcar, Ver Lista Simple)
     * ============================
     *
     * 1. ProjectSelectionManager (Clase Principal):
     *    - Campos:
     *        - `private final Path archivoSeleccionTemporal = Paths.get(System.getProperty("user.home"), ".miVisorImagenes", "seleccion_temporal.txt");`
     *          (Asegurar que el directorio `~/.miVisorImagenes` se cree si no existe).
     *        - `private Set<String> clavesMarcadasEnMemoria;` (claves relativas de imágenes).
     *    - Constructor:
     *        - Llama a `cargarDesdeArchivo(archivoSeleccionTemporal)` para poblar `clavesMarcadasEnMemoria`.
     *    - Métodos Privados:
     *        - `cargarDesdeArchivo(Path rutaArchivo)`: Lee el archivo, llena `clavesMarcadasEnMemoria`. Maneja si el archivo no existe.
     *        - `guardarAArchivo(Path rutaArchivo, Set<String> claves)`: Escribe el Set al archivo, una clave por línea.
     *    - Métodos Públicos:
     *        - `marcarImagen(String claveRelativa)`:
     *            - Añade `claveRelativa` a `clavesMarcadasEnMemoria`.
     *            - Llama a `guardarAArchivo(archivoSeleccionTemporal, clavesMarcadasEnMemoria)`.
     *        - `desmarcarImagen(String claveRelativa)`:
     *            - Quita `claveRelativa` de `clavesMarcadasEnMemoria`.
     *            - Llama a `guardarAArchivo(archivoSeleccionTemporal, clavesMarcadasEnMemoria)`.
     *        - `estaMarcada(String claveRelativa)`:
     *            - Devuelve `clavesMarcadasEnMemoria.contains(claveRelativa)`.
     *        - `getClavesMarcadas()`:
     *            - Devuelve `new ArrayList<>(clavesMarcadasEnMemoria)` (o una copia inmutable).
     *        - `alternarMarcaImagen(String claveRelativa)`:
     *            - Si `estaMarcada`, llama a `desmarcarImagen`. Sino, llama a `marcarImagen`.
     *            - Devuelve el nuevo estado (true si quedó marcada, false si desmarcada).
     *
     * 2. AppActionCommands:
     *    - `CMD_PROYECTO_TOGGLE_MARCA = "cmd.proyecto.toggle_marca";`
     *    - `CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO = "cmd.proyecto.mostrar_seleccion_dialogo";` // Para el JOptionPane inicial
     *
     * 3. Nuevas Actions (en controlador.actions.proyecto o similar):
     *    - `ToggleMarkImageAction extends BaseVisorAction`:
     *        - Comando: `CMD_PROYECTO_TOGGLE_MARCA`.
     *        - Icono: `5003-marcar_imagen_48x48.png` (o el nombre final).
     *        - Tooltip: "Marcar/Desmarcar imagen para el proyecto actual".
     *        - `actionPerformed`:
     *            - Obtiene `selectedImageKey` del `VisorModel`.
     *            - Si no es null, llama a `controller.toggleMarcaImagenActual(selectedImageKey);` (nuevo método en controller).
     *    - `ShowProjectSelectionDialogAction extends BaseVisorAction`:
     *        - Comando: `CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO`.
     *        - Icono: `7003-Mostrar_Favoritos_48x48.png` (o el nombre final).
     *        - Tooltip: "Mostrar imágenes marcadas para el proyecto actual".
     *        - `actionPerformed`:
     *            - Llama a `controller.mostrarDialogoSeleccionProyectoActual();` (nuevo método en controller).
     *
     * 4. VisorController:
     *    - Añadir campo: `private ProjectSelectionManager projectManager;` (inicializar en `AppInitializer`).
     *    - Inicializar `toggleMarkImageAction` y `showProjectSelectionDialogAction`. Añadirlas al `actionMap`.
     *    - Nuevo método: `public void toggleMarcaImagenActual(String claveImagen)`:
     *        - Llama a `projectManager.alternarMarcaImagen(claveImagen)` para obtener `boolean nuevoEstadoMarcada`.
     *        - Actualiza `toggleMarkImageAction.putValue(Action.SELECTED_KEY, nuevoEstadoMarcada);`.
     *        - Llama a `actualizarEstadoVisualBotonMarcarYBarraEstado(nuevoEstadoMarcada);` (nuevo método).
     *    - Nuevo método: `public void actualizarEstadoVisualBotonMarcarYBarraEstado(boolean estaMarcada)`:
     *        - Llama a `view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);`.
     *        - Actualiza `view.setTextoRuta()` para añadir/quitar "[MARCADA]".
     *    - Nuevo método: `public void mostrarDialogoSeleccionProyectoActual()`:
     *        - Llama a `projectManager.getClavesMarcadas()`.
     *        - Construye un String con estas claves.
     *        - Muestra el String en un `JOptionPane.showMessageDialog`.
     *    - En `actualizarImagenPrincipal(int indiceSeleccionado)`:
     *        - Después de cargar la imagen y actualizar el modelo, obtener `selectedImageKey`.
     *        - Llamar a `boolean marcada = projectManager.estaMarcada(selectedImageKey);`.
     *        - Llamar a `actualizarEstadoVisualBotonMarcarYBarraEstado(marcada);`.
     *
     * 5. UIDefinitionService:
     *    - `generateMenuStructure()`:
     *        - En Menú "Imagen": `new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, MenuItemType.CHECKBOX_ITEM, "Marcar para Proyecto", null)`
     *        - En Menú "Vista" (o nuevo menú "Proyecto"): `new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO, MenuItemType.ITEM, "Ver Selección de Proyecto", null)`
     *    - `generateToolbarStructure()`:
     *        - Botón "Marcar": `new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, "5003-marcar_imagen_48x48.png", "Marcar/Desmarcar Imagen", "control")` (o la categoría que prefieras).
     *        - Botón "Mostrar Selección": `new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO, "7003-Mostrar_Favoritos_48x48.png", "Mostrar Imágenes Marcadas", "control")` (o la categoría que prefieras).
     *
     * 6. VisorView (Opcional para Iteración 1, pero bueno para el futuro):
     *    - Si `ToggleMarkImageAction` no cambia el icono del botón directamente, `actualizarAspectoBotonToggle` podría necesitar lógica para cambiar entre icono de estrella vacía/llena.
     *
     * ============================
     * ITERACIÓN 2: GESTIÓN DE PROYECTOS CON NOMBRE (Guardar Como, Abrir, Nuevo)
     * ============================
     *
     * 1. ProjectSelectionManager:
     *    - Campo: `private Path archivoProyectoActivo;` (puede ser null si es el temporal).
     *    - Modificar constructor y métodos para usar `archivoProyectoActivo` si no es null, sino `archivoSeleccionTemporal`.
     *    - `nuevoProyecto()`: `clavesMarcadasEnMemoria.clear(); archivoProyectoActivo = null; guardarAArchivo(archivoSeleccionTemporal, ...);` Actualizar título de ventana.
     *    - `guardarProyectoComo(Path destino)`: `guardarAArchivo(destino, clavesMarcadasEnMemoria); archivoProyectoActivo = destino;` Actualizar título.
     *    - `abrirProyecto(Path origen)`: `cargarDesdeArchivo(origen); archivoProyectoActivo = origen;` Actualizar título.
     *    - `hayCambiosSinGuardar()`: Compara `clavesMarcadasEnMemoria` con el contenido de `archivoProyectoActivo` (si existe).
     *
     * 2. AppActionCommands:
     *    - `CMD_PROYECTO_NUEVO`, `CMD_PROYECTO_ABRIR`, `CMD_PROYECTO_GUARDAR_COMO`, `CMD_PROYECTO_GUARDAR` (si el proyecto activo tiene nombre).
     *
     * 3. Nuevas Actions: `NuevoProyectoAction`, `AbrirProyectoAction`, `GuardarProyectoComoAction`, `GuardarProyectoAction`.
     *
     * 4. VisorController:
     *    - Métodos para manejar estas nuevas acciones, usando `JFileChooser` para guardar/abrir.
     *    - Lógica para "Guardar" (si `archivoProyectoActivo` no es null, guarda ahí; sino, actúa como "Guardar Como").
     *    - Modificar `ShutdownHook`: Si `projectManager.hayCambiosSinGuardar()`, preguntar al usuario si desea guardar antes de salir. Si guarda, y es temporal, preguntar si quiere darle nombre. Si no guarda y es temporal, se podría borrar `archivoSeleccionTemporal`.
     *    - Actualizar título de la `JFrame` (`VisorView`) para incluir el nombre del proyecto activo o "(Temporal)".
     *
     * 5. UIDefinitionService:
     *    - Nuevo Menú "Proyecto" con ítems para Nuevo, Abrir, Guardar, Guardar Como.
     *
     * ============================
     * ITERACIÓN 3: VISTA INTEGRADA DE SELECCIÓN DE PROYECTO (Toggle de ListModel)
     * ============================
     *
     * 1. AppActionCommands:
     *    - `CMD_PROYECTO_TOGGLE_VISTA_SELECCION = "cmd.proyecto.toggle_vista_seleccion";`
     *
     * 2. Nueva Action: `ToggleVistaSeleccionProyectoAction extends BaseVisorAction`.
     *    - `actionPerformed` llama a `controller.toggleVistaSeleccionProyecto();`.
     *    - Mantiene `Action.SELECTED_KEY` para el estado del toggle.
     *
     * 3. VisorModel:
     *    - `private boolean mostrandoSeleccionProyecto = false;`
     *    - `private DefaultListModel<String> modeloListaCarpetaOriginal;`
     *    - `private String claveSeleccionadaEnCarpetaOriginal;`
     *
     * 4. VisorController:
     *    - `toggleVistaSeleccionProyecto()`:
     *        - Invierte `model.mostrandoSeleccionProyecto`.
     *        - Actualiza `Action.SELECTED_KEY` de `ToggleVistaSeleccionProyectoAction`.
     *        - Llama a `actualizarAspectoBotonToggle(...)` para el botón de la toolbar.
     *        - Llama a `refrescarVistaPrincipalSegunModo();` (nuevo método).
     *    - `refrescarVistaPrincipalSegunModo()`:
     *        - Si `model.mostrandoSeleccionProyecto`:
     *            - Guarda `model.getModeloLista()` en `model.modeloListaCarpetaOriginal`.
     *            - Guarda `model.getSelectedImageKey()` en `model.claveSeleccionadaEnCarpetaOriginal`.
     *            - Obtiene `projectManager.getClavesMarcadas()`.
     *            - Crea `nuevoModeloSeleccion` a partir de esas claves.
     *            - Llama a `model.setModeloLista(nuevoModeloSeleccion);` (¡OJO! Este método debe ser cuidadoso para no limpiar `selectedImageKey` si la clave anterior está en el nuevo modelo).
     *            - Llama a `view.setListaImagenesModel(nuevoModeloSeleccion);` (o que `cargarListaImagenes` lo haga).
     *            - Llama a `cargarListaImagenes(primeraClaveDeSeleccionSiExiste)`.
     *            - Cambia título del panel izquierdo: "Selección: [Nombre Proyecto]".
     *        - Else (volviendo a vista de carpeta):
     *            - Llama a `model.setModeloLista(model.modeloListaCarpetaOriginal);`.
     *            - Llama a `view.setListaImagenesModel(...)`.
     *            - Llama a `cargarListaImagenes(model.claveSeleccionadaEnCarpetaOriginal)`.
     *            - Cambia título del panel izquierdo: "Lista de Archivos".
     *
     * 5. UIDefinitionService:
     *    - Botón en Toolbar para `CMD_PROYECTO_TOGGLE_VISTA_SELECCION`.
     *    - Ítem de Menú (quizás `JCheckBoxMenuItem`) en "Vista" para `CMD_PROYECTO_TOGGLE_VISTA_SELECCION`.
     *
     * ============================
     * CONSIDERACIONES ADICIONALES (Futuras):
     * ============================
     * - **Modo Grid y Marcar:** Deshabilitar `ToggleMarkImageAction` general. Añadir menú contextual en el grid.
     * - **Rendimiento:** Si `getClavesMarcadas()` es muy grande y se llama frecuentemente para la UI, optimizar.
     * - **Multi-selección para Marcar/Desmarcar:** En `listaNombres` o Grid.
     * - **Internacionalización (i18n)** de todos los textos.
     * - **Deshacer/Rehacer** para marcado/desmarcado.
     *
     */
    // -------------------------------------------------------------------------
    // --- FIN MEGACOMENTARIO ---
    // -------------------------------------------------------------------------
