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
    
    private String menuDefinition;
    
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

    
    public MenuBarBuilder(Map<String, Action> actionMap) {
    	menuDefinition = getMenuDefinitionString();
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
                                 fullConfigKey = parentKey + "." + baseActionCommand;//.toLowerCase();
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

 // Método para obtener la definición del menú como String
    private String getMenuDefinitionString() {
        // menu como un único String multilinea. Asegúrate de que los saltos
        // de línea sean correctos (puedes usar \n).
    	String parteMenuArchivo = (
    			"- Archivo\n"+
            	"--- Abrir Archivo\n"+
            	"--- Abrir en ventana nueva\n"+
            	"--- Guardar\n"+
            	"--- Guardar Como\n"+
            	
            	"_\n"+
            	
            	"--- Abrir Con...\n"+
            	"--- Editar Imagen\n"+
            	"--- Imprimir\n"+
            	"--- Compartir\n"+
            	
            	"_\n"+
            	
//				"--- Abrir Ubicacion del Archivo\n"+
            	"--- Refrescar Imagen\n"+
            	"--- Volver a Cargar\n"+
            	"--- Recargar Lista de Imagenes\n"+
            	"--- Unload Imagen\n"+
            "\n");
    	
    	String parteMenuNavegacion = ("- Navegacion\n"+
	            "--- Primera Imagen\n"+
	            "--- Imagen Aterior\n"+
	            "--- Imagen Siguiente\n"+
	            "--- Ultima Imagen\n"+
	            
	            "_\n"+
	            
	            "--- Ir a...\n"+
	            "--- Buscar...\n"+
	            //"--- Primera Imagen\n"+
	            //"--- Ultima Imagen\n"+
	            
	            "_\n"+
	            
	            "--- Anterior Fotograma\n"+
	            "--- Siguiente Fotograma\n"+
	            "--- Primer Fotograma\n"+
	            "--- Ultimo Fotograma\n"+
            "\n");
    	
    	String parteMenuZoom= ("- Zoom\n"+
	            "--- Acercar\n"+
	            "--- Alejar\n"+
	            "--- Zoom Personalizado %\n"+
	            "--- Zoom Tamaño Real\n"+
	            "---* Mantener Proporciones\n"+
	            
	            "_\n"+
	            
	            "---* Activar Zoom Manual\n"+
	            "--- Resetear Zoom\n"+
	            
	            "_\n"+
	            
	            "--< Tipos de Zoom\n"+
		            "--{\n"+
		            "---. Zoom Automatico\n"+
		            "---. Zoom a lo Ancho\n"+
		            "---. Zoom a lo Alto\n"+
		            "---. Escalar Para Ajustar\n"+
		            "---. Zoom Actual Fijo\n"+
		            "---. Zoom Especificado\n"+
		            "---. Escalar Para Rellenar\n"+
		            "--}\n"+
	            "-->\n"+
            "\n");
    	
    	String parteMenuImagen =(
                "- Imagen\n"+
    	            "--< Carga y Orden\n"+
    			        "--{\n"+
    			            "----. Nombre por Defecto\n"+
    			            "----. Tamaño de Archivo\n"+
    			            "----. Fecha de Creacion\n"+
    			            "----. Extension\n"+
    			        "--}\n"+
    			            
    		            "_\n"+
    		            
    			        "--{\n"+
    			            "----. Sin Ordenar\n"+
    			            "----. Ascendente\n"+
    			            "----. Descendente\n"+
    			        "--}\n"+
    		            
    	            "-->\n"+
    	            "--< Edicion\n"+
    		            "---- Girar Izquierda\n"+
    		            "---- Girar Derecha\n"+
    		            "---- Voltear Horizontal\n"+
    		            "---- Voltear Vertical\n"+
    	            "-->\n"+
    		            
    	            "_\n"+
    	            
    	            "--- Cambiar Nombre de la Imagen\n"+
    	            "--- Mover a la Papelera\n"+
    	            "--- Eliminar Permanentemente\n"+
    	            
    	            "_\n"+
    	            
    	            "--- Establecer Como Fondo de Escritorio\n"+
    	            "--- Establecer Como Imagen de Bloqueo\n"+
    	            
    	            "_\n"+
    	            
					"--- Abrir Ubicacion del Archivo\n"+
    	            "--- Propiedades de la imagen\n"+
                "\n");
    	
    	String parteMenuVista =(
    			"- Vista\n"+
        	            "---* Barra de Menu\n"+
        	            "---* Barra de Botones\n"+
        	            "---* Mostrar/Ocultar la Lista de Archivos\n"+
        	            "---* Imagenes en Miniatura\n"+
        	            "---* Linea de Ubicacion del Archivo\n"+
        	            
        	            "_\n"+
        	            
        	            "---* Fondo a Cuadros\n"+
        	            "---* Mantener Ventana Siempre Encima\n"+
        	            
        	            "_\n"+
        	            
        	            "--- Mostrar Dialogo Lista de Imagenes\n"+
                    "\n");
    	
    	String parteMenuGeneral = (
    			"- Configuracion\n"+
        	            "--< Carga de Imagenes\n"+
                    
        		            "--{\n"+
        		            "---. Mostrar Solo Carpeta Actual\n"+
        		            "---. Mostrar Imagenes de Subcarpetas\n"+
        		            "--}\n"+
        		            
        		            "_\n"+
        		        
        		            "---- Miniaturas en la Barra de Imagenes\n"+
        		        "-->\n"+
        		            
        	            "_\n"+
        	            
        	            "--< General\n"+
        		            "---* Mostrar Imagen de Bienvenida\n"+
        		            "---* Abrir Ultima Imagen Vista\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Volver a la Primera Imagen al Llegar al final de la Lista\n"+
        		            "---* Mostrar Flechas de Navegacion\n"+
        	            "-->\n"+
        		            
        	            "_\n"+
        	            
        	            "--< Visualizar Botones\n"+
        		            "---* Botón Rotar Izquierda\n"+
        		            "---* Botón Rotar Derecha\n"+
        		            "---* Botón Espejo Horizontal\n"+
        		            "---* Botón Espejo Vertical\n"+
        		            "---* Botón Recortar\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Botón Zoom\n"+
        		            "---* Botón Zoom Automatico\n"+
        		            "---* Botón Ajustar al Ancho\n"+
        		            "---* Botón Ajustar al Alto\n"+
        		            "---* Botón Escalar para Ajustar\n"+
        		            "---* Botón Zoom Fijo\n"+
        		            "---* Botón Reset Zoom\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Botón Panel-Galeria\n"+
        		            "---* Botón Grid\n"+
        		            "---* Botón Pantalla Completa\n"+
        		            "---* Botón Lista\n"+
        		            "---* Botón Carrousel\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Botón Refrescar\n"+
        		            "---* Botón Subcarpetas\n"+
        		            "---* Botón Lista de Favoritos\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Botón Borrar\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Botón Menu\n"+
        		            "---* Mostrar Boton de Botones Ocultos\n"+
        		        "-->\n"+
        		            
        	            "_\n"+
        	            
        	            "--< Barra de Informacion\n"+
        		            "--{\n"+
        		            "---. Nombre del Archivo\n"+
        		            "---. Ruta y Nombre del Archivo\n"+
        		            "--}\n"+
        		            
        		            "_\n"+
        		            
        		            "---* Numero de Imagenes en la Carpeta Actual\n"+
        		            "---* % de Zoom actual\n"+
        		            "---* Tamaño del Archivo\n"+
        		            "---* Fecha y Hora de la Imagen\n"+
        	            "-->\n"+
        		            
        		        "--< Tema\n"+
        	            	"--{\n"+
        	            	"---. Tema Clear\n"+
        	            	"---. Tema Dark\n"+
        	            	"---. Tema Blue\n"+
        	            	"---. Tema Orange\n"+
        	            	"---. Tema Green\n"+
        	            	"--}\n"+
        	            "-->\n"+
        	            	
        	            "_\n"+
        	            
        	            "--- Guardar Configuracion Actual\n"+
        	            "--- Cargar Configuracion Inicial\n"+
        	            "_\n"+
        	            "--- Version");
    	
    	return
    			parteMenuArchivo+ "\n" +
    			parteMenuNavegacion+ "\n" +
    			parteMenuZoom+ "\n" +
    			parteMenuImagen+ "\n" +
    			parteMenuVista+ "\n" +
    			parteMenuGeneral;
    }
} // <-- Cierra CLASE MenuBarBuilder

