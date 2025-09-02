package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import modelo.proyecto.ExportItem;

public class ExportPanel extends JPanel {

	private static final Logger logger = LoggerFactory.getLogger(ExportPanel.class);
	
    private static final long serialVersionUID = 1L;

    private JTable tablaExportacion;
    private JButton btnSeleccionarCarpeta;
    private JTextField txtCarpetaDestino;
    private JLabel lblResumen;
    private ProjectController projectController;
    private JToolBar actionToolBar; 
    private ExportTableModel tableModel;
    
    private Border originalTextFieldBorder; // Guardar el borde original
    private final Border warningBorder = javax.swing.BorderFactory.createLineBorder(Color.RED, 1);
    
    public ExportPanel(ProjectController controller, java.util.function.Consumer<javax.swing.event.TableModelEvent> tableChangedCallback) {
        super(new BorderLayout(5, 5));
        this.projectController = controller;
        
        // --- Inicializar el TableModel aquí ---
        this.tableModel = new ExportTableModel(tableChangedCallback);
        
        initComponents(tableChangedCallback);
    } // --- Fin del método ExportPanel (constructor) ---


    private void initComponents(java.util.function.Consumer<javax.swing.event.TableModelEvent> tableChangedCallback) {
        // --- Panel Superior: Selección de Carpeta de Destino ---
        JPanel panelDestino = new JPanel(new BorderLayout(5, 0));

        txtCarpetaDestino = new JTextField("Seleccione una carpeta de destino...");
        txtCarpetaDestino.setEditable(false); 
        
        this.originalTextFieldBorder = txtCarpetaDestino.getBorder();
        
        panelDestino.add(txtCarpetaDestino, BorderLayout.CENTER);

        
        this.add(panelDestino, BorderLayout.NORTH);
        
        // --- Tabla de Exportación ---
        // --- La tabla ahora se crea con el tableModel que ya hemos inicializado ---
        tablaExportacion = new JTable(this.tableModel); 
        tablaExportacion.setFillsViewportHeight(true);
        tablaExportacion.setRowHeight(24); // Altura de fila para que los iconos se vean bien
        tablaExportacion.setShowGrid(false); // Opcional: ocultar la rejilla
        
        // --- Configuración de Columnas y Renderers/Editores ---
        // Columna 0: Checkbox
        TableColumn checkColumn = tablaExportacion.getColumnModel().getColumn(0);
        checkColumn.setPreferredWidth(30);
        checkColumn.setMaxWidth(30);
        checkColumn.setMinWidth(30);
        
        // --- Añadimos el HeaderRenderer para la columna 0 ---
        tablaExportacion.getTableHeader().getColumnModel().getColumn(0).setHeaderRenderer(new CheckHeaderRenderer());

        // Columna 1: Nombre de la Imagen
        TableColumn imageNameColumn = tablaExportacion.getColumnModel().getColumn(1);
        imageNameColumn.setPreferredWidth(250); // Un ancho más grande para el nombre del archivo

        // Columna 2: Estado de Exportación (Icono + Texto)
        TableColumn statusColumn = tablaExportacion.getColumnModel().getColumn(2);
        statusColumn.setPreferredWidth(150); // Suficiente para el estado y texto
        
        // --- Usamos tu StatusCellRenderer.java para esta columna ---
        // Asumo que StatusCellRenderer es la clase que quieres usar como ExportStatusCellRenderer
        // y está importada como vista.renderers.ExportStatusCellRenderer.
        statusColumn.setCellRenderer(new vista.panels.export.StatusCellRenderer(projectController.getController().getIconUtils()));


        // Añadir listener a la cabecera de la primera columna para seleccionar/desseleccionar todo
        tablaExportacion.getTableHeader().addMouseListener(new HeaderMouseListener(tablaExportacion));

        // --- NUEVO: Listener de selección de fila para cargar la imagen en el visor principal ---
        tablaExportacion.getSelectionModel().addListSelectionListener(e -> {
            // Asegurarse de que la selección ha terminado y no es parte de un arrastre o redibujado intermedio
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tablaExportacion.getSelectedRow();
                // Asegurarse de que hay una fila seleccionada (no -1)
                if (selectedRow != -1) {
                    ExportTableModel modelTabla = (ExportTableModel) tablaExportacion.getModel();
                    ExportItem selectedItem = modelTabla.getItemAt(selectedRow);
                    if (selectedItem != null) {
                        // Llamar al ProjectController para que muestre la imagen de este item.
                        // Aseguramos que projectController NO sea null.
                        if (projectController != null) {
                            projectController.mostrarImagenDeExportacion(selectedItem.getRutaImagen());
                        } else {
                            System.err.println("WARN [ExportPanel]: ProjectController es null, no se puede mostrar la imagen de exportación.");
                        }
                    }
                }
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaExportacion);
        this.add(scrollTabla, BorderLayout.CENTER);

        // --- Panel Inferior: Barra de Acciones y Resumen ---
        
        // 1. Obtenemos el ToolbarManager a través del controlador
        ToolbarManager toolbarManager = projectController.getController().getToolbarManager();
        
        if (toolbarManager != null) {
            // 2. Pedimos la barra de herramientas de acciones de exportación
            JToolBar exportActionsToolbar = toolbarManager.getToolbar("acciones_exportacion");
            exportActionsToolbar.setOpaque(false);
            
            if (exportActionsToolbar != null) {
                // 3. Añadimos el resumen de texto al final de la toolbar
                exportActionsToolbar.add(javax.swing.Box.createHorizontalGlue());
                lblResumen = new JLabel("Cargue la selección para ver el estado.");
                exportActionsToolbar.add(lblResumen);
                exportActionsToolbar.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(5, 0)));
                
                // 4. Añadimos la toolbar completa al sur del panel
                this.add(exportActionsToolbar, BorderLayout.SOUTH);
            } 
        } else {
            System.err.println("ERROR CRÍTICO: ToolbarManager es nulo, no se puede construir la barra de acciones de exportación.");
        }

    } // --- Fin del método initComponents ---
     
    
    /**
     * Establece o reemplaza de forma segura la barra de herramientas de acciones.
     * Este método se encarga de quitar la barra antigua y añadir la nueva en la
     * región sur (SOUTH) del BorderLayout del panel.
     * @param newToolbar La nueva JToolBar a mostrar.
     */
    public void setActionsToolbar(JToolBar newToolbar) {
        // 1. Obtener el layout del panel (sabemos que es BorderLayout)
        BorderLayout layout = (BorderLayout) getLayout();

        // 2. Buscar si ya existe un componente en la región SUR
        Component oldToolbar = layout.getLayoutComponent(BorderLayout.SOUTH);
        if (oldToolbar != null) {
            // Si existe, lo eliminamos primero
            remove(oldToolbar);
            logger.debug("  [ExportPanel] Barra de herramientas antigua eliminada.");
        }

        // 3. Añadir la nueva barra de herramientas si no es nula
        if (newToolbar != null) {
            add(newToolbar, BorderLayout.SOUTH);
            logger.debug("  [ExportPanel] Nueva barra de herramientas añadida.");
        }

        // 4. Revalidar y repintar el panel para que los cambios se muestren
        revalidate();
        repaint();
        
    } // --- FIN del metodo setActionsToolbar ---
    
    
    // El método actualizarEstadoControles ahora solo necesita actualizar el resumen
    // ya que el estado de los botones lo gestionan las Actions.
    public void actualizarEstadoControles(boolean puedeExportar, String mensajeResumen) {
        if (lblResumen != null) {
            lblResumen.setText("  " + mensajeResumen);
        }
        
        if (puedeExportar) {
            lblResumen.setForeground(UIManager.getColor("Label.foreground")); // Color normal
        } else {
            lblResumen.setForeground(Color.ORANGE); // Color de advertencia
        }
        
        // El estado del botón de exportación lo maneja su propia Action, pero podemos forzarlo si es necesario.
        Action iniciarExportAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_INICIAR_EXPORTACION);
        if (iniciarExportAction != null) {
            iniciarExportAction.setEnabled(puedeExportar);
        }
        
    } // --- Fin del método actualizarEstadoControles ---
    
    
    public JButton getBotonSeleccionarCarpeta() {
        return this.btnSeleccionarCarpeta;
    } // --- Fin del método getBotonSeleccionarCarpeta ---
    
    
    public void setRutaDestino(String ruta) {
        if (this.txtCarpetaDestino != null) {
            this.txtCarpetaDestino.setText(ruta);
        }
    } // --- Fin del método setRutaDestino ---
    
    
    public void resaltarRutaDestino(boolean resaltar) {
        if (txtCarpetaDestino != null) {
            if (resaltar) {

            	txtCarpetaDestino.setBackground(new Color(255, 220, 220));
            	
            	txtCarpetaDestino.setBorder(javax.swing.BorderFactory.createLineBorder(UIManager.getColor("Component.error.borderColor"), 2));
            	
            } else {
            	
                txtCarpetaDestino.setBackground(null);
                txtCarpetaDestino.setBorder(originalTextFieldBorder);
                
            }
        }
    } // --- Fin del método resaltarRutaDestino ---
    
    
    public String getRutaDestino() {
        return (this.txtCarpetaDestino != null) ? this.txtCarpetaDestino.getText() : "";
    } // --- Fin del método getRutaDestino ---
    

    /**
     * Proporciona acceso directo a la JTable de exportación encapsulada en este panel.
     * @return La instancia de JTable utilizada para mostrar la cola de exportación.
     */
    public JTable getTablaExportacion() {
        return this.tablaExportacion;
    } // --- Fin del método getTablaExportacion ---
    
    
} // --- FIN de la clase ExportPanel ---