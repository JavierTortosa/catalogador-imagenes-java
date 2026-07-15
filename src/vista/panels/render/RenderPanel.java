package vista.panels.render;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import modelo.renderer.StlEntry;
import servicios.renderer.Zip2PngScanner.RenderCandidate;

public class RenderPanel extends JPanel {

    private final DefaultListModel<RenderCandidate> listModel;
    private final JList<RenderCandidate> candidateList;
    private final DefaultListModel<StlEntry> contentListModel;
    private final JList<StlEntry> contentList;
    private final JPanel thumbnailGrid;
    private final PreviewPanel3DFX preview3DFX;

    private final JLabel brightnessLabel;
    private final JSlider brightnessSlider;
    private final JTextField brightnessField;
    private final JLabel contrastLabel;
    private final JSlider contrastSlider;
    private final JTextField contrastField;
    private final JCheckBox chkAntiAlias;
    private final JCheckBox chkCrosshair;

    // --- Controles de fondo ---
    private final ButtonGroup bgGroup;
    private final JRadioButton rbSolid;
    private final JRadioButton rbGradient;
    private final JRadioButton rbImage;
    private final JRadioButton rbTransparent;

    private final JPanel solidColorPreview;
    private final JButton btnSolidColor;
    private Color solidBgColor = new Color(60, 60, 65);

    private final JPanel gradientPreview;
    private final JPanel gradientStartPreview;
    private final JPanel gradientEndPreview;
    private final JButton btnGradientStart;
    private final JButton btnGradientEnd;
    private Color gradientStartColor = new Color(45, 45, 50);
    private Color gradientEndColor = new Color(75, 75, 80);

    private final JTextField bgImageField;
    private final JButton btnBrowseImage;
    private final JSlider bgImageScaleSlider;
    private final JLabel bgImageScaleLabel;

    private final JCheckBox chkCheckerboard;

    private final JPanel cardPanel;
    private static final String CARD_SOLID = "solid";
    private static final String CARD_GRADIENT = "gradient";
    private static final String CARD_IMAGE = "image";
    private static final String CARD_TRANSPARENT = "transparent";

    public RenderPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(new Color(35, 35, 40));

        listModel = new DefaultListModel<>();
        candidateList = new JList<>(listModel);
        candidateList.setCellRenderer(new RenderListCellRenderer());
        candidateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(candidateList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Archivos sin imagen"));
        listScroll.setPreferredSize(new Dimension(280, 0));

        contentListModel = new DefaultListModel<>();
        contentList = new JList<>(contentListModel);
        contentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane contentScroll = new JScrollPane(contentList);
        contentScroll.setBorder(BorderFactory.createTitledBorder("Contenido del ZIP"));

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroll, contentScroll);
        leftSplit.setResizeWeight(0.6);
        leftSplit.setDividerLocation(0.6);
        leftSplit.setBorder(null);

        thumbnailGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        thumbnailGrid.setBackground(new Color(40, 40, 45));
        JScrollPane gridScroll = new JScrollPane(thumbnailGrid);
        gridScroll.setBorder(BorderFactory.createTitledBorder("Thumbnails generados"));

        JPanel rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.setBackground(new Color(30, 30, 35));
        rightPanel.setPreferredSize(new Dimension(340, 0));

        preview3DFX = new PreviewPanel3DFX();
        preview3DFX.setBorder(BorderFactory.createTitledBorder("Preview"));
        rightPanel.add(preview3DFX, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(40, 40, 45));
        tabbedPane.setForeground(Color.WHITE);

        // --- Tab "Imagen" ---
        JPanel imagenTab = new JPanel(new GridLayout(0, 1, 2, 2));
        imagenTab.setBackground(new Color(40, 40, 45));

        brightnessLabel = new JLabel("Brillo");
        brightnessLabel.setForeground(Color.WHITE);
        brightnessLabel.setPreferredSize(new Dimension(70, 20));
        brightnessSlider = new JSlider(-100, 100, 0);
        brightnessField = new JTextField("0", 5);
        imagenTab.add(buildSliderRow(brightnessLabel, brightnessSlider, brightnessField));

