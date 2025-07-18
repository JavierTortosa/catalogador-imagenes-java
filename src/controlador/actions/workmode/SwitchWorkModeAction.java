package controlador.actions.workmode;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.util.Objects;

import controlador.GeneralController; // Necesitamos acceso al GeneralController
import modelo.VisorModel.WorkMode;     // Necesitamos el enum WorkMode

/**
 * Acción para cambiar el modo de trabajo (WorkMode) de la aplicación.
 * Esta acción es responsable de actualizar el modelo y delegar al GeneralController
 * para que orqueste los cambios en la UI y el comportamiento global.
 */
public class SwitchWorkModeAction extends AbstractAction {

    private final GeneralController generalController;
    private final WorkMode targetWorkMode;

    /**
     * Constructor.
     * @param generalController La instancia del GeneralController para delegar el cambio de modo.
     * @param targetWorkMode El WorkMode al que cambiar cuando se ejecute esta acción.
     * @param name El nombre a mostrar para la acción (ej. "Modo Carrusel").
     * @param icon Icono opcional para la acción.
     * @param tooltip Texto de ayuda (tooltip) para la acción.
     * @param mnemonic Mnemotécnico opcional para la acción.
     * @param smallIconName Nombre del icono pequeño para el Action.SMALL_ICON (si tu ActionFactory lo carga así).
     */
    public SwitchWorkModeAction(
            GeneralController generalController,
            WorkMode targetWorkMode,
            String name,
            javax.swing.Icon icon,
            String tooltip,
            Integer mnemonic,
            String smallIconName // Este parámetro no se usa directamente en esta clase, es para el ActionFactory
    ) {
        super(name, icon); // Llama al constructor de AbstractAction

        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null.");
        this.targetWorkMode = Objects.requireNonNull(targetWorkMode, "Target WorkMode no puede ser null.");

        // Configurar propiedades adicionales de la Action
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        if (mnemonic != null) {
            putValue(Action.MNEMONIC_KEY, mnemonic);
        }
        // Asignar el comando canónico (opcional, pero útil si se registra así)
        putValue(Action.ACTION_COMMAND_KEY, targetWorkMode.name()); 
        
        // Propiedad para que el JRadioButtonMenuItem o JToggleButton pueda ser "seleccionado"
        putValue(Action.SELECTED_KEY, false); // Estado inicial: no seleccionado
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[SwitchWorkModeAction] Acción disparada para cambiar a WorkMode: " + targetWorkMode);
        // Delegar la lógica real del cambio de WorkMode al GeneralController.
        generalController.cambiarModoDeTrabajo(targetWorkMode);
    }
    
    /**
     * Sincroniza el estado de selección de esta acción con el WorkMode actual del modelo.
     * Es crucial para que los JRadioButtonMenuItem y JToggleButton reflejen el modo activo.
     */
    public void sincronizarEstadoSeleccionConModelo(WorkMode currentModelWorkMode) {
        boolean isSelected = (this.targetWorkMode == currentModelWorkMode);
        if (Boolean.TRUE.equals(getValue(Action.SELECTED_KEY)) != isSelected) {
            putValue(Action.SELECTED_KEY, isSelected);
            System.out.println("  [SwitchWorkModeAction] Sincronizada acción '" + getValue(Action.NAME) + "'. Seleccionado: " + isSelected);
        }
    }
}