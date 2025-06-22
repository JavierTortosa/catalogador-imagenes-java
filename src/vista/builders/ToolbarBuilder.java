// Archivo: ToolbarBuilder.java

package vista.builders;

import java.awt.Insets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import controlador.VisorController;
import servicios.ConfigKeys;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class ToolbarBuilder {

    // --- Dependencias Clave ---
    private Map<String, Action> actionMap;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;
    private final VisorController controllerRef;
    
    // <<< AÑADIR/MANTENER ESTOS CAMPOS >>>
    private final int iconoAncho;
    private final int iconoAlto;

    // --- Resultados del Builder ---
    private Map<String, JButton> botonesPorNombre;

    // --- CONSTRUCTOR REFACTORIZADO Y CORREGIDO ---
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
        
        // El actionMap se inyectará después. Lo inicializamos a un mapa vacío
        // para evitar NullPointerExceptions si se llama a algún método antes del cableado.
        this.actionMap = new HashMap<>();
        
        this.botonesPorNombre = new HashMap<>();
        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
    } // --- Fin del constructor ToolbarBuilder ---
    
    
//    public ToolbarBuilder(
//            Map<String, Action> actionMap,
//            ThemeManager themeManager,
//            IconUtils iconUtils,
//            VisorController controller,
//            int iconoAncho,   // <<< NUEVO PARÁMETRO
//            int iconoAlto     // <<< NUEVO PARÁMETRO
//    ) {
//        System.out.println("[ToolbarBuilder Constructor] Iniciando...");
//        this.actionMap = Objects.requireNonNull(actionMap);
//        this.themeManager = Objects.requireNonNull(themeManager);
//        this.iconUtils = Objects.requireNonNull(iconUtils);
//        this.controllerRef = Objects.requireNonNull(controller);
//        
//        // Guardar los tamaños de los iconos
//        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
//        this.iconoAlto = (iconoAlto > 0) ? iconoAlto : 24;
//        
//        this.botonesPorNombre = new HashMap<>();
//        System.out.println("[ToolbarBuilder Constructor] Finalizado.");
//    } // --- Fin del constructor ToolbarBuilder ---

    public JToolBar buildSingleToolbar(ToolbarDefinition toolbarDef) {
        // ... (este método no necesita cambios) ...
        System.out.println("\n--- [ToolbarBuilder] Construyendo barra: '" + toolbarDef.titulo() + "' ---");
        JToolBar toolbar = new JToolBar(toolbarDef.titulo());
        toolbar.setName(toolbarDef.claveBarra());
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBackground(themeManager.getTemaActual().colorFondoPrincipal());

        if (toolbarDef.botones() != null) {
            for (ToolbarButtonDefinition botonDef : toolbarDef.botones()) {
                JButton boton = crearBotonIndividual(botonDef);
                if (boton != null) {
                    toolbar.add(boton);
                }
            }
        }
        return toolbar;
    } // --- Fin del método buildSingleToolbar ---

    private JButton crearBotonIndividual(ToolbarButtonDefinition definition) {
        if (definition == null || definition.comandoCanonico() == null) {
            return null;
        }

        Tema temaActual = themeManager.getTemaActual();
        
        // <<< CORRECCIÓN: Usamos los campos de la clase, no llamamos a getters inexistentes >>>
        int ancho = this.iconoAncho;
        int alto = this.iconoAlto;

        JButton button = new JButton();
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
            // Usamos las variables locales 'ancho' y 'alto'
            ImageIcon icono = this.iconUtils.getScaledIcon(definition.claveIcono(), ancho, alto);
            button.setIcon(icono);
        }

        String nombreBotonParaClave = extraerNombreClave(definition.comandoCanonico());
        String claveBaseBoton = ConfigKeys.buildKey(
            "interfaz.boton",
            definition.categoriaLayout(),
            nombreBotonParaClave
        );
        this.botonesPorNombre.put(claveBaseBoton, button);
        
        return button;
    } // --- Fin del método crearBotonIndividual ---

    // ... (resto de la clase: extraerNombreClave, getBotonesPorNombre) ...
     private String extraerNombreClave(String comandoCanonico) {
        if (comandoCanonico == null) return "desconocido";
        String resultado = comandoCanonico.startsWith("cmd.") ? comandoCanonico.substring(4) : comandoCanonico;
        resultado = resultado.startsWith("toggle.") ? resultado.substring(7) : resultado;
        return resultado.replace('.', '_');
    }

    public Map<String, JButton> getBotonesPorNombre() {
        return Collections.unmodifiableMap(this.botonesPorNombre);
    }
    
    
    /**
     * Inyecta el mapa de acciones de la aplicación.
     * @param actionMap El mapa de acciones (comando -> Action).
     */
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del método setActionMap ---
    

} // --- Fin de la clase ToolbarBuilder ---