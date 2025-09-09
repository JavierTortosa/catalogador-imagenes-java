package vista.panels.export;

import java.awt.Color;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class MultiLineCellRenderer extends JPanel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

    public MultiLineCellRenderer() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    } // ---FIN de metodo [MultiLineCellRenderer]---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Limpiamos el panel de componentes anteriores
        removeAll();

        // Establecemos los colores de fondo y texto según si la fila está seleccionada
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        // El valor que recibimos del modelo es la List<Path>
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Path> files = (List<Path>) value;

            if (files.isEmpty()) {
                // Si no hay archivos, mostramos un texto indicativo
                add(createLabel("- Ninguno -"));
            } else {
                // Si hay archivos, creamos un JLabel para cada uno
                for (Path file : files) {
                    add(createLabel(file.getFileName().toString()));
                }
            }
        }
        
        return this;
    } // ---FIN de metodo [getTableCellRendererComponent]---

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(getForeground());
        label.setBackground(getBackground());
        label.setOpaque(true);
        return label;
    } // ---FIN de metodo [createLabel]---

} // --- FIN de clase [MultiLineCellRenderer]---