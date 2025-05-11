package controlador.actions.projects; // Ajusta el paquete

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem; // Para la lógica del actionPerformed

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.commands.AppActionCommands;
import vista.util.IconUtils;

public class ToggleMarkImageAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public ToggleMarkImageAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        super("Marcar/Desmarcar para Proyecto", controller);
        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        putValue(Action.SELECTED_KEY, Boolean.FALSE); // Estado inicial

        if (iconUtils != null) {
            ImageIcon icon = iconUtils.getScaledIcon("5003-marcar_imagen_48x48.png", iconoAncho, iconoAlto);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            } else {
                System.err.println("WARN [ToggleMarkImageAction]: No se pudo cargar el icono '5003-marcar_imagen_48x48.png'");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { /* ... error ... */ return; }
        controller.logActionInfo(e);

        // Determinar el nuevo estado lógico DESEADO
        boolean estadoLogicoDeseado;
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            // Si es un JCheckBoxMenuItem, su estado isSelected() YA refleja el nuevo estado
            estadoLogicoDeseado = ((JCheckBoxMenuItem) source).isSelected();
        } else {
            // Para otros componentes (como un JButton que actúa como toggle),
            // invertimos el estado actual de la Action.
            boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
            estadoLogicoDeseado = !estadoActualAction;
        }
        
        // Llamar al método del controller que ahora toma el Path absoluto
        controller.toggleMarcaImagenActual(estadoLogicoDeseado);
        // El controller se encargará de llamar a ProjectManager y de actualizar el SELECTED_KEY de esta Action.
    }
}

//package controlador.actions.projects; // O el paquete donde la tengas
//
//import java.awt.event.ActionEvent;
//
//import javax.swing.Action;
//import javax.swing.ImageIcon; // Solo si vas a poner icono en la Action directamente
//import javax.swing.JButton;
//import javax.swing.JCheckBoxMenuItem;
//
//import controlador.VisorController;
//import controlador.actions.BaseVisorAction;
//import controlador.commands.AppActionCommands;
//import servicios.ProjectManager; // Necesitarás esta importación
//import vista.util.IconUtils;   // Solo si vas a poner icono en la Action directamente
//
//public class ToggleMarkImageAction extends BaseVisorAction {
//
//    private static final long serialVersionUID = 1L; // Considera generar uno nuevo
//
//    // Constructor
//    public ToggleMarkImageAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
//        // El texto "Marcar/Desmarcar para Proyecto" se usará para Action.NAME
//        super("Marcar/Desmarcar para Proyecto", controller);
//
//        // Establecer propiedades de la Action
//        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
//
//        // Asignar icono (este icono será usado por defecto si el componente UI no tiene uno propio)
//        // El ToolbarButtonDefinition puede especificar un icono diferente que lo sobrescribirá para ese botón.
//        if (iconUtils != null) {
//            // Asegúrate que "5003-marcar_imagen_48x48.png" es el nombre correcto y está en la ruta de IconUtils
//            ImageIcon icon = iconUtils.getScaledIcon("5003-marcar_imagen_48x48.png", iconoAncho, iconoAlto);
//            if (icon != null) {
//                putValue(Action.SMALL_ICON, icon);
//            } else {
//                System.err.println("WARN [ToggleMarkImageAction]: No se pudo cargar el icono '5003-marcar_imagen_48x48.png'");
//            }
//        }
//
//        // Inicialmente, ninguna imagen está marcada, por lo que el estado "seleccionado" de la Action es false.
//        // Esto se actualizará dinámicamente cuando cambie la imagen principal.
//        putValue(Action.SELECTED_KEY, Boolean.FALSE);
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // ... (validaciones iniciales) ...
//        Object source = e.getSource();
//        boolean nuevoEstadoLogico;
//
//        if (source instanceof JCheckBoxMenuItem) {
//            // Para un JCheckBoxMenuItem, su estado isSelected() YA refleja el nuevo estado después del clic.
//            nuevoEstadoLogico = ((JCheckBoxMenuItem) source).isSelected();
//            System.out.println("  [ToggleMarkImageAction] Evento desde JCheckBoxMenuItem. Estado isSelected() del item: " + nuevoEstadoLogico);
//        } else if (source instanceof JButton) {
//            // Para un JButton que actúa como toggle, invertimos el estado actual de la Action
//            boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
//            nuevoEstadoLogico = !estadoActualAction;
//            System.out.println("  [ToggleMarkImageAction] Evento desde JButton. Estado Action actual: " + estadoActualAction + ". Nuevo estado lógico: " + nuevoEstadoLogico);
//        } else {
//            // Fuente desconocida, comportamiento de toggle simple
//            boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
//            nuevoEstadoLogico = !estadoActualAction;
//            System.out.println("  [ToggleMarkImageAction] Evento desde fuente desconocida. Estado Action actual: " + estadoActualAction + ". Nuevo estado lógico: " + nuevoEstadoLogico);
//        }
//
//        String selectedImageKey = controller.getModel().getSelectedImageKey();
//        if (selectedImageKey == null || selectedImageKey.isEmpty()) {
//            System.out.println("[ToggleMarkImageAction] No hay imagen seleccionada para marcar/desmarcar.");
//            // IMPORTANTE: Si no hay imagen, no deberíamos cambiar el estado de marca ni el SELECTED_KEY
//            // Podríamos incluso revertir el SELECTED_KEY si la fuente era un JCheckBoxMenuItem
//            // que se acaba de marcar sin haber imagen seleccionada.
//            // O, mejor aún, la Action debería estar deshabilitada (setEnabled(false)) si no hay imagen.
//            // Por ahora, simplemente no hacemos nada con el ProjectManager ni con el SELECTED_KEY.
//            return;
//        }
//
//        ProjectManager pm = controller.getProjectManager();
//        if (pm == null) { /* ... error ... */ return; }
//
//        // Llamar al ProjectManager con el nuevoEstadoLogico determinado
//        pm.marcarDesmarcarImagenActual(selectedImageKey, nuevoEstadoLogico);
//
//        // Actualizar el estado SELECTED_KEY de ESTA Action para que coincida con el nuevoEstadoLogico
//        putValue(Action.SELECTED_KEY, nuevoEstadoLogico); // <--- ESTO ES LO QUE SINCRONIZA LA ACTION
//        System.out.println("  [ToggleMarkImageAction] Estado Action.SELECTED_KEY (ID: " + System.identityHashCode(this) + ") actualizado a: " + nuevoEstadoLogico);
//
//        // Notificar al controller para que actualice otras partes de la UI
//        controller.actualizarEstadoVisualBotonMarcarYBarraEstado(nuevoEstadoLogico);
//
//        // Log de estado visual del JCheckBoxMenuItem DESPUÉS de todo
//        if (source instanceof JCheckBoxMenuItem) {
//            System.out.println("  [ToggleMarkImageAction] Estado VISUAL del JCheckBoxMenuItem DESPUÉS de putValue: " + ((JCheckBoxMenuItem) source).isSelected());
//        }
//    }    
//}