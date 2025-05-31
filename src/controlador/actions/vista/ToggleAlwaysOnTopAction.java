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
    private String configKeyForState; // Ej: "interfaz.menu.vista.mantener_ventana_siempre_encima.seleccionado"

    public ToggleAlwaysOnTopAction(String name, 
                                   ImageIcon icon, // Probablemente null
                                   VisorView view, 
                                   ConfigurationManager configManager, 
                                   String configKeyForSelectedState,
                                   String actionCommandKey) {
        super(name, icon); 
        
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleAlwaysOnTopAction");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mantener la ventana siempre visible encima de otras");
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // Leer el estado inicial de la configuración
        // El default es 'false' (no siempre encima)
        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, false); 
        putValue(Action.SELECTED_KEY, initialState);
        
        // Aplicar el estado inicial a la ventana (JFrame)
        if (this.viewRef != null) {
            // Comprobar si el estado actual es diferente para evitar llamadas innecesarias
            if (this.viewRef.isAlwaysOnTop() != initialState) {
                this.viewRef.setAlwaysOnTop(initialState);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewRef == null || configManagerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleAlwaysOnTopAction]: View o ConfigManager nulos.");
            return;
        }

        boolean nuevoEstadoAlwaysOnTop = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleAlwaysOnTopAction actionPerformed] Nuevo estado: " + nuevoEstadoAlwaysOnTop);

        // 1. Actualizar la configuración en memoria
        configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoAlwaysOnTop));
        System.out.println("  -> Configuración '" + this.configKeyForState + "' actualizada a: " + nuevoEstadoAlwaysOnTop);
            
        // 2. Llamar al método de VisorView (que es un JFrame) para cambiar el estado "AlwaysOnTop"
        //    Comprobar si el estado actual es diferente para evitar llamadas innecesarias
        if (viewRef.isAlwaysOnTop() != nuevoEstadoAlwaysOnTop) {
            viewRef.setAlwaysOnTop(nuevoEstadoAlwaysOnTop);
        }
        // No se necesita revalidate/repaint para setAlwaysOnTop, el sistema operativo lo maneja.
    }
}