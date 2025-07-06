package vista.theme; 

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap; // Para seguridad en hilos si fuera necesario

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import controlador.VisorController;
import servicios.ConfigKeys;
import servicios.ConfigurationManager; // Necesita leer/guardar el nombre

public class ThemeManager {

    private final ConfigurationManager configManager;
    private final Map<String, Tema> temasDisponibles;
    private Tema temaActual;

    private VisorController controllerRefParaNotificacion;
    
    public ThemeManager(ConfigurationManager configManager) 
    {
        this.configManager = Objects.requireNonNull(configManager, "ConfigurationManager no puede ser null");
        this.temasDisponibles = new ConcurrentHashMap<>(); // O HashMap si no hay concurrencia

        // --- Cargar/Definir Temas Disponibles ---
        cargarTemasPredeterminados(); // Carga los temas hardcodeados (o podría leerlos de otro lado)

        // --- Establecer Tema Inicial ---
        String nombreTemaInicial = configManager.getString("tema.nombre", "claro"); // Leer clave de config
        this.temaActual = temasDisponibles.getOrDefault(nombreTemaInicial, obtenerTemaPorDefecto());
        System.out.println("[ThemeManager] Tema inicial establecido a: " + temaActual.nombreInterno());
    }
    
    
    private void cargarTemasPredeterminados() {
        // --- 1. Tema Claro (Clear) - Revisado ---
        // Objetivo: Un look profesional, limpio y legible. El azul como color de acento.
        Color claroAcento = new Color(57, 105, 138); // Un azul corporativo, serio.
        Tema temaClaro = new Tema(
            "clear", "Tema Claro", "black",
            new Color(245, 245, 245), 	// colorFondoPrincipal: Un blanco roto, menos duro que el blanco puro.
            new Color(255, 255, 255), 	// colorFondoSecundario: Blanco puro para paneles de contenido.
            new Color(20, 20, 20),    	// colorTextoPrimario: Casi negro, pero no 100% para menos fatiga.
            new Color(85, 85, 85),    	// colorTextoSecundario: Gris oscuro para info menos importante.
            new Color(200, 200, 200), 	// colorBorde: Un gris claro para separadores sutiles.
            new Color(20, 20, 20),    	// colorBordeTitulo: Mismo que el texto principal.
            claroAcento,              	// colorSeleccionFondo: El azul de acento.
            Color.WHITE,              	// colorSeleccionTexto: Blanco para máximo contraste sobre el azul.
            new Color(230, 230, 230), 	// colorBotonFondo: Un gris ligeramente más oscuro que el fondo para que destaque un poco.
            new Color(20, 20, 20),    	// colorBotonTexto.
            new Color(179, 205, 224), 	// colorBotonFondoActivado: El mismo azul de acento. Coherencia.
            new Color(173, 216, 230), 	// colorBotonFondoAnimacion: Un azul claro para el feedback de hover/clic.
            new Color(0, 122, 204),   	// colorBordeSeleccionActiva: Un azul más brillante para bordes de foco.
            claroAcento					// colorLabelActivo: El azul de acento.
        );
        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);

