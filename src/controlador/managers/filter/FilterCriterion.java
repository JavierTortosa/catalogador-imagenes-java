package controlador.managers.filter;


import java.util.Objects;

/**
 * Representa una regla de filtrado única y completa.
 * Define QUÉ buscar, DÓNDE buscarlo y CÓMO aplicar la regla.
 */
public class FilterCriterion {

    public enum FilterSource {
        FILENAME,
        FOLDER_PATH
    } // --- Fin del enum FilterSource ---

    public enum FilterType {
        CONTAINS,
        DOES_NOT_CONTAIN
    } // --- Fin del enum FilterType ---

    private final String value;
    private final FilterSource source;
    private final FilterType type;

    public FilterCriterion(String value, FilterSource source, FilterType type) {
        this.value = Objects.requireNonNull(value, "El valor del filtro no puede ser nulo.");
        this.source = Objects.requireNonNull(source, "La fuente del filtro no puede ser nula.");
        this.type = Objects.requireNonNull(type, "El tipo de filtro no puede ser nulo.");
    } // --- Fin del constructor FilterCriterion ---

    public String getValue() { return value; }
    public FilterSource getSource() { return source; }
    public FilterType getType() { return type; }

    @Override
    public String toString() {
        String typePrefix = (type == FilterType.CONTAINS) ? "[+]" : "[-]";
        String sourcePrefix = (source == FilterSource.FILENAME) ? "n:" : "c:";
        return String.format("%s %s\"%s\"", typePrefix, sourcePrefix, value);
    } // --- Fin del método toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterCriterion that = (FilterCriterion) o;
        return value.equals(that.value) && source == that.source && type == that.type;
    } // --- Fin del método equals ---

    @Override
    public int hashCode() {
        return Objects.hash(value, source, type);
    } // --- Fin del método hashCode ---

} // --- Fin de la clase FilterCriterion ---