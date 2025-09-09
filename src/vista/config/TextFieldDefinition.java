package vista.config;

import java.util.Objects;

/**
 * Define la estructura de un campo de texto (JTextField) en una barra de herramientas.
 */
public record TextFieldDefinition(
    String comandoCanonico, // Identificador único para el registro y acciones
    String textoPorDefecto,   // Texto que aparecerá inicialmente
    int columns
) implements ToolbarComponentDefinition {

    // Constructor canónico para validar
    public TextFieldDefinition {
        Objects.requireNonNull(comandoCanonico, "comandoCanonico no puede ser nulo");
        Objects.requireNonNull(textoPorDefecto, "textoPorDefecto no puede ser nulo");
    }
}