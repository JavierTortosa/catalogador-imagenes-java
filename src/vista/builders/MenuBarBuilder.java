package vista.builders;

import java.util.HashMap; // Asegúrate de tener los imports
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
//import javax.swing.*; // Action, ButtonGroup, etc.

import vista.config.MenuItemDefinition; // Importar
import vista.config.MenuItemType;      // Importar

// Enum interno (o importado si está en otro archivo)
enum MenuComponentType { MAIN_MENU, SUB_MENU, MENU_ITEM, UNKNOWN }

public class MenuBarBuilder {

    // --- Resultados del Builder ---
    private JMenuBar menuBar;
    private Map<String, JMenuItem> menuItemsPorNombre; // Mapa Clave Larga -> Item

    // --- Estado Interno del Builder ---
//    private JMenu currentMenu = null;
//    private JMenu subMenu = null;
//    private final String menuOptionsString;
//    private String menuDefinition;
    private Map<String, Action> actionMap; // Mapa Comando CORTO -> Action
    private ButtonGroup currentButtonGroup = null;
    
    // Prefijo base para las claves de configuración del menú
    private final String CONFIG_KEY_PREFIX = "interfaz.menu";
    
    
    /**
     * Constructor simplificado. Inicializa las estructuras internas.
     */
    public MenuBarBuilder() {
        this.menuItemsPorNombre = new HashMap<>();
        this.menuBar = new JMenuBar();
        // actionMap se recibirá en buildMenuBar
        // currentButtonGroup se inicializa a null
    }
    
//    /**
//     * Constructor que recibe la definición del menú y el mapa de Actions.
//     */
//    public MenuBarBuilder(String menuDefinition, Map<String, Action> actionMap) {
//        if (menuDefinition == null || menuDefinition.trim().isEmpty()) {
//            throw new IllegalArgumentException("La definición del menú no puede ser nula o vacía.");
//        }
//        this.menuOptionsString = menuDefinition;
//        this.actionMap = actionMap != null ? actionMap : new HashMap<>(); // Evitar NullPointer
//        this.menuItemsPorNombre = new HashMap<>();
//        this.menuBar = new JMenuBar();
//    }
//
//    
//    public MenuBarBuilder(Map<String, Action> actionMap) {
//    	menuDefinition = getMenuDefinitionString();
//        if (menuDefinition == null || menuDefinition.trim().isEmpty()) {
//            throw new IllegalArgumentException("La definición del menú no puede ser nula o vacía.");
//        }
//        this.menuOptionsString = menuDefinition;
//        this.actionMap = actionMap != null ? actionMap : new HashMap<>(); // Evitar NullPointer
//        this.menuItemsPorNombre = new HashMap<>();
//        this.menuBar = new JMenuBar();
//    }
    

