package vista.config;

import java.util.List;
import java.util.Set;
import modelo.VisorModel.WorkMode;

/**
 * Define una barra de herramientas temática, incluyendo su identificador,
 * título, orden de visualización, modos en los que es visible, la lista
 * de botones que contiene y su alineamiento en el contenedor principal.
 *
 * @param claveBarra Identificador único para la barra (ej: "navegacion").
 * @param titulo El texto que puede mostrar la JToolBar (ej: "Navegación").
 * @param orden Un entero que define la posición relativa de esta barra de herramientas
 *              en el contenedor. Un número menor aparece antes.
 * @param modosVisibles Un conjunto de {@link WorkMode} en los que esta barra de herramientas
 *                      debería ser visible.
 * @param botones La lista de {@link ToolbarButtonDefinition} para esta barra.
 * @param alignment El alineamiento deseado (LEFT, CENTER, RIGHT) para esta barra.
 */
public record ToolbarDefinition(
    String claveBarra,
    String titulo,
    int orden,
    Set<WorkMode> modosVisibles,
    List<ToolbarButtonDefinition> botones,
    ToolbarAlignment alignment // <-- NUEVO PARÁMETRO
) {
    // Constructor compacto para añadir un valor por defecto si el alineamiento es nulo
    public ToolbarDefinition {
        if (alignment == null) {
            alignment = ToolbarAlignment.LEFT; // Por defecto, todo a la izquierda
        }
    }

    // Sobrecarga del constructor para mantener la compatibilidad con el código antiguo.
    // El código que no especifique alineamiento usará este constructor,
    // que delega al principal asignando LEFT por defecto.
    public ToolbarDefinition(String claveBarra, String titulo, int orden, Set<WorkMode> modosVisibles, List<ToolbarButtonDefinition> botones) {
        this(claveBarra, titulo, orden, modosVisibles, botones, ToolbarAlignment.LEFT);
    }
} // --- FIN del record ToolbarDefinition ---