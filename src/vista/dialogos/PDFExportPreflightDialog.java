package vista.dialogos;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class PDFExportPreflightDialog extends JDialog {
    private static final long serialVersionUID = 1L;
	private boolean generateConfirmed = false;

    public PDFExportPreflightDialog(JFrame owner, List<Path> files, List<String> warnings) {
        super(owner, "Verificación previa de exportación", true);
        setLayout(new BorderLayout(10, 10));

        // Lista de archivos
        DefaultListModel<String> fileModel = new DefaultListModel<>();
        files.forEach(p -> fileModel.addElement(p.getFileName().toString()));
        JList<String> fileList = new JList<>(fileModel);
        
        // Lista de errores
        DefaultListModel<String> warnModel = new DefaultListModel<>();
        warnings.forEach(warnModel::addElement);
        JList<String> warnList = new JList<>(warnModel);
        warnList.setForeground(Color.RED);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                new JScrollPane(fileList), new JScrollPane(warnList));
        splitPane.setResizeWeight(0.5);
        
        JPanel panelContenido = new JPanel(new BorderLayout(5, 5));
        panelContenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelContenido.add(new JLabel("Archivos a exportar y problemas detectados:"), BorderLayout.NORTH);
        panelContenido.add(splitPane, BorderLayout.CENTER);
        add(panelContenido, BorderLayout.CENTER);

        // Botones
        JButton btnGenerate = new JButton("Generar PDF");
        btnGenerate.setEnabled(true); // Siempre permitido, el usuario decide
        btnGenerate.addActionListener(e -> {
            generateConfirmed = true;
            dispose();
        });

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.add(btnGenerate);
        panelBotones.add(btnCancel);
        add(panelBotones, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(500, 400));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isGenerateConfirmed() {
        return generateConfirmed;
    }
}
