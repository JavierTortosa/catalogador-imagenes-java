package vista.theme.themes;

import java.awt.Color;
import java.util.List;
import javax.swing.UIDefaults;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

public class FlatObsidianOrangeIJTheme extends FlatMacDarkLaf {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String NAME = "Obsidian Orange";

    public static boolean setup() {
        return setup(new FlatObsidianOrangeIJTheme());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public UIDefaults getDefaults() {
        // Obtenemos todos los valores por defecto del tema padre (FlatMacDarkLaf)
        UIDefaults defaults = super.getDefaults();

        // --- Definición de la Paleta de Colores ---
        // Aquí definimos nuestros colores como objetos Color de Java
        Color accentColor = new Color(0xD66825);
        Color focusedColor = new Color(0xF0782B);
        Color selectionBackground = accentColor;
        Color selectionForeground = Color.WHITE;
        Color background = new Color(0x1A1B26);
        Color secondaryBackground = new Color(0x24283B);
        Color thirdBackground = new Color(0x2A2F41);
        Color foreground = new Color(0xC0CAF5);
        Color disabledForeground = new Color(0x565F89);
        Color borderColor = new Color(0x414868);
        Color separatorColor = new Color(0x414868);

        // --- Aplicación de colores a componentes específicos ---
        // Creamos una lista de pares [clave, valor] para sobrescribir los valores del tema padre.
        // FlatLaf usará estos nuevos colores base para recalcular todos los colores derivados.
        List<Object> newDefaults = List.of(
            // General
            "Panel.background", background,
            "windowBackground", background,
            "ToolBar.background", secondaryBackground,
            "MenuBar.background", secondaryBackground,
            "TitlePane.background", secondaryBackground,
            "TabbedPane.contentAreaColor", thirdBackground,

            // Botones
            "Button.arc", 8,
            "Button.default.background", accentColor,
            "Button.default.foreground", Color.WHITE,

            // Listas, Tablas, Árboles
            "List.selectionBackground", selectionBackground,
            "List.selectionForeground", selectionForeground,
            "Table.selectionBackground", selectionBackground,
            "Table.selectionForeground", selectionForeground,
            "Tree.selectionBackground", selectionBackground,
            "Tree.selectionForeground", selectionForeground,

            // Campos de Texto y ComboBox
            "TextField.background", thirdBackground,
            "TextArea.background", thirdBackground,
            "ComboBox.background", thirdBackground,

            // Bordes y Separadores
            "Component.borderColor", borderColor,
            "Separator.color", separatorColor,
            
            // Foco y Acento
            "Component.focusColor", accentColor,
            "Component.focusedBorderColor", focusedColor,
            "Component.accentColor", accentColor
        );

        // Aplicamos nuestras sobrescrituras a los defaults
        defaults.putDefaults(newDefaults.toArray());

        return defaults;
    } // ---FIN de metodo [getDefaults]---
    
} // --- FIN de clase [FlatObsidianOrangeIJTheme]---



