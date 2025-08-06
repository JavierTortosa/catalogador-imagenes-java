package vista.panels;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import modelo.VisorModel;
import servicios.image.ThumbnailService;
import vista.renderers.GridCellRenderer;
import vista.theme.ThemeManager;
import vista.util.IconUtils;
import vista.util.ThumbnailPreviewer; // <--- 1. IMPORTAMOS LA CLASE

/**
 * Panel de visualización que muestra imágenes en una cuadrícula.
 * REUTILIZA ThumbnailPreviewer para mostrar un previsualizador en doble clic.
 */
@SuppressWarnings("serial")
public class GridDisplayPanel extends JPanel {

    private final JList<String> gridList;
    private final DefaultListModel<String> listModel;

    public GridDisplayPanel(VisorModel model, ThumbnailService thumbnailService, ThemeManager themeManager, IconUtils iconUtils) {
        super(new BorderLayout());
        
        this.listModel = new DefaultListModel<>();
        this.gridList = new JList<>(listModel);

        // --- Configuración del JList (sin cambios) ---
        gridList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        gridList.setVisibleRowCount(-1);
        gridList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        GridCellRenderer cellRenderer = new GridCellRenderer(thumbnailService, model, themeManager, iconUtils);
        gridList.setCellRenderer(cellRenderer);

        gridList.setFixedCellWidth(150);
        gridList.setFixedCellHeight(180);
        gridList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
        
        JScrollPane scrollPane = new JScrollPane(gridList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        this.add(scrollPane, BorderLayout.CENTER);
        
        // =========================================================================
        // === LA SOLUCIÓN: INSTANCIAR Y "CONECTAR" EL PREVISUALIZADOR ===
        // =========================================================================
        
        // 2. ¡Y YA ESTÁ! Creamos una instancia de ThumbnailPreviewer y le pasamos
        //    nuestra JList del grid. Él se encargará de añadir sus propios listeners.
        new ThumbnailPreviewer(this.gridList, model, themeManager);
        
        // =========================================================================
        
    } // --- Fin del constructor GridDisplayPanel ---

    /**
     * Rellena el grid con una nueva lista de identificadores de imagen.
     * @param imageKeys La lista de claves de imagen a mostrar.
     */
    public void setImageKeys(List<String> imageKeys) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            if (imageKeys != null) {
                listModel.addAll(imageKeys);
            }
        });
    } // --- Fin del método setImageKeys ---
    
    /**
     * Expone la JList interna para que los controladores puedan interactuar con ella.
     * @return La JList que conforma el grid.
     */
    public JList<String> getGridList() {
        return gridList;
    } // --- Fin del método getGridList ---
    
    
    /**
     * Actualiza el color de fondo de la JList interna basándose en el tema
     * actualmente activo en el ThemeManager.
     */
    public void actualizarColorDeFondoPorTema(ThemeManager themeManager) {
        if (themeManager != null && gridList != null) {
            gridList.setBackground(themeManager.getTemaActual().colorFondoSecundario());
            System.out.println("  -> GridDisplayPanel (JList interna) actualizado al color de fondo del nuevo tema.");
        }
    }
    

} // --- Fin de la clase GridDisplayPanel ---

