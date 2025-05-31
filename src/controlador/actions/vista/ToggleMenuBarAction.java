// En controlador.actions.vista.ToggleMenuBarAction.java
package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import controlador.VisorController; // <<<< AÑADIR IMPORT SI NO ESTÁ
import servicios.ConfigurationManager;
import vista.VisorView;
// Ya no necesitamos ViewManager aquí si el controller hace el despacho
// import controlador.managers.ViewManager;

public class ToggleMenuBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    // private ViewManager viewManagerRef; // Ya no se necesita si llamamos al controller
    private ConfigurationManager configManagerRef;
    private String configKeyForState;
    private String componentIdForController; // Renombrado para claridad, este es el uiElementIdentifier
    private VisorView viewRef;
    private VisorController controllerRef; // <<<< AÑADIR REFERENCIA AL CONTROLLER

    // CONSTRUCTOR ACTUALIZADO para recibir VisorController
    public ToggleMenuBarAction(String name,
                               ImageIcon icon,
                               // ViewManager viewManager, // Ya no se necesita como parámetro directo
                               ConfigurationManager configManager,
                               VisorView view,
                               VisorController controller, // <<<< AÑADIR PARÁMETRO CONTROLLER
                               String configKeyForSelectedState,
                               String componentIdentifier, // Este será el uiElementIdentifier para el controller
                               String actionCommandKey) {
        super(name, icon);

        // this.viewManagerRef = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null"); // <<<< ASIGNAR CONTROLLER
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForSelectedState no puede ser null");
        this.componentIdForController = Objects.requireNonNull(componentIdentifier, "componentIdentifier no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (configManagerRef == null || viewRef == null || controllerRef == null) { // Actualizar validación
            System.err.println("ERROR CRÍTICO [ToggleMenuBarAction]: ConfigManager, ViewRef o ControllerRef nulos.");
            return;
        }

        boolean nuevaVisibilidadComponente = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleMenuBarAction actionPerformed] para: " + componentIdForController +
                           ", nuevo estado de visibilidad: " + nuevaVisibilidadComponente);

        // 1. Guardar el nuevo estado en ConfigurationManager
        this.configManagerRef.setString(this.configKeyForState, String.valueOf(nuevaVisibilidadComponente));
        System.out.println("  -> [ToggleMenuBarAction] Estado guardado en config: " + this.configKeyForState + " = " + nuevaVisibilidadComponente);

        // 2. Notificar al VisorController para que actualice la UI
        //    this.componentIdForController es el "Barra_de_Menu", "Barra_de_Botones", etc.
        this.controllerRef.solicitarActualizacionInterfaz(
            this.componentIdForController,
            this.configKeyForState,
            nuevaVisibilidadComponente
        );

        // 3. Lógica específica de ToggleMenuBarAction: Actualizar visibilidad del botón "Menú Especial"
        //    Esta lógica se mantiene aquí porque es particular de esta acción.
        if ("Barra_de_Menu".equals(this.componentIdForController)) { // Solo si esta acción es para la barra de menú
            Map<String, JButton> botonesToolbar = viewRef.getBotonesPorNombre();
            if (botonesToolbar != null) {
                String claveBotonMenuEspecial = "interfaz.boton.especiales.Menu_48x48"; // Asegúrate que esta clave es correcta
                JButton botonMenuEspecial = botonesToolbar.get(claveBotonMenuEspecial);

                if (botonMenuEspecial != null) {
                    boolean visibilidadBotonMenu = !nuevaVisibilidadComponente; // Inversa a la JMenuBar
                    if (botonMenuEspecial.isVisible() != visibilidadBotonMenu) {
                        botonMenuEspecial.setVisible(visibilidadBotonMenu);
                        System.out.println("  -> [ToggleMenuBarAction] Visibilidad del botón Menu_48x48 (toolbar) actualizada a: " + visibilidadBotonMenu);
                        // Opcional: Si la visibilidad de ESTE botón también se guarda en config y tiene su propio control,
                        // esta lógica de guardado no iría aquí. Pero si solo depende de la JMenuBar, está bien.
                        // configManagerRef.setString(claveBotonMenuEspecial + ".visible", String.valueOf(visibilidadBotonMenu));
                    }
                } else {
                    System.err.println("WARN [ToggleMenuBarAction]: Botón Menu_48x48 no encontrado en la toolbar.");
                }
            } else {
                 System.err.println("WARN [ToggleMenuBarAction]: Mapa de botones de la toolbar es nulo.");
            }
        }
    }
}