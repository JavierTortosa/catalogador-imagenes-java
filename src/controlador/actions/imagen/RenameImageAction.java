package controlador.actions.imagen;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Acción para cambiar el nombre de un archivo de imagen.
 */
public class RenameImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;
    private final VisorModel model;


    /**
     * Constructor para RenameImageAction.
     *
     * @param name  El nombre de la acción.
     * @param icon  El icono de la acción.
     * @param model El modelo del visor.
     */
    public RenameImageAction(String name, ImageIcon icon, VisorModel model) {
        super(name, icon);
        this.model = Objects.requireNonNull(model);
        putValue(Action.SHORT_DESCRIPTION, "Cambiar el nombre del archivo de imagen");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_IMAGEN_RENOMBRAR);
    } // --- Fin del método RenameImageAction ---


    /**
     * Ejecuta la acción de renombrar la imagen.
     *
     * @param e El evento de acción.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String key = model.getSelectedImageKey();
        if (key == null) return;

        Path rutaActual = model.getRutaCompleta(key);
        if (rutaActual == null || !Files.exists(rutaActual)) return;

        String nuevoNombre = JOptionPane.showInputDialog(null,
                "Nuevo nombre para la imagen:\n(se mantendrá la extensión " +
                        getExtension(rutaActual) + ")",
                "Cambiar Nombre de Imagen",
                JOptionPane.QUESTION_MESSAGE);

        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return;
        nuevoNombre = nuevoNombre.trim();

        Path directorio = rutaActual.getParent();
        String extension = getExtension(rutaActual);
        Path rutaNueva = directorio.resolve(nuevoNombre + "." + extension);

        if (Files.exists(rutaNueva)) {
            JOptionPane.showMessageDialog(null,
                    "Ya existe un archivo con ese nombre:\n" + rutaNueva.getFileName(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null,
                "¿Renombrar el archivo?\n" +
                        rutaActual.getFileName() + " → " + rutaNueva.getFileName(),
                "Confirmar Renombrado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Files.move(rutaActual, rutaNueva);

            String nuevaKey = rutaNueva.getFileName().toString();
            model.getRutaCompletaMap().remove(key);
            model.getRutaCompletaMap().put(nuevaKey, rutaNueva);

            int idx = model.getModeloLista().indexOf(key);
            if (idx >= 0) {
                model.getModeloLista().set(idx, nuevaKey);
            }

            model.setSelectedImageKey(nuevaKey);

            JOptionPane.showMessageDialog(null,
                    "Imagen renombrada correctamente a:\n" + nuevaKey,
                    "Renombrado Exitoso",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Error al renombrar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    } // --- Fin del método actionPerformed ---


    /**
     * Método auxiliar para obtener la extensión de un archivo.
     *
     * @param path El archivo.
     * @return La extensión del archivo.
     */
    private static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    } // --- Fin del método getExtension ---


    /**
     * Actualiza el estado de habilitación de la acción basándose en la existencia de la imagen y el modo de trabajo.
     *
     * @param model El modelo del visor.
     */
    @Override
    public void updateEnabledState(VisorModel model) {
        String key = model.getSelectedImageKey();
        Path ruta = (key != null) ? model.getRutaCompleta(key) : null;
        setEnabled(ruta != null && Files.exists(ruta) && model.getCurrentWorkMode() != WorkMode.DATOS);
    } // --- Fin del método updateEnabledState ---

} // --- Fin de la clase RenameImageAction ---
