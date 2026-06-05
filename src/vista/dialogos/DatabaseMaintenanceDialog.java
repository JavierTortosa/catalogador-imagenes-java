package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import controlador.managers.DataManager;
import modelo.datos.ImagenInfo;
import modelo.datos.Tag;
import vista.util.IconUtils;

public class DatabaseMaintenanceDialog extends JDialog {

    private static final long serialVersionUID = 1L;
	private final DataManager dataManager;
    private boolean dbChanged = false;

    public DatabaseMaintenanceDialog(Frame parent, DataManager dataManager) {
        super(parent, "Mantenimiento de la Base de Datos", true);
        this.dataManager = dataManager;

        initComponents();
        setSize(500, 300);
        setLocationRelativeTo(parent);
    }

    public boolean isDbChanged() {
        return dbChanged;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();

        // Pestaña 1: Imágenes Huérfanas
        tabbedPane.addTab("Imágenes Huérfanas", createOrphansPanel());

        // Pestaña 2: Limpiar Etiquetas
        tabbedPane.addTab("Etiquetas Sin Uso", createUnusedTagsPanel());

        // Pestaña 3: Fusionar Etiquetas
        tabbedPane.addTab("Fusionar Etiquetas", createMergeTagsPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        buttonPanel.add(btnClose);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createOrphansPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Busca imágenes registradas en la base de datos cuyos archivos físicos ya no existen en el disco duro. Esto puede tardar unos segundos.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JButton btnScan = new JButton("Buscar y Limpiar Huérfanas");
        btnScan.addActionListener(e -> scanAndCleanOrphans(panel));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnScan);
        panel.add(btnPanel, BorderLayout.CENTER);

        return panel;
    }

    private void scanAndCleanOrphans(Component parent) {
        // Ejecutar en segundo plano
        new SwingWorker<List<Long>, Void>() {
            @Override
            protected List<Long> doInBackground() throws Exception {
                List<ImagenInfo> allImages = dataManager.getImagenDAO().getAllImagenes();
                List<Long> orphans = new ArrayList<>();
                for (ImagenInfo img : allImages) {
                    Path p = Paths.get(img.getRutaCompleta());
                    if (!Files.exists(p)) {
                        orphans.add(img.getId());
                    }
                }
                return orphans;
            }

            @Override
            protected void done() {
                try {
                    List<Long> orphans = get();
                    if (orphans.isEmpty()) {
                        JOptionPane.showMessageDialog(parent, "No se encontraron imágenes huérfanas.", "Resultado", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int confirm = JOptionPane.showConfirmDialog(parent,
                            "Se encontraron " + orphans.size() + " imágenes huérfanas.\n¿Deseas eliminarlas de la base de datos?",
                            "Limpiar Huérfanas", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        int deleted = dataManager.getImagenDAO().deleteImagenes(orphans);
                        JOptionPane.showMessageDialog(parent, "Se han eliminado " + deleted + " imágenes huérfanas.", "Limpieza completada", JOptionPane.INFORMATION_MESSAGE);
                        dbChanged = true;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent, "Error al buscar imágenes huérfanas.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel createUnusedTagsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Busca etiquetas que no están asociadas a ninguna imagen y que no tienen etiquetas hijas.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JButton btnScan = new JButton("Buscar Etiquetas Sin Uso");
        btnScan.addActionListener(e -> {
            List<Tag> unused = dataManager.getTagDAO().getUnusedTags();
            if (unused.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "No hay etiquetas sin uso.", "Resultado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int confirm = JOptionPane.showConfirmDialog(panel,
                        "Se encontraron " + unused.size() + " etiquetas sin uso.\n¿Deseas eliminarlas?",
                        "Limpiar Etiquetas", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    int count = 0;
                    for (Tag t : unused) {
                        if (dataManager.getTagDAO().deleteTag(t.getId())) count++;
                    }
                    JOptionPane.showMessageDialog(panel, "Se han eliminado " + count + " etiquetas.", "Limpieza completada", JOptionPane.INFORMATION_MESSAGE);
                    dbChanged = true;
                }
            }
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnScan);
        panel.add(btnPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMergeTagsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("<html>Transfiere todas las imágenes de una etiqueta a otra, y luego elimina la etiqueta original.</html>");
        panel.add(lblInfo, BorderLayout.NORTH);

        JPanel comboPanel = new JPanel(new java.awt.GridLayout(2, 2, 10, 10));
        List<Tag> allTags = dataManager.getAllTags();
        
        JComboBox<TagComboItem> comboSource = new JComboBox<>();
        JComboBox<TagComboItem> comboTarget = new JComboBox<>();
        
        for (Tag t : allTags) {
            TagComboItem item = new TagComboItem(t);
            comboSource.addItem(item);
            comboTarget.addItem(item);
        }

        comboPanel.add(new JLabel("Etiqueta a eliminar (Origen):"));
        comboPanel.add(comboSource);
        comboPanel.add(new JLabel("Etiqueta destino:"));
        comboPanel.add(comboTarget);

        panel.add(comboPanel, BorderLayout.CENTER);

        JButton btnMerge = new JButton("Fusionar");
        btnMerge.addActionListener(e -> {
            TagComboItem source = (TagComboItem) comboSource.getSelectedItem();
            TagComboItem target = (TagComboItem) comboTarget.getSelectedItem();
            
            if (source == null || target == null) return;
            if (source.tag.getId() == target.tag.getId()) {
                JOptionPane.showMessageDialog(panel, "Debes seleccionar dos etiquetas distintas.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(panel,
                    "¿Estás seguro de que quieres mover todo de '" + source.tag.getNombre() + 
                    "' a '" + target.tag.getNombre() + "'?\nLa etiqueta origen será eliminada.",
                    "Confirmar Fusión", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                boolean ok = dataManager.getTagDAO().mergeTags(source.tag.getId(), target.tag.getId());
                if (ok) {
                    JOptionPane.showMessageDialog(panel, "Fusión completada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dbChanged = true;
                    // Actualizar los combos para quitar la eliminada
                    comboSource.removeItem(source);
                    comboTarget.removeItem(source);
                } else {
                    JOptionPane.showMessageDialog(panel, "Hubo un error al fusionar las etiquetas.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnMerge);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static class TagComboItem {
        Tag tag;
        TagComboItem(Tag tag) { this.tag = tag; }
        @Override
        public String toString() { return tag.getNombre(); }
    }
}
