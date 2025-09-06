package vista.theme.themes;

import java.awt.Color;
import java.util.List;
import javax.swing.UIDefaults;
import com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme;

public class FlatCarbonOrangeIJTheme extends FlatCarbonIJTheme {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String NAME = "Carbon Orange";

    public static boolean setup() {
        return setup(new FlatCarbonOrangeIJTheme());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public UIDefaults getDefaults() {
        // Obtenemos todos los valores por defecto del tema padre (FlatCarbonIJTheme)
        UIDefaults defaults = super.getDefaults();

        // --- Definición de la Paleta de Colores ---
        Color accentOrange = new Color(0xE67E22);
        Color background = new Color(0x2C3E50);
        Color backgroundSecondary = new Color(0x34495E);
        Color backgroundComponents = new Color(0x3B5369);
        Color foreground = new Color(0xECF0F1);
        Color borderColor = new Color(0x4A657D);

        // --- Aplicación de colores y estilos ---
        // Creamos una lista de pares [clave, valor] para sobrescribir los valores del tema padre.
        List<Object> newDefaults = List.of(
            // Colores de Acento (Naranja)
            "Component.accentColor", accentOrange,
            "Component.focusColor", accentOrange,
            "Button.default.background", accentOrange,
            "Button.default.foreground", Color.WHITE,
            "ProgressBar.foreground", accentOrange,
            "List.selectionBackground", accentOrange,
            "List.selectionForeground", Color.WHITE,
            "Table.selectionBackground", accentOrange,
            "Table.selectionForeground", Color.WHITE,
            "Tree.selectionBackground", accentOrange,
            "Tree.selectionForeground", Color.WHITE,
            "TabbedPane.underlineColor", accentOrange,
            "Component.focusedBorderColor", accentOrange,

            // Colores de Fondo (Grises oscuros)
            "Panel.background", background,
            "windowBackground", background,
            "TabbedPane.contentAreaColor", background,
            "ToolBar.background", backgroundSecondary,
            "StatusBar.background", backgroundSecondary,
            "TitlePane.background", backgroundSecondary,
            "MenuBar.background", backgroundSecondary,
            "TableHeader.background", backgroundSecondary,

            // Fondos para componentes de entrada
            "TextField.background", backgroundComponents,
            "TextArea.background", backgroundComponents,
            "PasswordField.background", backgroundComponents,
            "ComboBox.background", backgroundComponents,
            "Spinner.background", backgroundComponents,

            // Colores de Texto
            "Label.foreground", foreground,
            "Button.foreground", foreground,
            "CheckBox.foreground", foreground,
            "RadioButton.foreground", foreground,
            "TitledBorder.titleColor", foreground,
            "TabbedPane.foreground", foreground,
            "TextField.foreground", foreground,

            // Colores de Bordes y Separadores
            "Component.borderColor", borderColor,
            "Button.borderColor", borderColor,
            "Separator.foreground", borderColor,

            // Ajustes de Estilo Adicionales
            "Component.arc", 8,
            "Button.arc", 8,
            "Component.focusWidth", 2
        );

        // Aplicamos nuestras sobrescrituras a los defaults
        defaults.putDefaults(newDefaults.toArray());

        return defaults;
    } // ---FIN de metodo [getDefaults]---

} // --- FIN de clase [FlatCarbonOrangeIJTheme]---