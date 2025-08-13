package controlador.actions.pan;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import vista.components.Direction;

/**
 * Action que encapsula una operación de paneo de imagen (absoluta o incremental).
 * Delega la lógica real de paneo a GeneralController.
 * Implementa ContextSensitiveAction para que su estado 'enabled' pueda ser
 * actualizado dinámicamente según el estado del modelo (modo paneo).
 */
public class PanAction extends AbstractAction implements ContextSensitiveAction { 
	
	private static final Logger logger = LoggerFactory.getLogger(PanAction.class);

    private static final long serialVersionUID = 1L;

    // Enum interno para diferenciar los tipos de paneo que puede representar esta Action
    public enum PanType {
        ABSOLUTE_EDGE,   // Paneo hasta el borde de la imagen (ej. Panear arriba del todo)
        INCREMENTAL      // Paneo por una cantidad fija de píxeles (ej. Panear 50px a la izquierda)
    }

    private final GeneralController generalController; // Referencia al GeneralController para ejecutar el paneo
    private final Direction direction;               // La dirección de paneo de esta Action
    private final PanType panType;                   // El tipo de paneo (absoluto o incremental)
    private final int amount;                        // Cantidad de píxeles, solo relevante para INCREMENTAL

    /**
     * Constructor para acciones de paneo absoluto (al borde).
     * Establece el nombre de la Action para el ToolTipText o el texto del menú.
     * @param generalController La instancia de GeneralController para delegar la acción.
     * @param command           El comando canónico asociado a esta acción (ej. AppActionCommands.CMD_PAN_TOP_EDGE).
     * @param direction         La dirección del paneo (UP, DOWN, LEFT, RIGHT).
     */
    public PanAction(GeneralController generalController, String command, Direction direction) {
        // Llama al constructor privado centralizado, pasando el PanType ABSOLUTE_EDGE y amount 0.
        this(generalController, command, direction, PanType.ABSOLUTE_EDGE, 0);
        // Establece el nombre visible de la Action.
        putValue(Action.NAME, "Panear a " + direction.name().toLowerCase() + " (Borde)");
    } // --- Fin del método PanAction (constructor absoluto) ---

    /**
     * Constructor para acciones de paneo incremental.
     * Establece el nombre de la Action para el ToolTipText o el texto del menú.
     * @param generalController La instancia de GeneralController para delegar la acción.
     * @param command           El comando canónico asociado a esta acción (ej. AppActionCommands.CMD_PAN_UP_INCREMENTAL).
     * @param direction         La dirección del paneo (UP, DOWN, LEFT, RIGHT).
     * @param amount            La cantidad de píxeles a mover en cada paso (para paneo incremental).
     */
    public PanAction(GeneralController generalController, String command, Direction direction, int amount) {
        // Llama al constructor privado centralizado, pasando el PanType INCREMENTAL y la cantidad.
        this(generalController, command, direction, PanType.INCREMENTAL, amount);
        // Establece el nombre visible de la Action.
        putValue(Action.NAME, "Panear " + direction.name().toLowerCase() + " (" + amount + "px)");
    } // --- Fin del método PanAction (constructor incremental) ---

    /**
     * Constructor privado centralizado para inicializar todos los campos finales.
     * Realiza las validaciones de nulidad.
     */
    private PanAction(GeneralController generalController, String command, Direction direction, PanType panType, int amount) {
        // Se asegura de que las dependencias vitales no sean nulas.
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null.");
        this.direction = Objects.requireNonNull(direction, "La dirección de paneo no puede ser null.");
        this.panType = Objects.requireNonNull(panType, "El tipo de paneo no puede ser null.");
        this.amount = amount; // amount puede ser 0, no necesita validación de nulidad.
        
        // Asigna el comando canónico a la Action, necesario para su identificación.
        putValue(Action.ACTION_COMMAND_KEY, command);
        // Puedes añadir aquí otros valores de Action si lo necesitas (ej. ICON)
        // putValue(Action.SMALL_ICON, new ImageIcon("path/to/icon.png"));
    } // --- Fin del método PanAction (constructor privado) ---

    /**
     * Este método es llamado cuando la acción es disparada (ej. por un clic en el D-Pad).
     * Delega la lógica real de paneo al GeneralController basándose en el tipo de paneo.
     * @param e El evento de acción (ActionEvent).
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Loguea la ejecución de la acción para depuración.
        logger.debug("[PanAction] Ejecutando comando: " + e.getActionCommand() + 
                           " (Dir: " + direction + ", Tipo: " + panType + ")");
        
        // Delega la ejecución real de la lógica de paneo al GeneralController.
        if (generalController != null) {
            switch (panType) {
                case ABSOLUTE_EDGE:
                    generalController.panImageToEdge(direction);
                    break;
                case INCREMENTAL:
                    generalController.panImageIncrementally(direction, amount);
                    break;
                default:
                    // Esto no debería ocurrir si los constructores están bien definidos.
                    logger.warn("WARN [PanAction]: Tipo de paneo no reconocido: " + panType);
                    break;
            }
        } else {
            // Log de error si GeneralController no fue inyectado correctamente.
            logger.error("ERROR [PanAction]: GeneralController es null. No se puede ejecutar el paneo.");
        }
    } // --- Fin del método actionPerformed ---

    
    /**
     * Implementación del método de la interfaz ContextSensitiveAction.
     * Habilita o deshabilita esta acción de paneo basándose en si el modo de paneo
     * manual está activo en el modelo.
     * @param model El modelo de la aplicación a consultar.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        if (model != null) {
            // La acción de paneo solo está habilitada si el "zoom manual" (modo paneo) está activo.
            this.setEnabled(model.isZoomHabilitado());
        } else {
            // Si el modelo es nulo, deshabilitar por seguridad.
            this.setEnabled(false);
        }
    } // --- Fin del método updateEnabledState ---
    

} // --- FIN de la clase PanAction ---