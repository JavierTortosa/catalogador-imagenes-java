package vista.theme;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import controlador.VisorController;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;

public class ThemeManager {

    private final ConfigurationManager configManager;
    private final Map<String, Tema> temasDisponibles;
    private Tema temaActual;
    private VisorController controllerRefParaNotificacion;

    public ThemeManager(ConfigurationManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.temasDisponibles = new ConcurrentHashMap<>();
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

                if (controllerRefParaNotificacion != null) {
                    controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
                    controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
                    controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
                }

                JOptionPane.showMessageDialog(null, 
                    "El tema se ha cambiado a '" + nuevoTema.nombreDisplay() + "'.", 
                    "Cambio de Tema", 
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente.");
                e.printStackTrace();
            }
        });
        return true;
    }

    /**
     * Sobrescribe las propiedades de color en el UIManager.
     * Este es el núcleo de la solución. Se llama después de establecer un L&F.
     */
    private void applyCustomizations() {
        System.out.println("[ThemeManager] Aplicando personalizaciones de color globales...");

        Tema currentTema = getTemaActual();
        if (currentTema == null) {
            System.err.println("ERROR [ThemeManager]: currentTema es null al aplicar personalizaciones. No se aplicarán colores personalizados.");
            return;
        }

        // --- Configuración general de ToolBar y Botones ---
        UIManager.put("ToolBar.floatable", true);
        UIManager.put("Button.paintToolBarButton", true); // Permitir que FlatLaf pinte los botones de la ToolBar.
        UIManager.put("ToolBar.toggleButton.selectedBackgroundOpaque", true); // Asegura que el fondo del toggle sea opaco.

        // --- Colores base para TODOS los botones (JButton, JToggleButton) ---
        // Esto afecta a botones en general, no solo a los de la ToolBar.
        UIManager.put("Button.background", currentTema.colorBotonFondo());
        UIManager.put("Button.foreground", currentTema.colorBotonTexto());
        UIManager.put("Button.borderColor", currentTema.colorBorde());
        UIManager.put("Button.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("Button.pressedBackground", currentTema.colorBotonFondoActivado()); 

        // --- Colores específicos para JToggleButton (general, fuera de ToolBars si FlatLaf las distingue) ---
        // Estas son las claves principales para el estado "seleccionado" de ToggleButtons en general.
        UIManager.put("ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
        UIManager.put("ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
        UIManager.put("ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva());
        UIManager.put("ToggleButton.background", currentTema.colorBotonFondo()); // No seleccionado
        UIManager.put("ToggleButton.foreground", currentTema.colorBotonTexto()); // No seleccionado
        UIManager.put("ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion());
        UIManager.put("ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado());

        // --- CLAVES CRUCIALES: PROPIEDADES DE BOTONES Y TOGGLES DENTRO DE TOOLBARS ---
        // Estas propiedades son más específicas y pueden tener mayor precedencia.
        // Afectan a *todos* los botones dentro de una JToolBar.
        UIManager.put( "ToolBar.Button.background", currentTema.colorBotonFondo() );
        UIManager.put( "ToolBar.Button.foreground", currentTema.colorBotonTexto() );
        UIManager.put( "ToolBar.Button.hoverBackground", currentTema.colorBotonFondoAnimacion() );
        UIManager.put( "ToolBar.Button.pressedBackground", currentTema.colorBotonFondoActivado() );
        // También para el estado "selected" de botones genéricos en toolbars (no solo JToggleButtons)
        UIManager.put( "ToolBar.Button.selectedBackground", currentTema.colorBotonFondoActivado() );
        UIManager.put( "ToolBar.Button.selectedForeground", currentTema.colorSeleccionTexto() );
        UIManager.put( "ToolBar.Button.selectedHoverBackground", currentTema.colorBotonFondoActivado().darker() ); // Un tono más oscuro al pasar el ratón estando seleccionado.

        // Las propiedades más específicas para JToggleButtons dentro de JToolBar.
        // Estas deberían ser las que realmente definan el color de un toggle button seleccionado en la toolbar.
        UIManager.put( "ToolBar.ToggleButton.background", currentTema.colorBotonFondo() ); // Fondo cuando no seleccionado.
        UIManager.put( "ToolBar.ToggleButton.foreground", currentTema.colorBotonTexto() ); // Texto cuando no seleccionado.
        UIManager.put( "ToolBar.ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado() ); // EL COLOR QUE QUEREMOS CUANDO ESTÁ SELECCIONADO.
        UIManager.put( "ToolBar.ToggleButton.selectedForeground", currentTema.colorSeleccionTexto() ); // Color de texto cuando seleccionado.
        UIManager.put( "ToolBar.ToggleButton.hoverBackground", currentTema.colorBotonFondoAnimacion() ); // Color de fondo al pasar el ratón (no seleccionado).
        UIManager.put( "ToolBar.ToggleButton.selectedHoverBackground", currentTema.colorBotonFondoActivado().darker() ); // Color de fondo al pasar el ratón (seleccionado).
        UIManager.put( "ToolBar.ToggleButton.pressedBackground", currentTema.colorBotonFondoActivado() ); // Color al pulsar (puede ser igual al seleccionado).
        UIManager.put( "ToolBar.ToggleButton.selectedPressedBackground", currentTema.colorBotonFondoActivado().brighter() ); // Color al pulsar estando seleccionado (opcional).


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

        // --- Para los JRadioButtonMenuItem y JCheckBoxMenuItem en los menús ---
        UIManager.put("RadioButtonMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("RadioButtonMenuItem.selectionForeground", currentTema.colorSeleccionTexto());

        UIManager.put("CheckBoxMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
        UIManager.put("CheckBoxMenuItem.selectionForeground", currentTema.colorSeleccionTexto());
        
        // --- Asegurar que el fondo de la barra de herramientas misma sea correcto ---
        UIManager.put( "ToolBar.background", currentTema.colorFondoPrincipal() ); // O el color de fondo que desees para la ToolBar.
        UIManager.put( "ToolBar.foreground", currentTema.colorTextoPrimario() );


        // Aplicar estilos de TabbedPane (ya existente)
        applyTabbedPaneTheme();
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

        // --- 3. Tema Azul (Blue) ---
        Color azulAcento = new Color(0, 100, 180); 
        Tema temaAzul = new Tema(
             "blue", "Tema Azul", "black", 
             new Color(237, 244, 252),  // colorFondoPrincipal
             new Color(255, 255, 255),  // colorFondoSecundario
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
}

//package vista.theme;
//
//import java.awt.Color;
//import java.awt.Insets;
//import java.awt.Window;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
//import javax.swing.JOptionPane;
//import javax.swing.SwingUtilities;
//import javax.swing.UIManager;
//import com.formdev.flatlaf.FlatDarkLaf;
//import com.formdev.flatlaf.FlatLaf;
//import com.formdev.flatlaf.FlatLightLaf;
//import controlador.VisorController;
//import servicios.ConfigKeys;
//import servicios.ConfigurationManager;
//
//public class ThemeManager {
//
//    private final ConfigurationManager configManager;
//    private final Map<String, Tema> temasDisponibles;
//    private Tema temaActual;
//    private VisorController controllerRefParaNotificacion;
//
//    // --- Colores CUSTOM_SELECTION_BLUE y CUSTOM_SELECTION_INACTIVE eliminados ---
//    // Ahora estos colores se obtendrán directamente del objeto Tema activo.
//
//    public ThemeManager(ConfigurationManager configManager) {
//        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
//        this.temasDisponibles = new ConcurrentHashMap<>();
//        cargarTemasPredeterminados();
//        // La inicialización del tema se delega al método install()
//    }
//
//    /**
//     * Instala el Look and Feel de FlatLaf con personalizaciones.
//     * DEBE llamarse al inicio de la aplicación, ANTES de crear cualquier JFrame.
//     */
//    public void install() {
//        String nombreTemaGuardado = configManager.getString("tema.nombre", "clear");
//        this.temaActual = temasDisponibles.getOrDefault(nombreTemaGuardado, obtenerTemaPorDefecto());
//
//        try {
//            // Aplicar el tema base nativo de FlatLaf
//            if (isDarkTheme(temaActual.nombreInterno())) {
//                UIManager.setLookAndFeel(new FlatDarkLaf());
//            } else {
//                UIManager.setLookAndFeel(new FlatLightLaf());
//            }
//
//            // Aplicar nuestras personalizaciones globales de color
//            applyCustomizations();
//            System.out.println("[ThemeManager] Tema inicial '" + temaActual.nombreInterno() + "' con personalizaciones instalado.");
//        } catch (Exception ex) {
//            System.err.println("Falló al inicializar el tema por defecto.");
//            ex.printStackTrace();
//        }
//    }
//
//    /**
//     * Establece un nuevo tema como el actual, cambiando el LookAndFeel en caliente
//     * y aplicando los colores personalizados.
//     *
//     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "dark").
//     * @return true si el tema se cambió exitosamente, false si no.
//     */
//    public boolean setTemaActual(String nombreTemaInterno) {
//        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
//
//        if (nuevoTema == null || nuevoTema.equals(this.temaActual)) {
//            return false;
//        }
//
//        this.temaActual = nuevoTema;
//        configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
//        System.out.println("[ThemeManager] Solicitud para cambiar tema a: " + this.temaActual.nombreInterno());
//
//        SwingUtilities.invokeLater(() -> {
//            try {
//                // 1. Establecer el Look and Feel base (Light o Dark)
//                if (isDarkTheme(nuevoTema.nombreInterno())) {
//                    UIManager.setLookAndFeel(new FlatDarkLaf());
//                } else {
//                    UIManager.setLookAndFeel(new FlatLightLaf());
//                }
//
//                // 2. Aplicar nuestras personalizaciones de color globales
//                applyCustomizations();
//
//                // 3. Actualizar la UI de toda la aplicación
//                FlatLaf.updateUI();
//
//                // 4. Notificar al controlador para que refresque lo específico de la app
//                if (controllerRefParaNotificacion != null) {
//                    controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
//                    controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
//                    controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
//                    // Importante: No es necesario reconstruir las toolbars aquí,
//                    // FlatLaf.updateUI() maneja el refresco de los componentes existentes.
//                    // Si tienes toolbars flotantes que se cierran, la lógica de AncestorListener
//                    // en ToolbarManager ya se encarga de reconstruirlas cuando sea necesario.
//                }
//
//                JOptionPane.showMessageDialog(null, 
//                    "El tema se ha cambiado a '" + nuevoTema.nombreDisplay() + "'.", 
//                    "Cambio de Tema", 
//                    JOptionPane.INFORMATION_MESSAGE);
//
//            } catch (Exception e) {
//                System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente.");
//                e.printStackTrace();
//            }
//        });
//        return true;
//    }
//
//    /**
//     * Sobrescribe las propiedades de color en el UIManager.
//     * Este es el núcleo de la solución. Se llama después de establecer un L&F.
//     */
//    private void applyCustomizations() {
//        System.out.println("[ThemeManager] Aplicando personalizaciones de color globales...");
//
//        // Usamos el tema actual para obtener los colores.
//        Tema currentTema = getTemaActual();
//        if (currentTema == null) {
//            System.err.println("ERROR [ThemeManager]: currentTema es null al aplicar personalizaciones. No se aplicarán colores personalizados.");
//            return;
//        }
//
//        // --- Configuración general de ToolBar y Botones ---
//        // Descomentar si el problema persiste, pero lo más probable es que FlatLaf
//        // lo maneje por defecto si no le decimos lo contrario.
//        UIManager.put("ToolBar.floatable", true);
//        
//        // ¡Esta línea es la sospechosa! La eliminamos o la dejamos como true
//        // para que FlatLaf aplique su pintado especial de botones de toolbar.
//        // UIManager.put("Button.paintToolBarButton", false); // <-- ELIMINADO o cambiar a true
//        UIManager.put("Button.paintToolBarButton", true); // Asegurarse de que FlatLaf pinte correctamente
//
//        UIManager.put("ToolBar.toggleButton.selectedBackgroundOpaque", true); // Asegura que el fondo sea opaco
//
//        // --- JToggleButton (incluidos los de la Toolbar) ---
//        // Usamos los colores del tema actual
//        UIManager.put("ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
//        UIManager.put("ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
//        UIManager.put("ToggleButton.selectedBorderColor", currentTema.colorBordeSeleccionActiva());
//
//        // Aseguramos la propiedad específica para JToggleButtons en Toolbars
//        UIManager.put("ToolBar.ToggleButton.selectedBackground", currentTema.colorBotonFondoActivado());
//        UIManager.put("ToolBar.ToggleButton.selectedForeground", currentTema.colorSeleccionTexto());
//        
//        // --- JList, JTable, JTree (comparten colores de selección) ---
//        UIManager.put("List.selectionBackground", currentTema.colorSeleccionFondo());
//        UIManager.put("List.selectionForeground", currentTema.colorSeleccionTexto());
//        // Podrías definir un color "selectionInactive" en Tema si lo necesitas
//        UIManager.put("List.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter()); // Ejemplo: un poco más claro
//        UIManager.put("List.selectionInactiveForeground", currentTema.colorSeleccionTexto());
//
//        UIManager.put("Table.selectionBackground", currentTema.colorSeleccionFondo());
//        UIManager.put("Table.selectionForeground", currentTema.colorSeleccionTexto());
//        UIManager.put("Table.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter());
//        
//        UIManager.put("Tree.selectionBackground", currentTema.colorSeleccionFondo());
//        UIManager.put("Tree.selectionForeground", currentTema.colorSeleccionTexto());
//        UIManager.put("Tree.selectionInactiveBackground", currentTema.colorSeleccionFondo().brighter());
//
//        // --- Para los JRadioButtonMenuItem en los menús ---
//        UIManager.put("RadioButtonMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
//        UIManager.put("RadioButtonMenuItem.selectionForeground", currentTema.colorSeleccionTexto());
//
//        // --- Para los JCheckBoxMenuItem en los menús ---
//        UIManager.put("CheckBoxMenuItem.selectionBackground", currentTema.colorSeleccionFondo());
//        UIManager.put("CheckBoxMenuItem.selectionForeground", currentTema.colorSeleccionTexto());
//        
//        // Aplicar estilos de TabbedPane
//        applyTabbedPaneTheme(); // Asegurarse de que se aplique cada vez que cambia el tema
//    }
//
//    private boolean isDarkTheme(String themeName) {
//        return "dark".equalsIgnoreCase(themeName) || "green".equalsIgnoreCase(themeName) || "orange".equalsIgnoreCase(themeName);
//    }
//    
//    private void cargarTemasPredeterminados() {
//        // --- 1. Tema Claro (Clear) ---
//        Color claroAcento = new Color(57, 105, 138); // Un azul corporativo, serio.
//        Tema temaClaro = new Tema(
//            "clear", "Tema Claro", "black",
//            new Color(245, 245, 245),   // colorFondoPrincipal: Un blanco roto, menos duro que el blanco puro.
//            new Color(255, 255, 255),   // colorFondoSecundario: Blanco puro para paneles de contenido.
//            new Color(20, 20, 20),      // colorTextoPrimario: Casi negro, pero no 100% para menos fatiga.
//            new Color(85, 85, 85),      // colorTextoSecundario: Gris oscuro para info menos importante.
//            new Color(200, 200, 200),   // colorBorde: Un gris claro para separadores sutiles.
//            new Color(20, 20, 20),      // colorBordeTitulo: Mismo que el texto principal.
//            claroAcento,                // colorSeleccionFondo: El azul de acento.
//            Color.WHITE,                // colorSeleccionTexto: Blanco para máximo contraste sobre el azul.
//            new Color(230, 230, 230),   // colorBotonFondo: Un gris ligeramente más oscuro que el fondo para que destaque un poco.
//            new Color(20, 20, 20),      // colorBotonTexto.
//            new Color(179, 205, 224),   // colorBotonFondoActivado: Un azul más suave para el estado activo.
//            new Color(173, 216, 230),   // colorBotonFondoAnimacion: Un azul claro para el feedback de hover/clic.
//            new Color(0, 122, 204),     // colorBordeSeleccionActiva: Un azul más brillante para bordes de foco.
//            claroAcento                 // colorLabelActivo: El azul de acento.
//        );
//        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
//
//        // --- 2. Tema Oscuro (Dark) ---
//        Color oscuroAcentoSeleccion = new Color(0, 122, 204); // Azul brillante para selección de lista
//        Tema temaOscuro = new Tema(
//            "dark", "Tema Oscuro", "white",
//            new Color(50, 53, 59),      // colorFondoPrincipal: Un gris "pizarra" para menús y barras. Es el color principal de la "carcasa".
//            new Color(43, 45, 49),      // colorFondoSecundario: Un gris aún más oscuro para el área de contenido (visor y lista), para que la imagen destaque.
//            new Color(220, 221, 222),   // colorTextoPrimario: Un blanco roto muy legible.
//            new Color(140, 142, 145),   // colorTextoSecundario: Un gris más suave para info secundaria.
//            new Color(60, 63, 68),      // colorBorde: Un borde sutil que separa paneles.
//            new Color(200, 201, 202),   // colorBordeTitulo: Un color de texto claro para los títulos de panel.
//            oscuroAcentoSeleccion,      // colorSeleccionFondo: El Azul Acero, claro y visible.
//            Color.WHITE,                // colorSeleccionTexto: Blanco para máximo contraste sobre la selección.
//            new Color(66, 70, 77),      // colorBotonFondo: Un gris ligeramente más claro que la barra, para que los botones "apagados" sean visibles pero integrados.
//            new Color(220, 221, 222),   // colorBotonTexto.
//            new Color(88, 101, 242),    // colorBotonFondoActivado: Un azul/púrpura tipo Discord, muy claro y moderno para indicar estado "ON".
//            new Color(75, 78, 84),      // colorBotonFondoAnimacion: Un gris más claro para el efecto "hover".
//            oscuroAcentoSeleccion,      // colorBordeSeleccionActiva: El mismo color de acento para el foco.
//            oscuroAcentoSeleccion       // colorLabelActivo: El Azul Acero, claro y visible.
//       );
//        temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
//
//        // --- 3. Tema Azul (Blue) ---
//        Color azulAcento = new Color(0, 100, 180); // Un azul más profundo que el del tema claro.
//        Tema temaAzul = new Tema(
//             "blue", "Tema Azul", "black", // Iconos negros sobre fondo claro.
//             new Color(237, 244, 252),  // colorFondoPrincipal: Un blanco muy ligeramente azulado.
//             new Color(255, 255, 255),  // colorFondoSecundario: Blanco puro.
//             new Color(10, 25, 40),     // colorTextoPrimario: Un azul muy oscuro, casi negro.
//             new Color(60, 80, 100),    // colorTextoSecundario: Un gris azulado.
//             new Color(180, 210, 240),  // colorBorde: Un azul pálido.
//             new Color(10, 25, 40),     // colorBordeTitulo.
//             azulAcento,                // colorSeleccionFondo: El azul profundo de acento.
//             Color.WHITE,               // colorSeleccionTexto.
//             new Color(225, 235, 245),  // colorBotonFondo: Un blanco azulado que destaca un poco.
//             new Color(10, 25, 40),     // colorBotonTexto.
//             azulAcento,                // colorBotonFondoActivado: El mismo azul profundo. Coherencia.
//             new Color(200, 220, 240),  // colorBotonFondoAnimacion: Un azul claro para hover.
//             new Color(0, 122, 204),    // colorBordeSeleccionActiva.
//             azulAcento                 // colorLabelActivo: colorSeleccionFondo: El azul profundo de acento.
//        );
//        temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
//
//        // --- 4. Tema Verde (Green) ---
//        Color verdeAcento = new Color(0, 204, 102); // Un verde menta brillante y moderno.
//        Tema temaVerde = new Tema(
//             "green", "Tema Verde", "green", // Iconos verdes sobre fondo oscuro.
//             new Color(20, 30, 25),     // colorFondoPrincipal: Verde muy oscuro.
//             new Color(30, 45, 38),     // colorFondoSecundario: Verde oscuro algo más claro.
//             new Color(230, 255, 230),  // colorTextoPrimario: Blanco con un levísimo tinte verde.
//             new Color(140, 190, 150),  // colorTextoSecundario: Verde pálido.
//             new Color(40, 60, 50),     // colorBorde: Verde grisáceo oscuro.
//             new Color(140, 190, 150),  // colorBordeTitulo.
//             verdeAcento,               // colorSeleccionFondo: El verde menta de acento.
//             Color.BLACK,               // colorSeleccionTexto: Negro para el máximo contraste sobre el verde menta.
//             new Color(45, 65, 55),     // colorBotonFondo.
//             new Color(230, 255, 230),  // colorBotonTexto.
//             verdeAcento,               // colorBotonFondoActivado: El mismo verde menta. Coherencia.
//             new Color(60, 80, 70),     // colorBotonFondoAnimacion: Verde oscuro para hover.
//             new Color(50, 255, 150),   // colorBordeSeleccionActiva: Verde neón para foco.
//             verdeAcento                // colorLabelActivo: El verde menta de acento.
//        );
//        temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
//
//        // --- 5. Tema Naranja (Orange) ---
//        Color naranjaAcento = new Color(230, 126, 34); // Un naranja elegante, no tan estridente.
//        Tema temaNaranja = new Tema(
//             "orange", "Tema Naranja", "orange", // Iconos naranjas sobre fondo oscuro.
//             new Color(35, 30, 25),     // colorFondoPrincipal: Gris oscuro cálido.
//             new Color(50, 40, 35),     // colorFondoSecundario: Marrón oscuro.
//             new Color(250, 230, 210),  // colorTextoPrimario: Blanco cálido (hueso).
//             new Color(190, 160, 140),  // colorTextoSecundario: "Beige" oscuro.
//             new Color(70, 60, 50),     // colorBorde.
//             new Color(190, 160, 140),  // colorBordeTitulo.
//             naranjaAcento,             // colorSeleccionFondo: El naranja de acento.
//             Color.WHITE,               // colorSeleccionTexto: Blanco para contraste.
//             new Color(65, 55, 50),     // colorBotonFondo.
//             new Color(250, 230, 210),  // colorBotonTexto.
//             naranjaAcento,             // colorBotonFondoActivado: El mismo naranja. Coherencia.
//             new Color(80, 70, 65),     // colorBotonFondoAnimacion: Marrón más claro para hover.
//             new Color(255, 152, 0),    // colorBordeSeleccionActiva: Naranja brillante para foco.
//             naranjaAcento              // colorLabelActivo: El naranja de acento.
//        );
//        temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
//    }
//
//    private Tema obtenerTemaPorDefecto() {
//        return temasDisponibles.getOrDefault("clear", temasDisponibles.values().stream().findFirst().orElse(null));
//    }
//
//    public Tema getTemaActual() {
//        return temaActual;
//    }
//    
//    public void setControllerParaNotificacion(VisorController controller) {
//        this.controllerRefParaNotificacion = controller;
//    }
//    
//    public List<String> getNombresTemasDisponibles() {
//        return List.copyOf(temasDisponibles.keySet());
//    }
//
//    public List<Tema> getTemasDisponibles() {
//        return List.copyOf(temasDisponibles.values());
//    }
//
//    public Color getFondoSecundarioParaTema(String nombreTemaInterno) {
//        if (nombreTemaInterno == null || nombreTemaInterno.isBlank()) {
//            System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: nombreTemaInterno es nulo o vacío. Devolviendo fallback.");
//            return Color.DARK_GRAY;
//        }
//        Tema tema = temasDisponibles.get(nombreTemaInterno.toLowerCase());
//        if (tema != null) {
//            Color color = tema.colorFondoSecundario();
//            if (color != null) {
//                return color;
//            } else {
//                System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: Tema '" + nombreTemaInterno + "' no tiene definido colorFondoSecundario. Devolviendo fallback.");
//                return Color.DARK_GRAY;
//            }
//        }
//        System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: No se encontró tema '" + nombreTemaInterno + "'. Devolviendo fallback.");
//        return Color.DARK_GRAY;
//    }
//
//    private void applyTabbedPaneTheme() {
//        System.out.println("[ThemeManager] Aplicando estilos de tarjeta con mayor profundidad para JTabbedPane...");
//        Tema currentTema = getTemaActual();
//        if (currentTema == null) {
//            System.err.println("ERROR [ThemeManager]: currentTema es null al aplicar estilos de TabbedPane. No se aplicarán.");
//            return;
//        }
//
//        UIManager.put("TabbedPane.tabType", "card");
//        UIManager.put("TabbedPane.selectedBackground", currentTema.colorBordeSeleccionActiva());
//        UIManager.put("TabbedPane.unselectedBackground", currentTema.colorBotonFondoAnimacion().brighter()); // Color un poco más claro para pestañas no seleccionadas
//        UIManager.put("TabbedPane.selectedForeground", currentTema.colorSeleccionTexto());
//        UIManager.put("TabbedPane.unselectedForeground", currentTema.colorTextoSecundario());
//        UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));
//        UIManager.put("TabbedPane.tabHeight", 40);
//        UIManager.put("TabbedPane.showTabSeparators", true);
//        UIManager.put("TabbedPane.tabSeparatorColor", currentTema.colorBorde());
//        UIManager.put("TabbedPane.tabBorderColor", currentTema.colorBorde().darker());
//        UIManager.put("TabbedPane.selectedTabBorderColor", currentTema.colorBordeSeleccionActiva().darker());
//        UIManager.put("TabbedPane.tabAreaBackground", currentTema.colorFondoPrincipal().darker());
//        UIManager.put("TabbedPane.contentAreaColor", currentTema.colorFondoSecundario());
//        UIManager.put("TabbedPane.selectedTabColor", currentTema.colorFondoSecundario());
//        UIManager.put("TabbedPane.contentBorderColor", currentTema.colorBorde());
//        UIManager.put("TabbedPane.contentAreaTabInsets", new Insets(8, 0, 0, 0));
//        UIManager.put("TabbedPane.tabCornerRadius", 8);
//        UIManager.put("TabbedPane.selectedTabCornerRadius", 8);
//        UIManager.put("TabbedPane.hoverColor", currentTema.colorBotonFondoAnimacion());
//        UIManager.put("TabbedPane.focusColor", currentTema.colorBordeSeleccionActiva());
//        UIManager.put("TabbedPane.selectedTabFocusColor", currentTema.colorLabelActivo());
//        System.out.println("[ThemeManager] Estilos de tarjeta con mayor profundidad para JTabbedPane aplicados.");
//    }
//} // --- FIN de la clase ThemeManager ---


//package vista.theme; 
//
//import java.awt.Color;
//import java.awt.Insets;
//import java.awt.Window;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap; // Para seguridad en hilos si fuera necesario
//
//import javax.swing.SwingUtilities;
//import javax.swing.UIManager;
//
//import com.formdev.flatlaf.FlatDarkLaf;
//import com.formdev.flatlaf.FlatLaf;
//import com.formdev.flatlaf.FlatLightLaf;
//
//import controlador.VisorController;
//import servicios.ConfigKeys;
//import servicios.ConfigurationManager; // Necesita leer/guardar el nombre
//
//public class ThemeManager {
//
//    private final ConfigurationManager configManager;
//    private final Map<String, Tema> temasDisponibles;
//    private Tema temaActual;
//
//    private VisorController controllerRefParaNotificacion;
//    
//    public ThemeManager(ConfigurationManager configManager) 
//    {
//        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
//        this.temasDisponibles = new ConcurrentHashMap<>(); // O HashMap si no hay concurrencia
//
//        // --- Cargar/Definir Temas Disponibles ---
//        cargarTemasPredeterminados(); // Carga los temas hardcodeados (o podría leerlos de otro lado)
//
//        // --- Establecer Tema Inicial ---
//        String nombreTemaInicial = configManager.getString("tema.nombre", "claro"); // Leer clave de config
//        this.temaActual = temasDisponibles.getOrDefault(nombreTemaInicial, obtenerTemaPorDefecto());
//        System.out.println("[ThemeManager] Tema inicial establecido a: " + temaActual.nombreInterno());
//        
//        applyTabbedPaneTheme();
//    }
//    
//    
//    private void cargarTemasPredeterminados() {
//        // --- 1. Tema Claro (Clear) ---
//        // Objetivo: Un look profesional, limpio y legible. El azul como color de acento.
//        Color claroAcento = new Color(57, 105, 138); // Un azul corporativo, serio.
//        Tema temaClaro = new Tema(
//            "clear", "Tema Claro", "black",
//            new Color(245, 245, 245),   // colorFondoPrincipal: Un blanco roto, menos duro que el blanco puro.
//            new Color(255, 255, 255),   // colorFondoSecundario: Blanco puro para paneles de contenido.
//            new Color(20, 20, 20),      // colorTextoPrimario: Casi negro, pero no 100% para menos fatiga.
//            new Color(85, 85, 85),      // colorTextoSecundario: Gris oscuro para info menos importante.
//            new Color(200, 200, 200),   // colorBorde: Un gris claro para separadores sutiles.
//            new Color(20, 20, 20),      // colorBordeTitulo: Mismo que el texto principal.
//            claroAcento,                // colorSeleccionFondo: El azul de acento.
//            Color.WHITE,                // colorSeleccionTexto: Blanco para máximo contraste sobre el azul.
//            new Color(230, 230, 230),   // colorBotonFondo: Un gris ligeramente más oscuro que el fondo para que destaque un poco.
//            new Color(20, 20, 20),      // colorBotonTexto.
//            new Color(179, 205, 224),   // colorBotonFondoActivado: El mismo azul de acento. Coherencia.
//            new Color(173, 216, 230),   // colorBotonFondoAnimacion: Un azul claro para el feedback de hover/clic.
//            new Color(0, 122, 204),     // colorBordeSeleccionActiva: Un azul más brillante para bordes de foco.
//            claroAcento                 // colorLabelActivo: El azul de acento.
//        );
//        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
//
//        // --- 2. Tema Oscuro (Dark) ---
//        // Objetivo: Clásico tema oscuro, fácil para la vista, con un azul eléctrico como acento.
//        Color oscuroAcentoSeleccion = new Color(0, 122, 204); // Azul brillante para selección de lista
//        Tema temaOscuro = new Tema(
//            "dark", "Tema Oscuro", "white",
//            new Color(50, 53, 59),      // colorFondoPrincipal: Un gris "pizarra" para menús y barras. Es el color principal de la "carcasa".
//            new Color(43, 45, 49),      // colorFondoSecundario: Un gris aún más oscuro para el área de contenido (visor y lista), para que la imagen destaque.
//            new Color(220, 221, 222),   // colorTextoPrimario: Un blanco roto muy legible.
//            new Color(140, 142, 145),   // colorTextoSecundario: Un gris más suave para info secundaria.
//            new Color(60, 63, 68),      // colorBorde: Un borde sutil que separa paneles.
//            new Color(200, 201, 202),   // colorBordeTitulo: Un color de texto claro para los títulos de panel.
//            oscuroAcentoSeleccion,      // colorSeleccionFondo: El Azul Acero, claro y visible.
//            Color.WHITE,                // colorSeleccionTexto: Blanco para máximo contraste sobre la selección.
//            new Color(66, 70, 77),      // colorBotonFondo: Un gris ligeramente más claro que la barra, para que los botones "apagados" sean visibles pero integrados.
//            new Color(220, 221, 222),   // colorBotonTexto.
//            new Color(88, 101, 242),    // colorBotonFondoActivado: Un azul/púrpura tipo Discord, muy claro y moderno para indicar estado "ON".
//            new Color(75, 78, 84),      // colorBotonFondoAnimacion: Un gris más claro para el efecto "hover".
//            oscuroAcentoSeleccion,      // colorBordeSeleccionActiva: El mismo color de acento para el foco.
//            oscuroAcentoSeleccion       // colorLabelActivo: El Azul Acero, claro y visible.
//       );
//        temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
//
//        // --- 3. Tema Azul (Blue) ---
//        // Objetivo: Un tema claro pero con un toque de color azul en los fondos.
//        Color azulAcento = new Color(0, 100, 180); // Un azul más profundo que el del tema claro.
//        Tema temaAzul = new Tema(
//             "blue", "Tema Azul", "black", // Iconos negros sobre fondo claro.
//             new Color(237, 244, 252),  // colorFondoPrincipal: Un blanco muy ligeramente azulado.
//             new Color(255, 255, 255),  // colorFondoSecundario: Blanco puro.
//             new Color(10, 25, 40),     // colorTextoPrimario: Un azul muy oscuro, casi negro.
//             new Color(60, 80, 100),    // colorTextoSecundario: Un gris azulado.
//             new Color(180, 210, 240),  // colorBorde: Un azul pálido.
//             new Color(10, 25, 40),     // colorBordeTitulo.
//             azulAcento,                // colorSeleccionFondo: El azul profundo de acento.
//             Color.WHITE,               // colorSeleccionTexto.
//             new Color(225, 235, 245),  // colorBotonFondo: Un blanco azulado que destaca un poco.
//             new Color(10, 25, 40),     // colorBotonTexto.
//             azulAcento,                // colorBotonFondoActivado: El mismo azul profundo. Coherencia.
//             new Color(200, 220, 240),  // colorBotonFondoAnimacion: Un azul claro para hover.
//             new Color(0, 122, 204),    // colorBordeSeleccionActiva.
//             azulAcento                 // colorLabelActivo: colorSeleccionFondo: El azul profundo de acento.
//        );
//        temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
//
//        // --- 4. Tema Verde (Green) ---
//        // Objetivo: Un look "hacker" o "matrix", oscuro y con acentos verdes.
//        Color verdeAcento = new Color(0, 204, 102); // Un verde menta brillante y moderno.
//        Tema temaVerde = new Tema(
//             "green", "Tema Verde", "green", // Iconos verdes sobre fondo oscuro.
//             new Color(20, 30, 25),     // colorFondoPrincipal: Verde muy oscuro.
//             new Color(30, 45, 38),     // colorFondoSecundario: Verde oscuro algo más claro.
//             new Color(230, 255, 230),  // colorTextoPrimario: Blanco con un levísimo tinte verde.
//             new Color(140, 190, 150),  // colorTextoSecundario: Verde pálido.
//             new Color(40, 60, 50),     // colorBorde: Verde grisáceo oscuro.
//             new Color(140, 190, 150),  // colorBordeTitulo.
//             verdeAcento,               // colorSeleccionFondo: El verde menta de acento.
//             Color.BLACK,               // colorSeleccionTexto: Negro para el máximo contraste sobre el verde menta.
//             new Color(45, 65, 55),     // colorBotonFondo.
//             new Color(230, 255, 230),  // colorBotonTexto.
//             verdeAcento,               // colorBotonFondoActivado: El mismo verde menta. Coherencia.
//             new Color(60, 80, 70),     // colorBotonFondoAnimacion: Verde oscuro para hover.
//             new Color(50, 255, 150),   // colorBordeSeleccionActiva: Verde neón para foco.
//             verdeAcento                // colorLabelActivo: El verde menta de acento.
//        );
//        temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
//
//        // --- 5. Tema Naranja (Orange) ---
//        // Objetivo: Un tema oscuro, cálido y energético, con acentos en ámbar/naranja.
//        Color naranjaAcento = new Color(230, 126, 34); // Un naranja elegante, no tan estridente.
//        Tema temaNaranja = new Tema(
//             "orange", "Tema Naranja", "orange", // Iconos naranjas sobre fondo oscuro.
//             new Color(35, 30, 25),     // colorFondoPrincipal: Gris oscuro cálido.
//             new Color(50, 40, 35),     // colorFondoSecundario: Marrón oscuro.
//             new Color(250, 230, 210),  // colorTextoPrimario: Blanco cálido (hueso).
//             new Color(190, 160, 140),  // colorTextoSecundario: "Beige" oscuro.
//             new Color(70, 60, 50),     // colorBorde.
//             new Color(190, 160, 140),  // colorBordeTitulo.
//             naranjaAcento,             // colorSeleccionFondo: El naranja de acento.
//             Color.WHITE,               // colorSeleccionTexto: Blanco para contraste.
//             new Color(65, 55, 50),     // colorBotonFondo.
//             new Color(250, 230, 210),  // colorBotonTexto.
//             naranjaAcento,             // colorBotonFondoActivado: El mismo naranja. Coherencia.
//             new Color(80, 70, 65),     // colorBotonFondoAnimacion: Marrón más claro para hover.
//             new Color(255, 152, 0),    // colorBordeSeleccionActiva: Naranja brillante para foco.
//             naranjaAcento              // colorLabelActivo: El naranja de acento.
//        );
//        temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
//    }// --- FIN DEL METODO cargarTemasPredeterminados ---
//    
//    
////    private void cargarTemasPredeterminados() {
////        // --- 1. Tema Claro (Clear) - Revisado ---
////        // Objetivo: Un look profesional, limpio y legible. El azul como color de acento.
////        Color claroAcento = new Color(57, 105, 138); // Un azul corporativo, serio.
////        Tema temaClaro = new Tema(
////            "clear", "Tema Claro", "black",
////            new Color(245, 245, 245), 	// colorFondoPrincipal: Un blanco roto, menos duro que el blanco puro.
////            new Color(255, 255, 255), 	// colorFondoSecundario: Blanco puro para paneles de contenido.
////            new Color(20, 20, 20),    	// colorTextoPrimario: Casi negro, pero no 100% para menos fatiga.
////            new Color(85, 85, 85),    	// colorTextoSecundario: Gris oscuro para info menos importante.
////            new Color(200, 200, 200), 	// colorBorde: Un gris claro para separadores sutiles.
////            new Color(20, 20, 20),    	// colorBordeTitulo: Mismo que el texto principal.
////            claroAcento,              	// colorSeleccionFondo: El azul de acento.
////            Color.WHITE,              	// colorSeleccionTexto: Blanco para máximo contraste sobre el azul.
////            new Color(230, 230, 230), 	// colorBotonFondo: Un gris ligeramente más oscuro que el fondo para que destaque un poco.
////            new Color(20, 20, 20),    	// colorBotonTexto.
////            new Color(179, 205, 224), 	// colorBotonFondoActivado: El mismo azul de acento. Coherencia.
////            new Color(173, 216, 230), 	// colorBotonFondoAnimacion: Un azul claro para el feedback de hover/clic.
////            new Color(0, 122, 204),   	// colorBordeSeleccionActiva: Un azul más brillante para bordes de foco.
////            claroAcento					// colorLabelActivo: El azul de acento.
////        );
////        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
////
////        // --- 2. Tema Oscuro (Dark) - Revisado ---
////        // Objetivo: Clásico tema oscuro, fácil para la vista, con un azul eléctrico como acento.
////        Color oscuroAcentoSeleccion = new Color(0, 122, 204); // Azul brillante para selección de lista
////        Tema temaOscuro = new Tema(
////            "dark", "Tema Oscuro", "white",
////            new Color(50, 53, 59),    	// colorFondoPrincipal: Un gris "pizarra" para menús y barras. Es el color principal de la "carcasa".
////            new Color(43, 45, 49),    	// colorFondoSecundario: Un gris aún más oscuro para el área de contenido (visor y lista), para que la imagen destaque.
////            new Color(220, 221, 222), 	// colorTextoPrimario: Un blanco roto muy legible.
////            new Color(140, 142, 145), 	// colorTextoSecundario: Un gris más suave para info secundaria.
////            new Color(60, 63, 68),    	// colorBorde: Un borde sutil que separa paneles.
////            new Color(200, 201, 202), 	// colorBordeTitulo: Un color de texto claro para los títulos de panel.
////            oscuroAcentoSeleccion,    	// colorSeleccionFondo: El Azul Acero, claro y visible.
////            Color.WHITE,              	// colorSeleccionTexto: Blanco para máximo contraste sobre la selección.
////            new Color(66, 70, 77),    	// colorBotonFondo: Un gris ligeramente más claro que la barra, para que los botones "apagados" sean visibles pero integrados.
////            new Color(220, 221, 222), 	// colorBotonTexto.
////            new Color(88, 101, 242),  	// colorBotonFondoActivado: Un azul/púrpura tipo Discord, muy claro y moderno para indicar estado "ON".
////            new Color(75, 78, 84),    	// colorBotonFondoAnimacion: Un gris más claro para el efecto "hover".
////            oscuroAcentoSeleccion,     	// colorBordeSeleccionActiva: El mismo color de acento para el foco.
////            oscuroAcentoSeleccion		// colorLabelActivo: El Azul Acero, claro y visible.
////       );
////        temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
////
////        // --- 3. Tema Azul (Blue) - Revisado ---
////        // Objetivo: Un tema claro pero con un toque de color azul en los fondos.
////        Color azulAcento = new Color(0, 100, 180); // Un azul más profundo que el del tema claro.
////        Tema temaAzul = new Tema(
////             "blue", "Tema Azul", "black", // Iconos negros sobre fondo claro.
////             new Color(237, 244, 252), 	// colorFondoPrincipal: Un blanco muy ligeramente azulado.
////             new Color(255, 255, 255), 	// colorFondoSecundario: Blanco puro.
////             new Color(10, 25, 40),    	// colorTextoPrimario: Un azul muy oscuro, casi negro.
////             new Color(60, 80, 100),   	// colorTextoSecundario: Un gris azulado.
////             new Color(180, 210, 240), 	// colorBorde: Un azul pálido.
////             new Color(10, 25, 40),    	// colorBordeTitulo.
////             azulAcento,               	// colorSeleccionFondo: El azul profundo de acento.
////             Color.WHITE,              	// colorSeleccionTexto.
////             new Color(225, 235, 245), 	// colorBotonFondo: Un blanco azulado que destaca un poco.
////             new Color(10, 25, 40),    	// colorBotonTexto.
////             azulAcento,               	// colorBotonFondoActivado: El mismo azul profundo. Coherencia.
////             new Color(200, 220, 240), 	// colorBotonFondoAnimacion: Un azul claro para hover.
////             new Color(0, 122, 204),   	// colorBordeSeleccionActiva.
////             azulAcento					// colorLabelActivo: colorSeleccionFondo: El azul profundo de acento.
////        );
////        temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
////
////        // --- 4. Tema Verde (Green) - Revisado ---
////        // Objetivo: Un look "hacker" o "matrix", oscuro y con acentos verdes.
////        Color verdeAcento = new Color(0, 204, 102); // Un verde menta brillante y moderno.
////        Tema temaVerde = new Tema(
////             "green", "Tema Verde", "green", // Iconos verdes sobre fondo oscuro.
////             new Color(20, 30, 25),    	// colorFondoPrincipal: Verde muy oscuro.
////             new Color(30, 45, 38),   	// colorFondoSecundario: Verde oscuro algo más claro.
////             new Color(230, 255, 230), 	// colorTextoPrimario: Blanco con un levísimo tinte verde.
////             new Color(140, 190, 150), 	// colorTextoSecundario: Verde pálido.
////             new Color(40, 60, 50),    	// colorBorde: Verde grisáceo oscuro.
////             new Color(140, 190, 150), 	// colorBordeTitulo.
////             verdeAcento,              	// colorSeleccionFondo: El verde menta de acento.
////             Color.BLACK,              	// colorSeleccionTexto: Negro para el máximo contraste sobre el verde menta.
////             new Color(45, 65, 55),    	// colorBotonFondo.
////             new Color(230, 255, 230), 	// colorBotonTexto.
////             verdeAcento,              	// colorBotonFondoActivado: El mismo verde menta. Coherencia.
////             new Color(60, 80, 70),    	// colorBotonFondoAnimacion: Verde oscuro para hover.
////             new Color(50, 255, 150),   // colorBordeSeleccionActiva: Verde neón para foco.
////             verdeAcento				// colorLabelActivo: El verde menta de acento.
////        );
////        temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
////
////        // --- 5. Tema Naranja (Orange) - Revisado ---
////        // Objetivo: Un tema oscuro, cálido y energético, con acentos en ámbar/naranja.
////        Color naranjaAcento = new Color(230, 126, 34); // Un naranja elegante, no tan estridente.
////        Tema temaNaranja = new Tema(
////             "orange", "Tema Naranja", "orange", // Iconos naranjas sobre fondo oscuro.
////             new Color(35, 30, 25),    	// colorFondoPrincipal: Gris oscuro cálido.
////             new Color(50, 40, 35),    	// colorFondoSecundario: Marrón oscuro.
////             new Color(250, 230, 210), 	// colorTextoPrimario: Blanco cálido (hueso).
////             new Color(190, 160, 140), 	// colorTextoSecundario: "Beige" oscuro.
////             new Color(70, 60, 50),    	// colorBorde.
////             new Color(190, 160, 140), 	// colorBordeTitulo.
////             naranjaAcento,            	// colorSeleccionFondo: El naranja de acento.
////             Color.WHITE,              	// colorSeleccionTexto: Blanco para contraste.
////             new Color(65, 55, 50),    	// colorBotonFondo.
////             new Color(250, 230, 210), 	// colorBotonTexto.
////             naranjaAcento,            	// colorBotonFondoActivado: El mismo naranja. Coherencia.
////             new Color(80, 70, 65),    	// colorBotonFondoAnimacion: Marrón más claro para hover.
////             new Color(255, 152, 0),    // colorBordeSeleccionActiva: Naranja brillante para foco.
////             naranjaAcento				// colorLabelActivo: El naranja de acento.
////        );
////        temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
////    }
//    
//
////    private void cargarTemasPredeterminados() {
////        // --- Definir los temas aquí (Hardcodeado para empezar) ---
////
////        // Tema Claro (Replicando los defaults anteriores)
////        Tema temaClaro = new Tema(
////            "clear", "Tema Claro", "black",
////            new Color(238, 238, 238), new Color(255, 255, 255), // fondo principal, secundario
////            new Color(0, 0, 0), new Color(80, 80, 80),           // texto primario, secundario
////            new Color(184, 207, 229), new Color(0, 0, 0),        // borde color, titulo
////            new Color(57, 105, 138), new Color(255, 255, 255),  // seleccion fondo, texto
////            new Color(238, 238, 238), new Color(0, 0, 0),        // boton fondo, texto
////            new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion
////            new Color(255, 140, 0)
////        );
////        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
////
////        // Tema Oscuro
////        Tema temaOscuro = new Tema(
////             "dark", "Tema Oscuro", "white",
////             new Color(45, 45, 45), new Color(60, 60, 60),      // fondo principal, secundario
////             new Color(210, 210, 210), new Color(160, 160, 160), // texto primario, secundario
////             new Color(80, 80, 80), new Color(180, 180, 180),   // borde color, titulo
////             new Color(0, 80, 150), new Color(255, 255, 255),   // seleccion fondo, texto
////             new Color(55, 55, 55), new Color(210, 210, 210),   // boton fondo, texto
////             new Color(74, 134, 154), new Color(100, 100, 100), // boton activado, animacion
////             new Color(0, 128, 255)
////        );
////         temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
////
////         // Tema Azul
////         Tema temaAzul = new Tema(
////             "blue", "Tema Azul", "blue",
////             new Color(229, 241, 251), new Color(255, 255, 255), // fondo principal, secundario
////             new Color(0, 0, 0), new Color(50, 50, 50),           // texto primario, secundario
////             new Color(153, 209, 255), new Color(0, 0, 0),        // borde color, titulo
////             new Color(0, 120, 215), new Color(255, 255, 255),  // seleccion fondo, texto
////             new Color(229, 241, 251), new Color(0, 0, 0),        // boton fondo, texto
////             new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion (igual que claro?)
////             new Color(0, 100, 200)
////         );
////         temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
////
////         //Tema Green
////         Tema temaVerde = new Tema(
////                 "green",                     // nombreInterno (minúsculas)
////                 "Tema Verde",                // nombreDisplay
////                 "green",                     // carpetaIconos
////                 new Color(10, 25, 15),       // colorFondoPrincipal (muy oscuro, tinte verde)
////                 new Color(20, 40, 30),       // colorFondoSecundario (ligeramente más claro)
////                 new Color(0, 255, 100),      // colorTextoPrimario (verde brillante)
////                 new Color(0, 180, 80),       // colorTextoSecundario (verde menos intenso)
////                 new Color(0, 80, 40),        // colorBorde (verde oscuro sutil)
////                 new Color(0, 180, 80),       // colorBordeTitulo (igual que texto secundario)
////                 new Color(0, 100, 50),       // colorSeleccionFondo (verde medio oscuro)
////                 new Color(220, 255, 230),    // colorSeleccionTexto (blanco verdoso muy claro para contraste)
////                 new Color(20, 40, 30),       // colorBotonFondo (igual que fondo secundario)
////                 new Color(0, 255, 100),      // colorBotonTexto (igual que texto primario)
////                 new Color(0, 150, 70),       // colorBotonFondoActivado (verde más intenso)
////                 new Color(0, 100, 50),        // colorBotonFondoAnimacion (igual que selección)
////                 new Color(0, 200, 0)
////            );
////            temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
////
////            // --- Tema Naranja ("Energía/HUD") ---
////            Tema temaNaranja = new Tema(
////                 "orange",                    // nombreInterno
////                 "Tema Naranja",              // nombreDisplay
////                 "orange",                    // carpetaIconos
////                 new Color(35, 30, 25),       // colorFondoPrincipal (gris oscuro cálido)
////                 new Color(55, 45, 40),       // colorFondoSecundario (gris más claro cálido)
////                 new Color(255, 150, 0),      // colorTextoPrimario (naranja brillante)
////                 new Color(200, 120, 0),      // colorTextoSecundario (naranja menos intenso)
////                 new Color(90, 70, 50),       // colorBorde (marrón oscuro/naranja)
////                 new Color(200, 120, 0),      // colorBordeTitulo (igual que texto secundario)
////                 new Color(180, 90, 0),       // colorSeleccionFondo (naranja oscuro)
////                 new Color(255, 240, 220),    // colorSeleccionTexto (blanco anaranjado muy claro)
////                 new Color(55, 45, 40),       // colorBotonFondo (igual que fondo secundario)
////                 new Color(255, 150, 0),      // colorBotonTexto (igual que texto primario)
////                 new Color(255, 120, 0),      // colorBotonFondoActivado (naranja más vivo)
////                 new Color(180, 90, 0),       // colorBotonFondoAnimacion (igual que selección)
////                 new Color(255, 90, 0)
////            );
////            temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
////    }
//    
//
//    private Tema obtenerTemaPorDefecto() {
//        // Devuelve el tema "claro" o el primero que encuentre si "claro" no está
//        return temasDisponibles.getOrDefault("claro", temasDisponibles.values().stream().findFirst().orElse(null));
//    }
//
//    /**
//     * Obtiene el objeto Tema actualmente activo.
//     * @return El Tema actual.
//     */
//    public Tema getTemaActual() {
//        // Podría volver a leer de config aquí si fuera necesario sincronizar,
//        // pero generalmente se confía en que setCurrentTheme lo mantiene actualizado.
//        return temaActual;
//    }
//
//    
//    /**
//     * Establece un nuevo tema como el actual.
//     * Esta versión está refactorizada para usar FlatLaf de forma nativa.
//     * Cambia el LookAndFeel en caliente y aplica los colores personalizados del objeto Tema.
//     *
//     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "oscuro").
//     * @return true si el tema se cambió exitosamente, false si no.
//     */
//    /**
//     * Establece un nuevo tema como el actual.
//     * Esta versión está refactorizada para usar FlatLaf de forma nativa.
//     * Cambia el LookAndFeel en caliente y aplica los colores personalizados del objeto Tema.
//     *
//     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "oscuro").
//     * @return true si el tema se cambió exitosamente, false si no.
//     */
//    public boolean setTemaActual(String nombreTemaInterno) {
//        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
//
//        if (nuevoTema == null || nuevoTema.equals(this.temaActual)) {
//            return false;
//        }
//
//        this.temaActual = nuevoTema;
//        configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
//        System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
//
//        SwingUtilities.invokeLater(() -> {
//            try {
//                // --- PASO 1: APLICAR EL TEMA BASE NATIVO ---
//                System.out.println("  -> [EDT] Aplicando Look and Feel base de FlatLaf...");
//                if ("dark".equalsIgnoreCase(nuevoTema.nombreInterno())) {
//                    FlatDarkLaf.setup();
//                } else {
//                    FlatLightLaf.setup();
//                }
//
//                // --- PASO 2: APLICAR NUESTRAS REGLAS DE COHERENCIA ---
//                System.out.println("  -> [EDT] Aplicando personalizaciones de coherencia visual...");
//
//                // a) Definir color turquesa explícitamente
//                Color colorTurquesa = new Color(64, 200, 196); // Ajusta los valores RGB si necesitas otro tono
//                Color colorTextoSeleccion = UIManager.getColor("List.selectionForeground");
//
//                // b) Forzar el color de selección para listas y botones
//                UIManager.put("List.selectionInactiveBackground", colorTurquesa);
//                UIManager.put("List.selectionInactiveForeground", colorTextoSeleccion);
//
//                // c) Forzar el color de fondo para JToggleButton y botones en JToolBar
//                UIManager.put("ToggleButton.selectedBackground", colorTurquesa);
//                UIManager.put("Button.selectedBackground", colorTurquesa); // Cubrir botones en JToolBar
//                UIManager.put("ToolBar.Button.selectedBackground", colorTurquesa); // Variante para toolbar
//                UIManager.put("ToggleButton.background", colorTurquesa); // Fondo general para toggle buttons
//                UIManager.put("ToggleButton.selectedForeground", colorTextoSeleccion);
//                UIManager.put("Button.selectedForeground", colorTextoSeleccion);
//                UIManager.put("ToolBar.Button.selectedForeground", colorTextoSeleccion);
//
//                // d) Log para depuración
//                System.out.println("Color asignado a ToggleButton.selectedBackground: " + UIManager.getColor("ToggleButton.selectedBackground"));
//                System.out.println("Color asignado a Button.selectedBackground: " + UIManager.getColor("Button.selectedBackground"));
//                System.out.println("Color asignado a ToolBar.Button.selectedBackground: " + UIManager.getColor("ToolBar.Button.selectedBackground"));
//
//                // --- PASO 3: LIMPIAR EL CACHÉ DE TOOLBARS ---
//                System.out.println("  -> [EDT] Limpiando caché de toolbars...");
//                controllerRefParaNotificacion.getToolbarManager().clearToolbarCache();
//
//                // --- PASO 4: REFRESCO GLOBAL ---
//                System.out.println("  -> [EDT] Llamando a FlatLaf.updateUI() para aplicar todo...");
//                FlatLaf.updateUI();
//
//                // --- PASO 5: REFRESCO DE NUESTRA APP ---
//                if (controllerRefParaNotificacion != null) {
//                    controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
//                    controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
//                    controllerRefParaNotificacion.getToolbarManager().reconstruirContenedorDeToolbars(
//                        controllerRefParaNotificacion.getModel().getCurrentWorkMode()
//                    );
//                    controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
//                }
//
//                // --- PASO 6: FORZAR REPINTADO DE TODAS LAS VENTANAS ---
//                for (Window window : Window.getWindows()) {
//                    SwingUtilities.updateComponentTreeUI(window);
//                    window.repaint();
//                }
//
//            } catch (Exception e) {
//                System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente.");
//                e.printStackTrace();
//            }
//        });
//
//        return true;
//    }// --- Fin del método setTemaActual ---
//    
////    public boolean setTemaActual(String nombreTemaInterno) {
////        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
////        
////        if (nuevoTema == null) {
////            System.err.println("WARN [ThemeManager]: No se encontró el tema con nombre: " + nombreTemaInterno);
////            return false;
////        }
////        
////        if (!nuevoTema.equals(this.temaActual)) {
////            this.temaActual = nuevoTema;
////            
////            configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
////            System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
////            
////            SwingUtilities.invokeLater(() -> {
////                try {
////                    // --- PASO 1: APLICAR LOOK AND FEEL BASE ---
////                    System.out.println("  -> [EDT] Aplicando Look and Feel de FlatLaf...");
////                    if ("dark".equalsIgnoreCase(nuevoTema.nombreInterno()) || "green".equalsIgnoreCase(nuevoTema.nombreInterno()) || "orange".equalsIgnoreCase(nuevoTema.nombreInterno())) {
////                        FlatDarkLaf.setup();
////                    } else {
////                        FlatLightLaf.setup();
////                    }
////
////                    // --- PASO 2: PERSONALIZAR COLORES GLOBALES ---
////                    System.out.println("  -> [EDT] Personalizando colores del UIManager con el tema: " + nuevoTema.nombreDisplay());
////                    
////                    // --- ARREGLO PARA LA SELECCIÓN DE LA LISTA (GRIS VS AZUL) ---
////                    // Le decimos que para el fondo de selección INACTIVO, use el mismo color que el ACTIVO.
////                    UIManager.put("List.selectionInactiveBackground", nuevoTema.colorSeleccionFondo());
////                    // Hacemos lo mismo para el color del texto, para asegurar la legibilidad.
////                    UIManager.put("List.selectionInactiveForeground", nuevoTema.colorSeleccionTexto());
////                    
////                    // --- ARREGLO PARA EL COLOR DE LOS BOTONES MARCADOS ---
////                    // Le decimos que el color de fondo para un ToggleButton seleccionado...
////                    UIManager.put("ToggleButton.selectedBackground", nuevoTema.colorSeleccionFondo()); // <-- MODIFICADO: Usar el color de selección de la lista.
////                    // ...y el color para uno en una ToolBar...
////                    UIManager.put("ToolBar.ToggleButton.selectedBackground", nuevoTema.colorSeleccionFondo()); // <-- MODIFICADO: Usar el color de selección de la lista.
////                    // ...sea el mismo azul de la selección de la lista para que todo sea consistente.
////                    
////                    // El resto de tus propiedades se quedan como están
////                    UIManager.put("ToggleButton.selectedForeground", nuevoTema.colorSeleccionTexto());
////                    UIManager.put("ToggleButton.selectedBorderColor", nuevoTema.colorBordeSeleccionActiva());
////                    UIManager.put("Panel.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("ToolBar.background", nuevoTema.colorFondoPrincipal());
////                    // ... etc ...
////                    UIManager.put("Button.focusedBorderColor", nuevoTema.colorBordeSeleccionActiva());
////                    
////                    // --- PASO 3: APLICAR ESTILOS DE JTABBEDPANE ---
////                    System.out.println("  -> [EDT] Aplicando estilos específicos para JTabbedPane...");
////                    applyTabbedPaneTheme();
////
////                    // --- PASO 4: REFRESCO GLOBAL DE LA UI DE SWING ---
////                    System.out.println("  -> [EDT] Llamando a FlatLaf.updateUI() para refrescar todos los componentes...");
////                    FlatLaf.updateUI();
////
////                    // --- EL RESTO DE PASOS SE MANTIENEN IGUAL ---
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getViewManager() != null) {
////                        controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
////                    }        
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getActionFactory() != null) {
////                        controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
////                    }
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getToolbarManager() != null && controllerRefParaNotificacion.getModel() != null) {
////                        controllerRefParaNotificacion.getToolbarManager().reconstruirContenedorDeToolbars(
////                            controllerRefParaNotificacion.getModel().getCurrentWorkMode()
////                        );
////                    }                    
////                    if (controllerRefParaNotificacion != null) {
////                        controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
////                    }
////
////                } catch (Exception e) {
////                    System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente con FlatLaf.");
////                    e.printStackTrace();
////                }
////            });
////            
////            return true;
////            
////        } else {
////            System.out.println("[ThemeManager] Intento de establecer el tema que ya está activo: " + nombreTemaInterno);
////            return false;
////        }
////        
////    }// --- Fin del método setTemaActual ---
//    
//    
////    /**
////     * Establece un nuevo tema como el actual.
////     * Esta versión está refactorizada para usar FlatLaf de forma nativa.
////     * Cambia el LookAndFeel en caliente y aplica los colores personalizados del objeto Tema.
////     *
////     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "oscuro").
////     * @return true si el tema se cambió exitosamente, false si no.
////     */
////    public boolean setTemaActual(String nombreTemaInterno) {
////        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
////        
////        if (nuevoTema == null) {
////            System.err.println("WARN [ThemeManager]: No se encontró el tema con nombre: " + nombreTemaInterno);
////            return false;
////        }
////        
////        if (!nuevoTema.equals(this.temaActual)) {
////            this.temaActual = nuevoTema;
////            
////            configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
////            System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
////            
////            SwingUtilities.invokeLater(() -> {
////                try {
////                    // --- PASO 1: APLICAR LOOK AND FEEL BASE ---
////                    System.out.println("  -> [EDT] Aplicando Look and Feel de FlatLaf...");
////                    if ("dark".equalsIgnoreCase(nuevoTema.nombreInterno()) || "green".equalsIgnoreCase(nuevoTema.nombreInterno()) || "orange".equalsIgnoreCase(nuevoTema.nombreInterno())) {
////                        FlatDarkLaf.setup();
////                    } else {
////                        FlatLightLaf.setup();
////                    }
////
////                    // --- PASO 2: PERSONALIZAR COLORES GLOBALES ---
////                    System.out.println("  -> [EDT] Personalizando colores del UIManager con el tema: " + nuevoTema.nombreDisplay());
////                    UIManager.put("Panel.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("ToolBar.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("MenuBar.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("Menu.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("MenuItem.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("PopupMenu.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("List.background", nuevoTema.colorFondoSecundario());
////                    UIManager.put("Viewport.background", nuevoTema.colorFondoSecundario());
////                    UIManager.put("ScrollPane.background", nuevoTema.colorFondoPrincipal());
////                    UIManager.put("Button.background", nuevoTema.colorBotonFondo());
////                    UIManager.put("Component.foreground", nuevoTema.colorTextoPrimario());
////                    UIManager.put("Label.foreground", nuevoTema.colorTextoPrimario());
////                    UIManager.put("Button.foreground", nuevoTema.colorBotonTexto());
////                    UIManager.put("Menu.foreground", nuevoTema.colorTextoPrimario());
////                    UIManager.put("MenuItem.foreground", nuevoTema.colorTextoPrimario());
////                    UIManager.put("TitledBorder.titleColor", nuevoTema.colorBordeTitulo());
////                    UIManager.put("List.selectionBackground", nuevoTema.colorSeleccionFondo());
////                    UIManager.put("List.selectionForeground", nuevoTema.colorSeleccionTexto());
////                    UIManager.put("Component.borderColor", nuevoTema.colorBorde());
////                    UIManager.put("Separator.borderColor", nuevoTema.colorBorde());
////                    UIManager.put("MenuBar.borderColor", nuevoTema.colorBorde());
////                    UIManager.put("Component.focusColor", nuevoTema.colorBordeSeleccionActiva());
////                    UIManager.put("Button.focusedBorderColor", nuevoTema.colorBordeSeleccionActiva());
////
////                    // --- PASO 3: REFRESCO GLOBAL DE LA UI DE SWING ---
////                    System.out.println("  -> [EDT] Llamando a FlatLaf.updateUI() para refrescar todos los componentes...");
////                    FlatLaf.updateUI();
////
////                    // --- PASO 4: REFRESCO DE COMPONENTES PERSONALIZADOS ---
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getViewManager() != null) {
////                        System.out.println("  -> [EDT] Refrescando fondo del panel de imagen al valor por defecto del nuevo tema...");
////                        controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
////                    }        
////                    
////                    // --- PASO 5: ACTUALIZAR ICONOS EN CACHÉ DE ACTIONS ---
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getActionFactory() != null) {
////                        System.out.println("  -> [EDT] Actualizando los iconos cacheados en las Actions...");
////                        controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
////                    }
////                    
////                    // --- PASO 6: RECONSTRUIR BARRAS DE HERRAMIENTAS ---
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getToolbarManager() != null && controllerRefParaNotificacion.getModel() != null) {
////                        System.out.println("  -> [EDT] Reconstruyendo barras de herramientas...");
////                        controllerRefParaNotificacion.getToolbarManager().reconstruirContenedorDeToolbars(
////                            controllerRefParaNotificacion.getModel().getCurrentWorkMode()
////                        );
////                    }                    
////                    
////                    // --- INICIO DE LA MODIFICACIÓN ---
////                    // --- PASO 7 (NUEVO): SINCRONIZAR ESTADO VISUAL DE BOTONES TOGGLE ---
////                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getConfigApplicationManager() != null) {
////                        System.out.println("  -> [EDT] Sincronizando estado visual de los botones toggle con el nuevo tema...");
////                        // Llamamos al nuevo método centralizado
////                        controllerRefParaNotificacion.getConfigApplicationManager().sincronizarAparienciaTodosLosToggles();
////                    }
////                    // --- FIN DE LA MODIFICACIÓN ---
////                    
////                    // --- PASO 8: SINCRONIZAR RADIOS DEL MENÚ DE TEMA ---
////                    if (controllerRefParaNotificacion != null) {
////                        controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
////                    }
////
////                } catch (Exception e) {
////                    System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente con FlatLaf.");
////                    e.printStackTrace();
////                }
////            });
////            
////            return true;
////            
////        } else {
////            System.out.println("[ThemeManager] Intento de establecer el tema que ya está activo: " + nombreTemaInterno);
////            return false;
////        }
////    } // --- Fin del método setTemaActual ---
//    
//    
////    private void notificarCambioDeTemaAlControlador(Tema temaAnterior, Tema temaNuevo) {
////        if (this.controllerRefParaNotificacion != null) {
////            // Llamar a un método en VisorController para que actualice las Actions de tema
////            // y cualquier otra cosa que dependa del tema.
////            this.controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
////            // Podrías pasar temaAnterior y temaNuevo si el controller los necesita
////            // this.controllerRefParaNotificacion.temaHaCambiado(temaAnterior, temaNuevo);
////        } else {
////            System.err.println("WARN [ThemeManager]: controllerRefParaNotificacion es null. No se pudo notificar cambio de tema para actualizar Actions de UI.");
////        }
////    }
//    
//    
//    public void setControllerParaNotificacion(VisorController controller) {
//        this.controllerRefParaNotificacion = controller; // No es necesario Objects.requireNonNull aquí si se permite null inicialmente
//                                                        // pero AppInitializer debería pasarlo no nulo.
//    }
//    
//
//    /**
//     * Obtiene una lista de los nombres internos de los temas disponibles.
//     * Útil para poblar menús o selectores.
//     * @return Una lista de los nombres de tema disponibles.
//     */
//    public List<String> getNombresTemasDisponibles() {
//        return List.copyOf(temasDisponibles.keySet()); // Devuelve copia inmutable
//    }
//
//     /**
//     * Obtiene una lista de los objetos Tema disponibles.
//     * @return Una lista de los Temas.
//     */
//     public List<Tema> getTemasDisponibles() {
//         return List.copyOf(temasDisponibles.values()); // Devuelve copia inmutable
//     }
//     
//     
//     /**
//      * Obtiene el color de fondo secundario para un tema específico.
//      * Este es el color que se usará como fondo para la previsualización de iconos.
//      *
//      * @param nombreTemaInterno El nombre interno del tema (ej. "clear", "dark").
//      * @return El Color de fondo secundario del tema, o un color de fallback (ej. Color.DARK_GRAY)
//      *         si el tema no se encuentra o el color no está definido.
//      */
//     public Color getFondoSecundarioParaTema(String nombreTemaInterno) {
//         if (nombreTemaInterno == null || nombreTemaInterno.isBlank()) {
//             System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: nombreTemaInterno es nulo o vacío. Devolviendo fallback.");
//             return Color.DARK_GRAY; // Fallback
//         }
//         Tema tema = temasDisponibles.get(nombreTemaInterno.toLowerCase()); // Asegurar búsqueda en minúsculas
//         if (tema != null) {
//             // Asumiendo que la clase Tema tiene un método público/campo colorFondoSecundario()
//             Color color = tema.colorFondoSecundario(); 
//             if (color != null) {
//                 return color;
//             } else {
//                 System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: Tema '" + nombreTemaInterno + "' no tiene definido colorFondoSecundario. Devolviendo fallback.");
//                 return Color.DARK_GRAY; // Fallback si el color específico es null en el tema
//             }
//         }
//         System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: No se encontró tema '" + nombreTemaInterno + "'. Devolviendo fallback.");
//         return Color.DARK_GRAY; // Fallback si el tema no existe
//     }
//     
//     
//     /**
//      * Aplica configuraciones específicas de estilo para JTabbedPane
//      * usando UIManager, para darles un aspecto más de "tarjeta" con mayor profundidad.
//      * Se llama después de configurar el LookAndFeel FlatLaf y de aplicar los colores globales.
//      */
//     private void applyTabbedPaneTheme() {
//         System.out.println("[ThemeManager] Aplicando estilos de tarjeta con mayor profundidad para JTabbedPane...");
//
//         Tema currentTema = getTemaActual();
//
//         // 1. Tipo de pestaña: "card" es fundamental para el efecto de tarjeta.
//         UIManager.put("TabbedPane.tabType", "card");
//
//         // 2. Fondos de las pestañas individuales (la "tarjeta" en sí)
//         //    selectedBackground: Color de fondo de la pestaña activa.
//         //    Usamos colorBordeSeleccionActiva para que la pestaña activa sea un acento fuerte.
//         UIManager.put("TabbedPane.selectedBackground", currentTema.colorBordeSeleccionActiva()); 
//         
//         //    unselectedBackground: Color de fondo de las pestañas inactivas.
//         //    Aquí es donde ajustamos el tono. Usamos el color de animación (hover) y lo aclaramos.
//         //    Esto debería darle el "tono un poco más claro que el azul de cuando tenemos el ratón encima".
//         UIManager.put("TabbedPane.unselectedBackground", currentTema.colorBotonFondoAnimacion().brighter()); 
//
//         // 3. Colores de texto de las pestañas
//         UIManager.put("TabbedPane.selectedForeground", currentTema.colorSeleccionTexto()); // Texto de la pestaña activa (alto contraste)
//         UIManager.put("TabbedPane.unselectedForeground", currentTema.colorTextoSecundario()); // Texto de las pestañas inactivas (más tenue)
//         
//         // 4. Acolchado (padding) y altura para hacerlas prominentes y con forma de botón/tarjeta
//         UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16)); // Padding interno de las pestañas
//         UIManager.put("TabbedPane.tabHeight", 40); // Altura total de cada pestaña
//
//         // 5. Bordes de las pestañas y separadores
//         UIManager.put("TabbedPane.showTabSeparators", true); // Muestra líneas divisorias entre pestañas
//         UIManager.put("TabbedPane.tabSeparatorColor", currentTema.colorBorde()); // Color de los separadores verticales
//
//         //    Color del borde de las pestañas individuales (seleccionada y no seleccionadas)
//         //    Estos valores son cruciales para que la "tarjeta" tenga un contorno visible.
//         //    Para las no seleccionadas, hacemos el borde un poco más oscuro que el borde general para que resalte.
//         UIManager.put("TabbedPane.tabBorderColor", currentTema.colorBorde().darker()); 
//         //    Para la seleccionada, usamos un derivado del color de acento.
//         UIManager.put("TabbedPane.selectedTabBorderColor", currentTema.colorBordeSeleccionActiva().darker()); 
//
//         // 6. Fondos y bordes del área general del JTabbedPane
//         //    tabAreaBackground: El fondo de toda la barra donde se asientan las pestañas.
//         //    Lo hacemos ligeramente más oscuro que el fondo principal para que las pestañas resalten sobre él.
//         UIManager.put("TabbedPane.tabAreaBackground", currentTema.colorFondoPrincipal().darker()); 
//         
//         //    contentAreaColor: El fondo del panel de contenido que muestra cada pestaña.
//         UIManager.put("TabbedPane.contentAreaColor", currentTema.colorFondoSecundario()); 
//         //    selectedTabColor: El color que se "extiende" desde la pestaña seleccionada al contenido.
//         UIManager.put("TabbedPane.selectedTabColor", currentTema.colorFondoSecundario()); 
//         
//         //    contentBorderColor: El borde entre la barra de pestañas y el área de contenido.
//         UIManager.put("TabbedPane.contentBorderColor", currentTema.colorBorde()); 
//         //    contentAreaTabInsets: Espacio entre la parte inferior de las pestañas y el borde superior del área de contenido.
//         //    Esto crea una separación visual entre las pestañas y el contenido real del panel.
//         UIManager.put("TabbedPane.contentAreaTabInsets", new Insets(8, 0, 0, 0)); // Añadimos un padding superior para la zona de contenido
//
//         // 7. Esquinas redondeadas para un look de tarjeta más moderno
//         UIManager.put("TabbedPane.tabCornerRadius", 8); // Radio de esquina para pestañas no seleccionadas
//         UIManager.put("TabbedPane.selectedTabCornerRadius", 8); // Mismo para la seleccionada
//
//         // 8. Colores de interacción (hover/foco)
//         UIManager.put("TabbedPane.hoverColor", currentTema.colorBotonFondoAnimacion()); // Color al pasar el ratón por encima
//         UIManager.put("TabbedPane.focusColor", currentTema.colorBordeSeleccionActiva()); // Color del foco (ej. con TAB)
//         UIManager.put("TabbedPane.selectedTabFocusColor", currentTema.colorLabelActivo()); // Color del foco para la pestaña ya seleccionada
//         
//         System.out.println("[ThemeManager] Estilos de tarjeta con mayor profundidad para JTabbedPane aplicados.");
//         
//     } // --- Fin del nuevo método applyTabbedPaneTheme ---
//     
//     
//} // --- FIN de la clase ThemeManager ---