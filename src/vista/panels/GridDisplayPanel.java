package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.managers.interfaces.IProjectManager; // <<< AÑADIR IMPORT
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

    /**
     * Constructor para el MODO VISUALIZADOR.
     * No necesita un IProjectManager.
     */
    public GridDisplayPanel(
            VisorModel model,
            ThumbnailService gridThumbnailService,
            ThemeManager themeManager,
            IconUtils iconUtils,
            ThumbnailPreviewer gridPreviewer
    ) {
        // Llama al constructor principal pasando 'null' para el projectManager.
        this(model, gridThumbnailService, themeManager, iconUtils, gridPreviewer, null, null);
    } // ---FIN de metodo ---

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
            ProjectController projectController
    ) {
        super(new BorderLayout());
        logger.debug("Creando un nuevo GridDisplayPannel");
        
        toolbarContainer = new JPanel(new BorderLayout());
        add(toolbarContainer, BorderLayout.SOUTH);
        
        gridList = new JList<>();
        gridList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gridList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        gridList.setSelectionBackground(new Color(0, 0, 0, 0));
        gridList.setSelectionForeground(new Color(0, 0, 0, 0));
        gridList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        gridList.setVisibleRowCount(-1);
        
        
        
        GridCellRenderer renderer = new GridCellRenderer(
            gridThumbnailService,
            model,
            themeManager,
            iconUtils,
            ConfigurationManager.getInstance(),
            gridPreviewer,
            projectManager,
            projectController
        );
        gridList.setCellRenderer(renderer);

        gridList.setFixedCellWidth(renderer.getCellSize().width);
        gridList.setFixedCellHeight(renderer.getCellSize().height);

        JScrollPane scrollPane = new JScrollPane(gridList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        
        add(scrollPane, BorderLayout.CENTER);
    } // ---FIN de metodo ---

    public JList<String> getGridList() {
        return gridList;
    } // ---FIN de metodo ---

    public void setGridCellSize(int nuevoAncho, int nuevoAlto) {
        if (gridList != null) {
            gridList.setFixedCellWidth(nuevoAncho);
            gridList.setFixedCellHeight(nuevoAlto);
            gridList.revalidate();
            gridList.repaint();
            logger.debug("Tamaño de celda del grid actualizado a: {}x{}", nuevoAncho, nuevoAlto);
        }
    } // ---FIN de metodo ---
    
    public void setToolbar(JToolBar toolbar) {
        toolbarContainer.removeAll();
        if (toolbar != null) {
        	
            // 1. Creamos un panel "envoltorio" que usa FlowLayout.
            //    FlowLayout.RIGHT alinea todos los componentes que contiene a la derecha.
            JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); // 0, 0 para quitar espacios
            wrapperPanel.setOpaque(false); // Hacemos el envoltorio transparente

            // 2. Añadimos la toolbar al envoltorio.
            wrapperPanel.add(toolbar);
            
            // 3. Añadimos el envoltorio (y no la toolbar directamente) al contenedor principal.
            toolbarContainer.add(wrapperPanel, BorderLayout.CENTER);

        }
        
        toolbarContainer.revalidate();
        toolbarContainer.repaint();
    } // ---FIN de metodo ---

} // --- FIN de clase ---