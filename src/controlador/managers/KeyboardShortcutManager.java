package controlador.managers;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import vista.VisorView;

/**
 * Gestiona la configuración de atajos de teclado de la aplicación.
 * Centraliza los bindings de Swing (InputMap/ActionMap) tanto globales
 * como los específicos de las JList.
 */
public class KeyboardShortcutManager {
    private static final Logger logger = LoggerFactory.getLogger(KeyboardShortcutManager.class);

    // --- Dependencias ---
    private final VisorView view;
    private final Map<String, Action> actionMap;
    private final IListCoordinator listCoordinator;
    private final ComponentRegistry registry;
    private final InfobarStatusManager statusBarManager;

    public KeyboardShortcutManager(VisorView view, Map<String, Action> actionMap,
                                   IListCoordinator listCoordinator, ComponentRegistry registry,
                                   InfobarStatusManager statusBarManager) {
        this.view = view;
        this.actionMap = actionMap;
        this.listCoordinator = listCoordinator;
        this.registry = registry;
        this.statusBarManager = statusBarManager;
    }

    // ==================== ATAJOS GLOBALES (ROOT PANE) ====================

    /**
     * Registra los atajos de teclado globales en el root pane de la ventana.
     * Estos atajos funcionan sin importar qué componente tenga el foco.
     */
    public void configurarAtajosTecladoGlobales() {
        if (view == null || actionMap == null) {
            logger.warn("WARN [configurarAtajosTecladoGlobales]: Vista o ActionMap nulos.");
            return;
        }
        logger.debug("  [KeyboardShortcutManager] Configurando atajos de teclado globales...");

        JRootPane rootPane = view.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap actionMapGlobal = rootPane.getActionMap();

        final int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // --- Atajos Generales ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), AppActionCommands.CMD_ESPECIAL_REFRESCAR);
        actionMapGlobal.put(AppActionCommands.CMD_ESPECIAL_REFRESCAR, actionMap.get(AppActionCommands.CMD_ESPECIAL_REFRESCAR));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA);
        actionMapGlobal.put(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA, actionMap.get(AppActionCommands.CMD_VISTA_PANTALLA_COMPLETA));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, shortcutKeyMask), AppActionCommands.CMD_IMAGEN_LOCALIZAR);
        actionMapGlobal.put(AppActionCommands.CMD_IMAGEN_LOCALIZAR, actionMap.get(AppActionCommands.CMD_IMAGEN_LOCALIZAR));

        // --- Atajos de Proyecto ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask), AppActionCommands.CMD_PROYECTO_GUARDAR);
        actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_GUARDAR, actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKeyMask | KeyEvent.SHIFT_DOWN_MASK), AppActionCommands.CMD_PROYECTO_GUARDAR_COMO);
        actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO, actionMap.get(AppActionCommands.CMD_PROYECTO_GUARDAR_COMO));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKeyMask), AppActionCommands.CMD_PROYECTO_NUEVO);
        actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_NUEVO, actionMap.get(AppActionCommands.CMD_PROYECTO_NUEVO));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKeyMask), AppActionCommands.CMD_PROYECTO_ABRIR);
        actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_ABRIR, actionMap.get(AppActionCommands.CMD_PROYECTO_ABRIR));

        // --- Atajos de Modo (Ctrl+1..7, en el orden de la barra de modos) ---
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, shortcutKeyMask), AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR);
        actionMapGlobal.put(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR, actionMap.get(AppActionCommands.CMD_VISTA_SWITCH_TO_VISUALIZADOR));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, shortcutKeyMask), AppActionCommands.CMD_PROYECTO_GESTIONAR);
        actionMapGlobal.put(AppActionCommands.CMD_PROYECTO_GESTIONAR, actionMap.get(AppActionCommands.CMD_PROYECTO_GESTIONAR));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, shortcutKeyMask), AppActionCommands.CMD_MODO_CLIENTE);
        actionMapGlobal.put(AppActionCommands.CMD_MODO_CLIENTE, actionMap.get(AppActionCommands.CMD_MODO_CLIENTE));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, shortcutKeyMask), AppActionCommands.CMD_MODO_DATOS);
        actionMapGlobal.put(AppActionCommands.CMD_MODO_DATOS, actionMap.get(AppActionCommands.CMD_MODO_DATOS));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_5, shortcutKeyMask), AppActionCommands.CMD_VISTA_CAROUSEL);
        actionMapGlobal.put(AppActionCommands.CMD_VISTA_CAROUSEL, actionMap.get(AppActionCommands.CMD_VISTA_CAROUSEL));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_6, shortcutKeyMask), AppActionCommands.CMD_MODO_RENDER);
        actionMapGlobal.put(AppActionCommands.CMD_MODO_RENDER, actionMap.get(AppActionCommands.CMD_MODO_RENDER));

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_7, shortcutKeyMask), AppActionCommands.CMD_MODO_EDITOR);
        actionMapGlobal.put(AppActionCommands.CMD_MODO_EDITOR, actionMap.get(AppActionCommands.CMD_MODO_EDITOR));

        logger.debug("  -> Atajos de teclado globales configurados.");
    }

    // ==================== ATAJOS EN JLISTS ====================

    /**
     * Configura los bindings de teclado personalizados para las JList.
     * Flechas izquierda/derecha y dígitos 0-9 en la lista de nombres.
     */
    public void interceptarAccionesTecladoListas() {
        if (view == null || listCoordinator == null || registry == null) {
            logger.warn("WARN [interceptarAccionesTecladoListas]: Dependencias nulas.");
            return;
        }
        logger.debug("  -> Configurando bindings de teclado para JLists...");

        // --- Acciones reutilizables ---
        Action selectPreviousAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (listCoordinator != null) listCoordinator.seleccionarAnterior(); }
        };
        Action selectNextAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (listCoordinator != null) listCoordinator.seleccionarSiguiente(); }
        };

        // --- Lista de nombres ---
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            InputMap inputMap = listaNombres.getInputMap(JComponent.WHEN_FOCUSED);
            javax.swing.ActionMap actionMapLocal = listaNombres.getActionMap();

            final String ACT_PREV = "coordSelectPrevious";
            final String ACT_NEXT = "coordSelectNext";

            inputMap.put(KeyStroke.getKeyStroke("LEFT"), ACT_PREV);
            inputMap.put(KeyStroke.getKeyStroke("RIGHT"), ACT_NEXT);

            actionMapLocal.put(ACT_PREV, selectPreviousAction);
            actionMapLocal.put(ACT_NEXT, selectNextAction);

            // Dígitos 0-9 para navegación rápida
            for (int i = 0; i <= 9; i++) {
                final String key = String.valueOf(i);
                inputMap.put(KeyStroke.getKeyStroke(key), "selectNextMatch");
            }
            // La acción selectNextMatch ya está en la JList por defecto

            logger.debug("    -> Bindings de teclado configurados para list.nombresArchivo.");
        }
    }

    // ==================== FOCUS LISTENER MENÚ ====================

    /**
     * Configura el listener de foco del menú bar para navegación con Alt.
     */
    public void configurarFocusListenerMenu() {
        if (view == null) return;
        JMenuBar menuBar = view.getJMenuBar();
        if (menuBar != null) {
            menuBar.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    logger.debug("--- [FocusListener] JMenuBar GANÓ el foco (forzado). ---");
                    if (menuBar.getMenuCount() > 0) menuBar.getMenu(0).setSelected(true);
                    if (statusBarManager != null) statusBarManager.mostrarMensajeTemporal("Navegación por menú activada (pulsa Alt o Esc para salir)", 4000);
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    logger.debug("--- [FocusListener] JMenuBar PERDIÓ el foco. ---");
                    if (menuBar.getMenuCount() > 0) {
                        if (menuBar.getMenu(0).isSelected()) menuBar.getMenu(0).setSelected(false);
                    }
                    if (statusBarManager != null) statusBarManager.limpiarMensaje();
                }
            });
        }
    }
}
