package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ToggleSyncAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(ToggleSyncAction.class);

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final VisorModel model;
    private final ConfigurationManager configuration;
    private final GeneralController generalController;

    public ToggleSyncAction(String text, ImageIcon icon, VisorModel model, ConfigurationManager configuration, GeneralController generalController) {
        super(text, icon);
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        putValue(Action.ACTION_COMMAND_KEY, controlador.commands.AppActionCommands.CMD_TOGGLE_SYNC_VISOR_CARRUSEL);
        sincronizarEstadoConModelo();
    } // --- Fin del constructor ToggleSyncAction ---

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean estadoActual = model.isSyncVisualizadorCarrusel();
        boolean estadoDeseado = !estadoActual;
        
        String titulo = estadoDeseado ? "Activar Sincronización" : "Desactivar Sincronización";
        String mensaje = estadoDeseado 
            ? "<html><b>¿Activar la sincronización entre el Visualizador y el Carrusel?</b><br><br>"
                + "<u>Activado:</u><br>"
                + "• Al entrar al Carrusel, se cargará la lista y la imagen del Visualizador.<br>"
                + "• Al volver al Visualizador, se mantendrá la última imagen vista en el Carrusel.<br><br>"
                + "<i>Esto es útil para usar el Carrusel como una vista automática de tu trabajo actual.</i></html>"
            : "<html><b>¿Desactivar la sincronización?</b><br><br>"
                + "<u>Desactivado:</u><br>"
                + "• El Carrusel y el Visualizador funcionarán de forma independiente.<br>"
                + "• Cada modo recordará su propia lista e imagen entre sesiones.<br><br>"
                + "<i>Esto es útil para revisar colecciones (ej. 'novedades') sin perder tu progreso en el Visualizador.</i></html>";

        int respuesta = JOptionPane.showConfirmDialog(null, mensaje, titulo, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            // El usuario confirmó: cambiamos el estado y notificamos a todos.
            logger.debug("[ToggleSyncAction] El usuario ha confirmado el cambio. Nuevo estado: " + (estadoDeseado ? "ACTIVADO" : "DESACTIVADO"));
            model.setSyncVisualizadorCarrusel(estadoDeseado);
            configuration.setString(ConfigKeys.COMPORTAMIENTO_SYNC_VISOR_CARRUSEL, String.valueOf(estadoDeseado));
            generalController.notificarAccionesSensiblesAlContexto();
        } else {
            // El usuario canceló: NO cambiamos el estado del modelo.
            System.out.println("[ToggleSyncAction] El usuario ha cancelado el cambio.");
            
            // ---> INICIO DE LA CORRECCIÓN CLAVE <---
            // Forzamos la actualización del estado 'SELECTED_KEY' de esta acción
            // para que vuelva a coincidir con el estado del modelo (que no ha cambiado).
            // Esto hará que el JToggleButton revierta su estado visual automáticamente.
            putValue(Action.SELECTED_KEY, model.isSyncVisualizadorCarrusel());
            // ---> FIN DE LA CORRECCIÓN CLAVE <---
        }
    } // --- Fin del método actionPerformed ---

    public void sincronizarEstadoConModelo() {
        putValue(Action.SELECTED_KEY, model.isSyncVisualizadorCarrusel());
    } // --- Fin del método sincronizarEstadoConModelo ---

} // --- Fin de la clase ToggleSyncAction ---