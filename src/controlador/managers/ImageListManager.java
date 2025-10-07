package controlador.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.managers.FilterManager.FilterResult;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import controlador.worker.BuscadorArchivosWorker;
import modelo.VisorModel;
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
    private final GeneralController generalController;
    
    // --- Estado Interno ---
    private Future<?> cargaImagenesFuture;


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
        this.generalController = visorController.getGeneralController();
    } // --- FIN de constructor ImageListManager ---
    
    
    /**
     * Carga o recarga la lista de imágenes desde disco para una carpeta específica,
     * utilizando un SwingWorker para no bloquear el EDT. Muestra un diálogo de
     * progreso durante la carga. Una vez cargada la lista: 
     * - Actualiza el modelo principal de datos (`VisorModel`). 
     * - Actualiza las JList en la vista (`VisorView`). 
     * - Inicia el precalentamiento ASÍNCRONO y DIRIGIDO del caché de miniaturas. 
     * - Selecciona una imagen específica (si se proporciona `claveImagenAMantener`) 
     *   o la primera imagen de la lista. 
     * - Ejecuta un callback opcional al finalizar con éxito.
     *
     * @param claveImagenAMantener La clave única (ruta relativa) de la imagen que
     *                             se intentará seleccionar después de que la lista
     *                             se cargue. Si es `null`, se seleccionará la
     *                             primera imagen (índice 0).
     * @param alFinalizarConExito Un objeto Runnable cuya lógica se ejecutará en el EDT
     *                            después de que la carga y el procesamiento de la lista
     *                            hayan finalizado con éxito. Puede ser `null`.
     */
    public void cargarListaImagenes(String claveImagenAMantener, Runnable alFinalizarConExito) {
        logger.debug("-->>> INICIO ImageListManager.cargarListaImagenes | Mantener Clave: " + claveImagenAMantener);

        if (visorController.getConfigurationManager() == null || model == null || executorService == null || executorService.isShutdown() || view == null) {
            logger.error("ERROR [cargarListaImagenes]: Dependencias nulas o Executor apagado.");
            if (view != null) SwingUtilities.invokeLater(visorController.getViewManager()::limpiarUI);
            return;
        }

        if (cargaImagenesFuture != null && !cargaImagenesFuture.isDone()) {
            logger.debug("  -> Cancelando tarea de carga de lista anterior...");
            cargaImagenesFuture.cancel(true);
        }

        final boolean mostrarSoloCarpeta = model.isMostrarSoloCarpetaActual();
        int depth = mostrarSoloCarpeta ? 1 : Integer.MAX_VALUE;
        Path pathDeInicioWalk = model.getCarpetaRaizActual();

        if (pathDeInicioWalk == null || !Files.isDirectory(pathDeInicioWalk)) {
            logger.warn("[cargarListaImagenes] No se puede cargar: Carpeta de inicio inválida o nula: " + pathDeInicioWalk);
            visorController.getViewManager().limpiarUI();
            if (statusBarManager != null) {
                statusBarManager.mostrarMensaje("No hay una carpeta válida seleccionada. Usa 'Archivo -> Abrir Carpeta'.");
            }
            return;
        }
        
        if (this.thumbnailService != null) {
            this.thumbnailService.limpiarCache();
        }
        
        final TaskProgressDialog dialogo = new TaskProgressDialog(view, "Cargando Imágenes", "Escaneando carpeta de imágenes...");
        final BuscadorArchivosWorker worker = new BuscadorArchivosWorker(
            pathDeInicioWalk,
            depth,
            pathDeInicioWalk,
            this::esArchivoImagenSoportado,
            dialogo
        );
        dialogo.setWorkerAsociado(worker);
        this.cargaImagenesFuture = worker;

        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                
                if (worker.isCancelled()) {
                    logger.debug("    -> Tarea CANCELADA por el usuario.");
                    dialogo.setFinalMessageAndClose("Carga cancelada.", false, 1500);
                    if (statusBarManager != null) {
                        statusBarManager.mostrarMensaje("Carga cancelada por el usuario.");
                    }
                    return;
                }

                try {
                    Map<String, Path> mapaResultado = worker.get();

                    if (mapaResultado == null || mapaResultado.isEmpty()) {
                        logger.info("    -> La búsqueda no encontró imágenes soportadas. Entrando en estado de bienvenida final.");
                        dialogo.setFinalMessageAndClose("La carpeta no contiene imágenes.", false, 2000);
                        
                        visorController.getViewManager().limpiarUI(); 
                        
                        if (listCoordinator != null) {
                            listCoordinator.forzarActualizacionEstadoAcciones();
                        }
                        
                        if (statusBarManager != null) {
                            statusBarManager.mostrarMensaje("La carpeta no contiene imágenes. Abre otra para empezar.");
                        }
                        
                        return; 
                    }

                    dialogo.closeDialog();
                    
                    if (statusBarManager != null) statusBarManager.limpiarMensaje();
                    
                    logger.debug("    -> Restaurando visibilidad de paneles según la configuración del usuario.");
                    if (registry != null && actionMap != null) {
                        
                        Action fileListAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_FILE_LIST);
                        if (fileListAction != null) {
                            boolean shouldBeVisible = Boolean.TRUE.equals(fileListAction.getValue(Action.SELECTED_KEY));
                            JPanel panelIzquierdo = registry.get("panel.izquierdo.contenedorPrincipal");
                            if (panelIzquierdo != null) {
                                panelIzquierdo.setVisible(shouldBeVisible);
                                if (shouldBeVisible) {
                                    JSplitPane splitPane = registry.get("splitpane.main");
                                    if (splitPane != null) {
                                        splitPane.setDividerLocation(0.25);
                                    }
                                }
                            }
                        }

                        Action thumbnailsAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_THUMBNAILS);
                        if (thumbnailsAction != null) {
                            boolean shouldBeVisible = Boolean.TRUE.equals(thumbnailsAction.getValue(Action.SELECTED_KEY));
                            JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
                            if (scrollMiniaturas != null) {
                                scrollMiniaturas.setVisible(shouldBeVisible);
                            }
                        }
                    }

                    List<String> clavesOrdenadas = new ArrayList<>(mapaResultado.keySet());
                    java.util.Collections.sort(clavesOrdenadas);

                    DefaultListModel<String> nuevoModeloListaPrincipal = new DefaultListModel<>();
                    nuevoModeloListaPrincipal.addAll(new java.util.Vector<>(clavesOrdenadas));
                    
                    model.setMasterListAndNotify(nuevoModeloListaPrincipal, mapaResultado, visorController);

                    // --- INICIO DE LA MODIFICACIÓN CRÍTICA ---
                    // Notificamos al FilterManager cuál es la nueva lista maestra absoluta.
                    // Esta es ahora la única "fuente de la verdad" para todos los filtros.
                    if (this.filterManager != null) {
                        this.filterManager.setAbsoluteMasterList(nuevoModeloListaPrincipal);
                    }
                    // --- FIN DE LA MODIFICACIÓN CRÍTICA ---
                    
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

                    if (listCoordinator != null && indiceCalculado != -1) {
                        listCoordinator.reiniciarYSeleccionarIndice(indiceCalculado);
                    }

                    if (alFinalizarConExito != null) {
                        alFinalizarConExito.run();
                    }

                } catch (Exception e) {
                    logger.error("    -> ERROR durante la ejecución del worker: " + e.getMessage(), e);
                    dialogo.setFinalMessageAndClose("Error durante la carga.", true, 2500);
                    visorController.getViewManager().limpiarUI();
                    
                    if (statusBarManager != null) {
                        statusBarManager.mostrarMensaje("Error al leer la carpeta. Consulta los logs para más detalles.");
                    }
                } finally {
                    if (cargaImagenesFuture == worker) {
                        cargaImagenesFuture = null;
                    }
                }
            }
        });

        worker.execute();
        SwingUtilities.invokeLater(() -> {
            if (dialogo != null) {
                dialogo.setVisible(true);
            }
        });
        
    } // --- fin del metodo cargarListaImagenes ---
    
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

} // --- FIN de clase ImageListManager ---

