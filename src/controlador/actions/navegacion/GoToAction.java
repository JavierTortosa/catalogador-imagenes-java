package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.ListContext;
import modelo.VisorModel;

/**
 * Acción para ir a una imagen específica por su índice en la lista.
 */
public class GoToAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final GeneralController generalController;
    private final VisorModel model;


    /**
     * Constructor para GoToAction.
     *
     * @param name              El nombre de la acción.
     * @param icon              El icono de la acción.
     * @param generalController El controlador general.
     * @param model             El modelo del visor.
     */
    public GoToAction(String name, ImageIcon icon, GeneralController generalController, VisorModel model) {
        super(name, icon);
        this.generalController = Objects.requireNonNull(generalController);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Ir a una imagen específica por número");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_IR_A);
    } // --- Fin del método GoToAction ---


    /**
     * Ejecuta la acción de ir a una imagen específica.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        ListContext context = model.getCurrentListContext();
        if (context == null || context.getModeloLista() == null) return;

        int total = context.getModeloLista().getSize();
        if (total <= 0) return;

        String input = JOptionPane.showInputDialog(null,
                "Ir a imagen (1 - " + total + "):",
                "Ir a...", JOptionPane.QUESTION_MESSAGE);

        if (input == null) return;

        try {
            int target = Integer.parseInt(input.trim());
            if (target < 1 || target > total) {
                JOptionPane.showMessageDialog(null,
                        "El número debe estar entre 1 y " + total + ".",
                        "Número inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            generalController.getVisorController()
                    .getListCoordinator().seleccionarImagenPorIndice(target - 1);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    "Ingrese un número válido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en si hay una lista con elementos.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        ListContext context = model.getCurrentListContext();
        boolean enabled = context != null && context.getModeloLista() != null
                && !context.getModeloLista().isEmpty();
        setEnabled(enabled);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase GoToAction ---
