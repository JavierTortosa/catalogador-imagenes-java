package vista.theme;

import java.awt.Color;

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
 public Tema { // Constructor canónico compacto
     if (nombreInterno == null || nombreInterno.isBlank()) throw new IllegalArgumentException("nombreInterno no puede ser vacío");
     if (nombreDisplay == null || nombreDisplay.isBlank()) throw new IllegalArgumentException("nombreDisplay no puede ser vacío");
     if (carpetaIconos == null || carpetaIconos.isBlank()) throw new IllegalArgumentException("carpetaIconos no puede ser vacía");
     // Añadir chequeos de null para los colores si quieres ser estricto
 }
}
