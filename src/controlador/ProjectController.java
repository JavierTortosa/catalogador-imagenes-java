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
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

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
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.dialogos.ExportProgressDialog;
import vista.panels.export.ExportTableModel;
import vista.theme.Tema;

public class ProjectController implements IModoController {

    private IProjectManager projectManager;
    private ComponentRegistry registry;
    private IZoomManager zoomManager;
    private VisorView view;
    private VisorModel model;
    private VisorController controllerRef;
    private ExportQueueManager exportQueueManager;
    private IViewManager viewManager;
    private IListCoordinator listCoordinator; 
    private Map<String, Action> actionMap;
    

    public ProjectController() {
        System.out.println("[ProjectController] Instancia creada.");
        this.exportQueueManager = new ExportQueueManager();
    } // --- Fin del método ProjectController (constructor) ---

    
    public VisorController getController() {
        return this.controllerRef;
    } // --- Fin del método getController ---

    
    void configurarListeners() {
        if (registry == null || model == null) {
            System.err.println("ERROR [ProjectController]: Dependencias nulas (registry, model).");
            return;
        }

        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");

        // --- Listener para el cambio de FOCO (clic del ratón) ---
        MouseAdapter listMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JList<?> sourceList = (JList<?>) e.getSource();
                String nombreListaClicada = (sourceList == projectList) ? "seleccion" : "descartes";
                
                // Si hacemos clic en una lista que no es la activa, cambiamos el foco.
                if (!nombreListaClicada.equals(model.getProyectoListContext().getNombreListaActiva())) {
                    cambiarFocoListaActiva(nombreListaClicada);
                }
            }
        };

        // --- Listener para la SELECCIÓN en la lista de "Selección Actual" ---
        if (projectList != null) {
            projectList.addMouseListener(listMouseAdapter);
            projectList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return; // Ignorar eventos intermedios
                
                // Solo reaccionar si esta lista TIENE el foco
                if ("seleccion".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    String selectedKey = projectList.getSelectedValue();
                    model.getProyectoListContext().setSeleccionListKey(selectedKey); // Actualizar mochila
                    seleccionarImagenEnListaActiva(selectedKey); // Actualizar visor
                }
            });
        }

        // --- Listener para la SELECCIÓN en la lista de "Descartes" ---
        if (descartesList != null) {
            descartesList.addMouseListener(listMouseAdapter);
            descartesList.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return; // Ignorar eventos intermedios

                // Solo reaccionar si esta lista TIENE el foco
                if ("descartes".equals(model.getProyectoListContext().getNombreListaActiva())) {
                    String selectedKey = descartesList.getSelectedValue();
                    model.getProyectoListContext().setDescartesListKey(selectedKey); // Actualizar mochila
                    seleccionarImagenEnListaActiva(selectedKey); // Actualizar visor
                }
            });
        }
    } // --- Fin del método configurarListeners ---

    private void seleccionarImagenEnListaActiva(String clave) {
        if (clave == null || clave.equals(model.getSelectedImageKey())) {
            return;
        }
        
        model.setSelectedImageKey(clave);
        int indiceMaestro = model.getProyectoListContext().getModeloLista().indexOf(clave);
        if (indiceMaestro >= 0) {
            controllerRef.actualizarImagenPrincipal(indiceMaestro);
        }
    } // --- Fin del método seleccionarImagenEnListaActiva ---

    
    private void cambiarFocoListaActiva(String nuevoFoco) {
        System.out.println("  [ProjectController] Cambiando foco a lista: " + nuevoFoco);
        model.getProyectoListContext().setNombreListaActiva(nuevoFoco);
        
        String claveARestaurar;
        JList<String> listaQueGanaFoco;
        JList<String> listaQuePierdeFoco;

        if ("seleccion".equals(nuevoFoco)) {
            claveARestaurar = model.getProyectoListContext().getSeleccionListKey();
            listaQueGanaFoco = registry.get("list.proyecto.nombres");
            listaQuePierdeFoco = registry.get("list.proyecto.descartes");
        } else {
            claveARestaurar = model.getProyectoListContext().getDescartesListKey();
            listaQueGanaFoco = registry.get("list.proyecto.descartes");
            listaQuePierdeFoco = registry.get("list.proyecto.nombres");
        }
        
        if (listaQuePierdeFoco != null) {
            listaQuePierdeFoco.clearSelection();
        }

        if (claveARestaurar == null && listaQueGanaFoco != null && listaQueGanaFoco.getModel().getSize() > 0) {
            claveARestaurar = listaQueGanaFoco.getModel().getElementAt(0);
        }
        
        seleccionarImagenEnListaActiva(claveARestaurar);
        
        // --- LÓGICA DE SCROLL Y SELECCIÓN MÁS CONTROLADA ---
        final JList<String> finalListaActiva = listaQueGanaFoco;
        final String finalClaveActiva = claveARestaurar;

        javax.swing.SwingUtilities.invokeLater(() -> {
            if (finalListaActiva != null && finalClaveActiva != null) {
                // 1. Establecer la selección SIN disparar el scroll automático
                finalListaActiva.setSelectedValue(finalClaveActiva, false); // <--- CAMBIO a 'false'

                // 2. Calcular el rectángulo del item seleccionado
                int index = finalListaActiva.getSelectedIndex();
                if (index != -1) {
                    java.awt.Rectangle rect = finalListaActiva.getCellBounds(index, index);
                    if (rect != null) {
                        // 3. Mover explícitamente el viewport del JScrollPane a ese rectángulo
                        finalListaActiva.scrollRectToVisible(rect);
                    }
                }
            }
            actualizarAparienciaListasPorFoco();
        });
    } // --- Fin del método cambiarFocoListaActiva ---
    
    
    /**
     * Sincroniza la apariencia de los paneles de las listas de proyecto
     * cambiando el color del título de sus bordes para indicar cuál tiene el foco.
     * Este método actúa como el actualizador para nuestros "componentes tontos".
     */
    private void actualizarBordesDeFoco() {
        if (registry == null || model == null || controllerRef.getThemeManager() == null) {
            System.err.println("WARN [actualizarBordesDeFoco]: Dependencias nulas. No se puede actualizar la apariencia.");
            return;
        }

        JPanel panelSeleccion = registry.get("panel.proyecto.listas.container");
        JPanel panelHerramientas = registry.get("panel.proyecto.herramientas.container");
        if (panelSeleccion == null || panelHerramientas == null) {
            System.err.println("WARN [actualizarBordesDeFoco]: No se encontraron los paneles de proyecto en el registro.");
            return;
        }

        // Consultar las fuentes de verdad: el foco lógico y el tema actual
        String focoActivo = model.getProyectoListContext().getNombreListaActiva();
        Tema tema = controllerRef.getThemeManager().getTemaActual();
        
        java.awt.Color colorBordeActivo = tema.colorBordeSeleccionActiva();
        java.awt.Color colorBordeInactivo = tema.colorBordeTitulo();

        // Actualizar el borde del panel de la lista de Selección
        if (panelSeleccion.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder borde = (javax.swing.border.TitledBorder) panelSeleccion.getBorder();
            borde.setTitleColor("seleccion".equals(focoActivo) ? colorBordeActivo : colorBordeInactivo);
            panelSeleccion.repaint(); // Forzar repintado del borde
        }

        // Actualizar el borde del panel que contiene la pestaña de Descartes
        if (panelHerramientas.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder borde = (javax.swing.border.TitledBorder) panelHerramientas.getBorder();
            borde.setTitleColor("descartes".equals(focoActivo) ? colorBordeActivo : colorBordeInactivo);
            panelHerramientas.repaint(); // Forzar repintado del borde
        }
    } // --- Fin del método actualizarBordesDeFoco ---
    
    
    /**
     * Usa propiedades de cliente de FlatLaf para cambiar la apariencia de las listas
     * y simular un estado de foco activo/inactivo.
     */
    private void sincronizarEstiloVisualListas() {
        if (registry == null || model == null) return;
        
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null) return;

        String focoActivo = model.getProyectoListContext().getNombreListaActiva();

        // Estilos de FlatLaf. 'null' restaura el estilo por defecto del Look and Feel.
        // '$... ' son variables de color del tema de FlatLaf.
        String estiloActivo = "background: null; foreground: null;"; 
        String estiloInactivo = "background: $Panel.background; foreground: $Label.disabledForeground;";

        // Aplicar el estilo correcto a cada lista basándose en el foco lógico
        if ("seleccion".equals(focoActivo)) {
            projectList.putClientProperty("FlatLaf.style", estiloActivo);
            descartesList.putClientProperty("FlatLaf.style", estiloInactivo);
        } else {
            projectList.putClientProperty("FlatLaf.style", estiloInactivo);
            descartesList.putClientProperty("FlatLaf.style", estiloActivo);
        }
    } // --- Fin del método sincronizarEstiloVisualListas ---
    
    
    /**
     * Sincroniza la apariencia de las JList de proyecto cambiando su color de fondo
     * para indicar cuál tiene el foco lógico.
     */
    private void actualizarAparienciaListasPorFoco() {
        if (registry == null || model == null || controllerRef.getThemeManager() == null) return;

        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList == null || descartesList == null) return;

        String focoActivo = model.getProyectoListContext().getNombreListaActiva();
        Tema tema = controllerRef.getThemeManager().getTemaActual();
        
        java.awt.Color colorFondoActivo = tema.colorFondoSecundario();
        java.awt.Color colorTextoActivo = tema.colorTextoPrimario();
        
        java.awt.Color colorFondoInactivo = tema.colorBorde(); //tema.colorFondoPrincipal();
        java.awt.Color colorTextoInactivo = tema.colorTextoSecundario().brighter();

        // Aplicar el color de fondo correcto a cada JList
        if ("seleccion".equals(focoActivo)) {
        	projectList.setBackground(colorFondoActivo);
            projectList.setForeground(colorTextoActivo);
            
            descartesList.setBackground(colorFondoInactivo);
            descartesList.setForeground(colorTextoInactivo);
            
        } else { // El foco está en "descartes"
        	projectList.setBackground(colorFondoInactivo);
            projectList.setForeground(colorTextoInactivo);
            
            descartesList.setBackground(colorFondoActivo);
            descartesList.setForeground(colorTextoActivo);
        }
    }// --- FIN DEL METODO actualizarAparienciaListasPorFoco ---

    
    
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
        if (registry == null || model == null || projectManager == null || controllerRef == null) {
            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
            return;
        }

        // --- 1. CARGAR ESTADO DESDE CONFIG A LA "MOCHILA" (SI ES LA PRIMERA VEZ) ---
        ListContext proyectoContext = model.getProyectoListContext();
        if (proyectoContext.getSeleccionListKey() == null && proyectoContext.getDescartesListKey() == null) {
            ConfigurationManager config = controllerRef.getConfigurationManager();
            String focoGuardado = config.getString(ConfigKeys.PROYECTOS_LISTA_ACTIVA, "seleccion");
            String seleccionGuardada = config.getString(ConfigKeys.PROYECTOS_ULTIMA_SELECCION_KEY, null);
            String descartesGuardado = config.getString(ConfigKeys.PROYECTOS_ULTIMA_DESCARTES_KEY, null);
            proyectoContext.setNombreListaActiva(focoGuardado);
            proyectoContext.setSeleccionListKey(seleccionGuardada);
            proyectoContext.setDescartesListKey(descartesGuardado);
        }
        
        // --- 2. POBLAR LOS MODELOS DE LAS LISTAS CON DATOS ---
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");

        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
        DefaultListModel<String> modeloSeleccion = new DefaultListModel<>();
        for (Path p : imagenesMarcadas) {
            modeloSeleccion.addElement(p.toString().replace("\\", "/"));
        }
        if (projectList != null) {
            projectList.setModel(modeloSeleccion);
        }
        
        poblarListaDescartes();

        // --- 3. ACTUALIZAR TÍTULO DEL PANEL ---
        JPanel panelListas = registry.get("panel.proyecto.listas.container");
        if (panelListas != null && panelListas.getBorder() instanceof javax.swing.border.TitledBorder) {
            ((javax.swing.border.TitledBorder) panelListas.getBorder()).setTitle("Selección Actual: " + modeloSeleccion.getSize());
            panelListas.repaint();
        }
        
        // --- 4. DETERMINAR IMAGEN A MOSTRAR Y FOCO FINAL (LÓGICA DE FALLBACK) ---
        String claveParaMostrar = null;
        String focoFinal = proyectoContext.getNombreListaActiva();

        if ("descartes".equals(focoFinal)) {
            claveParaMostrar = proyectoContext.getDescartesListKey();
        } else {
            claveParaMostrar = proyectoContext.getSeleccionListKey();
        }

        boolean claveValida = claveParaMostrar != null && proyectoContext.getModeloLista().contains(claveParaMostrar);
        
        if (!claveValida) {
            if (projectList != null && projectList.getModel().getSize() > 0) {
                claveParaMostrar = projectList.getModel().getElementAt(0);
                focoFinal = "seleccion";
            } else if (descartesList != null && descartesList.getModel().getSize() > 0) {
                claveParaMostrar = descartesList.getModel().getElementAt(0);
                focoFinal = "descartes";
            }
        }
        
        // --- 5. APLICAR ESTADO AL MODELO Y AL VISOR ---
        proyectoContext.setNombreListaActiva(focoFinal);
        seleccionarImagenEnListaActiva(claveParaMostrar);
        
        // --- 6. SINCRONIZAR LA UI VISUALMENTE ---
        if (projectList != null) projectList.setSelectedValue(proyectoContext.getSeleccionListKey(), true);
        if (descartesList != null) descartesList.setSelectedValue(proyectoContext.getDescartesListKey(), true);
        
        actualizarEstadoVisualDeListas();
        actualizarAparienciaListasPorFoco();

        // --- 7. ESTABLECER POSICIÓN INICIAL DE LOS DIVISORES ---
        final JSplitPane leftSplit = registry.get("splitpane.proyecto.left");
        if (leftSplit != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                leftSplit.setDividerLocation(0.60);
            });
        }
        
        final JSplitPane mainSplit = registry.get("splitpane.proyecto.main");
        if (mainSplit != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                mainSplit.setDividerLocation(0.25);
            });
        }
        
        System.out.println("  [ProjectController] UI de la vista de proyecto activada y restaurada.");
    } // --- Fin del método activarVistaProyecto ---
    
    
