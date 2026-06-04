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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.commands.AppActionCommands;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.proyecto.ExportItem;

public class ExportPanel extends JPanel implements vista.theme.ThemeChangeListener {

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
    private JLabel lblTotalSize;
    
    private PdfDetailsTablePanel pdfDetailsTablePanel;
    private boolean pdfTableVisible = false;
    
    private boolean highlightingListenerConfigured = false;
    
    
    private int lastDividerLocation = -1; // Para recordar la posición del divisor
    private boolean isDetailsPanelVisible = false; // Para saber el estado actual
    
    private JTextField txtProjectName;
    private JTextArea areaProjectDescription;
    
    private javax.swing.JToggleButton btnMoveCopy;
    
    public ExportPanel(ProjectController controller, java.util.function.Consumer<javax.swing.event.TableModelEvent> tableChangedCallback) {
        super(new BorderLayout(5, 5));
        this.projectController = controller;
        
        this.tableModel = new ExportTableModel(tableChangedCallback);
        
        initComponents();
        setupHighlightingListener();
        
        if (projectController != null && projectController.getGeneralController() != null &&
            projectController.getGeneralController().getVisorController() != null) {
            projectController.getGeneralController().getVisorController().getThemeManager().addThemeChangeListener(this);
        }
        
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

        ToolbarManager toolbarManager = projectController.getGeneralController().getToolbarManager(); 
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
        projectController.getGeneralController().getRegistry().register("panel.exportacion.container", mainContentPanel);

        tablaExportacion = new JTable(this.tableModel); 
        tablaExportacion.setFillsViewportHeight(true);
        tablaExportacion.setShowGrid(true);
        tablaExportacion.setGridColor(UIManager.getColor("Component.borderColor"));
        
        // tamaño columnas panel exportar
        
        //checkbox
        TableColumn checkColumn = tablaExportacion.getColumnModel().getColumn(0);
        checkColumn.setPreferredWidth(30);
        checkColumn.setMaxWidth(30);
        tablaExportacion.getTableHeader().getColumnModel().getColumn(0).setHeaderRenderer(new CheckHeaderRenderer());

        //codigo
        TableColumn codeColumn = tablaExportacion.getColumnModel().getColumn(1);
        codeColumn.setPreferredWidth(60);
        codeColumn.setMaxWidth(70);

        //nombre imagen
        TableColumn imageColumn = tablaExportacion.getColumnModel().getColumn(2);
        imageColumn.setPreferredWidth(550);
        
        //status
        TableColumn statusColumn = tablaExportacion.getColumnModel().getColumn(3);
        statusColumn.setPreferredWidth(120);
        statusColumn.setMaxWidth(120);
        statusColumn.setCellRenderer(new vista.panels.export.StatusCellRenderer(projectController.getGeneralController().getVisorController().getIconUtils()));
        
        //archivos asignados
        TableColumn assignedFilesColumn = tablaExportacion.getColumnModel().getColumn(4);
        Action toggleDetailsAction = projectController.getActionMap().get(AppActionCommands.CMD_EXPORT_DETALLES_SELECCION);
        assignedFilesColumn.setCellRenderer(new MultiLineCellRenderer(toggleDetailsAction));
        assignedFilesColumn.setPreferredWidth(500);
        
        tablaExportacion.getTableHeader().addMouseListener(new HeaderMouseListener(tablaExportacion));
        
        TableRowSorter<ExportTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(3, (ExportItem a, ExportItem b) -> {
            String sA = a.tieneConflictoDeNombre() ? modelo.proyecto.ExportStatus.NOMBRE_DUPLICADO.getDisplay()
                    : a.getEstadoArchivoComprimido().getDisplay();
            String sB = b.tieneConflictoDeNombre() ? modelo.proyecto.ExportStatus.NOMBRE_DUPLICADO.getDisplay()
                    : b.getEstadoArchivoComprimido().getDisplay();
            return sA.compareTo(sB);
        });
        sorter.setComparator(5, (String a, String b) -> Long.compare(parseFileSize(a), parseFileSize(b)));
        sorter.setSortable(0, false);
        sorter.setSortable(1, false);
        sorter.setSortable(4, false);
        tablaExportacion.setRowSorter(sorter);
        
        tablaExportacion.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tablaExportacion.getSelectedRow();
                int modelRow = selectedRow != -1 ? tablaExportacion.convertRowIndexToModel(selectedRow) : -1;
                ExportItem selectedItem = (modelRow != -1) ? tableModel.getItemAt(modelRow) : null;
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
        
        Action addAction = projectController.getActionMap().get(AppActionCommands.CMD_EXPORT_ADD_ASSOCIATED_FILE);
        Action removeAction = projectController.getActionMap().get(AppActionCommands.CMD_EXPORT_DEL_ASSOCIATED_FILE);
        Action locateAction = projectController.getActionMap().get(AppActionCommands.CMD_EXPORT_LOCATE_ASSOCIATED_FILE);
        
        detailPanel.setActions(addAction, removeAction, locateAction);
        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, detailPanel);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(1.0);
        
