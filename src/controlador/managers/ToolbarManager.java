package controlador.managers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarAlignment;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

/**
 * Gestiona el ciclo de vida, la visibilidad y el posicionamiento de las
 * barras de herramientas (JToolBar) de la aplicación.
 */
public class ToolbarManager {

    // --- Dependencias ---
    private final ComponentRegistry registry;
    private final ConfigurationManager configuration;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefService;
    private final VisorModel model; // <-- NUEVO CAMPO

    // --- Estado Interno ---
    private final Map<String, JToolBar> managedToolbars;

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
            VisorModel model // <-- NUEVO PARÁMETRO
    ) {
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en ToolbarManager.");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null.");
        this.toolbarBuilder = Objects.requireNonNull(toolbarBuilder, "ToolbarBuilder no puede ser null.");
        this.uiDefService = Objects.requireNonNull(uiDefService, "UIDefinitionService no puede ser null.");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ToolbarManager."); // <-- ASIGNACIÓN
        
        this.managedToolbars = new ConcurrentHashMap<>();
        System.out.println("[ToolbarManager] Instancia creada con éxito.");
    } // --- Fin del método ToolbarManager (constructor) ---

    /**
     * Método helper que construye una JToolBar y le añade el listener de reconstrucción.
     *
     * @param def La definición de la barra de herramientas a construir.
     * @return La JToolBar construida y con el listener ya configurado.
     */

    private JToolBar buildAndConfigureToolbar(ToolbarDefinition def) {
        // 1. Construir la barra usando el builder.
        final JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
        
        // --- INICIO DE LA LÓGICA BASADA EN ANCESTORLISTENER (VERSIÓN FINAL) ---
        System.out.println("  [DEBUG ToolbarManager] Añadiendo AncestorListener a la barra: '" + toolbar.getName() + "'");

        // Inicializamos nuestro flag personalizado en la propia barra de herramientas.
        toolbar.putClientProperty("isCurrentlyFloating", false);

        toolbar.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                // Este evento se dispara cuando la barra es añadida a un contenedor.
                
                // Obtenemos la ventana a la que pertenece la barra.
                java.awt.Window windowAncestor = SwingUtilities.getWindowAncestor(toolbar);

                // Si la ventana es un JDialog, significa que la barra se ha vuelto flotante.
                // (La ventana principal es un JFrame, no un JDialog).
                if (windowAncestor instanceof javax.swing.JDialog) {
                    System.out.println("  [AncestorListener] La barra '" + toolbar.getName() + "' ha sido añadida a un JDialog. Ahora es flotante.");
                    // Establecemos nuestro flag para saber que está en estado flotante.
                    toolbar.putClientProperty("isCurrentlyFloating", true);
                }
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                // Este evento se dispara cuando se quita la barra de un contenedor.
                
                // Leemos nuestro flag. Si es TRUE, significa que la barra estaba flotando
                // y ahora está siendo retirada de su JDialog flotante (porque se ha cerrado).
                // ¡Este es nuestro disparador!
                Boolean estabaFlotando = (Boolean) toolbar.getClientProperty("isCurrentlyFloating");

                if (Boolean.TRUE.equals(estabaFlotando)) {
                    System.out.println("  [AncestorListener] La barra flotante '" + toolbar.getName() + "' ha sido cerrada.");
                    System.out.println("    >>> CONDICIÓN DE RECONSTRUCCIÓN CUMPLIDA para '" + toolbar.getName() + "' <<<");

                    // Inmediatamente ponemos el flag a false para el siguiente ciclo.
                    toolbar.putClientProperty("isCurrentlyFloating", false);
                    
                    // Disparamos la reconstrucción completa y ordenada de todas las barras.
                    SwingUtilities.invokeLater(() -> {
                        ToolbarManager.this.reconstruirContenedorDeToolbars(
                            model.getCurrentWorkMode()
                        );
                    });
                }
            }

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {
                // No nos interesa este evento.
            }
        });

        return toolbar;
        
    } // --- Fin del método buildAndConfigureToolbar ---
    

    /**
     * Orquesta la reconstrucción completa del contenedor de barras de herramientas.
     */
    public void reconstruirContenedorDeToolbars(WorkMode modoActual) {
        System.out.println("\n--- [ToolbarManager] Iniciando reconstrucción del contenedor de toolbars para el modo: " + modoActual + " ---");

        final JPanel leftPanel = (JPanel) registry.get("container.toolbars.left");
        final JPanel centerPanel = (JPanel) registry.get("container.toolbars.center");
        final JPanel rightPanel = (JPanel) registry.get("container.toolbars.right");

        if (leftPanel == null || centerPanel == null || rightPanel == null) {
            System.err.println("  ERROR [ToolbarManager]: Uno o más paneles de alineamiento no se encontraron. Abortando.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            // Limpiamos los contenedores visuales. Las instancias de JToolBar siguen vivas en 'managedToolbars'.
            leftPanel.removeAll();
            centerPanel.removeAll();
            rightPanel.removeAll();

            List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();

            for (ToolbarDefinition def : todasLasBarras) {
                if ("controles_imagen_inferior".equals(def.claveBarra())) {
                    continue;
                }

                if (def.modosVisibles().contains(modoActual)) {
                    // Obtenemos la barra. Si no existe, se crea y se guarda en el mapa.
                    JToolBar toolbar = getToolbar(def.claveBarra()); 

                    if (toolbar != null) {
                        String configKeyVisibilidad = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra(), "visible");
                        boolean isVisibleInConfig = configuration.getBoolean(configKeyVisibilidad, true);
                        toolbar.setVisible(isVisibleInConfig);
                        
                        // Reposicionamos la barra existente en el panel correcto.
                        ToolbarAlignment alignment = def.alignment();
                        switch (alignment) {
                            case LEFT: leftPanel.add(toolbar); break;
                            case CENTER: centerPanel.add(toolbar); break;
                            case RIGHT: rightPanel.add(toolbar); break;
                            default: leftPanel.add(toolbar); break;
                        }
                    }
                }
            }

            leftPanel.revalidate();
            leftPanel.repaint();
            centerPanel.revalidate();
            centerPanel.repaint();
            rightPanel.revalidate();
            rightPanel.repaint();

            System.out.println("--- [ToolbarManager] Reconstrucción de toolbars en EDT completada. ---");
        });
    } // --- Fin del método reconstruirContenedorDeToolbars ---
    
    
    /**
     * Obtiene una barra de herramientas específica por su clave. Si no existe en el
     * caché interno (managedToolbars), la construye, le añade los listeners y la guarda.
     *
     * @param claveBarra La clave de la barra a obtener.
     * @return La instancia de JToolBar, ya sea cacheada o recién creada.
     */
    public JToolBar getToolbar(String claveBarra) {
        // Comprueba si la barra ya existe en nuestro mapa.
        if (!this.managedToolbars.containsKey(claveBarra)) {
            System.out.println("  [ToolbarManager getToolbar] La barra '" + claveBarra + "' no está en caché. Construyéndola ahora...");
            
            // Busca la definición correspondiente a la clave.
            uiDefService.generateModularToolbarStructure().stream()
                .filter(def -> def.claveBarra().equals(claveBarra))
                .findFirst()
                .ifPresent(def -> {
                    // Si se encuentra la definición, se construye y configura.
                    JToolBar newToolbar = buildAndConfigureToolbar(def);
                    // Se guarda en el mapa para futuras reutilizaciones.
                    this.managedToolbars.put(claveBarra, newToolbar);
                });
        }
        // Devuelve la barra del mapa (ya sea la que existía o la que acabamos de crear).
        return this.managedToolbars.get(claveBarra);
    } // --- Fin del método getToolbar ---
    

    /**
     * Devuelve un mapa inmutable de las barras de herramientas actualmente gestionadas.
     */
    public Map<String, JToolBar> getManagedToolbars() {
        return java.util.Collections.unmodifiableMap(this.managedToolbars);
    } // --- Fin del método getManagedToolbars ---

} // --- FIN de la clase ToolbarManager ---

