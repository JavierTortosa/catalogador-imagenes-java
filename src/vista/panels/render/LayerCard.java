package vista.panels.render;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import modelo.editor.ImageLayer;
import modelo.editor.LayerModel;

public class LayerCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BG_NORMAL = new Color(50, 50, 55);
    private static final Color BG_SELECTED = new Color(65, 90, 120);
    private static final Color FG_TEXT = Color.WHITE;
    private static final Color BORDER = new Color(60, 60, 65);

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

    private final ImageLayer layer;
    private final LayerModel layerModel;
    private final int index;
    private final JLabel thumbLabel;
    private final JTextField nameField;
    private final JToggleButton eyeButton;
    private final JToggleButton lockButton;

    public LayerCard(ImageLayer layer, LayerModel layerModel, int index) {
        this.layer = layer;
        this.layerModel = layerModel;
        this.index = index;

        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setPreferredSize(new Dimension(190, 56));
        setMaximumSize(new Dimension(Short.MAX_VALUE, 56));
        setBorder(BorderFactory.createLineBorder(BORDER));
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
        nameField.setForeground(FG_TEXT);
        nameField.setCaretColor(FG_TEXT);
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

        // Click to select
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (layerModel != null) {
                    layerModel.setActiveLayer(index);
                    // parent panel will repaint via model change
                }
            }
        });

        updateSelection();
    } // --- Fin del constructor LayerCard ---


    public void updateThumbnail() {
        BufferedImage img = layer.getImage();
        if (img != null) {
            int w = Math.min(img.getWidth(), 48);
            int h = Math.min(img.getHeight(), 48);
            BufferedImage scaled = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
            Graphics g = scaled.getGraphics();
            int x = (48 - w) / 2;
            int y = (48 - h) / 2;
            g.drawImage(img, x, y, w, h, null);
            g.dispose();
            thumbLabel.setIcon(new ImageIcon(scaled));
        } else {
            thumbLabel.setIcon(null);
            thumbLabel.setText("?");
            thumbLabel.setForeground(FG_TEXT);
        }
    } // --- Fin del metodo updateThumbnail ---


    public void updateSelection() {
        boolean selected = layerModel != null
                && layerModel.getActiveIndex() == index;
        setBackground(selected ? BG_SELECTED : BG_NORMAL);
        repaint();
    } // --- Fin del metodo updateSelection ---


    public ImageLayer getLayer() {
        return layer;
    } // --- Fin del metodo getLayer ---


    public int getIndex() {
        return index;
    } // --- Fin del metodo getIndex ---

} // --- Fin de la clase LayerCard ---
