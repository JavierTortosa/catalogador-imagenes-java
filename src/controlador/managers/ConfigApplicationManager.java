package controlador.managers;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import vista.VisorView; // Mantener solo si es necesario para el JOptionPane o setAlwaysOnTop
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class ConfigApplicationManager {

    // --- DEPENDENCIAS REFACTORIZADAS ---
    private final VisorModel model;
    private final ConfigurationManager config;
    private final ThemeManager themeManager;
    private final ComponentRegistry registry;
    private Map<String, Action> actionMap;
    private VisorView view;
    
    // --- ESTADO ---
    private final Map<String, String> configAlInicio;
    

    // --- CONSTRUCTOR REFACTORIZADO ---
    
    public ConfigApplicationManager(
            VisorModel model, 
            ConfigurationManager config, 
            ThemeManager themeManager,
            ComponentRegistry registry
            ) {
        
        System.out.println("[ConfigApplicationManager] Creando instancia refactorizada...");
        
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.registry = Objects.requireNonNull(registry);
        
        // 'view' y 'actionMap' serán nulos aquí. Se inyectarán después.
        this.actionMap = new HashMap<>(); // Inicializar a un mapa vacío para evitar NullPointerException

        this.configAlInicio = new HashMap<>(config.getConfigMap());
        System.out.println("[ConfigApplicationManager] Instancia creada.");
    } // --- Fin del Constructor ---
    

    // --- MÉTODOS PÚBLICOS ---
     
    public void aplicarConfiguracionGlobalmente() {
        System.out.println("--- [ConfigApplicationManager] Aplicando configuración globalmente... ---");
        aplicarConfiguracionAlModelo();
        SwingUtilities.invokeLater(() -> {
            aplicarConfiguracionAlaVista();
            sincronizarUIFinal();
        });
    } // --- Fin del método aplicarConfiguracionGlobalmente ---

    public void restaurarConfiguracionPredeterminada() {
        System.out.println("--- [ConfigApplicationManager] Restaurando configuración predeterminada... ---");
        this.config.resetToDefaults();
        aplicarConfiguracionGlobalmente();
        
        JFrame mainFrame = registry.get("frame.main");
        JOptionPane.showMessageDialog(mainFrame, "La configuración ha sido restaurada a los valores de fábrica.", "Configuración Restaurada", JOptionPane.INFORMATION_MESSAGE);
    } // --- Fin del método restaurarConfiguracionPredeterminada ---

    public void guardarConfiguracionActual() {
        System.out.println("--- [ConfigApplicationManager] Guardando configuración actual... ---");
        try {
            this.config.guardarConfiguracion(this.config.getConfigMap());
            JFrame mainFrame = registry.get("frame.main");
            JOptionPane.showMessageDialog(mainFrame, "La configuración actual ha sido guardada en config.cfg.", "Configuración Guardada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            // ...
        }
    } // --- Fin del método guardarConfiguracionActual ---


    // --- MÉTODOS PRIVADOS DE AYUDA ---

    private void aplicarConfiguracionAlModelo() {
        // ... (Este método no necesita cambios, ya que solo interactúa con el modelo y la config) ...
    } // --- Fin del método aplicarConfiguracionAlModelo ---

    private void aplicarConfiguracionAlaVista() {
        System.out.println("  [ConfigAppManager] Aplicando configuración a la Vista (usando Registry)...");
        
        // La lógica anterior que iteraba sobre view.getBotonesPorNombre() y
        // view.getMenuItemsPorNombre() ahora puede iterar sobre los componentes del registro.
        
        for(String key : registry.getAllComponentKeys()) {
        	
            if (key.startsWith("interfaz.boton.")) {
            	AbstractButton  button = registry.get(key);
                if (button != null) {
                    button.setVisible(config.getBoolean(key + ".visible", true));
                    // El estado 'enabled' es mejor que lo gestione la propia Action.
                }
            } else if (key.startsWith("interfaz.menu.")) {
                JMenuItem menuItem = registry.get(key);
                if (menuItem != null) {
                    // Aquí iría la lógica para aplicar estado a los menús si es necesario,
                    // como habilitar/deshabilitar, pero de nuevo, la Action es el mejor lugar.
                }
            }
        }
        
        // --- INICIO DE LA MODIFICACIÓN ---
        // Aplicar el estado inicial del fondo a cuadros, que es una configuración de la vista.
        
        // 1. Definimos la clave de configuración que controla el estado persistente.
        String configKeyFondo = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        
        // 2. Leemos el valor de la configuración. Si no existe, por defecto es 'false'.
        boolean fondoACuadrosInicial = this.config.getBoolean(configKeyFondo, false);
        
        // 3. Obtenemos el panel de visualización desde el registro.
        vista.panels.ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");

        if (displayPanel != null) {
            System.out.println("    -> Aplicando estado inicial de fondo a cuadros desde config: " + fondoACuadrosInicial);
            // 4. Le decimos al panel que establezca su estado inicial.
            //    Si 'fondoACuadrosInicial' es true, se pondrá a cuadros.
            //    Si es false, se pondrá de un color sólido (el del tema actual),
            //    porque la lógica interna del panel se encargará de eso.
            displayPanel.setCheckeredBackground(fondoACuadrosInicial);
        } else {
            System.err.println("WARN [ConfigAppManager]: No se encontró 'panel.display.imagen' para aplicar el fondo inicial.");
        }
        // --- FIN DE LA MODIFICACIÓN ---
        
        System.out.println("    -> Configuración básica de Vista aplicada.");
    } // --- Fin del método aplicarConfiguracionAlaVista ---

    
    private void sincronizarUIFinal() {
        System.out.println("  [ConfigAppManager] Sincronizando UI final...");
        
        if (actionMap == null || actionMap.isEmpty()) {
            System.err.println("WARN [ConfigAppManager]: ActionMap vacío, no se puede sincronizar la UI final.");
            return;
        }

        // --- 1. Sincronizar el estado lógico de TODOS los botones toggle ---
        //    Esto asegura que los botones reflejen el estado cargado desde la config.
        System.out.println("    -> Sincronizando estado lógico de todos los botones toggle...");
        for (Action action : actionMap.values()) {
            Object selectedValue = action.getValue(Action.SELECTED_KEY);
            // Comprobamos si la Action tiene un estado de selección booleano.
            if (selectedValue instanceof Boolean) {
                // Llamamos al método helper para que fuerce la sincronización visual del botón.
                actualizarAspectoBotonToggle(action, (Boolean) selectedValue);
            }
        }
        System.out.println("    -> Sincronización de estado lógico de toggles completada.");

        // --- 2. Sincronizar estados específicos de la ventana ---
        JFrame mainFrame = registry.get("frame.main");
        if (mainFrame == null) return;

        // a) Sincronizar "Siempre Encima"
        Action alwaysOnTopAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
        if (alwaysOnTopAction != null) {
            boolean estadoTop = Boolean.TRUE.equals(alwaysOnTopAction.getValue(Action.SELECTED_KEY));
            if (mainFrame.isAlwaysOnTop() != estadoTop) {
                mainFrame.setAlwaysOnTop(estadoTop);
            }
        }

        // --- 3. (Opcional pero recomendado) Refrescar la UI ---
        //    A veces, después de cambiar muchos estados, un revalidate/repaint ayuda.
        if (mainFrame != null) {
            mainFrame.revalidate();
            mainFrame.repaint();
        }
        
        System.out.println("  [ConfigAppManager] Sincronización de UI final completada.");
    } // --- Fin del método sincronizarUIFinal ---
    

    public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) {
        // --- MÉTODO SIMPLIFICADO ---
        // Ya no cambiaremos el color del botón de zoom aquí.
        // Solo nos preocupamos de habilitar/deshabilitar el botón de reset.
        
        // Obtenemos la Action de reset desde el mapa de acciones.
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        
        if (resetAction != null) {
            // Habilitamos o deshabilitamos la Action. Los componentes asociados
            // (botón y menú item) se actualizarán automáticamente.
            resetAction.setEnabled(resetHabilitado);
        }
    } // --- FIN del metodo actualizarEstadoControlesZoom ---
    
    
    /**
     * Aplica una animación visual breve a un botón de la barra de herramientas.
     * Busca el botón asociado a un ActionCommand y cambia su color de fondo
     * temporalmente.
     * @param actionCommandDelBoton El ActionCommand del botón a animar.
     */
    public void aplicarAnimacionBoton(String actionCommandDelBoton) {
        if (actionCommandDelBoton == null || actionCommandDelBoton.isBlank()) {
            return;
        }

        // Buscar el botón que tiene la Action con el ActionCommand especificado.
        // Ahora iteramos sobre TODOS los JButtons registrados.
        JButton botonParaAnimar = null;
        for (JButton boton : registry.getAllComponentsOfType(JButton.class)) {
            Action action = boton.getAction();
            if (action != null && actionCommandDelBoton.equals(action.getValue(Action.ACTION_COMMAND_KEY))) {
                botonParaAnimar = boton;
                break;
            }
        }

        if (botonParaAnimar == null) {
            return;
        }

        // Obtener colores del tema actual (el manager ya tiene 'themeManager')
        Tema tema = this.themeManager.getTemaActual();
        if (tema == null) return;

        final JButton finalBoton = botonParaAnimar;
        final Color colorOriginal = finalBoton.getBackground();
        final Color colorAnimacion = tema.colorBotonFondoAnimacion();

        Timer timer = new Timer(150, e -> {
            finalBoton.setBackground(colorOriginal);
        });
        timer.setRepeats(false);

        finalBoton.setBackground(colorAnimacion);
        timer.start();
    } // --- FIN del metodo aplicarAnimacionBoton ---
    
    
    /**
     * Sincroniza el estado visual y lógico de un botón toggle.
     * Esta versión aplica manualmente los colores de fondo y texto si el botón
     * es un JToggleButton, para asegurar que el tema se aplique correctamente
     * incluso si FlatLaf no lo hace por defecto para estos componentes.
     *
     * @param action La Action cuyo botón asociado se va a actualizar.
     * @param isSelected El estado de selección (true si está activado, false si no).
     */
    public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        if (action == null) {
            return;
        }

        AbstractButton button = null;
        for (java.awt.Component comp : registry.getAllComponents()) {
            if (comp instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) comp;
                if (action.equals(btn.getAction())) {
                    button = btn;
                    break;
                }
            }
        }
        
        if (button == null) {
            return; 
        }

        // --- INICIO CORRECCIÓN CLAVE: Lógica de selección y HABILITACIÓN/DESHABILITACIÓN ---
        // 1. Sincronizar el estado .isSelected() del botón con el de la Action.
        if (button.isSelected() != isSelected) {
            button.setSelected(isSelected);
        }

        // 2. Control de habilitación/deshabilitación:
        // Para JToggleButtons que actúan como radios (seleccionadores de modo),
        // no los deshabilitamos cuando están seleccionados. Esto es crucial
        // para que FlatLaf les aplique los colores de 'selected' correctamente
        // y para que nuestro setBackground manual funcione.
        if (button instanceof JToggleButton) {
            // Un JToggleButton que está seleccionado y forma parte de un ButtonGroup
            // generalmente no se deshabilita, sino que simplemente está "presionado".
            // La visibilidad de su color de fondo depende de setOpaque(true).
            // Lo habilitamos siempre (a menos que la Action misma esté globalmente deshabilitada).
            // setEnabled(true); // Podría causar problemas si la acción debería estar deshabilitada por otra razón.
            // Mejor: si la Action está deshabilitada (por ejemplo, porque no hay imagen), que se mantenga deshabilitada.
            // Si la Action está habilitada, entonces el botón también lo estará.
            button.setEnabled(action.isEnabled()); // <-- Asegura que el estado enabled del botón siga al de la Action.
                                                  // <-- Y NO LO DESHabilita si está seleccionado.
        } else {
            // Para otros tipos de AbstractButton, la lógica anterior de habilitación podría ser válida
            // si se quiere que se deshabiliten visualmente cuando no tienen sentido.
            // Para JButtons normales, su estado 'enabled' no afecta el color de fondo de esta manera.
            button.setEnabled(action.isEnabled());
        }
        // --- FIN CORRECCIÓN CLAVE ---


        // --- LÓGICA DE PINTADO MANUAL PARA JToggleButtons ---
        // (La que ya habíamos acordado y probado que funciona con Color.RED)
        if (button instanceof JToggleButton) {
            Tema tema = themeManager.getTemaActual();
            if (tema == null) {
                System.err.println("WARN [actualizarAspectoBotonToggle]: Tema actual es nulo. No se pueden aplicar colores manuales.");
                return; 
            }

            button.setOpaque(true);
            button.setContentAreaFilled(true); 

            if (isSelected) {
                button.setBackground(tema.colorBotonFondoActivado());
                button.setForeground(tema.colorSeleccionTexto());
            } else {
                button.setBackground(tema.colorBotonFondo());
                button.setForeground(tema.colorBotonTexto());
            }
            button.repaint(); 
            System.out.println("  [ConfigAppManager] Pintado manual aplicado a JToggleButton: " + action.getValue(Action.NAME) + " - Seleccionado: " + isSelected + ", Habilitado: " + button.isEnabled());
        } else {
            System.out.println("  [ConfigAppManager] No se aplicó pintado manual a botón tipo: " + button.getClass().getSimpleName());
        }
    } // --- Fin del método actualizarAspectoBotonToggle ---
    
    
    /**
     * Inyecta la instancia principal de la vista.
     * @param view La instancia de VisorView.
     */
    public void setView(VisorView view) {
        this.view = view;
    } // --- Fin del método setView ---

    /**
     * Inyecta el mapa de acciones de la aplicación.
     * @param actionMap El mapa de acciones (comando -> Action).
     */
    public void setActionMap(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap);
    } // --- Fin del método setActionMap ---
    
	
} // --- FIN de la clase ConfigApplicationManager ---