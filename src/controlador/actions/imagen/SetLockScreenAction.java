package controlador.actions.imagen;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import modelo.VisorModel.WorkMode;

/**
 * Acción para establecer la imagen actual como pantalla de bloqueo en Windows.
 */
public class SetLockScreenAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;


    /**
     * Constructor para SetLockScreenAction.
     *
     * @param name  El nombre de la acción.
     * @param icon  El icono de la acción.
     * @param model El modelo del visor.
     */
    public SetLockScreenAction(String name, ImageIcon icon, VisorModel model) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Establecer la imagen actual como pantalla de bloqueo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_FONDO_BLOQUEO);
    } // --- Fin del método SetLockScreenAction ---


    /**
     * Ejecuta la acción de establecer la imagen de bloqueo.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Path ruta = getImagePath();
        if (ruta == null || !Files.exists(ruta)) return;

        String fileName = ruta.getFileName().toString();
        int confirm = JOptionPane.showConfirmDialog(null,
                "¿Establecer \"" + fileName + "\" como imagen de bloqueo?",
                "Imagen de Bloqueo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                setWindowsLockScreen(ruta.toAbsolutePath().toString());
            } else {
                JOptionPane.showMessageDialog(null,
                        "Esta función solo está disponible en Windows.",
                        "No soportado", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(null,
                    "Imagen establecida como pantalla de bloqueo:\n" + fileName,
                    "Imagen de Bloqueo",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Error al establecer imagen de bloqueo:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Método privado para establecer la pantalla de bloqueo en Windows mediante PowerShell.
     *
     * @param imagePath La ruta absoluta de la imagen.
     * @throws Exception Si ocurre un error durante la ejecución de PowerShell.
     */
    private static void setWindowsLockScreen(String imagePath) throws Exception {
        String escapedPath = imagePath.replace("\\", "\\\\").replace("'", "\\'");
        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "$img = [Windows.Storage.StorageFile]::GetFileFromPathAsync('" + escapedPath + "').GetAwaiter().GetResult();\n" +
                "[Windows.System.UserProfile.LockScreen]::SetImageFileAsync($img).GetAwaiter().GetResult()"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
    } // --- Fin del método setWindowsLockScreen ---


    /**
     * Obtiene la ruta de la imagen seleccionada.
     *
     * @return La ruta de la imagen, o null si no hay ninguna seleccionada.
     */
    private Path getImagePath() {
        String key = model.getSelectedImageKey();
        return (key != null) ? model.getRutaCompleta(key) : null;
    } // --- Fin del método getImagePath ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en la existencia de la imagen y el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        Path ruta = getImagePath();
        setEnabled(ruta != null && Files.exists(ruta) && model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase SetLockScreenAction ---
