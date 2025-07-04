package vista.builders;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import controlador.VisorController;
import controlador.actions.config.ToggleToolbarButtonVisibilityAction;
import controlador.actions.config.ToggleUIElementVisibilityAction;
import controlador.commands.AppActionCommands; // Asumo que lo necesitas para algún log o comparación, aunque no directamente aquí
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.config.MenuItemDefinition;
import vista.config.MenuItemType;


public class MenuBarBuilder {

    // --- Resultados del Builder ---
    private JMenuBar menuBar;
    private Map<String, JMenuItem> menuItemsPorNombre; // Mapa Clave Larga -> Item

    // --- Estado Interno del Builder ---
    private Map<String, Action> actionMap; // Mapa Comando CORTO (AppActionCommands) -> Action
    private ButtonGroup currentButtonGroup = null;
    private ActionListener controllerGlobalActionListener; // Para ítems sin Action propia

    // --- Prefijo base para las claves de configuración del menú
    private final String CONFIG_KEY_PREFIX = "interfaz.menu";
    
    // --- Clases internas
    private final ConfigurationManager configuration;
    private final VisorController controllerRef;
    private final IViewManager viewManager;
    private final ComponentRegistry registry;

    /**
     * Constructor simplificado. Inicializa las estructuras internas.
     */
    public MenuBarBuilder(VisorController controller, ConfigurationManager config, IViewManager viewManager, ComponentRegistry registry) {
        this.menuItemsPorNombre = new HashMap<>();
        this.menuBar = new JMenuBar();
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.configuration = Objects.requireNonNull(config, "ConfigurationManager no puede ser null");
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en MenuBarBuilder");
        // actionMap y controllerGlobalActionListener se recibirán/establecerán externamente.
        // currentButtonGroup se inicializa a null y se gestiona durante la construcción.
    } // --- Fin del método MenuBarBuilder (constructor) ---

    /**
     * Establece el ActionListener global que se usará para los JMenuItems
     * que no tengan una Action específica asociada. Típicamente, este será
     * el VisorController.
     *
     * @param listener El ActionListener a utilizar.
     */
    public void setControllerGlobalActionListener(ActionListener listener) {
        this.controllerGlobalActionListener = listener;
        if (listener != null) {
            System.out.println("[MenuBarBuilder] ControllerGlobalActionListener establecido: " + listener.getClass().getName());
        } else {
            System.out.println("[MenuBarBuilder] ADVERTENCIA: ControllerGlobalActionListener establecido a null.");
        }
    }

