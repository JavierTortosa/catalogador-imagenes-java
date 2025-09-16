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
import java.util.LinkedHashMap; 
import java.util.List;
import java.util.Map; 
import java.util.Objects;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;

public class ProjectManager implements IProjectManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);
	
    // --- INICIO DE LA MODIFICACIÓN ---
    private static final String SEPARADOR_ETIQUETA = "||";
    // --- FIN DE LA MODIFICACIÓN ---

    private Path archivoSeleccionActualPath;
    private ConfigurationManager configManager;

    // --- INICIO DE NUEVOS CAMPOS PARA MULTIPROYECTO ---
    private Path archivoProyectoActivo; // Guarda la ruta del proyecto .prj cargado. Es null si trabajamos con el temporal.
    private Path carpetaBaseProyectos; // Directorio donde se guardan los proyectos.
    private Path archivoSeleccionTemporalPath; // Ruta completa al archivo temporal.
    // --- FIN DE NUEVOS CAMPOS ---

    private Map<Path, String> seleccionActual;
    private Map<Path, String> seleccionDescartada;
    
    private boolean hayCambiosSinGuardar = false;

    public ProjectManager() {
    	this.seleccionActual = new LinkedHashMap<>();
        this.seleccionDescartada = new LinkedHashMap<>();
    } // --- Fin del método ProjectManager (constructor) ---

    
    public void initialize() {
        if (this.configManager == null) {
            throw new IllegalStateException("ProjectManager no puede inicializarse sin ConfigurationManager.");
        }
        inicializarRutaArchivoSeleccion(); // Este método ahora configura el archivo temporal.
        cargarProyectoActivo(); // Carga el proyecto activo (temporal o el último guardado).
        
        logger.debug("[ProjectManager] Instancia inicializada. Selección actual desde: " + (this.archivoProyectoActivo != null ? this.archivoProyectoActivo.toAbsolutePath() : this.archivoSeleccionTemporalPath.toAbsolutePath()));
        logger.debug("  -> Proyecto inicial cargado. Selección: " + this.seleccionActual.size() + ", Descartes: " + this.seleccionDescartada.size());
    } // --- Fin del método initialize ---
    
    // --- MÉTODO MODIFICADO ---
    private void inicializarRutaArchivoSeleccion() {
        String carpetaBaseProyectosStr = this.configManager.getString(
            ConfigKeys.PROYECTOS_CARPETA_BASE,
            ".project_selections"
        );
        this.carpetaBaseProyectos = Paths.get(carpetaBaseProyectosStr);

        if (!this.carpetaBaseProyectos.isAbsolute()) {
            String userHome = System.getProperty("user.home");
            this.carpetaBaseProyectos = Paths.get(userHome, ".miVisorImagenesApp", carpetaBaseProyectosStr).toAbsolutePath();
        }

        try {
            if (!Files.exists(this.carpetaBaseProyectos)) {
                Files.createDirectories(this.carpetaBaseProyectos);
            }
        } catch (IOException e) {
            logger.warn("WARN [ProjectManager]: No se pudo crear el directorio base de proyectos.", e);
            this.carpetaBaseProyectos = Paths.get("").toAbsolutePath();
        }

        String nombreArchivoTemporal = this.configManager.getString(
            ConfigKeys.PROYECTOS_ARCHIVO_TEMPORAL,
            "seleccion_temporal_rutas.txt"
        );
        this.archivoSeleccionTemporalPath = this.carpetaBaseProyectos.resolve(nombreArchivoTemporal);
        // El obsoleto 'archivoSeleccionActualPath' ahora apunta al temporal por defecto.
        this.archivoSeleccionActualPath = this.archivoSeleccionTemporalPath;
    } // --- Fin del método inicializarRutaArchivoSeleccion ---

    // --- NUEVO MÉTODO DE INICIALIZACIÓN ---
    private void cargarProyectoActivo() {
        // Lógica para determinar qué proyecto cargar al inicio.
        // Por ahora, simplemente cargamos el temporal. En el futuro podría cargar el último abierto.
        this.archivoProyectoActivo = null; // Empezamos en modo temporal.
        cargarDesdeArchivo(this.archivoSeleccionTemporalPath);
    } // --- Fin del método cargarProyectoActivo ---
    

    private void cargarDesdeArchivo(Path rutaArchivo) {
        this.seleccionActual.clear();
        this.seleccionDescartada.clear();

        if (Files.exists(rutaArchivo) && Files.isReadable(rutaArchivo)) {
            String seccionActual = "SELECCION"; 

            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) {
                        continue;
                    }

                    if (linea.equalsIgnoreCase("[SELECCION]")) {
                        seccionActual = "SELECCION";
                        continue;
                    } else if (linea.equalsIgnoreCase("[DESCARTES]")) {
                        seccionActual = "DESCARTES";
                        continue;
                    }

                    try {
                        String rutaStr;
                        String etiqueta = null;
                        
                        if (linea.contains(SEPARADOR_ETIQUETA)) {
                            String[] partes = linea.split("\\|\\|", 2);
                            rutaStr = partes[0];
                            etiqueta = (partes.length > 1) ? partes[1] : null;
                        } else {
                            rutaStr = linea;
                        }

                        Path rutaParseada = Paths.get(rutaStr);
                        
                        if ("SELECCION".equals(seccionActual)) {
                            this.seleccionActual.put(rutaParseada, etiqueta);
                        } else if ("DESCARTES".equals(seccionActual)) {
                            this.seleccionDescartada.put(rutaParseada, etiqueta);
                        }

                    } catch (Exception e) {
                        logger.warn("WARN [PM cargar]: Ruta inválida en archivo de proyecto: '" + linea + "'", e);
                    }
                }
                logger.debug("  [ProjectManager] Proyecto cargado desde {}. Selección: {}, Descartes: {}", 
                             rutaArchivo, this.seleccionActual.size(), this.seleccionDescartada.size());

            } catch (IOException e) {
                logger.error("ERROR [ProjectManager]: No se pudo leer el archivo de proyecto: " + rutaArchivo, e);
            }
        } else {
            logger.debug("  [ProjectManager] Archivo de proyecto no encontrado: {}. Se iniciará con proyecto vacío.", rutaArchivo);
        }
    } // --- Fin del método cargarDesdeArchivo ---
    
    // --- MÉTODO MODIFICADO ---
    public void guardarAArchivo() {
        Path rutaGuardado = (this.archivoProyectoActivo != null) ? this.archivoProyectoActivo : this.archivoSeleccionTemporalPath;
        if (rutaGuardado == null) {
            logger.error("ERROR [PM guardar]: La ruta de guardado es null. No se puede guardar.");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(rutaGuardado,
                                                            StandardOpenOption.CREATE,
                                                            StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# VisorImagenes - Archivo de Proyecto");
            writer.newLine();
            writer.newLine();

            writer.write("[SELECCION]");
            writer.newLine();
            for (Map.Entry<Path, String> entry : this.seleccionActual.entrySet()) {
                String linea = entry.getKey().toString().replace("\\", "/");
                String etiqueta = entry.getValue();
                if (etiqueta != null && !etiqueta.isBlank()) {
                    linea += SEPARADOR_ETIQUETA + etiqueta;
                }
                writer.write(linea);
                writer.newLine();
            }

            writer.newLine();
            writer.write("[DESCARTES]");
            writer.newLine();
            for (Map.Entry<Path, String> entry : this.seleccionDescartada.entrySet()) {
                writer.write(entry.getKey().toString().replace("\\", "/"));
                writer.newLine();
            }

            logger.debug("  [ProjectManager] Proyecto guardado en {} (Selección: {}, Descartes: {}).",
                         rutaGuardado, this.seleccionActual.size(), this.seleccionDescartada.size());

            this.hayCambiosSinGuardar = false;
            
        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo guardar el archivo de proyecto: " + rutaGuardado, e);
        }
    } // --- Fin del método guardarAArchivo ---
    
    // --- INICIO DE NUEVOS MÉTODOS PÚBLICOS PARA MULTIPROYECTO ---

    public void nuevoProyecto() {
        logger.info("[ProjectManager] Creando nuevo proyecto.");
        this.seleccionActual.clear();
        this.seleccionDescartada.clear();
        this.archivoProyectoActivo = null;
        
        this.hayCambiosSinGuardar = false;
        
        guardarAArchivo(); // Guarda el estado vacío en el archivo temporal.
        
    } // ---FIN de metodo nuevoProyecto---

    
    public void abrirProyecto(Path rutaArchivo) throws ProyectoIOException { // <-- CAMBIO: Añadido 'throws'
        logger.info("[ProjectManager] Abriendo proyecto desde: {}", rutaArchivo);
        
        if (rutaArchivo == null || !Files.isReadable(rutaArchivo)) {
            String errorMsg = "Error al abrir proyecto: La ruta es nula o el archivo no se puede leer. Ruta: " + rutaArchivo;
            logger.error(errorMsg);
            // Lanzamos la excepción para que quien nos llamó se entere del problema.
            throw new ProyectoIOException(errorMsg);
        }

        // La lógica de cargarDesdeArchivo puede lanzar una IOException normal, así que ya está cubierta.
        cargarDesdeArchivo(rutaArchivo);
        
        this.hayCambiosSinGuardar = false;
        
        this.archivoProyectoActivo = rutaArchivo;
        
    } // ---FIN de metodo abrirProyecto---
    

    public void guardarProyectoComo(Path rutaArchivo) {
        logger.info("[ProjectManager] Guardando proyecto como: {}", rutaArchivo);
        this.archivoProyectoActivo = rutaArchivo;
        guardarAArchivo(); // Guarda el contenido actual en la nueva ruta.
    } // ---FIN de metodo guardarProyectoComo---
    
    public Path getArchivoProyectoActivo() {
        return this.archivoProyectoActivo;
    } // ---FIN de metodo getArchivoProyectoActivo---
    
    public String getNombreProyectoActivo() {
        if (this.archivoProyectoActivo != null) {
            return this.archivoProyectoActivo.getFileName().toString();
        }
        return "Proyecto Temporal";
    } // ---FIN de metodo getNombreProyectoActivo---
    
    public Path getCarpetaBaseProyectos() {
        return this.carpetaBaseProyectos;
    } // ---FIN de metodo getCarpetaBaseProyectos---

    // --- FIN DE NUEVOS MÉTODOS ---

    public String getEtiqueta(Path rutaImagen) {
        if (rutaImagen == null) return null;
        return seleccionActual.get(rutaImagen);
    } // ---FIN de metodo getEtiqueta ---

    public void setEtiqueta(Path rutaImagen, String etiqueta) {
    	
    	this.hayCambiosSinGuardar = true;
        
    	if (rutaImagen == null) return;
        if (seleccionActual.containsKey(rutaImagen)) {
            seleccionActual.put(rutaImagen, etiqueta);
            guardarAArchivo();
            logger.debug("Etiqueta '{}' asignada a: {}", etiqueta, rutaImagen.getFileName());
        } else {
            logger.warn("Intento de etiquetar una imagen que no está en la selección actual: {}", rutaImagen.getFileName());
        }
    } // ---FIN de metodo setEtiqueta ---


    @Override
    public List<Path> getImagenesMarcadas() {
    	return new ArrayList<>(this.seleccionActual.keySet());
    } // --- Fin del método getImagenesMarcadas ---

    @Override
    public void gestionarSeleccionProyecto(Component parentComponent) {
        String message = "Funcionalidad para gestionar la selección de imágenes del proyecto (marcar, ver, guardar, cargar) aún no implementada.\n\n" +
                         "Actualmente se usa: " + (this.archivoSeleccionActualPath != null ? this.archivoSeleccionActualPath.toAbsolutePath() : "Ninguno") +
                         "\nImágenes seleccionadas: " + this.seleccionActual.size() + "\nImágenes descartadas: " + this.seleccionDescartada.size();
        JOptionPane.showMessageDialog(parentComponent, message, "Gestión de Selección de Proyecto (Pendiente)", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin del método gestionarSeleccionProyecto ---
    
    @Override
    public void marcarImagenInterno(Path rutaAbsoluta) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (rutaAbsoluta == null) return;
        if (this.seleccionActual.putIfAbsent(rutaAbsoluta, null) == null) {
            guardarAArchivo();
        }
    } // --- Fin del método marcarImagenInterno ---

    @Override
    public void desmarcarImagenInterno(Path rutaAbsoluta) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (rutaAbsoluta == null) return;
        if (this.seleccionActual.remove(rutaAbsoluta) != null) {
            guardarAArchivo();
        }
    } // --- Fin del método desmarcarImagenInterno ---
    
    @Override
    public boolean estaMarcada(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        return this.seleccionActual.containsKey(rutaAbsolutaImagen);
    } // --- Fin del método estaMarcada ---

    @Override
    public boolean alternarMarcaImagen(Path rutaAbsolutaImagen) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (estaMarcada(rutaAbsolutaImagen)) {
            desmarcarImagenInterno(rutaAbsolutaImagen);
            return false;
        } else {
            marcarImagenInterno(rutaAbsolutaImagen);
            return true;
        }
    } // --- Fin del método alternarMarcaImagen ---
    
    @Override
    public boolean hayCambiosSinGuardar() {
        return this.hayCambiosSinGuardar;
    }

    @Override
    public void notificarModificacion() {
        if (!this.hayCambiosSinGuardar) {
            this.hayCambiosSinGuardar = true;
            // Aquí podrías notificar a un listener si tuvieras esa arquitectura,
            // pero por ahora, el cambio de título lo hará el controller.
        }
    }
    
    
    

    public void vaciarDescartes() {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (this.seleccionDescartada.isEmpty()) {
            return;
        }
        this.seleccionDescartada.clear();
        guardarAArchivo();
        logger.debug("[ProjectManager] Lista de descartes vaciada.");
    } // --- Fin del método vaciarDescartes ---
    
    public List<Path> getImagenesDescartadas() {
    	return new ArrayList<>(this.seleccionDescartada.keySet());
    } // --- Fin del método getImagenesDescartadas ---

    public void moverAdescartes(Path rutaAbsolutaImagen) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (rutaAbsolutaImagen == null) return;
        if (this.seleccionActual.containsKey(rutaAbsolutaImagen)) {
            this.seleccionActual.remove(rutaAbsolutaImagen);
            this.seleccionDescartada.put(rutaAbsolutaImagen, null);
            
            notificarModificacion();
            
            guardarAArchivo();
        }
    } // --- Fin del método moverAdescartes ---

    public void restaurarDeDescartes(Path rutaAbsolutaImagen) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (rutaAbsolutaImagen == null) return;
        if (this.seleccionDescartada.containsKey(rutaAbsolutaImagen)) {
            this.seleccionDescartada.remove(rutaAbsolutaImagen);
            this.seleccionActual.put(rutaAbsolutaImagen, null);
            guardarAArchivo();
        }
    } // --- Fin del método restaurarDeDescartes ---

    public boolean estaEnDescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        return this.seleccionDescartada.containsKey(rutaAbsolutaImagen);
    } // --- Fin del método estaEnDescartes ---
    
    public void eliminarDeProyecto(Path rutaAbsolutaImagen) {
    	
    	this.hayCambiosSinGuardar = true;
    	
        if (rutaAbsolutaImagen == null) return;
        boolean removidoDeSeleccion = this.seleccionActual.remove(rutaAbsolutaImagen) != null;
        boolean removidoDeDescartes = this.seleccionDescartada.remove(rutaAbsolutaImagen) != null;
        if (removidoDeSeleccion || removidoDeDescartes) {
            guardarAArchivo();
        }
    } // --- Fin del método eliminarDeProyecto ---
    
    public void setConfigManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null en ProjectManager");
    } // --- Fin del método setConfigManager ---

    /**
     * Vacía el contenido del archivo de selección temporal.
     * Este método se debe llamar después de que el usuario guarde el proyecto
     * con un nombre específico ("Guardar Como..."), para evitar tener datos
     * duplicados y potencialmente conflictivos.
     */
    public void limpiarArchivoTemporal() {
        if (archivoSeleccionTemporalPath == null) {
            logger.warn("WARN [limpiarArchivoTemporal]: La ruta del archivo temporal es nula. No se puede limpiar.");
            return;
        }

        // Si el archivo temporal no existe, no hay nada que hacer.
        if (!Files.exists(archivoSeleccionTemporalPath)) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(archivoSeleccionTemporalPath,
                                                            StandardOpenOption.CREATE,
                                                            StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# Archivo temporal de proyecto. Vaciado después de 'Guardar Como...'.");
            writer.newLine();
            logger.info("El archivo de proyecto temporal ({}) ha sido vaciado.", archivoSeleccionTemporalPath.getFileName());
        } catch (IOException e) {
            logger.error("ERROR [limpiarArchivoTemporal]: No se pudo vaciar el archivo de proyecto temporal: " + archivoSeleccionTemporalPath, e);
        }
    } // ---FIN de metodo limpiarArchivoTemporal---
    
    
    /**
     * Renombra el archivo de selección temporal a un archivo de respaldo con fecha y hora.
     * Se debe llamar al cerrar la aplicación para asegurar un arranque limpio la próxima vez.
     * Si el archivo temporal está vacío o no existe, simplemente lo elimina.
     */
    @Override
    public void archivarTemporalAlCerrar() {
        if (archivoSeleccionTemporalPath == null || !Files.exists(archivoSeleccionTemporalPath)) {
            return; // No hay nada que archivar
        }

        try {
            // Si el archivo está vacío (o tiene un tamaño muy pequeño, como solo el header), lo borramos.
            if (Files.size(archivoSeleccionTemporalPath) < 50) {
                Files.delete(archivoSeleccionTemporalPath);
                logger.info("Archivo temporal vacío o casi vacío eliminado en el cierre.");
                return;
            }
            
            // Creamos un nombre de archivo para el respaldo con timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String nombreBase = archivoSeleccionTemporalPath.getFileName().toString().replace(".txt", "");
            Path rutaRespaldo = archivoSeleccionTemporalPath.resolveSibling(nombreBase + "_backup_" + timestamp + ".txt");

            // Renombramos el archivo
            Files.move(archivoSeleccionTemporalPath, rutaRespaldo);
            logger.info("Proyecto temporal archivado como: {}", rutaRespaldo.getFileName());

        } catch (IOException e) {
            logger.error("ERROR [archivarTemporalAlCerrar]: No se pudo archivar el archivo de proyecto temporal.", e);
        }
    } // ---FIN de metodo archivarTemporalAlCerrar---
    
    
    
} // --- FIN de la clase ProjectManager ---



