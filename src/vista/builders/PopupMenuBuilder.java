// En vista.builders.PopupMenuBuilder.java
package vista.builders;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.config.ViewUIConfig;

public class PopupMenuBuilder {
    private ViewUIConfig uiConfig;
    private ButtonGroup currentRadioGroup; // Para manejar grupos de radio buttons

    public PopupMenuBuilder(ViewUIConfig uiConfig) {
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo en PopupMenuBuilder");
    }

    /**
     * Construye un JPopupMenu a partir de una lista de MenuItemDefinition.
     * Este método ahora puede manejar estructuras de menú anidadas.
     */
    public JPopupMenu buildPopupMenuWithNestedMenus(List<MenuItemDefinition> definitions, Map<String, Action> actionMap) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (uiConfig != null) {
            popupMenu.setBackground(uiConfig.colorFondoSecundario);
            // popupMenu.setBorder(...); // Estilo del borde si es necesario
        }
        addItemsToMenuComponent(popupMenu, definitions, actionMap);
        return popupMenu;
    }

    // Método recursivo para añadir items a un JMenu o JPopupMenu
    private void addItemsToMenuComponent(JComponent parentMenuComponent, List<MenuItemDefinition> items, Map<String, Action> actionMap) {
        for (MenuItemDefinition def : items) {
            MenuItemType type = def.tipo();
            Action action = (def.comandoOClave() != null) ? actionMap.get(def.comandoOClave()) : null;
            String text = (action != null && action.getValue(Action.NAME) != null) ? (String) action.getValue(Action.NAME) : def.textoMostrado();
            if (text == null) text = ""; // Evitar null text

            switch (type) {
                case SEPARATOR:
                    if (parentMenuComponent instanceof JMenu) ((JMenu)parentMenuComponent).addSeparator();
                    else if (parentMenuComponent instanceof JPopupMenu) ((JPopupMenu)parentMenuComponent).addSeparator();
                    break;
                case RADIO_GROUP_START:
                    currentRadioGroup = new ButtonGroup();
                    break;
                case RADIO_GROUP_END:
                    currentRadioGroup = null; // Finalizar grupo
                    break;
                case ITEM:
                case CHECKBOX_ITEM:
                case RADIO_BUTTON_ITEM:
                    JMenuItem menuItem;
                    if (type == MenuItemType.CHECKBOX_ITEM) {
                        menuItem = new JCheckBoxMenuItem(text);
                        if (action != null) menuItem.setAction(action); // Asignar acción para estado y evento
                    } else if (type == MenuItemType.RADIO_BUTTON_ITEM) {
                        menuItem = new JRadioButtonMenuItem(text);
                        if (action != null) menuItem.setAction(action);
                        if (currentRadioGroup != null) {
                            currentRadioGroup.add(menuItem);
                        }
                    } else { // ITEM
                        menuItem = new JMenuItem(text);
                        if (action != null) menuItem.setAction(action);
                    }

                    if (action == null && def.comandoOClave() != null) {
                        System.err.println("WARN [PopupMenuBuilder]: No se encontró Action para: " + def.comandoOClave() + " (Texto: " + text + ")");
                        menuItem.setEnabled(false);
                    }
                    applyMenuItemStyle(menuItem);
                    if (parentMenuComponent instanceof JMenu) ((JMenu)parentMenuComponent).add(menuItem);
                    else if (parentMenuComponent instanceof JPopupMenu) ((JPopupMenu)parentMenuComponent).add(menuItem);
                    break;

                case MAIN_MENU: // En un JPopupMenu, un MAIN_MENU se trata como un JMenu
                case SUB_MENU:
                    JMenu subMenu = new JMenu(text);
                    applyMenuItemStyle(subMenu); // Estilo para el JMenu en sí
                    if (def.subItems() != null && !def.subItems().isEmpty()) {
                        addItemsToMenuComponent(subMenu, def.subItems(), actionMap); // Llamada recursiva
                    }
                    if (parentMenuComponent instanceof JMenu) ((JMenu)parentMenuComponent).add(subMenu);
                    else if (parentMenuComponent instanceof JPopupMenu) ((JPopupMenu)parentMenuComponent).add(subMenu);
                    break;
                default:
                    System.err.println("WARN [PopupMenuBuilder]: Tipo de MenuItemType no manejado: " + type);
                    break;
            }
        }
    }
    
    private void applyMenuItemStyle(JMenuItem menuItem) {
        if (uiConfig != null && menuItem != null) {
            menuItem.setBackground(uiConfig.colorFondoSecundario);
            menuItem.setForeground(uiConfig.colorTextoPrimario);
            // Configurar Opaque si es necesario para que el fondo se vea, especialmente en JMenu
            if (menuItem instanceof JMenu) {
                // Para JMenu, el color de fondo del popup es más relevante
                // JPopupMenu subPopupMenu = ((JMenu) menuItem).getPopupMenu();
                // subPopupMenu.setBackground(uiConfig.colorFondoSecundario);
                // subPopupMenu.setBorder(...);
            }
        }
    }
}