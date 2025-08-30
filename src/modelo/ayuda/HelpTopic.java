package modelo.ayuda;

/**
 * Un registro inmutable que contiene la información extraída de una definición de UI
 * para ser usada en la generación de la ayuda.
 *
 * @param command El comando canónico (ej. AppActionCommands.CMD_NAV_SIGUIENTE).
 * @param description El texto de ayuda (tooltip o nombre del menú).
 * @param iconName El nombre del archivo del icono (puede ser null).
 * @param category La categoría a la que pertenece (ej. "Toolbar: Navegación").
 */
public record HelpTopic(
    String command,
    String description,
    String iconName,
    String category
) {}// --- FIN de record HelpTopic ---