//    // -------------------------------------------------------------------------
//    // --- MEGACOMENTARIO: PLAN DE IMPLEMENTACIÓN SELECCIÓN DE PROYECTO ---
//    // -------------------------------------------------------------------------
//    /*
//     * == Plan Detallado para la Funcionalidad de "Selección de Imágenes para Proyecto" ==
//     *
//     * OBJETIVO PRINCIPAL:
//     * Permitir al usuario marcar imágenes individuales de interés dentro de un directorio
//     * grande, guardar esta selección como un "proyecto", y poder ver/cargar estas
//     * selecciones posteriormente. Esto es para ayudar en proyectos de impresión 3D
//     * donde las imágenes representan los archivos STL.
//     *
//     * TERMINOLOGÍA:
//     * - "Selección de Proyecto" o "Imágenes Marcadas": El conjunto de imágenes que el usuario ha marcado.
//     * - "Archivo de Proyecto": El archivo en disco (ej. .prj, .txt) que guarda una Selección de Proyecto.
//     * - "Archivo de Selección Temporal": Un archivo por defecto donde se guardan las marcas si no se ha guardado/cargado un proyecto.
//     *
//     * ============================
//     * ITERACIÓN 1: FUNCIONALIDAD BÁSICA (Archivo Temporal Único, Marcar/Desmarcar, Ver Lista Simple)
//     * ============================
//     *
//     * 1. ProjectSelectionManager (Clase Principal):
//     *    - Campos:
//     *        - `private final Path archivoSeleccionTemporal = Paths.get(System.getProperty("user.home"), ".miVisorImagenes", "seleccion_temporal.txt");`
//     *          (Asegurar que el directorio `~/.miVisorImagenes` se cree si no existe).
//     *        - `private Set<String> clavesMarcadasEnMemoria;` (claves relativas de imágenes).
//     *    - Constructor:
//     *        - Llama a `cargarDesdeArchivo(archivoSeleccionTemporal)` para poblar `clavesMarcadasEnMemoria`.
//     *    - Métodos Privados:
//     *        - `cargarDesdeArchivo(Path rutaArchivo)`: Lee el archivo, llena `clavesMarcadasEnMemoria`. Maneja si el archivo no existe.
//     *        - `guardarAArchivo(Path rutaArchivo, Set<String> claves)`: Escribe el Set al archivo, una clave por línea.
//     *    - Métodos Públicos:
//     *        - `marcarImagen(String claveRelativa)`:
//     *            - Añade `claveRelativa` a `clavesMarcadasEnMemoria`.
//     *            - Llama a `guardarAArchivo(archivoSeleccionTemporal, clavesMarcadasEnMemoria)`.
//     *        - `desmarcarImagen(String claveRelativa)`:
//     *            - Quita `claveRelativa` de `clavesMarcadasEnMemoria`.
//     *            - Llama a `guardarAArchivo(archivoSeleccionTemporal, clavesMarcadasEnMemoria)`.
//     *        - `estaMarcada(String claveRelativa)`:
//     *            - Devuelve `clavesMarcadasEnMemoria.contains(claveRelativa)`.
//     *        - `getClavesMarcadas()`:
//     *            - Devuelve `new ArrayList<>(clavesMarcadasEnMemoria)` (o una copia inmutable).
//     *        - `alternarMarcaImagen(String claveRelativa)`:
//     *            - Si `estaMarcada`, llama a `desmarcarImagen`. Sino, llama a `marcarImagen`.
//     *            - Devuelve el nuevo estado (true si quedó marcada, false si desmarcada).
//     *
//     * 2. AppActionCommands:
//     *    - `CMD_PROYECTO_TOGGLE_MARCA = "cmd.proyecto.toggle_marca";`
//     *    - `CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO = "cmd.proyecto.mostrar_seleccion_dialogo";` // Para el JOptionPane inicial
//     *
//     * 3. Nuevas Actions (en controlador.actions.proyecto o similar):
//     *    - `ToggleMarkImageAction extends BaseVisorAction`:
//     *        - Comando: `CMD_PROYECTO_TOGGLE_MARCA`.
//     *        - Icono: `5003-marcar_imagen_48x48.png` (o el nombre final).
//     *        - Tooltip: "Marcar/Desmarcar imagen para el proyecto actual".
//     *        - `actionPerformed`:
//     *            - Obtiene `selectedImageKey` del `VisorModel`.
//     *            - Si no es null, llama a `controller.toggleMarcaImagenActual(selectedImageKey);` (nuevo método en controller).
//     *    - `ShowProjectSelectionDialogAction extends BaseVisorAction`:
//     *        - Comando: `CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO`.
//     *        - Icono: `7003-Mostrar_Favoritos_48x48.png` (o el nombre final).
//     *        - Tooltip: "Mostrar imágenes marcadas para el proyecto actual".
//     *        - `actionPerformed`:
//     *            - Llama a `controller.mostrarDialogoSeleccionProyectoActual();` (nuevo método en controller).
//     *
//     * 4. VisorController:
//     *    - Añadir campo: `private ProjectSelectionManager projectManager;` (inicializar en `AppInitializer`).
//     *    - Inicializar `toggleMarkImageAction` y `showProjectSelectionDialogAction`. Añadirlas al `actionMap`.
//     *    - Nuevo método: `public void toggleMarcaImagenActual(String claveImagen)`:
//     *        - Llama a `projectManager.alternarMarcaImagen(claveImagen)` para obtener `boolean nuevoEstadoMarcada`.
//     *        - Actualiza `toggleMarkImageAction.putValue(Action.SELECTED_KEY, nuevoEstadoMarcada);`.
//     *        - Llama a `actualizarEstadoVisualBotonMarcarYBarraEstado(nuevoEstadoMarcada);` (nuevo método).
//     *    - Nuevo método: `public void actualizarEstadoVisualBotonMarcarYBarraEstado(boolean estaMarcada)`:
//     *        - Llama a `view.actualizarAspectoBotonToggle(toggleMarkImageAction, estaMarcada);`.
//     *        - Actualiza `view.setTextoRuta()` para añadir/quitar "[MARCADA]".
//     *    - Nuevo método: `public void mostrarDialogoSeleccionProyectoActual()`:
//     *        - Llama a `projectManager.getClavesMarcadas()`.
//     *        - Construye un String con estas claves.
//     *        - Muestra el String en un `JOptionPane.showMessageDialog`.
//     *    - En `actualizarImagenPrincipal(int indiceSeleccionado)`:
//     *        - Después de cargar la imagen y actualizar el modelo, obtener `selectedImageKey`.
//     *        - Llamar a `boolean marcada = projectManager.estaMarcada(selectedImageKey);`.
//     *        - Llamar a `actualizarEstadoVisualBotonMarcarYBarraEstado(marcada);`.
//     *
//     * 5. UIDefinitionService:
//     *    - `generateMenuStructure()`:
//     *        - En Menú "Imagen": `new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, MenuItemType.CHECKBOX_ITEM, "Marcar para Proyecto", null)`
//     *        - En Menú "Vista" (o nuevo menú "Proyecto"): `new MenuItemDefinition(AppActionCommands.CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO, MenuItemType.ITEM, "Ver Selección de Proyecto", null)`
//     *    - `generateToolbarStructure()`:
//     *        - Botón "Marcar": `new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA, "5003-marcar_imagen_48x48.png", "Marcar/Desmarcar Imagen", "control")` (o la categoría que prefieras).
//     *        - Botón "Mostrar Selección": `new ToolbarButtonDefinition(AppActionCommands.CMD_PROYECTO_MOSTRAR_SELECCION_DIALOGO, "7003-Mostrar_Favoritos_48x48.png", "Mostrar Imágenes Marcadas", "control")` (o la categoría que prefieras).
//     *
//     * 6. VisorView (Opcional para Iteración 1, pero bueno para el futuro):
//     *    - Si `ToggleMarkImageAction` no cambia el icono del botón directamente, `actualizarAspectoBotonToggle` podría necesitar lógica para cambiar entre icono de estrella vacía/llena.
//     *
//     * ============================
//     * ITERACIÓN 2: GESTIÓN DE PROYECTOS CON NOMBRE (Guardar Como, Abrir, Nuevo)
//     * ============================
//     *
//     * 1. ProjectSelectionManager:
//     *    - Campo: `private Path archivoProyectoActivo;` (puede ser null si es el temporal).
//     *    - Modificar constructor y métodos para usar `archivoProyectoActivo` si no es null, sino `archivoSeleccionTemporal`.
//     *    - `nuevoProyecto()`: `clavesMarcadasEnMemoria.clear(); archivoProyectoActivo = null; guardarAArchivo(archivoSeleccionTemporal, ...);` Actualizar título de ventana.
//     *    - `guardarProyectoComo(Path destino)`: `guardarAArchivo(destino, clavesMarcadasEnMemoria); archivoProyectoActivo = destino;` Actualizar título.
//     *    - `abrirProyecto(Path origen)`: `cargarDesdeArchivo(origen); archivoProyectoActivo = origen;` Actualizar título.
//     *    - `hayCambiosSinGuardar()`: Compara `clavesMarcadasEnMemoria` con el contenido de `archivoProyectoActivo` (si existe).
//     *
//     * 2. AppActionCommands:
//     *    - `CMD_PROYECTO_NUEVO`, `CMD_PROYECTO_ABRIR`, `CMD_PROYECTO_GUARDAR_COMO`, `CMD_PROYECTO_GUARDAR` (si el proyecto activo tiene nombre).
//     *
//     * 3. Nuevas Actions: `NuevoProyectoAction`, `AbrirProyectoAction`, `GuardarProyectoComoAction`, `GuardarProyectoAction`.
//     *
//     * 4. VisorController:
//     *    - Métodos para manejar estas nuevas acciones, usando `JFileChooser` para guardar/abrir.
//     *    - Lógica para "Guardar" (si `archivoProyectoActivo` no es null, guarda ahí; sino, actúa como "Guardar Como").
//     *    - Modificar `ShutdownHook`: Si `projectManager.hayCambiosSinGuardar()`, preguntar al usuario si desea guardar antes de salir. Si guarda, y es temporal, preguntar si quiere darle nombre. Si no guarda y es temporal, se podría borrar `archivoSeleccionTemporal`.
//     *    - Actualizar título de la `JFrame` (`VisorView`) para incluir el nombre del proyecto activo o "(Temporal)".
//     *
//     * 5. UIDefinitionService:
//     *    - Nuevo Menú "Proyecto" con ítems para Nuevo, Abrir, Guardar, Guardar Como.
//     *
//     * ============================
//     * ITERACIÓN 3: VISTA INTEGRADA DE SELECCIÓN DE PROYECTO (Toggle de ListModel)
//     * ============================
//     *
//     * 1. AppActionCommands:
//     *    - `CMD_PROYECTO_TOGGLE_VISTA_SELECCION = "cmd.proyecto.toggle_vista_seleccion";`
//     *
//     * 2. Nueva Action: `ToggleVistaSeleccionProyectoAction extends BaseVisorAction`.
//     *    - `actionPerformed` llama a `controller.toggleVistaSeleccionProyecto();`.
//     *    - Mantiene `Action.SELECTED_KEY` para el estado del toggle.
//     *
//     * 3. VisorModel:
//     *    - `private boolean mostrandoSeleccionProyecto = false;`
//     *    - `private DefaultListModel<String> modeloListaCarpetaOriginal;`
//     *    - `private String claveSeleccionadaEnCarpetaOriginal;`
//     *
//     * 4. VisorController:
//     *    - `toggleVistaSeleccionProyecto()`:
//     *        - Invierte `model.mostrandoSeleccionProyecto`.
//     *        - Actualiza `Action.SELECTED_KEY` de `ToggleVistaSeleccionProyectoAction`.
//     *        - Llama a `actualizarAspectoBotonToggle(...)` para el botón de la toolbar.
//     *        - Llama a `refrescarVistaPrincipalSegunModo();` (nuevo método).
//     *    - `refrescarVistaPrincipalSegunModo()`:
//     *        - Si `model.mostrandoSeleccionProyecto`:
//     *            - Guarda `model.getModeloLista()` en `model.modeloListaCarpetaOriginal`.
//     *            - Guarda `model.getSelectedImageKey()` en `model.claveSeleccionadaEnCarpetaOriginal`.
//     *            - Obtiene `projectManager.getClavesMarcadas()`.
//     *            - Crea `nuevoModeloSeleccion` a partir de esas claves.
//     *            - Llama a `model.setModeloLista(nuevoModeloSeleccion);` (¡OJO! Este método debe ser cuidadoso para no limpiar `selectedImageKey` si la clave anterior está en el nuevo modelo).
//     *            - Llama a `view.setListaImagenesModel(nuevoModeloSeleccion);` (o que `cargarListaImagenes` lo haga).
//     *            - Llama a `cargarListaImagenes(primeraClaveDeSeleccionSiExiste)`.
//     *            - Cambia título del panel izquierdo: "Selección: [Nombre Proyecto]".
//     *        - Else (volviendo a vista de carpeta):
//     *            - Llama a `model.setModeloLista(model.modeloListaCarpetaOriginal);`.
//     *            - Llama a `view.setListaImagenesModel(...)`.
//     *            - Llama a `cargarListaImagenes(model.claveSeleccionadaEnCarpetaOriginal)`.
//     *            - Cambia título del panel izquierdo: "Lista de Archivos".
//     *
//     * 5. UIDefinitionService:
//     *    - Botón en Toolbar para `CMD_PROYECTO_TOGGLE_VISTA_SELECCION`.
//     *    - Ítem de Menú (quizás `JCheckBoxMenuItem`) en "Vista" para `CMD_PROYECTO_TOGGLE_VISTA_SELECCION`.
//     *
//     * ============================
//     * CONSIDERACIONES ADICIONALES (Futuras):
//     * ============================
//     * - **Modo Grid y Marcar:** Deshabilitar `ToggleMarkImageAction` general. Añadir menú contextual en el grid.
//     * - **Rendimiento:** Si `getClavesMarcadas()` es muy grande y se llama frecuentemente para la UI, optimizar.
//     * - **Multi-selección para Marcar/Desmarcar:** En `listaNombres` o Grid.
//     * - **Internacionalización (i18n)** de todos los textos.
//     * - **Deshacer/Rehacer** para marcado/desmarcado.
//     *
//     */
//    // -------------------------------------------------------------------------
//    // --- FIN MEGACOMENTARIO ---
//    // -------------------------------------------------------------------------