//    public void activarVistaProyecto() {
//        System.out.println("  [ProjectController] Activando la UI de la vista de proyecto...");
//        if (registry == null || model == null || projectManager == null || controllerRef == null) {
//            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
//            return;
//        }
//
//        ListContext proyectoContext = model.getProyectoListContext();
//        
//        if (proyectoContext.getSeleccionListKey() == null && proyectoContext.getDescartesListKey() == null) {
//            ConfigurationManager config = controllerRef.getConfigurationManager();
//            String focoGuardado = config.getString(ConfigKeys.PROYECTOS_LISTA_ACTIVA, "seleccion");
//            String seleccionGuardada = config.getString(ConfigKeys.PROYECTOS_ULTIMA_SELECCION_KEY, null);
//            String descartesGuardado = config.getString(ConfigKeys.PROYECTOS_ULTIMA_DESCARTES_KEY, null);
//            proyectoContext.setNombreListaActiva(focoGuardado);
//            proyectoContext.setSeleccionListKey(seleccionGuardada);
//            proyectoContext.setDescartesListKey(descartesGuardado);
//        }
//        
//        JList<String> projectList = registry.get("list.proyecto.nombres");
//        JList<String> descartesList = registry.get("list.proyecto.descartes");
//
//        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
//        DefaultListModel<String> modeloSeleccion = new DefaultListModel<>();
//        for (Path p : imagenesMarcadas) {
//            modeloSeleccion.addElement(p.toString().replace("\\", "/"));
//        }
//        if (projectList != null) projectList.setModel(modeloSeleccion);
//        
//        poblarListaDescartes();
//
//        JPanel panelListas = registry.get("panel.proyecto.listas.container");
//        if (panelListas != null && panelListas.getBorder() instanceof javax.swing.border.TitledBorder) {
//            ((javax.swing.border.TitledBorder) panelListas.getBorder()).setTitle("Selección Actual: " + modeloSeleccion.getSize());
//            panelListas.repaint();
//        }
//        
//        String claveParaMostrar = null;
//        String focoFinal = proyectoContext.getNombreListaActiva();
//
//        if ("descartes".equals(focoFinal)) {
//            claveParaMostrar = proyectoContext.getDescartesListKey();
//        } else {
//            claveParaMostrar = proyectoContext.getSeleccionListKey();
//        }
//
//        boolean claveValida = claveParaMostrar != null && proyectoContext.getModeloLista().contains(claveParaMostrar);
//        
//        if (!claveValida) {
//            if (projectList != null && projectList.getModel().getSize() > 0) {
//                claveParaMostrar = projectList.getModel().getElementAt(0);
//                focoFinal = "seleccion";
//            } else if (descartesList != null && descartesList.getModel().getSize() > 0) {
//                claveParaMostrar = descartesList.getModel().getElementAt(0);
//                focoFinal = "descartes";
//            }
//        }
//        
//        proyectoContext.setNombreListaActiva(focoFinal);
//        seleccionarImagenEnListaActiva(claveParaMostrar);
//        
//        if (projectList != null) projectList.setSelectedValue(proyectoContext.getSeleccionListKey(), true);
//        if (descartesList != null) descartesList.setSelectedValue(proyectoContext.getDescartesListKey(), true);
//        
//        actualizarEstadoVisualDeListas();
//        actualizarAparienciaListasPorFoco();
//
//        // --- LÓGICA PARA LA POSICIÓN INICIAL DEL DIVISOR ---
//        final JSplitPane leftSplit = registry.get("splitpane.proyecto.left");
//        if (leftSplit != null) {
//            // Usamos invokeLater para asegurar que se ejecuta después de que los componentes
//            // se hayan renderizado y tengan un tamaño válido.
//            javax.swing.SwingUtilities.invokeLater(() -> {
//                // Establece la posición del divisor al 70% del espacio para el panel superior.
//                leftSplit.setDividerLocation(0.7);
//            });
//        }
//        // --- FIN DE LA LÓGICA DEL DIVISOR ---
//        
//        System.out.println("  [ProjectController] UI de la vista de proyecto activada y restaurada.");
//    } // --- Fin del método activarVistaProyecto ---
    

    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); }
    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }

    @Override
    public void navegarSiguiente() {
        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
        if (listaParaNavegar != null) {
            int total = listaParaNavegar.getModel().getSize();
            if (total == 0) return;
            int idx = listaParaNavegar.getSelectedIndex();
            idx = idx + 1;
            if (idx >= total) {
                idx = model.isNavegacionCircularActivada() ? 0 : total - 1;
            }
            listaParaNavegar.setSelectedIndex(idx);
            listaParaNavegar.ensureIndexIsVisible(idx);
        }
    } // --- Fin del método navegarSiguiente ---

    @Override
    public void navegarAnterior() {
        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
        if (listaParaNavegar != null) {
            int total = listaParaNavegar.getModel().getSize();
            if (total == 0) return;
            int idx = listaParaNavegar.getSelectedIndex();
            if (idx == -1) idx = 0;
            idx = idx - 1;
            if (idx < 0) {
                idx = model.isNavegacionCircularActivada() ? total - 1 : 0;
            }
            listaParaNavegar.setSelectedIndex(idx);
            listaParaNavegar.ensureIndexIsVisible(idx);
        }
    } // --- Fin del método navegarAnterior ---

    @Override
    public void navegarPrimero() {
        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
        if (listaParaNavegar != null && listaParaNavegar.getModel().getSize() > 0) {
            listaParaNavegar.setSelectedIndex(0);
            listaParaNavegar.ensureIndexIsVisible(0);
        }
    } // --- Fin del método navegarPrimero ---

    @Override
    public void navegarUltimo() {
        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
        if (listaParaNavegar != null) {
            int ultimoIdx = listaParaNavegar.getModel().getSize() - 1;
            if (ultimoIdx >= 0) {
                listaParaNavegar.setSelectedIndex(ultimoIdx);
                listaParaNavegar.ensureIndexIsVisible(ultimoIdx);
            }
        }
    } // --- Fin del método navegarUltimo ---

    @Override
    public void navegarBloqueSiguiente() {
        JList<?> listaActiva = obtenerListaActivaDesdeModelo();
        if (listaActiva != null && listaActiva.getModel().getSize() > 0) {
            int indiceActual = listaActiva.getSelectedIndex();
            if (indiceActual == -1) indiceActual = 0;
            
            int nuevoIndice = Math.min(indiceActual + model.getSaltoDeBloque(), listaActiva.getModel().getSize() - 1);
            
            if (nuevoIndice != indiceActual) {
                listaActiva.setSelectedIndex(nuevoIndice);
                listaActiva.ensureIndexIsVisible(nuevoIndice);
            }
        }
    } // --- Fin del método navegarBloqueSiguiente ---

    @Override
    public void navegarBloqueAnterior() {
        JList<?> listaActiva = obtenerListaActivaDesdeModelo();
        if (listaActiva != null && listaActiva.getModel().getSize() > 0) {
            int indiceActual = listaActiva.getSelectedIndex();
            if (indiceActual == -1) indiceActual = 0;

            int nuevoIndice = Math.max(0, indiceActual - model.getSaltoDeBloque());
            
            if (nuevoIndice != indiceActual) {
                listaActiva.setSelectedIndex(nuevoIndice);
                listaActiva.ensureIndexIsVisible(nuevoIndice);
            }
        }
    } // --- Fin del método navegarBloqueAnterior ---

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
                if ("Descartes".equals(herramientasTabbedPane.getTitleAt(i)) || herramientasTabbedPane.getTitleAt(i).startsWith("Descartes:")) {
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
        poblarListaDescartes();
    } // --- Fin del método refrescarListasDeProyecto ---
    
    public void solicitarPreparacionColaExportacion() {
        if (projectManager == null || exportQueueManager == null || registry == null) {
            System.err.println("ERROR [solicitarPreparacionColaExportacion]: Dependencias nulas.");
            return;
        }
        List<Path> seleccionActual = projectManager.getImagenesMarcadas();
        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual);
        JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
        if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
            vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
            if (exportPanel.getComponentCount() > 1 && exportPanel.getComponent(1) instanceof javax.swing.JScrollPane) {
                javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) exportPanel.getComponent(1);
                JTable tablaUI = (JTable) scrollPane.getViewport().getView();
                if (tablaUI.getModel() instanceof ExportTableModel) {
                    ((ExportTableModel) tablaUI.getModel()).setCola(exportQueueManager.getColaDeExportacion());
                    System.out.println("[ProjectController] Modelo de tabla de exportación actualizado.");
                }
            }
        }
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
            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
        String rutaDestino = exportPanel.getRutaDestino();
        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank() && !rutaDestino.equals("Seleccione una carpeta de destino...");
        boolean puedeExportar = carpetaOk && !itemsSeleccionadosParaExportar.isEmpty() && todosLosSeleccionadosEstanListos;
        String mensaje = itemsSeleccionadosParaExportar.size() + " de " + colaCompleta.size() + " archivos seleccionados para exportar.";
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

    private JTable getTablaExportacionDesdeRegistro() {
        JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
        if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
            vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
            if (exportPanel.getComponent(1) instanceof javax.swing.JScrollPane) {
                javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) exportPanel.getComponent(1);
                return (JTable) scrollPane.getViewport().getView();
            }
        }
        return null;
    } // --- Fin del método getTablaExportacionDesdeRegistro ---
    
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
    
    private void actualizarEstadoVisualDeListas() {
        if (registry == null || model == null || model.getProyectoListContext() == null) {
            return;
        }
        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        JList<String> projectList = registry.get("list.proyecto.nombres");
        JList<String> descartesList = registry.get("list.proyecto.descartes");
        if (projectList != null && descartesList != null) {
            if ("seleccion".equals(nombreListaActiva)) {
                descartesList.clearSelection();
            } else {
                projectList.clearSelection();
            }
        }
    } // --- Fin del método actualizarEstadoVisualDeListas ---
    
    private JList<?> obtenerListaActivaDesdeModelo() {
        if (registry == null || model == null || model.getProyectoListContext() == null) {
            return null;
        }
        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
        if ("descartes".equals(nombreListaActiva)) {
            return registry.get("list.proyecto.descartes");
        } else {
            return registry.get("list.proyecto.nombres");
        }
    } // --- Fin del método obtenerListaActivaDesdeModelo ---
    
} // --- FIN de la clase ProjectController ---

