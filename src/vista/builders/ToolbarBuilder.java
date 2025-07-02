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
        Action action;

        switch (definition.tipoBoton()) {
            case TOGGLE:
                JToggleButton toggleButton = new JToggleButton();
                this.modoDeTrabajoGroup.add(toggleButton);
                componentToBuild = toggleButton;
                ((AbstractButton) componentToBuild).setMargin(new Insets(2, 2, 2, 2));
                ((AbstractButton) componentToBuild).setBorderPainted(false);
                ((AbstractButton) componentToBuild).setFocusPainted(false);
                ((AbstractButton) componentToBuild).setContentAreaFilled(true);
                componentToBuild.setBackground(temaActual.colorBotonFondo());
                componentToBuild.setOpaque(true);
                ((AbstractButton) componentToBuild).setToolTipText(definition.textoTooltip());
                action = this.actionMap.get(definition.comandoCanonico());
                if (action != null) {
                    ((AbstractButton) componentToBuild).setAction(action);
                    ((AbstractButton) componentToBuild).setText(null);
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando TOGGLE: '" + definition.comandoCanonico() + "'");
                    ((AbstractButton) componentToBuild).setText("?");
                    ((AbstractButton) componentToBuild).setActionCommand(definition.comandoCanonico());
                    ((AbstractButton) componentToBuild).addActionListener(this.controllerRef);
                }
                if (((AbstractButton) componentToBuild).getIcon() == null && definition.claveIcono() != null) {
                    ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
                    ((AbstractButton) componentToBuild).setIcon(icono);
                }
                break;

            case DPAD:
                System.out.println("[ToolbarBuilder] Construyendo DPadComponent con Builder...");
                final int DPAD_SIZE = 32;
                Image baseImage = this.iconUtils.getRawCommonImage(definition.claveIcono());
                Image pressedImage = this.iconUtils.getRawCommonImage("D-Pad_all_48x48.png");
                Image upImage = this.iconUtils.getRawCommonImage("D-Pad_up_48x48.png");
                Image downImage = this.iconUtils.getRawCommonImage("D-Pad_down_48x48.png");
                Image leftImage = this.iconUtils.getRawCommonImage("D-Pad_Left_48x48.png");
                Image rightImage = this.iconUtils.getRawCommonImage("D-Pad_right_48x48.png");
                Action panUpAction = actionMap.get(AppActionCommands.CMD_PAN_TOP_EDGE);
                Action panDownAction = actionMap.get(AppActionCommands.CMD_PAN_BOTTOM_EDGE);
                Action panLeftAction = actionMap.get(AppActionCommands.CMD_PAN_LEFT_EDGE);
                Action panRightAction = actionMap.get(AppActionCommands.CMD_PAN_RIGHT_EDGE);
                int zoneSize = DPAD_SIZE / 3;
                Rectangle upBounds = new Rectangle(zoneSize, 0, zoneSize, zoneSize);
                Rectangle downBounds = new Rectangle(zoneSize, zoneSize * 2, zoneSize, zoneSize);
                Rectangle leftBounds = new Rectangle(0, zoneSize, zoneSize, zoneSize);
                Rectangle rightBounds = new Rectangle(zoneSize * 2, zoneSize, zoneSize, zoneSize);
                componentToBuild = new DPadComponent.Builder().withSize(DPAD_SIZE, DPAD_SIZE).withBaseImage(baseImage).withPressedImage(pressedImage).withManualHotspot("up", upBounds, upImage, panUpAction).withManualHotspot("down", downBounds, downImage, panDownAction).withManualHotspot("left", leftBounds, leftImage, panLeftAction).withManualHotspot("right", rightBounds, rightImage, panRightAction).build();
                componentToBuild.setToolTipText(definition.textoTooltip());
                break;

            case TRANSPARENT:
                JButton transparentButton = new JButton();
                componentToBuild = transparentButton;
                transparentButton.setOpaque(false);
                transparentButton.setContentAreaFilled(false);
                transparentButton.setBorderPainted(false);
                transparentButton.setFocusPainted(false);
                transparentButton.setBorder(null);
                transparentButton.setToolTipText(definition.textoTooltip());
                action = this.actionMap.get(definition.comandoCanonico());
                if (action != null) {
                    transparentButton.setAction(action);
                    transparentButton.setText(null);
                    ImageIcon baseIcon = this.iconUtils.getScaledCommonIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                    transparentButton.setIcon(baseIcon);
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando TRANSPARENT: '" + definition.comandoCanonico() + "'");
                    transparentButton.setText("?");
                    transparentButton.setActionCommand(definition.comandoCanonico());
                    transparentButton.addActionListener(this.controllerRef);
                }
                break;

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
                action = this.actionMap.get(definition.comandoCanonico());
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
                    ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
                    button.setIcon(icono);
                }
                break;
        }

        if (componentToBuild instanceof AbstractButton) {
            String nombreBotonParaClave;
            if (AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE.equals(definition.comandoCanonico())) {
                nombreBotonParaClave = extraerNombreClave(definition.claveIcono());
            } else {
                nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
            }
            String claveBaseBoton = ConfigKeys.buildKey("interfaz.boton", definition.categoriaLayout(), nombreBotonParaClave);
            if (componentToBuild instanceof JButton) {
                this.botonesPorNombre.put(claveBaseBoton, (JButton) componentToBuild);
            }
            this.registry.register(claveBaseBoton, (JComponent) componentToBuild);
            ((AbstractButton) componentToBuild).putClientProperty("buttonType", definition.tipoBoton());
            ((AbstractButton) componentToBuild).putClientProperty("baseIconName", definition.claveIcono());
            ((AbstractButton) componentToBuild).putClientProperty("customOverlayKey", definition.customOverlayKey());
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

