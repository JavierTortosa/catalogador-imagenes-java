package controlador.tools;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta cuentagotas.
 * <p>
 * Captura el color del píxel (o el promedio de la muestra 1×1, 3×3, 5×5) bajo
 * el cursor en la capa activa y lo almacena como color frontal accesible desde
 * el contexto. Lee el tamaño de muestra de la barra de opciones (Parte B).
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

        int sample = ctx.componentBar().getEyedropperSampleSize();
        pickedColor = sampleAverage(img, mx, my, sample);

        // Store as client property on canvas for other tools to read
        ctx.canvasPanel().putClientProperty("foregroundColor", pickedColor);
        // Mostrar la muestra en la barra de opciones (swatch + RGB)
        ctx.componentBar().setEyedropperColor(pickedColor);
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    /**
     * Promedia el color de la región de muestra centrada en (mx, my).
     */
    private static Color sampleAverage(BufferedImage img, int mx, int my, int sample) {
        int radius = Math.max(0, (sample - 1) / 2);
        long r = 0, g = 0, b = 0, a = 0;
        int count = 0;
        for (int y = Math.max(0, my - radius); y <= Math.min(img.getHeight() - 1, my + radius); y++) {
            for (int x = Math.max(0, mx - radius); x <= Math.min(img.getWidth() - 1, mx + radius); x++) {
                int rgb = img.getRGB(x, y);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                a += (rgb >> 24) & 0xFF;
                count++;
            }
        }
        if (count == 0) return Color.BLACK;
        return new Color((int) (r / count), (int) (g / count), (int) (b / count), (int) (a / count));
    } // --- Fin del metodo sampleAverage ---

    /**
     * @return el último color capturado, o null
     */
    public Color getPickedColor() {
        return pickedColor;
    } // --- Fin del metodo getPickedColor ---

} // --- Fin de la clase ColorPickerTool ---
