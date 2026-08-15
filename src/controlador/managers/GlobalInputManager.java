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

import vista.theme.Tema;
import vista.theme.ThemeChangeListener;

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

import controlador.GridNavigationController;
import controlador.ProjectController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.IModoController;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;

/**
 * Gestor centralizado para los eventos de entrada globales de la aplicación.
 * Implementa la lógica para atajos de teclado globales, resaltado de foco
 * y la rueda del ratón universal, liberando a otros controladores de esta
 * responsabilidad.
 */
public class GlobalInputManager implements KeyEventDispatcher, PropertyChangeListener, ThemeChangeListener {

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
    private final List<String> focusablePanelKeys = new ArrayList<>();
    private TitledBorder borderListaArchivosOriginal;
    private TitledBorder borderFiltrosActivosOriginal;
    
    private int lastMouseX, lastMouseY;
    
    
    /**
     * Constructor para GestorEntradaGlobal.
     * Las dependencias se inyectan para desacoplar el gestor de otros componentes.
     */
    public GlobalInputManager() {
        // Constructor vacío. La inicialización se hace a través de setters y el método initialize.
    } // --- FIN de metodo GestorEntradaGlobal (constructor) ---

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
    } // --- FIN de metodo initialize ---
    
    /**
     * Configura los listeners globales de la aplicación (rueda del ratón, paneo)
     * y los asocia a los componentes correspondientes del registro.
     */
    public void configurarListeners() {
        logger.debug("[GestorEntradaGlobal] Configurando listeners de entrada globales...");

        java.awt.event.MouseWheelListener masterWheelListener = e -> {
            // Obtenemos las referencias a los JLabels de imagen UNA SOLA VEZ, al inicio del evento.
            Component etiquetaImagenVisualizador = registry.get("label.imagenPrincipal");
            Component etiquetaImagenProyecto = registry.get("label.proyecto.imagen");
            Component etiquetaImagenCarrusel = registry.get("label.carousel.imagen");
            Component etiquetaPolaroid = registry.get("label.polaroid.imagen");
            Component etiquetaPolaroidProyecto = registry.get("label.proyecto.polaroid.imagen");
            Component etiquetaPolaroidDatos = registry.get("label.datamode.polaroid.imagen");
            Component etiquetaImagenDatos = registry.get("label.datamode.imagen");
            Component etiquetaCliente = registry.get("label.cliente.imagen");
            Component etiquetaPolaroidCliente = registry.get("label.cliente.polaroid.imagen");
            Component sourceComponent = e.getComponent();

            // Comprobamos si el componente que originó el evento es uno de nuestros JLabels.
            // Esta es la forma más robusta de saber si estamos sobre la imagen.
            boolean sobreLaImagen = (etiquetaImagenVisualizador != null && sourceComponent == etiquetaImagenVisualizador) ||
                                    (etiquetaImagenProyecto != null && sourceComponent == etiquetaImagenProyecto) ||
                                    (etiquetaImagenCarrusel != null && sourceComponent == etiquetaImagenCarrusel) ||
                                    (etiquetaImagenDatos != null && sourceComponent == etiquetaImagenDatos) ||
                                    (etiquetaPolaroid != null && sourceComponent == etiquetaPolaroid) ||
                                    (etiquetaPolaroidProyecto != null && sourceComponent == etiquetaPolaroidProyecto) ||
                                    (etiquetaPolaroidDatos != null && sourceComponent == etiquetaPolaroidDatos) ||
                                    (etiquetaCliente != null && sourceComponent == etiquetaCliente) ||
                                    (etiquetaPolaroidCliente != null && sourceComponent == etiquetaPolaroidCliente);

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

            // Verificar si estamos sobre un grid configurado (tag GRID_NAVIGABLE) para scroll por filas.
            // El evento puede venir del JList directamente o de un hijo (cellRenderer panel),
            // así que buscamos el JList más cercano en la jerarquía.
            // Nota: SwingUtilities.getAncestorOfClass NO incluye el propio sourceComponent,
            // así que lo verificamos explícitamente.
            Component possibleJList = (sourceComponent instanceof JList)
                ? sourceComponent
                : SwingUtilities.getAncestorOfClass(JList.class, sourceComponent);
            if (possibleJList instanceof JList && esGridNavegable((JList<?>) possibleJList)) {
                GridNavigationController gridNav = crearGridNavigationController((JList<?>) possibleJList);
                // Un tick de rueda = una fila completa (equivalente al antiguo pasos = rotation * elementosPorFila).
                // Con circular=false el clamp produce EXACTAMENTE el mismo targetIndex que el antiguo
                // branch especial de DATOS (GlobalInputManager:251-258): clamp(current + rotation*columnas).
                gridNav.moveRow(e.getWheelRotation());
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

        // --- Lista de archivos (panel izquierdo): navegar items con la rueda en vez de scroll ---
        // El JList ya tiene masterWheelListener via WHEEL_NAVIGABLE.
        // Para el JScrollBar, añadimos un listener que navegue y consuma el evento.
        JScrollPane nameScrollPane = registry.get("scroll.nombresArchivo");
        if (nameScrollPane != null) {
            javax.swing.JScrollBar verticalBar = nameScrollPane.getVerticalScrollBar();
            if (verticalBar != null) {
                verticalBar.addMouseWheelListener(e -> {
                    if (e.getWheelRotation() < 0) modoController.navegarAnterior();
                    else modoController.navegarSiguiente();
                    e.consume();
                });
            }
        }
        // Idem para la lista de filtros activos
        JScrollPane filterScrollPane = registry.get("scroll.filtrosActivos");
        if (filterScrollPane != null) {
            javax.swing.JScrollBar verticalBar = filterScrollPane.getVerticalScrollBar();
            if (verticalBar != null) {
                verticalBar.addMouseWheelListener(e -> {
                    if (e.getWheelRotation() < 0) modoController.navegarAnterior();
                    else modoController.navegarSiguiente();
                    e.consume();
                });
            }
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
        Component etiquetaPolaroid = registry.get("label.polaroid.imagen");
        Component etiquetaPolaroidProyecto = registry.get("label.proyecto.polaroid.imagen");
        Component etiquetaPolaroidDatos = registry.get("label.datamode.polaroid.imagen");
        Component etiquetaImagenDatos = registry.get("label.datamode.imagen");
        Component etiquetaCliente = registry.get("label.cliente.imagen");
        Component etiquetaPolaroidCliente = registry.get("label.cliente.polaroid.imagen");

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
        if (etiquetaPolaroid != null) {
            etiquetaPolaroid.addMouseListener(paneoMouseAdapter);
            etiquetaPolaroid.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaPolaroidProyecto != null) {
            etiquetaPolaroidProyecto.addMouseListener(paneoMouseAdapter);
            etiquetaPolaroidProyecto.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaPolaroidDatos != null) {
            etiquetaPolaroidDatos.addMouseListener(paneoMouseAdapter);
            etiquetaPolaroidDatos.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaImagenDatos != null) {
            etiquetaImagenDatos.addMouseListener(paneoMouseAdapter);
            etiquetaImagenDatos.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaCliente != null) {
            etiquetaCliente.addMouseListener(paneoMouseAdapter);
            etiquetaCliente.addMouseMotionListener(paneoMouseMotionAdapter);
        }
        if (etiquetaPolaroidCliente != null) {
            etiquetaPolaroidCliente.addMouseListener(paneoMouseAdapter);
            etiquetaPolaroidCliente.addMouseMotionListener(paneoMouseMotionAdapter);
        }

        logger.debug("[GestorEntradaGlobal] Listeners configurados.");
    } // --- FIN de metodo configurarListeners ---

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
            case KeyEvent.VK_NUMPAD9: command = AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR; break;
            case KeyEvent.VK_NUMPAD0: command = AppActionCommands.CMD_ZOOM_RESET; break;
        }
        
        if (command != null) {
            if (e.getComponent() instanceof javax.swing.text.JTextComponent) return false;
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
            
            if (action != null) {
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                e.consume();
                return true;
            }
        }
        
        // Ctrl++ / Ctrl+-: cambiar tamaño de miniaturas (grid o tira), en cualquier modo
        if (e.isControlDown()) {
            Action action = null;
            if (e.getKeyCode() == KeyEvent.VK_ADD) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA);
            else if (e.getKeyCode() == KeyEvent.VK_SUBTRACT) action = actionMap.get(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA);
            
            if (action != null && action.isEnabled()) {
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

        // Navegación global: las teclas funcionan desde cualquier componente
        // excepto aquellos que las usan para su propia edición (texto, tablas, árboles)
        boolean focoEnComponenteConNavegacionPropia = (focusOwner instanceof javax.swing.text.JTextComponent
                || focusOwner instanceof javax.swing.JTable
                || focusOwner instanceof javax.swing.JTree);
        
        if (!focoEnComponenteConNavegacionPropia && !e.isShiftDown()) {
            boolean consumed = false;

            // --- Navegación de GRID: si el display actual es GRID y hay grid activo ---
            GridNavigationController gridNav = getGridNavSiActivo();
            if (gridNav != null && gridNav.getModelSize() > 0) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: gridNav.previousRow(); consumed = true; break;
                    case KeyEvent.VK_DOWN: gridNav.nextRow(); consumed = true; break;
                    case KeyEvent.VK_LEFT: gridNav.previous(); consumed = true; break;
                    case KeyEvent.VK_RIGHT: gridNav.next(); consumed = true; break;
                    case KeyEvent.VK_HOME: gridNav.first(); consumed = true; break;
                    case KeyEvent.VK_END: gridNav.last(); consumed = true; break;
                    case KeyEvent.VK_PAGE_UP: gridNav.previousPage(); consumed = true; break;
                    case KeyEvent.VK_PAGE_DOWN: gridNav.nextPage(); consumed = true; break;
                }
            } else {
                // --- Comportamiento 1×1 actual (lista maestra, carrusel, etc.) ---
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: case KeyEvent.VK_LEFT: modoController.navegarAnterior(); consumed = true; break;
                    case KeyEvent.VK_DOWN: case KeyEvent.VK_RIGHT: modoController.navegarSiguiente(); consumed = true; break;
                    case KeyEvent.VK_HOME: modoController.navegarPrimero(); consumed = true; break;
                    case KeyEvent.VK_END: modoController.navegarUltimo(); consumed = true; break;
                    case KeyEvent.VK_PAGE_UP: modoController.navegarBloqueAnterior(); consumed = true; break;
                    case KeyEvent.VK_PAGE_DOWN: modoController.navegarBloqueSiguiente(); consumed = true; break;
                }
            }
            if (consumed) {
                e.consume();
                return true;
            }
        }
        
        return false;
    } // --- FIN de metodo dispatchKeyEvent ---

    /**
     * Devuelve un {@link GridNavigationController} sobre el grid activo si el
     * display actual es GRID y el grid tiene modelo con elementos; si no, null.
     * Usado por el teclado para bifurcar la navegación 1×1 vs por-filas.
     * @return El controlador de navegación del grid activo, o null.
     */
    private GridNavigationController getGridNavSiActivo() {
        if (model == null || visorController == null || visorController.getDisplayModeManager() == null) return null;
        if (model.getCurrentDisplayMode() != DisplayMode.GRID) return null;
        javax.swing.JList<String> gridActivo = visorController.getDisplayModeManager().getActiveGridList();
        if (gridActivo == null || gridActivo.getModel().getSize() == 0) return null;
        return crearGridNavigationController(gridActivo);
    } // --- FIN de metodo getGridNavSiActivo ---

    /**
     * Comprueba si una JList está registrada como grid navegable (tag GRID_NAVIGABLE).
     * Evita deducir el comportamiento exclusivamente del layout (Precisión 1).
     * @param candidate La JList a comprobar.
     * @return {@code true} si está registrada con el tag GRID_NAVIGABLE.
     */
    private boolean esGridNavegable(JList<?> candidate) {
        if (registry == null || candidate == null) return false;
        List<Component> grids = registry.getComponentsByTag("GRID_NAVIGABLE");
        if (grids == null) return false;
        return grids.contains(candidate);
    } // --- FIN de metodo esGridNavegable ---

    /**
     * Crea un {@link GridNavigationController} sobre una JList configurándolo con
     * la circularidad y el salto de bloque actuales de la aplicación.
     * @param gridList La JList configurada como grid.
     * @return El controlador ya configurado.
     */
    private GridNavigationController crearGridNavigationController(JList<?> gridList) {
        GridNavigationController gridNav = new GridNavigationController(gridList);
        if (model != null) {
            gridNav.setCircular(model.isNavegacionCircularActivada());
        }
        if (visorController != null && visorController.getConfigurationManager() != null) {
            int saltoBloque = visorController.getConfigurationManager().getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
            gridNav.setPageScrollIncrement(saltoBloque);
        }
        return gridNav;
    } // --- FIN de metodo crearGridNavigationController ---

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
    } // --- FIN de metodo propertyChange ---
    
    private void navegarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) modoController.navegarAnterior();
        else modoController.navegarSiguiente();
    } // --- FIN de metodo navegarSiguienteOAnterior ---

    private void registerFocusablePanel(String registryKey) {
        javax.swing.JComponent panel = registry.get(registryKey);
        if (panel != null) {
            focusablePanels.add(panel);
            focusablePanelKeys.add(registryKey);
        } else {
            logger.warn("[FOCUS_INIT] No se pudo registrar el panel para foco: '{}'", registryKey);
        }
    } // --- FIN de metodo registerFocusablePanel ---
    
    /**
     * Refresca la lista de paneles con foco desde el registro.
     * Esencial tras un cambio de tema, donde los componentes se han recreado.
     */
    public void refreshFocusablePanels() {
        focusablePanels.clear();
        for (String key : focusablePanelKeys) {
            javax.swing.JComponent panel = registry.get(key);
            if (panel != null) {
                focusablePanels.add(panel);
            }
        }
        // Re-descubrir el JScrollPane de detalles de exportacion
        if (registry.get("panel.exportacion.detalles") instanceof javax.swing.JPanel detailPanel) {
            for (java.awt.Component comp : detailPanel.getComponents()) {
                if (comp instanceof javax.swing.JPanel) {
                    for (java.awt.Component innerComp : ((javax.swing.JPanel) comp).getComponents()) {
                        if (innerComp instanceof javax.swing.JScrollPane
                                && "scroll.detalles.exportacion".equals(innerComp.getName())) {
                            focusablePanels.add((javax.swing.JScrollPane) innerComp);
                            break;
                        }
                    }
                }
            }
        }
        logger.debug("  [GlobalInputManager] focusablePanels refrescados ({} paneles).", focusablePanels.size());
    } // --- FIN de metodo refreshFocusablePanels ---
    
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        refreshFocusablePanels();
    } // --- FIN de metodo onThemeChanged ---

} // --- FIN de clase GlobalInputManager ---