    /**
     * Construye la barra de menú completa basada en la estructura de definiciones explícita.
     *
     * @param menuStructure La lista de {@link MenuItemDefinition} que define la jerarquía
     *                      y los ítems del menú.
     * @param actionMap     El mapa que asocia los comandos canónicos (Strings de
     *                      {@link AppActionCommands}) con sus instancias de {@link Action}.
     *                      Este mapa es proporcionado por el VisorController o AppInitializer.
     * @return La {@link JMenuBar} construida.
     */
    public JMenuBar buildMenuBar(List<MenuItemDefinition> menuStructure, Map<String, Action> actionMap) {

        // --- SECCIÓN 1: INICIALIZACIÓN Y VALIDACIÓN DEL MÉTODO ---
        // 1.1. Reiniciar el estado interno del builder.
        //      Esto permite que la misma instancia de MenuBarBuilder pueda ser reutilizada
        //      (aunque típicamente se crea una nueva cada vez).
        this.menuBar = new JMenuBar();
        this.menuItemsPorNombre = new HashMap<>();
        this.currentButtonGroup = null; // Resetear el ButtonGroup actual

        // 1.2. Validar y almacenar el actionMap proporcionado.
        //      Este mapa es crucial para vincular JMenuItems a sus funcionalidades.
        this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null en MenuBarBuilder.buildMenuBar");

        // 1.3. Log de inicio del proceso de construcción.
        System.out.println("--- MenuBarBuilder: Iniciando construcción de JMenuBar ---");
        System.out.println("  [MenuBarBuilder] ActionMap recibido con " + (this.actionMap != null ? this.actionMap.size() : "null") + " acciones.");
        System.out.println("  [MenuBarBuilder] controllerGlobalActionListener: " +
                           (this.controllerGlobalActionListener != null ? "PRESENTE (" + this.controllerGlobalActionListener.getClass().getSimpleName() + ")" : "AUSENTE"));

        // 1.4. Validar la estructura de menú de entrada.
        //      Si no hay definiciones, no se puede construir nada.
        if (menuStructure == null || menuStructure.isEmpty()) {
            System.err.println("WARN [MenuBarBuilder]: La estructura del menú (menuStructure) está vacía o es nula. Se devolverá una JMenuBar vacía.");
            return this.menuBar; // Devolver una barra de menú vacía.
        }

        // --- SECCIÓN 2: PROCESAMIENTO DE LOS ELEMENTOS RAÍZ DEL MENÚ ---
        // Los elementos raíz de la `menuStructure` deben ser de tipo `MenuItemType.MAIN_MENU`.
        // Estos corresponden a los menús principales que se añaden directamente a la `JMenuBar`.
        System.out.println("  [MenuBarBuilder] Procesando elementos raíz de la estructura del menú...");
        for (MenuItemDefinition itemDef : menuStructure) {
            // 2.1. Verificar que el tipo del elemento raíz sea MAIN_MENU.
            if (itemDef.tipo() == MenuItemType.MAIN_MENU) {
                // 2.2. Llamar al método recursivo `processMenuItemDefinition` para construir
                //      este menú principal y todos sus sub-ítems.
                //      - `itemDef`: La definición del menú principal actual.
                //      - `menuBar`: El contenedor padre (la JMenuBar).
                //      - `null` para parentMainMenu: Porque este es un menú principal, no tiene un JMenu padre.
                //      - `null` para parentSubMenu: Idem.
                processMenuItemDefinition(itemDef, this.menuBar, null, null);
            } else {
                // 2.3. Log de error si se encuentra un tipo inesperado en el nivel raíz.
                System.err.println("WARN [MenuBarBuilder]: Se encontró un tipo de ítem inesperado (" + itemDef.tipo() +
                                   ") en el nivel superior de la estructura del menú. Se esperaba MAIN_MENU. " +
                                   "Texto del ítem: '" + itemDef.textoMostrado() + "'. Este ítem será ignorado en el nivel raíz.");
            }
        }

        // --- SECCIÓN 3: FINALIZACIÓN Y RETORNO ---
        // 3.1. Log de finalización del proceso.
        System.out.println("--- MenuBarBuilder: Construcción de JMenuBar completada. ---");
        System.out.println("  [MenuBarBuilder] Total de JMenuItems mapeados por clave de configuración: " + this.menuItemsPorNombre.size());

        // 3.2. Devolver la JMenuBar completamente construida.
        
        // Usamos una clave estándar para la barra de menú principal.
        this.registry.register("menubar.main", this.menuBar);
        System.out.println("  [MenuBarBuilder] JMenuBar registrada en ComponentRegistry con la clave 'menubar.main'.");

        System.out.println("--- MenuBarBuilder: Construcción de JMenuBar completada. ---");
        return this.menuBar;
        
    } // --- FIN del método buildMenuBar ---


    // (El resto de los métodos: processMenuItemDefinition, assignActionOrCommand,
    //  addMenuItemToParent, generateKeyPart, generateFullConfigKey, getMenuItemsMap
    //  permanecerían como te los mostré en la respuesta anterior, con la adición
    //  de la lógica para usar `this.controllerGlobalActionListener` en `assignActionOrCommand`)
    // Por completitud, te los incluyo aquí de nuevo con los comentarios de sección.

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

        // --- 1. PREPARACIÓN Y GENERACIÓN DE CLAVE ---
        JMenuItem menuItemComponent = null; // El componente Swing que se creará.
        // 1.1. Generar la clave larga de configuración para este ítem.
        //      Esta clave se usa para el mapa `menuItemsPorNombre` y potencialmente para logs.
        String fullConfigKey = generateFullConfigKey(itemDef, parentContainer); 
        // System.out.println("  [ProcessDef] Procesando: " + itemDef.textoMostrado() + " (Tipo: " + itemDef.tipo() + ", Clave: " + fullConfigKey + ")"); // Log detallado opcional

