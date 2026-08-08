package controlador.tools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;

/**
 * Herramienta de selección por capa.
 * <p>
 * Al pulsar dentro de la capa activa, selecciona el contenido opaco de esa capa
 * (el área delimitada por sus píxeles no transparentes), como la selección de
 * capa de Photoshop. Un clic fuera de la capa activa limpia la selección.
 * <p>
 * Resolución de capa estilo Photoshop: si la auto-selección está activada o no
 * hay capa activa, se busca la capa visible más frontal bajo el cursor y se
 * activa antes de calcular el contenido opaco.
 */
public class LayerSelectionTool extends Tool {

    @Override
    public String getCommandKey() {
        return AppActionCommands.CMD_ADVANCED_EDITOR_SELECCION_CAPA;
    } // --- Fin del metodo getCommandKey ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    @Override
    public void mousePressed(MouseEvent e) {
        if (ctx.selectionModel() == null) return;

        ImageLayer layer = findTargetLayer(e.getPoint());
        if (layer == null || layer.getBounds() == null) {
            ctx.selectionModel().clear();
            ctx.canvasPanel().repaint();
            return;
        }

        Rectangle b = layer.getBounds();
        if (!b.contains(e.getPoint())) {
            ctx.selectionModel().clear();
            ctx.canvasPanel().repaint();
            return;
        }

        ctx.selectionModel().setBounds(contenidoOpaco(layer, b));
        ctx.selectionModel().setFeather(ctx.componentBar().getFeatherAmount());
        ctx.canvasPanel().repaint();
    } // --- Fin del metodo mousePressed ---

    /**
     * Capa sobre la que actúa la herramienta. Si la auto-selección está
     * activada o no hay capa activa, busca la capa visible más frontal bajo el
     * cursor y la activa. Si no hay ninguna capa en ese punto, devuelve null.
     *
     * @param p coordenadas de canvas del punto pulsado
     * @return la capa de imagen a usar, o null si no hay ninguna
     */
    private ImageLayer findTargetLayer(Point p) {
        boolean autoSelect = ctx.componentBar() != null && ctx.componentBar().isAutoSelect();
        boolean sinActiva = getActiveLayer() == null;
        if (autoSelect || sinActiva) {
            if (ctx.layerPicker() != null) {
                Layer hit = ctx.layerPicker().findLayerAt(p);
                if (hit instanceof ImageLayer il && il.getImage() != null) {
                    ctx.layerPicker().activateLayer(il);
                    return il;
                }
            }
            return null;
        }
        return getActiveLayer();
    } // --- Fin del metodo findTargetLayer ---

    /**
     * Calcula el rectángulo delimitador de los píxeles opacos de la capa, en
     * coordenadas canvas, escalando al tamaño en pantalla del bounds cuando este
     * no coincide con el tamaño real de la imagen. Si la capa no tiene contenido
     * visible, usa sus bounds.
     */
    private Rectangle contenidoOpaco(ImageLayer layer, Rectangle b) {
        BufferedImage img = layer.getImage();
        if (img == null) return b;

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int minX = imgW;
        int minY = imgH;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < imgH; y++) {
            for (int x = 0; x < imgW; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a <= 0) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < 0) return b;
        int fx = b.x + (int) ((long) minX * b.width / imgW);
        int fy = b.y + (int) ((long) minY * b.height / imgH);
        int fw = Math.max(1, (int) ((long) (maxX - minX + 1) * b.width / imgW));
        int fh = Math.max(1, (int) ((long) (maxY - minY + 1) * b.height / imgH));
        return new Rectangle(fx, fy, fw, fh);
    } // --- Fin del metodo contenidoOpaco ---

} // --- Fin de la clase LayerSelectionTool ---