//package controlador;
//
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//import javax.swing.Action;
//import javax.swing.DefaultListModel;
//import javax.swing.JFileChooser;
//import javax.swing.JList;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JTable;
//
//import controlador.interfaces.IModoController;
//import controlador.managers.ExportQueueManager;
//import controlador.managers.interfaces.IListCoordinator;
//import controlador.managers.interfaces.IProjectManager;
//import controlador.managers.interfaces.IViewManager;
//import controlador.managers.interfaces.IZoomManager;
//import controlador.utils.ComponentRegistry;
//import controlador.utils.DesktopUtils;
//import controlador.worker.ExportWorker;
//import modelo.ListContext;
//import modelo.VisorModel;
//import vista.VisorView;
//import vista.dialogos.ExportProgressDialog;
//import vista.panels.export.ExportTableModel;
//
//public class ProjectController implements IModoController { // <-- MODIFICADO: implementa la interfaz
//
//    private IProjectManager projectManager;
//    private ComponentRegistry registry;
//    private IZoomManager zoomManager;
//    private VisorView view;
//    private VisorModel model;
//    private VisorController controllerRef;
//    private ExportQueueManager exportQueueManager;
//    private IViewManager viewManager;
//    private IListCoordinator listCoordinator; 
//    private Map<String, Action> actionMap;
//    
//
//    public ProjectController() {
//        System.out.println("[ProjectController] Instancia creada.");
//        this.exportQueueManager = new ExportQueueManager();
//    } // --- Fin del método ProjectController (constructor) ---
//
//    
//    public VisorController getController() {
//        return this.controllerRef;
//    } // --- Fin del método getController ---
//
//    void configurarListeners() {
//        if (registry == null || model == null || controllerRef == null) {
//            System.err.println("ERROR [ProjectController]: Dependencias nulas (registry, model o controllerRef).");
//            return;
//        }
//
//        JList<String> projectList = registry.get("list.proyecto.nombres");
//        JList<String> descartesList = registry.get("list.proyecto.descartes");
//
//        MouseAdapter listMouseAdapter = new MouseAdapter() {
//            @Override
//            public void mousePressed(MouseEvent e) {
//                JList<?> sourceList = (JList<?>) e.getSource();
//                String nombreListaClicada = (sourceList == projectList) ? "seleccion" : "descartes";
//                model.getProyectoListContext().setNombreListaActiva(nombreListaClicada);
//                actualizarEstadoVisualDeListas();
//            }
//        };
//
//        if (projectList != null) {
//            projectList.addMouseListener(listMouseAdapter);
//            projectList.addListSelectionListener(e -> {
//                if (!e.getValueIsAdjusting()) {
//                    String selectedKey = projectList.getSelectedValue();
//                    if (selectedKey != null) {
//                        model.setSelectedImageKey(selectedKey);
//                        int indiceMaestro = model.getModeloLista().indexOf(selectedKey);
//                        if (indiceMaestro >= 0) {
//                            controllerRef.actualizarImagenPrincipal(indiceMaestro);
//                        }
//                    }
//                }
//            });
//        }
//
//        if (descartesList != null) {
//            descartesList.addMouseListener(listMouseAdapter);
//            descartesList.addListSelectionListener(e -> {
//                if (!e.getValueIsAdjusting()) {
//                    String selectedKey = descartesList.getSelectedValue();
//                    if (selectedKey != null) {
//                        model.setSelectedImageKey(selectedKey);
//                        int indiceMaestro = model.getModeloLista().indexOf(selectedKey);
//                        if (indiceMaestro >= 0) {
//                            controllerRef.actualizarImagenPrincipal(indiceMaestro);
//                        }
//                    }
//                }
//            });
//        }
//    } // --- Fin del método configurarListeners ---
//
//    
//    public boolean prepararDatosProyecto() {
//        System.out.println("  [ProjectController] Preparando datos para el modo proyecto...");
//        if (projectManager == null || model == null) {
//            System.err.println("ERROR CRÍTICO [prepararDatosProyecto]: ProjectManager o Model nulos.");
//            return false;
//        }
//
//        // <<< LÓGICA MODIFICADA PARA UNIR AMBAS LISTAS EN UN SOLO MODELO >>>
//        List<java.nio.file.Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
//        List<java.nio.file.Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
//        
//        // Unimos ambas listas para que el ListCoordinator pueda manejarlas
//        List<java.nio.file.Path> todasLasImagenes = new java.util.ArrayList<>();
//        todasLasImagenes.addAll(imagenesMarcadas);
//        todasLasImagenes.addAll(imagenesDescartadas);
//        
//        // Usamos un Set para eliminar duplicados si por alguna razón una imagen estuviera en ambos sitios
//        todasLasImagenes = todasLasImagenes.stream().distinct().collect(Collectors.toList());
//        java.util.Collections.sort(todasLasImagenes); // Ordenar la lista unificada
//
//        if (todasLasImagenes.isEmpty()) {
//            JOptionPane.showMessageDialog(view, "No hay imágenes marcadas ni descartadas en el proyecto actual.", "Proyecto Vacío", JOptionPane.INFORMATION_MESSAGE);
//            return false;
//        }
//
//        DefaultListModel<String> modeloUnificado = new DefaultListModel<>();
//        Map<String, Path> mapaRutasProyecto = new HashMap<>();
//        
//        for (java.nio.file.Path rutaAbsoluta : todasLasImagenes) {
//            String clave = rutaAbsoluta.toString().replace("\\", "/");
//            modeloUnificado.addElement(clave);
//            mapaRutasProyecto.put(clave, rutaAbsoluta);
//        }
//
//        ListContext proyectoContext = model.getProyectoListContext();
//        proyectoContext.actualizarContextoCompleto(modeloUnificado, mapaRutasProyecto);
//        
//        System.out.println("    -> Datos del proyecto preparados en proyectoListContext. Total de imágenes (selección + descartes): " + modeloUnificado.getSize());
//        return true;
//    } // --- Fin del método prepararDatosProyecto ---
//    
//    
//    public void activarVistaProyecto() {
//        System.out.println("  [ProjectController] Activando la UI de la vista de proyecto...");
//        if (registry == null || model == null || projectManager == null || controllerRef == null) {
//            System.err.println("ERROR [ProjectController.activarVistaProyecto]: Dependencias nulas.");
//            return;
//        }
//        
//        // --- 1. POBLAR LAS LISTAS DE LA UI ---
//        JList<String> projectList = registry.get("list.proyecto.nombres");
//        JList<String> descartesList = registry.get("list.proyecto.descartes"); // <<< AHORA SÍ SE USARÁ
//
//        // Poblar la lista de Selección Actual
//        List<Path> imagenesMarcadas = projectManager.getImagenesMarcadas();
//        DefaultListModel<String> modeloSeleccion = new DefaultListModel<>();
//        for (Path p : imagenesMarcadas) {
//            modeloSeleccion.addElement(p.toString().replace("\\", "/"));
//        }
//        if (projectList != null) {
//            projectList.setModel(modeloSeleccion);
//        }
//        
//        // Poblar la lista de Descartes
//        poblarListaDescartes();
//
//        // Actualizar los títulos
//        JPanel panelListas = registry.get("panel.proyecto.listas.container");
//        if (panelListas != null && panelListas.getBorder() instanceof javax.swing.border.TitledBorder) {
//            ((javax.swing.border.TitledBorder) panelListas.getBorder()).setTitle("Selección Actual: " + modeloSeleccion.getSize());
//            panelListas.repaint();
//        }
//        
//        // --- 2. RESTAURAR ESTADO DE LA "MOCHILA" ---
//        
//        // Forzamos un refresco visual inicial para que la lista correcta aparezca como inactiva
//        actualizarEstadoVisualDeListas();
//        
//        // Obtenemos la última clave seleccionada que guardó la mochila
//        String claveGuardada = model.getSelectedImageKey();
//        
//        // --- INICIO DE LA CORRECCIÓN ---
//        // Restaurar la selección visual en la lista correcta
//        if (claveGuardada != null) {
//            boolean enDescartes = projectManager.estaEnDescartes(Paths.get(claveGuardada));
//            if (enDescartes) {
//                if (descartesList != null) {
//                    descartesList.setSelectedValue(claveGuardada, true);
//                }
//            } else {
//                if (projectList != null) {
//                    projectList.setSelectedValue(claveGuardada, true);
//                }
//            }
//        } else if (!modeloSeleccion.isEmpty()) {
//            // Caso de fallback: si no había nada seleccionado, seleccionamos el primer item de la lista principal
//            if (projectList != null) {
//                projectList.setSelectedIndex(0);
//            }
//        }
//        // --- FIN DE LA CORRECCIÓN ---
//
//        // La llamada a setSelectedValue/setSelectedIndex en el bloque anterior disparará el 
//        // ListSelectionListener correspondiente, que a su vez cargará la imagen.
//        // Por tanto, no se necesita una llamada explícita a actualizarImagenPrincipal aquí.
//        
//        System.out.println("  [ProjectController] UI de la vista de proyecto activada.");
//    } // --- Fin del método activarVistaProyecto ---
//
//
//    // --- Setters de Dependencias ---
//    public void setProjectManager(IProjectManager projectManager) {this.projectManager = Objects.requireNonNull(projectManager);}
//    public void setViewManager(IViewManager viewManager) { this.viewManager = Objects.requireNonNull(viewManager); }
//    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
//    public void setZoomManager(IZoomManager zoomManager) { this.zoomManager = Objects.requireNonNull(zoomManager); }
//    public void setListCoordinator(IListCoordinator listCoordinator) { this.listCoordinator = Objects.requireNonNull(listCoordinator); } // <-- NUEVO SETTER
//    public void setView(VisorView view) { this.view = Objects.requireNonNull(view); }
//    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
//    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
//    public void setController(VisorController controller) { this.controllerRef = Objects.requireNonNull(controller); }
//
//
//// ****************************************************************** --- INICIO DE LA IMPLEMENTACIÓN DE IModoController ---
//
//    @Override
//    public void navegarSiguiente() {
//        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
//        if (listaParaNavegar != null) {
//            int total = listaParaNavegar.getModel().getSize();
//            if (total == 0) return;
//            int idx = listaParaNavegar.getSelectedIndex();
//            idx = (idx + 1);
//            if (idx >= total) { // Aplicar navegación circular si es necesario
//                idx = model.isNavegacionCircularActivada() ? 0 : total - 1;
//            }
//            listaParaNavegar.setSelectedIndex(idx);
//            listaParaNavegar.ensureIndexIsVisible(idx);
//        }
//    } // --- Fin del método navegarSiguiente ---
//
//    @Override
//    public void navegarAnterior() {
//        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
//        if (listaParaNavegar != null) {
//            int total = listaParaNavegar.getModel().getSize();
//            if (total == 0) return;
//            int idx = listaParaNavegar.getSelectedIndex();
//            if (idx == -1) idx = 0; // Si no hay nada seleccionado, empezamos desde el principio
//            idx = idx - 1;
//            if (idx < 0) { // Aplicar navegación circular si es necesario
//                idx = model.isNavegacionCircularActivada() ? total - 1 : 0;
//            }
//            listaParaNavegar.setSelectedIndex(idx);
//            listaParaNavegar.ensureIndexIsVisible(idx);
//        }
//    } // --- Fin del método navegarAnterior ---
//
//    @Override
//    public void navegarPrimero() {
//        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
//        if (listaParaNavegar != null && listaParaNavegar.getModel().getSize() > 0) {
//            listaParaNavegar.setSelectedIndex(0);
//            listaParaNavegar.ensureIndexIsVisible(0);
//        }
//    } // --- Fin del método navegarPrimero ---
//
//    @Override
//    public void navegarUltimo() {
//        JList<?> listaParaNavegar = obtenerListaActivaDesdeModelo();
//        if (listaParaNavegar != null) {
//            int ultimoIdx = listaParaNavegar.getModel().getSize() - 1;
//            if (ultimoIdx >= 0) {
//                listaParaNavegar.setSelectedIndex(ultimoIdx);
//                listaParaNavegar.ensureIndexIsVisible(ultimoIdx);
//            }
//        }
//    } // --- Fin del método navegarUltimo ---
//
//    @Override
//    public void navegarBloqueSiguiente() {
//        JList<?> listaActiva = obtenerListaActivaDesdeModelo();
//        if (listaActiva != null && model != null) {
//            int indiceActual = listaActiva.getSelectedIndex();
//
//            if (indiceActual == -1) {
//                indiceActual = 0;
//            } else {
//                indiceActual = Math.min(indiceActual + 10, listaActiva.getModel().getSize() - 1);
//            }
//
//            if (listaActiva.getModel().getSize() > indiceActual) {
//            	String nuevaClave ="descartes";
//                model.setSelectedImageKey(nuevaClave);
//
//                int indiceMaestro = model.getModeloLista().indexOf(nuevaClave);
//                if (controllerRef != null && indiceMaestro >= 0) {
//                    controllerRef.actualizarImagenPrincipal(indiceMaestro);
//                    listaActiva.setSelectedIndex(indiceActual); // Sincroniza selección visual
//                }
//            }
//        }
//    } // --- Fin del método navegarBloqueSiguiente ---
//
//    
//    @Override
//    public void navegarBloqueAnterior() {
//        JList<?> listaActiva = obtenerListaActivaDesdeModelo();
//        if (listaActiva != null && model != null) {
//            int indiceActual = listaActiva.getSelectedIndex();
//
//            if (indiceActual == -1) {
//                indiceActual = 0;
//            } else {
//                indiceActual = Math.max(indiceActual - 10, 0);
//            }
//
//            if (listaActiva.getModel().getSize() > indiceActual) {
////                String nuevaClave = listaActiva.getModel().getElementAt(indiceActual);
//            	String nuevaClave ="descartes";
//                model.setSelectedImageKey(nuevaClave);
//
//                int indiceMaestro = model.getModeloLista().indexOf(nuevaClave);
//                if (indiceMaestro >= 0 && controllerRef != null) {
//                    controllerRef.actualizarImagenPrincipal(indiceMaestro);
//                    listaActiva.setSelectedIndex(indiceActual);
//                    listaActiva.ensureIndexIsVisible(indiceActual);
//                }
//            }
//        }
//    } // --- Fin del método navegarBloqueAnterior ---
//
//    @Override
//    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
//        if (zoomManager != null) {
//            zoomManager.aplicarZoomConRueda(e);
//            if (controllerRef != null) {
//                controllerRef.sincronizarEstadoVisualBotonesYRadiosZoom();
//            }
//        }
//    } // --- Fin del método aplicarZoomConRueda ---
//
//    @Override
//    public void aplicarPan(int deltaX, int deltaY) {
//        if (zoomManager != null) {
//            zoomManager.aplicarPan(deltaX, deltaY);
//        }
//    } // --- Fin del método aplicarPan ---
//
//    @Override
//    public void iniciarPaneo(java.awt.event.MouseEvent e) {
//        if (zoomManager != null && model.isZoomHabilitado()) {
//            zoomManager.iniciarPaneo(e);
//        }
//    } // --- Fin del método iniciarPaneo ---
//
//    @Override
//    public void continuarPaneo(java.awt.event.MouseEvent e) {
//        if (zoomManager != null && model.isZoomHabilitado()) {
//            zoomManager.continuarPaneo(e);
//        }
//    } // --- Fin del método continuarPaneo ---
//    
//// ****************************************************************** --- FIN DE LA IMPLEMENTACIÓN DE IModoController ---
//    
//// ******************************************************************************************** GESTION DEL MODO PROYECTO
//    
//    
//    
//    /**
//     * Puebla la JList de descartes con los datos actuales del ProjectManager.
//     * Este método debe ser llamado cuando se activa la vista de proyecto.
//     */
//    public void poblarListaDescartes() {
//        if (registry == null || projectManager == null) {
//            System.err.println("WARN [poblarListaDescartes]: Registry o ProjectManager nulos.");
//            return;
//        }
//
//        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
//        if (listaDescartesUI == null) {
//            System.err.println("WARN [poblarListaDescartes]: JList 'list.proyecto.descartes' no encontrada en el registro.");
//            return;
//        }
//
//        List<Path> imagenesDescartadas = projectManager.getImagenesDescartadas();
//        DefaultListModel<String> modeloDescartes = new DefaultListModel<>();
//
//        for (Path rutaAbsoluta : imagenesDescartadas) {
//            String clave = rutaAbsoluta.toString().replace("\\", "/");
//            modeloDescartes.addElement(clave);
//        }
//
//        listaDescartesUI.setModel(modeloDescartes);
//        System.out.println("  [ProjectController] Lista de descartes actualizada en la UI. Total: " + modeloDescartes.getSize());
//    } // --- Fin del método poblarListaDescartes ---
//
//    /**
//     * Mueve la imagen actualmente seleccionada en la lista principal a la lista de descartes.
//     */
//    public void moverSeleccionActualADescartes() {
//        if (model == null || projectManager == null) return;
//
//        String claveSeleccionada = model.getSelectedImageKey();
//        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
//            System.out.println("  [ProjectController] No hay imagen seleccionada para mover a descartes.");
//            return;
//        }
//
//        Path rutaAbsoluta = model.getRutaCompleta(claveSeleccionada);
//        if (rutaAbsoluta != null) {
//            projectManager.moverAdescartes(rutaAbsoluta);
//            // Después de mover, refrescamos ambas listas en la UI
//            refrescarListasDeProyecto();
//        }
//    } // --- Fin del método moverSeleccionActualADescartes ---
//
//    /**
//     * Mueve la imagen actualmente seleccionada en la lista de descartes de vuelta a la selección principal.
//     */
//    public void restaurarDesdeDescartes() {
//        if (registry == null || projectManager == null) return;
//        
//        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
//        if (listaDescartesUI == null) return;
//
//        String claveSeleccionada = listaDescartesUI.getSelectedValue();
//        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
//            System.out.println("  [ProjectController] No hay imagen seleccionada en descartes para restaurar.");
//            return;
//        }
//
//        // Como la clave es la ruta absoluta, podemos convertirla directamente a Path
//        Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
//        projectManager.restaurarDeDescartes(rutaAbsoluta);
//        
//        // Después de restaurar, refrescamos ambas listas en la UI
//        refrescarListasDeProyecto();
//    } // --- Fin del método restaurarDesdeDescartes ---
//
//    /**
//     * Refresca el contenido de las listas de "Selección Actual" y "Descartes"
//     * para que reflejen el estado actual del ProjectManager.
//     */
//    private void refrescarListasDeProyecto() {
//        System.out.println("  [ProjectController] Refrescando ambas listas del proyecto...");
//        
//        // Refrescar la lista de selección principal
//        // El método prepararDatosProyecto ya hace esto, así que lo reutilizamos
//        prepararDatosProyecto(); 
//        
//        // Lo activamos de nuevo para que la JList coja el nuevo modelo
//        activarVistaProyecto(); 
//
//        // Refrescar la lista de descartes
//        poblarListaDescartes();
//    } // --- Fin del método refrescarListasDeProyecto ---
//    
//// ************************************************************************************ FIN DE  GESTION DEL MODO PROYECTO    
//    
//    
//    /**
//     * Inicia el proceso de preparación de la cola de exportación y actualiza la UI.
//     * Es llamado por el ActionListener del botón "Cargar Selección a la Cola".
//     */
//    public void solicitarPreparacionColaExportacion() {
//        if (projectManager == null || exportQueueManager == null || registry == null) {
//            System.err.println("ERROR [solicitarPreparacionColaExportacion]: Dependencias nulas.");
//            return;
//        }
//        
//        List<Path> seleccionActual = projectManager.getImagenesMarcadas();
//        exportQueueManager.prepararColaDesdeSeleccion(seleccionActual);
//
//        // Ahora, actualizamos la tabla en la UI
//        JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
//        if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
//            vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
//            
//            // Navegamos por la estructura interna del ExportPanel para encontrar la tabla.
//            // Component[1] es el JScrollPane, su Viewport contiene la JTable.
//            if (exportPanel.getComponentCount() > 1 && exportPanel.getComponent(1) instanceof javax.swing.JScrollPane) {
//                javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) exportPanel.getComponent(1);
//                JTable tablaUI = (JTable) scrollPane.getViewport().getView();
//            
//                if (tablaUI.getModel() instanceof ExportTableModel) {
//                    ((ExportTableModel) tablaUI.getModel()).setCola(exportQueueManager.getColaDeExportacion());
//                    System.out.println("[ProjectController] Modelo de tabla de exportación actualizado.");
//                }
//            }
//        }
//        
//        actualizarEstadoExportacionUI();
//        
//    } // --- Fin del método solicitarPreparacionColaExportacion ---
//    
//    
//    /**
//     * Muestra un diálogo JFileChooser para que el usuario seleccione una carpeta de destino
//     * para la exportación. Si se selecciona una carpeta, actualiza el campo de texto
//     * correspondiente en la interfaz.
//     */
//    public void solicitarSeleccionCarpetaDestino() {
//        if (registry == null || view == null) {
//            System.err.println("ERROR [solicitarSeleccionCarpetaDestino]: Registry o View nulos.");
//            return;
//        }
//
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle("Seleccionar Carpeta de Destino para la Exportación");
//        // Configuramos el chooser para que solo permita seleccionar directorios.
//        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        fileChooser.setAcceptAllFileFilterUsed(false); // No mostrar el filtro "Todos los archivos"
//
//        // Mostramos el diálogo. El 'view' (nuestro JFrame principal) actúa como padre.
//        int resultado = fileChooser.showOpenDialog(view);
//
//        // Si el usuario hace clic en "Abrir" o "Aceptar"
//        if (resultado == JFileChooser.APPROVE_OPTION) {
//            Path carpetaSeleccionada = fileChooser.getSelectedFile().toPath();
//            System.out.println("  [ProjectController] Carpeta de destino seleccionada: " + carpetaSeleccionada);
//
//            // Ahora, buscamos el panel de exportación y su campo de texto para actualizarlo.
//            JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
//            if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
//                vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
//                
//                // Obtenemos el JTextField del panel de exportación y le ponemos la nueva ruta.
//                exportPanel.setRutaDestino(carpetaSeleccionada.toString());
//            }
//        } else {
//            System.out.println("  [ProjectController] Selección de carpeta de destino cancelada por el usuario.");
//        }
//        
//        actualizarEstadoExportacionUI();
//        
//    } // --- Fin del método solicitarSeleccionCarpetaDestino ---
//    
//    /**
//     * Callback que se ejecuta cuando el usuario asigna un archivo manualmente en la tabla de exportación.
//     * Refresca la tabla y el estado de los controles.
//     * @param itemModificado El ExportItem que fue modificado.
//     */
//    public void onExportItemManuallyAssigned(modelo.proyecto.ExportItem itemModificado) {
//        System.out.println("  [ProjectController] Archivo asignado manualmente para: " + itemModificado.getRutaImagen().getFileName());
//        
//        // Simplemente refrescamos el estado general de la UI de exportación
//        actualizarEstadoExportacionUI();
//    } // --- Fin del método onExportItemManuallyAssigned ---
//    
//    /**
//     * DEFINITIVO: Comprueba el estado de la cola y actualiza la UI.
//     * El botón de exportar se activa si hay una carpeta de destino, si hay al menos un
//     * ítem seleccionado para exportar, y si TODOS los ítems seleccionados para exportar
//     * tienen un estado válido.
//     */
//    public void actualizarEstadoExportacionUI() {
//        if (registry == null || exportQueueManager == null) {
//            return;
//        }
//
//        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
//        if (exportPanel == null) return;
//        
//        java.util.List<modelo.proyecto.ExportItem> colaCompleta = exportQueueManager.getColaDeExportacion();
//        
//        // 1. Filtrar solo los ítems que el usuario ha marcado con el checkbox.
//        List<modelo.proyecto.ExportItem> itemsSeleccionadosParaExportar = colaCompleta.stream()
//                                                                              .filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar)
//                                                                              .collect(Collectors.toList());
//
//        // 2. Comprobar si TODOS los ítems seleccionados para exportar tienen un estado válido.
//        boolean todosLosSeleccionadosEstanListos = itemsSeleccionadosParaExportar.stream().allMatch(item ->
//            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
//            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
//            item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO
//        );
//        
//        // 3. Comprobar si se ha seleccionado una carpeta de destino.
//        String rutaDestino = exportPanel.getRutaDestino();
//        boolean carpetaOk = rutaDestino != null && !rutaDestino.isBlank() && !rutaDestino.equals("Seleccione una carpeta de destino...");
//
//        // 4. Determinar si el botón "Iniciar Exportación" debe estar activo.
//        //    Debe estar activo SI Y SOLO SI:
//        //    a) Hay una carpeta seleccionada.
//        //    b) Hay al menos un ítem seleccionado para exportar.
//        //    c) TODOS los que están seleccionados están en un estado válido.
//        boolean puedeExportar = carpetaOk && !itemsSeleccionadosParaExportar.isEmpty() && todosLosSeleccionadosEstanListos;
//        
//        // 5. Crear el mensaje de resumen.
//        String mensaje = itemsSeleccionadosParaExportar.size() + " de " + colaCompleta.size() + " archivos seleccionados para exportar.";
//        
//        // 6. Actualizar la UI.
//        exportPanel.actualizarEstadoControles(puedeExportar, mensaje); // Modificamos el método para que solo necesite el resultado final
//        
//        // La lógica de resaltado de la carpeta de destino no cambia.
//        boolean resaltarDestino = !colaCompleta.isEmpty() && !carpetaOk;
//        exportPanel.resaltarRutaDestino(resaltarDestino);
//        
//        System.out.println("  [ProjectController] Estado de exportación UI actualizado. Puede exportar: " + puedeExportar);
//    } // --- Fin del método actualizarEstadoExportacionUI ---
//    
//    
//    /**
//     * Inicia el proceso de exportación de archivos.
//     * Crea y ejecuta un SwingWorker para realizar la copia en segundo plano.
//     */
//    public void solicitarInicioExportacion() {
//        if (exportQueueManager == null || registry == null || view == null) {
//            System.err.println("ERROR [solicitarInicioExportacion]: Dependencias nulas.");
//            return;
//        }
//        
//        vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) registry.get("panel.proyecto.herramientas.exportar");
//        Path carpetaDestino = java.nio.file.Paths.get(exportPanel.getRutaDestino());
//
//        // Obtener solo la lista de ítems que el usuario ha marcado con el checkbox
//        List<modelo.proyecto.ExportItem> colaParaCopiar = exportQueueManager.getColaDeExportacion().stream()
//            .filter(modelo.proyecto.ExportItem::isSeleccionadoParaExportar)
//            .filter(item -> 
//                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ENCONTRADO_OK ||
//                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.ASIGNADO_MANUAL ||
//                item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO
//            )
//            .collect(Collectors.toList());
//            
//        if (colaParaCopiar.isEmpty()) {
//            JOptionPane.showMessageDialog(view, "No hay archivos válidos seleccionados en la cola para exportar.", "Exportación Vacía", JOptionPane.WARNING_MESSAGE);
//            return;
//        }
//        
//        ExportProgressDialog dialogo = new ExportProgressDialog(view);
//        ExportWorker worker = new ExportWorker(colaParaCopiar, carpetaDestino, dialogo);
//        
//        worker.addPropertyChangeListener(evt -> {
//            if ("progress".equals(evt.getPropertyName())) {
//                dialogo.setProgress((Integer) evt.getNewValue());
//            }
//        });
//        
//        worker.execute();
//        dialogo.setVisible(true);
//    } // --- Fin del método solicitarInicioExportacion ---
//    
//    
//    /**
//     * Abre la carpeta que contiene el archivo de imagen especificado en el explorador de archivos del sistema.
//     * Se activa desde el menú contextual de la tabla de exportación.
//     */
//    public void solicitarAbrirUbicacionImagen() {
//        if (exportQueueManager == null || registry == null) return;
//        
//        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
//        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
//
//        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
//        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
//
//        if (itemSeleccionado != null) {
//            try {
//                // Llamamos a nuestro nuevo método de utilidad
//                DesktopUtils.openAndSelectFile(itemSeleccionado.getRutaImagen());
//            } catch (Exception e) {
//                System.err.println("Error al intentar abrir y seleccionar el archivo: " + e.getMessage());
//                JOptionPane.showMessageDialog(view, "No se pudo abrir la ubicación del archivo.", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        }
//    } // --- Fin del método solicitarAbrirUbicacionImagen ---
//    
//    
//    /**
//     * Alterna el estado de un ítem entre NO_ENCONTRADO e IGNORAR_COMPRIMIDO.
//     */
//    public void solicitarAlternarIgnorarComprimido() {
//        if (registry == null) return;
//        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
//        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
//
//        int filaSeleccionada = tablaExportacion.getSelectedRow();
//        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
//        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
//
//        if (item != null) {
//            if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.NO_ENCONTRADO) {
//                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO);
//            } else if (item.getEstadoArchivoComprimido() == modelo.proyecto.ExportStatus.IGNORAR_COMPRIMIDO) {
//                item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.NO_ENCONTRADO);
//            }
//            
//            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
//            actualizarEstadoExportacionUI();
//        }
//    } // --- Fin del método solicitarAlternarIgnorarComprimido ---
//    
//    
//    /**
//     * Inicia el proceso para que el usuario asigne manualmente un archivo
//     * (comprimido o STL) a un ítem de la cola de exportación.
//     * Reutiliza la lógica del TableCellEditor.
//     */
//    public void solicitarAsignacionManual() {
//        if (registry == null) return;
//        
//        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
//        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
//        
//        int filaSeleccionada = tablaExportacion.getSelectedRow();
//        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
//        modelo.proyecto.ExportItem item = modelTabla.getItemAt(filaSeleccionada);
//
//        if (item == null) return;
//
//        JFileChooser fileChooser = new JFileChooser();
//        
//        // Establecer el directorio inicial del FileChooser en la carpeta de la imagen
//        Path carpetaImagen = item.getRutaImagen().getParent();
//        if (carpetaImagen != null && Files.isDirectory(carpetaImagen)) {
//            fileChooser.setCurrentDirectory(carpetaImagen.toFile());
//        }
//        
//        fileChooser.setDialogTitle("Localizar Archivo para " + item.getRutaImagen().getFileName());
//        
//        int result = fileChooser.showOpenDialog(view); // Usamos 'view' (el JFrame) como padre
//        if (result == JFileChooser.APPROVE_OPTION) {
//            Path selectedPath = fileChooser.getSelectedFile().toPath();
//            
//            item.setRutaArchivoComprimido(selectedPath);
//            item.setEstadoArchivoComprimido(modelo.proyecto.ExportStatus.ASIGNADO_MANUAL);
//            
//            // Refrescar la tabla y el estado de los controles
//            modelTabla.fireTableRowsUpdated(filaSeleccionada, filaSeleccionada);
//            actualizarEstadoExportacionUI();
//        }
//    } // --- Fin del método solicitarAsignacionManual ---
//
//    /**
//     * Quita el ítem seleccionado de la cola de exportación.
//     */
//    public void solicitarQuitarDeLaCola() {
//        if (exportQueueManager == null || registry == null) return;
//
//        JTable tablaExportacion = getTablaExportacionDesdeRegistro();
//        if (tablaExportacion == null || tablaExportacion.getSelectedRow() == -1) return;
//        
//        ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
//        modelo.proyecto.ExportItem itemSeleccionado = modelTabla.getItemAt(tablaExportacion.getSelectedRow());
//
//        if (itemSeleccionado != null) {
//            exportQueueManager.getColaDeExportacion().remove(itemSeleccionado);
//            // Refrescamos la tabla y el estado de los botones
//            modelTabla.setCola(exportQueueManager.getColaDeExportacion());
//            actualizarEstadoExportacionUI();
//        }
//    } // --- Fin del método solicitarQuitarDeLaCola ---
//
//    /**
//     * Método de ayuda privado para obtener la JTable de exportación de forma segura.
//     * @return La JTable o null si no se encuentra.
//     */
//    private JTable getTablaExportacionDesdeRegistro() {
//        JPanel exportPanelPlaceholder = registry.get("panel.proyecto.herramientas.exportar");
//        if (exportPanelPlaceholder instanceof vista.panels.export.ExportPanel) {
//            vista.panels.export.ExportPanel exportPanel = (vista.panels.export.ExportPanel) exportPanelPlaceholder;
//            if (exportPanel.getComponent(1) instanceof javax.swing.JScrollPane) {
//                javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) exportPanel.getComponent(1);
//                return (JTable) scrollPane.getViewport().getView();
//            }
//        }
//        return null;
//    } // --- Fin del método getTablaExportacionDesdeRegistro ---
//    
//    
//    /**
//     * Orquesta la operación de alternar el estado de marca de la imagen
//     * seleccionada DENTRO DEL MODO PROYECTO.
//     * En este modo, "alternar marca" se reinterpreta como mover la imagen a la lista de descartes.
//     * Esta solicitud es delegada por GeneralController.
//     */
//    public void solicitudAlternarMarcaImagen() {
//        System.out.println("  [ProjectController] Solicitud para mover selección a descartes (desde botón 'Marcar')...");
//        // En modo proyecto, la acción de "marcar/desmarcar" se reinterpreta como "mover a descartes".
//        // Simplemente delegamos la llamada al método que ya contiene esta lógica.
//        this.moverSeleccionActualADescartes();
//    } // --- Fin del método solicitudAlternarMarcaImagen ---
//    
//    
//    /**
//     * <<< NUEVO MÉTODO >>>
//     * Es llamado por la Action del menú contextual para eliminar una imagen del proyecto.
//     * Pide confirmación al usuario antes de proceder.
//     */
//    public void solicitarEliminacionPermanente() {
//        if (registry == null || projectManager == null || view == null) {
//            System.err.println("WARN [solicitarEliminacionPermanente]: Dependencias nulas.");
//            return;
//        }
//        
//        JList<String> listaDescartesUI = registry.get("list.proyecto.descartes");
//        if (listaDescartesUI == null) {
//            System.err.println("WARN [solicitarEliminacionPermanente]: JList 'list.proyecto.descartes' no encontrada.");
//            return;
//        }
//
//        String claveSeleccionada = listaDescartesUI.getSelectedValue();
//        if (claveSeleccionada == null || claveSeleccionada.isEmpty()) {
//            System.out.println("  [ProjectController] No hay imagen seleccionada en descartes para eliminar.");
//            return;
//        }
//
//        // Confirmación del usuario
//        int confirm = JOptionPane.showConfirmDialog(
//            view,
//            "¿Seguro que quieres eliminar esta imagen del proyecto?\n(No se borrará el archivo del disco)",
//            "Confirmar Eliminación",
//            JOptionPane.YES_NO_OPTION,
//            JOptionPane.WARNING_MESSAGE
//        );
//
//        if (confirm == JOptionPane.YES_OPTION) {
//            Path rutaAbsoluta = java.nio.file.Paths.get(claveSeleccionada);
//            projectManager.eliminarDeProyecto(rutaAbsoluta);
//            
//            refrescarListasDeProyecto();
//        }
//    } // --- Fin del método solicitarEliminacionPermanente ---
//    
//    /**
//     * <<< NUEVO MÉTODO >>>
//     * Actualiza el estado visual de las listas de proyecto basándose en la
//     * información guardada en el modelo ("mochila").
//     * La lista inactiva tendrá su selección limpiada.
//     */
//    private void actualizarEstadoVisualDeListas() {
//        if (registry == null || model == null || model.getProyectoListContext() == null) {
//            return;
//        }
//
//        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
//        JList<String> projectList = registry.get("list.proyecto.nombres");
//        JList<String> descartesList = registry.get("list.proyecto.descartes");
//
//        if (projectList != null && descartesList != null) {
//            if ("seleccion".equals(nombreListaActiva)) {
//                descartesList.clearSelection();
//            } else {
//                projectList.clearSelection();
//            }
//        }
//    } // --- Fin del método actualizarEstadoVisualDeListas ---
//    
//    
//    /**
//     * <<< NUEVO MÉTODO HELPER >>>
//     * Lee el estado del modelo para determinar cuál es la JList activa
//     * y la devuelve.
//     * @return El componente JList activo, o null si no se puede determinar.
//     */
//    private JList<?> obtenerListaActivaDesdeModelo() {
//        if (registry == null || model == null || model.getProyectoListContext() == null) {
//            return null;
//        }
//
//        String nombreListaActiva = model.getProyectoListContext().getNombreListaActiva();
//        
//        if ("descartes".equals(nombreListaActiva)) {
//            return registry.get("list.proyecto.descartes");
//        } else {
//            // Por defecto, o si es "seleccion", la lista activa es la principal.
//            return registry.get("list.proyecto.nombres");
//        }
//    } // --- Fin del método obtenerListaActivaDesdeModelo ---
//    
//    
//    
//} // --- FIN de la clase ProjectController ---
