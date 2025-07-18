package controlador.actions.displaymode; 
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.util.Objects;

import controlador.GeneralController; // Necesitamos acceso al GeneralController
import modelo.VisorModel.DisplayMode; // Necesitamos el enum DisplayMode

/**
 * Acción para cambiar el modo de visualización de contenido (DisplayMode) de la aplicación.
 * Esta acción es responsable de actualizar el modelo y delegar al GeneralController
 * para que orqueste los cambios en la UI.
 */
public class SwitchDisplayModeAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final GeneralController generalController;
    private final DisplayMode targetDisplayMode;

    /**
     * Constructor.
     * @param generalController La instancia del GeneralController para delegar el cambio de modo.
     * @param targetDisplayMode El DisplayMode al que cambiar cuando se ejecute esta acción.
     * @param name El nombre a mostrar para la acción (ej. "Vista Imagen Única").
     * @param icon Icono opcional para la acción.
     * @param tooltip Texto de ayuda (tooltip) para la acción.
     * @param mnemonic Mnemotécnico opcional para la acción.
     * @param smallIconName Nombre del icono pequeño para el Action.SMALL_ICON.
     */
    public SwitchDisplayModeAction(
            GeneralController generalController,
            DisplayMode targetDisplayMode,
            String name,
            javax.swing.Icon icon,
            String tooltip,
            Integer mnemonic,
            String smallIconName // Si tu ActionFactory usa esto para cargar iconos
    ) {
        super(name, icon); // Llama al constructor de AbstractAction

        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null.");
        this.targetDisplayMode = Objects.requireNonNull(targetDisplayMode, "Target DisplayMode no puede ser null.");

        // Configurar propiedades adicionales de la Action
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        if (mnemonic != null) {
            putValue(Action.MNEMONIC_KEY, mnemonic);
        }
        // Asignar el comando canónico para esta acción (puede ser usado para buscarla en el actionMap)
        // Puedes definir un comando específico para cada DisplayMode en AppActionCommands.
        // Por ejemplo, AppActionCommands.CMD_VISTA_SINGLE, CMD_VISTA_GRID, etc.
        putValue(Action.ACTION_COMMAND_KEY, targetDisplayMode.name()); // Usamos el nombre del enum como comando por simplicidad.
                                                                        // Preferiblemente usaría AppActionCommands.CMD_VISTA_SINGLE, etc.
        
        // Propiedad para que el JRadioButtonMenuItem o JToggleButton pueda ser "seleccionado"
        putValue(Action.SELECTED_KEY, false); // Estado inicial: no seleccionado
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[SwitchDisplayModeAction] Acción disparada para cambiar a: " + targetDisplayMode);
        // Delegar la lógica real del cambio de DisplayMode al GeneralController.
        generalController.cambiarDisplayMode(targetDisplayMode);
    }
    
    /**
     * Sincroniza el estado de selección de esta acción con el DisplayMode actual del modelo.
     * Es crucial para que los JRadioButtonMenuItem y JToggleButton reflejen el modo activo.
     */
    public void sincronizarEstadoSeleccionConModelo(DisplayMode currentModelDisplayMode) {
        boolean isSelected = (this.targetDisplayMode == currentModelDisplayMode);
        if (Boolean.TRUE.equals(getValue(Action.SELECTED_KEY)) != isSelected) {
            putValue(Action.SELECTED_KEY, isSelected);
            System.out.println("  [SwitchDisplayModeAction] Sincronizada acción '" + getValue(Action.NAME) + "'. Seleccionado: " + isSelected);
        }
    }
    
} // --- FIN DE LA CLASE SwitchDisplayModeAction ---