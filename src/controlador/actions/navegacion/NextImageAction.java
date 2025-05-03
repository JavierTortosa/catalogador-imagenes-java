package controlador.actions.navegacion; // Asegúrate que el paquete sea correcto

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
// import javax.swing.KeyStroke; // Descomentar si usas atajos
// import java.awt.event.KeyEvent; // Descomentar si usas atajos

import controlador.ListCoordinator; // Importar el Coordinador
import vista.util.IconUtils;      // Importar IconUtils

public class NextImageAction extends AbstractAction {

    private static final long serialVersionUID = 1L; // Considera actualizar si cambias campos
    private final ListCoordinator coordinator; // Referencia al Coordinador
    private final IconUtils iconUtils;         // Para el icono

    /**
     * Constructor para la acción de ir a la imagen siguiente.
     * @param coordinator El ListCoordinator que maneja la navegación.
     * @param iconUtils La utilidad para cargar iconos.
     * @param iconoAncho Ancho deseado para el icono.
     * @param iconoAlto Alto deseado para el icono.
     */
    public NextImageAction(ListCoordinator coordinator, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        // Nombre visible de la acción (puede usarse en menús si no se pone texto explícito)
        super("Imagen Siguiente");

        // Guardar referencias (validando nulls)
        this.coordinator = Objects.requireNonNull(coordinator, "ListCoordinator no puede ser null");
        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");

        // Configurar propiedades de la Action
        putValue(Action.SHORT_DESCRIPTION, "Ir a la imagen siguiente"); // Tooltip

        // Cargar y asignar icono (usando IconUtils)
        putValue(Action.SMALL_ICON, iconUtils.getScaledIcon("1003-Siguiente_48x48.png", iconoAncho, iconoAlto));

        // Asignar atajo de teclado (opcional)
        // putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)); // Flecha derecha
        // putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)); // Av Pág
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Acción: Imagen Siguiente -> Llamando coordinator.seleccionarSiguiente()");
        // Delegar la acción al método correspondiente del ListCoordinator
        coordinator.seleccionarSiguiente();
    }
}