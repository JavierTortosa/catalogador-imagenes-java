package controlador.actions.navegacion; // Asegúrate que el paquete sea correcto

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
// import javax.swing.KeyStroke; // Descomentar si usas atajos
// import java.awt.event.KeyEvent; // Descomentar si usas atajos

import controlador.ListCoordinator; // Importar el Coordinador
import vista.util.IconUtils;      // Importar IconUtils

public class LastImageAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final ListCoordinator coordinator;
    private final IconUtils iconUtils;

    /**
     * Constructor para la acción de ir a la última imagen.
     * @param coordinator El ListCoordinator que maneja la navegación.
     * @param iconUtils La utilidad para cargar iconos.
     * @param iconoAncho Ancho deseado para el icono.
     * @param iconoAlto Alto deseado para el icono.
     */
    public LastImageAction(ListCoordinator coordinator, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        super("Última Imagen"); // Nombre visible

        this.coordinator = Objects.requireNonNull(coordinator, "ListCoordinator no puede ser null");
        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");

        // Configurar propiedades
        putValue(Action.SHORT_DESCRIPTION, "Ir a la última imagen de la lista"); // Tooltip
        putValue(Action.SMALL_ICON, iconUtils.getScaledIcon("1004-Ultima_48x48.png", iconoAncho, iconoAlto)); // Icono

        // Atajo de teclado (opcional)
        // putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)); // Tecla Fin
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Acción: Última Imagen -> Llamando coordinator.seleccionarUltimo()");
        // Delegar al ListCoordinator
        coordinator.seleccionarUltimo();
    }
}