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
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

    
    public void refrescarTodaLaUIConTemaActual() {
        System.out.println("--- [ConfigApplicationManager] INICIANDO REFRESCO DE COLORES POR TEMA ---");
        if (themeManager == null || registry == null) {
            System.err.println("  ERROR: ThemeManager o Registry nulos.");
            return;
        }

        Tema tema = themeManager.getTemaActual();
        JFrame mainFrame = registry.get("frame.main");

        // --- 1. Contenedores Principales ---
        if (mainFrame != null) {
            mainFrame.getContentPane().setBackground(tema.colorFondoPrincipal());
        }
        
        JPanel toolbarContainer = registry.get("container.toolbars");
        if (toolbarContainer != null) {
            toolbarContainer.setBackground(tema.colorFondoPrincipal());
        }

        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");
        if (scrollMiniaturas != null) {
            scrollMiniaturas.setBackground(tema.colorFondoPrincipal());
            scrollMiniaturas.getViewport().setBackground(tema.colorFondoPrincipal());
            if(scrollMiniaturas.getBorder() instanceof javax.swing.border.TitledBorder) {
                ((javax.swing.border.TitledBorder)scrollMiniaturas.getBorder()).setTitleColor(tema.colorBordeTitulo());
            }
        }

        // --- 2. JMenuBar y sus ítems ---
        if (mainFrame != null && mainFrame.getJMenuBar() != null) {
        	JMenuBar menuBar = registry.get("menubar.main");
//            JMenuBar menuBar = mainFrame.getJMenuBar();
        	if (menuBar != null) {
        		menuBar.setBackground(tema.colorFondoPrincipal());
        		menuBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, tema.colorBorde()));
        	}
        	
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                menu.setForeground(tema.colorTextoPrimario());
                menu.setOpaque(true); // Necesario para que el fondo del popup se vea
                
                for (java.awt.Component comp : menu.getMenuComponents()) {
                    if (comp instanceof JMenuItem) {
                        JMenuItem menuItem = (JMenuItem) comp;
                        menuItem.setForeground(tema.colorTextoPrimario());
                        menuItem.setBackground(tema.colorFondoPrincipal());
                    }
                }
            }
        }
        
        // --- 3. Panel Izquierdo (Lista de Archivos) ---
        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");
        if (panelIzquierdo != null) {
            panelIzquierdo.setBackground(tema.colorFondoPrincipal());
            if(panelIzquierdo.getBorder() instanceof javax.swing.border.TitledBorder) {
                ((javax.swing.border.TitledBorder)panelIzquierdo.getBorder()).setTitleColor(tema.colorBordeTitulo());
            }
            
            JList<String> listaNombres = registry.get("list.nombresArchivo");
            if(listaNombres != null) {
                listaNombres.setForeground(tema.colorTextoPrimario());
                listaNombres.setBackground(tema.colorFondoSecundario());
                listaNombres.setSelectionForeground(tema.colorSeleccionTexto());
                listaNombres.setSelectionBackground(tema.colorSeleccionFondo());
            }
        }

        // --- 4. Panel Derecho (Visor) ---
        JPanel panelDerecho = registry.get("panel.derecho.visor");
        if (panelDerecho != null) {
            panelDerecho.setBackground(tema.colorFondoSecundario());
        }
        
        // --- 5. Barras de Información (Superior e Inferior) ---
        JPanel topInfoPanel = registry.get("panel.info.superior");
        if(topInfoPanel != null) {
            topInfoPanel.setBackground(tema.colorFondoSecundario());
            topInfoPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, tema.colorBorde()));
            for(JLabel label : registry.getAllComponentsOfType(JLabel.class)) {
                if(SwingUtilities.isDescendingFrom(label, topInfoPanel)) {
                    label.setForeground(tema.colorTextoSecundario());
                }
            }
        }
        
        JPanel bottomStatusBar = registry.get("panel.estado.inferior");
        if(bottomStatusBar != null) {
            bottomStatusBar.setBackground(tema.colorFondoPrincipal());
            bottomStatusBar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, tema.colorBorde()),
                javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            
            JLabel rutaLabel = registry.get("label.estado.ruta");
            if(rutaLabel != null) rutaLabel.setForeground(tema.colorTextoPrimario());
            
            JLabel mensajesLabel = registry.get("label.estado.mensajes");
            if(mensajesLabel != null) mensajesLabel.setForeground(tema.colorTextoSecundario());
            
            JLabel zoomPctLabel = registry.get("label.control.zoomPorcentaje");
            if(zoomPctLabel != null) zoomPctLabel.setForeground(tema.colorTextoPrimario());
        }
        
        // --- 6. Forzar repintado ---
        if (mainFrame != null) {
            mainFrame.revalidate();
            mainFrame.repaint();
        }
        System.out.println("--- [ConfigApplicationManager] REFRESCO DE COLORES FINALIZADO ---");
        
    }// --- FIN del metodo refrescarTodaLaUIConTemaActual --- 
    
    
    /**
     * Sincroniza el estado visual (color de fondo) de todos los botones de la UI
     * que están asociados a una Action de tipo "toggle" (que tiene un estado ON/OFF).
     * Itera sobre todas las acciones conocidas y actualiza el botón correspondiente.
     */
    public void sincronizarEstadoVisualBotonesToggle() {
        System.out.println("    -> Sincronizando estado visual de botones toggle...");

        
        //FIXME HACER QUE ESTE METODO NO NECESITE UNA INCORPORACION MANUAL DE BOTONES ON OFF
        
        // Llamamos a un método helper para cada botón que queramos sincronizar.
        // Esto mantiene el código limpio y fácil de leer.
        sincronizarUnBotonToggle(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        sincronizarUnBotonToggle(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        sincronizarUnBotonToggle(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        sincronizarUnBotonToggle(AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        sincronizarUnBotonToggle(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR);
        
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
     * Busca el botón asociado a la Action proporcionada y le aplica los estilos del tema.
     * 
     * @param action La Action cuyo botón asociado se va a actualizar.
     * @param isSelected El estado de selección (true si está activado, false si no).
     */
    public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        if (action == null) return;

        JButton botonAsociado = null;
        // La búsqueda del botón se mantiene igual.
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
        
        // --- LÓGICA DE ACTUALIZACIÓN ---
        
        if (isSelected) {
            // ESTADO "ON": Se pinta el fondo verde de "activado".
            botonAsociado.setBackground(temaActual.colorBotonFondoActivado());
            botonAsociado.setOpaque(true);
            botonAsociado.setContentAreaFilled(true);
            botonAsociado.setBorderPainted(true); // Opcional: mostrar un borde para destacar más.
        } else {
            // ESTADO "OFF": Se hace el botón completamente transparente.
            botonAsociado.setOpaque(false);
            botonAsociado.setContentAreaFilled(false);
            botonAsociado.setBorderPainted(false);
        }
        
        botonAsociado.setSelected(isSelected);
        
    } // --- FIN del metodo actualizarAspectoBotonToggle ---
    
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