package vista.config;

import java.util.List;
import java.util.Set;

import modelo.VisorModel.WorkMode;

public record ToolbarDefinition(
		
    String claveBarra,
    String titulo,
    int orden,
    Set<WorkMode> modosVisibles,
    List<ToolbarComponentDefinition> componentes, // <-- CAMBIO DE NOMBRE: de "botones" a "componentes"
    ToolbarAlignment alignment
) {
    
    public ToolbarDefinition {
        if (alignment == null) {
            alignment = ToolbarAlignment.LEFT;
        }
    }

    public ToolbarDefinition(String claveBarra, String titulo, int orden, Set<WorkMode> modosVisibles, List<ToolbarButtonDefinition> botones) {
        // La llamada al constructor principal ahora usa el nuevo nombre "componentes"
        this(claveBarra, titulo, orden, modosVisibles, List.copyOf(botones), ToolbarAlignment.LEFT);
    }
    
} // --- FIN del record ToolbarDefinition ---