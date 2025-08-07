package vista.panels.export;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;

public class CheckHeaderRenderer extends JCheckBox implements TableCellRenderer {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
    private static final long serialVersionUID = 1L;

    public CheckHeaderRenderer() {
        // Configuramos el JCheckBox que usaremos como renderer.
        setHorizontalAlignment(JCheckBox.CENTER);
        setOpaque(true);
        // Es importante quitarle el borde para que se integre bien en la cabecera.
        setBorderPainted(false);
    } // --- Fin del método CheckHeaderRenderer (constructor) ---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        if (table != null) {
            // Hacemos que la apariencia del checkbox (fondo, borde) coincida
            // con la de la cabecera de la tabla por defecto.
            setBackground(table.getTableHeader().getBackground());
            setForeground(table.getTableHeader().getForeground());
            setFont(table.getTableHeader().getFont());
            setBorder(table.getTableHeader().getBorder());
        }

        // El texto del encabezado será el checkbox en sí, sin texto adicional.
        setText("");
        
        // Determinamos si el checkbox debe estar marcado o no.
        // Contamos cuántas filas están marcadas como "para exportar".
        int selectedCount = 0;
        for (int i = 0; i < table.getRowCount(); i++) {
            if ((Boolean) table.getValueAt(i, 0)) {
                selectedCount++;
            }
        }

        // Si todas las filas están seleccionadas, el checkbox estará marcado.
        // Si no hay filas, también estará desmarcado.
        setSelected(selectedCount == table.getRowCount() && table.getRowCount() > 0);
        
        return this;
    } // --- Fin del método getTableCellRendererComponent ---
    
} // --- FIN de la clase CheckHeaderRenderer ---