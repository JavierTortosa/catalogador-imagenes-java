package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class ExportPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable tablaExportacion;
    private JButton btnSeleccionarCarpeta;
    private JTextField txtCarpetaDestino;
    private JButton btnIniciarExportacion;
    private JLabel lblResumen;
    private JButton btnCargarProyecto;
    private controlador.ProjectController projectController;

    public ExportPanel(controlador.ProjectController controller) { // <-- NUEVO PARÁMETRO
        super(new BorderLayout(5, 5));
        this.projectController = controller; // <-- NUEVA ASIGNACIÓN
        initComponents();
    } // --- Fin del método ExportPanel (constructor) ---

    private void initComponents() {
        // --- Panel Superior: Selección de Carpeta de Destino ---
        JPanel panelDestino = new JPanel(new BorderLayout(5, 0));
        txtCarpetaDestino = new JTextField("Seleccione una carpeta de destino...", 40);
        txtCarpetaDestino.setEditable(false);
        btnSeleccionarCarpeta = new JButton("...");
        panelDestino.add(new JLabel(" Destino: "), BorderLayout.WEST);
        panelDestino.add(txtCarpetaDestino, BorderLayout.CENTER);
        panelDestino.add(btnSeleccionarCarpeta, BorderLayout.EAST);
        
        // --- Panel Central: La Tabla con la Cola de Exportación ---
        tablaExportacion = new JTable();
        tablaExportacion.setModel(new ExportTableModel());

        // --- INICIO DE LA MODIFICACIÓN ---
        // Asignar el renderer y editor a la columna "Estado" (que es la columna con índice 2).
        // Se asume que las columnas son: 0="Imagen", 1="Archivo Comprimido", 2="Estado".
        if (tablaExportacion.getColumnCount() > 2) {
            tablaExportacion.getColumnModel().getColumn(2).setCellRenderer(new ExportStatusRenderer());
            tablaExportacion.getColumnModel().getColumn(2).setCellEditor(new ExportStatusEditor(
                // Se le pasa una referencia al método onExportItemManuallyAssigned del ProjectController.
                // Esto es un "callback" que se ejecutará cuando el editor termine su trabajo.
                projectController::onExportItemManuallyAssigned
            ));
            // Aumentar el alto de las filas para que los colores se vean mejor y haya espacio para clicar.
            tablaExportacion.setRowHeight(24);
        } else {
            System.err.println("WARN [ExportPanel]: El TableModel no tiene suficientes columnas para asignar Renderer/Editor.");
        }
        // --- FIN DE LA MODIFICACIÓN ---
        
        JScrollPane scrollTabla = new JScrollPane(tablaExportacion);

        // --- Panel Inferior: Botones de Acción y Resumen ---
        JPanel panelAccion = new JPanel(new BorderLayout(5, 0));
        btnCargarProyecto = new JButton("Cargar Selección a la Cola");
        panelAccion.add(btnCargarProyecto, BorderLayout.WEST);
        
        btnIniciarExportacion = new JButton("Iniciar Exportación");
        btnIniciarExportacion.setEnabled(false); // Deshabilitado hasta que todo esté listo
        lblResumen = new JLabel("  0 de 0 archivos listos para exportar.");
        panelAccion.add(btnIniciarExportacion, BorderLayout.EAST);
        panelAccion.add(lblResumen, BorderLayout.CENTER);

        // --- Ensamblaje Final ---
        this.add(panelDestino, BorderLayout.NORTH);
        this.add(scrollTabla, BorderLayout.CENTER);
        this.add(panelAccion, BorderLayout.SOUTH);

    } // --- Fin del método initComponents ---
    
    
    /**
     * Actualiza el estado de los controles del panel de exportación.
     * Habilita el botón "Iniciar Exportación" solo si se ha seleccionado una carpeta
     * de destino y todos los ítems de la cola están listos.
     * @param carpetaDestinoSeleccionada true si hay una carpeta de destino válida.
     * @param todosLosItemsListos true si todos los ítems en la cola tienen un estado válido.
     * @param mensajeResumen El texto a mostrar en la etiqueta de resumen.
     */
    public void actualizarEstadoControles(boolean carpetaDestinoSeleccionada, boolean todosLosItemsListos, String mensajeResumen) {
        if (btnIniciarExportacion != null) {
            btnIniciarExportacion.setEnabled(carpetaDestinoSeleccionada && todosLosItemsListos);
        }
        if (lblResumen != null) {
            lblResumen.setText("  " + mensajeResumen);
        }
    } // --- Fin del método actualizarEstadoControles ---
    
    
    public JButton getBotonCargarProyecto() {
        return this.btnCargarProyecto;
    } // --- Fin del método getBotonCargarProyecto ---
    
    /**
     * Devuelve la instancia del botón para seleccionar la carpeta de destino.
     * @return El JButton para seleccionar la carpeta.
     */
    public JButton getBotonSeleccionarCarpeta() {
        return this.btnSeleccionarCarpeta;
    } // --- Fin del método getBotonSeleccionarCarpeta ---
    
    
    /**
     * Establece la ruta de destino mostrada en el campo de texto.
     * @param ruta La ruta a mostrar.
     */
    public void setRutaDestino(String ruta) {
        if (this.txtCarpetaDestino != null) {
            this.txtCarpetaDestino.setText(ruta);
        }
    } // --- Fin del método setRutaDestino ---
    
    
    /**
     * Resalta el campo de texto de la ruta de destino.
     * @param resaltar true para poner un fondo de advertencia (rosa), false para volver al normal.
     */
    public void resaltarRutaDestino(boolean resaltar) {
        if (txtCarpetaDestino != null) {
            if (resaltar) {
                // Un color rosa claro para advertencia.
                txtCarpetaDestino.setBackground(new Color(255, 220, 220));
            } else {
                // Volver al color de fondo por defecto de un JTextField.
                // Usar null hace que herede el color del Look and Feel.
                txtCarpetaDestino.setBackground(null);
            }
        }
    } // --- Fin del método resaltarRutaDestino ---
    
    
    public String getRutaDestino() {
        return (this.txtCarpetaDestino != null) ? this.txtCarpetaDestino.getText() : "";
    } // --- Fin del método getRutaDestino ---
    
    
    public JButton getBotonIniciarExportacion() {
        return this.btnIniciarExportacion;
    } // --- Fin del método getBotonIniciarExportacion ---
    

} // --- FIN de la clase ExportPanel ---