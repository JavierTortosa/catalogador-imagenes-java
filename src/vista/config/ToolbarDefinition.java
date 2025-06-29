package vista.config;

import java.util.List;
import java.util.Set; // <-- Importación necesaria

import modelo.VisorModel.WorkMode; // <-- Importación necesaria

/**
 * Define una barra de herramientas temática, incluyendo su identificador,
 * título, orden de visualización, modos en los que es visible y la lista
 * de botones que contiene.
 *
 * @param claveBarra Identificador único para la barra (ej: "navegacion").
 * @param titulo El texto que puede mostrar la JToolBar (ej: "Navegación").
 * @param orden Un entero que define la posición relativa de esta barra de herramientas
 *              en el contenedor. Un número menor aparece antes.
 * @param modosVisibles Un conjunto de {@link WorkMode} en los que esta barra de herramientas
 *                      debería ser visible.
 * @param botones La lista de {@link ToolbarButtonDefinition} para esta barra.
 */
public record ToolbarDefinition(
    String claveBarra,
    String titulo,
    int orden,
    Set<WorkMode> modosVisibles,
    List<ToolbarButtonDefinition> botones
) {}
