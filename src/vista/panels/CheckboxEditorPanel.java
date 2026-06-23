package vista.panels;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IProjectManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import vista.theme.ThemeManager;

/**
 * Panel de edición de checkboxes en modo cliente.
 * Contiene un ImageDisplayPanel con un cabecera (código de imagen)
 * y un CheckboxEditorMouseHandler para la interacción del usuario.
 */
public class CheckboxEditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CheckboxEditorPanel.class);

    private final VisorModel model;
    private final IProjectManager projectManager;
    private final ComponentRegistry registry;
    private final ImageDisplayPanel imagePanel;
    private final JLabel headerLabel;

    private final CheckboxEditorMouseHandler mouseHandler;

    public CheckboxEditorPanel(ThemeManager themeManager, VisorModel model,
                                IProjectManager projectManager, ComponentRegistry registry) {
        this.model = model;
        this.projectManager = projectManager;
        this.registry = registry;

        setLayout(new BorderLayout());
        setBackground(themeManager.getTemaActual().colorFondoSecundario());
        setBorder(BorderFactory.createTitledBorder("Editor de Checkboxes"));

        headerLabel = new JLabel(" ", SwingConstants.CENTER);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(new EmptyBorder(4, 0, 4, 0));
        add(headerLabel, BorderLayout.NORTH);

        this.imagePanel = new ImageDisplayPanel(themeManager, model);
        this.imagePanel.setProjectManager(projectManager);
        add(imagePanel, BorderLayout.CENTER);

        this.mouseHandler = new CheckboxEditorMouseHandler(
                model, projectManager, registry, imagePanel, headerLabel);
        imagePanel.addMouseListener(mouseHandler);
        imagePanel.addMouseMotionListener(mouseHandler);
    } // --- Fin de metodo CheckboxEditorPanel (constructor) ---


    public void actualizarCabecera() {
        mouseHandler.actualizarCabecera();
    } // --- Fin de metodo actualizarCabecera ---


    public void refresh() {
        mouseHandler.actualizarCabecera();
        imagePanel.repaint();
    } // --- Fin de metodo refresh ---


    public ImageDisplayPanel getImagePanel() {
        return imagePanel;
    } // --- Fin de metodo getImagePanel ---

} // --- Fin de clase CheckboxEditorPanel ---
