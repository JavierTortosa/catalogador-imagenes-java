package controlador.managers;

import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.config.ViewUIConfig; // Puede ser necesaria para los colores, etc.

/**
* Gestiona la aplicación de la configuración a los diferentes componentes de la UI.
* Centraliza la lógica de refresco y restauración del estado visual y del modelo
* basándose en la configuración en memoria.
*/
public class ConfigApplicationManager {

	// --- DEPENDENCIAS ---
	private final VisorModel model;
	private final VisorView view;
	private final ConfigurationManager config;
	private final Map<String, Action> actionMap;
	private final ViewUIConfig uiConfig; // Para acceder a colores, etc.
	
	/**
	 * Constructor del gestor de aplicación de configuración.
	 *
	 * @param model      El modelo de datos principal.
	 * @param view       La vista principal (JFrame).
	 * @param config     El gestor de configuración.
	 * @param actionMap  El mapa de todas las Actions de la aplicación.
	 * @param uiConfig   La configuración de la UI con colores, etc.
	 */
	public ConfigApplicationManager(
	        VisorModel model, 
	        VisorView view, 
	        ConfigurationManager config, 
	        Map<String, Action> actionMap, 
	        ViewUIConfig uiConfig) {
	     
	    System.out.println("[ConfigApplicationManager] Creando instancia...");
	     
	    this.model = Objects.requireNonNull(model, "Model no puede ser null");
	    this.view = Objects.requireNonNull(view, "View no puede ser null");
	    this.config = Objects.requireNonNull(config, "ConfigurationManager no puede ser null");
	    this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null");
	    this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser null");
	
	    System.out.println("[ConfigApplicationManager] Instancia creada con todas las dependencias.");
	
	} // --- FIN del Constructor ---
	
	// --- MÉTODOS PÚBLICOS (API del Manager) ---
	 
	/**
     * Punto de entrada principal para aplicar toda la configuración actual en memoria
     * a los componentes de la aplicación (Modelo y Vista).
     * Orquesta la llamada a métodos privados más específicos.
     */
    public void aplicarConfiguracionGlobalmente() {
        System.out.println("--- [ConfigApplicationManager] Aplicando configuración globalmente... ---");

        // 1. Aplicar configuración al Modelo de datos
        // (Esta lógica estaba antes en aplicarConfiguracionInicial de VisorController)
        aplicarConfiguracionAlModelo();

        // 2. Aplicar configuración a los componentes de la Vista (botones, menús)
        //    y sincronizar estados visuales específicos.
        //    (Esta lógica viene de aplicarConfigAlaVistaInternal de VisorController)
        SwingUtilities.invokeLater(() -> {
            aplicarConfiguracionAlaVista();
            sincronizarUIFinal();
            // ¡Ojo! Necesitamos una referencia al controller para este método.
            // Por ahora, asumimos que se puede pasar o que la lógica se mueve aquí.
            // controller.sincronizarEstadoVisualCheckboxesDeBotones(); 
        });

        System.out.println("--- [ConfigApplicationManager] Aplicación de configuración global completada. ---");
    } // --- FIN del método aplicarConfiguracionGlobalmente ---


    // --- MÉTODOS PRIVADOS DE AYUDA ---

