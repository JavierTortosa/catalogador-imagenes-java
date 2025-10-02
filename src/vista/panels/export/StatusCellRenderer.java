package vista.panels.export;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import vista.util.IconUtils;

public class StatusCellRenderer extends DefaultTableCellRenderer {
	
	private static final Logger logger = LoggerFactory.getLogger(StatusCellRenderer.class);

    private static final long serialVersionUID = 1L;
    private IconUtils iconUtils;

    private static final Color COLOR_DESACTIVADO = new Color(220, 220, 220); // Gris claro para fondo
    private static final Color COLOR_TEXTO_DESACTIVADO = new Color(128, 128, 128); // Gris oscuro para texto
    
    // Guardamos una referencia a IconUtils para poder cargar los iconos.
    public StatusCellRenderer(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        setOpaque(true); // Es crucial para que el fondo se pinte correctamente.
    } // --- Fin del método StatusCellRenderer (constructor) ---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (!(value instanceof ExportItem)) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        ExportItem item = (ExportItem) value;
        ExportStatus status = item.tieneConflictoDeNombre() ? ExportStatus.NOMBRE_DUPLICADO : item.getEstadoArchivoComprimido();
        
        String textoCelda = status.toString();
        if (status == ExportStatus.ASIGNADO_MANUAL) {
             textoCelda = "ASIGNADO MANUALMENTE";
        }
        setText(textoCelda);
        
        ImageIcon icon = iconUtils.getCommonIcon(status.getIconName());
        setIcon(iconUtils.scaleImageIcon(icon, 16, 16));
        setToolTipText(status.getTooltip());

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else if (!item.isSeleccionadoParaExportar()) {
            setBackground(COLOR_DESACTIVADO);
            setForeground(COLOR_TEXTO_DESACTIVADO);
        } else {
            Color statusColor = status.getColor();
            if (statusColor != null) {
                setBackground(statusColor);
            } else {
                setBackground(table.getBackground());
            }
            
            // --- INICIO DE LA MODIFICACIÓN FINAL ---
            // Si el estado es de error, ponemos el texto en blanco. Para el resto, negro.
            if (status == ExportStatus.IMAGEN_NO_ENCONTRADA ||
                    status == ExportStatus.ERROR_COPIA ||
                    status == ExportStatus.NOMBRE_DUPLICADO ||
                    status == ExportStatus.ASIGNADO_DUPLICADO ||
                    status == ExportStatus.ENCONTRADO_OK ||
                    status == ExportStatus.ASIGNADO_MANUAL ||
                    status == ExportStatus.COPIADO_OK)
            {
                setForeground(Color.WHITE);
            } else {
                setForeground(Color.BLACK);
            }
            // --- FIN DE LA MODIFICACIÓN FINAL ---
        }
        
        return this;
    } // --- Fin del método getTableCellRendererComponent ---
    
} // --- FIN de la clase StatusCellRenderer ---