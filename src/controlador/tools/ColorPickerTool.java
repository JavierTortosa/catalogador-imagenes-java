package controlador.tools;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta cuentagotas.
 * <p>
 * Captura el color del píxel bajo el cursor en la capa activa y lo almacena
 * como color frontal accesible desde el contexto.
 */
public class ColorPickerTool extends Tool {

    private Color pickedColor;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_CUENTAGOTAS;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        ImageLayer layer = getActiveLayer();
        if (layer == null) return;
        BufferedImage img = layer.getImage();
        if (img == null) return;

        int mx = e.getX() - layer.getBounds().x;
        int my = e.getY() - layer.getBounds().y;
        if (mx < 0 || my < 0 || mx >= img.getWidth() || my >= img.getHeight()) return;

        int rgb = img.getRGB(mx, my);
        pickedColor = new Color(rgb, true);

        // Store as client property on canvas for other tools to read
        ctx.canvasPanel().putClientProperty("foregroundColor", pickedColor);
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    /**
     * @return el último color capturado, o null
     */
    public Color getPickedColor() {
        return pickedColor;
    } // --- Fin del metodo getPickedColor ---

} // --- Fin de la clase ColorPickerTool ---
