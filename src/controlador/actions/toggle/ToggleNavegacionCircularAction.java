package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import modelo.VisorModel;
import servicios.ConfigurationManager; // No lo usa directamente en actionPerformed, pero sí en constructor

public class ToggleNavegacionCircularAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private VisorModel modelRef; // Referencia al modelo
    private VisorController controllerRef; // Referencia al controlador
    private ConfigurationManager configManagerRef; // Para leer el estado inicial
    private String configKey; // Clave para leer/guardar estado

    public ToggleNavegacionCircularAction(String name, ImageIcon icon,
                                        ConfigurationManager configManager,
                                        VisorModel model,
                                        VisorController controller,
                                        String configKeyForState,
                                        String actionCommandKey) {
        super(name, icon);
        this.modelRef = model;
        this.controllerRef = controller;
        this.configManagerRef = configManager;
        this.configKey = configKeyForState;

        putValue(Action.ACTION_COMMAND_KEY, actionCommandKey);
        putValue(Action.SHORT_DESCRIPTION, "Activa/Desactiva la navegación circular en las listas");

        // Inicializar el estado SELECTED_KEY desde la configuración
        // Y sincronizar el modelo
        boolean initialState = this.configManagerRef.getBoolean(this.configKey, false);
        putValue(Action.SELECTED_KEY, initialState);
        if (this.modelRef != null) { // Asegurar que el modelo esté disponible
            this.modelRef.setNavegacionCircularActivada(initialState);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef == null || modelRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleNavegacionCircularAction]: controllerRef o modelRef es nulo.");
            return;
        }

        // 1. Leer el estado ACTUAL desde el VisorModel (la fuente de verdad)
        boolean actualmenteCircular = modelRef.isNavegacionCircularActivada();

        // 2. Determinar el NUEVO estado deseado invirtiendo el estado actual del modelo
        boolean nuevoEstadoCircularDeseado = !actualmenteCircular;

        System.out.println("[ToggleNavegacionCircularAction actionPerformed] Estado actual (circular): " + actualmenteCircular +
                           ". Solicitando cambiar a (circular): " + nuevoEstadoCircularDeseado);
        
        // 3. Llamar a un método en VisorController para que aplique la lógica
        //    (actualizar modelo, config, y sincronizar toda la UI relacionada).
        controllerRef.setNavegacionCircularLogicaYUi(nuevoEstadoCircularDeseado);
    }
}