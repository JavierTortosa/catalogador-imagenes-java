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

    private final TagSearchEngine engine = new TagSearchEngine();
    private final JPopupMenu popupMenu;
    private final JList<PathResult> suggestionList;
    private final DefaultListModel<PathResult> suggestionModel;

    private Tag lastSelectedTag = null;
    private boolean suppressListener = false;
    private boolean pendingCreationCheck = false;
    private boolean inCreationDialog = false;

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
                            applySelectedSuggestion();
                        }
                        pendingCreationCheck = true;
                        SwingUtilities.invokeLater(TagIntelliSenseField.this::checkAndHandleCreation);
                        break;

                    case KeyEvent.VK_PERIOD:
                        if (popupMenu.isVisible()) {
                            PathResult sel = suggestionList.getSelectedValue();
                            if (sel != null && !MSG_NOT_FOUND.equals(sel.displayName())
                                    && !MSG_NO_CHILDREN.equals(sel.displayName())) {
                                applySelectedSuggestion();
                                String txt = getText();
                                if (!txt.endsWith(".")) {
                                    suppressListener = true;
                                    setText(txt + ".");
                                    suppressListener = false;
                                    updateSuggestions();
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
                pendingCreationCheck = true;
                SwingUtilities.invokeLater(
                        TagIntelliSenseField.this::checkAndHandleCreation);
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

        for (PathResult s : suggestions) {
            suggestionModel.addElement(s);
        }
        suggestionList.setSelectedIndex(0);
        showPopup(suggestions.size());
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

    private void applySelectedSuggestion() {
        PathResult selected = suggestionList.getSelectedValue();
        if (selected == null && !suggestionModel.isEmpty()) {
            selected = suggestionModel.get(0);
        }
        if (selected == null) return;

        if (MSG_NOT_FOUND.equals(selected.displayName())
                || MSG_NO_CHILDREN.equals(selected.displayName())) {
            hidePopup();
            return;
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
    }

    // ─── CREACION (Fase 3) ─────────────────────────────────────────────────

    private void checkAndHandleCreation() {
        if (!pendingCreationCheck) return;
        pendingCreationCheck = false;

        String text = getText().trim();
        if (text.isEmpty()) return;

        String pathToResolve = text.startsWith(".") ? text.substring(1) : text;

        if (engine.pathExists(pathToResolve)) return;

        String[] segments = pathToResolve.split("\\.");
        if (segments.length == 0) return;

        String newTagName = segments[segments.length - 1];
        if (newTagName.isEmpty()) return;

        StringBuilder parentPathBuilder = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            if (i > 0) parentPathBuilder.append(".");
            parentPathBuilder.append(segments[i]);
        }
        String parentPath = parentPathBuilder.toString();

        Tag parentTag = null;
        if (!parentPath.isEmpty()) {
            parentTag = engine.resolveTagByPath(parentPath);
            if (parentTag == null) {
                logger.debug("[IntelliSense] Ruta padre '{}' no existe.", parentPath);
                return;
            }
        }

        String parentDisplay = (parentTag != null) ? parentTag.getNombre() : "raiz";
        String message = String.format(
                "El tag '%s' no existe dentro de '%s'. \u00bfDesea crearlo?",
                newTagName, parentDisplay);

        inCreationDialog = true;
        int response = JOptionPane.showConfirmDialog(
                this, message, "Crear nuevo tag",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        inCreationDialog = false;

        if (response == JOptionPane.YES_OPTION) {
            try {
                TagDAO dao = new TagDAO();
                Long parentId = (parentTag != null) ? parentTag.getId() : null;
                Optional<Tag> created = dao.addTag(newTagName, parentId);
                if (created.isPresent()) {
                    logger.info("[IntelliSense] Tag '{}' creado bajo padre ID {}",
                            newTagName, parentId);
                    hidePopup();
                    refreshTags(dao.getAllTags());
                    String createdFullPath = engine.getFullPath(created.get());
                    suppressListener = true;
                    try {
                        setText(createdFullPath);
                        setCaretPosition(getText().length());
                    } finally {
                        suppressListener = false;
                    }
                    lastSelectedTag = created.get();
                } else {
                    logger.warn("[IntelliSense] No se pudo crear el tag '{}'.", newTagName);
                }
            } catch (Exception ex) {
                logger.error("Error al crear tag '{}'", newTagName, ex);
                JOptionPane.showMessageDialog(this,
                        "Error al crear el tag. Revisa el log para mas detalles.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            if (lastSelectedTag != null) {
                String lastPath = engine.getFullPath(lastSelectedTag);
                suppressListener = true;
                try {
                    setText(lastPath);
                    setCaretPosition(getText().length());
                } finally {
                    suppressListener = false;
                }
            }
        }
    }

} // --- FIN de clase TagIntelliSenseField ---
