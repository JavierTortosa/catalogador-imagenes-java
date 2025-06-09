package vista.config;

import java.util.List;

/**
 * Define una barra de herramientas temática, incluyendo su identificador,
 * título visible y la lista de botones que contiene.
 *
 * @param claveBarra Identificador único para la barra (ej: "barra_navegacion").
 * @param tituloVisible El texto que puede mostrar la JToolBar (ej: "Navegación").
 * @param botones La lista de ToolbarButtonDefinition para esta barra.
 */
public record ToolbarDefinition( // <-- LA CLAVE ES "public"
	    String claveBarra,
	    String titulo,
	    List<ToolbarButtonDefinition> botones
	) {}
