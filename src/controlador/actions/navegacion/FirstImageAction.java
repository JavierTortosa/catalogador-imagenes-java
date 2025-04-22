package controlador.actions.navegacion;

// --- TEXTO MODIFICADO ---
// Ya no necesitas Image aquí
// import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils; // <-- Importar IconUtils
// --- FIN MODIFICACION ---

public class FirstImageAction extends BaseVisorAction
{

    private static final long serialVersionUID = 1L;
    // Opcional: guardar iconUtils si lo necesitas fuera
    // private IconUtils iconUtils;

    // --- TEXTO MODIFICADO: Constructor CORRECTO ---
    public FirstImageAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de la superclase (ajusta si es diferente)
        super("Primera Imagen", controller);

        // Guarda referencias si es necesario
        // this.iconUtils = iconUtils;

        // Establece descripción (tooltip)
        putValue(Action.SHORT_DESCRIPTION, "Ir a la primera imagen");

        // --- ¡LA PARTE IMPORTANTE! Usa IconUtils ---
        // Llama a getScaledIcon pasando el nombre del archivo y los tamaños recibidos
        ImageIcon icon = iconUtils.getScaledIcon("1001-Primera_48x48.png", width, height);

        // Verifica si se cargó y asigna
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
             // System.out.println("  -> Icono para NextImageAction cargado y asignado vía IconUtils."); // Opcional
        } else {
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '1003-Siguiente_48x48.png' usando IconUtils.");
            // Opcional: texto de fallback
            // putValue(Action.NAME, "->");
        }
        // --- FIN DE LA PARTE IMPORTANTE ---
    }
    // --- FIN CONSTRUCTOR MODIFICADO ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Loguear
        if (controller != null) {
            controller.logActionInfo(e);
        }

        // Acción
        if (controller != null) {
            controller.navegarAIndice(0);
        } else {
             System.err.println("Error: Controller es null en FirstImageAction");
        }
    }
}