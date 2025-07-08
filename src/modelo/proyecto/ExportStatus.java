package modelo.proyecto;

public enum ExportStatus {
    PENDIENTE,          // Aún no se ha buscado el archivo
    ENCONTRADO_OK,      // Encontrado automáticamente y con confianza
    NO_ENCONTRADO,      // No se encontró ningún candidato
    ASIGNADO_MANUAL,    // El usuario lo ha localizado a mano
    SUGERENCIA,         // Encontrado un candidato probable, pero no 100% seguro
    MULTIPLES_CANDIDATOS, // Se encontraron varios posibles archivos
    COPIANDO,           // Proceso de copia en curso
    COPIADO_OK,         // Copia finalizada con éxito
    ERROR_COPIA         // Fallo durante la copia
} // --- FIN del enum ExportStatus ---