package controlador.tools;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import controlador.commands.AppActionCommands;
import modelo.editor.ImageLayer;
import modelo.editor.SelectionModel;
import servicios.editor.EditorHistory;

/**
 * Herramienta recortar.
 * <p>
 * Requiere una selección activa. Ofrece dos modos: eliminar el contenido
 * seleccionado (dejando transparencia) o crear una nueva capa con el contenido
 * recortado.
 * <p>
 * La selección se expresa en coordenadas de canvas; la imagen de la capa puede
 * estar escalada respecto a sus bounds, por lo que la región de imagen se
 * calcula por proyección {@code (canvas -> píxel)} teniendo en cuenta la escala.
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

        int lx = layer.getBounds().x;
        int ly = layer.getBounds().y;
        int lw = Math.max(1, layer.getBounds().width);
        int lh = Math.max(1, layer.getBounds().height);
        int imgW = img.getWidth();
        int imgH = img.getHeight();

        // Región de imagen correspondiente a la selección (canvas -> píxel).
        int imgX0 = Math.max(0, floorDiv((long) (sel.x - lx) * imgW, lw));
        int imgY0 = Math.max(0, floorDiv((long) (sel.y - ly) * imgH, lh));
        int imgX1 = Math.min(imgW, ceilDiv((long) (sel.x + sel.width - lx) * imgW, lw));
        int imgY1 = Math.min(imgH, ceilDiv((long) (sel.y + sel.height - ly) * imgH, lh));
        int iw = imgX1 - imgX0;
        int ih = imgY1 - imgY0;
        if (iw <= 0 || ih <= 0) return;

        var history = ctx.componentBar().getEditorHistory();
        String nombre = eliminar ? "Eliminar selección" : "Recortar capa";
        // Operación de píxeles: se fuerza el paso porque la mutación in-place
        // no es detectable por comparación de snapshots.
        if (history != null) {
            history.endGesture();
            history.beginGesture(nombre, true);
        }
        try {
            if (eliminar) {
                eliminarContenido(layer, img, sm, imgX0, imgY0, iw, ih, lx, ly, lw, lh, imgW, imgH);
            } else {
                nuevaCapaContenido(layer, img, sm, keepOriginal, imgX0, imgY0, iw, ih, lx, ly, lw, lh, imgW, imgH);
            }
        } finally {
            if (history != null) {
                history.endGesture();
            }
        }

        ctx.selectionModel().clear();
    } // --- Fin del metodo mousePressed ---

    /**
     * Borra el contenido de la selección (transparencia) sobre la capa.
     * Con máscara se borra solo la forma irregular; sin ella, el rect de imagen.
     */
    private void eliminarContenido(ImageLayer layer, BufferedImage img, SelectionModel sm,
            int imgX0, int imgY0, int iw, int ih,
            int lx, int ly, int lw, int lh, int imgW, int imgH) {
        if (sm.hasMask()) {
            java.util.BitSet maskImg = new java.util.BitSet(imgW * imgH);
            for (int y = imgY0; y < imgY0 + ih; y++) {
                for (int x = imgX0; x < imgX0 + iw; x++) {
                    int cx = lx + (int) ((long) x * lw / imgW);
                    int cy = ly + (int) ((long) y * lh / imgH);
                    if (sm.contains(cx, cy)) {
                        maskImg.set(y * imgW + x);
                    }
                }
            }
            layer.clearMask(maskImg);
        } else {
            Graphics2D clearG = img.createGraphics();
            clearG.setComposite(java.awt.AlphaComposite.Clear);
            clearG.fillRect(imgX0, imgY0, iw, ih);
            clearG.dispose();
        }
    } // --- Fin del metodo eliminarContenido ---

    /**
     * Extrae el contenido de la selección a una capa nueva. Con máscara, el
     * fondo queda transparente (recorte de la forma real). Si {@code keepOriginal}
     * es {@code false}, el área original se deja transparente.
     */
    private void nuevaCapaContenido(ImageLayer layer, BufferedImage img, SelectionModel sm, boolean keepOriginal,
            int imgX0, int imgY0, int iw, int ih,
            int lx, int ly, int lw, int lh, int imgW, int imgH) {
        BufferedImage copy = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        if (sm.hasMask()) {
            for (int y = 0; y < ih; y++) {
                for (int x = 0; x < iw; x++) {
                    int sx = imgX0 + x;
                    int sy = imgY0 + y;
                    int cx = lx + (int) ((long) sx * lw / imgW);
                    int cy = ly + (int) ((long) sy * lh / imgH);
                    if (sm.contains(cx, cy)) {
                        copy.setRGB(x, y, img.getRGB(sx, sy));
                    }
                }
            }
        } else {
            BufferedImage sub = img.getSubimage(imgX0, imgY0, iw, ih);
            Graphics2D g2 = copy.createGraphics();
            g2.drawImage(sub, 0, 0, null);
            g2.dispose();
        }

        // La nueva capa ocupa, en canvas, la región que la selección recubre
        // sobre la capa original, para conservar posición y escala visuales.
        int nbX = lx + (int) ((long) imgX0 * lw / imgW);
        int nbY = ly + (int) ((long) imgY0 * lh / imgH);
        int nbW = (int) ((long) iw * lw / imgW);
        int nbH = (int) ((long) ih * lh / imgH);
        Rectangle newBounds = new Rectangle(nbX, nbY, Math.max(1, nbW), Math.max(1, nbH));
        ImageLayer newLayer = ImageLayer.crearConId(null,
                layer.getName() + " (recorte)", copy, newBounds);
        ctx.layerModel().addLayer(newLayer);

        // Clear original selection if not keeping original
        if (!keepOriginal) {
            Graphics2D clearG = img.createGraphics();
            clearG.setComposite(java.awt.AlphaComposite.Clear);
            clearG.fillRect(imgX0, imgY0, iw, ih);
            clearG.dispose();
        }
    } // --- Fin del metodo nuevaCapaContenido ---

    private ImageLayer getActiveLayer() {
        if (ctx.layerModel() == null) return null;
        return ctx.layerModel().getActiveLayer() instanceof ImageLayer il ? il : null;
    } // --- Fin del metodo getActiveLayer ---

    private static int floorDiv(long a, int b) {
        return (int) Math.floorDiv(a, b);
    } // --- Fin del metodo floorDiv ---

    private static int ceilDiv(long a, int b) {
        return (int) Math.floorDiv(a + b - 1, b);
    } // --- Fin del metodo ceilDiv ---

} // --- Fin de la clase CropTool ---
