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

import controlador.VisorController;
import vista.config.ToolbarButtonDefinition;
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
    private final VisorController controllerRef;
    
 // --- Constructor (MODIFICADO) ---
	public ToolbarBuilder(
			Map<String, Action> actionMap, 
			Color colorBotonFondo, 
			Color colorBotonTexto,
			Color _colorBotonActivadoIgnorado, 
			Color _colorBotonAnimacionIgnorado, 
			int iconoAncho, int iconoAlto,
			IconUtils iconUtils,
			VisorController controller
			)
	{

		this.actionMap = (actionMap != null) ? actionMap : new HashMap<>();
		this.iconUtils = Objects.requireNonNull(iconUtils, "IconUtils no puede ser null");
		this.controllerRef = Objects.requireNonNull(controller, "VisorController no puede ser null en ToolbarBuilder");
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
	public JPanel buildToolbar(List<ToolbarButtonDefinition> toolbarStructure, Map<String, Action> actionMap) {
	    // Guardar el actionMap recibido si lo necesitas en otros métodos (como procesar...)
	    this.actionMap = Objects.requireNonNull(actionMap, "ActionMap no puede ser null en ToolbarBuilder");

	    // Reiniciar los paneles por si se reutiliza el builder
	    panelBotonesIzquierda.removeAll();
	    panelBotonesCentro.removeAll();
	    panelBotonesDerecha.removeAll();
	    this.botonesPorNombre.clear(); // Limpiar mapa interno

	    System.out.println("--- ToolbarBuilder: Construyendo toolbar desde estructura definida ---");
	    if (toolbarStructure == null || toolbarStructure.isEmpty()) {
	         System.err.println("WARN [ToolbarBuilder]: La estructura de la toolbar está vacía o es nula.");
	         // Revalidar paneles vacíos y devolver el panel principal vacío
	         toolbarPanel.revalidate();
	         toolbarPanel.repaint();
	         return this.toolbarPanel;
	    }

	    String ultimaCategoriaProcesada = null; // Para saber cuándo añadir separador

	    // Iterar sobre la nueva estructura
	    for (ToolbarButtonDefinition definition : toolbarStructure) {
	        // Añadir separador si cambiamos de categoría (opcional, pero preserva aspecto similar)
	        String categoriaActual = definition.categoriaLayout();
	        if (ultimaCategoriaProcesada != null && !ultimaCategoriaProcesada.equals(categoriaActual)) {
	            // Determinar alineación basada en la *nueva* categoría para el separador
	            int alignment = getAlignmentForCategory(categoriaActual); // Necesitarás un helper para esto
	            addSeparator(alignment);
	        }

	        // Procesar la definición para crear y añadir el botón
	        procesarToolbarButtonDefinition(definition); // Llama al método refactorizado/nuevo

	        ultimaCategoriaProcesada = categoriaActual; // Actualizar última categoría
	    }

	    // Revalidar y repintar los paneles al final
	    panelBotonesIzquierda.revalidate();
	    panelBotonesIzquierda.repaint();
	    panelBotonesCentro.revalidate();
	    panelBotonesCentro.repaint();
	    panelBotonesDerecha.revalidate();
	    panelBotonesDerecha.repaint();
	    toolbarPanel.revalidate();
	    toolbarPanel.repaint();

	    System.out.println("--- ToolbarBuilder: Toolbar construida. Total botones en mapa: " + botonesPorNombre.size() + " ---");
	    return toolbarPanel;
	} //--- FIN metodo buildToolbar 
	
	
	
	
	private int getAlignmentForCategory(String category) {
	    // Define aquí qué categorías van a qué alineación
	    // Basado en tu antiguo ButtonGroupConfig:
	    switch (category) {
	        case "movimiento":
	            return FlowLayout.LEFT;
	        case "edicion":
	        case "zoom":
	        case "vista":
	        case "toggle":
	        case "control":
	        case "proyecto":
	            return FlowLayout.CENTER;
	        case "especiales":
	            return FlowLayout.RIGHT;
	        default:
	            System.err.println("WARN [ToolbarBuilder]: Categoría desconocida para alineación: " + category + ". Usando CENTER por defecto.");
	            return FlowLayout.CENTER; // O LEFT, o lanzar excepción
	    }
	} // FIN metodo getAlignmentForCategory
	
	
    /**
     * Procesa una definición de botón, crea el JButton correspondiente,
     * lo configura (acción, icono, tooltip) y lo añade al panel y mapa correctos.
     *
     * @param definition La definición del botón a procesar.
     */
    private void procesarToolbarButtonDefinition(ToolbarButtonDefinition definition) {
        // --- 0. Validación de Entrada ---
        if (definition == null) {
            System.err.println("WARN [ToolbarBuilder]: Se recibió una ToolbarButtonDefinition nula. Saltando botón.");
            return;
        }
        if (definition.comandoCanonico() == null && definition.claveIcono() == null) {
             System.err.println("WARN [ToolbarBuilder]: Definición de botón inválida (sin comando ni claveIcono). Tooltip: " + definition.textoTooltip());
             return;
        }

        // --- 1. Creación y Configuración Básica del Botón ---
        JButton button = new JButton();
        int anchoBoton = this.iconoAncho;
        int altoBotonCalculado = (this.iconoAlto <= 0) ? anchoBoton : this.iconoAlto;

        button.setPreferredSize(new Dimension(anchoBoton, altoBotonCalculado));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true); // Importante para que setBackground funcione
        button.setBackground(this.colorBotonFondoDefault);
        button.setForeground(this.colorBotonTextoDefault);
        button.setOpaque(true);

        // --- 2. Asignar Tooltip ---
        if (definition.textoTooltip() != null && !definition.textoTooltip().isBlank()) {
            button.setToolTipText(definition.textoTooltip());
        } else if (definition.comandoCanonico() != null) {
            // Fallback si no hay tooltip definido, usar el comando canónico
            button.setToolTipText(definition.comandoCanonico());
        } else {
            // Fallback aún más genérico si tampoco hay comando (raro)
            button.setToolTipText(definition.claveIcono());
        }

        // --- 3. Conectar Acción e Icono ---
        
        // === INICIO BLOQUE DE LOGGING PARA DEPURACIÓN ===
        System.out.println("\n\n **************************************************************** ");
        System.out.println("  [TB procesar] Para definición con comando: '" + definition.comandoCanonico() +
                           "' y claveIcono: '" + definition.claveIcono() + "'");
        if (this.actionMap == null) {
            System.out.println("    -> ERROR CRÍTICO: this.actionMap en ToolbarBuilder es NULL.");
        } else {
            boolean contieneClave = this.actionMap.containsKey(definition.comandoCanonico());
            System.out.println("    -> ¿ActionMap contiene clave '" + definition.comandoCanonico() + "'? " + contieneClave);
            if (contieneClave) {
                Action foundAction = this.actionMap.get(definition.comandoCanonico());
                System.out.println("    -> Action encontrada en mapa: " +
                                   (foundAction != null ? foundAction.getClass().getName() : "NULL (inesperado si la clave existe)"));
            }
        }
        // === FIN BLOQUE DE LOGGING PARA DEPURACIÓN ===
        
        Action action = null;
        ImageIcon iconoFinalParaBoton = null; // Variable para el icono que realmente se usará

        // 3.1. Intentar obtener la Action desde el actionMap usando el comando canónico
        if (definition.comandoCanonico() != null && this.actionMap != null) {
            action = this.actionMap.get(definition.comandoCanonico());
        }

        // 3.2. Si se encontró una Action, configurarla en el botón
        if (action != null) {
            button.setAction(action); // Esto establece texto, estado enabled, y el tooltip por defecto de la Action

            // 3.2.1. Intentar obtener el icono DESDE la Action.
            //        Se asume que la Action ya tiene su SMALL_ICON configurado con el tamaño correcto
            //        (o al menos un icono base que luego podríamos escalar si fuera necesario aquí).
            Object iconValueFromAction = action.getValue(Action.SMALL_ICON);
            if (iconValueFromAction instanceof ImageIcon) {
                iconoFinalParaBoton = (ImageIcon) iconValueFromAction;
                System.out.println("  -> Icono obtenido de Action para: " + definition.comandoCanonico());
            } else {
                System.out.println("  -> WARN: Action '" + definition.comandoCanonico() + "' no tiene un ImageIcon en SMALL_ICON.");
                // Fallback: Si la Action no tiene icono, intentar cargarlo desde claveIcono de la definición
                if (definition.claveIcono() != null) {
                    iconoFinalParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                    if (iconoFinalParaBoton == null) {
                        System.err.println("    -> ERROR: Falló carga fallback del icono desde claveIcono: " + definition.claveIcono());
                    }
                }
            }

            // 3.2.2. Sobrescribir el tooltip si el de la definición es específico y diferente al de la Action.
            //         (Tu lógica actual para el tooltip al inicio ya es bastante buena, esto es un refinamiento)
            String tooltipDef = definition.textoTooltip();
            String tooltipAction = (String) action.getValue(Action.SHORT_DESCRIPTION);
            if (tooltipDef != null && !tooltipDef.isBlank() && !tooltipDef.equals(tooltipAction)) {
                button.setToolTipText(tooltipDef);
            }

        }
        // 3.3. Si NO se encontró una Action (o comandoCanonico era null)
        else {
            if (definition.comandoCanonico() != null) { // Solo si se esperaba una Action
                 System.err.println("WARN [ToolbarBuilder]: No se encontró Action para comando: " + definition.comandoCanonico() + ". Configurando manualmente.");
            }
            // 3.3.1. Asignar el comando canónico (si existe) como ActionCommand.
            //          Esto es útil para botones que no tienen Action funcional pero necesitan ser identificados
            //          (ej. botones de configuración de UI que son manejados por el actionPerformed central).
            if (definition.comandoCanonico() != null) {
                button.setActionCommand(definition.comandoCanonico());
            }

            // 3.3.2. Cargar el icono directamente desde la claveIcono de la definición.
            if (definition.claveIcono() != null) {
                iconoFinalParaBoton = this.iconUtils.getScaledIcon(definition.claveIcono(), this.iconoAncho, this.iconoAlto);
                if (iconoFinalParaBoton == null) {
                    System.err.println("    -> ERROR: Falló carga del icono desde claveIcono: " + definition.claveIcono());
                }
            }

            // 3.3.3. Añadir ActionListener fallback si este botón no tiene Action funcional
            //          (para que al menos el log se active si se hace clic).
            if (this.controllerRef != null) {
                 button.addActionListener(this.controllerRef); // VisorController implementa ActionListener
                 System.out.println("  -> Añadido ActionListener fallback a botón sin Action: " + (definition.comandoCanonico() != null ? definition.comandoCanonico() : definition.claveIcono()));
            } else {
                 // Fallback si controllerRef no está disponible (no debería pasar si el constructor se actualizó)
                 button.addActionListener(event -> {
                     System.out.println("--- BOTÓN SIN ACTION CLICADO (Fallback genérico) ---");
                     System.out.println("  > Comando: " + event.getActionCommand());
                     System.out.println("----------------------------------------------");
                 });
            }
        }

        // --- 4. Establecer Icono Final y Manejar Texto del Botón ---
        if (iconoFinalParaBoton != null) {
            button.setIcon(iconoFinalParaBoton);
            button.setHideActionText(true); // Si hay icono, generalmente no queremos texto.
            button.setText("");             // Asegurar que no haya texto.
        } else {
            // Si no hay icono, decidimos qué texto mostrar.
            if (action != null && action.getValue(Action.NAME) != null) {
                // Si hay Action y tiene nombre, usarlo (setAction ya lo hizo, pero setHideActionText lo controla).
                button.setHideActionText(false);
            } else if (definition.comandoCanonico() != null){
                // Si no hay Action pero sí comando, mostrar una parte del comando o un placeholder.
                button.setText("?"); // O una abreviatura del comando
                button.setHideActionText(false);
            } else {
                 button.setText("Err"); // Error, sin icono ni info de texto
                 button.setHideActionText(false);
            }
            System.err.println("WARN [ToolbarBuilder]: No se pudo establecer icono para botón: " + (definition.comandoCanonico() != null ? definition.comandoCanonico() : definition.claveIcono()));
        }
        // Opcional: configurar button.setDisabledIcon(...) si tienes iconos específicos para estado deshabilitado.


        // --- 5. Generar Clave Larga de Configuración y Añadir al Mapa del Builder ---
        //    Usamos la 'claveIcono' para generar el nombre base para mantener compatibilidad
        //    con tu `config.cfg` actual.
        String nombreBaseBoton;
        if (definition.claveIcono() != null && !definition.claveIcono().isBlank()) {
            nombreBaseBoton = definition.claveIcono().replace(".png", "").replaceAll("^\\d+-", "");
        } else if (definition.comandoCanonico() != null) {
            // Fallback si no hay claveIcono, intentar generar desde comando canónico
            nombreBaseBoton = definition.comandoCanonico().replace("cmd.", "").replace(".", "_");
        } else {
            nombreBaseBoton = "unknown_button_" + definition.hashCode(); // Fallback muy genérico
        }

        String fullConfigKey = "interfaz.boton." + definition.categoriaLayout() + "." + nombreBaseBoton;
        this.botonesPorNombre.put(fullConfigKey, button);

        // --- 6. Añadir Botón al Panel Correcto ---
        int alignment = getAlignmentForCategory(definition.categoriaLayout());
        if (alignment == FlowLayout.LEFT) panelBotonesIzquierda.add(button);
        else if (alignment == FlowLayout.CENTER) panelBotonesCentro.add(button);
        else if (alignment == FlowLayout.RIGHT) panelBotonesDerecha.add(button);
        else {
            System.err.println("WARN [ToolbarBuilder]: Alineación desconocida para categoría '" + definition.categoriaLayout() + "'. Usando panel central.");
            panelBotonesCentro.add(button); // Fallback
        }

    } // --- Fin procesarToolbarButtonDefinition ---	
	

	/**
	 * Añade un separador visual al panel correspondiente. (Lógica movida desde
	 * VisorView.separadorVisualParaBotones)
	 */
	private void addSeparator (int alignment)
	{
		if (alignment == FlowLayout.LEFT)
			panelBotonesIzquierda.add(Box.createHorizontalStrut(10));
		else if (alignment == FlowLayout.CENTER)
			panelBotonesCentro.add(Box.createHorizontalStrut(10));
		else if (alignment == FlowLayout.RIGHT)
			panelBotonesDerecha.add(Box.createHorizontalStrut(10));
	}
	
	
    /**
     * Devuelve el mapa que asocia las claves de configuración LARGAS
     * con las instancias de JButton creadas.
     *
     * @return El mapa de botones por nombre (clave larga).
     */
    public Map<String, JButton> getBotonesPorNombre() { // Sin "Map" al final del nombre
        return this.botonesPorNombre;
    }

