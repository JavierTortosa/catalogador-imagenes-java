package vista.builders;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import vista.util.IconUtils;

// Clase auxiliar interna para la configuración del layout de botones
class ButtonGroupConfig { // Puedes mantenerla aquí o hacerla pública si es necesario
    final String category;
    final int alignment; // FlowLayout.LEFT, CENTER, RIGHT
    final List<String> iconNames;

    ButtonGroupConfig(String category, int alignment, List<String> iconNames) {
        this.category = category;
        this.alignment = alignment;
        this.iconNames = iconNames;
    }
}

public class ToolbarBuilder {

    // --- Resultados del Builder ---
    private JPanel toolbarPanel; // El panel principal de la barra de herramientas
    private Map<String, JButton> botonesPorNombre; // Mapa clave Larga -> Botón
    
    // --- Componentes Internos ---
    private JPanel panelBotonesIzquierda;
    private JPanel panelBotonesCentro;
    private JPanel panelBotonesDerecha;
    private final Color colorBotonFondoDefault;
    private final Color colorBotonTextoDefault;
    
    private int iconoAncho;
    private int iconoAlto;
    
    private Map<String, Action> actionMap; // NUEVO: Mapa de acciones
    
    private final IconUtils iconUtils;
    
 // --- Constructor (MODIFICADO) ---
	public ToolbarBuilder(Map<String, Action> actionMap, Color colorBotonFondo, Color colorBotonTexto,
			Color _colorBotonActivadoIgnorado, Color _colorBotonAnimacionIgnorado, int iconoAncho, int iconoAlto,
			IconUtils iconUtils)
	{

		this.actionMap = (actionMap != null) ? actionMap : new HashMap<>();
		this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");
		this.colorBotonFondoDefault = (colorBotonFondo != null) ? colorBotonFondo : new Color(238, 238, 238);
		this.colorBotonTextoDefault = (colorBotonTexto != null) ? colorBotonTexto : Color.BLACK;
		this.iconoAncho = (iconoAncho > 0) ? iconoAncho : 24;
		this.iconoAlto = (iconoAlto <= 0) ? (this.iconoAncho > 0 ? -1 : 24) : iconoAlto;

		// --- TEXTO A AÑADIR ---
		// Inicializar el mapa de botones AQUI
		this.botonesPorNombre = new HashMap<>();
		// --- FIN TEXTO A AÑADIR ---

		// Inicializar paneles (esto ya lo tenías)
		panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Inicializar paneles
        panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelBotonesCentro = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panelBotonesDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        // Aplicar fondo a los paneles internos de la toolbar
        panelBotonesIzquierda.setBackground(this.colorBotonFondoDefault);
        panelBotonesCentro.setBackground(this.colorBotonFondoDefault);
        panelBotonesDerecha.setBackground(this.colorBotonFondoDefault);
        panelBotonesIzquierda.setOpaque(true); // Asegurar visibilidad
        panelBotonesCentro.setOpaque(true);
        panelBotonesDerecha.setOpaque(true);

        toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(panelBotonesIzquierda, BorderLayout.WEST);
        toolbarPanel.add(panelBotonesCentro, BorderLayout.CENTER);
        toolbarPanel.add(panelBotonesDerecha, BorderLayout.EAST);
        // Aplicar fondo al panel principal de la toolbar
        toolbarPanel.setBackground(this.colorBotonFondoDefault);
        toolbarPanel.setOpaque(true);
        
        // Aplicar borde con color del tema?
        // toolbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, colorBordeDelTema)); // Ejemplo borde inferior
    }
    
    

