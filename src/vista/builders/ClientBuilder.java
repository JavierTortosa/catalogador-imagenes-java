package vista.builders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ClientController;
import controlador.GeneralController;
import controlador.managers.ToolbarManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import modelo.proyecto.ProjectModel;
import servicios.ProjectManager;
import vista.models.ClienteTableModel;
import vista.models.ProyectoClienteTableModel;
import vista.panels.ClientReviewPanel;
import vista.panels.GridDisplayPanel;
import vista.panels.CheckboxEditorPanel;
import vista.panels.ImageDisplayPanel;
import vista.panels.PolaroidDisplayPanel;
import vista.renderers.ActionCellRenderer;
import vista.renderers.CodeCellRenderer;
import vista.renderers.CommentCellRenderer;
import vista.renderers.TristateCellRenderer;
import vista.theme.ThemeManager;

/**
 * Constructor del panel principal del Modo Cliente.
 * Organiza las tablas de proyecto (izquierda), visor con editor (centro)
 * y tablas de cliente (derecha) en un JSplitPane jerárquico,
 * además de registrar todos los componentes en el ComponentRegistry.
 */
public class ClientBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClientBuilder.class);

    private final ComponentRegistry registry;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final GeneralController generalController;
    private final ToolbarManager toolbarManager;
    private final ClientController clientController;
    private final ProjectManager projectManager;

    public ClientBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager,
                         GeneralController generalController, ToolbarManager toolbarManager,
                         ClientController clientController, ProjectManager projectManager) {

        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ClientBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ClientBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null");
        this.generalController = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.toolbarManager = Objects.requireNonNull(toolbarManager, "ToolbarManager no puede ser null");
        this.clientController = Objects.requireNonNull(clientController, "ClientController no puede ser null");
        this.projectManager = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
    } // --- FIN de constructor ClientBuilder ---


    public JPanel buildClientViewPanel() {
        logger.info("[ClientBuilder] Construyendo el panel del Modo Cliente...");

        JPanel panelClienteRaiz = new JPanel(new BorderLayout());
        registry.register("view.panel.cliente", panelClienteRaiz);

        JSplitPane leftPanel = createProjectTablesPanel();
        JSplitPane rightPanel = createClientTablesPanel();

        JPanel displayPlaceholder = new JPanel(new CardLayout());
        registry.register("container.displaymodes.cliente", displayPlaceholder);
        registry.register("placeholder.display.cliente", displayPlaceholder);

        ImageDisplayPanel sharedImagePanel = registry.get("panel.display.imagen");
        GridDisplayPanel sharedGridPanel = registry.get("panel.display.grid");
        PolaroidDisplayPanel sharedPolaroidPanel = registry.get("panel.display.polaroid");

        if (sharedImagePanel != null) {
            registry.register("panel.cliente.display", sharedImagePanel);
            registry.register("label.cliente.imagen", sharedImagePanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        }
        if (sharedGridPanel != null) {
            registry.register("panel.display.grid.cliente", sharedGridPanel);
            registry.register("list.grid.cliente", sharedGridPanel.getGridList(), "WHEEL_NAVIGABLE");
        }
        if (sharedPolaroidPanel != null) {
            registry.register("panel.cliente.display.polaroid", sharedPolaroidPanel);
            registry.register("panel.cliente.display.polaroid.image", sharedPolaroidPanel.getImagePanel());
            registry.register("label.cliente.polaroid.imagen", sharedPolaroidPanel.getInternalLabel(), "WHEEL_NAVIGABLE");
        }

        JPanel displayWrapper = new JPanel(new CardLayout());
        registry.register("container.displaymodes.cliente.wrapper", displayWrapper);
        displayWrapper.add(displayPlaceholder, "DISPLAY_NORMAL");

        ClientReviewPanel reviewPanel = new ClientReviewPanel(themeManager, model, projectManager, registry);
        registry.register("panel.cliente.review", reviewPanel);
        displayWrapper.add(reviewPanel, "DISPLAY_REVIEW");

        ((CardLayout) displayWrapper.getLayout()).show(displayWrapper, "DISPLAY_NORMAL");

        JToolBar editorToolbar = toolbarManager.getToolbar("editor_checkboxes");
        CheckboxEditorPanel checkboxEditorPanel = new CheckboxEditorPanel(themeManager, model, projectManager, registry, editorToolbar);
        registry.register("panel.cliente.editor", checkboxEditorPanel);
        java.awt.Window mainWindow = registry.get("frame.main");
        javax.swing.JDialog editorDialog = new javax.swing.JDialog(
                mainWindow instanceof java.awt.Frame ? (java.awt.Frame) mainWindow : null,
                "Editor de Checkboxes", false);
        editorDialog.setDefaultCloseOperation(javax.swing.JDialog.HIDE_ON_CLOSE);
        editorDialog.setContentPane(checkboxEditorPanel);
        editorDialog.setSize(900, 700);
        editorDialog.setLocationRelativeTo(mainWindow);
        editorDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                model.setClienteEditorPanelVisible(false);
            }
        });
        registry.register("dialog.cliente.editor", editorDialog);

        JPanel centerPanel = createCenterPanel(displayWrapper);

        JSplitPane centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        centerRightSplit.setResizeWeight(0.625);
        centerRightSplit.setContinuousLayout(true);
        centerRightSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerRightSplit);
        mainSplit.setResizeWeight(0.2);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        registry.register("splitpane.cliente.main", mainSplit);

        panelClienteRaiz.add(mainSplit, BorderLayout.CENTER);

        return panelClienteRaiz;
    } // --- FIN de metodo buildClientViewPanel ---


    private JSplitPane createProjectTablesPanel() {
        JPanel panelSeleccion = createProjectTable("Selecci\u00f3n Actual (Proyecto)", "table.cliente.proyecto.seleccion", true);
        JPanel panelDescartes = createProjectTable("Descartes (Proyecto)", "table.cliente.proyecto.descartes", false);

        panelDescartes.setMinimumSize(new java.awt.Dimension(100, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(100, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createProjectTablesPanel ---


    private JSplitPane createClientTablesPanel() {
        JPanel panelSeleccion = createClientTable("Selecci\u00f3n (Cliente)", "table.cliente.cliente.seleccion", true);
        JPanel panelDescartes = createClientTable("Descartes (Cliente)", "table.cliente.cliente.descartes", false);

        panelDescartes.setMinimumSize(new java.awt.Dimension(150, 100));
        panelSeleccion.setMinimumSize(new java.awt.Dimension(150, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSeleccion, panelDescartes);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        return split;
    } // --- FIN de metodo createClientTablesPanel ---


    private JPanel createProjectTable(String title, String tableName, boolean isSeleccion) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        ProjectModel project = projectManager.getCurrentProject();
        ProyectoClienteTableModel tableModel = new ProyectoClienteTableModel(project, isSeleccion);
        JTable table = new JTable(tableModel);
        table.setName(tableName);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO)
                .setCellRenderer(new CodeCellRenderer());
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ACCIONES)
                .setCellRenderer(new ActionCellRenderer(isSeleccion));

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_CODIGO).setMaxWidth(40);
        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_ACCIONES).setMaxWidth(70);

        table.getColumnModel().getColumn(ProyectoClienteTableModel.COL_NOMBRE).setMinWidth(320);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ProyectoClienteTableModel model = (ProyectoClienteTableModel) table.getModel();
                if (col == ProyectoClienteTableModel.COL_ACCIONES) {
                    String key = model.getImageKey(row);
                    String code = model.getValueAt(row, ProyectoClienteTableModel.COL_CODIGO).toString();
                    String nuevo = JOptionPane.showInputDialog(table,
                            "Editar c\u00f3digo de cat\u00e1logo:", code);
                    if (nuevo != null && !nuevo.trim().isEmpty()) {
                        projectManager.getCurrentProject().getImageCodes().put(key, nuevo.trim());
                        model.refrescar();
                    }
                } else {
                    String key = model.getImageKey(row);
                    if (key != null && clientController.getVisorController() != null) {
                        clientController.getVisorController()
                                .actualizarImagenPrincipalPorPath(Paths.get(key), key);
                    }
                }
            }
        });

        table.addMouseWheelListener(e -> {
            ProyectoClienteTableModel model = (ProyectoClienteTableModel) table.getModel();
            int total = model.getRowCount();
            if (total == 0) return;
            int row = table.getSelectedRow();
            int newRow;
            if (e.getWheelRotation() < 0) {
                newRow = (row <= 0) ? 0 : row - 1;
            } else {
                newRow = (row < 0) ? 0 : Math.min(total - 1, row + 1);
            }
            if (newRow != row) {
                table.setRowSelectionInterval(newRow, newRow);
                table.scrollRectToVisible(table.getCellRect(newRow, 0, true));
                String key = model.getImageKey(newRow);
                if (key != null && clientController.getVisorController() != null) {
                    clientController.getVisorController()
                            .actualizarImagenPrincipalPorPath(Paths.get(key), key);
                }
            }
            e.consume();
        });

        // Navegación con teclado (flechas arriba/abajo)
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            ProyectoClienteTableModel m = (ProyectoClienteTableModel) table.getModel();
            String key = m.getImageKey(row);
            if (key != null && clientController.getVisorController() != null) {
                clientController.getVisorController()
                        .actualizarImagenPrincipalPorPath(Paths.get(key), key);
            }
        });

        // Popup menu para tabla de proyecto (selección y descartes)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
                String key = ((ProyectoClienteTableModel) table.getModel()).getImageKey(table.getSelectedRow());
                if (key == null) return;
                JPopupMenu popup = new JPopupMenu();
                if (isSeleccion) {
                    JMenuItem moverADescartes = new JMenuItem("Mover a descartes");
                    moverADescartes.addActionListener(ev -> {
                        clientController.moverADescartesCliente(key);
                    });
                    popup.add(moverADescartes);
                } else {
                    JMenuItem moverASeleccion = new JMenuItem("Mover a selección");
                    moverASeleccion.addActionListener(ev -> {
                        clientController.restaurarDeDescartesCliente(key);
                    });
                    popup.add(moverASeleccion);
                    popup.add(new JPopupMenu.Separator());
                    JMenuItem borrar = new JMenuItem("Borrar imagen");
                    borrar.addActionListener(ev -> {
                        int confirm = JOptionPane.showConfirmDialog(table,
                                "¿Seguro que quieres eliminar esta imagen del proyecto?\n"
                                + "(No se borrará el archivo del disco)",
                                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            projectManager.eliminarDeProyecto(Paths.get(key));
                            ((ProyectoClienteTableModel) table.getModel()).refrescar();
                        }
                    });
                    popup.add(borrar);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        registry.register(tableName, table);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    } // --- FIN de metodo createProjectTable ---


    private JPanel createClientTable(String title, String tableName, boolean isSeleccion) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        ProjectModel project = projectManager.getCurrentProject();
        ClienteTableModel tableModel = new ClienteTableModel(project, isSeleccion);
        JTable table = new JTable(tableModel);
        table.setName(tableName);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        TableColumn estadoCol = table.getColumnModel().getColumn(ClienteTableModel.COL_ESTADO);
        estadoCol.setCellRenderer(new TristateCellRenderer());
        estadoCol.setMaxWidth(40);

        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG).setMaxWidth(40);
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_IMG)
                .setCellRenderer(new CodeCellRenderer());

        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_CB).setMaxWidth(40);
        table.getColumnModel().getColumn(ClienteTableModel.COL_CODIGO_CB)
                .setCellRenderer(new DefaultTableCellRenderer() {
                    private static final long serialVersionUID = 1L;
                    private final Color codeColor = new Color(70, 130, 180);
                    @Override
                    public Component getTableCellRendererComponent(JTable t, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        JLabel label = (JLabel) super.getTableCellRendererComponent(
                                t, value, isSelected, hasFocus, row, column);
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                        if (!isSelected) {
                            label.setForeground(codeColor);
                        }
                        label.setHorizontalAlignment(JLabel.CENTER);
                        ClienteTableModel m = (ClienteTableModel) t.getModel();
                        if (m.isChildRow(row)) {
                            label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
                        } else {
                            label.setBorder(null);
                        }
                        return label;
                    }
                });

        TableColumn commentCol = table.getColumnModel().getColumn(ClienteTableModel.COL_COMENTARIO);
        commentCol.setCellRenderer(new CommentCellRenderer());

        table.getColumnModel().getColumn(ClienteTableModel.COL_NOMBRE).setMinWidth(330);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                ClienteTableModel model = (ClienteTableModel) table.getModel();
                String key = model.getImageKey(row);

                if (col == ClienteTableModel.COL_COMENTARIO && e.getClickCount() >= 2) {
                    ProjectModel currentProject = projectManager.getCurrentProject();
                    String actual = currentProject != null && currentProject.hasClientSelection()
                            ? currentProject.getClientSelection().getComments().getOrDefault(key, "")
                            : "";
                    String nuevo = JOptionPane.showInputDialog(table,
                            "Comentario para esta imagen:", actual);
                    if (nuevo != null && currentProject != null) {
                        currentProject.getClientSelection().getComments().put(key, nuevo);
                        model.refrescar();
                    }
                } else if (col != ClienteTableModel.COL_ESTADO && col != ClienteTableModel.COL_COMENTARIO) {
                    if (key != null && clientController.getVisorController() != null) {
                        ProjectModel curProject = projectManager.getCurrentProject();
                        String resolvedPath = curProject != null ? curProject.resolverClaveImagenCanonica(key) : null;
                        if (resolvedPath != null) {
                            String cbCode = null;
                            int sep = key.lastIndexOf('_');
                            if (sep > 0 && key.substring(sep + 1).startsWith("cb")) {
                                cbCode = key.substring(sep + 1);
                            }
                            ClientBuilder.this.model.setSelectedCheckboxCode(cbCode);
                            clientController.getVisorController()
                                    .actualizarImagenPrincipalPorPath(Paths.get(resolvedPath), resolvedPath);
                        }
                    }
                }
            }
        });

        table.addMouseWheelListener(e -> {
            ClienteTableModel model = (ClienteTableModel) table.getModel();
            int total = model.getRowCount();
            if (total == 0) return;
            int row = table.getSelectedRow();
            int newRow;
            if (e.getWheelRotation() < 0) {
                newRow = (row <= 0) ? 0 : row - 1;
            } else {
                newRow = (row < 0) ? 0 : Math.min(total - 1, row + 1);
            }
            if (newRow != row) {
                table.setRowSelectionInterval(newRow, newRow);
                table.scrollRectToVisible(table.getCellRect(newRow, 0, true));
                String key = model.getImageKey(newRow);
                if (key != null && clientController.getVisorController() != null) {
                    ProjectModel curProject = projectManager.getCurrentProject();
                    String resolvedPath = curProject != null ? curProject.resolverClaveImagenCanonica(key) : null;
                    if (resolvedPath != null) {
                        String cbCode = null;
                        int sep = key.lastIndexOf('_');
                        if (sep > 0 && key.substring(sep + 1).startsWith("cb")) {
                            cbCode = key.substring(sep + 1);
                        }
                        ClientBuilder.this.model.setSelectedCheckboxCode(cbCode);
                        clientController.getVisorController()
                                .actualizarImagenPrincipalPorPath(Paths.get(resolvedPath), resolvedPath);
                    }
                }
            }
            e.consume();
        });

        // Navegación con teclado (flechas arriba/abajo)
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0) return;
            ClienteTableModel m = (ClienteTableModel) table.getModel();
            String key = m.getImageKey(row);
            if (key != null && clientController.getVisorController() != null) {
                ProjectModel curProject = projectManager.getCurrentProject();
                String resolvedPath = curProject != null ? curProject.resolverClaveImagenCanonica(key) : null;
                if (resolvedPath != null) {
                    String cbCode = null;
                    int sep = key.lastIndexOf('_');
                    if (sep > 0 && key.substring(sep + 1).startsWith("cb")) {
                        cbCode = key.substring(sep + 1);
                    }
                    ClientBuilder.this.model.setSelectedCheckboxCode(cbCode);
                    clientController.getVisorController()
                            .actualizarImagenPrincipalPorPath(Paths.get(resolvedPath), resolvedPath);
                }
            }
        });

        // Popup menu para tabla de cliente (selección y descartes)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
                ClienteTableModel m = (ClienteTableModel) table.getModel();
                String key = m.getImageKey(table.getSelectedRow());
                if (key == null) return;
                JPopupMenu popup = new JPopupMenu();
                if (isSeleccion) {
                    JMenuItem moverADescartes = new JMenuItem("Mover a descartes");
                    moverADescartes.addActionListener(ev -> {
                        if (projectManager.getCurrentProject() != null
                                && projectManager.getCurrentProject().hasClientSelection()) {
                            clientController.moverADescartesCliente(key);
                            m.refrescar();
                        }
                    });
                    popup.add(moverADescartes);
                } else {
                    JMenuItem moverASeleccion = new JMenuItem("Mover a selección");
                    moverASeleccion.addActionListener(ev -> {
                        if (projectManager.getCurrentProject() != null
                                && projectManager.getCurrentProject().hasClientSelection()) {
                            clientController.restaurarDeDescartesCliente(key);
                            m.refrescar();
                        }
                    });
                    popup.add(moverASeleccion);
                    popup.add(new JPopupMenu.Separator());
                    JMenuItem borrar = new JMenuItem("Borrar imagen");
                    borrar.addActionListener(ev -> {
                        if (key != null && projectManager.getCurrentProject() != null
                                && projectManager.getCurrentProject().hasClientSelection()) {
                            int confirm = JOptionPane.showConfirmDialog(table,
                                    "¿Seguro que quieres eliminar esta imagen de la selección del cliente?",
                                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (confirm == JOptionPane.YES_OPTION) {
                                projectManager.getCurrentProject().getClientSelection().getImages().remove(key);
                                projectManager.notificarModificacion();
                                m.refrescar();
                            }
                        }
                    });
                    popup.add(borrar);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        registry.register(tableName, table);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    } // --- FIN de metodo createClientTable ---


    private JPanel createCenterPanel(JPanel displayModesContainer) {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Visor (Comparativa)"));

        if (displayModesContainer != null) {
            centerPanel.add(displayModesContainer, BorderLayout.CENTER);
        }

        JPanel commentsPanel = new JPanel(new BorderLayout());
        commentsPanel.setBorder(BorderFactory.createTitledBorder("Comentarios del Cliente"));
        javax.swing.JTextArea txtComments = new javax.swing.JTextArea(3, 20);
        txtComments.setEditable(false);
        txtComments.setLineWrap(true);
        txtComments.setWrapStyleWord(true);
        registry.register("textarea.cliente.comentarios", txtComments);
        commentsPanel.add(new JScrollPane(txtComments), BorderLayout.CENTER);

        centerPanel.add(commentsPanel, BorderLayout.SOUTH);

        return centerPanel;
    } // --- FIN de metodo createCenterPanel ---

} // --- FIN de clase ClientBuilder ---
