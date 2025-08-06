package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import controlador.GeneralController;
// import controlador.VisorController; // <-- ELIMINAR ESTE IMPORT
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

public class ToggleSubfoldersAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;
    // private final VisorController controller; // <-- ELIMINAR ESTA LÍNEA
    private final GeneralController generalController;

    public ToggleSubfoldersAction(String name, ImageIcon icon, VisorModel model, /*VisorController controller,*/ GeneralController generalController) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        // this.controller = Objects.requireNonNull(controller); // <-- ELIMINAR ESTA LÍNEA
        this.generalController = Objects.requireNonNull(generalController);
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (model.isSyncVisualizadorCarrusel() && (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR || model.getCurrentWorkMode() == WorkMode.CARROUSEL)) {
            String mensaje = "<html>La <b>Sincronización</b> está activa.<br>"
                           + "Cambiar la carga de subcarpetas recargará la lista y desactivará la sincronización.<br><br>"
                           + "¿Desea continuar?</html>";
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Desactivar Sincronización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (respuesta != JOptionPane.YES_OPTION) {
                generalController.sincronizarTodaLaUIConElModelo();
                return;
            }

            // --- INICIO DE LA MODIFICACIÓN ---
            // 1. Actualizamos el modelo.
            model.setSyncVisualizadorCarrusel(false);
            
            // 2. ¡LA LÍNEA CLAVE QUE FALTABA!
            //    Inmediatamente después de cambiar el modelo, forzamos una sincronización
            //    de la UI para que el botón de Sync se "apague" visualmente.
            generalController.notificarAccionesSensiblesAlContexto();
            // --- FIN DE LA MODIFICACIÓN ---
        }

        // La ejecución continúa hacia el método orquestador
        generalController.solicitarToggleModoCargaSubcarpetas();
    } // --- Fin del método actionPerformed ---
    
    
    
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // --- Diálogo de confirmación para el Modo Sync (CORREGIDO) ---
//        if (model.isSyncVisualizadorCarrusel() && (model.getCurrentWorkMode() == WorkMode.VISUALIZADOR || model.getCurrentWorkMode() == WorkMode.CARROUSEL)) {
//            String mensaje = "<html>La <b>Sincronización</b> está activa.<br>"
//                           + "Cambiar la carga de subcarpetas recargará la lista y desactivará la sincronización.<br><br>"
//                           + "¿Desea continuar?</html>";
//            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Desactivar Sincronización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
//            if (respuesta != JOptionPane.YES_OPTION) {
//                generalController.sincronizarTodaLaUIConElModelo(); // Revertir visualmente el botón
//                return;
//            }
//            model.setSyncVisualizadorCarrusel(false); // Desactivar y continuar
//        }
//
//        // --- INICIO DE LA MODIFICACIÓN CLAVE ---
//        // La acción ya no calcula el estado. Simplemente ordena al GeneralController que "invierta el estado actual".
//        generalController.solicitarToggleModoCargaSubcarpetas();
//        // --- FIN DE LA MODIFICACIÓN CLAVE ---
//    } // --- Fin del método actionPerformed ---


}