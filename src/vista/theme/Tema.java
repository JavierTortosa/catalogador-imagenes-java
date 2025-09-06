package vista.theme;

import java.awt.Color;
import java.util.Properties;
import javax.swing.UIManager;

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
    Color colorBotonFondo,
    Color colorBotonTexto,
    Color colorBotonFondoActivado,
    Color colorBotonFondoAnimacion,
    Color colorBordeSeleccionActiva,
    Color colorLabelActivo,
    Color colorBordeActivo,
    Color colorTextoActivo,
    Color colorBarraEstadoFondo,
    Color colorBarraEstadoTexto
) {
    // Constructor para la carga inicial o temas sin .properties
    public Tema(String nombreInterno, String nombreDisplay) {
        this(nombreInterno, nombreDisplay, new Properties()); // Llama al constructor principal con Properties vacías
    } // ---FIN de metodo [Constructor Tema simple]---

    // --- INICIO DE LA CORRECCIÓN ---
    /**
     * Constructor principal que lee colores de un objeto Properties.
     * Si una clave no se encuentra en Properties, recurre a UIManager como fallback.
     * Este es ahora el ÚNICO constructor que contiene la lógica de asignación.
     */
    public Tema(String nombreInterno, String nombreDisplay, Properties customProps) {
        this(
            nombreInterno,
            nombreDisplay,
            isDarkTheme() ? "white" : "black",
            getColorFromPropsOrUIManager(customProps, "Panel.background"),
            getColorFromPropsOrUIManager(customProps, "TabbedPane.contentAreaColor"),
            getColorFromPropsOrUIManager(customProps, "Label.foreground"),
            getColorFromPropsOrUIManager(customProps, "Label.disabledForeground"),
            getColorFromPropsOrUIManager(customProps, "Component.borderColor"),
            getColorFromPropsOrUIManager(customProps, "TitledBorder.titleColor"),
            getColorFromPropsOrUIManager(customProps, "List.selectionBackground"),
            getColorFromPropsOrUIManager(customProps, "List.selectionForeground"),
            getColorFromPropsOrUIManager(customProps, "Button.selectedBackground"),
            getColorFromPropsOrUIManager(customProps, "Button.foreground"),
            getColorFromPropsOrUIManager(customProps, "Button.selectedBackground"),
            getColorFromPropsOrUIManager(customProps, "Button.hoverBackground"),
            getColorFromPropsOrUIManager(customProps, "Component.accentColor"),
            getColorFromPropsOrUIManager(customProps, "Label.foreground"),
            getColorFromPropsOrUIManager(customProps, "Component.accentColor"),
            getColorFromPropsOrUIManager(customProps, "List.selectionForeground"),
            getColorFromPropsOrUIManager(customProps, ThemeManager.KEY_STATUSBAR_BACKGROUND),
            getColorFromPropsOrUIManager(customProps, ThemeManager.KEY_STATUSBAR_FOREGROUND)
        );
    } // ---FIN de metodo [Constructor Tema con Properties]---

    /**
     * Método de ayuda para obtener un color. Primero busca en el objeto Properties.
     * Si no lo encuentra, busca en el UIManager.
     * @param props El objeto Properties con las personalizaciones.
     * @param key La clave a buscar (tanto en props como en UIManager).
     * @return El Color encontrado o null si no se encuentra en ninguna parte.
     */
    private static Color getColorFromPropsOrUIManager(Properties props, String key) {
        Object propValue = props.get(key);
        if (propValue instanceof Color) {
            return (Color) propValue;
        }
        // Fallback: si no está en props como Color, usar el UIManager.
        return UIManager.getColor(key);
    } // ---FIN de metodo [getColorFromPropsOrUIManager]---
    // --- FIN DE LA CORRECCIÓN ---

    private static boolean isDarkTheme() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) return true; 
        return (Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null)[2] < 0.5);
    } // ---FIN de metodo [isDarkTheme]---

} // --- FIN de record [Tema]---

