package vista.tree;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import modelo.datos.Tag;
import servicios.db.TagDAO;

/**
 * Renderizador personalizado para los nodos del árbol de etiquetas.
 * Muestra el nombre del tag seguido del número de imágenes asociadas (incluyendo sub-etiquetas).
 */
public class TagTreeCellRenderer extends DefaultTreeCellRenderer {

    private final TagDAO tagDAO;

    public TagTreeCellRenderer(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    } // ---FIN de constructor [TagTreeCellRenderer]---

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, 
                                                  boolean leaf, int row, boolean hasFocus) {
        
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof Tag) {
            Tag tag = (Tag) value;
            // Obtenemos el conteo recursivo desde la base de datos
            int count = tagDAO.getImageCountForTagRecursive(tag.getId());
            
            // Formateamos el texto: "Nombre (Count)"
            setText(tag.getNombre() + " (" + count + ")");
        } else if (value instanceof String && value.equals("Biblioteca")) {
            // Opcional: Podríamos contar el total de la biblioteca si quisiéramos
            setText("Biblioteca");
        }

        return this;
    } // ---FIN de metodo [getTreeCellRendererComponent]---

} // --- FIN de clase TagTreeCellRenderer ---
