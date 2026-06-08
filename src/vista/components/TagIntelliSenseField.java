package vista.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;
import servicios.db.TagDAO;

public class TagIntelliSenseField extends JTextField {

    private static final long serialVersionUID = 2L;
    private static final Logger logger = LoggerFactory.getLogger(TagIntelliSenseField.class);

    private static final String MSG_NOT_FOUND = "(no existe)";
    private static final String MSG_NO_CHILDREN = "(sin hijos)";

    private final JPopupMenu popupMenu;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> suggestionModel;

    private List<Tag> allTags = new ArrayList<>();
    private Map<Long, Tag> tagById = new HashMap<>();
    private Map<Long, List<Tag>> childrenByParentId = new HashMap<>();

    /** Ultimo tag seleccionado por Tab/clic en el popup (para drilling). */
    private Tag lastSelectedTag = null;

    private boolean suppressListener = false;
    private boolean dotHandled = false;

    public TagIntelliSenseField() {
        super();

        suggestionModel = new DefaultListModel<>();
        suggestionList  = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(getFont());
        suggestionList.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new java.awt.BorderLayout());
        popupMenu.add(scrollPane, java.awt.BorderLayout.CENTER);
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);

        // Evitar que Tab sea usado para focus traversal (lo manejamos nosotros para el popup)
        setFocusTraversalKeysEnabled(false);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '.' && dotHandled) {
                    dotHandled = false;
                    e.consume();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
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
                            hidePopup();
                        }
                        // Enter se propaga al padre (no consume) para formularios
                        break;

                    case KeyEvent.VK_PERIOD:
                        if (popupMenu.isVisible()) {
                            dotHandled = true;
                            String sel = suggestionList.getSelectedValue();
                            if (sel != null && !MSG_NOT_FOUND.equals(sel) && !MSG_NO_CHILDREN.equals(sel)) {
                                applySelectedSuggestion();
                                // Si estamos en modo jerarquico, anadir "." para bajar al siguiente nivel
                                String txt = getText();
                                if (txt.startsWith(".") && !txt.endsWith(".")) {
                                    suppressListener = true;
                                    setText(txt + ".");
                                    suppressListener = false;
                                    // Forzar actualizacion para mostrar hijos
                                    SwingUtilities.invokeLater(TagIntelliSenseField.this::updateSuggestions);
                                }
                            }
                            e.consume();
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

    // ─────────────────────────────────────────────────────────────────────────
    // API PUBLICA
    // ─────────────────────────────────────────────────────────────────────────

    public void refreshTags(List<Tag> tags) {
        if (tags == null) tags = new ArrayList<>();
        this.allTags = new ArrayList<>(tags);

        this.tagById.clear();
        this.childrenByParentId.clear();
        for (Tag t : this.allTags) {
            this.tagById.put(t.getId(), t);
            Long pid = t.getParentId();
            childrenByParentId.computeIfAbsent(pid, k -> new ArrayList<>()).add(t);
        }

        logger.debug("[IntelliSense] {} tags cargados para autocompletado.", allTags.size());
    }

    public void loadTagsFromDB() {
        try {
            TagDAO dao = new TagDAO();
            refreshTags(dao.getAllTags());
        } catch (Exception e) {
            logger.error("Error al cargar tags para IntelliSense", e);
        }
    }

    /** Devuelve el ultimo tag seleccionado via popup, o null. */
    public Tag getLastSelectedTag() {
        return lastSelectedTag;
    }

    /** Busca si existe un tag cuyo nombre coincida exactamente (sin importar padre). */
    public boolean hasExactMatch(String name) {
        if (name == null || name.isBlank()) return false;
        return allTags.stream().anyMatch(t -> t.getNombre().equalsIgnoreCase(name.trim()));
    }

    /**
     * Resuelve el texto actual del campo al nombre del tag final.
     * Para rutas "." devuelve el ultimo segmento (el tag hoja).
     * Para texto plano devuelve el texto tal cual.
     */
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

    /**
     * Resuelve el tag final correspondiente al texto actual, caminando la jerarquia.
     * @return el Tag si se puede resolver completamente, o null si no.
     */
    public Tag resolveCurrentTag() {
        String text = getText().trim();
        if (text.isEmpty()) return null;
        if (text.startsWith(".")) {
            String path = text.substring(1);
            return resolveTagByPath(path);
        }
        // Texto plano: buscar por nombre exacto
        return allTags.stream()
                .filter(t -> t.getNombre().equalsIgnoreCase(text))
                .findFirst().orElse(null);
    }

    /** Busca si existe un tag cuyo nombre coincida exactamente bajo un padre concreto. */
    public boolean hasExactMatch(String name, Long parentId) {
        if (name == null || name.isBlank()) return false;
        List<Tag> siblings = childrenByParentId.get(parentId);
        if (siblings == null) return false;
        return siblings.stream().anyMatch(t -> t.getNombre().equalsIgnoreCase(name.trim()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGICA INTERNA
    // ─────────────────────────────────────────────────────────────────────────

    private void onTextChanged() {
        if (suppressListener) return;
        SwingUtilities.invokeLater(this::updateSuggestions);
    }

    private void updateSuggestions() {
        String raw = getText();
        if (raw.isEmpty() || allTags.isEmpty()) {
            hidePopup();
            return;
        }

        List<String> suggestions;
        boolean isHierarchy = raw.startsWith(".");

        if (isHierarchy) {
            suggestions = buildHierarchySuggestions(raw);
        } else {
            suggestions = buildFlatSuggestions(raw);
        }

        if (suggestions == null) {
            hidePopup();
            return;
        }

        suggestionModel.clear();

        if (suggestions.isEmpty()) {
            // Sin resultados: mostrar mensaje "no existe"
            String msg = isHierarchy ? MSG_NOT_FOUND : MSG_NOT_FOUND;
            suggestionModel.addElement(msg);
            suggestionList.setSelectedIndex(0);
            showPopup(1);
            return;
        }

        for (String s : suggestions) {
            suggestionModel.addElement(s);
        }
        suggestionList.setSelectedIndex(0);
        showPopup(suggestions.size());
    }

    /**
     * Modo "." — navegacion jerarquica.
     */
    private List<String> buildHierarchySuggestions(String text) {
        String withoutDot = text.substring(1); // quitar "." inicial

        if (withoutDot.isEmpty()) {
            // Solo "." → mostrar raices
            return getRootTagNames();
        }

        if (withoutDot.endsWith(".")) {
            // '.bases.' → resolver "bases" → mostrar todos sus hijos
            String path = withoutDot.substring(0, withoutDot.length() - 1);
            Tag parent = resolveTagByPath(path);
            if (parent == null) {
                return Collections.emptyList();
            }
            List<String> childNames = getChildNames(parent.getId());
            if (childNames.isEmpty()) {
                // Mostrar mensaje de que no tiene hijos
                List<String> noKids = new ArrayList<>();
                noKids.add(MSG_NO_CHILDREN);
                return noKids;
            }
            return childNames;
        }

        // Sin trailing "."
        if (!withoutDot.contains(".")) {
            // '.b' → prefijo global: todos los tags que empiecen por 'b'
            return filterTagsByPrefix(withoutDot);
        }

        // '.bases.c' → ultimo segmento es prefijo sobre hijos del padre
        int lastDotIdx = withoutDot.lastIndexOf(".");
        String parentPath = withoutDot.substring(0, lastDotIdx);
        String childPrefix = withoutDot.substring(lastDotIdx + 1);

        Tag parent = resolveTagByPath(parentPath);
        if (parent == null) {
            return Collections.emptyList();
        }

        return filterChildNamesByPrefix(parent.getId(), childPrefix);
    }

    /**
     * Modo plano (sin ".") — prefijo global sobre todos los tags.
     */
    private List<String> buildFlatSuggestions(String text) {
        return filterTagsByPrefix(text);
    }

    // ─── Utilidades de busqueda ──────────────────────────────────────────────

    /** Resuelve una ruta en notacion punto (sin "." inicial) a un Tag.
     *  Ej: "juegos.estrategia" → camina hijos de raiz. */
    private Tag resolveTagByPath(String dotPath) {
        if (dotPath == null || dotPath.isEmpty()) return null;
        String[] segments = dotPath.split("\\.");
        Long currentParentId = null;
        Tag current = null;

        for (String seg : segments) {
            if (seg.isEmpty()) return null;
            List<Tag> candidates = childrenByParentId.get(currentParentId);
            if (candidates == null) return null;
            current = null;
            for (Tag t : candidates) {
                if (t.getNombre().equalsIgnoreCase(seg)) {
                    current = t;
                    break;
                }
            }
            if (current == null) return null;
            currentParentId = current.getId();
        }
        return current;
    }

    /** Todos los tags raiz (parentId == null), ordenados. */
    private List<String> getRootTagNames() {
        List<Tag> roots = childrenByParentId.get(null);
        if (roots == null || roots.isEmpty()) return Collections.emptyList();
        return roots.stream()
                .map(Tag::getNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /** Nombres de hijos directos de un tag padre. */
    private List<String> getChildNames(Long parentId) {
        List<Tag> children = childrenByParentId.get(parentId);
        if (children == null || children.isEmpty()) return Collections.emptyList();
        return children.stream()
                .map(Tag::getNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /** Hijos directos cuyo nombre empiece por un prefijo. */
    private List<String> filterChildNamesByPrefix(Long parentId, String prefix) {
        if (prefix.isEmpty()) return getChildNames(parentId);
        List<Tag> children = childrenByParentId.get(parentId);
        if (children == null) return Collections.emptyList();
        return children.stream()
                .map(Tag::getNombre)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /** Todos los tags (cualquier nivel) cuyo nombre empiece por un prefijo. */
    private List<String> filterTagsByPrefix(String prefix) {
        if (prefix.isEmpty()) {
            return allTags.stream()
                    .map(Tag::getNombre)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
        return allTags.stream()
                .map(Tag::getNombre)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .distinct()
                .collect(Collectors.toList());
    }

    // ─── Popup ───────────────────────────────────────────────────────────────

    private void showPopup(int itemCount) {
        int visibleItems = Math.min(itemCount, 8);
        int rowHeight = suggestionList.getFixedCellHeight() > 0
                ? suggestionList.getFixedCellHeight() : 20;
        int popupH = visibleItems * rowHeight + 4;
        int popupW = Math.max(getWidth(), 200);

        popupMenu.setPreferredSize(new Dimension(popupW, popupH));

        if (!popupMenu.isVisible()) {
            popupMenu.show(this, getWidth(), 0);
        } else {
            popupMenu.pack();
            popupMenu.repaint();
        }
    }

    private void hidePopup() {
        if (popupMenu.isVisible()) {
            popupMenu.setVisible(false);
        }
    }

    // ─── Seleccion ───────────────────────────────────────────────────────────

    private void applySelectedSuggestion() {
        String selected = suggestionList.getSelectedValue();
        if (selected == null && !suggestionModel.isEmpty()) {
            selected = suggestionModel.get(0);
        }
        if (selected == null) return;

        // No aplicar si es un mensaje de estado (no existe / sin hijos)
        if (MSG_NOT_FOUND.equals(selected) || MSG_NO_CHILDREN.equals(selected)) {
            hidePopup();
            return;
        }

        boolean isHierarchy = getText().startsWith(".");

        suppressListener = true;
        try {
            if (isHierarchy) {
                String current = getText();
                if (current.endsWith(".")) {
                    // Drilling puro: ".chibi." + "coches" → ".chibi.coches"
                    setText(current + selected);
                } else {
                    String withoutDot = current.substring(1);
                    if (withoutDot.contains(".")) {
                        // Drilling parcial: ".chibi.c" → base ".chibi." + "coches"
                        int lastDot = current.lastIndexOf(".");
                        setText(current.substring(0, lastDot + 1) + selected);
                    } else {
                        // Jerarquia simple: ".ch" → ".chibi"
                        setText("." + selected);
                    }
                }
            } else {
                setText(selected);
            }
            setCaretPosition(getText().length());
        } finally {
            suppressListener = false;
        }
        hidePopup();

        // Buscar y guardar el tag seleccionado para futuros drillings
        if (isHierarchy) {
            String path = getText().substring(1); // quitar "." inicial
            lastSelectedTag = resolveTagByPath(path);
        } else {
            String name = getText();
            lastSelectedTag = allTags.stream()
                    .filter(t -> t.getNombre().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }
    }

} // --- FIN de clase TagIntelliSenseField ---
