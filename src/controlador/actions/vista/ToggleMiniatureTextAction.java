// En src/controlador/actions/vista/ToggleMiniatureTextAction.java
package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import servicios.ConfigurationManager;
import vista.VisorView; // Dependencia directa
import controlador.commands.AppActionCommands; // Para la clave de comando

public class ToggleMiniatureTextAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ConfigurationManager configManagerRef;
    private VisorView viewRef;
    private String configKeyForState; // La clave de configuración para este estado

    /**
     * Constructor para la acción de mostrar/ocultar texto en miniaturas.
     * @param name El nombre para el JCheckBoxMenuItem.
     * @param icon El icono (probablemente null para un item de menú).
     * @param configManager El gestor de configuración para leer/guardar el estado.
     * @param view La referencia a VisorView para solicitar el refresco del renderer.
     * @param configKeyForSelectedState La clave de configuración específica para este toggle.
     * @param actionCommandKey El comando canónico para esta acción.
     */
    public ToggleMiniatureTextAction(String name, 
                                     ImageIcon icon, 
                                     ConfigurationManager configManager,
                                     VisorView view,
                                     String configKeyForSelectedState,
                                     String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null en ToggleMiniatureTextAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleMiniatureTextAction");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null en ToggleMiniatureTextAction");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar los nombres de archivo en las miniaturas");
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // Leer el estado inicial de la configuración.
        // El valor por defecto es 'true' (mostrar nombres en miniaturas).
        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true); 
        putValue(Action.SELECTED_KEY, initialState);
        
        // No es necesario aplicar el estado inicial a la vista aquí,
        // ya que VisorView (a través de VisorController.setMostrarNombresMiniaturas o su propio
        // solicitarRefrescoRenderersMiniaturas en la inicialización) debería configurar
        // el renderer con el estado correcto al arrancar la aplicación, basado en esta misma clave de config.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMiniatureTextAction]: ConfigurationManager o VisorView nulos.");
            return;
        }

        // El nuevo estado 'seleccionado' ya está en la propiedad SELECTED_KEY de la Action
        // porque JCheckBoxMenuItem (o el componente que use esta Action) lo actualiza
        // antes de llamar a actionPerformed.
        boolean nuevoEstadoMostrarTexto = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleMiniatureTextAction actionPerformed] Nuevo estado para mostrar texto en miniaturas: " + nuevoEstadoMostrarTexto);

        // 1. Actualizar la configuración en memoria.
        configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoMostrarTexto));
        System.out.println("  -> Configuración '" + this.configKeyForState + "' actualizada a: " + nuevoEstadoMostrarTexto);
            
        // 2. Llamar al método de VisorView para que actualice el renderer y repinte la lista de miniaturas.
        //    Este método en VisorView leerá la configuración actualizada para saber si mostrar o no los nombres.
        viewRef.solicitarRefrescoRenderersMiniaturas(); 
        System.out.println("  -> [ToggleMiniatureTextAction] Llamada a viewRef.solicitarRefrescoRenderersMiniaturas() realizada.");
        
        // El JCheckBoxMenuItem asociado se actualizará visualmente de forma automática
        // debido al cambio en la propiedad Action.SELECTED_KEY.
    }
}