        // --- 2. Tema Oscuro (Dark) - Revisado ---
        // Objetivo: Clásico tema oscuro, fácil para la vista, con un azul eléctrico como acento.
        Color oscuroAcentoSeleccion = new Color(0, 122, 204); // Azul brillante para selección de lista
        Tema temaOscuro = new Tema(
            "dark", "Tema Oscuro", "white",
            new Color(50, 53, 59),    	// colorFondoPrincipal: Un gris "pizarra" para menús y barras. Es el color principal de la "carcasa".
            new Color(43, 45, 49),    	// colorFondoSecundario: Un gris aún más oscuro para el área de contenido (visor y lista), para que la imagen destaque.
            new Color(220, 221, 222), 	// colorTextoPrimario: Un blanco roto muy legible.
            new Color(140, 142, 145), 	// colorTextoSecundario: Un gris más suave para info secundaria.
            new Color(60, 63, 68),    	// colorBorde: Un borde sutil que separa paneles.
            new Color(200, 201, 202), 	// colorBordeTitulo: Un color de texto claro para los títulos de panel.
            oscuroAcentoSeleccion,    	// colorSeleccionFondo: El Azul Acero, claro y visible.
            Color.WHITE,              	// colorSeleccionTexto: Blanco para máximo contraste sobre la selección.
            new Color(66, 70, 77),    	// colorBotonFondo: Un gris ligeramente más claro que la barra, para que los botones "apagados" sean visibles pero integrados.
            new Color(220, 221, 222), 	// colorBotonTexto.
            new Color(88, 101, 242),  	// colorBotonFondoActivado: Un azul/púrpura tipo Discord, muy claro y moderno para indicar estado "ON".
            new Color(75, 78, 84),    	// colorBotonFondoAnimacion: Un gris más claro para el efecto "hover".
            oscuroAcentoSeleccion,     	// colorBordeSeleccionActiva: El mismo color de acento para el foco.
            oscuroAcentoSeleccion		// colorLabelActivo: El Azul Acero, claro y visible.
       );
        temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);

        // --- 3. Tema Azul (Blue) - Revisado ---
        // Objetivo: Un tema claro pero con un toque de color azul en los fondos.
        Color azulAcento = new Color(0, 100, 180); // Un azul más profundo que el del tema claro.
        Tema temaAzul = new Tema(
             "blue", "Tema Azul", "black", // Iconos negros sobre fondo claro.
             new Color(237, 244, 252), 	// colorFondoPrincipal: Un blanco muy ligeramente azulado.
             new Color(255, 255, 255), 	// colorFondoSecundario: Blanco puro.
             new Color(10, 25, 40),    	// colorTextoPrimario: Un azul muy oscuro, casi negro.
             new Color(60, 80, 100),   	// colorTextoSecundario: Un gris azulado.
             new Color(180, 210, 240), 	// colorBorde: Un azul pálido.
             new Color(10, 25, 40),    	// colorBordeTitulo.
             azulAcento,               	// colorSeleccionFondo: El azul profundo de acento.
             Color.WHITE,              	// colorSeleccionTexto.
             new Color(225, 235, 245), 	// colorBotonFondo: Un blanco azulado que destaca un poco.
             new Color(10, 25, 40),    	// colorBotonTexto.
             azulAcento,               	// colorBotonFondoActivado: El mismo azul profundo. Coherencia.
             new Color(200, 220, 240), 	// colorBotonFondoAnimacion: Un azul claro para hover.
             new Color(0, 122, 204),   	// colorBordeSeleccionActiva.
             azulAcento					// colorLabelActivo: colorSeleccionFondo: El azul profundo de acento.
        );
        temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);

        // --- 4. Tema Verde (Green) - Revisado ---
        // Objetivo: Un look "hacker" o "matrix", oscuro y con acentos verdes.
        Color verdeAcento = new Color(0, 204, 102); // Un verde menta brillante y moderno.
        Tema temaVerde = new Tema(
             "green", "Tema Verde", "green", // Iconos verdes sobre fondo oscuro.
             new Color(20, 30, 25),    	// colorFondoPrincipal: Verde muy oscuro.
             new Color(30, 45, 38),   	// colorFondoSecundario: Verde oscuro algo más claro.
             new Color(230, 255, 230), 	// colorTextoPrimario: Blanco con un levísimo tinte verde.
             new Color(140, 190, 150), 	// colorTextoSecundario: Verde pálido.
             new Color(40, 60, 50),    	// colorBorde: Verde grisáceo oscuro.
             new Color(140, 190, 150), 	// colorBordeTitulo.
             verdeAcento,              	// colorSeleccionFondo: El verde menta de acento.
             Color.BLACK,              	// colorSeleccionTexto: Negro para el máximo contraste sobre el verde menta.
             new Color(45, 65, 55),    	// colorBotonFondo.
             new Color(230, 255, 230), 	// colorBotonTexto.
             verdeAcento,              	// colorBotonFondoActivado: El mismo verde menta. Coherencia.
             new Color(60, 80, 70),    	// colorBotonFondoAnimacion: Verde oscuro para hover.
             new Color(50, 255, 150),   // colorBordeSeleccionActiva: Verde neón para foco.
             verdeAcento				// colorLabelActivo: El verde menta de acento.
        );
        temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);

        // --- 5. Tema Naranja (Orange) - Revisado ---
        // Objetivo: Un tema oscuro, cálido y energético, con acentos en ámbar/naranja.
        Color naranjaAcento = new Color(230, 126, 34); // Un naranja elegante, no tan estridente.
        Tema temaNaranja = new Tema(
             "orange", "Tema Naranja", "orange", // Iconos naranjas sobre fondo oscuro.
             new Color(35, 30, 25),    	// colorFondoPrincipal: Gris oscuro cálido.
             new Color(50, 40, 35),    	// colorFondoSecundario: Marrón oscuro.
             new Color(250, 230, 210), 	// colorTextoPrimario: Blanco cálido (hueso).
             new Color(190, 160, 140), 	// colorTextoSecundario: "Beige" oscuro.
             new Color(70, 60, 50),    	// colorBorde.
             new Color(190, 160, 140), 	// colorBordeTitulo.
             naranjaAcento,            	// colorSeleccionFondo: El naranja de acento.
             Color.WHITE,              	// colorSeleccionTexto: Blanco para contraste.
             new Color(65, 55, 50),    	// colorBotonFondo.
             new Color(250, 230, 210), 	// colorBotonTexto.
             naranjaAcento,            	// colorBotonFondoActivado: El mismo naranja. Coherencia.
             new Color(80, 70, 65),    	// colorBotonFondoAnimacion: Marrón más claro para hover.
             new Color(255, 152, 0),    // colorBordeSeleccionActiva: Naranja brillante para foco.
             naranjaAcento				// colorLabelActivo: El naranja de acento.
        );
        temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
    }
    

