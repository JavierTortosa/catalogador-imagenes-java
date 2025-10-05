package controlador.actions.carousel;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.CarouselManager;
import modelo.VisorModel;

/**
 * Establece una velocidad (retardo) específica para el carrusel.
 * Se utiliza para los ítems del menú emergente de velocidad.
 */
public class SetCarouselSpeedAction extends AbstractAction {
	
	private static final Logger logger = LoggerFactory.getLogger(SetCarouselSpeedAction.class);

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
    	
    	logger.debug ("SetCarouselSpeedAction activado ");
    	
        // 1. Actualizar el modelo directamente con el valor objetivo
        model.setCarouselDelay(targetDelayMs);

        // 2. Pedir al CarouselManager que actualice la UI (el JLabel)
        if (carouselManager != null) {
            carouselManager.updateSpeedLabel();
        }
    } // --- Fin del método actionPerformed ---

} // --- FIN de la clase SetCarouselSpeedAction ---