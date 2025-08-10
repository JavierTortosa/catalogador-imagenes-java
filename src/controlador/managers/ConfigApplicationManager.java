package controlador.managers;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.DisplayMode;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.VisorView; // Mantener solo si es necesario para el JOptionPane o setAlwaysOnTop
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class ConfigApplicationManager {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    // --- DEPENDENCIAS REFACTORIZADAS ---
    private final VisorModel model;
    private final ConfigurationManager config;
    private final ThemeManager themeManager;
    private final ComponentRegistry registry;
    private Map<String, Action> actionMap;
    private VisorView view;
    private BackgroundControlManager backgroundControlManager;
    private DisplayModeManager displayModeManager;
    
    // --- ESTADO ---
    private final Map<String, String> configAlInicio;
    

    public ConfigApplicationManager(
            VisorModel model, 
            ConfigurationManager config, 
            ThemeManager themeManager,
            ComponentRegistry registry
            ) {
        
        logger.debug("[ConfigApplicationManager] Creando instancia refactorizada...");
        
        this.model = Objects.requireNonNull(model);
        this.config = Objects.requireNonNull(config);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.registry = Objects.requireNonNull(registry);
        
        // 'view' y 'actionMap' serán nulos aquí. Se inyectarán después.
        this.actionMap = new HashMap<>(); // Inicializar a un mapa vacío para evitar NullPointerException

        this.configAlInicio = new HashMap<>(config.getConfigMap());
        logger.debug("[ConfigApplicationManager] Instancia creada.");
    } // --- Fin del Constructor ---
    

    // --- MÉTODOS PÚBLICOS ---
     
    
    public void aplicarConfiguracionGlobalmente() {
    	logger.debug("--- [ConfigApplicationManager] Aplicando configuración globalmente... ---");
        
        // El orden es importante:
        // 1. Aplicamos la config leída del archivo al modelo.
        aplicarConfiguracionAlModelo();
        
        // 2. AHORA sembramos los colores custom si es necesario.
        seedInitialCustomColors(); // <-- AÑADE ESTA LÍNEA AQUÍ
        
        // Ahora, este método solo se encarga de aplicar la configuración,
        // no de la sincronización visual, que ocurrirá más tarde.
        SwingUtilities.invokeLater(this::aplicarConfiguracionAlaVista);
    }// --- Fin del método aplicarConfiguracionGlobalmente ---
    
    
    /**
     * Rota inteligentemente los colores de fondo por defecto en las ranuras
     * personalizables cuando el tema de la aplicación cambia.
     *
     * Si una ranura no ha sido modificada por el usuario (es decir, su color
     * coincide con el de un tema por defecto), su color se actualizará para
     * reflejar el nuevo conjunto de "temas no activos".
     *
     * Si una ranura ha sido personalizada, su color se mantendrá intacto.
     *
     * @param temaAnterior El tema que estaba activo ANTES del cambio.
     * @param temaNuevo    El tema que está activo AHORA.
     */
    public void rotarColoresDeSlotPorCambioDeTema(Tema temaAnterior, Tema temaNuevo) {
        if (temaAnterior == null || temaNuevo == null || temaAnterior.equals(temaNuevo)) {
            return; 
        }
        
        logger.debug("[ConfigApplicationManager] Rotando colores de slot por cambio de tema de '" + temaAnterior.nombreInterno() + "' a '" + temaNuevo.nombreInterno() + "'.");

        // Los dos colores clave en esta operación de "intercambio".
        Color colorTemaViejo = temaAnterior.colorFondoSecundario();
        Color colorTemaNuevo = temaNuevo.colorFondoSecundario();

        // La lista de claves de configuración para las ranuras personalizables.
        List<String> colorKeys = List.of(
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_1,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_2,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_3,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_4
        );

        // Iteramos sobre cada ranura para ver si alguna tiene el color del NUEVO tema.
        for (String key : colorKeys) {
            Color colorActualEnSlot = config.getColor(key, null);

            // Comprobamos si la ranura tiene un color y si ese color es igual al del NUEVO tema.
            if (colorActualEnSlot != null && colorActualEnSlot.equals(colorTemaNuevo)) {
                
                // ¡Bingo! Esta es la ranura que ha quedado "ocupada" por el nuevo tema de reset.
                // La "liberamos" asignándole el color del tema que acabamos de dejar.
                config.setColor(key, colorTemaViejo);
                
                logger.debug("    -> ROTADO: La clave '" + key + "' (que coincidía con el nuevo tema '" + temaNuevo.nombreInterno() + "') " +
                                   "ahora tiene el color del tema anterior '" + temaAnterior.nombreInterno() + "'.");
                
                // Ya hemos hecho el intercambio, no necesitamos seguir buscando.
                return; 
            }
        }

        // Si el bucle termina sin encontrar ninguna coincidencia, significa que todas las
        // ranuras tienen colores personalizados o de otros temas. No hacemos nada.
        logger.debug("    -> No se encontró ninguna ranura con el color del nuevo tema. No se realizó ninguna rotación.");
        
    } // --- FIN del metodo rotarColoresDeSlotPorCambioDeTema ---
    

    public void restaurarConfiguracionPredeterminada() {
    	
        logger.debug("--- [ConfigApplicationManager] Restaurando configuración predeterminada... ---");
        this.config.resetToDefaults();
        aplicarConfiguracionGlobalmente();
        
        JFrame mainFrame = registry.get("frame.main");
        JOptionPane.showMessageDialog(mainFrame, "La configuración ha sido restaurada a los valores de fábrica.", "Configuración Restaurada", JOptionPane.INFORMATION_MESSAGE);
        
    } // --- Fin del método restaurarConfiguracionPredeterminada ---

    public void guardarConfiguracionActual() {
    	
        logger.debug("--- [ConfigApplicationManager] Guardando configuración actual... ---");
        try {
            this.config.guardarConfiguracion(this.config.getConfigMap());
            JFrame mainFrame = registry.get("frame.main");
            JOptionPane.showMessageDialog(mainFrame, "La configuración actual ha sido guardada en config.cfg.", "Configuración Guardada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
        	logger.error("ERROR CRÍTICO [ConfigApplicationManager]: Fallo al guardar la configuración en el archivo.");
            e.printStackTrace();

            // 2. Para el usuario: Muestra un diálogo de error claro y útil.
            JFrame mainFrame = registry.get("frame.main"); // Obtener el frame para centrar el diálogo
            String mensajeUsuario = "No se pudo guardar el archivo de configuración 'config.cfg'.\n\n" +
                                    "Sus cambios actuales solo se mantendrán durante esta sesión.\n" +
                                    "Por favor, verifique que tiene permisos de escritura en la carpeta de la aplicación.\n\n" +
                                    "Detalle del error: " + e.getMessage();
            
            JOptionPane.showMessageDialog(
                mainFrame, 
                mensajeUsuario, 
                "Error al Guardar Configuración", 
                JOptionPane.ERROR_MESSAGE
            );
        }
        
    } // --- Fin del método guardarConfiguracionActual ---


    // --- MÉTODOS PRIVADOS DE AYUDA ---

    
    private void aplicarConfiguracionAlModelo() {
    	
        logger.debug("  [ConfigAppManager] Aplicando configuración al Modelo...");
        if (this.config == null || this.model == null) {
            logger.warn("WARN [ConfigAppManager]: No se puede aplicar config al modelo por dependencias nulas.");
            return;
        }

        // --- Lee todas las configuraciones de comportamiento y estado ---
        boolean mantenerProp = config.getBoolean("interfaz.menu.zoom.mantener_proporciones.seleccionado", true);
        boolean incluirSubcarpetas = config.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
        boolean soloCarpeta = !incluirSubcarpetas;
        boolean navCircular = config.getBoolean(ConfigKeys.COMPORTAMIENTO_NAVEGACION_CIRCULAR, false);
        boolean zoomManualInicial = config.getBoolean(ConfigKeys.COMPORTAMIENTO_ZOOM_MANUAL_INICIAL, true);
        boolean zoomAlCursor = config.getBoolean("comportamiento.zoom.al_cursor.activado", false);
        int saltoBloque = config.getInt(ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10);
        
        // Lógica para obtener el modo de zoom inicial de forma segura
        servicios.zoom.ZoomModeEnum modoZoomInicial;
        String ultimoModoStr = config.getString(ConfigKeys.COMPORTAMIENTO_ZOOM_ULTIMO_MODO, "FIT_TO_SCREEN").toUpperCase();
        try {
            modoZoomInicial = servicios.zoom.ZoomModeEnum.valueOf(ultimoModoStr);
        } catch (IllegalArgumentException e) {
            logger.warn("WARN: Modo de zoom guardado '" + ultimoModoStr + "' no es válido. Usando FIT_TO_SCREEN.");
            modoZoomInicial = servicios.zoom.ZoomModeEnum.FIT_TO_SCREEN;
        }
        
        // --- Aplica las configuraciones leídas al modelo ---
        this.model.initializeContexts(mantenerProp, soloCarpeta, modoZoomInicial, zoomManualInicial, navCircular, zoomAlCursor);
        
        this.model.setMiniaturasAntes(config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
        this.model.setMiniaturasDespues(config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 8));
        this.model.setMiniaturaSelAncho(config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60));
        this.model.setMiniaturaSelAlto(config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60));
        this.model.setMiniaturaNormAncho(config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40));
        this.model.setMiniaturaNormAlto(config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40));
        this.model.setSaltoDeBloque(saltoBloque);
        
        boolean pantallaCompleta = config.getBoolean(ConfigKeys.COMPORTAMIENTO_PANTALLA_COMPLETA, false);
        this.model.setModoPantallaCompletaActivado(pantallaCompleta);
        
        // Cargar el último DisplayMode utilizado
        DisplayMode ultimoDisplayMode;
        String ultimoDisplayModeStr = config.getString(ConfigKeys.COMPORTAMIENTO_DISPLAY_MODE_ULTIMO_USADO, "SINGLE_IMAGE").toUpperCase();
        try {
            // Intenta convertir el String del fichero a un valor del enum
            ultimoDisplayMode = VisorModel.DisplayMode.valueOf(ultimoDisplayModeStr);
        } catch (IllegalArgumentException e) {
            // Si el valor en el fichero es inválido o no existe, usa un valor seguro por defecto
            logger.warn("WARN: DisplayMode guardado '" + ultimoDisplayModeStr + "' no es válido. Usando SINGLE_IMAGE por defecto.");
            ultimoDisplayMode = VisorModel.DisplayMode.SINGLE_IMAGE;
        }
        // Llama al nuevo método del modelo para establecer el estado inicial
        model.setInitialDisplayMode(ultimoDisplayMode);
        
        logger.debug("  -> Configuración del Modelo aplicada.");
    
    } // --- Fin del método aplicarConfiguracionAlModelo ---

    
    private void aplicarConfiguracionAlaVista() {
        logger.debug("  [ConfigAppManager] Aplicando configuración a la Vista (usando Registry)...");
        
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
        
        // Aplicar el estado inicial del fondo a cuadros, que es una configuración de la vista.
        
        // 1. Definimos la clave de configuración que controla el estado persistente.
        String configKeyFondo = "interfaz.menu.vista.fondo_a_cuadros.seleccionado";
        
        // 2. Leemos el valor de la configuración. Si no existe, por defecto es 'false'.
        boolean fondoACuadrosInicial = this.config.getBoolean(configKeyFondo, false);
        
        // 3. Obtenemos el panel de visualización desde el registro.
        vista.panels.ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");

        if (displayPanel != null) {
            logger.debug("    -> Aplicando estado inicial de fondo a cuadros desde config: " + fondoACuadrosInicial);
            // 4. Le decimos al panel que establezca su estado inicial.
            //    Si 'fondoACuadrosInicial' es true, se pondrá a cuadros.
            //    Si es false, se pondrá de un color sólido (el del tema actual),
            //    porque la lógica interna del panel se encargará de eso.
            displayPanel.setCheckeredBackground(fondoACuadrosInicial);
        } else {
            logger.warn("WARN [ConfigAppManager]: No se encontró 'panel.display.imagen' para aplicar el fondo inicial.");
        }
        
        if (this.backgroundControlManager != null) {
            this.backgroundControlManager.initializeAndLinkControls();
            this.backgroundControlManager.sincronizarSeleccionConEstadoActual(); 
        }
        
        logger.debug("    -> Configuración básica de Vista aplicada.");
        
    } // --- Fin del método aplicarConfiguracionAlaVista ---

    
    private void sincronizarUIFinal() {
        logger.debug("  [ConfigAppManager] Sincronizando UI final...");
        
        if (actionMap == null || actionMap.isEmpty()) {
            logger.warn("WARN [ConfigAppManager]: ActionMap vacío, no se puede sincronizar la UI final.");
            return;
        }

        // --- 1. Sincronizar el estado lógico de TODOS los botones toggle ---
        //    Esto asegura que los botones reflejen el estado cargado desde la config.
        logger.debug("    -> Sincronizando estado lógico de todos los botones toggle...");
        for (Action action : actionMap.values()) {
            Object selectedValue = action.getValue(Action.SELECTED_KEY);
            // Comprobamos si la Action tiene un estado de selección booleano.
            if (selectedValue instanceof Boolean) {
                // Llamamos al método helper para que fuerce la sincronización visual del botón.
                actualizarAspectoBotonToggle(action, (Boolean) selectedValue);
            }
        }
        logger.debug("    -> Sincronización de estado lógico de toggles completada.");

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
        
        logger.debug("  [ConfigAppManager] Sincronización de UI final completada.");
    } // --- Fin del método sincronizarUIFinal ---
    
    
    /**
     * Revisa si los colores personalizados de fondo existen en la configuración.
     * Si no existen (porque es el primer arranque o se borró el config), 
     * los "siembra" en el mapa de configuración EN MEMORIA con los colores 
     * por defecto de los otros temas disponibles.
     */
    private void seedInitialCustomColors() {
        logger.debug("  [ConfigAppManager] Verificando y sembrando colores de fondo por defecto...");

        // Lista de las claves que vamos a revisar.
        List<String> colorKeys = List.of(
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_1,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_2,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_3,
            ConfigKeys.BACKGROUND_CUSTOM_COLOR_4
        );

        // Obtenemos la lista ordenada de temas y quitamos el actual para tener los "otros".
        List<Tema> otrosTemas = new java.util.ArrayList<>(themeManager.getTemasOrdenados());
        otrosTemas.remove(themeManager.getTemaActual());

        for (int i = 0; i < colorKeys.size(); i++) {
            String key = colorKeys.get(i);
            
            // Usamos containsKey() para una comprobación de solo lectura.
            if (!config.getConfigMap().containsKey(key)) {
                
                if (i < otrosTemas.size()) {
                    Color colorPorDefecto = otrosTemas.get(i).colorFondoSecundario();
                    
                    // "Sembramos" el valor en el mapa de configuración en memoria.
                    // Este es el ÚNICO lugar (fuera de la acción del usuario) donde se escribe en config.
                    config.setColor(key, colorPorDefecto);
                    
                    logger.debug("    -> SEMBRADO: La clave '" + key + "' se ha inicializado en memoria con el color del tema '" + otrosTemas.get(i).nombreInterno() + "'.");
                }
            }
        }

    } // --- FIN DEL Metodo seedInitialCustomColors ---
    

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
     * Sincroniza el estado de selección LÓGICO de un botón toggle.
     * NO aplica colores manualmente, confía en que el Look & Feel (FlatLaf)
     * lo hará basándose en el estado 'selected' del botón y las propiedades
     * del UIManager.
     *
     * @param action La Action cuyo botón asociado se va a actualizar.
     * @param isSelected El estado de selección (true si está activado, false si no).
     */
    public void actualizarAspectoBotonToggle(Action action, boolean isSelected) {
        if (action == null) {
            return;
        }

        // 1. Encontrar el botón asociado a la acción.
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
            return; // No se encontró botón para esta acción.
        }

        // --- Lógica de Depuración ---
        String actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
        logger.info("--- [ConfigAppManager] Sincronizando estado lógico para Action: " + actionCommand + " ---");
        
        // 2. Sincronizar el estado de selección lógico del botón.
        //    Este es el único paso realmente necesario. Al cambiar el estado de
        //    selección, se dispara un repintado y FlatLaf aplicará los colores correctos.
        if (button.isSelected() != isSelected) {
            button.setSelected(isSelected);
            logger.debug("    -> Estado .isSelected() del botón cambiado a " + isSelected);
        }
        
        // Ya no es necesario llamar a button.repaint() aquí, porque setSelected()
        // ya notifica al sistema de repintado.
        
        logger.debug("--- Fin sincronización lógica para " + actionCommand + " ---");

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
    
    
    public void setBackgroundControlManager(BackgroundControlManager backgroundControlManager) {
        this.backgroundControlManager = Objects.requireNonNull(backgroundControlManager);
    }
    
    
    public void setDisplayModeManager(DisplayModeManager dmm) { this.displayModeManager = dmm; }
    
} // --- FIN de la clase ConfigApplicationManager ---