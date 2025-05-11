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

public class ProjectManager {

    private Path archivoSeleccionActualPath; // Ruta al archivo .txt que guarda la selección actual
    private Set<Path> rutasAbsolutasMarcadas; // Almacena los Path absolutos de las imágenes marcadas
    private final ConfigurationManager configManager;

    public ProjectManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null en ProjectManager");
        this.rutasAbsolutasMarcadas = new HashSet<>();
        inicializarRutaArchivoSeleccion(); // Determina y carga el archivo de selección por defecto
        System.out.println("[ProjectManager] Instancia creada. Selección actual desde: " + this.archivoSeleccionActualPath.toAbsolutePath());
        System.out.println("  -> Imágenes marcadas inicialmente: " + this.rutasAbsolutasMarcadas.size());
    }

    private void inicializarRutaArchivoSeleccion() {
        String carpetaBaseProyectosStr = this.configManager.getString(
            ConfigurationManager.KEY_PROYECTOS_CARPETA_BASE,
            ".project_selections" // Default si la clave no está en config
        );
        Path carpetaBaseProyectos = Paths.get(carpetaBaseProyectosStr);

        if (!carpetaBaseProyectos.isAbsolute()) {
            // Si es relativa, resolver desde el home del usuario para consistencia
            String userHome = System.getProperty("user.home");
            carpetaBaseProyectos = Paths.get(userHome, ".miVisorImagenesApp", carpetaBaseProyectosStr).toAbsolutePath();
        }

        try {
            if (!Files.exists(carpetaBaseProyectos)) {
                Files.createDirectories(carpetaBaseProyectos);
                System.out.println("  [ProjectManager] Directorio base de proyectos creado: " + carpetaBaseProyectos);
            }
        } catch (IOException e) {
            System.err.println("WARN [ProjectManager]: No se pudo crear el directorio base de proyectos: " + carpetaBaseProyectos +
                               ". Usando directorio actual de la aplicación como fallback.");
            carpetaBaseProyectos = Paths.get("").toAbsolutePath();
        }

        String nombreArchivoSeleccion = this.configManager.getString(
            ConfigurationManager.KEY_PROYECTOS_ARCHIVO_TEMPORAL,
            "seleccion_actual_rutas.txt" // Default si la clave no está en config
        );
        this.archivoSeleccionActualPath = carpetaBaseProyectos.resolve(nombreArchivoSeleccion);
        cargarDesdeArchivo(this.archivoSeleccionActualPath); // Cargar al inicializar
    }

    private void cargarDesdeArchivo(Path rutaArchivo) {
        this.rutasAbsolutasMarcadas.clear(); // Limpiar antes de cargar
        if (Files.exists(rutaArchivo) && Files.isReadable(rutaArchivo)) {
            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
                String linea;
                int count = 0;
                while ((linea = reader.readLine()) != null) {
                    String rutaStr = linea.trim();
                    if (!rutaStr.isEmpty() && !rutaStr.startsWith("#")) { // Ignorar comentarios y vacías
                        try {
                            this.rutasAbsolutasMarcadas.add(Paths.get(rutaStr));
                            count++;
                        } catch (Exception e) {
                            System.err.println("WARN [PM cargar]: Ruta inválida en archivo de selección: '" + rutaStr + "' - " + e.getMessage());
                        }
                    }
                }
                System.out.println("  [ProjectManager] Cargadas " + count + " rutas absolutas desde " + rutaArchivo);
            } catch (IOException e) {
                System.err.println("ERROR [ProjectManager]: No se pudo leer el archivo de selección: " + rutaArchivo + " - " + e.getMessage());
            }
        } else {
            System.out.println("  [ProjectManager] Archivo de selección no encontrado o no legible: " + rutaArchivo + ". Se iniciará con selección vacía.");
        }
    }

    private void guardarAArchivo() { // Ya no necesita parámetros, usa los campos de instancia
        if (this.archivoSeleccionActualPath == null) {
            System.err.println("ERROR [PM guardar]: archivoSeleccionActualPath es null. No se puede guardar.");
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(this.archivoSeleccionActualPath,
                                                            StandardOpenOption.CREATE,
                                                            StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# VisorImagenes - Selección de Proyecto (Rutas Absolutas)");
            writer.newLine();
            for (Path ruta : this.rutasAbsolutasMarcadas) {
                writer.write(ruta.toString().replace("\\", "/")); // Normalizar a / para consistencia
                writer.newLine();
            }
            System.out.println("  [ProjectManager] Selección guardada en " + this.archivoSeleccionActualPath +
                               " (" + this.rutasAbsolutasMarcadas.size() + " rutas).");
        } catch (IOException e) {
            System.err.println("ERROR [ProjectManager]: No se pudo guardar el archivo de selección: " +
                               this.archivoSeleccionActualPath + " - " + e.getMessage());
        }
    }


//    public boolean alternarMarcaImagen(Path rutaAbsolutaImagen) {
//        if (rutaAbsolutaImagen == null) return false;
//
//        boolean estabaMarcada = this.rutasAbsolutasMarcadas.contains(rutaAbsolutaImagen);
//        boolean ahoraEstaMarcada;
//
//        if (estabaMarcada) {
//            this.rutasAbsolutasMarcadas.remove(rutaAbsolutaImagen);
//            ahoraEstaMarcada = false;
//            System.out.println("  [ProjectManager] Imagen desmarcada (ruta abs): " + rutaAbsolutaImagen);
//        } else {
//            this.rutasAbsolutasMarcadas.add(rutaAbsolutaImagen);
//            ahoraEstaMarcada = true;
//            System.out.println("  [ProjectManager] Imagen marcada (ruta abs): " + rutaAbsolutaImagen);
//        }
//        guardarAArchivo(); // Guardar el estado actual del Set en memoria
//        return ahoraEstaMarcada;
//    }


//    public boolean estaMarcada(Path rutaAbsolutaImagen) {
//        if (rutaAbsolutaImagen == null) {
//            return false;
//        }
//        return this.rutasAbsolutasMarcadas.contains(rutaAbsolutaImagen);
//    }

    /**
     * Devuelve una lista de los Paths absolutos de todas las imágenes marcadas.
     * @return Una nueva lista de Paths (ordenada).
     */
    public List<Path> getImagenesMarcadas() {
        List<Path> sortedList = new ArrayList<>(this.rutasAbsolutasMarcadas);
        // Ordenar Paths es un poco más complejo si quieres un orden natural de sistema de archivos
        // Por ahora, un sort estándar de Path (que compara lexicográficamente sus strings)
        Collections.sort(sortedList);
        return sortedList;
    }

    // Placeholder para la gestión de proyectos
    public void gestionarSeleccionProyecto(Component parentComponent) {
        String message = "Funcionalidad para gestionar la selección de imágenes del proyecto (marcar, ver, guardar, cargar) aún no implementada.\n\n" +
                         "Actualmente se usa: " + (this.archivoSeleccionActualPath != null ? this.archivoSeleccionActualPath.toAbsolutePath() : "Ninguno") +
                         "\nImágenes marcadas: " + this.rutasAbsolutasMarcadas.size();
        JOptionPane.showMessageDialog(parentComponent,
                                      message,
                                      "Gestión de Selección de Proyecto (Pendiente)",
                                      JOptionPane.INFORMATION_MESSAGE);
        System.out.println("[ProjectManager] Diálogo placeholder 'gestionarSeleccionProyecto' mostrado.");
    }

    
    public void marcarImagenInterno(Path rutaAbsoluta) { // Renombrado a interno para evitar confusión
        if (rutaAbsoluta == null) return;
        if (this.rutasAbsolutasMarcadas.add(rutaAbsoluta)) {
            guardarAArchivo();
            System.out.println("  [ProjectManager] Imagen marcada (ruta abs): " + rutaAbsoluta);
        }
    }

    public void desmarcarImagenInterno(Path rutaAbsoluta) { // Renombrado a interno
        if (rutaAbsoluta == null) return;
        if (this.rutasAbsolutasMarcadas.remove(rutaAbsoluta)) {
            guardarAArchivo();
            System.out.println("  [ProjectManager] Imagen desmarcada (ruta abs): " + rutaAbsoluta);
        }
    }

    /**
     * Verifica si una imagen (dada por su Path absoluto) está actualmente marcada.
     * @param rutaAbsolutaImagen El Path absoluto de la imagen a verificar.
     * @return true si la imagen está marcada, false en caso contrario.
     */    
    public boolean estaMarcada(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        return this.rutasAbsolutasMarcadas.contains(rutaAbsolutaImagen);
    }

    /**
     * Alterna el estado de marca de una imagen (dada por su Path absoluto).
     * @param rutaAbsolutaImagen El Path absoluto de la imagen.
     * @return true si la imagen quedó marcada, false si quedó desmarcada.
     */
    public boolean alternarMarcaImagen(Path rutaAbsolutaImagen) {
        if (rutaAbsolutaImagen == null) return false;
        boolean estabaMarcada = estaMarcada(rutaAbsolutaImagen);
        if (estabaMarcada) {
            desmarcarImagenInterno(rutaAbsolutaImagen); // Llama al interno
            return false; // Ahora está desmarcada
        } else {
            marcarImagenInterno(rutaAbsolutaImagen);   // Llama al interno
            return true;  // Ahora está marcada
        }
    }

    
    
//package servicios; // o el paquete que hayas elegido
//
//import java.awt.Component; // Para el JOptionPane
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//
//import javax.swing.JOptionPane;
//
//public class ProjectManager {
//
//    private final Path archivoSeleccionTemporal;
//    private Set<String> clavesMarcadasEnMemoria;
//    private final ConfigurationManager configManager;
//    
//    
//    public ProjectManager(ConfigurationManager configManager) {
//        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null en ProjectManager");
//
//        // 1. Obtener la ruta base para la carpeta de proyectos desde la configuración
//        String carpetaBaseProyectosStr = this.configManager.getString(ConfigurationManager.KEY_PROYECTOS_CARPETA_BASE, ".proyectos_data"); // Fallback
//        Path carpetaBaseProyectos = Paths.get(carpetaBaseProyectosStr);
//
//        // 2. Asegurar que la ruta base sea absoluta o resolverla si es relativa
//        if (!carpetaBaseProyectos.isAbsolute()) {
//            // Si es relativa, la resolvemos desde el directorio actual de la aplicación
//            // Esto es útil si el config.cfg se distribuye con la app y ".proyectos_data" está junto al JAR/exe.
//            // Si quieres que SIEMPRE sea en el home del usuario, incluso si la config dice ".",
//            // tendrías que añadir esa lógica aquí.
//            // Por ahora, si es relativa, es relativa al CWD (Current Working Directory).
//            // Para que sea relativa al directorio del JAR, se necesitaría código más complejo.
//            // Una solución simple es que el usuario ponga una ruta absoluta en config.cfg si es necesario.
//            carpetaBaseProyectos = Paths.get("").toAbsolutePath().resolve(carpetaBaseProyectos);
//            System.out.println("  [ProjectManager] Carpeta base de proyectos resuelta a ruta absoluta: " + carpetaBaseProyectos);
//        }
//
//        // 3. Intentar crear el directorio base de proyectos si no existe
//        try {
//            if (!Files.exists(carpetaBaseProyectos)) {
//                Files.createDirectories(carpetaBaseProyectos);
//                System.out.println("  [ProjectManager] Directorio base de proyectos creado: " + carpetaBaseProyectos);
//            }
//        } catch (IOException e) {
//            System.err.println("WARN [ProjectManager]: No se pudo crear el directorio base de proyectos: " + carpetaBaseProyectos +
//                               ". Se usará el directorio actual como fallback para el archivo de selección.");
//            // Fallback si no se puede crear el directorio especificado
//            carpetaBaseProyectos = Paths.get("").toAbsolutePath(); // Directorio actual de la aplicación
//        }
//
//        // 4. Obtener el nombre del archivo de selección temporal/actual desde la configuración
//        String nombreArchivoSeleccion = this.configManager.getString(ConfigurationManager.KEY_PROYECTOS_ARCHIVO_TEMPORAL, "seleccion_default.txt");
//
//        // 5. Combinar la carpeta base con el nombre del archivo
//        this.archivoSeleccionTemporal = carpetaBaseProyectos.resolve(nombreArchivoSeleccion);
//
//        // 6. Cargar la selección desde este archivo
//        this.clavesMarcadasEnMemoria = cargarDesdeArchivo(this.archivoSeleccionTemporal);
//        System.out.println("[ProjectManager] Instancia creada. Selección actual cargada desde: " + this.archivoSeleccionTemporal.toAbsolutePath());
//        System.out.println("  -> Imágenes marcadas inicialmente: " + this.clavesMarcadasEnMemoria.size());
//    }
//
//
//
////    public ProjectManager() {
////    	
////    	
////        // Definir la ruta del archivo temporal.
////        // Puedes hacerlo más robusto creando un subdirectorio en System.getProperty("user.home")
////        String userHome = System.getProperty("user.home");
////        Path configDir = Paths.get(userHome, ".miVisorImagenesApp"); // Ejemplo de directorio de config
////        System.out.println("el archivo temporal esta en " + configDir.toString() + "//" + userHome);
////        try {
////            if (!Files.exists(configDir)) {
////                Files.createDirectories(configDir);
////            }
////        } catch (IOException e) {
////            System.err.println("WARN [ProjectManager]: No se pudo crear el directorio de configuración: " + configDir);
////            // Fallback a directorio actual si falla la creación del directorio de usuario
////            configDir = Paths.get("."); // Directorio actual de la aplicación
////        }
////        this.archivoSeleccionTemporal = configDir.resolve("seleccion_temporal.txt");
////
////        this.clavesMarcadasEnMemoria = cargarDesdeArchivo(this.archivoSeleccionTemporal);
////        System.out.println("[ProjectManager] Instancia creada. Selección temporal cargada desde: " + this.archivoSeleccionTemporal);
////        System.out.println("  -> Imágenes marcadas inicialmente: " + this.clavesMarcadasEnMemoria.size());
////    }
//
//    
//    private Set<String> cargarDesdeArchivo(Path rutaArchivo) {
//        Set<String> claves = new HashSet<>();
//        if (Files.exists(rutaArchivo) && Files.isReadable(rutaArchivo)) {
//            try (BufferedReader reader = Files.newBufferedReader(rutaArchivo)) {
//                String linea;
//                while ((linea = reader.readLine()) != null) {
//                    String clave = linea.trim();
//                    if (!clave.isEmpty()) {
//                        claves.add(clave);
//                    }
//                }
//                System.out.println("  [ProjectManager] Cargadas " + claves.size() + " claves desde " + rutaArchivo);
//            } catch (IOException e) {
//                System.err.println("ERROR [ProjectManager]: No se pudo leer el archivo de selección: " + rutaArchivo + " - " + e.getMessage());
//            }
//        } else {
//            System.out.println("  [ProjectManager] Archivo de selección no encontrado o no legible: " + rutaArchivo + ". Se iniciará con selección vacía.");
//        }
//        return claves;
//    }
//
//    private void guardarAArchivo(Path rutaArchivo, Set<String> claves) {
//        try (BufferedWriter writer = Files.newBufferedWriter(rutaArchivo, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
//            for (String clave : claves) {
//                writer.write(clave);
//                writer.newLine();
//            }
//            System.out.println("  [ProjectManager] Selección guardada en " + rutaArchivo + " (" + claves.size() + " claves).");
//        } catch (IOException e) {
//            System.err.println("ERROR [ProjectManager]: No se pudo guardar el archivo de selección: " + rutaArchivo + " - " + e.getMessage());
//            // Considerar mostrar un error al usuario si el guardado es crítico
//        }
//    }
//
//    public void marcarImagen(String claveRelativa) {
//        if (claveRelativa == null || claveRelativa.isBlank()) return;
//        if (this.clavesMarcadasEnMemoria.add(claveRelativa)) {
//            guardarAArchivo(this.archivoSeleccionTemporal, this.clavesMarcadasEnMemoria); // Usa el path actual
//            System.out.println("  [ProjectManager] Imagen marcada: " + claveRelativa);
//        }
//    }
//
//    public void desmarcarImagen(String claveRelativa) {
//        if (claveRelativa == null || claveRelativa.isBlank()) return;
//        if (this.clavesMarcadasEnMemoria.remove(claveRelativa)) {
//            guardarAArchivo(this.archivoSeleccionTemporal, this.clavesMarcadasEnMemoria); // Usa el path actual
//            System.out.println("  [ProjectManager] Imagen desmarcada: " + claveRelativa);
//        }
//    }
//
//    /**
//     * Verifica si una imagen, identificada por su clave relativa, está actualmente marcada.
//     * @param claveRelativa La clave de la imagen a verificar.
//     * @return true si la imagen está marcada, false en caso contrario o si la clave es null/vacía.
//     */
//    public boolean estaMarcada(String claveRelativa) {
//        if (claveRelativa == null || claveRelativa.isBlank()) {
//            return false;
//        }
//        boolean marcada = this.clavesMarcadasEnMemoria.contains(claveRelativa);
//        // System.out.println("  [ProjectManager estaMarcada] Verificando '" + claveRelativa + "': " + marcada); // Log opcional para depuración
//        return marcada;
//    }
//
//    public List<String> getClavesMarcadas() {
//        // Devuelve una copia ordenada para consistencia si se muestra en una lista
//        List<String> sortedList = new ArrayList<>(this.clavesMarcadasEnMemoria);
//        java.util.Collections.sort(sortedList);
//        return sortedList;
//    }
//
//    /**
//     * Alterna el estado de marca de una imagen. Si estaba marcada, la desmarca, y viceversa.
//     * @param claveRelativa La clave de la imagen.
//     * @return true si la imagen quedó marcada después de la operación, false si quedó desmarcada.
//     */
//    public boolean alternarMarcaImagen(String claveRelativa) {
//        if (claveRelativa == null || claveRelativa.isBlank()) return false;
//
//        boolean estabaMarcada = estaMarcada(claveRelativa);
//        if (estabaMarcada) {
//            desmarcarImagen(claveRelativa);
//            return false; // Ahora está desmarcada
//        } else {
//            marcarImagen(claveRelativa);
//            return true;  // Ahora está marcada
//        }
//    }
//
//    /**
//     * Método placeholder que será llamado para gestionar la selección de proyecto.
//     * Actualmente solo muestra un JOptionPane.
//     * @param parentComponent El componente padre para el JOptionPane (puede ser null).
//     */
//    public void gestionarSeleccionProyecto(Component parentComponent) {
//        String message = "Funcionalidad para gestionar la selección de imágenes del proyecto (marcar, ver, guardar, cargar) aún no implementada.\n\n" +
//                         "Consulta el megacomentario en ProjectManager.java para ver el plan detallado.";
//        JOptionPane.showMessageDialog(parentComponent,
//                                      message,
//                                      "Gestión de Selección de Proyecto (Pendiente)",
//                                      JOptionPane.INFORMATION_MESSAGE);
//        System.out.println("[ProjectManager] Diálogo placeholder 'gestionarSeleccionProyecto' mostrado.");
//    }
//
//    /**
//     * (Este método estaba en tu Action placeholder, lo muevo aquí para que sea el ProjectManager quien lo haga)
//     * Método placeholder para la lógica de marcar/desmarcar. En la implementación real,
//     * interactuará con `clavesMarcadasEnMemoria` y `guardarAArchivo`.
//     * @param claveImagen La clave de la imagen a marcar/desmarcar.
//     * @param marcar true para marcar, false para desmarcar.
//     */
//    public void marcarDesmarcarImagenActual(String claveImagen, boolean marcar) {
//        // Esta es la lógica que realmente altera el estado
//        if (marcar) {
//            marcarImagen(claveImagen);
//        } else {
//            desmarcarImagen(claveImagen);
//        }
//        // El log ya lo hacen marcarImagen/desmarcarImagen
//    }


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
}