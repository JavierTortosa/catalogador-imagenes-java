package vista.theme;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
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
// ... (todos tus otros imports de temas)
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme;

import controlador.managers.ConfigApplicationManager;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.config.UIDefinitionService;
import vista.theme.themes.FlatAzulMedianochePersonalizadoIJTheme;
import vista.theme.themes.FlatCarbonOrangeIJTheme;
import vista.theme.themes.FlatObsidianOrangeIJTheme;
import vista.theme.themes.FlatPurpuraMisteriosoIJTheme;

public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    public static final String KEY_STATUSBAR_BACKGROUND = "Visor.statusBarBackground";
    public static final String KEY_STATUSBAR_FOREGROUND = "Visor.statusBarForeground";
    
    private final ConfigurationManager configManager;
    private final List<ThemeChangeListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private ConfigApplicationManager configAppManager;

    private final Map<String, ThemeInfo> TEMAS_DISPONIBLES;

   
    private Tema temaActual;

    public ThemeManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        
        // --- INICIO DE LA CORRECCIÓN ---
        // 1. Inicializamos el mapa de temas disponibles.
        this.TEMAS_DISPONIBLES = new HashMap<>();

        // 2. Poblamos el mapa con los temas base definidos en UIDefinitionService.
        //    (Este bucle es necesario porque el mapa original de TEMAS_DISPONIBLES ya no existe).
        poblarTemasBase();

        // 3. AHORA, cargamos los temas personalizados desde los archivos.
        loadCustomThemes();
        // --- FIN DE LA CORRECCIÓN ---
    } // ---FIN de metodo [Constructor ThemeManager]---
    
    /**
     * Método helper que puebla el mapa TEMAS_DISPONIBLES con los temas base
     * definidos estáticamente, para ser usado en el constructor.
     * Este método reemplaza el bloque estático para asegurar un orden de inicialización correcto.
     */
    private void poblarTemasBase() {
        // Mantenemos la definición original de los temas aquí.
        Map<String, ThemeInfo> temasBase = new HashMap<>(Map.ofEntries(
            Map.entry("arc_light", 					new ThemeInfo("Arc", FlatArcIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("arc_orange_light", 			new ThemeInfo("Arc Orange", FlatArcOrangeIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("cyan_light", 				new ThemeInfo("Cyan Light", FlatCyanLightIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("github_light", 				new ThemeInfo("GitHub", FlatGitHubIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("light_owl_light", 			new ThemeInfo("Light Owl", FlatLightOwlIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("material_lighter", 			new ThemeInfo("Material Lighter", FlatMaterialLighterIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("solarized_light", 			new ThemeInfo("Solarized Light", FlatSolarizedLightIJTheme::new, null, ThemeCategory.LIGHT)),
    	    Map.entry("arc_dark", 					new ThemeInfo("Arc Dark", FlatArcDarkIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("arc_dark_orange", 			new ThemeInfo("Arc Dark Orange", FlatArcDarkOrangeIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("carbon_dark", 				new ThemeInfo("Carbon", FlatCarbonIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("cobalt_2_dark", 				new ThemeInfo("Cobalt 2", FlatCobalt2IJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("dark_purple", 				new ThemeInfo("Dark Purple", FlatDarkPurpleIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("dracula_dark", 				new ThemeInfo("Dracula", FlatDraculaIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("github_dark", 				new ThemeInfo("GitHub Dark", FlatGitHubDarkIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("gruvbox_dark_hard", 			new ThemeInfo("Gruvbox Dark Hard", FlatGruvboxDarkHardIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("gruvbox_dark_medium", 		new ThemeInfo("Gruvbox Dark Medium", FlatGruvboxDarkMediumIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("gruvbox_dark_soft", 			new ThemeInfo("Gruvbox Dark Soft", FlatGruvboxDarkSoftIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("high_contrast", 				new ThemeInfo("High Contrast", FlatHighContrastIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("material_darker", 			new ThemeInfo("Material Darker", FlatMaterialDarkerIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("material_deep_ocean", 		new ThemeInfo("Material Deep Ocean", FlatMaterialDeepOceanIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("material_oceanic", 			new ThemeInfo("Material Oceanic", FlatMaterialOceanicIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("monocai_dark", 				new ThemeInfo("Monocai", FlatMonocaiIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("monokai_pro_dark", 			new ThemeInfo("Monokai Pro", FlatMonokaiProIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("nord_dark", 					new ThemeInfo("Nord", FlatNordIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("one_dark", 					new ThemeInfo("One Dark", FlatOneDarkIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("solarized_dark", 			new ThemeInfo("Solarized Dark", FlatSolarizedDarkIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("spacegray_dark", 			new ThemeInfo("Spacegray", FlatSpacegrayIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("vuesion_dark", 				new ThemeInfo("Vuesion", FlatVuesionIJTheme::new, null, ThemeCategory.DARK)),
    	    Map.entry("gradianto_dark_fuchsia", 	new ThemeInfo("Gradianto Dark Fuchsia", FlatGradiantoDarkFuchsiaIJTheme::new, null, ThemeCategory.GRADIENT)),
    	    Map.entry("gradianto_deep_ocean", 		new ThemeInfo("Gradianto Deep Ocean", FlatGradiantoDeepOceanIJTheme::new, null, ThemeCategory.GRADIENT)),
    	    Map.entry("gradianto_midnight_blue", 	new ThemeInfo("Gradianto Midnight Blue", FlatGradiantoMidnightBlueIJTheme::new, null, ThemeCategory.GRADIENT)),
    	    Map.entry("gradianto_nature_green", 	new ThemeInfo("Gradianto Nature Green", FlatGradiantoNatureGreenIJTheme::new, null, ThemeCategory.GRADIENT)),
    	    Map.entry("purpura_misterioso", 		new ThemeInfo("Púrpura Misterioso", FlatPurpuraMisteriosoIJTheme::new, loadInternalProperties("/vista/theme/themes/FlatPurpuraMisteriosoIJTheme.properties"), ThemeCategory.CUSTOM_INTERNAL)),
    	    Map.entry("gradianto_azul_medianoche", 	new ThemeInfo("Azul Medianoche", FlatAzulMedianochePersonalizadoIJTheme::new, loadInternalProperties("/vista/theme/themes/FlatAzulMedianochePersonalizadoIJTheme.properties"), ThemeCategory.CUSTOM_INTERNAL)), 
    	    Map.entry("carbon_orange", 				new ThemeInfo("Carbon Orange", FlatCarbonOrangeIJTheme::new, loadInternalProperties("/vista/theme/themes/FlatCarbonOrangeIJTheme.properties"), ThemeCategory.CUSTOM_INTERNAL)),
    	    Map.entry("obsidian_orange", 			new ThemeInfo("Obsidian Orange", FlatObsidianOrangeIJTheme::new, loadInternalProperties("/vista/theme/themes/FlatObsidianOrangeIJTheme.properties"), ThemeCategory.CUSTOM_INTERNAL))
        ));
        this.TEMAS_DISPONIBLES.putAll(temasBase);
    } // ---FIN de metodo [poblarTemasBase]---

    public void install() {
        String idTemaGuardado = configManager.getString(ConfigKeys.TEMA_NOMBRE, "cyan_light");
        setTemaActual(idTemaGuardado, false);
    } // ---FIN de metodo [install]---
    
    
    public boolean setTemaActual(String idTema, boolean notificarYRepintar) {
        if (idTema == null || !TEMAS_DISPONIBLES.containsKey(idTema)) {
            logger.warn("ID de tema '{}' no válido. Usando 'cyan_light' como fallback.", idTema);
            idTema = "cyan_light";
        }

        final Tema temaAnterior = this.temaActual;

        try {
            ThemeInfo info = TEMAS_DISPONIBLES.get(idTema);

            // --- INICIO DE LA LÓGICA DEFINITIVA ---

            // 1. OBTENEMOS las propiedades personalizadas del tema, ya sea interno o externo.
            //    El objeto ThemeInfo ya debería tenerlas cargadas.
            Properties customProps = (info.customProperties() != null) ? info.customProperties() : new Properties();
            
            // 2. RESOLVEMOS las variables (ej. @orange) para tener los valores finales.
            Properties resolvedProps = resolveProperties(customProps);

            // 3. APLICAMOS las propiedades ANTES de instalar el tema.
            if (!resolvedProps.isEmpty()) {
                Map<String, String> extraDefaults = new HashMap<>();
                resolvedProps.forEach((key, value) -> extraDefaults.put(String.valueOf(key), String.valueOf(value)));
                FlatLaf.setGlobalExtraDefaults(extraDefaults);
                
                String focusColorHex = resolvedProps.getProperty("Visor.borderColor.focused");
                if (focusColorHex != null) {
                    try {
                        UIManager.put("Visor.borderColor.focused", Color.decode(focusColorHex));
                        logger.debug(" -> Clave personalizada 'Visor.borderColor.focused' inyectada en UIManager.");
                    } catch (NumberFormatException e) {
                        logger.error(" -> Error al decodificar el color para Visor.borderColor.focused: {}", focusColorHex);
                    }
                }
                
                logger.debug(" -> GlobalExtraDefaults establecidos para el tema '{}'. Total: {} propiedades.", info.nombreDisplay(), extraDefaults.size());
            } else {
                FlatLaf.setGlobalExtraDefaults(java.util.Collections.emptyMap());
                logger.debug(" -> GlobalExtraDefaults limpiados (sin propiedades personalizadas).");
            }

            // 4. INSTALAMOS el LookAndFeel base. Cogerá las propiedades que acabamos de establecer.
            UIManager.setLookAndFeel(info.lafSupplier().get());
            logger.debug("LookAndFeel base '{}' aplicado.", UIManager.getLookAndFeel().getName());

            // --- FIN DE LA LÓGICA DEFINITIVA ---

            // El resto del método sigue igual...
            final Tema nuevoTema = new Tema(idTema, info.nombreDisplay(), customProps);
            this.temaActual = nuevoTema;
            configManager.setString(ConfigKeys.TEMA_NOMBRE, idTema);

            if (notificarYRepintar) {
                SwingUtilities.invokeLater(() -> {
                    JFrame mainFrame = null;
                    if (!listeners.isEmpty() && listeners.get(0) instanceof Component) {
                        mainFrame = (JFrame) SwingUtilities.getWindowAncestor((Component) listeners.get(0));
                    }
                    if (mainFrame != null) {
                        SwingUtilities.updateComponentTreeUI(mainFrame);
                    }
                    
                    logger.info("Tema '{}' establecido y UI actualizada.", this.temaActual.nombreDisplay());

                    if (configAppManager != null && temaAnterior != null) {
                        configAppManager.rotarColoresDeSlotPorCambioDeTema(temaAnterior, this.temaActual);
                    }
                    notificarListeners(this.temaActual);
                });
            }
            
            return true;
        } catch (Exception ex) {
            logger.error("Falló al aplicar el tema con ID '{}'.", idTema, ex);
            FlatLaf.setGlobalExtraDefaults(java.util.Collections.emptyMap());
            return false;
        }
    
    }  // ---FIN de metodo [setTemaActual]---
    
    
    // El resto de la clase (loadCustomThemes, resolveProperties, etc.) se queda como estaba en la última versión.
    
    private static Properties loadInternalProperties(String resourcePath) {
        Properties props = new Properties();
        if (resourcePath == null) return props;
        try (InputStream stream = ThemeManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                props.load(stream);
            } else {
                logger.error("No se pudo encontrar el recurso de propiedades interno: {}", resourcePath);
            }
        } catch (IOException e) {
            logger.error("Error al leer el recurso de propiedades interno: {}", resourcePath, e);
        }
        return props;
    }

    private Properties resolveProperties(Properties props) {
        Properties resolvedProps = new Properties();
        final Pattern varPattern = Pattern.compile("@(\\w+)");

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            Matcher matcher = varPattern.matcher(value);
            while (matcher.find()) {
                String varName = matcher.group(1);
                String varValue = props.getProperty(varName, "");
                value = value.replace("@" + varName, varValue);
                matcher = varPattern.matcher(value);
            }
            if (!key.startsWith("@") && !key.startsWith("#")) {
                resolvedProps.put(key, value);
            }
        }
        return resolvedProps;
    }

    private void loadCustomThemes() {
        File customThemesDir = new File(".temas_personalizados");
        if (!customThemesDir.exists() || !customThemesDir.isDirectory()) {
            return;
        }

        File[] files = customThemesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".properties"));
        if (files == null) return;

        for (File file : files) {
            String themeName = null;
            String baseThemeClassName = null;
            Properties themeProperties = new Properties();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("#=")) {
                        themeName = line.substring(2).trim();
                    } else if (line.startsWith("# Basado en=")) {
                        baseThemeClassName = line.substring(12).trim();
                    } else if (!line.startsWith("#") && line.contains("=")) {
                        int sep = line.indexOf('=');
                        String key = line.substring(0, sep).trim();
                        String value = line.substring(sep + 1).trim();
                        themeProperties.put(key, value);
                    }
                }
            } catch (IOException e) {
                logger.error("Error al leer archivo de tema: " + file.getName(), e);
                continue;
            }

            if (baseThemeClassName != null && !baseThemeClassName.isBlank() && themeName != null && !themeName.isBlank()) {
                try {
                    String themeId = "custom_" + file.getName().replace(".properties", "").toLowerCase().replaceAll("\\s+", "_");
                    
                    final String finalThemeName = themeName;
                    final String finalBaseThemeClassName = baseThemeClassName.trim();

                    Class<?> baseClass = Class.forName(finalBaseThemeClassName);
                    Supplier<LookAndFeel> supplier = () -> {
                        try {
                            return (LookAndFeel) baseClass.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            logger.error("No se pudo instanciar el tema base para el tema personalizado: " + finalThemeName, e);
                            return null;
                        }
                    };
                    
                    TEMAS_DISPONIBLES.put(themeId, new ThemeInfo(finalThemeName, supplier, themeProperties, ThemeCategory.CUSTOM));
                    logger.info("Tema personalizado cargado: '{}' (ID: {}) basado en '{}'", finalThemeName, themeId, finalBaseThemeClassName);

                } catch (Exception e) {
                    logger.error("Error procesando tema personalizado: " + file.getName(), e);
                }
            } else {
                 logger.warn("El archivo de tema '{}' no contiene las cabeceras requeridas. Se ignora.", file.getName());
            }
        }
    }
    
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) { this.configAppManager = configAppManager; }
    public Tema getTemaActual() { return this.temaActual; }
    public List<Tema> getTemasDisponibles() {
        return UIDefinitionService.getTemasPrincipales().stream()
                .map(def -> new Tema(def.id(), def.nombreDisplay()))
                .toList();
    }
    public Color getFondoSecundarioParaTema(String idTema) { return null; }
    public boolean isTemaActualOscuro() { return FlatLaf.isLafDark(); }
    public void addThemeChangeListener(ThemeChangeListener listener) { listeners.add(listener); }
    public void removeThemeChangeListener(ThemeChangeListener listener) { listeners.remove(listener); }
    private void notificarListeners(Tema temaCambiado) {
        for (ThemeChangeListener listener : listeners) { 
            if (listener != null) {
                listener.onThemeChanged(temaCambiado); 
            } 
        } 
    }
    
    public Map<String, ThemeInfo> getAvailableThemes() {
        return new java.util.HashMap<>(TEMAS_DISPONIBLES);
    }
    
    public record ThemeInfo(String nombreDisplay, Supplier<LookAndFeel> lafSupplier, Properties customProperties, ThemeCategory category) {}
    
    public enum ThemeCategory {
        LIGHT("Temas Claros"),
        DARK("Temas Oscuros"),
        GRADIENT("Degradados"),
        CUSTOM("Personalizados"),
        CUSTOM_INTERNAL("Personalizados Internos");

        private final String displayName;
        ThemeCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
} // --- FIN de clase [ThemeManager]---
