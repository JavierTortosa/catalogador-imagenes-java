package vista.panels.render;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import modelo.editor.LayerModel;

public class LayerCardPanel extends JScrollPane {

    private static final long serialVersionUID = 1L;

    private static Color clr(String key, int r, int g, int b) {
        Color c = UIManager.getColor(key);
        return c != null ? c : new Color(r, g, b);
    }

    private Color bgColor = clr("Panel.background", 50, 50, 55);
    private Color borderColor = clr("Component.borderColor", 60, 60, 65);
    private Color fgColor = clr("Label.foreground", 255, 255, 255);
    private Color selectedBg = clr("List.selectionBackground", 65, 90, 120);

    private final JPanel container;
    private LayerModel layerModel;

    public LayerCardPanel() {
        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(bgColor);

        setViewportView(container);
        setBorder(null);
        getViewport().setBackground(bgColor);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setUnitIncrement(16);
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
        container.removeAll();

        if (layerModel != null) {
            for (int i = 0; i < layerModel.size(); i++) {
                LayerCard card = new LayerCard(
                        layerModel.getLayer(i), layerModel, i,
                        bgColor, borderColor, fgColor, selectedBg);
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

} // --- Fin de la clase LayerCardPanel ---
