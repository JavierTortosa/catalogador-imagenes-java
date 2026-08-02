package controlador.utils;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import controlador.commands.AppActionCommands;

/**
 * Mapa central de atajos de teclado del editor avanzado.
 * <p>
 * Cada herramienta del editor tiene una letra asignada (estilo Photoshop) que
 * permite activarla sin usar el ratón. La misma fuente se usa para pintar el
 * chivato sobre el icono del botón en la barra de herramientas y para
 * interceptar la tecla desde el {@code CanvasController}.
 */
public final class EditorHotkeys {

    private EditorHotkeys() {
    } // --- Fin del constructor privado EditorHotkeys ---

    /** Asociación commandKey → letra del atajo. */
    private static final Map<String, Character> COMMAND_TO_KEY = buildCommandToKey();

    /** Asociación KeyEvent keyCode → commandKey. */
    private static final Map<Integer, String> KEYCODE_TO_COMMAND = buildKeycodeToKey();


    private static Map<String, Character> buildCommandToKey() {
        Map<String, Character> m = new HashMap<>();
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_EDICION, 'E');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR, 'V');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO, 'M');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA, 'L');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_VARITA, 'W');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR, 'C');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS, 'I');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA, 'G');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO, 'D');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO, 'T');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS, 'U');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_ZOOM, 'Z');
        m.put(AppActionCommands.CMD_ADVANCED_EDITOR_PANTALLA_COMPLETA, 'F');
        return Collections.unmodifiableMap(m);
    } // --- Fin del metodo buildCommandToKey ---


    private static Map<Integer, String> buildKeycodeToKey() {
        Map<Integer, String> m = new HashMap<>();
        for (Map.Entry<String, Character> e : COMMAND_TO_KEY.entrySet()) {
            m.put(KeyEvent.getExtendedKeyCodeForChar(e.getValue()), e.getKey());
        }
        return Collections.unmodifiableMap(m);
    } // --- Fin del metodo buildKeycodeToKey ---


    /**
     * @return la letra del atajo para un commandKey, o null si no tiene
     */
    public static Character hotkeyFor(String commandKey) {
        return commandKey != null ? COMMAND_TO_KEY.get(commandKey) : null;
    } // --- Fin del metodo hotkeyFor ---


    /**
     * Devuelve el commandKey de la herramienta asociada a la tecla pulsada
     * (ignorando mayúsculas), o null si la tecla no es ningún atajo.
     *
     * @param keyCode código de tecla de {@link KeyEvent}
     */
    public static String commandForKeyCode(int keyCode) {
        return KEYCODE_TO_COMMAND.get(keyCode);
    } // --- Fin del metodo commandForKeyCode ---

} // --- Fin de la clase EditorHotkeys ---
