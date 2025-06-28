package vista.builders;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.panels.ImageDisplayPanel;
import vista.renderers.NombreArchivoRenderer;
import vista.theme.ThemeManager;

public class ProjectBuilder {

    private final ComponentRegistry registry;
    private final VisorModel model;
    // --- INICIO DE LA MODIFICACIÓN: Nuevas dependencias ---
    private final ThemeManager themeManager;
    // --- FIN DE LA MODIFICACIÓN ---

    // <--- MODIFICADO: Constructor ahora necesita ThemeManager ---
    public ProjectBuilder(ComponentRegistry registry, VisorModel model, ThemeManager themeManager) {
        this.registry = Objects.requireNonNull(registry, "Registry no puede ser null en ProjectBuilder");
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ProjectBuilder");
        this.themeManager = Objects.requireNonNull(themeManager, "ThemeManager no puede ser null en ProjectBuilder");
    } // --- Fin del método ProjectBuilder (constructor) ---

    // <--- MODIFICADO: El método ahora construye una UI completa ---
    public JPanel buildProjectViewPanel() {
        JPanel panelProyecto = new JPanel(new BorderLayout());
        registry.register("view.panel.proyectos", panelProyecto);

        // Creamos la estructura principal con un JSplitPane
        JSplitPane splitPaneProyecto = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            createProjectLeftPanel(), 
            createProjectRightPanel()
        );
        splitPaneProyecto.setResizeWeight(0.25);
        splitPaneProyecto.setContinuousLayout(true);
        splitPaneProyecto.setBorder(null);
        registry.register("splitpane.proyecto", splitPaneProyecto);

        panelProyecto.add(splitPaneProyecto, BorderLayout.CENTER);
        
        // Podríamos añadir una barra de estado o de miniaturas específica para el proyecto aquí si quisiéramos
        // panelProyecto.add(createProjectStatusBar(), BorderLayout.SOUTH);

        return panelProyecto;
    } // --- Fin del método buildProjectViewPanel ---

    /**
     * Crea el panel izquierdo para la vista de proyecto, que contiene la lista de imágenes marcadas.
     * @return Un JPanel configurado.
     */
    private JPanel createProjectLeftPanel() {
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(themeManager.getTemaActual().colorFondoPrincipal());
        
        TitledBorder border = BorderFactory.createTitledBorder("Imágenes del Proyecto: 0");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        panelIzquierdo.setBorder(border);
        registry.register("panel.proyecto.lista", panelIzquierdo);

        // Creamos una JList específica para el proyecto. Inicialmente estará vacía.
        JList<String> projectFileList = new JList<>();
        projectFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Usamos el mismo renderer que la lista principal para consistencia visual.
        projectFileList.setCellRenderer(new NombreArchivoRenderer(themeManager));
        registry.register("list.proyecto.nombres", projectFileList);

        JScrollPane scrollPane = new JScrollPane(projectFileList);
        scrollPane.setBorder(BorderFactory.createLineBorder(themeManager.getTemaActual().colorBorde()));
        registry.register("scroll.proyecto.nombres", scrollPane);

        panelIzquierdo.add(scrollPane, BorderLayout.CENTER);
        return panelIzquierdo;
    } // --- Fin del método createProjectLeftPanel ---

    /**
     * Crea el panel derecho para la vista de proyecto, que contiene el visor de imagen.
     * @return Un JPanel configurado.
     */
    private JPanel createProjectRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        registry.register("panel.proyecto.visor", rightPanel);

        // Creamos un ImageDisplayPanel específico para esta vista.
        ImageDisplayPanel imageDisplayPanel = new ImageDisplayPanel(this.themeManager, this.model);
        registry.register("panel.proyecto.display", imageDisplayPanel);
        
        TitledBorder border = BorderFactory.createTitledBorder("");
        border.setTitleColor(themeManager.getTemaActual().colorBordeTitulo());
        imageDisplayPanel.setBorder(border);
        
        rightPanel.add(imageDisplayPanel, BorderLayout.CENTER);
        
        // Aquí podríamos añadir controles específicos para el modo proyecto en el futuro.

        return rightPanel;
    } // --- Fin del método createProjectRightPanel ---

} // --- FIN DE LA CLASE ProjectBuilder ---


