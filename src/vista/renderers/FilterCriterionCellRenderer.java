package vista.renderers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import controlador.managers.filter.FilterCriterion;

public class FilterCriterionCellRenderer extends JPanel implements ListCellRenderer<FilterCriterion> {
    private static final long serialVersionUID = 5L; // Nueva versión
    
    private final JLabel logicLabel;
    private final JLabel typeLabel;
    private final JLabel valueLabel;
    private final JLabel deleteLabel;

    private final Map<FilterCriterion.Logic, Icon> logicIcons;
    private final Map<FilterCriterion.SourceType, Icon> typeIcons;

    public FilterCriterionCellRenderer(Map<FilterCriterion.Logic, Icon> logicIcons,
                                       Map<FilterCriterion.SourceType, Icon> typeIcons,
                                       Icon deleteIcon) {
        
        this.logicIcons = logicIcons;
        this.typeIcons = typeIcons;

        setLayout(new BorderLayout(5, 0));
        setOpaque(true);
        setBorder(new EmptyBorder(2, 2, 2, 2));

        logicLabel = new JLabel();
        typeLabel = new JLabel();
        deleteLabel = new JLabel(deleteIcon);
        
        JPanel leftIconsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftIconsPanel.setOpaque(false);
        leftIconsPanel.add(logicLabel);
        leftIconsPanel.add(typeLabel);

        valueLabel = new JLabel();
        valueLabel.setBorder(new CompoundBorder(
            UIManager.getBorder("TextField.border"),
            new EmptyBorder(0, 4, 0, 4)
        ));
        valueLabel.setOpaque(true);

        add(leftIconsPanel, BorderLayout.WEST);
        add(valueLabel, BorderLayout.CENTER);
        add(deleteLabel, BorderLayout.EAST);
    } // ---FIN de metodo FilterCriterionCellRenderer---

    @Override
    public Component getListCellRendererComponent(JList<? extends FilterCriterion> list, FilterCriterion criterion,
            int index, boolean isSelected, boolean cellHasFocus) {

        logicLabel.setIcon(logicIcons.get(criterion.getLogic()));
        logicLabel.setToolTipText(criterion.getLogic().getDisplayName());
        
        typeLabel.setIcon(typeIcons.get(criterion.getSourceType()));
        typeLabel.setToolTipText(criterion.getSourceType().getDisplayName());
        
        valueLabel.setText(criterion.getValue());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            valueLabel.setBackground(list.getSelectionBackground());
            valueLabel.setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            valueLabel.setBackground(UIManager.getColor("TextField.background"));
            valueLabel.setForeground(UIManager.getColor("TextField.foreground"));
        }

        return this;
    } // ---FIN de metodo getListCellRendererComponent---
    
    public JLabel getLogicLabel() { return logicLabel; } // ---FIN de metodo getLogicLabel---
    public JLabel getTypeLabel() { return typeLabel; } // ---FIN de metodo getTypeLabel---
    public JLabel getDeleteLabel() { return deleteLabel; } // ---FIN de metodo getDeleteLabel---
    public JLabel getValueLabel() { return valueLabel; } // ---FIN de metodo getValueLabel---

} // --- FIN de la clase FilterCriterionCellRenderer ---