package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import modelo.proyecto.ExportItem;

public class ExportDetailPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel titleLabel;
    private JList<Path> associatedFilesList;
    private DefaultListModel<Path> associatedFilesModel;
    
    private Action addAction;
    private Action removeAction;
    private Action locateAction;

    public ExportDetailPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        initComponents();
    } // ---FIN de metodo [ExportDetailPanel]---

    private void initComponents() {
        // --- Título ---
        titleLabel = new JLabel("Detalles para: (ningún ítem seleccionado)");
        titleLabel.putClientProperty("FlatLaf.style", "font: bold");
        add(titleLabel, BorderLayout.NORTH);

        // --- Panel Principal con GridBagLayout ---
        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // --- Columna 0: Lista de archivos asociados ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 10);

        associatedFilesModel = new DefaultListModel<>();
        associatedFilesList = new JList<>(associatedFilesModel);
        associatedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mainContentPanel.add(new JScrollPane(associatedFilesList), gbc);

        // --- Columna 1: Barra de herramientas ---
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        JToolBar actionsToolbar = new JToolBar(JToolBar.VERTICAL);
        actionsToolbar.setFloatable(false);
        // La registramos con una clave única para que el ProjectBuilder pueda encontrarla.
        actionsToolbar.setName("toolbar.acciones_det_exportacion");
        mainContentPanel.add(actionsToolbar, gbc);

        add(mainContentPanel, BorderLayout.CENTER);

        // --- Listeners para habilitar/deshabilitar botones ---
        associatedFilesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean isSelected = associatedFilesList.getSelectedIndex() != -1;
                if (removeAction != null) removeAction.setEnabled(isSelected);
                if (locateAction != null) locateAction.setEnabled(isSelected);
            }
        });
    } // ---FIN de metodo [initComponents]---

    public void updateDetails(ExportItem item) {
        if (item == null) {
            titleLabel.setText("Detalles para: (ningún ítem seleccionado)");
            associatedFilesModel.clear();
            if (addAction != null) addAction.setEnabled(false);
            if (removeAction != null) removeAction.setEnabled(false);
            if (locateAction != null) locateAction.setEnabled(false);
        } else {
            titleLabel.setText("Detalles para: " + item.getRutaImagen().getFileName().toString());
            associatedFilesModel.clear();
            if (item.getRutasArchivosAsociados() != null) {
                for (Path p : item.getRutasArchivosAsociados()) {
                    associatedFilesModel.addElement(p);
                }
            }
            if (addAction != null) addAction.setEnabled(true);
            boolean isSelected = associatedFilesList.getSelectedIndex() != -1;
            if (removeAction != null) removeAction.setEnabled(isSelected);
            if (locateAction != null) locateAction.setEnabled(isSelected);
        }
    } // ---FIN de metodo [updateDetails]---
    
    public void setActions(Action add, Action remove, Action locate) {
        this.addAction = add;
        this.removeAction = remove;
        this.locateAction = locate;

        JToolBar toolbar = findToolbar();
        if (toolbar != null) {
            toolbar.removeAll(); // Limpiamos por si se llama varias veces
            if (add != null) toolbar.add(add);
            if (remove != null) toolbar.add(remove);
            if (locate != null) toolbar.add(locate);
        }
        
        // Sincronizar estado inicial (deshabilitado)
        updateDetails(null); 
    } // ---FIN de metodo [setActions]---
    
    public Path getArchivoAsociadoSeleccionado() {
        return associatedFilesList.getSelectedValue();
    } // ---FIN de metodo [getArchivoAsociadoSeleccionado]---
    
    private JToolBar findToolbar() {
        // El mainContentPanel está en la posición 1 (CENTER) del BorderLayout del panel principal
        Component mainContent = this.getComponent(1);
        if (mainContent instanceof JPanel) {
             for (Component comp : ((JPanel) mainContent).getComponents()) {
                if (comp instanceof JToolBar) {
                    return (JToolBar) comp;
                }
            }
        }
        return null;
    } // ---FIN de metodo [findToolbar]---

} // --- FIN de clase [ExportDetailPanel]---