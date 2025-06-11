package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController; // <<< ¡DEPENDENCIA PRINCIPAL AHORA!
import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import modelo.VisorModel;
import servicios.ProjectManager;

public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction {

    private static final long serialVersionUID = 1L;

    // --- DEPENDENCIAS MODIFICADAS ---
    private final ProjectManager projectManagerRef;
    private final VisorModel modelRef;
    private final VisorController controllerRef; // <<< SE REEMPLAZA VisorView POR VisorController

    /**
     * Constructor MODIFICADO. Ahora recibe el VisorController para delegar la actualización de la UI.
     */
    public ToggleMarkImageAction(
            ProjectManager projectManager,
            VisorModel model,
            VisorController controller, // <<< CAMBIO EN LA FIRMA
            String name,
            ImageIcon icon
    ) {
        super(name, icon);

        this.projectManagerRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null"); // <<< ASIGNAR DEPENDENCIA

        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
    } // --- FIN del Constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("[ToggleMarkImageAction actionPerformed] Comando: " + e.getActionCommand());
        if (projectManagerRef == null || modelRef == null || controllerRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMarkImageAction]: Dependencias nulas.");
            return;
        }

        String claveActual = modelRef.getSelectedImageKey();
        if (claveActual == null || claveActual.isEmpty()) {
            System.out.println("  -> No hay imagen seleccionada para marcar/desmarcar.");
            // Notificar al controller para que actualice la UI al estado "no marcado"
            controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
            return;
        }

        Path rutaAbsoluta = modelRef.getRutaCompleta(claveActual);
        if (rutaAbsoluta == null) {
            System.err.println("ERROR [ToggleMarkImageAction]: No se pudo obtener ruta absoluta para la clave: " + claveActual);
            controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
            return;
        }

        // --- LÓGICA DE NEGOCIO (SIN CAMBIOS) ---
        
        // --- INICIO DE LA CORRECCIÓN ---
        // Usamos TU línea, que lee el estado visual del componente que originó el evento.
        // Esto asume que el evento SIEMPRE vendrá de un componente seleccionable.
        boolean marcarImagenAhora = false;
        if (e.getSource() instanceof javax.swing.JToggleButton) { // Para JToggleButton en la toolbar
            marcarImagenAhora = ((javax.swing.JToggleButton) e.getSource()).isSelected();
        } else if (e.getSource() instanceof javax.swing.JCheckBoxMenuItem) { // Para el menú
            marcarImagenAhora = ((javax.swing.JCheckBoxMenuItem) e.getSource()).isSelected();
        } else {
             // Fallback por si la Action se dispara de otra forma (poco probable)
             // Aquí SÍ tendría sentido usar getValue, pero para ser consistentes, lo alternamos manualmente.
             marcarImagenAhora = !Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        }

        // Resincronizamos el estado interno de la Action con el estado real que acabamos de determinar.
        putValue(Action.SELECTED_KEY, marcarImagenAhora);

        // --- FIN DE LA CORRECCIÓN ---
        
        
        System.out.println("  -> Intención del usuario: " + (marcarImagenAhora ? "MARCAR" : "DESMARCAR") + " imagen: " + rutaAbsoluta);
        
        if (marcarImagenAhora) {
            projectManagerRef.marcarImagenInterno(rutaAbsoluta);
        } else {
            projectManagerRef.desmarcarImagenInterno(rutaAbsoluta);
        }
        System.out.println("  -> Imagen '" + rutaAbsoluta.getFileName() + (marcarImagenAhora ? "' MARCADA." : "' DESMARCADA."));

        // --- DELEGAR ACTUALIZACIÓN DE UI AL CONTROLADOR ---
        // El controlador se encargará de todo: actualizar el botón Y la barra de estado.
        controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(marcarImagenAhora, rutaAbsoluta);
        
    } // --- FIN del método actionPerformed ---

    @Override
    public void updateEnabledState(VisorModel modelo) {
        if (modelo == null || projectManagerRef == null || controllerRef == null) {
            this.setEnabled(false);
            putValue(Action.SELECTED_KEY, Boolean.FALSE);
            return;
        }

        String claveSeleccionada = modelo.getSelectedImageKey();
        if (claveSeleccionada != null && !claveSeleccionada.isEmpty()) {
            Path rutaImagen = modelo.getRutaCompleta(claveSeleccionada);
            if (rutaImagen != null) {
                this.setEnabled(true);
                boolean estaMarcada = projectManagerRef.estaMarcada(rutaImagen);
                putValue(Action.SELECTED_KEY, estaMarcada);
                
                // También sincronizar la UI al actualizar estado, delegando en el controller
                controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(estaMarcada, rutaImagen);
                return;
            }
        }

        this.setEnabled(false);
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
        controllerRef.actualizarEstadoVisualBotonMarcarYBarraEstado(false, null);
    } // --- FIN del método updateEnabledState ---


} // --- FIN de la clase ToggleMarkImageAction ---