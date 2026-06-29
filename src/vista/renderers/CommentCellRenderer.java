package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.proyecto.ImageCheckboxOverlay;
import modelo.proyecto.Mensaje;
import modelo.proyecto.ProjectImage;
import vista.models.ClienteTableModel;

/**
 * Renderizador de celdas para comentarios en las tablas de modo cliente.
 * Muestra un icono de sobre (✉) si hay comentario, con tooltip.
 * Dorado si el último mensaje es del cliente.
 * Verde si el último mensaje es del taller.
 * Gris si está vacío.
 */
public class CommentCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    // Paleta de colores "Semáforo"
    private static final Color PENDIENTE_CLIENTE = new Color(200, 160, 0);   // Dorado
    private static final Color RESPONDIDO_TALLER = new Color(87, 235, 113);  // Verde
    private static final Color SIN_MENSAJES = new Color(160, 160, 160);      // Gris

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        label.setHorizontalAlignment(JLabel.CENTER);
        label.setText(""); // Limpiamos texto por defecto

        if (!(table.getModel() instanceof ClienteTableModel ctm)) {
            return label;
        }

        int modelRow = table.convertRowIndexToModel(row);
        boolean isParent = ctm.isParentRow(modelRow);
        
        List<Mensaje> thread = null;
        
        // 1. Extraer el hilo dependiendo de si es Padre (Imagen) o Hijo (Checkbox)
        if (isParent) {
            ProjectImage pi = ctm.getProjectImage(modelRow);
            if (pi != null) thread = pi.getCommentThread();
        } else {
            ImageCheckboxOverlay cb = ctm.getCheckbox(modelRow);
            if (cb != null) thread = cb.getCommentThread();
        }

        // 2. Lógica del Semáforo con el sobre
        if (thread != null && !thread.isEmpty()) {
            label.setText("\u2709"); // Icono de sobre
            
            Mensaje ultimoMsg = thread.get(thread.size() - 1);
            // Tooltip útil para ver el último msj sin hacer clic
            label.setToolTipText(ultimoMsg.de().toUpperCase() + ": " + ultimoMsg.texto());
            
            if (!isSelected) {
                boolean requiereAtencion = "cliente".equalsIgnoreCase(ultimoMsg.de());
                label.setForeground(requiereAtencion ? PENDIENTE_CLIENTE : RESPONDIDO_TALLER);
            }
        } else {
            // Si no hay mensajes, mostramos el sobre en gris para hijos, y nada para padres
            label.setText(isParent ? "" : "\u2709");
            label.setToolTipText(null);
            if (!isSelected) label.setForeground(SIN_MENSAJES);
        }

        // 3. Resumen de hijos colapsados (Mantiene tu funcionalidad de ver el '📝 X')
        if (isParent) {
            int msgCount = ctm.getCheckboxMessageCount(modelRow);
            if (msgCount > 0 && ctm.getCollapseFilter(modelRow) != ClienteTableModel.CollapseFilter.NONE) {
                String textoActual = label.getText();
                label.setText((textoActual.isEmpty() ? "" : textoActual + " | ") + "\uD83D\uDCDD " + msgCount);
                
                // Si el color base era gris, lo ponemos en azul para destacar que hay notas colapsadas
                if (label.getForeground() == SIN_MENSAJES && !isSelected) {
                    label.setForeground(new Color(70, 130, 180)); 
                }
            }
        }

        return label;
    } 
}

//package vista.renderers;
//
//import java.awt.Color;
//import java.awt.Component;
//import java.util.List;
//
//import javax.swing.JLabel;
//import javax.swing.JTable;
//import javax.swing.table.DefaultTableCellRenderer;
//
//import modelo.proyecto.ImageCheckboxOverlay;
//import modelo.proyecto.Mensaje;
//import vista.models.ClienteTableModel;
//
///**
// * Renderizador de celdas para comentarios en las tablas de modo cliente.
// * Muestra un icono de sobre (✉) si hay comentario, con tooltip.
// * Dorado si el �ltimo mensaje es del cliente, gris si es del fot�grafo.
// * En filas padre muestra el texto directamente (sin icono de sobre).
// */
//public class CommentCellRenderer extends DefaultTableCellRenderer {
//
//    private static final long serialVersionUID = 1L;
//
//    private static final Color GOLD = new Color(200, 160, 0);
//    private static final Color GRAY = new Color(160, 160, 160);
//    private static final Color BLUE_MSG = new Color(70, 130, 180);
//
//    @Override
//    public Component getTableCellRendererComponent(JTable table, Object value,
//                                                   boolean isSelected, boolean hasFocus, int row, int column) {
//        JLabel label = (JLabel) super.getTableCellRendererComponent(
//                table, value, isSelected, hasFocus, row, column);
//
//        String text = (value != null) ? value.toString() : "";
//        label.setHorizontalAlignment(JLabel.CENTER);
//
//        if (table.getModel() instanceof ClienteTableModel ctm) {
//            int modelRow = table.convertRowIndexToModel(row);
//            // Filas padre: mostrar texto directamente
//            if (ctm.isParentRow(modelRow)) {
//                label.setText(text);
//                label.setToolTipText(null);
//                if (!isSelected && !text.isEmpty()) {
//                    label.setForeground(BLUE_MSG);
//                } else if (text.isEmpty()) {
//                    label.setForeground(null);
//                }
//                return label;
//            }
//        }
//
//        // Filas hija: comportamiento actual con icono de sobre
//        if (!text.trim().isEmpty()) {
//            label.setText("\u2709");
//            label.setToolTipText(text);
//
//            boolean esCliente = false;
//            if (table.getModel() instanceof ClienteTableModel ctm) {
//                int modelRow = table.convertRowIndexToModel(row);
//                ImageCheckboxOverlay cb = ctm.getCheckbox(modelRow);
//                if (cb != null) {
//                    List<Mensaje> thread = cb.getCommentThread();
//                    if (!thread.isEmpty() && "cliente".equals(thread.get(thread.size() - 1).de())) {
//                        esCliente = true;
//                    }
//                }
//            }
//            label.setForeground(esCliente && !isSelected ? GOLD : GRAY);
//        } else {
//            label.setText("");
//            label.setToolTipText(null);
//            label.setForeground(null);
//        }
//        return label;
//    } // --- Fin de metodo getTableCellRendererComponent ---
//
//} // --- Fin de clase CommentCellRenderer ---
