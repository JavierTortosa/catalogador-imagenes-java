// EN: controlador.actions.zoom.ZoomFixedAction.java

package controlador.actions.zoom;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import controlador.managers.interfaces.IZoomManager;
import servicios.zoom.ZoomModeEnum;

public class ZoomFixedAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    private final IZoomManager zoomManager;

    /**
     * Constructor para la acción que activa el modo "Zoom Fijo".
     * @param controller El controlador principal.
     * @param zoomManager El gestor de zoom.
     * @param icon El icono para el botón/menú.
     */
    public ZoomFixedAction(VisorController controller, IZoomManager zoomManager, ImageIcon icon) {
        // Llamamos al constructor de la clase base.
        super("Zoom Fijo", controller);
        
        // Asignamos las dependencias.
        this.zoomManager = Objects.requireNonNull(zoomManager, "ZoomManager no puede ser nulo");
        
        // Configuramos las propiedades de la Action.
        putValue(Action.SMALL_ICON, icon);
        putValue(Action.SHORT_DESCRIPTION, "Fija el nivel de zoom actual para las siguientes imágenes");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null && zoomManager != null) {
            // 1. Llama al ZoomManager para aplicar la lógica del modo "Zoom Fijo".
            zoomManager.aplicarModoDeZoom(ZoomModeEnum.MAINTAIN_CURRENT_ZOOM);
            
            // 2. Llama al Controller para que sincronice toda la UI de zoom.
            controller.sincronizarEstadoVisualBotonesYRadiosZoom();
        } else {
            System.err.println("Error: Controller o ZoomManager son nulos en ZoomFixedAction");
        }
    }
} // --- fin de la clase ZoomFixedAction ---