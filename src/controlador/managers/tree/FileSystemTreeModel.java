package controlador.managers.tree;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.filechooser.FileSystemView;

public class FileSystemTreeModel implements TreeModel {

    // --- INICIO DE LA CORRECCIÓN: Usamos un nodo raíz virtual mejor ---
    private final FileNode root;
    // --- FIN DE LA CORRECCIÓN ---

    public FileSystemTreeModel() {
        // Usamos un nodo raíz virtual para mostrar todas las unidades del sistema
        this.root = new FileNode("Equipo"); 
    } // --- Fin del método FileSystemTreeModel (constructor) ---

    @Override
    public Object getRoot() {
        return this.root;
    } // --- Fin del método getRoot ---

    @Override
    public Object getChild(Object parent, int index) {
        // --- INICIO DE LA CORRECCIÓN ---
        if (parent == this.root) {
            return File.listRoots()[index];
        }
        // --- FIN DE LA CORRECCIÓN ---
        
        File parentFile = (File) parent;
        File[] children = parentFile.listFiles();
        if (children == null) return null;
        
        List<File> directories = Arrays.stream(children)
            .filter(f -> f.isDirectory() && !f.isHidden())
            .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
            
        return directories.get(index);
    } // --- Fin del método getChild ---

    @Override
    public int getChildCount(Object parent) {
        // --- INICIO DE LA CORRECCIÓN ---
        if (parent == this.root) {
            return File.listRoots().length;
        }
        // --- FIN DE LA CORRECCIÓN ---

        File parentFile = (File) parent;
        File[] children = parentFile.listFiles();
        if (children == null) return 0;

        return (int) Arrays.stream(children)
            .filter(f -> f.isDirectory() && !f.isHidden())
            .count();
    } // --- Fin del método getChildCount ---

    @Override
    public boolean isLeaf(Object node) {
        // --- INICIO DE LA CORRECCIÓN ---
        if (node == this.root) {
            return false; // La raíz NUNCA es una hoja, tiene las unidades como hijos
        }
        // --- FIN DE LA CORRECCIÓN ---
        File file = (File) node;
        // Un nodo es una hoja si NO es un directorio.
        return !file.isDirectory();
    } // --- Fin del método isLeaf ---

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        File childFile = (File) child;

        // --- INICIO DE LA CORRECCIÓN ---
        if (parent == this.root) {
            File[] roots = File.listRoots();
            for (int i = 0; i < roots.length; i++) {
                if (roots[i].equals(childFile)) return i;
            }
            return -1;
        }
        // --- FIN DE LA CORRECCIÓN ---
        
        File parentFile = (File) parent;
        File[] children = parentFile.listFiles();
        if (children == null) return -1;
        
        List<File> directories = Arrays.stream(children)
            .filter(f -> f.isDirectory() && !f.isHidden())
            .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
            
        return directories.indexOf(childFile);
    } // --- Fin del método getIndexOfChild ---

    // Métodos no necesarios para un modelo de solo lectura del sistema de archivos
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {}
    @Override
    public void addTreeModelListener(TreeModelListener l) {}
    @Override
    public void removeTreeModelListener(TreeModelListener l) {}
    
    // --- NUEVA CLASE INTERNA ---
    /**
     * Clase auxiliar para representar el nodo raíz virtual.
     */
    private static class FileNode extends File {
        public FileNode(String pathname) {
            super(pathname);
        }
    } // --- Fin de la clase FileNode ---

} // --- FIN DE LA CLASE FileSystemTreeModel ---

//package controlador.managers.tree;
//
//import java.io.File;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.List;
//import java.util.stream.Collectors;
//import javax.swing.event.TreeModelListener;
//import javax.swing.tree.TreeModel;
//import javax.swing.tree.TreePath;
//
//public class FileSystemTreeModel implements TreeModel {
//
//    private final File root;
//
//    public FileSystemTreeModel() {
//        // Usamos un nodo raíz virtual para mostrar todas las unidades del sistema
//        this.root = new File("root"); 
//    } // --- Fin del método FileSystemTreeModel (constructor) ---
//
//    @Override
//    public Object getRoot() {
//        return this.root;
//    } // --- Fin del método getRoot ---
//
//    @Override
//    public Object getChild(Object parent, int index) {
//        File parentFile = (File) parent;
//        if (parentFile == this.root) {
//            return File.listRoots()[index];
//        }
//        File[] children = parentFile.listFiles();
//        if (children == null) return null;
//        
//        List<File> directories = Arrays.stream(children)
//            .filter(f -> f.isDirectory() && !f.isHidden())
//            .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
//            .collect(Collectors.toList());
//            
//        return directories.get(index);
//    } // --- Fin del método getChild ---
//
//    @Override
//    public int getChildCount(Object parent) {
//        File parentFile = (File) parent;
//        if (parentFile == this.root) {
//            return File.listRoots().length;
//        }
//        File[] children = parentFile.listFiles();
//        if (children == null) return 0;
//
//        return (int) Arrays.stream(children)
//            .filter(f -> f.isDirectory() && !f.isHidden())
//            .count();
//    } // --- Fin del método getChildCount ---
//
//    @Override
//    public boolean isLeaf(Object node) {
//        File file = (File) node;
//        return !file.isDirectory() || file == this.root;
//    } // --- Fin del método isLeaf ---
//
//    @Override
//    public int getIndexOfChild(Object parent, Object child) {
//        File parentFile = (File) parent;
//        File childFile = (File) child;
//
//        if (parentFile == this.root) {
//            File[] roots = File.listRoots();
//            for (int i = 0; i < roots.length; i++) {
//                if (roots[i].equals(childFile)) return i;
//            }
//            return -1;
//        }
//
//        File[] children = parentFile.listFiles();
//        if (children == null) return -1;
//        
//        List<File> directories = Arrays.stream(children)
//            .filter(f -> f.isDirectory() && !f.isHidden())
//            .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
//            .collect(Collectors.toList());
//            
//        return directories.indexOf(childFile);
//    } // --- Fin del método getIndexOfChild ---
//
//    // Métodos no necesarios para un modelo de solo lectura del sistema de archivos
//    @Override
//    public void valueForPathChanged(TreePath path, Object newValue) {}
//    @Override
//    public void addTreeModelListener(TreeModelListener l) {}
//    @Override
//    public void removeTreeModelListener(TreeModelListener l) {}
//
//} // --- FIN DE LA CLASE FileSystemTreeModel ---