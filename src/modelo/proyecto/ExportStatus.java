package modelo.proyecto;

public enum ExportStatus {
    // Definición de cada estado con su nombre de icono y su tooltip asociado.
    PENDIENTE("status-pending.png", "Buscando archivo asociado..."),
    ENCONTRADO_OK("status-ok.png", "Archivo comprimido encontrado y listo para exportar."),
    NO_ENCONTRADO("status-warning.png", "No se encontró un archivo comprimido. Clic derecho para más opciones."),
    ASIGNADO_MANUAL("status-user.png", "Archivo comprimido asignado manualmente por el usuario."),
    MULTIPLES_CANDIDATOS("status-warning.png", "Se encontraron varios archivos. Clic derecho para seleccionar el correcto."),
    IGNORAR_COMPRIMIDO("status-ignore.png", "Se ignora el archivo comprimido. Solo se exportará la imagen."),
    IMAGEN_NO_ENCONTRADA("status-error.png", "¡ERROR! La imagen original no se encuentra en su ruta. Clic derecho para relocalizar."),
    COPIANDO("status-copying.png", "Copiando archivos..."),
    COPIADO_OK("status-ok.png", "Archivos copiados con éxito."),
    ERROR_COPIA("status-error.png", "Error durante la copia de archivos.");

    // --- Campos para almacenar los nuevos datos ---
    private final String iconName;
    private final String tooltip;

    /**
     * Constructor privado del enum.
     * @param iconName El nombre del archivo de imagen del icono (ej. "status-ok.png").
     * @param tooltip El texto que se mostrará cuando el ratón se pose sobre el icono.
     */
    ExportStatus(String iconName, String tooltip) {
        this.iconName = iconName;
        this.tooltip = tooltip;
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

} // --- FIN del enum ExportStatus ---

