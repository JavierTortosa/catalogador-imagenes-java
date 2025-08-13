package vista.theme;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLaf;

import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme;
import com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;

// los temas estan en esta ubicacion: 
// C:\Users\ameri\.m2\repository\com\formdev\flatlaf-intellij-themes\3.4.1

import controlador.managers.ConfigApplicationManager;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.config.UIDefinitionService;

public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private final ConfigurationManager configManager;
    private final List<ThemeChangeListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private ConfigApplicationManager configAppManager; // Mantenemos la referencia

    // Mapa de temas disponibles, igual que antes.
    private static final Map<String, ThemeInfo> TEMAS_DISPONIBLES = Map.ofEntries(
            // --- TEMAS CLAROS ---
            Map.entry("arc_light", new ThemeInfo("Arc", FlatArcIJTheme::new)),
            Map.entry("arc_orange_light", new ThemeInfo("Arc Orange", FlatArcOrangeIJTheme::new)),
            Map.entry("cyan_light", new ThemeInfo("Cyan Light", FlatCyanLightIJTheme::new)),
            Map.entry("github_light", new ThemeInfo("GitHub", FlatGitHubIJTheme::new)),
            Map.entry("light_owl_light", new ThemeInfo("Light Owl", FlatLightOwlIJTheme::new)),
            Map.entry("material_lighter", new ThemeInfo("Material Lighter", FlatMaterialLighterIJTheme::new)),
            Map.entry("solarized_light", new ThemeInfo("Solarized Light", FlatSolarizedLightIJTheme::new)),

            // --- TEMAS OSCUROS ---
            Map.entry("arc_dark", new ThemeInfo("Arc Dark", FlatArcDarkIJTheme::new)),
            Map.entry("arc_dark_orange", new ThemeInfo("Arc Dark Orange", FlatArcDarkOrangeIJTheme::new)),
            Map.entry("carbon_dark", new ThemeInfo("Carbon", FlatCarbonIJTheme::new)),
            Map.entry("cobalt_2_dark", new ThemeInfo("Cobalt 2", FlatCobalt2IJTheme::new)),
            Map.entry("dark_purple", new ThemeInfo("Dark Purple", FlatDarkPurpleIJTheme::new)),
            Map.entry("dracula_dark", new ThemeInfo("Dracula", FlatDraculaIJTheme::new)),
            Map.entry("github_dark", new ThemeInfo("GitHub Dark", FlatGitHubDarkIJTheme::new)),
            Map.entry("gruvbox_dark_hard", new ThemeInfo("Gruvbox Dark Hard", FlatGruvboxDarkHardIJTheme::new)),
            Map.entry("gruvbox_dark_medium", new ThemeInfo("Gruvbox Dark Medium", FlatGruvboxDarkMediumIJTheme::new)),
            Map.entry("gruvbox_dark_soft", new ThemeInfo("Gruvbox Dark Soft", FlatGruvboxDarkSoftIJTheme::new)),
            Map.entry("high_contrast", new ThemeInfo("High Contrast", FlatHighContrastIJTheme::new)),
            Map.entry("material_darker", new ThemeInfo("Material Darker", FlatMaterialDarkerIJTheme::new)),
            Map.entry("material_deep_ocean", new ThemeInfo("Material Deep Ocean", FlatMaterialDeepOceanIJTheme::new)),
            Map.entry("material_oceanic", new ThemeInfo("Material Oceanic", FlatMaterialOceanicIJTheme::new)),
            Map.entry("monocai_dark", new ThemeInfo("Monocai", FlatMonocaiIJTheme::new)),
            Map.entry("monokai_pro_dark", new ThemeInfo("Monokai Pro", FlatMonokaiProIJTheme::new)),
            Map.entry("nord_dark", new ThemeInfo("Nord", FlatNordIJTheme::new)),
            Map.entry("one_dark", new ThemeInfo("One Dark", FlatOneDarkIJTheme::new)),
            Map.entry("solarized_dark", new ThemeInfo("Solarized Dark", FlatSolarizedDarkIJTheme::new)),
            Map.entry("spacegray_dark", new ThemeInfo("Spacegray", FlatSpacegrayIJTheme::new)),
            Map.entry("vuesion_dark", new ThemeInfo("Vuesion", FlatVuesionIJTheme::new)),
            
            // --- TEMAS CON DEGRADADOS (pueden ser más "llamativos") ---
            Map.entry("gradianto_dark_fuchsia", new ThemeInfo("Gradianto Dark Fuchsia", FlatGradiantoDarkFuchsiaIJTheme::new)),
            Map.entry("gradianto_deep_ocean", new ThemeInfo("Gradianto Deep Ocean", FlatGradiantoDeepOceanIJTheme::new)),
            Map.entry("gradianto_midnight_blue", new ThemeInfo("Gradianto Midnight Blue", FlatGradiantoMidnightBlueIJTheme::new)),
            Map.entry("gradianto_nature_green", new ThemeInfo("Gradianto Nature Green", FlatGradiantoNatureGreenIJTheme::new)),
            
            // --- TU TEMA PERSONALIZADO ---
            Map.entry("purpura_misterioso", new ThemeInfo("Púrpura Misterioso", FlatDarkPurpleIJTheme::new))
        );
    
    
