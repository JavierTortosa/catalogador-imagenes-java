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
 * Acción UNIFICADA y "tonta" para navegar a la última imagen.
 * Notifica al controlador de modo activo para que ejecute la lógica.
 */
public class LastImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final IModoController modoController;

    /**
     * Constructor de la acción de navegación "última".
     * @param modoController El controlador que implementa la lógica de navegación.
     * @param displayName El texto a mostrar.
     * @param icon El icono para el botón.
     */
    public LastImageAction(IModoController modoController, String displayName, ImageIcon icon) {
        super(displayName, icon);
        this.modoController = Objects.requireNonNull(modoController, "IModoController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Ir a la última imagen (Fin)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_ULTIMA);
        setEnabled(false);
    } // --- Fin del constructor LastImageAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        this.modoController.navegarUltimo();
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

        int total = currentContext.getModeloLista().getSize();
        String selectedKey = currentContext.getSelectedImageKey();
        int currentIndex = (selectedKey != null) ? currentContext.getModeloLista().indexOf(selectedKey) : -1;
        
        // Se habilita si hay imágenes y no estamos ya en el último elemento.
        boolean isEnabled = currentIndex != -1 && currentIndex < total - 1;
        setEnabled(isEnabled);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase LastImageAction ---