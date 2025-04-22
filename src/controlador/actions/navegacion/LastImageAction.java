package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils;

public class LastImageAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // Opcional: private IconUtils iconUtils;

    // --- TEXTO MODIFICADO: Constructor ---
    public LastImageAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Cambiar texto base y descripción
        super("Última Imagen", controller); // <-- Corregido
        putValue(Action.SHORT_DESCRIPTION, "Ir a la última imagen"); // <-- Corregido

        // Cargar icono
        ImageIcon icon = iconUtils.getScaledIcon("1004-Ultima_48x48.png", width, height);

        // Verificar y asignar
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            // Corregir mensaje de error
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '1004-Ultima_48x48.png' usando IconUtils."); // <-- Corregido
            // Opcional: putValue(Action.NAME, ">>|");
        }
    }
    // --- FIN MODIFICACION CONSTRUCTOR ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Loguear
        if (controller != null) {
            controller.logActionInfo(e);
        } else {
             System.err.println("Error: Controller es null en LastImageAction");
             return; // Salir si no hay controller
        }

        // --- TEXTO MODIFICADO: Acción ---
        // Necesitamos obtener el tamaño de la lista para calcular el último índice.
        // La forma más limpia es añadir un método al Controller para esto.
        int lastIndex = controller.getTamanioListaImagenes() - 1;

        // Solo navegar si el índice es válido (la lista no está vacía)
        if (lastIndex >= 0) {
            controller.navegarAIndice(lastIndex);
        } else {
            System.out.println("[LastImageAction] La lista está vacía, no se puede navegar a la última imagen.");
            // Opcional: puedes poner un pequeño beep o feedback visual si lo deseas
            // java.awt.Toolkit.getDefaultToolkit().beep();
        }
        // --- FIN MODIFICACION ACCION ---
    }
}