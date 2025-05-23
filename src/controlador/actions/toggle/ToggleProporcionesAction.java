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
        // 1. Validar dependencias (controllerRef y modelRef)
        if (controllerRef == null || modelRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleProporcionesAction]: controllerRef o modelRef es nulo.");
            return;
        }

        // 2. Leer el estado ACTUAL de "mantener proporciones" desde el VisorModel.
        boolean actualmenteMantieneProporciones = modelRef.isMantenerProporcion();

        // 3. Determinar el NUEVO estado deseado invirtiendo el estado actual del modelo.
        boolean nuevoEstadoDeseadoMantenerProporciones = !actualmenteMantieneProporciones;

        System.out.println("[ToggleProporcionesAction actionPerformed] Estado actual (mantener prop): " + actualmenteMantieneProporciones +
                           ". Solicitando cambiar a (mantener prop): " + nuevoEstadoDeseadoMantenerProporciones);
        
        // 4. Llamar al método en VisorController para que aplique la lógica
        //    (actualizar modelo, config, y sincronizar toda la UI relacionada, incluyendo
        //    el SELECTED_KEY de esta Action y la apariencia del botón).
        controllerRef.setMantenerProporcionesLogicaYUi(nuevoEstadoDeseadoMantenerProporciones);

        // No es necesario actualizar Action.SELECTED_KEY aquí directamente.
        // VisorController.setMantenerProporcionesLogicaYUi(...)
        //   -> llama a VisorController.sincronizarUiControlesProporciones(...)
        //     -> llama a this.sincronizarSelectedKeyConModelo(...)
        // Esto asegura que el SELECTED_KEY de la Action se actualice después de que el modelo
        // haya sido modificado y sea la fuente de verdad.
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