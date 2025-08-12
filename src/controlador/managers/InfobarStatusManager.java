package controlador.managers;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.zoom.ZoomModeEnum;
import vista.config.ToolbarButtonDefinition;
import vista.config.UIDefinitionService;
import vista.theme.Tema;
//import vista.theme.ThemeChangeListener;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class InfobarStatusManager{//  implements ThemeChangeListener{

	private static final Logger logger = LoggerFactory.getLogger(InfobarStatusManager.class);
	
    private VisorController visorController;
    private final VisorModel model;
    private final ComponentRegistry registry;
    private final ThemeManager themeManager;
    private final ConfigurationManager configuration;
    private final ProjectManager projectService;
    private final Map<String, Action> actionMap;
    private final IconUtils iconUtils;
    private final UIDefinitionService uiDefService;
    private javax.swing.Timer mensajeTemporalTimer;
    private MouseListener zoomLabelMouseListener;
    
    public InfobarStatusManager(
            VisorModel model, 
            ComponentRegistry registry, 
            ThemeManager themeManager,
            ConfigurationManager configuration, 
            ProjectManager projectService,
            Map<String, Action> actionMap,
            IconUtils iconUtils) 
    {
        this.model = Objects.requireNonNull(model);
        this.registry = Objects.requireNonNull(registry);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.configuration = Objects.requireNonNull(configuration);
        this.projectService = Objects.requireNonNull(projectService);
        this.actionMap = Objects.requireNonNull(actionMap);
        this.iconUtils = Objects.requireNonNull(iconUtils); 
        this.uiDefService = new UIDefinitionService();
        
        configurarListenersControles();
        
    }// --- Fin del constructor --- 

    
    public void actualizar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::actualizarBarraEstado);
        } else {
            actualizarBarraEstado();
        }
    }// --- Fin del método actualizar ---

    /**
     * Muestra un mensaje en la etiqueta de estado de la aplicación.
     * Este mensaje permanecerá visible hasta que se llame a limpiarMensaje()
     * o se muestre otro mensaje.
     *
     * @param mensaje El texto a mostrar.
     */
    public void mostrarMensaje(String mensaje) {
        if (registry == null) return;
        JLabel mensajesAppLabel = registry.get("label.estado.mensajes");
        if (mensajesAppLabel != null) {
            mensajesAppLabel.setText(" " + mensaje + " "); // Añadir padding
        }
    }// --- Fin del método mostrarMensaje ---
    
    /**
     * Muestra un mensaje en la etiqueta de estado durante un tiempo determinado
     * y luego lo borra automáticamente.
     *
     * @param mensaje El texto a mostrar.
     * @param delayMs El tiempo en milisegundos que el mensaje será visible.
     */
    public void mostrarMensajeTemporal(String mensaje, int delayMs) {
        if (registry == null) return;
        JLabel mensajesAppLabel = registry.get("label.estado.mensajes");
        if (mensajesAppLabel == null) return;

        // Mostrar el mensaje inmediatamente
        mostrarMensaje(mensaje);

        // Si ya hay un timer en marcha, lo detenemos para empezar de nuevo
        if (mensajeTemporalTimer != null && mensajeTemporalTimer.isRunning()) {
            mensajeTemporalTimer.stop();
        }

        // Creamos o reconfiguramos el timer para que borre el mensaje después del delay
        mensajeTemporalTimer = new javax.swing.Timer(delayMs, e -> {
            // Esta acción se ejecuta cuando el timer termina
            limpiarMensaje();
        });
        
        mensajeTemporalTimer.setRepeats(false); // Queremos que se ejecute solo una vez
        mensajeTemporalTimer.start(); // Iniciar la cuenta atrás
    }// --- Fin del método mostrarMensajeTemporal ---

    
    
    
    /**
     * MÉTODO AÑADIDO: Actualiza la UI de zoom para un modo no compatible como GRID.
     */
    public void actualizarParaModoNoCompatible() {//weno
        logger.debug("[InfobarStatusManager] Actualizando UI de zoom para modo no compatible (Grid).");
        // Llama a los métodos que ya tienes para actualizar los componentes.
        // No necesitamos métodos nuevos como 'actualizarPorcentajeZoom(String)'.
        actualizarControlesDeZoom();
    }
    
    
    private void actualizarControlesDeZoom() {
        actualizarLabelZoom();

        // NO usamos registry.get("button.control.modoZoom").
        // En su lugar, buscamos el botón VIVO en el registro por su propiedad.
        JButton modoZoomBoton = null;
        for (Component comp : registry.getAllComponents()) { // Busca en todos los componentes registrados
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                Object commandProperty = button.getClientProperty("canonicalCommand");
                if ("cmd.control.modoZoom".equals(commandProperty)) {
                    modoZoomBoton = button; // Hemos encontrado el botón que está AHORA en la UI
                    
                    break;
                }
            }
        }

        if (modoZoomBoton != null && modoZoomBoton.isEnabled()) {
            boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, true);
            if (modoZoomBoton.isVisible() != esVisible) {
                modoZoomBoton.setVisible(esVisible);
            }
            
            if (esVisible && model.getCurrentZoomMode() != null && iconUtils != null) {
                ZoomModeEnum modoActual = model.getCurrentZoomMode();
                
                modoZoomBoton.setToolTipText("Modo actual: " + modoActual.getNombreLegible() + " (Clic para cambiar)");
                
                String iconKey = getIconKeyForZoomMode(modoActual);
                
                if (iconKey != null) {
                    ImageIcon nuevoIcono = iconUtils.getScaledIcon(iconKey, 18, 18);
                    modoZoomBoton.setIcon(nuevoIcono);
                    modoZoomBoton.setText(null);
                } else {
                    logger.warn("WARN [InfobarStatusManager]: No se encontró clave de icono para el modo: " + modoActual);
                    modoZoomBoton.setIcon(null);
                    modoZoomBoton.setText("?");
                }
                
                // Si quieres hacer la prueba, ahora sí que funcionará:
                // modoZoomBoton.setBackground(Color.RED); 
            }
        }
        
    } // Fin del metodo actualizarControlesDeZoom
    
    
    private void actualizarLabelZoom() {
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
        if (porcentajeLabel == null) return;
        if (!porcentajeLabel.isEnabled()) return;

        ZoomModeEnum modo = model.getCurrentZoomMode();
        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, true);
        
        if (porcentajeLabel.isVisible() != esVisible) {
            porcentajeLabel.setVisible(esVisible);
        }
        if (!esVisible) {
            porcentajeLabel.setOpaque(false);
            return;
        }

        // --- INICIO DE LA LÓGICA CORREGIDA ---

        boolean esModoManual = (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM || modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE);

        // Regla 1: Apariencia ("Falso Disable")
        Tema tema = themeManager.getTemaActual();
        if (tema != null) {
            if (esModoManual) {
                porcentajeLabel.setBackground(UIManager.getColor("StatusBar.zoomLabel.activeBackground"));
                porcentajeLabel.setForeground(UIManager.getColor("StatusBar.zoomLabel.activeForeground"));
            } else {
                porcentajeLabel.setBackground(UIManager.getColor("StatusBar.zoomLabel.inactiveBackground"));
                porcentajeLabel.setForeground(UIManager.getColor("StatusBar.zoomLabel.inactiveForeground"));
            }
            porcentajeLabel.setOpaque(true);
        }
        
        // Regla 2: Contenido del Texto
        // El label SIEMPRE muestra el valor del nuevo campo zoomCustomPercentage.
        double porcentajeAMostrar = model.getZoomCustomPercentage();
        porcentajeLabel.setText(String.format("Z: %.0f%%", porcentajeAMostrar));

        // Regla 3: Tooltip
        // El tooltip siempre muestra el zoom REAL de la imagen (zoomFactor).
        double porcentajeReal = model.getZoomFactor() * 100.0;
        porcentajeLabel.setToolTipText("Clic para establecer zoom. Fijado en: " + String.format("%.0f%%", porcentajeAMostrar) + ". Zoom real: " + String.format("%.2f%%", porcentajeReal));

    } // --- FIN del método actualizarLabelZoom ---
    
    
    /**
     * Limpia el texto de la etiqueta de mensajes de la barra de estado,
     * dejándola con un espacio en blanco.
     */
    public void limpiarMensaje() {
        if (registry == null) return;
        JLabel mensajesAppLabel = registry.get("label.estado.mensajes");
        if (mensajesAppLabel != null) {
            // Comprobar si hay un timer y detenerlo, para evitar que un
            // mensaje antiguo borre uno nuevo que se haya puesto manualmente.
            if (mensajeTemporalTimer != null && mensajeTemporalTimer.isRunning()) {
                mensajeTemporalTimer.stop();
            }
            mensajesAppLabel.setText(" "); // Dejar un espacio para mantener la altura
        }
    } // --- Fin del método limpiarMensaje ---
    
    private void actualizarBarraEstado() {
        JPanel panelBarraInferior = registry.get("panel.estado.inferior");
        if (panelBarraInferior == null) return;
        boolean panelVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_VISIBLE, true);
        if (panelBarraInferior.isVisible() != panelVisible) panelBarraInferior.setVisible(panelVisible);
        if (!panelVisible) return;
        
        actualizarRutaArchivoInferior();
        actualizarIndicadoresDeEstado();
        actualizarControlesDeZoom(); // Este es el método que depende del estado de habilitación
        actualizarMensajeDeEstado();
        panelBarraInferior.revalidate();
        panelBarraInferior.repaint();
    }// --- Fin del método actualizarBarraEstado ---

    private void actualizarRutaArchivoInferior() {
        JLabel label = registry.get("label.estado.ruta");
        if (label == null) return;
        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_VISIBLE, true);
        if(label.isVisible() != esVisible) label.setVisible(esVisible);
        if(esVisible) {
            String textoRutaDisplay = "(Ninguna imagen seleccionada)";
            Path rutaActual = (model.getSelectedImageKey() != null) ? model.getRutaCompleta(model.getSelectedImageKey()) : null;
            if (rutaActual != null) {
                String formato = configuration.getString(ConfigKeys.INFOBAR_INF_NOMBRE_RUTA_FORMATO, "ruta_completa");
                textoRutaDisplay = "solo_nombre".equalsIgnoreCase(formato) 
                    ? rutaActual.getFileName().toString() 
                    : rutaActual.toString();
            } else if (model.getCarpetaRaizActual() != null) {
                textoRutaDisplay = "Carpeta: " + model.getCarpetaRaizActual().toString();
            }
            label.setText(textoRutaDisplay);
            label.setToolTipText(textoRutaDisplay);
        }
    }// --- Fin del método actualizarRutaArchivoInferior ---

    private void actualizarIndicadoresDeEstado() {
    	
    	//FIXME corregir para que no se mire el metodo en particular desde aqui
        boolean funcionalidadesVisorActivas = (
        		model.getCurrentWorkMode() == VisorModel.WorkMode.VISUALIZADOR ||
                model.getCurrentWorkMode() == VisorModel.WorkMode.CARROUSEL);

        Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        Action proporcionesAction = actionMap.get(AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        Action subcarpetasAction = actionMap.get(AppActionCommands.CMD_TOGGLE_SUBCARPETAS);

        if (subcarpetasAction != null) {
            subcarpetasAction.setEnabled(funcionalidadesVisorActivas);
        }
        
        if (proporcionesAction != null) {
            proporcionesAction.setEnabled(funcionalidadesVisorActivas);
        }

        if (zoomManualAction != null) {
        	zoomManualAction.setEnabled(funcionalidadesVisorActivas);
        }
    } // --- Fin del método actualizarIndicadoresDeEstado ---
    
    
    private void actualizarMensajeDeEstado() {
        JLabel mensajesLabel = registry.get("label.estado.mensajes");
        if (mensajesLabel == null) {
            return;
        }

        Path rutaActual = null;
        if (model != null && model.getSelectedImageKey() != null) {
            rutaActual = model.getRutaCompleta(model.getSelectedImageKey());
        }

        boolean estaMarcada = false;
        if (rutaActual != null && projectService != null) {
            estaMarcada = projectService.estaMarcada(rutaActual);
        }

        if (estaMarcada) {
            mensajesLabel.setText("[MARCADA]");
            if (themeManager != null) {
                mensajesLabel.setForeground(themeManager.getTemaActual().colorBotonFondoActivado()); 
            }
        } else {
            mensajesLabel.setText(" ");
        }
    } // --- Fin del método actualizarMensajeDeEstado ---

    
    /**
     * Configura los listeners interactivos para los controles de la barra de estado,
     * como el popup de porcentaje de zoom y el menú de modos de zoom.
     * Este método está diseñado para ser seguro y poder llamarse varias veces
     * (por ejemplo, después de un cambio de tema) sin duplicar listeners.
     */
    public void configurarListenersControles() {
        logger.debug("  [StatusBarManager] Configurando Listeners para controles de la barra de estado...");
        
        // --- 1. Configuración del Listener para el JLabel de Porcentaje de Zoom ---
        // (Esta parte ya era correcta y no se modifica)
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
        
        if (porcentajeLabel != null) {
            if (this.zoomLabelMouseListener != null) {
                porcentajeLabel.removeMouseListener(this.zoomLabelMouseListener);
            }

            this.zoomLabelMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (porcentajeLabel.isEnabled()) {
                        mostrarMenuPorcentajes(porcentajeLabel);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (porcentajeLabel.isEnabled()) {
                        porcentajeLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    porcentajeLabel.setCursor(java.awt.Cursor.getDefaultCursor());
                }
            };
            
            porcentajeLabel.addMouseListener(this.zoomLabelMouseListener);
            logger.debug("    -> MouseListener configurado para 'label.control.zoomPorcentaje'.");
            
        } else {
            logger.warn("WARN [InfobarStatusManager]: Componente 'label.control.zoomPorcentaje' no encontrado en el registro.");
        }

        // --- 2. Configuración del Listener para el JButton de Modo de Zoom (CORREGIDO) ---

        JButton modoZoomBoton = null; // Mantenemos el nombre original
        for (Component comp : registry.getAllComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                Object commandProperty = button.getClientProperty("canonicalCommand");
                if ("cmd.control.modoZoom".equals(commandProperty)) {
                    modoZoomBoton = button;
                    break; 
                }
            }
        }
        
        // <<-- INICIO DE LA CORRECCIÓN MÍNIMA -->>
        // Asignamos la variable (que puede haber cambiado en el bucle) a una nueva
        // variable 'final' para que el lambda pueda usarla.
        final JButton finalModoZoomBoton = modoZoomBoton; 
        // <<-- FIN DE LA CORRECCIÓN MÍNIMA -->>
        
        if (finalModoZoomBoton != null) {
            // Quitamos los listeners antiguos
            for(ActionListener l : finalModoZoomBoton.getActionListeners()) {
                finalModoZoomBoton.removeActionListener(l);
            }
            
            // Añadimos el nuevo listener usando la variable final
            finalModoZoomBoton.addActionListener(e -> mostrarMenuModosZoom(finalModoZoomBoton));
            logger.debug("    -> ActionListener configurado para el botón de Modo de Zoom.");
            
        } else {
            logger.warn("WARN [InfobarStatusManager]: No se pudo encontrar el botón con el comando canónico 'cmd.control.modoZoom'.");
        }
        
    } // --- Fin del método configurarListenersControles ---
    
    
    private void mostrarMenuPorcentajes(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        int[] porcentajes = {25, 50, 75, 100, 150, 200};
        for (int p : porcentajes) {
            JMenuItem item = new JMenuItem(p + "%");
            item.addActionListener(e -> aplicarZoomPersonalizado(p));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem otrosItem = new JMenuItem("Otro...");
        otrosItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(invoker, "Introduce el porcentaje:", "Zoom Personalizado", JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                try {
                    aplicarZoomPersonalizado(Double.parseDouble(input.replace('%', ' ').trim()));
                } catch (NumberFormatException ex) { // <-- ¡CORRECCIÓN AQUÍ!
                    JOptionPane.showMessageDialog(invoker, "Porcentaje inválido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(otrosItem);
        menu.show(invoker, 0, -invoker.getHeight());
    }// --- Fin del método mostrarMenuPorcentajes
    
    private void mostrarMenuModosZoom(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        ZoomModeEnum[] modosParaMenu = {
            ZoomModeEnum.FIT_TO_SCREEN, 
            ZoomModeEnum.FIT_TO_WIDTH, 
            ZoomModeEnum.FIT_TO_HEIGHT,
            ZoomModeEnum.DISPLAY_ORIGINAL, 
            ZoomModeEnum.FILL,
            ZoomModeEnum.MAINTAIN_CURRENT_ZOOM, 
            ZoomModeEnum.USER_SPECIFIED_PERCENTAGE
        };
        for (ZoomModeEnum modo : modosParaMenu) {
            Action accionAsociada = actionMap.get(modo.getAssociatedActionCommand());
            if (accionAsociada != null) {
                JMenuItem item = new JMenuItem(accionAsociada);
                item.setText(modo.getNombreLegible());
                item.setIcon((javax.swing.Icon) accionAsociada.getValue(Action.SMALL_ICON));
                menu.add(item);
            }
        }
        menu.show(invoker, 0, -menu.getPreferredSize().height);
    }// --- Fin del método mostrarMenuModosZoom
    
    
    private void aplicarZoomPersonalizado(double porcentaje) {
        logger.debug("[StatusBarManager] ---> Delegando al VisorController la solicitud de zoom: " + porcentaje + "%");
        
        if (this.visorController != null) {
            this.visorController.solicitarZoomPersonalizado(porcentaje);
        } else {
            logger.error("ERROR: visorController es nulo. No se puede delegar la acción.");
        }
    } // --- FIN del metodo aplicarZoomPersonalizado ---
    
    
     /**
      * MÉTODO HELPER para aclarar un color.
      * @param color El color original.
      * @param factor Cantidad a sumar a cada componente RGB (0-255).
      * @return El nuevo color aclarado.
      */
     private java.awt.Color aclararColor(java.awt.Color color, int factor) {
         if (color == null) return java.awt.Color.LIGHT_GRAY;
         return new java.awt.Color(
             Math.min(255, color.getRed() + factor),
             Math.min(255, color.getGreen() + factor),
             Math.min(255, color.getBlue() + factor)
         );
     } // --- Fin del método aclararColor ---

     
     /**
      * Habilita o deshabilita todos los controles de zoom en la barra de estado.
      * En estado deshabilitado, muestra "N/A" y desactiva la interacción.
      * En estado habilitado, restaura la funcionalidad y actualiza al estado del modelo.
      * @param habilitados true para habilitar, false para deshabilitar.
      */
     public void setControlesDeZoomHabilitados(boolean habilitados) {
         JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
         JButton modoZoomBoton = registry.get("button.control.modoZoom");

         if (porcentajeLabel == null || modoZoomBoton == null) {
             logger.warn("WARN [setControlesDeZoomHabilitados]: No se encontraron componentes de zoom en el registro.");
             return;
         }

         if (habilitados) {
             logger.debug("[InfobarStatusManager] Habilitando controles de zoom.");
             porcentajeLabel.setEnabled(true);
             modoZoomBoton.setEnabled(true);
             
             // <<< CAMBIO: Volvemos a añadir el listener del popup
             if (zoomLabelMouseListener != null) {
                 porcentajeLabel.addMouseListener(zoomLabelMouseListener);
             }
             
             actualizarControlesDeZoom();
         } else {
             logger.debug("[InfobarStatusManager] Deshabilitando controles de zoom.");
             porcentajeLabel.setEnabled(false);
             porcentajeLabel.setText("N/A");
             porcentajeLabel.setToolTipText("El zoom no aplica en esta vista");
             
             // <<< CAMBIO: Quitamos el listener del popup
             if (zoomLabelMouseListener != null) {
                 porcentajeLabel.removeMouseListener(zoomLabelMouseListener);
                 // También reseteamos el cursor por si se quedó en "mano"
                 porcentajeLabel.setCursor(java.awt.Cursor.getDefaultCursor());
             }

             modoZoomBoton.setEnabled(false);
             modoZoomBoton.setToolTipText("El modo de zoom no aplica en esta vista");
         }
     } // --- Fin del método setControlesDeZoomHabilitados ---
     
    
     private String getIconKeyForZoomMode(ZoomModeEnum modo) {
    	    String command = modo.getAssociatedActionCommand();
    	    
    	    // Usamos un Stream para hacer la búsqueda más concisa
    	    return uiDefService.generateModularToolbarStructure().stream()
    	        // 1. Nos quedamos solo con la barra de herramientas de "zoom"
    	        .filter(toolbarDef -> "zoom".equals(toolbarDef.claveBarra()))
    	        // 2. Aplanamos la lista de componentes de esa barra en un único Stream de componentes
    	        .flatMap(toolbarDef -> toolbarDef.componentes().stream())
    	        // 3. Filtramos para quedarnos solo con los que son botones
    	        .filter(comp -> comp instanceof ToolbarButtonDefinition)
    	        // 4. Hacemos el cast a ToolbarButtonDefinition
    	        .map(comp -> (ToolbarButtonDefinition) comp)
    	        // 5. Buscamos el primer botón cuyo comando coincida con el que buscamos
    	        .filter(buttonDef -> buttonDef.comandoCanonico().equals(command))
    	        // 6. Obtenemos su clave de icono
    	        .map(ToolbarButtonDefinition::claveIcono)
    	        // 7. Si lo encontramos, lo devolvemos. Si no, devolvemos null.
    	        .findFirst()
    	        .orElse(null);
    	        
    	}// --- Fin del métodogetIconKeyForZoomMode ---
     
     
    /**
     * Lee el texto del JLabel del zoom en la barra de estado, lo parsea y
     * devuelve el valor numérico del porcentaje que está mostrando.
     *
     * @return El valor del porcentaje de zoom (ej. 150.0) o un valor por defecto
     *         (ej. 100.0) si el label no existe o el texto es inválido.
     */
    public double getValorActualDelLabelZoom() {
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
        if (porcentajeLabel == null) {
            logger.warn("WARN [getValorActualDelLabelZoom]: El componente 'label.control.zoomPorcentaje' no se encontró en el registro.");
            return 100.0;
        }

        String textoActual = porcentajeLabel.getText();
        if (textoActual == null || textoActual.isBlank()) {
            logger.warn("WARN [getValorActualDelLabelZoom]: El texto del label de zoom está vacío.");
            return 100.0;
        }

        try {
            String numeroComoTexto = textoActual.replace("Z:", "").replace("%", "").trim();
            return Double.parseDouble(numeroComoTexto);
        } catch (NumberFormatException e) {
            logger.warn("WARN [getValorActualDelLabelZoom]: No se pudo parsear el valor del label de zoom: '" + textoActual + "'. Devolviendo 100.");
            return 100.0;
        }
    }// --- FIN DEL METODO getValorActualDelLabelZoom ---
    
    
    public void setController(VisorController controller) {this.visorController = Objects.requireNonNull(controller);}
    
}// --- Fin de la clase InfobarStatusManager ---

