package vista.config;

import java.util.List;

// Usamos nullability explícita en comentarios para claridad
public record MenuItemDefinition(
    /** Comando Canónico (de AppActionCommands) o Clave de Config (para control UI). Null si no aplica (SEPARATOR, GROUP_START/END, Menú contenedor sin acción directa). */
    String comandoOClave,
    /** Tipo de elemento del menú. */
    MenuItemType tipo,
    /** Texto a mostrar al usuario. Null o vacío para SEPARATOR, GROUP_START/END. */
    String textoMostrado,
    /** Lista de sub-items para MAIN_MENU o SUB_MENU. Null o vacía para los demás. */
    List<MenuItemDefinition> subItems
) {
    // Constructor compacto para validación (opcional pero recomendado)
    public MenuItemDefinition {
        if (tipo == null) throw new IllegalArgumentException("El tipo no puede ser nulo");
        if ((tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU || tipo == MenuItemType.ITEM || tipo == MenuItemType.CHECKBOX_ITEM || tipo == MenuItemType.RADIO_BUTTON_ITEM) && (textoMostrado == null || textoMostrado.isBlank())) {
           // Permitir texto vacío si el comandoOClave no es nulo (la Action podría poner el texto)
           if (comandoOClave == null || comandoOClave.isBlank()) {
        	   throw new IllegalArgumentException("Texto mostrado no puede ser vacío para el tipo: " + tipo + " si no hay comando/clave");
           }
        }
        if ((tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU) && subItems == null) {
        	// Permitir subItems nulos si se prefiere a lista vacía
            // throw new IllegalArgumentException("subItems no puede ser nulo para MAIN_MENU o SUB_MENU");
        }
        if (!(tipo == MenuItemType.MAIN_MENU || tipo == MenuItemType.SUB_MENU) && subItems != null && !subItems.isEmpty()) {
            throw new IllegalArgumentException("subItems debe ser nulo o vacío para tipos diferentes de MAIN_MENU o SUB_MENU");
        }
    }
}