package controlador.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ListCoordinator;
import controlador.VisorController;
import controlador.managers.FilterManager.FilterResult;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import controlador.worker.BuscadorArchivosWorker;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import modelo.datos.ImagenInfo;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.db.ImagenDAO;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.dialogos.TaskProgressDialog;


public class ImageListManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageListManager.class);
    private static final Set<String> DIRECTORIOS_OMITIR = inicializarDirectoriosOmitir();

    private static Set<String> inicializarDirectoriosOmitir() {
        Set<String> dirs = new HashSet<>();
        String raw = ConfigurationManager.getInstance().getString(ConfigKeys.INDEXACION_OMITIR_DIRECTORIOS, "__MACOSX");
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(",")) {
                dirs.add(part.trim().toLowerCase());
            }
        }
        return dirs;
    }

    private static boolean perteneceADirectorioOmitido(Path path) {
        for (int i = 0; i < path.getNameCount(); i++) {
            if (DIRECTORIOS_OMITIR.contains(path.getName(i).toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // --- Dependencias ---
    private FilterManager filterManager;
    private final VisorController visorController;
    private final VisorModel model;
    private final VisorView view;
    private final IListCoordinator listCoordinator;
    private final ThumbnailService thumbnailService;
    private final ExecutorService executorService;
    private InfobarStatusManager statusBarManager;
    private final ComponentRegistry registry;
    private servicios.ConfigurationManager configuration;
    private vista.theme.ThemeManager themeManager;
    private vista.util.IconUtils iconUtils;
    
//    private final GeneralController generalController;

    // --- Estado Interno ---
    private boolean isSyncing = false;

    private final ImagenDAO imagenDAO;

    /**
     * Constructor que inicializa el gestor de la lista de imágenes con todas sus dependencias.
     * @param visorController El controlador principal que provee acceso a los componentes del sistema.
     */
    public ImageListManager(VisorController visorController) {
        logger.debug("Creando instancia de ImageListManager...");
        this.visorController = visorController;

        // Obtenemos las dependencias desde el controlador principal
        this.model = visorController.getModel();
        this.view = visorController.getView();
        this.listCoordinator = visorController.getListCoordinator();
        this.thumbnailService = visorController.getServicioMiniaturas();
        this.executorService = visorController.getExecutorService();
        this.statusBarManager = visorController.getStatusBarManager();
        this.registry = visorController.getComponentRegistry();
        this.configuration = visorController.getConfigurationManager();
        this.themeManager = visorController.getThemeManager();
        this.iconUtils = visorController.getIconUtils();
        
        this.imagenDAO = new ImagenDAO();
        
//        this.generalController = visorController.getGeneralController();
        
    } // --- FIN de constructor ImageListManager ---
    
    
    /**
     * Carga la lista de imágenes DESDE LA BASE DE DATOS para una carpeta específica.
     * VERSIÓN FINAL REVISADA: Es una operación de SOLO LECTURA. No inicia
     * sincronización bajo ninguna circunstancia para evitar bucles.
     *
     * @param claveImagenAMantener La clave (ruta relativa) de la imagen a seleccionar.
     * @param alFinalizarConExito Un Runnable a ejecutar en el EDT al finalizar con éxito.
     */
    public void cargarListaImagenes(String claveImagenAMantener, Runnable alFinalizarConExito) {
        if (isSyncing) {
            logger.warn("Llamada a cargarListaImagenes ignorada porque una sincronización está en progreso.");
            return;
        }
        logger.debug("-->>> INICIO ImageListManager.cargarListaImagenes (MODO LECTURA ESTRICTO) | Mantener Clave: {}", claveImagenAMantener);
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(stack.length, 8); i++) {
            logger.debug("       at {}.{}({}:{})", stack[i].getClassName(), stack[i].getMethodName(),
                stack[i].getFileName(), stack[i].getLineNumber());
        }

        if (visorController.getConfigurationManager() == null || model == null || view == null || imagenDAO == null) {
            logger.error("ERROR [cargarListaImagenes BD]: Dependencias nulas.");
            if (view != null) SwingUtilities.invokeLater(visorController.getViewManager()::limpiarUI);
            return;
        }

        Path pathDeInicio = model.getCarpetaRaizActual();

        if (pathDeInicio == null || !Files.isDirectory(pathDeInicio)) {
            logger.warn("[cargarListaImagenes BD] Carpeta de inicio inválida: {}", pathDeInicio);
            visorController.getViewManager().limpiarUI();
            if (statusBarManager != null) statusBarManager.mostrarMensaje("No hay una carpeta válida seleccionada.");
            return;
        }

        List<ImagenInfo> imagenesDesdeBD = imagenDAO.getImagenesInFolder(pathDeInicio);

        if (model.isMostrarSoloCarpetaActual()) {
            Path normalizedRaiz = pathDeInicio.toAbsolutePath().normalize();
            List<ImagenInfo> filtradas = new ArrayList<>();
            for (ImagenInfo img : imagenesDesdeBD) {
                Path parent = img.getRutaCompletaAsPath().getParent();
                if (parent != null && parent.toAbsolutePath().normalize().equals(normalizedRaiz)) {
                    filtradas.add(img);
                }
            }
            imagenesDesdeBD = filtradas;
        }
        // incluso si está vacía. El bloque que causaba el bucle ha sido eliminado.

        logger.debug("    -> Restaurando visibilidad de paneles (vía ViewManager).");
        if (visorController != null && visorController.getViewManager() != null) {
            visorController.getViewManager().asegurarVisibilidadPanelesBase();
        }
        
        DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
        Map<String, Path> nuevoMapaDeRutas = new java.util.HashMap<>();
        
        List<String> clavesOrdenadas = new ArrayList<>();
        Path carpetaRaizParaRelativizar = model.getCarpetaRaizActual();
        if (carpetaRaizParaRelativizar == null) {
        	carpetaRaizParaRelativizar = Paths.get(""); 
        }
        
        for (ImagenInfo imgInfo : imagenesDesdeBD) {
            try {
                Path rutaCompleta = imgInfo.getRutaCompletaAsPath();
                Path rutaRelativa = carpetaRaizParaRelativizar.relativize(rutaCompleta);
                String claveUnica = rutaRelativa.toString().replace("\\", "/");
                
                clavesOrdenadas.add(claveUnica);
                nuevoMapaDeRutas.put(claveUnica, rutaCompleta);
            } catch (Exception e) {
                 logger.error("Error al relativizar la ruta {} contra la raíz {}", imgInfo.getRutaCompleta(), carpetaRaizParaRelativizar, e);
            }
        }
        java.util.Collections.sort(clavesOrdenadas);
        nuevoModeloListaPrincipal.addAll(new java.util.Vector<>(clavesOrdenadas));

        if (statusBarManager != null) statusBarManager.limpiarMensaje();
        
        model.setMasterListAndNotify(nuevoModeloListaPrincipal, nuevoMapaDeRutas, visorController);

        if (this.filterManager != null) {
            this.filterManager.setAbsoluteMasterList(nuevoModeloListaPrincipal);
        }
        
        if (view != null) {
            view.setListaImagenesModel(model.getModeloLista());
            int totalArchivos = this.filterManager != null && this.filterManager.getAbsoluteMasterListSize() > 0 ? this.filterManager.getAbsoluteMasterListSize() : model.getModeloLista().getSize();
            boolean isFilterActive = this.filterManager != null && this.filterManager.isFilterActive();
            String titulo = isFilterActive ? "Archivos (Filtro): " + totalArchivos + " - " + model.getModeloLista().getSize() : "Archivos: " + totalArchivos;
            view.setTituloPanelIzquierdo(titulo);
        }
        
        int indiceCalculado = -1;
        if (claveImagenAMantener != null && !claveImagenAMantener.isEmpty()) {
            indiceCalculado = model.getModeloLista().indexOf(claveImagenAMantener);
        }
        if (indiceCalculado == -1 && !model.getModeloLista().isEmpty()) {
            indiceCalculado = 0;
        }

        if (listCoordinator != null) {
            listCoordinator.reiniciarYSeleccionarIndice(indiceCalculado);
        }

        if (alFinalizarConExito != null) {
            alFinalizarConExito.run();
        }

        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }

        logger.debug("-->>> FIN ImageListManager.cargarListaImagenes (MODO LECTURA ESTRICTO)");
    } // --- fin del metodo cargarListaImagenes ---
    
    
    /**
     * Inicia un proceso en segundo plano para sincronizar el contenido de una carpeta en disco
     * con la base de datos. TODO EL PROCESO (lectura y escritura) se hace en background.
     * 
     * @return true si se inició el proceso de sincronización, false en caso contrario.
     */
    public boolean sincronizarCarpetaConBD() {
    	
    	isSyncing = true;
    	
    	// --- INICIO DEPURACIÓN ---
        System.out.println("ImageListManager: Se ha recibido la orden de sincronizar.");
        final Path pathDeInicio = model.getCarpetaRaizActual();
        System.out.println("La carpeta que voy a sincronizar es: " + pathDeInicio);
        // --- FIN DEPURACIÓN ---
    	
        if (model == null || executorService == null || executorService.isShutdown()) {
            logger.error("ERROR [sincronizarCarpetaConBD]: Dependencias nulas o Executor apagado.");
            isSyncing = false;
            return false;
        }

        if (pathDeInicio == null || !Files.isDirectory(pathDeInicio)) {
            logger.warn("[sincronizarCarpetaConBD] Carpeta de inicio inválida: {}", pathDeInicio);
            isSyncing = false;
            return false;
        }
        
        // El escaneo SIEMPRE es recursivo (profundidad completa). El toggle
        // "Mostrar subcarpetas" solo filtra la presentación, no el barrido.
        final int depth = Integer.MAX_VALUE;

        final TaskProgressDialog dialogoBusqueda = new TaskProgressDialog(view, "Sincronizando Carpeta", "Paso 1/2: Buscando archivos en disco...");
        final BuscadorArchivosWorker buscador = new BuscadorArchivosWorker(pathDeInicio, depth, pathDeInicio, this::esArchivoImagenSoportado, dialogoBusqueda);
        dialogoBusqueda.setWorkerAsociado(buscador);
        
        buscador.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                dialogoBusqueda.dispose(); // Cerramos el primer diálogo
                if (buscador.isCancelled()) {
                	isSyncing = false; 
                	return;
                }

                try {
                    Map<String, Path> archivosEnDisco = buscador.get();
                    if (archivosEnDisco == null || archivosEnDisco.isEmpty()) {
                        logger.info("No se encontraron imágenes en disco para sincronizar.");
                        // Aun así, eliminamos los huérfanos que pudieran quedar en la BD
                        eliminarHuerfanosDeBD(pathDeInicio, new java.util.HashSet<>());
                        SwingUtilities.invokeLater(() -> recargarListaDesdeBDSinSincronizar(null, null));
                        isSyncing = false;
                        return;
                    }
                    
                    // Guardamos el conjunto de rutas absolutas que SÍ existen en disco.
                    final java.util.Set<String> rutasEnDisco = new java.util.HashSet<>();
                    for (Path p : archivosEnDisco.values()) {
                        rutasEnDisco.add(p.toAbsolutePath().normalize().toString());
                    }
                    
                    // Paso 2/2: Indexar los archivos encontrados en disco.
                    List<Path> archivosAIndexar = new ArrayList<>(archivosEnDisco.values());
                    
                    TaskProgressDialog dialogoIndexacion = new TaskProgressDialog(view, "Sincronizando Base de Datos", "Paso 2/2: Actualizando base de datos...");
                    controlador.worker.IndexationWorker indexerWorker = new controlador.worker.IndexationWorker(archivosAIndexar, pathDeInicio, dialogoIndexacion);
                    
                    // Cuando el IndexationWorker termine: 1) limpiamos huérfanos, 2) recargamos la lista.
                    indexerWorker.addPropertyChangeListener(propChangeEvent -> {
                        if ("state".equals(propChangeEvent.getPropertyName()) && SwingWorker.StateValue.DONE.equals(propChangeEvent.getNewValue())) {
                            dialogoIndexacion.dispose();
                            // Paso 3/3 (en background): borrar registros de BD cuyo archivo ya no existe.
                            eliminarHuerfanosDeBD(pathDeInicio, rutasEnDisco);
                            SwingUtilities.invokeLater(() -> {
                                recargarListaDesdeBDSinSincronizar(model.getSelectedImageKey(), null);
                                isSyncing = false;
                            });
                        }
                    });

                    dialogoIndexacion.setWorkerAsociado(indexerWorker);
                    indexerWorker.execute();
                    dialogoIndexacion.setVisible(true);

                } catch (Exception e) {
                    logger.error("Error al finalizar el worker de búsqueda durante la sincronización", e);
                    isSyncing = false;
                }
            }
        });

        buscador.execute();
        dialogoBusqueda.setVisible(true);
        
        return true;
        
    } // --- fin del metodo sincronizarCarpetaConBD ---
    
    
    /**
     * Carga una nueva "lista maestra" en el modelo a partir de un resultado de filtro precalculado.
     * Este método actualiza el modelo de datos y luego reinicia el ListCoordinator.
     *
     * @param resultadoFiltro Un objeto FilterResult que contiene el nuevo modelo de lista y el mapa de rutas.
     * @param alFinalizarConExito Un Runnable opcional para ejecutar al final.
     */
    public void cargarListaDesdeFiltro(FilterResult resultadoFiltro, Runnable alFinalizarConExito) {
        logger.debug("-->>> INICIO ImageListManager.cargarListaDesdeFiltro | Tamaño: {}", resultadoFiltro.model().getSize());

        if (model == null || listCoordinator == null) {
            logger.error("ERROR [cargarListaDesdeFiltro]: Dependencias críticas (model, listCoordinator) nulas.");
            return;
        }

        DefaultListModel<String> modeloFiltrado = resultadoFiltro.model();
        Map<String, Path> mapaFiltrado = resultadoFiltro.pathMap();

        model.actualizarListaCompleta(modeloFiltrado, mapaFiltrado);

        int indiceASeleccionar = modeloFiltrado.isEmpty() ? -1 : 0;

        listCoordinator.reiniciarYSeleccionarIndice(indiceASeleccionar);

        if (alFinalizarConExito != null) {
            alFinalizarConExito.run();
        }
        
        logger.debug("-->>> FIN ImageListManager.cargarListaDesdeFiltro. Modelo actualizado.");
    
    } // --- Fin del método cargarListaDesdeFiltro ---

     /**
      * Verifica si un archivo, dado por su Path, tiene una extensión
      * correspondiente a los formatos de imagen que la aplicación soporta actualmente.
      *
      * @param path El objeto Path que representa la ruta del archivo a verificar.
      * @return true si el archivo tiene una extensión de imagen soportada.
      */
     private boolean esArchivoImagenSoportado(Path path) {
         if (path == null) {
             return false;
         }

         Path nombreArchivoPath = path.getFileName();
         if (nombreArchivoPath == null) {
             return false;
         }
          String nombreArchivo = nombreArchivoPath.toString();

         // Omitir archivos dentro de directorios problemáticos (ej. __MACOSX)
         if (perteneceADirectorioOmitido(path)) {
              return false;
         }

         try {
               if (!Files.isRegularFile(path) || Files.isHidden(path)) {
                   return false;
              }
         } catch (IOException e) {
               logger.warn("WARN [esArchivoImagenSoportado]: Error al comprobar atributos de " + path + ": " + e.getMessage());
               return false;
         } catch (SecurityException se) {
               logger.warn("WARN [esArchivoImagenSoportado]: Sin permisos para comprobar atributos de " + path);
               return false;
         }

         int lastDotIndex = nombreArchivo.lastIndexOf('.');
         if (lastDotIndex <= 0 || lastDotIndex == nombreArchivo.length() - 1) {
             return false;
         }

         String extension = nombreArchivo.substring(lastDotIndex + 1).toLowerCase();

         switch (extension) {
             case "jpg":
             case "jpeg":
             case "png":
             case "gif":
             case "bmp":
             case "tiff":
             case "psd":
             case "webp":
             case "tga":
             case "pcx":
                 return true;
             default:
                 return false;
         }
     } // --- FIN esArchivoImagenSoportado ---
    
    /**
      * Lanza tareas en segundo plano usando el ExecutorService para generar y cachear
      * las miniaturas de tamaño normal para la lista de rutas de imágenes proporcionada.
      *
      * @param rutas La lista de objetos Path correspondientes a todas las imágenes
      *              cargadas actualmente en el modelo principal.
      */
     public void precalentarCacheMiniaturasAsync(List<Path> rutas) {
         if (thumbnailService == null) {
              logger.error("ERROR [Precalentar Cache]: ThumbnailService es nulo.");
              return;
         }
         if (executorService == null || executorService.isShutdown()) {
              logger.error("ERROR [Precalentar Cache]: ExecutorService no está disponible o está apagado.");
              return;
         }
         if (rutas == null || rutas.isEmpty()) {
             logger.debug("[Precalentar Cache]: Lista de rutas vacía o nula. No hay nada que precalentar.");
             return;
         }
         if (model == null) {
              logger.error("ERROR [Precalentar Cache]: Modelo es nulo.");
              return;
         }

         logger.debug("[ImageListManager] Iniciando pre-calentamiento de caché para " + rutas.size() + " miniaturas...");

         final int anchoNormal = model.getMiniaturaNormAncho();
         final int altoNormal = model.getMiniaturaNormAlto();

         if (anchoNormal <= 0) {
             logger.error("ERROR [Precalentar Cache]: Ancho normal de miniatura inválido (" + anchoNormal + "). Abortando.");
             return;
         }

         int tareasLanzadas = 0;
         for (Path ruta : rutas) {
             if (ruta == null) continue;

             executorService.submit(() -> {
                 try {
                     Path relativePath = null;
                     Path carpetaRaizDelModelo = this.model.getCarpetaRaizActual();
                     
                     if (carpetaRaizDelModelo != null) {
                          try {
                              relativePath = carpetaRaizDelModelo.relativize(ruta);
                          } catch (Exception e) {
                               logger.error("ERROR [Precalentar Cache BG]: Relativizando " + ruta + ": " + e.getMessage());
                               relativePath = ruta.getFileName();
                          }
                     } else {
                          relativePath = ruta.getFileName();
                     }

                     if (relativePath == null) {
                          logger.error("ERROR [Precalentar Cache BG]: No se pudo obtener ruta relativa para " + ruta);
                          return;
                     }
                     String claveUnica = relativePath.toString().replace("\\", "/");

                     thumbnailService.obtenerOCrearMiniatura(
                    		 ruta, claveUnica, anchoNormal, altoNormal, true
                     );

                 } catch (Exception e) {
                     logger.error("ERROR INESPERADO [Precalentar Cache BG] Procesando " + ruta + ": " + e.getMessage(), e);
                 }
             });
             tareasLanzadas++;
         }

         logger.debug("[ImageListManager] " + tareasLanzadas + " tareas de pre-calentamiento de caché lanzadas al ExecutorService.");

         if (view != null && registry.get("list.miniaturas") != null) {
             SwingUtilities.invokeLater(() -> {
                 if (view != null && registry.get("list.miniaturas") != null) {
                      logger.debug("  -> Solicitando repintado inicial de listaMiniaturas.");
                      registry.get("list.miniaturas").repaint();
                 }
             });
         }

     } // --- FIN precalentarCacheMiniaturasAsync ---

     public void setFilterManager(FilterManager filterManager) {
         this.filterManager = filterManager;
     } // ---FIN de metodo setFilterManager---
     
     /**
      * Versión "segura" y de solo lectura para cargar la lista desde la BD.
      * NUNCA inicia una sincronización, garantizando que no haya bucles.
      * Se usa como callback después de procesos de escritura en la BD.
      * 
      * @param claveImagenAMantener La clave de la imagen a seleccionar.
      * @param alFinalizarConExito Runnable a ejecutar al final.
      */
      private void recargarListaDesdeBDSinSincronizar(String claveImagenAMantener, Runnable alFinalizarConExito) {
          logger.debug("-->>> INICIO ImageListManager.recargarListaDesdeBDSinSincronizar (SEGURO) | Clave: {}", claveImagenAMantener);
          StackTraceElement[] stack = Thread.currentThread().getStackTrace();
          for (int i = 2; i < Math.min(stack.length, 8); i++) {
              logger.debug("       at {}.{}({}:{})", stack[i].getClassName(), stack[i].getMethodName(),
                  stack[i].getFileName(), stack[i].getLineNumber());
          }

         Path pathDeInicio = model.getCarpetaRaizActual();
         if (pathDeInicio == null || !Files.isDirectory(pathDeInicio)) {
             visorController.getViewManager().limpiarUI();
             return;
         }

         List<ImagenInfo> imagenesDesdeBD = imagenDAO.getImagenesInFolder(pathDeInicio);

         if (model.isMostrarSoloCarpetaActual()) {
             Path normalizedRaiz = pathDeInicio.toAbsolutePath().normalize();
             List<ImagenInfo> filtradas = new ArrayList<>();
             for (ImagenInfo img : imagenesDesdeBD) {
                 Path parent = img.getRutaCompletaAsPath().getParent();
                 if (parent != null && parent.toAbsolutePath().normalize().equals(normalizedRaiz)) {
                     filtradas.add(img);
                 }
             }
             imagenesDesdeBD = filtradas;
         }
         
         // El resto del código es idéntico a cargarListaImagenes
         DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
         Map<String, Path> nuevoMapaDeRutas = new java.util.HashMap<>();
         
         List<String> clavesOrdenadas = new ArrayList<>();
         Path carpetaRaizParaRelativizar = model.getCarpetaRaizActual();
         if (carpetaRaizParaRelativizar == null) {
         	carpetaRaizParaRelativizar = Paths.get(""); 
         }
         
         for (ImagenInfo imgInfo : imagenesDesdeBD) {
             try {
                 Path rutaCompleta = imgInfo.getRutaCompletaAsPath();
                 Path rutaRelativa = carpetaRaizParaRelativizar.relativize(rutaCompleta);
                 String claveUnica = rutaRelativa.toString().replace("\\", "/");
                 
                 clavesOrdenadas.add(claveUnica);
                 nuevoMapaDeRutas.put(claveUnica, rutaCompleta);
             } catch (Exception e) {
                  logger.error("Error al relativizar la ruta {} contra la raíz {}", imgInfo.getRutaCompleta(), carpetaRaizParaRelativizar, e);
             }
         }
         java.util.Collections.sort(clavesOrdenadas);
         nuevoModeloListaPrincipal.addAll(new java.util.Vector<>(clavesOrdenadas));

         model.setMasterListAndNotify(nuevoModeloListaPrincipal, nuevoMapaDeRutas, visorController);

         if (this.filterManager != null) {
             this.filterManager.setAbsoluteMasterList(nuevoModeloListaPrincipal);
         }
         
         if (view != null) {
             view.setListaImagenesModel(model.getModeloLista());
             int totalArchivos = this.filterManager != null && this.filterManager.getAbsoluteMasterListSize() > 0 ? this.filterManager.getAbsoluteMasterListSize() : model.getModeloLista().getSize();
             boolean isFilterActive = this.filterManager != null && this.filterManager.isFilterActive();
             String titulo = isFilterActive ? "Archivos (Filtro): " + totalArchivos + " - " + model.getModeloLista().getSize() : "Archivos: " + totalArchivos;
             view.setTituloPanelIzquierdo(titulo);
         }
         
         int indiceCalculado = -1;
         if (claveImagenAMantener != null && !claveImagenAMantener.isEmpty()) {
             indiceCalculado = model.getModeloLista().indexOf(claveImagenAMantener);
         }
         if (indiceCalculado == -1 && !model.getModeloLista().isEmpty()) {
             indiceCalculado = 0;
         }

         
         
      // --- INICIO DE LA MODIFICACIÓN FINAL ---
         // Comprobamos en qué modo estamos para no usar el ListCoordinator del Visualizador
         // cuando estamos en el Modo Datos.
         if (model.getCurrentWorkMode() == WorkMode.DATOS) {
             // En el Modo Datos, no hay imagen principal que seleccionar, así que
             // simplemente nos aseguramos de que el grid se actualice. El listener
             // `onMasterListChanged` en GeneralController ya hace esto.
             logger.debug("Recarga finalizada en Modo Datos. La actualización del grid es manejada por el listener.");
         } else {
             // Si estamos en cualquier otro modo (como Visualizador), usamos el ListCoordinator.
             if (listCoordinator != null) {
                 listCoordinator.reiniciarYSeleccionarIndice(indiceCalculado);
             }
         }
         
         if (alFinalizarConExito != null) {
             alFinalizarConExito.run();
         }
         // --- FIN DE LA MODIFICACIÓN FINAL ---
         
         
         
         // 10. Restaurar la visibilidad de los paneles según la configuración
        if (visorController != null && visorController.getViewManager() != null) {
            visorController.getViewManager().asegurarVisibilidadPanelesBase();
        }

        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }

        logger.debug("-->>> FIN ImageListManager.recargarListaDesdeBDSinSincronizar (SEGURO)");
     } // ---FIN de metodo [recargarListaDesdeBDSinSincronizar]---

     /**
      * Identifica y elimina de la base de datos aquellos registros de imágenes que
      * ya no existen físicamente en el disco dentro de la carpeta especificada.
      *
      * @param carpetaRaiz  La carpeta donde buscar huérfanos.
      * @param rutasEnDisco El conjunto de rutas (normalizadas) que SÍ existen en disco.
      */
     private void eliminarHuerfanosDeBD(Path carpetaRaiz, java.util.Set<String> rutasEnDisco) {
         if (imagenDAO == null || carpetaRaiz == null) return;

         logger.debug("[ImageListManager] Buscando registros huérfanos en la BD para: {}", carpetaRaiz);
         List<ImagenInfo> imagenesEnBD = imagenDAO.getImagenesInFolder(carpetaRaiz);
         int borrados = 0;

         for (ImagenInfo img : imagenesEnBD) {
             String rutaBD = img.getRutaCompletaAsPath().toAbsolutePath().normalize().toString();
             if (!rutasEnDisco.contains(rutaBD)) {
                 logger.debug("  -> Eliminando registro huérfano (no existe en disco): {}", img.getRutaCompleta());
                 imagenDAO.deleteImagen(img.getId());
                 borrados++;
             }
         }

         if (borrados > 0) {
             logger.info("[ImageListManager] Sincronización completa: {} registros eliminados de la BD.", borrados);
         }
     }

    // ==================== GESTIÓN DE MINIATURAS ====================

    public DefaultListModel<String> getModeloMiniaturasVisualizador() {
        return visorController.getModeloMiniaturasVisualizador();
    }

    public DefaultListModel<String> getModeloMiniaturasCarrusel() {
        return visorController.getModeloMiniaturasCarrusel();
    }

    public DefaultListModel<String> getModeloMiniaturas() {
        if (model != null && model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
            return visorController.getModeloMiniaturasCarrusel();
        }
        return visorController.getModeloMiniaturasVisualizador();
    }

    public void solicitarRefrescoRenderersMiniaturas() {
        if (registry.get("list.miniaturas") != null) {
            logger.debug("  [ImageListManager] Solicitando repintado de listaMiniaturas.");
            registry.get("list.miniaturas").repaint();
        }
    }

    public void setMostrarNombresMiniaturas(boolean mostrar) {
        logger.debug("[ImageListManager] Solicitud para cambiar 'Mostrar Nombres en Miniaturas' a: " + mostrar);

        if (configuration == null || view == null || registry.get("list.miniaturas") == null || model == null ||
            thumbnailService == null || themeManager == null || iconUtils == null) {
            logger.error("ERROR CRÍTICO [setMostrarNombresMiniaturas]: Faltan dependencias esenciales. Operación cancelada.");
            return;
        }

        configuration.setString(servicios.ConfigKeys.VISTA_MOSTRAR_NOMBRES_MINIATURAS_STATE, String.valueOf(mostrar));

        int thumbWidth = configuration.getInt("miniaturas.tamano.normal.ancho", 40);
        int thumbHeight = configuration.getInt("miniaturas.tamano.normal.alto", 40);

        vista.renderers.MiniaturaListCellRenderer newRenderer = new vista.renderers.MiniaturaListCellRenderer(
            thumbnailService,
            model,
            visorController.getProjectManager(),
            themeManager,
            iconUtils,
            thumbWidth,
            thumbHeight,
            mostrar
        );

        final vista.renderers.MiniaturaListCellRenderer finalRenderer = newRenderer;
        SwingUtilities.invokeLater(() -> {
            javax.swing.JList<String> listaMin = registry.get("list.miniaturas");
            listaMin.setCellRenderer(finalRenderer);
            listaMin.setFixedCellHeight(finalRenderer.getAlturaCalculadaDeCelda());
            listaMin.setFixedCellWidth(finalRenderer.getAnchoCalculadaDeCelda());
            listaMin.revalidate();
            listaMin.repaint();

            if (listCoordinator != null) {
                listCoordinator.forzarActualizacionDeTiraDeMiniaturas();
            }
        });

        logger.debug("[ImageListManager] setMostrarNombresMiniaturas completado.");
    }

    private vista.panels.GridDisplayPanel getActiveGridPanel() {
        if (model.getCurrentWorkMode() == modelo.VisorModel.WorkMode.PROYECTO) {
            return registry.get("panel.display.grid.proyecto");
        } else if (model.getCurrentWorkMode() == modelo.VisorModel.WorkMode.DATOS) {
            return registry.get("panel.datamode.grid");
        }
        return registry.get("panel.display.grid");
    }

    public void aumentarTamanoMiniaturas() {
        final int STEP = 10;
        final int MAX_SIZE = 300;

        if (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID) {
            int currentWidth = configuration.getInt(servicios.ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
            int newWidth = Math.min(currentWidth + STEP, MAX_SIZE);
            configuration.setString(servicios.ConfigKeys.GRID_THUMBNAIL_WIDTH, String.valueOf(newWidth));
            configuration.setString(servicios.ConfigKeys.GRID_THUMBNAIL_HEIGHT, String.valueOf(newWidth));
            vista.panels.GridDisplayPanel gridVisor = getActiveGridPanel();
            if (gridVisor != null) gridVisor.setGridCellSize(newWidth, newWidth);
        } else {
            int currentNormWidth = configuration.getInt(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 70);
            int newNormWidth = Math.min(currentNormWidth + STEP, MAX_SIZE);
            configuration.setString(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, String.valueOf(newNormWidth));
            configuration.setString(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, String.valueOf(newNormWidth));

            if (thumbnailService != null) thumbnailService.limpiarCache();
            if (view != null) view.actualizarLayoutBarraMiniaturas();
            if (listCoordinator instanceof ListCoordinator) {
                ((ListCoordinator) listCoordinator).forzarActualizacionDeTiraDeMiniaturas();
            }
        }
    }

    public void reducirTamanoMiniaturas() {
        final int STEP = 10;
        final int MIN_SIZE = 40;

        if (model.getCurrentDisplayMode() == VisorModel.DisplayMode.GRID) {
            int currentWidth = configuration.getInt(servicios.ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
            int newWidth = Math.max(currentWidth - STEP, MIN_SIZE);
            configuration.setString(servicios.ConfigKeys.GRID_THUMBNAIL_WIDTH, String.valueOf(newWidth));
            configuration.setString(servicios.ConfigKeys.GRID_THUMBNAIL_HEIGHT, String.valueOf(newWidth));
            vista.panels.GridDisplayPanel gridVisor = getActiveGridPanel();
            if (gridVisor != null) gridVisor.setGridCellSize(newWidth, newWidth);
        } else {
            int currentNormWidth = configuration.getInt(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 70);
            int newNormWidth = Math.max(currentNormWidth - STEP, MIN_SIZE);
            configuration.setString(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, String.valueOf(newNormWidth));
            configuration.setString(servicios.ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, String.valueOf(newNormWidth));

            if (thumbnailService != null) thumbnailService.limpiarCache();
            if (view != null) view.actualizarLayoutBarraMiniaturas();
            if (listCoordinator instanceof ListCoordinator) {
                ((ListCoordinator) listCoordinator).forzarActualizacionDeTiraDeMiniaturas();
            }
        }
    }

    public RangoMiniaturasCalculado calcularNumMiniaturasDinamicas() {
        int cfgMiniaturasAntes, cfgMiniaturasDespues;

        if (model != null) {
            cfgMiniaturasAntes = model.getMiniaturasAntes();
            cfgMiniaturasDespues = model.getMiniaturasDespues();
        } else if (configuration != null) {
            cfgMiniaturasAntes = configuration.getInt("miniaturas.cantidad.antes", 8);
            cfgMiniaturasDespues = configuration.getInt("miniaturas.cantidad.despues", 8);
        } else {
            cfgMiniaturasAntes = 8;
            cfgMiniaturasDespues = 8;
        }

        javax.swing.JScrollPane scrollPane = registry.get("scroll.miniaturas");
        javax.swing.JList<String> listaMin = registry.get("list.miniaturas");

        if (scrollPane == null || listaMin == null) {
            return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
        }

        int viewportWidth = scrollPane.getViewport().getWidth();
        int cellWidth = listaMin.getFixedCellWidth();

        if (viewportWidth <= 0 || cellWidth <= 0 || !scrollPane.isShowing()) {
            return new RangoMiniaturasCalculado(cfgMiniaturasAntes, cfgMiniaturasDespues);
        }

        int totalMiniaturasQueCaben = viewportWidth / cellWidth;
        int numAntesCalculado;
        int numDespuesCalculado;
        int maxTotalConfigurado = cfgMiniaturasAntes + 1 + cfgMiniaturasDespues;

        if (totalMiniaturasQueCaben >= maxTotalConfigurado) {
            numAntesCalculado = cfgMiniaturasAntes;
            numDespuesCalculado = cfgMiniaturasDespues;
        } else if (totalMiniaturasQueCaben <= 1) {
            numAntesCalculado = 0;
            numDespuesCalculado = 0;
        } else {
            int miniaturasLateralesDisponibles = totalMiniaturasQueCaben - 1;
            double ratioAntesOriginal = (cfgMiniaturasAntes + cfgMiniaturasDespues > 0)
                    ? (double) cfgMiniaturasAntes / (cfgMiniaturasAntes + cfgMiniaturasDespues)
                    : 0.5;
            numAntesCalculado = (int) Math.round(miniaturasLateralesDisponibles * ratioAntesOriginal);
            numDespuesCalculado = miniaturasLateralesDisponibles - numAntesCalculado;
            numAntesCalculado = Math.min(numAntesCalculado, cfgMiniaturasAntes);
            numDespuesCalculado = Math.min(numDespuesCalculado, cfgMiniaturasDespues);
        }

        return new RangoMiniaturasCalculado(numAntesCalculado, numDespuesCalculado);
    }

    public static class RangoMiniaturasCalculado {
        public final int antes;
        public final int despues;

        public RangoMiniaturasCalculado(int antes, int despues) {
            this.antes = antes;
            this.despues = despues;
        }
    }

} // --- FIN de clase ImageListManager ---

