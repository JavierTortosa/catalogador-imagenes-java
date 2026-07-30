package vista.panels.render;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import java.util.function.Consumer;

import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.TextLayer;

public class LayerCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final ImageIcon ICON_EYE_OPEN;
    private static final ImageIcon ICON_EYE_CLOSED;
    private static final ImageIcon ICON_LOCK;
    private static final ImageIcon ICON_UNLOCK;

    static {
        ImageIcon open = null, closed = null, lock = null, unlock = null;
        try {
            java.net.URL urlOpen = LayerCard.class.getResource("/iconos/white/eye.png");
            java.net.URL urlClosed = LayerCard.class.getResource("/iconos/white/eye-closed.png");
            if (urlOpen != null) {
                open = new ImageIcon(new javax.swing.ImageIcon(urlOpen).getImage()
                        .getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
            }
            if (urlClosed != null) {
                closed = new ImageIcon(new javax.swing.ImageIcon(urlClosed).getImage()
                        .getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
            }
            // Lock icons - use eye icons as fallback if no lock icon exists
            java.net.URL urlLock = LayerCard.class.getResource("/iconos/white/lock.png");
            java.net.URL urlUnlock = LayerCard.class.getResource("/iconos/white/lock-open.png");
            if (urlLock != null) {
                lock = new ImageIcon(new javax.swing.ImageIcon(urlLock).getImage()
                        .getScaledInstance(14, 14, java.awt.Image.SCALE_SMOOTH));
            }
            if (urlUnlock != null) {
                unlock = new ImageIcon(new javax.swing.ImageIcon(urlUnlock).getImage()
                        .getScaledInstance(14, 14, java.awt.Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {
            // fallback: keep null
        }
        ICON_EYE_OPEN = open;
        ICON_EYE_CLOSED = closed;
        ICON_LOCK = lock;
        ICON_UNLOCK = unlock;
    }

    private final Layer layer;
    private final LayerModel layerModel;
    private final int index;
    private final JLabel thumbLabel;
    private final JTextField nameField;
    private final JToggleButton eyeButton;
    private final JToggleButton lockButton;

    private Consumer<TextLayer> onDoubleClick;

    // Colores temáticos (se reciben por constructor)
    private Color bgNormal;
    private Color bgSelected;
    private Color fgText;
    private Color border;

    public LayerCard(Layer layer, LayerModel layerModel, int index,
                     Color bgNormal, Color border, Color fgText, Color bgSelected,
                     Consumer<TextLayer> onDoubleClick) {
        this.layer = layer;
        this.layerModel = layerModel;
        this.index = index;
        this.bgNormal = bgNormal;
        this.border = border;
        this.fgText = fgText;
        this.bgSelected = bgSelected;
        this.onDoubleClick = onDoubleClick;

        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setPreferredSize(new Dimension(190, 56));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 56));
        setBorder(BorderFactory.createLineBorder(border));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Thumbnail
        thumbLabel = new JLabel();
        thumbLabel.setPreferredSize(new Dimension(48, 48));
        thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateThumbnail();
        add(thumbLabel);

        // Name
        nameField = new JTextField(layer.getName());
        nameField.setBorder(null);
        nameField.setOpaque(false);
        nameField.setForeground(fgText);
        nameField.setCaretColor(fgText);
        nameField.addActionListener(e -> layer.setName(nameField.getText()));
        add(nameField);

        // Eye toggle
        eyeButton = new JToggleButton();
        eyeButton.setSelected(layer.isVisible());
        eyeButton.setPreferredSize(new Dimension(20, 20));
        eyeButton.setBorder(null);
        eyeButton.setOpaque(false);
        eyeButton.setFocusPainted(false);
        if (ICON_EYE_CLOSED != null) eyeButton.setIcon(ICON_EYE_CLOSED);
        if (ICON_EYE_OPEN != null) eyeButton.setSelectedIcon(ICON_EYE_OPEN);
        eyeButton.addActionListener(e -> layer.setVisible(eyeButton.isSelected()));
        add(eyeButton);

        // Lock toggle
        lockButton = new JToggleButton();
        lockButton.setSelected(layer.isLocked());
        lockButton.setPreferredSize(new Dimension(18, 18));
        lockButton.setBorder(null);
        lockButton.setOpaque(false);
        lockButton.setFocusPainted(false);
        if (ICON_UNLOCK != null) lockButton.setIcon(ICON_UNLOCK);
        if (ICON_LOCK != null) lockButton.setSelectedIcon(ICON_LOCK);
        lockButton.addActionListener(e -> layer.setLocked(lockButton.isSelected()));
        add(lockButton);

        // Click to select; double-click to edit text
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (layerModel != null) {
                    layerModel.setActiveLayer(index);
                }
                if (e.getClickCount() == 2 && layer instanceof TextLayer && onDoubleClick != null) {
                    onDoubleClick.accept((TextLayer) layer);
                }
            }
        });

        updateSelection();
    } // --- Fin del constructor LayerCard ---


    public void updateThumbnail() {
        BufferedImage thumb = layer.renderThumbnail(48);
        if (thumb != null) {
            thumbLabel.setIcon(new ImageIcon(thumb));
            thumbLabel.setText(null);
        } else {
            thumbLabel.setIcon(null);
            thumbLabel.setText("?");
            thumbLabel.setForeground(fgText);
        }
    } // --- Fin del metodo updateThumbnail ---


    public void updateSelection() {
        boolean selected = layerModel != null
                && layerModel.getActiveIndex() == index;
        setBackground(selected ? bgSelected : bgNormal);
        repaint();
    } // --- Fin del metodo updateSelection ---


    public Layer getLayer() {
        return layer;
    } // --- Fin del metodo getLayer ---


    public int getIndex() {
        return index;
    } // --- Fin del metodo getIndex ---

} // --- Fin de la clase LayerCard ---
