package controlador.actions.editoravanzado;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;

import controlador.commands.AppActionCommands;

public abstract class EditorToolAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static final Map<String, EditorToolAction> cache = new ConcurrentHashMap<>();


    protected EditorToolAction(String commandKey, String iconKey, String tooltip) {
        putValue(Action.ACTION_COMMAND_KEY, commandKey);
        putValue("claveIcono", iconKey);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    } // --- Fin del constructor EditorToolAction ---


    public String getIconKey() {
        return (String) getValue("claveIcono");
    } // --- Fin del metodo getIconKey ---


    @Override
    public void actionPerformed(ActionEvent e) {
        putValue(Action.SELECTED_KEY, Boolean.TRUE);
    } // --- Fin del metodo actionPerformed ---


    public static EditorToolAction createForCommand(String commandKey) {
        if (commandKey == null) return null;
        EditorToolAction cached = cache.get(commandKey);
        if (cached != null) return cached;

        EditorToolAction action = switch (commandKey) {
            case AppActionCommands.CMD_ADVANCED_EDITOR_EDICION -> new EdicionAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_TRANSFORMAR -> new TransformarAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_MARCO -> new SeleccionMarcoAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA -> new SeleccionCapaAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_VARITA -> new VaritaMagicaAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR -> new RecortarAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS -> new CuentagotasAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_BOTE_PINTURA -> new BotePinturaAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_DEGRADADO -> new DegradadoAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_TEXTO -> new TextoAction();
            case AppActionCommands.CMD_ADVANCED_EDITOR_FORMAS -> new FormasAction();
            default -> null;
        };
        if (action != null) {
            cache.put(commandKey, action);
        }
        return action;
    } // --- Fin del metodo createForCommand ---

} // --- Fin de la clase EditorToolAction ---
