package controlador;

// Define los posibles estados de visualización del panel de proyecto
public enum ProjectViewState {
    VIEW_SELECTION, // Foco en la lista de Selección, Grid normal
    VIEW_DISCARDS, // Foco en la lista de Descartes, Grid normal
    VIEW_EXPORT // Panel de exportación activo, Grid muestra Selección con bordes de estado
}
