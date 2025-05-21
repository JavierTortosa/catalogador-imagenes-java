package vista.theme;

import java.awt.Color;

//Usando un Record (Java 16+) para concisión (requiere que los campos sean finales)
//Si usas una versión anterior, crea una clase normal con campos privados finales,
//constructor y getters públicos.
public record Tema(
 String nombreInterno, // ej: "claro", "oscuro" (para config)
 String nombreDisplay, // ej: "Tema Claro", "Tema Oscuro" (para UI)
 String carpetaIconos, // ej: "black", "white"

 // Colores (añade todos los que necesites)
 Color colorFondoPrincipal,
 Color colorFondoSecundario,
 Color colorTextoPrimario,
 Color colorTextoSecundario,
 Color colorBorde,
 Color colorBordeTitulo,
 Color colorSeleccionFondo,
 Color colorSeleccionTexto,
 Color colorBotonFondo,
 Color colorBotonTexto,
 Color colorBotonFondoActivado,
 Color colorBotonFondoAnimacion,
 Color colorBordeSeleccionActiva
 // ... otros colores que definas ...
) {
 // Los Records generan automáticamente constructor, getters, equals, hashCode, toString.
 // Puedes añadir validación en el constructor canónico si es necesario:
 public Tema { // Constructor canónico compacto
     if (nombreInterno == null || nombreInterno.isBlank()) throw new IllegalArgumentException("nombreInterno no puede ser vacío");
     if (nombreDisplay == null || nombreDisplay.isBlank()) throw new IllegalArgumentException("nombreDisplay no puede ser vacío");
     if (carpetaIconos == null || carpetaIconos.isBlank()) throw new IllegalArgumentException("carpetaIconos no puede ser vacía");
     // Añadir chequeos de null para los colores si quieres ser estricto
 }
}
