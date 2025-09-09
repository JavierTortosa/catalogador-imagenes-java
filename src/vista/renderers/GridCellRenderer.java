package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ProjectController;
import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import modelo.proyecto.ExportItem;
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
    
    private final ProjectController projectController;

    private static final Color COLOR_OK = new Color(34, 139, 34);
    private static final Color COLOR_WARNING = new Color(255, 165, 0);
    private static final Color COLOR_ERROR = Color.RED;//new Color(178, 34, 34);
    
    
    public GridCellRenderer(
            ThumbnailService gridThumbnailService,
            VisorModel modeloVisor,
            ThemeManager themeManager,
            IconUtils iconUtils,
            ConfigurationManager configuration,
            ThumbnailPreviewer previewer,
            IProjectManager projectManager,
            ProjectController projectController
    ) {
        this.gridThumbnailService = Objects.requireNonNull(gridThumbnailService);
        this.modeloVisor = Objects.requireNonNull(modeloVisor);
        this.iconUtils = Objects.requireNonNull(iconUtils);
        this.previewer = Objects.requireNonNull(previewer);
        this.projectManager = projectManager;
        this.projectController = projectController;
        
        this.selectionBorderColor = themeManager.getTemaActual().colorSeleccionFondo();
        
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
            if (projectManager != null) {
                String etiqueta = projectManager.getEtiqueta(rutaCompleta);
                if (etiqueta != null && !etiqueta.isBlank()) {
                    textoParaMostrar = etiqueta;
                }
            }
            if (textoParaMostrar == null && this.showNamesDefault) {
                textoParaMostrar = rutaCompleta.getFileName().toString();
            }
        }

        // =========================================================================
        // === INICIO DE LA CORRECCIÓN ===
        // =========================================================================
        
        Color backgroundColor = list.getBackground();
        boolean desaturar = false;

        // La única condición ahora es si el modelo dice que hay que mostrar el estado.
        // Ya no nos importa si estamos en modo exportación aquí.
        if (this.projectController != null && modeloVisor.isGridMuestraEstado()) {
            
            ExportItem item = this.projectController.getExportItem(value);

            if (item != null) {
                switch (item.getEstadoArchivoComprimido()) {
                    case ENCONTRADO_OK:
                        backgroundColor = COLOR_OK;
                        break;
                    case ASIGNADO_MANUAL:
                    case IGNORAR_COMPRIMIDO:
                        backgroundColor = COLOR_WARNING;
                        break;
                    case NO_ENCONTRADO:
                    case IMAGEN_NO_ENCONTRADA:
                        backgroundColor = COLOR_ERROR;
                        break;
                    default:
                        break;
                }
                
                if (!item.isSeleccionadoParaExportar()) {
                    desaturar = true;
                }
            }
        }
        
        ImageIcon iconoFinal = miniaturaIcono;
        if (desaturar && miniaturaIcono != null && miniaturaIcono.getImage() != null) {
        	// Convertimos Image a BufferedImage para poder desaturar
            BufferedImage bufferedImage = new BufferedImage(
                miniaturaIcono.getIconWidth(),
                miniaturaIcono.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = bufferedImage.createGraphics();
            g.drawImage(miniaturaIcono.getImage(), 0, 0, null);
            g.dispose();
            
            // Usamos la clase IconUtils para la conversión
            iconoFinal = new ImageIcon(IconUtils.toGrayscale(bufferedImage));
        }

        // AHORA llamamos a setData con el icono y color correctos
        this.cellPanel.setData(iconoFinal, textoParaMostrar, isSelected, backgroundColor, this.selectionBorderColor);
        // =========================================================================
        // === FIN DE LA CORRECCIÓN ===
        // =========================================================================

        return this.cellPanel;
    } // ---FIN de metodo ---
    
} // --- FIN de clase GridCellRenderer ---