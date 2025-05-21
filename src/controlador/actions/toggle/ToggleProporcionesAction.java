// En src/controlador/actions/toggle/ToggleProporcionesAction.java
package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // Para llamar al método centralizado de lógica y UI
import controlador.commands.AppActionCommands;
import modelo.VisorModel; // Para leer el estado inicial desde el modelo
// ConfigurationManager y ZoomManager no son necesarios aquí directamente si VisorController lo maneja

public class ToggleProporcionesAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    // La clave de configuración será manejada por VisorController al aplicar el cambio.

    private VisorController controllerRef;
    private VisorModel modelRef; // Para leer el estado inicial

    public ToggleProporcionesAction(
            String name,
            ImageIcon icon,
            VisorModel model, // Necesario para el estado inicial
            VisorController controller
    ) {
        super(name, icon);
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en ToggleProporcionesAction");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en ToggleProporcionesAction");

        putValue(Action.SHORT_DESCRIPTION, "Alternar mantener relación de aspecto al escalar/zoomear");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);

        // Estado Inicial del SELECTED_KEY: Se lee del modelo, que ya debería tener el valor de config.
        // Esto asegura que el JCheckBoxMenuItem/JToggleButton se inicie correctamente.
        if (this.modelRef != null) {
            // Asumimos que VisorModel.isMantenerProporcion() ya refleja el estado cargado de ConfigurationManager
            boolean initialStateSelected = this.modelRef.isMantenerProporcion();
            putValue(Action.SELECTED_KEY, initialStateSelected);
            System.out.println("[ToggleProporcionesAction Constructor] Estado inicial SELECTED_KEY: " + initialStateSelected + " (desde modelo)");
        } else {
            putValue(Action.SELECTED_KEY, true); // Fallback, debería coincidir con el default de config
            System.out.println("[ToggleProporcionesAction Constructor] WARN: modelRef era null, SELECTED_KEY fallback a true");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleProporcionesAction]: controllerRef es nulo.");
            return;
        }

        // El componente UI (JCheckBoxMenuItem o JToggleButton) ya actualizó
        // la propiedad Action.SELECTED_KEY de ESTA Action al nuevo estado.
        boolean nuevoEstadoMantenerProporciones = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));

        System.out.println("[ToggleProporcionesAction actionPerformed] El componente UI cambió SELECTED_KEY a: " + nuevoEstadoMantenerProporciones);
        System.out.println("  -> Llamando a controllerRef.setMantenerProporcionesLogicaYUi(" + nuevoEstadoMantenerProporciones + ")");

        // Delegar toda la lógica (actualizar modelo, config, refrescar imagen, sincronizar otros controles UI)
        // a un método centralizado en VisorController.
        controllerRef.setMantenerProporcionesLogicaYUi(nuevoEstadoMantenerProporciones);
    }

    /**
     * Método para ser llamado por el VisorController para sincronizar
     * el estado SELECTED_KEY de esta Action con el estado actual del modelo.
     * @param estadoActualModeloMantenerProporciones El estado actual de 'mantenerProporciones' en el modelo.
     */
    public void sincronizarSelectedKeyConModelo(boolean estadoActualModeloMantenerProporciones) {
        if (!Objects.equals(getValue(Action.SELECTED_KEY), estadoActualModeloMantenerProporciones)) {
            putValue(Action.SELECTED_KEY, estadoActualModeloMantenerProporciones);
            // System.out.println("  [ToggleProporcionesAction sincronizar] SELECTED_KEY actualizado a: " + estadoActualModeloMantenerProporciones);
        }
    }
}