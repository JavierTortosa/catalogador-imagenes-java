package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import modelo.proyecto.ExportItem;

public class ExportDetailPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel titleLabel;
    private JList<Path> associatedFilesList;
    private DefaultListModel<Path> associatedFilesModel;

    private Action addAction;
    private Action removeAction;
    private Action locateAction;

    private JToolBar actionsToolbar;

    public ExportDetailPanel() {
        // 1. El panel exterior (este recibe el foco). 
        // Le damos un borde vacío para que el anillo morado tenga espacio y no pise al interior.
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3)); 
        
        initComponents();
    }

    private void initComponents() {
        // Extraemos el color de borde del tema actual (FlatLaf)
        Color bColor = UIManager.getColor("Component.borderColor");
        if (bColor == null) bColor = Color.GRAY;
        Border baseLineBorder = BorderFactory.createLineBorder(bColor, 1);

        // --- CONTENEDOR PROTEGIDO 1 (El Panel Completo) ---
        // Este panel interno mantiene el borde visual permanente a salvo del foco
        JPanel protectedMainPanel = new JPanel(new BorderLayout(5, 5));
        TitledBorder mainBorder = BorderFactory.createTitledBorder(baseLineBorder, "Detalles de Asignación");
        protectedMainPanel.setBorder(mainBorder);

        titleLabel = new JLabel("(ningún ítem seleccionado)");
        titleLabel.putClientProperty("FlatLaf.style", "font: bold");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); 
        protectedMainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        associatedFilesModel = new DefaultListModel<>();
        associatedFilesList = new JList<>(associatedFilesModel);
        associatedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // El JScrollPane está registrado. Tu sistema le cambiará el borde a morado al pulsarlo.
        JScrollPane scrollAssociatedFiles = new JScrollPane(associatedFilesList);
        scrollAssociatedFiles.setName("scroll.detalles.exportacion");

        // --- CONTENEDOR PROTEGIDO 2 (La Lista de Archivos) ---
        // Envolvemos el ScrollPane en otro panel para que el texto de la caja sea intocable
        JPanel protectedListPanel = new JPanel(new BorderLayout());
        TitledBorder listBorder = BorderFactory.createTitledBorder(baseLineBorder, "Archivos Asignados");
        protectedListPanel.setBorder(listBorder);
        // Añadimos el scroll. Cuando se pinche, el borde morado aparecerá DENTRO del cuadro gris.
        protectedListPanel.add(scrollAssociatedFiles, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 5, 5);
        listPanel.add(protectedListPanel, gbc);

        actionsToolbar = new JToolBar(JToolBar.VERTICAL);
        actionsToolbar.setFloatable(false);
        actionsToolbar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 5));

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        listPanel.add(actionsToolbar, gbc);

        protectedMainPanel.add(listPanel, BorderLayout.CENTER);

        // Finalmente, añadimos el contenedor blindado al panel base
        add(protectedMainPanel, BorderLayout.CENTER);

        associatedFilesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean isSelected = associatedFilesList.getSelectedIndex() != -1;
                if (removeAction != null) removeAction.setEnabled(isSelected);
                if (locateAction != null) locateAction.setEnabled(isSelected);
            }
        });
    }

    public void updateDetails(ExportItem item) {
        if (item == null) {
            titleLabel.setText("(ningún ítem seleccionado)");
            associatedFilesModel.clear();
            if (addAction != null) addAction.setEnabled(false);
            if (removeAction != null) removeAction.setEnabled(false);
            if (locateAction != null) locateAction.setEnabled(false);
        } else {
            Path fn = item.getRutaImagen().getFileName();
            titleLabel.setText("Imagen: " + (fn != null ? fn.toString() : item.getRutaImagen().toString()));
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
    }

    public void setActions(Action add, Action remove, Action locate) {
        this.addAction = add;
        this.removeAction = remove;
        this.locateAction = locate;

        actionsToolbar.removeAll();
        if (add != null) actionsToolbar.add(createToolbarButton(add));
        if (remove != null) actionsToolbar.add(createToolbarButton(remove));
        if (locate != null) actionsToolbar.add(createToolbarButton(locate));

        updateDetails(null);
    }

    private JButton createToolbarButton(Action action) {
        JButton button = new JButton(action);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    }

    public Path getArchivoAsociadoSeleccionado() {
        return associatedFilesList.getSelectedValue();
    }

}
