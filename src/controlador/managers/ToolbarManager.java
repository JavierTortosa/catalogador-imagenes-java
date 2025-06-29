package controlador.managers;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

public class ToolbarManager {

    // --- Dependencias Clave ---
    private final ConfigurationManager config;
    private final ToolbarBuilder toolbarBuilder;
    private final UIDefinitionService uiDefService;
    private final ComponentRegistry registry;
    private final VisorModel model;

    // --- Estado Interno ---
    private final Map<String, JToolBar> toolbarInstances = new HashMap<>();
    private volatile boolean reconstruyendo = false; // <-- volatile para asegurar visibilidad entre hilos

    // --- CONSTRUCTOR ---
    public ToolbarManager(
            ConfigurationManager config, 
            ToolbarBuilder builder, 
            UIDefinitionService uiDefService,
            ComponentRegistry registry,
            VisorModel model
    ) {
        this.config = config;
        this.toolbarBuilder = builder;
        this.uiDefService = uiDefService;
        this.registry = registry;
        this.model = model;
    } // --- Fin del constructor ToolbarManager ---

    public void inicializarBarrasDeHerramientas() {
        System.out.println("[ToolbarManager] FASE 1: Creando instancias de barras de herramientas...");
        List<ToolbarDefinition> todasLasBarras = uiDefService.generateModularToolbarStructure();

        for (ToolbarDefinition def : todasLasBarras) {
            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
            addReattachListener(toolbar);
            toolbarInstances.put(def.claveBarra(), toolbar);
            registry.register("toolbar." + def.claveBarra(), toolbar);
        }
        
        System.out.println("  [ToolbarManager] Registrando botones individuales...");
        Map<String, JButton> botonesCreados = toolbarBuilder.getBotonesPorNombre();
        botonesCreados.forEach(registry::register);
        System.out.println("    -> " + botonesCreados.size() + " botones registrados.");
        System.out.println("[ToolbarManager] Creación de instancias de barras completada.");
    } // --- Fin del método inicializarBarrasDeHerramientas ---

    public void reconstruirContenedorDeToolbars(WorkMode modoActual) {
        if (reconstruyendo) {
            return; // La guarda original sigue siendo útil para llamadas síncronas.
        }
        
        this.reconstruyendo = true;
        try {
            System.out.println("[ToolbarManager] FASE 2: Reconstruyendo contenedor de toolbars para el modo: " + modoActual);
            
            JPanel toolbarContainer = registry.get("container.toolbars");
            if (toolbarContainer == null) {
                System.err.println("ERROR CRÍTICO [ToolbarManager]: No se encontró 'container.toolbars' en el registro.");
                return;
            }
    
            toolbarContainer.removeAll();
            
            List<ToolbarDefinition> todasLasDefiniciones = new ArrayList<>(uiDefService.generateModularToolbarStructure());
            todasLasDefiniciones.sort(Comparator.comparingInt(ToolbarDefinition::orden));
            
            for (ToolbarDefinition def : todasLasDefiniciones) {
                if (def.modosVisibles().contains(modoActual)) {
                    JToolBar toolbar = toolbarInstances.get(def.claveBarra());
                    if (toolbar != null) {
                        if (toolbar.getParent() == null || toolbar.getParent() == toolbarContainer) {
                             toolbarContainer.add(toolbar);
                             
                             String configKey = "interfaz.herramientas." + def.claveBarra() + ".visible";
                             boolean esVisible = config.getBoolean(configKey, true);
                             toolbar.setVisible(esVisible);
                        }
                    }
                }
            }
            
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
            System.out.println("  [ToolbarManager] Contenedor de toolbars reconstruido.");

        } finally {
            this.reconstruyendo = false;
        }
    } // --- fin del método reconstruirContenedorDeToolbars ---

    /**
     * Añade un listener a una JToolBar que detecta cuándo su padre cambia.
     */
    private void addReattachListener(JToolBar toolbar) {
        // --- CAMBIO: La lógica del listener se ha modificado ---
        toolbar.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    // Solo encolamos una nueva reconstrucción si NO hay una ya en progreso.
                    // Esto rompe el bucle de eventos.
                    if (!isReconstruyendo()) {
                        System.out.println("  [HierarchyListener] Detectado cambio de padre en: " + toolbar.getName() + ". Solicitando reconstrucción.");
                        SwingUtilities.invokeLater(() -> {
                            reconstruirContenedorDeToolbars(model.getCurrentWorkMode());
                        });
                    } else {
                        System.out.println("  [HierarchyListener] Detectado cambio de padre en: " + toolbar.getName() + ". IGNORADO (reconstrucción en progreso).");
                    }
                }
            }
        });
    } // --- fin del método addReattachListener ---

    /**
     * --- MÉTODO NUEVO ---
     * Devuelve de forma segura el estado del flag de reconstrucción.
     * @return true si una reconstrucción está actualmente en progreso.
     */
    public boolean isReconstruyendo() {
        return this.reconstruyendo;
    } // --- fin del método isReconstruyendo ---

    /**
     * Refresca la visibilidad de todas las barras de herramientas gestionadas.
     */
    public void refrescarVisibilidadBarras() {
        System.out.println("[ToolbarManager] Refrescando visibilidad de barras...");
        for (Map.Entry<String, JToolBar> entry : toolbarInstances.entrySet()) {
            String claveBarra = entry.getKey();
            JToolBar toolbar = entry.getValue();
            
            String configKey = "interfaz.herramientas." + claveBarra + ".visible";
            boolean esVisible = config.getBoolean(configKey, true);
            
            if (toolbar.isVisible() != esVisible) {
                toolbar.setVisible(esVisible);
            }
        }
        
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer != null) {
            toolbarContainer.revalidate();
            toolbarContainer.repaint();
        }
    } // --- Fin del método refrescarVisibilidadBarras ---

} // --- Fin de la clase ToolbarManager ---