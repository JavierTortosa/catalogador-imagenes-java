package vista.config;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;

import servicios.ConfigurationManager;
import vista.util.IconUtils; // Importar la nueva clase


/**
 * Contiene los parámetros de configuración específicos de la UI
 * que se pasan desde el Controller a la View durante la inicialización.
 * Se usan campos públicos finales para simplicidad e inmutabilidad.
 */
public class ViewUIConfig {

    // --- Propiedades de Configuración Visual ---
	public final Map<String, Action> actionMap; // Mapa (comando corto -> Action)

	
	public final int iconoAlto; // Puede ser -1 para mantener proporción
	public final int iconoAncho;
	public final IconUtils iconUtils;
	
	
    public final Color colorFondo;
    public final Color colorBotonActivado;
    public final Color colorBotonAnimacion;
    public Color colorBotonFondo;
    public Color colorBotonTexto;
    
    public Color colorFondoPrincipal;
    public Color colorFondoSecundario;
    public Color colorTextoPrimario;
    public Color colorTextoSecundario;
    public Color colorBorde;
    public Color colorBordeTitulo;
    public Color colorSeleccionFondo;
    public Color colorSeleccionTexto;
    
    public final ConfigurationManager configurationManager; // <-- AÑADIR CAMPO

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
    // --- CONSTRUCTOR DENTRO DE LA CLASE ViewUIConfig ---
    public ViewUIConfig(
            // Parámetros obsoletos (pueden eliminarse si no se usan)
            Color _colorFondoObsoleto,           // 1 Color
            Color _colorBotonActivadoObsoleto,  // 2 Color
            Color _colorBotonAnimacionObsoleto, // 3 Color
            // Configuración estándar
            int iconoAncho,                     // 4 int
            int iconoAlto,                      // 5 int
            Map<String, Action> actionMap,      // 6 Map<String, Action>
            IconUtils iconUtils,                // 7 IconUtils
            // Nuevos parámetros de color específicos
            Color colorFondoPrincipal,          // 8 Color
            Color colorFondoSecundario,         // 9 Color
            Color colorTextoPrimario,           // 10 Color
            Color colorTextoSecundario,         // 11 Color
            Color colorBorde,                   // 12 Color
            Color colorBordeTitulo,             // 13 Color
            Color colorSeleccionFondo,          // 14 Color
            Color colorSeleccionTexto,          // 15 Color
            Color colorBotonFondo,              // 16 Color
            Color colorBotonTexto,              // 17 Color
            Color colorBotonFondoActivado,      // 18 Color
            Color colorBotonFondoAnimacion,       // 19 Color
            ConfigurationManager configurationManager
        ) {

    	// Asignar estándar
    	this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
        this.iconoAlto = (iconoAlto <= 0) ? (this.iconoAncho > 0 ? -1 : 24) : iconoAlto;
        this.actionMap = (actionMap != null) ? Collections.unmodifiableMap(new HashMap<>(actionMap)) : Collections.emptyMap();
        this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");

        // Asignar colores específicos
        this.colorFondoPrincipal = Objects.requireNonNull(colorFondoPrincipal, "colorFondoPrincipal nulo");
        this.colorFondoSecundario = Objects.requireNonNull(colorFondoSecundario, "colorFondoSecundario nulo");
        this.colorTextoPrimario = Objects.requireNonNull(colorTextoPrimario, "colorTextoPrimario nulo");
        this.colorTextoSecundario = Objects.requireNonNull(colorTextoSecundario, "colorTextoSecundario nulo");
        this.colorBorde = Objects.requireNonNull(colorBorde, "colorBorde nulo");
        this.colorBordeTitulo = Objects.requireNonNull(colorBordeTitulo, "colorBordeTitulo nulo");
        this.colorSeleccionFondo = Objects.requireNonNull(colorSeleccionFondo, "colorSeleccionFondo nulo");
        this.colorSeleccionTexto = Objects.requireNonNull(colorSeleccionTexto, "colorSeleccionTexto nulo");
        this.colorBotonFondo = Objects.requireNonNull(colorBotonFondo, "colorBotonFondo nulo");
        this.colorBotonTexto = Objects.requireNonNull(colorBotonTexto, "colorBotonTexto nulo");

        // Asignar a campos "obsoletos" (o puedes renombrar/eliminar estos campos)
        this.colorBotonActivado = Objects.requireNonNull(colorBotonFondoActivado, "colorBotonFondoActivado nulo");
        this.colorBotonAnimacion = Objects.requireNonNull(colorBotonFondoAnimacion, "colorBotonFondoAnimacion nulo");
        this.colorFondo = this.colorFondoPrincipal;
    	
        this.configurationManager = configurationManager;
        
        
        
        System.out.println("[ViewUIConfig] Configuración UI creada con colores de tema.");
    }
    


    // No se necesitan getters si los campos son públicos finales,
    // pero puedes añadirlos si prefieres campos privados.
}