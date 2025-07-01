package vista.builders;

import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import controlador.GeneralController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
// --- CAMBIO ---: Importar ComponentRegistry
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import vista.components.DPadComponent;
import vista.components.Hotspot;
import vista.config.ButtonType;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ToolbarBuilder {

    private Map<String, Action> actionMap;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;
    private final VisorController controllerRef;
    private GeneralController generalController;
    // --- CAMBIO ---: Añadir campo para el registry
    private final ComponentRegistry registry;

    private final int iconoAncho;
    private final int iconoAlto;

    private Map<String, JButton> botonesPorNombre;
    
    private final ButtonGroup modoDeTrabajoGroup;

    // --- CAMBIO ---: Actualizar la firma del constructor
    public ToolbarBuilder(
            ThemeManager themeManager,
            IconUtils iconUtils,
            VisorController controller,
            int iconoAncho,
            int iconoAlto,
            ComponentRegistry registry // <-- Nuevo parámetro
    ) {
        System.out.println("[ToolbarBuilder Constructor] Iniciando...");
        this.themeManager = Objects.requireNonNull(themeManager);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.controllerRef = Objects.requireNonNull(controller);
        // --- CAMBIO ---: Asignar el registry
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en ToolbarBuilder.");
        
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;
        
        this.actionMap = new HashMap<>();
        this.botonesPorNombre = new HashMap<>();
        
        this.modoDeTrabajoGroup = new ButtonGroup();

        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    } // --- Fin del método ToolbarBuilder (constructor) ---

    public void setGeneralController(GeneralController generalController) {
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null en ToolbarBuilder.");
    } // --- Fin del método setGeneralController ---

    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) { 
        System.out.println("\n--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");
        JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra());
        toolbar.setFloatable(true);
        toolbar.setRollover(true);
        toolbar.setBackground(themeManager.getTemaActual().colorFondoPrincipal());

        if (toolbarDef.botones() != null) {
            for (ToolbarButtonDefinition botonDef : toolbarDef.botones()) {
                JComponent componente = crearComponenteIndividual(botonDef); 
                if (componente != null) {
                    toolbar.add(componente); 
                }
            }
        }
        return toolbar;
    } // --- Fin del método buildSingleToolbar ---
    
    
    private JComponent crearComponenteIndividual(ToolbarButtonDefinition definition) { 
        if (definition == null || definition.comandoCanonico() == null) {
            return null;
        }

        Tema temaActual = themeManager.getTemaActual();
        int ancho = this.iconoAncho;
        int alto = this.iconoAlto;

        JComponent componentToBuild; 

        switch (definition.tipoBoton()) {
            case TOGGLE:
                JToggleButton toggleButton = new JToggleButton();
                this.modoDeTrabajoGroup.add(toggleButton); 
                componentToBuild = toggleButton; 
                
                ((AbstractButton)componentToBuild).setMargin(new Insets(2, 2, 2, 2));
                ((AbstractButton)componentToBuild).setBorderPainted(false);
                ((AbstractButton)componentToBuild).setFocusPainted(false);
                ((AbstractButton)componentToBuild).setContentAreaFilled(true);
                componentToBuild.setBackground(temaActual.colorBotonFondo());
                componentToBuild.setOpaque(true);
                ((AbstractButton)componentToBuild).setToolTipText(definition.textoTooltip());
                
                Action actionToggle = this.actionMap.get(definition.comandoCanonico());
                if (actionToggle != null) {
                    ((AbstractButton)componentToBuild).setAction(actionToggle);
                    ((AbstractButton)componentToBuild).setText(null); 
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando TOGGLE: '" + definition.comandoCanonico() + "'");
                    ((AbstractButton)componentToBuild).setText("?");
                    ((AbstractButton)componentToBuild).setActionCommand(definition.comandoCanonico());
                    ((AbstractButton)componentToBuild).addActionListener(this.controllerRef); 
                }

                if (((AbstractButton)componentToBuild).getIcon() == null && definition.claveIcono() != null) {
                    ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
                    ((AbstractButton)componentToBuild).setIcon(icono);
                }
                break;

            case DPAD:
                System.out.println("[ToolbarBuilder] Creando DPadComponent para barra: " + definition.categoriaLayout());
                DPadComponent dpadComponent = new DPadComponent();
                
                Image baseImage = this.iconUtils.getRawCommonImage(definition.claveIcono());
                Image pressedImage = this.iconUtils.getRawCommonImage("D-Pad_all_48x48.png"); 
                Image upImage = this.iconUtils.getRawCommonImage("D-Pad_up_48x48.png");
                Image downImage = this.iconUtils.getRawCommonImage("D-Pad_down_48x48.png");
                Image leftImage = this.iconUtils.getRawCommonImage("D-Pad_Left_48x48.png");
                Image rightImage = this.iconUtils.getRawCommonImage("D-Pad_right_48x48.png");

                dpadComponent.setBaseImage(baseImage);
                dpadComponent.setPressedImage(pressedImage); 
                
                if (actionMap == null) {
                    System.err.println("ERROR [ToolbarBuilder]: ActionMap es null al configurar DPadComponent.");
                    return new JButton("DPAD Err"); 
                }

                Action panUpAction = actionMap.get(AppActionCommands.CMD_PAN_TOP_EDGE);
                Action panDownAction = actionMap.get(AppActionCommands.CMD_PAN_BOTTOM_EDGE);
                Action panLeftAction = actionMap.get(AppActionCommands.CMD_PAN_LEFT_EDGE);
                Action panRightAction = actionMap.get(AppActionCommands.CMD_PAN_RIGHT_EDGE);

                int iconPxSize = 48;
                int zonePxSize = iconPxSize / 3;
                
                dpadComponent.addHotspot(new Hotspot("up", new Rectangle(zonePxSize, 0, zonePxSize, zonePxSize), upImage, panUpAction));
                dpadComponent.addHotspot(new Hotspot("down", new Rectangle(zonePxSize, zonePxSize * 2, zonePxSize, zonePxSize), downImage, panDownAction));
                dpadComponent.addHotspot(new Hotspot("left", new Rectangle(0, zonePxSize, zonePxSize, zonePxSize), leftImage, panLeftAction));
                dpadComponent.addHotspot(new Hotspot("right", new Rectangle(zonePxSize * 2, zonePxSize, zonePxSize, zonePxSize), rightImage, panRightAction));

                componentToBuild = dpadComponent; 
                componentToBuild.setToolTipText(definition.textoTooltip()); 
                break;

            case COLOR_OVERLAY_ICON_BUTTON:
            case CHECKERED_OVERLAY_ICON_BUTTON:
            case NORMAL: 
            default:
                JButton button = new JButton(); 
                componentToBuild = button; 
                
                button.setMargin(new Insets(2, 2, 2, 2));
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setBackground(temaActual.colorBotonFondo());
                button.setOpaque(true);
                button.setToolTipText(definition.textoTooltip());

                Action action = this.actionMap.get(definition.comandoCanonico());
                if (action != null) {
                    button.setAction(action);
                    button.setText(null);
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando: '" + definition.comandoCanonico() + "'");
                    button.setText("?");
                    button.setActionCommand(definition.comandoCanonico());
                    button.addActionListener(this.controllerRef); 
                }

                if (button.getIcon() == null && definition.claveIcono() != null) {
                    if (definition.tipoBoton() == ButtonType.COLOR_OVERLAY_ICON_BUTTON ||
                        definition.tipoBoton() == ButtonType.CHECKERED_OVERLAY_ICON_BUTTON ||
                        definition.comandoCanonico().equals(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR)) {
                        
                        ImageIcon icono = this.iconUtils.getScaledCommonIcon(definition.claveIcono(), ancho, alto);
                        button.setIcon(icono);
                    } else {
                        ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
                        button.setIcon(icono);
                    }
                }
                
                button.putClientProperty("buttonType", definition.tipoBoton());
                button.putClientProperty("baseIconName", definition.claveIcono());
                button.putClientProperty("customOverlayKey", definition.customOverlayKey());
                
                break;
        }

        if (componentToBuild instanceof JButton || componentToBuild instanceof JToggleButton) { 
            String nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
            String claveBaseBoton = ConfigKeys.buildKey(
                "interfaz.boton",
                definition.categoriaLayout(),
                nombreBotonParaClave
            );
            
            // --- CAMBIO ---: Registrar el botón/toggle en el registry y en el mapa local.
            if(componentToBuild instanceof JButton){
                this.botonesPorNombre.put(claveBaseBoton, (JButton) componentToBuild); 
                this.registry.register(claveBaseBoton, (JButton) componentToBuild);
            } else if (componentToBuild instanceof JToggleButton){
                 // Si necesitas un mapa separado para toggles, aquí lo añadirías.
                 // Por ahora lo registramos en el registry general.
                 this.registry.register(claveBaseBoton, (JToggleButton) componentToBuild);
            }
        }
        
        return componentToBuild; 
    } // --- Fin del método crearComponenteIndividual ---

    
     private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null) return "desconocido";
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;
        return resultado.replace('.', '_');
    } // --- Fin del método extraerNombreClave ---

    public Map<String, JButton> getBotonesPorNombre() {
        return Collections.unmodifiableMap(this.botonesPorNombre);
    } // --- Fin del método getBotonesPorNombre ---
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del método setActionMap ---

} // --- FIN de la clase ToolbarBuilder ---

