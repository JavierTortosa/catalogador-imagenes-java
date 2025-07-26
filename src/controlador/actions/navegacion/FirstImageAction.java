package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.interfaces.IModoController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.ListContext;
import modelo.VisorModel;

/**
 * Acción UNIFICADA y "tonta" para navegar a la primera imagen.
 * Notifica al controlador de modo activo para que ejecute la lógica.
 */
public class FirstImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final IModoController modoController;

    /**
     * Constructor de la acción de navegación "primera".
     * @param modoController El controlador que implementa la lógica de navegación.
     * @param displayName El texto a mostrar.
     * @param icon El icono para el botón.
     */
    public FirstImageAction(IModoController modoController, String displayName, ImageIcon icon) {
        super(displayName, icon);
        this.modoController = Objects.requireNonNull(modoController, "IModoController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Ir a la primera imagen (Inicio)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_PRIMERA);
        setEnabled(false);
    } // --- Fin del constructor FirstImageAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        this.modoController.navegarPrimero();
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel model) {
        if (model == null) {
            setEnabled(false);
            return;
        }

        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null || currentContext.getModeloLista() == null || currentContext.getModeloLista().isEmpty()) {
            setEnabled(false);
            return;
        }

        String selectedKey = currentContext.getSelectedImageKey();
        int currentIndex = (selectedKey != null) ? currentContext.getModeloLista().indexOf(selectedKey) : -1;
        
        // Se habilita si hay imágenes y no estamos ya en el primer elemento.
        boolean isEnabled = currentIndex > 0;
        setEnabled(isEnabled);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase FirstImageAction ---