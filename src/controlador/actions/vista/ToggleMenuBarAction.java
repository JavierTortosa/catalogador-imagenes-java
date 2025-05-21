// En src/controlador/actions/vista/ToggleMenuBarAction.java
package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Map; // Para el mapa de botones
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton; // Para el botón de la toolbar

import controlador.commands.AppActionCommands; // Para la clave del botón
import servicios.ConfigurationManager;
import vista.VisorView;       // Para obtener el mapa de botones
import controlador.managers.ViewManager;

public class ToggleMenuBarAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ViewManager viewManagerRef;
    private ConfigurationManager configManagerRef; 
    private String configKeyForState;       
    private String componentIdForViewManager; 
    private VisorView viewRef; // <<< AÑADIR REFERENCIA A VisorView

    // CONSTRUCTOR ACTUALIZADO
    public ToggleMenuBarAction(String name, 
                               ImageIcon icon, 
                               ViewManager viewManager,
                               ConfigurationManager configManager, 
                               VisorView view, // <<< AÑADIR VisorView
                               String configKeyForSelectedState,
                               String componentIdentifier,
                               String actionCommandKey) {
        super(name, icon); 
        
        this.viewManagerRef = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleMenuBarAction"); // <<< ASIGNAR
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForSelectedState no puede ser null");
        this.componentIdForViewManager = Objects.requireNonNull(componentIdentifier, "componentIdentifier no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
        
        // Aplicar visibilidad inicial del botón Menú Especial
        // (se hace una vez al inicio, AppInitializer llamará a setComponentePrincipalVisible
        // que a su vez llamará a este actionPerformed indirectamente si el estado cambia,
        // o podemos forzar la lógica aquí para el botón menú especial).
        // Es mejor que esta lógica se ejecute cuando la JMenuBar *realmente* cambia de estado.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewManagerRef == null || configManagerRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMenuBarAction]: ViewManager, ConfigManager o ViewRef nulos.");
            return;
        }

        // El JCheckBoxMenuItem ya actualizó el SELECTED_KEY de esta Action
        boolean nuevaVisibilidadMenuBar = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleMenuBarAction actionPerformed] para: " + componentIdForViewManager + 
                           ", nuevo estado de JMenuBar: " + nuevaVisibilidadMenuBar);

        // 1. Hacer que ViewManager cambie la visibilidad de la JMenuBar y guarde la config
        this.viewManagerRef.setComponentePrincipalVisible(
            this.componentIdForViewManager, 
            nuevaVisibilidadMenuBar, 
            this.configKeyForState
        );

        // 2. Actualizar la visibilidad del botón "Menú Especial" de la toolbar
        //    Si la JMenuBar está visible, el botón "Menú Especial" debe estar oculto.
        //    Si la JMenuBar está oculta, el botón "Menú Especial" debe estar visible.
        Map<String, JButton> botonesToolbar = viewRef.getBotonesPorNombre();
        if (botonesToolbar != null) {
            // La clave larga del botón "Menú Especial" como se define en UIDefinitionService y ToolbarBuilder
            String claveBotonMenuEspecial = "interfaz.boton.especiales.Menu_48x48"; 
            JButton botonMenuEspecial = botonesToolbar.get(claveBotonMenuEspecial);

            if (botonMenuEspecial != null) {
                boolean visibilidadBotonMenu = !nuevaVisibilidadMenuBar; // Inversa a la JMenuBar
                if (botonMenuEspecial.isVisible() != visibilidadBotonMenu) {
                    botonMenuEspecial.setVisible(visibilidadBotonMenu);
                    System.out.println("  -> Visibilidad del botón Menu_48x48 (toolbar) actualizada a: " + visibilidadBotonMenu);
                    // Aquí también podrías guardar la configuración de visibilidad de este botón si es necesario.
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