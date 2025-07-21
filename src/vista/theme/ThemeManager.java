package vista.theme;

import java.awt.Color;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import controlador.VisorController;
import controlador.managers.ConfigApplicationManager;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ThemeManager {

    private final ConfigurationManager configManager;
    private final Map<String, Tema> temasDisponibles;
    private final List<Tema> temasOrdenados;
    private Tema temaActual;
    private VisorController controllerRefParaNotificacion;
    private ConfigApplicationManager configAppManager;

    public ThemeManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.temasDisponibles = new ConcurrentHashMap<>();
        this.temasOrdenados = new java.util.ArrayList<>();
        cargarTemasPredeterminados();
    }

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
            System.out.println("[ThemeManager] Tema inicial '" + temaActual.nombreInterno() + "' con personalizaciones instalado.");
        } catch (Exception ex) {
            System.err.println("Falló al inicializar el tema por defecto.");
            ex.printStackTrace();
        }
    }

    /**
     * Establece un nuevo tema como el actual, cambiando el LookAndFeel en caliente
     * y aplicando los colores personalizados.
     *
     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "dark").
     * @return true si el tema se cambió exitosamente, false si no.
     */
    public boolean setTemaActual(String nombreTemaInterno) {
        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
        final Tema temaAnterior = this.temaActual;
        
        if (nuevoTema == null || nuevoTema.equals(this.temaActual)) {
            return false;
        }

        this.temaActual = nuevoTema;
        configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
        System.out.println("[ThemeManager] Solicitud para cambiar tema a: " + this.temaActual.nombreInterno());

        SwingUtilities.invokeLater(() -> {
            try {
                if (isDarkTheme(nuevoTema.nombreInterno())) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }

                applyCustomizations();
                FlatLaf.updateUI();

                if (configAppManager != null) {
                    configAppManager.rotarColoresDeSlotPorCambioDeTema(temaAnterior, nuevoTema);
                }
                
                if (controllerRefParaNotificacion != null) {
                	
                	controllerRefParaNotificacion.onThemeChanged();
                	
//                    controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
//                    controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
//                    controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
                }

//                JOptionPane.showMessageDialog(null, 
//                    "El tema se ha cambiado a '" + nuevoTema.nombreDisplay() + "'.", 
//                    "Cambio de Tema", 
//                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente.");
                e.printStackTrace();
            }
        });
        return true;
    }

    /**
     * Sobrescribe las propiedades de color en el UIManager.
     * Este es el núcleo de la solución de personalización. Se llama después de establecer un L&F.
     */
    private void applyCustomizations() {
        System.out.println("[ThemeManager] Aplicando personalizaciones de color globales...");

        Tema currentTema = getTemaActual();
        if (currentTema == null) {
            System.err.println("ERROR [ThemeManager]: currentTema es null al aplicar personalizaciones. No se aplicarán colores personalizados.");
            return;
        }

        // --- Configuración general de ToolBar ---
        UIManager.put("ToolBar.floatable", true);

        // --- Colores base para TODOS los botones (JButton, JToggleButton) ---
        // Esto establece el estilo por defecto.
        UIManager.put("Button.background", currentTema.colorBotonFondo());
        UIManager.put("Button.foreground", currentTema.colorBotonTexto());
        UIManager.put("Button.borderColor", currentTema.colorBorde());
        UIManager.put("Button.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("Button.pressedBackground", currentTema.colorBotonFondoActivado()); 

        // --- Colores específicos para JToggleButton (en general) ---
        
        UIManager.put("Component.accentColor", currentTema.colorBotonFondoActivado());
        
        UIManager.put("ToggleButton.background", currentTema.colorBotonFondo()); // No seleccionado
        UIManager.put("ToggleButton.foreground", currentTema.colorBotonTexto()); // No seleccionado
        UIManager.put("ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado());
        // Clave general para el estado seleccionado
        UIManager.put("ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva());


        // --- CLAVES CRUCIALES Y ESPECÍFICAS PARA COMPONENTES DENTRO DE JToolBar ---
        // Estas propiedades tienen mayor precedencia y son las que probablemente resuelvan el problema.

        // 1. Color de fondo genérico para CUALQUIER COSA seleccionada dentro de una ToolBar.
        //    Esta es una clave de "refuerzo" muy potente.
        UIManager.put("ToolBar.selectedBackground", currentTema.colorBotonFondoActivado());

        // 2. Estilo para JButtons normales dentro de una ToolBar.
        UIManager.put("ToolBar.Button.background", currentTema.colorBotonFondo());
        UIManager.put("ToolBar.Button.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToolBar.Button.pressedBackground", currentTema.colorBotonFondoActivado());

        // 3. Estilo específico para JToggleButtons dentro de una ToolBar (LA SECCIÓN MÁS IMPORTANTE).
        UIManager.put("ToolBar.ToggleButton.background", currentTema.colorBotonFondo()); // Fondo cuando NO está seleccionado.
        UIManager.put("ToolBar.ToggleButton.foreground", currentTema.colorBotonTexto()); // Texto cuando NO está seleccionado.
        
        // --> ¡¡LA CLAVE MÁS IMPORTANTE, AHORA DESCOMENTADA!! <--
        // Este es el color que queremos ver cuando el botón está activado/presionado.
        UIManager.put("ToolBar.ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
        
        UIManager.put("ToolBar.ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("ToolBar.ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToolBar.ToggleButton.selectedHoverBackground", currentTema.colorBotonFondoActivado().darker()); // Opcional: un tono más oscuro al pasar el ratón
        UIManager.put("ToolBar.ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToolBar.ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva()); // Borde cuando está seleccionado


        // --- Estilo de la propia JToolBar ---
        UIManager.put("ToolBar.background", currentTema.colorFondoPrincipal());
        UIManager.put("ToolBar.foreground", currentTema.colorTextoPrimario());


        // --- Colores para componentes de selección (List, Table, Tree) ---
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


        // --- Colores para los items de menú seleccionables ---
        UIManager.put("RadioButtonMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("RadioButtonMenuItem.selectionForeground", currentTema.colorSeleccionTexto());

        UIManager.put("CheckBoxMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("CheckBoxMenuItem.selectionForeground", currentTema.colorSeleccionTexto());

        // --- Aplicar estilos de TabbedPane ---
        applyTabbedPaneTheme();
        
        System.out.println("[ThemeManager] Todas las personalizaciones de color globales han sido aplicadas.");
    }

    private boolean isDarkTheme(String themeName) {
        return "dark".equalsIgnoreCase(themeName) || "green".equalsIgnoreCase(themeName) || "orange".equalsIgnoreCase(themeName);
    }
    
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
    }

    private Tema obtenerTemaPorDefecto() {
        return temasDisponibles.getOrDefault("clear", temasDisponibles.values().stream().findFirst().orElse(null));
    }

    public Tema getTemaActual() {
        return temaActual;
    }
    
    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller;
    }
    
    public List<String> getNombresTemasDisponibles() {
        return List.copyOf(temasDisponibles.keySet());
    }

    public List<Tema> getTemasDisponibles() {
        return List.copyOf(temasDisponibles.values());
    }

    public Color getFondoSecundarioParaTema(String nombreTemaInterno) {
        if (nombreTemaInterno == null || nombreTemaInterno.isBlank()) {
            System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: nombreTemaInterno es nulo o vacío. Devolviendo fallback.");
            return Color.DARK_GRAY;
        }
        Tema tema = temasDisponibles.get(nombreTemaInterno.toLowerCase());
        if (tema != null) {
            Color color = tema.colorFondoSecundario();
            if (color != null) {
                return color;
            } else {
                System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: Tema '" + nombreTemaInterno + "' no tiene definido colorFondoSecundario. Devolviendo fallback.");
                return Color.DARK_GRAY;
            }
        }
        System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: No se encontró tema '" + nombreTemaInterno + "'. Devolviendo fallback.");
        return Color.DARK_GRAY;
    }

    private void applyTabbedPaneTheme() {
        System.out.println("[ThemeManager] Aplicando estilos de tarjeta con mayor profundidad para JTabbedPane...");
        Tema currentTema = getTemaActual();
        if (currentTema == null) {
            System.err.println("ERROR [ThemeManager]: currentTema es null al aplicar estilos de TabbedPane. No se aplicarán.");
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
        System.out.println("[ThemeManager] Estilos de tarjeta con mayor profundidad para JTabbedPane aplicados.");
    }
    
    
    public List<Tema> getTemasOrdenados() {return List.copyOf(this.temasOrdenados);}
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) {this.configAppManager = configAppManager;}
    
    
} // --- FIN DE LA CLASE ThemeManager ---