    /**
     * Construye la barra de herramientas completa basada en la configuración
     * interna de iconos y layout.
     *
     * @return El JPanel que contiene la barra de herramientas construida.
     */
    public JPanel buildToolbar() {
        // --- Definición de Iconos y Layout (Movido desde VisorView) ---
    	
        List<String> iconosMovimiento = List.of(
        		"1001-Primera_48x48.png",	//new
        		"1002-Anterior_48x48.png", 
        		"1003-Siguiente_48x48.png",
        		"1004-Ultima_48x48.png"		//new
        		);
        
        List<String> iconosEdicion = List.of(
        		"2001-Rotar_Izquierda_48x48.png", 
        		"2002-Rotar_Derecha_48x48.png", 
        		"2003-Espejo_Horizontal_48x48.png", 
        		"2004-Espejo_Vertical_48x48.png", 
        		"2005-Recortar_48x48.png");
        
        List<String> iconosZoom = List.of(
        		"3001-Zoom_48x48.png", 
        		"3002-Zoom_Auto_48x48.png", 
        		"3003-Ajustar_al_Ancho_48x48.png", 
        		"3004-Ajustar_al_Alto_48x48.png", 
        		"3005-Escalar_Para_Ajustar_48x48.png", 
        		"3006-Zoom_Fijo_48x48.png", 
        		"3007-zoom_especifico_48x48.png",
        		"3008-Reset_48x48.png");
        
        List<String> iconosVista = List.of(
        		"4001-Panel-Galeria_48x48.png", 
        		"4002-Grid_48x48.png", 
        		"4003-Pantalla_Completa_48x48.png", 
        		"4004-Lista_48x48.png", 
        		"4005-Carrousel_48x48.png");
        
        List<String> iconosControl = List.of(
        		"5001-Refrescar_48x48.png", 
        		"5003-lista_de_favoritos_48x48.png", 
        		"5004-Borrar_48x48.png");
        
        List<String> iconosEspeciales = List.of(
        		"6001-Selector_de_Carpetas_48x48.png", 
        		"6002-Menu_48x48.png", 
        		"6003-Botones_Ocultos_48x48.png");
        
        List<String> iconosToggle = List.of(
        		"7001-Subcarpetas_48x48.png",
        		"7002-Mantener_Proporciones_48x48.png",
        		"7003-Mostrar_Favoritos_48x48.png"
        		);

        List<ButtonGroupConfig> buttonLayoutConfig = List.of(
            new ButtonGroupConfig("movimiento", FlowLayout.LEFT, iconosMovimiento),
            new ButtonGroupConfig("edicion", FlowLayout.CENTER, iconosEdicion),
            new ButtonGroupConfig("zoom", FlowLayout.CENTER, iconosZoom),
            new ButtonGroupConfig("vista", FlowLayout.CENTER, iconosVista),
            new ButtonGroupConfig("toggle", FlowLayout.CENTER, iconosToggle),
            new ButtonGroupConfig("control", FlowLayout.CENTER, iconosControl),
            new ButtonGroupConfig("especiales", FlowLayout.RIGHT, iconosEspeciales)
            
        );
        // -------------------------------------------------------------

        boolean firstGroup = true;
        for (ButtonGroupConfig config : buttonLayoutConfig) {
            if (!firstGroup) {
                addSeparator(config.alignment); // Usa método interno
            } else {
                firstGroup = false;
            }

            for (String nombreIcono : config.iconNames) {
                // Llama al método interno para crear y configurar el botón
                procesarBoton(nombreIcono, config.category, config.alignment);
            }
        }

        return toolbarPanel; // Devuelve el panel construido
    }

