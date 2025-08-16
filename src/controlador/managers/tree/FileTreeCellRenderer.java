package controlador.managers.tree;

import java.awt.Component;
import java.io.File;
import javax.swing.JTree;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;

public class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private final FileSystemView fileSystemView;

    public FileTreeCellRenderer() {
        this.fileSystemView = FileSystemView.getFileSystemView();
    } // --- Fin del método FileTreeCellRenderer (constructor) ---

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        
        File file = (File) value;
        
        // Usamos el FileSystemView para obtener el icono y nombre correctos del sistema operativo
        setIcon(fileSystemView.getSystemIcon(file));
        setText(fileSystemView.getSystemDisplayName(file));
        
        if ("root".equals(file.getName())) { // Nodo raíz virtual
            setText("Equipo");
        }
        
        return this;
    } // --- Fin del método getTreeCellRendererComponent ---
} // --- FIN DE LA CLASE FileTreeCellRenderer ---