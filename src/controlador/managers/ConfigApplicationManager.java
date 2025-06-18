package controlador.managers;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
    private final Map<String, Action> actionMap;
    private final ThemeManager themeManager;
    private final ComponentRegistry registry;

    // --- ESTADO ---
    private final Map<String, String> configAlInicio;
    

    // --- CONSTRUCTOR REFACTORIZADO ---
    public ConfigApplicationManager(
            VisorModel model, 
            VisorView view, // Se mantiene temporalmente para el padre del JOptionPane
            ConfigurationManager config, 
            Map<String, Action> actionMap, 
            ThemeManager themeManager,
            ComponentRegistry registry
            ) {
        
        System.out.println("[ConfigApplicationManager] Creando instancia refactorizada...");
        
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
        this.actionMap = Objects.requireNonNull(actionMap);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.registry = Objects.requireNonNull(registry);
        // this.view se mantiene temporalmente para los diálogos.

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
                JButton button = registry.get(key);
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
        
        sincronizarEstadoVisualBotonesToggle();
        
        // Aplicar el estado inicial del fondo a cuadros, que es una configuración de la vista.
        String configKeyFondo = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        boolean fondoACuadrosInicial = this.config.getBoolean(configKeyFondo, false);
        
        vista.panels.ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            System.out.println("    -> Aplicando estado inicial de fondo a cuadros desde config: " + fondoACuadrosInicial);
            // La lógica para decidir si se pone a cuadros o sólido la tiene el propio panel.
            // Aquí solo le damos la orden inicial.
            displayPanel.setCheckeredBackground(fondoACuadrosInicial);
        } else {
            System.err.println("WARN [ConfigAppManager]: No se encontró 'panel.display.imagen' para aplicar el fondo inicial.");
        }
        
        System.out.println("    -> Configuración básica de Vista aplicada.");
    } // --- Fin del método aplicarConfiguracionAlaVista ---

    /**
     * Sincroniza el estado visual (color de fondo) de todos los botones de la UI
     * que están asociados a una Action de tipo "toggle" (que tiene un estado ON/OFF).
     * Itera sobre todas las acciones conocidas y actualiza el botón correspondiente.
     */
    public void sincronizarEstadoVisualBotonesToggle() {
        System.out.println("    -> Sincronizando estado visual de botones toggle...");

        // Llamamos a un método helper para cada botón que queramos sincronizar.
        // Esto mantiene el código limpio y fácil de leer.
        sincronizarUnBotonToggle(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        sincronizarUnBotonToggle(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        sincronizarUnBotonToggle(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        sincronizarUnBotonToggle(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        
        // ... Añade aquí cualquier otra Action de tipo toggle que tengas ...

        System.out.println("    -> Sincronización de botones toggle completada.");
    } // --- Fin del método sincronizarEstadoVisualBotonesToggle ---
    
    /**
     * Método helper privado para actualizar el aspecto de UN botón toggle específico.
     * @param actionCommand La clave de la Action en el actionMap.
     */
    private void sincronizarUnBotonToggle(String actionCommand) {
        Action action = actionMap.get(actionCommand);
        if (action == null) {
            // System.err.println("WARN [ConfigAppManager]: No se encontró la acción para el comando: " + actionCommand);
            return;
        }

        boolean isSelected = Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
        Tema temaActual = themeManager.getTemaActual();

        // Buscar el botón asociado a esta Action en el registro de componentes
        // Este enfoque es desacoplado pero puede ser lento si hay miles de componentes.
        // Para una UI normal, es perfectamente aceptable.
        registry.getAllJComponents().stream()
            .filter(c -> c instanceof JButton && action.equals(((JButton) c).getAction()))
            .findFirst()
            .ifPresent(componente -> {
                JButton button = (JButton) componente;
                Color colorFondo = isSelected ? temaActual.colorBotonFondoActivado() : temaActual.colorBotonFondo();
                button.setBackground(colorFondo);
                // Asegurarse de que el botón es opaco para que el color se vea
                if (!button.isOpaque()) {
                    button.setOpaque(true);
                }
            });
    } // --- Fin del método sincronizarUnBotonToggle ---

    private void sincronizarBotonToggle(String actionCommand) {
        Action action = actionMap.get(actionCommand);
        if (action != null) {
            boolean isSelected = Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
            
            // Buscar el botón asociado a esta acción en el registro
            // Esto es más lento que el mapa de la vista, pero más desacoplado.
            // Se puede optimizar si es necesario.
            registry.getAllJComponents().stream()
                .filter(c -> c instanceof JButton && action.equals(((JButton) c).getAction()))
                .findFirst()
                .ifPresent(c -> {
                    JButton button = (JButton) c;
                    button.setBackground(isSelected ? themeManager.getTemaActual().colorBotonFondoActivado() : themeManager.getTemaActual().colorBotonFondo());
                });
        }
    } // --- Fin del método sincronizarBotonToggle ---

    private void sincronizarUIFinal() {
        System.out.println("  [ConfigAppManager] Sincronizando UI final...");
        
        JFrame mainFrame = registry.get("frame.main");
        if(mainFrame == null) return;

        // Sincronizar Siempre Encima
        Action alwaysOnTopAction = actionMap.get(AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
        if (alwaysOnTopAction != null) {
            boolean estadoTop = Boolean.TRUE.equals(alwaysOnTopAction.getValue(Action.SELECTED_KEY));
            if (mainFrame.isAlwaysOnTop() != estadoTop) {
                mainFrame.setAlwaysOnTop(estadoTop);
            }
        }

        // Sincronizar Fondo a Cuadros
        // Esto ahora lo debería hacer directamente la Action sobre la vista o el modelo,
        // pero si lo mantenemos aquí, lo haríamos a través del registry.
        JLabel etiquetaImagen = registry.get("label.imagenPrincipal");
        // ... lógica si fuera necesario.

    } // --- Fin del método sincronizarUIFinal ---
    
    
    public void actualizarEstadoControlesZoom(boolean zoomManualActivado, boolean resetHabilitado) {
        // Obtenemos los componentes desde el registro
        JButton zoomButton = registry.get("interfaz.boton.zoom.Zoom_48x48");
        JButton resetButton = registry.get("interfaz.boton.zoom.Reset_48x48");
        JMenuItem resetMenuItem = registry.get("interfaz.menu.zoom.resetear_zoom");
        
        Tema temaActual = themeManager.getTemaActual();
        
        if (zoomButton != null) {
            zoomButton.setBackground(zoomManualActivado ? temaActual.colorBotonFondoActivado() : temaActual.colorBotonFondo());
            zoomButton.setOpaque(true);
        }
        
        if (resetButton != null) {
            resetButton.setEnabled(resetHabilitado);
        }
        
        if (resetMenuItem != null) {
            resetMenuItem.setEnabled(resetHabilitado);
        }
    }// --- FIN del metodo actualizarEstadoControlesZoom ---
    
    
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
     * Actualiza la apariencia visual de un botón toggle (como un JToggleButton)
     * para reflejar su estado de selección (activado/desactivado).
     * Busca el botón asociado a la Action proporcionada y le aplica los colores del tema.
     * 
     * @param action La Action cuyo botón asociado se va a actualizar.
     * @param isSelected El estado de selección (true si está activado, false si no).
     */
    public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        if (action == null) return;

        JButton botonAsociado = null;
        // La búsqueda del botón se mantiene igual
        for (JButton boton : registry.getAllComponentsOfType(JButton.class)) {
            if (action.equals(boton.getAction())) {
                botonAsociado = boton;
                break;
            }
        }
        
        if (botonAsociado == null) {
            return;
        }

        Tema temaActual = this.themeManager.getTemaActual();
        if (temaActual == null) return;
        
        // <<< LA LÓGICA DE ACTUALIZACIÓN CORRECTA >>>
        
        // 1. Establecer el color de fondo
        Color colorFondoDestino = isSelected 
            ? temaActual.colorBotonFondoActivado() 
            : temaActual.colorBotonFondo();
        botonAsociado.setBackground(colorFondoDestino);
        botonAsociado.setOpaque(true);
        
        // 2. Establecer el estado de selección.
        // Esto funcionará tanto para JButton (donde no tiene efecto visual)
        // como para JToggleButton (donde sí lo tiene).
        botonAsociado.setSelected(isSelected);
        
    } // --- FIN del metodo actualizarAspectoBotonToggle ---
    

    
	
} // --- FIN de la clase ConfigApplicationManager ---