    /**
     * Procesa la creación y configuración de un botón individual,
     * usando los colores y tamaños de icono configurados.
     */
    private void procesarBoton(String nombreIcono, String categoria, int flowLayoutAlignment) 
    {
        JButton button = new JButton(); // Crea el botón

        // --- Claves y Action Command ---
        String nombreBotonBase = nombreIcono.replaceAll("\\d+-", "").replace(".png", "");
        String fullConfigKey = "interfaz.boton." + categoria + "." + nombreBotonBase; // Clave Larga
        String shortActionCommand = nombreBotonBase; // Comando Corto

        // --- Configurar propiedades básicas del botón ---
        
        // Determinar tamaño final del botón basado en config
        int anchoBoton = this.iconoAncho;
        int altoBotonCalculado = (this.iconoAlto <= 0) ? anchoBoton : this.iconoAlto;
        
        button.setPreferredSize(new Dimension(anchoBoton, altoBotonCalculado));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(this.colorBotonFondoDefault);
        button.setForeground(this.colorBotonTextoDefault);
        button.setOpaque(true); // Asegurarse de que el fondo sea visible si no es transparente

        //LOG [ToolbarBuilder] Procesando botón:
        System.out.println("  [ToolbarBuilder] Procesando botón: " + shortActionCommand + " (Icono Archivo: " + nombreIcono + ")");

        // --- Intentar asignar Action ---
        Action action = this.actionMap.get(shortActionCommand); // Busca Action por comando CORTO

        ImageIcon iconParaBoton = null; // Variable para guardar el icono final
        String toolTipParaBoton = null; // Variable para el tooltip final

        if (action != null) {
            // --- Caso 1: Se encontró una Action ---
        	
        	//LOG -> Action encontrada: 
            //System.out.println("    -> Action encontrada: " + action.getValue(Action.NAME));
            button.setAction(action); // ASIGNAR ACTION!

            // Intentar obtener icono y tooltip DESDE la Action
            Object iconValue = action.getValue(Action.SMALL_ICON);
            
            if (iconValue instanceof ImageIcon) { // Comprobar que sea ImageIcon
                iconParaBoton = (ImageIcon) iconValue;
            } else {
                // Si la Action no tiene icono (o es incorrecto), intentar cargarlo aquí como fallback
                //System.out.println("    -> Icono NO válido/NULL en Action. Intentando carga fallback con IconUtils...");
                iconParaBoton = this.iconUtils.getScaledIcon(nombreIcono, this.iconoAncho, this.iconoAlto);
                 
                if (iconParaBoton != null) {
                      button.setHideActionText(true);
                      button.setText("");
                 } else {
                      System.err.println("    -> ERROR: Falló carga fallback del icono: " + nombreIcono);
                      button.setIcon(null); // Asegurar que no hay icono
                      button.setHideActionText(false); // Mostrar texto de la Action
                 }
            }

            Object tooltipValue = action.getValue(Action.SHORT_DESCRIPTION);
             if (tooltipValue instanceof String) {
                 toolTipParaBoton = (String) tooltipValue; // Guardar tooltip de la action
             }

        } else {
            // --- Caso 2: No se encontró Action (Fallback) ---
            button.setActionCommand(shortActionCommand);

            // Cargar icono manualmente usando IconUtils
            
        	//LOG     -> Sin Action asociada. Configurando manualmente.
            //System.out.println("      -> Intentando cargar icono fallback con IconUtils: " + nombreIcono);
            
            iconParaBoton = this.iconUtils.getScaledIcon(nombreIcono, this.iconoAncho, this.iconoAlto);

            if (iconParaBoton == null) {
                 button.setText("?");
            } else {
                 button.setText(""); // Borrar texto si hay icono fallback
            }
        }

        // --- Asignar Icono y Tooltip finales ---
        
        if (iconParaBoton != null) {
            button.setIcon(iconParaBoton);
            // Opcional: Poner icono deshabilitado si tienes uno específico
            // button.setDisabledIcon(...);
        }

        if (toolTipParaBoton != null) {
             button.setToolTipText(toolTipParaBoton);
        } else {
            // Generar tooltip si no vino de la Action
            button.setToolTipText(
                    nombreIcono.replace(".png", "").replace("_", " ").replace("48x48", "").replaceAll("\\d+-", "").trim());
        }

        // --- Añadir al Mapa del Builder ---
        this.botonesPorNombre.put(fullConfigKey, button); // Usa clave LARGA para el mapa

        // --- Añadir al Panel Correcto ---
        if (flowLayoutAlignment == FlowLayout.LEFT) panelBotonesIzquierda.add(button);
        else if (flowLayoutAlignment == FlowLayout.CENTER) panelBotonesCentro.add(button);
        else if (flowLayoutAlignment == FlowLayout.RIGHT) panelBotonesDerecha.add(button);
        else System.err.println("Advertencia: Alineación desconocida para botón '" + nombreIcono + "'");

    } // Fin de procesarBoton

   

    /**
     * Añade un separador visual al panel correspondiente.
     * (Lógica movida desde VisorView.separadorVisualParaBotones)
     */
    private void addSeparator(int alignment) {
        if (alignment == FlowLayout.LEFT) panelBotonesIzquierda.add(Box.createHorizontalStrut(10));
        else if (alignment == FlowLayout.CENTER) panelBotonesCentro.add(Box.createHorizontalStrut(10));
        else if (alignment == FlowLayout.RIGHT) panelBotonesDerecha.add(Box.createHorizontalStrut(10));
    }

    
    /**
     * Devuelve el mapa que asocia las claves de configuración LARGAS
     * con las instancias de JButton creadas.
     *
     * @return El mapa de botones por nombre (clave larga).
     */
    public Map<String, JButton> getBotonesPorNombreMap() {
        return this.botonesPorNombre;
    }
}
