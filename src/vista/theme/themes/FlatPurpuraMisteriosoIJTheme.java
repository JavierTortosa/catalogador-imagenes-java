package vista.theme.themes;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;

/**
 * Esta clase extiende el tema estándar 'Dark Purple' para actuar como
 * un tema personalizado independiente llamado "Púrpura Misterioso", con sus
 * propias modificaciones cargadas desde un archivo .properties.
 */
public class FlatPurpuraMisteriosoIJTheme extends FlatDarkPurpleIJTheme {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "Púrpura Misterioso";

    public static boolean setup() {
        return setup(new FlatPurpuraMisteriosoIJTheme());
    } // ---FIN de metodo [setup]---
    
    @Override
    public String getName() {
        return NAME;
    } // ---FIN de metodo [getName]---

} // --- FIN de clase [FlatPurpuraMisteriosoIJTheme]---