package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.ConfigurationManager;
import vista.VisorView; 

public class ToggleMiniatureTextAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(ToggleMiniatureTextAction.class); 

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

        logger.debug("[ToggleMiniatureTextAction Constructor] Configurada con clave de estado: " + this.configKeyForState);

        
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

        // 1. Obtener el estado directamente del componente que disparó el evento.
        //    Esto es 100% fiable porque leemos el estado DESPUÉS de que el usuario ha hecho clic.
        boolean nuevoEstadoMostrarTexto = false;
        if (e.getSource() instanceof JCheckBoxMenuItem) {
            nuevoEstadoMostrarTexto = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        } else {
            // Fallback por si la acción se usa en otro tipo de componente en el futuro.
            // Si no sabemos la fuente, podemos invertir el estado guardado, pero es menos fiable.
            System.err.println("WARN [ToggleMiniatureTextAction]: La fuente del evento no es un JCheckBoxMenuItem. El estado podría ser incorrecto.");
            // Como fallback, invertimos el valor que tenemos en la acción
            nuevoEstadoMostrarTexto = !Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        }
        
        // 2. Actualizar la propiedad de la acción para mantener la consistencia.
        //    Esto es útil si algún otro componente escucha cambios en esta propiedad.
        putValue(Action.SELECTED_KEY, nuevoEstadoMostrarTexto);

        System.out.println("[ToggleMiniatureTextAction actionPerformed] Nuevo estado para mostrar texto: " + nuevoEstadoMostrarTexto);

        // 3. Actualizar la configuración en memoria.
        configManagerRef.setString(this.configKeyForState, String.valueOf(nuevoEstadoMostrarTexto));
        System.out.println("  -> Configuración '" + this.configKeyForState + "' actualizada a: " + nuevoEstadoMostrarTexto);
            
        // 4. Llamar al método de VisorView para que actualice el renderer.
        viewRef.solicitarRefrescoRenderersMiniaturas(); 
        System.out.println("  -> Llamada a viewRef.solicitarRefrescoRenderersMiniaturas() realizada.");
        
    } // --- FIN del metodo actionPerformed ---
    
} // FIN de la clase ToggleMiniatureTextAction ---