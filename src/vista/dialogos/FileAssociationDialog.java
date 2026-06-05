package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileAssociationDialog extends JDialog {

    private String selectedPath = null;
    private JTextField txtPath;

    public FileAssociationDialog(Frame parent) {
        super(parent, "Vincular Archivo 3D", true);
        initComponents();
        setSize(400, 150);
        setLocationRelativeTo(parent);
    }

    public String getSelectedPath() {
        return selectedPath;
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel lblInfo = new JLabel("Selecciona el archivo .zip, .rar o .stl a vincular:");
        mainPanel.add(lblInfo, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        txtPath = new JTextField();
        txtPath.setEditable(false);
        centerPanel.add(txtPath, BorderLayout.CENTER);

        JButton btnBrowse = new JButton("Explorar...");
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Archivos 3D y Comprimidos", "zip", "rar", "stl", "obj"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                txtPath.setText(f.getAbsolutePath());
            }
        });
        centerPanel.add(btnBrowse, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Aceptar");
        btnOk.addActionListener(e -> {
            if (!txtPath.getText().isEmpty()) {
                selectedPath = txtPath.getText();
            }
            dispose();
        });
        
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        
        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }
}