        contrastLabel = new JLabel("Contraste");
        contrastLabel.setForeground(Color.WHITE);
        contrastLabel.setPreferredSize(new Dimension(70, 20));
        contrastSlider = new JSlider(-100, 100, 0);
        contrastField = new JTextField("0", 5);
        imagenTab.add(buildSliderRow(contrastLabel, contrastSlider, contrastField));

        chkAntiAlias = new JCheckBox("Antialiasing");
        chkAntiAlias.setBackground(new Color(40, 40, 45));
        chkAntiAlias.setForeground(Color.WHITE);
        chkCrosshair = new JCheckBox("Cruceta (ejes)");
        chkCrosshair.setBackground(new Color(40, 40, 45));
        chkCrosshair.setForeground(Color.WHITE);
        chkCrosshair.setSelected(true);
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        checkPanel.setBackground(new Color(40, 40, 45));
        checkPanel.add(chkAntiAlias);
        checkPanel.add(chkCrosshair);
        JPanel imagenBottom = new JPanel(new BorderLayout());
        imagenBottom.setBackground(new Color(40, 40, 45));
        imagenBottom.add(checkPanel, BorderLayout.NORTH);
        imagenTab.add(imagenBottom);

        tabbedPane.addTab("Imagen", imagenTab);

        // --- Tab "Fondo" ---
        JPanel fondoTab = new JPanel(new BorderLayout(4, 4));
        fondoTab.setBackground(new Color(40, 40, 45));

        // Radio buttons
        rbSolid = new JRadioButton("S\u00F3lido");
        rbGradient = new JRadioButton("Degradado");
        rbImage = new JRadioButton("Cargar fondo");
        rbTransparent = new JRadioButton("Fondo transparente");
        for (JRadioButton rb : new JRadioButton[]{rbSolid, rbGradient, rbImage, rbTransparent}) {
            rb.setBackground(new Color(40, 40, 45));
            rb.setForeground(Color.WHITE);
        }
        bgGroup = new ButtonGroup();
        bgGroup.add(rbSolid);
        bgGroup.add(rbGradient);
        bgGroup.add(rbImage);
        bgGroup.add(rbTransparent);
        rbSolid.setSelected(true);

        JPanel radioPanel = new JPanel(new GridLayout(2, 2, 4, 2));
        radioPanel.setBackground(new Color(40, 40, 45));
        radioPanel.add(rbSolid);
        radioPanel.add(rbGradient);
        radioPanel.add(rbImage);
        radioPanel.add(rbTransparent);

        JPanel radioContainer = new JPanel(new BorderLayout());
        radioContainer.setBackground(new Color(40, 40, 45));
        radioContainer.add(radioPanel, BorderLayout.NORTH);
        radioContainer.add(new javax.swing.JSeparator(), BorderLayout.SOUTH);
        fondoTab.add(radioContainer, BorderLayout.NORTH);

        // CardLayout con las opciones de cada radio
        cardPanel = new JPanel(new CardLayout());
        cardPanel.setBackground(new Color(40, 40, 45));

