package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import modelo.proyecto.ExportItem;

public class ExportDetailPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel titleLabel;
    private JList<Path> associatedFilesList;
    private DefaultListModel<Path> associatedFilesModel;
    
    // Las acciones siguen siendo necesarias para habilitar/deshabilitar
    private Action addAction;
    private Action removeAction;
    private Action locateAction;

    // CAMBIO: Se añade una referencia a la barra de herramientas interna
    private JToolBar actionsToolbar;

    public ExportDetailPanel() {
        super(new BorderLayout(5, 5));
        
     // Creamos un TitledBorder y se lo asignamos al panel
        TitledBorder detailsBorder = BorderFactory.createTitledBorder("Detalles para:");
        setBorder(detailsBorder);
        
        // Lo registramos para que el ThemeManager pueda cambiarle el color del texto
        putClientProperty("borderTitleKey", "Detalles para:"); // Guardamos el título base
        
        initComponents();
    } // ---FIN de metodo [ExportDetailPanel]---

    private void initComponents() {
        titleLabel = new JLabel("Detalles para: (ningún ítem seleccionado)");
        titleLabel.putClientProperty("FlatLaf.style", "font: bold");
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        associatedFilesModel = new DefaultListModel<>();
        associatedFilesList = new JList<>(associatedFilesModel);
        associatedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // --- INICIO DE LA MODIFICACIÓN ---
        JScrollPane scrollAssociatedFiles = new JScrollPane(associatedFilesList);
        // Le damos una identidad inequívoca al JScrollPane.
        scrollAssociatedFiles.setName("scroll.detalles.exportacion"); 
        // --- FIN DE LA MODIFICACIÓN ---

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(scrollAssociatedFiles, gbc); // Añadimos el JScrollPane con nombre
        
        actionsToolbar = new JToolBar(JToolBar.VERTICAL);
        actionsToolbar.setFloatable(false);
        actionsToolbar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        centerPanel.add(actionsToolbar, gbc);

        add(centerPanel, BorderLayout.CENTER);

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

        // CAMBIO: Poblar la barra de herramientas interna
        actionsToolbar.removeAll();
        if (add != null) actionsToolbar.add(createToolbarButton(add));
        if (remove != null) actionsToolbar.add(createToolbarButton(remove));
        if (locate != null) actionsToolbar.add(createToolbarButton(locate));
        
        updateDetails(null); 
    } // ---FIN de metodo [setActions]---
    
    // CAMBIO: Pequeño método helper para configurar los botones
    private JButton createToolbarButton(Action action) {
        JButton button = new JButton(action);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    } // ---FIN de metodo [createToolbarButton]---
    
    public Path getArchivoAsociadoSeleccionado() {
        return associatedFilesList.getSelectedValue();
    } // ---FIN de metodo [getArchivoAsociadoSeleccionado]---

} // --- FIN de clase [ExportDetailPanel]---


