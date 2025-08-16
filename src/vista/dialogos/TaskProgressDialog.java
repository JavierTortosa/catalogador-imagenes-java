package vista.dialogos;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskProgressDialog extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(TaskProgressDialog.class);
    
    private static final long serialVersionUID = 1L;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblTitle;
    private JButton btnCancel;
    private SwingWorker<?, ?> workerAsociado; 

    public TaskProgressDialog(JFrame parent, String windowTitle, String initialMessage) {
        super(parent, windowTitle, true);
        initComponents(initialMessage);
        pack();
        setSize(530, 150);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
    } // --- Fin del método TaskProgressDialog (constructor) ---

    private void initComponents(String initialMessage) {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();

        lblTitle = new JLabel(initialMessage);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(lblTitle, gbc);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setIndeterminate(true); 
        
        progressBar.setPreferredSize(new java.awt.Dimension(10, 12)); // Prueba con 18px de alto
        
        gbc.gridy = 1; 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.weightx = 1.0;
        contentPanel.add(progressBar, gbc);

        lblStatus = new JLabel("Iniciando...");
        gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 10, 0);
        contentPanel.add(lblStatus, gbc);

        btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> {
            if (workerAsociado != null && !workerAsociado.isDone()) {
                workerAsociado.cancel(true);
                btnCancel.setText("Cancelando...");
                btnCancel.setEnabled(false);
                lblTitle.setText("Cancelación en progreso...");
            }
        });
        gbc.gridy = 3; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        contentPanel.add(btnCancel, gbc);

        setContentPane(contentPanel);
    } // --- Fin del método initComponents ---

    public void setWorkerAsociado(SwingWorker<?, ?> worker) {
        this.workerAsociado = worker;
        if (this.btnCancel != null) {
            this.btnCancel.setEnabled(this.workerAsociado != null && !this.workerAsociado.isDone());
        }
    } // --- Fin del método setWorkerAsociado ---
    
    // --- MÉTODOS DE COMPATIBILIDAD ---

    public void setMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> lblTitle.setText(mensaje));
    } // --- Fin del método setMensaje ---

    public void actualizarContador(int contador) {
        updateStatusText("Archivos encontrados: " + contador);
    } // --- Fin del método actualizarContador ---

    public void setProgress(int value) {
        updateProgress(value, progressBar.getMaximum(), null);
    } // --- Fin del método setProgress ---

    public void setCurrentFileText(String text) {
        updateStatusText(text);
    } // --- Fin del método setCurrentFileText ---
    
    public void cerrar() {
        closeDialog();
    } // --- Fin del método cerrar ---
    
    // --- MÉTODOS INTERNOS ---

    public void updateProgress(int value, int max, String progressString) {
        SwingUtilities.invokeLater(() -> {
            if (progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(false);
                progressBar.setMaximum(max);
            }
            progressBar.setValue(value);
            if (progressString != null) {
//                progressBar.setString(progressString);
            	lblTitle.setText("Copiando archivos... (" + progressString + ")");
            }
        });
    } // --- Fin del método updateProgress ---

    // <<< CAMBIO: de 'private' a 'public' para que sea visible desde otras clases
    public void updateStatusText(String text) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("<html><body style='width: 300px;'>" + text + "</body></html>");
        });
    } // --- Fin del método updateStatusText ---
    
    public void setFinalMessageAndClose(String finalMessage, boolean isError, int delayMs) {
        SwingUtilities.invokeLater(() -> {
            lblTitle.setText(finalMessage);
            lblStatus.setText("");
            btnCancel.setVisible(false);
            if (isError) {
                progressBar.setValue(0);
                progressBar.setString("Error");
            } else {
                progressBar.setValue(progressBar.getMaximum());
            }
            javax.swing.Timer closeTimer = new javax.swing.Timer(delayMs, e -> closeDialog());
            closeTimer.setRepeats(false);
            closeTimer.start();
        });
    } // --- Fin del método setFinalMessageAndClose ---

    public void closeDialog() {
        SwingUtilities.invokeLater(this::dispose);
    } // --- Fin del método closeDialog ---

} // --- FIN de la clase TaskProgressDialog ---