//package vista.theme;
//
//import java.awt.Color;
//import java.util.Properties; // AÑADIDO
//
//import javax.swing.UIManager;
//
//public record Tema(
//    String nombreInterno,
//    String nombreDisplay,
//    String carpetaIconos,
//    Color colorFondoPrincipal,
//    Color colorFondoSecundario,
//    Color colorTextoPrimario,
//    Color colorTextoSecundario,
//    Color colorBorde,
//    Color colorBordeTitulo,
//    Color colorSeleccionFondo,
//    Color colorSeleccionTexto,
//    Color colorBotonFondo,
//    Color colorBotonTexto,
//    Color colorBotonFondoActivado,
//    Color colorBotonFondoAnimacion,
//    Color colorBordeSeleccionActiva,
//    Color colorLabelActivo,
//    Color colorBordeActivo,
//    Color colorTextoActivo,
//    Color colorBarraEstadoFondo,
//    Color colorBarraEstadoTexto
//) {
//    public Tema(String nombreInterno, String nombreDisplay) {
//        this(
//            nombreInterno,
//            nombreDisplay,
//            isDarkTheme() ? "white" : "black",
//            UIManager.getColor("Panel.background"),
//            UIManager.getColor("TabbedPane.contentAreaColor"),
//            UIManager.getColor("Label.foreground"),
//            UIManager.getColor("Label.disabledForeground"),
//            UIManager.getColor("Component.borderColor"),
//            UIManager.getColor("TitledBorder.titleColor"),
//            UIManager.getColor("List.selectionBackground"),
//            UIManager.getColor("List.selectionForeground"),
//            UIManager.getColor("Button.selectedBackground"),
//            UIManager.getColor("Button.foreground"),
//            UIManager.getColor("Button.selectedBackground"),
//            UIManager.getColor("Button.hoverBackground"),
//            UIManager.getColor("Component.accentColor"),
//            UIManager.getColor("Label.foreground"),
//            UIManager.getColor("Component.accentColor"),
//            UIManager.getColor("List.selectionForeground"),
//            UIManager.getColor(ThemeManager.KEY_STATUSBAR_BACKGROUND),
//            UIManager.getColor(ThemeManager.KEY_STATUSBAR_FOREGROUND)
//        );
//    }
//
//    // --- INICIO DE LA CORRECCIÓN ---
//    
//    /**
//     * Nuevo constructor que acepta un objeto Properties para inicializar los colores.
//     * Intenta leer cada color del objeto Properties. Si una clave no existe o el valor
//     * es inválido, recurre a UIManager.getColor() como fallback.
//     */
//    public Tema(String nombreInterno, String nombreDisplay, Properties customProps) {
//        this(
//            nombreInterno,
//            nombreDisplay,
//            isDarkTheme() ? "white" : "black",
//            getColorFromPropsOrUIManager(customProps, "Panel.background", "Panel.background"),
//            getColorFromPropsOrUIManager(customProps, "TabbedPane.contentAreaColor", "TabbedPane.contentAreaColor"),
//            getColorFromPropsOrUIManager(customProps, "Label.foreground", "Label.foreground"),
//            getColorFromPropsOrUIManager(customProps, "Label.disabledForeground", "Label.disabledForeground"),
//            getColorFromPropsOrUIManager(customProps, "Component.borderColor", "Component.borderColor"),
//            getColorFromPropsOrUIManager(customProps, "TitledBorder.titleColor", "TitledBorder.titleColor"),
//            getColorFromPropsOrUIManager(customProps, "List.selectionBackground", "List.selectionBackground"),
//            getColorFromPropsOrUIManager(customProps, "List.selectionForeground", "List.selectionForeground"),
//            getColorFromPropsOrUIManager(customProps, "Button.selectedBackground", "Button.selectedBackground"),
//            getColorFromPropsOrUIManager(customProps, "Button.foreground", "Button.foreground"),
//            getColorFromPropsOrUIManager(customProps, "Button.selectedBackground", "Button.selectedBackground"),
//            getColorFromPropsOrUIManager(customProps, "Button.hoverBackground", "Button.hoverBackground"),
//            getColorFromPropsOrUIManager(customProps, "Component.accentColor", "Component.accentColor"),
//            getColorFromPropsOrUIManager(customProps, "Label.foreground", "Label.foreground"),
//            getColorFromPropsOrUIManager(customProps, "Component.accentColor", "Component.accentColor"),
//            getColorFromPropsOrUIManager(customProps, "List.selectionForeground", "List.selectionForeground"),
//            getColorFromPropsOrUIManager(customProps, ThemeManager.KEY_STATUSBAR_BACKGROUND, ThemeManager.KEY_STATUSBAR_BACKGROUND),
//            getColorFromPropsOrUIManager(customProps, ThemeManager.KEY_STATUSBAR_FOREGROUND, ThemeManager.KEY_STATUSBAR_FOREGROUND)
//        );
//    } // ---FIN de metodo [Constructor Tema con Properties]---
//
//    /**
//     * Método de ayuda para obtener un color. Primero busca en el objeto Properties.
//     * Si no lo encuentra, busca en el UIManager.
//     * @param props El objeto Properties con las personalizaciones.
//     * @param propKey La clave a buscar en el archivo .properties.
//     * @param uiManagerFallbackKey La clave a usar para UIManager.getColor() si la otra falla.
//     * @return El Color encontrado o null.
//     */
//    private static Color getColorFromPropsOrUIManager(Properties props, String propKey, String uiManagerFallbackKey) {
//        if (props != null && props.containsKey(propKey)) {
//            String colorString = props.getProperty(propKey);
//            try {
//                // Color.decode puede manejar formatos como #RRGGBB, 0xRRGGBB, etc.
//                return Color.decode(colorString);
//            } catch (NumberFormatException e) {
//                System.err.println("ADVERTENCIA: Formato de color inválido en properties para la clave '" + propKey + "': " + colorString);
//            }
//        }
//        // Fallback: si no está en props o el formato es incorrecto, usar el UIManager.
//        return UIManager.getColor(uiManagerFallbackKey);
//    } // ---FIN de metodo [getColorFromPropsOrUIManager]---
//
//    // --- FIN DE LA CORRECCIÓN ---
//
//    private static boolean isDarkTheme() {
//        Color bg = UIManager.getColor("Panel.background");
//        if (bg == null) return true; 
//        return (Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null)[2] < 0.5);
//    }
//
//} // --- FIN de record [Tema] ---
//
