package controlador.actions.vista; // O el paquete donde la tengas

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem; // Para determinar el estado si la fuente es un checkbox
// No necesitas importar JButton aquí si la lógica de toggle es la misma

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.commands.AppActionCommands; // Asegúrate de tener los comandos

// No necesita IconUtils si no tiene icono propio en la Action

public class ToggleCheckeredBackgroundAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L; // Actualiza si cambias la clase

    /**
     * Constructor para la acción de activar/desactivar el fondo a cuadros.
     *
     * @param controller La instancia del VisorController.
     */
    public ToggleCheckeredBackgroundAction(VisorController controller) {
        // 1. Llamar al constructor de la superclase (BaseVisorAction)
        super("fondo_a_cuadros", controller);

        // 2. Establecer propiedades de la Action
        // 2.1. Descripción corta (Tooltip)
        putValue(Action.SHORT_DESCRIPTION, "Mostrar/Ocultar fondo a cuadros detrás de la imagen");

        // 2.2. Comando Canónico (ACTION_COMMAND_KEY)
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_VISTA_TOGGLE_CHECKERED_BG);
    }

    /**
     * Se ejecuta cuando el usuario interactúa con un componente UI asociado a esta Action
     * (ej. JCheckBoxMenuItem "Fondo a Cuadros").
     *
     * @param e El ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // --- 1. Validar Controller y Loguear ---
        if (controller == null) {
            System.err.println("Error: Controller es null en ToggleCheckeredBackgroundAction. No se puede ejecutar la acción.");
            return;
        }
        controller.logActionInfo(e); // Loguear la acción

        // --- 2. Determinar el Nuevo Estado Deseado ---
        //    Para una Action de tipo toggle (como esta, que se usa con JCheckBoxMenuItem),
        //    el nuevo estado es simplemente la inversión del estado actual de SELECTED_KEY.
        //    Si la fuente fuera un JCheckBoxMenuItem, también podríamos leer su estado,
        //    pero invertir el SELECTED_KEY de la Action es más genérico y robusto.

        boolean nuevoEstado;
        Object source = e.getSource();

        if (source instanceof JCheckBoxMenuItem) {
            // Si la fuente es un JCheckBoxMenuItem, su estado 'selected' ES el nuevo estado.
            nuevoEstado = ((JCheckBoxMenuItem) source).isSelected();
            System.out.println("  [ToggleCheckeredBGAction] Evento desde JCheckBoxMenuItem. Nuevo estado (isSelected): " + nuevoEstado);
        } else {
            // Si la fuente es otra (ej. un botón de toggle que no actualiza SELECTED_KEY antes de llamar),
            // o para un comportamiento de toggle general, invertimos el estado actual de la Action.
            Object valorSeleccionadoActual = getValue(Action.SELECTED_KEY);
            boolean estadoActual = (valorSeleccionadoActual instanceof Boolean && (Boolean) valorSeleccionadoActual);
            nuevoEstado = !estadoActual;
            System.out.println("  [ToggleCheckeredBGAction] Evento desde fuente desconocida o botón. Estado Action actual: "
                               + estadoActual + ". Nuevo estado deseado: " + nuevoEstado);
        }

        // --- 3. Llamar al Método del Controller para Aplicar el Cambio ---
        controller.setComponenteVisibleAndUpdateConfig("fondo_a_cuadros", nuevoEstado);
        // Nota: "Fondo_a_Cuadros" es la clave que tu método setComponenteVisibleAndUpdateConfig espera.
        // Este método en el controller también debería actualizar la configuración.

        // --- 4. Actualizar el Estado SELECTED_KEY de Esta Action ---
        //    Esto asegura que cualquier otro componente vinculado a esta Action refleje el nuevo estado.
        //    Si setComponenteVisibleAndUpdateConfig ya actualiza la config y esta Action
        //    lee su estado inicial de la config, esta línea podría ser redundante en el
        //    siguiente arranque, pero es buena para la consistencia inmediata de la UI.
        putValue(Action.SELECTED_KEY, nuevoEstado);
        System.out.println("  [ToggleCheckeredBGAction] Estado Action.SELECTED_KEY actualizado a: " + getValue(Action.SELECTED_KEY));
    }
}

