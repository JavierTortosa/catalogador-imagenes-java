package vista.theme;

import java.awt.Color;

// ¡Restauramos tu record original!
public record Tema(
    String nombreInterno,
    String nombreDisplay,
    String carpetaIconos,
    Color colorFondoPrincipal,
    Color colorFondoSecundario,
    Color colorTextoPrimario,
    Color colorTextoSecundario,
    Color colorBorde,
    Color colorBordeTitulo,
    Color colorSeleccionFondo,
    Color colorSeleccionTexto,
    // --- NUEVOS CAMPOS AÑADIDOS AQUÍ ---
    Color colorBotonFondo,
    Color colorBotonTexto,
    Color colorBotonFondoActivado,
    Color colorBotonFondoAnimacion,
    Color colorBordeSeleccionActiva,
    Color colorLabelActivo,
    
    Color colorBordeActivo,
    Color colorTextoActivo
) {
	
}


