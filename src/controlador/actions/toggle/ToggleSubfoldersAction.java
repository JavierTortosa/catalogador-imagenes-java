package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import controlador.GeneralController;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

public class ToggleSubfoldersAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;
    private final VisorController controller;
    private final GeneralController generalController;

    // Nota: el constructor ahora NO pide ConfigurationManager
    public ToggleSubfoldersAction(String name, ImageIcon icon, VisorModel model, VisorController controller, GeneralController generalController) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        this.controller = Objects.requireNonNull(controller);
        this.generalController = Objects.requireNonNull(generalController);
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_TOGGLE_SUBCARPETAS);
    } // --- Fin del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Si estamos en Carrusel, el usuario debe confirmar el cambio de modo
    	
        if (model.getCurrentWorkMode() == WorkMode.CARROUSEL) {
            String mensaje = "<html>Esta acción recargará la lista de imágenes.<br>"
                           + "Para ello, es necesario volver al modo Visualizador.<br><br>"
                           + "¿Desea continuar?</html>";
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Cambio de Modo Requerido", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                // El usuario canceló. Como el botón ya cambió visualmente, pedimos una resincronización para revertirlo.
                generalController.sincronizarTodaLaUIConElModelo();
                return;
            }
            // Cambiamos de modo. El resto de la lógica se ejecutará en la siguiente pulsación.
            generalController.cambiarModoDeTrabajo(WorkMode.VISUALIZADOR);
            return; // Salimos, la acción ya se completó (cambiar de modo).
        }

        // Si estamos en modo Visualizador y Sync está activo, advertimos.
        if (model.isSyncVisualizadorCarrusel()) {
            String mensaje = "<html>La <b>Sincronización</b> está activa.<br>"
                           + "Cambiar la carga de subcarpetas recargará la lista y desactivará la sincronización.<br><br>"
                           + "¿Desea continuar?</html>";
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Desactivar Sincronización", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.YES_OPTION) {
                generalController.sincronizarTodaLaUIConElModelo(); // Revertir visualmente el botón
                return;
            }
            model.setSyncVisualizadorCarrusel(false);
        }

        // Determinamos el nuevo estado y llamamos al controlador para la lógica pesada.
        boolean nuevoEstado = !(!model.isMostrarSoloCarpetaActual());
        controller.setMostrarSubcarpetasLogicaYUi(nuevoEstado);
        
        // Al final de la acción, el GeneralController resincroniza toda la UI.
        generalController.sincronizarTodaLaUIConElModelo();
    } // --- Fin del método actionPerformed ---
} // --- Fin de la clase ToggleSubfoldersAction ---
