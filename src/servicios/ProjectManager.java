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
    private ProjectController projectControllerRef;
    private VisorModel modelRef;
    private Gson gson;
    
    private boolean hayCambiosSinGuardar = false;

    public ProjectManager() {
    	this.currentProject = new ProjectModel();
        // 'setPrettyPrinting' hace que el archivo JSON sea legible para humanos
        this.gson = new GsonBuilder()
                      .setPrettyPrinting()
                      .disableHtmlEscaping() // Para evitar que las barras '\' se conviertan en \u005c
//                      .serializeNulls()
                      .create();
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
    

    private void cargarProyectoActivo() {
        // Lógica para determinar qué proyecto cargar al inicio.
        // Por ahora, simplemente cargamos el temporal. En el futuro podría cargar el último abierto.
        this.archivoProyectoActivo = null; // Empezamos en modo temporal.
        cargarDesdeArchivo(this.archivoSeleccionTemporalPath);
    } // --- Fin del método cargarProyectoActivo ---
    
    
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
    	
    	if (modelRef != null && modelRef.isEnModoProyecto()) {
            // Para hacer esto, necesitamos acceso al ProjectController, que sí tiene acceso a la UI.
            // Pero ProjectManager no debe conocer a ProjectController.
            // La responsabilidad de sincronizar la tiene quien inicia el guardado.
            
            // ¡Este método NO debe llamar a sincronizar! La sincronización debe ocurrir ANTES de llamar a guardar.
        }
    	
        Path rutaGuardado = (this.archivoProyectoActivo != null) ? this.archivoProyectoActivo : this.archivoSeleccionTemporalPath;
        boolean esGuardadoDefinitivo = (this.archivoProyectoActivo != null);
        
        // --- INICIO DE LA MODIFICACIÓN DE DEPURACIÓN ---
        
        logger.info("--- INICIANDO GUARDADO DE PROYECTO ---");
        logger.debug("Ruta de destino: {}", rutaGuardado.toAbsolutePath());
        
        if (this.currentProject == null) {
            logger.error("!!! CRÍTICO: currentProject es NULO. No se puede guardar nada. !!!");
            return;
        }

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
        
        // --- FIN DE LA MODIFICACIÓN DE DEPURACIÓN ---


        // Actualizamos metadatos antes de guardar
        if (this.archivoProyectoActivo != null) {
            String fileName = this.archivoProyectoActivo.getFileName().toString();
            if (fileName.toLowerCase().endsWith(".prj")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            this.currentProject.setProjectName(fileName);
            logger.warn(fileName);
        } else {
            this.currentProject.setProjectName("Proyecto Temporal");
        }
        
        this.currentProject.setLastModifiedDate(System.currentTimeMillis());

        try (FileWriter writer = new FileWriter(rutaGuardado.toFile())) {
        	
            gson.toJson(this.currentProject, writer);
        
            System.out.println(this.currentProject.toString());
            
            logger.debug("  [ProjectManager] Proyecto JSON guardado en {}. Selección: {}, Descartes: {}.",
                         rutaGuardado.getFileName(), 
                         this.currentProject.getSelectedImages().size(), 
                         this.currentProject.getDiscardedImages().size());
            
            // ¡LÓGICA CORREGIDA! Solo reseteamos el flag si es un guardado definitivo (con nombre).
            if (esGuardadoDefinitivo) {
                this.hayCambiosSinGuardar = false;
            }

        } catch (IOException e) {
            logger.error("ERROR [ProjectManager]: No se pudo guardar el archivo de proyecto JSON: " + rutaGuardado, e);
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
        
        guardarAArchivo();
    } // ---FIN de metodo nuevoProyecto---

    
    public void abrirProyecto(Path rutaArchivo) throws ProyectoIOException {
        logger.info("[ProjectManager] Abriendo proyecto desde: {}", rutaArchivo);
        if (rutaArchivo == null || !Files.isReadable(rutaArchivo)) {
            String errorMsg = "La ruta es nula o el archivo no se puede leer. Ruta: " + rutaArchivo;
            logger.error(errorMsg);
            throw new ProyectoIOException(errorMsg);
        }

        cargarDesdeArchivo(rutaArchivo);
        this.hayCambiosSinGuardar = false;
        this.archivoProyectoActivo = rutaArchivo;
        
        // --- NUEVA LÍNEA ---
        if (modelRef != null) {
            modelRef.setRutaProyectoActivoConNombre(rutaArchivo);
        }
    } // ---FIN de metodo abrirProyecto---
    

    public void guardarProyectoComo(Path rutaArchivo) {
        logger.info("[ProjectManager] Guardando proyecto como: {}", rutaArchivo);
        this.archivoProyectoActivo = rutaArchivo;
        
        // --- NUEVA LÍNEA ---
        if (modelRef != null) {
            modelRef.setRutaProyectoActivoConNombre(rutaArchivo);
        }

        guardarAArchivo(); // Guarda el contenido actual en la nueva ruta.
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

    } // ---FIN de metodo cargarDesdeRecuperacion---
    
    
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
            guardarAArchivo();
            logger.debug("Etiqueta '{}' asignada a: {}", etiquetaAGuardar, rutaImagen.getFileName());
        } else {
            logger.warn("Intento de etiquetar una imagen que no está en la selección actual: {}", rutaImagen.getFileName());
        }
    } // ---FIN de metodo setEtiqueta ---


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
    
    
    @Override
    public boolean hayCambiosSinGuardar() {
        return this.hayCambiosSinGuardar;
    } // Fin del metodo hayCambiosSinGuardar

    
    @Override
    public void notificarModificacion() {
        if (!this.hayCambiosSinGuardar) {
            this.hayCambiosSinGuardar = true;
        }
        
    }// Fin del metodo notificarModificacion
    
    
    public void vaciarDescartes() {
        if (this.currentProject.getDiscardedImages().isEmpty()) {
            return;
        }
        notificarModificacion();
        this.currentProject.getDiscardedImages().clear();
        guardarAArchivo();
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

