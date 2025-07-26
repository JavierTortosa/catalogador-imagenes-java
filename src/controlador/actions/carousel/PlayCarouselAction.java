package controlador.actions.carousel;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import controlador.managers.CarouselManager;

public class PlayCarouselAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final CarouselManager carouselManager;

    public PlayCarouselAction(String name, CarouselManager carouselManager) {
        super(name); // Llama al constructor del padre con el nombre
        this.carouselManager = carouselManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        carouselManager.play(); // Solo llama al manager
    }
}