//  public JPanel buildToolbar() {
//  // --- Definición de Iconos y Layout (Movido desde VisorView) ---
//	
//  List<String> iconosMovimiento = List.of(
//  		"1001-Primera_48x48.png",	//new
//  		"1002-Anterior_48x48.png", 
//  		"1003-Siguiente_48x48.png",
//  		"1004-Ultima_48x48.png"		//new
//  		);
//  
//  List<String> iconosEdicion = List.of(
//  		"2001-Rotar_Izquierda_48x48.png", 
//  		"2002-Rotar_Derecha_48x48.png", 
//  		"2003-Espejo_Horizontal_48x48.png", 
//  		"2004-Espejo_Vertical_48x48.png", 
//  		"2005-Recortar_48x48.png"
//  		);
//  
//  List<String> iconosZoom = List.of(
//  		"3001-Zoom_48x48.png", 
//  		"3002-Zoom_Auto_48x48.png", 
//  		"3003-Ajustar_al_Ancho_48x48.png", 
//  		"3004-Ajustar_al_Alto_48x48.png", 
//  		"3005-Escalar_Para_Ajustar_48x48.png", 
//  		"3006-Zoom_Fijo_48x48.png", 
//  		"3007-zoom_especifico_48x48.png",
//  		"3008-Reset_48x48.png"
//  		);
//  
//  List<String> iconosVista = List.of(
//  		"4001-Panel-Galeria_48x48.png", 
//  		"4002-Grid_48x48.png", 
//  		"4003-Pantalla_Completa_48x48.png", 
//  		"4004-Lista_48x48.png", 
//  		"4005-Carrousel_48x48.png"
//  		);
//  
//  List<String> iconosControl = List.of(
//  		"5001-Refrescar_48x48.png", 
//  		"5003-marcar_imagen_48x48.png", 
//  		"5004-Borrar_48x48.png",
//  		"7004-Ubicacion_de_Archivo_48x48.png"
//  		);
//  
//  List<String> iconosEspeciales = List.of(
//  		"6001-Selector_de_Carpetas_48x48.png", 
//  		"6002-Menu_48x48.png", 
//  		"6003-Botones_Ocultos_48x48.png"
//  		);
//  
//  List<String> iconosToggle = List.of(
//  		"7001-Subcarpetas_48x48.png",
//  		"7002-Mantener_Proporciones_48x48.png",
//  		"7003-Mostrar_Favoritos_48x48.png"
//  		);
//
//  List<ButtonGroupConfig> buttonLayoutConfig = List.of(
//      new ButtonGroupConfig("movimiento", FlowLayout.LEFT, iconosMovimiento),
//      new ButtonGroupConfig("edicion", FlowLayout.CENTER, iconosEdicion),
//      new ButtonGroupConfig("zoom", FlowLayout.CENTER, iconosZoom),
//      new ButtonGroupConfig("vista", FlowLayout.CENTER, iconosVista),
//      new ButtonGroupConfig("toggle", FlowLayout.CENTER, iconosToggle),
//      new ButtonGroupConfig("control", FlowLayout.CENTER, iconosControl),
//      new ButtonGroupConfig("especiales", FlowLayout.RIGHT, iconosEspeciales)
//      
//  );
//  // -------------------------------------------------------------
//
//  boolean firstGroup = true;
//  for (ButtonGroupConfig config : buttonLayoutConfig) {
//      if (!firstGroup) {
//          addSeparator(config.alignment); // Usa método interno
//      } else {
//          firstGroup = false;
//      }
//
//      for (String nombreIcono : config.iconNames) {
//          // Llama al método interno para crear y configurar el botón
//          procesarBoton(nombreIcono, config.category, config.alignment);
//      }
//  }
//
//  return toolbarPanel; // Devuelve el panel construido
//} //--- FIN metodo buildToolbar 
    
    
} //--- FIN ToolbarBuilder
