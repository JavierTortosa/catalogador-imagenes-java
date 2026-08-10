package vista.panels.render;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Layout que extiende {@link FlowLayout} para que sus componentes se
 * distribuyan en varias filas aunque el contenedor esté dentro de un
 * {@link JScrollPane}. El layout estándar de Swing no envuelve en ese caso
 * porque conserva el ancho preferido (una sola fila) y el scroll aparece en
 * horizontal.
 */
public class WrapLayout extends FlowLayout {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Construye un {@code WrapLayout} con alineación {@code LEADING} y
     * separaciones de 6 píxeles.
     */
    public WrapLayout() {
        super(FlowLayout.LEADING, 6, 6);
    }

    /**
     * Construye un {@code WrapLayout} con la alineación, separación horizontal
     * y separación vertical indicadas.
     *
     * @param align  alineación de los componentes en cada fila
     * @param hgap   separación horizontal entre componentes
     * @param vgap   separación vertical entre filas
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Calcula el tamaño preferido teniendo en cuenta el ancho disponible del
     * contenedor. Si el contenedor está dentro de un scroll pane se usa el
     * ancho visible del viewport; si no, el ancho actual del contenedor.
     *
     * @param target contenedor sobre el que se calcula el tamaño
     * @return dimensiones preferidas con las filas ya distribuidas
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        Dimension size = layoutSize(target, true);
        return size;
    }

    /**
     * Calcula el tamaño mínimo teniendo en cuenta el ancho disponible del
     * contenedor, con la misma lógica que el tamaño preferido.
     *
     * @param target contenedor sobre el que se calcula el tamaño
     * @return dimensiones mínimas con las filas ya distribuidas
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension size = layoutSize(target, false);
        return size;
    }

    /**
     * Devuelve el ancho real disponible para distribuir los componentes.
     * Si el contenedor está dentro de un scroll pane se usa el ancho del
     * viewport menos los bordes; en caso contrario el ancho actual del
     * contenedor menos los insets.
     *
     * @param target contenedor del que se quiere conocer el ancho
     * @return ancho efectivo en píxeles
     */
    private int availableWidth(Container target) {
        Insets insets = target.getInsets();
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

        if (scroll != null) {
            int viewportWidth = scroll.getViewport().getWidth();
            if (viewportWidth > 0) {
                return viewportWidth - (insets.left + insets.right + 6);
            }
        }

        return target.getWidth() - (insets.left + insets.right + 6);
    }

    /**
     * Calcula el tamaño del layout distribuyendo los componentes en filas según
     * el ancho disponible. Usa el tamaño preferido de cada componente cuando
     * {@code preferred} es {@code true} y el mínimo cuando es {@code false}.
     *
     * @param target    contenedor sobre el que se calcula el tamaño
     * @param preferred {@code true} para usar tamaños preferidos, {@code false}
     *                  para usar mínimos
     * @return dimensiones calculadas
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = availableWidth(target);
            Insets insets = target.getInsets();

            int hgap = getHgap();
            int vgap = getVgap();

            int maxWidth = targetWidth - (insets.left + insets.right);
            int rowWidth = 0;
            int rowHeight = 0;
            int totalWidth = 0;
            int totalHeight = insets.top + insets.bottom;

            for (Component comp : target.getComponents()) {
                if (comp.isVisible()) {
                    Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                    int width = d.width;
                    int height = d.height;

                    if (rowWidth > 0 && rowWidth + hgap + width > maxWidth) {
                        totalWidth = Math.max(totalWidth, rowWidth);
                        totalHeight += vgap + rowHeight;
                        rowWidth = width;
                        rowHeight = height;
                    } else {
                        if (rowWidth > 0) {
                            rowWidth += hgap;
                        }
                        rowWidth += width;
                        rowHeight = Math.max(rowHeight, height);
                    }
                }
            }

            totalWidth = Math.max(totalWidth, rowWidth);
            totalHeight += rowHeight;

            return new Dimension(totalWidth + insets.left + insets.right, totalHeight);
        }
    }
} // --- Fin de la clase WrapLayout ---
