package controlador.actions.especiales;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import servicios.ConfigurationManager;
import vista.builders.PopupMenuBuilder;
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.theme.ThemeManager;

/**
 * Acción para mostrar un menú emergente con las acciones de los botones que no caben en la barra de herramientas.
 */
public class HiddenButtonsAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private Map<String, Action> actionMapRef;
    private ThemeManager themeManager;
    private ConfigurationManager configManagerRef;
    private ToolbarManager toolbarManager;


    /**
     * Constructor para HiddenButtonsAction.
     *
     * @param name           El nombre de la acción.
     * @param icon           El icono de la acción.
     * @param actionMap      El mapa de acciones de la aplicación.
     * @param themeManager   El gestor de temas.
     * @param configManager  El gestor de configuración.
     * @param toolbarManager El gestor de toolbars.
     */
    public HiddenButtonsAction(
            String name,
            ImageIcon icon,
            Map<String, Action> actionMap,
            ThemeManager themeManager,
            ConfigurationManager configManager,
            ToolbarManager toolbarManager) {
        super(name, icon);
        this.actionMapRef = Objects.requireNonNull(actionMap, "ActionMap no puede ser nulo");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser nulo");
        this.configManagerRef = Objects.requireNonNull(configManager);
        this.toolbarManager = toolbarManager;

        putValue(Action.SHORT_DESCRIPTION, "Mostrar acciones adicionales o botones que no caben");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ESPECIAL_BOTONES_OCULTOS);
    } // --- Fin del método HiddenButtonsAction ---


    /**
     * Ejecuta la acción de mostrar el menú emergente con los botones ocultos.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (actionMapRef == null || themeManager == null) {
            System.err.println("ERROR CRÍTICO [HiddenButtonsAction]: Dependencias nulas.");
            return;
        }

        Object source = e.getSource();
        if (!(source instanceof Component)) {
            System.err.println("[HiddenButtonsAction] La fuente del evento no es un Component.");
            return;
        }
        Component invokerComponent = (Component) source;

        List<MenuItemDefinition> itemsParaPopup = new ArrayList<>();
        Map<String, Action> popupActionMap = new HashMap<>(this.actionMapRef);

        // Obtener comandos de botones ocultos dinámicamente desde ToolbarManager
        List<String> hiddenCommands = (toolbarManager != null)
                ? toolbarManager.getHiddenButtonCommands()
                : new ArrayList<>();

        for (String comando : hiddenCommands) {
            Action action = actionMapRef.get(comando);
            if (action != null) {
                Icon hiddenIcon = toolbarManager != null ? toolbarManager.getHiddenButtonIcon(comando) : null;
                if (hiddenIcon != null) {
                    popupActionMap.put(comando, createPopupAction(action, comando, hiddenIcon));
                }

                String actionName = (String) action.getValue(Action.NAME);
                itemsParaPopup.add(new MenuItemDefinition(comando, MenuItemType.ITEM,
                        actionName != null ? actionName : comando, null));
            }
        }

        if (itemsParaPopup.isEmpty()) {
            itemsParaPopup.add(new MenuItemDefinition(null, MenuItemType.ITEM, "(No hay acciones adicionales)", null));
        }

        PopupMenuBuilder popupBuilder = new PopupMenuBuilder(
                this.themeManager,
                this.configManagerRef);

        JPopupMenu popupMenu = popupBuilder.buildPopupMenuWithNestedMenus(itemsParaPopup, popupActionMap);

        popupMenu.show(invokerComponent, 0, invokerComponent.getHeight());
    } // --- Fin del método actionPerformed ---


    /**
     * Crea una nueva acción para el menú emergente que delega su ejecución a la acción original.
     *
     * @param delegate La acción original.
     * @param command  El comando de la acción.
     * @param icon     El icono de la acción.
     * @return La nueva acción para el popup.
     */
    private Action createPopupAction(Action delegate, String command, Icon icon) {
        String name = (String) delegate.getValue(Action.NAME);
        AbstractAction popupAction = new AbstractAction(name, icon) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                delegate.actionPerformed(e);
            }
        };

        popupAction.setEnabled(delegate.isEnabled());
        popupAction.putValue(Action.ACTION_COMMAND_KEY, command);
        popupAction.putValue(Action.SHORT_DESCRIPTION, delegate.getValue(Action.SHORT_DESCRIPTION));
        return popupAction;
    } // --- Fin del método createPopupAction ---

} // --- Fin de la clase HiddenButtonsAction ---
