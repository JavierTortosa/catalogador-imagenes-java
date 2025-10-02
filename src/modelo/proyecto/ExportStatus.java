package modelo.proyecto;

public enum ExportStatus {
    // Definición de cada estado con su nombre de icono y su tooltip asociado.
	// Estados neutrales (color de fondo por defecto)
    PENDIENTE("status-pending.png", "Buscando archivo asociado...", null), // null para usar color por defecto
    COPIANDO("status-copying.png", "Copiando archivos...", null),
    
    // Estados OK (Verde)
    ENCONTRADO_OK("status-ok.png", "Archivo comprimido encontrado y listo para exportar.", new java.awt.Color(34, 139, 34)),
    ASIGNADO_MANUAL("status-user.png", "Archivo comprimido asignado manualmente por el usuario.", new java.awt.Color(34, 139, 34)),
    COPIADO_OK("status-ok.png", "Archivos copiados con éxito.", new java.awt.Color(34, 139, 34)),
    
    // Estados de Advertencia (Amarillo)
    MULTIPLES_CANDIDATOS("status-warning.png", "Se encontraron varios archivos. Clic derecho para seleccionar el correcto.", new java.awt.Color(255, 255, 180)),
    IGNORAR_COMPRIMIDO("status-ignore.png", "Se ignora el archivo comprimido. Solo se exportará la imagen.", new java.awt.Color(255, 255, 180)),
    NO_ENCONTRADO("status-warning.png", "No se encontró un archivo comprimido. Clic derecho para más opciones.", new java.awt.Color(255, 255, 180)),
    
    // Estados de Error (Rojo)
    IMAGEN_NO_ENCONTRADA("status-error.png", "¡ERROR! La imagen original no se encuentra en su ruta. Clic derecho para relocalizar.", new java.awt.Color(220, 50, 50)),
    ERROR_COPIA("status-error.png", "Error durante la copia de archivos.", new java.awt.Color(220, 50, 50)),
    NOMBRE_DUPLICADO("status-error.png", "Conflicto: ya existe otro archivo con este mismo nombre en la cola de exportación.", new java.awt.Color(220, 50, 50)),
    ASIGNADO_DUPLICADO("status-error.png", "Conflicto: otro archivo diferente será exportado con este mismo nombre, causando sobreescritura.", new java.awt.Color(220, 50, 50))
    ;
	

    // --- Campos para almacenar los nuevos datos ---
    private final String iconName;
    private final String tooltip;
    private final java.awt.Color color;

    /**
     * Constructor privado del enum.
     * @param iconName El nombre del archivo de imagen del icono (ej. "status-ok.png").
     * @param tooltip El texto que se mostrará cuando el ratón se pose sobre el icono.
     * @param color El color del estado
     */
    ExportStatus(String iconName, String tooltip, java.awt.Color color) {
        this.iconName = iconName;
        this.tooltip = tooltip;
        this.color = color;
    } // --- Fin del constructor ExportStatus ---

    /**
     * Devuelve el nombre del archivo del icono asociado a este estado.
     * @return El nombre del icono.
     */
    public String getIconName() {
        return iconName;
    } // --- Fin del método getIconName ---

    /**
     * Devuelve el texto descriptivo (tooltip) para este estado.
     * @return El texto del tooltip.
     */
    public String getTooltip() {
        return tooltip;
    } // --- Fin del método getTooltip ---
    
    /**
     * Devuelve el color de fondo asociado a este estado.
     * @return El color del estado. Puede ser null si se debe usar el color por defecto.
     */
    public java.awt.Color getColor() {
        return color;
    } // --- Fin del método getColor ---

} // --- FIN del enum ExportStatus ---

