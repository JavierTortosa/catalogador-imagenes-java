package controlador.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.FilterManager.FilterResult;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import controlador.worker.BuscadorArchivosWorker;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import modelo.datos.ImagenInfo;
import servicios.db.ImagenDAO;
import servicios.db.TagDAO;
import servicios.image.ThumbnailService;
import vista.VisorView;
import vista.dialogos.TaskProgressDialog;


public class ImageListManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageListManager.class);

    // --- Dependencias ---
    private FilterManager filterManager;
    private final VisorController visorController;
    private final VisorModel model;
    private final VisorView view;
    private final IListCoordinator listCoordinator;
    private final ThumbnailService thumbnailService;
    private final ExecutorService executorService;
    private final InfobarStatusManager statusBarManager;
    private final ComponentRegistry registry;
    private final Map<String, Action> actionMap;
    
//    private final GeneralController generalController;
    
    // --- Estado Interno ---
    private Future<?> cargaImagenesFuture;
    private boolean isSyncing = false;

    private final ImagenDAO imagenDAO;
    private final TagDAO tagDAO;

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
        this.actionMap = visorController.getActionMap();
        
        this.imagenDAO = new ImagenDAO();
        this.tagDAO = new TagDAO(); 
        
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

        // A partir de aquí, el método simplemente procesa la lista 'imagenesDesdeBD',
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
            view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());
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
        
        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
        final int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;

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

         Path pathDeInicio = model.getCarpetaRaizActual();
         if (pathDeInicio == null || !Files.isDirectory(pathDeInicio)) {
             visorController.getViewManager().limpiarUI();
             return;
         }

         List<ImagenInfo> imagenesDesdeBD = imagenDAO.getImagenesInFolder(pathDeInicio);
         
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
             view.setTituloPanelIzquierdo("Archivos: " + model.getModeloLista().getSize());
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

} // --- FIN de clase ImageListManager ---

