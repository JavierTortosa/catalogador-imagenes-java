package controlador.actions.carousel;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.managers.CarouselManager;

public class PauseCarouselAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CarouselManager carouselManager;

    public PauseCarouselAction(String name, CarouselManager carouselManager) {
        super(name);
        this.carouselManager = carouselManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        carouselManager.pause();
    }
}