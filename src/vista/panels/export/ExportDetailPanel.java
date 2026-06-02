package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.nio.file.Path;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import modelo.proyecto.ExportItem;

public class ExportDetailPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel titleLabel;
    private JList<Path> associatedFilesList;
    private DefaultListModel<Path> associatedFilesModel;

    private JTextField piezasField;
    private JTextField lvlField;
    private JTextField pvpField;
    private JTextField notasField;

    private ExportItem currentItem;

    private Action addAction;
    private Action removeAction;
    private Action locateAction;

    private JToolBar actionsToolbar;

    public ExportDetailPanel() {
        super(new BorderLayout(5, 5));

        TitledBorder detailsBorder = BorderFactory.createTitledBorder("Detalles para:");
        setBorder(detailsBorder);

        putClientProperty("borderTitleKey", "Detalles para:");

        initComponents();
    }

    private void initComponents() {
        titleLabel = new JLabel("(ningún ítem seleccionado)");
        titleLabel.putClientProperty("FlatLaf.style", "font: bold");
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 3));
        formPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        formPanel.add(new JLabel("Piezas:"));
        piezasField = new JTextField();
        formPanel.add(piezasField);

        formPanel.add(new JLabel("LVL:"));
        lvlField = new JTextField();
        formPanel.add(lvlField);

        formPanel.add(new JLabel("PVP:"));
        pvpField = new JTextField();
        formPanel.add(pvpField);

        formPanel.add(new JLabel("Notas:"));
        notasField = new JTextField();
        formPanel.add(notasField);

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { saveFields(); }
            public void removeUpdate(DocumentEvent e) { saveFields(); }
            public void changedUpdate(DocumentEvent e) { saveFields(); }
        };
        piezasField.getDocument().addDocumentListener(docListener);
        lvlField.getDocument().addDocumentListener(docListener);
        pvpField.getDocument().addDocumentListener(docListener);
        notasField.getDocument().addDocumentListener(docListener);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(formPanel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        associatedFilesModel = new DefaultListModel<>();
        associatedFilesList = new JList<>(associatedFilesModel);
        associatedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollAssociatedFiles = new JScrollPane(associatedFilesList);
        scrollAssociatedFiles.setName("scroll.detalles.exportacion");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        listPanel.add(scrollAssociatedFiles, gbc);

        actionsToolbar = new JToolBar(JToolBar.VERTICAL);
        actionsToolbar.setFloatable(false);
        actionsToolbar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        listPanel.add(actionsToolbar, gbc);

        centerPanel.add(listPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        associatedFilesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean isSelected = associatedFilesList.getSelectedIndex() != -1;
                if (removeAction != null) removeAction.setEnabled(isSelected);
                if (locateAction != null) locateAction.setEnabled(isSelected);
            }
        });
    }

    private void saveFields() {
        if (currentItem == null) return;
        try {
            currentItem.setPiezas(piezasField.getText().isEmpty() ? 0 : Integer.parseInt(piezasField.getText()));
        } catch (NumberFormatException e) {
        }
        currentItem.setLvl(lvlField.getText());
        currentItem.setPvp(pvpField.getText());
        currentItem.setNotas(notasField.getText());
    }

    public void updateDetails(ExportItem item) {
        this.currentItem = item;
        if (item == null) {
            titleLabel.setText("(ningún ítem seleccionado)");
            associatedFilesModel.clear();
            piezasField.setText("");
            lvlField.setText("");
            pvpField.setText("");
            notasField.setText("");
            if (addAction != null) addAction.setEnabled(false);
            if (removeAction != null) removeAction.setEnabled(false);
            if (locateAction != null) locateAction.setEnabled(false);
        } else {
            Path fn = item.getRutaImagen().getFileName();
            titleLabel.setText((fn != null ? fn.toString() : item.getRutaImagen().toString()));
            associatedFilesModel.clear();
            if (item.getRutasArchivosAsociados() != null) {
                for (Path p : item.getRutasArchivosAsociados()) {
                    associatedFilesModel.addElement(p);
                }
            }
            piezasField.setText(item.getPiezas() > 0 ? String.valueOf(item.getPiezas()) : "");
            lvlField.setText(item.getLvl() != null ? item.getLvl() : "");
            pvpField.setText(item.getPvp() != null ? item.getPvp() : "");
            notasField.setText(item.getNotas() != null ? item.getNotas() : "");
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