        // --- 2. CREACIÓN DEL COMPONENTE SWING SEGÚN MenuItemType ---
        switch (itemDef.tipo()) {
            case MAIN_MENU:
                // 2.1.1. Crear un JMenu para un menú principal.
                JMenu mainMenu = new JMenu(itemDef.textoMostrado());
                menuItemComponent = mainMenu; // Guardar referencia para registro.
                // 2.1.2. Añadir este JMenu a su contenedor padre (debería ser la JMenuBar).
                addMenuItemToParent(mainMenu, parentContainer); // Usa el helper para añadir y validar.
                // 2.1.3. Establecer la clave de configuración larga como ActionCommand.
                //        Esto es útil si se necesita identificar el JMenu fuente de un evento,
                //        aunque los JMenu generalmente no disparan ActionEvents directos por clic.
                mainMenu.setActionCommand(fullConfigKey);
                // 2.1.4. Procesar recursivamente los sub-ítems de este menú principal.
                if (itemDef.subItems() != null && !itemDef.subItems().isEmpty()) {
                    for (MenuItemDefinition subDef : itemDef.subItems()) {
                        processMenuItemDefinition(subDef, mainMenu, mainMenu, null);
                    }
                }
                break; // Fin del caso MAIN_MENU

            case SUB_MENU:
                JMenu menuToCreate;
                String actionCommand = itemDef.actionCommand();

                if (actionCommand != null && actionCommand.startsWith("interfaz.herramientas.")) {
                    
                    // Creamos la Action al vuelo para este menú.
                    Action toggleAction = new ToggleUIElementVisibilityAction(
                        this.viewManager,
                        this.configuration,
                        itemDef.textoMostrado(),
                        actionCommand, // La clave de config a modificar (...visible)
                        actionCommand.split("\\.")[2], // El ID de la UI (ej: "edicion")
                        actionCommand // El ActionCommand
                    );
                    
                    // Creamos nuestra clase JCheckBoxMenu personalizada.
                    menuToCreate = new JCheckBoxMenu(toggleAction);

                } else {
                    // Si no, es un JMenu normal.
                    menuToCreate = new JMenu(itemDef.textoMostrado());
                }
                
                menuItemComponent = menuToCreate;
                addMenuItemToParent(menuToCreate, parentContainer);
                menuToCreate.setActionCommand(fullConfigKey);
                
                // Procesamos los sub-ítems recursivamente.
                if (itemDef.subItems() != null && !itemDef.subItems().isEmpty()) {
                    for (MenuItemDefinition subDef : itemDef.subItems()) {
                        processMenuItemDefinition(subDef, menuToCreate, parentMainMenu, menuToCreate);
                    }
                }
                break;
                
            case ITEM:
                // 2.3.1. Crear un JMenuItem estándar.
                menuItemComponent = new JMenuItem(); // Texto se asignará en assignActionOrCommand
                // 2.3.2. Añadir al contenedor padre.
                addMenuItemToParent(menuItemComponent, parentContainer);
                // 2.3.3. Asignar la Action correspondiente o configurar el ActionCommand y el listener.
                assignActionOrCommand(menuItemComponent, itemDef);
                break; // Fin del caso ITEM

            case CHECKBOX_ITEM:
                JCheckBoxMenuItem checkboxItem = new JCheckBoxMenuItem();
                menuItemComponent = checkboxItem;
                String comandoOClave = itemDef.actionCommand();
                Action actionPredefinida = (actionMap != null) ? actionMap.get(comandoOClave) : null;

                if (actionPredefinida != null) {
                    checkboxItem.setAction(actionPredefinida);
                } 
                else if (comandoOClave != null && comandoOClave.startsWith("interfaz.boton.")) {
                    // --- INICIO DEL CAMBIO ---
                    // comandoOClave es la clave BASE del botón: "interfaz.boton.edicion.imagen_rotar_izq"
                    
                    String buttonKeyBase = comandoOClave;
                    String menuKeyBase = generateFullConfigKey(itemDef, parentContainer);
                    String toolbarId = buttonKeyBase.split("\\.")[2];

                    // Se crea la Action pasándole las CLAVES BASE.
                    // La Action se encargará de añadir los sufijos ".visible" y ".seleccionado" internamente.
                    Action toggleAction = new ToggleToolbarButtonVisibilityAction(
                        itemDef.textoMostrado(),
                        this.configuration,
                        this.viewManager,
                        menuKeyBase,      // Clave base del menú
                        buttonKeyBase,    // Clave base del botón
                        toolbarId
                    );
                    //checkboxItem.setSelected(true);
                    checkboxItem.setAction(toggleAction);
                    // --- FIN DEL CAMBIO ---
                    
                } else {
                    assignActionOrCommand(checkboxItem, itemDef);
                }
                addMenuItemToParent(checkboxItem, parentContainer);
                break;
                
            case CHECKBOX_ITEM_WITH_SUBMENU:
                // --- INICIO DEL CAMBIO ---
                // El actionCommand que llega de UIDefinitionService AHORA es la clave completa con ".visible"
                String toolbarVisibilityKey = itemDef.actionCommand();
                
                // Extraemos la clave BASE y el uiIdentifier de la clave completa
                String toolbarKeyBase = toolbarVisibilityKey.replace(".visible", "");
                String[] parts = toolbarKeyBase.split("\\.");
                String uiIdentifier = parts[parts.length - 1];
                
                // Se crea la Action pasándole la CLAVE BASE
                Action toggleBarAction = new ToggleUIElementVisibilityAction(
                    this.viewManager,
                    this.configuration,
                    itemDef.textoMostrado(),
                    toolbarKeyBase, // <<< Se pasa la clave BASE
                    uiIdentifier,
                    toolbarVisibilityKey // El action command puede seguir siendo la clave completa
                );
                // --- FIN DEL CAMBIO ---

                JCheckBoxMenu menuConCheckbox = new JCheckBoxMenu(toggleBarAction);
                menuItemComponent = menuConCheckbox;
                addMenuItemToParent(menuConCheckbox, parentContainer);
                menuConCheckbox.setActionCommand(fullConfigKey);
                
                if (itemDef.subItems() != null && !itemDef.subItems().isEmpty()) {
                    for (MenuItemDefinition subDef : itemDef.subItems()) {
                        processMenuItemDefinition(subDef, menuConCheckbox, parentMainMenu, menuConCheckbox);
                    }
                }
                break;
                
            case RADIO_BUTTON_ITEM:
                // 2.5.1. Crear un JRadioButtonMenuItem.
                JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem(); // Texto y estado se asignarán
                menuItemComponent = radioItem;
                // 2.5.2. Añadir al ButtonGroup actual (si está activo).
                //        Esto asegura la exclusividad mutua entre los radios del grupo.
                if (currentButtonGroup != null) {
                    currentButtonGroup.add(radioItem);
                } else {
                    System.err.println("WARN [MenuBarBuilder]: JRadioButtonMenuItem '" + itemDef.textoMostrado() +
                                       "' creado sin un ButtonGroup activo. ¿Falta un RADIO_GROUP_START en la definición?");
                }
                // 2.5.3. Añadir al contenedor padre.
                addMenuItemToParent(radioItem, parentContainer);
                // 2.5.4. Asignar la Action o configurar ActionCommand/listener.
                assignActionOrCommand(radioItem, itemDef);
                break; // Fin del caso RADIO_BUTTON_ITEM

            case SEPARATOR:
                // 2.6.1. Añadir un separador visual al menú padre.
                //        Solo tiene sentido si el padre es un JMenu (no una JMenuBar).
                if (parentContainer instanceof JMenu) {
                    ((JMenu) parentContainer).addSeparator();
                } else {
                    System.err.println("WARN [MenuBarBuilder]: SEPARATOR definido fuera de un JMenu. Contenedor padre: " +
                                       (parentContainer != null ? parentContainer.getClass().getName() : "null"));
                }
                // 2.6.2. No se crea un `menuItemComponent` ni se registra. Salir.
                return; // Salir del método para SEPARATOR

            case RADIO_GROUP_START:
                // 2.7.1. Iniciar un nuevo ButtonGroup.
                //        Si ya existía uno, se reemplaza (podría indicar un error en la definición del menú).
                if (currentButtonGroup != null) {
                    System.err.println("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_START pero ya había un ButtonGroup activo. Se reemplazará el grupo anterior.");
                }
                currentButtonGroup = new ButtonGroup();
                // System.out.println("  [ProcessDef] ButtonGroup INICIADO."); // Log opcional
                // 2.7.2. No es un componente visible. Salir.
                return; // Salir del método para RADIO_GROUP_START

            case RADIO_GROUP_END:
                // 2.8.1. Finalizar (anular) el ButtonGroup actual.
                if (currentButtonGroup == null) {
                    System.err.println("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_END sin un ButtonGroup activo. ¿Definición de menú incorrecta?");
                }
                currentButtonGroup = null;
                // System.out.println("  [ProcessDef] ButtonGroup FINALIZADO."); // Log opcional
                // 2.8.2. No es un componente visible. Salir.
                return; // Salir del método para RADIO_GROUP_END

            
            default:
                // 2.9. Manejar tipos de ítem desconocidos o no soportados.
                System.err.println("ERROR [MenuBarBuilder]: Tipo de MenuItemDefinition no reconocido o no manejado: " +
                                   itemDef.tipo() + " para el ítem con texto: '" + itemDef.textoMostrado() + "'. Ítem ignorado.");
                return; // Salir si el tipo no se reconoce
        } // Fin del switch sobre itemDef.tipo()

