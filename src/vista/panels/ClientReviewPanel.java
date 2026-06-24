package vista.panels;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;
import servicios.ProjectManager;
import vista.theme.ThemeManager;

/**
 * Panel dedicado para la revisión por parte del cliente.
 * Muestra la imagen seleccionada y permite gestionar checkboxes de selección
 * con menú popup para añadir, borrar, comentar y tarifar.
 */
public class ClientReviewPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ClientReviewPanel.class);

    private final VisorModel model;
    private final ProjectManager projectManager;
    private final ImageDisplayPanel imagePanel;

    private int draggingOverlayIndex = -1;
    private int dragOffsetX, dragOffsetY;
    private boolean overlayMouseListenersInitialized = false;

    public ClientReviewPanel(ThemeManager themeManager, VisorModel model, ProjectManager projectManager) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.projectManager = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");

        setLayout(new BorderLayout());
        setBackground(themeManager.getTemaActual().colorFondoSecundario());

        TitledBorder border = BorderFactory.createTitledBorder("Revisión de Checkboxes");
        setBorder(border);

        this.imagePanel = new ImageDisplayPanel(themeManager, model);
        this.imagePanel.setProjectManager(projectManager);
        add(imagePanel, BorderLayout.CENTER);

        initClienteOverlayMouseHandling();
    } // --- Fin del constructor ClientReviewPanel ---

    private void initClienteOverlayMouseHandling() {
        if (overlayMouseListenersInitialized) return;
        overlayMouseListenersInitialized = true;

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isActive()) return;
                if (e.isPopupTrigger()) {
                    showOverlayPopup(e);
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int idx = findOverlayAt(e.getX(), e.getY());
                    if (idx >= 0) {
                        draggingOverlayIndex = idx;
                        Point2D dst = overlayScreenPos(idx);
                        if (dst != null) {
                            dragOffsetX = e.getX() - (int) Math.round(dst.getX());
                            dragOffsetY = e.getY() - (int) Math.round(dst.getY());
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isActive()) return;
                if (e.isPopupTrigger()) {
                    showOverlayPopup(e);
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (draggingOverlayIndex >= 0) {
                        draggingOverlayIndex = -1;
                        return;
                    }
                    int idx = findOverlayAt(e.getX(), e.getY());
                    if (idx >= 0 && projectManager.getCurrentProject() != null) {
                        String imageKey = getCurrentImageKey();
                        if (imageKey != null) {
                            var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
                            if (idx < overlays.size()) {
                                var ov = overlays.get(idx);
                                SelectionState current = ov.getState();
                                SelectionState next;
                                switch (current) {
                                    case SELECTED  -> next = SelectionState.DISCARDED;
                                    case DISCARDED -> next = SelectionState.UNDEFINED;
                                    default        -> next = SelectionState.SELECTED;
                                }
                                ov.setState(next);
                                // Sincronizar entrada compuesta
                                String imgCode = projectManager.getCurrentProject()
                                        .getCodigoImagen(ProjectModel.normalizarClaveImagen(imageKey));
                                String compositeKey = imgCode + "_" + ov.getCheckboxCode();
                                projectManager.getCurrentProject().getClientSelection().getImages().put(compositeKey, next);
                                imagePanel.repaint();
                            }
                        }
                    }
                }
                draggingOverlayIndex = -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isActive() || draggingOverlayIndex < 0) return;
                if (projectManager.getCurrentProject() == null) return;
                String imageKey = getCurrentImageKey();
                if (imageKey == null) return;
                var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
                if (draggingOverlayIndex >= overlays.size()) return;
                AffineTransform transform = getImageTransform();
                if (transform == null) return;

                try {
                    Point2D invSrc = new Point2D.Double(
                            e.getX() - dragOffsetX, e.getY() - dragOffsetY);
                    Point2D invDst = new Point2D.Double();
                    transform.inverseTransform(invSrc, invDst);
                    var ov = overlays.get(draggingOverlayIndex);
                    ov.setImageX((int) Math.round(invDst.getX()));
                    ov.setImageY((int) Math.round(invDst.getY()));
                    imagePanel.repaint();
                } catch (NoninvertibleTransformException ex) {
                    // ignore
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    } // --- Fin del método initClienteOverlayMouseHandling ---

    private boolean isActive() {
        return model.getCurrentWorkMode() == WorkMode.CLIENTE && model.isClienteCheckboxVisible();
    } // --- Fin del método isActive ---

    private String getCurrentImageKey() {
        return imagePanel.getCurrentImageKey();
    } // --- Fin del método getCurrentImageKey ---

    private AffineTransform getImageTransform() {
        return imagePanel.getCurrentImageTransform();
    } // --- Fin del método getImageTransform ---

    private int findOverlayAt(int sx, int sy) {
        if (projectManager.getCurrentProject() == null) return -1;
        String imageKey = getCurrentImageKey();
        if (imageKey == null) return -1;
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        if (overlays == null) return -1;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            Point2D dst = overlayScreenPos(i);
            if (dst == null) continue;
            int cx = (int) Math.round(dst.getX());
            int cy = (int) Math.round(dst.getY());
            int half = 24;
            if (sx >= cx - half && sx <= cx + half && sy >= cy - half && sy <= cy + half) {
                return i;
            }
        }
        return -1;
    } // --- Fin del método findOverlayAt ---

    private Point2D overlayScreenPos(int index) {
        if (projectManager.getCurrentProject() == null) return null;
        String imageKey = getCurrentImageKey();
        if (imageKey == null) return null;
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        if (overlays == null || index >= overlays.size()) return null;
        AffineTransform transform = getImageTransform();
        if (transform == null) return null;
        var ov = overlays.get(index);
        Point2D src = new Point2D.Double(ov.getImageX(), ov.getImageY());
        Point2D dst = new Point2D.Double();
        transform.transform(src, dst);
        return dst;
    } // --- Fin del método overlayScreenPos ---

    private void showOverlayPopup(MouseEvent e) {
        if (projectManager.getCurrentProject() == null) return;
        String imageKey = getCurrentImageKey();
        if (imageKey == null) return;
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        int hitIdx = findOverlayAt(e.getX(), e.getY());
        JPopupMenu popup = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Añadir checkbox aquí");
        addItem.addActionListener(ev -> {
            AffineTransform transform = getImageTransform();
            if (transform == null) return;
            try {
                Point2D invSrc = new Point2D.Double(e.getX(), e.getY());
                Point2D invDst = new Point2D.Double();
                transform.inverseTransform(invSrc, invDst);
                String imgCode = projectManager.getCurrentProject()
                        .getCodigoImagen(ProjectModel.normalizarClaveImagen(getCurrentImageKey()));
                int seq = overlays.size() + 1;
                String cbCode = String.format("cb%02d", seq);
                var ov = new ImageCheckboxOverlay(
                        (int) Math.round(invDst.getX()), (int) Math.round(invDst.getY()),
                        SelectionState.UNDEFINED, "", cbCode, "", 0.0, 32);
                overlays.add(ov);
                String compositeKey = imgCode + "_" + cbCode;
                projectManager.getCurrentProject().getClientSelection().getImages().put(compositeKey, ov.getState());
                imagePanel.repaint();
            } catch (NoninvertibleTransformException ex) {
                // ignore
            }
        });
        popup.add(addItem);

        if (hitIdx >= 0) {
            JMenuItem removeItem = new JMenuItem("Eliminar checkbox");
            int idx = hitIdx;
            removeItem.addActionListener(ev -> {
                var ov = overlays.get(idx);
                overlays.remove(idx);
                // Eliminar entrada compuesta del cliente
                String imgCode = projectManager.getCurrentProject()
                        .getCodigoImagen(ProjectModel.normalizarClaveImagen(getCurrentImageKey()));
                String compositeKey = imgCode + "_" + ov.getCheckboxCode();
                projectManager.getCurrentProject().getClientSelection().getImages().remove(compositeKey);
                imagePanel.repaint();
            });
            popup.add(removeItem);

            JMenuItem priceItem = new JMenuItem("Editar precio...");
            priceItem.addActionListener(ev -> {
                var ov = overlays.get(idx);
                String result = JOptionPane.showInputDialog(this,
                        "Precio / etiqueta:", ov.getLabel());
                if (result != null) {
                    ov.setLabel(result);
                    imagePanel.repaint();
                }
            });
            popup.add(priceItem);
        }

        JMenuItem commentItem = new JMenuItem("Añadir comentario...");
        commentItem.addActionListener(ev -> {
            String currentComment = projectManager.getCurrentProject().getClientSelection()
                    .getComments().getOrDefault(imageKey, "");
            String result = JOptionPane.showInputDialog(this,
                    "Comentario para esta imagen:", currentComment);
            if (result != null) {
                projectManager.getCurrentProject().getClientSelection().getComments().put(imageKey, result);
                imagePanel.repaint();
            }
        });
        popup.add(commentItem);

        popup.show(this, e.getX(), e.getY());
    } // --- Fin del método showOverlayPopup ---

    /**
     * Fuerza el repintado del panel de imagen interno.
     */
    public void refresh() {
        imagePanel.repaint();
    } // --- Fin del método refresh ---

} // --- FIN DE LA CLASE ClientReviewPanel ---
