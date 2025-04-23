package controlador.actions.zoom;

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

public class ResetZoomAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    // Opcional: private IconUtils iconUtils;

    // --- TEXTO MODIFICADO: Constructor CORRECTO ---
    public ResetZoomAction(VisorController controller, IconUtils iconUtils, int width, int height) {
        // Llama al constructor de la superclase
        super("Resetear Zoom", controller);

        // Establece descripción y estado inicial
        putValue(Action.SHORT_DESCRIPTION, "Volver al zoom 100% sin desplazamiento");
        setEnabled(false); // Correcto, se habilita con zoom manual

        // --- ¡LA PARTE IMPORTANTE! Usa IconUtils ---
        // Llama a getScaledIcon con el nombre del icono y los tamaños recibidos
        ImageIcon icon = iconUtils.getScaledIcon("3007-Reset_48x48.png", width, height);

        // Verifica y asigna el icono
        if (icon != null) {
            putValue(Action.SMALL_ICON, icon);
        } else {
            System.err.println("  -> ERROR: No se pudo cargar/escalar el icono '3014-Reset_48x48.png' usando IconUtils.");
            // Opcional: putValue(Action.NAME, "Reset");
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
            controller.aplicarResetZoomAction();
        } else {
            System.err.println("Error: Controller es null en ResetZoomAction");
        }
    }
}