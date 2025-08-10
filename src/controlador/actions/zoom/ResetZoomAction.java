package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IZoomManager;

public class ResetZoomAction extends AbstractAction {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IZoomManager zoomManager;

    public ResetZoomAction(String name, ImageIcon icon, IZoomManager zoomManager) {
        super(name, icon);
        this.zoomManager = Objects.requireNonNull(zoomManager);
        putValue(Action.SHORT_DESCRIPTION, "Restablecer zoom y paneo para el modo actual");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_ZOOM_RESET);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	zoomManager.resetZoom();
    }
    
} // --- FIN de la clase ResetZoomAction ---

