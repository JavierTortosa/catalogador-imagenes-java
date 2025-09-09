package vista.config;

public record SeparatorDefinition(boolean elastic) implements ToolbarComponentDefinition {
    // Constructor de conveniencia para no tener que cambiar los antiguos
    public SeparatorDefinition() {
        this(false);
    }
}