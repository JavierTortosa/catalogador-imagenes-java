package vista.builders;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import controlador.VisorController;
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import vista.components.DPadComponent;
import vista.config.ButtonType;
import vista.config.HotspotDefinition;
import vista.config.IconScope;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ToolbarBuilder {

    private Map<String, Action> actionMap;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;
    private final VisorController controllerRef;
    private final ComponentRegistry registry;

    private final int iconoAncho;
    private final int iconoAlto;

    private Map<String, AbstractButton> botonesPorNombre;
    private final Map<String, ButtonGroup> radioGroups = new HashMap<>();
    private final List<String> groupToolbarKeys = List.of("zoom", "modo", "vistas");
    
    private final ButtonGroup modoDeTrabajoGroup;

    public ToolbarBuilder(
            ThemeManager themeManager,
            IconUtils iconUtils,
            VisorController controller,
            int iconoAncho,
            int iconoAlto,
            ComponentRegistry registry 
    ) {
        System.out.println("[ToolbarBuilder Constructor] Iniciando...");
        this.themeManager = Objects.requireNonNull(themeManager);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.controllerRef = Objects.requireNonNull(controller);
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en ToolbarBuilder.");
        
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;
        
        this.actionMap = new HashMap<>();
        this.botonesPorNombre = new HashMap<>();
        
        this.modoDeTrabajoGroup = new ButtonGroup();

        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    } // --- Fin del método ToolbarBuilder (constructor) ---


    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) { 
        System.out.println("\n--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");
        
        final JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra());
        toolbar.setFloatable(true);
        toolbar.setRollover(true);
        toolbar.putClientProperty("toolbarDefinition", toolbarDef);

        // --- INICIO DE LA MODIFICACIÓN ---
        // Lógica para agrupar JToggleButtons
        ButtonGroup group = null;
        // Comprueba si la clave de la barra actual está en nuestra lista de barras a agrupar.
        if (groupToolbarKeys.contains(toolbarDef.claveBarra())) {
            group = new ButtonGroup();
            this.radioGroups.put(toolbarDef.claveBarra(), group);
            System.out.println("    -> Creado ButtonGroup para la barra: '" + toolbarDef.claveBarra() + "'");
        }
        // --- FIN DE LA MODIFICACIÓN ---

        if (toolbarDef.botones() != null) {
            for (ToolbarButtonDefinition botonDef : toolbarDef.botones()) {
                JComponent componente = crearComponenteIndividual(botonDef); 
                if (componente != null) {
                    toolbar.add(componente);
                    // --- INICIO DE LA MODIFICACIÓN ---
                    // Si hemos creado un grupo y el componente es un JToggleButton, lo añadimos.
                    if (group != null && componente instanceof JToggleButton) {
                        group.add((JToggleButton) componente);
                        System.out.println("      -> Botón '" + componente.getName() + "' añadido al ButtonGroup.");
                    }
                    // --- FIN DE LA MODIFICACIÓN ---
                }
            }
        }
        return toolbar;
    } // --- Fin del método buildSingleToolbar ---
    
    
    /**
     * Crea un componente Swing individual (JButton, JToggleButton, DPadComponent, etc.)
     * basado en la definición proporcionada en {@link ToolbarButtonDefinition}.
     * Aplica configuraciones básicas y asigna acciones e iconos.
     *
     * @param definition La definición del botón/componente.
     * @return El componente Swing creado, o null si la definición es inválida.
     */
    private JComponent crearComponenteIndividual(ToolbarButtonDefinition definition) {
        // --- 1. VALIDACIONES INICIALES ---
        if (definition == null || definition.comandoCanonico() == null) {
            System.err.println("ERROR [ToolbarBuilder.crearComponenteIndividual]: Definición de botón o comando canónico es nulo.");
            return null;
        }

        // --- 2. VARIABLES COMUNES ---
        final int targetIconWidth = this.iconoAncho;
        final int targetIconHeight = this.iconoAlto;
        Action associatedAction = this.actionMap.get(definition.comandoCanonico());

        AbstractButton abstractButtonComponent = null;
        JComponent finalComponent = null;

        // --- 3. CREACIÓN DEL COMPONENTE SWING SEGÚN EL ButtonType ---
        switch (definition.tipoBoton()) {
            case NORMAL:
                abstractButtonComponent = new JButton();
                abstractButtonComponent.setOpaque(true);
                abstractButtonComponent.setContentAreaFilled(true);
                break;

            case TOGGLE:
                abstractButtonComponent = new JToggleButton();
                
                abstractButtonComponent.setOpaque(true);
                abstractButtonComponent.setContentAreaFilled(true);
//                abstractButtonComponent.putClientProperty("JButton.buttonType", "regular"); 

                System.out.println("[ToolbarBuilder] Creando JToggleButton para: " + definition.comandoCanonico() +
                                  ", Background: " + abstractButtonComponent.getBackground() +
                                  ", SelectedBackground: " + UIManager.getColor("ToggleButton.selectedBackground") +
                                  ", UI: " + abstractButtonComponent.getUI().getClass().getName());
                break;
                
                
//            case TOGGLE:
//                abstractButtonComponent = new JToggleButton();
////                abstractButtonComponent.setOpaque(true);
////                abstractButtonComponent.setContentAreaFilled(true);
//                
//                System.out.println("[ToolbarBuilder] Creando JToggleButton para: " + definition.comandoCanonico() +
//                                  ", Background: " + abstractButtonComponent.getBackground() +
//                                  ", SelectedBackground: " + UIManager.getColor("ToggleButton.selectedBackground") +
//                                  ", UI: " + abstractButtonComponent.getUI().getClass().getName());
//                break;
                
//            case TOGGLE:
//                abstractButtonComponent = new JToggleButton();
//                abstractButtonComponent.setOpaque(true);
//                abstractButtonComponent.setContentAreaFilled(true);
//                break;

            case COLOR_OVERLAY_ICON_BUTTON:
                JButton colorButton = new JButton();
                colorButton.setOpaque(true);
                colorButton.setContentAreaFilled(true);
                colorButton.setBorderPainted(false);
                colorButton.setFocusPainted(false);
                colorButton.setBorder(null);
                
                // Lógica para obtener el color inicial. Usamos textoTooltip como clave de tema, como discutimos.
                // Para el slot #1 será "clear", "dark", etc. Para los otros, será la clave del color custom.
                String colorKey = definition.textoTooltip(); 
                java.awt.Color initialColor = themeManager.getFondoSecundarioParaTema(colorKey);
                
                Icon colorOverlayIcon = this.iconUtils.getColoredOverlayIcon(definition.claveIcono(), initialColor, targetIconWidth, targetIconHeight);
                colorButton.setIcon(colorOverlayIcon);
                abstractButtonComponent = colorButton;
                break;

            case CHECKERED_OVERLAY_ICON_BUTTON:
                JButton checkeredButton = new JButton();
                checkeredButton.setOpaque(true);
                checkeredButton.setContentAreaFilled(true);
                checkeredButton.setBorderPainted(false);
                checkeredButton.setFocusPainted(false);
                checkeredButton.setBorder(null);

                Icon checkeredOverlayIcon = this.iconUtils.getCheckeredOverlayIcon(definition.claveIcono(), targetIconWidth, targetIconHeight);
                checkeredButton.setIcon(checkeredOverlayIcon);
                abstractButtonComponent = checkeredButton;
                break;

            case TRANSPARENT:
                JButton transparentButton = new JButton();
                transparentButton.setOpaque(false);
                transparentButton.setContentAreaFilled(false);
                transparentButton.setBorderPainted(false);
                transparentButton.setFocusPainted(false);
                transparentButton.setBorder(null);
                
                ImageIcon transparentIcon = (definition.scopeIconoBase() == IconScope.COMMON)
                                           ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), targetIconWidth, targetIconHeight)
                                           : this.iconUtils.getScaledIcon(definition.claveIcono(), targetIconWidth, targetIconHeight);
                transparentButton.setIcon(transparentIcon);
                abstractButtonComponent = transparentButton;
                break;

            case DPAD_CRUZ:
            case DPAD_GRID:
                break; // Se manejan después

            default:
                System.err.println("ERROR [ToolbarBuilder.crearComponenteIndividual]: Tipo de botón no reconocido: " + definition.tipoBoton());
                abstractButtonComponent = new JButton();
                abstractButtonComponent.setOpaque(true);
                abstractButtonComponent.setContentAreaFilled(true);
                break;
        }

        // --- 4. LÓGICA COMÚN PARA AbstractButton ---
        if (abstractButtonComponent != null) {
            abstractButtonComponent.setMargin(new Insets(2, 2, 2, 2));
            abstractButtonComponent.setToolTipText(definition.textoTooltip());
            
            if (associatedAction != null) {
                abstractButtonComponent.setAction(associatedAction);
                abstractButtonComponent.setText(null);
                
                if (abstractButtonComponent.getIcon() == null) {
                    if (associatedAction.getValue(Action.SMALL_ICON) instanceof Icon) {
                        abstractButtonComponent.setIcon((Icon) associatedAction.getValue(Action.SMALL_ICON));
                    } else if (definition.claveIcono() != null) {
                        ImageIcon defaultIcon = (definition.scopeIconoBase() == IconScope.COMMON)
                                                ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), targetIconWidth, targetIconHeight)
                                                : this.iconUtils.getScaledIcon(definition.claveIcono(), targetIconWidth, targetIconHeight);
                        abstractButtonComponent.setIcon(defaultIcon);
                    }
                }
            } else {
                System.err.println("WARN [ToolbarBuilder.crearComponenteIndividual]: No se encontró Action para comando '" + definition.comandoCanonico() + "'. Añadiendo listener directo.");
                abstractButtonComponent.setActionCommand(definition.comandoCanonico());
                abstractButtonComponent.addActionListener(this.controllerRef);
                
                if (abstractButtonComponent.getIcon() == null && definition.claveIcono() != null) {
                     ImageIcon defaultIcon = (definition.scopeIconoBase() == IconScope.COMMON)
                                           ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), targetIconWidth, targetIconHeight)
                                           : this.iconUtils.getScaledIcon(definition.claveIcono(), targetIconWidth, targetIconHeight);
                     abstractButtonComponent.setIcon(defaultIcon);
                }
                abstractButtonComponent.setText("?");
            }

            String nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
            String claveBaseBoton = ConfigKeys.buildKey("interfaz.boton", definition.categoriaLayout(), nombreBotonParaClave);
            
