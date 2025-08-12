package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IViewManager;
import servicios.ConfigurationManager;
import vista.VisorView;

public class ToggleAlwaysOnTopAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(ToggleAlwaysOnTopAction.class);
	
    private static final long serialVersionUID = 1L;

    // --- LÍNEA MODIFICADA (1/4) ---
    private final IViewManager viewManager;
    
    private final ConfigurationManager configuration;
    private final String configKey;

    // --- LÍNEA MODIFICADA (2/4) ---
    public ToggleAlwaysOnTopAction(String name, Icon icon, IViewManager viewManager, ConfigurationManager configuration, String configKey, String commandKey) {
        super(name, icon);
        
        // --- LÍNEA MODIFICADA (3/4) ---
        this.viewManager = Objects.requireNonNull(viewManager, "ViewManager no puede ser nulo");
        
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser nulo");
        this.configKey = Objects.requireNonNull(configKey, "ConfigKey no puede ser nulo");

        // Configuración inicial de la Action
        putValue(Action.ACTION_COMMAND_KEY, commandKey);
        boolean initialState = configuration.getBoolean(configKey, false);
        putValue(Action.SELECTED_KEY, initialState);
    } // --- FIN del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. OBTENER LA FUENTE DEL EVENTO
        // Es crucial saber quién nos ha llamado (el botón, un item de menú, etc.)
        Object source = e.getSource();
        if (!(source instanceof AbstractButton)) {
            // Si no es un botón (raro), no podemos saber el estado. No hacemos nada.
            logger.warn("WARN [ToggleAlwaysOnTopAction]: La acción fue disparada por un componente no-botón.");
            return;
        }

        // 2. OBTENER EL NUEVO ESTADO DIRECTAMENTE DEL BOTÓN
        // El método isSelected() nos da el estado DESPUÉS del clic. Es la fuente de verdad.
        AbstractButton button = (AbstractButton) source;
        boolean newState = button.isSelected();

        // 3. OBTENER LA VISTA
        VisorView view = viewManager.getView();
        if (view == null) {
            logger.error("ERROR [ToggleAlwaysOnTopAction]: La vista es nula. No se puede ejecutar la acción.");
            // Revertimos el estado visual del botón si la acción no se puede completar.
            button.setSelected(!newState); 
            return;
        }

        // 4. SINCRONIZAR TODO CON EL NUEVO ESTADO

        // a) Actualizar el estado interno de la Action para que coincida con el botón.
        //    Esto es importante para que los items de menú y otros componentes se enteren.
        putValue(Action.SELECTED_KEY, newState);

        // b) Actualizar la configuración en memoria.
        configuration.setString(configKey, String.valueOf(newState));
        
        // c) Aplicar el cambio real a la ventana.
        view.setAlwaysOnTop(newState);
        
        logger.debug("[ToggleAlwaysOnTopAction] 'Siempre Encima' cambiado a: " + newState);

    } // --- FIN del metodo actionPerformed ---
    
} // --- FIN de la clase ToggleAlwaysOnTopAction ---