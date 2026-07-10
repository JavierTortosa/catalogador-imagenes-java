package vista.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import modelo.VisorModel.WorkMode;

public record ToolbarDefinition(
		
    String claveBarra,
    String titulo,
    int orden,
    Set<WorkMode> modosVisibles,
    List<ToolbarComponentDefinition> componentes,
    ToolbarAlignment alignment,
    Set<ToolbarAlignment> colocaciones
) {
    
    public ToolbarDefinition {
        if (alignment == null) {
            alignment = ToolbarAlignment.LEFT;
        }
        if (colocaciones == null) {
            colocaciones = Collections.emptySet();
        }
    }

    public ToolbarDefinition(String claveBarra, String titulo, int orden, Set<WorkMode> modosVisibles, List<ToolbarButtonDefinition> botones) {
        this(claveBarra, titulo, orden, modosVisibles, List.copyOf(botones), ToolbarAlignment.LEFT, Collections.emptySet());
    }

    public ToolbarDefinition(String claveBarra, String titulo, int orden, Set<WorkMode> modosVisibles, List<ToolbarComponentDefinition> componentes, ToolbarAlignment alignment) {
        this(claveBarra, titulo, orden, modosVisibles, componentes, alignment, Collections.emptySet());
    }
    
} // --- FIN del record ToolbarDefinition ---