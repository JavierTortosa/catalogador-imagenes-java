package vista.components;

/**
 * Represents a suggestion in the IntelliSense popup.
 * 
 * @param displayName The text to be displayed in the list (e.g., "armas ❯ escudo").
 * @param fullPath The canonical dot-notation path (e.g., "armas.escudo").
 */
public record PathResult(String displayName, String fullPath) {
    @Override
    public String toString() {
        return displayName;
    }
}
