package controlador.managers;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.IModoController;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;

/**
 * Gestor centralizado para los eventos de entrada globales de la aplicación.
 * Implementa la lógica para atajos de teclado globales, resaltado de foco
 * y la rueda del ratón universal, liberando a otros controladores de esta
 * responsabilidad.
 */
public class GlobalInputManager implements KeyEventDispatcher, PropertyChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(GlobalInputManager.class);

    // --- Dependencias Clave ---
    private VisorModel model;
    private ComponentRegistry registry;
    private Map<String, Action> actionMap;
    private IModoController modoController; // Delegado para acciones de navegación/zoom
    private VisorController visorController; // Necesario para acceder a la vista/menubar
    private ProjectController projectController; // Necesario para la rueda en la tabla de exportación

    // --- Estado de la UI de Foco ---
    private javax.swing.border.Border focusedBorder;
    private javax.swing.border.Border unfocusedBorder;
    private List<javax.swing.JComponent> focusablePanels;
    private TitledBorder borderListaArchivosOriginal;
    private TitledBorder borderFiltrosActivosOriginal;
    
    private int lastMouseX, lastMouseY;
    
    
    /**
     * Constructor para GestorEntradaGlobal.
     * Las dependencias se inyectan para desacoplar el gestor de otros componentes.
     */
    public GlobalInputManager() {
        // Constructor vacío. La inicialización se hace a través de setters y el método initialize.
    } // --- Fin del método GestorEntradaGlobal (constructor) ---

    // --- Setters para Inyección de Dependencias ---
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry); }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = Objects.requireNonNull(actionMap); }
    public void setModoController(IModoController modoController) { this.modoController = Objects.requireNonNull(modoController); }
    public void setVisorController(VisorController visorController) { this.visorController = Objects.requireNonNull(visorController); }
    public void setProjectController(ProjectController projectController) { this.projectController = Objects.requireNonNull(projectController); }
    
    /**
     * Inicializa los componentes internos del gestor, como la lista de paneles
     * que pueden recibir foco visual. Debe llamarse después de que todas las
     * dependencias hayan sido inyectadas.
     */
    public void initialize() {
        logger.debug("[GestorEntradaGlobal] Inicializado.");
        
        this.focusablePanels = new ArrayList<>();
        int thickness = 2;
        this.unfocusedBorder = javax.swing.BorderFactory.createEmptyBorder(thickness, thickness, thickness, thickness);
        // El borde de foco se creará dinámicamente en propertyChange para usar el color del tema.

        // --- Registro de Paneles para Foco ---
        registerFocusablePanel("tabbedpane.izquierdo");
        registerFocusablePanel("scroll.miniaturas");
        registerFocusablePanel("panel.derecho.visor");
        
        registerFocusablePanel("panel.display.imagen");
        registerFocusablePanel("panel.proyecto.display");
        registerFocusablePanel("panel.display.grid.proyecto");
        
        registerFocusablePanel("panel.izquierdo.listaArchivos");
        registerFocusablePanel("list.nombresArchivo"); 
        registerFocusablePanel("scroll.nombresArchivo");
        registerFocusablePanel("scroll.arbol");
        registerFocusablePanel("tree.carpetas");
        registerFocusablePanel("panel.izquierdo.filtros");
        registerFocusablePanel("list.filtrosActivos");
        registerFocusablePanel("scroll.filtrosActivos");
        registerFocusablePanel("panel.display.grid.proyecto");
        registerFocusablePanel("scroll.grid.visualizador");
        registerFocusablePanel("scroll.grid.proyecto");
        registerFocusablePanel("tabbedpane.proyecto.herramientas");
        registerFocusablePanel("scroll.proyecto.nombres");
        registerFocusablePanel("scroll.proyecto.descartes");
        registerFocusablePanel("panel.proyecto.exportacion.completo"); 
        registerFocusablePanel("scroll.tabla.exportacion");
        registerFocusablePanel("panel.exportacion.detalles");
        registerFocusablePanel("textfield.filtro.orden");
        registerFocusablePanel("textfield.export.destino");
//        registerFocusablePanel("textfield.filtro.texto");
        registerFocusablePanel("interfaz.boton.acciones_exportacion.export_detalles_seleccion");
        
        SwingUtilities.invokeLater(() -> {
            JPanel panelLista = registry.get("panel.izquierdo.listaArchivos");
            if (panelLista != null && panelLista.getBorder() instanceof TitledBorder) {
                this.borderListaArchivosOriginal = (TitledBorder) panelLista.getBorder();
            }
            
            JPanel panelFiltros = registry.get("panel.izquierdo.filtros");
            if (panelFiltros != null && panelFiltros.getBorder() instanceof TitledBorder) {
                this.borderFiltrosActivosOriginal = (TitledBorder) panelFiltros.getBorder();
            }
            
            if(registry.get("panel.exportacion.detalles") instanceof JPanel) {
                JPanel detailPanel = registry.get("panel.exportacion.detalles");
                for(Component comp : detailPanel.getComponents()) {
                    if (comp instanceof JPanel) {
                         for(Component innerComp : ((JPanel)comp).getComponents()){
                            if (innerComp instanceof JScrollPane && "scroll.detalles.exportacion".equals(innerComp.getName())) {
                                focusablePanels.add((JScrollPane) innerComp);
                                break;
                            }
                         }
                    }
                }
            }
        });
    } // --- Fin del método initialize ---
    
    /**
     * Configura los listeners globales de la aplicación (rueda del ratón, paneo)
     * y los asocia a los componentes correspondientes del registro.
     */
    public void configurarListeners() {
        logger.debug("[GestorEntradaGlobal] Configurando listeners de entrada globales...");

        java.awt.event.MouseWheelListener masterWheelListener = e -> {
            // --- INICIO DE LA CORRECCIÓN ---
            // Obtenemos las referencias a los JLabels de imagen UNA SOLA VEZ, al inicio del evento.
            Component etiquetaImagenVisualizador = registry.get("label.imagenPrincipal");
            Component etiquetaImagenProyecto = registry.get("label.proyecto.imagen");
            Component etiquetaImagenCarrusel = registry.get("label.carousel.imagen");
            Component sourceComponent = e.getComponent();

            // Comprobamos si el componente que originó el evento es uno de nuestros JLabels.
            // Esta es la forma más robusta de saber si estamos sobre la imagen.
            boolean sobreLaImagen = (etiquetaImagenVisualizador != null && sourceComponent == etiquetaImagenVisualizador) ||
                                    (etiquetaImagenProyecto != null && sourceComponent == etiquetaImagenProyecto) ||
                                    (etiquetaImagenCarrusel != null && sourceComponent == etiquetaImagenCarrusel);
            // --- FIN DE LA CORRECCIÓN ---

            JTable tablaExportacion = registry.get("tabla.exportacion");
            boolean sobreTablaExportacion = (tablaExportacion != null && SwingUtilities.isDescendingFrom(sourceComponent, tablaExportacion));
            
            if (e.isControlDown() && e.isAltDown()) {
                if (e.getWheelRotation() < 0) this.modoController.navegarBloqueAnterior();
                else this.modoController.navegarBloqueSiguiente();
                e.consume();
                return;
            }

            if (sobreLaImagen) {
                if (model.isZoomHabilitado()) {
                    if (e.isShiftDown()) {
                        this.modoController.aplicarPan(-e.getWheelRotation() * 30, 0);
                    } else if (e.isControlDown()) {
                        this.modoController.aplicarPan(0, e.getWheelRotation() * 30);
                    } else {
                        this.modoController.aplicarZoomConRueda(e);
                    }
                } else {
                    navegarSiguienteOAnterior(e.getWheelRotation());
                }
                e.consume();
                return;
            }
            
            if (sobreTablaExportacion && model.getCurrentWorkMode() == WorkMode.PROYECTO) {
                projectController.navegarTablaExportacionConRueda(e);
                e.consume();
                return;
            }

            navegarSiguienteOAnterior(e.getWheelRotation());
            e.consume();
        };

        List<Component> componentesConRueda = registry.getComponentsByTag("WHEEL_NAVIGABLE");
        for (Component c : componentesConRueda) {
            for (java.awt.event.MouseWheelListener mwl : c.getMouseWheelListeners()) c.removeMouseWheelListener(mwl);
            c.addMouseWheelListener(masterWheelListener);
        }

     // --- Listeners de clic y arrastre para paneo ---
        MouseAdapter paneoMouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                // Guardamos la posición inicial del ratón
                lastMouseX = ev.getX();
                lastMouseY = ev.getY();
                // Notificamos al controlador de modo que el paneo ha comenzado (por si necesita cambiar el cursor, etc.)
                modoController.iniciarPaneo(ev);
            }
        };
        MouseMotionAdapter paneoMouseMotionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                // Calculamos el delta de movimiento aquí, en el Gestor
                int deltaX = ev.getX() - lastMouseX;
                int deltaY = ev.getY() - lastMouseY;
                
                // Actualizamos la última posición para el siguiente evento de arrastre
                lastMouseX = ev.getX();
                lastMouseY = ev.getY();

                // Llamamos al método aplicarPan, pasando el delta calculado
                modoController.aplicarPan(deltaX, deltaY);
            }
        };
        
        Component etiquetaVisor = registry.get("label.imagenPrincipal");
        Component etiquetaProyecto = registry.get("label.proyecto.imagen");
        Component etiquetaCarrusel = registry.get("label.carousel.imagen");

        if (etiquetaVisor != null) {
            etiquetaVisor.addMouseListener(paneoMouseAdapter);
            etiquetaVisor.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaProyecto != null) {
            etiquetaProyecto.addMouseListener(paneoMouseAdapter);
            etiquetaProyecto.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaCarrusel != null) {
            etiquetaCarrusel.addMouseListener(paneoMouseAdapter);
            etiquetaCarrusel.addMouseMotionListener(paneoMouseMotionAdapter);
        }

        logger.debug("[GestorEntradaGlobal] Listeners configurados.");
    } // --- Fin del método configurarListeners ---

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_PRESSED) return false;

        String command = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_NUMPAD1: command = AppActionCommands.CMD_ZOOM_TIPO_AJUSTAR; break;
            case KeyEvent.VK_NUMPAD2: command = AppActionCommands.CMD_ZOOM_TIPO_AUTO; break;
            case KeyEvent.VK_NUMPAD3: command = AppActionCommands.CMD_ZOOM_TIPO_ANCHO; break;
            case KeyEvent.VK_NUMPAD4: command = AppActionCommands.CMD_ZOOM_TIPO_ALTO; break;
            case KeyEvent.VK_NUMPAD5: command = AppActionCommands.CMD_ZOOM_TIPO_RELLENAR; break;
            case KeyEvent.VK_NUMPAD6: command = AppActionCommands.CMD_ZOOM_TIPO_FIJO; break;
            case KeyEvent.VK_NUMPAD7: command = AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO; break;
            case KeyEvent.VK_NUMPAD8: command = AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE; break;
            case KeyEvent.VK_NUMPAD9: command = AppActionCommands.CMD_ZOOM_RESET; break;
        }
        
        if (command != null) {
            Action action = actionMap.get(command);
            if (action != null && action.isEnabled()) {
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, command));
                e.consume();
                return true;
            }
        }

        if (model.getCurrentWorkMode() == WorkMode.PROYECTO && model.getCurrentDisplayMode() == DisplayMode.GRID) {
            Action action = null;
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_T) action = actionMap.get(AppActionCommands.CMD_GRID_SET_TEXT);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DELETE) action = actionMap.get(AppActionCommands.CMD_GRID_REMOVE_TEXT);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ADD) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA);
            else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SUBTRACT) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA);
            
            if (action != null) {
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                e.consume();
                return true;
            }
        }
        
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (e.getComponent() instanceof javax.swing.text.JTextComponent) return false;
            Action toggleMarkAction = actionMap.get(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
            if (toggleMarkAction != null && toggleMarkAction.isEnabled()) {
                toggleMarkAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA));
                return true; 
            }
        }
        
        if (e.getKeyCode() == KeyEvent.VK_ALT) {
            if (e.getComponent() instanceof javax.swing.text.JTextComponent) return false;
            if (visorController != null && visorController.getView() != null && visorController.getView().getJMenuBar() != null) {
                JMenuBar menuBar = visorController.getView().getJMenuBar();
                if (menuBar.isSelected()) {
                    menuBar.getSelectionModel().clearSelection();
                } else {
                    if (menuBar.getMenuCount() > 0) {
                        JMenu primerMenu = menuBar.getMenu(0);
                        if (primerMenu != null) primerMenu.doClick();
                    }
                }
                e.consume();
                return true;
            }
        }
        
        if (model == null || registry == null) return false;

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) return false;

        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
        JList<String> listaNombres = registry.get("list.nombresArchivo");

        boolean focoEnAreaMiniaturas = (scrollMiniaturas != null && SwingUtilities.isDescendingFrom(focusOwner, scrollMiniaturas));
        boolean focoEnListaNombres = (listaNombres != null && SwingUtilities.isDescendingFrom(focusOwner, listaNombres));

        if (focoEnAreaMiniaturas || focoEnListaNombres) {
            boolean consumed = false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: case KeyEvent.VK_LEFT: modoController.navegarAnterior(); consumed = true; break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_RIGHT: modoController.navegarSiguiente(); consumed = true; break;
                case KeyEvent.VK_HOME: modoController.navegarPrimero(); consumed = true; break;
                case KeyEvent.VK_END: modoController.navegarUltimo(); consumed = true; break;
                case KeyEvent.VK_PAGE_UP: modoController.navegarBloqueAnterior(); consumed = true; break;
                case KeyEvent.VK_PAGE_DOWN: modoController.navegarBloqueSiguiente(); consumed = true; break;
            }
            if (consumed) {
                e.consume();
                return true;
            }
        }
        
        return false;
    } // --- Fin del método dispatchKeyEvent ---

    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if ("focusOwner".equals(evt.getPropertyName())) {
            Component newFocusOwner = (Component) evt.getNewValue();

            java.awt.Color accentColor = javax.swing.UIManager.getColor("Component.accentColor");
            if (accentColor == null) accentColor = new java.awt.Color(255, 153, 51);
            this.focusedBorder = javax.swing.BorderFactory.createLineBorder(accentColor, 2);

            JPanel panelListaArchivos = registry.get("panel.izquierdo.listaArchivos");
            
            // --- INICIO DE LA MODIFICACIÓN ---
            JPanel panelFiltrosActivos = registry.get("panel.izquierdo.filtros");
            // --- FIN DE LA MODIFICACIÓN ---

            for (javax.swing.JComponent panel : focusablePanels) {
                boolean debeTenerFoco = (newFocusOwner != null && (panel == newFocusOwner || SwingUtilities.isDescendingFrom(newFocusOwner, panel)));
                
                if (panel == panelListaArchivos && this.borderListaArchivosOriginal != null) {
                    if (debeTenerFoco) panel.setBorder(BorderFactory.createCompoundBorder(focusedBorder, this.borderListaArchivosOriginal));
                    else panel.setBorder(BorderFactory.createCompoundBorder(unfocusedBorder, this.borderListaArchivosOriginal));
                
                // --- INICIO DE LA MODIFICACIÓN ---
                } else if (panel == panelFiltrosActivos && this.borderFiltrosActivosOriginal != null) {
                    if (debeTenerFoco) panel.setBorder(BorderFactory.createCompoundBorder(focusedBorder, this.borderFiltrosActivosOriginal));
                    else panel.setBorder(BorderFactory.createCompoundBorder(unfocusedBorder, this.borderFiltrosActivosOriginal));
                // --- FIN DE LA MODIFICACIÓN ---

                } else {
                    panel.setBorder(debeTenerFoco ? focusedBorder : unfocusedBorder);
                }
            }
        }
    } // --- FIN del metodo propertyChange ---
    
    private void navegarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) modoController.navegarAnterior();
        else modoController.navegarSiguiente();
    } // --- FIN del metodo navegarSiguienteOAnterior ---

    private void registerFocusablePanel(String registryKey) {
        javax.swing.JComponent panel = registry.get(registryKey);
        if (panel != null) {
            focusablePanels.add(panel);
        } else {
            logger.warn("[FOCUS_INIT] No se pudo registrar el panel para foco: '{}'", registryKey);
        }
    } // --- FIN de metodo registerFocusablePanel ---

} // --- Fin de la clase GestorEntradaGlobal ---