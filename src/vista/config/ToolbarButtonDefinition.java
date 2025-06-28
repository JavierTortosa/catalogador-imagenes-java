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
    String categoriaLayout,
    
    /** El tipo de botón a crear (NORMAL o TOGGLE). */
    ButtonType tipoBoton 
) {
	
    // Constructor principal (canónico) con validación
    public ToolbarButtonDefinition {
        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
        Objects.requireNonNull(claveIcono, "claveIcono no puede ser nula");
        Objects.requireNonNull(categoriaLayout, "categoriaLayout no puede ser nula");
        Objects.requireNonNull(tipoBoton, "tipoBoton no puede ser nulo");
    }

    // --- Constructor sobrecargado para retrocompatibilidad ---
    /**
     * Constructor para botones normales. Por defecto, el tipo será `ButtonType.NORMAL`.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout) {
        // Llama al constructor principal, asignando ButtonType.NORMAL por defecto.
        this(comandoCanonico, claveIcono, textoTooltip, categoriaLayout, ButtonType.NORMAL);
    }
    
} // --- FIN del record ToolbarButtonDefinition ---

