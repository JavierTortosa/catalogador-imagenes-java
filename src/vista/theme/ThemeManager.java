// Sugerencia de paquete: vista.theme o servicios
package vista.theme; // O servicios

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap; // Para seguridad en hilos si fuera necesario

import controlador.VisorController;
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
        // --- Definir los temas aquí (Hardcodeado para empezar) ---

        // Tema Claro (Replicando los defaults anteriores)
        Tema temaClaro = new Tema(
            "clear", "Tema Claro", "black",
            new Color(238, 238, 238), new Color(255, 255, 255), // fondo principal, secundario
            new Color(0, 0, 0), new Color(80, 80, 80),           // texto primario, secundario
            new Color(184, 207, 229), new Color(0, 0, 0),        // borde color, titulo
            new Color(57, 105, 138), new Color(255, 255, 255),  // seleccion fondo, texto
            new Color(238, 238, 238), new Color(0, 0, 0),        // boton fondo, texto
            new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion
            new Color(255, 140, 0)
        );
        temasDisponibles.put(temaClaro.nombreInterno(), temaClaro);

        // Tema Oscuro
        Tema temaOscuro = new Tema(
             "dark", "Tema Oscuro", "white",
             new Color(45, 45, 45), new Color(60, 60, 60),      // fondo principal, secundario
             new Color(210, 210, 210), new Color(160, 160, 160), // texto primario, secundario
             new Color(80, 80, 80), new Color(180, 180, 180),   // borde color, titulo
             new Color(0, 80, 150), new Color(255, 255, 255),   // seleccion fondo, texto
             new Color(55, 55, 55), new Color(210, 210, 210),   // boton fondo, texto
             new Color(74, 134, 154), new Color(100, 100, 100), // boton activado, animacion
             new Color(0, 128, 255)
        );
         temasDisponibles.put(temaOscuro.nombreInterno(), temaOscuro);

         // Tema Azul
         Tema temaAzul = new Tema(
             "blue", "Tema Azul", "blue",
             new Color(229, 241, 251), new Color(255, 255, 255), // fondo principal, secundario
             new Color(0, 0, 0), new Color(50, 50, 50),           // texto primario, secundario
             new Color(153, 209, 255), new Color(0, 0, 0),        // borde color, titulo
             new Color(0, 120, 215), new Color(255, 255, 255),  // seleccion fondo, texto
             new Color(229, 241, 251), new Color(0, 0, 0),        // boton fondo, texto
             new Color(84, 144, 164), new Color(173, 216, 230),    // boton activado, animacion (igual que claro?)
             new Color(0, 100, 200)
         );
         temasDisponibles.put(temaAzul.nombreInterno(), temaAzul);

         //Tema Green
         Tema temaVerde = new Tema(
                 "green",                     // nombreInterno (minúsculas)
                 "Tema Verde",                // nombreDisplay
                 "green",                     // carpetaIconos
                 new Color(10, 25, 15),       // colorFondoPrincipal (muy oscuro, tinte verde)
                 new Color(20, 40, 30),       // colorFondoSecundario (ligeramente más claro)
                 new Color(0, 255, 100),      // colorTextoPrimario (verde brillante)
                 new Color(0, 180, 80),       // colorTextoSecundario (verde menos intenso)
                 new Color(0, 80, 40),        // colorBorde (verde oscuro sutil)
                 new Color(0, 180, 80),       // colorBordeTitulo (igual que texto secundario)
                 new Color(0, 100, 50),       // colorSeleccionFondo (verde medio oscuro)
                 new Color(220, 255, 230),    // colorSeleccionTexto (blanco verdoso muy claro para contraste)
                 new Color(20, 40, 30),       // colorBotonFondo (igual que fondo secundario)
                 new Color(0, 255, 100),      // colorBotonTexto (igual que texto primario)
                 new Color(0, 150, 70),       // colorBotonFondoActivado (verde más intenso)
                 new Color(0, 100, 50),        // colorBotonFondoAnimacion (igual que selección)
                 new Color(0, 200, 0)
            );
            temasDisponibles.put(temaVerde.nombreInterno(), temaVerde);

            // --- Tema Naranja ("Energía/HUD") ---
            Tema temaNaranja = new Tema(
                 "orange",                    // nombreInterno
                 "Tema Naranja",              // nombreDisplay
                 "orange",                    // carpetaIconos
                 new Color(35, 30, 25),       // colorFondoPrincipal (gris oscuro cálido)
                 new Color(55, 45, 40),       // colorFondoSecundario (gris más claro cálido)
                 new Color(255, 150, 0),      // colorTextoPrimario (naranja brillante)
                 new Color(200, 120, 0),      // colorTextoSecundario (naranja menos intenso)
                 new Color(90, 70, 50),       // colorBorde (marrón oscuro/naranja)
                 new Color(200, 120, 0),      // colorBordeTitulo (igual que texto secundario)
                 new Color(180, 90, 0),       // colorSeleccionFondo (naranja oscuro)
                 new Color(255, 240, 220),    // colorSeleccionTexto (blanco anaranjado muy claro)
                 new Color(55, 45, 40),       // colorBotonFondo (igual que fondo secundario)
                 new Color(255, 150, 0),      // colorBotonTexto (igual que texto primario)
                 new Color(255, 120, 0),      // colorBotonFondoActivado (naranja más vivo)
                 new Color(180, 90, 0),       // colorBotonFondoAnimacion (igual que selección)
                 new Color(255, 90, 0)
            );
            temasDisponibles.put(temaNaranja.nombreInterno(), temaNaranja);
    }

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
     * Establece un nuevo tema como el actual y guarda el nombre en la configuración.
     * @param nombreTemaInterno El nombre interno del tema a activar (ej. "oscuro").
     * @return true si el tema se cambió exitosamente, false si el nombre no existe.
     */
    public boolean setTemaActual(String nombreTemaInterno) {
        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
        if (nuevoTema != null) {
            if (!nuevoTema.equals(this.temaActual)) { // Cambiar solo si es diferente
                Tema temaAnterior = this.temaActual;
                this.temaActual = nuevoTema;
                configManager.setString(ConfigurationManager.KEY_TEMA_NOMBRE, this.temaActual.nombreInterno()); // Usar la constante
                System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
                notificarCambioDeTemaAlControlador(temaAnterior, this.temaActual);
                return true; // <--- Tema realmente cambiado
            } else {
                System.out.println("[ThemeManager] Intento de establecer el tema que ya está activo: " + nombreTemaInterno);
                return false; // <--- No hubo cambio, pero el tema solicitado es el actual
            }
        } else {
            System.err.println("WARN [ThemeManager]: No se encontró el tema con nombre: " + nombreTemaInterno);
            return false; // <--- Tema no válido
        }
    }
    
