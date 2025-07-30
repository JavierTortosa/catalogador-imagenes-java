package controlador.managers;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import controlador.VisorController;
import controlador.commands.AppActionCommands;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import servicios.ConfigurationManager;
import servicios.ProjectManager;
import servicios.zoom.ZoomModeEnum;
import vista.config.ToolbarButtonDefinition;
import vista.config.ToolbarDefinition;
import vista.config.UIDefinitionService;
import vista.theme.Tema;
import vista.theme.ThemeManager;
import vista.util.IconUtils;

public class InfobarStatusManager {

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
        actualizarControlesDeZoom();
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
    

    private void actualizarControlesDeZoom() {
        actualizarLabelZoom();

        JButton modoZoomBoton = registry.get("button.control.modoZoom");
        if (modoZoomBoton != null) {
            boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, true);
            if (modoZoomBoton.isVisible() != esVisible) {
                modoZoomBoton.setVisible(esVisible);
            }
            if (esVisible && model.getCurrentZoomMode() != null && iconUtils != null) {
                ZoomModeEnum modoActual = model.getCurrentZoomMode();
                modoZoomBoton.setToolTipText("Modo actual: " + modoActual.getNombreLegible() + " (Clic para cambiar)");
                
                String iconKey = getIconKeyForZoomMode(modoActual);
                if (iconKey != null) {
                    modoZoomBoton.setIcon(iconUtils.getScaledIcon(iconKey, 18, 18));
                } else {
                    modoZoomBoton.setIcon(null);
                    modoZoomBoton.setText(modoActual.name().length() > 0 ? modoActual.name().substring(0, 1) : "?");
                }
            }
        }
    } // --- Fin del método actualizarControlesDeZoom ---
    
    
    private void actualizarLabelZoom() {
        ZoomModeEnum modo = model.getCurrentZoomMode();
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");

        if (porcentajeLabel == null) {
            System.err.println("WARN [InfobarStatusManager]: label.control.zoomPorcentaje no encontrado en el registro.");
            return;
        }

        boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, true);
        if (porcentajeLabel.isVisible() != esVisible) {
            porcentajeLabel.setVisible(esVisible);
        }
        
        if (esVisible) {
            // Lógica existente para el texto y el tooltip (NO SE MODIFICA)
            if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {// || modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
                double zoomFactorModelo = model.getZoomFactor();
                double porcentajeModelo = zoomFactorModelo * 100.0;
                porcentajeLabel.setText("%"+String.format("Z: %.0f%%", porcentajeModelo));
                porcentajeLabel.setToolTipText("Clic para establecer un nuevo zoom. Actual: " + String.format("%.2f%%", porcentajeModelo));
            } else {
                // Si el modo es automático, el label conserva su último valor numérico.
                // El tooltip podría indicar que no está en modo manual, pero el requisito es mantener el tooltip actual.
            }

            // Lógica para el cambio de colores (IMPLEMENTACIÓN)
            Tema tema = themeManager.getTemaActual();
            if (tema == null) {
                System.err.println("WARN [InfobarStatusManager]: Tema actual es nulo. No se pueden aplicar colores.");
                return;
            }

            boolean estaActivoVisualmente = (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM || modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE);

            if (estaActivoVisualmente) {
            	porcentajeLabel.setBackground(tema.colorBotonFondo());
                porcentajeLabel.setForeground(tema.colorTextoSecundario());
            } else {
            	porcentajeLabel.setBackground(tema.colorBotonFondoActivado());
                porcentajeLabel.setForeground(tema.colorSeleccionTexto());
            	
            	
                
            }
            porcentajeLabel.setOpaque(true);
        } else {
            porcentajeLabel.setOpaque(false);
        }
    } // --- FIN del metodo actualizarLabelZoom ---
    

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

    
    private void configurarListenersControles() {
        System.out.println("  [StatusBarManager] Configurando Listeners para controles de la barra de estado...");
        
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
        if (porcentajeLabel != null) {
            for(MouseListener l : porcentajeLabel.getMouseListeners()) porcentajeLabel.removeMouseListener(l);
            
            porcentajeLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { mostrarMenuPorcentajes(porcentajeLabel); }
                @Override public void mouseEntered(MouseEvent e) { porcentajeLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e) { porcentajeLabel.setCursor(java.awt.Cursor.getDefaultCursor()); }
            });
            System.out.println("    -> MouseListener añadido a 'label.control.zoomPorcentaje'.");
        }

        JButton modoZoomBoton = registry.get("button.control.modoZoom");
        if (modoZoomBoton != null) {
            for(ActionListener l : modoZoomBoton.getActionListeners()) modoZoomBoton.removeActionListener(l);
            
            modoZoomBoton.addActionListener(e -> mostrarMenuModosZoom(modoZoomBoton));
            System.out.println("    -> ActionListener añadido a 'button.control.modoZoom'.");
        }
    } // --- Fin del método configurarListenersControles
    
    
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
        System.out.println("[StatusBarManager] ---> Delegando al VisorController la solicitud de zoom: " + porcentaje + "%");
        
        if (this.visorController != null) {
            this.visorController.solicitarZoomPersonalizado(porcentaje);
        } else {
            System.err.println("ERROR: visorController es nulo. No se puede delegar la acción.");
        }
    } // --- FIN del metodo aplicarZoomPersonalizado ---
    
    
    private void configurarListenerParaIndicador(String componentKey, String actionCommand) {
        JLabel label = registry.get(componentKey);
        Action action = actionMap.get(actionCommand);
        if (label != null && action != null) {
            for (MouseListener listener : label.getMouseListeners()) label.removeMouseListener(listener);
            label.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { action.actionPerformed(new ActionEvent(label, ActionEvent.ACTION_PERFORMED, actionCommand)); }
                @Override public void mouseEntered(MouseEvent e) { label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e) { label.setCursor(java.awt.Cursor.getDefaultCursor()); }
            });
        }
    }// --- Fin del método configurarListenerParaIndicador

    
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

     private void actualizarUnIndicador(String key, String configKeyVisible, boolean activo, String tooltipPrefix, boolean habilitado) {
         JLabel label = registry.get(key);
         if (label != null) {
             boolean esVisible = configuration.getBoolean(configKeyVisible, true);
             if(label.isVisible() != esVisible) label.setVisible(esVisible);
             
             label.setEnabled(habilitado);

             if(esVisible) {
                 Tema tema = themeManager.getTemaActual();
                 
                 label.setOpaque(true);
                 
                 if (habilitado) {
                     label.setToolTipText(tooltipPrefix + ": " + (activo ? "Activado" : "Desactivado"));
                     
                     if (activo) {
                         Color colorOriginal = tema.colorBotonFondoActivado();
                         Color colorAclarado = aclararColor(colorOriginal, 40);
                         
                         label.setBackground(colorAclarado);
                         label.setForeground(tema.colorSeleccionTexto());
                     } else {
                         label.setBackground(tema.colorFondoSecundario());
                         label.setForeground(tema.colorTextoPrimario());
                     }

                 } else {
                     label.setToolTipText(tooltipPrefix + " (No disponible en este modo)");
                     label.setBackground(tema.colorFondoSecundario());
                     label.setForeground(tema.colorTextoSecundario());
                  }
             } else {
                  label.setOpaque(false);
             }
         }
     } // --- Fin del método actualizarUnIndicador ---
    
    
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
            System.err.println("WARN [getValorActualDelLabelZoom]: El componente 'label.control.zoomPorcentaje' no se encontró en el registro.");
            return 100.0;
        }

        String textoActual = porcentajeLabel.getText();
        if (textoActual == null || textoActual.isBlank()) {
            System.err.println("WARN [getValorActualDelLabelZoom]: El texto del label de zoom está vacío.");
            return 100.0;
        }

        try {
            String numeroComoTexto = textoActual.replace("Z:", "").replace("%", "").trim();
            return Double.parseDouble(numeroComoTexto);
        } catch (NumberFormatException e) {
            System.err.println("WARN [getValorActualDelLabelZoom]: No se pudo parsear el valor del label de zoom: '" + textoActual + "'. Devolviendo 100.");
            return 100.0;
        }
    }// --- FIN DEL METODO getValorActualDelLabelZoom ---
    
    
    public void setController(VisorController controller) {
        this.visorController = Objects.requireNonNull(controller);
    }
    
}// --- Fin de la clase InfobarStatusManager ---

