package controlador.actions.config; // O el paquete que prefieras para este tipo de action

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // O FileOperationsManager

public class SetSubfolderReadModeAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private VisorController controller; // O private FileOperationsManager fileOpsManager;
    private boolean modoIncluirSubcarpetasAlQueEstablece;

    public SetSubfolderReadModeAction(
            String name, 
            ImageIcon icon, 
            VisorController controller, // Cambiar a FileOperationsManager si la lógica se mueve
            boolean modoIncluirSubcarpetasAlQueEstablece,
            String commandKey
    ) {
        super(name, icon);
        this.controller = controller; // O this.fileOpsManager = fileOpsManager;
        this.modoIncluirSubcarpetasAlQueEstablece = modoIncluirSubcarpetasAlQueEstablece;
        
        putValue(Action.ACTION_COMMAND_KEY, commandKey);
        // El estado SELECTED_KEY será manejado por el VisorController
        // al sincronizar todos los controles relacionados con esta configuración.
        // Similar a como se hace con ToggleThemeAction.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[" + getClass().getSimpleName() + "] ejecutada. Estableciendo modo incluir subcarpetas a: " + modoIncluirSubcarpetasAlQueEstablece);
        
        if (controller != null) {
            controller.setMostrarSubcarpetasAndUpdateConfig(this.modoIncluirSubcarpetasAlQueEstablece);
        } 
        else {
            System.err.println("ERROR CRÍTICO [" + getClass().getSimpleName() + "]: Controller (o FileOpsManager) es nulo.");
        }
    }

    /**
     * Método para ser llamado por el VisorController para sincronizar
     * el estado SELECTED_KEY de esta Action con el estado actual del modelo.
     * @param incluirSubcarpetasActualEstadoModelo El estado actual de si se deben incluir subcarpetas en el modelo.
     */
    public void sincronizarSelectedKey(boolean incluirSubcarpetasActualEstadoModelo) {
        boolean deberiaEstarSeleccionada = (this.modoIncluirSubcarpetasAlQueEstablece == incluirSubcarpetasActualEstadoModelo);
        if (!Objects.equals(getValue(Action.SELECTED_KEY), deberiaEstarSeleccionada)) {
            putValue(Action.SELECTED_KEY, deberiaEstarSeleccionada);
        }
    }
}