        // Card: Sólido
        JPanel solidCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        solidCard.setBackground(new Color(40, 40, 45));
        solidColorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(solidBgColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        solidColorPreview.setPreferredSize(new Dimension(32, 24));
        btnSolidColor = new JButton();
        btnSolidColor.setPreferredSize(new Dimension(24, 24));
        solidCard.add(new JLabel("Color:"));
        solidCard.add(solidColorPreview);
        solidCard.add(btnSolidColor);
        cardPanel.add(solidCard, CARD_SOLID);

        // Card: Degradado
        JPanel gradientCard = new JPanel(new BorderLayout(4, 4));
        gradientCard.setBackground(new Color(40, 40, 45));

        JPanel gradientTop = new JPanel(new GridLayout(1, 2, 8, 0));
        gradientTop.setBackground(new Color(40, 40, 45));

        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        startPanel.setBackground(new Color(40, 40, 45));
        gradientStartPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(gradientStartColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientStartPreview.setPreferredSize(new Dimension(32, 24));
        btnGradientStart = new JButton();
        btnGradientStart.setPreferredSize(new Dimension(24, 24));
        startPanel.add(new JLabel("Inicio:"));
        startPanel.add(gradientStartPreview);
        startPanel.add(btnGradientStart);

        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        endPanel.setBackground(new Color(40, 40, 45));
        gradientEndPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(gradientEndColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientEndPreview.setPreferredSize(new Dimension(32, 24));
        btnGradientEnd = new JButton();
        btnGradientEnd.setPreferredSize(new Dimension(24, 24));
        endPanel.add(new JLabel("Fin:"));
        endPanel.add(gradientEndPreview);
        endPanel.add(btnGradientEnd);

        gradientTop.add(startPanel);
        gradientTop.add(endPanel);

        gradientPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, gradientStartColor,
                        getWidth(), 0, gradientEndColor);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        gradientPreview.setPreferredSize(new Dimension(0, 24));

        gradientCard.add(gradientTop, BorderLayout.NORTH);
        gradientCard.add(gradientPreview, BorderLayout.CENTER);
        cardPanel.add(gradientCard, CARD_GRADIENT);

        // Card: Cargar fondo
        JPanel imageCard = new JPanel(new BorderLayout(4, 4));
        imageCard.setBackground(new Color(40, 40, 45));
        JPanel imageTop = new JPanel(new BorderLayout(4, 4));
        imageTop.setBackground(new Color(40, 40, 45));
        bgImageField = new JTextField();
        bgImageField.setEditable(false);
        btnBrowseImage = new JButton("...");
        btnBrowseImage.setPreferredSize(new Dimension(28, 22));
        imageTop.add(bgImageField, BorderLayout.CENTER);
        imageTop.add(btnBrowseImage, BorderLayout.EAST);
        JPanel imageScaleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        imageScaleRow.setBackground(new Color(40, 40, 45));
        bgImageScaleSlider = new JSlider(10, 100, 50);
        bgImageScaleSlider.setBackground(new Color(50, 50, 55));
        bgImageScaleLabel = new JLabel("0.5x");
        bgImageScaleLabel.setForeground(Color.WHITE);
        bgImageScaleLabel.setPreferredSize(new Dimension(35, 20));
        imageScaleRow.add(new JLabel("Escala:"));
        imageScaleRow.add(bgImageScaleSlider);
        imageScaleRow.add(bgImageScaleLabel);
        imageCard.add(imageTop, BorderLayout.NORTH);
        imageCard.add(imageScaleRow, BorderLayout.SOUTH);
        cardPanel.add(imageCard, CARD_IMAGE);

        // Card: Transparente
        JPanel transparentCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        transparentCard.setBackground(new Color(40, 40, 45));
        JLabel lblTransparent = new JLabel("Sin fondo (PNG con canal alfa)");
        lblTransparent.setForeground(Color.LIGHT_GRAY);
        transparentCard.add(lblTransparent);
        cardPanel.add(transparentCard, CARD_TRANSPARENT);

        fondoTab.add(cardPanel, BorderLayout.CENTER);

        // Checkerboard visual
        chkCheckerboard = new JCheckBox("Fondo a cuadros (preview)");
        chkCheckerboard.setBackground(new Color(40, 40, 45));
        chkCheckerboard.setForeground(Color.WHITE);
        JPanel cbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cbPanel.setBackground(new Color(40, 40, 45));
        cbPanel.add(chkCheckerboard);
        fondoTab.add(cbPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Fondo", fondoTab);

        rightPanel.add(tabbedPane, BorderLayout.SOUTH);

        add(leftSplit, BorderLayout.WEST);
        add(gridScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private JPanel buildSliderRow(JLabel lbl, JSlider slider, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(new Color(40, 40, 45));
        row.add(lbl, BorderLayout.WEST);
        slider.setBackground(new Color(50, 50, 55));
        row.add(slider, BorderLayout.CENTER);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setPreferredSize(new Dimension(40, 22));
        row.add(field, BorderLayout.EAST);
        return row;
    }

    public void repaintGradientPreview() {
        gradientPreview.repaint();
    }

    public void repaintColorPreviews() {
        solidColorPreview.repaint();
        gradientStartPreview.repaint();
        gradientEndPreview.repaint();
    }

    // --- Getters existentes ---
    public DefaultListModel<RenderCandidate> getListModel() { return listModel; }
    public JList<RenderCandidate> getCandidateList() { return candidateList; }
    public DefaultListModel<StlEntry> getContentListModel() { return contentListModel; }
    public JList<StlEntry> getContentList() { return contentList; }
    public JPanel getThumbnailGrid() { return thumbnailGrid; }
    public PreviewPanel3DFX getPreview3DFX() { return preview3DFX; }

    public int getBrightness() {
        try { return Integer.parseInt(brightnessField.getText().trim()); }
        catch (NumberFormatException e) { return brightnessSlider.getValue(); }
    }
    public int getContrast() {
        try { return Integer.parseInt(contrastField.getText().trim()); }
        catch (NumberFormatException e) { return contrastSlider.getValue(); }
    }
    public boolean isCheckerboard() { return chkCheckerboard.isSelected(); }
    public boolean isAntiAlias() { return chkAntiAlias.isSelected(); }
    public boolean isCrosshair() { return chkCrosshair.isSelected(); }

    public JSlider getBrightnessSlider() { return brightnessSlider; }
    public JSlider getContrastSlider() { return contrastSlider; }
    public JTextField getBrightnessField() { return brightnessField; }
    public JTextField getContrastField() { return contrastField; }
    public JCheckBox getChkCheckerboard() { return chkCheckerboard; }
    public JCheckBox getChkAntiAlias() { return chkAntiAlias; }
    public JCheckBox getChkCrosshair() { return chkCrosshair; }

    // --- Getters nuevos: fondo ---
    public ButtonGroup getBgGroup() { return bgGroup; }
    public JRadioButton getRbSolid() { return rbSolid; }
    public JRadioButton getRbGradient() { return rbGradient; }
    public JRadioButton getRbImage() { return rbImage; }
    public JRadioButton getRbTransparent() { return rbTransparent; }

    public Color getSolidBgColor() { return solidBgColor; }
    public void setSolidBgColor(Color c) { this.solidBgColor = c; solidColorPreview.repaint(); }
    public Color getGradientStartColor() { return gradientStartColor; }
    public void setGradientStartColor(Color c) { this.gradientStartColor = c; gradientStartPreview.repaint(); gradientPreview.repaint(); }
    public Color getGradientEndColor() { return gradientEndColor; }
    public void setGradientEndColor(Color c) { this.gradientEndColor = c; gradientEndPreview.repaint(); gradientPreview.repaint(); }

    public JPanel getSolidColorPreview() { return solidColorPreview; }
    public JButton getBtnSolidColor() { return btnSolidColor; }
    public JButton getBtnGradientStart() { return btnGradientStart; }
    public JButton getBtnGradientEnd() { return btnGradientEnd; }
    public JPanel getGradientPreview() { return gradientPreview; }

    public JTextField getBgImageField() { return bgImageField; }
    public JButton getBtnBrowseImage() { return btnBrowseImage; }
    public JSlider getBgImageScaleSlider() { return bgImageScaleSlider; }
    public JLabel getBgImageScaleLabel() { return bgImageScaleLabel; }

    public String getSelectedBgMode() {
        if (rbSolid.isSelected()) return CARD_SOLID;
        if (rbGradient.isSelected()) return CARD_GRADIENT;
        if (rbImage.isSelected()) return CARD_IMAGE;
        return CARD_TRANSPARENT;
    }

    public void showBgCard(String card) {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, card);
    }

    public void setColorPickerIcons(javax.swing.Icon icon) {
        btnSolidColor.setIcon(icon);
        btnGradientStart.setIcon(icon);
        btnGradientEnd.setIcon(icon);
    }

    public void setBrightnessIcon(javax.swing.Icon icon) { brightnessLabel.setIcon(icon); }
    public void setContrastIcon(javax.swing.Icon icon) { contrastLabel.setIcon(icon); }
    public void setAntiAliasIcon(javax.swing.Icon icon) { chkAntiAlias.setIcon(icon); }
    public void setCrosshairIcon(javax.swing.Icon icon) { chkCrosshair.setIcon(icon); }

} // --- Fin de la clase RenderPanel ---
