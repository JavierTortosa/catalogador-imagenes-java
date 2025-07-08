package vista.panels.export;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import modelo.proyecto.ExportStatus;

public class ExportStatusRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    public ExportStatusRenderer() {
        super();
        setOpaque(true); // Asegurarse de que el fondo se pinte
        setHorizontalAlignment(JLabel.CENTER);
    } // --- Fin del método ExportStatusRenderer (constructor) ---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        // El valor que llega es nuestro enum ExportStatus
        if (value instanceof ExportStatus) {
            ExportStatus status = (ExportStatus) value;
            setText(status.toString()); // El texto será el nombre del enum
            
            // Establecer el color de fondo basado en el estado
            switch (status) {
                case ENCONTRADO_OK:
                case ASIGNADO_MANUAL:
                case COPIADO_OK:
                    setBackground(new Color(180, 255, 180)); // Verde claro
                    setForeground(Color.BLACK);
                    break;
                case NO_ENCONTRADO:
                case ERROR_COPIA:
                    setBackground(new Color(255, 180, 180)); // Rojo claro
                    setForeground(Color.BLACK);
                    break;
                case SUGERENCIA:
                case MULTIPLES_CANDIDATOS:
                    setBackground(new Color(255, 255, 180)); // Amarillo claro
                    setForeground(Color.BLACK);
                    break;
                case COPIANDO:
                    setBackground(new Color(180, 200, 255)); // Azul claro
                    setForeground(Color.BLACK);
                    break;
                case PENDIENTE:
                default:
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                    break;
            }
        } else {
            // Fallback por si llega un valor inesperado
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        // Si la fila está seleccionada, usar los colores de selección de la tabla
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        }

        return this;
    } // --- Fin del método getTableCellRendererComponent ---
    
} // --- FIN de la clase ExportStatusRenderer ---