        // --- 3. REGISTRO DEL COMPONENTE CREADO (SI APLICA) ---
        // 3.1. Si se creó un componente JMenuItem y se generó una clave de configuración válida,
        //      añadirlo al mapa `menuItemsPorNombre`.
        //      Esto permite acceder al JMenuItem desde fuera del builder (ej. VisorController)
        //      para modificar su estado si es necesario.
        if (menuItemComponent != null && fullConfigKey != null && !fullConfigKey.isEmpty()) {
            // Antes de añadir, verificar si la clave ya existe (no debería si las claves son únicas)
            if (this.menuItemsPorNombre.containsKey(fullConfigKey)) {
                System.err.println("WARN [MenuBarBuilder]: Clave de configuración duplicada encontrada: '" + fullConfigKey +
                                   "'. El ítem anterior será sobrescrito en el mapa. Item nuevo: " + menuItemComponent.getText());
            }
            this.menuItemsPorNombre.put(fullConfigKey, menuItemComponent);
        }
        // 3.2. Log de advertencia si se creó un componente pero no se pudo generar una clave.
        //      Esto es poco probable si `generateFullConfigKey` está bien, pero es una comprobación de seguridad.
        else if (menuItemComponent != null && (fullConfigKey == null || fullConfigKey.isEmpty())) {
            // No imprimir para tipos que intencionalmente no tienen clave (como JMenu que usa su ActionCommand)
            // Los tipos SEPARATOR, RADIO_GROUP_START, RADIO_GROUP_END ya retornan antes.
            if (!(menuItemComponent instanceof JMenu)) { // JMenu usa su ActionCommand como clave
                 System.err.println("WARN [MenuBarBuilder]: JMenuItem creado '" + menuItemComponent.getText() +
                                   "' (" + itemDef.tipo() + ") no tiene una clave de configuración generada y no será mapeado.");
            }
        }
    } // --- FIN del método processMenuItemDefinition ---

    
    /**
     * Asigna la Action correspondiente del actionMap al JMenuItem si existe una para su comando/clave,
     * o asigna el comando/clave como ActionCommand y (si está configurado) el listener global
     * si no hay una Action específica. También ajusta el texto del ítem.
     *
     * @param item El JMenuItem (puede ser JCheckBoxMenuItem, JRadioButtonMenuItem) a configurar.
     * @param itemDef La {@link MenuItemDefinition} de donde obtener la información.
     */
    private void assignActionOrCommand(JMenuItem item, MenuItemDefinition itemDef) {
        // --- 1. VALIDACIONES BÁSICAS ---
        if (item == null) {
            System.err.println("ERROR [assignActionOrCommand]: El JMenuItem es null.");
            return;
        }
        if (itemDef == null) {
            System.err.println("ERROR [assignActionOrCommand]: La MenuItemDefinition es null para el item: " + item.getText());
            // Intentar poner el texto si el itemDef es null pero el item no lo es (caso raro)
            if (item.getText() == null || item.getText().isEmpty()) item.setText("(Definición Nula)");
            return;
        }

        // --- 2. OBTENER COMANDO/CLAVE DE LA DEFINICIÓN ---
        String comandoOClave = itemDef.actionCommand();

        // --- 3. PROCESAR SI HAY UN COMANDO O CLAVE DEFINIDO EN MenuItemDefinition ---
        if (comandoOClave != null && !comandoOClave.isBlank()) {
            // 3.1. Intentar buscar una Action en el `this.actionMap` usando el `comandoOClave`.
            Action action = (this.actionMap != null) ? this.actionMap.get(comandoOClave) : null;

            // 3.2. CASO: SE ENCONTRÓ UNA ACTION EN EL MAPA
            if (action != null) {
                // 3.2.1. Asignar la Action al JMenuItem.
                item.setAction(action);
                
                // --- INICIO DE LA MODIFICACIÓN ---
                // Forzar la asignación del icono, ya que JMenuItem a veces no lo toma al vuelo de la Action.
                // Verificamos si la Action tiene un valor para SMALL_ICON y si es una instancia de Icon.
                if (action.getValue(Action.SMALL_ICON) instanceof javax.swing.Icon) {
                    item.setIcon((javax.swing.Icon) action.getValue(Action.SMALL_ICON));
                }
                // --- FIN DE LA MODIFICACIÓN ---

                // 3.2.2. Sobrescribir el texto del JMenuItem si `itemDef.textoMostrado()` es diferente
                //        al nombre de la Action (Action.NAME).
                if (itemDef.textoMostrado() != null &&
                    !itemDef.textoMostrado().isBlank() &&
                    !Objects.equals(itemDef.textoMostrado(), action.getValue(Action.NAME))) {
                    item.setText(itemDef.textoMostrado());
                }
            }
            // 3.3. CASO: NO SE ENCONTRÓ UNA ACTION EN EL MAPA PARA ESTE COMANDO/CLAVE
            else {
                // 3.3.1. Establecer el `comandoOClave` como el `ActionCommand` del JMenuItem.
                item.setActionCommand(comandoOClave);

                // 3.3.2. Establecer el texto del JMenuItem desde `itemDef.textoMostrado()`.
                if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                    item.setText(itemDef.textoMostrado());
                } else {
                    item.setText(comandoOClave);
                }

                // 3.3.3. Añadir el ActionListener global (VisorController) a este JMenuItem.
                if (this.controllerGlobalActionListener != null && !(item instanceof JMenu)) {
                    item.addActionListener(this.controllerGlobalActionListener);
                } else if (this.controllerGlobalActionListener == null && !(item instanceof JMenu)) {
                    System.err.println("WARN [AssignActCmd]: controllerGlobalActionListener ES NULL. No se pudo añadir listener a JMenuItem: " +
                                       item.getText() + " (Comando: " + comandoOClave + "). Este ítem no funcionará.");
                }
            }
        }
        // --- 4. CASO: NO HAY COMANDO O CLAVE DEFINIDO EN MenuItemDefinition ---
        else {
            // 4.1. Si no hay `comandoOClave`, solo se puede establecer el texto.
            if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                item.setText(itemDef.textoMostrado());
            } else {
                item.setText("(Ítem sin texto ni comando)"); // Placeholder
            }
        }
    } // --- Fin del método assignActionOrCommand ---
    

