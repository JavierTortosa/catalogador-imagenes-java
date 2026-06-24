package vista.panels;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;
import modelo.proyecto.CommentOverlay;
import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.ProjectModel;
import modelo.proyecto.SelectionState;

/**
 * Manejador de eventos de ratón para el editor de checkboxes del modo cliente.
 * Gestiona añadir/borrar/arrastrar checkboxes, comentarios y PVP
 * sobre la imagen, así como el menú popup contextual.
 */
public class CheckboxEditorMouseHandler extends java.awt.event.MouseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CheckboxEditorMouseHandler.class);

    private final VisorModel model;
    private final IProjectManager projectManager;
    private final ComponentRegistry registry;
    private final ImageDisplayPanel imagePanel;
    private final JLabel headerLabel;

    private int draggingOverlayIndex = -1;
    private boolean draggingComment = false;
    private int dragOffsetX, dragOffsetY;

    public CheckboxEditorMouseHandler(VisorModel model, IProjectManager projectManager,
                                       ComponentRegistry registry, ImageDisplayPanel imagePanel,
                                       JLabel headerLabel) {
        this.model = model;
        this.projectManager = projectManager;
        this.registry = registry;
        this.imagePanel = imagePanel;
        this.headerLabel = headerLabel;
    } // --- FIN de metodo CheckboxEditorMouseHandler (constructor) ---


    void actualizarCabecera() {
        ProjectModel project = projectManager != null ? projectManager.getCurrentProject() : null;
        if (project != null && imagePanel.getCurrentImageKey() != null) {
            String code = project.getCodigoImagen(
                    ProjectModel.normalizarClaveImagen(imagePanel.getCurrentImageKey()));
            headerLabel.setText("Imagen: " + code);
        } else {
            headerLabel.setText(" ");
        }
    } // --- FIN de metodo actualizarCabecera ---


    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        if (!isActive()) return;
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
            // Try overlay first, then comment
            int idx = findOverlayAt(e.getX(), e.getY());
            if (idx >= 0) {
                draggingOverlayIndex = idx;
                draggingComment = false;
                Point2D dst = overlayScreenPos(idx);
                if (dst != null) {
                    dragOffsetX = e.getX() - (int) Math.round(dst.getX());
                    dragOffsetY = e.getY() - (int) Math.round(dst.getY());
                }
                return;
            }
            // Check if clicking on comment
            if (isOverComment(e.getX(), e.getY())) {
                draggingComment = true;
                draggingOverlayIndex = -1;
                Point2D dst = commentScreenPos();
                if (dst != null) {
                    dragOffsetX = e.getX() - (int) Math.round(dst.getX());
                    dragOffsetY = e.getY() - (int) Math.round(dst.getY());
                }
                return;
            }
        }
    } // --- FIN de metodo mousePressed ---


    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        if (!isActive()) return;
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
            if (draggingOverlayIndex >= 0 || draggingComment) {
                projectManager.notificarModificacion();
                imagePanel.repaint();
                draggingOverlayIndex = -1;
                draggingComment = false;
                return;
            }
            // Click: cycle overlay state UNDEFINED → SELECTED → DISCARDED → UNDEFINED
            int idx = findOverlayAt(e.getX(), e.getY());
            if (idx >= 0 && projectManager.getCurrentProject() != null) {
                String imageKey = imagePanel.getCurrentImageKey();
                if (imageKey != null) {
                    String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
                    var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(canonicalKey);
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
                        String imgCode = projectManager.getCurrentProject()
                                .getCodigoImagen(canonicalKey);
                        String compositeKey = imgCode + "_" + ov.getCheckboxCode();
                        projectManager.getCurrentProject().getClientSelection().getImages().put(compositeKey, next);
                        derivarEstadoImagen(canonicalKey);
                        projectManager.notificarModificacion();
                        actualizarModeloTabla();
                        imagePanel.repaint();
                    }
                }
            }
        }
        draggingOverlayIndex = -1;
        draggingComment = false;
    } // --- FIN de metodo mouseReleased ---


    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (!isActive()) return;
        if (projectManager.getCurrentProject() == null) return;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return;

        if (draggingOverlayIndex >= 0) {
            dragOverlay(e, imageKey);
        } else if (draggingComment) {
            dragComment(e, imageKey);
        }
    } // --- FIN de metodo mouseDragged ---


    private void dragOverlay(java.awt.event.MouseEvent e, String imageKey) {
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        if (draggingOverlayIndex >= overlays.size()) return;
        AffineTransform transform = imagePanel.getCurrentImageTransform();
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
    } // --- FIN de metodo dragOverlay ---


    private void dragComment(java.awt.event.MouseEvent e, String imageKey) {
        var clientSel = projectManager.getCurrentProject().getClientSelection();
        CommentOverlay co = clientSel.getCommentOverlays().get(imageKey);
        if (co == null) return;
        AffineTransform transform = imagePanel.getCurrentImageTransform();
        if (transform == null) return;
        try {
            Point2D invSrc = new Point2D.Double(
                    e.getX() - dragOffsetX, e.getY() - dragOffsetY);
            Point2D invDst = new Point2D.Double();
            transform.inverseTransform(invSrc, invDst);
            co.setImageX((int) Math.round(invDst.getX()));
            co.setImageY((int) Math.round(invDst.getY()));
            imagePanel.repaint();
        } catch (NoninvertibleTransformException ex) {
            // ignore
        }
    } // --- FIN de metodo dragComment ---


    private boolean isActive() {
        return model.getCurrentWorkMode() == WorkMode.CLIENTE;
    } // --- FIN de metodo isActive ---


    private int findOverlayAt(int sx, int sy) {
        if (projectManager.getCurrentProject() == null) return -1;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return -1;
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        if (overlays == null) return -1;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            var ov = overlays.get(i);
            Point2D dst = transformPoint(ov.getImageX(), ov.getImageY());
            if (dst == null) continue;
            int cx = (int) Math.round(dst.getX());
            int cy = (int) Math.round(dst.getY());
            int half = Math.max(ov.getSize() / 2, 16);
            if (sx >= cx - half && sx <= cx + half && sy >= cy - half && sy <= cy + half) {
                return i;
            }
            // Also check PVP label area (to the right)
            if (ov.getPrice() > 0) {
                int pvpX = cx + half + 4;
                int pvpY = cy - 12;
                if (sx >= pvpX && sx <= pvpX + 80 && sy >= pvpY && sy <= pvpY + 24) {
                    return i;
                }
            }
        }
        return -1;
    } // --- FIN de metodo findOverlayAt ---


    private boolean isOverComment(int sx, int sy) {
        if (projectManager.getCurrentProject() == null) return false;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return false;
        var co = projectManager.getCurrentProject().getClientSelection().getCommentOverlays().get(imageKey);
        if (co == null || co.getText() == null || co.getText().isEmpty()) return false;
        Point2D dst = transformPoint(co.getImageX(), co.getImageY());
        if (dst == null) return false;
        int cx = (int) Math.round(dst.getX());
        int cy = (int) Math.round(dst.getY());
        // Approximate hit area for comment label
        int approxW = Math.min(co.getText().length() * 7 + 12, 300);
        int approxH = 30;
        return sx >= cx - 4 && sx <= cx - 4 + approxW
            && sy >= cy - 4 && sy <= cy - 4 + approxH;
    } // --- FIN de metodo isOverComment ---


    private Point2D transformPoint(int imgX, int imgY) {
        AffineTransform transform = imagePanel.getCurrentImageTransform();
        if (transform == null) return null;
        Point2D src = new Point2D.Double(imgX, imgY);
        Point2D dst = new Point2D.Double();
        transform.transform(src, dst);
        return dst;
    } // --- FIN de metodo transformPoint ---


    private Point2D overlayScreenPos(int index) {
        if (projectManager.getCurrentProject() == null) return null;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return null;
        var overlays = projectManager.getCurrentProject().getClientSelection().getImageCheckboxes(imageKey);
        if (overlays == null || index >= overlays.size()) return null;
        var ov = overlays.get(index);
        return transformPoint(ov.getImageX(), ov.getImageY());
    } // --- FIN de metodo overlayScreenPos ---


    private Point2D commentScreenPos() {
        if (projectManager.getCurrentProject() == null) return null;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return null;
        var co = projectManager.getCurrentProject().getClientSelection().getCommentOverlays().get(imageKey);
        if (co == null) return null;
        return transformPoint(co.getImageX(), co.getImageY());
    } // --- FIN de metodo commentScreenPos ---


    private void showPopup(java.awt.event.MouseEvent e) {
        ProjectModel project = projectManager.getCurrentProject();
        if (project == null) return;
        String imageKey = imagePanel.getCurrentImageKey();
        if (imageKey == null) return;
        var overlays = project.getClientSelection().getImageCheckboxes(imageKey);
        int hitIdx = findOverlayAt(e.getX(), e.getY());
        boolean overComment = isOverComment(e.getX(), e.getY());

        JPopupMenu popup = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("A\u00f1adir Checkbox (48x48)");
        addItem.addActionListener(ev -> addCheckbox(overlays, e.getX(), e.getY(), project, imageKey, 48));
        popup.add(addItem);

        JMenuItem addSmallItem = new JMenuItem("A\u00f1adir Checkbox (32x32)");
        addSmallItem.addActionListener(ev -> addCheckbox(overlays, e.getX(), e.getY(), project, imageKey, 32));
        popup.add(addSmallItem);

        popup.addSeparator();

        JMenuItem commentItem = new JMenuItem("A\u00f1adir Comentario...");
        commentItem.addActionListener(ev -> addComment(e.getX(), e.getY(), project, imageKey));
        popup.add(commentItem);

        if (hitIdx >= 0) {
            popup.addSeparator();
            int idx = hitIdx;

            JMenuItem removeItem = new JMenuItem("Borrar Checkbox");
            removeItem.addActionListener(ev -> {
                var ov = overlays.get(idx);
                overlays.remove(idx);
                // Eliminar entrada compuesta del cliente
                String imgCode = project.getImageCodes().getOrDefault(imageKey, "");
                if (imgCode.isEmpty()) {
                    imgCode = project.getCodigoImagen(ProjectModel.normalizarClaveImagen(imageKey));
                }
                String compositeKey = imgCode + "_" + ov.getCheckboxCode();
                project.getClientSelection().getImages().remove(compositeKey);
                derivarEstadoImagen(imageKey);
                projectManager.notificarModificacion();
                actualizarModeloTabla();
                imagePanel.repaint();
            });
            popup.add(removeItem);

            JMenuItem priceItem = new JMenuItem("Poner PVP...");
            priceItem.addActionListener(ev -> setPrice(overlays.get(idx)));
            popup.add(priceItem);
        }

        if (overComment) {
            popup.addSeparator();
            JMenuItem editCommentItem = new JMenuItem("Editar comentario...");
            editCommentItem.addActionListener(ev -> editComment(project, imageKey));
            popup.add(editCommentItem);

            JMenuItem removeCommentItem = new JMenuItem("Borrar comentario");
            removeCommentItem.addActionListener(ev -> {
                project.getClientSelection().getCommentOverlays().remove(imageKey);
                projectManager.notificarModificacion();
                imagePanel.repaint();
            });
            popup.add(removeCommentItem);
        }

        popup.show(imagePanel, e.getX(), e.getY());
    } // --- FIN de metodo showPopup ---


    private void addCheckbox(List<ImageCheckboxOverlay> overlays, int screenX, int screenY,
                              ProjectModel project, String imageKey, int size) {
        AffineTransform transform = imagePanel.getCurrentImageTransform();
        if (transform == null) return;
        try {
            Point2D invSrc = new Point2D.Double(screenX, screenY);
            Point2D invDst = new Point2D.Double();
            transform.inverseTransform(invSrc, invDst);

            int seq = overlays.size() + 1;
            String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
            String imgCode = project.getCodigoImagen(canonicalKey);
            String cbCode = String.format("cb%02d", seq);

            var ov = new ImageCheckboxOverlay(
                    (int) Math.round(invDst.getX()), (int) Math.round(invDst.getY()),
                    SelectionState.UNDEFINED, "", cbCode, "", 0.0, size);
            overlays.add(ov);
            projectManager.notificarModificacion();

            // Añadir entrada compuesta al panel de selección del cliente: imgCode_cbCode
            String compositeKey = imgCode + "_" + cbCode;
            project.getClientSelection().getImages().put(compositeKey, ov.getState());

            derivarEstadoImagen(canonicalKey);

            actualizarModeloTabla();
            imagePanel.repaint();
        } catch (NoninvertibleTransformException ex) {
            // ignore
        }
    } // --- FIN de metodo addCheckbox ---


    /**
     * Deriva el estado de la imagen a partir del estado de sus checkboxes internos.
     * - Al menos un SELECTED → imagen SELECTED
     * - Ningún SELECTED, al menos un UNDEFINED → imagen UNDEFINED
     * - Todos DISCARDED → imagen DISCARDED
     * También sincroniza las entradas compuestas (imgCode_cbCode) con el estado de cada overlay.
     */
    private void derivarEstadoImagen(String imageKey) {
        if (projectManager.getCurrentProject() == null) return;
        var clientSel = projectManager.getCurrentProject().getClientSelection();
        if (clientSel == null) return;
        String canonicalKey = ProjectModel.normalizarClaveImagen(imageKey);
        var checkboxes = clientSel.getImageCheckboxes(canonicalKey);
        if (checkboxes == null || checkboxes.isEmpty()) return;

        String imgCode = projectManager.getCurrentProject()
                .getCodigoImagen(canonicalKey);

        boolean hasSelected = false;
        boolean hasUndefined = false;
        for (var cb : checkboxes) {
            String compositeKey = imgCode + "_" + cb.getCheckboxCode();
            clientSel.getImages().put(compositeKey, cb.getState());
            clientSel.getImages().remove("_" + cb.getCheckboxCode());
            clientSel.getImages().remove(cb.getCheckboxCode());

            switch (cb.getState()) {
                case SELECTED  -> hasSelected = true;
                case UNDEFINED -> hasUndefined = true;
                default -> {}
            }
        }

        SelectionState derived;
        if (hasSelected) {
            derived = SelectionState.SELECTED;
        } else if (hasUndefined) {
            derived = SelectionState.UNDEFINED;
        } else {
            derived = SelectionState.DISCARDED;
        }
        clientSel.getImages().put(canonicalKey, derived);
    } // --- FIN de metodo derivarEstadoImagen ---


    private void addComment(int screenX, int screenY, ProjectModel project, String imageKey) {
        // Create a dialog for comment input
        String text = showTextInputDialog("A\u00f1adir Comentario", "Comentario:", "");
        if (text == null || text.trim().isEmpty()) return;

        AffineTransform transform = imagePanel.getCurrentImageTransform();
        if (transform == null) return;
        try {
            Point2D invSrc = new Point2D.Double(screenX, screenY);
            Point2D invDst = new Point2D.Double();
            transform.inverseTransform(invSrc, invDst);

            CommentOverlay co = new CommentOverlay(
                    (int) Math.round(invDst.getX()),
                    (int) Math.round(invDst.getY()),
                    text.trim());
            project.getClientSelection().getCommentOverlays().put(imageKey, co);
            projectManager.notificarModificacion();
            imagePanel.repaint();
        } catch (NoninvertibleTransformException ex) {
            // ignore
        }
    } // --- FIN de metodo addComment ---


    private void editComment(ProjectModel project, String imageKey) {
        CommentOverlay co = project.getClientSelection().getCommentOverlays().get(imageKey);
        if (co == null) return;
        String current = co.getText();
        String result = showTextInputDialog("Editar Comentario", "Comentario:", current);
        if (result != null) {
            if (result.trim().isEmpty()) {
                project.getClientSelection().getCommentOverlays().remove(imageKey);
            } else {
                co.setText(result.trim());
            }
            projectManager.notificarModificacion();
            imagePanel.repaint();
        }
    } // --- FIN de metodo editComment ---


    private void setPrice(ImageCheckboxOverlay ov) {
        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.insets = new java.awt.Insets(4, 4, 4, 4);
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new javax.swing.JLabel("Precio (\u20AC):"), gc);
        gc.gridx = 1;
        javax.swing.JTextField priceField = new javax.swing.JTextField(10);
        if (ov.getPrice() > 0) {
            priceField.setText(String.format("%.2f", ov.getPrice()));
        }
        panel.add(priceField, gc);

        int result = JOptionPane.showConfirmDialog(imagePanel, panel,
                "Poner PVP", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
                ov.setPrice(Math.max(0, price));
            } catch (NumberFormatException ex) {
                ov.setPrice(0.0);
            }
            projectManager.notificarModificacion();
            imagePanel.repaint();
        }
    } // --- FIN de metodo setPrice ---


    private String showTextInputDialog(String title, String label, String current) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new javax.swing.JLabel(label), BorderLayout.NORTH);
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(current, 4, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(new javax.swing.JScrollPane(textArea), BorderLayout.CENTER);

        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(imagePanel, title);
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        if (selectedValue instanceof Integer && (Integer) selectedValue == JOptionPane.OK_OPTION) {
            return textArea.getText();
        }
        return null;
    } // --- FIN de metodo showTextInputDialog ---


    private void actualizarModeloTabla() {
        if (registry == null) return;
        javax.swing.JTable t;
        t = registry.get("table.cliente.proyecto.seleccion");
        if (t != null && t.getModel() instanceof vista.models.ProyectoClienteTableModel pm1) {
            pm1.refrescar();
        }
        t = registry.get("table.cliente.proyecto.descartes");
        if (t != null && t.getModel() instanceof vista.models.ProyectoClienteTableModel pm2) {
            pm2.refrescar();
        }
        t = registry.get("table.cliente.cliente.seleccion");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm1) {
            cm1.refrescar();
        }
        t = registry.get("table.cliente.cliente.descartes");
        if (t != null && t.getModel() instanceof vista.models.ClienteTableModel cm2) {
            cm2.refrescar();
        }
    } // --- FIN de metodo actualizarModeloTabla ---

} // --- FIN de clase CheckboxEditorMouseHandler ---
