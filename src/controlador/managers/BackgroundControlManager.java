package controlador.managers;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.border.Border;

import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import controlador.utils.ComponentRegistry;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;


/**
 * Gestiona la lógica y la apariencia de la barra de herramientas
 * para el control del fondo de la imagen principal.
 */
public class BackgroundControlManager {

    // --- Dependencias ---
    private final ComponentRegistry registry;
    private final ThemeManager themeManager;
    private final IViewManager viewManager;
    private final ConfigurationManager config;
    private final IconUtils iconUtils;

    // --- Estado Interno ---
    private final List<JButton> swatchButtons = new ArrayList<>();
    private final Border bordeNormal;
    private final Border bordeSeleccionado;
    private JButton botonSeleccionadoActual;
    
    // Color que representa una ranura "vacía", lista para elegir un nuevo color.
    private static final Color EMPTY_SLOT_COLOR = Color.WHITE;

    
    public BackgroundControlManager(ComponentRegistry registry, ThemeManager themeManager, IViewManager viewManager, ConfigurationManager config, IconUtils iconUtils, Border bordeNormal, Border bordeSeleccionado) {
        this.registry = registry;
        this.themeManager = themeManager;
        this.viewManager = viewManager;
        this.config = config;
        this.iconUtils = iconUtils;
        
        int underlineThickness = 2; // Grosor del subrayado en píxeles.
        
        // El borde SELECCIONADO es nuestro subrayado (esto ya estaba bien).
        Color selectionColor = themeManager.getTemaActual().colorBordeSeleccionActiva();
        this.bordeSeleccionado = new vista.util.UnderlineBorder(selectionColor, underlineThickness);
        
        // --- INICIO DE LA CORRECCIÓN CLAVE ---
        // El borde NORMAL debe ser un borde vacío que reserve espacio ABAJO,
        // exactamente igual que el UnderlineBorder.
        // ANTES: createEmptyBorder(underlineThickness, 0, 0, 0) <-- Mal, espacio arriba
        // AHORA: createEmptyBorder(0, 0, underlineThickness, 0) <-- Bien, espacio abajo
        this.bordeNormal = javax.swing.BorderFactory.createEmptyBorder(0, 0, underlineThickness, 0);
        
        // --- FIN DE LA CORRECCIÓN CLAVE ---
    }
    
    
    public void initializeAndLinkControls() {
        System.out.println("\n--- [BackgroundControlManager] Iniciando y enlazando controles de fondo ---");
        JToolBar controlToolbar = registry.get("toolbar.controles_imagen_inferior");
        if (controlToolbar == null) {
            System.err.println("  ERROR CRÍTICO: No se encontró la toolbar 'toolbar.controles_imagen_inferior'.");
            return;
        }
        
        this.swatchButtons.clear();

        for (java.awt.Component comp : controlToolbar.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String command = (String) button.getClientProperty("canonicalCommand");

                if (command != null && command.startsWith("cmd.background.")) {
                	
                	if (command.equals(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR)) {
                        continue; // <-- ¡ESTA LÍNEA ES LA SOLUCIÓN!
                    }
                	
                    transformToSwatchButton(button);
                    this.swatchButtons.add(button);
                    updateSwatchAppearance(button);

                    // Limpiamos CUALQUIER listener previo para tener control total
                    for (ActionListener al : button.getActionListeners()) {
                        button.removeActionListener(al);
                    }
                    for (java.awt.event.MouseListener ml : button.getMouseListeners()) {
                        // Evitamos quitar listeners internos de LookAndFeel, solo los nuestros.
                        if (ml instanceof java.awt.event.MouseAdapter) {
                            button.removeMouseListener(ml);
                        }
                    }

                    // Añadimos nuestro nuevo y único MouseListener a CADA botón de fondo.
                    button.addMouseListener(createUnifiedMouseListener());
                }
            }
        }
        
        // ======================= BLOQUE ELIMINADO =======================
        // El siguiente bucle que intentaba forzar la selección ha sido eliminado
        // porque ahora el método de sincronización se encargará de esto de
        // forma más robusta.
        // ================================================================
        
