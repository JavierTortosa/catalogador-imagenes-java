package vista.tree;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import modelo.datos.Tag;
import servicios.db.TagDAO;

/**
 * Renderizador personalizado para los nodos del árbol de etiquetas.
 * Muestra el nombre del tag seguido del número de imágenes asociadas (incluyendo sub-etiquetas).
 */
public class TagTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;
	private final TagDAO tagDAO;
    private java.util.List<Long> connectedDiscoIds;
    private final Map<Long, int[]> countCache = new ConcurrentHashMap<>();

    public TagTreeCellRenderer(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    } // ---FIN de constructor [TagTreeCellRenderer]---
    
    public void setConnectedDiscoIds(java.util.List<Long> connectedDiscoIds) {
        this.connectedDiscoIds = connectedDiscoIds;
    }
    
    public void refreshCache() {
        countCache.clear();
    }
    
    public void setCountCache(java.util.Map<Long, int[]> counts) {
        countCache.clear();
        if (counts != null) {
            countCache.putAll(counts);
        }
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, 
                                                  boolean leaf, int row, boolean hasFocus) {
        
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof Tag) {
            Tag tag = (Tag) value;
            int[] cached = countCache.get(tag.getId());
            int total, available;
            if (cached != null) {
                available = cached[0];
                total = cached[1];
            } else {
                total = tagDAO.getImageCountForTagRecursive(tag.getId());
                available = tagDAO.getAvailableImageCountForTagRecursive(tag.getId(), connectedDiscoIds);
                countCache.put(tag.getId(), new int[]{available, total});
            }
            
            setText(tag.getNombre() + " (" + available + "/" + total + ")");
            
            if (!sel) {
                if (tag.isReadOnly()) {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    java.awt.Color accent = javax.swing.UIManager.getColor("Component.accentColor");
                    if (accent == null) accent = new java.awt.Color(50, 150, 255);
                    if (available == 0 && total > 0) accent = accent.darker();
                    setForeground(accent);
                } else {
                    setFont(getFont().deriveFont(Font.ITALIC));
                    setForeground(javax.swing.UIManager.getColor("Label.foreground"));
                }
            }
        } else if (value instanceof String && value.equals("Biblioteca")) {
            setText("Biblioteca");
        }

        return this;
    } // ---FIN de metodo [getTreeCellRendererComponent]---

} // --- FIN de clase TagTreeCellRenderer ---
