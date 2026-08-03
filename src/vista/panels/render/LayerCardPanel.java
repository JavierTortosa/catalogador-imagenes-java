package vista.panels.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.util.function.Consumer;

import modelo.editor.LayerModel;
import modelo.editor.TextLayer;

public class LayerCardPanel extends JScrollPane {

    private static final long serialVersionUID = 1L;

    private static final int CARD_HEIGHT = 56;

    private static Color clr(String key, int r, int g, int b) {
        Color c = UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    private Color bgColor = clr("Panel.background", 50, 50, 55);
    private Color borderColor = clr("Component.borderColor", 60, 60, 65);
    private Color fgColor = clr("Label.foreground", 255, 255, 255);
    private Color selectedBg = clr("List.selectionBackground", 65, 90, 120);

    private final JPanel container;
    private final JPanel dropIndicator;
    private LayerModel layerModel;

    private Consumer<TextLayer> onDoubleClick;

    // --- estado del drag & drop manual ---
    private int dragStartIndex = -1;
    private Point dragStartPoint;
    private boolean dropDragging;
    private int dropTargetVisual = -1;
    private LayerCard pressedCard;

    private final MouseAdapter dragHandler = new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e) {
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), container);
            dragStartPoint = p;
            dragStartIndex = -1;
            dropDragging = false;
            dropTargetVisual = -1;
            pressedCard = null;
            if (layerModel == null) return;
            // No iniciar arrastre desde el campo de nombre ni los toggles
            Component deepest = container.getComponentAt(p);
            if (deepest instanceof javax.swing.text.JTextComponent
                    || deepest instanceof javax.swing.AbstractButton) {
                return;
            }
            Component c = findCardAt(p);
            if (c instanceof LayerCard card) {
                dragStartIndex = card.getIndex();
                pressedCard = card;
            }
        } // --- Fin del metodo mousePressed ---


        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragStartIndex < 0 || dragStartPoint == null || layerModel == null) return;
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), container);
            if (!dropDragging) {
                if (p.distance(dragStartPoint) < 5) return;
                dropDragging = true;
            }
            updateDropIndicator(p);
        } // --- Fin del metodo mouseDragged ---


        @Override
        public void mouseReleased(MouseEvent e) {
            if (dropDragging) {
                if (pressedCard != null) {
                    pressedCard.setSelectionSuppressed(true);
                }
                performDrop();
            }
            dragStartIndex = -1;
            dragStartPoint = null;
            pressedCard = null;
        } // --- Fin del metodo mouseReleased ---

    };

    public void setOnDoubleClick(Consumer<TextLayer> listener) {
        this.onDoubleClick = listener;
        rebuild();
    } // --- Fin del metodo setOnDoubleClick ---

    public LayerCardPanel() {
        setBorder(null);
        setBackground(bgColor);
        getViewport().setBackground(bgColor);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        getVerticalScrollBar().setUnitIncrement(16);

        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(bgColor);
        setViewportView(container);

        dropIndicator = new JPanel();
        dropIndicator.setPreferredSize(new Dimension(10, 3));
        dropIndicator.setMaximumSize(new Dimension(Short.MAX_VALUE, 3));
        dropIndicator.setBackground(new Color(80, 160, 255));

        installDragAndDrop();
    } // --- Fin del constructor LayerCardPanel ---


    /**
     * Actualiza los colores temáticos y reconstruye las tarjetas.
     */
    public void updateTheme(Color bg, Color border, Color fg, Color selBg) {
        this.bgColor = bg;
        this.borderColor = border;
        this.fgColor = fg;
        this.selectedBg = selBg;
        container.setBackground(bgColor);
        getViewport().setBackground(bgColor);
        setBackground(bgColor);
        rebuild();
    } // --- Fin del metodo updateTheme ---


    public void setLayerModel(LayerModel layerModel) {
        this.layerModel = layerModel;
        rebuild();
    } // --- Fin del metodo setLayerModel ---


    public LayerModel getLayerModel() {
        return layerModel;
    } // --- Fin del metodo getLayerModel ---


    public void rebuild() {
        cancelDrop();
        container.removeAll();

        if (layerModel != null) {
            // Estilo Photoshop: la capa superior (índice mayor) arriba, el fondo abajo
            for (int i = layerModel.size() - 1; i >= 0; i--) {
                LayerCard card = new LayerCard(
                        layerModel.getLayer(i), layerModel, i,
                        bgColor, borderColor, fgColor, selectedBg,
                        onDoubleClick);
                card.addMouseListener(dragHandler);
                card.addMouseMotionListener(dragHandler);
                container.add(card);
            }
        }

        container.add(Box.createVerticalGlue());
        container.revalidate();
        container.repaint();
    } // --- Fin del metodo rebuild ---


    public void updateSelection() {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof LayerCard card) {
                card.updateSelection();
            }
        }
    } // --- Fin del metodo updateSelection ---


    // ======================== DRAG & DROP MANUAL ========================


    /**
     * Registra los listeners de arrastre sobre el contenedor (zonas vacías) y
     * el binding de ESC. Los arrastres sobre las tarjetas se registran por
     * separado en {@link #rebuild()} sobre cada tarjeta, con ambos tipos de
     * listener (ratón y movimiento) para que mouseDragged se dispare.
     */
    private void installDragAndDrop() {
        container.addMouseListener(dragHandler);
        container.addMouseMotionListener(dragHandler);

        // ESC cancela el arrastre en curso
        container.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelarDrop");
        container.getActionMap().put("cancelarDrop", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancelDrop();
            }
        });
    } // --- Fin del metodo installDragAndDrop ---


    /**
     * Localiza la tarjeta bajo el punto (coordenadas del contenedor).
     */
    private Component findCardAt(Point p) {
        for (Component c : container.getComponents()) {
            if (c instanceof LayerCard && c.getBounds().contains(p)) {
                return c;
            }
        }
        return null;
    } // --- Fin del metodo findCardAt ---


    /**
     * Calcula la posición visual de inserción (0..size) a partir del punto del
     * ratón, usando la altura fija de tarjeta para evitar realimentación por el
     * desplazamiento de la línea indicadora.
     */
    private int computeTargetVisual(Point p) {
        int size = layerModel.size();
        int target = (int) Math.floor(p.y / (double) CARD_HEIGHT);
        if (target < 0) target = 0;
        if (target > size) target = size;
        return target;
    } // --- Fin del metodo computeTargetVisual ---


    /**
     * Mueve (o crea) la línea indicadora en la posición de inserción visual.
     */
    private void updateDropIndicator(Point p) {
        if (layerModel == null) return;
        int target = computeTargetVisual(p);
        if (target == dropTargetVisual) return;

        if (dropIndicator.getParent() == container) {
            container.remove(dropIndicator);
        }
        dropTargetVisual = target;
        container.add(dropIndicator, Math.min(target, container.getComponentCount()));
        container.revalidate();
        container.repaint();
    } // --- Fin del metodo updateDropIndicator ---


    /**
     * Quita la línea indicadora si está visible.
     */
    private void removeDropIndicator() {
        if (dropIndicator.getParent() == container) {
            container.remove(dropIndicator);
        }
        dropTargetVisual = -1;
        container.revalidate();
        container.repaint();
    } // --- Fin del metodo removeDropIndicator ---


    /**
     * Cancela el arrastre en curso y elimina la línea indicadora.
     */
    public void cancelDrop() {
        dropDragging = false;
        dragStartIndex = -1;
        dragStartPoint = null;
        pressedCard = null;
        removeDropIndicator();
    } // --- Fin del metodo cancelDrop ---


    /**
     * Ejecuta el reordenamiento al soltar: convierte la posición visual de
     * inserción en el índice real del modelo y delega en moveLayer.
     */
    private void performDrop() {
        if (layerModel == null || dragStartIndex < 0 || dropTargetVisual < 0) return;
        removeDropIndicator();

        int size = layerModel.size();
        int finalIdx = size - 1 - dropTargetVisual;
        if (finalIdx < 0) finalIdx = 0;
        if (finalIdx > size - 1) finalIdx = size - 1;

        if (finalIdx != dragStartIndex) {
            layerModel.moveLayer(dragStartIndex, finalIdx);
        }
        dropTargetVisual = -1;
    } // --- Fin del metodo performDrop ---


    /**
     * Comprueba si hay un arrastre en curso (para que el panel padre pueda
     * reflejarlo, p. ej. desactivando el scroll automático).
     */
    public boolean isDragging() {
        return dropDragging;
    } // --- Fin del metodo isDragging ---

} // --- Fin de la clase LayerCardPanel ---
