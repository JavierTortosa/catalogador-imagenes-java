package controlador.actions.config;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.managers.interfaces.IViewManager;
import servicios.ConfigurationManager;

/**
 * Una Action especializada que conecta un JCheckBoxMenuItem del menú con la
 * visibilidad de un botón de la toolbar.
 * AHORA trabaja con claves BASE y construye las claves completas internamente.
 */
@SuppressWarnings ("serial")
public class ToggleToolbarButtonVisibilityAction extends AbstractAction {
    
	private static final Logger logger = LoggerFactory.getLogger(ToggleToolbarButtonVisibilityAction.class);
	
    private final ConfigurationManager config;
    private final IViewManager viewManager;
    // --- CAMBIO 1: Renombrar campos para reflejar que son claves BASE ---
    private final String menuConfigKeyBase;        // Clave BASE del menú (ej: ...menu.configuracion.herramientas...)
    private final String buttonConfigKeyBase;      // Clave BASE del botón (ej: ...boton.edicion.imagen_rotar_izq)
    private final String toolbarId;                // El ID de la barra a refrescar

    /**
     * Constructor MODIFICADO. Recibe las claves BASE del menú y del botón.
     *
     * @param name             El texto para el JCheckBoxMenuItem.
     * @param config           El gestor de configuración.
     * @param controller       El controlador principal.
     * @param menuKeyBase      La clave de configuración BASE para el item de menú.
     * @param buttonKeyBase    La clave de configuración BASE para el botón de la toolbar.
     * @param toolbarId        El identificador de la toolbar que contiene el botón.
     */
    public ToggleToolbarButtonVisibilityAction(String name, ConfigurationManager config, IViewManager viewManager, String menuKeyBase, String buttonKeyBase, String toolbarId) {
        super(name);
        this.config = config;
        this.viewManager = viewManager;
        // --- CAMBIO 1: Asignar a los nuevos campos ---
        this.menuConfigKeyBase = menuKeyBase;
        this.buttonConfigKeyBase = buttonKeyBase;
        this.toolbarId = toolbarId;

        // --- CAMBIO 2: Construir la clave completa para leer el estado inicial ---
        // La fuente de la verdad es si el botón era visible.
        String fullButtonVisibilityKey = this.buttonConfigKeyBase + ".visible";
        boolean isButtonVisible = config.getBoolean(fullButtonVisibilityKey, true);
        putValue(Action.SELECTED_KEY, isButtonVisible);
        
    } // --- FIN del Constructor ---

    @Override
    public void actionPerformed(ActionEvent e) {
        // El estado de SELECTED_KEY ya ha sido actualizado por Swing al hacer clic.
//        boolean isSelected = Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        boolean isSelected = ((JCheckBoxMenuItem) e.getSource()).isSelected();

        // --- CAMBIO 3: Construir las claves completas para guardar ---
        // 1. Construir y actualizar la clave de visibilidad del BOTÓN.
        String fullButtonVisibilityKey = this.buttonConfigKeyBase + ".visible";
        config.setString(fullButtonVisibilityKey, String.valueOf(isSelected));

        // 2. Construir y actualizar la clave de estado del MENÚ.
        String fullMenuStateKey = this.menuConfigKeyBase + ".seleccionado";
        config.setString(fullMenuStateKey, String.valueOf(isSelected));
        
        System.out.println("[ToggleToolbarButtonVisibilityAction] '" + getValue(Action.NAME) + "' toggled. Nuevo estado: " + isSelected);
        System.out.println("  -> Config Guardada: '" + fullButtonVisibilityKey + "' = " + isSelected);
        System.out.println("  -> Config Guardada: '" + fullMenuStateKey + "' = " + isSelected);
        
        // 3. Notificar al controller para que refresque la UI.
        //    Pasamos la clave COMPLETA del botón para que el controlador sepa qué componente específico cambió.
        viewManager.solicitarActualizacionUI(
            this.toolbarId,            // El ID del contenedor a revalidar (ej: "edicion")
            fullButtonVisibilityKey,   // La clave exacta del componente que cambió
            isSelected
        );
    } // --- FIN del método actionPerformed ---
    
    
    /**
     * Devuelve la clave de configuración COMPLETA para la visibilidad del botón
     * que esta acción controla.
     * @return La clave, ej: "interfaz.boton.edicion.imagen_rotar_izq.visible".
     */
    public String getButtonVisibilityKey() {
        return this.buttonConfigKeyBase + ".visible";
    } // --- FIN del método getButtonVisibilityKey ---
    
    
} // --- FIN de la clase ToggleToolbarButtonVisibilityAction ---