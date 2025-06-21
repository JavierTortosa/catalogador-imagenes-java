package vista.builders;

import java.awt.GridBagLayout;
import java.awt.Font; // Importa la clase Font
import javax.swing.JLabel;
import javax.swing.JPanel;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;

public class ProjectBuilder {

    private final ComponentRegistry registry;
    private final VisorModel model;

    public ProjectBuilder(ComponentRegistry registry, VisorModel model) {
        this.registry = registry;
        this.model = model;
    }

    public JPanel buildProjectViewPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        
        JLabel placeholderLabel = new JLabel("Aquí irá la Vista de Gestión de Proyectos");
        placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.PLAIN, 24f));
        
        panel.add(placeholderLabel);
        
        // Registrar el panel principal de esta vista para futuras referencias
        registry.register("view.panel.proyectos", panel);
        
        return panel;
    }
}