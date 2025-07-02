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
    
    /** El tipo de botón a crear (NORMAL, TOGGLE, DPAD, COLOR_OVERLAY_ICON_BUTTON, CHECKERED_OVERLAY_ICON_BUTTON). */
    ButtonType tipoBoton,

    // --- INICIO DE LA MODIFICACIÓN ---
    /**
     * Clave opcional para indicar cómo se debe superponer un color o patrón al icono base.
     * Para COLOR_OVERLAY_ICON_BUTTON: Una clave que ThemeManager puede usar para obtener un Color.
     * Para CHECKERED_OVERLAY_ICON_BUTTON: Una clave especial como "checkered" para indicar el patrón.
     * Para otros tipos de botón: Debería ser null.
     */
    String customOverlayKey
    // --- FIN DE LA MODIFICACIÓN ---
) {
	
    // Constructor principal (canónico) con validación
    public ToolbarButtonDefinition {
        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
        Objects.requireNonNull(claveIcono, "claveIcono no puede ser nula");
        Objects.requireNonNull(categoriaLayout, "categoriaLayout no puede ser nula");
        Objects.requireNonNull(tipoBoton, "tipoBoton no puede ser nulo");
        // customOverlayKey puede ser null, por lo que NO se valida con Objects.requireNonNull
    }

    // --- Constructor sobrecargado para retrocompatibilidad y botones NORMAL/TOGGLE/DPAD ---
    /**
     * Constructor para botones que no requieren una clave de superposición personalizada (color/patrón).
     * Se usa para ButtonType.NORMAL, ButtonType.TOGGLE, y ButtonType.DPAD.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout, ButtonType tipoBoton) {
        // Llama al constructor principal, asignando customOverlayKey a null por defecto.
        this(comandoCanonico, claveIcono, textoTooltip, categoriaLayout, tipoBoton, null);
    }

    // --- Constructor sobrecargado para retrocompatibilidad para botones NORMAL por defecto ---
    /**
     * Constructor para botones normales. Por defecto, el tipo será `ButtonType.NORMAL`
     * y no tendrá clave de superposición.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout) {
        // Llama al constructor que acepta el tipo de botón, asignando ButtonType.NORMAL y null para customOverlayKey.
        this(comandoCanonico, claveIcono, textoTooltip, categoriaLayout, ButtonType.NORMAL, null);
    }
    
} // --- FIN del record ToolbarButtonDefinition ---

//package vista.config;
//
//import java.util.Objects;
//
//public record ToolbarButtonDefinition(
//    /** Comando Canónico (de AppActionCommands) que ejecuta este botón. */
//    String comandoCanonico,
//    
//    /** Clave o nombre del archivo de icono (ej: "1001-Primera_48x48.png"). */
//    String claveIcono,
//    
//    /** Texto que aparecerá como tooltip. */
//    String textoTooltip,
//    
//    /** Categoría lógica para agrupación y layout (ej: "movimiento", "edicion"). */
//    String categoriaLayout,
//    
//    /** El tipo de botón a crear (NORMAL o TOGGLE). */
//    ButtonType tipoBoton 
//) {
//	
//    // Constructor principal (canónico) con validación
//    public ToolbarButtonDefinition {
//        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
//        Objects.requireNonNull(claveIcono, "claveIcono no puede ser nula");
//        Objects.requireNonNull(categoriaLayout, "categoriaLayout no puede ser nula");
//        Objects.requireNonNull(tipoBoton, "tipoBoton no puede ser nulo");
//    }
//
//    // --- Constructor sobrecargado para retrocompatibilidad ---
//    /**
//     * Constructor para botones normales. Por defecto, el tipo será `ButtonType.NORMAL`.
//     */
//    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout) {
//        // Llama al constructor principal, asignando ButtonType.NORMAL por defecto.
//        this(comandoCanonico, claveIcono, textoTooltip, categoriaLayout, ButtonType.NORMAL);
//    }
//    
//} // --- FIN del record ToolbarButtonDefinition ---
//
