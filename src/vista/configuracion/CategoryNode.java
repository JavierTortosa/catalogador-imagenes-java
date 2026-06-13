package vista.configuracion;

import javax.swing.Icon;

/**
 * Nodo que representa una categoría o una página de configuración en el árbol de navegación.
 */
public class CategoryNode {

    private final String displayName;
    private final Icon icon;
    private final ConfigurationPanel panel;
    private final boolean isLeaf;

    /**
     * Constructor para un nodo que representa una página de configuración (hoja).
     *
     * @param displayName Nombre a mostrar
     * @param icon Icono del nodo
     * @param panel Panel asociado
     */
    public CategoryNode(String displayName, Icon icon, ConfigurationPanel panel) {
        this.displayName = displayName;
        this.icon = icon;
        this.panel = panel;
        this.isLeaf = (panel != null);
    } // --- Fin del metodo/clase CategoryNode ---


    /**
     * Constructor para un nodo que representa una categoría (rama).
     *
     * @param displayName Nombre a mostrar
     * @param icon Icono del nodo
     */
    public CategoryNode(String displayName, Icon icon) {
        this(displayName, icon, null);
    } // --- Fin del metodo/clase CategoryNode ---


    /**
     * Obtiene el nombre a mostrar.
     *
     * @return Nombre para mostrar
     */
    public String getDisplayName() {
        return displayName;
    } // --- Fin del metodo/clase getDisplayName ---


    /**
     * Obtiene el icono del nodo.
     *
     * @return Icono asociado
     */
    public Icon getIcon() {
        return icon;
    } // --- Fin del metodo/clase getIcon ---


    /**
     * Obtiene el panel de configuración asociado.
     *
     * @return Panel asociado
     */
    public ConfigurationPanel getPanel() {
        return panel;
    } // --- Fin del metodo/clase getPanel ---


    /**
     * Determina si el nodo es una hoja (tiene un panel asociado).
     *
     * @return true si es hoja, false si es categoría
     */
    public boolean isLeaf() {
        return isLeaf;
    } // --- Fin del metodo/clase isLeaf ---


    /**
     * Devuelve la representación en cadena del nodo.
     *
     * @return Nombre del nodo
     */
    @Override
    public String toString() {
        return displayName;
    } // --- Fin del metodo/clase toString ---


} // --- Fin del metodo/clase CategoryNode ---
