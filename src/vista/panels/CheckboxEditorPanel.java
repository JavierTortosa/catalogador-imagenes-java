package vista.panels;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.theme.ThemeManager;

public class CheckboxEditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ImageDisplayPanel imagePanel;
    private final JLabel headerLabel;
    private final JLabel zoomLabel;

    private final CheckboxEditorMouseHandler mouseHandler;

    public CheckboxEditorPanel(ThemeManager themeManager, VisorModel model,
                                IProjectManager projectManager, ComponentRegistry registry,
                                JToolBar editorToolbar) {
        setLayout(new BorderLayout());
        setBackground(themeManager.getTemaActual().colorFondoSecundario());
        setBorder(BorderFactory.createTitledBorder("Editor de Checkboxes"));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        headerLabel = new JLabel(" ", SwingConstants.CENTER);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(new EmptyBorder(4, 0, 4, 0));
        topPanel.add(headerLabel, BorderLayout.CENTER);

        zoomLabel = new JLabel("Z: 100%", SwingConstants.RIGHT);
        zoomLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        topPanel.add(zoomLabel, BorderLayout.EAST);

        if (editorToolbar != null) {
            editorToolbar.setFloatable(false);
            editorToolbar.setOpaque(false);
            topPanel.add(editorToolbar, BorderLayout.SOUTH);
        }

        add(topPanel, BorderLayout.NORTH);

        this.imagePanel = new ImageDisplayPanel(themeManager, model);
        this.imagePanel.setProjectManager(projectManager);
        this.imagePanel.setEditorOverlayInstance(true);
        this.imagePanel.setUseEditorZoom(true);
        add(imagePanel, BorderLayout.CENTER);

        this.mouseHandler = new CheckboxEditorMouseHandler(
                model, projectManager, registry, imagePanel, headerLabel);
        imagePanel.addMouseListener(mouseHandler);
        imagePanel.addMouseMotionListener(mouseHandler);
        imagePanel.addMouseWheelListener(e -> {
            if (!mouseHandler.isActive()) return;
            int notches = e.getWheelRotation();
            double factor = imagePanel.getEditorZoomFactor();
            factor *= (notches < 0) ? 1.15 : 1 / 1.15;
            factor = Math.max(0.1, Math.min(10.0, factor));
            imagePanel.setEditorZoomFactor(factor);
            actualizarZoomLabel();
        });
    }

    private void actualizarZoomLabel() {
        int pct = (int) Math.round(imagePanel.getEditorZoomFactor() * 100);
        zoomLabel.setText("Z: " + pct + "%");
    }

    public void actualizarCabecera() {
        mouseHandler.actualizarCabecera();
    }

    public void refresh() {
        mouseHandler.actualizarCabecera();
        imagePanel.repaint();
    }

    public ImageDisplayPanel getImagePanel() {
        return imagePanel;
    }

}
