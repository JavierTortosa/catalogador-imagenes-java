package vista.panels.export;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField; // Importamos JTextField para usar su borde
import javax.swing.UIManager;

public class ProjectMetadataPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private JLabel lblProjectNameValue;
    private JTextArea areaProjectDescription;

    public ProjectMetadataPanel() {
        super(new GridBagLayout());
        initComponents();
    } // ---FIN de metodo [ProjectMetadataPanel]---

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        // Fila 0: Etiqueta "Nombre Proyecto:"
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(0, 0, 5, 10);
        add(new JLabel("Nombre Proyecto:"), gbc);

        // Fila 0: Valor del Nombre del Proyecto (JLabel estilizado como campo de texto)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        lblProjectNameValue = new JLabel(" (No guardado)");
        lblProjectNameValue.putClientProperty("FlatLaf.style", "font: bold");
        // Simulamos la apariencia de un campo de texto no editable para consistencia visual
        lblProjectNameValue.setBorder(new JTextField().getBorder()); 
        lblProjectNameValue.setOpaque(true);
        lblProjectNameValue.setBackground(UIManager.getColor("TextField.disabledBackground"));
        add(lblProjectNameValue, gbc);
        
        // Fila 1: Etiqueta "Descripción:"
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(10, 0, 5, 10);
        add(new JLabel("Descripción:"), gbc);
        
        // Fila 1: Área de texto para la Descripción
        gbc.gridx = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        areaProjectDescription = new JTextArea(5, 20);
        areaProjectDescription.setLineWrap(true);
        areaProjectDescription.setWrapStyleWord(true);
        add(new JScrollPane(areaProjectDescription), gbc);
    } // ---FIN de metodo [initComponents]---

    public JLabel getProjectNameLabel() {
        return lblProjectNameValue;
    } // ---FIN de metodo [getProjectNameLabel]---

    public JTextArea getProjectDescriptionArea() {
        return areaProjectDescription;
    } // ---FIN de metodo [getProjectDescriptionArea]---

} // --- FIN de clase [ProjectMetadataPanel]---