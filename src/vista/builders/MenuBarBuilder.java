package vista.builders;

import java.util.HashMap; // Asegúrate de tener los imports
import java.util.Map;
import javax.swing.*; // Action, ButtonGroup, etc.

// Enum interno (o importado si está en otro archivo)
enum MenuComponentType { MAIN_MENU, SUB_MENU, MENU_ITEM, UNKNOWN }

public class MenuBarBuilder {

    // --- Resultados del Builder ---
    private JMenuBar menuBar;
    private Map<String, JMenuItem> menuItemsPorNombre; // Mapa Clave Larga -> Item

    // --- Estado Interno del Builder ---
    private JMenu currentMenu = null;
    private JMenu subMenu = null;
    private ButtonGroup currentButtonGroup = null;
    private final String menuOptionsString;
    private Map<String, Action> actionMap; // Mapa Comando CORTO -> Action

    // Prefijo base para las claves de configuración del menú
    private final String CONFIG_KEY_PREFIX = "interfaz.menu";

    /**
     * Constructor que recibe la definición del menú y el mapa de Actions.
     */
    public MenuBarBuilder(String menuDefinition, Map<String, Action> actionMap) {
        if (menuDefinition == null || menuDefinition.trim().isEmpty()) {
            throw new IllegalArgumentException("La definición del menú no puede ser nula o vacía.");
        }
        this.menuOptionsString = menuDefinition;
        this.actionMap = actionMap != null ? actionMap : new HashMap<>(); // Evitar NullPointer
        this.menuItemsPorNombre = new HashMap<>();
        this.menuBar = new JMenuBar();
    }