        System.out.println("--- [BackgroundControlManager] Enlace de controles completado. " + this.swatchButtons.size() + " botones de fondo gestionados. ---");
    } // --- Fin del método initializeAndLinkControls ---
    
    
    private void handleDoubleClickOnSlot(JButton button) {
        String command = (String) button.getClientProperty("canonicalCommand");
        String configKey = getConfigKeyForCommand(command);
        if (configKey == null) return;

        // Preguntar al usuario (esto ya estaba bien)
        int response = JOptionPane.showConfirmDialog(
            registry.get("frame.main"),
            "¿Deseas borrar el color personalizado de esta ranura?",
            "Restaurar Ranura de Color",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            // Borrar la clave del archivo de configuración (esto ya estaba bien)
            config.removeProperty(configKey);
            
            // Volver a pintar el botón para que tome su color por defecto (esto ya estaba bien)
            updateSwatchAppearance(button);
            System.out.println("Color para la ranura " + command + " restaurado.");
            
            // --- INICIO DE LA LÓGICA AÑADIDA ---
            
            // 1. Aplicamos el color del tema actual como el nuevo fondo.
            Color colorDelTema = themeManager.getTemaActual().colorFondoSecundario();
            viewManager.setSessionBackgroundColor(colorDelTema);
            
            // 2. Buscamos el botón de 'reset' por su comando canónico.
            JButton botonReset = findButtonByCommand(AppActionCommands.CMD_BACKGROUND_THEME_COLOR);
            
            // 3. Cambiamos la selección visual al botón de reset.
            if (botonReset != null) {
                selectButton(botonReset);
            } else {
                // Si por alguna razón no lo encontramos, al menos deseleccionamos el actual.
                selectButton(null);
            }
            
            // --- FIN DE LA LÓGICA AÑADIDA ---
        }
    } // --- Fin del Metodo handleDoubleClickOnSlot ---
    
    
    /**
     * Busca y devuelve un botón de la lista de swatches gestionados que coincida
     * con el comando canónico proporcionado.
     *
     * @param command El comando canónico a buscar (ej. AppActionCommands.CMD_BACKGROUND_THEME_COLOR).
     * @return El JButton correspondiente, o null si no se encuentra.
     */
    private JButton findButtonByCommand(String command) {
        for (JButton btn : this.swatchButtons) {
            String btnCommand = (String) btn.getClientProperty("canonicalCommand");
            if (command.equals(btnCommand)) {
                return btn;
            }
        }
        System.err.println("WARN [BackgroundControlManager]: No se pudo encontrar el botón con comando: " + command);
        return null; // No se encontró
        
    } // FIN del metodo findButtonByCommand ---
    
    
    
    /**
     * Transforma un JButton genérico en un "botón de muestra de color" (swatch),
     * aplicando las propiedades visuales necesarias para que se vea plano, sin bordes,
     * y listo para que se le pinte un icono de color.
     * @param button El JButton a transformar.
     */
    private void transformToSwatchButton(JButton button) {
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(true); 
        button.setMargin(new java.awt.Insets(1, 1, 1, 1));
        button.setPreferredSize(new java.awt.Dimension(22, 22));
    } // --- Fin del método transformToSwatchButton ---
    

    private void updateSwatchAppearance(JButton button) {
        String command = (String) button.getClientProperty("canonicalCommand");
        if (command == null) return;
        
        

        // --- INICIO DE LA MODIFICACIÓN ---

        // CASO ESPECIAL 1: Botón de la Paleta.
        // Este botón tiene un icono estático y NUNCA debe ser modificado por este método.
        // Salimos inmediatamente.
        if (command.equals(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR)) {
            // No hacemos NADA. Dejamos el icono que puso el ToolbarBuilder.
            return; 
        }

        // --- AHORA, para el resto de botones, que SÍ son "swatches", continuamos ---

        // Obtenemos el icono base (el círculo "stopw.png").
        Icon baseIcon = (Icon) button.getClientProperty("baseIcon");
        if (baseIcon == null) {
            // Esta lógica de rescate es buena, la mantenemos por robustez.
            Object baseIconNameObj = button.getClientProperty("baseIconName");
            if (baseIconNameObj instanceof String) {
                String baseIconName = (String) baseIconNameObj;
                ImageIcon reloadedIcon = iconUtils.getScaledCommonIcon(baseIconName, 16, 16);
                if (reloadedIcon != null) {
                    baseIcon = reloadedIcon;
                    button.putClientProperty("baseIcon", baseIcon);
                }
            }
        }
        
        // Si después del rescate sigue sin haber icono base, no podemos continuar.
        if (baseIcon == null) {
            System.err.println("ERROR GRAVE [updateSwatchAppearance]: El botón para el comando '" + command + "' no tiene un icono base para pintar.");
            return;
        }

        // CASO ESPECIAL 2: Botón de Cuadros.
        // Este sí lo modificamos, pero con su propia lógica de patrón.
        if (command.equals(AppActionCommands.CMD_BACKGROUND_CHECKERED)) {
            button.setIcon(iconUtils.getCheckeredOverlayIcon(baseIcon, 16, 16));
            return; // Salimos después de aplicar el patrón.
        }

        // --- FIN DE LA MODIFICACIÓN ---
        
        // Lógica para el resto de botones (los de colores tintados).
        Color colorOriginal;
        if (command.equals(AppActionCommands.CMD_BACKGROUND_THEME_COLOR)) {
            colorOriginal = themeManager.getTemaActual().colorFondoSecundario();
        } else {
            String configKey = getConfigKeyForCommand(command);
            String colorGuardadoStr = config.getString(configKey, "");
            if (!colorGuardadoStr.isEmpty()) {
                colorOriginal = config.getColor(configKey, Color.MAGENTA);
            } else {
                colorOriginal = getDefaultColorForSlot(command);
            }
        }

        Color colorFinalParaPintar = ajustarContrasteSiEsNecesario(colorOriginal);
        
        button.setIcon(iconUtils.getTintedIcon(baseIcon, colorFinalParaPintar, 16, 16));
    
    } // --- FIN del metodo updateSwatchAppearance ---
    
    
