package controlador.actions.config;

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import controlador.VisorController;
import servicios.ConfigurationManager;

public class SetInfoBarTextFormatAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final VisorController controllerRef;
    private final ConfigurationManager configManagerRef;
    private final String configKeyForFormatStorage; // La clave donde se guarda el formato (ej. KEY_..._NOMBRE_RUTA_FORMATO)
    private final String formatValueThisActionRepresents; // El valor que esta Action guarda (ej. "solo_nombre")
    private final String uiElementIdentifierToRefresh; // El ID de la zona a refrescar (ej. "REFRESH_INFO_BAR_SUPERIOR")

    /**
     * Constructor para una acción que establece el formato de texto para un elemento de la InfoBar.
     *
     * @param controller        Referencia al VisorController.
     * @param configManager     El gestor de configuración.
     * @param name              El texto para el JRadioButtonMenuItem.
     * @param configKeyFormat   La clave en ConfigurationManager donde se almacena el formato actual.
     * @param formatValue       El valor de formato que esta acción representa y guardará (ej. "solo_nombre").
     * @param uiElementId       El identificador de la zona de UI a refrescar.
     * @param actionCommandKey  El comando canónico para esta acción.
     */
    public SetInfoBarTextFormatAction(VisorController controller,
                                      ConfigurationManager configManager,
                                      String name,
                                      String configKeyFormat,
                                      String formatValue,
                                      String uiElementId,
                                      String actionCommandKey) {
        super(name);
        this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null");
        this.configManagerRef = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.configKeyForFormatStorage = Objects.requireNonNull(configKeyFormat, "configKeyForFormatStorage no puede ser null");
        this.formatValueThisActionRepresents = Objects.requireNonNull(formatValue, "formatValueThisActionRepresents no puede ser null");
        this.uiElementIdentifierToRefresh = Objects.requireNonNull(uiElementId, "uiElementId no puede ser null");

        putValue(Action.ACTION_COMMAND_KEY, Objects.requireNonNull(actionCommandKey, "actionCommandKey no puede ser null"));
        putValue(Action.SHORT_DESCRIPTION, "Establecer formato a: " + name);

        // Inicializar el estado SELECTED_KEY
        sincronizarSelectedKeyConConfig();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controllerRef == null || configManagerRef == null) {
            System.err.println("ERROR CRÍTICO [SetInfoBarTextFormatAction]: Controller o ConfigManager nulos.");
            return;
        }

        System.out.println("[SetInfoBarTextFormatAction] Seleccionado: " + getValue(Action.NAME) +
                           ". Guardando '" + formatValueThisActionRepresents + "' en config key '" + configKeyForFormatStorage + "'");

        // 1. Guardar el nuevo valor de formato en ConfigurationManager
        configManagerRef.setString(configKeyForFormatStorage, formatValueThisActionRepresents);

        // 2. Notificar a todas las Actions del mismo grupo para que actualicen su estado SELECTED_KEY
        //    Esto se puede hacer a través del VisorController o un sistema de eventos más general si fuera necesario.
        //    Por ahora, la forma más simple es que VisorController tenga un método para esto,
        //    o que el ActionMap se use para encontrar las otras Actions.
        //    Alternativamente, cada Action se actualiza cuando su JRadioButtonMenuItem es clickeado.
        //    El ButtonGroup asegura que solo una esté seleccionada visualmente.
        //    La clave es que TODAS las actions de radio en el mismo grupo deben
        //    poder re-evaluar su estado Action.SELECTED_KEY.
        //    Una forma es que el VisorController, al ser notificado, pida a todas las
        //    actions de formato de ESA BARRA que se sincronicen.
        //
        //    Más simple: confiamos en que MenuBarBuilder usa un ButtonGroup.
        //    Cuando esta action se ejecuta, el JRadioButtonMenuItem asociado se selecciona.
        //    Las OTRAS actions del grupo no se ejecutan, pero sus JRadioButtonMenuItems
        //    se deseleccionan visualmente por el ButtonGroup. Necesitamos que sus
        //    Action.SELECTED_KEY también se pongan a false.
        //    El VisorController puede iterar las actions relevantes y llamar a sincronizarSelectedKeyConConfig().

        // 3. Actualizar el estado Action.SELECTED_KEY de esta propia Action (debería ser true)
        //    y luego notificar al controller para refrescar la UI.
        //    Swing ya debería haber puesto esta Action a SELECTED_KEY = true.
        //    sincronizarSelectedKeyConConfig(); // Para asegurar que esta esté true.

        // 4. Notificar al VisorController para que refresque la barra de información
        controllerRef.solicitarActualizacionInterfaz(
            this.uiElementIdentifierToRefresh,
            this.configKeyForFormatStorage, // Pasamos la clave de config afectada
            true // El 'true' aquí es menos relevante, ya que InfoBarManager reconstruirá
        );

        // 5. Pedir al VisorController que resincronice TODAS las actions de formato para esta barra
        //    para que los JRadioButtonMenuItems reflejen correctamente el estado de la config.
        if (uiElementIdentifierToRefresh.equals("REFRESH_INFO_BAR_SUPERIOR")) {
            controllerRef.sincronizarAccionesFormatoBarraSuperior();
        } else if (uiElementIdentifierToRefresh.equals("REFRESH_INFO_BAR_INFERIOR")) {
            controllerRef.sincronizarAccionesFormatoBarraInferior();
        }
    }

    /**
     * Actualiza el estado Action.SELECTED_KEY de esta acción basándose en
     * si el valor de formato que representa coincide con el valor actual
     * almacenado en ConfigurationManager.
     */
    public void sincronizarSelectedKeyConConfig() {
        if (configManagerRef == null) return;
        String currentFormatInConfig = configManagerRef.getString(configKeyForFormatStorage, "solo_nombre");
        boolean isSelected = formatValueThisActionRepresents.equals(currentFormatInConfig);
        
        if (!Objects.equals(getValue(Action.SELECTED_KEY), isSelected)) { // Solo actualizar si es diferente
            putValue(Action.SELECTED_KEY, isSelected);
            // System.out.println("  [SetInfoBarTextFormatAction " + getValue(Action.NAME) + "] SELECTED_KEY actualizado a: " + isSelected);
        }
    }
}