package controlador.actions.toggle;

import java.awt.event.ActionEvent;
import java.util.Objects; // Asegúrate de tener este import
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import modelo.VisorModel;
import servicios.ConfigurationManager;

public class ToggleSubfoldersAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private static final String CONFIG_KEY = "comportamiento.carpeta.cargarSubcarpetas";

    private ConfigurationManager configManagerRef; // No se usa aquí si VisorController lo maneja
    private VisorModel modelRef;                 // Necesario para el estado inicial
    private VisorController controllerRef;

    // Constructor (como lo tenías)
    public ToggleSubfoldersAction(
            String name,
            ImageIcon icon,
            ConfigurationManager configManager, // Se podría quitar si VisorController centraliza todo
            VisorModel model,
            VisorController controller
    ) {
        super(name, icon);
        // this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null"); // Podría ser opcional
        this.modelRef = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");

        putValue(Action.SHORT_DESCRIPTION, "Alternar mostrar solo carpeta actual o incluir subcarpetas");
        putValue(Action.ACTION_COMMAND_KEY, AppActionCommands.CMD_TOGGLE_SUBCARPETAS);

        // Estado Inicial del SELECTED_KEY (basado en el modelo)
        if (this.modelRef != null) {
            // El estado de "incluir subcarpetas" es lo opuesto a "mostrar solo carpeta actual"
            boolean initialStateIncluirSubcarpetas = !this.modelRef.isMostrarSoloCarpetaActual();
            putValue(Action.SELECTED_KEY, initialStateIncluirSubcarpetas);
             System.out.println("[ToggleSubfoldersAction Constructor] Estado inicial SELECTED_KEY: " + initialStateIncluirSubcarpetas);
        } else {
            putValue(Action.SELECTED_KEY, true); // Fallback si el modelo es null
            System.out.println("[ToggleSubfoldersAction Constructor] WARN: modelRef era null, SELECTED_KEY fallback a true (incluir subcarpetas)");
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef == null || modelRef == null) {
            System.err.println("ERROR CRÍTICO [ToggleSubfoldersAction]: controllerRef o modelRef es nulo.");
            return;
        }
        
        // 1. Leer el estado ACTUAL del modelo para "incluir subcarpetas"
        boolean actualmenteIncluyeSubcarpetas = !modelRef.isMostrarSoloCarpetaActual();
        
        // 2. Determinar el NUEVO estado deseado invirtiendo el estado actual del modelo
        boolean nuevoEstadoDeseadoParaIncluirSubcarpetas = !actualmenteIncluyeSubcarpetas;

        System.out.println("[ToggleSubfoldersAction actionPerformed] Estado actual (incluir sub): " + actualmenteIncluyeSubcarpetas + 
                           ". Solicitando cambiar a (incluir sub): " + nuevoEstadoDeseadoParaIncluirSubcarpetas);
        
        // 3. Llamar al controlador para que aplique la lógica Y ACTUALICE EL MODELO
        //    Asumimos que setMostrarSubcarpetasLogicaYUi actualiza el modelo y devuelve true si hubo un cambio real.
        //    Si no devuelve un booleano, podemos comparar el estado del modelo antes y después.
        controllerRef.setMostrarSubcarpetasLogicaYUi(nuevoEstadoDeseadoParaIncluirSubcarpetas);

        // 4. DESPUÉS de que el controlador haya actualizado el modelo,
        //    sincronizar el SELECTED_KEY de ESTA Action con el nuevo estado REAL del modelo.
        //    También se deben sincronizar los radios del menú y el aspecto del botón.
        //    VisorController.setMostrarSubcarpetasLogicaYUi llama a sincronizarUiControlesSubcarpetas,
        //    y sincronizarUiControlesSubcarpetas llama a this.sincronizarSelectedKeyConModelo().
        //    Así que el SELECTED_KEY de esta Action se actualizará a través de ese camino.
        //    Y también se llamará a view.actualizarAspectoBotonToggle() para el botón.
    }


    /**
     * Actualiza el estado SELECTED_KEY de esta Action para que coincida
     * con el estado actual del modelo respecto a la inclusión de subcarpetas.
     * Llamado por VisorController durante la sincronización de la UI.
     *
     * @param estadoModeloIncluirSubcarpetas El estado actual en el modelo (true si se deben incluir subcarpetas).
     */
    public void sincronizarSelectedKeyConModelo(boolean estadoModeloIncluirSubcarpetas) {
        if (!Objects.equals(getValue(Action.SELECTED_KEY), estadoModeloIncluirSubcarpetas)) {
            putValue(Action.SELECTED_KEY, estadoModeloIncluirSubcarpetas);
            // System.out.println("  [ToggleSubfoldersAction sincronizar] SELECTED_KEY actualizado a: " + estadoModeloIncluirSubcarpetas);
        }
    }
}