package controlador.tools;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;

/**
 * Herramienta recortar.
 * <p>
 * Requiere una selección activa. Ofrece dos modos: eliminar el contenido
 * seleccionado (dejando transparencia) o crear una nueva capa con el contenido
 * recortado.
 */
public class CropTool extends Tool {

    private boolean keepOriginal;

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public void mousePressed(MouseEvent e) {
        if (ctx.selectionModel() == null || !ctx.selectionModel().isActive()) return;
        ImageLayer layer = getActiveLayer();
        if (layer == null) return;

        Rectangle sel = ctx.selectionModel().getBounds();
        BufferedImage img = layer.getImage();
        if (img == null) return;

        // Clip selection to image bounds relative to layer position
        int lx = layer.getBounds().x;
        int ly = layer.getBounds().y;
        int rx = sel.x - lx;
        int ry = sel.y - ly;
        int rw = sel.width;
        int rh = sel.height;

        // Intersect with image bounds
        int ix = Math.max(0, rx);
        int iy = Math.max(0, ry);
        int iw = Math.min(rw, img.getWidth() - ix);
        int ih = Math.min(rh, img.getHeight() - iy);
        if (iw <= 0 || ih <= 0) return;

        BufferedImage sub = img.getSubimage(ix, iy, iw, ih);
        BufferedImage copy = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(sub, 0, 0, null);
        g2.dispose();

        // Create new layer from selection
        Rectangle newBounds = new Rectangle(lx + ix, ly + iy, iw, ih);
        ImageLayer newLayer = new ImageLayer(
                layer.getName() + " (recorte)", copy, newBounds);
        ctx.layerModel().addLayer(newLayer);

        // Clear original selection if not keeping original
        if (!keepOriginal) {
            Graphics2D clearG = img.createGraphics();
            clearG.setComposite(java.awt.AlphaComposite.Clear);
            clearG.fillRect(ix, iy, iw, ih);
            clearG.dispose();
        }

        ctx.selectionModel().clear();
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

} // --- Fin de la clase CropTool ---
