package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class MultiLineCellRenderer extends JPanel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;
    private JLabel label;
    private JButton visualIndicatorButton; // Ahora es solo un indicador visual

    public MultiLineCellRenderer(Action toggleDetailsAction) {
        super(new BorderLayout(5, 0));
        setBorder(null);

        label = new JLabel();
        visualIndicatorButton = new JButton();
        
        if (toggleDetailsAction != null && toggleDetailsAction.getValue(Action.SMALL_ICON) != null) {
            visualIndicatorButton.setIcon((Icon) toggleDetailsAction.getValue(Action.SMALL_ICON));
        }
        
        visualIndicatorButton.setMargin(new Insets(0, 0, 0, 0));
        visualIndicatorButton.setContentAreaFilled(false);
        visualIndicatorButton.setBorderPainted(false);
        visualIndicatorButton.setFocusPainted(false);
        
        add(label, BorderLayout.CENTER);
        add(visualIndicatorButton, BorderLayout.EAST);
    } // ---FIN de metodo [MultiLineCellRenderer]---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }

        String displayText;
        boolean buttonVisible = false;

        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Path> files = (List<Path>) value;

            if (files.isEmpty()) {
                displayText = "- Ninguno -";
            } else if (files.size() == 1) {
                displayText = files.get(0).getFileName().toString();
            } else {
                displayText = String.format("%s (y %d más...)", files.get(0).getFileName().toString(), files.size() - 1);
                buttonVisible = true;
            }
        } else {
            displayText = (value != null) ? value.toString() : "";
        }
        
        label.setText(displayText);
        visualIndicatorButton.setVisible(buttonVisible);
        
        return this;
    } // ---FIN de metodo [getTableCellRendererComponent]---

} // --- FIN de clase [MultiLineCellRenderer]---

//package vista.panels.export;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.Insets;
//import java.nio.file.Path;
//import java.util.List;
//import javax.swing.Action;
//import javax.swing.Icon;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JTable;
//import javax.swing.table.TableCellRenderer;
//
//public class MultiLineCellRenderer extends JPanel implements TableCellRenderer {
//
//    private static final long serialVersionUID = 1L;
//    private JLabel label;
//    private JButton button; // Guardamos la referencia para que el listener pueda medirlo
//
//    // --- ESTE ES EL CONSTRUCTOR CORRECTO QUE FALTABA ---
//    public MultiLineCellRenderer(Action toggleDetailsAction) {
//        super(new BorderLayout(5, 0));
//        setBorder(null);
//
//        label = new JLabel();
//        button = new JButton();
//        
//        // Asignamos el icono directamente desde la Action que nos pasan
//        if (toggleDetailsAction != null && toggleDetailsAction.getValue(Action.SMALL_ICON) != null) {
//            button.setIcon((Icon) toggleDetailsAction.getValue(Action.SMALL_ICON));
//        }
//        
//        // Configuramos el botón para que sea solo un icono
//        button.setMargin(new Insets(0, 0, 0, 0));
//        button.setContentAreaFilled(false);
//        button.setBorderPainted(false);
//        button.setFocusPainted(false);
//        
//        // Añadimos los componentes al panel
//        add(label, BorderLayout.CENTER);
//        add(button, BorderLayout.EAST);
//    } // ---FIN de metodo [MultiLineCellRenderer]---
//
//    @Override
//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        // Establecemos los colores de la celda
//        if (isSelected) {
//            setBackground(table.getSelectionBackground());
//            label.setForeground(table.getSelectionForeground());
//        } else {
//            setBackground(table.getBackground());
//            label.setForeground(table.getForeground());
//        }
//
//        String displayText;
//        boolean buttonVisible = false;
//
//        // Determinamos el texto y si el botón debe ser visible
//        if (value instanceof List) {
//            @SuppressWarnings("unchecked")
//            List<Path> files = (List<Path>) value;
//
//            if (files.isEmpty()) {
//                displayText = "- Ninguno -";
//            } else if (files.size() == 1) {
//                displayText = files.get(0).getFileName().toString();
//            } else {
//                displayText = String.format("%s (y %d más...)", files.get(0).getFileName().toString(), files.size() - 1);
//                buttonVisible = true;
//            }
//        } else {
//            displayText = (value != null) ? value.toString() : "";
//        }
//        
//        // Actualizamos el contenido de nuestros componentes
//        label.setText(displayText);
//        button.setVisible(buttonVisible);
//        
//        return this;
//    } // ---FIN de metodo [getTableCellRendererComponent]---
//    
//    // Método para que el listener externo pueda acceder al botón
//    public JButton getButton() {
//        return button;
//    } // ---FIN de metodo [getButton]---
//
//} // --- FIN de clase [MultiLineCellRenderer]---