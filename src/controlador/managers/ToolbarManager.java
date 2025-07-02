package controlador.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import controlador.utils.ComponentRegistry; // Importación para ComponentRegistry si lo usas
import modelo.VisorModel;
import modelo.VisorModel.WorkMode; // Importación para el enum WorkMode
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

/**
 * Gestiona la creación, visibilidad y estado de las diferentes barras de herramientas.
 * Ahora centraliza las instancias de JToolBar.
 */
public class ToolbarManager {

    private final ConfigurationManager configuration;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefinitionService;
    private final ComponentRegistry registry;
    private final VisorModel model;

    // --- NUEVO: Mapa para almacenar las instancias de JToolBar creadas ---
    private final Map<String, JToolBar> toolbarInstances; 

    /**
     * Constructor del ToolbarManager.
     * @param configuration El gestor de configuración.
     * @param toolbarBuilder El builder para construir las JToolBar.
     * @param uiDefinitionService El servicio que define la estructura de la UI.
     * @param registry El registro de componentes.
     * @param model El modelo de la aplicación.
     */
    public ToolbarManager(
            ConfigurationManager configuration,
            ToolbarBuilder toolbarBuilder,
            UIDefinitionService uiDefinitionService,
            ComponentRegistry registry,
            VisorModel model) {
        this.configuration = Objects.requireNonNull(configuration);
        this.toolbarBuilder = Objects.requireNonNull(toolbarBuilder);
        this.uiDefinitionService = Objects.requireNonNull(uiDefinitionService);
        this.registry = Objects.requireNonNull(registry);
        this.model = Objects.requireNonNull(model);
        this.toolbarInstances = new HashMap<>(); // Inicializa el mapa
        System.out.println("[ToolbarManager Constructor] Finalizado.");
    } // --- Fin del constructor ToolbarManager ---

    /**
     * Inicializa todas las barras de herramientas definidas en el UIDefinitionService,
     * las construye y las almacena internamente en el mapa `toolbarInstances`.
     * Se llama una sola vez durante la inicialización de la aplicación.
     */
    public void inicializarBarrasDeHerramientas() {
        System.out.println("[ToolbarManager] Inicializando todas las barras de herramientas...");
        List<ToolbarDefinition> allToolbarDefs = uiDefinitionService.generateModularToolbarStructure();
        
        for (ToolbarDefinition def : allToolbarDefs) {
            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
            toolbarInstances.put(def.claveBarra(), toolbar); // Guarda la instancia
            registry.register("toolbar." + def.claveBarra(), toolbar); // Opcional: registrar en ComponentRegistry
            System.out.println("  -> Barra '" + def.claveBarra() + "' construida y almacenada.");
        }
        System.out.println("[ToolbarManager] Inicialización de barras completada. Total: " + toolbarInstances.size());
    } // --- Fin del método inicializarBarrasDeHerramientas ---

    /**
     * Reconstruye el contenedor de toolbars para mostrar solo las barras relevantes
     * para el modo de trabajo actual y su estado de visibilidad configurado.
     * @param currentWorkMode El modo de trabajo actual de la aplicación.
     */
    public void reconstruirContenedorDeToolbars(WorkMode currentWorkMode) {
        System.out.println("[ToolbarManager] Reconstruyendo contenedor para modo: " + currentWorkMode);
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer == null) {
            System.err.println("ERROR [ToolbarManager]: 'container.toolbars' no encontrado en el registro.");
            return;
        }

        toolbarContainer.removeAll(); // Limpiar el contenedor actual

        // --- INICIO BLOQUE DE MODIFICACIÓN ---
        // Línea anterior: List<ToolbarDefinition> allToolbarDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
        List<ToolbarDefinition> allToolbarDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
        // --- FIN BLOQUE DE MODIFICACIÓN ---
        
        // Ordenar las barras por su 'order' para que aparezcan consistentemente
        allToolbarDefs.sort((td1, td2) -> Integer.compare(td1.orden(), td2.orden()));

        for (ToolbarDefinition def : allToolbarDefs) {
            // --- INICIO DE LA MODIFICACIÓN ---
            // Excluir la barra de control inferior de ser añadida al contenedor principal
            if ("controles_imagen_inferior".equals(def.claveBarra())) {
                System.out.println("  -> EXCLUYENDO barra '" + def.claveBarra() + "' del contenedor principal. Se añadirá en ViewBuilder.");
                continue; // Saltar esta barra, no la añadimos aquí.
            }
            // --- FIN DE LA MODIFICACIÓN ---

            // Comprobar si la barra es relevante para el modo actual
            if (def.modosVisibles().contains(currentWorkMode)) {
                // Comprobar si la barra está configurada para ser visible (interfaz.herramientas.<claveBarra>)
                String toolbarVisibilityKey = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra());
                boolean isToolbarVisible = configuration.getBoolean(toolbarVisibilityKey, true);

                if (isToolbarVisible) {
                    JToolBar toolbar = toolbarInstances.get(def.claveBarra()); // Obtener la instancia ya creada
                    if (toolbar != null) {
                        toolbarContainer.add(toolbar);
                        System.out.println("  -> Añadiendo barra '" + def.claveBarra() + "' al contenedor.");
                    } else {
                        System.err.println("WARN [ToolbarManager]: Instancia de barra '" + def.claveBarra() + "' no encontrada en el mapa.");
                    }
                } else {
                    System.out.println("  -> Barra '" + def.claveBarra() + "' oculta por configuración.");
                }
            } else {
                System.out.println("  -> Barra '" + def.claveBarra() + "' no relevante para modo '" + currentWorkMode + "'.");
            }
        }
        
        toolbarContainer.revalidate();
        toolbarContainer.repaint();
        System.out.println("[ToolbarManager] Reconstrucción de contenedor completada.");
    } // --- Fin del método reconstruirContenedorDeToolbars ---

    /**
     * Devuelve una instancia de JToolBar por su clave.
     * Útil para ViewBuilder o cualquier otro componente que necesite una barra específica.
     * @param key La clave de la barra (ej. "control_imagen").
     * @return La JToolBar asociada a la clave, o null si no se encuentra.
     */
    public JToolBar getToolbar(String key) { // <-- NUEVO: Método público para obtener toolbars
        return toolbarInstances.get(key);
    } // --- Fin del método getToolbar ---

} // --- FIN de la clase ToolbarManager ---

