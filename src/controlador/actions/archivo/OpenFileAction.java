package controlador.actions.archivo;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class OpenFileAction extends BaseVisorAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OpenFileAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de la superclase
        // El texto "Abrir Archivo..." se usa para el menú
        super("Abrir Archivo...", controller);

        // Establece descripción (tooltip)
        putValue(Action.SHORT_DESCRIPTION, "Abrir una nueva carpeta de imágenes");

        // --- ¡LA PARTE IMPORTANTE! Usa IconUtils ---
        // Llama a getScaledIcon con el nombre del icono y los tamaños recibidos.
        // Verifica el nombre del archivo: ¿es 'Selector_de_Carpetas' o 'selector_de_carpetas'?
        // Usa el nombre que realmente tengan tus archivos PNG. Asumiré mayúsculas basado en el botón.
        ImageIcon icon = iconUtils.getScaledIcon("6001-Selector_de_Carpetas_48x48.png", width, height);

        // Verifica y asigna el icono
        if (icon != null) {
            // Este icono se asociará con la Action. Si el menú o el botón usan
            // setAction(), tomarán este icono (si el LookAndFeel lo permite para menús).
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '6001-Selector_de_Carpetas_48x48.png' usando IconUtils.");
            // Opcional: texto fallback
            // putValue(Action.NAME, "Abrir");
        }
        // --- FIN DE LA PARTE IMPORTANTE ---
    }
	
    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.abrirSelectorDeCarpeta();
        }
    }
}