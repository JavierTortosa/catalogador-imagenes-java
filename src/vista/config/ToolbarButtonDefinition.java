package vista.config;

import java.util.List;
import java.util.Objects;

/**
 * Define la estructura y propiedades de un botón o componente en una barra de herramientas.
 * Este record es la base para la construcción declarativa de la UI.
 */
public record ToolbarButtonDefinition (
    /** Comando Canónico (de AppActionCommands) que ejecuta este botón o que sirve como clave base. */
    String comandoCanonico,
    
    /** Clave o nombre del archivo del icono base del componente. */
    String claveIcono,

    /** Ámbito del icono base (si depende del tema o es común). */
    IconScope scopeIconoBase,

    /** Texto que aparecerá como tooltip general del componente. */
    String textoTooltip,
    
    /** Categoría lógica para agrupación y layout (ej: "movimiento", "edicion"). */
    String categoriaLayout,
    
    /** El tipo de componente a crear (NORMAL, TOGGLE, DPAD_CRUZ, etc.). */
    ButtonType tipoBoton,

    /** Lista de definiciones de hotspots para componentes complejos como D-Pads. Null para botones simples. */
    List<HotspotDefinition> listaDeHotspots,

    /** Número de filas para layouts de tipo DPAD_GRID. 0 para otros. */
    int gridRows,

    /** Número de columnas para layouts de tipo DPAD_GRID. 0 para otros. */
    int gridCols

		) implements ToolbarComponentDefinition {
	
    // Constructor Canónico (valida los campos no nulos)
    public ToolbarButtonDefinition {
        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
        Objects.requireNonNull(claveIcono, "claveIcono no puede ser nula");
        Objects.requireNonNull(scopeIconoBase, "scopeIconoBase no puede ser nulo");
        Objects.requireNonNull(categoriaLayout, "categoriaLayout no puede ser nula");
        Objects.requireNonNull(tipoBoton, "tipoBoton no puede ser nulo");
        // listaDeHotspots, gridRows, y gridCols pueden ser null o 0.
    }

    /**
     * Constructor para botones SIMPLES (NORMAL, TOGGLE, TRANSPARENT) que usan un icono del tema.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout, ButtonType tipoBoton) {
        this(comandoCanonico, claveIcono, IconScope.THEMED, textoTooltip, categoriaLayout, tipoBoton, null, 0, 0);
    }
    
    /**
     * Constructor para botones SIMPLES (NORMAL, TOGGLE, TRANSPARENT) que usan un icono COMÚN.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, IconScope scope, String textoTooltip, String categoriaLayout, ButtonType tipoBoton) {
        this(comandoCanonico, claveIcono, scope, textoTooltip, categoriaLayout, tipoBoton, null, 0, 0);
    }

    /**
     * Constructor de conveniencia para botones NORMALES que usan un icono del tema.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, String textoTooltip, String categoriaLayout) {
        this(comandoCanonico, claveIcono, IconScope.THEMED, textoTooltip, categoriaLayout, ButtonType.NORMAL, null, 0, 0);
    }

    /**
     * Constructor para DPAD_CRUZ.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, IconScope scope, String textoTooltip, String categoriaLayout, List<HotspotDefinition> listaDeHotspots) {
        this(comandoCanonico, claveIcono, scope, textoTooltip, categoriaLayout, ButtonType.DPAD_CRUZ, listaDeHotspots, 0, 0);
    }

    /**
     * Constructor para DPAD_GRID.
     */
    public ToolbarButtonDefinition(String comandoCanonico, String claveIcono, IconScope scope, String textoTooltip, String categoriaLayout, List<HotspotDefinition> listaDeHotspots, int rows, int cols) {
        this(comandoCanonico, claveIcono, scope, textoTooltip, categoriaLayout, ButtonType.DPAD_GRID, listaDeHotspots, rows, cols);
    }

} // --- FIN del record ToolbarButtonDefinition ---