    /**
     * Construye la barra de menú completa basada en la estructura de definiciones explícita.
     * @param menuStructure La lista que define la jerarquía y los ítems del menú.
     * @param actionMap El mapa que asocia comandos canónicos (AppActionCommands) con sus Actions.
     * @return La JMenuBar construida.
     */
    public JMenuBar buildMenuBar(List<MenuItemDefinition> menuStructure, Map<String, Action> actionMap) {

        // --- 1. Inicialización y Validación ---
        // 1.1. Reiniciar estado interno por si se reutiliza el builder
        this.menuBar = new JMenuBar();
        this.menuItemsPorNombre = new HashMap<>();
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null"); // Guardar referencia
        this.currentButtonGroup = null;

        System.out.println("--- MenuBarBuilder: Construyendo menú desde estructura definida ---");
        // 1.2. Validar estructura de entrada
        if (menuStructure == null || menuStructure.isEmpty()) {
            System.err.println("WARN [MenuBarBuilder]: La estructura del menú está vacía o es nula.");
            return this.menuBar; // Devuelve barra vacía
        }

        // --- 2. Procesar Elementos Raíz ---
        // 2.1. Iterar sobre los elementos de nivel superior
        for (MenuItemDefinition itemDef : menuStructure) {
            // 2.2. Validar que los elementos raíz sean menús principales
            if (itemDef.tipo() == MenuItemType.MAIN_MENU) {
                // 2.3. Llamar al método recursivo para procesar este menú y sus hijos
                processMenuItemDefinition(itemDef, menuBar, null, null); // Padre es JMenuBar, sin menús padre
            } else {
                System.err.println("WARN [MenuBarBuilder]: Se encontró un tipo inesperado (" + itemDef.tipo()
                                   + ") en el nivel superior. Se esperaba MAIN_MENU. Texto: '" + itemDef.textoMostrado() + "'");
            }
        }

        // --- 3. Finalización ---
        System.out.println("--- MenuBarBuilder: Menú construido. Total items en mapa: " + menuItemsPorNombre.size() + " ---");
        return this.menuBar; // Devolver la barra construida
    }// --- FIN metodo buildMenuBar    
    
    
//    /**
//     * Construye la barra de menú completa basada en la definición proporcionada.
//     * @return La JMenuBar construida.
//     */
//    public JMenuBar buildMenuBar() {
//        // Dividir la definición en líneas
//        String[] menuOptions = menuOptionsString.split("\\R"); // Divide por saltos de línea
//
//        // Reiniciar estado por si acaso
//        currentMenu = null;
//        subMenu = null;
//        currentButtonGroup = null;
//
//        for (String option : menuOptions) { // <-- Abre FOR
//            String trimmedOption = option.trim();
//
//            if (trimmedOption.isEmpty()) continue;
//
//            // --- Manejo del separador ---
//            if (trimmedOption.equals("_")) { // <-- Abre IF separador
//                if (subMenu != null) {
//                    subMenu.addSeparator();
//                } else if (currentMenu != null) {
//                    currentMenu.addSeparator();
//                }
//                continue;
//            } // <-- Cierra IF separador
//
//            // --- Manejo de menús y items ---
//            if (trimmedOption.startsWith("-")) { // <-- Abre IF principal menus/items
//
//                 // --- Lógica para -->, --{, --} ---
//                 if (trimmedOption.equals("-->")) { subMenu = null; continue; }
//                 if (trimmedOption.equals("--{")) { currentButtonGroup = new ButtonGroup(); continue; }
//                 if (trimmedOption.equals("--}")) { currentButtonGroup = null; continue; }
//
//                 // --- Declaración de variables DENTRO del ámbito correcto ---
//                 String text = "";
//                 String baseActionCommand = "";
//                 String fullConfigKey = "";
//                 MenuComponentType type = MenuComponentType.UNKNOWN;
//
//                 // --- Determinar tipo y extraer texto ---
//                 if (trimmedOption.startsWith("----")) { text = trimmedOption.substring(4).trim(); type = MenuComponentType.MENU_ITEM; }
//                 else if (trimmedOption.startsWith("---")) { text = trimmedOption.substring(3).trim(); type = MenuComponentType.MENU_ITEM; }
//                 else if (trimmedOption.startsWith("--<")) { text = trimmedOption.substring(3).trim(); type = MenuComponentType.SUB_MENU; }
//                 else if (trimmedOption.startsWith("-") && !trimmedOption.startsWith("--")) { text = trimmedOption.substring(1).trim(); type = MenuComponentType.MAIN_MENU; }
//
//
//                 if (!text.isEmpty() && type != MenuComponentType.UNKNOWN) { // <-- Abre IF (!text.isEmpty...)
//                     // --- Crear comando base (corto) ---
//                     baseActionCommand = text.replace("_", "").replace("*", "").replace(".", "").trim().replace(" ", "_");
//
//                     // --- Construir Clave Jerárquica COMPLETA para CONFIGURACIÓN ---
//                     switch (type) { // <-- Abre SWITCH
//                         case MAIN_MENU:
//                             fullConfigKey = CONFIG_KEY_PREFIX + "." + baseActionCommand.toLowerCase();
//                             break;
//                         case SUB_MENU:
//                             if (currentMenu != null && currentMenu.getActionCommand() != null) {
//                                 fullConfigKey = currentMenu.getActionCommand() + "." + baseActionCommand.toLowerCase();
//                             } else { /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".error." + baseActionCommand.toLowerCase(); }
//                             break;
//                         case MENU_ITEM:
//                             String parentKey = "";
//                             if (subMenu != null && subMenu.getActionCommand() != null) parentKey = subMenu.getActionCommand();
//                             else if (currentMenu != null && currentMenu.getActionCommand() != null) parentKey = currentMenu.getActionCommand();
//
//                             if (!parentKey.isEmpty()) {
//                                 fullConfigKey = parentKey + "." + baseActionCommand;//.toLowerCase();
//                             } else { /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".error." + baseActionCommand; }
//                             break;
//                         default: /* fallback */ fullConfigKey = CONFIG_KEY_PREFIX + ".unknown." + baseActionCommand; break;
//                     } // <-- Cierra SWITCH
//
//                     // --- Crear el componente Swing ---
//                     JMenuItem menuItemComponent = null;
//                     if (type == MenuComponentType.MAIN_MENU) { // <-- Abre IF (type == MAIN_MENU)
//                         JMenu mainMenu = new JMenu(text);
//                         menuItemComponent = mainMenu;
//                         menuBar.add(mainMenu);
//                         currentMenu = mainMenu;
//                         subMenu = null;
//                     } else if (type == MenuComponentType.SUB_MENU) { // <-- Abre ELSE IF (type == SUB_MENU)
//                         JMenu newSubMenu = new JMenu(text);
//                         menuItemComponent = newSubMenu;
//                         if (currentMenu != null) currentMenu.add(newSubMenu);
//                         else menuBar.add(newSubMenu);
//                         subMenu = newSubMenu;
//                     } else if (type == MenuComponentType.MENU_ITEM) { // <-- Abre ELSE IF (type == MENU_ITEM)
//                         menuItemComponent = createMenuItemInternal(text, currentButtonGroup); // Llama al método interno
//                         if (subMenu != null) subMenu.add(menuItemComponent);
//                         else if (currentMenu != null) currentMenu.add(menuItemComponent);
//                         else System.err.println("WARN [MenuBarBuilder]: Intentando añadir item sin menú/submenú padre: " + text);
//                     } // <-- Cierra ELSE IF (type == MENU_ITEM)
//
//                     // --- Asignar Claves y Añadir al Mapa ---
//                     if (menuItemComponent != null && !fullConfigKey.isEmpty() && !baseActionCommand.isEmpty()) { // <-- Abre IF (menuItemComponent != null...)
//                         menuItemsPorNombre.put(fullConfigKey, menuItemComponent); // Clave larga para config
//
//                         // --- Usar setAction si existe ---
//                         Action action = actionMap.get(baseActionCommand); // Busca Action por comando CORTO
//                         if (action != null && !(menuItemComponent instanceof JMenu)) { // <-- Abre IF (action != null...)
//                             // Asignar Action solo a items finales, no a los JMenu contenedores
//                             menuItemComponent.setAction(action);
//                             // System.out.println("  Menu Item Creado: '" + text + "' -> Action Asignada: " + action.getValue(Action.NAME));
//                         } else { // <-- Abre ELSE (no hay action o es JMenu)
//                             // Fallback o asignación normal de AC
//                             if (menuItemComponent instanceof JMenu) { // <-- Abre IF (instanceof JMenu)
//                                 menuItemComponent.setActionCommand(fullConfigKey); // JMenu usa clave larga
//                             } else { // <-- Abre ELSE (item final sin action)
//                                 menuItemComponent.setActionCommand(baseActionCommand); // Item final usa clave corta
//                             } // <-- Cierra ELSE (item final sin action)
//                            // System.out.println("  Menu/Item Creado: '" + text + "' -> AC: '" + menuItemComponent.getActionCommand() + "' (Config Key: "+fullConfigKey+")");
//                         } // <-- Cierra ELSE (no hay action o es JMenu)
//
//                     } // <-- Cierra IF (menuItemComponent != null...)
//                 } // <-- Cierra IF (!text.isEmpty...)
//
//            } // <-- Cierra IF principal menus/items (if trimmedOption.startsWith("-"))
//
//        } // <-- Cierra FOR
//
//        System.out.println("MenuBarBuilder: Menú construido. Total items en mapa: " + menuItemsPorNombre.size());
//        return menuBar;
//
//    } // <-- Cierra MÉTODO buildMenuBar
    

