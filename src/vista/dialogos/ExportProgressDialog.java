package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

public class ExportProgressDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private JProgressBar overallProgressBar;
    private JLabel lblCurrentFile;

    public ExportProgressDialog(JFrame parent) {
        super(parent, "Progreso de la Exportación", true); // true para hacerlo modal
        initComponents();
        setSize(450, 120);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // El worker controlará el cierre
    } // --- Fin del método ExportProgressDialog (constructor) ---

    private void initComponents() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblTitle = new JLabel("Copiando archivos del proyecto...");
        contentPanel.add(lblTitle, BorderLayout.NORTH);

        overallProgressBar = new JProgressBar(0, 100);
        overallProgressBar.setStringPainted(true);
        contentPanel.add(overallProgressBar, BorderLayout.CENTER);

        lblCurrentFile = new JLabel("Iniciando...");
        lblCurrentFile.setPreferredSize(new Dimension(100, 20)); // Evita que el diálogo cambie de tamaño
        contentPanel.add(lblCurrentFile, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    } // --- Fin del método initComponents ---

    public void setProgress(int value) {
        overallProgressBar.setValue(value);
    } // --- Fin del método setProgress ---

    public void setCurrentFileText(String text) {
        lblCurrentFile.setText(text);
    } // --- Fin del método setCurrentFileText ---

} // --- FIN de la clase ExportProgressDialog ---