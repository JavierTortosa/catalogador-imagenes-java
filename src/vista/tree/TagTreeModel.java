package vista.tree;

import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import modelo.datos.Tag;
import servicios.db.TagDAO;

/**
 * Modelo de datos para el JTree de etiquetas.
 * Se comunica con el TagDAO para construir la jerarquía de tags de forma perezosa (lazy loading).
 */
public class TagTreeModel implements TreeModel {

    private final TagDAO tagDAO;
    private final String rootNode = "Biblioteca"; // Un objeto simple para la raíz del árbol.
    private List<Tag> rootTagsCache; // Caché para los tags de primer nivel.

    public TagTreeModel(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    } // ---FIN de constructor [TagTreeModel]---

    @Override
    public Object getRoot() {
        return rootNode;
    } // ---FIN de metodo [getRoot]---

    @Override
    public Object getChild(Object parent, int index) {
        if (parent.equals(rootNode)) {
            // Si el padre es la raíz, obtenemos los tags de primer nivel.
            if (rootTagsCache == null) {
                rootTagsCache = tagDAO.getRootTags();
            }
            return rootTagsCache.get(index);
        } else if (parent instanceof Tag) {
            // Si el padre es un Tag, obtenemos sus hijos.
            Tag parentTag = (Tag) parent;
            return tagDAO.getChildTags(parentTag.getId()).get(index);
        }
        return null;
    } // ---FIN de metodo [getChild]---

    @Override
    public int getChildCount(Object parent) {
        if (parent.equals(rootNode)) {
            // Si el padre es la raíz, contamos los tags de primer nivel.
            if (rootTagsCache == null) {
                rootTagsCache = tagDAO.getRootTags();
            }
            return rootTagsCache.size();
        } else if (parent instanceof Tag) {
            // Si el padre es un Tag, contamos sus hijos.
            Tag parentTag = (Tag) parent;
            return tagDAO.getChildTags(parentTag.getId()).size();
        }
        return 0;
    } // ---FIN de metodo [getChildCount]---

    @Override
    public boolean isLeaf(Object node) {
        // Un nodo es una hoja si no es la raíz y no tiene hijos.
        return !node.equals(rootNode) && getChildCount(node) == 0;
    } // ---FIN de metodo [isLeaf]---

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        
        List<Tag> children;
        if (parent.equals(rootNode)) {
            children = tagDAO.getRootTags();
        } else if (parent instanceof Tag) {
            children = tagDAO.getChildTags(((Tag) parent).getId());
        } else {
            return -1;
        }
        
        return children.indexOf(child);
    } // ---FIN de metodo [getIndexOfChild]---
    
    // --- Métodos no implementados (para un modelo de solo lectura por ahora) ---

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // No se implementa para un modelo de solo lectura.
    } // ---FIN de metodo [valueForPathChanged]---

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // No se implementa, ya que el modelo no cambia dinámicamente por ahora.
    } // ---FIN de metodo [addTreeModelListener]---

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // No se implementa.
    } // ---FIN de metodo [removeTreeModelListener]---

} // --- FIN de clase TagTreeModel ---