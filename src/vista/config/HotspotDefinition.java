package vista.config;

/**
 * Define un "hotspot" o zona interactiva dentro de un componente complejo como un D-Pad.
 * Contiene la información necesaria para que el ToolbarBuilder construya el Hotspot real,
 * asociando un comando, un tooltip y un icono.
 */
public record HotspotDefinition(
	    String comando,
	    String icono,
	    String tooltip,
	    IconScope scope
	) {
    /**
     * Constructor de conveniencia que asume un IconScope.THEMED por defecto.
     * Simplifica la creación de hotspots que usan iconos del tema actual.
     *
     * @param comando El comando de acción (de AppActionCommands) que ejecutará este hotspot.
     * @param tooltip El texto a mostrar cuando el ratón se pose sobre el hotspot.
     * @param icono   La clave del icono a mostrar para el estado 'hover' de este hotspot.
     */
    public HotspotDefinition(String comando, String tooltip, String icono) {
        this(comando, icono, tooltip, IconScope.THEMED);
    }
} // --- FIN del record HotspotDefinition ---