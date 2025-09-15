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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.VisorController;
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import vista.components.DPadComponent;
import vista.components.ThemedToggleButton;
import vista.config.ButtonType;
import vista.config.HotspotDefinition;
import vista.config.IconScope;
import vista.config.LabelDefinition;
import vista.config.SeparatorDefinition;
import vista.config.TextFieldDefinition;
import vista.config.ToolbarAlignment;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


public class ToolbarBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(ToolbarBuilder.class);

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
    
    public ToolbarBuilder(
            ThemeManager themeManager,
            IconUtils iconUtils,
            VisorController controller,
            int iconoAncho,
            int iconoAlto,
            ComponentRegistry registry 
    ) {
    	
        logger.debug("[ToolbarBuilder Constructor] Iniciando...");
        
        this.themeManager = Objects.requireNonNull(themeManager);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.controllerRef = Objects.requireNonNull(controller);
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en ToolbarBuilder.");
        
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;
        
        this.actionMap = new HashMap<>();
        this.botonesPorNombre = new HashMap<>();
        
        logger.debug("[ToolbarBuilder Constructor] Finalizado.");
        
    } // --- Fin del método ToolbarBuilder (constructor) ---

    
    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) { 
        logger.info("--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");
        
        final JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra());
        toolbar.setFloatable(true);
        toolbar.setRollover(true);
        toolbar.putClientProperty("toolbarDefinition", toolbarDef);

        // Comprobamos si el alineamiento de esta barra es 'FREE'.
        // Si lo es, significa que está diseñada para integrarse en un panel
        // personalizado (como la barra de estado roja o los controles de imagen),
        // y por lo tanto, debe ser transparente.
        
        if (toolbarDef.alignment() == ToolbarAlignment.FREE) {
            logger.debug("    -> Barra con alineamiento FREE detectada ('{}'). Aplicando transparencia.", toolbarDef.claveBarra());
            
            // La instrucción clave para FlatLaf: no pintar el fondo.
            toolbar.putClientProperty("FlatLaf.style", "background: null");
            
            // También usamos el método estándar de Swing para asegurar el comportamiento.
            toolbar.setOpaque(false);
        }
        
        // La lista ahora es 'vista', 'zoom' y 'modo'
        List<String> groupToolbarKeys = List.of("vista", "zoom", "modo");
        
        ButtonGroup group = null;
        if (groupToolbarKeys.contains(toolbarDef.claveBarra())) {
            group = new ButtonGroup();
            this.radioGroups.put(toolbarDef.claveBarra(), group);
            logger.debug("    -> Creado ButtonGroup para la barra: '" + toolbarDef.claveBarra() + "'");
        }

        if (toolbarDef.componentes() != null) {
            // Ahora iteramos sobre la lista genérica "componentes"
            for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                
                // Usamos 'instanceof' para saber qué tipo de componente es
                if (compDef instanceof ToolbarButtonDefinition botonDef) {
                    // Si es un botón, llamamos al método que ya teníamos
                    JComponent componenteBoton = crearComponenteIndividual(botonDef); 
                    if (componenteBoton != null) {
                    	toolbar.add(componenteBoton);
                    	if (group != null && componenteBoton instanceof JToggleButton) {
                    		group.add((JToggleButton) componenteBoton);
                    		logger.debug("      -> Botón Toggle '" + botonDef.comandoCanonico() + "' añadido al ButtonGroup.");
                        }
                    }
                } else if (compDef instanceof LabelDefinition labelDef) {
                    // Si es un label, lo creamos aquí mismo
                    javax.swing.JLabel speedLabel = new javax.swing.JLabel(labelDef.textoDefecto(), javax.swing.SwingConstants.CENTER);
                    speedLabel.setPreferredSize(new Dimension(60, 25));
                    speedLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
                    
                    registry.unregister(labelDef.id());
                    registry.register(labelDef.id(), speedLabel);
                    
                    toolbar.add(speedLabel);
                    logger.debug("    -> JLabel '" + labelDef.id() + "' añadido a la barra.");

                } else if (compDef instanceof SeparatorDefinition sepDef) {
                    // Comprobamos si el separador es elástico
                    if (sepDef.elastic()) {
                        // Si es elástico, añadimos un "muelle" que ocupa el espacio sobrante
                        toolbar.add(javax.swing.Box.createHorizontalGlue());
                        logger.debug("    -> Separador elástico (Glue) añadido a la barra.");
                    } else {
                        // Si no, añadimos un separador visual normal
                        toolbar.addSeparator();
                        logger.debug("    -> Separador visual añadido a la barra.");
                    }
                
                } else if (compDef instanceof TextFieldDefinition textFieldDef) {
                    // Usamos el número de columnas de la definición.
                    javax.swing.JTextField textField = new javax.swing.JTextField(textFieldDef.textoPorDefecto(), textFieldDef.columns());
                    textField.setToolTipText("Ruta de destino para la exportación");
                    
                    // LA CLAVE: Limitamos su tamaño máximo a su tamaño preferido.
                    // Esto evita que "robe" todo el espacio horizontal.
                    textField.setMaximumSize(textField.getPreferredSize());
                    
                    registry.register(textFieldDef.comandoCanonico(), textField);
                    toolbar.add(textField);
                    logger.debug("    -> JTextField '" + textFieldDef.comandoCanonico() + "' añadido a la barra.");
                }
            }
        }
        // ***** FIN DE LA CORRECCIÓN DEL BUCLE *****
        
        return toolbar;
        
    } // --- Fin del método buildSingleToolbar ---
    
    
    private JComponent crearComponenteIndividual(ToolbarButtonDefinition definition) {
    	
        // --- 1. VALIDACIONES INICIALES ---
        if (definition == null || definition.comandoCanonico() == null) {
            logger.error("ERROR [ToolbarBuilder.crearComponenteIndividual]: Definición de botón o comando canónico es nulo.");
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
                abstractButtonComponent = new ThemedToggleButton(this.themeManager, associatedAction);
                abstractButtonComponent.putClientProperty("JButton.buttonType", "regular");
                break;

            case COLOR_OVERLAY_ICON_BUTTON:
            case CHECKERED_OVERLAY_ICON_BUTTON:
                // Estos botones se crean como JButtons normales, su icono especial se pone después
                JButton specialButton = new JButton();
                specialButton.setOpaque(true);
                specialButton.setContentAreaFilled(true);
                specialButton.setBorderPainted(false);
                specialButton.setFocusPainted(false);
                specialButton.setBorder(null);
                abstractButtonComponent = specialButton;
                break;

            case TRANSPARENT:
                JButton transparentButton = new JButton();
                transparentButton.setOpaque(false);
                transparentButton.setContentAreaFilled(false);
                transparentButton.setBorderPainted(false);
                transparentButton.setFocusPainted(false);
                transparentButton.setBorder(null);
                abstractButtonComponent = transparentButton;
                break;

            case STATUS_BAR_BUTTON:
                JButton statusBarButton = new JButton();
                statusBarButton.setOpaque(false);
                statusBarButton.setContentAreaFilled(false);
                statusBarButton.setBorderPainted(false); // Muy importante para que se integre
                statusBarButton.setFocusPainted(false);
                abstractButtonComponent = statusBarButton;
                break;
                
            case DPAD_CRUZ:
            case DPAD_GRID:
                break; // Se manejan después

            default:
                logger.error("ERROR [ToolbarBuilder.crearComponenteIndividual]: Tipo de botón no reconocido: " + definition.tipoBoton());
                abstractButtonComponent = new JButton();
                break;
        }

        // --- 4. LÓGICA COMÚN PARA AbstractButton ---
        if (abstractButtonComponent != null) {
            abstractButtonComponent.setMargin(new Insets(2, 2, 2, 2));
            abstractButtonComponent.setToolTipText(definition.textoTooltip());

            if (associatedAction != null) {
                abstractButtonComponent.setAction(associatedAction);
            } else {
                abstractButtonComponent.setActionCommand(definition.comandoCanonico());
                abstractButtonComponent.addActionListener(this.controllerRef);
            }
            abstractButtonComponent.setText(null);

            
            // 1. Obtener SIEMPRE el icono FRESCO desde IconUtils, ignorando el que pueda tener la Action.
            //    Esto asegura que usamos el icono del tema actual, ya que IconUtils depende de ThemeManager.
            Icon finalIcon = null;
            if (definition.claveIcono() != null && !definition.claveIcono().isBlank()) {
                finalIcon = (definition.scopeIconoBase() == IconScope.COMMON)
                    ? this.iconUtils.getScaledCommonIcon(definition.claveIcono(), targetIconWidth, targetIconHeight)
                    : this.iconUtils.getScaledIcon(definition.claveIcono(), targetIconWidth, targetIconHeight);
            }
            
            // 2. Asignar explícitamente el icono al botón DESPUÉS de haber asignado la Action.
            //    Esto sobreescribe el icono que la Action pudiera haber puesto.
            if (finalIcon != null) {
                abstractButtonComponent.setIcon(finalIcon);
            }
            
            // Asignar iconos especiales a los botones de color/cuadros
            if (definition.tipoBoton() == ButtonType.COLOR_OVERLAY_ICON_BUTTON) {
                String colorKey = definition.textoTooltip();
                java.awt.Color initialColor = themeManager.getFondoSecundarioParaTema(colorKey);
                abstractButtonComponent.setIcon(this.iconUtils.getColoredOverlayIcon(definition.claveIcono(), initialColor, targetIconWidth, targetIconHeight));
            } else if (definition.tipoBoton() == ButtonType.CHECKERED_OVERLAY_ICON_BUTTON) {
                abstractButtonComponent.setIcon(this.iconUtils.getCheckeredOverlayIcon(definition.claveIcono(), targetIconWidth, targetIconHeight));
            }

            if (abstractButtonComponent.getIcon() != null) {
                abstractButtonComponent.putClientProperty("baseIcon", abstractButtonComponent.getIcon());
            }

            String nombreBotonParaClave = ConfigKeys.keyPartFromCommand(definition.comandoCanonico());
            String claveBaseBoton = ConfigKeys.buildKey("interfaz.boton", definition.categoriaLayout(), nombreBotonParaClave);
            
            this.registry.register(claveBaseBoton, abstractButtonComponent);
            
            abstractButtonComponent.putClientProperty("buttonType", definition.tipoBoton());
            abstractButtonComponent.putClientProperty("baseIconName", definition.claveIcono());
            abstractButtonComponent.putClientProperty("buttonConfigKey", claveBaseBoton);
            abstractButtonComponent.putClientProperty("canonicalCommand", definition.comandoCanonico());

            finalComponent = abstractButtonComponent;

        } else if (definition.tipoBoton() == ButtonType.DPAD_CRUZ || definition.tipoBoton() == ButtonType.DPAD_GRID) {
            logger.debug("[ToolbarBuilder.crearComponenteIndividual] Construyendo DPadComponent para tipo: " + definition.tipoBoton());
            final int DPAD_DISPLAY_SIZE = 32;

            if (definition.listaDeHotspots() == null || definition.listaDeHotspots().isEmpty()) {
                logger.error("ERROR [ToolbarBuilder.crearComponenteIndividual]: La definición de DPAD_CRUZ/GRID no contiene una lista de hotspots.");
                return new JButton("Error D-Pad");
            }
            
            Image dpadBaseImage = (definition.scopeIconoBase() == IconScope.COMMON)
                                ? iconUtils.getRawCommonImage(definition.claveIcono())
                                : iconUtils.getRawIcon(definition.claveIcono());
            Image pressedImage = iconUtils.getRawCommonImage("d-pad_all_48x48.png"); //FIXME cambiar este icono por el de UIDefinitionService 
            
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

            DPadComponent dpadComponent = DPadComponent.createCrossLayout(
                new Dimension(DPAD_DISPLAY_SIZE, DPAD_DISPLAY_SIZE),
                dpadBaseImage, pressedImage, hotspotKeys, hoverImages, hotspotActions
            );
            dpadComponent.setToolTipText(definition.textoTooltip());
            
            finalComponent = dpadComponent;
            
            String dpadClave = ConfigKeys.buildKey(
            		"interfaz.dpad", 
            		definition.categoriaLayout(), 
            		ConfigKeys.keyPartFromCommand(definition.comandoCanonico())
            		);
            this.registry.register(dpadClave, finalComponent);
        } else {
            logger.error("ERROR [ToolbarBuilder.crearComponenteIndividual]: No se pudo crear componente. Tipo no manejado: " + definition.tipoBoton());
            return null;
        }

        return finalComponent;

    } // --- Fin del método crearComponenteIndividual ---
    
    
     public Map<String, AbstractButton> getBotonesPorNombre() {
         return Collections.unmodifiableMap(this.botonesPorNombre);
     }
    
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del método setActionMap ---
    
    
} // --- FIN de la clase ToolbarBuilder ---

