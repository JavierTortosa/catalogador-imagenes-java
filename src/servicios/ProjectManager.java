package servicios;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import controlador.ProjectController;
import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import modelo.proyecto.ProjectModel;

public class ProjectManager implements IProjectManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ProjectManager.class);
	
    private static final String SEPARADOR_ETIQUETA_LEGACY = "||";

    private ConfigurationManager configManager;

    // --- CAMPOS PARA MULTIPROYECTO ---
    private Path archivoProyectoActivo;
    private Path carpetaBaseProyectos;
    private Path archivoSeleccionTemporalPath;
    
    // --- ESTADO CENTRALIZADO ---
    private ProjectModel currentProject;
    private ProjectModel lastSavedProjectState;
    private ProjectController projectControllerRef;
    private VisorModel modelRef;
    private Gson gson;
    
    private final List<ProjectStateListener> stateListeners = new ArrayList<>();
    
    private boolean hayCambiosSinGuardar = false;


    public ProjectManager() {
        // 1. Crear la herramienta GSON PRIMERO.
        this.gson = new GsonBuilder()
                      .setPrettyPrinting()
                      .disableHtmlEscaping() // Para evitar que las barras '\' se conviertan en \u005c
//                    .serializeNulls()
                      .create();
                      
        // 2. Ahora que GSON existe, podemos inicializar los modelos de proyecto.
        this.currentProject = new ProjectModel();
        this.lastSavedProjectState = deepCopyProjectModel(this.currentProject);

    } // --- Fin del método ProjectManager (constructor) ---


    public void initialize() {
        if (this.configManager == null) {
            throw new IllegalStateException("ProjectManager no puede inicializarse sin ConfigurationManager.");
        }
        inicializarRutaArchivoSeleccion();
        // La llamada a cargarProyectoActivo() se elimina.
        // ProjectManager ahora SIEMPRE empieza con un 'new ProjectModel()' vacío.
        
        logger.debug("[ProjectManager] Instancia inicializada. El proyecto en memoria está limpio.");
        logger.debug("  -> Ruta para proyecto temporal: {}", this.archivoSeleccionTemporalPath.toAbsolutePath());
    } // --- Fin del método initialize ---
    
    
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
            "seleccion_temporal.prj" // CAMBIO: Usamos la misma extensión para el temporal
        );
        this.archivoSeleccionTemporalPath = this.carpetaBaseProyectos.resolve(nombreArchivoTemporal);
    } // --- Fin del método inicializarRutaArchivoSeleccion ---
    
    
    /**
     * Compara el estado actual del proyecto (currentProject) con el último estado
     * guardado (lastSavedProjectState) para determinar si hay cambios.
     * @return true si hay diferencias, false si son idénticos.
     */
    private boolean isProjectDirty() {
        // Si por alguna razón el estado guardado es nulo, consideramos que hay cambios.
        if (this.lastSavedProjectState == null) {
            return true;
        }

        // Comparamos los mapas de imágenes seleccionadas (rutas y etiquetas).
        // El método .equals() para Maps es exhaustivo: compara tamaño, claves y valores.
        if (!Objects.equals(this.currentProject.getSelectedImages(), this.lastSavedProjectState.getSelectedImages())) {
            logger.debug("[Dirty Check] Diferencia detectada en: selectedImages");
            return true;
        }

        // Comparamos las listas de imágenes descartadas.
        if (!Objects.equals(this.currentProject.getDiscardedImages(), this.lastSavedProjectState.getDiscardedImages())) {
            logger.debug("[Dirty Check] Diferencia detectada en: discardedImages");
            return true;
        }

        // Comparamos las configuraciones de exportación.
        // Esto es crucial para detectar cambios en la tabla de exportación.
        // Nota: Esto requiere que la clase ExportConfig tenga un método .equals() bien implementado.
        if (!Objects.equals(this.currentProject.getExportConfigs(), this.lastSavedProjectState.getExportConfigs())) {
             logger.debug("[Dirty Check] Diferencia detectada en: exportConfigs");
            return true;
        }
        
        // Comparamos la descripción del proyecto
        if (!Objects.equals(this.currentProject.getProjectDescription(), this.lastSavedProjectState.getProjectDescription())) {
            logger.debug("[Dirty Check] Diferencia detectada en: projectDescription");
            return true;
        }

        // Si hemos llegado hasta aquí, no hay diferencias.
        return false;
    } // ---FIN de metodo isProjectDirty---
    

    private void cargarProyectoActivo() {
        // Lógica para determinar qué proyecto cargar al inicio.
        // Por ahora, simplemente cargamos el temporal. En el futuro podría cargar el último abierto.
        this.archivoProyectoActivo = null; // Empezamos en modo temporal.
        cargarDesdeArchivo(this.archivoSeleccionTemporalPath);
    } // --- Fin del método cargarProyectoActivo ---
    
    
    /**
     * Realiza una copia profunda (deep copy) de un objeto ProjectModel.
     * La forma más sencilla y robusta de hacer esto es serializarlo a JSON y
     * deserializarlo de nuevo en un nuevo objeto. Esto asegura que no queden
     * referencias compartidas.
     * @param original El objeto ProjectModel a copiar.
     * @return Una instancia completamente nueva e independiente de ProjectModel.
     */
    private ProjectModel deepCopyProjectModel(ProjectModel original) {
        if (original == null) {
            return null;
        }
        String jsonString = gson.toJson(original);
        return gson.fromJson(jsonString, ProjectModel.class);
    } // ---FIN de metodo deepCopyProjectModel---
    
    
    private void cargarDesdeArchivo(Path rutaArchivo) {
        if (!Files.exists(rutaArchivo) || !Files.isReadable(rutaArchivo)) {
            this.currentProject = new ProjectModel();
            logger.debug("  [ProjectManager] Archivo de proyecto no encontrado: {}. Se iniciará con proyecto vacío.", rutaArchivo);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
            String primeraLinea = reader.readLine();
            if (primeraLinea != null && primeraLinea.trim().startsWith("{")) {
                // --- CARGAR FORMATO NUEVO (JSON) ---
                cargarDesdeJson(rutaArchivo);
            } else {
                // --- CARGAR Y MIGRAR FORMATO ANTIGUO (TXT) ---
                logger.info("Detectado formato de proyecto antiguo en {}. Migrando a JSON...", rutaArchivo.getFileName());
                cargarDesdeTxtLegado(rutaArchivo);
                // Después de cargar, guardamos inmediatamente en el nuevo formato.
                guardarAArchivo();
                logger.info("Migración completada. El proyecto ha sido guardado en formato JSON.");
            }
        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: Fallo crítico al intentar leer el archivo de proyecto: " + rutaArchivo, e);
            this.currentProject = new ProjectModel();
        }
    } // --- Fin del método cargarDesdeArchivo ---
    
    
    /**
     * Carga el estado del proyecto desde un archivo en formato JSON.
     * @param rutaArchivo La ruta al archivo .prj en formato JSON.
     */
    private void cargarDesdeJson(Path rutaArchivo) {
        try (FileReader reader = new FileReader(rutaArchivo.toFile())) {
            ProjectModel loadedProject = gson.fromJson(reader, ProjectModel.class);
            if (loadedProject != null) {
                this.currentProject = loadedProject;
                logger.debug("  [ProjectManager] Proyecto JSON cargado. Selección: {}, Descartes: {}",
                             this.currentProject.getSelectedImages().size(),
                             this.currentProject.getDiscardedImages().size());
            } else {
                this.currentProject = new ProjectModel();
                logger.warn("WARN [PM cargar]: El archivo JSON {} está vacío o mal formado. Se inicia un proyecto nuevo.", rutaArchivo);
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            logger.error("ERROR [ProjectManager]: No se pudo leer o parsear el archivo JSON: " + rutaArchivo, e);
            this.currentProject = new ProjectModel();
        }
    } // ---FIN de metodo cargarDesdeJson---

    /**
     * Lógica de carga para el formato de texto antiguo (legado).
     * Pobla el 'currentProject' en memoria pero no guarda en disco.
     * @param rutaArchivo La ruta al archivo .prj en formato TXT.
     */
    private void cargarDesdeTxtLegado(Path rutaArchivo) {
        ProjectModel modelMigrado = new ProjectModel();
        String seccionActual = "SELECCION";

        try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;

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
                    if (linea.contains(SEPARADOR_ETIQUETA_LEGACY)) {
                        String[] partes = linea.split("\\|\\|", 2);
                        rutaStr = partes[0];
                        etiqueta = (partes.length > 1) ? partes[1] : null;
                    } else {
                        rutaStr = linea;
                    }

                    if ("SELECCION".equals(seccionActual)) {
                        modelMigrado.getSelectedImages().put(rutaStr, etiqueta);
                    } else if ("DESCARTES".equals(seccionActual)) {
                        modelMigrado.getDiscardedImages().add(rutaStr);
                    }
                } catch (Exception e) {
                    logger.warn("WARN [PM migración]: Ruta inválida en archivo de proyecto legado: '{}'", linea, e);
                }
            }
        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo leer el archivo de proyecto legado: " + rutaArchivo, e);
        }
        this.currentProject = modelMigrado;
    } // ---FIN de metodo cargarDesdeTxtLegado---
    

    public void guardarAArchivo() {
        Path rutaGuardado = (this.archivoProyectoActivo != null) ? this.archivoProyectoActivo : this.archivoSeleccionTemporalPath;
        boolean esGuardadoDefinitivo = (this.archivoProyectoActivo != null);

        logger.info("--- INICIANDO GUARDADO DE PROYECTO ---");
        logger.debug("Ruta de destino: {}", rutaGuardado.toAbsolutePath());

        if (this.currentProject == null) {
            logger.error("!!! CRÍTICO: currentProject es NULO. No se puede guardar nada. !!!");
            return;
        }
        
        // La lógica de depuración que tenías se mantiene, es útil.
        Map<String, String> imagenesParaGuardar = this.currentProject.getSelectedImages();
        logger.debug(">>> CONTENIDO DE 'selectedImages' A PUNTO DE SER GUARDADO (Total: {} elementos):", imagenesParaGuardar.size());
        
        System.out.println("------------------------------------------------------------------ IMAGENES PARA GUARDAR");
        System.out.println(imagenesParaGuardar.toString());
        System.out.println("------------------------------------------------------------------");

        if (imagenesParaGuardar.isEmpty()) {
            logger.info("    -> La lista de selección está VACÍA.");
        } else {
            int i = 1;
            for (String clave : imagenesParaGuardar.keySet()) {
                logger.debug("    {}. {}", i++, clave);
            }
        }
        
        try (FileWriter writer = new FileWriter(rutaGuardado.toFile())) {
            gson.toJson(this.currentProject, writer);
            System.out.println(this.currentProject.toString());
            logger.debug("  [ProjectManager] Proyecto JSON guardado en {}. Selección: {}, Descartes: {}.",
                         rutaGuardado.getFileName(), 
                         this.currentProject.getSelectedImages().size(), 
                         this.currentProject.getDiscardedImages().size());
        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo guardar el archivo de proyecto JSON: " + rutaGuardado, e);
        }
        
        if (esGuardadoDefinitivo) {
            this.lastSavedProjectState = deepCopyProjectModel(this.currentProject);
            logger.debug("   -> Estado 'lastSavedProjectState' actualizado tras guardado definitivo.");
            
            // Si el estado "sucio" cambia, lo actualizamos y notificamos.
            if (this.hayCambiosSinGuardar) {
                this.hayCambiosSinGuardar = false;
                fireProjectStateChanged();
                logger.debug("   -> [FLAG] El proyecto ha sido marcado como GUARDADO. 'hayCambiosSinGuardar' es ahora 'false'.");
            }
        }
    } // --- Fin del método guardarAArchivo ---
    
    
    // --- INICIO DE NUEVOS MÉTODOS PÚBLICOS PARA MULTIPROYECTO ---

    
    public void nuevoProyecto() {
        logger.info("[ProjectManager] Creando nuevo proyecto.");
        
        this.currentProject = new ProjectModel();
        this.archivoProyectoActivo = null;
        this.hayCambiosSinGuardar = false;
        
        if (modelRef != null) {
            modelRef.setRutaProyectoActivoConNombre(null);
        }
        
        this.lastSavedProjectState = deepCopyProjectModel(this.currentProject);
        
    } // ---FIN de metodo nuevoProyecto---

    
    public void abrirProyecto(Path rutaArchivo) throws ProyectoIOException {
        logger.info("[ProjectManager] Abriendo proyecto desde: {}", rutaArchivo);
        if (rutaArchivo == null || !Files.isReadable(rutaArchivo)) {
            String errorMsg = "La ruta es nula o el archivo no se puede leer. Ruta: " + rutaArchivo;
            logger.error(errorMsg);
            throw new ProyectoIOException(errorMsg);
        }

        cargarDesdeArchivo(rutaArchivo);
        this.lastSavedProjectState = deepCopyProjectModel(this.currentProject); 
        this.archivoProyectoActivo = rutaArchivo;
        
        // --- NUEVA LÍNEA ---
        if (modelRef != null) {
            modelRef.setRutaProyectoActivoConNombre(rutaArchivo);
        }
    } // ---FIN de metodo abrirProyecto---
    

    public void guardarProyectoComo(Path rutaArchivo) {
        logger.info("[ProjectManager] Guardando proyecto como: {}", rutaArchivo);
        this.archivoProyectoActivo = rutaArchivo;
        
        // --- LÍNEA MODIFICADA ---
        if (modelRef != null) {
            // Pasamos el nuevo Path al modelo para que el título se pueda actualizar correctamente.
            modelRef.setRutaProyectoActivoConNombre(rutaArchivo);
        }

        // Esta llamada se encarga de guardar el contenido en la nueva ruta y de
        // resetear el estado de "cambios sin guardar".
        guardarAArchivo(); 
    } // ---FIN de metodo guardarProyectoComo---
    
    
    /**
     * Guarda el estado actual del proyecto en un archivo de recuperación con nombre fijo.
     * Antes de guardar, almacena la ruta del proyecto original (si existe) dentro
     * del propio archivo de recuperación para poder restaurar el contexto correctamente.
     *
     * @return La ruta del archivo de recuperación que se ha creado/sobrescrito.
     */
    public Path guardarSesionDeRecuperacion() {
        if (this.currentProject == null) {
            logger.error("!!! CRÍTICO: currentProject es NULO. No se puede guardar la sesión de recuperación. !!!");
            return null;
        }

        // 1. Determinar la ruta del archivo de recuperación.
        String nombreArchivoRecuperacion = this.configManager.getString(
            ConfigKeys.PROYECTOS_ARCHIVO_RECUPERACION, // Necesitaremos añadir esta clave a ConfigKeys
            "session_recovery.prj"
        );
        Path rutaRecuperacion = this.carpetaBaseProyectos.resolve(nombreArchivoRecuperacion);

        logger.info("[ProjectManager] Iniciando guardado de sesión de recuperación en: {}", rutaRecuperacion.toAbsolutePath());

        // 2. Almacenar la ruta del proyecto original en el modelo ANTES de guardarlo.
        if (this.archivoProyectoActivo != null) {
            this.currentProject.setOriginalProjectPath(this.archivoProyectoActivo.toAbsolutePath().toString());
            logger.debug("   -> Se guardará la referencia al proyecto original: {}", this.archivoProyectoActivo.getFileName());
        } else {
            // Si estábamos en un "Proyecto Temporal", no hay ruta original.
            this.currentProject.setOriginalProjectPath(null);
            logger.debug("   -> Se está recuperando un 'Proyecto Temporal', no hay ruta original que guardar.");
        }
        
        // 3. Actualizar metadatos del proyecto.
        this.currentProject.setLastModifiedDate(System.currentTimeMillis());

        // 4. Guardar el modelo en el archivo de recuperación.
        try (FileWriter writer = new FileWriter(rutaRecuperacion.toFile())) {
            gson.toJson(this.currentProject, writer);
            logger.info("  [ProjectManager] Sesión de recuperación guardada correctamente.");
        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo guardar el archivo de recuperación: " + rutaRecuperacion, e);
            return null;
        } finally {
            // 5. Limpiar el campo 'originalProjectPath' del modelo en memoria para no contaminar
            //    operaciones futuras dentro de la misma sesión si el usuario cancela el cierre.
            this.currentProject.setOriginalProjectPath(null);
        }

        return rutaRecuperacion;
    } // ---FIN de metodo guardarSesionDeRecuperacion---
    
    
    /**
     * Carga un proyecto desde un archivo de recuperación.
     * Este método lee el ProjectModel del archivo de recuperación y, crucialmente,
     * restaura la ruta del proyecto original si estaba guardada, para que el
     * usuario continúe trabajando en el archivo correcto. A diferencia de 'abrirProyecto',
     * este método establece explícitamente el estado como 'con cambios sin guardar'.
     *
     * @param rutaArchivoRecuperacion La ruta al archivo de recuperación (ej. session_recovery.prj).
     * @throws ProyectoIOException Si el archivo de recuperación no se puede leer o está corrupto.
     */
    public void cargarDesdeRecuperacion(Path rutaArchivoRecuperacion) throws ProyectoIOException {
        logger.info("[ProjectManager] Iniciando carga desde archivo de recuperación: {}", rutaArchivoRecuperacion.getFileName());
        if (!Files.exists(rutaArchivoRecuperacion) || !Files.isReadable(rutaArchivoRecuperacion)) {
            throw new ProyectoIOException("El archivo de recuperación no se encuentra o no se puede leer.");
        }

        // 1. Cargar el ProjectModel desde el archivo JSON de recuperación.
        cargarDesdeArchivo(rutaArchivoRecuperacion);

        // 2. Comprobar si el modelo cargado contiene una referencia al proyecto original.
        String rutaOriginalStr = this.currentProject.getOriginalProjectPath();
        if (rutaOriginalStr != null && !rutaOriginalStr.isBlank()) {
            logger.debug("   -> Archivo de recuperación contiene referencia al proyecto original: {}", rutaOriginalStr);
            try {
                this.archivoProyectoActivo = Paths.get(rutaOriginalStr);
                // Actualizamos el modelo de la aplicación para que el título de la ventana sea correcto.
                if (modelRef != null) {
                    modelRef.setRutaProyectoActivoConNombre(this.archivoProyectoActivo);
                }
                logger.info("   -> Proyecto activo restaurado a: {}", this.archivoProyectoActivo.getFileName());
            } catch (java.nio.file.InvalidPathException e) {
                logger.error("ERROR: La ruta del proyecto original guardada en el archivo de recuperación es inválida: '{}'", rutaOriginalStr, e);
                this.archivoProyectoActivo = null;
                 if (modelRef != null) {
                    modelRef.setRutaProyectoActivoConNombre(null);
                }
            }
        } else {
            logger.info("   -> El archivo de recuperación corresponde a un 'Proyecto Temporal'. No hay ruta original que restaurar.");
            this.archivoProyectoActivo = null;
            if (modelRef != null) {
                modelRef.setRutaProyectoActivoConNombre(null);
            }
        }
        
        // 3. ¡LA LÍNEA CLAVE! Marcar que hay cambios sin guardar.
        // A diferencia de abrirProyecto(), aquí FORZAMOS el estado "sucio".
        this.hayCambiosSinGuardar = true;
        
        logger.info("   -> [FLAG] 'hayCambiosSinGuardar' FORZADO a 'true' tras cargar desde recuperación.");

    } // ---FIN de metodo cargarDesdeRecuperacion---
    
    
    public Path getArchivoProyectoActivo() {
        return this.archivoProyectoActivo;
    } // ---FIN de metodo getArchivoProyectoActivo---
    
    
    public String getNombreProyectoActivo() {
        if (this.archivoProyectoActivo != null) {
            String fileName = this.archivoProyectoActivo.getFileName().toString();
            if (fileName.toLowerCase().endsWith(".prj")) {
                return fileName;
            }
            return fileName + ".prj"; // Aseguramos que tenga la extensión por consistencia
        }
        
        // --- INICIO DE LA MODIFICACIÓN (FALLBACK INTELIGENTE) ---
        // Si archivoProyectoActivo es nulo, pero el modelo cargado SÍ tiene un nombre
        // (porque viene de un archivo de recuperación), usamos ese nombre.
        if (this.currentProject != null && this.currentProject.getProjectName() != null && !this.currentProject.getProjectName().equals("Proyecto Temporal")) {
            String projectNameFromModel = this.currentProject.getProjectName();
            if (!projectNameFromModel.toLowerCase().endsWith(".prj")) {
                return projectNameFromModel + ".prj";
            }
            return projectNameFromModel;
        }
        // --- FIN DE LA MODIFICACIÓN ---

        return "Proyecto Temporal";
        
    } // ---FIN de metodo getNombreProyectoActivo---
    
    
    public Path getCarpetaBaseProyectos() {
        return this.carpetaBaseProyectos;
    } // ---FIN de metodo getCarpetaBaseProyectos---

    
    /**
     * Devuelve la ruta completa y resuelta del archivo de recuperación de sesión.
     * Utiliza el nombre de archivo definido en la configuración.
     * @return La Path al archivo de recuperación.
     */
    public Path getRutaArchivoRecuperacion() {
        String nombreArchivoRecuperacion = this.configManager.getString(
            ConfigKeys.PROYECTOS_ARCHIVO_RECUPERACION,
            "session_recovery.prj"
        );
        return this.carpetaBaseProyectos.resolve(nombreArchivoRecuperacion);
    } // ---FIN de metodo getRutaArchivoRecuperacion---

    
    
    // --- FIN DE NUEVOS MÉTODOS ---

    
    
    public String getEtiqueta(Path rutaImagen) {
        if (rutaImagen == null) return null;
        String clave = rutaImagen.toString().replace("\\", "/");
        return this.currentProject.getSelectedImages().get(clave);
    } // ---FIN de metodo getEtiqueta ---

    
    public void setEtiqueta(Path rutaImagen, String etiqueta) {
        if (rutaImagen == null) return;
        String clave = rutaImagen.toString().replace("\\", "/");
        if (this.currentProject.getSelectedImages().containsKey(clave)) {
            // --- LÓGICA DE SEGURIDAD ---
            // Si la nueva etiqueta es null, la guardamos como un string vacío.
            String etiquetaAGuardar = (etiqueta == null) ? "" : etiqueta;
            // -------------------------

            this.currentProject.getSelectedImages().put(clave, etiquetaAGuardar); // <--- CAMBIO
            notificarModificacion(); 
            logger.debug("Etiqueta '{}' asignada a: {}", etiquetaAGuardar, rutaImagen.getFileName());
        } else {
            logger.warn("Intento de etiquetar una imagen que no está en la selección actual: {}", rutaImagen.getFileName());
        }
    } // ---FIN de metodo setEtiqueta ---
    
    
    /**
     * Añade una asociación entre una imagen y un archivo relacionado en el modelo del proyecto.
     * Guarda los cambios en el archivo de proyecto.
     *
     * @param rutaImagen La imagen principal a la que se asocia el archivo.
     * @param rutaArchivoAsociado El archivo (.stl, .zip, etc.) que se va a asociar.
     */
    public void addAssociatedFile(Path rutaImagen, Path rutaArchivoAsociado) {
        if (rutaImagen == null || rutaArchivoAsociado == null) {
            logger.warn("WARN [addAssociatedFile]: Se intentó añadir una asociación con rutas nulas.");
            return;
        }

        String claveImagen = rutaImagen.toString().replace("\\", "/");
        String rutaAsociadoStr = rutaArchivoAsociado.toString().replace("\\", "/");

        // 1. Obtener el mapa de configuraciones de exportación.
        Map<String, modelo.proyecto.ExportConfig> exportConfigsMap = this.currentProject.getExportConfigs();

        // 2. Obtener (o crear si no existe) el objeto ExportConfig para esta imagen.
        modelo.proyecto.ExportConfig config = exportConfigsMap.computeIfAbsent(claveImagen, k -> new modelo.proyecto.ExportConfig());

        // 3. Obtener la lista de archivos asociados DENTRO del objeto de configuración.
        List<String> files = config.getAssociatedFiles();

        // 4. La lógica de añadir y guardar permanece igual.
        if (!files.contains(rutaAsociadoStr)) {
            files.add(rutaAsociadoStr);
            notificarModificacion();
            logger.debug("Archivo asociado '{}' añadido para la imagen '{}' y proyecto guardado.", rutaArchivoAsociado.getFileName(), rutaImagen.getFileName());
        }
    } // ---FIN de metodo addAssociatedFile---

    /**
     * Elimina una asociación entre una imagen y un archivo relacionado en el modelo del proyecto.
     * Si la lista de archivos asociados para una imagen queda vacía, se elimina la entrada del mapa.
     * Guarda los cambios en el archivo de proyecto.
     *
     * @param rutaImagen La imagen principal de la que se desasocia el archivo.
     * @param rutaArchivoAsociado El archivo que se va a desasociar.
     */
    public void removeAssociatedFile(Path rutaImagen, Path rutaArchivoAsociado) {
        if (rutaImagen == null || rutaArchivoAsociado == null) {
            logger.warn("WARN [removeAssociatedFile]: Se intentó quitar una asociación con rutas nulas.");
            return;
        }

        String claveImagen = rutaImagen.toString().replace("\\", "/");
        String rutaAsociadoStr = rutaArchivoAsociado.toString().replace("\\", "/");

        // 1. Obtener el mapa de configuraciones de exportación.
        Map<String, modelo.proyecto.ExportConfig> exportConfigsMap = this.currentProject.getExportConfigs();

        // 2. Obtener el objeto ExportConfig para esta imagen. Si no existe, no hay nada que hacer.
        modelo.proyecto.ExportConfig config = exportConfigsMap.get(claveImagen);

        if (config != null) {
            // 3. Obtener la lista de archivos de DENTRO del objeto de configuración.
            List<String> files = config.getAssociatedFiles();
            boolean removed = files.remove(rutaAsociadoStr);

            if (removed) {
                // 4. Si la lista de archivos queda vacía Y los otros flags están en su estado por defecto,
                //    podemos eliminar toda la entrada de ExportConfig para mantener el JSON limpio.
                if (files.isEmpty() && config.isExportEnabled() && !config.isIgnoreCompressed()) {
                    exportConfigsMap.remove(claveImagen);
                }
                notificarModificacion();
                logger.debug("Archivo asociado '{}' eliminado para la imagen '{}' y proyecto guardado.", rutaArchivoAsociado.getFileName(), rutaImagen.getFileName());
            }
        }
    } // ---FIN de metodo removeAssociatedFile---


    @Override
    public List<Path> getImagenesMarcadas() {
        return this.currentProject.getSelectedImages().keySet().stream()
                   .map(Paths::get)
                   .collect(Collectors.toList());
    } // --- Fin del método getImagenesMarcadas ---

    
    @Override
    public void gestionarSeleccionProyecto(Component parentComponent) {
        String message = "Funcionalidad para gestionar la selección de imágenes del proyecto (marcar, ver, guardar, cargar) aún no implementada.\n\n" +
                         "Actualmente se usa: " + (this.archivoProyectoActivo != null ? this.archivoProyectoActivo.toAbsolutePath() : this.archivoSeleccionTemporalPath.toAbsolutePath()) +
                         "\nImágenes seleccionadas: " + this.currentProject.getSelectedImages().size() + 
                         "\nImágenes descartadas: " + this.currentProject.getDiscardedImages().size();
        JOptionPane.showMessageDialog(parentComponent, message, "Gestión de Selección de Proyecto (Pendiente)", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin del método gestionarSeleccionProyecto ---
    
    
    public void marcarImagenInterno(Path rutaAbsoluta) {
        if (rutaAbsoluta == null) return;
        String clave = rutaAbsoluta.toString().replace("\\", "/");
        // putIfAbsent devuelve null si la clave no existía, indicando que hubo un cambio.
        if (this.currentProject.getSelectedImages().putIfAbsent(clave, "") == null) { // <--- CAMBIO: null por ""
            notificarModificacion();
        }
    } // --- Fin del método marcarImagenInterno ---

    
    public void desmarcarImagenInterno(Path rutaAbsoluta) {
        if (rutaAbsoluta == null) return;
        String clave = rutaAbsoluta.toString().replace("\\", "/");
        // remove devuelve el valor anterior si existía, indicando que hubo un cambio.
        if (this.currentProject.getSelectedImages().remove(clave) != null) {
            notificarModificacion();
        }
    } // --- Fin del método desmarcarImagenInterno ---
    
    
    @Override
    public boolean estaMarcada(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        String clave = rutaAbsolutaImagen.toString().replace("\\", "/");
        return this.currentProject.getSelectedImages().containsKey(clave);
    } // --- Fin del método estaMarcada ---

    
    @Override
    public boolean alternarMarcaImagen(Path rutaAbsolutaImagen) {
        if (estaMarcada(rutaAbsolutaImagen)) {
            desmarcarImagenInterno(rutaAbsolutaImagen);
            return false;
        } else {
            marcarImagenInterno(rutaAbsolutaImagen);
            return true;
        }
    } // --- Fin del método alternarMarcaImagen ---
    
    
    /**
     * Establece explícitamente el estado del proyecto como "guardado",
     * reseteando el flag de cambios pendientes y notificando a los listeners.
     * Este método es llamado por el controlador DESPUÉS de una operación
     * de guardado, apertura o nuevo proyecto exitosa.
     */
    public void markProjectAsSaved() {
        this.lastSavedProjectState = deepCopyProjectModel(this.currentProject);
        if (this.hayCambiosSinGuardar) {
            this.hayCambiosSinGuardar = false;
            logger.debug("[FLAG] El proyecto ha sido marcado como GUARDADO. 'hayCambiosSinGuardar' es ahora 'false'.");
            fireProjectStateChanged(); // Notificar que ya no hay cambios
        }
    } // ---FIN de metodo markProjectAsSaved---
    
    
    @Override
    public boolean hayCambiosSinGuardar() {
        return this.hayCambiosSinGuardar;
    } // Fin del metodo hayCambiosSinGuardar

    
    @Override
    public void notificarModificacion() {
        logger.info("--- PASO 4: notificarModificacion en ProjectManager EJECUTADO ---");

        // Comprobamos si el estado "sucio" va a cambiar.
        boolean estabaSucio = this.hayCambiosSinGuardar;
        boolean estaSucioAhora = isProjectDirty();

        if (estaSucioAhora && !estabaSucio) {
            // El proyecto estaba limpio y ahora tiene cambios.
            this.hayCambiosSinGuardar = true;
            logger.debug("   -> [FLAG] El proyecto ha sido marcado como MODIFICADO. 'hayCambiosSinGuardar' es ahora 'true'.");
            fireProjectStateChanged(); // Notificamos a los oyentes del cambio.
        } else if (!estaSucioAhora && estabaSucio) {
            // Esto podría ocurrir si el usuario deshace un cambio y vuelve al estado guardado.
            this.hayCambiosSinGuardar = false;
            logger.debug("   -> [FLAG] El proyecto ha vuelto a un estado sin modificar. 'hayCambiosSinGuardar' es ahora 'false'.");
            fireProjectStateChanged(); // Notificamos también.
        }
        // Si el estado no cambia (ya estaba sucio y sigue sucio), no hacemos nada.
    }// ---FIN de metodo notificarModificacion---
    
    
    public void vaciarDescartes() {
        if (this.currentProject.getDiscardedImages().isEmpty()) {
            return;
        }
        notificarModificacion();
        this.currentProject.getDiscardedImages().clear();
        logger.debug("[ProjectManager] Lista de descartes vaciada.");
    } // --- Fin del método vaciarDescartes ---
    
    
    public List<Path> getImagenesDescartadas() {
        return this.currentProject.getDiscardedImages().stream()
                   .map(Paths::get)
                   .collect(Collectors.toList());
    } // --- Fin del método getImagenesDescartadas ---
    

    public void moverAdescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;
        String clave = rutaAbsolutaImagen.toString().replace("\\", "/");
        if (this.currentProject.getSelectedImages().containsKey(clave)) {
            this.currentProject.getSelectedImages().remove(clave);
            if (!this.currentProject.getDiscardedImages().contains(clave)) {
                this.currentProject.getDiscardedImages().add(clave);
            }
            notificarModificacion();
        }
    } // --- Fin del método moverAdescartes ---

    
    public void restaurarDeDescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;
        String clave = rutaAbsolutaImagen.toString().replace("\\", "/");
        if (this.currentProject.getDiscardedImages().contains(clave)) {
            this.currentProject.getDiscardedImages().remove(clave);
            this.currentProject.getSelectedImages().putIfAbsent(clave, ""); // <--- CAMBIO: null por ""
            notificarModificacion();
        }
    } // --- Fin del método restaurarDeDescartes ---
    
    
    public boolean estaEnDescartes(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        String clave = rutaAbsolutaImagen.toString().replace("\\", "/");
        return this.currentProject.getDiscardedImages().contains(clave);
    } // --- Fin del método estaEnDescartes ---
    
    
    public void eliminarDeProyecto(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return;
        String clave = rutaAbsolutaImagen.toString().replace("\\", "/");
        boolean removidoDeSeleccion = this.currentProject.getSelectedImages().remove(clave) != null;
        boolean removidoDeDescartes = this.currentProject.getDiscardedImages().remove(clave);
        if (removidoDeSeleccion || removidoDeDescartes) {
            notificarModificacion();
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
    
    
    @Override
    public void archivarTemporalAlCerrar() {
        // Durante un cierre limpio, el archivo de proyecto temporal de la sesión
        // actual ya no es necesario para la siguiente. Lo borramos.
        
        if (this.archivoSeleccionTemporalPath != null && Files.exists(this.archivoSeleccionTemporalPath)) {
            try {
                Files.delete(this.archivoSeleccionTemporalPath);
                logger.info("Cierre limpio: Archivo de proyecto temporal ({}) eliminado.", this.archivoSeleccionTemporalPath.getFileName());
            } catch (IOException e) {
                logger.error("ERROR al intentar eliminar el archivo de proyecto temporal durante el cierre.", e);
            }
        }
    } // ---FIN de metodo archivarTemporalAlCerrar---
    
    
    /**
     * Añade un oyente para ser notificado de los cambios de estado del proyecto.
     * @param listener El oyente a añadir.
     */
    public void addProjectStateListener(ProjectStateListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    } // ---FIN de metodo addProjectStateListener---

    /**
     * Elimina un oyente de la lista de notificaciones.
     * @param listener El oyente a eliminar.
     */
    public void removeProjectStateListener(ProjectStateListener listener) {
        stateListeners.remove(listener);
    } // ---FIN de metodo removeProjectStateListener---

    /**
     * Notifica a todos los oyentes registrados sobre un cambio en el estado
     * de "cambios sin guardar".
     */
    private void fireProjectStateChanged() {
        for (ProjectStateListener listener : stateListeners) {
            listener.onProjectStateChanged(this.hayCambiosSinGuardar);
        }
    } // ---FIN de metodo fireProjectStateChanged---
    
    
    /**
     * Devuelve la instancia actual del modelo de proyecto.
     * Es utilizado por el controlador para la sincronización directa UI -> Modelo.
     * @return El ProjectModel activo.
     */
    public ProjectModel getCurrentProject() {
        return this.currentProject;
    } // ---FIN de metodo getCurrentProject---
    
   public void setProjectController(ProjectController projectController) {
        this.projectControllerRef = projectController;
    } // ---FIN de metodo setProjectController
    
    public void setModel(VisorModel model) {
        this.modelRef = model;
    }// ---FIN de metodo setModel
    
    
} // --- FIN de la clase ProjectManager ---