        pdfDetailsTablePanel = new PdfDetailsTablePanel();
        // Le damos una altura por defecto de 200 píxeles para que se vea bien al abrirse
        pdfDetailsTablePanel.setPreferredSize(new java.awt.Dimension(0, 200)); 
        pdfDetailsTablePanel.setVisible(false);
        projectController.getRegistry().register("panel.exportacion.pdfdetalles.tabla", pdfDetailsTablePanel);
        
        // Enganchar el panel a la sincronización global del proyecto (lo que vimos antes)
        pdfDetailsTablePanel.setOnDataChangedListener(() -> {
            if (projectController != null) {
                projectController.notificarCambioEnProyecto();
            }
        });

        // Al seleccionar una fila en la tabla de detalles PDF, mostrar esa imagen en el visor
        pdfDetailsTablePanel.setOnSelectionChangedListener(exportItem -> {
            if (projectController != null) {
                projectController.mostrarImagenDeExportacion(
                        exportItem != null ? exportItem.getRutaImagen() : null);
            }
        });
        
        JPanel centerContentPanel = new JPanel(new BorderLayout());
        centerContentPanel.add(splitPane, BorderLayout.CENTER);
        centerContentPanel.add(pdfDetailsTablePanel, BorderLayout.SOUTH);
        
        mainContentPanel.add(centerContentPanel, BorderLayout.CENTER);

        
        
        
        
        
        JPanel southPanel = new JPanel(new BorderLayout(10, 0));
        southPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // 1. Panel de Destino (irá en el centro)
        JPanel destinationPanel = new JPanel(new BorderLayout(5, 0));
        
