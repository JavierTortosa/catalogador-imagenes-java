package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.managers.interfaces.IProjectManager; 
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.renderers.GridCellRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;

public class GridDisplayPanel extends JPanel {

	private static final Logger logger = LoggerFactory.getLogger(GridDisplayPanel.class); 
	
    private static final long serialVersionUID = 1L;
    private JList<String> gridList;
    private JPanel toolbarContainer;
    private GridCellRenderer renderer;
    
    /**
     * Constructor para el MODO VISUALIZADOR.
     * No necesita un IProjectManager.
     */
    public GridDisplayPanel(
            VisorModel model,
            ThumbnailService gridThumbnailService,
            ThemeManager themeManager,
            IconUtils iconUtils,
            ThumbnailPreviewer gridPreviewer,
            ComponentRegistry registry
    ) {
        // Llama al constructor principal pasando 'null' para el projectManager.
        this(model, gridThumbnailService, themeManager, iconUtils, gridPreviewer, null, null, registry);
    } // ---FIN de constructor GridDisplayPanel ---

    /**
     * Constructor principal y extendido para el MODO PROYECTO.
     * Acepta un IProjectManager para poder gestionar etiquetas.
     */
    public GridDisplayPanel(
            VisorModel model,
            ThumbnailService gridThumbnailService,
            ThemeManager themeManager,
            IconUtils iconUtils,
            ThumbnailPreviewer gridPreviewer,
            IProjectManager projectManager,
            ProjectController projectController,
            ComponentRegistry registry
    ) {
        super(new BorderLayout());
        
        logger.debug("Creando un nuevo GridDisplayPannel");
        
        toolbarContainer = new JPanel(new BorderLayout());
        add(toolbarContainer, BorderLayout.SOUTH);
        
        gridList = new JList<>();
        gridList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        gridList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        gridList.setSelectionBackground(new Color(0, 0, 0, 0));
        gridList.setSelectionForeground(new Color(0, 0, 0, 0));
        gridList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        gridList.setVisibleRowCount(-1);
        
        
        
        this.renderer = new GridCellRenderer(
            gridThumbnailService,
            model,
            themeManager,
            iconUtils,
            ConfigurationManager.getInstance(),
            gridPreviewer,
            projectManager,
            projectController
        );
        gridList.setCellRenderer(this.renderer);

        gridList.setFixedCellWidth(this.renderer.getCellSize().width);
        gridList.setFixedCellHeight(this.renderer.getCellSize().height);

        JScrollPane scrollPane = new JScrollPane(gridList);
        
        
        if (projectManager == null) {
            registry.register("scroll.grid.visualizador", scrollPane);
        } else {
            registry.register("scroll.grid.proyecto", scrollPane);
        }
        
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        
        add(scrollPane, BorderLayout.CENTER);
    } // ---FIN de Constructor GridDisplayPanel ---

    public void setProjectManager(IProjectManager pm) {
        if (this.renderer != null) {
            this.renderer.setProjectManager(pm);
        }
    }

    public JList<String> getGridList() {
        return gridList;
    } // ---FIN de metodo getGridList ---

    public void setGridCellSize(int nuevoAncho, int nuevoAlto) {
        if (gridList != null) {
            gridList.setFixedCellWidth(nuevoAncho);
            gridList.setFixedCellHeight(nuevoAlto);
            gridList.revalidate();
            gridList.repaint();
            logger.debug("Tamaño de celda del grid actualizado a: {}x{}", nuevoAncho, nuevoAlto);
        }
    } // ---FIN de metodo setGridCellSize ---
    
    
    public void setToolbars(List<JToolBar> toolbars) {
        toolbarContainer.removeAll(); // Limpiamos el contenedor
        if (toolbars != null && !toolbars.isEmpty()) {
            
            // Creamos un panel "envoltorio" que usa FlowLayout para alinear las barras.
            // FlowLayout.RIGHT las alineará a la derecha.
            JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); // 5px de espacio horizontal, 0 vertical
            wrapperPanel.setOpaque(false); // Hacemos el envoltorio transparente

            // Añadimos cada toolbar de la lista al panel envoltorio.
            for (JToolBar toolbar : toolbars) {
                wrapperPanel.add(toolbar);
            }
            
            // Añadimos el envoltorio (que contiene todas las toolbars) al contenedor principal.
            toolbarContainer.add(wrapperPanel, BorderLayout.CENTER);
        }
        
        toolbarContainer.revalidate();
        toolbarContainer.repaint();
    } // ---FIN de metodo setToolbars---
    
    
    
    
    
} // --- FIN de clase ---