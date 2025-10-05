package controlador.managers.filter;


import java.util.Objects;

/**
 * Representa una regla de filtrado única y completa.
 * Define QUÉ buscar, DÓNDE buscarlo y CÓMO aplicar la regla.
 */
public class FilterCriterion {

    // --- ENUMS ORIGINALES (SIN MODIFICAR) ---
    public enum FilterSource {
        FILENAME,
        FOLDER_PATH
    } // --- Fin del enum FilterSource ---

    public enum FilterType {
        CONTAINS,
        DOES_NOT_CONTAIN
    } // --- Fin del enum FilterType ---

    // --- NUEVOS ENUMS PARA LA UI AVANZADA ---
    public enum Logic {
        ADD("Añadir"),
        NOT("Excluir");

        private final String displayName;
        Logic(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    } // --- FIN de enum Logic ---

    public enum SourceType {
        TEXT("Texto"),
        FOLDER("Carpeta"),
        TAG("Etiqueta");

        private final String displayName;
        SourceType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    } // --- FIN de enum SourceType ---
    
    // --- CAMPOS ORIGINALES (SIN MODIFICAR) ---
    private String value;
    
    private final FilterSource source;
    private final FilterType type;
    
    // --- NUEVOS CAMPOS (ADITIVOS) ---
    private Logic logic;
    private SourceType sourceType;

    // --- CONSTRUCTOR ORIGINAL (MODIFICADO INTERNAMENTE PARA COMPATIBILIDAD) ---
    public FilterCriterion(String value, FilterSource source, FilterType type) {
        this.value = Objects.requireNonNull(value, "El valor del filtro no puede ser nulo.");
        this.source = Objects.requireNonNull(source, "La fuente del filtro no puede ser nula.");
        this.type = Objects.requireNonNull(type, "El tipo de filtro no puede ser nulo.");

        // Lógica de compatibilidad: inicializa los nuevos campos a partir de los antiguos.
        this.logic = (type == FilterType.CONTAINS) ? Logic.ADD : Logic.NOT;
        this.sourceType = (source == FilterSource.FILENAME) ? SourceType.TEXT : SourceType.FOLDER;
    } // --- FIN del constructor FilterCriterion(String, FilterSource, FilterType) ---

    // --- NUEVO CONSTRUCTOR (PARA CREAR FILTROS VACÍOS DESDE LA UI) ---
    public FilterCriterion() {
        this.value = ""; // Valor inicial vacío
        this.source = FilterSource.FILENAME; // Valor por defecto
        this.type = FilterType.CONTAINS; // Valor por defecto

        // Inicializamos los nuevos campos con valores por defecto
        this.logic = Logic.ADD;
        this.sourceType = SourceType.TEXT;
    } // --- FIN del constructor FilterCriterion() ---

    // --- GETTERS PARA CAMPOS ORIGINALES (SIN MODIFICAR) ---
    public String getValue() { return value; }
    public FilterSource getSource() { return source; }
    public FilterType getType() { return type; }

    // --- GETTERS Y SETTERS PARA LOS NUEVOS CAMPOS ---
    public Logic getLogic() { return logic; }
    public void setLogic(Logic logic) { this.logic = logic; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public void setValue(String value) {this.value = value;}
    
    // --- MÉTODOS DE VISUALIZACIÓN Y COMPARACIÓN (SIN MODIFICAR) ---
    @Override
    public String toString() {
        String typePrefix = (type == FilterType.CONTAINS) ? "[+]" : "[-]";
        String sourcePrefix = (source == FilterSource.FILENAME) ? "n:" : "c:";
        return String.format("%s %s\"%s\"", typePrefix, sourcePrefix, value);
    } // --- FIN del metodo toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterCriterion that = (FilterCriterion) o;
        return value.equals(that.value) && source == that.source && type == that.type;
    } // --- FIN del metodo equals ---

    @Override
    public int hashCode() {
        return Objects.hash(value, source, type);
    } // --- FIN del metodo hashCode ---

} // --- FIN de la clase FilterCriterion ---

