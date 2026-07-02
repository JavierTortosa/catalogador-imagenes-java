package vista.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import modelo.datos.Tag;

/**
 * Un panel de UI para mostrar, añadir y eliminar tags asociados a una imagen.
 * Este panel es "controlado", lo que significa que la lógica de negocio
 * (interacción con la BBDD) es gestionada desde fuera.
 */
public class TagManagementPanel extends JPanel {

    private static final long serialVersionUID = 2L;
    private final JList<Tag> tagsList;
    private final JScrollPane scrollPane;
    private List<Tag> currentTags = new java.util.ArrayList<>();
    private List<Tag> allTagsBackup = new java.util.ArrayList<>();

    public TagManagementPanel() {
        super(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Etiquetas de la Imagen Seleccionada"));
        
        // --- Panel para mostrar los tags existentes ---
        tagsList = new JList<Tag>(new javax.swing.DefaultListModel<>());
        tagsList.setCellRenderer(new TagListCellRenderer());
        tagsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        scrollPane = new JScrollPane(tagsList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(tagsList.getBackground());
        
        add(scrollPane, BorderLayout.CENTER);
    } // ---FIN de constructor [TagManagementPanel]---

    /**
     * Actualiza la lista de tags disponibles para seleccionar en el combo.
     * Se debe llamar cada vez que cambie la lista maestra de tags.
     * @param allTags La lista completa de tags existentes en la base de datos.
     */
    public void setAvailableTags(List<Tag> allTags) {
        // Este método ahora es responsabilidad del DataBuilder/DataController
        // ya que el JComboBox se ha movido a la toolbar.
    } // ---FIN de metodo [setAvailableTags]---

    /**
     * Limpia el panel de tags y lo repuebla con una nueva lista de tags.
     * @param tags La lista de objetos Tag a mostrar.
     */
    public void setTags(List<Tag> tags) {
        this.allTagsBackup = (tags != null) ? new java.util.ArrayList<>(tags) : new java.util.ArrayList<>();
        this.currentTags = new java.util.ArrayList<>(this.allTagsBackup);
        refreshListModel();
        revalidate();
        repaint();
    } // ---FIN de metodo [setTags]---
    
    /**
     * Limpia completamente el panel, mostrando un estado por defecto.
     * Útil cuando no hay ninguna imagen seleccionada.
     */
    public void clearPanel() {
        this.allTagsBackup.clear();
        this.currentTags.clear();
        ((javax.swing.DefaultListModel<Tag>) tagsList.getModel()).clear();
        revalidate();
        repaint();
    } // ---FIN de metodo [clearPanel]---

    /**
     * Filtra la lista mostrando solo tags cuyo nombre contenga el texto (ignore case).
     */
    public void filter(String text) {
        if (text == null || text.trim().isEmpty()) {
            clearFilter();
            return;
        }
        String lower = text.toLowerCase().trim();
        this.currentTags.clear();
        for (Tag tag : allTagsBackup) {
            if (tag.getNombre().toLowerCase().contains(lower)) {
                this.currentTags.add(tag);
            }
        }
        refreshListModel();
    }

    /**
     * Restaura la lista completa de tags (quita el filtro).
     */
    public void clearFilter() {
        this.currentTags = new java.util.ArrayList<>(this.allTagsBackup);
        refreshListModel();
    }

    /**
     * Busca la siguiente ocurrencia del texto (ignore case) a partir de la selección actual.
     * Hace wrap-around si llega al final.
     * @return true si encontró match, false si no.
     */
    public boolean findNext(String text) {
        if (text == null || text.trim().isEmpty() || currentTags.isEmpty()) return false;
        String lower = text.toLowerCase().trim();
        int start = tagsList.getSelectedIndex();
        if (start < 0) start = -1;

        for (int i = start + 1; i < currentTags.size(); i++) {
            if (currentTags.get(i).getNombre().toLowerCase().contains(lower)) {
                tagsList.setSelectedIndex(i);
                tagsList.ensureIndexIsVisible(i);
                return true;
            }
        }
        // Wrap around
        for (int i = 0; i <= start; i++) {
            if (currentTags.get(i).getNombre().toLowerCase().contains(lower)) {
                tagsList.setSelectedIndex(i);
                tagsList.ensureIndexIsVisible(i);
                return true;
            }
        }
        return false;
    }

    public JList<Tag> getTagsList() {
        return tagsList;
    }

    private void refreshListModel() {
        javax.swing.DefaultListModel<Tag> model = (javax.swing.DefaultListModel<Tag>) tagsList.getModel();
        model.clear();
        for (Tag tag : currentTags) {
            model.addElement(tag);
        }
    }

    /**
     * Añade un tag localmente para dar feedback inmediato.
     * @param tagName El nombre del tag.
     */
    public void addTagLocally(String tagName) {
        Tag dummyTag = new Tag(-1, tagName);
        currentTags.add(dummyTag);
        ((javax.swing.DefaultListModel<Tag>) tagsList.getModel()).addElement(dummyTag);
    }

    /**
     * Obtiene el tag actualmente seleccionado en la lista.
     * @return El objeto Tag seleccionado, o null si no hay ninguno.
     */
    public Tag getSelectedTag() {
        return tagsList.getSelectedValue();
    }

    /**
     * Crea un renderizador de celdas para la lista de tags.
     */
    private class TagListCellRenderer implements ListCellRenderer<Tag> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Tag> list, Tag value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JPanel tagPanel = new JPanel(new BorderLayout(3, 0));
            tagPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? list.getSelectionForeground() : Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(1, 4, 1, 1)
            ));
            
            if (isSelected) {
                tagPanel.setBackground(list.getSelectionBackground());
                tagPanel.setForeground(list.getSelectionForeground());
            } else {
                tagPanel.setBackground(list.getBackground());
                tagPanel.setForeground(list.getForeground());
            }

            JLabel nameLabel = new JLabel(value.getNombre());
            if (!isSelected) {
                if (value.isReadOnly()) {
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN));
                    Color accent = javax.swing.UIManager.getColor("Component.accentColor");
                    if (accent == null) accent = new Color(50, 150, 255);
                    nameLabel.setForeground(accent);
                } else {
                    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
                    nameLabel.setForeground(javax.swing.UIManager.getColor("Label.foreground"));
                }
            }
            tagPanel.add(nameLabel, BorderLayout.CENTER);

            // Botón de borrado rápido (opcional, ya que tenemos el botón en la toolbar)
            // Pero el usuario lo pedía en el panel también si quería.
            // Para evitar duplicidad con la toolbar, lo dejaremos solo como visual.
            // Si queremos que funcione, hay que añadir un MouseListener al JList.

            return tagPanel;
        }
    }
    
} // --- FIN de clase TagManagementPanel ---