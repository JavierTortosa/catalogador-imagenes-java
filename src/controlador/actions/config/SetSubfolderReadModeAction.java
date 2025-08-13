package controlador.actions.config;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.GeneralController;
import modelo.VisorModel;

public class SetSubfolderReadModeAction extends AbstractAction {

	private static final Logger logger = LoggerFactory.getLogger(SetSubfolderReadModeAction.class);
	
    private static final long serialVersionUID = 1L; 

    private GeneralController generalControllerRef;
    private boolean modoIncluirSubcarpetasQueEstaActionRepresenta;

    public SetSubfolderReadModeAction(
            String name, 
            ImageIcon icon, 
            GeneralController generalController,
            VisorModel model, // Se mantiene para la inicialización
            boolean modoIncluirSubcarpetasAlQueEstablece,
            String commandKey
    ) {
        super(name, icon);
        this.generalControllerRef = Objects.requireNonNull(generalController, "GeneralController no puede ser null");
        this.modoIncluirSubcarpetasQueEstaActionRepresenta = modoIncluirSubcarpetasAlQueEstablece;
        
        putValue(Action.NAME, name);
        putValue(Action.ACTION_COMMAND_KEY, commandKey);

        // La inicialización del estado 'selected' al arrancar es correcta y no causa problemas.
        boolean modeloActualIncluyeSubcarpetas = !model.isMostrarSoloCarpetaActual();
        putValue(Action.SELECTED_KEY, (modeloActualIncluyeSubcarpetas == this.modoIncluirSubcarpetasQueEstaActionRepresenta));
    } // --- Fin del constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // La acción simplemente comanda al GeneralController para que establezca el modo que ella representa.
        if (generalControllerRef != null) {
            generalControllerRef.solicitarCambioModoCargaSubcarpetas(this.modoIncluirSubcarpetasQueEstaActionRepresenta);
        } else {
            logger.error("ERROR CRÍTICO [" + getClass().getSimpleName() + "]: generalControllerRef es nulo.");
        }
    } // --- Fin del método actionPerformed ---

} // --- Fin de la clase SetSubfolderReadModeAction ---