        // 1a. La nueva etiqueta para el campo de texto
        JLabel lblCarpetaDestino = new JLabel("Carpeta de Destino:");
        lblCarpetaDestino.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5)); // Margen derecho
        destinationPanel.add(lblCarpetaDestino, BorderLayout.WEST);

        // 1b. El campo de texto en sí
        this.txtCarpetaDestino = (JTextField) projectController.getGeneralController().getRegistry().get("textfield.export.destino");
        if (this.txtCarpetaDestino != null) {
            this.originalTextFieldBorder = this.txtCarpetaDestino.getBorder();
            destinationPanel.add(this.txtCarpetaDestino, BorderLayout.CENTER);
        }
        
        // 1c. El botón del selector de carpetas
        javax.swing.Action selectFolderAction = projectController.getActionMap().get("cmd.export.seleccionar.carpeta");
        if (selectFolderAction != null) {
            javax.swing.JButton btnSelectFolder = new javax.swing.JButton(selectFolderAction);
            btnSelectFolder.setText(""); // Ocultamos el texto para que solo se vea el icono
            btnSelectFolder.setToolTipText("Seleccionar Carpeta de Destino");
            btnSelectFolder.setFocusPainted(false);
            destinationPanel.add(btnSelectFolder, java.awt.BorderLayout.EAST);
        }
        
        southPanel.add(destinationPanel, BorderLayout.CENTER); // Añadimos el panel completo al centro

        // 2. El label de resumen (ahora en el SUR)
        // Al ponerlo en SOUTH, ocupará todo el ancho inferior sin afectar al panel central.
        this.lblResumen = new JLabel("Cargue la selección para ver el estado.");
        southPanel.add(this.lblResumen, BorderLayout.SOUTH);
        
        // 3. El panel del Este con el tamaño total y botón de Mover/Copiar
        JPanel eastPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        eastPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        
        btnMoveCopy = new javax.swing.JToggleButton();
        btnMoveCopy.putClientProperty("JButton.buttonType", "toolBarButton");
        actualizarIconos();
        btnMoveCopy.setToolTipText("Activar para MOVER los archivos (Desactivado = Copiar)");
        btnMoveCopy.setFocusPainted(false);
        btnMoveCopy.setContentAreaFilled(false);
        btnMoveCopy.setOpaque(false);
        btnMoveCopy.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        btnMoveCopy.addItemListener(e -> {
            if (btnMoveCopy.isSelected()) {
                btnMoveCopy.setContentAreaFilled(true);
                btnMoveCopy.setOpaque(true);
                btnMoveCopy.setBackground(new Color(255, 0, 0)); // Rojo muy intenso para peligro
                btnMoveCopy.setToolTipText("MOVER archivos activo (se eliminarán del origen)");
            } else {
                btnMoveCopy.setContentAreaFilled(false);
                btnMoveCopy.setOpaque(false);
                btnMoveCopy.setBackground(null);
                btnMoveCopy.setToolTipText("COPIAR archivos activos (se conservarán en el origen)");
            }
        });
        eastPanel.add(btnMoveCopy);

        lblTotalSize = new JLabel();
        lblTotalSize.setHorizontalAlignment(JLabel.RIGHT);
        actualizarTamañoTotalExportacion();
        eastPanel.add(lblTotalSize);
        southPanel.add(eastPanel, BorderLayout.EAST);

        mainContentPanel.add(southPanel, BorderLayout.SOUTH);
        
        this.add(mainContentPanel, BorderLayout.CENTER);
        
    } // ---FIN de metodo [initComponents]---
    
    
    public void setupHighlightingListener() {
        // Si ya está configurado, no hacemos nada más.
        if (highlightingListenerConfigured) {
            return;
        }
        
        ComponentRegistry registry = projectController.getRegistry();
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
                int modelRow = selectedRow != -1 ? tablaExportacion.convertRowIndexToModel(selectedRow) : -1;
                ExportItem selectedItem = (modelRow != -1) ? tableModel.getItemAt(modelRow) : null;
                
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
    	ComponentRegistry registry = projectController.getGeneralController().getRegistry();
        JPanel panelExportacion = registry.get("panel.exportacion.container");
        
        if (panelExportacion != null && panelExportacion.getBorder() instanceof TitledBorder) {
            TitledBorder border = (TitledBorder) panelExportacion.getBorder();
            String nuevoTitulo = String.format("Exportar (%d/%d)", seleccionados, total);
            border.setTitle(nuevoTitulo);
            panelExportacion.repaint();
        }
    } // FIN del metodo actualizarTituloExportacion
    
    
    /**
     * Calcula el tamaño total de todos los items seleccionados para exportar y
     * actualiza el JLabel correspondiente.
     */
    public void actualizarTamañoTotalExportacion() {
        if (tableModel == null || lblTotalSize == null) {
            return;
        }
        
        long totalSize = 0;
        for (ExportItem item : tableModel.getCola()) {
            if (item.isSeleccionadoParaExportar()) {
                totalSize += item.getTotalSize();
            }
        }
        
        String formattedSize = utils.StringUtils.formatFileSize(totalSize);
        lblTotalSize.setText(String.format("Tamaño total: %s", formattedSize));
    } // ---FIN de metodo [actualizarTamañoTotalExportacion]---
    
    
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
        Action iniciarExportAction = projectController.getActionMap().get(AppActionCommands.CMD_INICIAR_EXPORTACION);
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
    
    
    public void togglePdfDetailsTableVisibility() {
    	
        pdfTableVisible = !pdfTableVisible;
        pdfDetailsTablePanel.setVisible(pdfTableVisible);
        
        if (pdfTableVisible) {
            pdfDetailsTablePanel.setItems(tableModel.getCola());
        }
        
        // Forzamos a Swing a recalcular los tamaños de los contenedores
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }
    
    public ExportDetailPanel getDetailPanel() {
        return this.detailPanel;
    } // ---FIN de metodo [getDetailPanel]---
    
    public PdfDetailsTablePanel getPdfDetailsTablePanel() {
        return this.pdfDetailsTablePanel;
    }
    
    public boolean isPdfDetailsTableVisible() {
        return this.pdfTableVisible;
    }

    public void stopExportTableEditing() {
        if (tablaExportacion != null && tablaExportacion.isEditing()) {
            tablaExportacion.getCellEditor().stopCellEditing();
        }
    }

    public boolean isMoveOperationActive() {
        return btnMoveCopy != null && btnMoveCopy.isSelected();
    }

    public void resetMoveOperation() {
        if (btnMoveCopy != null && btnMoveCopy.isSelected()) {
            btnMoveCopy.setSelected(false);
        }
    }

    private void actualizarIconos() {
        if (projectController == null || btnMoveCopy == null) return;
        vista.util.IconUtils iconUtils = projectController.getGeneralController().getVisorController().getIconUtils();
        if (iconUtils != null) {
            javax.swing.ImageIcon iconCopy = iconUtils.getScaledIcon("21012 copy files.png", 24, 24);
            javax.swing.ImageIcon iconMove = iconUtils.getScaledIcon("21011 move files.png", 24, 24);
            btnMoveCopy.setIcon(iconCopy);
            btnMoveCopy.setSelectedIcon(iconMove);
        }
    }

    @Override
    public void onThemeChanged(vista.theme.Tema nuevoTema) {
        SwingUtilities.invokeLater(() -> {
            actualizarIconos();
        });
    }

    private long parseFileSize(String size) {
        if (size == null || size.isEmpty() || "0 B".equals(size))
            return 0;
        try {
            String[] parts = size.split(" ");
            double value = Double.parseDouble(parts[0]);
            String unit = parts.length > 1 ? parts[1] : "B";
            switch (unit) {
                case "B":  return (long) value;
                case "KB": return (long) (value * 1024);
                case "MB": return (long) (value * 1024 * 1024);
                case "GB": return (long) (value * 1024 * 1024 * 1024);
                default:   return (long) value;
            }
        } catch (Exception e) {
            return 0;
        }
    }

} // --- FIN de clase [ExportPanel]---



