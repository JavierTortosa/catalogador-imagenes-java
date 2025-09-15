package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.BorderFactory;
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
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
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
    
    private boolean highlightingListenerConfigured = false;
    
    
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
        
        // LA LLAMADA A setupHighlightingListener() SE ELIMINA COMPLETAMENTE DE AQUÍ
        
        Component parent = tablaExportacion.getParent();
        if (parent instanceof javax.swing.JViewport) {
            Component grandparent = parent.getParent();
            if (grandparent instanceof JScrollPane) {
                projectController.getRegistry().register("scroll.tabla.exportacion", (JScrollPane) grandparent);
            }
        }
        
        if (detailPanel != null && splitPane != null) {
            detailPanel.setVisible(false);
            splitPane.setDividerSize(0);
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(1.0));
        }
        
    } // ---FIN de metodo [ExportPanel]---


    private void initComponents() {
        this.setLayout(new BorderLayout(5, 5));

        ToolbarManager toolbarManager = projectController.getController().getToolbarManager();
        if (toolbarManager != null) {
            JToolBar exportActionsToolbar = toolbarManager.getToolbar("acciones_exportacion");
            if (exportActionsToolbar != null) {
                exportActionsToolbar.setOrientation(JToolBar.VERTICAL);
                exportActionsToolbar.setFloatable(false);
                this.add(exportActionsToolbar, BorderLayout.EAST);
            }
        }
        
        JPanel mainContentPanel = new JPanel(new BorderLayout(5, 5));
        TitledBorder exportBorder = BorderFactory.createTitledBorder("Exportar");
        mainContentPanel.setBorder(exportBorder);
        projectController.getController().getGeneralController().getRegistry().register("panel.exportacion.container", mainContentPanel);

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
        Action toggleDetailsAction = projectController.getController().getActionMap().get(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION);
        assignedFilesColumn.setCellRenderer(new MultiLineCellRenderer(toggleDetailsAction));
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
        
        projectController.getRegistry().register("panel.exportacion.detalles", detailPanel);
        
        detailPanel.setPreferredSize(new java.awt.Dimension(0, 120));
        detailPanel.setMinimumSize(new java.awt.Dimension(0, 120));
        
        Action addAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE);
        Action removeAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE);
        Action locateAction = projectController.getController().getActionFactory().getActionMap().get(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE);
        
        detailPanel.setActions(addAction, removeAction, locateAction);
        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, detailPanel);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(1.0);
        
        mainContentPanel.add(splitPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(10, 0));
        southPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
        this.lblResumen = new JLabel("Cargue la selección para ver el estado.");
        southPanel.add(this.lblResumen, BorderLayout.WEST);

        this.txtCarpetaDestino = (JTextField) projectController.getController().getGeneralController().getRegistry().get("textfield.export.destino");
        if (this.txtCarpetaDestino != null) {
            this.originalTextFieldBorder = this.txtCarpetaDestino.getBorder();
            southPanel.add(this.txtCarpetaDestino, BorderLayout.CENTER);
        }
        
        mainContentPanel.add(southPanel, BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);
        
    } // ---FIN de metodo [initComponents]---
    
    
    public void setupHighlightingListener() {
        // Si ya está configurado, no hacemos nada más.
        if (highlightingListenerConfigured) {
            return;
        }
        
        ComponentRegistry registry = projectController.getController().getComponentRegistry();
        final javax.swing.AbstractButton detailsButton = registry.get("interfaz.boton.acciones_exportacion.export_detalles_seleccion");
        
        if (detailsButton == null) {
            logger.error("CRITICAL: El botón de detalles no se encontró en el registro. El resaltado no funcionará.");
            return;
        }

        // 1. Guardamos el borde que el tema le ha puesto al botón.
        final javax.swing.border.Border normalBorder = detailsButton.getBorder();
        
        // 2. Obtenemos el color de acento del tema. Es el color naranja/azul que usan los componentes con foco.
        java.awt.Color accentColor = javax.swing.UIManager.getColor("Component.accentColor");
        if (accentColor == null) {
            // Si por alguna razón el tema no lo define, usamos un azul brillante como respaldo.
            accentColor = new java.awt.Color(50, 150, 255); 
        }
        
        // 3. Creamos nuestro borde de realce. 2 píxeles de grosor.
        final javax.swing.border.Border highlightBorder = javax.swing.BorderFactory.createLineBorder(accentColor, 2);
        
        tablaExportacion.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tablaExportacion.getSelectedRow();
                ExportItem selectedItem = (selectedRow != -1) ? tableModel.getItemAt(selectedRow) : null;
                
                boolean shouldHighlight = (selectedItem != null 
                                           && selectedItem.getRutasArchivosAsociados() != null 
                                           && selectedItem.getRutasArchivosAsociados().size() > 1);
                
                // --- APLICACIÓN DEL PLAN B ---
                // Simplemente cambiamos el objeto Border del botón.
                detailsButton.setBorder(shouldHighlight ? highlightBorder : normalBorder);
            }
        });
        
        highlightingListenerConfigured = true;
        logger.debug("[ExportPanel] Listener de resaltado (con setBorder) configurado CORRECTAMENTE.");
    } // ---FIN de metodo [setupHighlightingListener]---
    
    
    public void actualizarTituloExportacion(int seleccionados, int total) {
        ComponentRegistry registry = projectController.getController().getGeneralController().getRegistry();
        JPanel panelExportacion = registry.get("panel.exportacion.container");
        
        if (panelExportacion != null && panelExportacion.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panelExportacion.getBorder();
            String nuevoTitulo = String.format("Exportar (%d/%d)", seleccionados, total);
            border.setTitle(nuevoTitulo);
            panelExportacion.repaint();
        }
    } // FIN del metodo actualizarTituloExportacion
    
    
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



