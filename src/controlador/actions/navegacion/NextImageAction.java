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
 * Acción UNIFICADA y "tonta" para navegar a la siguiente imagen.
 * No contiene lógica de navegación. Su única responsabilidad es notificar
 * al controlador de modo activo (a través de la interfaz IModoController)
 * para que él decida qué hacer.
 * Implementa ContextSensitiveAction para que su estado (enabled/disabled)
 * pueda ser actualizado por el coordinador del modo activo.
 */
public class NextImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final IModoController modoController;

    /**
     * Constructor de la acción de navegación "siguiente".
     * @param modoController El controlador que implementa la lógica de navegación para el modo actual.
     * @param displayName El texto a mostrar.
     * @param icon El icono para el botón.
     */
    public NextImageAction(IModoController modoController, String displayName, ImageIcon icon) {
        super(displayName, icon);
        this.modoController = Objects.requireNonNull(modoController, "IModoController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Ir a la siguiente imagen (Siguiente)");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_NAV_SIGUIENTE);
        setEnabled(false);
    } // --- Fin del constructor NextImageAction ---

    /**
     * Se ejecuta cuando el usuario interactúa.
     * Delega la responsabilidad al controlador de modo activo.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // La acción es "tonta": solo le dice al controlador de modo "quieren ir al siguiente".
        // El GeneralController, que implementa IModoController, recibirá esta llamada.
        this.modoController.navegarSiguiente();
    } // --- Fin del método actionPerformed ---

    /**
     * Actualiza el estado 'enabled' de esta acción basándose en el modelo.
     * Este método será llamado por el ListCoordinator ACTIVO.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        if (model == null) {
            setEnabled(false);
            return;
        }

        // Obtiene el contexto de la lista del MODO DE TRABAJO ACTUAL.
        ListContext currentContext = model.getCurrentListContext();
        if (currentContext == null || currentContext.getModeloLista() == null || currentContext.getModeloLista().isEmpty()) {
            setEnabled(false);
            return;
        }

        int total = currentContext.getModeloLista().getSize();
        String selectedKey = currentContext.getSelectedImageKey();
        int currentIndex = (selectedKey != null) ? currentContext.getModeloLista().indexOf(selectedKey) : -1;

        // Se habilita si la navegación circular está activa o si no estamos en el último elemento.
        boolean isEnabled = model.isNavegacionCircularActivada() || (currentIndex < total - 1);
        setEnabled(isEnabled);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase NextImageAction ---