    /**
     * Procesa recursivamente una definición de ítem de menú y sus sub-ítems,
     * creando y configurando los componentes Swing correspondientes.
     *
     * @param itemDef La definición del ítem actual a procesar.
     * @param parentContainer El componente Swing (JMenuBar o JMenu) al que se añadirá el nuevo ítem.
     * @param parentMainMenu El JMenu principal al que pertenece este ítem (o null si está en la barra).
     * @param parentSubMenu El JMenu submenú directo al que pertenece este ítem (o null si no está en un submenú).
     */
    private void processMenuItemDefinition(MenuItemDefinition itemDef, JComponent parentContainer, JMenu parentMainMenu, JMenu parentSubMenu) {

        // --- 1. Preparación ---
        JMenuItem menuItemComponent = null; // El componente Swing que se creará
        // 1.1. Generar la clave larga de configuración para este ítem
        String fullConfigKey = generateFullConfigKey(itemDef, parentMainMenu, parentSubMenu);

        // --- 2. Creación del Componente según el Tipo ---
        switch (itemDef.tipo()) {
            case MAIN_MENU:
                // 2.1.1. Crear JMenu principal
                JMenu mainMenu = new JMenu(itemDef.textoMostrado());
                menuItemComponent = mainMenu;
                // 2.1.2. Añadir a JMenuBar
                if (parentContainer instanceof JMenuBar) {
                     ((JMenuBar) parentContainer).add(mainMenu);
                } else {
                     System.err.println("ERROR: MAIN_MENU '" + itemDef.textoMostrado() + "' no tiene JMenuBar como padre.");
                     this.menuBar.add(mainMenu); // Fallback
                }
                // 2.1.3. Establecer clave larga como ActionCommand (para identificación)
                mainMenu.setActionCommand(fullConfigKey);
                // 2.1.4. Procesar recursivamente los sub-ítems
                if (itemDef.subItems() != null) {
                    for (MenuItemDefinition subDef : itemDef.subItems()) {
                        // El contenedor padre para los hijos es este mainMenu
                        // El menú principal padre para los hijos es este mainMenu
                        // No hay submenú padre para los hijos directos de un mainMenu
                        processMenuItemDefinition(subDef, mainMenu, mainMenu, null);
                    }
                }
                break; // Fin MAIN_MENU

            case SUB_MENU:
                // 2.2.1. Crear JMenu (submenú)
                JMenu subMenu = new JMenu(itemDef.textoMostrado());
                menuItemComponent = subMenu;
                // 2.2.2. Añadir al contenedor padre (debería ser otro JMenu)
                addMenuItemToParent(subMenu, parentContainer);
                // 2.2.3. Establecer clave larga como ActionCommand
                subMenu.setActionCommand(fullConfigKey);
                // 2.2.4. Procesar recursivamente los sub-ítems
                if (itemDef.subItems() != null) {
                     for (MenuItemDefinition subDef : itemDef.subItems()) {
                         // El contenedor padre para los hijos es este subMenu
                         // El menú principal padre sigue siendo el mismo (parentMainMenu)
                         // El submenú padre para los hijos es este subMenu
                         processMenuItemDefinition(subDef, subMenu, parentMainMenu, subMenu);
                     }
                }
                break; // Fin SUB_MENU

            case ITEM:
                // 2.3.1. Crear JMenuItem estándar
                menuItemComponent = new JMenuItem(itemDef.textoMostrado());
                // 2.3.2. Añadir al contenedor padre
                addMenuItemToParent(menuItemComponent, parentContainer);
                // 2.3.3. Asignar Action o ActionCommand
                assignActionOrCommand(menuItemComponent, itemDef);
                break; // Fin ITEM

            case CHECKBOX_ITEM:
                // 2.4.1. Crear JCheckBoxMenuItem
                menuItemComponent = new JCheckBoxMenuItem(itemDef.textoMostrado());
                // 2.4.2. Añadir al contenedor padre
                addMenuItemToParent(menuItemComponent, parentContainer);
                // 2.4.3. Asignar Action o ActionCommand
                assignActionOrCommand(menuItemComponent, itemDef);
                break; // Fin CHECKBOX_ITEM

            case RADIO_BUTTON_ITEM:
                // 2.5.1. Crear JRadioButtonMenuItem
                JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem(itemDef.textoMostrado());
                menuItemComponent = radioItem;
                // 2.5.2. Añadir al ButtonGroup actual (si existe)
                if (currentButtonGroup != null) {
                    currentButtonGroup.add(radioItem);
                } else {
                    System.err.println("WARN [MenuBarBuilder]: JRadioButtonMenuItem '" + itemDef.textoMostrado() + "' sin ButtonGroup activo (falta RADIO_GROUP_START?).");
                }
                // 2.5.3. Añadir al contenedor padre
                addMenuItemToParent(menuItemComponent, parentContainer);
                // 2.5.4. Asignar Action o ActionCommand
                assignActionOrCommand(menuItemComponent, itemDef);
                break; // Fin RADIO_BUTTON_ITEM

            case SEPARATOR:
                // 2.6.1. Añadir separador si el padre es un JMenu
                if (parentContainer instanceof JMenu) {
                    ((JMenu) parentContainer).addSeparator();
                } else {
                     System.err.println("WARN [MenuBarBuilder]: SEPARATOR definido fuera de un JMenu.");
                }
                // 2.6.2. Salir, no es un componente con estado/acción
                return; // Importante salir aquí

            case RADIO_GROUP_START:
                // 2.7.1. Iniciar un nuevo ButtonGroup
                if (currentButtonGroup != null) {
                     System.err.println("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_START pero ya había un grupo activo. Se reemplazará.");
                }
                currentButtonGroup = new ButtonGroup();
                 // 2.7.2. Salir, no es un componente visible
                return; // Importante salir aquí

            case RADIO_GROUP_END:
                // 2.8.1. Finalizar el ButtonGroup actual
                if (currentButtonGroup == null) {
                    System.err.println("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_END sin un grupo activo.");
                }
                currentButtonGroup = null;
                // 2.8.2. Salir, no es un componente visible
                return; // Importante salir aquí

            default:
                // 2.9. Manejar tipos desconocidos
                System.err.println("ERROR [MenuBarBuilder]: Tipo de MenuItemDefinition no reconocido: " + itemDef.tipo());
                return; // Salir si no se reconoce
        } // Fin del switch

        // --- 3. Registro del Componente (si aplica) ---
        // 3.1. Añadir al mapa `menuItemsPorNombre` si es un componente válido con clave
        if (menuItemComponent != null && !fullConfigKey.isEmpty()) {
            this.menuItemsPorNombre.put(fullConfigKey, menuItemComponent);
        }
        // 3.2. Log de advertencia si se creó componente pero no clave (excepto separador/grupos)
        else if (menuItemComponent != null && fullConfigKey.isEmpty() && itemDef.tipo() != MenuItemType.SEPARATOR) {
            System.err.println("WARN [MenuBarBuilder]: Componente '" + itemDef.textoMostrado() + "' ("+itemDef.tipo()+") no tiene clave de configuración generada.");
        }
    }// --- FIN metodo processMenuItemDefinition   
    
    
    /**
     * Asigna la Action correspondiente del actionMap al item si existe,
     * o asigna el comandoOClave como ActionCommand si no hay Action.
     * También ajusta el texto del ítem si es necesario.
     *
     * @param item El JMenuItem a configurar.
     * @param itemDef La definición de donde obtener la información.
     */
    private void assignActionOrCommand(JMenuItem item, MenuItemDefinition itemDef) {
        // 1. Validaciones básicas
        if (item == null || itemDef == null) return;

        // 2. Obtener comando/clave de la definición
        String comandoOClave = itemDef.comandoOClave();

        // 3. Procesar si hay un comando/clave definido
        if (comandoOClave != null && !comandoOClave.isBlank()) {
            // 3.1. Intentar buscar la Action en el mapa
            Action action = this.actionMap.get(comandoOClave);

            // 3.2. Si se encontró una Action
            if (action != null) {
                // 3.2.1. Asignar la Action al componente
                item.setAction(action);
                // 3.2.2. Sobrescribir texto si el definido es diferente al de la Action
                if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank() && !itemDef.textoMostrado().equals(action.getValue(Action.NAME))) {
                    item.setText(itemDef.textoMostrado());
                }
            }
            // 3.3. Si NO se encontró Action
            else {
                // 3.3.1. Asignar el comando/clave como ActionCommand del componente
                //        (Útil para checkboxes de config UI o items sin Action funcional)
                item.setActionCommand(comandoOClave);
                // 3.3.2. Asegurarse de que el texto se muestre si no hay Action
                if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                     item.setText(itemDef.textoMostrado());
                } else {
                     // Si no hay ni Action ni texto, el texto será el que JMenuItem ponga por defecto (vacío)
                     // O podríamos poner el comando como texto por defecto para debug:
                     // item.setText(comandoOClave);
                }
            }
        }
        // 4. Si NO hay comando/clave definido en la definición
        else {
             // 4.1. Solo aseguramos que el texto se muestre (si existe)
             if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                 item.setText(itemDef.textoMostrado());
             }
             // Este sería un ítem puramente informativo sin acción.
        }
    }// --- FIN metodo assignActionOrCommand 
    
    
    /**
     * Helper para añadir un JMenuItem a su contenedor padre (JMenu o JMenuBar).
     * Incluye validación y logs de error.
     *
     * @param item El JMenuItem (o JMenu, JCheckBoxMenuItem, etc.) a añadir.
     * @param parentContainer El JComponent padre (esperado JMenu o JMenuBar).
     */
    private void addMenuItemToParent(JMenuItem item, JComponent parentContainer) {
        // 1. Validar que el item a añadir no sea null
        if (item == null) {
             System.err.println("ERROR [MenuBarBuilder]: Intentando añadir un ítem nulo.");
             return;
        }
        // 2. Validar que el contenedor padre no sea null
        if (parentContainer == null) {
             System.err.println("ERROR [MenuBarBuilder]: Intentando añadir ítem '" + item.getText() + "' sin contenedor padre válido (null).");
             return;
        }

        // 3. Añadir al contenedor según su tipo
        if (parentContainer instanceof JMenu) {
            ((JMenu) parentContainer).add(item);
        } else if (parentContainer instanceof JMenuBar) {
            // Esto es menos común, para items directamente en la barra (no dentro de un menú)
            ((JMenuBar) parentContainer).add(item);
        } else {
            // Si el contenedor no es ni JMenu ni JMenuBar, es un error de lógica
            System.err.println("ERROR [MenuBarBuilder]: Contenedor padre inesperado para ítem '" + item.getText() + "': " + parentContainer.getClass().getName());
        }
    }// --- FIN metodo addMenuItemToParent

    /**
     * Helper para generar la parte de la clave de configuración a partir de texto o comando.
     * Limpia el texto, reemplaza espacios y caracteres no válidos.
     *
     * @param text El texto (o comando/clave) base para generar la parte de la clave.
     * @return Una cadena segura para usar como parte de una clave de configuración.
     */
    private String generateKeyPart(String text) {
        // 1. Manejar caso null o vacío
        if (text == null || text.isBlank()) return "unknown_key_part"; // Devolver algo identificable

        // 2. Limpiar y normalizar
        String cleanedText = text.trim()
                   .replace(" ", "_")        // Espacios a guion bajo
                   .replace("/", "_")        // Barras a guion bajo
                   .replace("\\", "_")       // Contrabarras a guion bajo
                   .replace("*", "")         // Quitar asteriscos
                   .replace(".", "")         // Quitar puntos
                   .replace("%", "porc")     // Reemplazar %
                   .replace(":", "")         // Quitar dos puntos
                   .replace("?", "")         // Quitar interrogación
                   // Añadir más reemplazos específicos si son necesarios
                   .replaceAll("[^a-zA-Z0-9_]", ""); // Quitar todo lo que no sea letra, número o guion bajo

        // 3. Convertir a minúsculas
        cleanedText = cleanedText.toLowerCase();

        // 4. Asegurar que no quede vacío después de limpiar
        if (cleanedText.isEmpty()) {
            // Si queda vacío, generar algo basado en el hash (poco legible pero único)
             return "emptykey_" + text.hashCode();
        }

        return cleanedText;
    } // --- FIN metodo generateKeyPart

    
    /**
     * Helper para generar la clave larga de configuración jerárquica completa.
     * Construye la clave basándose en la jerarquía de menús padre.
     *
     * @param itemDef La definición del ítem actual.
     * @param parentMainMenu El JMenu principal padre (puede ser null).
     * @param parentSubMenu El JMenu submenú padre directo (puede ser null).
     * @return La clave larga de configuración (ej: "interfaz.menu.archivo.abrir_archivo")
     *         o una cadena vacía si no se debe generar clave para este tipo.
     */
    private String generateFullConfigKey(MenuItemDefinition itemDef, JMenu parentMainMenu, JMenu parentSubMenu) {
        // 1. No generar clave para tipos que no tienen estado configurable individual
        if (itemDef.tipo() == MenuItemType.SEPARATOR ||
            itemDef.tipo() == MenuItemType.RADIO_GROUP_START ||
            itemDef.tipo() == MenuItemType.RADIO_GROUP_END) {
            return ""; // Clave vacía indica no almacenar
        }

        // 2. Generar la parte final de la clave desde el texto o el comando/clave
        String keyPart;
        // Priorizar el texto mostrado si existe
        if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
            keyPart = generateKeyPart(itemDef.textoMostrado());
        }
        // Si no hay texto, intentar usar el comando/clave
        else if (itemDef.comandoOClave() != null && !itemDef.comandoOClave().isBlank()) {
             // Intentar extraer una parte legible del comando/clave
             String comando = itemDef.comandoOClave();
             // Intentar usar la parte después del último punto
             int lastDot = comando.lastIndexOf('.');
             if (lastDot != -1 && lastDot < comando.length() - 1) {
                 keyPart = generateKeyPart(comando.substring(lastDot + 1));
             } else {
                 // Si no hay punto o es el último carácter, usar el comando completo
                 keyPart = generateKeyPart(comando);
             }
             // Si después de limpiar el comando queda "unknown", usar hash como fallback
             if (keyPart.equals("unknown_key_part") || keyPart.startsWith("emptykey_")) {
                  keyPart = "cmdkey_" + comando.hashCode(); // Fallback basado en hash del comando
             }
        }
        // Si no hay ni texto ni comando, generar un identificador único (poco probable)
        else {
             keyPart = "item_" + itemDef.hashCode();
             System.err.println("WARN [MenuBarBuilder]: Generando clave fallback para item sin texto ni comando: " + keyPart);
        }


        // 3. Construir la jerarquía de la clave
        String baseKey;
        // 3.1. Determinar la base según los padres
        if (parentSubMenu != null) {
            // Dentro de un submenú: usar la clave del submenú como base
            // Asumimos que el ActionCommand del JMenu ya contiene su clave larga correcta
            baseKey = parentSubMenu.getActionCommand();
            if (baseKey == null || baseKey.isBlank()) {
                 System.err.println("ERROR [MenuBarBuilder]: Submenú padre '" + parentSubMenu.getText() + "' no tiene ActionCommand (clave larga)!");
                 // Fallback MUY básico si falla la clave del padre
                 baseKey = generateFullConfigKey(null, parentMainMenu, null) + "." + generateKeyPart(parentSubMenu.getText());
            }
        } else if (parentMainMenu != null) {
            // Dentro de un menú principal (pero no submenú): usar clave del menú principal
            baseKey = parentMainMenu.getActionCommand();
            if (baseKey == null || baseKey.isBlank()) {
                System.err.println("ERROR [MenuBarBuilder]: Menú principal padre '" + parentMainMenu.getText() + "' no tiene ActionCommand (clave larga)!");
                 baseKey = CONFIG_KEY_PREFIX + "." + generateKeyPart(parentMainMenu.getText()); // Fallback
            }
        } else if (itemDef.tipo() == MenuItemType.MAIN_MENU) {
            // Es un menú principal (nivel raíz): usar el prefijo base
            baseKey = CONFIG_KEY_PREFIX;
        } else {
            // Caso inesperado (ej. ITEM añadido directamente a JMenuBar?)
            System.err.println("WARN [MenuBarBuilder]: No se pudo determinar jerarquía clara para: " + keyPart + " (" + itemDef.tipo() + ")");
            baseKey = CONFIG_KEY_PREFIX + ".error"; // Clave de error
        }

        // 4. Combinar base y parte final
        return baseKey + "." + keyPart;
    } // FIN metodo generateFullConfigKey

    
    // --- Getter para el mapa ---
    public Map<String, JMenuItem> getMenuItemsMap() {
    	return this.menuItemsPorNombre;
    }
    
    
    /**
     * MÉTODO TEMPORAL - SOLO PARA COMPATIBILIDAD CON LA FASE INTERMEDIA DEL REFACTOR.
     * Permite inyectar el actionMap antes de llamar al método buildMenuBar() antiguo.
     * ESTE MÉTODO SERÁ ELIMINADO EN LA FASE 3.
     * @param actionMap El mapa de acciones.
     */
    @Deprecated // Marcar como obsoleto para recordar quitarlo
    void setActionMapInternalForLegacyBuild(Map<String, Action> actionMap) {
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null");
    }    
    
    
