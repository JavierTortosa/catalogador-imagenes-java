package controlador.actions.projects; // Ajusta el paquete

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import vista.util.IconUtils;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.commands.AppActionCommands;
import servicios.ProjectManager; // Necesario

public class GestionarProyectoAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;

    public GestionarProyectoAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        super("Gestionar Proyecto", controller);
        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);

        if (iconUtils != null) {
            // Usa el icono que definiste para "mostrar favoritos/gestionar"
            ImageIcon icon = iconUtils.getScaledIcon("7003-Mostrar_Favoritos_48x48.png", iconoAncho, iconoAlto);
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            } else {
                System.err.println("WARN [GestionarProyectoAction]: No se pudo cargar el icono '7003-Mostrar_Favoritos_48x48.png'");
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) { /* ... error ... */ return; }
        controller.logActionInfo(e);
        ProjectManager pm = controller.getProjectManager();
        if (pm == null) { /* ... error ... */ return; }
        pm.gestionarSeleccionProyecto(controller.getView() != null ? controller.getView().getFrame() : null);
    }
}

//package controlador.actions.projects; // O el paquete donde la tengas
//
//import java.awt.event.ActionEvent;
//
//import javax.swing.Action;
//import javax.swing.ImageIcon;     // Para el icono
//import javax.swing.JOptionPane;
//
//import controlador.VisorController;
//import controlador.actions.BaseVisorAction;
//import controlador.commands.AppActionCommands;
//import servicios.ProjectManager; // Necesitarás esta importación
//import vista.util.IconUtils;      // Para cargar/escalar el icono
//
//public class GestionarProyectoAction extends BaseVisorAction {
//
//    private static final long serialVersionUID = 1L; // Considera generar uno nuevo
//
//    /**
//     * Constructor para la acción que gestiona (actualmente muestra un placeholder)
//     * la selección del proyecto.
//     *
//     * @param controller La instancia del VisorController.
//     * @param iconUtils  El servicio para cargar y escalar iconos.
//     * @param iconoAncho El ancho deseado para el icono de esta acción.
//     * @param iconoAlto  El alto deseado para el icono de esta acción.
//     */
//    public GestionarProyectoAction(VisorController controller, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
//        // Llama al constructor de BaseVisorAction.
//        // El texto "Gestionar Proyecto" se usará para Action.NAME si el componente UI
//        // no tiene un texto explícito o si se crea directamente desde esta Action.
//        super("Gestionar Proyecto", controller);
//
//        // Establecer propiedades de la Action
//        putValue(Action.SHORT_DESCRIPTION, "Gestionar la selección de imágenes del proyecto actual (ver, guardar, cargar, etc.)");
//        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_GESTIONAR);
//
//        // Asignar icono
//        // El icono "7003-Mostrar_Favoritos_48x48.png" se usa aquí como ejemplo,
//        // cámbialo si tienes un icono más apropiado para "Gestionar Proyecto".
//        if (iconUtils != null) {
//            ImageIcon icon = iconUtils.getScaledIcon("7003-Mostrar_Favoritos_48x48.png", iconoAncho, iconoAlto);
//            if (icon != null) {
//                putValue(Action.SMALL_ICON, icon);
//            } else {
//                System.err.println("WARN [GestionarProyectoAction]: No se pudo cargar el icono '7003-Mostrar_Favoritos_48x48.png'");
//            }
//        }
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // 1. Validar que el controller no sea null
//        if (controller == null) {
//            System.err.println("Error: Controller es null en GestionarProyectoAction. No se puede ejecutar la acción.");
//            return;
//        }
//
//        // 2. Loguear la información del evento
//        controller.logActionInfo(e);
//
//        // 3. Obtener la referencia al ProjectManager desde el controller
//        ProjectManager pm = controller.getProjectManager(); // Asume que tienes este getter en VisorController
//
//        // 4. Validar que el ProjectManager no sea null
//        if (pm == null) {
//            System.err.println("Error: ProjectManager es null en GestionarProyectoAction (obtenido del controller).");
//            // Opcionalmente, mostrar un error al usuario
//            JOptionPane.showMessageDialog(
//                (controller.getView() != null ? controller.getView().getFrame() : null),
//                "Error interno: El gestor de proyectos no está disponible.",
//                "Error",
//                JOptionPane.ERROR_MESSAGE
//            );
//            return;
//        }
//
//        // 5. Llamar al método del ProjectManager que muestra el JOptionPane placeholder
//        // Se pasa el frame de la vista como componente padre para el diálogo, si está disponible.
//        pm.gestionarSeleccionProyecto(controller.getView() != null ? controller.getView().getFrame() : null);
//    }
//}