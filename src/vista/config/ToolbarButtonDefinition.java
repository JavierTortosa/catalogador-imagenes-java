package vista.config;

import java.util.Objects;

public record ToolbarButtonDefinition(
    /** Comando Canónico (de AppActionCommands) que ejecuta este botón. */
    String comandoCanonico,
    /** Clave o nombre del archivo de icono (ej: "1001-Primera_48x48.png"). */
    String claveIcono,
    /** Texto que aparecerá como tooltip. */
    String textoTooltip,
    /** Categoría lógica para agrupación y layout (ej: "movimiento", "edicion"). */
    String categoriaLayout
) {
    // Constructor compacto para validación
    public ToolbarButtonDefinition {
        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
        Objects.requireNonNull(claveIcono, "claveIcono no puede ser nula"); // Requerimos icono para botones
        // textoTooltip puede ser null o vacío
        Objects.requireNonNull(categoriaLayout, "categoriaLayout no puede ser nula");
    }
}