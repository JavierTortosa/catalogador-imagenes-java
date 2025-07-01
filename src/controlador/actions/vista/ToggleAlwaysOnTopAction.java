package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import servicios.ConfigurationManager;
import vista.VisorView; // Dependencia directa

public class ToggleAlwaysOnTopAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private VisorView viewRef;
    private ConfigurationManager configManagerRef; 
    private String configKeyForState;

    public ToggleAlwaysOnTopAction(String name, 
                                   ImageIcon icon,
                                   VisorView view, // Ahora puede ser null en la construcción
                                   ConfigurationManager configManager, 
                                   String configKeyForSelectedState,
                                   String actionCommandKey) {
        super(name, icon); 
        
        this.viewRef = view; // Se asigna, pero sin requireNonNull
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mantener la ventana siempre visible encima de otras");
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, false); 
        putValue(Action.SELECTED_KEY, initialState);
        
        // La aplicación del estado inicial se mueve al método setView.
    } // --- Fin del método ToggleAlwaysOnTopAction (constructor) ---

    /**
     * Inyecta la referencia a la vista después de que ha sido creada.
     * @param view La instancia de VisorView.
     */
    public void setView(VisorView view) {
        this.viewRef = Objects.requireNonNull(view, "La vista no puede ser nula al inyectarse en ToggleAlwaysOnTopAction");
        // Aplicar el estado inicial ahora que tenemos la vista
        boolean initialState = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        if (this.viewRef.isAlwaysOnTop() != initialState) {
            this.viewRef.setAlwaysOnTop(initialState);
        }
    } // --- Fin del método setView ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewRef == null || configManagerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleAlwaysOnTopAction]: View o ConfigManager nulos.");
            return;
        }

        // El estado seleccionado del JCheckBoxMenuItem se actualiza automáticamente.
        // Lo leemos para saber cuál es el nuevo estado deseado.
        boolean nuevoEstadoAlwaysOnTop = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleAlwaysOnTopAction actionPerformed] Nuevo estado: " + nuevoEstadoAlwaysOnTop);

        configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoAlwaysOnTop));
        System.out.println("  -> Configuración '" + this.configKeyForState + "' actualizada a: " + nuevoEstadoAlwaysOnTop);
            
        if (viewRef.isAlwaysOnTop() != nuevoEstadoAlwaysOnTop) {
            viewRef.setAlwaysOnTop(nuevoEstadoAlwaysOnTop);
        }
    } // --- Fin del método actionPerformed ---
    
} // --- FIN de la clase ToggleAlwaysOnTopAction ---