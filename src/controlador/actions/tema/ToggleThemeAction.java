package controlador.actions.tema;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JRadioButtonMenuItem; // Importar para detectar la fuente

import controlador.VisorController;
import controlador.actions.BaseVisorAction;
// No necesita IconUtils ni tamaño si esta Action es solo para el menú y no tendrá icono propio
// import vista.util.IconUtils;

public class ToggleThemeAction extends BaseVisorAction {

    private static final long serialVersionUID = 1L;
    private final String nombreTema; // Nombre del tema en minúsculas (ej. "claro", "oscuro")

    /**
     * Constructor para la acción de cambio de tema.
     * @param controller La instancia del controlador principal.
     * @param nombreTema El nombre del tema que esta acción representa (ej. "claro", "oscuro", "azul").
     *                   Debe estar en minúsculas para coincidir con la configuración.
     * @param textoMenu El texto que se mostrará en el menú (ej. "Tema Claro", "Tema Oscuro").
     */
    public ToggleThemeAction(VisorController controller, String nombreTema, String textoMenu) {
        // Llama al constructor de BaseVisorAction pasando el texto del menú
        super(textoMenu, controller);

        if (nombreTema == null || nombreTema.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tema no puede ser nulo o vacío");
        }
        this.nombreTema = nombreTema.toLowerCase().trim(); // Guarda en minúsculas

        // Descripción (Tooltip) - Opcional para menús
        putValue(Action.SHORT_DESCRIPTION, "Establecer el tema visual a " + textoMenu);

        // --- Iconos para el Menú (Opcional) ---
        // Generalmente los JRadioButtonMenuItem no muestran iconos por defecto.
        // Si quisieras forzarlo, necesitarías IconUtils aquí y cargarlo.
        // ImageIcon icon = iconUtils.getScaledIcon("icono_tema_" + this.nombreTema + ".png", 16, 16);
        // if (icon != null) {
        //     putValue(Action.SMALL_ICON, icon);
        // }
        // ------------------------------------

        // --- Estado Inicial Seleccionado ---
        // Comprueba si el tema actual en la configuración coincide con el tema de esta acción
        if (controller != null && controller.getConfigurationManager() != null) { // Añadir chequeo configMgr
             String temaActual = controller.getConfigurationManager().getTemaActual();
             // Poner el estado SELECTED_KEY si este tema es el actual
             putValue(Action.SELECTED_KEY, this.nombreTema.equals(temaActual));
        } else {
             System.err.println("WARN [ToggleThemeAction]: No se pudo obtener el tema actual durante la inicialización para: " + this.nombreTema);
             putValue(Action.SELECTED_KEY, false); // Default a no seleccionado
        }
        // ---------------------------------
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null) {
            System.err.println("Error: Controller es null en ToggleThemeAction para " + nombreTema);
            return;
        }

        // Loguear
        controller.logActionInfo(e);

        // --- Lógica Principal ---
        // 1. Obtener el tema actual ANTES de cambiarlo (por si acaso)
        String temaActualAntes = controller.getConfigurationManager().getTemaActual();

        // 2. Comprobar si el tema ya es el seleccionado (evitar guardado innecesario)
        if (this.nombreTema.equals(temaActualAntes)) {
            System.out.println("[ToggleThemeAction] El tema '" + this.nombreTema + "' ya está seleccionado. No se hace nada.");
             // Asegurar que el estado SELECTED esté correcto (por si acaso se desincronizó)
             putValue(Action.SELECTED_KEY, true);
            return;
        }

        // 3. Llamar al método del controlador para CAMBIAR y GUARDAR el tema
        //    Pasamos el nombre del tema que esta acción representa (ya está en minúsculas)
        controller.cambiarTemaYNotificar(this.nombreTema);

        // 4. Actualizar el estado SELECTED_KEY de ESTA Action
        //    Esto asegura que el JRadioButtonMenuItem asociado se marque correctamente.
        //    No es estrictamente necesario si el controlador actualiza todas las Actions,
        //    pero es más seguro hacerlo aquí también.
        putValue(Action.SELECTED_KEY, true);

        // 5. Desmarcar las OTRAS actions de tema (IMPORTANTE para Radio Buttons)
        //    El controlador debería manejar esto idealmente, pero lo podemos hacer aquí
        //    si la Action tiene acceso a las otras Actions (lo cual complica el diseño).
        //    => Es MEJOR que el controlador se encargue de actualizar el estado de todas las Actions de tema.
    }

    // --- Nuevo método para actualizar estado ---
    /**
     * Actualiza el estado de selección (SELECTED_KEY) de esta acción
     * basándose en si su tema coincide con el tema global actual.
     * Este método debe ser llamado por el controlador cuando el tema cambia.
     * @param temaGlobalActual El nombre del tema que está activo globalmente.
     */
    public void actualizarEstadoSeleccion(String temaGlobalActual) {
         putValue(Action.SELECTED_KEY, this.nombreTema.equals(temaGlobalActual));
    }
}