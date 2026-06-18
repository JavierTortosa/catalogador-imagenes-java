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
 * Acción para establecer la imagen actual como fondo de escritorio en Windows.
 */
public class SetWallpaperAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;


    /**
     * Constructor para SetWallpaperAction.
     *
     * @param name  El nombre de la acción.
     * @param icon  El icono de la acción.
     * @param model El modelo del visor.
     */
    public SetWallpaperAction(String name, ImageIcon icon, VisorModel model) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Establecer la imagen actual como fondo de escritorio");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_FONDO_ESCRITORIO);
    } // --- Fin del método SetWallpaperAction ---


    /**
     * Ejecuta la acción de establecer el fondo de escritorio.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Path ruta = getImagePath();
        if (ruta == null || !Files.exists(ruta)) return;

        String fileName = ruta.getFileName().toString();
        int confirm = JOptionPane.showConfirmDialog(null,
                "¿Establecer \"" + fileName + "\" como fondo de escritorio?",
                "Fondo de Escritorio",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                setWindowsWallpaper(ruta.toAbsolutePath().toString());
            } else {
                JOptionPane.showMessageDialog(null,
                        "Esta función solo está disponible en Windows.",
                        "No soportado", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(null,
                    "Imagen establecida como fondo de escritorio:\n" + fileName,
                    "Fondo de Escritorio",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Error al establecer fondo de escritorio:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Método privado para establecer el fondo de escritorio en Windows mediante PowerShell.
     *
     * @param imagePath La ruta absoluta de la imagen.
     * @throws Exception Si ocurre un error durante la ejecución de PowerShell.
     */
    private static void setWindowsWallpaper(String imagePath) throws Exception {
        String escapedPath = imagePath.replace("'", "''");
        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "Add-Type -TypeDefinition @\"\n" +
                "using System.Runtime.InteropServices;\n" +
                "public class WP {\n" +
                "    [DllImport(\"user32.dll\", CharSet = CharSet.Auto)]\n" +
                "    public static extern int SystemParametersInfo(int uAction, int uParam, string lpvParam, int fuWinIni);\n" +
                "}\n" +
                "@\"\n" +
                "[WP]::SystemParametersInfo(20, 0, '" + escapedPath + "', 2)"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
    } // --- Fin del método setWindowsWallpaper ---


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

} // --- Fin de la clase SetWallpaperAction ---
