	package controlador;
	
	import java.awt.event.MouseAdapter;
	import java.awt.event.MouseEvent;
	import java.nio.file.Files;
	import java.nio.file.Path;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	import java.util.Objects;
	import java.util.stream.Collectors;
	
	import javax.swing.Action;
	import javax.swing.DefaultListModel;
	import javax.swing.JButton;
	import javax.swing.JFileChooser;
	import javax.swing.JList;
	import javax.swing.JOptionPane;
	import javax.swing.JPanel;
	import javax.swing.JSplitPane;
	import javax.swing.JTable;
	import javax.swing.SwingUtilities;
	
	import controlador.interfaces.IModoController;
	import controlador.managers.ExportQueueManager;
	import controlador.managers.interfaces.IListCoordinator;
	import controlador.managers.interfaces.IProjectManager;
	import controlador.managers.interfaces.IViewManager;
	import controlador.managers.interfaces.IZoomManager;
	import controlador.utils.ComponentRegistry;
	import controlador.utils.DesktopUtils;
	import controlador.worker.ExportWorker;
	import modelo.ListContext;
	import modelo.VisorModel;
	import modelo.proyecto.ExportItem;
	import vista.VisorView;
	import vista.dialogos.ExportProgressDialog;
	import vista.panels.export.ExportTableModel;
	
	public class ProjectController implements IModoController {
	
	    private IProjectManager projectManager;
	    private ComponentRegistry registry;
	    private IZoomManager zoomManager;
	    private VisorView view;
	    private VisorModel model;
	    private VisorController controllerRef;
	    private ExportQueueManager exportQueueManager;
	    private ProjectListCoordinator projectListCoordinator;
	    
	    private IViewManager viewManager;
	    private IListCoordinator listCoordinator; 
	    private Map<String, Action> actionMap;
	    
	
	    public ProjectController() {
	        System.out.println("[ProjectController] Instancia creada.");
	        this.exportQueueManager = new ExportQueueManager();
	    } // --- Fin del método ProjectController (constructor) ---
	
	    
	    void configurarListeners() {
	        if (registry == null || model == null || projectListCoordinator == null) {
	            System.err.println("ERROR [ProjectController]: Dependencias nulas (registry, model o projectListCoordinator).");
	            return;
	        }
	
	        // --- Listeners para las listas de Selección y Descartes ---
	        JList<String> projectList = registry.get("list.proyecto.nombres");
	        JList<String> descartesList = registry.get("list.proyecto.descartes");
	
	        // Listener para detectar el cambio de FOCO (sin cambios, esta lógica sigue siendo del ProjectController)
	        MouseAdapter listMouseAdapter = new MouseAdapter() {
	            @Override
	            public void mousePressed(MouseEvent e) {
	                JList<?> sourceList = (JList<?>) e.getSource();
	                String nombreListaClicada = (sourceList == projectList) ? "seleccion" : "descartes";
	                if (!nombreListaClicada.equals(model.getProyectoListContext().getNombreListaActiva())) {
	                    cambiarFocoListaActiva(nombreListaClicada);
	                }
	            }
	        };
	
	        // --- Listener de SELECCIÓN para la lista de "Selección" (MODIFICADO) ---
	        if (projectList != null) {
	            projectList.addMouseListener(listMouseAdapter);
	            // Limpiamos listeners antiguos
	            for (javax.swing.event.ListSelectionListener lsl : projectList.getListSelectionListeners()) {
	                projectList.removeListSelectionListener(lsl);
	            }
	            projectList.addListSelectionListener(e -> {
	                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;
	                
	                // Solo actuamos si esta es la lista activa
	                if ("seleccion".equals(model.getProyectoListContext().getNombreListaActiva())) {
	                    // Delegamos la selección al coordinador con el índice de la lista de selección
	                    projectListCoordinator.seleccionarImagenPorIndice(projectList.getSelectedIndex());
	                }
	            });
	        }
	
	        // --- Listener de SELECCIÓN para la lista de "Descartes" (MODIFICADO) ---
	        if (descartesList != null) {
	            descartesList.addMouseListener(listMouseAdapter);
	            // Limpiamos listeners antiguos
	            for (javax.swing.event.ListSelectionListener lsl : descartesList.getListSelectionListeners()) {
	                descartesList.removeListSelectionListener(lsl);
	            }
	            descartesList.addListSelectionListener(e -> {
	                if (e.getValueIsAdjusting() || projectListCoordinator.isSincronizandoUI()) return;
	
	                // Solo actuamos si esta es la lista activa
	                if ("descartes".equals(model.getProyectoListContext().getNombreListaActiva())) {
	                    // Delegamos la selección al coordinador con el índice de la lista de descartes
	                    projectListCoordinator.seleccionarImagenPorIndice(descartesList.getSelectedIndex());
	                }
	            });
	        }
	        
	        // --- Listeners para el Panel de Exportación (sin cambios) ---
	        vista.panels.export.ExportPanel panelExportar = registry.get("panel.proyecto.herramientas.exportar");
	        if(panelExportar != null) {
	            JButton btnSeleccionarCarpeta = panelExportar.getBotonSeleccionarCarpeta();
	            if (btnSeleccionarCarpeta != null) {
	                btnSeleccionarCarpeta.addActionListener(e -> solicitarSeleccionCarpetaDestino());
	            }
	        }
	    } // --- Fin del método configurarListeners ---
	    
	    
	    private void cambiarFocoListaActiva(String nuevoFoco) {
	        System.out.println("  [ProjectController] Cambiando foco a lista: " + nuevoFoco);
	        model.getProyectoListContext().setNombreListaActiva(nuevoFoco);
	        
	        String claveARestaurar;
	        JList<String> listaQueGanaFoco;
	        
	        if ("seleccion".equals(nuevoFoco)) {
	            claveARestaurar = model.getProyectoListContext().getSeleccionListKey();
	            listaQueGanaFoco = registry.get("list.proyecto.nombres");
	        } else {
	            claveARestaurar = model.getProyectoListContext().getDescartesListKey();
	            listaQueGanaFoco = registry.get("list.proyecto.descartes");
	        }
	
	        // Si no hay clave guardada para la lista que gana el foco, seleccionamos la primera.
	        if (claveARestaurar == null && listaQueGanaFoco != null && listaQueGanaFoco.getModel().getSize() > 0) {
	            claveARestaurar = listaQueGanaFoco.getModel().getElementAt(0);
	        }
	        
	        // Usamos el nuevo método del coordinador, que es mucho más robusto.
	        projectListCoordinator.seleccionarImagenPorClave(claveARestaurar);
	        
	        actualizarAparienciaListasPorFoco();
	
	    } // --- Fin del método cambiarFocoListaActiva ---
	    
	    
	    public boolean prepararDatosProyecto() {
	        System.out.println("  [ProjectController] Preparando datos para el modo proyecto...");
	        if (projectManager == null || model == null) {
	            System.err.println("ERROR CRÍTICO [prepararDatosProyecto]: ProjectManager o Model nulos.");
	            return false;
	        }
	
	        List<java.nio.file.Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
	        List<java.nio.file.Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
	        
	        List<java.nio.file.Path> todasLasImagenes = new java.util.ArrayList<>();
	        todasLasImagenes.addAll(imagenesMarcadas);
	        todasLasImagenes.addAll(imagenesDescartadas);
	        
	        todasLasImagenes = todasLasImagenes.stream().distinct().collect(Collectors.toList());
	        java.util.Collections.sort(todasLasImagenes);
	
	        if (todasLasImagenes.isEmpty()) {
	            JOptionPane.showMessageDialog(view, "No hay imágenes marcadas ni descartadas en el proyecto actual.", "Proyecto Vacío", JOptionPane.INFORMATION_MESSAGE);
	            return false;
	        }
	
	        DefaultListModel<String> modeloUnificado = new DefaultListModel<>();
	        Map<String, Path> mapaRutasProyecto = new HashMap<>();
	        
	        for (java.nio.file.Path rutaAbsoluta : todasLasImagenes) {
	            String clave = rutaAbsoluta.toString().replace("\\", "/");
	            modeloUnificado.addElement(clave);
	            mapaRutasProyecto.put(clave, rutaAbsoluta);
	        }
	
	        ListContext proyectoContext = model.getProyectoListContext();
	        proyectoContext.actualizarContextoCompleto(modeloUnificado, mapaRutasProyecto);
	        
	        System.out.println("    -> Datos del proyecto preparados en proyectoListContext. Total de imágenes (selección + descartes): " + modeloUnificado.getSize());
	        return true;
	    } // --- Fin del método prepararDatosProyecto ---
	
	    
	    public void activarVistaProyecto() {
	        System.out.println("  [ProjectController] Activando la UI de la vista de proyecto...");
	        if (registry == null || model == null || projectManager == null || projectListCoordinator == null) {
	            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
	            return;
	        }
	
	        // 1. Lógica para poblar las JList (se queda igual)
	        ListContext proyectoContext = model.getProyectoListContext();
	        JList<String> projectList = registry.get("list.proyecto.nombres");
	        JList<String> descartesList = registry.get("list.proyecto.descartes");
	
	        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
	        DefaultListModel<String> modeloSeleccion = new DefaultListModel<>();
	        for (Path p : imagenesMarcadas) {
	            modeloSeleccion.addElement(p.toString().replace("\\", "/"));
	        }
	        if (projectList != null) projectList.setModel(modeloSeleccion);
	        
	        poblarListaDescartes(); // Este método ya llena la lista de descartes
	
	        // 2. Lógica para determinar la CLAVE a seleccionar (lógica simplificada)
	        String claveParaMostrar = null;
	        String focoActual = proyectoContext.getNombreListaActiva();
	        
	        if ("descartes".equals(focoActual)) {
	            claveParaMostrar = proyectoContext.getDescartesListKey();
	        } else {
	            claveParaMostrar = proyectoContext.getSeleccionListKey();
	        }
	
	        // Si no hay clave guardada para el foco actual, o si la clave ya no es válida,
	        // intentamos seleccionar la primera de la lista con foco.
	        if (claveParaMostrar == null || !proyectoContext.getModeloLista().contains(claveParaMostrar)) {
	            if ("descartes".equals(focoActual) && descartesList.getModel().getSize() > 0) {
	                claveParaMostrar = descartesList.getModel().getElementAt(0);
	            } else if (projectList.getModel().getSize() > 0) {
	                // Si el foco no es descartes, o si descartes está vacía, intentamos con selección.
	                claveParaMostrar = projectList.getModel().getElementAt(0);
	                proyectoContext.setNombreListaActiva("seleccion"); // Corregimos el foco si es necesario
	            }
	        }
	        
	        // 3. ¡LA LLAMADA CLAVE!
	        // Le pasamos la clave a nuestro nuevo método del coordinador. Él se encarga del resto.
	        projectListCoordinator.seleccionarImagenPorClave(claveParaMostrar);
	
	        // 4. Lógica de UI que se ejecuta después
	        SwingUtilities.invokeLater(() -> {
	            actualizarAparienciaListasPorFoco();
	            
	            final JSplitPane leftSplit = registry.get("splitpane.proyecto.left");
	            if (leftSplit != null) leftSplit.setDividerLocation(0.55);
	            
	            final JSplitPane mainSplit = registry.get("splitpane.proyecto.main");
	            if (mainSplit != null) mainSplit.setDividerLocation(0.25);
	
	            System.out.println("  [ProjectController] UI de la vista de proyecto activada y restaurada.");
	        });
	
	    } // --- Fin del método activarVistaProyecto ---
	    
	    
	    @Override
	    public void navegarSiguiente() {
	        if (projectListCoordinator != null) {
	            projectListCoordinator.seleccionarSiguiente();
	        }
	    } 
	    
	    @Override
	    public void navegarAnterior() {
	        if (projectListCoordinator != null) projectListCoordinator.seleccionarAnterior();
	    }
	
	    @Override
	    public void navegarPrimero() {
	        if (projectListCoordinator != null) projectListCoordinator.seleccionarPrimero();
	    }
	
	    @Override
	    public void navegarUltimo() {
	        if (projectListCoordinator != null) projectListCoordinator.seleccionarUltimo();
	    }
	
	    @Override
	    public void navegarBloqueSiguiente() {
	        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueSiguiente();
	    }
	
	    @Override
	    public void navegarBloqueAnterior() {
	        if (projectListCoordinator != null) projectListCoordinator.seleccionarBloqueAnterior();
	    }
	
	    @Override
	    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
	        if (zoomManager != null) {
	            zoomManager.aplicarZoomConRueda(e);
	            if (controllerRef != null) {
	                controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
	            }
	        }
	    } // --- Fin del método aplicarZoomConRueda ---
	
	    @Override
	    public void aplicarPan(int deltaX, int deltaY) {
	        if (zoomManager != null) {
	            zoomManager.aplicarPan(deltaX, deltaY);
	        }
	    } // --- Fin del método aplicarPan ---
	
	    @Override
	    public void iniciarPaneo(java.awt.event.MouseEvent e) {
	        if (zoomManager != null && model.isZoomHabilitado()) {
	            zoomManager.iniciarPaneo(e);
	        }
	    } // --- Fin del método iniciarPaneo ---
	
	    @Override
	    public void continuarPaneo(java.awt.event.MouseEvent e) {
	        if (zoomManager != null && model.isZoomHabilitado()) {
	            zoomManager.continuarPaneo(e);
	        }
	    } // --- Fin del método continuarPaneo ---
	    
	    public void poblarListaDescartes() {
	        if (registry == null || projectManager == null) {
	            System.err.println("WARN [poblarListaDescartes]: Registry o ProjectManager nulos.");
	            return;
	        }
	        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
	        if (listaDescartesUI == null) {
	            System.err.println("WARN [poblarListaDescartes]: JList 'list.proyecto.descartes' no encontrada en el registro.");
	            return;
	        }
	        List<Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
	        DefaultListModel<String> modeloDescartes = new DefaultListModel<>();
	        for (Path rutaAbsoluta : imagenesDescartadas) {
	            String clave = rutaAbsoluta.toString().replace("\\", "/");
	            modeloDescartes.addElement(clave);
	        }
	        listaDescartesUI.setModel(modeloDescartes);
	        System.out.println("  [ProjectController] Lista de descartes actualizada en la UI. Total: " + modeloDescartes.getSize());
	        javax.swing.JTabbedPane herramientasTabbedPane = registry.get("tabbedpane.proyecto.herramientas");
	        if (herramientasTabbedPane != null) {
	            int tabCount = herramientasTabbedPane.getTabCount();
	            for (int i = 0; i < tabCount; i++) {
	                String tituloActual = herramientasTabbedPane.getTitleAt(i);
	                if ("Descartes".equals(tituloActual) || tituloActual.startsWith("Descartes:")) {
	                    herramientasTabbedPane.setTitleAt(i, "Descartes: " + modeloDescartes.getSize());
	                    break;
	                }
	            }
	        }
	    } // --- Fin del método poblarListaDescartes ---
	
	    public void moverSeleccionActualADescartes() {
	        if (model == null || projectManager == null) return;
	        String claveSeleccionada = model.getSelectedImageKey();
	        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
	            System.out.println("  [ProjectController] No hay imagen seleccionada para mover a descartes.");
	            return;
	        }
	        Path rutaAbsoluta = model.getProyectoListContext().getRutaCompleta(claveSeleccionada);
	        if (rutaAbsoluta != null) {
	            projectManager.moverAdescartes(rutaAbsoluta);
	            refrescarListasDeProyecto();
	        }
	    } // --- Fin del método moverSeleccionActualADescartes ---
	
	    public void restaurarDesdeDescartes() {
	        if (registry == null || projectManager == null) return;
	        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
	        if (listaDescartesUI == null) return;
	        String claveSeleccionada = listaDescartesUI.getSelectedValue();
	        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
	            System.out.println("  [ProjectController] No hay imagen seleccionada en descartes para restaurar.");
	            return;
	        }
	        Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
	        projectManager.restaurarDeDescartes(rutaAbsoluta);
	        refrescarListasDeProyecto();
	    } // --- Fin del método restaurarDesdeDescartes ---
	
	    private void refrescarListasDeProyecto() {
	        System.out.println("  [ProjectController] Refrescando ambas listas del proyecto...");
	        prepararDatosProyecto(); 
	        activarVistaProyecto(); 
	    } // --- Fin del método refrescarListasDeProyecto ---
	    
	    public void solicitarPreparacionColaExportacion() {
	        if (projectManager == null || exportQueueManager == null || registry == null) {
	            System.err.println("ERROR [solicitarPreparacionColaExportacion]: Dependencias nulas.");
	            return;
	        }
	        List<Path> seleccionActual = projectManager.getImagenesMarcadas();
	        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual);
	        
	        // --- INICIO DE CAMBIO ---
	        JTable tablaUI = getTablaExportacionDesdeRegistro(); // Usamos el método refactorizado
	        if (tablaUI != null && tablaUI.getModel() instanceof ExportTableModel) {
	            ((ExportTableModel) tablaUI.getModel()).setCola(exportQueueManager.getColaDeExportacion());
	            System.out.println("[ProjectController] Modelo de tabla de exportación actualizado.");
	        } else {
	            System.err.println("WARN [ProjectController]: No se pudo obtener la tabla de exportación o su modelo no es ExportTableModel.");
	        }
	        // --- FIN DE CAMBIO ---
	        
	        actualizarEstadoExportacionUI();
	    } // --- Fin del método solicitarPreparacionColaExportacion ---
	    
	    public void solicitarSeleccionCarpetaDestino() {
	        if (registry == null || view == null) {
	            System.err.println("ERROR [solicitarSeleccionCarpetaDestino]: Registry o View nulos.");
	            return;
	        }
	        JFileChooser fileChooser = new JFileChooser();
	        fileChooser.setDialogTitle("Seleccionar Carpeta de Destino para la Exportación");
	        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        fileChooser.setAcceptAllFileFilterUsed(false);
	        int resultado = fileChooser.showOpenDialog(view);
	        if (resultado == JFileChooser.APPROVE_OPTION) {
	            Path carpetaSeleccionada = fileChooser.getSelectedFile().toPath();
	            System.out.println("  [ProjectController] Carpeta de destino seleccionada: " + carpetaSeleccionada);
	            JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
	            if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
	                vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
	                exportPanel.setRutaDestino(carpetaSeleccionada.toString());
	            }
	        } else {
	            System.out.println("  [ProjectController] Selección de carpeta de destino cancelada por el usuario.");
	        }
	        actualizarEstadoExportacionUI();
	    } // --- Fin del método solicitarSeleccionCarpetaDestino ---
	    
	    public void onExportItemManuallyAssigned(modelo.proyecto.ExportItem itemModificado) {
	        System.out.println("  [ProjectController] Archivo asignado manualmente para: " + itemModificado.getRutaImagen().getFileName());
	        actualizarEstadoExportacionUI();
	    } // --- Fin del método onExportItemManuallyAssigned ---
	    
	    public void actualizarEstadoExportacionUI() {
	        if (registry == null || exportQueueManager == null) return;
	        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
	        if (exportPanel == null) return;
	        java.util.List<modelo.proyecto.ExportItem> colaCompleta = exportQueueManager.getColaDeExportacion();
	        List<modelo.proyecto.ExportItem> itemsSeleccionadosParaExportar = colaCompleta.stream().filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar).collect(Collectors.toList());
	        boolean todosLosSeleccionadosEstanListos = itemsSeleccionadosParaExportar.stream().allMatch(item ->
	            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
	            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
	            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO
	        );
	        String rutaDestino = exportPanel.getRutaDestino();
	        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank() && !rutaDestino.equals("Seleccione una carpeta de destino...");
	        boolean puedeExportar = carpetaOk && !itemsSeleccionadosParaExportar.isEmpty() && todosLosSeleccionadosEstanListos;
	        String mensaje = itemsSeleccionadosParaExportar.size() + " de " + colaCompleta.size() + " archivos a exportar.";
	        exportPanel.actualizarEstadoControles(puedeExportar, mensaje);
	        boolean resaltarDestino = !colaCompleta.isEmpty() && !carpetaOk;
	        exportPanel.resaltarRutaDestino(resaltarDestino);
	        System.out.println("  [ProjectController] Estado de exportación UI actualizado. Puede exportar: " + puedeExportar);
	    } // --- Fin del método actualizarEstadoExportacionUI ---
	    
	    public void solicitarInicioExportacion() {
	        if (exportQueueManager == null || registry == null || view == null) {
	            System.err.println("ERROR [solicitarInicioExportacion]: Dependencias nulas.");
	            return;
	        }
	        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
	        Path carpetaDestino = java.nio.file.Paths.get(exportPanel.getRutaDestino());
	        List<modelo.proyecto.ExportItem> colaParaCopiar = exportQueueManager.getColaDeExportacion().stream()
	            .filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar)
	            .filter(item -> 
	                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
	                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
	                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO)
	            .collect(Collectors.toList());
	        if (colaParaCopiar.isEmpty()) {
	            JOptionPane.showMessageDialog(view, "No hay archivos válidos seleccionados en la cola para exportar.", "Exportación Vacía", JOptionPane.WARNING_MESSAGE);
	            return;
	        }
	        ExportProgressDialog dialogo = new ExportProgressDialog(view);
	        ExportWorker worker = new ExportWorker(colaParaCopiar, carpetaDestino, dialogo);
	        worker.addPropertyChangeListener(evt -> {
	            if ("progress".equals(evt.getPropertyName())) {
	                dialogo.setProgress((Integer) evt.getNewValue());
	            }
	        });
	        worker.execute();
	        dialogo.setVisible(true);
	    } // --- Fin del método solicitarInicioExportacion ---
	    
	    public void solicitarAbrirUbicacionImagen() {
	        if (exportQueueManager == null || registry == null) return;
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
	        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
	        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
	        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
	        if (itemSeleccionado != null) {
	            try {
	                DesktopUtils.openAndSelectFile(itemSeleccionado.getRutaImagen());
	            } catch (Exception e) {
	                System.err.println("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
	                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	    } // --- Fin del método solicitarAbrirUbicacionImagen ---
	    
	    public void solicitarAlternarIgnorarComprimido() {
	        if (registry == null) return;
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
	        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
	        int filaSeleccionada = tablaExportacion.getSelectedRow();
	        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
	        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
	        if (item != null) {
	            if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.NO_ENCONTRADO) {
	                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
	            } else if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
	                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.NO_ENCONTRADO);
	            }
	            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
	            actualizarEstadoExportacionUI();
	        }
	    } // --- Fin del método solicitarAlternarIgnorarComprimido ---
	    
	    public void solicitarAsignacionManual() {
	        if (registry == null) return;
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
	        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
	        int filaSeleccionada = tablaExportacion.getSelectedRow();
	        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
	        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
	        if (item == null) return;
	        JFileChooser fileChooser = new JFileChooser();
	        Path carpetaImagen = item.getRutaImagen().getParent();
	        if (carpetaImagen != null && Files.isDirectory(carpetaImagen)) {
	            fileChooser.setCurrentDirectory(carpetaImagen.toFile());
	        }
	        fileChooser.setDialogTitle("Localizar Archivo para " + item.getRutaImagen().getFileName());
	        int result = fileChooser.showOpenDialog(view);
	        if (result == JFileChooser.APPROVE_OPTION) {
	            Path selectedPath = fileChooser.getSelectedFile().toPath();
	            item.setRutaArchivoComprimido(selectedPath);
	            item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.ASIGNADO_MANUAL);
	            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
	            actualizarEstadoExportacionUI();
	        }
	    } // --- Fin del método solicitarAsignacionManual ---
	
	    public void solicitarQuitarDeLaCola() {
	        if (exportQueueManager == null || registry == null) return;
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
	        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
	        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
	        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
	        if (itemSeleccionado != null) {
	            exportQueueManager.getColaDeExportacion().remove(itemSeleccionado);
	            modelTabla.setCola(exportQueueManager.getColaDeExportacion());
	            actualizarEstadoExportacionUI();
	        }
	    } // --- Fin del método solicitarQuitarDeLaCola ---
	
	    public void solicitudAlternarMarcaImagen() {
	        System.out.println("  [ProjectController] Solicitud para mover selección a descartes (desde botón 'Marcar')...");
	        this.moverSeleccionActualADescartes();
	    } // --- Fin del método solicitudAlternarMarcaImagen ---
	    
	    public void solicitarEliminacionPermanente() {
	        if (registry == null || projectManager == null || view == null) {
	            System.err.println("WARN [solicitarEliminacionPermanente]: Dependencias nulas.");
	            return;
	        }
	        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
	        if (listaDescartesUI == null) {
	            System.err.println("WARN [solicitarEliminacionPermanente]: JList 'list.proyecto.descartes' no encontrada.");
	            return;
	        }
	        String claveSeleccionada = listaDescartesUI.getSelectedValue();
	        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
	            System.out.println("  [ProjectController] No hay imagen seleccionada en descartes para eliminar.");
	            return;
	        }
	        int confirm = JOptionPane.showConfirmDialog(view, "¿Seguro que quieres eliminar esta imagen del proyecto?\n(No se borrará el archivo del disco)", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	        if (confirm == JOptionPane.YES_OPTION) {
	            Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
	            projectManager.eliminarDeProyecto(rutaAbsoluta);
	            refrescarListasDeProyecto();
	        }
	    } // --- Fin del método solicitarEliminacionPermanente ---
	    
	        
	    private void actualizarAparienciaListasPorFoco() {
	        if (registry == null || model == null || controllerRef.getThemeManager() == null) return;
	        JList<String> projectList = registry.get("list.proyecto.nombres");
	        JList<String> descartesList = registry.get("list.proyecto.descartes");
	        if (projectList == null || descartesList == null) return;
	        String focoActivo = model.getProyectoListContext().getNombreListaActiva();
	        vista.theme.Tema tema = controllerRef.getThemeManager().getTemaActual();
	        java.awt.Color colorFondoActivo = tema.colorFondoSecundario();
	        java.awt.Color colorFondoInactivo = tema.colorBorde(); 
	        java.awt.Color colorTextoActivo = tema.colorTextoPrimario();
	        java.awt.Color colorTextoInactivo = tema.colorTextoSecundario().brighter();
	        if ("seleccion".equals(focoActivo)) {
	            projectList.setBackground(colorFondoActivo);
	            projectList.setForeground(colorTextoActivo);
	            descartesList.setBackground(colorFondoInactivo);
	            descartesList.setForeground(colorTextoInactivo);
	        } else { 
	            projectList.setBackground(colorFondoInactivo);
	            projectList.setForeground(colorTextoInactivo);
	            descartesList.setBackground(colorFondoActivo);
	            descartesList.setForeground(colorTextoActivo);
	        }
	    } // --- Fin del método actualizarAparienciaListasPorFoco ---
	    
	
	    public void solicitarRelocalizacionImagen() {
	        if (registry == null || exportQueueManager == null || view == null) return;
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
	        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
	        int filaSeleccionada = tablaExportacion.getSelectedRow();
	        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
	        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
	        if (item == null || item.getEstadoArchivoComprimido() != modelo.proyecto.ExportStatus.IMAGEN_NO_ENCONTRADA) {
	            return;
	        }
	        JFileChooser fileChooser = new JFileChooser();
	        fileChooser.setDialogTitle("Relocalizar Imagen: " + item.getRutaImagen().getFileName());
	        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
	            "Imágenes (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp");
	        fileChooser.setFileFilter(filter);
	        int result = fileChooser.showOpenDialog(view);
	        if (result == JFileChooser.APPROVE_OPTION) {
	            Path nuevaRuta = fileChooser.getSelectedFile().toPath();
	            ExportItem newItem = new ExportItem(nuevaRuta);
	            exportQueueManager.buscarArchivoComprimidoAsociado(newItem); // <-- MÉTODO INACCESIBLE
	            exportQueueManager.getColaDeExportacion().set(filaSeleccionada, newItem);
	            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
	            actualizarEstadoExportacionUI();
	        }
	    } // --- Fin del método solicitarRelocalizacionImagen ---
	    
	    
	    /**
	     * Mueve el selector de la tabla de exportación arriba o abajo en respuesta a un evento de rueda del ratón.
	     * Actualiza la selección y asegura que la fila seleccionada sea visible.
	     * Es invocado por GeneralController.
	     *
	     * @param e El evento de la rueda del ratón.
	     */
	    public void navegarTablaExportacionConRueda(java.awt.event.MouseWheelEvent e) {
	        if (registry == null || model == null) {
	            System.err.println("WARN [navegarTablaExportacionConRueda]: Registry o Modelo nulos.");
	            return;
	        }
	
	        JTable tablaExportacion = getTablaExportacionDesdeRegistro(); // Reutilizamos tu método para obtener la tabla
	        if (tablaExportacion == null || tablaExportacion.getModel().getRowCount() == 0) {
	            System.out.println("  [navegarTablaExportacionConRueda] Tabla de exportación vacía o no encontrada. No se puede navegar.");
	            return;
	        }
	
	        int currentRow = tablaExportacion.getSelectedRow();
	        int newRow;
	        int totalRows = tablaExportacion.getModel().getRowCount();
	
	        // Si no hay nada seleccionado, empezamos desde el principio o el final.
	        if (currentRow == -1) {
	            newRow = (e.getWheelRotation() < 0) ? 0 : totalRows - 1;
	        } else {
	            // Calcular el nuevo índice basado en la dirección de la rueda.
	            newRow = currentRow + e.getWheelRotation();
	            
	            // Aplicar límites: asegurar que el índice no se salga del rango.
	            // Para la navegación con rueda, generalmente no hay "wrap-around".
	            newRow = Math.max(0, Math.min(newRow, totalRows - 1));
	        }
	
	        // Solo actualizar si el índice realmente cambia.
	        if (newRow != currentRow) {
	            tablaExportacion.setRowSelectionInterval(newRow, newRow);
	            // Hacer que la fila seleccionada sea visible.
	            tablaExportacion.scrollRectToVisible(tablaExportacion.getCellRect(newRow, 0, true));
	            System.out.println("  [navegarTablaExportacionConRueda] Selector movido a la fila: " + newRow);
	        } else {
	            System.out.println("  [navegarTablaExportacionConRueda] Selector no cambió. Fila actual: " + currentRow);
	        }
	    }// --- Fin del nuevo método navegarTablaExportacionConRueda ---
	    
	    
	    /**
	     * Muestra una imagen específica en el visor principal del modo proyecto.
	     * Este método es invocado cuando se selecciona un item en la tabla de exportación.
	     * No cambia el foco de las listas de seleccion/descartes, solo actualiza la imagen del visor.
	     *
	     * @param rutaImagen El Path absoluto de la imagen a mostrar.
	     */
	    public void mostrarImagenDeExportacion(Path rutaImagen) {
	        System.out.println("[ProjectController] Solicitud para mostrar imagen de exportación: " + rutaImagen);
	        
	        if (model == null || controllerRef == null) {
	            System.err.println("ERROR [mostrarImagenDeExportacion]: Modelo o VisorController de referencia nulos.");
	            return;
	        }
	        if (rutaImagen == null) {
	            System.out.println("WARN [mostrarImagenDeExportacion]: Ruta de imagen nula. Limpiando visor principal.");
	            controllerRef.actualizarImagenPrincipal(-1); 
	            return;
	        }
	
	        // Convertir Path a la clave String que el modelo usa para la lista de proyectos.
	        String claveImagen = rutaImagen.toString().replace("\\", "/");
	        
	        int indiceEnProyectoContext = model.getProyectoListContext().getModeloLista().indexOf(claveImagen);
	
	        if (indiceEnProyectoContext != -1) {
	            // La imagen existe en nuestro modelo unificado de proyecto.
	            // Establecemos esta imagen como la seleccionada en el modelo del proyecto
	            // Y luego le decimos al VisorController que la cargue por su índice.
	            // La actualización del model.selectedImageKey es a nivel interno del modelo
	            // y es importante para que el VisorController sepa qué imagen cargar.
	            model.getProyectoListContext().setSelectedImageKey(claveImagen);
	            System.out.println("  -> Imagen encontrada en el contexto del proyecto (índice " + indiceEnProyectoContext + "). Cargando...");
	            controllerRef.actualizarImagenPrincipal(indiceEnProyectoContext);
	
	            // --- INICIO DE LA MODIFICACIÓN ---
	            // Se ELIMINA la lógica de SwingUtilities.invokeLater para manipular la selección
	            // de las JList de seleccion y descartes. Esto asegura que la selección de la tabla
	            // de exportación NO cambie la selección visible en las listas de proyecto.
	            // --- FIN DE LA MODIFICACIÓN ---
	
	        } else {
	            // La imagen no está en ninguna de las listas actuales del proyecto (ej., asignada manualmente).
	            // La cargamos directamente usando la referencia que ProjectController tiene a VisorController.
	            System.out.println("  -> Imagen NO encontrada en el contexto del proyecto. Cargando directamente por Path...");
	            controllerRef.actualizarImagenPrincipalPorPath(rutaImagen, claveImagen);
	            
	            // Limpiar selección de listas si la imagen no proviene de ellas y no queremos que aparezca seleccionada.
	            // Este bloque SÍ permanece, ya que es para asegurar que si se carga algo externo,
	            // las listas del proyecto no conserven una selección que no corresponde a la imagen mostrada.
	            javax.swing.SwingUtilities.invokeLater(() -> {
	                JList<String> listaSeleccion = registry.get("list.proyecto.nombres");
	                JList<String> listaDescartes = registry.get("list.proyecto.descartes");
	                if (listaSeleccion != null) listaSeleccion.clearSelection();
	                if (listaDescartes != null) listaDescartes.clearSelection();
	            });
	        }
	    }// --- Fin del nuevo método mostrarImagenDeExportacion ---
	    
	    
	// *********************************************************************************************************
	// *************************************************************************************** GETTERS Y SETTERS    
	// *********************************************************************************************************    
	    
	    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
	    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
	    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
	    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
	    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); }
	    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
	    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
	    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
	    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }
	    public VisorController getController() {return this.controllerRef;}
	    
	    /**
	     * Obtiene la JTable de exportación a través del registro de componentes.
	     * Accede al ExportPanel registrado y luego usa su método getTablaExportacion().
	     * @return La JTable de la cola de exportación, o null si no se encuentra.
	     */
	    public JTable getTablaExportacionDesdeRegistro() {
	        if (registry == null) return null;
	        vista.panels.export.ExportPanel exportPanel = registry.get("panel.proyecto.herramientas.exportar");
	        if (exportPanel != null) {
	            // --- CAMBIO: Accedemos directamente al getter de la tabla ---
	            return exportPanel.getTablaExportacion(); 
	        }
	        System.err.println("WARN [ProjectController]: No se pudo encontrar 'tablaExportacion' a través del registro.");
	        return null;
	    } // --- Fin del método getTablaExportacionDesdeRegistro ---
	    
	    
	    public void setProjectListCoordinator(ProjectListCoordinator coordinator) {
	        this.projectListCoordinator = coordinator;
	    }
	    
	} // --- FIN de la clase ProjectController ---