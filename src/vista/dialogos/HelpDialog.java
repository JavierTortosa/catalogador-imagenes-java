package vista.dialogos;

import java.awt.BorderLayout;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vista.builders.HelpBuilder;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarComponentDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;

public class HelpDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(HelpDialog.class);

    private JTree navigationTree;
    private JEditorPane contentPane;

    public HelpDialog(JFrame owner) {
        super(owner, "Ayuda del Visor de Imágenes", true);
        
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        contentPane = new JEditorPane();
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);

        navigationTree = new JTree();
        navigationTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        DefaultMutableTreeNode topNode = createNavigationNodes();
        navigationTree.setModel(new DefaultTreeModel(topNode));

        JScrollPane treeScrollPane = new JScrollPane(navigationTree);
        JScrollPane contentScrollPane = new JScrollPane(contentPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, contentScrollPane);
        splitPane.setDividerLocation(250);

        add(splitPane, BorderLayout.CENTER);

        navigationTree.addTreeSelectionListener(this::treeSelectionChanged);

        navigationTree.setSelectionRow(0);
    } // --- Fin del método HelpDialog (constructor) ---

    private DefaultMutableTreeNode createNavigationNodes() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(new HelpPageInfo("Ayuda", "main_welcome.html"));

        top.add(new DefaultMutableTreeNode(new HelpPageInfo("Bienvenida", "index.html")));

        DefaultMutableTreeNode visualizadorFolder = new DefaultMutableTreeNode("Modo Visualizador");
        visualizadorFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Introducción", "visualizador.html")));
        visualizadorFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Panel de Navegación", "visualizador_panel_izquierdo.html")));
        visualizadorFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Área Principal y Vistas", "visualizador_area_principal.html")));
        visualizadorFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Sistema de Zoom", "visualizador_zoom.html")));
        visualizadorFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Barras de Información", "visualizador_barras_info.html")));
        top.add(visualizadorFolder);
        
        DefaultMutableTreeNode proyectoFolder = new DefaultMutableTreeNode("Modo Proyecto");
        proyectoFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Introducción", "proyecto.html")));
        proyectoFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Controles y Pestañas", "proyecto_controles.html")));
        proyectoFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("El Panel de Exportación", "proyecto_exportar.html")));
        top.add(proyectoFolder);

        DefaultMutableTreeNode carruselFolder = new DefaultMutableTreeNode("Modo Carrusel");
        carruselFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Introducción", "carrusel.html")));
        carruselFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Controles", "carrusel_controles.html")));
        top.add(carruselFolder);
        
        DefaultMutableTreeNode uiRefFolder = new DefaultMutableTreeNode("Referencia de UI");
        uiRefFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Barras de Herramientas", "toolbars_autogen.html")));
        uiRefFolder.add(new DefaultMutableTreeNode(new HelpPageInfo("Barra de Menús", "menus_autogen.html")));
        top.add(uiRefFolder);

        top.add(new DefaultMutableTreeNode(new HelpPageInfo("Atajos de Teclado", "atajos.html")));

        return top;
    } // --- Fin del método createNavigationNodes ---

    private void treeSelectionChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) navigationTree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            return;
        }

        Object nodeInfo = selectedNode.getUserObject();
        if (nodeInfo instanceof HelpPageInfo) {
            HelpPageInfo pageInfo = (HelpPageInfo) nodeInfo;
            
            // --- INICIO DE LA MODIFICACIÓN: DETECTAR QUÉ BUILDER USAR ---
            switch (pageInfo.getHtmlFileName()) {
                case "toolbars_autogen.html":
                    loadGeneratedHelp(true); // true para toolbars
                    break;
                case "menus_autogen.html":
                    loadGeneratedHelp(false); // false para menús
                    break;
                default:
                    loadStaticHelpPage(pageInfo.getHtmlFileName());
                    break;
            }
            // --- FIN DE LA MODIFICACIÓN ---
        }
    } // --- Fin del método treeSelectionChanged ---

    private void loadGeneratedHelp(boolean forToolbars) {
        try {
            HelpBuilder builder = new HelpBuilder();
            String htmlContent;

            if (forToolbars) {
                htmlContent = builder.generateToolbarsHelpHtml();
            } else {
                htmlContent = builder.generateMenusHelpHtml();
            }
            
            htmlContent = replaceIconPlaceholders(htmlContent);
            
            contentPane.setText(htmlContent);
            SwingUtilities.invokeLater(() -> contentPane.setCaretPosition(0));
        } catch (Exception ex) {
            logger.error("Error al generar la página de ayuda dinámica", ex);
            contentPane.setText("<html><body><h1>Error</h1><p>No se pudo generar la ayuda de la UI.</p></body></html>");
        }
    } // --- Fin del método loadGeneratedHelp ---

    private void loadStaticHelpPage(String htmlFileName) {
        String resourcePath = "/help/" + htmlFileName;
        try {
            URL helpURL = getClass().getResource(resourcePath);
            if (helpURL != null) {
                URL baseUrl = getClass().getResource("/help/");
                
                java.io.InputStream stream = helpURL.openStream();
                String htmlContent = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                stream.close();

                if (baseUrl != null) {
                    String baseTag = "<base href=\"" + baseUrl.toExternalForm() + "\">";
                    htmlContent = htmlContent.replaceFirst("(?i)<head>", "<head>" + baseTag);
                }
                
                // Procesamos los placeholders también para las páginas estáticas.
                htmlContent = replaceIconPlaceholders(htmlContent);
                
                contentPane.setText(htmlContent);
                SwingUtilities.invokeLater(() -> contentPane.setCaretPosition(0));
                logger.debug("Cargando página de ayuda: {}", helpURL);
            } else {
                throw new java.io.FileNotFoundException("Recurso de ayuda no encontrado: " + resourcePath);
            }
        } catch (Exception ex) {
            logger.error("Error al cargar la página de ayuda: " + resourcePath, ex);
            contentPane.setText("<html><body><h1>Error</h1><p>Ocurrió un error al cargar la página de ayuda.</p></body></html>");
        }
    } // --- Fin del método loadStaticHelpPage ---

    private String replaceIconPlaceholders(String htmlContent) {
        UIDefinitionService service = new UIDefinitionService();
        for (ToolbarDefinition toolbarDef : service.generateModularToolbarStructure()) {
            for (ToolbarComponentDefinition compDef : toolbarDef.componentes()) {
                if (compDef instanceof ToolbarButtonDefinition buttonDef && buttonDef.claveIcono() != null && !buttonDef.claveIcono().isBlank()) {
                    String placeholder = "ICON_PLACEHOLDER_" + buttonDef.claveIcono();
                    String iconUrl = getIconUrl("black", buttonDef.claveIcono());
                    htmlContent = htmlContent.replace(placeholder, iconUrl);
                }
            }
        }
        return htmlContent;
    } // --- Fin del método replaceIconPlaceholders ---

    private String getIconUrl(String themeFolder, String iconName) {
        String resourcePath = "/iconos/" + themeFolder + "/" + iconName;
        URL iconUrl = getClass().getResource(resourcePath);
        if (iconUrl != null) {
            return iconUrl.toExternalForm();
        } else {
            resourcePath = "/iconos/comunes/" + iconName;
            iconUrl = getClass().getResource(resourcePath);
            if (iconUrl != null) {
                return iconUrl.toExternalForm();
            }
        }
        logger.warn("No se pudo encontrar el icono '{}' en la carpeta '{}' o en 'comunes'", iconName, themeFolder);
        return "";
    } // --- Fin del método getIconUrl ---

    private static class HelpPageInfo {
        private final String title;
        private final String htmlFileName;
        public HelpPageInfo(String title, String htmlFileName) { this.title = title; this.htmlFileName = htmlFileName; }
        public String getHtmlFileName() { return htmlFileName; }
        @Override public String toString() { return title; }
    } // --- Fin de la clase interna HelpPageInfo ---

} // --- FIN de la clase HelpDialog ---