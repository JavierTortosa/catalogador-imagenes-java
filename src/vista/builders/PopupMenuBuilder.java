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

import servicios.ConfigurationManager; 
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;
import vista.config.ViewUIConfig;

public class PopupMenuBuilder {
    private ViewUIConfig uiConfig;
    private ButtonGroup currentRadioGroup;
    private ConfigurationManager configManagerRef;         
    private ActionListener specialConfigActionListenerRef; 

    // --- CONSTRUCTOR MODIFICADO ---
    public PopupMenuBuilder(
            ViewUIConfig uiConfig,
            ConfigurationManager configManager,         
            ActionListener specialConfigActionListener  
    ) {
        this.uiConfig = Objects.requireNonNull(uiConfig, "ViewUIConfig no puede ser nulo en PopupMenuBuilder");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser nulo en PopupMenuBuilder");
        this.specialConfigActionListenerRef = Objects.requireNonNull(specialConfigActionListener, "specialConfigActionListener no puede ser nulo en PopupMenuBuilder");
    }

    public JPopupMenu buildPopupMenuWithNestedMenus(List<MenuItemDefinition> definitions, Map<String, Action> actionMap) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (uiConfig != null) {
            popupMenu.setBackground(uiConfig.colorFondoSecundario);
        }
        addItemsToMenuComponent(popupMenu, definitions, actionMap);
        return popupMenu;
    }

    private void addItemsToMenuComponent(JComponent parentMenuComponent, List<MenuItemDefinition> items, Map<String, Action> actionMap) {
        for (MenuItemDefinition def : items) {
            MenuItemType type = def.tipo();
            String comandoOClave = def.actionCommand(); // Clave de la definición (puede ser AppActionCommand o clave de config)
            
            // Intentar obtener la Action del mapa principal si hay un comandoOClave
            Action action = (comandoOClave != null) ? actionMap.get(comandoOClave) : null;
            
            // Determinar el texto a mostrar
            // Prioridad 1: Nombre de la Action (si existe y tiene nombre)
            // Prioridad 2: Texto definido en MenuItemDefinition
            // Prioridad 3: Cadena vacía para evitar null
            String text = def.textoMostrado(); // Empezar con el texto de la definición
            if (action != null && action.getValue(Action.NAME) != null) {
                String actionName = (String) action.getValue(Action.NAME);
                if (actionName != null && !actionName.isBlank()) { // Si la Action tiene un nombre válido
                    text = actionName; // Usar el nombre de la Action
                }
            }
            if (text == null) text = ""; // Asegurar que text no sea null

            JMenuItem menuItem = null; // Variable para el JMenuItem que se creará

            switch (type) {
                case SEPARATOR:
                    if (parentMenuComponent instanceof JMenu) {
                        ((JMenu) parentMenuComponent).addSeparator();
                    } else if (parentMenuComponent instanceof JPopupMenu) {
                        ((JPopupMenu) parentMenuComponent).addSeparator();
                    }
                    // No se crea 'menuItem', así que el 'if (menuItem != null)' de abajo lo omitirá
                    break; // Importante salir aquí

                case RADIO_GROUP_START:
                    currentRadioGroup = new ButtonGroup();
                    // No se crea 'menuItem'
                    break; // Importante salir aquí

                case RADIO_GROUP_END:
                    currentRadioGroup = null;
                    // No se crea 'menuItem'
                    break; // Importante salir aquí

                case ITEM:
                    menuItem = new JMenuItem(text); // Crear JMenuItem con el texto determinado
                    if (action != null) {
                        // Si se encontró una Action en el actionMap principal, asignarla.
                        menuItem.setAction(action);
                    } else if (comandoOClave != null) {
                        // No hay Action directa, pero hay un comandoOClave.
                        // Asumimos que será manejado por el specialConfigActionListenerRef (VisorController)
                        // usando el comandoOClave como ActionCommand.
                        menuItem.setActionCommand(comandoOClave);
                        menuItem.addActionListener(this.specialConfigActionListenerRef);
                        menuItem.setEnabled(true); // Asegurar que esté habilitado
                        System.out.println("  [PopupMenuBuilder] ITEM '" + text + "' (sin Action directa) configurado para specialListener. Comando: " + comandoOClave);
                    }
                    // Si action es null Y comandoOClave es null, el JMenuItem será solo texto, sin acción.
                    break;

                case CHECKBOX_ITEM:
                    JCheckBoxMenuItem cbMenuItem = new JCheckBoxMenuItem(text);
                    menuItem = cbMenuItem; // Asignar a la variable genérica

                    if (action != null) {
                        // Caso 1: Hay una Action directa para este checkbox (ej. ToggleThemeAction)
                        cbMenuItem.setAction(action);
                    } else if (comandoOClave != null && comandoOClave.startsWith("interfaz.boton.")) {
                        // Caso 2: Es un checkbox de "Visualizar Botón de Toolbar"
                        // Su comandoOClave es la clave de config del botón de la toolbar (ej. "interfaz.boton.edicion...")
                        cbMenuItem.setActionCommand(comandoOClave); // El ActionCommand será la clave del botón
                        
                        // Leer el estado inicial 'selected' desde ConfigurationManager
                        // Asumimos que la clave para la visibilidad del botón es "comandoOClave.visible"
                        String configKeyVisibilidadBotonToolbar = comandoOClave + ".visible";
                        boolean botonEsVisible = false; // Default si configManagerRef es null
                        if (this.configManagerRef != null) {
                            botonEsVisible = this.configManagerRef.getBoolean(configKeyVisibilidadBotonToolbar, true);
                        } else {
                            System.err.println("WARN [PopupMenuBuilder]: configManagerRef es null. No se puede leer estado inicial para checkbox: " + text);
                        }
                        cbMenuItem.setSelected(botonEsVisible);
                        
                        // Añadir el specialConfigActionListenerRef (VisorController)
                        if (this.specialConfigActionListenerRef != null) {
                            cbMenuItem.addActionListener(this.specialConfigActionListenerRef);
                        } else {
                             System.err.println("WARN [PopupMenuBuilder]: specialConfigActionListenerRef es null. Checkbox '" + text + "' podría no ser funcional.");
                        }
                        cbMenuItem.setEnabled(true); // Asegurar que esté habilitado
                        System.out.println("  [PopupMenuBuilder] Checkbox (Visibilidad Botón) '" + text + "' configurado. Comando: " + comandoOClave + ", Estado Inicial: " + botonEsVisible);

                    } else if (comandoOClave != null) {
                        // Caso 3: Es otro tipo de checkbox sin Action directa y no es de "Visualizar Botón"
                        System.err.println("WARN [PopupMenuBuilder]: No se encontró Action (ni es config especial de botón) para CHECKBOX: " + comandoOClave + " | (Texto: " + text + ")");
                        cbMenuItem.setEnabled(false);
                    }
                    // Si action es null Y comandoOClave es null, es un checkbox solo visual (raro).
                    break;

                case RADIO_BUTTON_ITEM:
                    JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(text);
                    menuItem = rbMenuItem; // Asignar a la variable genérica

                    if (action != null) {
                        // Caso 1: Hay una Action directa para este radio (ej. AplicarModoZoomAction)
                        rbMenuItem.setAction(action);
                    } else if (comandoOClave != null) {
                        // Caso 2: RadioButton sin Action directa, manejado por specialConfigActionListenerRef (VisorController)
                        rbMenuItem.setActionCommand(comandoOClave);
                        if (this.specialConfigActionListenerRef != null) {
                            rbMenuItem.addActionListener(this.specialConfigActionListenerRef);
                        } else {
                            System.err.println("WARN [PopupMenuBuilder]: specialConfigActionListenerRef es null. Radio '" + text + "' podría no ser funcional.");
                        }
                        rbMenuItem.setEnabled(true); // Asegurar que esté habilitado

                        // Establecer el estado 'selected' inicial es complejo aquí sin la clave de config.
                        // Si la Action no lo maneja, y el VisorController lo hace,
                        // el estado inicial podría no ser el correcto hasta la primera interacción,
                        // o necesitar una sincronización explícita después de construir el popup.
                        // Por ahora, dejamos que el ButtonGroup gestione la selección si otros radios SÍ tienen Action.
                        System.out.println("  [PopupMenuBuilder] RADIO '" + text + "' (sin Action directa) configurado para specialListener. Comando: " + comandoOClave);
                    }
                    // Si action es null Y comandoOClave es null, es un radio solo visual (raro).

                    if (currentRadioGroup != null) {
                        currentRadioGroup.add(rbMenuItem);
                    }
                    break;

                case MAIN_MENU:
                case SUB_MENU:
                    JMenu subMenu = new JMenu(text); // Un MAIN_MENU en un JPopupMenu se trata como un JMenu normal
                    menuItem = subMenu; // Asignar para applyStyle y añadirlo
                    // Los JMenu (contenedores) generalmente no tienen 'action' o 'comandoOClave' funcional propio.
                    // Si tuvieran una Action aquí (raro para un JMenu), se podría asignar, pero no es lo usual.
                    // subMenu.setAction(action); // Si fuera necesario, pero menuItem.setAction() no funciona bien para JMenu para texto
                    
                    if (def.subItems() != null && !def.subItems().isEmpty()) {
                        addItemsToMenuComponent(subMenu, def.subItems(), actionMap); // Llamada recursiva
                    }
                    break;
                default:
                    System.err.println("WARN [PopupMenuBuilder]: Tipo de MenuItemType no manejado: " + type + " para texto: " + text);
                    // No se crea 'menuItem'
                    break; // Importante salir aquí
            }

            // Añadir el menuItem al componente padre SI se creó uno
            if (menuItem != null) {
                applyMenuItemStyle(menuItem); // Aplicar estilo ANTES de añadirlo
                if (parentMenuComponent instanceof JMenu) {
                    ((JMenu) parentMenuComponent).add(menuItem);
                } else if (parentMenuComponent instanceof JPopupMenu) {
                    ((JPopupMenu) parentMenuComponent).add(menuItem);
                }
            }
        }
    }
    
    private void applyMenuItemStyle(JMenuItem menuItem) {
        if (uiConfig != null && menuItem != null) {
            menuItem.setBackground(uiConfig.colorFondoSecundario);
            menuItem.setForeground(uiConfig.colorTextoPrimario);
        }
    }
} // --- FIN de la clase PopupMenuBuilder

