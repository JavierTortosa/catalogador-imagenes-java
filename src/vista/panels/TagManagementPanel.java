package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import modelo.datos.Tag;

/**
 * Un panel de UI para mostrar, añadir y eliminar tags asociados a una imagen.
 * Este panel es "controlado", lo que significa que la lógica de negocio
 * (interacción con la BBDD) es gestionada desde fuera.
 * 
 * CAMBIO: El campo de texto libre se ha reemplazado por un JComboBox editable
 * que muestra los tags existentes para evitar inconsistencias en los nombres.
 */
public class TagManagementPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JPanel tagsDisplayPanel;
    private final JComboBox<String> comboNewTag;
    private final JButton btnAddTag;
    private final JScrollPane scrollPane;

    // Callbacks para notificar al controlador
    private Consumer<String> onAddTag;
    private Consumer<Tag> onRemoveTag;

    public TagManagementPanel() {
        super(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Etiquetas de la Imagen Seleccionada"));

        // --- Panel para mostrar los tags existentes ---
        tagsDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        scrollPane = new JScrollPane(tagsDisplayPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(tagsDisplayPanel.getBackground());
        
        // --- Panel inferior para añadir nuevos tags ---
        JPanel addTagPanel = new JPanel(new BorderLayout(5, 0));
        addTagPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        // CAMBIO: JComboBox editable en lugar de JTextField libre
        comboNewTag = new JComboBox<>();
        comboNewTag.setEditable(true);
        comboNewTag.setToolTipText("Selecciona un tag existente o escribe uno nuevo");
        // Configurar el editor para que funcione como placeholder
        JTextField editorField = (JTextField) comboNewTag.getEditor().getEditorComponent();
        editorField.setColumns(15);
        
        btnAddTag = new JButton("+");
        btnAddTag.setToolTipText("Añadir etiqueta a la imagen seleccionada");
        
        addTagPanel.add(comboNewTag, BorderLayout.CENTER);
        addTagPanel.add(btnAddTag, BorderLayout.EAST);
        
        add(scrollPane, BorderLayout.CENTER);
        add(addTagPanel, BorderLayout.SOUTH);

        // --- Lógica de eventos ---
        btnAddTag.addActionListener(e -> {
            Object selected = comboNewTag.getSelectedItem();
            String tagName = (selected != null) ? selected.toString().trim() : "";
            if (!tagName.isEmpty() && onAddTag != null) {
                onAddTag.accept(tagName);
                comboNewTag.setSelectedItem(""); // Limpiar campo después de añadir
            }
        });
        
        // Permitir añadir con la tecla Enter en el editor del combo
        editorField.addActionListener(e -> btnAddTag.doClick());

    } // ---FIN de constructor [TagManagementPanel]---

    /**
     * Actualiza la lista de tags disponibles para seleccionar en el combo.
     * Se debe llamar cada vez que cambie la lista maestra de tags.
     * @param allTags La lista completa de tags existentes en la base de datos.
     */
    public void setAvailableTags(List<Tag> allTags) {
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement(""); // Primer elemento vacío para permitir escribir
        if (allTags != null) {
            for (Tag tag : allTags) {
                comboModel.addElement(tag.getNombre());
            }
        }
        comboNewTag.setModel(comboModel);
        comboNewTag.setSelectedItem(""); // Dejar el combo vacío por defecto
    } // ---FIN de metodo [setAvailableTags]---

    /**
     * Limpia el panel de tags y lo repuebla con una nueva lista de tags.
     * @param tags La lista de objetos Tag a mostrar.
     */
    public void setTags(List<Tag> tags) {
        tagsDisplayPanel.removeAll();
        if (tags == null || tags.isEmpty()) {
            JLabel noTagsLabel = new JLabel("Sin etiquetas.");
            noTagsLabel.setForeground(Color.GRAY);
            tagsDisplayPanel.add(noTagsLabel);
        } else {
            for (Tag tag : tags) {
                tagsDisplayPanel.add(createTagComponent(tag));
            }
        }
        revalidate();
        repaint();
    } // ---FIN de metodo [setTags]---
    
    /**
     * Limpia completamente el panel, mostrando un estado por defecto.
     * Útil cuando no hay ninguna imagen seleccionada.
     */
    public void clearPanel() {
        tagsDisplayPanel.removeAll();
        JLabel noSelectionLabel = new JLabel("Ninguna imagen seleccionada.");
        noSelectionLabel.setForeground(Color.GRAY);
        tagsDisplayPanel.add(noSelectionLabel);
        comboNewTag.setSelectedItem("");
        revalidate();
        repaint();
    } // ---FIN de metodo [clearPanel]---

    /**
     * Crea un componente visual para un único tag, incluyendo un botón de borrado.
     * @param tag El objeto Tag para el que se crea el componente.
     * @return Un JPanel que representa visualmente el tag.
     */
    private Component createTagComponent(Tag tag) {
        JPanel tagPanel = new JPanel(new BorderLayout(3, 0));
        tagPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true),
            BorderFactory.createEmptyBorder(1, 4, 1, 1)
        ));
        
        JLabel nameLabel = new JLabel(tag.getNombre());
        
        JButton removeButton = new JButton("x");
        removeButton.setMargin(new Insets(0, 2, 0, 2));
        removeButton.setFont(removeButton.getFont().deriveFont(Font.BOLD, 10f));
        removeButton.setFocusable(false);
        removeButton.setToolTipText("Eliminar etiqueta '" + tag.getNombre() + "'");
        removeButton.addActionListener(e -> {
            if (onRemoveTag != null) {
                onRemoveTag.accept(tag);
            }
        });

        tagPanel.add(nameLabel, BorderLayout.CENTER);
        tagPanel.add(removeButton, BorderLayout.EAST);
        
        return tagPanel;
    } // ---FIN de metodo [createTagComponent]---
    
    // --- Setters para los callbacks ---
    
    public void setOnAddTag(Consumer<String> onAddTag) {
        this.onAddTag = onAddTag;
    }

    public void setOnRemoveTag(Consumer<Tag> onRemoveTag) {
        this.onRemoveTag = onRemoveTag;
    }
    
    public JComboBox<String> getComboNewTag() {
        return comboNewTag;
    }

} // --- FIN de clase TagManagementPanel ---