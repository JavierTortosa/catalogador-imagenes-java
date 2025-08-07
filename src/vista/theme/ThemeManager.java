package vista.theme;

import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import controlador.AppInitializer;
import controlador.managers.ConfigApplicationManager;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ThemeManager {

	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);
	
    private final ConfigurationManager configManager;
    private final Map<String, Tema> temasDisponibles;
    private final List<Tema> temasOrdenados;
    private Tema temaActual;
    private ConfigApplicationManager configAppManager;
    
    private final List<ThemeChangeListener> listeners = new ArrayList<>();
    

    public ThemeManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.temasDisponibles = new ConcurrentHashMap<>();
        this.temasOrdenados = new java.util.ArrayList<>();
        cargarTemasPredeterminados();
    } // --- FIN DEL CONSTRUCTOR ThemeManager ---
    
    
    public void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    } // --- FIN DEL MÉTODO addThemeChangeListener ---
    
    
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    } // --- FIN DEL MÉTODO removeThemeChangeListener ---
    
    
	/**
	 * Instala el Look and Feel de FlatLaf con personalizaciones.
	 * DEBE llamarse al inicio de la aplicación, ANTES de crear cualquier JFrame.
	*/
    public void install() {
        String nombreTemaGuardado = configManager.getString("tema.nombre", "clear");
        this.temaActual = temasDisponibles.getOrDefault(nombreTemaGuardado, obtenerTemaPorDefecto());

        try {
            if (isDarkTheme(temaActual.nombreInterno())) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            applyCustomizations();
            logger.debug("[ThemeManager] Tema inicial '" + temaActual.nombreInterno() + "' con personalizaciones instalado.");
        } catch (Exception ex) {
            logger.error("Falló al inicializar el tema por defecto.");
            ex.printStackTrace();
        }
    } // --- FIN DEL MÉTODO install ---
    
    
    public boolean setTemaActual(String nombreTemaInterno) {
        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
        if (nuevoTema == null || nuevoTema.equals(this.temaActual)) {
            return false;
        }

        final Tema temaAnterior = this.temaActual;
        this.temaActual = nuevoTema;
        configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
        logger.debug("[ThemeManager] Solicitud para cambiar tema a: " + this.temaActual.nombreInterno());

        SwingUtilities.invokeLater(() -> {
            try {
                if (isDarkTheme(nuevoTema.nombreInterno())) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }

                applyCustomizations();
                FlatLaf.updateUI();

                // --- ¡PASO CLAVE 1: ACTUALIZAR LOS DATOS DE COLOR! ---
                if (configAppManager != null) {
                    // Se llama al método que implementa tu "historieta" ANTES de notificar.
                    configAppManager.rotarColoresDeSlotPorCambioDeTema(temaAnterior, nuevoTema);
                }
                
                // --- ¡PASO CLAVE 2: NOTIFICAR A LOS LISTENERS PARA QUE REPINTEN! ---
                logger.debug("[ThemeManager] Notificando a " + listeners.size() + " listeners sobre el cambio de tema...");
                for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
                    try {
                        listener.onThemeChanged(nuevoTema);
                    } catch (Exception e) {
                        logger.error("ERROR: El listener " + listener.getClass().getName() + " lanzó una excepción.");
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                logger.error("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente.");
                e.printStackTrace();
            }
        });
        
        return true;
    } // --- FIN DEL MÉTODO setTemaActual ---
    
    
    /**
     * Sobrescribe las propiedades de color en el UIManager.
     * Este es el núcleo de la solución de personalización. Se llama después de establecer un L&F.
     */
    private void applyCustomizations() {
        logger.debug("[ThemeManager] Aplicando personalizaciones de color globales...");
        
        Tema currentTema = getTemaActual();
        if (currentTema == null) {
            logger.error("ERROR [ThemeManager]: currentTema es null al aplicar personalizaciones.");
            return;
        }
        UIManager.put("ToolBar.floatable", true);
        UIManager.put("Button.background", currentTema.colorBotonFondo());
        UIManager.put("Button.foreground", currentTema.colorBotonTexto());
        UIManager.put("Button.borderColor", currentTema.colorBorde());
        UIManager.put("Button.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("Button.pressedBackground", currentTema.colorBotonFondoActivado()); 
        UIManager.put("Component.accentColor", currentTema.colorBotonFondoActivado());
        UIManager.put("ToggleButton.background", currentTema.colorBotonFondo());
        UIManager.put("ToggleButton.foreground", currentTema.colorBotonTexto());
        UIManager.put("ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva());
        UIManager.put("ToolBar.selectedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToolBar.Button.background", currentTema.colorBotonFondo());
        UIManager.put("ToolBar.Button.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToolBar.Button.pressedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToolBar.ToggleButton.background", currentTema.colorBotonFondo());
        UIManager.put("ToolBar.ToggleButton.foreground", currentTema.colorBotonTexto());
        UIManager.put("ToolBar.ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToolBar.ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("ToolBar.ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToolBar.ToggleButton.selectedHoverBackground", currentTema.colorBotonFondoActivado().darker());
        UIManager.put("ToolBar.ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToolBar.ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva());
        UIManager.put("ToolBar.background", currentTema.colorFondoPrincipal());
        UIManager.put("ToolBar.foreground", currentTema.colorTextoPrimario());
        UIManager.put("List.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("List.selectionForeground", currentTema.colorSeleccionTexto());
        UIManager.put("List.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter()); 
        UIManager.put("List.selectionInactiveForeground", currentTema.colorSeleccionTexto());
        UIManager.put("Table.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("Table.selectionForeground", currentTema.colorSeleccionTexto());
        UIManager.put("Table.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter());
        UIManager.put("Tree.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("Tree.selectionForeground", currentTema.colorSeleccionTexto());
        UIManager.put("Tree.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter());
        UIManager.put("RadioButtonMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("RadioButtonMenuItem.selectionForeground", currentTema.colorSeleccionTexto());
        UIManager.put("CheckBoxMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("CheckBoxMenuItem.selectionForeground", currentTema.colorSeleccionTexto());
        
     // Claves personalizadas para la barra de estado
        UIManager.put("StatusBar.zoomLabel.activeBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("StatusBar.zoomLabel.activeForeground", currentTema.colorSeleccionTexto());
        UIManager.put("StatusBar.zoomLabel.inactiveBackground", currentTema.colorBotonFondo());
        UIManager.put("StatusBar.zoomLabel.inactiveForeground", currentTema.colorTextoPrimario());

        // Clave para el botón de modo de zoom (si es un JButton normal)
        UIManager.put("StatusBar.zoomModeButton.background", currentTema.colorBotonFondo());
        
        
        applyTabbedPaneTheme();
        logger.debug("[ThemeManager] Todas las personalizaciones de color globales han sido aplicadas.");
    } // --- FIN DEL MÉTODO applyCustomizations ---
    
    
    private boolean isDarkTheme(String themeName) {
        return "dark".equalsIgnoreCase(themeName) || "green".equalsIgnoreCase(themeName) || "orange".equalsIgnoreCase(themeName);
    } // --- FIN DEL MÉTODO isDarkTheme ---
    
    
    private void cargarTemasPredeterminados() {
        // --- 1. Tema Claro (Clear) ---
        Color claroAcento = new Color(57, 105, 138); 
        Tema temaClaro = new Tema(
            "clear", "Tema Claro", "black",
            new Color(245, 245, 245),   // colorFondoPrincipal
            new Color(255, 255, 255),   // colorFondoSecundario
            new Color(20, 20, 20),      // colorTextoPrimario
            new Color(85, 85, 85),      // colorTextoSecundario
            new Color(200, 200, 200),   // colorBorde
            new Color(20, 20, 20),      // colorBordeTitulo
            claroAcento,                // colorSeleccionFondo
            Color.WHITE,                // colorSeleccionTexto
            new Color(230, 230, 230),   // colorBotonFondo
            new Color(20, 20, 20),      // colorBotonTexto
            new Color(179, 205, 224),   // colorBotonFondoActivado 
            new Color(173, 216, 230),   // colorBotonFondoAnimacion
            new Color(0, 122, 204),     // colorBordeSeleccionActiva
            claroAcento                 // colorLabelActivo
        );
        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
        temasOrdenados.add(temaClaro); 

        // --- 2. Tema Oscuro (Dark) ---
        Color oscuroAcentoSeleccion = new Color(0, 122, 204); 
        Tema temaOscuro = new Tema(
            "dark", "Tema Oscuro", "white",
            new Color(50, 53, 59),      // colorFondoPrincipal
            new Color(43, 45, 49),      // colorFondoSecundario
            new Color(220, 221, 222),   // colorTextoPrimario
            new Color(140, 142, 145),   // colorTextoSecundario
            new Color(60, 63, 68),      // colorBorde
            new Color(200, 201, 202),   // colorBordeTitulo
            oscuroAcentoSeleccion,      // colorSeleccionFondo
            Color.WHITE,                // colorSeleccionTexto
            new Color(66, 70, 77),      // colorBotonFondo
            new Color(220, 221, 222),   // colorBotonTexto
            new Color(88, 101, 242),    // colorBotonFondoActivado 
            new Color(75, 78, 84),      // colorBotonFondoAnimacion
            oscuroAcentoSeleccion,      // colorBordeSeleccionActiva
            oscuroAcentoSeleccion       // colorLabelActivo
       );
        temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
        temasOrdenados.add(temaOscuro);

        // --- 3. Tema Azul (Blue) ---
        Color azulAcento = new Color(0, 100, 180); 
        Tema temaAzul = new Tema(
             "blue", "Tema Azul", "black", 
             new Color(237, 244, 252),  // colorFondoPrincipal
//             new Color(255, 255, 255),  // colorFondoSecundario
             new Color(225, 235, 245),
             
             new Color(10, 25, 40),     // colorTextoPrimario
             new Color(60, 80, 100),    // colorTextoSecundario
             new Color(180, 210, 240),  // colorBorde
             new Color(10, 25, 40),     // colorBordeTitulo
             azulAcento,                // colorSeleccionFondo
             Color.WHITE,               // colorSeleccionTexto
             new Color(225, 235, 245),  // colorBotonFondo
             new Color(10, 25, 40),     // colorBotonTexto
             azulAcento,                // colorBotonFondoActivado
             new Color(200, 220, 240),  // colorBotonFondoAnimacion
             new Color(0, 122, 204),    // colorBordeSeleccionActiva
             azulAcento                 // colorLabelActivo
        );
        temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
        temasOrdenados.add(temaAzul);
        
        // --- 4. Tema Verde (Green) ---
        Color verdeAcento = new Color(0, 204, 102); 
        Tema temaVerde = new Tema(
             "green", "Tema Verde", "green", 
             new Color(20, 30, 25),     // colorFondoPrincipal
             new Color(30, 45, 38),     // colorFondoSecundario
             new Color(230, 255, 230),  // colorTextoPrimario
             new Color(140, 190, 150),  // colorTextoSecundario
             new Color(40, 60, 50),     // colorBorde
             new Color(140, 190, 150),  // colorBordeTitulo
             verdeAcento,               // colorSeleccionFondo
             Color.BLACK,               // colorSeleccionTexto
             new Color(45, 65, 55),     // colorBotonFondo
             new Color(230, 255, 230),  // colorBotonTexto
             verdeAcento,               // colorBotonFondoActivado
             new Color(60, 80, 70),     // colorBotonFondoAnimacion
             new Color(50, 255, 150),   // colorBordeSeleccionActiva
             verdeAcento                // colorLabelActivo
        );
        temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
        temasOrdenados.add(temaVerde);
        
        // --- 5. Tema Naranja (Orange) ---
        Color naranjaAcento = new Color(230, 126, 34); 
        Tema temaNaranja = new Tema(
             "orange", "Tema Naranja", "orange", 
             new Color(35, 30, 25),     // colorFondoPrincipal
             new Color(50, 40, 35),     // colorFondoSecundario
             new Color(250, 230, 210),  // colorTextoPrimario
             new Color(190, 160, 140),  // colorTextoSecundario
             new Color(70, 60, 50),     // colorBorde
             new Color(190, 160, 140),  // colorBordeTitulo
             naranjaAcento,             // colorSeleccionFondo
             Color.WHITE,               // colorSeleccionTexto
             new Color(65, 55, 50),     // colorBotonFondo
             new Color(250, 230, 210),  // colorBotonTexto
             naranjaAcento,             // colorBotonFondoActivado
             new Color(80, 70, 65),     // colorBotonFondoAnimacion
             new Color(255, 152, 0),    // colorBordeSeleccionActiva
             naranjaAcento              // colorLabelActivo
        );
        temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
        temasOrdenados.add(temaNaranja);
    } // --- FIN DEL MÉTODO cargarTemasPredeterminados ---
    

    private Tema obtenerTemaPorDefecto() {
        return temasDisponibles.getOrDefault("clear", temasDisponibles.values().stream().findFirst().orElse(null));
    }  // --- FIN DEL MÉTODO obtenerTemaPorDefecto ---
    
    
    // El método setControllerParaNotificacion ya no es necesario, pero lo dejamos por si se usa en otro sitio.
    // Lo ideal sería eliminarlo y cambiar las llamadas por addThemeChangeListener.
    @Deprecated
    public void setControllerParaNotificacion(Object controller) {
        if (controller instanceof ThemeChangeListener) {
            addThemeChangeListener((ThemeChangeListener) controller);
        }
    } // --- FIN DEL MÉTODO setControllerParaNotificacion ---
    
    
    public Color getFondoSecundarioParaTema(String nombreTemaInterno) {
        if (nombreTemaInterno == null || nombreTemaInterno.isBlank()) {
            logger.warn("WARN [ThemeManager.getFondoSecundarioParaTema]: nombreTemaInterno es nulo o vacío. Devolviendo fallback.");
            return Color.DARK_GRAY;
        }
        Tema tema = temasDisponibles.get(nombreTemaInterno.toLowerCase());
        if (tema != null) {
            Color color = tema.colorFondoSecundario();
            if (color != null) {
                return color;
            } else {
                logger.warn("WARN [ThemeManager.getFondoSecundarioParaTema]: Tema '" + nombreTemaInterno + "' no tiene definido colorFondoSecundario. Devolviendo fallback.");
                return Color.DARK_GRAY;
            }
        }
        logger.warn("WARN [ThemeManager.getFondoSecundarioParaTema]: No se encontró tema '" + nombreTemaInterno + "'. Devolviendo fallback.");
        return Color.DARK_GRAY;
    }

    private void applyTabbedPaneTheme() {
        logger.debug("[ThemeManager] Aplicando estilos de tarjeta con mayor profundidad para JTabbedPane...");
        Tema currentTema = getTemaActual();
        if (currentTema == null) {
            logger.error("ERROR [ThemeManager]: currentTema es null al aplicar estilos de TabbedPane. No se aplicarán.");
            return;
        }

        UIManager.put("TabbedPane.tabType", "card");
        UIManager.put("TabbedPane.selectedBackground", currentTema.colorBordeSeleccionActiva());
        UIManager.put("TabbedPane.unselectedBackground", currentTema.colorBotonFondoAnimacion().brighter()); 
        UIManager.put("TabbedPane.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("TabbedPane.unselectedForeground", currentTema.colorTextoSecundario());
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));
        UIManager.put("TabbedPane.tabHeight", 40);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorColor", currentTema.colorBorde());
        UIManager.put("TabbedPane.tabBorderColor", currentTema.colorBorde().darker());
        UIManager.put("TabbedPane.selectedTabBorderColor", currentTema.colorBordeSeleccionActiva().darker());
        UIManager.put("TabbedPane.tabAreaBackground", currentTema.colorFondoPrincipal().darker());
        UIManager.put("TabbedPane.contentAreaColor", currentTema.colorFondoSecundario());
        UIManager.put("TabbedPane.selectedTabColor", currentTema.colorFondoSecundario());
        UIManager.put("TabbedPane.contentBorderColor", currentTema.colorBorde());
        UIManager.put("TabbedPane.contentAreaTabInsets", new Insets(8, 0, 0, 0));
        UIManager.put("TabbedPane.tabCornerRadius", 8);
        UIManager.put("TabbedPane.selectedTabCornerRadius", 8);
        UIManager.put("TabbedPane.hoverColor", currentTema.colorBotonFondoAnimacion());
        UIManager.put("TabbedPane.focusColor", currentTema.colorBordeSeleccionActiva());
        UIManager.put("TabbedPane.selectedTabFocusColor", currentTema.colorLabelActivo());
        logger.debug("[ThemeManager] Estilos de tarjeta con mayor profundidad para JTabbedPane aplicados.");
    }
    
    
    public List<Tema> getTemasOrdenados() {return List.copyOf(this.temasOrdenados);}

    public Tema getTemaActual() {return temaActual;}
    public List<String> getNombresTemasDisponibles() {return List.copyOf(temasDisponibles.keySet());}
    public List<Tema> getTemasDisponibles() {return List.copyOf(temasDisponibles.values());}
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {this.configAppManager = configAppManager;}
    
    
} // --- FIN DE LA CLASE ThemeManager ---

