package vista.renderers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import modelo.VisorModel;
import servicios.image.ThumbnailService;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

/**
 * Renderer para las celdas del GridDisplayPanel. Se encarga de dibujar una
 * miniatura de mayor tamaño con su nombre de archivo debajo.
 */
public class GridCellRenderer extends JPanel implements ListCellRenderer<String> {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// --- Componentes Internos ---
    private final JLabel imageLabel;
    private final JLabel nameLabel;
    
    // --- Dependencias (Inyectadas) ---
    private final ThumbnailService thumbnailService;
    private final VisorModel model;
    private final ThemeManager themeManager;
    private final IconUtils iconUtils;

    // --- Constantes de Diseño ---
    private static final int THUMBNAIL_WIDTH = 128;
    private static final int THUMBNAIL_HEIGHT = 128;
    private static final int NAME_LABEL_HEIGHT = 35; // Altura para aprox. 2 líneas de texto

    public GridCellRenderer(ThumbnailService thumbnailService, VisorModel model, ThemeManager themeManager, IconUtils iconUtils) {
        this.thumbnailService = thumbnailService;
        this.model = model;
        this.themeManager = themeManager;
        this.iconUtils = iconUtils;

        // Configuración del layout del panel del renderer
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setOpaque(true);
        
        // Label para la miniatura
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new java.awt.Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));

        // Label para el nombre del archivo
        nameLabel = new JLabel();
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setVerticalAlignment(SwingConstants.TOP);
        nameLabel.setPreferredSize(new java.awt.Dimension(THUMBNAIL_WIDTH, NAME_LABEL_HEIGHT));

        add(imageLabel, BorderLayout.CENTER);
        add(nameLabel, BorderLayout.SOUTH);
    } // --- Fin del Constructor ---

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        
        Tema temaActual = themeManager.getTemaActual();
        Path rutaCompleta = (value != null) ? model.getRutaCompleta(value) : null;
        
        // --- 1. Cargar la Miniatura ---
        ImageIcon thumbnail = null;
        if (rutaCompleta != null) {
            thumbnail = thumbnailService.obtenerOCrearMiniatura(rutaCompleta, value, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
        }
        
        if (thumbnail != null) {
            imageLabel.setIcon(thumbnail);
            imageLabel.setText(null);
        } else {
            imageLabel.setIcon(iconUtils.getScaledCommonIcon("imagen-rota.png", 64, 64));
            imageLabel.setText(null);
        }

        // --- 2. Establecer el Nombre del Archivo ---
        if (rutaCompleta != null) {
            String fileName = rutaCompleta.getFileName().toString();
            nameLabel.setText("<html><div style='text-align: center; width: 120px;'>" + fileName + "</div></html>");
        } else {
            nameLabel.setText("");
        }
        
        // --- 3. Aplicar Estilos de Selección ---
        if (isSelected) {
            setBackground(temaActual.colorSeleccionFondo());
            nameLabel.setForeground(temaActual.colorSeleccionTexto());
            setBorder(BorderFactory.createLineBorder(temaActual.colorBordeSeleccionActiva(), 2));
        } else {
            setBackground(temaActual.colorFondoPrincipal());
            nameLabel.setForeground(temaActual.colorTextoPrimario());
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(temaActual.colorFondoPrincipal(), 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        }
        
        return this;
    } // --- Fin del método getListCellRendererComponent ---

} // --- Fin de la clase GridCellRenderer ---