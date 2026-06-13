package vista.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;
import servicios.db.TagDAO;
import vista.components.TagSearchEngine.InputMode;

public class TagIntelliSenseField extends JTextField {

    private static final long serialVersionUID = 3L;
    private static final Logger logger = LoggerFactory.getLogger(TagIntelliSenseField.class);

    private static final String MSG_NOT_FOUND = "(no existe)";
    private static final String MSG_NO_CHILDREN = "(sin hijos)";
    private static final String MSG_SELECT = "Seleccionar...";

    private final TagSearchEngine engine = new TagSearchEngine();
    private final JPopupMenu popupMenu;
    private final JList<PathResult> suggestionList;
    private final DefaultListModel<PathResult> suggestionModel;

    private Tag lastSelectedTag = null;
    private boolean suppressListener = false;
    private boolean pendingCreationCheck = false;
    private boolean inCreationDialog = false;
    private Runnable onEnterNoPopupAction;

    public TagIntelliSenseField() {
        super();

        suggestionModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(getFont());
        suggestionList.setFocusable(false);
        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PathResult pr) {
                    setText(pr.displayName());
                }
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new java.awt.BorderLayout());
        popupMenu.add(scrollPane, java.awt.BorderLayout.CENTER);
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);

        setFocusTraversalKeysEnabled(false);

        getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTextChanged(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { onTextChanged(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '.' && getText().endsWith(".")) {
                    e.consume();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                pendingCreationCheck = false;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        if (popupMenu.isVisible()) {
                            int idx = suggestionList.getSelectedIndex();
                            int max = suggestionModel.getSize() - 1;
                            suggestionList.setSelectedIndex(Math.min(idx + 1, max));
                            suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_UP:
                        if (popupMenu.isVisible()) {
                            int idx = suggestionList.getSelectedIndex();
                            suggestionList.setSelectedIndex(Math.max(idx - 1, 0));
                            suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_TAB:
                        if (popupMenu.isVisible()) {
                            applySelectedSuggestion();
                            e.consume();
                        } else {
                            if (e.isShiftDown()) {
                                transferFocusBackward();
                            } else {
                                transferFocus();
                            }
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_ENTER:
                        if (popupMenu.isVisible()) {
                            boolean applied = applySelectedSuggestion();
                            if (applied) {
                                pendingCreationCheck = true;
                                SwingUtilities.invokeLater(TagIntelliSenseField.this::checkAndHandleCreation);
                            }
                        } else if (onEnterNoPopupAction != null) {
                            onEnterNoPopupAction.run();
                        }
                        break;

                    case KeyEvent.VK_PERIOD:
                        if (popupMenu.isVisible()) {
                            PathResult sel = suggestionList.getSelectedValue();
                            if (sel != null && !MSG_NOT_FOUND.equals(sel.displayName())
                                    && !MSG_NO_CHILDREN.equals(sel.displayName())) {
                                String txt = getText();
                                if (txt.startsWith(".") || MSG_SELECT.equals(sel.displayName())) {
                                    // In search mode or placeholder selected: just append "." to enter drill-down
                                    if (!txt.endsWith(".")) {
                                        suppressListener = true;
                                        setText(txt + ".");
                                        suppressListener = false;
                                        updateSuggestions();
                                    }
                                } else {
                                    // In imperative mode: apply suggestion and drill down
                                    applySelectedSuggestion();
                                    String newTxt = getText();
                                    if (!newTxt.endsWith(".")) {
                                        suppressListener = true;
                                        setText(newTxt + ".");
                                        suppressListener = false;
                                        updateSuggestions();
                                    }
                                }
                                e.consume();
                            }
                        }
                        break;

                    case KeyEvent.VK_ESCAPE:
                        hidePopup();
                        e.consume();
                        break;
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (e.isTemporary() || inCreationDialog) return;
                SwingUtilities.invokeLater(() -> {
                    if (!suggestionList.hasFocus()) {
                        hidePopup();
                    }
                });
            }
        });

        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() >= 1 && !suggestionList.isSelectionEmpty()) {
                    applySelectedSuggestion();
                    TagIntelliSenseField.this.requestFocusInWindow();
                }
            }
        });
    }

    // ─── API PUBLICA ────────────────────────────────────────────────────────

    public void refreshTags(List<Tag> tags) {
        engine.loadTags(tags);
        lastSelectedTag = null;
        logger.debug("[IntelliSense] {} tags cargados.", engine.getAllTags().size());
    }

    public void loadTagsFromDB() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return new TagDAO().getAllTags();
            } catch (Exception e) {
                logger.error("Error al cargar tags para IntelliSense", e);
                return Collections.<Tag>emptyList();
            }
        }).thenAccept(tags -> SwingUtilities.invokeLater(() -> refreshTags(tags)));
    }

    public Tag getLastSelectedTag() {
        return lastSelectedTag;
    }

    public String getResolvedTagName() {
        String text = getText().trim();
        if (text.isEmpty()) return text;
        if (text.startsWith(".")) {
            String path = text.substring(1);
            if (path.isEmpty()) return "";
            String[] segments = path.split("\\.");
            return segments[segments.length - 1].trim();
        }
        return text;
    }

    public Tag resolveCurrentTag() {
        String text = getText().trim();
        if (text.isEmpty()) return null;
        return engine.resolveTagByPath(text);
    }

    public boolean hasExactMatch(String name) {
        return engine.hasExactMatch(name, null);
    }

    public boolean hasExactMatch(String name, Long parentId) {
        return engine.hasExactMatch(name, parentId);
    }

    public boolean pathExists(String dotPath) {
        return engine.pathExists(dotPath);
    }

    public TagSearchEngine getEngine() {
        return engine;
    }

    public boolean isPopupVisible() {
        return popupMenu.isVisible();
    }

    /**
     * Establece una acción que se ejecuta al pulsar Enter cuando el popup NO está visible.
     * Útil para el modo tornado (find-next-match).
     */
    public void setOnEnterNoPopupAction(Runnable action) {
        this.onEnterNoPopupAction = action;
    }

    // ─── SUGERENCIAS ────────────────────────────────────────────────────────

    private void onTextChanged() {
        if (suppressListener) return;
        pendingCreationCheck = false;
        updateSuggestions();
    }

    private void updateSuggestions() {
        String raw = getText();
        if (raw.isEmpty() || engine.isEmpty()) {
            hidePopup();
            return;
        }

        InputMode mode = TagSearchEngine.parseInputMode(raw);
        List<PathResult> suggestions = engine.search(raw, mode);

        suggestionModel.clear();

        if (suggestions.isEmpty()) {
            suggestionModel.addElement(new PathResult(MSG_NOT_FOUND, ""));
            suggestionList.setSelectedIndex(0);
            showPopup(1);
            return;
        }

        // Pre-append placeholder as first option
        suggestionModel.addElement(new PathResult(MSG_SELECT, ""));
        for (PathResult s : suggestions) {
            suggestionModel.addElement(s);
        }
        suggestionList.setSelectedIndex(0);
        showPopup(suggestions.size() + 1);
    }

    // ─── POPUP ──────────────────────────────────────────────────────────────

    private void showPopup(int itemCount) {
        int visibleItems = Math.min(itemCount, 8);
        int rowHeight = suggestionList.getFixedCellHeight() > 0
                ? suggestionList.getFixedCellHeight() : 20;
        int popupH = visibleItems * rowHeight + 4;
        int popupW = Math.max(getWidth(), 250);

        popupMenu.setPreferredSize(new Dimension(popupW, popupH));

        if (!popupMenu.isVisible()) {
            popupMenu.show(this, getWidth(), 0);
        } else {
            suggestionList.repaint();
        }
    }

    private void hidePopup() {
        if (popupMenu.isVisible()) {
            popupMenu.setVisible(false);
        }
    }

    // ─── SELECCION ──────────────────────────────────────────────────────────

    private boolean applySelectedSuggestion() {
        PathResult selected = suggestionList.getSelectedValue();
        if (selected == null && !suggestionModel.isEmpty()) {
            selected = suggestionModel.get(0);
        }
        if (selected == null) return false;

        if (MSG_NOT_FOUND.equals(selected.displayName())
                || MSG_NO_CHILDREN.equals(selected.displayName())
                || MSG_SELECT.equals(selected.displayName())) {
            hidePopup();
            return false;
        }

        String fullPath = selected.fullPath();

        suppressListener = true;
        try {
            setText(fullPath);
            setCaretPosition(getText().length());
        } finally {
            suppressListener = false;
        }
        hidePopup();

        lastSelectedTag = engine.resolveTagByPath(fullPath);
        return true;
    }

    // ─── CREACION (Fase 3) ─────────────────────────────────────────────────

    /**
     * Comprueba si la ruta completa del texto actual existe, y si no,
     * pregunta al usuario si desea crearla.  Útil para que el botón
     * "Asignar" externo pueda pedir confirmación sin duplicar la lógica.
     * @return true si la ruta existe o se creó correctamente.
     */
    public boolean ensurePathResolved() {
        String text = getText().trim();
        if (text.isEmpty()) return false;
        String pathToResolve = text.startsWith(".") ? text.substring(1) : text;
        if (engine.pathExists(pathToResolve)) return true;

        String[] segments = pathToResolve.split("\\.");
        if (segments.length == 0) return false;

        Long parentId = null;
        int firstMissingIdx = -1;
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i].trim();
            if (seg.isEmpty()) continue;
            if (!engine.hasExactMatch(seg, parentId)) {
                firstMissingIdx = i;
                break;
            }
            List<Tag> siblings = engine.getChildren(parentId);
            if (siblings != null) {
                for (Tag t : siblings) {
                    if (t.getNombre().equalsIgnoreCase(seg)) {
                        parentId = t.getId();
                        break;
                    }
                }
            }
        }
        if (firstMissingIdx < 0) return true;

        StringBuilder existingPath = new StringBuilder();
        for (int i = 0; i < firstMissingIdx; i++) {
            if (i > 0) existingPath.append(".");
            existingPath.append(segments[i]);
        }
        String existingParentPath = existingPath.toString();

        StringBuilder missingChain = new StringBuilder();
        for (int i = firstMissingIdx; i < segments.length; i++) {
            if (i > firstMissingIdx) missingChain.append(".");
            missingChain.append(segments[i]);
        }
        String missingStr = missingChain.toString();

        Tag parentTag = !existingParentPath.isEmpty() ? engine.resolveTagByPath(existingParentPath) : null;
        String message = String.format(
                "Vas a asignar el tag \"%s\" y no existe en la ruta \"%s\". \u00bfLo creamos?",
                missingStr, existingParentPath.isEmpty() ? "ra\u00edz" : existingParentPath);

        inCreationDialog = true;
        int response = JOptionPane.showConfirmDialog(
                this, message, "Crear tag(s)",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        inCreationDialog = false;

        if (response == JOptionPane.YES_OPTION) {
            try {
                TagDAO dao = new TagDAO();
                Tag lastCreated = null;
                for (int i = firstMissingIdx; i < segments.length; i++) {
                    String seg = segments[i].trim();
                    if (seg.isEmpty()) continue;
                    Optional<Tag> created = dao.addTag(seg, parentId);
                    if (created.isPresent()) {
                        lastCreated = created.get();
                        parentId = lastCreated.getId();
                    } else {
                        logger.warn("[IntelliSense] No se pudo crear el segmento '{}'.", seg);
                        break;
                    }
                }
                if (lastCreated != null) {
                    hidePopup();
                    refreshTags(dao.getAllTags());
                    String createdFullPath = engine.getFullPath(lastCreated);
                    suppressListener = true;
                    try {
                        setText(createdFullPath);
                        setCaretPosition(getText().length());
                    } finally {
                        suppressListener = false;
                    }
                    lastSelectedTag = lastCreated;
                    return true;
                }
            } catch (Exception ex) {
                logger.error("Error al crear tag(s)", ex);
                JOptionPane.showMessageDialog(this,
                        "Error al crear el tag. Revisa el log para mas detalles.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    private void checkAndHandleCreation() {
        if (!pendingCreationCheck) return;
        pendingCreationCheck = false;

        String text = getText().trim();
        if (text.isEmpty()) return;

        String pathToResolve = text.startsWith(".") ? text.substring(1) : text;

        if (engine.pathExists(pathToResolve)) return;

        String[] segments = pathToResolve.split("\\.");
        if (segments.length == 0) return;

        // Encontrar hasta dónde existe la ruta y dónde empieza lo faltante
        Long parentId = null;
        int firstMissingIdx = -1;
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i].trim();
            if (seg.isEmpty()) continue;
            boolean exists = engine.hasExactMatch(seg, parentId);
            if (!exists) {
                firstMissingIdx = i;
                break;
            }
            List<Tag> siblings = engine.getChildren(parentId);
            if (siblings != null) {
                for (Tag t : siblings) {
                    if (t.getNombre().equalsIgnoreCase(seg)) {
                        parentId = t.getId();
                        break;
                    }
                }
            }
        }
        if (firstMissingIdx < 0) return;

        // Construir el padre existente (o raíz)
        StringBuilder existingPath = new StringBuilder();
        for (int i = 0; i < firstMissingIdx; i++) {
            if (i > 0) existingPath.append(".");
            existingPath.append(segments[i]);
        }
        String existingParentPath = existingPath.toString();

        // Construir la cadena faltante
        StringBuilder missingChain = new StringBuilder();
        for (int i = firstMissingIdx; i < segments.length; i++) {
            if (i > firstMissingIdx) missingChain.append(".");
            missingChain.append(segments[i]);
        }
        String missingStr = missingChain.toString();

        Tag parentTag = !existingParentPath.isEmpty() ? engine.resolveTagByPath(existingParentPath) : null;
        String parentDisplay = (parentTag != null) ? parentTag.getNombre() : "raiz";
        String message = String.format(
                "Vas a asignar el tag \"%s\" y no existe en la ruta \"%s\". \u00bfLo creamos?",
                missingStr, existingParentPath.isEmpty() ? "ra\u00edz" : existingParentPath);

        inCreationDialog = true;
        int response = JOptionPane.showConfirmDialog(
                this, message, "Crear tag(s)",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        inCreationDialog = false;

        if (response == JOptionPane.YES_OPTION) {
            try {
                TagDAO dao = new TagDAO();
                Tag lastCreated = null;
                for (int i = firstMissingIdx; i < segments.length; i++) {
                    String seg = segments[i].trim();
                    if (seg.isEmpty()) continue;
                    Optional<Tag> created = dao.addTag(seg, parentId);
                    if (created.isPresent()) {
                        lastCreated = created.get();
                        parentId = lastCreated.getId();
                        logger.debug("[IntelliSense] Tag '{}' creado bajo padre ID {}", seg, parentId);
                    } else {
                        logger.warn("[IntelliSense] No se pudo crear el segmento '{}'.", seg);
                        break;
                    }
                }
                if (lastCreated != null) {
                    hidePopup();
                    refreshTags(dao.getAllTags());
                    String createdFullPath = engine.getFullPath(lastCreated);
                    suppressListener = true;
                    try {
                        setText(createdFullPath);
                        setCaretPosition(getText().length());
                    } finally {
                        suppressListener = false;
                    }
                    lastSelectedTag = lastCreated;
                }
            } catch (Exception ex) {
                logger.error("Error al crear tag(s)", ex);
                JOptionPane.showMessageDialog(this,
                        "Error al crear el tag. Revisa el log para mas detalles.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        // Si cancela, no hacemos nada (ni borrar textbox, ni limpiar campo)
    }

} // --- FIN de clase TagIntelliSenseField ---
