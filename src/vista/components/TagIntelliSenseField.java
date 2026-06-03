package vista.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;
import servicios.db.TagDAO;

/**
 * Campo de texto avanzado con autocompletado inteligente (IntelliSense) para
 * la notación de ruta de etiquetas (ej: "juegos.blood bowl").
 *
 * Características:
 * - Muestra un popup con sugerencias filtradas en tiempo real.
 * - Soporta navegación con ↑ ↓ y selección con Enter o Tab.
 * - Construye la ruta completa de tags en formato "padre.hijo.nieto".
 * - Se actualiza automáticamente cuando se llama a {@link #refreshTags(List)}.
 */
public class TagIntelliSenseField extends JTextField {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TagIntelliSenseField.class);

    // ── Popup y lista de sugerencias ──────────────────────────────────────────
    private final JPopupMenu popupMenu;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> suggestionModel;

    // ── Datos de tags ─────────────────────────────────────────────────────────
    private List<Tag> allTags = new ArrayList<>();
    private Map<Long, Tag> tagById = new HashMap<>();
    /** Lista plana de rutas completas en notación punto (calculada una sola vez al cargar). */
    private List<String> flatPaths = new ArrayList<>();

    // ── Control interno ───────────────────────────────────────────────────────
    private boolean suppressListener = false;

    // ────────────────────────────────────────────────────────────────────────────

    public TagIntelliSenseField() {
        super();

        // ── Configurar popup ──────────────────────────────────────────────────
        suggestionModel = new DefaultListModel<>();
        suggestionList  = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(getFont());
        suggestionList.setFocusable(false);          // el foco lo mantiene el JTextField

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new java.awt.BorderLayout());
        popupMenu.add(scrollPane, java.awt.BorderLayout.CENTER);
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);               // evita que el popup robe el foco

        // ── Listeners del JTextField ──────────────────────────────────────────
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onTextChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { /* no aplica */ }
        });

        addKeyListener(new KeyAdapter() {
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

                    case KeyEvent.VK_ENTER:
                        if (popupMenu.isVisible()) {
                            applySelectedSuggestion();
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_TAB:
                        if (popupMenu.isVisible()) {
                            applySelectedSuggestion();
                            e.consume();
                        }
                        break;

                    case KeyEvent.VK_ESCAPE:
                        hidePopup();
                        e.consume();
                        break;

                    default:
                        break;
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Pequeño retardo para permitir clics en la lista del popup
                SwingUtilities.invokeLater(() -> {
                    if (!suggestionList.hasFocus()) {
                        hidePopup();
                    }
                });
            }
        });

        // Clic en sugerencia → aplicar
        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() >= 1 && !suggestionList.isSelectionEmpty()) {
                    applySelectedSuggestion();
                    TagIntelliSenseField.this.requestFocusInWindow();
                }
            }
        });
    } // ---FIN de constructor [TagIntelliSenseField]---

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Actualiza la lista de todos los tags disponibles para el autocompletado.
     * Este método recalcula las rutas completas en notación punto.
     *
     * @param tags Lista completa de tags de la base de datos.
     */
    public void refreshTags(List<Tag> tags) {
        if (tags == null) tags = new ArrayList<>();
        this.allTags = new ArrayList<>(tags);

        this.tagById.clear();
        for (Tag t : this.allTags) {
            this.tagById.put(t.getId(), t);
        }

        this.flatPaths = buildFlatPaths(this.allTags);
        logger.debug("[IntelliSense] {} rutas calculadas para el autocompletado.", flatPaths.size());
    } // ---FIN de metodo [refreshTags]---

    /**
     * Carga los tags directamente desde la base de datos.
     * Útil si no se dispone de una lista externa ya cargada.
     */
    public void loadTagsFromDB() {
        try {
            TagDAO dao = new TagDAO();
            refreshTags(dao.getAllTags());
        } catch (Exception e) {
            logger.error("Error al cargar tags para IntelliSense", e);
        }
    } // ---FIN de metodo [loadTagsFromDB]---

    // ─────────────────────────────────────────────────────────────────────────
    // LÓGICA INTERNA
    // ─────────────────────────────────────────────────────────────────────────

    private void onTextChanged() {
        if (suppressListener) return;
        SwingUtilities.invokeLater(this::updateSuggestions);
    }

    private void updateSuggestions() {
        String text = getText().trim().toLowerCase();

        if (text.isEmpty() || flatPaths.isEmpty()) {
            hidePopup();
            return;
        }

        // Filtramos rutas que contengan el texto (coincidencia parcial)
        List<String> matches = flatPaths.stream()
                .filter(path -> path.toLowerCase().contains(text))
                .sorted(Comparator.comparingInt(path -> {
                    // Priorizar las que empiezan con el texto
                    return path.toLowerCase().startsWith(text) ? 0 : 1;
                }))
                .limit(12)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            hidePopup();
            return;
        }

        suggestionModel.clear();
        for (String m : matches) {
            suggestionModel.addElement(m);
        }
        suggestionList.setSelectedIndex(0);

        showPopup(matches.size());
    }

    private void showPopup(int itemCount) {
        int visibleItems = Math.min(itemCount, 8);
        int rowHeight     = suggestionList.getFixedCellHeight() > 0
                ? suggestionList.getFixedCellHeight() : 20;
        int popupH        = visibleItems * rowHeight + 4;
        int popupW        = Math.max(getWidth(), 200);

        popupMenu.setPreferredSize(new Dimension(popupW, popupH));

        if (!popupMenu.isVisible()) {
            popupMenu.show(this, 0, getHeight());
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

    /**
     * Escribe la sugerencia seleccionada en el campo de texto.
     */
    private void applySelectedSuggestion() {
        String selected = suggestionList.getSelectedValue();
        if (selected == null && !suggestionModel.isEmpty()) {
            selected = suggestionModel.get(0);
        }
        if (selected != null) {
            suppressListener = true;
            try {
                setText(selected);
                setCaretPosition(selected.length());
            } finally {
                suppressListener = false;
            }
            hidePopup();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CÁLCULO DE RUTAS PLANAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A partir de la lista plana de tags construye una lista de rutas completas
     * en notación punto (ej. "juegos", "juegos.estrategia", "juegos.estrategia.medieval").
     */
    private List<String> buildFlatPaths(List<Tag> tags) {
        List<String> paths = new ArrayList<>();
        for (Tag tag : tags) {
            String fullPath = buildFullPath(tag);
            if (fullPath != null && !fullPath.isEmpty()) {
                paths.add(fullPath);
            }
        }
        paths.sort(String::compareTo);
        return paths;
    }

    /**
     * Construye la ruta completa de un tag recorriendo sus ancestros.
     */
    private String buildFullPath(Tag tag) {
        if (tag == null) return "";

        StringBuilder sb = new StringBuilder(tag.getNombre());
        Long parentId = tag.getParentId();
        int safetyCounter = 0;

        while (parentId != null && safetyCounter < 20) {
            Tag parent = tagById.get(parentId);
            if (parent == null) break;
            sb.insert(0, parent.getNombre() + ".");
            parentId = parent.getParentId();
            safetyCounter++;
        }

        return sb.toString();
    }

} // --- FIN de clase TagIntelliSenseField ---
