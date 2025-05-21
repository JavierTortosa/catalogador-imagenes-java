package controlador.actions.projects;

import java.awt.event.ActionEvent;
import java.nio.file.Path; // Necesario para interactuar con ProjectManager
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem; // Para la lógica específica en actionPerformed

import controlador.interfaces.ContextSensitiveAction; // <<< IMPORTAR
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.ProjectManager; // El servicio
import vista.VisorView;         // Para actualizar el botón de la toolbar

public class ToggleMarkImageAction extends AbstractAction implements ContextSensitiveAction { // <<< IMPLEMENTAR

    private static final long serialVersionUID = 1L;

    private ProjectManager projectManagerServiceRef;
    private VisorModel modelRef;
    private VisorView viewRef; // Para actualizar el aspecto del botón en la toolbar si es necesario

    // Constructor REFACTORIZADO
    public ToggleMarkImageAction(
            ProjectManager projectManager,
            VisorModel model,
            VisorView view, // Para la barra de estado y el botón de la toolbar
            String name,
            ImageIcon icon) {
        super(name, icon);
        this.projectManagerServiceRef = Objects.requireNonNull(projectManager, "ProjectManager no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.viewRef = Objects.requireNonNull(view, "VisorView no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Marcar o desmarcar la imagen actual para el proyecto");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_PROYECTO_TOGGLE_MARCA);
        
        // El estado inicial se establecerá la primera vez que se llame a updateEnabledState
        updateEnabledState(this.modelRef);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectManagerServiceRef == null || modelRef == null || viewRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleMarkImageAction]: Dependencias nulas.");
            return;
        }

        String currentImageKey = modelRef.getSelectedImageKey();
        if (currentImageKey == null || currentImageKey.isEmpty()) {
            System.out.println("[ToggleMarkImageAction] No hay imagen seleccionada para marcar/desmarcar.");
            // updateEnabledState debería haber deshabilitado esta acción.
            // Pero por si acaso, podemos asegurar que SELECTED_KEY sea false si no hay imagen.
            if (Boolean.TRUE.equals(getValue(Action.SELECTED_KEY))) {
                putValue(Action.SELECTED_KEY, Boolean.FALSE);
            }
            actualizarAspectoBotonToolbar(false); // Asegurar que el botón de la toolbar esté 'desmarcado'
            return;
        }

        Path imagePath = modelRef.getRutaCompleta(currentImageKey);
        if (imagePath == null) {
            System.err.println("[ToggleMarkImageAction] No se pudo obtener la ruta para la clave: " + currentImageKey);
            return;
        }

        // Determinar el nuevo estado lógico DESEADO
        boolean estadoLogicoDeseado;
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            // Para un JCheckBoxMenuItem, su estado isSelected() YA refleja el nuevo estado
            estadoLogicoDeseado = ((JCheckBoxMenuItem) source).isSelected();
        } else {
            // Para otros componentes (como un JButton que actúa como toggle),
            // invertimos el estado actual de la Action.
            boolean estadoActualAction = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
            estadoLogicoDeseado = !estadoActualAction;
        }
        
        System.out.println("[ToggleMarkImageAction] Solicitando cambiar marca a: " + estadoLogicoDeseado + " para " + imagePath);

        // Usar los métodos de ProjectManager
        if (estadoLogicoDeseado) {
            projectManagerServiceRef.marcarImagenInterno(imagePath);
        } else {
            projectManagerServiceRef.desmarcarImagenInterno(imagePath);
        }

        // Sincronizar el SELECTED_KEY de esta Action con el estado real
        // (que debería ser ahora estadoLogicoDeseado).
        // El componente JCheckBoxMenuItem ya lo hizo, pero esto asegura consistencia
        // si la acción se llama programáticamente o desde un JToggleButton.
        if (!Objects.equals(getValue(Action.SELECTED_KEY), estadoLogicoDeseado)) {
            putValue(Action.SELECTED_KEY, estadoLogicoDeseado);
        }
        
        // Actualizar la UI (aspecto del botón en toolbar y barra de estado)
        actualizarAspectoBotonToolbar(estadoLogicoDeseado);
        actualizarBarraDeEstado(estadoLogicoDeseado, imagePath);
    }

    // Implementación del método de la interfaz ContextSensitiveAction
    @Override
    public void updateEnabledState(VisorModel currentModel) {
        boolean enabled = false;
        boolean selected = false;

        if (currentModel != null && projectManagerServiceRef != null) {
            String currentImageKey = currentModel.getSelectedImageKey();
            if (currentImageKey != null && !currentImageKey.isEmpty()) {
                enabled = true; // Se puede marcar/desmarcar si hay una imagen
                Path imagePath = currentModel.getRutaCompleta(currentImageKey);
                if (imagePath != null) {
                    selected = projectManagerServiceRef.estaMarcada(imagePath);
                }
            }
        }
        setEnabled(enabled);
        if (!Objects.equals(getValue(Action.SELECTED_KEY), selected)) {
            putValue(Action.SELECTED_KEY, selected);
        }
        
        // Actualizar la UI también aquí por si el cambio de selección
        // no pasó por actionPerformed de esta Action (ej. cambio de imagen)
        Path imagePathForUI = null;
        if (enabled && currentModel != null) {
            imagePathForUI = currentModel.getRutaCompleta(currentModel.getSelectedImageKey());
        }
        actualizarAspectoBotonToolbar(selected);
        actualizarBarraDeEstado(selected, imagePathForUI);
    }

    // Helper para actualizar el botón de la toolbar
    private void actualizarAspectoBotonToolbar(boolean marcada) {
        if (viewRef != null) {
            // Asumiendo que VisorController tiene un método para esto,
            // o que podemos acceder al botón directamente si ViewManager lo permite,
            // o que el botón es un JToggleButton que reacciona a SELECTED_KEY.
            // Por ahora, si VisorView tiene un método genérico:
            // viewRef.actualizarAspectoBotonToggle(this, marcada); // 'this' es la Action
            // Si no, esta parte necesitaría un mecanismo.
            // TEMPORAL: Simulamos que VisorView.actualizarEstadoVisualBotonMarcarYBarraEstado
            //           puede ser llamado de alguna manera o que el botón es un JToggleButton.
            //           Idealmente, el botón de la toolbar es un JToggleButton que usa esta action.
        }
    }
    
    // Helper para actualizar la barra de estado
    private void actualizarBarraDeEstado(boolean marcada, Path rutaImagen) {
        if (viewRef != null) {
            String textoRuta = "";
            if (rutaImagen != null) {
                textoRuta = rutaImagen.toString();
            } else if (modelRef != null && modelRef.getSelectedImageKey() != null) {
                Path p = modelRef.getRutaCompleta(modelRef.getSelectedImageKey());
                if (p != null) textoRuta = p.toString();
                else textoRuta = modelRef.getSelectedImageKey();
            }

            if (marcada) {
                viewRef.setTextoRuta(textoRuta + " [MARCADA]");
            } else {
                viewRef.setTextoRuta(textoRuta);
            }
        }
    }
}