//    private static final Map<String, ThemeInfo> TEMAS_DISPONIBLES = Map.ofEntries(
//    		Map.entry("purpura_misterioso", new ThemeInfo("Purpura Misterioso", FlatDarkPurpleIJTheme::new)),
//    		
//    		Map.entry("cyan_light", new ThemeInfo("Cian Claro", FlatCyanLightIJTheme::new)),
//            Map.entry("solarized_light", new ThemeInfo("Solarized Claro", FlatSolarizedLightIJTheme::new)),
//            Map.entry("carbon", new ThemeInfo("Carbón", FlatCarbonIJTheme::new)),
//            Map.entry("material_darker", new ThemeInfo("Material Oscuro", FlatMaterialDarkerIJTheme::new)),
//            Map.entry("dark_purple", new ThemeInfo("Púrpura Oscuro", FlatDarkPurpleIJTheme::new)),
//            Map.entry("dracula", new ThemeInfo("Drácula", FlatDraculaIJTheme::new)),
//            Map.entry("cobalt2", new ThemeInfo("Cobalto 2", FlatCobalt2IJTheme::new)),
//            Map.entry("solarized_dark", new ThemeInfo("Solarized Oscuro", FlatSolarizedDarkIJTheme::new)),
//            Map.entry("monokai_pro", new ThemeInfo("Monokai Pro", FlatMonokaiProIJTheme::new)),
//            Map.entry("arc_dark", new ThemeInfo("Arc Oscuro", FlatArcDarkIJTheme::new)),
//            Map.entry("fuchsia_dark", new ThemeInfo("Fucsia Degradado", FlatGradiantoDarkFuchsiaIJTheme::new)),
//            Map.entry("high_contrast", new ThemeInfo("Alto Contraste", FlatHighContrastIJTheme::new))
//            
//        // ... añade los que quieras
//    );
    
    private Tema temaActual; // ¡Volvemos a usar tu objeto Tema!

    public ThemeManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
    } // --- FIN del constructor --- 

    public void install() {
        String idTemaGuardado = configManager.getString(ConfigKeys.TEMA_NOMBRE, "cyan_light");
        setTemaActual(idTemaGuardado, false); // Usamos un método unificado
    } // --- FIN del metodo install ---

    public boolean setTemaActual(String idTema, boolean notificarYRepintar) {
        if (idTema == null || !TEMAS_DISPONIBLES.containsKey(idTema)) {
            idTema = "cyan_light"; // Fallback
        }
        
        // Si no hay cambio, no hacemos nada
        if (this.temaActual != null && this.temaActual.nombreInterno().equals(idTema)) {
            return false;
        }

        final Tema temaAnterior = this.temaActual;

        try {
            // 1. Establecer el LookAndFeel de FlatLaf
            ThemeInfo info = TEMAS_DISPONIBLES.get(idTema);
            UIManager.setLookAndFeel(info.lafSupplier().get());

         // <<< ¡AQUÍ AÑADIMOS EL MISTERIO! >>>
            // 2. Si el tema es el nuestro, sobrescribimos los colores que queramos.
            if ("purpura_misterioso".equals(idTema)) {
                logger.debug("-> Aplicando personalizaciones para 'Púrpura Misterioso'...");
                // Hacemos que el color de acento (selecciones, toggles activos) sea un cian brillante
                UIManager.put("Component.accentColor", new Color(0, 255, 255)); 
                // Y que el fondo de la selección de listas también sea ese cian
                UIManager.put("List.selectionBackground", new Color(0, 255, 255));
                // Y el texto seleccionado, negro para que contraste
                UIManager.put("List.selectionForeground", Color.BLACK);
                // Hacemos los bordes de los botones un poco más púrpuras
                UIManager.put("Button.borderColor", new Color(120, 80, 150));
            }
            
            
            // 3. ¡EL PASO CLAVE! Creamos tu objeto `Tema` a partir de los colores del UIManager
            this.temaActual = construirTemaDesdeUIManager(idTema, info.nombreDisplay());
            
            // 4. Guardar configuración
            configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
            logger.info("Tema '{}' establecido con éxito.", this.temaActual.nombreDisplay());

            // 5. Ejecutar la lógica de tu aplicación
            if (notificarYRepintar) {
                SwingUtilities.invokeLater(() -> {
                    // Actualizamos la UI para que tome los nuevos valores base
                    FlatLaf.updateUI();

                    // Aplicamos nuestras personalizaciones ADICIONALES
                    // (Esto ya no es necesario si no tienes personalizaciones muy específicas)
                    // applyCustomizations(); 

                    // Notificamos al resto de la aplicación
                    if (configAppManager != null && temaAnterior != null) {
                        configAppManager.rotarColoresDeSlotPorCambioDeTema(temaAnterior, this.temaActual);
                    }
                    notificarListeners(this.temaActual);
                });
            }
            return true;
        } catch (Exception ex) {
            logger.error("Falló al aplicar el tema con ID '{}'.", idTema, ex);
            return false;
        }
    } // --- FIN del metodo setTemaActual ---
    
    /**
     * ¡Magia! Este método lee los colores del tema de FlatLaf que acabamos de poner
     * y los usa para crear una instancia de tu antiguo objeto `Tema`, manteniendo
     * la compatibilidad con el resto de tu código.
     */
    private Tema construirTemaDesdeUIManager(String id, String nombreDisplay) {
        return new Tema(
            id,
            nombreDisplay,
            isTemaActualOscuro() ? "white" : "black", // Lógica de iconos
            UIManager.getColor("Panel.background"),
            UIManager.getColor("TabbedPane.contentAreaColor"), // O la que uses para fondo secundario
            UIManager.getColor("Label.foreground"),
            UIManager.getColor("Label.disabledForeground"),
            UIManager.getColor("Component.borderColor"),
            UIManager.getColor("TitledBorder.titleColor"),
            UIManager.getColor("List.selectionBackground"),
            UIManager.getColor("List.selectionForeground"),
            // FALTAN LOS DOS NUEVOS COLORES AQUÍ
            UIManager.getColor("Button.selectedBackground"),
            UIManager.getColor("Button.hoverBackground"),
            UIManager.getColor("Button.selectedBackground"),
            UIManager.getColor("Button.hoverBackground"),
            UIManager.getColor("Component.accentColor"), // O la que uses para borde activo
            UIManager.getColor("Label.foreground"), // Para label activo
            
            // "Component.accentColor" es una excelente opción.
            UIManager.getColor("Component.accentColor"),
            
            // Usaremos el de la selección de lista como un buen candidato.
            UIManager.getColor("List.selectionForeground") 
        );
        
    } // --- FIN del metodo construirTemaDesdeUIManager ---
    

    // Mantenemos la referencia para que tu lógica de "rotarColores" funcione
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {
        this.configAppManager = configAppManager;
    } // --- FIN del metodo setConfigApplicationManager
    
    // --- Métodos que tu código antiguo necesita ---

    public Tema getTemaActual() {
        return this.temaActual;
    } // --- FIN del metodo getTemaActual

    /**
     * Devuelve una lista de los temas "principales" definidos en UIDefinitionService.
     * Este método es usado por clases como ConfigApplicationManager para la lógica de rotación de colores.
     */
    public List<Tema> getTemasDisponibles() {
        // Leemos la definición de la fuente de verdad.
        List<vista.config.UIDefinitionService.TemaDefinicion> temasDefs = UIDefinitionService.getTemasPrincipales();

        // Creamos y devolvemos los objetos `Tema` ligeros que tu lógica espera.
        return temasDefs.stream()
                .map(def -> new Tema(def.id(), def.nombreDisplay(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null))
                .toList();
    } // --- Fin del método getTemasDisponibles ---
    
    
    public Color getFondoSecundarioParaTema(String idTema) {
        Tema tema = construirTemaDesdeUIManager(idTema, ""); // Es un apaño, pero funciona
        return tema.colorFondoSecundario();
    }// --- Fin del método getFondoSecundarioParaTema ---

    
    // --- El resto de métodos de listener y ayuda ---

    private void notificarListeners(Tema temaCambiado) {
        listeners.forEach(listener -> listener.onThemeChanged(temaCambiado));
    } // --- FIN del metodo notificarListeners
    
    public boolean isTemaActualOscuro() {
        if (UIManager.getLookAndFeel() == null) return false;
        return UIManager.getLookAndFeel().getName().toLowerCase().contains("dark");
    } // --- FIN del metodo isTemaActualOscuro

    public void addThemeChangeListener(ThemeChangeListener listener) { listeners.add(listener); }
    public void removeThemeChangeListener(ThemeChangeListener listener) { listeners.remove(listener); }
    private record ThemeInfo(String nombreDisplay, Supplier<LookAndFeel> lafSupplier) {}

} // --- Fin de la clase ThemeManager ---

