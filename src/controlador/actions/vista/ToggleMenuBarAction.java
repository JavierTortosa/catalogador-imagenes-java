// Archivo: controlador/actions/vista/ToggleMenuBarAction.java

package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import servicios.ConfigurationManager;
// No se necesita importar VisorView

public class ToggleMenuBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final ConfigurationManager configManagerRef;
    private final String configKeyForState;
    private final String componentIdForController; 
    private final VisorController controllerRef; 

    public ToggleMenuBarAction(String name,
                               ImageIcon icon,
                               ConfigurationManager configManager,
                               VisorController controller,
                               String configKeyForSelectedState,
                               String componentIdentifier,
                               String actionCommandKey) {
        super(name, icon);

        this.configManagerRef = Objects.requireNonNull(configManager);
        this.controllerRef = Objects.requireNonNull(controller);
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState);
        this.componentIdForController = Objects.requireNonNull(componentIdentifier);

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMenuBarAction]: ConfigManager o ControllerRef nulos.");
            return;
        }

        // El estado seleccionado del JCheckBoxMenuItem se actualiza automáticamente.
        // Lo leemos para saber cuál es el nuevo estado deseado.
        boolean nuevaVisibilidadComponente = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        
        System.out.println("[ToggleMenuBarAction actionPerformed] para: " + componentIdForController +
                           ", nuevo estado de visibilidad: " + nuevaVisibilidadComponente);

        // 1. Guardar el nuevo estado en la configuración.
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevaVisibilidadComponente));
        
        // 2. Notificar al VisorController para que actualice la UI.
        this.controllerRef.solicitarActualizacionInterfaz(
            this.componentIdForController,
            this.configKeyForState,
            nuevaVisibilidadComponente
        );

        // 3. Lógica específica: Actualizar visibilidad del botón "Menú Especial".
        if ("Barra_de_Menu".equals(this.componentIdForController)) {
            // Obtenemos el mapa de botones DESDE EL CONTROLADOR.
            Map<String, JButton> botonesToolbar = controllerRef.getBotonesPorNombre();
            
            if (botonesToolbar != null) {
                String claveBotonMenuEspecial = "interfaz.boton.especiales.Menu_48x48";
                JButton botonMenuEspecial = botonesToolbar.get(claveBotonMenuEspecial);

                if (botonMenuEspecial != null) {
                    boolean visibilidadBotonMenu = !nuevaVisibilidadComponente; // Inversa a la JMenuBar
                    if (botonMenuEspecial.isVisible() != visibilidadBotonMenu) {
                        botonMenuEspecial.setVisible(visibilidadBotonMenu);
                        System.out.println("  -> [ToggleMenuBarAction] Visibilidad del botón Menu_48x48 actualizada a: " + visibilidadBotonMenu);
                    }
                } else {
                    System.err.println("WARN [ToggleMenuBarAction]: Botón Menu_48x48 no encontrado.");
                }
            } else {
                 System.err.println("WARN [ToggleMenuBarAction]: Mapa de botones en el controlador es nulo.");
            }
        }
    }
} // --- FIN de la clase ToggleMenuBarAction ---