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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import vista.components.DPadComponent;
import vista.config.IconScope;
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
    private final ComponentRegistry registry;

    private final int iconoAncho;
    private final int iconoAlto;

    private Map<String, JButton> botonesPorNombre;
    
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

        // Almacenamos la definición en la propia toolbar para poder acceder a ella.
        toolbar.putClientProperty("toolbarDefinition", toolbarDef);

        // El resto del método no cambia.
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

        int ancho = this.iconoAncho;
        int alto = this.iconoAlto;
        Action action = this.actionMap.get(definition.comandoCanonico());
        Tema temaActual = themeManager.getTemaActual();

        JComponent componentToBuild;

        // Trataremos NORMAL y TOGGLE de la misma forma para crear siempre un JButton
        // y tener control manual sobre su apariencia de "activado".
        switch (definition.tipoBoton()) {
            case TOGGLE: 
            case NORMAL:
            default:
                JButton button = new JButton();
                componentToBuild = button;
                
                button.setMargin(new Insets(2, 2, 2, 2));
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setBackground(temaActual.colorBotonFondo());
                
                button.setToolTipText(definition.textoTooltip());
                
                if (action != null) {
                    button.setAction(action);
                    button.setText(null);
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando '" + definition.comandoCanonico() + "'");
                    button.setText("?");
                    button.setActionCommand(definition.comandoCanonico());
                    button.addActionListener(this.controllerRef);
                }
                
                if (button.getIcon() == null && definition.claveIcono() != null) {
                    ImageIcon icono = (definition.scopeIconoBase() == IconScope.COMMON)
                                    ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), ancho, alto)
                                    : this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
                    button.setIcon(icono);
                }
                break;

            case DPAD_CRUZ:
                System.out.println("[ToolbarBuilder] Construyendo DPadComponent con Fábrica Estática...");
                final int DPAD_SIZE = 32;

                if (definition.listaDeHotspots() == null || definition.listaDeHotspots().isEmpty()) {
                    System.err.println("ERROR [ToolbarBuilder]: La definición de DPAD_CRUZ no contiene una lista de hotspots.");
                    return new JButton("Error D-Pad");
                }
                
                Image baseImage = (definition.scopeIconoBase() == IconScope.COMMON)
                                ? iconUtils.getRawCommonImage(definition.claveIcono())
                                : iconUtils.getRawIcon(definition.claveIcono());
                Image pressedImage = iconUtils.getRawCommonImage("D-Pad_all_48x48.png");

                List<String> hotspotKeys = new ArrayList<>();
                List<Image> hoverImages = new ArrayList<>();
                List<Action> actions = new ArrayList<>();
                
                for (int i = 0; i < definition.listaDeHotspots().size(); i++) {
                    var hotspotDef = definition.listaDeHotspots().get(i);
                    Action hotspotAction = actionMap.get(hotspotDef.comando());
                    Image hoverImage = (hotspotDef.scope() == IconScope.COMMON)
                                     ? iconUtils.getRawCommonImage(hotspotDef.icono())
                                     : iconUtils.getRawIcon(hotspotDef.icono());
                    
                    hotspotKeys.add(definition.comandoCanonico() + "-hotspot-" + i);
                    hoverImages.add(hoverImage);
                    actions.add(hotspotAction);
                }

                componentToBuild = DPadComponent.createCrossLayout(
                    new Dimension(DPAD_SIZE, DPAD_SIZE),
                    baseImage, pressedImage, hotspotKeys, hoverImages, actions
                );
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
                
                if (action != null) {
                    transparentButton.setAction(action);
                    transparentButton.setText(null);
                    ImageIcon baseIcon = (definition.scopeIconoBase() == IconScope.COMMON)
                                       ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto)
                                       : this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                    transparentButton.setIcon(baseIcon);
                } else {
                    System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando TRANSPARENT: '" + definition.comandoCanonico() + "'");
                    transparentButton.setText("?");
                    transparentButton.setActionCommand(definition.comandoCanonico());
                    transparentButton.addActionListener(this.controllerRef);
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
            this.registry.register(claveBaseBoton, componentToBuild);
            ((AbstractButton) componentToBuild).putClientProperty("buttonType", definition.tipoBoton());
            ((AbstractButton) componentToBuild).putClientProperty("baseIconName", definition.claveIcono());
        }

        if (componentToBuild instanceof DPadComponent) {
            String dpadClave = ConfigKeys.buildKey("interfaz.dpad", definition.categoriaLayout(), extraerNombreClave(definition.comandoCanonico()));
            this.registry.register(dpadClave, componentToBuild);
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

