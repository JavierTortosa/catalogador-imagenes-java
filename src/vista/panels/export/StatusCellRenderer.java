package vista.panels.export;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import modelo.proyecto.ExportItem;
import modelo.proyecto.ExportStatus;
import vista.util.IconUtils;

public class StatusCellRenderer extends DefaultTableCellRenderer {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    private static final long serialVersionUID = 1L;
    private IconUtils iconUtils;

    // Guardamos una referencia a IconUtils para poder cargar los iconos.
    public StatusCellRenderer(IconUtils iconUtils) {
        this.iconUtils = iconUtils;
        setOpaque(true); // Es crucial para que el fondo se pinte correctamente.
    } // --- Fin del método StatusCellRenderer (constructor) ---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // El valor que llega desde el TableModel es el objeto ExportItem completo.
        if (!(value instanceof ExportItem)) {
            // Si por alguna razón no es un ExportItem, usamos el comportamiento por defecto.
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        ExportItem item = (ExportItem) value;
        ExportStatus status = item.getEstadoArchivoComprimido();
        
        // 1. Establecer el texto de la celda
        String textoCelda = "";
        switch (status) {
            case ENCONTRADO_OK:
            case ASIGNADO_MANUAL:
                // Si está OK, mostramos el nombre del archivo comprimido.
                if (item.getRutaArchivoComprimido() != null) {
                    textoCelda = item.getRutaArchivoComprimido().getFileName().toString();
                }
                break;
            default:
                // Para los demás estados, usamos el nombre del enum.
                textoCelda = status.toString();
                break;
        }
        setText(textoCelda);

        // 2. Establecer el icono de la celda
        // Usamos el nombre del icono guardado en nuestro enum y lo cargamos con IconUtils.
        // Escalamos a un tamaño pequeño y consistente, por ejemplo, 16x16.
        ImageIcon icon = iconUtils.getCommonIcon(status.getIconName());
        setIcon(iconUtils.scaleImageIcon(icon, 16, 16));

        // 3. Establecer el ToolTipText de la celda
        setToolTipText(status.getTooltip());

        // 4. Establecer los colores de fondo
        // Si la fila está seleccionada, usamos los colores de selección de la tabla.
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            // Si no está seleccionada, usamos nuestro sistema de colores.
            setForeground(Color.BLACK); // Texto negro para mayor legibilidad sobre fondos de color.
            switch (status) {
                case ENCONTRADO_OK:
                case ASIGNADO_MANUAL:
                case COPIADO_OK:
                    setBackground(new Color(180, 255, 180)); // Verde claro
                    break;
                case IMAGEN_NO_ENCONTRADA:
                case NO_ENCONTRADO:
                case ERROR_COPIA:
                    setBackground(new Color(255, 180, 180)); // Rojo claro
                    break;
                case MULTIPLES_CANDIDATOS:
                    setBackground(new Color(255, 255, 180)); // Amarillo claro
                    break;
                case IGNORAR_COMPRIMIDO:
                    setBackground(new Color(210, 210, 210)); // Gris neutro
                    break;
                default: // PENDIENTE, COPIANDO, etc.
                    setBackground(table.getBackground()); // Color de fondo normal de la tabla
                    setForeground(table.getForeground());
                    break;
            }
        }
        
        return this;
    } // --- Fin del método getTableCellRendererComponent ---
    
} // --- FIN de la clase StatusCellRenderer ---