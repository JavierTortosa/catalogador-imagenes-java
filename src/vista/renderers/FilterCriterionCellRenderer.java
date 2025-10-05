package vista.renderers;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import controlador.managers.filter.FilterCriterion;
import vista.util.UnderlineBorder;

/**
 * Renderer para JList que muestra un FilterCriterion como una fila interactiva.
 * Utiliza JLabels con bordes de subrayado para simular componentes editables.
 */
public class FilterCriterionCellRenderer extends JPanel implements ListCellRenderer<FilterCriterion> {
    private static final long serialVersionUID = 1L;
    
    private final JLabel logicLabel;
    private final JLabel typeLabel;
    private final JLabel valueLabel;
    private final JLabel deleteLabel;

    public FilterCriterionCellRenderer(Icon deleteIcon) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setOpaque(true);

        logicLabel = new JLabel();
        // Le pasamos el color del texto del componente y un grosor de 1px
        logicLabel.setBorder(new UnderlineBorder(this.getForeground(), 1));
        
        typeLabel = new JLabel();
        typeLabel.setBorder(new UnderlineBorder(this.getForeground(), 1));

        valueLabel = new JLabel();
        valueLabel.setBorder(new UnderlineBorder(this.getForeground(), 1));
        valueLabel.setPreferredSize(new java.awt.Dimension(120, 20)); // Ancho preferido para que no se encoja

        deleteLabel = new JLabel(deleteIcon);
        deleteLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Espacio a la izquierda

        add(logicLabel);
        add(typeLabel);
        add(valueLabel);
        add(deleteLabel);
    } // ---FIN de metodo FilterCriterionCellRenderer---

    @Override
    public Component getListCellRendererComponent(JList<? extends FilterCriterion> list, FilterCriterion criterion,
            int index, boolean isSelected, boolean cellHasFocus) {
        
        // Configurar los valores de los labels a partir del objeto criterion
        logicLabel.setText(criterion.getLogic().getDisplayName());
        typeLabel.setText(criterion.getSourceType().getDisplayName());
        valueLabel.setText(criterion.getValue().isEmpty() ? "(clic para editar)" : criterion.getValue());

        // Gestionar colores de seleccion y fondo
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        
        // Aplicamos el color de texto a cada label individualmente.
        logicLabel.setForeground(getForeground());
        typeLabel.setForeground(getForeground());
        valueLabel.setForeground(getForeground());
        
        // Actualizamos el color del borde subrayado para que coincida con el texto
        logicLabel.setBorder(new UnderlineBorder(getForeground(), 1));
        typeLabel.setBorder(new UnderlineBorder(getForeground(), 1));
        valueLabel.setBorder(new UnderlineBorder(getForeground(), 1));

        return this;
    } // ---FIN de metodo getListCellRendererComponent---
    
    
    public JLabel getLogicLabel() {
        return logicLabel;
    } // ---FIN de metodo getLogicLabel---

    public JLabel getTypeLabel() {
        return typeLabel;
    } // ---FIN de metodo getTypeLabel---

    public JLabel getValueLabel() {
        return valueLabel;
    } // ---FIN de metodo getValueLabel---

    public JLabel getDeleteLabel() {
        return deleteLabel;
    } // ---FIN de metodo getDeleteLabel---
    

} // --- FIN de la clase FilterCriterionCellRenderer ---