//    private void cargarTemasPredeterminados() {
//        // --- Definir los temas aquí (Hardcodeado para empezar) ---
//
//        // Tema Claro (Replicando los defaults anteriores)
//        Tema temaClaro = new Tema(
//            "clear", "Tema Claro", "black",
//            new Color(238, 238, 238), new Color(255, 255, 255), // fondo principal, secundario
//            new Color(0, 0, 0), new Color(80, 80, 80),           // texto primario, secundario
//            new Color(184, 207, 229), new Color(0, 0, 0),        // borde color, titulo
//            new Color(57, 105, 138), new Color(255, 255, 255),  // seleccion fondo, texto
//            new Color(238, 238, 238), new Color(0, 0, 0),        // boton fondo, texto
//            new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion
//            new Color(255, 140, 0)
//        );
//        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);
//
//        // Tema Oscuro
//        Tema temaOscuro = new Tema(
//             "dark", "Tema Oscuro", "white",
//             new Color(45, 45, 45), new Color(60, 60, 60),      // fondo principal, secundario
//             new Color(210, 210, 210), new Color(160, 160, 160), // texto primario, secundario
//             new Color(80, 80, 80), new Color(180, 180, 180),   // borde color, titulo
//             new Color(0, 80, 150), new Color(255, 255, 255),   // seleccion fondo, texto
//             new Color(55, 55, 55), new Color(210, 210, 210),   // boton fondo, texto
//             new Color(74, 134, 154), new Color(100, 100, 100), // boton activado, animacion
//             new Color(0, 128, 255)
//        );
//         temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);
//
//         // Tema Azul
//         Tema temaAzul = new Tema(
//             "blue", "Tema Azul", "blue",
//             new Color(229, 241, 251), new Color(255, 255, 255), // fondo principal, secundario
//             new Color(0, 0, 0), new Color(50, 50, 50),           // texto primario, secundario
//             new Color(153, 209, 255), new Color(0, 0, 0),        // borde color, titulo
//             new Color(0, 120, 215), new Color(255, 255, 255),  // seleccion fondo, texto
//             new Color(229, 241, 251), new Color(0, 0, 0),        // boton fondo, texto
//             new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion (igual que claro?)
//             new Color(0, 100, 200)
//         );
//         temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);
//
//         //Tema Green
//         Tema temaVerde = new Tema(
//                 "green",                     // nombreInterno (minúsculas)
//                 "Tema Verde",                // nombreDisplay
//                 "green",                     // carpetaIconos
//                 new Color(10, 25, 15),       // colorFondoPrincipal (muy oscuro, tinte verde)
//                 new Color(20, 40, 30),       // colorFondoSecundario (ligeramente más claro)
//                 new Color(0, 255, 100),      // colorTextoPrimario (verde brillante)
//                 new Color(0, 180, 80),       // colorTextoSecundario (verde menos intenso)
//                 new Color(0, 80, 40),        // colorBorde (verde oscuro sutil)
//                 new Color(0, 180, 80),       // colorBordeTitulo (igual que texto secundario)
//                 new Color(0, 100, 50),       // colorSeleccionFondo (verde medio oscuro)
//                 new Color(220, 255, 230),    // colorSeleccionTexto (blanco verdoso muy claro para contraste)
//                 new Color(20, 40, 30),       // colorBotonFondo (igual que fondo secundario)
//                 new Color(0, 255, 100),      // colorBotonTexto (igual que texto primario)
//                 new Color(0, 150, 70),       // colorBotonFondoActivado (verde más intenso)
//                 new Color(0, 100, 50),        // colorBotonFondoAnimacion (igual que selección)
//                 new Color(0, 200, 0)
//            );
//            temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);
//
//            // --- Tema Naranja ("Energía/HUD") ---
//            Tema temaNaranja = new Tema(
//                 "orange",                    // nombreInterno
//                 "Tema Naranja",              // nombreDisplay
//                 "orange",                    // carpetaIconos
//                 new Color(35, 30, 25),       // colorFondoPrincipal (gris oscuro cálido)
//                 new Color(55, 45, 40),       // colorFondoSecundario (gris más claro cálido)
//                 new Color(255, 150, 0),      // colorTextoPrimario (naranja brillante)
//                 new Color(200, 120, 0),      // colorTextoSecundario (naranja menos intenso)
//                 new Color(90, 70, 50),       // colorBorde (marrón oscuro/naranja)
//                 new Color(200, 120, 0),      // colorBordeTitulo (igual que texto secundario)
//                 new Color(180, 90, 0),       // colorSeleccionFondo (naranja oscuro)
//                 new Color(255, 240, 220),    // colorSeleccionTexto (blanco anaranjado muy claro)
//                 new Color(55, 45, 40),       // colorBotonFondo (igual que fondo secundario)
//                 new Color(255, 150, 0),      // colorBotonTexto (igual que texto primario)
//                 new Color(255, 120, 0),      // colorBotonFondoActivado (naranja más vivo)
//                 new Color(180, 90, 0),       // colorBotonFondoAnimacion (igual que selección)
//                 new Color(255, 90, 0)
//            );
//            temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
//    }
    

    private Tema obtenerTemaPorDefecto() {
        // Devuelve el tema "claro" o el primero que encuentre si "claro" no está
        return temasDisponibles.getOrDefault("claro", temasDisponibles.values().stream().findFirst().orElse(null));
    }

    /**
     * Obtiene el objeto Tema actualmente activo.
     * @return El Tema actual.
     */
    public Tema getTemaActual() {
        // Podría volver a leer de config aquí si fuera necesario sincronizar,
        // pero generalmente se confía en que setCurrentTheme lo mantiene actualizado.
        return temaActual;
    }

    
    /**
     * Establece un nuevo tema como el actual.
     * Esta versión está refactorizada para usar FlatLaf de forma nativa.
     * Cambia el LookAndFeel en caliente y aplica los colores personalizados del objeto Tema.
     *
     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "oscuro").
     * @return true si el tema se cambió exitosamente, false si no.
     */
    public boolean setTemaActual(String nombreTemaInterno) {
        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
        
        if (nuevoTema == null) {
            System.err.println("WARN [ThemeManager]: No se encontró el tema con nombre: " + nombreTemaInterno);
            return false;
        }
        
        if (!nuevoTema.equals(this.temaActual)) {
            this.temaActual = nuevoTema;
            
            configManager.setString(ConfigKeys.TEMA_NOMBRE, this.temaActual.nombreInterno());
            System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
            
            SwingUtilities.invokeLater(() -> {
                try {
                    // --- PASO 1: APLICAR LOOK AND FEEL BASE ---
                    System.out.println("  -> [EDT] Aplicando Look and Feel de FlatLaf...");
                    if ("dark".equalsIgnoreCase(nuevoTema.nombreInterno()) || "green".equalsIgnoreCase(nuevoTema.nombreInterno()) || "orange".equalsIgnoreCase(nuevoTema.nombreInterno())) {
                        FlatDarkLaf.setup();
                    } else {
                        FlatLightLaf.setup();
                    }

                    // --- PASO 2: PERSONALIZAR COLORES GLOBALES ---
                    System.out.println("  -> [EDT] Personalizando colores del UIManager con el tema: " + nuevoTema.nombreDisplay());
                    UIManager.put("Panel.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("ToolBar.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("MenuBar.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("Menu.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("MenuItem.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("PopupMenu.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("List.background", nuevoTema.colorFondoSecundario());
                    UIManager.put("Viewport.background", nuevoTema.colorFondoSecundario());
                    UIManager.put("ScrollPane.background", nuevoTema.colorFondoPrincipal());
                    UIManager.put("Button.background", nuevoTema.colorBotonFondo());
                    UIManager.put("Component.foreground", nuevoTema.colorTextoPrimario());
                    UIManager.put("Label.foreground", nuevoTema.colorTextoPrimario());
                    UIManager.put("Button.foreground", nuevoTema.colorBotonTexto());
                    UIManager.put("Menu.foreground", nuevoTema.colorTextoPrimario());
                    UIManager.put("MenuItem.foreground", nuevoTema.colorTextoPrimario());
                    UIManager.put("TitledBorder.titleColor", nuevoTema.colorBordeTitulo());
                    UIManager.put("List.selectionBackground", nuevoTema.colorSeleccionFondo());
                    UIManager.put("List.selectionForeground", nuevoTema.colorSeleccionTexto());
                    UIManager.put("Component.borderColor", nuevoTema.colorBorde());
                    UIManager.put("Separator.borderColor", nuevoTema.colorBorde());
                    UIManager.put("MenuBar.borderColor", nuevoTema.colorBorde());
                    UIManager.put("Component.focusColor", nuevoTema.colorBordeSeleccionActiva());
                    UIManager.put("Button.focusedBorderColor", nuevoTema.colorBordeSeleccionActiva());

                    // --- PASO 3: REFRESCO GLOBAL DE LA UI DE SWING ---
                    System.out.println("  -> [EDT] Llamando a FlatLaf.updateUI() para refrescar todos los componentes...");
                    FlatLaf.updateUI();

                    // --- PASO 4: REFRESCO DE COMPONENTES PERSONALIZADOS ---
                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getViewManager() != null) {
                        System.out.println("  -> [EDT] Refrescando fondo del panel de imagen al valor por defecto del nuevo tema...");
                        controllerRefParaNotificacion.getViewManager().refrescarFondoAlPorDefecto();
                    }        
                    
                    // --- PASO 5: ACTUALIZAR ICONOS EN CACHÉ DE ACTIONS ---
                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getActionFactory() != null) {
                        System.out.println("  -> [EDT] Actualizando los iconos cacheados en las Actions...");
                        controllerRefParaNotificacion.getActionFactory().actualizarIconosDeAcciones();
                    }
                    
                    // --- PASO 6: RECONSTRUIR BARRAS DE HERRAMIENTAS ---
                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getToolbarManager() != null && controllerRefParaNotificacion.getModel() != null) {
                        System.out.println("  -> [EDT] Reconstruyendo barras de herramientas...");
                        controllerRefParaNotificacion.getToolbarManager().reconstruirContenedorDeToolbars(
                            controllerRefParaNotificacion.getModel().getCurrentWorkMode()
                        );
                    }                    
                    
                    // --- INICIO DE LA MODIFICACIÓN ---
                    // --- PASO 7 (NUEVO): SINCRONIZAR ESTADO VISUAL DE BOTONES TOGGLE ---
                    if (controllerRefParaNotificacion != null && controllerRefParaNotificacion.getConfigApplicationManager() != null) {
                        System.out.println("  -> [EDT] Sincronizando estado visual de los botones toggle con el nuevo tema...");
                        // Llamamos al nuevo método centralizado
                        controllerRefParaNotificacion.getConfigApplicationManager().sincronizarAparienciaTodosLosToggles();
                    }
                    // --- FIN DE LA MODIFICACIÓN ---
                    
                    // --- PASO 8: SINCRONIZAR RADIOS DEL MENÚ DE TEMA ---
                    if (controllerRefParaNotificacion != null) {
                        controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
                    }

                } catch (Exception e) {
                    System.err.println("ERROR [ThemeManager]: Fallo al aplicar el tema en caliente con FlatLaf.");
                    e.printStackTrace();
                }
            });
            
            return true;
            
        } else {
            System.out.println("[ThemeManager] Intento de establecer el tema que ya está activo: " + nombreTemaInterno);
            return false;
        }
    } // --- Fin del método setTemaActual ---
    
    
