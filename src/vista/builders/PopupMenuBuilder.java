package vista.builders;

import java.awt.event.ActionListener;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.commands.AppActionCommands;
import servicios.ConfigurationManager;
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.theme.Tema;
import vista.theme.ThemeManager;

public class PopupMenuBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PopupMenuBuilder.class);

    private ThemeManager themeManager;
    private ButtonGroup currentRadioGroup;
    private ConfigurationManager configManagerRef;
    private Map<String, Action> actionMap; // Movido a campo de instancia

    /**
     * Constructor que ya NO requiere un ActionListener de fallback.
     * @param themeManager El gestor de temas para aplicar estilos.
     * @param configManager El gestor de configuración.
     */
    public PopupMenuBuilder(
            ThemeManager themeManager,
            ConfigurationManager configManager
    ) {
        logger.debug("[PopupMenuBuilder] Iniciando...");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser nulo en PopupMenuBuilder");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser nulo en PopupMenuBuilder");
    } // ---FIN de metodo PopupMenuBuilder (constructor)---

    /**
     * Construye un JPopupMenu completo con menús anidados a partir de una estructura de definiciones.
     * @param definitions La estructura de menú a construir.
     * @param actionMap El mapa de acciones de la aplicación.
     * @return El JPopupMenu construido.
     */
    public JPopupMenu buildPopupMenuWithNestedMenus(List<MenuItemDefinition> definitions, Map<String, Action> actionMap) {
        JPopupMenu popupMenu = new JPopupMenu();
        this.actionMap = Objects.requireNonNull(actionMap); // Almacenar el actionMap

        Tema temaActual = themeManager.getTemaActual();
        if (temaActual != null) {
            popupMenu.setBackground(temaActual.colorFondoSecundario());
        }

        addItemsToMenuComponent(popupMenu, definitions, temaActual);
        return popupMenu;
    } // ---FIN de metodo buildPopupMenuWithNestedMenus---

    /**
     * Método recursivo que añade ítems a un componente de menú (JPopupMenu o JMenu).
     * @param parentMenuComponent El componente padre al que añadir los ítems.
     * @param items La lista de definiciones de ítems para este nivel.
     * @param temaActual El tema actual para aplicar estilos.
     */
    private void addItemsToMenuComponent(
            JComponent parentMenuComponent,
            List<MenuItemDefinition> items,
            Tema temaActual
    ) {
        for (MenuItemDefinition def : items) {
            MenuItemType type = def.tipo();
            String comandoOClave = def.actionCommand();
            Action action = (comandoOClave != null) ? this.actionMap.get(comandoOClave) : null;

            String text = def.textoMostrado();
            if (action != null && action.getValue(Action.NAME) != null) {
                String actionName = (String) action.getValue(Action.NAME);
                if (actionName != null && !actionName.isBlank()) {
                    text = actionName;
                }
            }
            if (text == null) text = "";

            JMenuItem menuItem = null;

            switch (type) {
                case SEPARATOR:
                    if (parentMenuComponent instanceof JMenu) {
                        ((JMenu) parentMenuComponent).addSeparator();
                    } else if (parentMenuComponent instanceof JPopupMenu) {
                        ((JPopupMenu) parentMenuComponent).addSeparator();
                    }
                    break;

                case RADIO_GROUP_START:
                    currentRadioGroup = new ButtonGroup();
                    break;

                case RADIO_GROUP_END:
                    currentRadioGroup = null;
                    break;

                case ITEM:
                    menuItem = new JMenuItem(text);
                    if (action != null) {
                        menuItem.setAction(action);
                    } else {
                        // No hay Action directa, asignar la de "funcionalidad pendiente"
                        Action pendiente = this.actionMap.get(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE);
                        if (pendiente != null) {
                            menuItem.setAction(pendiente);
                            menuItem.setText(text); // Restaurar texto por si la action lo cambia
                            menuItem.setActionCommand(comandoOClave); // Guardar comando original para el mensaje
                        } else {
                            menuItem.setEnabled(false); // Fallback si ni siquiera hay acción pendiente
                        }
                    }
                    break;

                case CHECKBOX_ITEM:
                    JCheckBoxMenuItem cbMenuItem = new JCheckBoxMenuItem(text);
                    menuItem = cbMenuItem;

                    if (action != null) {
                        cbMenuItem.setAction(action);
                    } else if (comandoOClave != null && comandoOClave.startsWith("interfaz.boton.")) {
                        // Lógica original para visibilidad de botones, pero sin ActionListener
                        cbMenuItem.setActionCommand(comandoOClave);
                        String configKeyVisibilidadBotonToolbar = comandoOClave + ".visible";
                        boolean botonEsVisible = configManagerRef.getBoolean(configKeyVisibilidadBotonToolbar, true);
                        cbMenuItem.setSelected(botonEsVisible);
                        
                        // Asignar acción pendiente en lugar de listener
                        Action pendiente = this.actionMap.get(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE);
                        if (pendiente != null) {
                            cbMenuItem.setAction(pendiente);
                            cbMenuItem.setText(text);
                            cbMenuItem.setActionCommand(comandoOClave);
                        } else {
                            cbMenuItem.setEnabled(false);
                        }
                        logger.debug("  [PopupMenuBuilder] Checkbox (Visibilidad Botón) '" + text + "' configurado sin Action directa. Comando: " + comandoOClave);
                    } else {
                        // Otro tipo de checkbox sin Action directa
                        Action pendiente = this.actionMap.get(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE);
                        if (pendiente != null) {
                            cbMenuItem.setAction(pendiente);
                            cbMenuItem.setText(text);
                            cbMenuItem.setActionCommand(comandoOClave);
                        } else {
                            cbMenuItem.setEnabled(false);
                        }
                    }
                    break;

                case RADIO_BUTTON_ITEM:
                    JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(text);
                    menuItem = rbMenuItem;

                    if (action != null) {
                        rbMenuItem.setAction(action);
                    } else {
                        // No hay Action directa, asignar la de "funcionalidad pendiente"
                        Action pendiente = this.actionMap.get(AppActionCommands.CMD_FUNCIONALIDAD_PENDIENTE);
                        if (pendiente != null) {
                            rbMenuItem.setAction(pendiente);
                            rbMenuItem.setText(text);
                            rbMenuItem.setActionCommand(comandoOClave);
                        } else {
                            rbMenuItem.setEnabled(false);
                        }
                    }

                    if (currentRadioGroup != null) {
                        currentRadioGroup.add(rbMenuItem);
                    }
                    break;

                case MAIN_MENU:
                case SUB_MENU:
                    JMenu subMenu = new JMenu(text);
                    menuItem = subMenu;
                    if (def.subItems() != null && !def.subItems().isEmpty()) {
                        addItemsToMenuComponent(subMenu, def.subItems(), temaActual); // Llamada recursiva
                    }
                    break;
                    
                default:
                    logger.warn("WARN [PopupMenuBuilder]: Tipo de MenuItemType no manejado: " + type + " para texto: " + text);
                    break;
            }

            if (menuItem != null) {
                applyMenuItemStyle(menuItem, temaActual);
                if (parentMenuComponent instanceof JMenu) {
                    ((JMenu) parentMenuComponent).add(menuItem);
                } else if (parentMenuComponent instanceof JPopupMenu) {
                    ((JPopupMenu) parentMenuComponent).add(menuItem);
                }
            }
        }
    } // ---FIN de metodo addItemsToMenuComponent---

    /**
     * Aplica el estilo del tema actual a un JMenuItem.
     * @param menuItem El ítem al que aplicar el estilo.
     * @param tema El tema actual.
     */
    private void applyMenuItemStyle(JMenuItem menuItem, Tema tema) {
        if (tema != null && menuItem != null) {
            menuItem.setBackground(tema.colorFondoSecundario());
            menuItem.setForeground(tema.colorTextoPrimario());
        }
    } // ---FIN de metodo applyMenuItemStyle---

} // --- FIN de la clase PopupMenuBuilder ---