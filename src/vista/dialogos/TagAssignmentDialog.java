package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import java.nio.file.Path;
import javax.swing.*;

import controlador.managers.DataManager;
import vista.components.TagIntelliSenseField;

/**
 * Diálogo para asignar etiquetas (existentes o nuevas) a un conjunto de imágenes.
 */
public class TagAssignmentDialog extends JDialog {
    private TagIntelliSenseField tagField;
    private DataManager dataManager;
    private List<Path> selectedPaths;
    private boolean confirmed = false;

    public TagAssignmentDialog(Frame owner, DataManager dataManager, List<Path> selectedPaths) {
        super(owner, "Asignar Etiquetas", true);
        this.dataManager = dataManager;
        this.selectedPaths = selectedPaths;
        initComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentPanel.add(new JLabel("Escribe la etiqueta (usa '.' para jerarquías):"), BorderLayout.NORTH);

        tagField = new TagIntelliSenseField();
        tagField.setColumns(20);
        tagField.refreshTags(dataManager.getAllTags());
        contentPanel.add(tagField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton btnOk = new JButton("Asignar");
        JButton btnCancel = new JButton("Cancelar");

        btnOk.addActionListener(e -> {
            String input = tagField.getText().trim();
            if (!input.isEmpty()) {
                dataManager.assignDotNotationTagToImages(selectedPaths, input);
                confirmed = true;
                dispose();
            }
        });

        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
} //--- Fin de la clase: TagAssignmentDialog
