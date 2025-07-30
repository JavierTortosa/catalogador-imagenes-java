package controlador.actions.carousel;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.managers.CarouselManager;
import modelo.VisorModel;
import servicios.ConfigurationManager;

/**
 * Establece una velocidad (retardo) específica para el carrusel.
 * Se utiliza para los ítems del menú emergente de velocidad.
 */
public class SetCarouselSpeedAction extends AbstractAction {

    // --- Dependencias ---
    private final VisorModel model;
    private final CarouselManager carouselManager;
    private final int targetDelayMs; // El retardo específico que esta acción establecerá

    public SetCarouselSpeedAction(
            VisorModel model,
            CarouselManager carouselManager,
            String menuText, 
            int targetDelayMs) {
        
        super(menuText); // El texto que aparecerá en el JMenuItem
        this.model = model;
        this.carouselManager = carouselManager;
        this.targetDelayMs = targetDelayMs;
    } // --- Fin del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Actualizar el modelo directamente con el valor objetivo
        model.setCarouselDelay(targetDelayMs);

        // 2. Pedir al CarouselManager que actualice la UI (el JLabel)
        if (carouselManager != null) {
            carouselManager.updateSpeedLabel();
        }
    } // --- Fin del método actionPerformed ---

} // --- FIN de la clase SetCarouselSpeedAction ---