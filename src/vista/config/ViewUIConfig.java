package vista.config;

import java.awt.Color;
import java.util.Map;

import javax.swing.Action;

import vista.util.IconUtils; // Importar la nueva clase


/**
 * Contiene los parámetros de configuración específicos de la UI
 * que se pasan desde el Controller a la View durante la inicialización.
 * Se usan campos públicos finales para simplicidad e inmutabilidad.
 */
public class ViewUIConfig {

    // --- Propiedades de Configuración Visual ---
	public final IconUtils iconUtils;
    public final Color colorFondo;
    public final Color colorBotonActivado;
    public final Color colorBotonAnimacion;
    public final int iconoAncho;
    public final int iconoAlto; // Puede ser -1 para mantener proporción
    
    // --- Otros Datos Necesarios para la Vista ---
    public final Map<String, Action> actionMap; // Mapa (comando corto -> Action)

    /**
     * Constructor para inicializar la configuración de la UI.
     *
     * @param colorFondo Color de fondo base (o default si es null).
     * @param colorBotonActivado Color para botones activos (o default si es null).
     * @param colorBotonAnimacion Color para animación de botones (o default si es null).
     * @param iconoAncho Ancho deseado para iconos (o default si es <= 0).
     * @param iconoAlto Alto deseado para iconos (o -1 para proporción, o default si es <=0 y ancho también).
     * @param actionMap El mapa de Actions proporcionado por el Controller (o mapa vacío si es null).
     */
    public ViewUIConfig(
            Color colorFondo,
            Color colorBotonActivado,
            Color colorBotonAnimacion,
            int iconoAncho,
            int iconoAlto,
            Map<String, Action> actionMap,
            // --- TEXTO MODIFICADO ---
            IconUtils iconUtils // Añadir parámetro al constructor
            // --- FIN MODIFICACION ---
        ) {
        this.colorFondo = colorFondo;
        this.colorBotonActivado = colorBotonActivado;
        this.colorBotonAnimacion = colorBotonAnimacion;
        this.iconoAncho = iconoAncho;
        this.iconoAlto = iconoAlto;
        this.actionMap = actionMap;
        // --- TEXTO MODIFICADO ---
        if (iconUtils == null) { // Validación
             throw new IllegalArgumentException("IconUtils no puede ser null en ViewUIConfig");
        }
        this.iconUtils = iconUtils; // Guardar la instancia
        // --- FIN MODIFICACION ---
    }
    
    
//    public ViewUIConfig(Color colorFondo, Color colorBotonActivado, Color colorBotonAnimacion,
//                        int iconoAncho, int iconoAlto, Map<String, Action> actionMap)
//    {
//        // Asignar con valores por defecto si los parámetros son null o inválidos
//        this.colorFondo = (colorFondo != null) ? colorFondo : new Color(238, 238, 238);
//        this.colorBotonActivado = (colorBotonActivado != null) ? colorBotonActivado : new Color(84, 144, 164);
//        this.colorBotonAnimacion = (colorBotonAnimacion != null) ? colorBotonAnimacion : new Color(173, 216, 230);
//
//        this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 32;
//        // Si alto es inválido (<=0), lo guardamos como -1 para indicar proporción,
//        // a menos que ancho también sea inválido, en cuyo caso usamos default 32.
//        this.iconoAlto = (iconoAlto <= 0) ? (this.iconoAncho > 0 ? -1 : 32) : iconoAlto;
//
//        // Crear una copia inmutable del mapa de acciones para seguridad
//        this.actionMap = (actionMap != null) ? Collections.unmodifiableMap(new HashMap<>(actionMap)) : Collections.emptyMap();
//
//        System.out.println("[ViewUIConfig] Configuración UI creada:");
//        System.out.println("  - Fondo: " + this.colorFondo);
//        System.out.println("  - Activado: " + this.colorBotonActivado);
//        System.out.println("  - Animacion: " + this.colorBotonAnimacion);
//        System.out.println("  - Icono WxH: " + this.iconoAncho + "x" + this.iconoAlto);
//        System.out.println("  - Actions: " + this.actionMap.size());
//    }

    // No se necesitan getters si los campos son públicos finales,
    // pero puedes añadirlos si prefieres campos privados.
}