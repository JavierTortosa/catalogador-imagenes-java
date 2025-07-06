package controlador.managers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent; // Import necesario para el cast
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel; // Import necesario
import modelo.VisorModel.WorkMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.builders.ToolbarBuilder;
import vista.config.ToolbarAlignment;
import vista.config.ToolbarDefinition;
import vista.config.ToolbarButtonDefinition; // Import necesario si usas el tipo
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
 // Reemplaza este método completo en tu ToolbarManager.java

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
        // --- FIN DE LA LÓGICA BASADA EN ANCESTORLISTENER ---

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

//package controlador.managers;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//import javax.swing.JPanel;
//import javax.swing.JToolBar;
//
//import controlador.utils.ComponentRegistry; // Importación para ComponentRegistry si lo usas
//import modelo.VisorModel;
//import modelo.VisorModel.WorkMode; // Importación para el enum WorkMode
//import servicios.ConfigKeys;
//import servicios.ConfigurationManager;
//import vista.builders.ToolbarBuilder;
//import vista.config.ToolbarAlignment;
//import vista.config.ToolbarDefinition;
//import vista.config.UIDefinitionService;
//
///**
// * Gestiona la creación, visibilidad y estado de las diferentes barras de herramientas.
// * Ahora centraliza las instancias de JToolBar.
// */
//public class ToolbarManager {
//
//    private final ConfigurationManager configuration;
//    private final ToolbarBuilder toolbarBuilder;
//    private final UIDefinitionService uiDefinitionService;
//    private final ComponentRegistry registry;
//    private final VisorModel model;
//
//    // --- NUEVO: Mapa para almacenar las instancias de JToolBar creadas ---
//    private final Map<String, JToolBar> toolbarInstances; 
//
//    /**
//     * Constructor del ToolbarManager.
//     * @param configuration El gestor de configuración.
//     * @param toolbarBuilder El builder para construir las JToolBar.
//     * @param uiDefinitionService El servicio que define la estructura de la UI.
//     * @param registry El registro de componentes.
//     * @param model El modelo de la aplicación.
//     */
//    public ToolbarManager(
//            ConfigurationManager configuration,
//            ToolbarBuilder toolbarBuilder,
//            UIDefinitionService uiDefinitionService,
//            ComponentRegistry registry,
//            VisorModel model) {
//        this.configuration = Objects.requireNonNull(configuration);
//        this.toolbarBuilder = Objects.requireNonNull(toolbarBuilder);
//        this.uiDefinitionService = Objects.requireNonNull(uiDefinitionService);
//        this.registry = Objects.requireNonNull(registry);
//        this.model = Objects.requireNonNull(model);
//        this.toolbarInstances = new HashMap<>(); // Inicializa el mapa
//        System.out.println("[ToolbarManager Constructor] Finalizado.");
//    } // --- Fin del constructor ToolbarManager ---
//
//    /**
//     * Inicializa todas las barras de herramientas definidas en el UIDefinitionService,
//     * las construye y las almacena internamente en el mapa `toolbarInstances`.
//     * Se llama una sola vez durante la inicialización de la aplicación.
//     */
//    public void inicializarBarrasDeHerramientas() {
//        System.out.println("[ToolbarManager] Inicializando todas las barras de herramientas...");
//        List<ToolbarDefinition> allToolbarDefs = uiDefinitionService.generateModularToolbarStructure();
//        
//        for (ToolbarDefinition def : allToolbarDefs) {
//            JToolBar toolbar = toolbarBuilder.buildSingleToolbar(def);
//            toolbarInstances.put(def.claveBarra(), toolbar); // Guarda la instancia
//            registry.register("toolbar." + def.claveBarra(), toolbar); // Opcional: registrar en ComponentRegistry
//            System.out.println("  -> Barra '" + def.claveBarra() + "' construida y almacenada.");
//        }
//        System.out.println("[ToolbarManager] Inicialización de barras completada. Total: " + toolbarInstances.size());
//    } // --- Fin del método inicializarBarrasDeHerramientas ---
//
//    /**
//     * Reconstruye el contenedor de toolbars para mostrar solo las barras relevantes
//     * para el modo de trabajo actual, colocándolas en su panel de alineamiento
//     * correspondiente (izquierda, centro o derecha).
//     * @param currentWorkMode El modo de trabajo actual de la aplicación.
//     */
//    public void reconstruirContenedorDeToolbars(WorkMode currentWorkMode) {
//        System.out.println("[ToolbarManager] Reconstruyendo contenedor para modo: " + currentWorkMode);
//        
//        JPanel leftToolbarPanel = registry.get("container.toolbars.left");
//        JPanel centerToolbarPanel = registry.get("container.toolbars.center");
//        JPanel rightToolbarPanel = registry.get("container.toolbars.right");
//        
//        if (leftToolbarPanel == null || centerToolbarPanel == null || rightToolbarPanel == null) {
//            System.err.println("ERROR [ToolbarManager]: No se encontraron los paneles de alineamiento (left/center/right) en el registro.");
//            return;
//        }
//
//        leftToolbarPanel.removeAll();
//        centerToolbarPanel.removeAll();
//        rightToolbarPanel.removeAll();
//        
//        List<ToolbarDefinition> allToolbarDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
//        allToolbarDefs.sort((def1, def2) -> Integer.compare(def1.orden(), def2.orden()));
//
//        for (ToolbarDefinition def : allToolbarDefs) {
//            if ("controles_imagen_inferior".equals(def.claveBarra())) {
//                continue;
//            }
//
//            if (def.modosVisibles().contains(currentWorkMode)) {
//                String toolbarVisibilityKey = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra());
//                boolean isToolbarVisible = configuration.getBoolean(toolbarVisibilityKey, true);
//
//                if (isToolbarVisible) {
//                    JToolBar toolbar = toolbarInstances.get(def.claveBarra());
//                    if (toolbar != null) {
//                        switch (def.alignment()) {
//                            case LEFT:   leftToolbarPanel.add(toolbar);   break;
//                            case CENTER: centerToolbarPanel.add(toolbar); break;
//                            case RIGHT:  rightToolbarPanel.add(toolbar);  break;
//                        }
//                    }
//                }
//            }
//        }
//        
//        leftToolbarPanel.revalidate();
//        leftToolbarPanel.repaint();
//        centerToolbarPanel.revalidate();
//        centerToolbarPanel.repaint();
//        rightToolbarPanel.revalidate();
//        rightToolbarPanel.repaint();
//        
//        System.out.println("[ToolbarManager] Reconstrucción de contenedor completada.");
//    } // --- Fin del método reconstruirContenedorDeToolbars ---
//    
//    
////    /**
////     * Calcula el índice de inserción correcto para una JToolBar dentro de su panel de alineamiento,
////     * basándose en el orden definido en UIDefinitionService.
////     * 
////     * @param toolbarKey La clave de la barra de herramientas (ej. "edicion").
////     * @return El índice donde la barra debe ser insertada en su panel de alineamiento, o -1 si hay un error.
////     */
////    public int getCorrectIndexForToolbar(String toolbarKey) {
////        // Obtenemos la definición de todas las barras y las ordenamos.
////        List<ToolbarDefinition> allDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
////        allDefs.sort((def1, def2) -> Integer.compare(def1.orden(), def2.orden()));
////
////        // Buscamos la definición de nuestra barra para saber su alineamiento
////        ToolbarDefinition targetDef = allDefs.stream()
////                                             .filter(d -> d.claveBarra().equals(toolbarKey))
////                                             .findFirst()
////                                             .orElse(null);
////
////        if (targetDef == null) {
////            System.err.println("ERROR [getCorrectIndexForToolbar]: No se encontró la definición para la clave: " + toolbarKey);
////            return -1;
////        }
////        
////        ToolbarAlignment targetAlignment = targetDef.alignment();
////        int indexInGroup = 0;
////
////        // Iteramos sobre TODAS las definiciones en orden
////        for (ToolbarDefinition def : allDefs) {
////            // Si la definición actual es la que buscamos, hemos encontrado su índice dentro de su grupo.
////            if (def.equals(targetDef)) {
////                return indexInGroup;
////            }
////            
////            // Si la definición actual pertenece al MISMO grupo de alineamiento,
////            // y su toolbar correspondiente está actualmente visible, contamos una posición.
////            if (def.alignment() == targetAlignment) {
////                JToolBar existingToolbar = toolbarInstances.get(def.claveBarra());
////                if (existingToolbar != null && existingToolbar.isVisible() && existingToolbar.getParent() != null) {
////                    indexInGroup++;
////                }
////            }
////        }
////        
////        return -1; // No debería llegar aquí
////    } // --- Fin del método getCorrectIndexForToolbar ---
//    
//    
////    public void reconstruirContenedorDeToolbars(WorkMode currentWorkMode) {
////        System.out.println("[ToolbarManager] Reconstruyendo contenedor para modo: " + currentWorkMode);
////        JPanel toolbarContainer = registry.get("container.toolbars");
////        if (toolbarContainer == null) {
////            System.err.println("ERROR [ToolbarManager]: 'container.toolbars' no encontrado en el registro.");
////            return;
////        }
////
////        toolbarContainer.removeAll(); // Limpiar el contenedor actual
////
////        // --- INICIO BLOQUE DE MODIFICACIÓN ---
////        // Línea anterior: List<ToolbarDefinition> allToolbarDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
////        List<ToolbarDefinition> allToolbarDefs = new ArrayList<>(uiDefinitionService.generateModularToolbarStructure());
////        // --- FIN BLOQUE DE MODIFICACIÓN ---
////        
////        // Ordenar las barras por su 'order' para que aparezcan consistentemente
////        allToolbarDefs.sort((td1, td2) -> Integer.compare(td1.orden(), td2.orden()));
////
////        for (ToolbarDefinition def : allToolbarDefs) {
////            // --- INICIO DE LA MODIFICACIÓN ---
////            // Excluir la barra de control inferior de ser añadida al contenedor principal
////            if ("controles_imagen_inferior".equals(def.claveBarra())) {
////                System.out.println("  -> EXCLUYENDO barra '" + def.claveBarra() + "' del contenedor principal. Se añadirá en ViewBuilder.");
////                continue; // Saltar esta barra, no la añadimos aquí.
////            }
////            // --- FIN DE LA MODIFICACIÓN ---
////
////            // Comprobar si la barra es relevante para el modo actual
////            if (def.modosVisibles().contains(currentWorkMode)) {
////                // Comprobar si la barra está configurada para ser visible (interfaz.herramientas.<claveBarra>)
////                String toolbarVisibilityKey = ConfigKeys.buildKey("interfaz.herramientas", def.claveBarra());
////                boolean isToolbarVisible = configuration.getBoolean(toolbarVisibilityKey, true);
////
////                if (isToolbarVisible) {
////                    JToolBar toolbar = toolbarInstances.get(def.claveBarra()); // Obtener la instancia ya creada
////                    if (toolbar != null) {
////                        toolbarContainer.add(toolbar);
////                        System.out.println("  -> Añadiendo barra '" + def.claveBarra() + "' al contenedor.");
////                    } else {
////                        System.err.println("WARN [ToolbarManager]: Instancia de barra '" + def.claveBarra() + "' no encontrada en el mapa.");
////                    }
////                } else {
////                    System.out.println("  -> Barra '" + def.claveBarra() + "' oculta por configuración.");
////                }
////            } else {
////                System.out.println("  -> Barra '" + def.claveBarra() + "' no relevante para modo '" + currentWorkMode + "'.");
////            }
////        }
////        
////        toolbarContainer.revalidate();
////        toolbarContainer.repaint();
////        System.out.println("[ToolbarManager] Reconstrucción de contenedor completada.");
////    } // --- Fin del método reconstruirContenedorDeToolbars ---
//
//    /**
//     * Devuelve una instancia de JToolBar por su clave.
//     * Útil para ViewBuilder o cualquier otro componente que necesite una barra específica.
//     * @param key La clave de la barra (ej. "control_imagen").
//     * @return La JToolBar asociada a la clave, o null si no se encuentra.
//     */
//    public JToolBar getToolbar(String key) { // <-- NUEVO: Método público para obtener toolbars
//        return toolbarInstances.get(key);
//    } // --- Fin del método getToolbar ---
//    
//    public java.util.Map<String, javax.swing.JToolBar> getManagedToolbars() {
//        return this.toolbarInstances;
//    }
//    
//    /**
//     * "Bomba Nuclear": Destruye todas las instancias de JToolBar y las
//     * vuelve a crear desde cero usando el ToolbarBuilder.
//     * Garantiza un estado completamente limpio.
//     */
//    public void forzarRecreacionDeTodasLasToolbars() {
//        System.out.println("  [ToolbarManager] Forzando recreación completa de todas las barras de herramientas (Bomba Nuclear)...");
//        inicializarBarrasDeHerramientas();
//    } // --- Fin del método forzarRecreacionDeTodasLasToolbars ---
//
//} // --- FIN de la clase ToolbarManager ---
//
