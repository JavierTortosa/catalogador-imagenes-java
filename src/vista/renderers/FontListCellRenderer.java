package vista.renderers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/**
 * Renderizador personalizado para el ComboBox de fuentes del Editor Avanzado.
 * Muestra el nombre de la fuente en tipografía estándar y un texto de muestra
 * renderizado con la propia fuente seleccionada, al estilo Photoshop.
 */
public class FontListCellRenderer extends JPanel implements ListCellRenderer<String> {

    private static final long serialVersionUID = 1L;

    private final JLabel labelName;
    private final JLabel labelSample;

    public FontListCellRenderer() {
        super(new BorderLayout(8, 0));
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        setPreferredSize(new Dimension(280, 24));

        labelName = new JLabel();
        labelName.setHorizontalAlignment(SwingConstants.LEFT);

        labelSample = new JLabel("Muestra Abc");
        labelSample.setHorizontalAlignment(SwingConstants.RIGHT);

        add(labelName, BorderLayout.WEST);
        add(labelSample, BorderLayout.EAST);
    } // --- Fin del constructor FontListCellRenderer ---


    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value,
            int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            labelName.setForeground(list.getSelectionForeground());
            labelSample.setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            labelName.setForeground(list.getForeground());
            labelSample.setForeground(list.getForeground());
        }

        if (value != null) {
            labelName.setText(value);
            try {
                Font sampleFont = new Font(value, Font.PLAIN, 14);
                labelSample.setFont(sampleFont);
            } catch (Exception ignored) {
                labelSample.setFont(list.getFont());
            }
        } else {
            labelName.setText("");
            labelSample.setText("");
        }

        return this;
    } // --- Fin del metodo getListCellRendererComponent ---

} // --- Fin de la clase FontListCellRenderer ---
