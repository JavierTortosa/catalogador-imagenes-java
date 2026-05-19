package vista.dialogos;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.datos.Tag;
import servicios.db.TagDAO;
import vista.util.IconUtils;

/**
 * Diálogo modal avanzado y premium para buscar, ordenar y seleccionar 
 * etiquetas (tags) de la biblioteca desde la base de datos local.
 * Utiliza botones integrados para cambiar de vista (Árbol/Lista) y ciclar ordenaciones.
 */
public class TagSelectionDialog extends JDialog {
    private static final long serialVersionUID = 2L;
    private static final Logger logger = LoggerFactory.getLogger(TagSelectionDialog.class);

    // Componentes visuales
    private JTextField txtBuscar;
    private JToggleButton btnVistaCarpeta;
    private JButton btnOrden;
    
    private JTree treeTags;
    private JList<Tag> listTags;
    private CardLayout cardLayout;
    private JPanel containerPanel;
    private JButton btnAceptar;
    private JButton btnCancelar;

    // Controladores de Iconos
    private final IconUtils iconUtils;

    // Estado de datos
    private final TagDAO tagDAO;
    private List<Tag> allTags;
    private Map<Long, Tag> tagMap;
    private String tagSeleccionado = null;

    // Estado de ordenación: 0 = A-Z, 1 = Z-A, 2 = Sin ordenar (db default)
    private int sortState = 0;

    public TagSelectionDialog(JDialog owner, IconUtils iconUtils) {
        super(owner, "Seleccionar Etiqueta de la Biblioteca", true);
        this.tagDAO = new TagDAO();
        this.tagMap = new HashMap<>();
        this.iconUtils = iconUtils;

        // Inicializar ventana
        setSize(480, 550);
        setMinimumSize(new Dimension(400, 450));
        setLocationRelativeTo(owner);

        // Cargar tags iniciales
        cargarTagsDesdeBD();

        // Inicializar componentes e interfaz
        initUI();

        // Configurar atajos de teclado globales (Escape para salir)
        configurarAtajosGlobales();
    } // --- FIN de constructor TagSelectionDialog ---

    private void cargarTagsDesdeBD() {
        try {
            this.allTags = tagDAO.getAllTags();
            if (this.allTags == null) {
                this.allTags = new ArrayList<>();
            }
            // Indexar en mapa rápido
            for (Tag t : this.allTags) {
                this.tagMap.put(t.getId(), t);
            }
            logger.debug("[TagSelectionDialog] Cargadas {} etiquetas de la BD.", this.allTags.size());
        } catch (Exception e) {
            logger.error("Error al cargar tags para el diálogo de selección", e);
            this.allTags = new ArrayList<>();
        }
    } // --- FIN de metodo cargarTagsDesdeBD ---

