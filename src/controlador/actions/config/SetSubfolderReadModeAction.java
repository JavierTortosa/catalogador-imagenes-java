package controlador.actions.config;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import controlador.VisorController;
import modelo.VisorModel; // Necesario para el estado inicial

public class SetSubfolderReadModeAction extends AbstractAction {

    private static final long serialVersionUID = 1L; 

    private VisorController controllerRef;
    private VisorModel modelRef; // Para leer el estado inicial
    private boolean modoIncluirSubcarpetasQueEstaActionRepresenta; // true si esta Action es para "Incluir Subcarpetas"

    public SetSubfolderReadModeAction(
            String name, 
            ImageIcon icon, 
            VisorController controller,
            VisorModel model, // <<--- PASAMOS EL MODELO
            boolean modoIncluirSubcarpetasAlQueEstablece, // El modo que esta Action representa
            String commandKey
    ) {
        super(name, icon);
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null"); // <<--- ASIGNAR
        this.modoIncluirSubcarpetasQueEstaActionRepresenta = modoIncluirSubcarpetasAlQueEstablece;
        
        putValue(Action.NAME, name);
        putValue(Action.ACTION_COMMAND_KEY, commandKey);

        // --- INICIALIZAR SELECTED_KEY DESDE EL ESTADO ACTUAL DEL MODELO ---
        // AppInitializer ya ha cargado la configuración en el modelo antes de que se cree esta Action.
        if (this.modelRef != null) {
            boolean modeloActualIncluyeSubcarpetas = !this.modelRef.isMostrarSoloCarpetaActual(); // Si solo muestra actual es FALSE, si incluye subc es TRUE
            
            boolean debeEstarSeleccionadaInicialmente = (modeloActualIncluyeSubcarpetas == this.modoIncluirSubcarpetasQueEstaActionRepresenta);
            putValue(Action.SELECTED_KEY, debeEstarSeleccionadaInicialmente);

            System.out.println("  [SetSubfolderReadModeAction Constructor: '" + name + "']" +
                               " representaModoIncluir=" + this.modoIncluirSubcarpetasQueEstaActionRepresenta +
                               ", modeloActualIncluyeSubcarpetas=" + modeloActualIncluyeSubcarpetas +
                               ", SELECTED_KEY inicializado a: " + getValue(Action.SELECTED_KEY));
        } else {
            // Este caso no debería ocurrir si las dependencias se inyectan correctamente.
            putValue(Action.SELECTED_KEY, Boolean.FALSE); 
            System.err.println("WARN [SetSubfolderReadModeAction Constructor: '" + name + "']: modelRef es null. SELECTED_KEY establecido a FALSE.");
        }
        // --- FIN INICIALIZAR SELECTED_KEY ---
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef != null) {
            controllerRef.setMostrarSubcarpetasLogicaYUi(this.modoIncluirSubcarpetasQueEstaActionRepresenta);
        } else {
            System.err.println("ERROR CRÍTICO [" + getClass().getSimpleName() + " (" + getValue(Action.NAME) + 
                               ")]: controllerRef es nulo.");
        }
    }

    public void sincronizarSelectedKey(boolean estadoActualDelSistemaIncluyeSubcarpetas) {
        boolean deberiaEstarSeleccionadaEstaAction = (this.modoIncluirSubcarpetasQueEstaActionRepresenta == estadoActualDelSistemaIncluyeSubcarpetas);
        if (!Objects.equals(getValue(Action.SELECTED_KEY), deberiaEstarSeleccionadaEstaAction)) {
            putValue(Action.SELECTED_KEY, deberiaEstarSeleccionadaEstaAction);
        }
    }
}

//package controlador.actions.config;
//
//import java.awt.event.ActionEvent;
//import java.util.Objects;
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.ImageIcon;
//import controlador.VisorController;
//// NO NECESITA VisorModel ni ConfigurationManager aquí directamente
//
//public class SetSubfolderReadModeAction extends AbstractAction {
//
//    private static final long serialVersionUID = 1L; 
//
//    private VisorController controllerRef;
//    private boolean modoIncluirSubcarpetasQueEstaActionRepresenta; // true si esta Action es para "Incluir Subcarpetas"
//
//    public SetSubfolderReadModeAction(
//            String name, 
//            ImageIcon icon, 
//            VisorController controller,
//            boolean modoIncluirSubcarpetasAlQueEstablece, // El modo que esta Action representa
//            String commandKey,
//            boolean estadoInicialDelSistemaIncluyeSubcarpetas // <<--- NUEVO PARÁMETRO para el estado inicial
//    ) {
//        super(name, icon);
//        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
//        this.modoIncluirSubcarpetasQueEstaActionRepresenta = modoIncluirSubcarpetasAlQueEstablece;
//        
//        putValue(Action.NAME, name);
//        putValue(Action.ACTION_COMMAND_KEY, commandKey);
//
//        // --- INICIALIZAR SELECTED_KEY BASÁNDOSE EN EL ESTADO INICIAL PASADO ---
//        boolean debeEstarSeleccionadaInicialmente = (estadoInicialDelSistemaIncluyeSubcarpetas == this.modoIncluirSubcarpetasQueEstaActionRepresenta);
//        putValue(Action.SELECTED_KEY, debeEstarSeleccionadaInicialmente);
//
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        if (controllerRef != null) {
//            // Llama al método del VisorController que centraliza la lógica
//            controllerRef.setMostrarSubcarpetasLogicaYUi(this.modoIncluirSubcarpetasQueEstaActionRepresenta);
//        } else {
//            System.err.println("ERROR CRÍTICO [" + getClass().getSimpleName() + " (" + getValue(Action.NAME) + 
//                               ")]: controllerRef es nulo.");
//        }
//    }
//
//    public void sincronizarSelectedKey(boolean estadoActualDelSistemaIncluyeSubcarpetas) {
//        boolean deberiaEstarSeleccionadaEstaAction = (this.modoIncluirSubcarpetasQueEstaActionRepresenta == estadoActualDelSistemaIncluyeSubcarpetas);
//        if (!Objects.equals(getValue(Action.SELECTED_KEY), deberiaEstarSeleccionadaEstaAction)) {
//            putValue(Action.SELECTED_KEY, deberiaEstarSeleccionadaEstaAction);
//        }
//    }
//}