package vista.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import modelo.proyecto.SelectionState;

public class TristateCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(34, 139, 34);
    private static final Color RED = new Color(200, 50, 50);
    private static final Color GRAY = new Color(128, 128, 128);

    private SelectionState currentState;

    private final JLabel label;

    public TristateCellEditor() {
        label = new JLabel("", JLabel.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setOpaque(true);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentState = switch (currentState) {
                    case UNDEFINED -> SelectionState.SELECTED;
                    case SELECTED -> SelectionState.DISCARDED;
                    case DISCARDED -> SelectionState.UNDEFINED;
                };
                updateLabel();
                stopCellEditing();
            }
        });
    } // --- Fin de metodo TristateCellEditor (constructor) ---


    private void updateLabel() {
        switch (currentState) {
            case SELECTED -> {
                label.setText("\u2713");
                label.setForeground(GREEN);
                label.setToolTipText("Seleccionado");
            }
            case DISCARDED -> {
                label.setText("\u2717");
                label.setForeground(RED);
                label.setToolTipText("Descartado");
            }
            case UNDEFINED -> {
                label.setText("\u25CB");
                label.setForeground(GRAY);
                label.setToolTipText("Sin definir");
            }
        }
    } // --- Fin de metodo updateLabel ---


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                  boolean isSelected, int row, int column) {
        currentState = value instanceof SelectionState s ? s : SelectionState.UNDEFINED;
        updateLabel();
        label.setBackground(table.getSelectionBackground());
        return label;
    } // --- Fin de metodo getTableCellEditorComponent ---


    @Override
    public Object getCellEditorValue() {
        return currentState;
    } // --- Fin de metodo getCellEditorValue ---

} // --- Fin de clase TristateCellEditor ---
