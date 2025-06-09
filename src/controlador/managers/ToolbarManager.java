package controlador.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JToolBar;

import servicios.ConfigurationManager;
import vista.VisorView;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

public class ToolbarManager {

    private final VisorView view;
    private final ConfigurationManager config;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefService;
    
    private final Map<String, JToolBar> managedToolbars = new HashMap<>();

    public ToolbarManager(VisorView view, ConfigurationManager config, ToolbarBuilder builder, UIDefinitionService uiDefService) {
        this.view = view;
        this.config = config;
        this.toolbarBuilder = builder;
        this.uiDefService = uiDefService;
    }

    /**
     * Construye todas las barras de herramientas modulares, las añade a la vista y
     * establece su visibilidad inicial según la configuración.
     */
    public void inicializarBarrasDeHerramientas() {
        System.out.println("[ToolbarManager] Inicializando barras de herramientas...");

        List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();

        for (ToolbarDefinition def : todasLasBarras) {
            // 1. Construir la barra
            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
            
            // 2. Guardar referencia
            managedToolbars.put(def.claveBarra(), toolbar);
            
            // 3. Añadirla a la vista
            view.addToolbar(def.claveBarra(), toolbar); // Necesitaremos crear este método en VisorView

            // 4. Establecer visibilidad inicial
            String configKey = "interfaz.herramientas." + def.claveBarra() + ".visible";
            boolean esVisible = config.getBoolean(configKey, true);
            toolbar.setVisible(esVisible);
        }
        
        view.revalidateToolbarContainer(); // Necesitaremos este método en VisorView
        System.out.println("[ToolbarManager] Inicialización de barras completada.");
    }

    /**
     * Refresca la visibilidad de todas las barras de herramientas gestionadas
     * leyendo su estado actual desde la configuración.
     */
    public void refrescarVisibilidadBarras() {
        System.out.println("[ToolbarManager] Refrescando visibilidad de barras...");
        for (Map.Entry<String, JToolBar> entry : managedToolbars.entrySet()) {
            String claveBarra = entry.getKey();
            JToolBar toolbar = entry.getValue();
            String configKey = "interfaz.herramientas." + claveBarra + ".visible";
            boolean esVisible = config.getBoolean(configKey, true);
            
            if (toolbar.isVisible() != esVisible) {
                toolbar.setVisible(esVisible);
            }
        }
        view.revalidateToolbarContainer();
    }
}