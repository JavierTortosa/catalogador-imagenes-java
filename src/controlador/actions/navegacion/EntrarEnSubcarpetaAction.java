package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.GeneralController;
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;

public class EntrarEnSubcarpetaAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
	private final GeneralController generalController;

    public EntrarEnSubcarpetaAction(GeneralController generalController, String text, ImageIcon icon) {
        super(text, icon);
        this.generalController = generalController;
        putValue(Action.SHORT_DESCRIPTION, "Ir a la carpeta que contiene la imagen seleccionada");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ORDEN_CARPETA_SIGUIENTE);
    } // --- Fin del método EntrarEnSubcarpetaAction (constructor) ---

    @Override
    public void actionPerformed(ActionEvent e) {
        if (generalController != null) {
            generalController.solicitarNavegarCarpetaSiguiente();
        }
    } // --- Fin del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel model) {
        boolean isEnabled = false;
        String selectedKey = model.getSelectedImageKey();
        Path carpetaRaizContexto = model.getCarpetaRaizActual();

        if (selectedKey != null && carpetaRaizContexto != null) {
            Path imagePath = model.getRutaCompleta(selectedKey);
            if (imagePath != null) {
                Path carpetaContenedoraImagen = imagePath.getParent();
                if (carpetaContenedoraImagen != null) {
                    // El botón se activa si la carpeta raíz del contexto NO es igual a la carpeta de la imagen
                    isEnabled = !carpetaRaizContexto.equals(carpetaContenedoraImagen);
                }
            }
        }
        setEnabled(isEnabled);
        
    } // --- Fin del método updateEnabledState ---
    
    
} // --- FIN DE LA CLASE EntrarEnSubcarpetaAction ---