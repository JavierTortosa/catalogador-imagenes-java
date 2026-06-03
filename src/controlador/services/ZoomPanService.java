package controlador.services;

import controlador.managers.interfaces.IViewManager;
import modelo.VisorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vista.components.Direction;
import vista.panels.ImageDisplayPanel;

import java.awt.image.BufferedImage;

/**
 * Servicio encargado de la lógica de paneo (movimiento) de la imagen
 * y la coordinación del zoom entre el modelo y la vista.
 */
public class ZoomPanService {
    private static final Logger logger = LoggerFactory.getLogger(ZoomPanService.class);

    private final VisorModel model;
    private final IViewManager viewManager;

    public ZoomPanService(VisorModel model, IViewManager viewManager) {
        this.model = model;
        this.viewManager = viewManager;
    }

    public void panToEdge(Direction direction) {
        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();
        if (displayPanel == null || model.getCurrentImage() == null) return;

        BufferedImage img = model.getCurrentImage();
        double zoom = model.getZoomFactor();
        int imageScaledWidth = (int) (img.getWidth() * zoom);
        int imageScaledHeight = (int) (img.getHeight() * zoom);

        int panelWidth = displayPanel.getWidth();
        int panelHeight = displayPanel.getHeight();

        double xBaseCentered = (double) (panelWidth - imageScaledWidth) / 2;
        double yBaseCentered = (double) (panelHeight - imageScaledHeight) / 2;

        int newOffsetX = 0;
        int newOffsetY = 0;

        switch (direction) {
            case UP -> { if (imageScaledHeight > panelHeight) newOffsetY = (int) -yBaseCentered; }
            case DOWN -> { if (imageScaledHeight > panelHeight) newOffsetY = (int) (panelHeight - imageScaledHeight - yBaseCentered); }
            case LEFT -> { if (imageScaledWidth > panelWidth) newOffsetX = (int) -xBaseCentered; }
            case RIGHT -> { if (imageScaledWidth > panelWidth) newOffsetX = (int) (panelWidth - imageScaledWidth - xBaseCentered); }
        }

        model.setImageOffsetX(newOffsetX);
        model.setImageOffsetY(newOffsetY);
        displayPanel.repaint();
    }

    @SuppressWarnings("incomplete-switch")
	public void panIncrementally(Direction direction, int amount) {
        ImageDisplayPanel displayPanel = viewManager.getActiveDisplayPanel();
        if (displayPanel == null || model.getCurrentImage() == null) return;

        int currentX = model.getImageOffsetX();
        int currentY = model.getImageOffsetY();

        switch (direction) {
            case UP -> model.setImageOffsetY(currentY - amount);
            case DOWN -> model.setImageOffsetY(currentY + amount);
            case LEFT -> model.setImageOffsetX(currentX - amount);
            case RIGHT -> model.setImageOffsetX(currentX + amount);
        }
        
        displayPanel.repaint();
    }
}