//            this.botonesPorNombre.put(claveBaseBoton, (JButton) abstractButtonComponent);
            this.registry.register(claveBaseBoton, abstractButtonComponent);
            
            abstractButtonComponent.putClientProperty("buttonType", definition.tipoBoton());
            abstractButtonComponent.putClientProperty("baseIconName", definition.claveIcono());
            abstractButtonComponent.putClientProperty("buttonConfigKey", claveBaseBoton);
            
            finalComponent = abstractButtonComponent;

        } else if (definition.tipoBoton() == ButtonType.DPAD_CRUZ || definition.tipoBoton() == ButtonType.DPAD_GRID) {
            // ... (Tu lógica para DPadComponent se mantiene aquí sin cambios) ...
            // Por completitud, la pego aquí de nuevo:
            System.out.println("[ToolbarBuilder.crearComponenteIndividual] Construyendo DPadComponent para tipo: " + definition.tipoBoton());
            final int DPAD_DISPLAY_SIZE = 32;

            if (definition.listaDeHotspots() == null || definition.listaDeHotspots().isEmpty()) {
                System.err.println("ERROR [ToolbarBuilder.crearComponenteIndividual]: La definición de DPAD_CRUZ/GRID no contiene una lista de hotspots.");
                return new JButton("Error D-Pad");
            }
            
            Image dpadBaseImage = (definition.scopeIconoBase() == IconScope.COMMON)
                                ? iconUtils.getRawCommonImage(definition.claveIcono())
                                : iconUtils.getRawIcon(definition.claveIcono());
            Image pressedImage = iconUtils.getRawCommonImage("D-Pad_all_48x48.png"); 
            
            List<String> hotspotKeys = new ArrayList<>();
            List<Image> hoverImages = new ArrayList<>();
            List<Action> hotspotActions = new ArrayList<>();
            
            for (int i = 0; i < definition.listaDeHotspots().size(); i++) {
                HotspotDefinition hotspotDef = definition.listaDeHotspots().get(i);
                Action currentHotspotAction = actionMap.get(hotspotDef.comando());
                
                Image hoverImage = (hotspotDef.scope() == IconScope.COMMON)
                                 ? iconUtils.getRawCommonImage(hotspotDef.icono())
                                 : iconUtils.getRawIcon(hotspotDef.icono());
                
                hotspotKeys.add(definition.comandoCanonico() + "-hotspot-" + i);
                hoverImages.add(hoverImage);
                hotspotActions.add(currentHotspotAction);
            }

            DPadComponent dpadComponent;
            dpadComponent = DPadComponent.createCrossLayout(
                new Dimension(DPAD_DISPLAY_SIZE, DPAD_DISPLAY_SIZE),
                dpadBaseImage, pressedImage, hotspotKeys, hoverImages, hotspotActions
            );
            dpadComponent.setToolTipText(definition.textoTooltip());
            finalComponent = dpadComponent;
            
            String dpadClave = ConfigKeys.buildKey("interfaz.dpad", definition.categoriaLayout(), extraerNombreClave(definition.comandoCanonico()));
            this.registry.register(dpadClave, finalComponent);
        } else {
            System.err.println("ERROR [ToolbarBuilder.crearComponenteIndividual]: No se pudo crear componente. Tipo no manejado: " + definition.tipoBoton());
            return null;
        }

        return finalComponent;
    } // --- Fin del método crearComponenteIndividual ---
    
    
     private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null) return "desconocido";
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;
        return resultado.replace('.', '_');
    } // --- Fin del método extraerNombreClave ---

     public Map<String, AbstractButton> getBotonesPorNombre() {
         return Collections.unmodifiableMap(this.botonesPorNombre);
     }
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del método setActionMap ---
    
    
} // --- FIN de la clase ToolbarBuilder ---