//    /**
//     * Asigna la Action correspondiente del actionMap al JMenuItem si existe una para su comando/clave,
//     * o asigna el comando/clave como ActionCommand y (si está configurado) el listener global
//     * si no hay una Action específica. También ajusta el texto del ítem.
//     *
//     * @param item El JMenuItem (puede ser JCheckBoxMenuItem, JRadioButtonMenuItem) a configurar.
//     * @param itemDef La {@link MenuItemDefinition} de donde obtener la información.
//     */
//    private void assignActionOrCommand(JMenuItem item, MenuItemDefinition itemDef) {
//        // --- 1. VALIDACIONES BÁSICAS ---
//        if (item == null) {
//            System.err.println("ERROR [assignActionOrCommand]: El JMenuItem es null.");
//            return;
//        }
//        if (itemDef == null) {
//            System.err.println("ERROR [assignActionOrCommand]: La MenuItemDefinition es null para el item: " + item.getText());
//            // Intentar poner el texto si el itemDef es null pero el item no lo es (caso raro)
//            if (item.getText() == null || item.getText().isEmpty()) item.setText("(Definición Nula)");
//            return;
//        }
//
//        // --- 2. OBTENER COMANDO/CLAVE DE LA DEFINICIÓN ---
//        String comandoOClave = itemDef.actionCommand();
//
//        // --- 3. PROCESAR SI HAY UN COMANDO O CLAVE DEFINIDO EN MenuItemDefinition ---
//        if (comandoOClave != null && !comandoOClave.isBlank()) {
//            // 3.1. Intentar buscar una Action en el `this.actionMap` usando el `comandoOClave`.
//            Action action = (this.actionMap != null) ? this.actionMap.get(comandoOClave) : null;
//
//            // 3.2. CASO: SE ENCONTRÓ UNA ACTION EN EL MAPA
//            if (action != null) {
//                // 3.2.1. Asignar la Action al JMenuItem.
//                //        Esto transfiere propiedades como nombre, icono, tooltip, estado enabled,
//                //        y lo más importante, el ActionListener. Para JCheckBoxMenuItem y
//                //        JRadioButtonMenuItem, también vincula su estado 'selected' a
//                //        la propiedad Action.SELECTED_KEY de la Action.
//                item.setAction(action);
//                // System.out.println("  [AssignActCmd] Action '" + action.getValue(Action.NAME) + "' asignada a JMenuItem para comando: " + comandoOClave + ". Texto menu: " + itemDef.textoMostrado());
//
//                // 3.2.2. Sobrescribir el texto del JMenuItem si `itemDef.textoMostrado()` es diferente
//                //        al nombre de la Action (Action.NAME). Esto permite tener un texto en el menú
//                //        diferente al nombre interno de la Action.
//                if (itemDef.textoMostrado() != null &&
//                    !itemDef.textoMostrado().isBlank() &&
//                    !Objects.equals(itemDef.textoMostrado(), action.getValue(Action.NAME))) {
//                    item.setText(itemDef.textoMostrado());
//                    // System.out.println("    -> Texto del JMenuItem sobrescrito a: '" + itemDef.textoMostrado() + "'");
//                }
//                // El JMenuItem ya tiene su ActionListener (el de la Action). No añadir el global.
//            }
//            // 3.3. CASO: NO SE ENCONTRÓ UNA ACTION EN EL MAPA PARA ESTE COMANDO/CLAVE
//            else {
//                // 3.3.1. Establecer el `comandoOClave` como el `ActionCommand` del JMenuItem.
//                //        Esto es crucial para que el ActionListener global (VisorController) pueda
//                //        identificar qué ítem fue presionado usando `event.getActionCommand()`.
//                item.setActionCommand(comandoOClave);
//                // System.out.println("  [AssignActCmd] No se encontró Action para '" + comandoOClave + "'. Estableciendo ActionCommand. Texto menu: " + itemDef.textoMostrado());
//
//                // 3.3.2. Establecer el texto del JMenuItem desde `itemDef.textoMostrado()`.
//                //        Si `textoMostrado` es nulo o vacío, el JMenuItem podría quedar sin texto visible.
//                if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
//                    item.setText(itemDef.textoMostrado());
//                } else {
//                    // Fallback: si no hay texto definido, usar el comando como texto (para depuración)
//                    // o dejarlo vacío si se prefiere.
//                    item.setText(comandoOClave); // O item.setText("");
//                    System.out.println("    -> Texto del JMenuItem establecido al comandoOClave (fallback): '" + comandoOClave + "'");
//                }
//
//                // 3.3.3. Añadir el ActionListener global (VisorController) a este JMenuItem.
//                //        Esto solo se hace si el ítem es "final" (no un JMenu contenedor)
//                //        y si el `controllerGlobalActionListener` ha sido establecido.
//                if (this.controllerGlobalActionListener != null && !(item instanceof JMenu)) {
//                    item.addActionListener(this.controllerGlobalActionListener);
//                    // System.out.println("    -> controllerGlobalActionListener AÑADIDO a JMenuItem: " + item.getText() + " (Comando: " + comandoOClave + ")");
//                } else if (this.controllerGlobalActionListener == null && !(item instanceof JMenu)) {
//                    System.err.println("WARN [AssignActCmd]: controllerGlobalActionListener ES NULL. No se pudo añadir listener a JMenuItem: " +
//                                       item.getText() + " (Comando: " + comandoOClave + "). Este ítem no funcionará.");
//                }
//            }
//        }
//        // --- 4. CASO: NO HAY COMANDO O CLAVE DEFINIDO EN MenuItemDefinition ---
//        else {
//            // 4.1. Si no hay `comandoOClave`, solo se puede establecer el texto.
//            //      Este sería un JMenuItem puramente informativo o decorativo sin funcionalidad directa.
//            if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
//                item.setText(itemDef.textoMostrado());
//            } else {
//                item.setText("(Ítem sin texto ni comando)"); // Placeholder
//            }
//            // System.out.println("  [AssignActCmd] JMenuItem '" + item.getText() + "' sin comandoOClave definido. Solo se estableció el texto.");
//            // Opcional: Añadir el listener global aquí también si se espera que ítems sin comando hagan algo.
//            // if (this.controllerGlobalActionListener != null && !(item instanceof JMenu) && itemDef.tipo() != MenuItemType.SUB_MENU) {
//            //    item.addActionListener(this.controllerGlobalActionListener);
//            // }
//        }
//    } // --- FIN del método assignActionOrCommand ---


    /**
     * Método de utilidad para añadir un JMenuItem (o sus subclases como JMenu, JCheckBoxMenuItem)
     * a su componente contenedor padre (JMenu o JMenuBar).
     * Incluye validaciones para evitar NullPointerExceptions y logs de error.
     *
     * @param item El JMenuItem a añadir. No debe ser null.
     * @param parentContainer El JComponent padre. Se espera que sea una instancia de JMenu o JMenuBar.
     *                        No debe ser null.
     */
    private void addMenuItemToParent(JMenuItem item, JComponent parentContainer) {
        // --- 1. VALIDACIÓN DE ENTRADAS ---
        // 1.1. Verificar que el ítem a añadir no sea null.
        if (item == null) {
            System.err.println("ERROR [addMenuItemToParent]: Se intentó añadir un JMenuItem nulo. Operación cancelada.");
            return;
        }
        // 1.2. Verificar que el contenedor padre no sea null.
        if (parentContainer == null) {
            System.err.println("ERROR [addMenuItemToParent]: Se intentó añadir JMenuItem '" + item.getText() +
                               "' a un contenedor padre nulo. Operación cancelada.");
            return;
        }

        // --- 2. AÑADIR EL ITEM AL CONTENEDOR SEGÚN EL TIPO DE CONTENEDOR ---
        // 2.1. Si el padre es un JMenu, usar su método `add(JMenuItem)`.
        if (parentContainer instanceof JMenu) {
            ((JMenu) parentContainer).add(item);
        }
        // 2.2. Si el padre es una JMenuBar, usar su método `add(JMenu)`.
        //      Esto es para los menús principales (File, Edit, etc.).
        //      Un JMenuItem individual raramente se añade directamente a una JMenuBar,
        //      pero se incluye por si acaso (aunque JMenu también es un JMenuItem).
        else if (parentContainer instanceof JMenuBar) {
            if (item instanceof JMenu) { // Solo añadir JMenu directamente a JMenuBar
                ((JMenuBar) parentContainer).add((JMenu)item);
            } else {
                System.err.println("WARN [addMenuItemToParent]: Se intentó añadir un JMenuItem que no es JMenu ('" +
                                   item.getText() + "') directamente a una JMenuBar. Esto es inusual.");
                // Podríamos decidir añadirlo de todas formas, o no. Por ahora, lo permitimos.
                // ((JMenuBar) parentContainer).add(item); // Si quieres permitirlo
            }
        }
        // 2.3. Si el contenedor padre no es ni JMenu ni JMenuBar, es un error de lógica
        //      en cómo se está llamando a este método.
        else {
            System.err.println("ERROR [addMenuItemToParent]: Contenedor padre de tipo inesperado para JMenuItem '" +
                               item.getText() + "'. Tipo de padre: " + parentContainer.getClass().getName() +
                               ". Se esperaba JMenu o JMenuBar.");
        }
    } // --- FIN del método addMenuItemToParent ---


    


    /**
     * Construye la clave de configuración jerárquica para un ítem de menú.
     * Sube por el árbol de componentes para asegurar una jerarquía correcta y
     * sigue el esquema definido "interfaz.menu.[...]"
     *
     * @param itemDef La definición del ítem actual.
     * @param parentContainer El JComponent padre directo (JMenu, JMenuBar, o JPopupMenu).
     * @return La clave de configuración canónica para el ítem.
     */
    private String generateFullConfigKey(MenuItemDefinition itemDef, JComponent parentContainer) {
        // Los tipos que no generan clave se ignoran, esto es correcto.
        if (itemDef.tipo() == MenuItemType.SEPARATOR ||
            itemDef.tipo() == MenuItemType.RADIO_GROUP_START ||
            itemDef.tipo() == MenuItemType.RADIO_GROUP_END) {
            return "";
        }

        // 1. Normalizar el nombre del ítem actual. Esta es la parte final de la clave.
        String itemNamePart = ConfigKeys.normalizePart(itemDef.textoMostrado());
        if ("unknown".equals(itemNamePart)) {
            // Si no hay texto, es un error de definición. Generamos una clave de error.
            return ConfigKeys.menu("error", "item_sin_texto_" + Math.abs(itemDef.hashCode()));
        }
        
        // 2. Construir la jerarquía de forma recursiva hacia arriba.
        List<String> hierarchyParts = new ArrayList<>();
        hierarchyParts.add(itemNamePart); // Añadir el nombre del hijo primero

        Component currentParent = parentContainer;
        while (currentParent != null) {
            // Si el padre es un JMenu, añadimos su texto al principio de la jerarquía.
            if (currentParent instanceof JMenu) {
                hierarchyParts.add(0, ConfigKeys.normalizePart(((JMenu) currentParent).getText()));
            }
            
            // Navegamos hacia el siguiente padre en la jerarquía de Swing.
            // Un JMenu está dentro de un JPopupMenu, cuyo "invocador" es el JMenu padre.
            if (currentParent.getParent() instanceof JPopupMenu) {
                currentParent = ((JPopupMenu) currentParent.getParent()).getInvoker();
            } else {
                // Si no, simplemente tomamos el padre directo.
                currentParent = currentParent.getParent();
            }
            
            // Paramos cuando llegamos a la JMenuBar o nos quedamos sin padres.
            if (currentParent instanceof JMenuBar) {
                break;
            }
        }

        // 3. Usar el KeyGenerator (ahora dentro de ConfigKeys) para construir la clave final.
        return ConfigKeys.menu(hierarchyParts.toArray(new String[0]));
        
    } // --- FIN del metodo generateFullConfigKey ---
    
    
    /**
     * Devuelve el mapa de JMenuItems construidos, donde la clave es la
     * clave de configuración larga y jerárquica del ítem.
     *
     * @return Un mapa (posiblemente inmutable o una copia) de los JMenuItems.
     */
    public Map<String, JMenuItem> getMenuItemsMap() {
        // Es una buena práctica devolver una copia o una vista inmutable
        // si se quiere proteger el mapa interno del builder de modificaciones externas.
        // return Collections.unmodifiableMap(new HashMap<>(this.menuItemsPorNombre));
        return this.menuItemsPorNombre; // O devolver la referencia directa si se confía en el uso.
    }
} // --- FIN de la clase MenuBarBuilder ---


