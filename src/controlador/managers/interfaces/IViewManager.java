package controlador.managers.interfaces;

import java.awt.Color;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JButton;

// --- INICIO DE LA MODIFICACIÓN: Añadir imports faltantes ---
import controlador.utils.ComponentRegistry;
import servicios.ConfigurationManager;
import vista.VisorView;
import vista.theme.ThemeManager;
// --- FIN DE LA MODIFICACIÓN ---

/**
 * Interfaz (Contrato) que define las responsabilidades del ViewManager.
 * Define el "qué" puede hacer el gestor de la vista, abstrayendo el "cómo" lo hace.
 * Esto permite desacoplar los componentes que interactúan con la gestión de la vista.
 */
public interface IViewManager {

    // --- MÉTODOS DE LÓGICA DE LA VISTA ---

    void setComponentePrincipalVisible(String identificadorComponente, boolean nuevoEstadoVisible, String configKeyParaEstado);
    void setCheckeredBackgroundEnabled(boolean activar);
    void setSessionBackgroundColor(Color color);
    void setSessionCheckeredBackground();
    void requestCustomBackgroundColor();
    void solicitarActualizacionUI(String uiElementId, String configKey, boolean nuevoEstado);
    void revalidateToolbarContainer();
    void ejecutarRefrescoCompletoUI();
    void refrescarFondoAlPorDefecto();
    void sincronizarAccionesFormatoBarraSuperior();
    void sincronizarAccionesFormatoBarraInferior();
    void sincronizarEstadoVisualInicialDeRadiosDeFormato();
    void setBotonMenuEspecialVisible(boolean visible);
    void cambiarAVista(String nombreVista);

    // --- MÉTODOS DE INYECCIÓN (SETTERS) ---

    /**
     * Inyecta el mapa de acciones de la aplicación.
     * @param actionMap El mapa de acciones (comando -> Action).
     */
    void setActionMap(Map<String, Action> actionMap);

    /**
     * Inyecta el mapa de botones de la aplicación.
     * @param botones El mapa de botones (clave larga -> JButton).
     */
    void setBotonesPorNombre(Map<String, JButton> botones);

    /**
     * Inyecta la instancia principal de la vista.
     * @param view La instancia de VisorView.
     */
    void setView(VisorView view);

    // --- INICIO DE LA MODIFICACIÓN: Añadir los setters que faltaban en la interfaz ---
    /**
     * Inyecta el gestor de configuración.
     * @param configuration La instancia de ConfigurationManager.
     */
    void setConfiguration(ConfigurationManager configuration);

    /**
     * Inyecta el registro de componentes de la UI.
     * @param registry La instancia de ComponentRegistry.
     */
    void setRegistry(ComponentRegistry registry);

    /**
     * Inyecta el gestor de temas visuales.
     * @param themeManager La instancia de ThemeManager.
     */
    void setThemeManager(ThemeManager themeManager);
    // --- FIN DE LA MODIFICACIÓN ---

} // --- FIN de la interfaz IViewManager ---