//    private void notificarCambioDeTemaAlControlador(Tema temaAnterior, Tema temaNuevo) {
//        if (this.controllerRefParaNotificacion != null) {
//            // Llamar a un método en VisorController para que actualice las Actions de tema
//            // y cualquier otra cosa que dependa del tema.
//            this.controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
//            // Podrías pasar temaAnterior y temaNuevo si el controller los necesita
//            // this.controllerRefParaNotificacion.temaHaCambiado(temaAnterior, temaNuevo);
//        } else {
//            System.err.println("WARN [ThemeManager]: controllerRefParaNotificacion es null. No se pudo notificar cambio de tema para actualizar Actions de UI.");
//        }
//    }
    
    
    public void setControllerParaNotificacion(VisorController controller) {
        this.controllerRefParaNotificacion = controller; // No es necesario Objects.requireNonNull aquí si se permite null inicialmente
                                                        // pero AppInitializer debería pasarlo no nulo.
    }
    

    /**
     * Obtiene una lista de los nombres internos de los temas disponibles.
     * Útil para poblar menús o selectores.
     * @return Una lista de los nombres de tema disponibles.
     */
    public List<String> getNombresTemasDisponibles() {
        return List.copyOf(temasDisponibles.keySet()); // Devuelve copia inmutable
    }

     /**
     * Obtiene una lista de los objetos Tema disponibles.
     * @return Una lista de los Temas.
     */
     public List<Tema> getTemasDisponibles() {
         return List.copyOf(temasDisponibles.values()); // Devuelve copia inmutable
     }
     
     
     /**
      * Obtiene el color de fondo secundario para un tema específico.
      * Este es el color que se usará como fondo para la previsualización de iconos.
      *
      * @param nombreTemaInterno El nombre interno del tema (ej. "clear", "dark").
      * @return El Color de fondo secundario del tema, o un color de fallback (ej. Color.DARK_GRAY)
      *         si el tema no se encuentra o el color no está definido.
      */
     public Color getFondoSecundarioParaTema(String nombreTemaInterno) {
         if (nombreTemaInterno == null || nombreTemaInterno.isBlank()) {
             System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: nombreTemaInterno es nulo o vacío. Devolviendo fallback.");
             return Color.DARK_GRAY; // Fallback
         }
         Tema tema = temasDisponibles.get(nombreTemaInterno.toLowerCase()); // Asegurar búsqueda en minúsculas
         if (tema != null) {
             // Asumiendo que la clase Tema tiene un método público/campo colorFondoSecundario()
             Color color = tema.colorFondoSecundario(); 
             if (color != null) {
                 return color;
             } else {
                 System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: Tema '" + nombreTemaInterno + "' no tiene definido colorFondoSecundario. Devolviendo fallback.");
                 return Color.DARK_GRAY; // Fallback si el color específico es null en el tema
             }
         }
         System.err.println("WARN [ThemeManager.getFondoSecundarioParaTema]: No se encontró tema '" + nombreTemaInterno + "'. Devolviendo fallback.");
         return Color.DARK_GRAY; // Fallback si el tema no existe
     }
} // --- FIN de la clase ThemeManager ---