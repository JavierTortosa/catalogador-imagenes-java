package controlador.actions.navegacion; // Asumiendo este paquete

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import controlador.ListCoordinator; // Asegúrate que el import sea correcto
import vista.util.IconUtils;     // Asegúrate que el import sea correcto

/**
 * Acción Swing para navegar a la primera imagen de la lista.
 * Esta acción se asocia típicamente a un botón de la barra de herramientas
 * y/o a un elemento del menú.
 * Al ejecutarse, delega la lógica de navegación al ListCoordinator.
 */
public class FirstImageAction extends AbstractAction {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Constante para la clave del Action Command (puede usarse en ActionListener centralizado si fuera necesario). */
    public static final String COMMAND_KEY = "Primera_Imagen"; // O "Navegar_Primero" o similar

    /** Referencia al coordinador de listas que maneja la lógica de selección/navegación. */
    private final ListCoordinator listCoordinator;

    /**
     * Constructor de la acción.
     *
     * @param listCoordinator La instancia del coordinador de listas. Debe ser no nula.
     * @param iconUtils La utilidad para cargar iconos. Debe ser no nula.
     * @param iconoAncho El ancho deseado para el icono.
     * @param iconoAlto El alto deseado para el icono (o <= 0 para mantener proporción).
     */
    public FirstImageAction(ListCoordinator listCoordinator, IconUtils iconUtils, int iconoAncho, int iconoAlto) {
        // --- 1. VALIDACIÓN DE PARÁMETROS ---
        if (listCoordinator == null) {
            throw new IllegalArgumentException("ListCoordinator no puede ser null para FirstImageAction");
        }
        if (iconUtils == null) {
            throw new IllegalArgumentException("IconUtils no puede ser null para FirstImageAction");
        }
        this.listCoordinator = listCoordinator;

        // --- 2. CONFIGURACIÓN DE PROPIEDADES DE LA ACCIÓN ---
        // 2.1. Nombre (Texto que podría aparecer en menús o botones si no hay icono)
        putValue(NAME, "Primera Imagen"); // Texto descriptivo

        // 2.2. Descripción Corta (Tooltip)
        putValue(SHORT_DESCRIPTION, "Ir a la primera imagen de la lista");

        // 2.3. Icono
        //      Cargar el icono usando IconUtils y las dimensiones proporcionadas.
        //      El nombre del archivo debe coincidir con el recurso.
        ImageIcon icono = iconUtils.getScaledIcon("1001-Primera_48x48.png", iconoAncho, iconoAlto);
        if (icono != null) {
            putValue(SMALL_ICON, icono);
        } else {
            // Si el icono no se carga, poner un texto alternativo en el botón/menú
             System.err.println("WARN [FirstImageAction]: No se pudo cargar el icono '1001-Primera_48x48.png'");
             // putValue(NAME, ">|"); // Texto corto alternativo si no hay icono
        }

        // 2.4. Action Command Key (Identificador único para la acción)
        putValue(ACTION_COMMAND_KEY, COMMAND_KEY);

        // 2.5. Estado inicial (Habilitada por defecto, se podría deshabilitar si la lista está vacía inicialmente)
        //      La lógica para habilitar/deshabilitar según el estado de la lista
        //      generalmente se maneja fuera de la acción (en el Controller al actualizar la lista).
        // setEnabled(false); // Ejemplo: deshabilitar inicialmente
    }

    /**
     * Método que se ejecuta cuando se activa la acción (clic en botón o menú).
     * Delega la llamada al método correspondiente del ListCoordinator.
     *
     * @param e El evento de acción generado.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // --- 1. LOG INICIAL ---
        //     (Añadir logs como te sugerí en la respuesta anterior)
        System.out.println(">>> FirstImageAction: actionPerformed INICIO");

        // --- 2. VALIDAR COORDINADOR ---
        //     (Aunque se valida en el constructor, una comprobación extra puede ser útil)
        if (listCoordinator != null) {
            // --- 3. DELEGAR AL COORDINADOR ---
            //     Llamar al método específico para ir al primer elemento.
            System.out.println("    -> Llamando a listCoordinator.seleccionarPrimero()...");
            listCoordinator.seleccionarPrimero();
            System.out.println("    -> Llamada a listCoordinator completada.");
        } else {
            // --- 4. MANEJAR ERROR (COORDINADOR NULO) ---
             System.err.println("ERROR CRÍTICO: listCoordinator es null en FirstImageAction.actionPerformed!");
             // Aquí podrías mostrar un mensaje de error al usuario si lo consideras necesario.
             // JOptionPane.showMessageDialog(null, "Error interno: No se puede navegar.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        // --- 5. LOG FINAL ---
        System.out.println(">>> FirstImageAction: actionPerformed FIN");
    }

} // --- FIN CLASE FirstImageAction ---