//    public boolean setTemaActual(String nombreTemaInterno) {
//        Tema nuevoTema = temasDisponibles.get(nombreTemaInterno);
//        if (nuevoTema != null) {
//            if (!nuevoTema.equals(this.temaActual)) { // Cambiar solo si es diferente
//            	Tema temaAnterior = this.temaActual;
//                this.temaActual = nuevoTema;
//                // Guardar SOLO el nombre en ConfigurationManager
//                configManager.setString("tema.nombre", this.temaActual.nombreInterno());
//                // NO llamar a guardarConfiguracion aquí, eso lo hace el Controller/ShutdownHook
//                System.out.println("[ThemeManager] Tema actual cambiado a: " + this.temaActual.nombreInterno());
//                // Aquí NO se notifica al usuario ni se refresca la UI, eso es responsabilidad del Controller/Vista
//                notificarCambioDeTemaAlControlador(temaAnterior, this.temaActual);
//            } else {
//                 System.out.println("[ThemeManager] Intento de establecer el tema que ya está activo: " + nombreTemaInterno);
//            }
//            return true;
//        } else {
//            System.err.println("WARN [ThemeManager]: No se encontró el tema con nombre: " + nombreTemaInterno);
//            return false;
//        }
//    }
    
    
    // --- NUEVO MÉTODO para notificar al VisorController ---
    private void notificarCambioDeTemaAlControlador(Tema temaAnterior, Tema temaNuevo) {
        if (this.controllerRefParaNotificacion != null) {
            // Llamar a un método en VisorController para que actualice las Actions de tema
            // y cualquier otra cosa que dependa del tema.
            this.controllerRefParaNotificacion.sincronizarEstadoDeTodasLasToggleThemeActions();
            // Podrías pasar temaAnterior y temaNuevo si el controller los necesita
            // this.controllerRefParaNotificacion.temaHaCambiado(temaAnterior, temaNuevo);
        } else {
            System.err.println("WARN [ThemeManager]: controllerRefParaNotificacion es null. No se pudo notificar cambio de tema para actualizar Actions de UI.");
        }
    }
    
    
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
}