// } //FIN clase MenuBarBuilder    
    
// ******************************************************************************************************    
// ****************************** DESDE AQUI PARA ABAJO TEORICAMENTE SOBRA ******************************      
// ******************************************************************************************************    
    
//    
//    // --- Método auxiliar interno para crear JMenuItems ---
//    private JMenuItem createMenuItemInternal(String text, ButtonGroup buttonGroup) {
//        if (text.startsWith("*")) {
//            return new JCheckBoxMenuItem(text.substring(1));
//        } else if (text.startsWith(".")) {
//            JRadioButtonMenuItem radioButtonMenuItem = new JRadioButtonMenuItem(text.substring(1));
//            if (buttonGroup != null) {
//                buttonGroup.add(radioButtonMenuItem);
//            }
//            return radioButtonMenuItem;
//        } else {
//            return new JMenuItem(text);
//        }
//    }
//
//
// // Método para obtener la definición del menú como String
//    private String getMenuDefinitionString() {
//        // menu como un único String multilinea. Asegúrate de que los saltos
//        // de línea sean correctos (puedes usar \n).
//    	String parteMenuArchivo = (
//    			"- Archivo\n"+
//            	"--- Abrir Archivo\n"+
//            	"--- Abrir en ventana nueva\n"+
//            	"--- Guardar\n"+
//            	"--- Guardar Como\n"+
//            	
//            	"_\n"+
//            	
//            	"--- Abrir Con...\n"+
//            	"--- Editar Imagen\n"+
//            	"--- Imprimir\n"+
//            	"--- Compartir\n"+
//            	
//            	"_\n"+
//            	
////				"--- Abrir Ubicacion del Archivo\n"+
//            	"--- Refrescar Imagen\n"+
//            	"--- Volver a Cargar\n"+
//            	"--- Recargar Lista de Imagenes\n"+
//            	"--- Unload Imagen\n"+
//            "\n");
//    	
//    	String parteMenuNavegacion = ("- Navegacion\n"+
//	            "--- Primera Imagen\n"+
//	            "--- Imagen Aterior\n"+
//	            "--- Imagen Siguiente\n"+
//	            "--- Ultima Imagen\n"+
//	            
//	            "_\n"+
//	            
//	            "--- Ir a...\n"+
//	            "--- Buscar...\n"+
//	            //"--- Primera Imagen\n"+
//	            //"--- Ultima Imagen\n"+
//	            
//	            "_\n"+
//	            
//	            "--- Anterior Fotograma\n"+
//	            "--- Siguiente Fotograma\n"+
//	            "--- Primer Fotograma\n"+
//	            "--- Ultimo Fotograma\n"+
//            "\n");
//    	
//    	String parteMenuZoom= ("- Zoom\n"+
//	            "--- Acercar\n"+
//	            "--- Alejar\n"+
//	            "--- Zoom Personalizado %\n"+
//	            "--- Zoom Tamaño Real\n"+
//	            "---* Mantener Proporciones\n"+
//	            
//	            "_\n"+
//	            
//	            "---* Activar Zoom Manual\n"+
//	            "--- Resetear Zoom\n"+
//	            
//	            "_\n"+
//	            
//	            "--< Tipos de Zoom\n"+
//		            "--{\n"+
//		            "---. Zoom Automatico\n"+
//		            "---. Zoom a lo Ancho\n"+
//		            "---. Zoom a lo Alto\n"+
//		            "---. Escalar Para Ajustar\n"+
//		            "---. Zoom Actual Fijo\n"+
//		            "---. Zoom Especificado\n"+
//		            "---. Escalar Para Rellenar\n"+
//		            "--}\n"+
//	            "-->\n"+
//            "\n");
//    	
//    	String parteMenuImagen =(
//                "- Imagen\n"+
//    	            "--< Carga y Orden\n"+
//    			        "--{\n"+
//    			            "----. Nombre por Defecto\n"+
//    			            "----. Tamaño de Archivo\n"+
//    			            "----. Fecha de Creacion\n"+
//    			            "----. Extension\n"+
//    			        "--}\n"+
//    			            
//    		            "_\n"+
//    		            
//    			        "--{\n"+
//    			            "----. Sin Ordenar\n"+
//    			            "----. Ascendente\n"+
//    			            "----. Descendente\n"+
//    			        "--}\n"+
//    		            
//    	            "-->\n"+
//    	            "--< Edicion\n"+
//    		            "---- Girar Izquierda\n"+
//    		            "---- Girar Derecha\n"+
//    		            "---- Voltear Horizontal\n"+
//    		            "---- Voltear Vertical\n"+
//    	            "-->\n"+
//    		            
//    	            "_\n"+
//    	            
//    	            "--- Cambiar Nombre de la Imagen\n"+
//    	            "--- Mover a la Papelera\n"+
//    	            "--- Eliminar Permanentemente\n"+
//    	            
//    	            "_\n"+
//    	            
//    	            "--- Establecer Como Fondo de Escritorio\n"+
//    	            "--- Establecer Como Imagen de Bloqueo\n"+
//    	            
//    	            "_\n"+
//    	            
//					"--- Abrir Ubicacion del Archivo\n"+
//    	            "--- Propiedades de la imagen\n"+
//                "\n");
//    	
//    	String parteMenuVista =(
//    			"- Vista\n"+
//        	            "---* Barra de Menu\n"+
//        	            "---* Barra de Botones\n"+
//        	            "---* Mostrar/Ocultar la Lista de Archivos\n"+
//        	            "---* Imagenes en Miniatura\n"+
//        	            "---* Linea de Ubicacion del Archivo\n"+
//        	            
//        	            "_\n"+
//        	            
//        	            "---* Fondo a Cuadros\n"+
//        	            "---* Mantener Ventana Siempre Encima\n"+
//        	            
//        	            "_\n"+
//        	            
//        	            "--- Mostrar Dialogo Lista de Imagenes\n"+
//                    "\n");
//    	
//    	String parteMenuGeneral = (
//    			"- Configuracion\n"+
//        	            "--< Carga de Imagenes\n"+
//                    
//        		            "--{\n"+
//        		            "---. Mostrar Solo Carpeta Actual\n"+
//        		            "---. Mostrar Imagenes de Subcarpetas\n"+
//        		            "--}\n"+
//        		            
//        		            "_\n"+
//        		        
//        		            "---- Miniaturas en la Barra de Imagenes\n"+
//        		        "-->\n"+
//        		            
//        	            "_\n"+
//        	            
//        	            "--< General\n"+
//        		            "---* Mostrar Imagen de Bienvenida\n"+
//        		            "---* Abrir Ultima Imagen Vista\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Volver a la Primera Imagen al Llegar al final de la Lista\n"+
//        		            "---* Mostrar Flechas de Navegacion\n"+
//        	            "-->\n"+
//        		            
//        	            "_\n"+
//        	            
//        	            "--< Visualizar Botones\n"+
//        		            "---* Botón Rotar Izquierda\n"+
//        		            "---* Botón Rotar Derecha\n"+
//        		            "---* Botón Espejo Horizontal\n"+
//        		            "---* Botón Espejo Vertical\n"+
//        		            "---* Botón Recortar\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Botón Zoom\n"+
//        		            "---* Botón Zoom Automatico\n"+
//        		            "---* Botón Ajustar al Ancho\n"+
//        		            "---* Botón Ajustar al Alto\n"+
//        		            "---* Botón Escalar para Ajustar\n"+
//        		            "---* Botón Zoom Fijo\n"+
//        		            "---* Botón Reset Zoom\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Botón Panel-Galeria\n"+
//        		            "---* Botón Grid\n"+
//        		            "---* Botón Pantalla Completa\n"+
//        		            "---* Botón Lista\n"+
//        		            "---* Botón Carrousel\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Botón Refrescar\n"+
//        		            "---* Botón Subcarpetas\n"+
//        		            "---* Botón Lista de Favoritos\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Botón Borrar\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Botón Menu\n"+
//        		            "---* Mostrar Boton de Botones Ocultos\n"+
//        		        "-->\n"+
//        		            
//        	            "_\n"+
//        	            
//        	            "--< Barra de Informacion\n"+
//        		            "--{\n"+
//        		            "---. Nombre del Archivo\n"+
//        		            "---. Ruta y Nombre del Archivo\n"+
//        		            "--}\n"+
//        		            
//        		            "_\n"+
//        		            
//        		            "---* Numero de Imagenes en la Carpeta Actual\n"+
//        		            "---* % de Zoom actual\n"+
//        		            "---* Tamaño del Archivo\n"+
//        		            "---* Fecha y Hora de la Imagen\n"+
//        	            "-->\n"+
//        		            
//        		        "--< Tema\n"+
//        	            	"--{\n"+
//        	            	"---. Tema Clear\n"+
//        	            	"---. Tema Dark\n"+
//        	            	"---. Tema Blue\n"+
//        	            	"---. Tema Orange\n"+
//        	            	"---. Tema Green\n"+
//        	            	"--}\n"+
//        	            "-->\n"+
//        	            	
//        	            "_\n"+
//        	            
//        	            "--- Guardar Configuracion Actual\n"+
//        	            "--- Cargar Configuracion Inicial\n"+
//        	            "_\n"+
//        	            "--- Version");
//    	
//    	return
//    			parteMenuArchivo+ "\n" +
//    			parteMenuNavegacion+ "\n" +
//    			parteMenuZoom+ "\n" +
//    			parteMenuImagen+ "\n" +
//    			parteMenuVista+ "\n" +
//    			parteMenuGeneral;
//    }
} // <-- Cierra CLASE MenuBarBuilder