//    private void updateSwatchAppearance(JButton button) {
//        String command = (String) button.getClientProperty("canonicalCommand");
//        if (command == null) return;
//
//        // --- INICIO DE LA LÓGICA CORREGIDA Y ROBUSTA ---
//        Icon baseIcon = (Icon) button.getClientProperty("baseIcon");
//        
//        // SI NO ENCUENTRA el icono base (porque es un botón nuevo de una toolbar reconstruida),
//        // lo intentamos cargar AHORA.
//        if (baseIcon == null) {
//            System.out.println("  [updateSwatchAppearance] WARN: Icono base no encontrado en el botón para '" + command + "'. Intentando recarga manual...");
//            Object baseIconNameObj = button.getClientProperty("baseIconName");
//            if (baseIconNameObj instanceof String) {
//                String baseIconName = (String) baseIconNameObj;
//                // Usamos iconUtils para obtener el icono escalado, igual que haría el ToolbarBuilder
//                ImageIcon reloadedIcon = iconUtils.getScaledCommonIcon(baseIconName, 16, 16);
//                if (reloadedIcon != null) {
//                    baseIcon = reloadedIcon;
//                    // Y lo guardamos para la próxima vez
//                    button.putClientProperty("baseIcon", baseIcon);
//                    System.out.println("    -> Icono base '" + baseIconName + "' recargado y cacheado en el botón con éxito.");
//                }
//            }
//        }
//        
//        if (baseIcon == null) {
//            System.err.println("ERROR GRAVE [updateSwatchAppearance]: El botón para el comando '" + command + "' no tiene un icono base para pintar y no se pudo recargar.");
//            return;
//        }
//        // --- FIN DE LA LÓGICA CORREGIDA Y ROBUSTA ---
//
//        // El resto de la lógica es la misma
//        if (command.equals(AppActionCommands.CMD_BACKGROUND_CHECKERED)) {
//            button.setIcon(iconUtils.getCheckeredOverlayIcon(baseIcon, 16, 16));
//            return;
//        }
//        if (command.equals(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR)) {
//            return; 
//        }
//
//        
//        
//        
//        
//        Color colorOriginal;
//        if (command.equals(AppActionCommands.CMD_BACKGROUND_THEME_COLOR)) {
//            colorOriginal = themeManager.getTemaActual().colorFondoSecundario();
//        } else {
//            String configKey = getConfigKeyForCommand(command);
//            String colorGuardadoStr = config.getString(configKey, "");
//            if (!colorGuardadoStr.isEmpty()) {
//                colorOriginal = config.getColor(configKey, Color.MAGENTA);
//            } else {
//                colorOriginal = getDefaultColorForSlot(command);
//            }
//        }
//
//        Color colorFinalParaPintar = ajustarContrasteSiEsNecesario(colorOriginal);
//        
//        button.setIcon(iconUtils.getTintedIcon(baseIcon, colorFinalParaPintar, 16, 16));
//    
//    } //--- FIN del metodo updateSwatchAppearance ---

    
	/**
	 * Revisa la luminosidad de un color y, si es demasiado claro o demasiado oscuro,
	 * devuelve una versión ajustada. Si no, devuelve el color original.
	 * @param colorOriginal El color a revisar.
	 * @return El color ajustado o el original.
	 */
	private Color ajustarContrasteSiEsNecesario(Color colorOriginal) {
	    if (colorOriginal == null) return Color.GRAY;
	
	    int luminosidad = colorOriginal.getRed() + colorOriginal.getGreen() + colorOriginal.getBlue();
	    
	    final int UMBRAL_OSCURO = 150;
	    final int UMBRAL_CLARO = 700;
	    final int CANTIDAD_AJUSTE = 55;
	
	    if (luminosidad < UMBRAL_OSCURO) {
	        // El iconUtils ahora tiene los métodos públicos.
	        return iconUtils.aclararColor(colorOriginal, CANTIDAD_AJUSTE);
	    } else if (luminosidad > UMBRAL_CLARO) {
	        return iconUtils.oscurecerColor(colorOriginal, CANTIDAD_AJUSTE);
	    }
	    
	    // Si el color está en un rango aceptable, lo devolvemos sin cambios.
	    return colorOriginal;
	    
	}// --- FIN del metodo ajustarContrasteSiEsNecesario ---
    
    
	private java.awt.event.MouseListener createUnifiedMouseListener() {
	    return new java.awt.event.MouseAdapter() {
	        private javax.swing.Timer clickTimer = null;

	        @Override
	        public void mouseClicked(java.awt.event.MouseEvent e) {
	            JButton sourceButton = (JButton) e.getSource();
	            String command = (String) sourceButton.getClientProperty("canonicalCommand");
	            if (command == null) return;

	            if (e.getClickCount() >= 2) {
	                // --- DOBLE CLIC ---
	                if (clickTimer != null && clickTimer.isRunning()) {
	                    clickTimer.stop();
	                }
	                if (command.startsWith("cmd.background.color.slot_")) {
	                    handleDoubleClickOnSlot(sourceButton); // Llama a la lógica que ya tenemos
	                }
	                
	                // --- !! LÍNEA ELIMINADA !! ---
	                // Se ha eliminado la siguiente línea que causaba el problema:
	                // selectButton(sourceButton); 
	                // Ahora, el método handleDoubleClickOnSlot tiene el control total sobre la selección.

	            } else if (e.getClickCount() == 1) {
	                // --- CLIC SIMPLE (CON TEMPORIZADOR PARA ESPERAR AL DOBLE CLIC) ---
	                clickTimer = new javax.swing.Timer(250, event -> {
	                    
	                    if (command.equals(AppActionCommands.CMD_BACKGROUND_CHECKERED)) {
	                        viewManager.setSessionCheckeredBackground();
	                    } else if (command.equals(AppActionCommands.CMD_BACKGROUND_THEME_COLOR)) {
	                        viewManager.setSessionBackgroundColor(themeManager.getTemaActual().colorFondoSecundario());
	                    } else if (command.equals(AppActionCommands.CMD_BACKGROUND_CUSTOM_COLOR)) {
	                        viewManager.requestCustomBackgroundColor();
	                        // Para la paleta, no seleccionamos ningún botón
	                        selectButton(null);
	                        return; // Salimos para no llamar a selectButton de nuevo
	                    } else if (command.startsWith("cmd.background.color.slot_")) {
	                        handleCustomColorSlotClick(sourceButton, command);
	                    }
	                    
	                    // Marcamos el botón en el que hemos hecho clic (esto está bien para clic simple)
	                    selectButton(sourceButton);
	                });
	                clickTimer.setRepeats(false);
	                clickTimer.start();
	            }
	        }
	    };
	}// --- FIN DEL METODO createUnifiedMouseListener ---
	
	
    /**
     * Gestiona la lógica de clic para una ranura de color personalizada.
     * Si la ranura está vacía, abre un selector de color. Si ya tiene un color, lo aplica.
     *
     * @param button El botón que fue presionado.
     * @param command El comando canónico del botón.
     */
    private void handleCustomColorSlotClick(JButton button, String command) {
        String configKey = getConfigKeyForCommand(command);
        if (configKey == null) return;

        // 1. Comprobamos si hay un color guardado en el archivo de configuración.
        //    Usamos getString() con un default vacío para saber si la clave tiene un valor real.
        String colorGuardadoStr = config.getString(configKey, "");

        if (!colorGuardadoStr.isEmpty()) {
            // 2. SI HAY UN COLOR GUARDADO: Simplemente lo aplicamos.
            Color colorGuardado = config.getColor(configKey, Color.BLACK); // Fallback por si el formato es corrupto
            viewManager.setSessionBackgroundColor(colorGuardado);
            
        } else {
            // 3. SI NO HAY UN COLOR GUARDADO (la ranura está "vacía"):
            //    Abrimos el selector de color.
            Color colorPorDefecto = getDefaultColorForSlot(command);
            Color nuevoColor = JColorChooser.showDialog(
                registry.get("frame.main"), 
                "Seleccionar Color para Ranura", 
                colorPorDefecto // Le pasamos el color por defecto como sugerencia inicial
            );
            
            // Si el usuario eligió un color (no canceló)
            if (nuevoColor != null) {
                // Guardamos el nuevo color en la configuración.
                config.setColor(configKey, nuevoColor);
                // Aplicamos el nuevo color a la sesión actual.
                viewManager.setSessionBackgroundColor(nuevoColor);
                // Repintamos el botón para que muestre su nuevo color.
                updateSwatchAppearance(button);
            }
        }
    } // --- FIN DEL METODO handleCustomColorSlotClick ---
    
    /**
     * Vuelve a pintar todos los botones de la barra de control de fondo.
     * Es útil para llamar después de un cambio de tema, ya que el botón
     * del tema actual debe actualizar su color.
     */
    public void repaintAllButtons() {
        System.out.println("[BackgroundControlManager] Solicitud de repintado para todos los botones de fondo.");
        for (JButton button : this.swatchButtons) {
            updateSwatchAppearance(button);
        }
    } // --- FIN DEL Metodo repaintAllButtons ---
    
    /**
     * Devuelve la clave de configuración de `ConfigKeys` correspondiente a un `ActionCommand` de ranura.
     */
    private String getConfigKeyForCommand(String command) {
        if (command == null) {
            return null;
        }

        switch (command) {
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1:
                return ConfigKeys.BACKGROUND_CUSTOM_COLOR_1;
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2:
                return ConfigKeys.BACKGROUND_CUSTOM_COLOR_2;
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3:
                return ConfigKeys.BACKGROUND_CUSTOM_COLOR_3;
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4:
                return ConfigKeys.BACKGROUND_CUSTOM_COLOR_4;
                
            default:
                return null;
        }
    } // --- Fin del método getConfigKeyForCommand ---
    
    
    /**
     * Devuelve el color por defecto para una ranura de color específica.
     * La lógica ahora mapea directamente cada slot a un tema predefinido.
     *
     * @param command El comando del botón de la ranura.
     * @return El color de fondo secundario del tema correspondiente.
     */
    private Color getDefaultColorForSlot(String command) {
        switch (command) {
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_1:
                return themeManager.getFondoSecundarioParaTema("dark");
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_2:
                return themeManager.getFondoSecundarioParaTema("blue");
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_3:
                // El slot 3 debe ser Naranja
                return themeManager.getFondoSecundarioParaTema("orange"); 
                
            case AppActionCommands.CMD_BACKGROUND_COLOR_SLOT_4:
                // El slot 4 debe ser Verde
                return themeManager.getFondoSecundarioParaTema("green");
                
            default:
                return EMPTY_SLOT_COLOR;
        }
    } // --- Fin del método getDefaultColorForSlot ---
    
    /**
     * Comprueba si un color dado coincide con el color de fondo secundario
     * de CUALQUIERA de los temas predefinidos en el ThemeManager.
     * Esta es la herramienta clave para distinguir entre un color por defecto y uno personalizado.
     * @param color El color a comprobar.
     * @return true si es un color "de fábrica", false si es personalizado o nulo.
     */
    private boolean esUnColorDeTemaPorDefecto(Color color) {
        if (color == null) {
            return false;
        }
        // Obtenemos la lista de todos los temas y comprobamos si el color
        // coincide con el colorFondoSecundario de alguno de ellos.
        for (Tema tema : themeManager.getTemasOrdenados()) {
            if (color.equals(tema.colorFondoSecundario())) {
                return true; // ¡Coincidencia! Es un color de tema.
            }
        }
        // Si hemos recorrido todos los temas y no hay coincidencia, es un color personalizado.
        return false;
        
    }// --- FIN del metodo esUnColorDeTemaPorDefecto ---

    
    /**
     * Actualiza el borde de los botones para resaltar el que está seleccionado.
     */
    private void selectButton(JButton botonASeleccionar) {
        // 1. Si ya había un botón seleccionado, le devolvemos el borde normal.
        if (this.botonSeleccionadoActual != null) {
            this.botonSeleccionadoActual.setBorder(bordeNormal);
        }

        // 2. Si el nuevo botón no es nulo, le ponemos el borde de seleccionado.
        if (botonASeleccionar != null) {
            botonASeleccionar.setBorder(bordeSeleccionado);
        }

        // 3. Actualizamos la referencia al botón seleccionado actualmente.
        this.botonSeleccionadoActual = botonASeleccionar;
    } // --- Fin del método selectButton ---
    
    /**
     * Sincroniza el borde de selección de los botones de color basándose en el estado
     * actual del panel de visualización de la imagen.
     */
    public void sincronizarSeleccionConEstadoActual() {
        System.out.println("[BackgroundControlManager] Sincronizando selección de botón de color...");
        
        vista.panels.ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) return;

        // Caso 1: El fondo es a cuadros
        if (displayPanel.isCheckeredBackground()) {
            for (JButton btn : this.swatchButtons) {
                String cmd = (String) btn.getClientProperty("canonicalCommand");
                if (AppActionCommands.CMD_BACKGROUND_CHECKERED.equals(cmd)) {
                    selectButton(btn);
                    return; // Encontramos el botón, trabajo terminado.
                }
            }
        }

        // Caso 2: El fondo es de un color sólido
        Color colorDeFondoActual = displayPanel.getBackground();

        // Buscamos qué botón representa a este color
        for (JButton btn : this.swatchButtons) {
            String cmd = (String) btn.getClientProperty("canonicalCommand");
            if (cmd == null || !cmd.startsWith("cmd.background.color.")) continue;

            Color colorDelBoton;
            if (AppActionCommands.CMD_BACKGROUND_THEME_COLOR.equals(cmd)) {
                // El botón de Reset representa el color del tema actual
                colorDelBoton = themeManager.getTemaActual().colorFondoSecundario();
            } else {
                // Un slot representa el color guardado, o su color por defecto si no hay nada guardado
                String configKey = getConfigKeyForCommand(cmd);
                String colorGuardadoStr = config.getString(configKey, "");
                if (!colorGuardadoStr.isEmpty()) {
                    colorDelBoton = config.getColor(configKey, null);
                } else {
                    colorDelBoton = getDefaultColorForSlot(cmd);
                }
            }
            
            if (colorDeFondoActual.equals(colorDelBoton)) {
                selectButton(btn);
                return; // Encontramos el botón, trabajo terminado.
            }
        }

        // Si llegamos aquí, el color de fondo no coincide con ningún botón (ej. elegido con la paleta)
        selectButton(null); // Deseleccionamos todos los botones.
        
    }// FIN del metodo sincronizarSeleccionConEstadoActual ---
    
} // --- FIN DE LA CLASE BackgroundControlManager ---