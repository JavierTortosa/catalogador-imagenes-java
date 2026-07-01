package vista.panels;

import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.BorderLayout;

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
import modelo.proyecto.Mensaje;
import modelo.proyecto.ProjectImage;
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
    private boolean draggingPan = false;
    private int dragOffsetX, dragOffsetY;
    private int panLastX, panLastY;

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


    private ProjectImage getProjectImage(String imageKey) {
        if (projectManager == null || projectManager.getCurrentProject() == null || imageKey == null) return null;
        return projectManager.getCurrentProject().getMasterImages()
                .get(ProjectModel.normalizarClaveImagen(imageKey));
    } // --- Fin del método getProjectImage ---


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
            // Click en espacio vacío → iniciar paneo
            draggingPan = true;
            draggingOverlayIndex = -1;
            draggingComment = false;
            panLastX = e.getX();
            panLastY = e.getY();
        }
    } // --- FIN de metodo mousePressed ---


    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        if (!isActive()) return;
        if (e.isPopupTrigger()) {
            showPopup(e);
            return;
        }
        if (draggingPan) {
            draggingPan = false;
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
            String imageKey = imagePanel.getCurrentImageKey();
            var pi = getProjectImage(imageKey);
            if (pi == null) return;
            var overlays = pi.getCheckboxes();
            int idx = findOverlayAt(e.getX(), e.getY());
            if (idx >= 0 && idx < overlays.size()) {
                var ov = overlays.get(idx);
                SelectionState current = ov.getState();
                SelectionState next;
                switch (current) {
                    case SELECTED  -> next = SelectionState.DISCARDED;
                    case DISCARDED -> next = SelectionState.UNDEFINED;
                    default        -> next = SelectionState.SELECTED;
                }
                ov.setState(next);
                derivarEstadoImagen(imageKey);
                projectManager.notificarModificacion();
                actualizarModeloTabla();
                imagePanel.repaint();
            }
        }
        draggingOverlayIndex = -1;
        draggingComment = false;
    } // --- FIN de metodo mouseReleased ---


    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (!isActive()) return;

        if (draggingPan) {
            int dx = e.getX() - panLastX;
            int dy = e.getY() - panLastY;
            panLastX = e.getX();
            panLastY = e.getY();
            imagePanel.setEditorOffset(dx, dy);
            return;
        }

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
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        var overlays = pi.getCheckboxes();
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
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        CommentOverlay co = pi.getCommentOverlay();
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


    boolean isActive() {
        return model.getCurrentWorkMode() == WorkMode.CLIENTE;
    } // --- FIN de metodo isActive ---


    private int findOverlayAt(int sx, int sy) {
        String imageKey = imagePanel.getCurrentImageKey();
        var pi = getProjectImage(imageKey);
        if (pi == null) return -1;
        var overlays = pi.getCheckboxes();
        if (overlays == null || overlays.isEmpty()) return -1;
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
        String imageKey = imagePanel.getCurrentImageKey();
        var pi = getProjectImage(imageKey);
        if (pi == null) return false;
        var co = pi.getCommentOverlay();
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
        String imageKey = imagePanel.getCurrentImageKey();
        var pi = getProjectImage(imageKey);
        if (pi == null) return null;
        var overlays = pi.getCheckboxes();
        if (overlays == null || index >= overlays.size()) return null;
        var ov = overlays.get(index);
        return transformPoint(ov.getImageX(), ov.getImageY());
    } // --- FIN de metodo overlayScreenPos ---


    private Point2D commentScreenPos() {
        String imageKey = imagePanel.getCurrentImageKey();
        var pi = getProjectImage(imageKey);
        if (pi == null) return null;
        var co = pi.getCommentOverlay();
        if (co == null) return null;
        return transformPoint(co.getImageX(), co.getImageY());
    } // --- FIN de metodo commentScreenPos ---


    private void showPopup(java.awt.event.MouseEvent e) {
        String imageKey = imagePanel.getCurrentImageKey();
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        var overlays = pi.getCheckboxes();
        int hitIdx = findOverlayAt(e.getX(), e.getY());
        boolean overComment = isOverComment(e.getX(), e.getY());

        JPopupMenu popup = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("A\u00f1adir Checkbox");
        addItem.addActionListener(ev -> addCheckbox(e.getX(), e.getY(), imageKey, 32));
        popup.add(addItem);

        popup.addSeparator();

        JMenuItem commentItem = new JMenuItem("A\u00f1adir Comentario Global...");
        commentItem.addActionListener(ev -> addComment(e.getX(), e.getY(), imageKey));
        popup.add(commentItem);

        if (hitIdx >= 0) {
            popup.addSeparator();
            int idx = hitIdx;
            var ov = overlays.get(idx);

            JMenuItem removeItem = new JMenuItem("Borrar Checkbox");
            removeItem.addActionListener(ev -> {
                overlays.remove(idx);
                derivarEstadoImagen(imageKey);
                projectManager.notificarModificacion();
                actualizarModeloTabla();
                imagePanel.repaint();
            });
            popup.add(removeItem);

            JMenuItem priceItem = new JMenuItem("Poner PVP...");
            priceItem.addActionListener(ev -> setPrice(ov));
            popup.add(priceItem);

            popup.addSeparator();

            boolean hasMsg = ov.hasThreadMessages();
            JMenuItem addMsgItem = new JMenuItem(hasMsg ? "A\u00f1adir otro mensaje al modelo..." : "A\u00f1adir mensaje al modelo...");
            addMsgItem.addActionListener(ev -> addCheckboxMessage(ov));
            popup.add(addMsgItem);

            if (hasMsg) {
                JMenuItem viewMsgItem = new JMenuItem("Ver mensajes del modelo...");
                viewMsgItem.addActionListener(ev -> mostrarDialogoMensajes(ov));
                popup.add(viewMsgItem);

                JMenuItem removeMsgItem = new JMenuItem("Borrar mensaje del modelo");
                removeMsgItem.addActionListener(ev -> removeCheckboxMessage(ov));
                popup.add(removeMsgItem);
            }
        }

        if (overComment) {
            popup.addSeparator();
            JMenuItem editCommentItem = new JMenuItem("Editar comentario...");
            editCommentItem.addActionListener(ev -> editComment(imageKey));
            popup.add(editCommentItem);

            JMenuItem removeCommentItem = new JMenuItem("Borrar comentario");
            removeCommentItem.addActionListener(ev -> {
                var p = getProjectImage(imageKey);
                if (p != null) p.setCommentOverlay(null);
                projectManager.notificarModificacion();
                imagePanel.repaint();
            });
            popup.add(removeCommentItem);
        }

        popup.show(imagePanel, e.getX(), e.getY());
    } // --- FIN de metodo showPopup ---


    private void addCheckbox(int screenX, int screenY, String imageKey, int size) {
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        AffineTransform transform = imagePanel.getCurrentImageTransform();
        if (transform == null) return;
        try {
            Point2D invSrc = new Point2D.Double(screenX, screenY);
            Point2D invDst = new Point2D.Double();
            transform.inverseTransform(invSrc, invDst);

            var overlays = pi.getCheckboxes();
            int seq = overlays.size() + 1;
            String cbCode = String.format("cb%02d", seq);

            var ov = new ImageCheckboxOverlay(
                    (int) Math.round(invDst.getX()), (int) Math.round(invDst.getY()),
                    SelectionState.UNDEFINED, "", cbCode, "", 0.0, size);
            overlays.add(ov);
            projectManager.notificarModificacion();

            derivarEstadoImagen(imageKey);

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
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        var checkboxes = pi.getCheckboxes();
        if (checkboxes == null || checkboxes.isEmpty()) return;

        boolean hasSelected = false;
        boolean hasUndefined = false;
        for (var cb : checkboxes) {
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
        pi.setEstadoCliente(derived);
    } // --- FIN de metodo derivarEstadoImagen ---


    private void addComment(int screenX, int screenY, String imageKey) {
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
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
            pi.setCommentOverlay(co);
            projectManager.notificarModificacion();
            imagePanel.repaint();
        } catch (NoninvertibleTransformException ex) {
            // ignore
        }
    } // --- FIN de metodo addComment ---


    private void editComment(String imageKey) {
        var pi = getProjectImage(imageKey);
        if (pi == null) return;
        CommentOverlay co = pi.getCommentOverlay();
        if (co == null) return;
        String current = co.getText();
        String result = showTextInputDialog("Editar Comentario", "Comentario:", current);
        if (result != null) {
            if (result.trim().isEmpty()) {
                pi.setCommentOverlay(null);
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


    private void addCheckboxMessage(ImageCheckboxOverlay ov) {
        String text = showTextInputDialog("A\u00f1adir mensaje al checkbox",
                "Mensaje (como fot\u00f3grafo):", "");
        if (text == null || text.trim().isEmpty()) return;
        int iter = projectManager.getCurrentProject() != null
                ? projectManager.getCurrentProject().getSharedIteration() : 0;
        ov.addMensaje("nosotros", text.trim(), iter);
        projectManager.notificarModificacion();
        actualizarModeloTabla();
        imagePanel.repaint();
    } // --- FIN de metodo addCheckboxMessage ---


    private void removeCheckboxMessage(ImageCheckboxOverlay ov) {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(imagePanel,
                "\u00bfBorrar todos los mensajes de este checkbox?",
                "Borrar mensaje", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
        ov.getCommentThreadAccess().setMessages(new java.util.ArrayList<>());
        ov.setComment("");
        projectManager.notificarModificacion();
        actualizarModeloTabla();
        imagePanel.repaint();
    } // --- FIN de metodo removeCheckboxMessage ---


    private void mostrarDialogoMensajes(ImageCheckboxOverlay ov) {
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(imagePanel);
        if (owner == null && registry != null) {
            owner = (Window) registry.get("frame.principal");
        }
        if (owner == null) return;
        String title = "Mensajes del checkbox " + ov.getCheckboxCode();
        int iter = projectManager.getCurrentProject() != null
                ? projectManager.getCurrentProject().getSharedIteration() : 0;
        MsgPopupDialog dlg = new MsgPopupDialog(owner, title,
                ov.getCommentThreadAccess(), projectManager, iter, () -> {
            projectManager.notificarModificacion();
            actualizarModeloTabla();
            imagePanel.repaint();
        });
        dlg.setVisible(true);
    } // --- FIN de metodo mostrarDialogoMensajes ---


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
