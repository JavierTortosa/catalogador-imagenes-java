package vista.theme.themes;

import com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme;

/**
 * Esta clase extiende el tema estándar 'Gradianto Midnight Blue' para actuar como
 * un tema personalizado independiente llamado "Azul Medianoche".
 * FlatLaf encontrará y aplicará automáticamente el archivo .properties
 * con el mismo nombre que esta clase.
 */
public class FlatAzulMedianochePersonalizadoIJTheme extends FlatGradiantoMidnightBlueIJTheme {

    private static final long serialVersionUID = 1L;
    // El nombre que se muestra en el menú es "Azul Medianoche"
    public static final String NAME = "Azul Medianoche"; 

    public static boolean setup() {
        return setup(new FlatAzulMedianochePersonalizadoIJTheme());
    } // ---FIN de metodo setup---

    @Override
    public String getName() {
        return NAME;
    } // ---FIN de metodo getName---

} // --- FIN de clase FlatAzulMedianochePersonalizadoIJTheme---