    /**
     * Construye la barra de menú completa basada en la definición proporcionada.
     * @return La JMenuBar construida.
     */
    public JMenuBar buildMenuBar() {
        // Dividir la definición en líneas
        String[] menuOptions = menuOptionsString.split("\\R"); // Divide por saltos de línea

        // Reiniciar estado por si acaso
        currentMenu = null;
        subMenu = null;
        currentButtonGroup = null;

        for (String option : menuOptions) { // <-- Abre FOR
            String trimmedOption = option.trim();

            if (trimmedOption.isEmpty()) continue;

            // --- Manejo del separador ---
            if (trimmedOption.equals("_")) { // <-- Abre IF separador
                if (subMenu != null) {
                    subMenu.addSeparator();
                } else if (currentMenu != null) {
                    currentMenu.addSeparator();
                }
                continue;
            } // <-- Cierra IF separador

            // --- Manejo de menús y items ---
            if (trimmedOption.startsWith("-")) { // <-- Abre IF principal menus/items

                 // --- Lógica para -->, --{, --} ---
                 if (trimmedOption.equals("-->")) { subMenu = null; continue; }
                 if (trimmedOption.equals("--{")) { currentButtonGroup = new ButtonGroup(); continue; }
                 if (trimmedOption.equals("--}")) { currentButtonGroup = null; continue; }

                 // --- Declaración de variables DENTRO del ámbito correcto ---
                 String text = "";
                 String baseActionCommand = "";
                 String fullConfigKey = "";
                 MenuComponentType type = MenuComponentType.UNKNOWN;

                 // --- Determinar tipo y extraer texto ---
                 if (trimmedOption.startsWith("----")) { text = trimmedOption.substring(4).trim(); type = MenuComponentType.MENU_ITEM; }
                 else if (trimmedOption.startsWith("---")) { text = trimmedOption.substring(3).trim(); type = MenuComponentType.MENU_ITEM; }
                 else if (trimmedOption.startsWith("--<")) { text = trimmedOption.substring(3).trim(); type = MenuComponentType.SUB_MENU; }
                 else if (trimmedOption.startsWith("-") && !trimmedOption.startsWith("--")) { text = trimmedOption.substring(1).trim(); type = MenuComponentType.MAIN_MENU; }


                 if (!text.isEmpty() && type != MenuComponentType.UNKNOWN) { // <-- Abre IF (!text.isEmpty...)
                     // --- Crear comando base (corto) ---
                     baseActionCommand = text.replace("_", "").replace("*", "").replace(".", "").trim().replace(" ", "_");

                     // --- Construir Clave Jerárquica COMPLETA para CONFIGURACIÓN ---
                     switch (type) { // <-- Abre SWITCH
                         case MAIN_MENU:
                             fullConfigKey = CONFIG_KEY_PREFIX + "." + baseActionCommand.toLowerCase();
                             break;
                         case SUB_MENU:
                             if (currentMenu != null && currentMenu.getActionCommand() != null) {
                                 fullConfigKey = currentMenu.getActionCommand() + "." + baseActionCommand.toLowerCase();
                             } else { /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".error." + baseActionCommand.toLowerCase(); }
                             break;
                         case MENU_ITEM:
                             String parentKey = "";
                             if (subMenu != null && subMenu.getActionCommand() != null) parentKey = subMenu.getActionCommand();
                             else if (currentMenu != null && currentMenu.getActionCommand() != null) parentKey = currentMenu.getActionCommand();

                             if (!parentKey.isEmpty()) {
                                 fullConfigKey = parentKey + "." + baseActionCommand;
                             } else { /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".error." + baseActionCommand; }
                             break;
                         default: /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".unknown." + baseActionCommand; break;
                     } // <-- Cierra SWITCH

                     // --- Crear el componente Swing ---
                     JMenuItem menuItemComponent = null;
                     if (type == MenuComponentType.MAIN_MENU) { // <-- Abre IF (type == MAIN_MENU)
                         JMenu mainMenu = new JMenu(text);
                         menuItemComponent = mainMenu;
                         menuBar.add(mainMenu);
                         currentMenu = mainMenu;
                         subMenu = null;
                     } else if (type == MenuComponentType.SUB_MENU) { // <-- Abre ELSE IF (type == SUB_MENU)
                         JMenu newSubMenu = new JMenu(text);
                         menuItemComponent = newSubMenu;
                         if (currentMenu != null) currentMenu.add(newSubMenu);
                         else menuBar.add(newSubMenu);
                         subMenu = newSubMenu;
                     } else if (type == MenuComponentType.MENU_ITEM) { // <-- Abre ELSE IF (type == MENU_ITEM)
                         menuItemComponent = createMenuItemInternal(text, currentButtonGroup); // Llama al método interno
                         if (subMenu != null) subMenu.add(menuItemComponent);
                         else if (currentMenu != null) currentMenu.add(menuItemComponent);
                         else System.err.println("WARN [MenuBarBuilder]: Intentando añadir item sin menú/submenú padre: " + text);
                     } // <-- Cierra ELSE IF (type == MENU_ITEM)

                     // --- Asignar Claves y Añadir al Mapa ---
                     if (menuItemComponent != null && !fullConfigKey.isEmpty() && !baseActionCommand.isEmpty()) { // <-- Abre IF (menuItemComponent != null...)
                         menuItemsPorNombre.put(fullConfigKey, menuItemComponent); // Clave larga para config

                         // --- Usar setAction si existe ---
                         Action action = actionMap.get(baseActionCommand); // Busca Action por comando CORTO
                         if (action != null && !(menuItemComponent instanceof JMenu)) { // <-- Abre IF (action != null...)
                             // Asignar Action solo a items finales, no a los JMenu contenedores
                             menuItemComponent.setAction(action);
                             // System.out.println("  Menu Item Creado: '" + text + "' -> Action Asignada: " + action.getValue(Action.NAME));
                         } else { // <-- Abre ELSE (no hay action o es JMenu)
                             // Fallback o asignación normal de AC
                             if (menuItemComponent instanceof JMenu) { // <-- Abre IF (instanceof JMenu)
                                 menuItemComponent.setActionCommand(fullConfigKey); // JMenu usa clave larga
                             } else { // <-- Abre ELSE (item final sin action)
                                 menuItemComponent.setActionCommand(baseActionCommand); // Item final usa clave corta
                             } // <-- Cierra ELSE (item final sin action)
                            // System.out.println("  Menu/Item Creado: '" + text + "' -> AC: '" + menuItemComponent.getActionCommand() + "' (Config Key: "+fullConfigKey+")");
                         } // <-- Cierra ELSE (no hay action o es JMenu)

                     } // <-- Cierra IF (menuItemComponent != null...)
                 } // <-- Cierra IF (!text.isEmpty...)

            } // <-- Cierra IF principal menus/items (if trimmedOption.startsWith("-"))

        } // <-- Cierra FOR

        System.out.println("MenuBarBuilder: Menú construido. Total items en mapa: " + menuItemsPorNombre.size());
        return menuBar;

    } // <-- Cierra MÉTODO buildMenuBar

    // --- Método auxiliar interno para crear JMenuItems ---
    private JMenuItem createMenuItemInternal(String text, ButtonGroup buttonGroup) {
        if (text.startsWith("*")) {
            return new JCheckBoxMenuItem(text.substring(1));
        } else if (text.startsWith(".")) {
            JRadioButtonMenuItem radioButtonMenuItem = new JRadioButtonMenuItem(text.substring(1));
            if (buttonGroup != null) {
                buttonGroup.add(radioButtonMenuItem);
            }
            return radioButtonMenuItem;
        } else {
            return new JMenuItem(text);
        }
    }

    // --- Getter para el mapa ---
    public Map<String, JMenuItem> getMenuItemsMap() {
        return this.menuItemsPorNombre;
    }

} // <-- Cierra CLASE MenuBarBuilder

