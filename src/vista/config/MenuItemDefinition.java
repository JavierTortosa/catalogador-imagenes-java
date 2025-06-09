package vista.config;

import java.util.List;
import java.util.Objects;

/**
 * Un 'record' inmutable que representa la definición de un solo ítem en un menú.
 * Contiene toda la información necesaria para que un 'Builder' (como MenuBarBuilder)
 * pueda construir el componente Swing correspondiente (JMenuItem, JMenu, JCheckBoxMenuItem, etc.).
 *
 * @param command         Para ítems accionables, este es el comando canónico (de AppActionCommands)
 *                        o una clave de configuración única. Para menús contenedores es null.
 * @param tipo            El tipo de ítem de menú, definido por el enum {@link MenuItemType}.
 * @param textoMostrado   El texto visible que se mostrará en el menú.
 * @param subItems        Una lista de MenuItemDefinition para los sub-ítems de este menú.
 */
public record MenuItemDefinition(
    String actionCommand, // Mantenemos tu nombre original 'command'
    MenuItemType tipo,
    String textoMostrado,
    List<MenuItemDefinition> subItems
) {
    /**
     * Constructor compacto para realizar validaciones.
     */
    public MenuItemDefinition {
        Objects.requireNonNull(tipo, "El tipo (MenuItemType) no puede ser nulo.");

        if (textoMostrado == null &&
            tipo != MenuItemType.SEPARATOR &&
            tipo != MenuItemType.RADIO_GROUP_START &&
            tipo != MenuItemType.RADIO_GROUP_END) {
            throw new IllegalArgumentException("El textoMostrado no puede ser nulo para el tipo: " + tipo);
        }

        // --- ¡ESTA ES LA ÚNICA LÍNEA QUE REALMENTE NECESITÁBAMOS CAMBIAR! ---
        if (subItems != null && !subItems.isEmpty() &&
            tipo != MenuItemType.MAIN_MENU &&
            tipo != MenuItemType.SUB_MENU &&
            tipo != MenuItemType.CHECKBOX_ITEM_WITH_SUBMENU) { // Añadimos la excepción

            throw new IllegalArgumentException(
                "La lista de subItems debe ser nula o vacía para el tipo de ítem '" + tipo + "'."
            );
        }
        // ----------------------------------------------------------------------
    }
    
    // Dejamos el constructor de conveniencia, pero usando tus nombres de campo
    public MenuItemDefinition(String command, MenuItemType type, String textoMostrado) {
        this(command, type, textoMostrado, null);
    }

    // Este getter es para mantener la compatibilidad con el código que te di para MenuBarBuilder
    // que usa "label" en lugar de "textoMostrado". Es un pequeño parche.
    public String label() {
        return this.textoMostrado;
    }
}

//package vista.config;
//
//import java.util.List;
//
//// Usamos nullability explícita en comentarios para claridad
//public record MenuItemDefinition(
//    /** Comando Canónico (de AppActionCommands) o Clave de Config (para control UI). Null si no aplica (SEPARATOR, GROUP_START/END, Menú contenedor sin acción directa). */
//    String actionCommand,
//    /** Tipo de elemento del menú. */
//    MenuItemType tipo,
//    /** Texto a mostrar al usuario. Null o vacío para SEPARATOR, GROUP_START/END. */
//    String textoMostrado,
//    /** Lista de sub-items para MAIN_MENU o SUB_MENU. Null o vacía para los demás. */
//    List<MenuItemDefinition> subItems
//) {
//	
//    // Constructor compacto para validación (opcional pero recomendado)
//    public MenuItemDefinition {
//        if (tipo == null) throw new IllegalArgumentException("El tipo no puede ser nulo");
//        if ((tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU || tipo == MenuItemType.ITEM || tipo == MenuItemType.CHECKBOX_ITEM || tipo == MenuItemType.RADIO_BUTTON_ITEM) && (textoMostrado == null || textoMostrado.isBlank())) {
//           // Permitir texto vacío si el comandoOClave no es nulo (la Action podría poner el texto)
//           if (actionCommand == null || actionCommand.isBlank()) {
//        	   throw new IllegalArgumentException("Texto mostrado no puede ser vacío para el tipo: " + tipo + " si no hay comando/clave");
//           }
//        }
//        if ((tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU) && subItems == null) {
//        	// Permitir subItems nulos si se prefiere a lista vacía
//            // throw new IllegalArgumentException("subItems no puede ser nulo para MAIN_MENU o SUB_MENU");
//        }
//        if (!(tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU) && subItems != null && !subItems.isEmpty()) {
//            throw new IllegalArgumentException("subItems debe ser nulo o vacío para tipos diferentes de MAIN_MENU o SUB_MENU");
//        }
//    }
//}