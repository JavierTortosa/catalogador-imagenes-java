package vista.panels.render;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import modelo.editor.LayerModel;

public class LayerCardPanel extends JScrollPane {

    private static final long serialVersionUID = 1L;

    private static final Color BG = new Color(50, 50, 55);

    private final JPanel container;
    private LayerModel layerModel;

    public LayerCardPanel() {
        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(BG);

        setViewportView(container);
        setBorder(null);
        getViewport().setBackground(BG);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setUnitIncrement(16);
    } // --- Fin del constructor LayerCardPanel ---


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
                        layerModel.getLayer(i), layerModel, i);
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