    /**
     * Aplica la configuración en memoria a las propiedades del VisorModel.
     */
    private void aplicarConfiguracionAlModelo() {
        System.out.println("  [ConfigAppManager] Aplicando configuración al Modelo...");
        try {
            this.model.setMiniaturasAntes(config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_ANTES, 8));
            this.model.setMiniaturasDespues(config.getInt(ConfigKeys.MINIATURAS_CANTIDAD_DESPUES, 8));
            this.model.setMiniaturaSelAncho(config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ANCHO, 60));
            this.model.setMiniaturaSelAlto(config.getInt(ConfigKeys.MINIATURAS_TAMANO_SEL_ALTO, 60));
            this.model.setMiniaturaNormAncho(config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ANCHO, 40));
            this.model.setMiniaturaNormAlto(config.getInt(ConfigKeys.MINIATURAS_TAMANO_NORM_ALTO, 40));

            boolean cargarSubcarpetas = config.getBoolean(ConfigKeys.COMPORTAMIENTO_CARGAR_SUBCARPETAS, true);
            this.model.setMostrarSoloCarpetaActual(!cargarSubcarpetas);

            // Añadir aquí cualquier otra configuración que deba ir al modelo.

            System.out.println("    -> Configuración del Modelo aplicada.");
        } catch (Exception e) {
            System.err.println("  ERROR aplicando configuración al Modelo: " + e.getMessage());
        }
    } // --- FIN del método aplicarConfiguracionAlModelo ---

    /**
     * Aplica la configuración a los componentes de la Vista (botones, menús)
     * y sincroniza estados visuales específicos.
     * Debe ser llamado desde el EDT.
     */
    private void aplicarConfiguracionAlaVista() {
        System.out.println("  [ConfigAppManager] Aplicando configuración a la Vista (en EDT)...");
        
        // --- Aplicar a Botones ---
        Map<String, javax.swing.JButton> botones = view.getBotonesPorNombre();
        if (botones != null) {
            botones.forEach((claveCompleta, button) -> {
                try {
                    button.setEnabled(config.getBoolean(claveCompleta + ".activado", true));
                    button.setVisible(config.getBoolean(claveCompleta + ".visible", true));
                } catch (Exception e) {
                    System.err.println("    ERROR aplicando config al botón '" + claveCompleta + "': " + e.getMessage());
                }
            });
        }

        // --- Aplicar a Menús ---
        Map<String, javax.swing.JMenuItem> menuItems = view.getMenuItemsPorNombre();
        if (menuItems != null) {
            menuItems.forEach((claveCompleta, menuItem) -> {
                // Aquí va la lógica que tenías para aplicar el estado a los menús,
                // especialmente la parte que maneja los checkboxes SIN action.
                // Esta lógica puede ser compleja y la tenías bien en VisorController.
            });
        }
        
     // --- INICIO DEL CAMBIO: Sincronización visual de botones Toggle ---
        System.out.println("    -> Sincronizando estado visual inicial de botones toggle...");

        // 1. Sincronizar botón de Zoom Manual
        Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (zoomManualAction != null) {
            boolean isZoomManualActivo = Boolean.TRUE.equals(zoomManualAction.getValue(Action.SELECTED_KEY));
            view.actualizarAspectoBotonToggle(zoomManualAction, isZoomManualActivo);
        }

        // 2. Sincronizar botón de Mantener Proporciones
        Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        if (proporcionesAction != null) {
            boolean mantenerProporcionesActivo = Boolean.TRUE.equals(proporcionesAction.getValue(Action.SELECTED_KEY));
            view.actualizarAspectoBotonToggle(proporcionesAction, mantenerProporcionesActivo);
        }

        // 3. Sincronizar botón de Incluir Subcarpetas
        Action subfoldersAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
        if (subfoldersAction != null) {
            boolean incluirSubcarpetasActivo = Boolean.TRUE.equals(subfoldersAction.getValue(Action.SELECTED_KEY));
            view.actualizarAspectoBotonToggle(subfoldersAction, incluirSubcarpetasActivo);
        }
        
        // Añade aquí cualquier otro botón toggle que necesite sincronización inicial
        
        System.out.println("    -> Sincronización de botones toggle completada.");
        // --- FIN DEL CAMBIO ---
        
        System.out.println("    -> Configuración básica de Vista (botones/menús) aplicada.");
    } // --- FIN del método aplicarConfiguracionAlaVista ---

    /**
     * Realiza sincronizaciones visuales finales que dependen del estado de las Actions.
     * Debe ser llamado desde el EDT.
     */
    private void sincronizarUIFinal() {
        System.out.println("  [ConfigAppManager] Sincronizando UI final (en EDT)...");
        try {
            // Sincronizar Fondo a Cuadros
            Action checkeredBgAction = actionMap.get(controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
            if (checkeredBgAction != null) {
                boolean estadoFondo = Boolean.TRUE.equals(checkeredBgAction.getValue(Action.SELECTED_KEY));
                view.setCheckeredBackgroundEnabled(estadoFondo);
            }

            // Sincronizar Siempre Encima
            Action alwaysOnTopAction = actionMap.get(controlador.commands.AppActionCommands.CMD_VISTA_TOGGLE_ALWAYS_ON_TOP);
            if (alwaysOnTopAction != null) {
                boolean estadoTop = Boolean.TRUE.equals(alwaysOnTopAction.getValue(Action.SELECTED_KEY));
                if (view.isAlwaysOnTop() != estadoTop) {
                    view.setAlwaysOnTop(estadoTop);
                }
            }
            // Añadir aquí cualquier otra sincronización final necesaria.

            System.out.println("    -> Sincronización de UI final completada.");
        } catch (Exception e) {
            System.err.println("  ERROR durante la sincronización final de la UI: " + e.getMessage());
        }
    } // --- FIN del método sincronizarUIFinal ---
	
	/**
	 * Restaura la configuración en memoria a los valores por defecto de fábrica
	 * y luego refresca toda la interfaz de usuario para reflejar estos cambios.
	 */
	public void restaurarConfiguracionPredeterminada() {
	    System.out.println("--- [ConfigApplicationManager] Solicitud para restaurar configuración predeterminada... ---");
	    
	    // 1. Resetear el mapa de configuración en memoria a los defaults.
	    this.config.resetToDefaults();
	     
	    // 2. Aplicar la nueva configuración (ahora los defaults) a toda la UI.
	    aplicarConfiguracionGlobalmente();
	
	    // 3. Notificar al usuario (opcional, pero recomendado).
	    // JOptionPane.showMessageDialog(...); // Esto podría hacerse en la Action o aquí.
	     
	} // --- FIN del método restaurarConfiguracionPredeterminada ---
	
	// Aquí irían los otros métodos públicos como guardarYRefrescar, etc.
	
}// --- FIN de la clase ConfigApplicationManager ---
	