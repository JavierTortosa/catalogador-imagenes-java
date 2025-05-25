package controlador.actions.projects; // O donde la tengas ubicada

import java.awt.event.ActionEvent;
import java.nio.file.Path; // Necesario para Path
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.commands.AppActionCommands; // Asumiendo que usas esto para el comando
import controlador.interfaces.ContextSensitiveAction; // Si implementa esta interfaz
import modelo.VisorModel;
import servicios.ProjectManager;
import vista.VisorView;

public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction { // Añadido ContextSensitiveAction

    private static final long serialVersionUID = 1L; // Considera generar uno único

    // --- 1. CAMPOS DE INSTANCIA (DEPENDENCIAS) ---
    private final ProjectManager projectManagerRef;
    private final VisorModel modelRef;
    private final VisorView viewRef;

    /**
     * Constructor para la acción de marcar/desmarcar una imagen para un proyecto.
     *
     * @param projectManager El servicio para gestionar las marcas de proyecto.
     * @param model El modelo de datos de la aplicación.
     * @param view La vista principal para actualizar la UI (barra de estado).
     * @param name El nombre/texto de la Action (para menús, tooltips).
     * @param icon El ImageIcon para esta Action.
     */
    public ToggleMarkImageAction(
            ProjectManager projectManager,
            VisorModel model,
            VisorView view,
            String name,
            ImageIcon icon
    ) {
        super(name, icon);

        // 1.1. Asignar dependencias inyectadas.
        this.projectManagerRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null en ToggleMarkImageAction");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null en ToggleMarkImageAction");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null en ToggleMarkImageAction");

        // 1.2. Establecer propiedades estándar de la Action.
        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto activo");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA); // Comando canónico

        // 1.3. Establecer el estado inicial de selección (SELECTED_KEY).
        //      Esto se hará dinámicamente por updateEnabledState() o cuando se seleccione una imagen.
        //      Inicialmente, podría estar desmarcado y deshabilitado.
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
        // this.setEnabled(false); // Se habilitará/deshabilitará por updateEnabledState
    }

    /**
     * Se ejecuta cuando la acción es disparada (ej. clic en botón o menú).
     * Alterna el estado de "marcado" de la imagen actual en el proyecto.
     *
     * @param e El ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 2.1. Log de inicio y validación de dependencias.
        System.out.println("[ToggleMarkImageAction actionPerformed] Comando: " + e.getActionCommand());
        if (projectManagerRef == null || modelRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMarkImageAction]: Dependencias (ProjectManager, Model o View) nulas.");
            return;
        }

        // 2.2. Obtener la clave y la ruta de la imagen actual desde el modelo.
        String claveActualVisor = modelRef.getSelectedImageKey();
        if (claveActualVisor == null || claveActualVisor.isEmpty()) {
            System.out.println("  -> No hay imagen seleccionada para marcar/desmarcar.");
            // Asegurar que el estado visual (botón) esté desmarcado y deshabilitado
            putValue(Action.SELECTED_KEY, Boolean.FALSE);
            this.setEnabled(false);
            actualizarEstadoVisualBotonToggleEnVista(false); // Actualiza el botón en la toolbar
            viewRef.setTextoBarraEstadoRuta(modelRef.getCarpetaRaizActual() != null ? modelRef.getCarpetaRaizActual().toString() : "");
            return;
        }

        Path rutaAbsolutaImagen = modelRef.getRutaCompleta(claveActualVisor);
        if (rutaAbsolutaImagen == null) {
            System.err.println("ERROR [ToggleMarkImageAction]: No se pudo obtener ruta absoluta para la clave: " + claveActualVisor);
            putValue(Action.SELECTED_KEY, Boolean.FALSE);
            this.setEnabled(false);
            actualizarEstadoVisualBotonToggleEnVista(false);
            viewRef.setTextoBarraEstadoRuta(claveActualVisor + " (Ruta no encontrada)");
            return;
        }

        // 2.3. Determinar el nuevo estado de "marcado" deseado.
        //      La propiedad SELECTED_KEY de la Action (que refleja el estado del JCheckBoxMenuItem/JToggleButton)
        //      indica la *intención* del usuario para el *nuevo* estado.
        boolean marcarImagenAhora = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        System.out.println("  -> Intención del usuario (desde Action.SELECTED_KEY): " + (marcarImagenAhora ? "MARCAR" : "DESMARCAR") + " imagen: " + rutaAbsolutaImagen);
        
        // 2.4. Aplicar el cambio usando ProjectManager.
        if (marcarImagenAhora) {
            projectManagerRef.marcarImagenInterno(rutaAbsolutaImagen);
        } else {
            projectManagerRef.desmarcarImagenInterno(rutaAbsolutaImagen);
        }
        // projectManagerRef debería haber guardado el cambio.

        // 2.5. Actualizar la UI (barra de estado y el aspecto del botón/menú).
        //      El estado SELECTED_KEY ya fue actualizado por Swing al hacer clic en el componente.
        //      Solo necesitamos asegurar que la barra de estado y el aspecto del botón se sincronicen.
        actualizarBarraEstadoConMarca(marcarImagenAhora, rutaAbsolutaImagen);
        actualizarEstadoVisualBotonToggleEnVista(marcarImagenAhora); // Para el botón de la toolbar

        System.out.println("  -> Imagen '" + rutaAbsolutaImagen.getFileName() + (marcarImagenAhora ? "' MARCADA." : "' DESMARCADA."));
    }

    /**
     * Actualiza el estado de habilitación y selección de esta Action
     * basándose en si hay una imagen seleccionada en el modelo y si esa
     * imagen está actualmente marcada en el proyecto.
     * Implementación de ContextSensitiveAction.
     *
     * @param modelo El modelo de datos actual de la aplicación.
     */
    @Override
    public void updateEnabledState(VisorModel modelo) {
        // 3.1. Validar dependencias.
        if (modelo == null || projectManagerRef == null) {
            this.setEnabled(false);
            putValue(Action.SELECTED_KEY, Boolean.FALSE);
            // System.out.println("[ToggleMarkImageAction updateEnabledState] Deshabilitada (modelo o projectManager nulos).");
            return;
        }

        // 3.2. Comprobar si hay una imagen seleccionada.
        String claveSeleccionada = modelo.getSelectedImageKey();
        if (claveSeleccionada != null && !claveSeleccionada.isEmpty()) {
            Path rutaImagen = modelo.getRutaCompleta(claveSeleccionada);
            if (rutaImagen != null) {
                // 3.2.1. Habilitar la acción.
                this.setEnabled(true);
                // 3.2.2. Comprobar si la imagen está marcada y actualizar SELECTED_KEY.
                boolean estaMarcada = projectManagerRef.estaMarcada(rutaImagen);
                putValue(Action.SELECTED_KEY, estaMarcada);
                // System.out.println("[ToggleMarkImageAction updateEnabledState] Habilitada. Imagen: " + rutaImagen.getFileName() + ", Marcada: " + estaMarcada);
                
                // 3.2.3. Sincronizar UI inmediatamente después de actualizar el estado de la Action
                actualizarBarraEstadoConMarca(estaMarcada, rutaImagen);
                actualizarEstadoVisualBotonToggleEnVista(estaMarcada);
                return;
            }
        }

        // 3.3. Si no hay imagen seleccionada o la ruta es inválida, deshabilitar y desmarcar.
        this.setEnabled(false);
        putValue(Action.SELECTED_KEY, Boolean.FALSE);
        // System.out.println("[ToggleMarkImageAction updateEnabledState] Deshabilitada (sin imagen seleccionada o ruta inválida).");
        
        // 3.4. Sincronizar UI para el estado deshabilitado/desmarcado
        actualizarBarraEstadoConMarca(false, null); // Pasa null para la ruta si no hay imagen
        actualizarEstadoVisualBotonToggleEnVista(false);
    }
    
    /**
     * Método helper para actualizar la barra de estado en la VisorView.
     * @param estaMarcada Si la imagen actual está marcada.
     * @param rutaImagen La ruta de la imagen actual (puede ser null).
     */
    private void actualizarBarraEstadoConMarca(boolean estaMarcada, Path rutaImagen) {
        if (viewRef == null) return;

        String textoRutaBase = "";
        if (rutaImagen != null) {
            textoRutaBase = rutaImagen.toString();
        } else if (modelRef != null && modelRef.getSelectedImageKey() != null) {
            // Fallback si rutaImagen es null pero hay una clave seleccionada
            Path p = modelRef.getRutaCompleta(modelRef.getSelectedImageKey());
            if (p != null) {
                textoRutaBase = p.toString();
            } else {
                textoRutaBase = modelRef.getSelectedImageKey();
            }
        } else if (modelRef != null && modelRef.getCarpetaRaizActual() != null) {
            // Si no hay imagen, mostrar carpeta raíz
            textoRutaBase = modelRef.getCarpetaRaizActual().toString();
        }


        if (estaMarcada) {
            viewRef.setTextoBarraEstadoRuta(textoRutaBase + " [MARCADA]");
        } else {
            viewRef.setTextoBarraEstadoRuta(textoRutaBase);
        }
    }

    /**
     * Método helper para actualizar el aspecto visual del botón toggle en la toolbar.
     * @param isSelected El estado de selección que debe reflejar el botón.
     */
    private void actualizarEstadoVisualBotonToggleEnVista(boolean isSelected) {
        if (viewRef != null) {
            // La Action (this) ya tiene su SELECTED_KEY actualizado por updateEnabledState o actionPerformed.
            // VisorView.actualizarAspectoBotonToggle usará 'this' para encontrar el botón y aplicar el estado.
            viewRef.actualizarAspectoBotonToggle(this, isSelected);
        }
    }

} // --- FIN CLASE ToggleMarkImageAction ---