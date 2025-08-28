package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.components.CustomGridCellPanel;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;

public class GridCellRenderer implements ListCellRenderer<String> {
	
	private static final Logger logger = LoggerFactory.getLogger(GridCellRenderer.class); 

    private final ThumbnailService gridThumbnailService;
    private final VisorModel modeloVisor;
    private final CustomGridCellPanel cellPanel;
    private final boolean showNamesDefault;
    private final IconUtils iconUtils;
    private final ThumbnailPreviewer previewer;
    private final IProjectManager projectManager;
    private final Color selectionBorderColor;
    
    public GridCellRenderer(
            ThumbnailService gridThumbnailService,
            VisorModel modeloVisor,
            ThemeManager themeManager,
            IconUtils iconUtils,
            ConfigurationManager configuration,
            ThumbnailPreviewer previewer,
            IProjectManager projectManager // <<< AHORA PUEDE SER NULL
    ) {
        this.gridThumbnailService = Objects.requireNonNull(gridThumbnailService);
        this.modeloVisor = Objects.requireNonNull(modeloVisor);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.previewer = Objects.requireNonNull(previewer);
        this.projectManager = projectManager; // <<< Simplemente lo asignamos, sea null o no.
        
        this.selectionBorderColor = themeManager.getTemaActual().colorSeleccionFondo();//.colorBordeSeleccionActiva();
        
        int anchoMiniatura = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
        int altoMiniatura = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_HEIGHT, 120);
        this.showNamesDefault = configuration.getBoolean("grid.mostrar.nombres.state", true);
        
        this.cellPanel = new CustomGridCellPanel();
        this.cellPanel.setPreferredSize(new java.awt.Dimension(anchoMiniatura + 12, altoMiniatura + 12));
        
        this.cellPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Component sourceComponent = (Component) e.getSource();
                    if (sourceComponent.getParent() instanceof JList) {
                        JList<?> list = (JList<?>) sourceComponent.getParent();
                        Point pointInList = SwingUtilities.convertPoint(sourceComponent, e.getPoint(), list);
                        int index = list.locationToIndex(pointInList);
                        if (index != -1) {
                            previewer.showPreviewForIndexPublic((JList<String>) list, index);
                        }
                    }
                }
            }
        });
    } // ---FIN de metodo ---
    
    public java.awt.Dimension getCellSize() {
        return this.cellPanel.getPreferredSize();
    } // ---FIN de metodo ---

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        
        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
        
        int anchoMiniatura = cellPanel.getPreferredSize().width - 12;
        int altoMiniatura = cellPanel.getPreferredSize().height - 12;
        
        ImageIcon miniaturaIcono = null;
        if (rutaCompleta != null) {
            miniaturaIcono = this.gridThumbnailService.obtenerOCrearMiniatura(
                rutaCompleta, value, anchoMiniatura, altoMiniatura, true,
                (generatedKey) -> {
                    if (list != null && list.isShowing()) {
                        Rectangle cellBounds = list.getCellBounds(index, index);
                        if (cellBounds != null && list.getVisibleRect().intersects(cellBounds)) {
                            list.repaint(cellBounds);
                        }
                    }
                }
            );
        }
        
        if (miniaturaIcono == null) {
            miniaturaIcono = this.iconUtils.getScaledCommonIcon("placeholder-grid.png", 32, 32);
        }

        String textoParaMostrar = null;
        if (rutaCompleta != null) {
            // --- INICIO DE LA MODIFICACIÓN ---
            // Solo intentamos obtener la etiqueta si estamos en un contexto de proyecto
            if (projectManager != null) {
                String etiqueta = projectManager.getEtiqueta(rutaCompleta);
                if (etiqueta != null && !etiqueta.isBlank()) {
                    textoParaMostrar = etiqueta;
                }
            }
            // Si después de comprobar la etiqueta, el texto sigue siendo null, usamos el nombre del archivo.
            if (textoParaMostrar == null && this.showNamesDefault) {
                textoParaMostrar = rutaCompleta.getFileName().toString();
            }
            // --- FIN DE LA MODIFICACIÓN ---
        }

        this.cellPanel.setData(miniaturaIcono, textoParaMostrar, isSelected, list.getBackground(), this.selectionBorderColor);

        return this.cellPanel;
    } // ---FIN de metodo ---
    
} // --- FIN de clase ---