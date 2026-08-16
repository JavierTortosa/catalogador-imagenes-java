package vista.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.nio.file.Path;
import java.util.function.Predicate;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import vista.panels.render.RenderThumbnailItem;

/**
 * Renderer de celdas para los grids de miniaturas del modo Render. Dibuja el
 * icono del thumbnail con un borde verde y check cuando está aprobado, un
 * borde cian cuando coincide con el preview actual y un rectángulo de
 * selección alrededor de la celda activa.
 */
public class RenderThumbnailGridCellRenderer extends JPanel implements ListCellRenderer<RenderThumbnailItem> {

    /** Lado (px) de la celda del grid. */
    public static final int LADO_CELDA = 186;
    private static final int LADO_IMAGEN = 180;
    private static final int MARGEN = 3;

    private static final Color BORDE_APROBADO = new Color(0, 160, 0);
    private static final Color BORDE_PREVIEW = new Color(100, 200, 255);
    private static final Color BORDE_SELECCION = new Color(70, 130, 180);
    private static final Color COLOR_CHECK = new Color(0, 160, 0);

    private Predicate<Path> aprobadoProvider = p -> false;
    private Predicate<Path> previewProvider = p -> false;

    private RenderThumbnailItem item;
    private boolean seleccionado;
    private boolean aprobado;
    private boolean esPreview;
    private Color fondo;

    public RenderThumbnailGridCellRenderer() {
        setLayout(null);
        setOpaque(true);
        setPreferredSize(new Dimension(LADO_CELDA, LADO_CELDA));
    }

    /**
     * Define el proveedor que decide si una ruta está aprobada.
     * @param provider predicado que recibe el {@link Path} del thumbnail
     */
    public void setAprobadoProvider(Predicate<Path> provider) {
        this.aprobadoProvider = provider != null ? provider : p -> false;
    } // --- Fin del metodo setAprobadoProvider ---


    /**
     * Define el proveedor que decide si una ruta coincide con el preview actual.
     * @param provider predicado que recibe el {@link Path} del thumbnail
     */
    public void setPreviewProvider(Predicate<Path> provider) {
        this.previewProvider = provider != null ? provider : p -> false;
    } // --- Fin del metodo setPreviewProvider ---


    @Override
    public Component getListCellRendererComponent(JList<? extends RenderThumbnailItem> list,
            RenderThumbnailItem value, int index, boolean isSelected, boolean cellHasFocus) {
        this.item = value;
        this.seleccionado = isSelected;
        this.aprobado = value != null && aprobadoProvider.test(value.pngPath());
        this.esPreview = value != null && previewProvider.test(value.pngPath());
        this.fondo = list.getBackground();
        setToolTipText(value != null ? value.label() : null);
        return this;
    } // --- Fin del metodo getListCellRendererComponent ---


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(fondo != null ? fondo : new Color(40, 40, 45));
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (item != null && item.icon() != null && item.icon().getIconWidth() > 0) {
            item.icon().paintIcon(this, g2, MARGEN, MARGEN);
        }

        if (aprobado || esPreview) {
            g2.setColor(aprobado ? BORDE_APROBADO : BORDE_PREVIEW);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(MARGEN, MARGEN, LADO_IMAGEN, LADO_IMAGEN);
        }

        if (aprobado) {
            g2.setColor(COLOR_CHECK);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
            g2.drawString("\u2714", MARGEN + LADO_IMAGEN - 30, MARGEN + 26);
        }

        if (seleccionado) {
            g2.setColor(BORDE_SELECCION);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(1, 1, LADO_CELDA - 4, LADO_CELDA - 4);
        }

        g2.dispose();
    } // --- Fin del metodo paintComponent ---
} // --- Fin de la clase RenderThumbnailGridCellRenderer ---