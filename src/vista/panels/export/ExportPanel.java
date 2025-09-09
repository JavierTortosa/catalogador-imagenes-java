package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
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
    private JTextField txtCarpetaDestino;
    private JLabel lblResumen;
    private ProjectController projectController;
    private ExportTableModel tableModel;
    
    private Border originalTextFieldBorder;
    
    private ExportDetailPanel detailPanel;
    private JSplitPane splitPane;
    
    private int lastDividerLocation = -1; // Para recordar la posición del divisor
    private boolean isDetailsPanelVisible = false; // Para saber el estado actual
    
    public ExportPanel(ProjectController controller, java.util.function.Consumer<javax.swing.event.TableModelEvent> tableChangedCallback) {
        super(new BorderLayout(5, 5));
        this.projectController = controller;
        
        this.tableModel = new ExportTableModel(e -> {
        	if (tableChangedCallback != null) {
                tableChangedCallback.accept(e);
            }
            adjustRowHeights();
        });
        
        initComponents();
        
        if (detailPanel != null && splitPane != null) {
            detailPanel.setVisible(false);
            splitPane.setDividerSize(0);
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(1.0));
        }
        
    } // ---FIN de metodo [ExportPanel]---


    private void initComponents() {
        // --- Tabla de Exportación y Panel de Detalles (SIN CAMBIOS) ---
        tablaExportacion = new JTable(this.tableModel); 
        tablaExportacion.setFillsViewportHeight(true);
        tablaExportacion.setShowGrid(true);
        tablaExportacion.setGridColor(UIManager.getColor("Component.borderColor"));
        
        TableColumn checkColumn = tablaExportacion.getColumnModel().getColumn(0);
        checkColumn.setPreferredWidth(30);
        checkColumn.setMaxWidth(30);
        tablaExportacion.getTableHeader().getColumnModel().getColumn(0).setHeaderRenderer(new CheckHeaderRenderer());
        TableColumn statusColumn = tablaExportacion.getColumnModel().getColumn(2);
        statusColumn.setCellRenderer(new vista.panels.export.StatusCellRenderer(projectController.getController().getIconUtils()));
        TableColumn assignedFilesColumn = tablaExportacion.getColumnModel().getColumn(3);
        assignedFilesColumn.setCellRenderer(new MultiLineCellRenderer());
        assignedFilesColumn.setPreferredWidth(300);
        tablaExportacion.getTableHeader().addMouseListener(new HeaderMouseListener(tablaExportacion));
        
        tablaExportacion.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tablaExportacion.getSelectedRow();
                ExportItem selectedItem = (selectedRow != -1) ? tableModel.getItemAt(selectedRow) : null;
                if (projectController != null) {
                    projectController.mostrarImagenDeExportacion(selectedItem != null ? selectedItem.getRutaImagen() : null);
                }
                if (detailPanel != null) {
                    detailPanel.updateDetails(selectedItem);
                }
            }
        });
        
        JScrollPane scrollTabla = new JScrollPane(tablaExportacion);

        detailPanel = new ExportDetailPanel();
        
        Action addAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE);
        Action removeAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE);
        Action locateAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE);
        
        detailPanel.setActions(addAction, removeAction, locateAction);
                
        detailPanel.setPreferredSize(new java.awt.Dimension(0, 120));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, detailPanel);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(1.0);

        this.add(splitPane, BorderLayout.CENTER);

        // --- LÓGICA DEL PANEL SUR (CORREGIDA) ---
        ToolbarManager toolbarManager = projectController.getController().getToolbarManager();
        if (toolbarManager != null) {
            // 1. Obtenemos la toolbar que ahora SÓLO contiene el JTextField y los botones.
            JToolBar exportActionsToolbar = toolbarManager.getToolbar("acciones_exportacion");
            
            if (exportActionsToolbar != null) {
                // 2. Obtenemos las referencias a los componentes que la toolbar SÍ crea.
                this.txtCarpetaDestino = (JTextField) projectController.getController().getGeneralController().getRegistry().get("textfield.export.destino");
                if (this.txtCarpetaDestino != null) {
                    this.originalTextFieldBorder = this.txtCarpetaDestino.getBorder();
                }
                
                // 3. Creamos un panel contenedor con BorderLayout para el layout manual.
                JPanel southPanel = new JPanel(new BorderLayout(10, 0));
                southPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Padding sutil
                
                // 4. Creamos nuestro JLabel manualmente.
                this.lblResumen = new JLabel("Cargue la selección para ver el estado.");
                
                // 5. LO MÁS IMPORTANTE: Registramos el JLabel creado manualmente.
                //projectController.getController().getGeneralController().getRegistry().register("label.export.resumen", this.lblResumen);
                
                // 6. Añadimos el JLabel a la IZQUIERDA del panel contenedor.
                southPanel.add(this.lblResumen, BorderLayout.WEST);
                
                // 7. Creamos un JPanel normal para los controles, en lugar de usar la JToolBar directamente.
                // FlowLayout alineado a la derecha, con 5px de espacio horizontal y vertical.
                JPanel controlsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 0));
                controlsPanel.setOpaque(false); // Para que herede el fondo del panel sur

                // 8. Movemos los componentes DESDE la JToolBar a nuestro nuevo JPanel.
                // Esto preserva las acciones y tooltips que el ToolbarBuilder ya configuró.
                while (exportActionsToolbar.getComponentCount() > 0) {
                    controlsPanel.add(exportActionsToolbar.getComponent(0));
                }

                // 9. Añadimos nuestro panel de controles a la DERECHA del panel sur.
                southPanel.add(controlsPanel, BorderLayout.EAST);
                
                // 10. Añadimos el panel contenedor al sur del ExportPanel principal.
                this.add(southPanel, BorderLayout.SOUTH);
                
            } 
        }
    } // ---FIN de metodo [initComponents]---
    
    
    /**
     * Recalcula y establece la posición del divisor del JSplitPane interno.
     * Debe llamarse DESPUÉS de que el panel se haya hecho visible.
     * Esta es la forma programática de "tocar" el divisor para que se auto-ajuste.
     */
    public void resetDividerLocation() {
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) {
                splitPane.resetToPreferredSizes();
                logger.debug("Se ha invocado resetToPreferredSizes() en el JSplitPane de ExportPanel.");
            }
        });
    } // ---FIN de metodo [resetDividerLocation]---
    
    
    private void adjustRowHeights() {
        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < tablaExportacion.getRowCount(); row++) {
                int rowHeight = tablaExportacion.getRowHeight(row);
                TableCellRenderer renderer = tablaExportacion.getCellRenderer(row, 3);
                Component comp = tablaExportacion.prepareRenderer(renderer, row, 3);
                int newHeight = comp.getPreferredSize().height;
                if (rowHeight != newHeight) {
                    tablaExportacion.setRowHeight(row, newHeight);
                }
            }
        });
    } // ---FIN de metodo [adjustRowHeights]---

    public void setActionsToolbar(JToolBar newToolbar) {
        BorderLayout layout = (BorderLayout) getLayout();
        Component oldToolbar = layout.getLayoutComponent(BorderLayout.SOUTH);
        if (oldToolbar != null) remove(oldToolbar);
        if (newToolbar != null) add(newToolbar, BorderLayout.SOUTH);
        revalidate();
        repaint();
    } // ---FIN de metodo [setActionsToolbar]---

    public void actualizarEstadoControles(boolean puedeExportar, String mensajeResumen) {
    	
    	logger.debug("Actualizando controles de exportación con mensaje: {}", mensajeResumen);
    	
        if (lblResumen != null) {
            lblResumen.setText("  " + mensajeResumen);
            lblResumen.setForeground(puedeExportar ? UIManager.getColor("Label.foreground") : Color.ORANGE);
        }
        Action iniciarExportAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_INICIAR_EXPORTACION);
        if (iniciarExportAction != null) {
            iniciarExportAction.setEnabled(puedeExportar);
        }
    } // ---FIN de metodo [actualizarEstadoControles]---

    public void setRutaDestino(String ruta) {
        if (this.txtCarpetaDestino != null) {
            this.txtCarpetaDestino.setText(ruta);
        }
    } // ---FIN de metodo [setRutaDestino]---

    public void resaltarRutaDestino(boolean resaltar) {
        if (txtCarpetaDestino != null) {
            if (resaltar) {
                txtCarpetaDestino.setBackground(new Color(255, 220, 220));
                txtCarpetaDestino.setBorder(javax.swing.BorderFactory.createLineBorder(UIManager.getColor("Component.error.borderColor"), 2));
            } else {
                txtCarpetaDestino.setBackground(UIManager.getColor("TextField.background"));
                txtCarpetaDestino.setBorder(originalTextFieldBorder);
            }
        }
    } // ---FIN de metodo [resaltarRutaDestino]---

    public String getRutaDestino() {
        return (this.txtCarpetaDestino != null) ? this.txtCarpetaDestino.getText() : "";
    } // ---FIN de metodo [getRutaDestino]---

    public JTable getTablaExportacion() {
        return this.tablaExportacion;
    } // ---FIN de metodo [getTablaExportacion]---
    
    /**
     * Alterna la visibilidad del panel de detalles inferior.
     * Si está visible, lo oculta y expande la tabla.
     * Si está oculto, lo muestra y restaura la posición del divisor.
     */
    public void toggleDetailsPanelVisibility() {
        isDetailsPanelVisible = !isDetailsPanelVisible; // Invertimos el estado

        if (isDetailsPanelVisible) {
            // --- MOSTRAR PANEL DE DETALLES ---
            logger.debug("Mostrando el panel de detalles de exportación.");
            detailPanel.setVisible(true);
            splitPane.setDividerSize(5); // Restauramos el grosor del divisor (valor por defecto)

            // Restauramos la posición del divisor a donde estaba, o a una posición por defecto
            if (lastDividerLocation != -1) {
                splitPane.setDividerLocation(lastDividerLocation);
            } else {
                // Si no hay una posición guardada, le decimos que se ajuste a los tamaños preferidos
                splitPane.resetToPreferredSizes();
            }

        } else {
            // --- OCULTAR PANEL DE DETALLES ---
            logger.debug("Ocultando el panel de detalles de exportación.");
            // Guardamos la posición actual del divisor ANTES de ocultarlo
            lastDividerLocation = splitPane.getDividerLocation();
            
            detailPanel.setVisible(false);
            splitPane.setDividerSize(0); // Ocultamos el divisor
            splitPane.setDividerLocation(1.0); // Movemos el divisor completamente hacia abajo
        }
        
        // Es importante revalidar el panel para que los cambios se apliquen correctamente
        revalidate();
        repaint();
    } // ---FIN de metodo [toggleDetailsPanelVisibility]---
    
    
    public ExportDetailPanel getDetailPanel() {
        return this.detailPanel;
    } // ---FIN de metodo [getDetailPanel]---

} // --- FIN de clase [ExportPanel]---
