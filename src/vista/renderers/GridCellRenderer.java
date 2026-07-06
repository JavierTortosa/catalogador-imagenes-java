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

import controlador.ProjectController;
import controlador.managers.interfaces.IProjectManager;
import modelo.VisorModel;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.image.ThumbnailService;
import vista.components.CustomGridCellPanel;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer;

public class GridCellRenderer implements ListCellRenderer<String> {
	
	
    private final ThumbnailService gridThumbnailService;
    private final VisorModel modeloVisor;
    private final CustomGridCellPanel cellPanel;
    private final boolean showNamesDefault;
    private final IconUtils iconUtils;
    private final ThumbnailPreviewer previewer;
    private IProjectManager projectManager;
    
    private final ProjectController projectController;

    private static final Color COLOR_DESACTIVADO = new Color(128, 128, 128); // Gris para el borde
    
    private static int MARCO_ESTADO_HEIGHT = 20;
    private static int MARCO_ESTADO_WIDTH = 20;
    
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
        
        
        int anchoMiniatura = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_WIDTH, 120);
        int altoMiniatura = configuration.getInt(ConfigKeys.GRID_THUMBNAIL_HEIGHT, 120);
        this.showNamesDefault = configuration.getBoolean(ConfigKeys.GRID_MOSTRAR_NOMBRES_STATE, true);
        
        this.cellPanel = new CustomGridCellPanel();
        
        int separacion = 10;
        this.cellPanel.setPreferredSize(new java.awt.Dimension(anchoMiniatura + separacion, altoMiniatura + separacion));

        this.cellPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Component p = (Component) e.getSource();
                    while (p != null) {
                        if (p instanceof JList) {
                            JList<?> list = (JList<?>) p;
                            Point pt = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), list);
                            int idx = list.locationToIndex(pt);
                            if (idx != -1) {
                            @SuppressWarnings("unchecked")
                                JList<String> sl = (JList<String>) list;
                                previewer.showPreviewForIndexPublic(sl, idx);
                            }
                            return;
                        }
                        p = p.getParent();
                    }
                }
            }
        });
    } // ---FIN de metodo constructor ---
    
    public void setProjectManager(IProjectManager pm) {
        this.projectManager = pm;
    }

    public java.awt.Dimension getCellSize() {
        return this.cellPanel.getPreferredSize();
    } // ---FIN de metodo ---

    public void setCellSize(int nuevoAncho, int nuevoAlto) {
        int separacion = 10;
        this.cellPanel.setPreferredSize(new java.awt.Dimension(nuevoAncho + separacion, nuevoAlto + separacion));
    } // ---FIN de metodo setCellSize ---

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        
        Path rutaCompleta = (value != null) ? this.modeloVisor.getRutaCompleta(value) : null;
        
        int anchoMiniatura = cellPanel.getPreferredSize().width - MARCO_ESTADO_WIDTH;
        int altoMiniatura = cellPanel.getPreferredSize().height - MARCO_ESTADO_HEIGHT;
        
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
                Path fileNamePath = rutaCompleta.getFileName();
                textoParaMostrar = (fileNamePath != null) ? fileNamePath.toString() : rutaCompleta.toString();
            }
        }

        // --- INICIO DE LA LÓGICA DE COLOR Y ESTADO UNIFICADA ---
        
        Color statusBorderColor = null; // null significa sin borde de estado
        boolean desaturar = false;

        if (this.projectController != null && modeloVisor.isGridMuestraEstado()) {
            ExportItem item = this.projectController.getExportItem(value);

            if (item != null) {
                if (!item.isSeleccionadoParaExportar()) {
                    // REGLA: Si el checkbox está desmarcado, borde gris e imagen desaturada.
                    statusBorderColor = COLOR_DESACTIVADO;
                    desaturar = true;
                } else {
                    // REGLA: Usar la misma lógica de conflicto que la tabla.
                    ExportStatus status = item.tieneConflictoDeNombre() 
                                          ? ExportStatus.NOMBRE_DUPLICADO 
                                          : item.getEstadoArchivoComprimido();
                    
                    // REGLA: Obtener el color directamente del enum.
                    statusBorderColor = status.getColor();
                }
            }
        }
        
        ImageIcon iconoFinal = miniaturaIcono;
        if (desaturar && miniaturaIcono != null && miniaturaIcono.getImage() != null) {
            BufferedImage bufferedImage = new BufferedImage(
                miniaturaIcono.getIconWidth(),
                miniaturaIcono.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = bufferedImage.createGraphics();
            g.drawImage(miniaturaIcono.getImage(), 0, 0, null);
            g.dispose();
            
            iconoFinal = new ImageIcon(IconUtils.toGrayscale(bufferedImage));
        }

        // --- Calcular estado de MARCADO (oculto en modo PROYECTO porque es redundante y tapa el selector) ---
        boolean isMarked = false;
        Color markedColor = null;
        if (modeloVisor.getCurrentWorkMode() != VisorModel.WorkMode.PROYECTO
                && rutaCompleta != null && projectManager != null) {
            isMarked = projectManager.estaMarcada(rutaCompleta);
        }
        if (isMarked) {
            markedColor = javax.swing.UIManager.getColor("Visor.markedColor");
            if (markedColor == null) {
                markedColor = new Color(255, 191, 0);
            }
        }

        // Llamada corregida a setData:
        // 1. cellBackground: Siempre el color de fondo de la lista.
        // 2. borderColor: El color que acabamos de calcular (puede ser null).
        this.cellPanel.setData(iconoFinal, textoParaMostrar, isSelected, list.getBackground(), statusBorderColor, isMarked, markedColor);
        
        // --- FIN DE LA LÓGICA DE COLOR Y ESTADO UNIFICADA ---

        return this.cellPanel;
    } // ---FIN de metodo getListCellRendererComponent ---
    
} // --- FIN de clase GridCellRenderer ---