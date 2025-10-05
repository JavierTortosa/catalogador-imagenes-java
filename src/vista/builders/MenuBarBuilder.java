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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import vista.theme.ThemeManager;


public class MenuBarBuilder {
	
	private static final Logger logger = LoggerFactory.getLogger(MenuBarBuilder.class);

    // --- Resultados del Builder ---
    private JMenuBar menuBar;
    private Map<String, JMenuItem> menuItemsPorNombre; // Mapa Clave Larga -> Item

    // --- Estado Interno del Builder ---
    private Map<String, Action> actionMap; // Mapa Comando CORTO (AppActionCommands) -> Action
    private ButtonGroup currentButtonGroup = null;
    private ActionListener controllerGlobalActionListener; // Para ítems sin Action propia

    // --- Clases internas
    private final ConfigurationManager configuration;
    private final VisorController controllerRef;
    private final IViewManager viewManager;
    private final ComponentRegistry registry;
    
    private final ThemeManager themeManager;

    /**
     * Constructor simplificado. Inicializa las estructuras internas.
     */
    public MenuBarBuilder(
    		VisorController controller, 
    		ConfigurationManager config, 
    		IViewManager viewManager, 
    		ComponentRegistry registry,
    		ThemeManager themeManager
    		) {
    	
    	logger.info ("[MenuBarBuilder] Iniciando...");
    			
        this.menuItemsPorNombre = new HashMap<>();
        this.menuBar = new JMenuBar();
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.configuration = Objects.requireNonNull(config, "ConfigurationManager no puede ser null");
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null en MenuBarBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        
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
            logger.debug("[MenuBarBuilder] ControllerGlobalActionListener establecido: " + listener.getClass().getName());
        } else {
            logger.debug("[MenuBarBuilder] ADVERTENCIA: ControllerGlobalActionListener establecido a null.");
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
        logger.debug("--- MenuBarBuilder: Iniciando construcción de JMenuBar ---");
        logger.debug("  [MenuBarBuilder] ActionMap recibido con " + (this.actionMap != null ? this.actionMap.size() : "null") + " acciones.");
        logger.debug("  [MenuBarBuilder] controllerGlobalActionListener: " +
                           (this.controllerGlobalActionListener != null ? "PRESENTE (" + this.controllerGlobalActionListener.getClass().getSimpleName() + ")" : "AUSENTE"));

        // 1.4. Validar la estructura de menú de entrada.
        //      Si no hay definiciones, no se puede construir nada.
        if (menuStructure == null || menuStructure.isEmpty()) {
            logger.warn("WARN [MenuBarBuilder]: La estructura del menú (menuStructure) está vacía o es nula. Se devolverá una JMenuBar vacía.");
            return this.menuBar; // Devolver una barra de menú vacía.
        }

        // --- SECCIÓN 2: PROCESAMIENTO DE LOS ELEMENTOS RAÍZ DEL MENÚ ---
        // Los elementos raíz de la `menuStructure` deben ser de tipo `MenuItemType.MAIN_MENU`.
        // Estos corresponden a los menús principales que se añaden directamente a la `JMenuBar`.
        logger.debug("  [MenuBarBuilder] Procesando elementos raíz de la estructura del menú...");
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
                logger.warn("WARN [MenuBarBuilder]: Se encontró un tipo de ítem inesperado (" + itemDef.tipo() +
                                   ") en el nivel superior de la estructura del menú. Se esperaba MAIN_MENU. " +
                                   "Texto del ítem: '" + itemDef.textoMostrado() + "'. Este ítem será ignorado en el nivel raíz.");
            }
        }

        // --- SECCIÓN 3: FINALIZACIÓN Y RETORNO ---
        // 3.1. Log de finalización del proceso.
        logger.debug("--- MenuBarBuilder: Construcción de JMenuBar completada. ---");
        logger.debug("  [MenuBarBuilder] Total de JMenuItems mapeados por clave de configuración: " + this.menuItemsPorNombre.size());

        // 3.2. Devolver la JMenuBar completamente construida.
        
        // Usamos una clave estándar para la barra de menú principal.
        this.registry.register("menubar.main", this.menuBar);
        logger.debug("  [MenuBarBuilder] JMenuBar registrada en ComponentRegistry con la clave 'menubar.main'.");

        logger.debug("--- MenuBarBuilder: Construcción de JMenuBar completada. ---");
        return this.menuBar;
        
    } // --- FIN del método buildMenuBar ---


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
        // logger.debug("  [ProcessDef] Procesando: " + itemDef.textoMostrado() + " (Tipo: " + itemDef.tipo() + ", Clave: " + fullConfigKey + ")"); // Log detallado opcional

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
                    logger.warn("WARN [MenuBarBuilder]: JRadioButtonMenuItem '" + itemDef.textoMostrado() +
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
                    logger.warn("WARN [MenuBarBuilder]: SEPARATOR definido fuera de un JMenu. Contenedor padre: " +
                                       (parentContainer != null ? parentContainer.getClass().getName() : "null"));
                }
                // 2.6.2. No se crea un `menuItemComponent` ni se registra. Salir.
                return; // Salir del método para SEPARATOR

            case RADIO_GROUP_START:
                // 2.7.1. Iniciar un nuevo ButtonGroup.
                //        Si ya existía uno, se reemplaza (podría indicar un error en la definición del menú).
                if (currentButtonGroup != null) {
                    logger.warn("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_START pero ya había un ButtonGroup activo. Se reemplazará el grupo anterior.");
                }
                currentButtonGroup = new ButtonGroup();
                // logger.debug("  [ProcessDef] ButtonGroup INICIADO."); // Log opcional
                // 2.7.2. No es un componente visible. Salir.
                return; // Salir del método para RADIO_GROUP_START

            case RADIO_GROUP_END:
                // 2.8.1. Finalizar (anular) el ButtonGroup actual.
                if (currentButtonGroup == null) {
                    logger.warn("WARN [MenuBarBuilder]: Se encontró RADIO_GROUP_END sin un ButtonGroup activo. ¿Definición de menú incorrecta?");
                }
                currentButtonGroup = null;
                // logger.debug("  [ProcessDef] ButtonGroup FINALIZADO."); // Log opcional
                // 2.8.2. No es un componente visible. Salir.
                return; // Salir del método para RADIO_GROUP_END

            case PLACEHOLDER:
                if ("placeholder.temas".equals(itemDef.actionCommand())) {
                    buildDynamicThemeMenu(parentContainer);
                }
                return;
                
            default:
                // 2.9. Manejar tipos de ítem desconocidos o no soportados.
                logger.error("ERROR [MenuBarBuilder]: Tipo de MenuItemDefinition no reconocido o no manejado: " +
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
                logger.warn("WARN [MenuBarBuilder]: Clave de configuración duplicada encontrada: '" + fullConfigKey +
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
                 logger.warn("WARN [MenuBarBuilder]: JMenuItem creado '" + menuItemComponent.getText() +
                                   "' (" + itemDef.tipo() + ") no tiene una clave de configuración generada y no será mapeado.");
            }
        }
    } // --- FIN del método processMenuItemDefinition ---

    
    /**
     * Construye dinámicamente el contenido del submenú de temas, organizándolos
     * en sub-submenús por categoría.
     * @param parentContainer El JMenu "Tema" donde se añadirán los ítems.
     */
    private void buildDynamicThemeMenu(JComponent parentContainer) {
        if (!(parentContainer instanceof JMenu)) {
            logger.error("Error: El placeholder de temas debe estar dentro de un JMenu.");
            return;
        }

        JMenu temaMenu = (JMenu) parentContainer;
        ButtonGroup themeGroup = new ButtonGroup();

        // Pedimos al ThemeManager la lista completa de temas
        Map<String, ThemeManager.ThemeInfo> todosLosTemas = this.themeManager.getAvailableThemes();
        
        // Agrupamos los temas por categoría
        Map<ThemeManager.ThemeCategory, List<Map.Entry<String, ThemeManager.ThemeInfo>>> temasAgrupados = 
            todosLosTemas.entrySet().stream()
                         .collect(java.util.stream.Collectors.groupingBy(entry -> entry.getValue().category()));

        logger.debug("[MenuBarBuilder] Construyendo menú dinámico con {} temas en {} categorías.", todosLosTemas.size(), temasAgrupados.size());

        // --- ORDEN DE LAS CATEGORÍAS EN EL MENÚ ---
        ThemeManager.ThemeCategory[] categoryOrder = {
            ThemeManager.ThemeCategory.LIGHT,
            ThemeManager.ThemeCategory.DARK,
            ThemeManager.ThemeCategory.GRADIENT,
            ThemeManager.ThemeCategory.CUSTOM_INTERNAL,
            ThemeManager.ThemeCategory.CUSTOM
        };

        boolean firstCategory = true;
        for (ThemeManager.ThemeCategory category : categoryOrder) {
            List<Map.Entry<String, ThemeManager.ThemeInfo>> temasEnCategoria = temasAgrupados.get(category);

            if (temasEnCategoria != null && !temasEnCategoria.isEmpty()) {
                // Añadir separador antes de cada categoría, excepto la primera
                if (!firstCategory) {
                    temaMenu.addSeparator();
                }
                firstCategory = false;

                // Crear el submenú para la categoría
                JMenu categorySubMenu = new JMenu(category.getDisplayName());
                temaMenu.add(categorySubMenu);

                // Añadir los temas a este submenú
                for (Map.Entry<String, ThemeManager.ThemeInfo> entry : temasEnCategoria) {
                    String id = entry.getKey();
                    ThemeManager.ThemeInfo temaInfo = entry.getValue();
                    
                    String commandKey = "cmd.tema." + id;
                    MenuItemDefinition temaDef = new MenuItemDefinition(commandKey, MenuItemType.RADIO_BUTTON_ITEM, temaInfo.nombreDisplay(), null);
                    
                    JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem();
                    themeGroup.add(radioItem);
                    categorySubMenu.add(radioItem);
                    
                    assignActionOrCommand(radioItem, temaDef);
                    
                    String fullConfigKey = generateFullConfigKey(temaDef, categorySubMenu);
                    if (fullConfigKey != null && !fullConfigKey.isEmpty()) {
                        this.menuItemsPorNombre.put(fullConfigKey, radioItem);
                    }
                }
            }
        }
    } // --- FIN del metodo buildDynamicThemeMenu ---
    
    
    /**
     * Asigna la Action correspondiente del actionMap al JMenuItem si existe una para su comando/clave,
     * o asigna el comando/clave como ActionCommand y (si está configurado) el listener global
     * si no hay una Action específica. También ajusta el texto del ítem.
     *
     * @param item El JMenuItem (puede ser JCheckBoxMenuItem, JRadioButtonMenuItem) a configurar.
     * @param itemDef La {@link MenuItemDefinition} de donde obtener la información.
     */
    private void assignActionOrCommand(JMenuItem item, MenuItemDefinition itemDef) {
        if (item == null || itemDef == null) {
            logger.error("ERROR [assignActionOrCommand]: JMenuItem o MenuItemDefinition es null.");
            return;
        }

        String comandoOClave = itemDef.actionCommand();

        if (comandoOClave != null && !comandoOClave.isBlank()) {
            Action action = (this.actionMap != null) ? this.actionMap.get(comandoOClave) : null;

            // --- INICIO DE LA LÓGICA MEJORADA ---
            if (action != null) {
                // Si encontramos una Action pre-creada, la usamos directamente.
                item.setAction(action);
                
                // Forzamos el texto del menú por si es diferente al de la Action.
                if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                    item.setText(itemDef.textoMostrado());
                }
            } else {
                // Si NO hay Action, volvemos al comportamiento original.
                item.setActionCommand(comandoOClave);
                item.setText(itemDef.textoMostrado());
                if (this.controllerGlobalActionListener != null) {
                    item.addActionListener(this.controllerGlobalActionListener);
                }
            }
            // --- FIN DE LA LÓGICA MEJORADA ---

        } else {
            if (itemDef.textoMostrado() != null && !itemDef.textoMostrado().isBlank()) {
                item.setText(itemDef.textoMostrado());
            } else {
                item.setText("(Ítem sin texto ni comando)");
            }
        }
    } // --- Fin del método assignActionOrCommand ---
    

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
            logger.error("ERROR [addMenuItemToParent]: Se intentó añadir un JMenuItem nulo. Operación cancelada.");
            return;
        }
        // 1.2. Verificar que el contenedor padre no sea null.
        if (parentContainer == null) {
            logger.error("ERROR [addMenuItemToParent]: Se intentó añadir JMenuItem '" + item.getText() +
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
                logger.warn("WARN [addMenuItemToParent]: Se intentó añadir un JMenuItem que no es JMenu ('" +
                                   item.getText() + "') directamente a una JMenuBar. Esto es inusual.");
                // Podríamos decidir añadirlo de todas formas, o no. Por ahora, lo permitimos.
                // ((JMenuBar) parentContainer).add(item); // Si quieres permitirlo
            }
        }
        // 2.3. Si el contenedor padre no es ni JMenu ni JMenuBar, es un error de lógica
        //      en cómo se está llamando a este método.
        else {
            logger.error("ERROR [addMenuItemToParent]: Contenedor padre de tipo inesperado para JMenuItem '" +
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


