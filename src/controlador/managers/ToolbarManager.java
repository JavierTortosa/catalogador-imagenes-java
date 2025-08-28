package controlador.managers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarAlignment;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.theme.Tema;
import vista.theme.ThemeChangeListener;

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
    
    /**
     * Constructor de ToolbarManager.
     *
     * @param registry El registro de componentes para acceder a los paneles contenedores.
     * @param configuration El gestor de configuración para leer el estado de visibilidad.
     * @param toolbarBuilder El constructor para crear instancias de JToolBar.
     * @param uiDefService El servicio que define la estructura de las toolbars.
     * @param model El modelo de la aplicación para obtener el modo de trabajo actual.
     */
    public ToolbarManager(
            ComponentRegistry registry,
            ConfigurationManager configuration,
            ToolbarBuilder toolbarBuilder,
            UIDefinitionService uiDefService,
            VisorModel model 
    ) {
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

        List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();

        // Obtenemos el DisplayMode actual desde el modelo. Es la fuente de la verdad.
        DisplayMode displayModeActual = model.getCurrentDisplayMode();
        
        for (ToolbarDefinition def : todasLasBarras) {
            if (def.modosVisibles().contains(modoActual)) {
                // Obtenemos la toolbar. Como la caché está vacía tras un cambio de tema,
                // esto forzará la llamada a buildAndConfigureToolbar.
                JToolBar toolbar = getToolbar(def.claveBarra()); 

                if (toolbar != null) {
                    	
                	if (def.alignment() != ToolbarAlignment.FREE) {
                		
                        String configKeyVisibilidad = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra(), "visible");
                        boolean isVisibleInConfig = configuration.getBoolean(configKeyVisibilidad, true);
                        toolbar.setVisible(isVisibleInConfig);
                        toolbar.setOpaque(false);
                        
                        // <<< INICIO DE LA LÓGICA DE VISIBILIDAD ADICIONAL >>>
                        // Si la barra es la de "zoom", aplicamos una regla extra.
                        if ("zoom".equals(def.claveBarra())) {
                            // La barra de zoom solo debe ser visible si estamos en un modo de imagen única
                            // Y si la configuración general de la barra dice que debe ser visible.
                            boolean debeSerVisible = (displayModeActual != DisplayMode.GRID) && isVisibleInConfig;
                            toolbar.setVisible(debeSerVisible);
                            logger.debug("  -> Visibilidad condicional para 'zoom': " + debeSerVisible);
                        }
                        // <<< FIN DE LA LÓGICA DE VISIBILIDAD ADICIONAL >>>
                        
                        switch (def.alignment()) {
                            case LEFT: leftPanel.add(toolbar); break;
                            case CENTER: centerPanel.add(toolbar); break;
                            case RIGHT: rightPanel.add(toolbar); break;
                            default: leftPanel.add(toolbar); break;
                        }
                    }
                }
            }
        }
        
        // <<< INICIO DE LA LÓGICA PARA LA BARRA DE ESTADO >>>
        // Gestionamos la barra de estado por separado al final, porque siempre existe.
        JToolBar barraEstadoControles = getToolbar("barra_estado_controles");
        if (barraEstadoControles != null) {
            boolean debeSerVisible = (displayModeActual != DisplayMode.GRID);
            barraEstadoControles.setVisible(debeSerVisible);
            logger.debug("  -> Visibilidad condicional para 'barra_estado_controles': " + debeSerVisible);
        }
        // <<< FIN DE LA LÓGICA PARA LA BARRA DE ESTADO >>>

        
//       // --- INICIO DE LA MODIFICACIÓN (CON LOGS DE DIAGNÓSTICO) ---
//        ToolbarManager toolbarManager = this.generalController.getToolbarManager();
//        if (toolbarManager != null) {
//            logger.debug("[ProjectBuilder] Obteniendo 'barra_grid' desde ToolbarManager...");
//            JToolBar gridToolbar = toolbarManager.getToolbar("barra_grid");
//            
//            if (gridToolbar != null) {
//                logger.info(">>> ÉXITO: Toolbar 'barra_grid' obtenida. Inyectando en GridDisplayPanel. <<<");
//                gridViewPanel.setToolbar(gridToolbar);
//            } else {
//                logger.error(">>> FALLO: ToolbarManager devolvió NULL para 'barra_grid'. La toolbar no se añadirá. <<<");
//            }
//        } else {
//            logger.error("[ProjectBuilder] ToolbarManager es NULL. No se puede obtener la 'barra_grid'.");
//        }
//        // --- FIN DE LA MODIFICACIÓN ---
        
        
        leftPanel.revalidate();
        leftPanel.repaint();
        centerPanel.revalidate();
        centerPanel.repaint();
        rightPanel.revalidate();
        rightPanel.repaint();

        
	     // --- INICIO DE LA LÓGICA AÑADIDA ---
	     // Después de que TODAS las toolbars han sido reconstruidas y puestas en su sitio,
	     // le decimos explícitamente al BackgroundControlManager que es su turno de actuar.
	     if (this.backgroundControlManager != null) {
	         logger.debug("  [ToolbarManager] Notificando a BackgroundControlManager para que se re-inicialice...");
	         // Lo hacemos en un invokeLater para asegurar que se ejecuta después de que
	         // el contenedor de toolbars se haya revalidado visualmente.
	         SwingUtilities.invokeLater(() -> {
	             backgroundControlManager.initializeAndLinkControls();
	             backgroundControlManager.sincronizarSeleccionConEstadoActual();
	         });
	     }
	     // --- FIN DE LA LÓGICA AÑADIDA ---
	     
        
        logger.debug("--- [ToolbarManager] Reconstrucción de toolbars completada. ---");
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
    
    
    public void setBackgroundControlManager(BackgroundControlManager backgroundControlManager) {
        this.backgroundControlManager = backgroundControlManager;
    } // --- Fin del método setBackgroundControlManager ---

} // --- FIN de la clase ToolbarManager ---

