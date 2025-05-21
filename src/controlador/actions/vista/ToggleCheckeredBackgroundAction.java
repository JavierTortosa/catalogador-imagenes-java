// En src/controlador/actions/vista/ToggleCheckeredBackgroundAction.java
package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import servicios.ConfigurationManager;
import vista.VisorView; // Dependencia directa para esta Action

public class ToggleCheckeredBackgroundAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private VisorView viewRef;
    private ConfigurationManager configManagerRef; 
    private String configKeyForState; // Ej: "interfaz.menu.vista.fondo_a_cuadros.seleccionado"
    // No necesitamos componentIdForViewManager aquí

    public ToggleCheckeredBackgroundAction(String name, 
                                       ImageIcon icon, 
                                       VisorView view, // Dependencia directa
                                       ConfigurationManager configManager, 
                                       String configKeyForSelectedState,
                                       String actionCommandKey) {
        super(name, icon); 
        
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleCheckeredBackgroundAction");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForState no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Activar o desactivar el fondo a cuadros de la imagen");
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        // Leer el estado inicial de la configuración
        // El default es 'false' (fondo a cuadros desactivado inicialmente)
        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, false); 
        putValue(Action.SELECTED_KEY, initialState);
        
        // Aplicar el estado inicial a la vista (importante si no se hace en otro lado)
        // Esto asegura que la vista refleje la configuración al arrancar.
        if (this.viewRef != null) {
            this.viewRef.setCheckeredBackgroundEnabled(initialState);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewRef == null || configManagerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleCheckeredBackgroundAction]: View o ConfigManager nulos.");
            return;
        }

        // El nuevo estado 'seleccionado' ya está en la propiedad SELECTED_KEY de la Action
        // porque JCheckBoxMenuItem lo actualiza antes de llamar a actionPerformed.
        boolean nuevoEstadoActivado = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleCheckeredBackgroundAction actionPerformed] Nuevo estado: " + nuevoEstadoActivado);

        // 1. Actualizar la configuración en memoria
        configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoActivado));
        System.out.println("  -> Configuración '" + this.configKeyForState + "' actualizada a: " + nuevoEstadoActivado);
        
        // 2. Llamar al método de VisorView para cambiar el estado del fondo a cuadros
        viewRef.setCheckeredBackgroundEnabled(nuevoEstadoActivado); 
        // VisorView.setCheckeredBackgroundEnabled internamente llama a repaint() en la etiquetaImagen.
    }
}