package controlador.managers;

import java.awt.Component;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.HotspotDefinition;
import vista.config.ToolbarAlignment;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;
import vista.util.IconUtils;

/**
 * Gestiona el ciclo de vida, la visibilidad y el posicionamiento de las
 * barras de herramientas (JToolBar) de la aplicación.
 */
public class ToolbarManager implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(ToolbarManager.class);
	
    // --- Dependencias ---
    private final ComponentRegistry registry;
    private final ConfigurationManager configuration;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefService;
    private final VisorModel model; 

    // --- Estado Interno ---
    private final Map<String, JToolBar> managedToolbars;
    
    private BackgroundControlManager backgroundControlManager;
    private IconUtils iconUtils;

    // --- Estado de overflow ---
    private IViewManager viewManager;
    private final List<String> overflowButtonCommands = new ArrayList<>();
    private final Map<String, Icon> overflowButtonIcons = new HashMap<>();
    private javax.swing.Timer overflowTimer;
    private boolean overflowListenersInicializados = false;
    private boolean previousOverflowState = false;
    private JButton overflowButton;
    private static final String OVERFLOW_TOOLBAR_KEY = "especiales";

    public ToolbarManager(ComponentRegistry registry, ConfigurationManager configuration, ToolbarBuilder toolbarBuilder, UIDefinitionService uiDefService, VisorModel model) {
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en ToolbarManager.");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null.");
        this.toolbarBuilder = Objects.requireNonNull(toolbarBuilder, "ToolbarBuilder no puede ser null.");
        this.uiDefService = Objects.requireNonNull(uiDefService, "UIDefinitionService no puede ser null.");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ToolbarManager."); 
        
        this.managedToolbars = new ConcurrentHashMap<>();
        logger.debug("[ToolbarManager] Instancia creada con éxito.");
    } // --- Fin del método ToolbarManager (constructor) ---



    
    /**
     * Se invoca cuando el ThemeManager notifica un cambio de tema.
     * La única responsabilidad de este método es invalidar todas las toolbars
     * existentes limpiando la caché. Esto fuerza a que se reconstruyan con
     * el nuevo tema la próxima vez que se soliciten.
     *
     * @param nuevoTema El tema que acaba de ser aplicado.
     */
    @Override
    public void onThemeChanged(Tema nuevoTema) {
        logger.debug("--- [ToolbarManager] Notificación de cambio de tema recibida. Limpiando caché de toolbars... ---");
        this.clearToolbarCache();
        // Nota: La reconstrucción REAL será disparada por otro listener (como ViewManager o VisorController).
        // Este manager solo prepara el terreno para la reconstrucción.
    } // --- Fin del método onThemeChanged ---
    
    
    /**
     * Método helper que construye una JToolBar y le añade el listener de reconstrucción.
     *
     * @param def La definición de la barra de herramientas a construir.
     * @return La JToolBar construida y con el listener ya configurado.
     */
    private JToolBar buildAndConfigureToolbar(ToolbarDefinition def) {
        final JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
        
        logger.debug("  [DEBUG ToolbarManager] Añadiendo AncestorListener a la barra: '" + toolbar.getName() + "'");
        toolbar.putClientProperty("isCurrentlyFloating", false);
        toolbar.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                java.awt.Window windowAncestor = SwingUtilities.getWindowAncestor(toolbar);
                if (windowAncestor instanceof javax.swing.JDialog) {
                    toolbar.putClientProperty("isCurrentlyFloating", true);
                }
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                Boolean estabaFlotando = (Boolean) toolbar.getClientProperty("isCurrentlyFloating");
                if (Boolean.TRUE.equals(estabaFlotando)) {
                    logger.debug("  [AncestorListener] La barra flotante '" + toolbar.getName() + "' ha sido cerrada. Disparando reconstrucción...");
                    toolbar.putClientProperty("isCurrentlyFloating", false);
                    SwingUtilities.invokeLater(() -> {
                        ToolbarManager.this.reconstruirContenedorDeToolbars(
                            model.getCurrentWorkMode()
                        );
                    });
                }
            }
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });
        return toolbar;
    } // --- Fin del método buildAndConfigureToolbar ---
    
    
    public void reconstruirContenedorDeToolbars(WorkMode modoActual) {
        logger.info("--- [ToolbarManager] Iniciando reconstrucción del contenedor de toolbars para el modo: " + modoActual + " ---");

        final JPanel leftPanel = registry.get("container.toolbars.left");
        final JPanel centerPanel = registry.get("container.toolbars.center");
        final JPanel rightPanel = registry.get("container.toolbars.right");

        if (leftPanel == null || centerPanel == null || rightPanel == null) {
            logger.error("  ERROR [ToolbarManager]: Uno o más paneles de alineamiento no se encontraron. Abortando.");
            return;
        }

        leftPanel.removeAll();
        centerPanel.removeAll();
        rightPanel.removeAll();

        List<ToolbarDefinition> todasLasBarras = new java.util.ArrayList<>(uiDefService.generateModularToolbarStructure());
        todasLasBarras.sort(java.util.Comparator.comparingInt(ToolbarDefinition::orden));

        DisplayMode displayModeActual = model.getCurrentDisplayMode();
        
        for (ToolbarDefinition def : todasLasBarras) {
            if (!def.modosVisibles().contains(modoActual)) continue;
            if (def.alignment() == ToolbarAlignment.FREE) continue;
            JToolBar toolbar = getToolbar(def.claveBarra()); 

            if (toolbar != null) {
                String configKeyVisibilidad = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra(), "visible");
                boolean isVisibleInConfig = configuration.getBoolean(configKeyVisibilidad, true);
                toolbar.setVisible(isVisibleInConfig);
                toolbar.setOpaque(false);
                
                if ("zoom".equals(def.claveBarra())) {
                    boolean esModoDatos = (modoActual == WorkMode.DATOS);
                    boolean debeSerVisible = ((displayModeActual != DisplayMode.GRID) || esModoDatos) && isVisibleInConfig;
                    toolbar.setVisible(debeSerVisible);
                    logger.debug("  -> Visibilidad condicional para 'zoom': " + debeSerVisible);
                }
                
                switch (def.alignment()) {
                    case LEFT: leftPanel.add(toolbar); break;
                    case CENTER: centerPanel.add(toolbar); break;
                    case RIGHT: rightPanel.add(toolbar); break;
                    default: leftPanel.add(toolbar); break;
                }
            }
        }
        
        JToolBar barraEstadoControles = getToolbar("barra_estado_controles");
        if (barraEstadoControles != null) {
            boolean debeSerVisible = (displayModeActual != DisplayMode.GRID);
            barraEstadoControles.setVisible(debeSerVisible);
            logger.debug("  -> Visibilidad condicional para 'barra_estado_controles': " + debeSerVisible);
        }

        leftPanel.revalidate();
        leftPanel.repaint();
        centerPanel.revalidate();
        centerPanel.repaint();
        rightPanel.revalidate();
        rightPanel.repaint();

	     if (this.backgroundControlManager != null) {
	         logger.debug("  [ToolbarManager] Notificando a BackgroundControlManager para que se re-inicialice...");
	         SwingUtilities.invokeLater(() -> {
	             backgroundControlManager.initializeAndLinkControls();
	             backgroundControlManager.sincronizarSeleccionConEstadoActual();
	         });
        // ... (rest of loop)
        }
        
        // Añadir botón de desbordamiento al panel derecho
        this.overflowButton = new JButton();
        this.overflowButton.setActionCommand(AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
        this.overflowButton.setToolTipText("Más Opciones");
        if (this.iconUtils != null) {
            this.overflowButton.setIcon(this.iconUtils.getScaledIcon("6003-botones_ocultos_48x48.png",
                    getConfiguredIconWidth(), getConfiguredIconHeight()));
        }
        this.overflowButton.setMargin(new Insets(2, 2, 2, 2));
        this.overflowButton.setVisible(false);
        rightPanel.add(this.overflowButton);

        inicializarOverflowListeners();
        javax.swing.Timer timer = new javax.swing.Timer(200, e -> aplicarOverflow());
        timer.setRepeats(false);
        timer.start();

        logger.debug("--- [ToolbarManager] Reconstrucción de toolbars completada. Aplicación de overflow programada en 200ms. ---");
    } // --- Fin del método reconstruirContenedorDeToolbars ---
    
    
    
    /**
     * Obtiene una barra de herramientas específica por su clave. Si no existe en el
     * caché interno (managedToolbars), la construye, le añade los listeners y la guarda.
     *
     * @param claveBarra La clave de la barra a obtener.
     * @return La instancia de JToolBar, ya sea cacheada o recién creada.
     */
    public JToolBar getToolbar(String claveBarra) {
        if (!this.managedToolbars.containsKey(claveBarra)) {
            logger.debug("  [ToolbarManager getToolbar] La barra '" + claveBarra + "' no está en caché. Construyéndola ahora...");
            
            uiDefService.generateModularToolbarStructure().stream()
                .filter(def -> def.claveBarra().equals(claveBarra))
                .findFirst()
                .ifPresent(def -> {
                    JToolBar newToolbar = buildAndConfigureToolbar(def);
                    this.managedToolbars.put(claveBarra, newToolbar);
                    
                    // Registramos la toolbar para que otros la encuentren.
                    String registryKey = "toolbar." + claveBarra;
                    this.registry.register(registryKey, newToolbar);
                    logger.debug("    -> Barra '" + claveBarra + "' registrada en ComponentRegistry con la clave: '" + registryKey + "'");
                });
        }
        return this.managedToolbars.get(claveBarra);
    } // --- Fin del método getToolbar ---
    
    
    /**
     * Devuelve un mapa inmutable de las barras de herramientas actualmente gestionadas.
     */
    public Map<String, JToolBar> getManagedToolbars() {
        return java.util.Collections.unmodifiableMap(this.managedToolbars);
    } // --- Fin del método getManagedToolbars ---
    
    
    
    /**
     * Limpia el caché interno de barras de herramientas. Esto forzará a que
     * se reconstruyan completamente la próxima vez que se llame a getToolbar()
     * o a reconstruirContenedorDeToolbars(). Esencial para el cambio de tema.
     */
    public void clearToolbarCache() {
        logger.debug("  [ToolbarManager] Limpiando caché de toolbars (" + managedToolbars.size() + " barras)...");
        managedToolbars.clear();
    } // --- Fin del método clearToolbarCache ---
    
    
    public void setViewManager(IViewManager viewManager) {
        this.viewManager = viewManager;
    } // --- Fin del método setViewManager ---

    public void setBackgroundControlManager(BackgroundControlManager backgroundControlManager) {
        this.backgroundControlManager = backgroundControlManager;
    } // --- Fin del método setBackgroundControlManager ---

    public void setIconUtils(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
    } // --- Fin del método setIconUtils ---
    
    /**
     * Sincroniza el estado de habilitación/deshabilitación de los botones de la toolbar
     * basándose en el modo de visualización y la selección de imágenes.
     * 
     * @param actionMap El mapa de acciones de la aplicación.
     * @param model El modelo de la aplicación.
     */
    public void sincronizarEstadoBotonesToolbar(Map<String, Action> actionMap, VisorModel model) {
        if (actionMap == null || model == null) return;

        DisplayMode displayMode = model.getCurrentDisplayMode();
        boolean hasSelection = model.getSelectedImageKey() != null;

        // 1. Botones Single y Polaroid: Solo activos si hay selección.
        // En modo PROYECTO y CLIENTE siempre están activos.
        if (model.getCurrentWorkMode() != WorkMode.PROYECTO && model.getCurrentWorkMode() != WorkMode.CLIENTE) {
            Action singleAction = actionMap.get(AppActionCommands.CMD_VISTA_SINGLE);
            Action polaroidAction = actionMap.get(AppActionCommands.CMD_VISTA_POLAROID);
            if (singleAction != null) singleAction.setEnabled(hasSelection);
            if (polaroidAction != null) polaroidAction.setEnabled(hasSelection);
        }

        // 2. Botones de Zoom de Grid (Tamaño de miniatura): Solo activos en modo GRID.
        Action gridZoomUp = actionMap.get(AppActionCommands.CMD_GRID_SIZE_UP_MINIATURA);
        Action gridZoomDown = actionMap.get(AppActionCommands.CMD_GRID_SIZE_DOWN_MINIATURA);
        boolean isGrid = (displayMode == DisplayMode.GRID);
        if (gridZoomUp != null) gridZoomUp.setEnabled(isGrid);
        if (gridZoomDown != null) gridZoomDown.setEnabled(isGrid);

        // 3. Botones de Zoom de Toolbar (Modo/Porcentaje): Solo activos en SINGLE o POLAROID.
        // Deshabilitamos todas las acciones cuya clave empiece con "cmd.zoom"
        // para cubrir todos los botones de la barra de zoom (acercar, alejar, modos, etc.).
        
        boolean isNotGrid = (displayMode != DisplayMode.GRID);
        
        for (String cmd : actionMap.keySet()) {
            if (cmd != null && cmd.startsWith("cmd.zoom")) {
                Action action = actionMap.get(cmd);
                if (action != null) {
                    action.setEnabled(isNotGrid);
                }
            }
        }
        
        logger.debug("[ToolbarManager] Sincronización de botones completada. Modo: {}, Selección: {}", displayMode, hasSelection);
    } // --- Fin del método sincronizarEstadoBotonesToolbar ---

    /**
     * Inicializa los listeners de redimension en los paneles de toolbars
     * para detectar overflow y mostrar/ocultar el botón de desbordamiento.
     * Solo se ejecuta una vez (bandera overflowListenersInicializados).
     */
    public void inicializarOverflowListeners() {
        if (overflowListenersInicializados)
            return;

        String[] panelKeys = { "container.toolbars.left", "container.toolbars.center", "container.toolbars.right" };
        for (String key : panelKeys) {
            JPanel panel = this.registry.get(key);
            if (panel != null) {
                panel.addComponentListener(new java.awt.event.ComponentAdapter() {
                    @Override
                    public void componentResized(java.awt.event.ComponentEvent e) {
                        programarOverflow();
                    }
                });
            }
        }
        overflowListenersInicializados = true;
        logger.debug("[ToolbarManager] Listeners de overflow inicializados.");
    } // --- Fin del método inicializarOverflowListeners ---

    private void programarOverflow() {
        if (overflowTimer == null) {
            overflowTimer = new javax.swing.Timer(150, e -> aplicarOverflow());
            overflowTimer.setRepeats(false);
        }
        overflowTimer.restart();
    } // --- Fin del método programarOverflow ---

    public void aplicarOverflow() {
        overflowButtonCommands.clear();
        overflowButtonIcons.clear();

        verificarOverflowEnPanel(registry.get("container.toolbars.right"));
        verificarOverflowEnPanel(registry.get("container.toolbars.left"));
        verificarOverflowEnPanel(registry.get("container.toolbars.center"));

        boolean hasHidden = !overflowButtonCommands.isEmpty();
        if (hasHidden != previousOverflowState) {
            previousOverflowState = hasHidden;
            if (overflowTimer != null) {
                overflowTimer.stop();
            }
            if (this.viewManager != null) {
                this.viewManager.setEspecialOverflowButtonVisible(hasHidden);
            }
        }
    } // --- Fin del método aplicarOverflow ---

    private void verificarOverflowEnPanel(JPanel panel) {
        if (panel == null || panel.getWidth() <= 0)
            return;

        int firstRowY = -1;
        boolean hayOverflow = false;

        for (Component comp : panel.getComponents()) {
            if (!(comp instanceof JToolBar))
                continue;

            JToolBar toolbar = (JToolBar) comp;
            if (!toolbar.isVisible() || OVERFLOW_TOOLBAR_KEY.equals(toolbar.getName())) {
                continue;
            }

            int x = toolbar.getX();
            int y = toolbar.getY();
            boolean toolbarFueraDelPanel = x < 0
                    || x + toolbar.getWidth() > panel.getWidth()
                    || y + toolbar.getHeight() > panel.getHeight()
                    || toolbar.getWidth() > panel.getWidth();

            if (firstRowY == -1) {
                firstRowY = y;
            }

            if (y > firstRowY || toolbarFueraDelPanel) {
                hayOverflow = true;
            }

            if (hayOverflow) {
                addOverflowCommandsFromToolbar(toolbar);
            }
        }
    } // --- Fin del método verificarOverflowEnPanel ---

    private void addOverflowCommandsFromToolbar(JToolBar toolbar) {
        Object definition = toolbar.getClientProperty("toolbarDefinition");

        if (definition instanceof ToolbarDefinition toolbarDefinition && toolbarDefinition.componentes() != null) {
            for (ToolbarComponentDefinition componentDefinition : toolbarDefinition.componentes()) {
                if (componentDefinition instanceof ToolbarButtonDefinition buttonDefinition) {
                    List<HotspotDefinition> hotspots = buttonDefinition.listaDeHotspots();
                    if (hotspots != null && !hotspots.isEmpty()) {
                        for (HotspotDefinition hotspot : hotspots) {
                            addOverflowCommand(hotspot.comando(), buildIcon(hotspot.icono(), hotspot.scope()));
                        }
                    } else {
                        addOverflowCommand(buttonDefinition.comandoCanonico(),
                                buildIcon(buttonDefinition.claveIcono(), buttonDefinition.scopeIconoBase()));
                    }
                }
            }
            return;
        }

        for (Component child : toolbar.getComponents()) {
            if (child instanceof AbstractButton btn) {
                addOverflowCommand(btn.getActionCommand(), btn.getIcon());
            }
        }
    }

    private Icon buildIcon(String iconKey, vista.config.IconScope scope) {
        if (this.iconUtils == null || iconKey == null || iconKey.isBlank()) {
            return null;
        }

        return scope == vista.config.IconScope.COMMON
                ? this.iconUtils.getScaledCommonIcon(iconKey, getConfiguredIconWidth(), getConfiguredIconHeight())
                : this.iconUtils.getScaledIcon(iconKey, getConfiguredIconWidth(), getConfiguredIconHeight());
    }

    private int getConfiguredIconWidth() {
        return this.configuration.getInt(ConfigKeys.ICONOS_ANCHO, 24);
    }

    private int getConfiguredIconHeight() {
        return this.configuration.getInt(ConfigKeys.ICONOS_ALTO, 24);
    }

    private void addOverflowCommand(String cmd, Icon icon) {
        if (cmd != null && !cmd.isEmpty()
                && !AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS.equals(cmd)
                && !overflowButtonCommands.contains(cmd)) {
            overflowButtonCommands.add(cmd);
            if (icon != null) {
                overflowButtonIcons.put(cmd, icon);
            }
        }
    }

    /**
     * Devuelve una copia de la lista de comandos de botones actualmente ocultos por overflow.
     */
    public List<String> getHiddenButtonCommands() {
        return new ArrayList<>(this.overflowButtonCommands);
    } // --- Fin del método getHiddenButtonCommands ---

    public Icon getHiddenButtonIcon(String command) {
        return this.overflowButtonIcons.get(command);
    } // --- Fin del método getHiddenButtonIcon ---

} // --- FIN de la clase ToolbarManager ---

