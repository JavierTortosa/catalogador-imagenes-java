package controlador.actions.navegacion;

import java.awt.event.ActionEvent;
// --- TEXTO MODIFICADO ---
// Ya no necesitas importar Image ni URL aquí si usas IconUtils correctamente
// import java.awt.Image;
// import java.net.URL;
// --- FIN MODIFICACION ---
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
// Asegúrate que BaseVisorAction tiene un constructor compatible (ej. super(nombre, controller))
import controlador.actions.BaseVisorAction;
import vista.util.IconUtils; // Importar IconUtils

public class PreviousImageAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // --- TEXTO MODIFICADO ---
    // Guarda la referencia a iconUtils si la necesitas fuera, si no, no es estrictamente necesario
    // private IconUtils iconUtils;
    // --- FIN MODIFICACION ---

    // --- TEXTO MODIFICADO: Constructor CORRECTO ---
    public PreviousImageAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de la superclase (asegúrate que coincida)
        super("Anterior", controller); // Asume que BaseVisorAction(String, VisorController) existe

        // Guarda la referencia si es necesario
        // this.iconUtils = iconUtils;

        // Establece la descripción (tooltip)
        putValue(Action.SHORT_DESCRIPTION, "Ir a la imagen anterior");
        System.out.println("[PreviousImageAction] Constructor iniciado.");

        // --- ¡LA PARTE IMPORTANTE! Usa IconUtils ---
        // Llama a getScaledIcon del IconUtils recibido, pasando el nombre del archivo
        // y los parámetros de tamaño (width y height) que recibe ESTE constructor.
        ImageIcon icon = iconUtils.getScaledIcon("1002-Anterior_48x48.png", width, height);

        // Verifica si el icono se cargó y asignalo a la Action
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
            System.out.println("  -> Icono para PreviousImageAction cargado y asignado vía IconUtils.");
        } else {
            // Si falla la carga, imprime un error
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '1002-Anterior_48x48.png' usando IconUtils.");
            // Opcionalmente, puedes poner un texto de fallback si el icono falla:
            // putValue(Action.NAME, "<-");
        }
        // --- FIN DE LA PARTE IMPORTANTE ---

        System.out.println("[PreviousImageAction] Constructor finalizado.");
    }
    // --- FIN CONSTRUCTOR MODIFICADO ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Loguear la acción
        if (controller != null) {
            controller.logActionInfo(e);
        }

        // Realizar la acción principal
        if (controller != null) {
            controller.navegarImagen(-1);
        } else {
             System.err.println("Error: Controller es null en PreviousImageAction");
        }
    }
}