    private void initUI() {
        setLayout(new BorderLayout());

        // --- PANEL DE CONTROL SUPERIOR (Búsqueda y Botones de Vista/Orden) ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        txtBuscar = new JTextField();
        txtBuscar.setToolTipText("Escribe para filtrar etiquetas al instante");
        topPanel.add(txtBuscar, BorderLayout.CENTER);

        // Subpanel de botones (Carpeta/Filtro y Ordenación)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        btnVistaCarpeta = new JToggleButton();
        btnVistaCarpeta.setToolTipText("Alternar Vista de Árbol Jerárquico (Carpeta ON) / Lista Plana (OFF)");
        btnVistaCarpeta.setSelected(true); // Por defecto árbol (ON)

        // Cargar icono de carpeta si está disponible
        ImageIcon folderIcon = null;
        if (iconUtils != null) {
            folderIcon = iconUtils.getScaledIcon("7001-subcarpetas_48x48.png", 20, 20);
        }
        if (folderIcon != null) {
            btnVistaCarpeta.setIcon(folderIcon);
        } else {
            btnVistaCarpeta.setText("📁");
        }

        btnOrden = new JButton();
        actualizarIconoOrden();

        buttonPanel.add(btnVistaCarpeta);
        buttonPanel.add(btnOrden);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- PANEL CENTRAL CON CARDLAYOUT (JTree vs JList) ---
        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        containerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Vista 1: Árbol Jerárquico
        treeTags = new JTree();
        treeTags.setShowsRootHandles(true);
        treeTags.setRootVisible(false); // Ocultar nodo raíz virtual
        JScrollPane scrollTree = new JScrollPane(treeTags);
        containerPanel.add(scrollTree, "ARBOL");

        // Vista 2: Lista Plana
        listTags = new JList<>();
        listTags.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollList = new JScrollPane(listTags);
        containerPanel.add(scrollList, "LISTA");

        add(containerPanel, BorderLayout.CENTER);

        // --- PANEL INFERIOR (Aceptar / Cancelar) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        btnAceptar = new JButton("Seleccionar");
        btnAceptar.setToolTipText("Confirmar selección de la etiqueta");
        btnCancelar = new JButton("Cancelar");
        btnCancelar.setToolTipText("Cerrar sin guardar cambios");

        bottomPanel.add(btnAceptar);
        bottomPanel.add(btnCancelar);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- INICIALIZAR Y POBLAR VISTAS ---
        actualizarVistasFiltradas();

        // --- LÓGICA DE EVENTOS ---
        
        // Listener para búsqueda en tiempo real
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { actualizarVistasFiltradas(); }
            @Override
            public void removeUpdate(DocumentEvent e) { actualizarVistasFiltradas(); }
            @Override
            public void changedUpdate(DocumentEvent e) { actualizarVistasFiltradas(); }
        });

        // Alternar vista de Árbol / Lista
        btnVistaCarpeta.addActionListener(e -> {
            if (btnVistaCarpeta.isSelected()) {
                cardLayout.show(containerPanel, "ARBOL");
            } else {
                cardLayout.show(containerPanel, "LISTA");
            }
            actualizarVistasFiltradas();
        });

        // Ciclar ordenación
        btnOrden.addActionListener(e -> {
            sortState = (sortState + 1) % 3;
            actualizarIconoOrden();
            actualizarVistasFiltradas();
        });

        // Doble clic sobre árbol
        treeTags.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = treeTags.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();
                        if (userObject instanceof Tag) {
                            confirmarSeleccion((Tag) userObject);
                        }
                    }
                }
            }
        });

        // Doble clic sobre lista
        listTags.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Tag selected = listTags.getSelectedValue();
                    if (selected != null) {
                        confirmarSeleccion(selected);
                    }
                }
            }
        });

        // Acción botones inferiores
        btnAceptar.addActionListener(e -> {
            if (btnVistaCarpeta.isSelected()) { // Árbol
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeTags.getLastSelectedPathComponent();
                if (node != null && node.getUserObject() instanceof Tag) {
                    confirmarSeleccion((Tag) node.getUserObject());
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Selecciona una etiqueta del árbol.", "Aviso", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            } else { // Lista
                Tag selected = listTags.getSelectedValue();
                if (selected != null) {
                    confirmarSeleccion(selected);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Selecciona una etiqueta de la lista.", "Aviso", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        btnCancelar.addActionListener(e -> {
            tagSeleccionado = null;
            dispose();
        });
    } // --- FIN de metodo initUI ---

    private void actualizarIconoOrden() {
        ImageIcon icon = null;
        String tooltip = "";

        if (sortState == 0) {
            if (iconUtils != null) {
                icon = iconUtils.getScaledIcon("30004-orden_ascendente.png", 20, 20);
            }
            if (icon != null) {
                btnOrden.setIcon(icon);
                btnOrden.setText(null);
            } else {
                btnOrden.setIcon(null);
                btnOrden.setText("A-Z ↑");
            }
            tooltip = "Orden A-Z Ascendente";
        } else if (sortState == 1) {
            if (iconUtils != null) {
                icon = iconUtils.getScaledIcon("30005-orden_descendente.png", 20, 20);
            }
            if (icon != null) {
                btnOrden.setIcon(icon);
                btnOrden.setText(null);
            } else {
                btnOrden.setIcon(null);
                btnOrden.setText("Z-A ↓");
            }
            tooltip = "Orden Z-A Descendente";
        } else {
            if (iconUtils != null) {
                icon = iconUtils.getScaledIcon("30006-orden_off.png", 20, 20);
            }
            if (icon != null) {
                btnOrden.setIcon(icon);
                btnOrden.setText(null);
            } else {
                btnOrden.setIcon(null);
                btnOrden.setText("S/O");
            }
            tooltip = "Sin ordenar (orden por defecto)";
        }

        btnOrden.setToolTipText(tooltip);
    } // --- FIN de metodo actualizarIconoOrden ---

    private void actualizarVistasFiltradas() {
        String query = txtBuscar.getText().trim().toLowerCase();

        if (btnVistaCarpeta.isSelected()) {
            // POBLAR ÁRBOL JERÁRQUICO
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
            if (query.isEmpty()) {
                // Modo completo
                buildTreeModel(rootNode, this.allTags);
            } else {
                // Modo búsqueda inteligente: filtrar tags que coincidan y retener sus ancestros
                List<Tag> matching = this.allTags.stream()
                        .filter(t -> t.getNombre().toLowerCase().contains(query))
                        .collect(Collectors.toList());

                Set<Tag> toInclude = new HashSet<>();
                for (Tag tag : matching) {
                    toInclude.add(tag);
                    Long parentId = tag.getParentId();
                    while (parentId != null) {
                        Tag parent = this.tagMap.get(parentId);
                        if (parent != null) {
                            toInclude.add(parent);
                            parentId = parent.getParentId();
                        } else {
                            parentId = null;
                        }
                    }
                }
                buildTreeModel(rootNode, new ArrayList<>(toInclude));
            }

            treeTags.setModel(new DefaultTreeModel(rootNode));
            // Expandir todos los nodos por comodidad del usuario
            expandAllNodes(treeTags);

        } else {
            // POBLAR LISTA PLANA (Orden Ascendente, Descendente o Desordenado)
            List<Tag> listData = this.allTags.stream()
                    .filter(t -> query.isEmpty() || t.getNombre().toLowerCase().contains(query))
                    .collect(Collectors.toList());

            if (sortState == 0) {
                Collections.sort(listData, Comparator.comparing(Tag::getNombre));
            } else if (sortState == 1) {
                Collections.sort(listData, (t1, t2) -> t2.getNombre().compareTo(t1.getNombre()));
            }

            DefaultListModel<Tag> listModel = new DefaultListModel<>();
            for (Tag tag : listData) {
                listModel.addElement(tag);
            }
            listTags.setModel(listModel);
        }
    } // --- FIN de metodo actualizarVistasFiltradas ---

    private void buildTreeModel(DefaultMutableTreeNode rootNode, List<Tag> subset) {
        // Estructurar según jerarquía padre-hijo
        Map<Long, List<Tag>> childrenMap = new HashMap<>();
        List<Tag> roots = new ArrayList<>();
        for (Tag tag : subset) {
            if (tag.getParentId() == null) {
                roots.add(tag);
            } else {
                childrenMap.computeIfAbsent(tag.getParentId(), k -> new ArrayList<>()).add(tag);
            }
        }

        // Ordenar según sortState
        if (sortState == 0) {
            roots.sort(Comparator.comparing(Tag::getNombre));
            childrenMap.values().forEach(list -> list.sort(Comparator.comparing(Tag::getNombre)));
        } else if (sortState == 1) {
            roots.sort((t1, t2) -> t2.getNombre().compareTo(t1.getNombre()));
            childrenMap.values().forEach(list -> list.sort((t1, t2) -> t2.getNombre().compareTo(t1.getNombre())));
        }

        // Construir nodos recursivamente
        for (Tag rootTag : roots) {
            DefaultMutableTreeNode rootNodeItem = new DefaultMutableTreeNode(rootTag);
            rootNode.add(rootNodeItem);
            agregarHijosRecursivamente(rootNodeItem, rootTag.getId(), childrenMap);
        }
    } // --- FIN de metodo buildTreeModel ---

    private void agregarHijosRecursivamente(DefaultMutableTreeNode parentNode, long parentId, Map<Long, List<Tag>> childrenMap) {
        List<Tag> children = childrenMap.get(parentId);
        if (children != null) {
            for (Tag child : children) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                parentNode.add(childNode);
                agregarHijosRecursivamente(childNode, child.getId(), childrenMap);
            }
        }
    } // --- FIN de agregarHijosRecursivamente ---

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    } // --- FIN de metodo expandAllNodes ---

    private void confirmarSeleccion(Tag tag) {
        if (tag != null) {
            this.tagSeleccionado = tag.getNombre();
            logger.debug("[TagSelectionDialog] Tag seleccionado: '{}'", this.tagSeleccionado);
            dispose();
        }
    } // --- FIN de metodo confirmarSeleccion ---

    private void configurarAtajosGlobales() {
        // Asignar tecla ESCAPE
        getRootPane().registerKeyboardAction(e -> {
            this.tagSeleccionado = null;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Atajo para ENTER en el cuadro de búsqueda
        txtBuscar.registerKeyboardAction(e -> {
            if (btnVistaCarpeta.isSelected()) { // Árbol
                // Selecciona el primer nodo visible que no sea el root virtual
                if (treeTags.getRowCount() > 0) {
                    TreePath path = treeTags.getPathForRow(0);
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.getUserObject() instanceof Tag) {
                            confirmarSeleccion((Tag) node.getUserObject());
                        }
                    }
                }
            } else { // Lista
                if (listTags.getModel().getSize() > 0) {
                    Tag tag = listTags.getModel().getElementAt(0);
                    confirmarSeleccion(tag);
                }
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
    } // --- FIN de metodo configurarAtajosGlobales ---

    /**
     * Abre el diálogo de forma modal y espera la interacción del usuario.
     * @return El nombre de la etiqueta seleccionada, o null si canceló.
     */
    public String showDialog() {
        setVisible(true);
        return tagSeleccionado;
    } // --- FIN de metodo showDialog ---
}
