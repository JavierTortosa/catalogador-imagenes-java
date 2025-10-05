package controlador.actions.carousel;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import controlador.managers.CarouselManager;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ChangeCarouselSpeedAction extends AbstractAction {

//	private static final Logger logger = LoggerFactory.getLogger(ChangeCarouselSpeedAction.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    public enum SpeedChangeType { INCREASE, DECREASE, RESET }

    private final VisorModel model;
    private final CarouselManager carouselManager;
    private final ConfigurationManager configuration;
    private final SpeedChangeType changeType;
    
    private static final int STEP_MS = 500;
    private static final int DEFAULT_DELAY_MS = 5000;

    public ChangeCarouselSpeedAction(
            VisorModel model, CarouselManager carouselManager,
            ConfigurationManager configuration, SpeedChangeType changeType) {
        this.model = model;
        this.carouselManager = carouselManager;
        this.configuration = configuration;
        this.changeType = changeType;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int currentDelay = model.getCarouselDelay();
        int newDelay = currentDelay;

        switch (changeType) {
            case INCREASE:
                // "Más rápido" significa acercarse a cero, desde cualquier dirección.
                newDelay = (currentDelay > 0) ? currentDelay - STEP_MS : currentDelay + STEP_MS;
                // Si cruza el cero, lo dejamos en el mínimo posible (positivo o negativo)
                if (Math.signum(currentDelay) != Math.signum(newDelay) && newDelay != 0) {
                   newDelay = (int) (Math.signum(currentDelay) * 500);
                }
                break;
            case DECREASE:
                // "Más lento" significa alejarse de cero.
                newDelay = (currentDelay > 0) ? currentDelay + STEP_MS : currentDelay - STEP_MS;
                break;
            case RESET:
                newDelay = configuration.getInt(ConfigKeys.CAROUSEL_DELAY_MS, DEFAULT_DELAY_MS);
                break;
        }

        // El modelo se encargará de validar los límites
        model.setCarouselDelay(newDelay);

        if (carouselManager != null) {
            carouselManager.updateSpeedLabel();
        }
    }
} // --- FIN de la clase ChangeCarouselSpeedAction ---