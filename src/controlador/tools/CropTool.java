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

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_RECORTAR;
    } // --- Fin del metodo getCommandKey ---

    @Override
    public boolean modifiesContent() {
        return true;
    } // --- Fin del metodo modifiesContent ---

    @Override
    public void mousePressed(MouseEvent e) {
        if (ctx.selectionModel() == null || !ctx.selectionModel().isActive()) return;
        ImageLayer layer = getActiveLayer();
        if (layer == null || layer.isLocked()) return;

        boolean keepOriginal = ctx.componentBar().isCropKeepOriginal();
        boolean eliminar = "eliminar".equals(ctx.componentBar().getCropMode());

        var sm = ctx.selectionModel();
        Rectangle sel = sm.getBounds();
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

        int imgW = img.getWidth();
        int imgH = img.getHeight();

        if (eliminar) {
            // Modo eliminar: borra el contenido seleccionado (transparencia).
            // Con máscara se borra solo la forma irregular; sin ella, el rect.
            if (sm.hasMask()) {
                java.util.BitSet maskImg = new java.util.BitSet(imgW * imgH);
                for (int y = iy; y < iy + ih; y++) {
                    for (int x = ix; x < ix + iw; x++) {
                        int cx = lx + (int) ((long) x * layer.getBounds().width / imgW);
                        int cy = ly + (int) ((long) y * layer.getBounds().height / imgH);
                        if (sm.contains(cx, cy)) {
                            maskImg.set(y * imgW + x);
                        }
                    }
                }
                layer.clearMask(maskImg);
            } else {
                Graphics2D clearG = img.createGraphics();
                clearG.setComposite(java.awt.AlphaComposite.Clear);
                clearG.fillRect(ix, iy, iw, ih);
                clearG.dispose();
            }
        } else {
            // Modo nueva capa: extrae el contenido a una capa nueva. Con
            // máscara, el fondo queda transparente (recorte de la forma real).
            BufferedImage copy = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            if (sm.hasMask()) {
                for (int y = 0; y < ih; y++) {
                    for (int x = 0; x < iw; x++) {
                        int sx = ix + x;
                        int sy = iy + y;
                        int cx = lx + (int) ((long) sx * layer.getBounds().width / imgW);
                        int cy = ly + (int) ((long) sy * layer.getBounds().height / imgH);
                        if (sm.contains(cx, cy)) {
                            copy.setRGB(x, y, img.getRGB(sx, sy));
                        }
                    }
                }
            } else {
                BufferedImage sub = img.getSubimage(ix, iy, iw, ih);
                Graphics2D g2 = copy.createGraphics();
                g2.drawImage(sub, 0, 0, null);
                g2.dispose();
            }

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
        }

        ctx.selectionModel().clear();
    } // --- Fin del metodo mousePressed ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

} // --- Fin de la clase CropTool ---
