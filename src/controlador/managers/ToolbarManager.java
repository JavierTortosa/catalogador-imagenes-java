// Contenido de la clase controlador.managers.ToolbarManager.java

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


//package controlador.managers;
//
//import java.awt.event.HierarchyEvent;
//import java.awt.event.HierarchyListener;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import javax.swing.JButton;
//import javax.swing.JPanel;
//import javax.swing.JToolBar;
//import javax.swing.SwingUtilities;
//
//import controlador.utils.ComponentRegistry;
//import modelo.VisorModel;
//import modelo.VisorModel.WorkMode;
//import servicios.ConfigurationManager;
//import vista.builders.ToolbarBuilder;
//import vista.config.ToolbarDefinition;
//import vista.config.UIDefinitionService;
//
//public class ToolbarManager {
//
//    // --- Dependencias Clave ---
//    private final ConfigurationManager config;
//    private final ToolbarBuilder toolbarBuilder;
//    private final UIDefinitionService uiDefService;
//    private final ComponentRegistry registry;
//    private final VisorModel model;
//
//    // --- Estado Interno ---
//    private final Map<String, JToolBar> toolbarInstances = new HashMap<>();
//    private volatile boolean reconstruyendo = false; // <-- volatile para asegurar visibilidad entre hilos
//
//    // --- CONSTRUCTOR ---
//    public ToolbarManager(
//            ConfigurationManager config, 
//            ToolbarBuilder builder, 
//            UIDefinitionService uiDefService,
//            ComponentRegistry registry,
//            VisorModel model
//    ) {
//        this.config = config;
//        this.toolbarBuilder = builder;
//        this.uiDefService = uiDefService;
//        this.registry = registry;
//        this.model = model;
//    } // --- Fin del constructor ToolbarManager ---
//
//    public void inicializarBarrasDeHerramientas() {
//        System.out.println("[ToolbarManager] FASE 1: Creando instancias de barras de herramientas...");
//        List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();
//
//        for (ToolbarDefinition def : todasLasBarras) {
//            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
//            addReattachListener(toolbar);
//            toolbarInstances.put(def.claveBarra(), toolbar);
//            registry.register("toolbar." + def.claveBarra(), toolbar);
//        }
//        
//        System.out.println("  [ToolbarManager] Registrando botones individuales...");
//        Map<String, JButton> botonesCreados = toolbarBuilder.getBotonesPorNombre();
//        botonesCreados.forEach(registry::register);
//        System.out.println("    -> " + botonesCreados.size() + " botones registrados.");
//        System.out.println("[ToolbarManager] Creación de instancias de barras completada.");
//    } // --- Fin del método inicializarBarrasDeHerramientas ---
//
//    public void reconstruirContenedorDeToolbars(WorkMode modoActual) {
//        if (reconstruyendo) {
//            return; // La guarda original sigue siendo útil para llamadas síncronas.
//        }
//        
//        this.reconstruyendo = true;
//        try {
//            System.out.println("[ToolbarManager] FASE 2: Reconstruyendo contenedor de toolbars para el modo: " + modoActual);
//            
//            JPanel toolbarContainer = registry.get("container.toolbars");
//            if (toolbarContainer == null) {
//                System.err.println("ERROR CRÍTICO [ToolbarManager]: No se encontró 'container.toolbars' en el registro.");
//                return;
//            }
//    
//            toolbarContainer.removeAll();
//            
//            List<ToolbarDefinition> todasLasDefiniciones = new ArrayList<>(uiDefService.generateModularToolbarStructure());
//            todasLasDefiniciones.sort(Comparator.comparingInt(ToolbarDefinition::orden));
//            
//            for (ToolbarDefinition def : todasLasDefiniciones) {
//                if (def.modosVisibles().contains(modoActual)) {
//                    JToolBar toolbar = toolbarInstances.get(def.claveBarra());
//                    if (toolbar != null) {
//                        if (toolbar.getParent() == null || toolbar.getParent() == toolbarContainer) {
//                             toolbarContainer.add(toolbar);
//                             
//                             String configKey = "interfaz.herramientas." + def.claveBarra() + ".visible";
//                             boolean esVisible = config.getBoolean(configKey, true);
//                             toolbar.setVisible(esVisible);
//                        }
//                    }
//                }
//            }
//            
//            toolbarContainer.revalidate();
//            toolbarContainer.repaint();
//            System.out.println("  [ToolbarManager] Contenedor de toolbars reconstruido.");
//
//        } finally {
//            this.reconstruyendo = false;
//        }
//    } // --- fin del método reconstruirContenedorDeToolbars ---
//
//    /**
//     * Añade un listener a una JToolBar que detecta cuándo su padre cambia.
//     */
//    private void addReattachListener(JToolBar toolbar) {
//        // --- CAMBIO: La lógica del listener se ha modificado ---
//        toolbar.addHierarchyListener(new HierarchyListener() {
//            @Override
//            public void hierarchyChanged(HierarchyEvent e) {
//                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
//                    // Solo encolamos una nueva reconstrucción si NO hay una ya en progreso.
//                    // Esto rompe el bucle de eventos.
//                    if (!isReconstruyendo()) {
//                        System.out.println("  [HierarchyListener] Detectado cambio de padre en: " + toolbar.getName() + ". Solicitando reconstrucción.");
//                        SwingUtilities.invokeLater(() -> {
//                            reconstruirContenedorDeToolbars(model.getCurrentWorkMode());
//                        });
//                    } else {
//                        System.out.println("  [HierarchyListener] Detectado cambio de padre en: " + toolbar.getName() + ". IGNORADO (reconstrucción en progreso).");
//                    }
//                }
//            }
//        });
//    } // --- fin del método addReattachListener ---
//
//    /**
//     * --- MÉTODO NUEVO ---
//     * Devuelve de forma segura el estado del flag de reconstrucción.
//     * @return true si una reconstrucción está actualmente en progreso.
//     */
//    public boolean isReconstruyendo() {
//        return this.reconstruyendo;
//    } // --- fin del método isReconstruyendo ---
//
//    /**
//     * Refresca la visibilidad de todas las barras de herramientas gestionadas.
//     */
//    public void refrescarVisibilidadBarras() {
//        System.out.println("[ToolbarManager] Refrescando visibilidad de barras...");
//        for (Map.Entry<String, JToolBar> entry : toolbarInstances.entrySet()) {
//            String claveBarra = entry.getKey();
//            JToolBar toolbar = entry.getValue();
//            
//            String configKey = "interfaz.herramientas." + claveBarra + ".visible";
//            boolean esVisible = config.getBoolean(configKey, true);
//            
//            if (toolbar.isVisible() != esVisible) {
//                toolbar.setVisible(esVisible);
//            }
//        }
//        
//        JPanel toolbarContainer = registry.get("container.toolbars");
//        if (toolbarContainer != null) {
//            toolbarContainer.revalidate();
//            toolbarContainer.repaint();
//        }
//    } // --- Fin del método refrescarVisibilidadBarras ---
//
//} // --- Fin de la clase ToolbarManager ---