package controlador.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton; // <<< AÑADIR IMPORT
import javax.swing.JPanel;
import javax.swing.JToolBar;

import controlador.utils.ComponentRegistry; // <<< AÑADIR IMPORT
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

public class ToolbarManager {

    // --- Dependencias Clave ---
//    private final VisorView view;
    private final ConfigurationManager config;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefService;
    private final ComponentRegistry registry; // <<< AÑADIR CAMPO PARA EL REGISTRO

    // --- Estado Interno ---
    private final Map<String, JToolBar> managedToolbars = new HashMap<>();

    // --- CONSTRUCTOR REFACTORIZADO ---
    public ToolbarManager(
//            VisorView view, 
            ConfigurationManager config, 
            ToolbarBuilder builder, 
            UIDefinitionService uiDefService,
            ComponentRegistry registry // <<< AÑADIR PARÁMETRO
    ) {
//        this.view = view;
        this.config = config;
        this.toolbarBuilder = builder;
        this.uiDefService = uiDefService;
        this.registry = registry; // <<< ASIGNAR DEPENDENCIA
    } // --- Fin del constructor ToolbarManager ---

    /**
     * Construye todas las barras de herramientas modulares, las añade a la vista,
     * registra sus componentes y establece su visibilidad inicial.
     */
    public void inicializarBarrasDeHerramientas() {
        System.out.println("[ToolbarManager] Inicializando barras de herramientas...");

        // <<< NUEVO: Obtener el panel contenedor desde el registro >>>
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer == null) {
            System.err.println("ERROR CRÍTICO [ToolbarManager]: No se encontró 'container.toolbars' en el registro.");
            return;
        }

        List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();

        for (ToolbarDefinition def : todasLasBarras) {
            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
            managedToolbars.put(def.claveBarra(), toolbar);
            registry.register("toolbar." + def.claveBarra(), toolbar);
            
            // <<< CAMBIO: Añadir la barra directamente al contenedor >>>
            toolbarContainer.add(toolbar);

            String configKey = "interfaz.herramientas." + def.claveBarra() + ".visible";
            boolean esVisible = config.getBoolean(configKey, true);
            toolbar.setVisible(esVisible);
        }
        
        System.out.println("  [ToolbarManager] Registrando botones individuales...");
        Map<String, JButton> botonesCreados = toolbarBuilder.getBotonesPorNombre();
        botonesCreados.forEach(registry::register);
        System.out.println("    -> " + botonesCreados.size() + " botones registrados.");

        // <<< CAMBIO: Revalidar el contenedor directamente >>>
        toolbarContainer.revalidate();
        toolbarContainer.repaint();
        
        System.out.println("[ToolbarManager] Inicialización de barras completada.");
    } // --- Fin del método inicializarBarrasDeHerramientas ---

    /**
     * Refresca la visibilidad de todas las barras de herramientas gestionadas
     * leyendo su estado actual desde la configuración.
     */
    public void refrescarVisibilidadBarras() {
        // ... (este método no necesita cambios, ya funciona bien) ...
    } // --- Fin del método refrescarVisibilidadBarras ---

} // --- Fin de la clase ToolbarManager ---