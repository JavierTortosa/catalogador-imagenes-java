package controlador.managers;

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
            IconUtils iconUtils) // <--- DEPENDENCIA AÑADIDA
    {
        this.model = Objects.requireNonNull(model);
        this.registry = Objects.requireNonNull(registry);
        this.themeManager = Objects.requireNonNull(themeManager);
        this.configuration = Objects.requireNonNull(configuration);
        this.projectService = Objects.requireNonNull(projectService);
        this.actionMap = Objects.requireNonNull(actionMap);
        this.iconUtils = Objects.requireNonNull(iconUtils); // <--- DEPENDENCIA AÑADIDA
        this.uiDefService = new UIDefinitionService();
        
        configurarListenersControles();
    }

    public void actualizar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::actualizarBarraEstado);
        } else {
            actualizarBarraEstado();
        }
    }

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
    }
    
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
    }

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
    }
    
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
    }

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
    }

    private void actualizarIndicadoresDeEstado() {
        actualizarUnIndicador("label.indicador.zoomManual", ConfigKeys.INFOBAR_INF_ICONO_ZM_VISIBLE, model.isZoomHabilitado(), "Zoom Manual");
        actualizarUnIndicador("label.indicador.proporciones", ConfigKeys.INFOBAR_INF_ICONO_PROP_VISIBLE, model.isMantenerProporcion(), "Mantener Proporciones");
        actualizarUnIndicador("label.indicador.subcarpetas", ConfigKeys.INFOBAR_INF_ICONO_SUBC_VISIBLE, !model.isMostrarSoloCarpetaActual(), "Incluir Subcarpetas");
    }

    private void actualizarControlesDeZoom() {
        // Actualizar el JLabel del Porcentaje
        JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
        if (porcentajeLabel != null) {
            boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_ZOOM_PCT_VISIBLE, true);
            if(porcentajeLabel.isVisible() != esVisible) porcentajeLabel.setVisible(esVisible);
            if(esVisible) {
                double pConfig = configuration.getDouble(ConfigKeys.COMPORTAMIENTO_ZOOM_PORCENTAJE_PERSONALIZADO, 100.0);
                porcentajeLabel.setText(String.format("Z: %.0f%%", pConfig));
                porcentajeLabel.setToolTipText("Clic para configurar el 'Zoom Personalizado'");
            }
        }

        // Actualizar el JButton del Modo de Zoom
        JButton modoZoomBoton = registry.get("button.control.modoZoom");
        if (modoZoomBoton != null) {
            boolean esVisible = configuration.getBoolean(ConfigKeys.INFOBAR_INF_CTRL_MODO_ZOOM_VISIBLE, true);
            if(modoZoomBoton.isVisible() != esVisible) modoZoomBoton.setVisible(esVisible);
            if(esVisible && model.getCurrentZoomMode() != null && iconUtils != null) {
                ZoomModeEnum modoActual = model.getCurrentZoomMode();
                modoZoomBoton.setToolTipText("Modo actual: " + modoActual.getNombreLegible() + " (Clic para cambiar)");
                
                String iconKey = getIconKeyForZoomMode(modoActual);
                if (iconKey != null) {
                    modoZoomBoton.setIcon(iconUtils.getScaledIcon(iconKey, 18, 18));
                } else {
                    modoZoomBoton.setIcon(null);
                    modoZoomBoton.setText(modoActual.name().substring(0, 1)); // Pone la inicial como fallback
                }
            }
        }
    }

    private void actualizarMensajeDeEstado() {
        JLabel mensajesLabel = registry.get("label.estado.mensajes");
        if (mensajesLabel == null) {
            return;
        }

        // 1. Obtener la ruta de la imagen actual desde el modelo.
        Path rutaActual = null;
        if (model != null && model.getSelectedImageKey() != null) {
            rutaActual = model.getRutaCompleta(model.getSelectedImageKey());
        }

        // 2. Comprobar si la imagen está marcada usando el ProjectManager.
        boolean estaMarcada = false;
        if (rutaActual != null && projectService != null) {
            estaMarcada = projectService.estaMarcada(rutaActual);
        }

        // 3. Actualizar el texto y el color del JLabel.
        if (estaMarcada) {
            mensajesLabel.setText("[MARCADA]");
            // Usar un color que destaque, por ejemplo el de un botón activado
            if (themeManager != null) {
                mensajesLabel.setForeground(themeManager.getTemaActual().colorBotonFondoActivado()); 
            }
        } else {
            // Si no está marcada, limpiamos el mensaje.
            mensajesLabel.setText(" "); // Un espacio para mantener la altura del panel.
        }
    } // --- Fin del método actualizarMensajeDeEstado ---

    private void configurarListenersControles() {
        System.out.println("  [StatusBarManager] Configurando Listeners para controles de la barra de estado...");
        configurarListenerParaIndicador("label.indicador.zoomManual", AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        configurarListenerParaIndicador("label.indicador.proporciones", AppActionCommands.CMD_TOGGLE_MANTENER_PROPORCIONES);
        configurarListenerParaIndicador("label.indicador.subcarpetas", AppActionCommands.CMD_TOGGLE_SUBCARPETAS);

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
    }

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
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(invoker, "Porcentaje inválido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(otrosItem);
        menu.show(invoker, 0, -invoker.getHeight());
    }
    
    private void mostrarMenuModosZoom(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        ZoomModeEnum[] modosParaMenu = {
            ZoomModeEnum.FIT_TO_SCREEN, ZoomModeEnum.FIT_TO_WIDTH, ZoomModeEnum.FIT_TO_HEIGHT,
            ZoomModeEnum.DISPLAY_ORIGINAL, ZoomModeEnum.FILL,
            ZoomModeEnum.MAINTAIN_CURRENT_ZOOM, ZoomModeEnum.USER_SPECIFIED_PERCENTAGE
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
    }
    
    private void aplicarZoomPersonalizado(double porcentaje) {
        configuration.setZoomPersonalizadoPorcentaje(porcentaje);
        Action accionZoomEspec = actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
        if (accionZoomEspec != null) {
            accionZoomEspec.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
        }
    }

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
    }
    
    private void actualizarUnIndicador(String key, String configKeyVisible, boolean activo, String tooltipPrefix) {
        JLabel label = registry.get(key);
        if (label != null) {
            boolean esVisible = configuration.getBoolean(configKeyVisible, true);
            if(label.isVisible() != esVisible) label.setVisible(esVisible);
            if(esVisible) {
                Tema tema = themeManager.getTemaActual();
                label.setToolTipText(tooltipPrefix + ": " + (activo ? "Activado" : "Desactivado"));
                label.setBackground(activo ? tema.colorBotonFondoActivado() : tema.colorFondoSecundario());
            }
        }
    }

    private String getIconKeyForZoomMode(ZoomModeEnum modo) {
        String command = modo.getAssociatedActionCommand();
        // Busca en la definición de la barra de "zoom" el botón que corresponde a este comando
        for (ToolbarDefinition def : uiDefService.generateModularToolbarStructure()) {
            if ("zoom".equals(def.claveBarra())) {
                for (ToolbarButtonDefinition buttonDef : def.botones()) {
                    if (buttonDef.comandoCanonico().equals(command)) {
                        return buttonDef.claveIcono();
                    }
                }
            }
        }
        return null; // No se encontró el icono
    }
}