package vista.panels.export;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.EventObject;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class MultiFileCellRendererAndEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    private static final long serialVersionUID = 1L;
    private JPanel panel;
    private JLabel label;
    private JButton button;
    private Action toggleDetailsAction;

    public MultiFileCellRendererAndEditor(Action toggleDetailsAction) {
        this.toggleDetailsAction = toggleDetailsAction;
        
        panel = new JPanel(new BorderLayout(5, 0)); // Añadimos un pequeño espacio horizontal
        panel.setBorder(null);

        label = new JLabel();
        button = new JButton();
        
        if (toggleDetailsAction != null && toggleDetailsAction.getValue(Action.SMALL_ICON) != null) {
            button.setIcon((Icon) toggleDetailsAction.getValue(Action.SMALL_ICON));
        }
        
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (MultiFileCellRendererAndEditor.this.toggleDetailsAction != null) {
                    MultiFileCellRendererAndEditor.this.toggleDetailsAction.actionPerformed(e);
                }
                fireEditingStopped();
            }
        });

        panel.add(label, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
    } // ---FIN de metodo [MultiFileCellRendererAndEditor]---

    private void updateData(Object value, JTable table, boolean isSelected) {
        if (isSelected) {
            panel.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            panel.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }

        String displayText = "";
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
        button.setVisible(buttonVisible);
    } // ---FIN de metodo [updateData]---

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        updateData(value, table, isSelected);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        updateData(value, table, isSelected);
        // Cuando la edición empieza, simulamos el clic en el botón inmediatamente.
        button.doClick();
        return panel;
    }

    // Este método ya no necesita una lógica compleja.
    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

} // --- FIN de clase [MultiFileCellRendererAndEditor]---