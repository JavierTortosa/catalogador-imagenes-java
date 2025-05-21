package controlador.actions.vista;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon; 
// import javax.swing.JCheckBoxMenuItem; // No es necesario si la Action no manipula directamente el componente

// import controlador.commands.AppActionCommands; // No es necesario si se pasa como parámetro
import servicios.ConfigurationManager;
import vista.VisorView; // No se usa directamente aquí si ViewManager lo maneja
import controlador.managers.ViewManager;


public class ToggleFileListAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private ViewManager viewManagerRef;
    private ConfigurationManager configManagerRef; 
    private String configKeyForState;       
    private String componentIdForViewManager; 

    // CONSTRUCTOR ACTUALIZADO PARA ACEPTAR 7 ARGUMENTOS
    public ToggleFileListAction(String name, 
                               ImageIcon icon, 
                               ViewManager viewManager,
                               ConfigurationManager configManager, 
                               String configKeyForSelectedState,
                               String componentIdentifier,
                               String actionCommandKey) { // <--- AÑADIDO actionCommandKey
        super(name, icon); 
        
        this.viewManagerRef = Objects.requireNonNull(viewManager, "ViewManager no puede ser null");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForState = Objects.requireNonNull(configKeyForSelectedState, "configKeyForSelectedState no puede ser null");
        this.componentIdForViewManager = Objects.requireNonNull(componentIdentifier, "componentIdentifier no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la " + name);
        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null")); // Usar el parámetro

        boolean initialState = this.configManagerRef.getBoolean(this.configKeyForState, true);
        putValue(Action.SELECTED_KEY, initialState);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewManagerRef == null || configManagerRef == null) { // configManagerRef no se usa aquí, pero está bien el chequeo
            System.err.println("ERROR CRÍTICO [ToggleFileListAction]: ViewManager es nulo.");
            return;
        }

        boolean nuevoEstadoVisible = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("[ToggleFileListAction actionPerformed] para: " + componentIdForViewManager + ", nuevo estado: " + nuevoEstadoVisible);

        this.viewManagerRef.setComponentePrincipalVisible(
            this.componentIdForViewManager, 
            nuevoEstadoVisible, 
            this.configKeyForState
        );
    }
}




//package controlador.actions.vista; // O el paquete donde la tengas
//
//import java.awt.event.ActionEvent;
//import javax.swing.Action;
//import javax.swing.JCheckBoxMenuItem; // Importar
//
//import controlador.VisorController;
//import controlador.actions.BaseVisorAction;
//// No suele necesitar IconUtils para menú
//// import vista.util.IconUtils;
//
//public class ToggleFileListAction extends BaseVisorAction {
//
//    private static final long serialVersionUID = 1L;
//    // Clave de configuración para el estado de este toggle
//    private static final String CONFIG_KEY = "interfaz.menu.vista.mostrar_ocultar_la_lista_de_archivos.seleccionado";
//
//    public ToggleFileListAction(VisorController controller) {
//        // --- TEXTO NUEVO ---
//        super("Ocultar la Lista de Archivos", controller); // Texto del menú
//        putValue(Action.SHORT_DESCRIPTION, "Mostrar u ocultar la Ocultar la Lista de Archivos");
//
//        // --- Estado Inicial Seleccionado ---
//        if (controller != null && controller.getConfigurationManager() != null) {
//            boolean initialState = controller.getConfigurationManager().getBoolean(CONFIG_KEY, true); // Default a true (visible)
//            putValue(Action.SELECTED_KEY, initialState);
//            System.out.println("[ToggleFileListAction] Estado inicial leído (" + CONFIG_KEY + "): " + initialState);
//        } else {
//             System.err.println("WARN [ToggleFileListAction]: Controller o ConfigMgr nulos en constructor. Estado inicial no establecido.");
//             putValue(Action.SELECTED_KEY, true); // Asumir visible por defecto si hay error
//        }
//        // --- FIN TEXTO NUEVO ---
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // --- TEXTO NUEVO ---
//        if (controller == null) { /*...*/ return; }
//        controller.logActionInfo(e);
//
//        // Determinar el nuevo estado deseado
//        boolean newState = false;
//        Object source = e.getSource();
//        if (source instanceof JCheckBoxMenuItem) {
//            // El estado deseado es el estado actual del checkbox después del clic
//            newState = ((JCheckBoxMenuItem) source).isSelected();
//        } else {
//             // Si se llama de otra forma (ej. atajo teclado), invertir estado actual de la Action
//             Object selectedValue = getValue(Action.SELECTED_KEY);
//             newState = !(selectedValue instanceof Boolean && (Boolean)selectedValue);
//             System.out.println("WARN [ToggleFileListAction]: Evento no es de JCheckBoxMenuItem, toggleando estado: " + newState);
//        }
//
//        // Llamar al método del controlador para aplicar el cambio y actualizar config
//        controller.setComponenteVisibleAndUpdateConfig("mostrar_ocultar_la_lista_de_archivos", newState);
//
//        // Actualizar el estado de esta Action para sincronizar
//        putValue(Action.SELECTED_KEY, newState);
//        // --- FIN TEXTO NUEVO ---
//    }
//}