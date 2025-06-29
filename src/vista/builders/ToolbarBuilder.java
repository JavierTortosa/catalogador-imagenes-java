package vista.builders;

import java.awt.Insets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractButton; 
import javax.swing.Action;
import javax.swing.ButtonGroup; 
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton; 
import javax.swing.JToolBar;
import controlador.VisorController;
import servicios.ConfigKeys;
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
    
    private final int iconoAncho;
    private final int iconoAlto;

    private Map<String, JButton> botonesPorNombre; // Se mantiene como JButton por ahora por simplicidad de la API externa
    
    // --- ButtonGroup para los modos de trabajo ---
    private final ButtonGroup modoDeTrabajoGroup;

    public ToolbarBuilder(
            ThemeManager themeManager,
            IconUtils iconUtils,
            VisorController controller,
            int iconoAncho,
            int iconoAlto
    ) {
        System.out.println("[ToolbarBuilder Constructor] Iniciando...");
        this.themeManager = Objects.requireNonNull(themeManager);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.controllerRef = Objects.requireNonNull(controller);
        
        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;
        
        this.actionMap = new HashMap<>();
        this.botonesPorNombre = new HashMap<>();
        
        // --- Inicializamos el ButtonGroup ---
        this.modoDeTrabajoGroup = new ButtonGroup();

        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    } // --- Fin del constructor ToolbarBuilder ---

    // --- La variable local ahora es AbstractButton ---
    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) {
        System.out.println("\n--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");
        JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra());
        toolbar.setFloatable(true);
        toolbar.setRollover(true);
        toolbar.setBackground(themeManager.getTemaActual().colorFondoPrincipal());

        if (toolbarDef.botones() != null) {
            for (ToolbarButtonDefinition botonDef : toolbarDef.botones()) {
                // El método crearBotonIndividual ahora devuelve el tipo base AbstractButton
                AbstractButton boton = crearBotonIndividual(botonDef);
                if (boton != null) {
                    toolbar.add(boton);
                }
            }
        }
        return toolbar;
    } // --- Fin del método buildSingleToolbar ---

    // --- El método ahora crea JButton o JToggleButton y devuelve AbstractButton ---
    private AbstractButton crearBotonIndividual(ToolbarButtonDefinition definition) {
        if (definition == null || definition.comandoCanonico() == null) {
            return null;
        }

        Tema temaActual = themeManager.getTemaActual();
        int ancho = this.iconoAncho;
        int alto = this.iconoAlto;

        AbstractButton button; // <--- Usamos la clase base

        // --- LÓGICA DE CREACIÓN CONDICIONAL ---
        if (definition.tipoBoton() == ButtonType.TOGGLE) {
            JToggleButton toggleButton = new JToggleButton();
            this.modoDeTrabajoGroup.add(toggleButton); // Añadir al grupo para selección exclusiva
            button = toggleButton;
        } else {
            button = new JButton();
        }

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
            ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
            button.setIcon(icono);
        }

        String nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
        String claveBaseBoton = ConfigKeys.buildKey(
            "interfaz.boton",
            definition.categoriaLayout(),
            nombreBotonParaClave
        );
        
        // Por ahora, solo guardamos JButtons en el mapa externo para mantener la compatibilidad.
        // Si necesitaras acceder a los JToggleButton, habría que cambiar el tipo del mapa.
        if (button instanceof JButton) {
            this.botonesPorNombre.put(claveBaseBoton, (JButton) button);
        }
        
        return button;
    } // --- Fin del método crearBotonIndividual ---
    
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

} // --- Fin de la clase ToolbarBuilder ---

