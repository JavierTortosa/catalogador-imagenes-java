package controlador.managers;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.ListCoordinator;
import controlador.VisorController;
import controlador.actions.zoom.AplicarModoZoomAction;
import controlador.commands.AppActionCommands;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.VisorView;
import vista.panels.ImageDisplayPanel;


/**
 * Gestiona todas las operaciones de zoom y paneo (arrastre) de la imagen.
 * Delega los cálculos de estado al modelo y la representación a la vista.
 */
public class ZoomManager implements IZoomManager {

	private static final Logger logger = LoggerFactory.getLogger(ZoomManager.class);
	
    // --- Dependencias ---
    private VisorModel model;
    private ComponentRegistry registry;
    private ConfigurationManager configuration;
    private InfobarStatusManager statusBarManager;
    private ListCoordinator listCoordinator;
    private IViewManager viewManager; // <<< Dependencia clave
    private VisorController visorController;
    

    // --- Estado Interno ---
    private int lastMouseX, lastMouseY;
    private VisorView view;
    private Map<String, Action> actionMap;
    private ConfigApplicationManager configAppManager;
    private ImageDisplayPanel specificPanel = null;
    private InfobarImageManager infobarImageManager;
    
    
    public ZoomManager() {
        // Constructor vacío. Las dependencias se inyectan.
    } // --- FIN del método ZoomManager (constructor) ---

// =================================================================================
// 										MÉTODOS PÚBLICOS DE LA INTERFAZ IZoomManager
// =================================================================================
    
    @Override
    public void setPermisoManual(boolean activar) {
        if (model == null) return;
        if (model.isZoomHabilitado() == activar) return;
        
        model.setZoomHabilitado(activar);
        
        if (statusBarManager != null) statusBarManager.actualizar();
    } // --- Fin del método setPermisoManual ---
    
    @Override
    public void aplicarModoDeZoom(ZoomModeEnum modo) {
        aplicarModoDeZoom(modo, null);
    } // --- Fin del método aplicarModoDeZoom (sin callback) ---    
    
    @Override
    public void aplicarModoDeZoom(ZoomModeEnum modo, Runnable onComplete) {
    	
        if (model == null) {
            logger.error("ERROR [ZoomManager]: El modelo es nulo.");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Obtenemos el DisplayMode actual del modelo.
        VisorModel.DisplayMode displayModeActual = model.getCurrentDisplayMode();

        // Comprobamos si el modo actual es uno que NO es compatible con el zoom de imagen única.
        // Por ahora, el único modo incompatible es GRID.
        if (displayModeActual == VisorModel.DisplayMode.GRID) {
            logger.debug("[ZoomManager] La operación de zoom de imagen principal no es aplicable en modo GRID. Operación omitida.");
            
            // Actualizamos la UI para que el usuario entienda que el zoom no aplica aquí.
            if (statusBarManager != null) {
            	statusBarManager.actualizarParaModoNoCompatible();
            }
            
            if (onComplete != null) onComplete.run();
            return; // Salimos y evitamos el bucle.
        }

        // Si llegamos aquí, significa que estamos en un modo compatible (como SINGLE_IMAGE),
        // sin importar si el WorkMode es VISUALIZADOR, EDICION o CARROUSEL.
        
        if (model == null || statusBarManager == null || registry == null || viewManager == null) {
            logger.error("ERROR [aplicarModoDeZoom]: Dependencias nulas.");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // La comprobación ahora solo falla si las dependencias ESENCIALES son nulas.
        // statusBarManager es opcional.
        if (registry == null || viewManager == null) {
            logger.error("ERROR [aplicarModoDeZoom]: Dependencias nulas (registry o viewManager).");
            if (onComplete != null) onComplete.run();
            return;
        }

        if (model.getCurrentImage() == null) {
            ImageDisplayPanel panelActivo = getActiveDisplayPanel();
            if (panelActivo != null) panelActivo.limpiar();
            
            if (onComplete != null) SwingUtilities.invokeLater(onComplete);
            
            return;
        }
        
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        if (displayPanel == null || !displayPanel.isShowing() || displayPanel.getWidth() <= 0) {
            logger.debug("[ZoomManager] Panel no listo/visible. Reintentando en EDT...");
            SwingUtilities.invokeLater(() -> aplicarModoDeZoom(modo, onComplete));
            return;
        }

        double factorDeZoomParaAplicar;
        
        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            factorDeZoomParaAplicar = model.getZoomFactor();
            JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
            if (porcentajeLabel != null) {
                double porcentaje = factorDeZoomParaAplicar * 100.0;
                porcentajeLabel.setText(String.format("Z: %.0f%%", porcentaje));
            }
        } else if (modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
        	
            if (statusBarManager != null) {
                double porcentajeDelLabel = statusBarManager.getValorActualDelLabelZoom();
                factorDeZoomParaAplicar = porcentajeDelLabel / 100.0;
            } else {
                factorDeZoomParaAplicar = model.getZoomCustomPercentage() / 100.0;
            }
            
        } else {
            factorDeZoomParaAplicar = _calcularFactorDeZoom(modo);
        }
        
        model.setCurrentZoomMode(modo);
        model.setZoomFactor(factorDeZoomParaAplicar);
        model.resetPan();
        refrescarVistaSincrono();

        if (onComplete != null) {
            SwingUtilities.invokeLater(onComplete);
        }
        
    } // --- Fin del método aplicarModoDeZoom (con callback) ---
    
    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
    	
        if (model == null || !model.isZoomHabilitado() || model.getCurrentImage() == null) return;
        
        double zoomActual = model.getZoomFactor();
        double nuevoZoom = (e.getWheelRotation() < 0) ? zoomActual * 1.1 : zoomActual / 1.1;
        
        ImageDisplayPanel panel = getActiveDisplayPanel();
        
        if (panel == null) return;
        
        java.awt.Point puntoRatonEnPanel = e.getPoint();
        
        if (model.isZoomToCursorEnabled()) {
        	
            double panelW = panel.getWidth(), panelH = panel.getHeight();
            double imgW = model.getCurrentImage().getWidth(), imgH = model.getCurrentImage().getHeight();
            double offsetXActual = model.getImageOffsetX(), offsetYActual = model.getImageOffsetY();
            double xBaseActual = (panelW - imgW * zoomActual) / 2.0, yBaseActual = (panelH - imgH * zoomActual) / 2.0;
            double ratonRelativoAIngenX = puntoRatonEnPanel.x - (xBaseActual + offsetXActual);
            double ratonRelativoAIngenY = puntoRatonEnPanel.y - (yBaseActual + offsetYActual);
            double ratioX = ratonRelativoAIngenX / (imgW * zoomActual), ratioY = ratonRelativoAIngenY / (imgH * zoomActual);
            double xBaseNuevo = (panelW - imgW * nuevoZoom) / 2.0, yBaseNuevo = (panelH - imgH * nuevoZoom) / 2.0;
            double nuevoOffsetX = puntoRatonEnPanel.x - xBaseNuevo - (ratioX * imgW * nuevoZoom);
            double nuevoOffsetY = puntoRatonEnPanel.y - yBaseNuevo - (ratioY * imgH * nuevoZoom);
            model.setImageOffsetX((int) nuevoOffsetX);
            model.setImageOffsetY((int) nuevoOffsetY);
            
        }
        
        model.setZoomFactor(nuevoZoom);
        refrescarVistaSincrono();
        
        if (model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            model.setZoomCustomPercentage(nuevoZoom * 100.0);
        }
        
        if (model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM || model.getCurrentZoomMode() == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
            if (statusBarManager != null) statusBarManager.actualizar();
        }
        
    } // --- Fin del método aplicarZoomConRueda ---
    
    @Override
    public void iniciarPaneo(MouseEvent e) {
        if (model != null && SwingUtilities.isLeftMouseButton(e) && model.isZoomHabilitado()) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    } // --- Fin del método iniciarPaneo ---
    
    @Override
    public void continuarPaneo(MouseEvent e) {
        if (model != null && SwingUtilities.isLeftMouseButton(e) && model.isZoomHabilitado()) {
            int deltaX = e.getX() - lastMouseX;
            int deltaY = e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            aplicarPan(deltaX, deltaY);
        }
    } // --- Fin del método continuarPaneo ---
    
    @Override
    public void aplicarPan(int deltaX, int deltaY) {
        if (model != null && model.isZoomHabilitado()) {
            model.addImageOffsetX(deltaX);
            model.addImageOffsetY(deltaY);
            refrescarVistaSincrono();
        }
    } // --- Fin del método aplicarPan ---
    
    @Override
    public void refrescarVistaSincrono() {
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        if (displayPanel != null) {
            displayPanel.repaint();
        }
    } // --- Fin del método refrescarVistaSincrono ---
    
    @Override
    public void manejarRuedaInteracciona(java.awt.event.MouseWheelEvent e) {
        if (model == null || !model.isZoomHabilitado()) return;
        if (e.isControlDown() && e.isShiftDown()) aplicarZoomConRueda(e);
        else if (e.isShiftDown()) aplicarPan(-e.getWheelRotation() * 30, 0);
        else if (e.isControlDown()) aplicarPan(0, -e.getWheelRotation() * 30);
    } // --- Fin del método manejarRuedaInteracciona ---
    
    
    @Override // Asegúrate de añadir esta @Override a la interfaz IZoomManager también
    public void resetZoom() {
        logger.debug("[ZoomManager] Solicitud para resetear zoom al modo actual.");
        if (model == null) {
            logger.error("ERROR [resetZoom]: El modelo es nulo.");
            return;
        }

        // 1. NO cambiamos el modo actual. Leemos el que ya está en el modelo.
        ZoomModeEnum modoActual = model.getCurrentZoomMode();
        logger.debug("  -> Reseteando para el modo: " + modoActual);

        // 2. Llamamos al método que ya tienes para que recalcule el factor de zoom
        //    y resetee el paneo, pero para el MODO ACTUAL.
        //    El tercer parámetro (el callback) es para sincronizar la UI después.
        aplicarModoDeZoom(modoActual, () -> {
            if (visorController != null) {
                logger.debug("  -> [Callback de Reset] Sincronizando UI de botones de zoom...");
                sincronizarEstadoVisualBotonesYRadiosZoom();
            }
        });
    } // FIN del metodo resetZoom 
    
    
    public void setSpecificPanel(ImageDisplayPanel panel) {
        this.specificPanel = panel;
    } // --- Fin del método setSpecificPanel ---
    
    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Obtiene el panel de visualización de imagen activo.
     * DELEGA la lógica al ViewManager para centralizar la selección del panel.
     * @return El ImageDisplayPanel correcto o null si no se encuentra.
     */
    private ImageDisplayPanel getActiveDisplayPanel() {
        if (specificPanel != null) return specificPanel;
        if (viewManager != null) return viewManager.getActiveDisplayPanel();
        logger.error("ERROR [ZoomManager.getActiveDisplayPanel]: ViewManager es nulo.");
        return null;
    } // --- Fin del método getActiveDisplayPanel ---
    
    
    private double _calcularFactorDeZoom(ZoomModeEnum modo) {
        if (modo == null) return 1.0;
        BufferedImage img = model.getCurrentImage();
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        if (img == null || displayPanel == null) return 1.0;
        int imgW = img.getWidth(), imgH = img.getHeight();
        int panelW = displayPanel.getWidth(), panelH = displayPanel.getHeight();
        if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) return model.getZoomFactor();
        switch (modo) {
            case SMART_FIT:
                double imgRatio = (double) imgW / imgH, panelRatio = (double) panelW / panelH;
                return (imgRatio > panelRatio) ? (double) panelW / imgW : (double) panelH / imgH;
            case FIT_TO_SCREEN: return Math.min((double) panelW / imgW, (double) panelH / imgH);
            case FILL: return Math.max((double) panelW / imgW, (double) panelH / imgH);
            case FIT_TO_WIDTH: return (double) panelW / imgW;
            case FIT_TO_HEIGHT: return (double) panelH / imgH;
            case DISPLAY_ORIGINAL: return 1.0;
            case MAINTAIN_CURRENT_ZOOM: return model.getZoomFactor();
            case USER_SPECIFIED_PERCENTAGE: return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
            
            default: return model.getZoomFactor(); 
        }
    } // --- Fin del método _calcularFactorDeZoom ---

    
// ***************************************************************************************************************************
// 			    								LÓGICA DE SINCRONIZACIÓN DE LA UI DE ZOOM.
// ***************************************************************************************************************************
    
    
    /**
     * Sincroniza explícitamente el estado visual y lógico de TODOS los botones y radios 
     * de la UI que controlan el zoom (modos, paneo manual, zoom al cursor, reset),
     * basándose en el estado actual del VisorModel.
     * Este es el método maestro para mantener la UI de zoom coherente.
     */
    public void sincronizarEstadoVisualBotonesYRadiosZoom() {
        // 1. Validar que las dependencias críticas no sean nulas.
        if (this.actionMap == null || this.model == null || this.configAppManager == null) {
            logger.warn("WARN [sincronizarEstadoVisualBotonesYRadiosZoom]: Dependencias críticas (actionMap, model, configAppManager) nulas. Abortando sincronización.");
            return;
        }
        
        // 2. Leer el estado "de verdad" desde el modelo una sola vez.
        final ZoomModeEnum modoActivoDelModelo = model.getCurrentZoomMode();
        final boolean permisoManualActivoDelModelo = model.isZoomHabilitado();
        final boolean zoomAlCursorActivoDelModelo = model.isZoomToCursorEnabled();

        logger.debug("[VisorController] Sincronizando UI de Zoom: Paneo=" + permisoManualActivoDelModelo + ", Modo=" + modoActivoDelModelo + ", ZoomAlCursor=" + zoomAlCursorActivoDelModelo);

        // --- 3. SINCRONIZAR EL BOTÓN DE PANEO MANUAL (ToggleZoomManualAction) ---
        Action zoomManualAction = actionMap.get(AppActionCommands.CMD_ZOOM_MANUAL_TOGGLE);
        if (zoomManualAction != null) {
            // a) Sincronizar el estado lógico (SELECTED_KEY) de la Action.
            zoomManualAction.putValue(Action.SELECTED_KEY, permisoManualActivoDelModelo);
            
            // b) Llamar al ConfigApplicationManager para que aplique el aspecto visual correcto al botón.
            configAppManager.actualizarAspectoBotonToggle(zoomManualAction, permisoManualActivoDelModelo);
        }

        // --- 4. SINCRONIZAR LOS BOTONES DE MODO DE ZOOM (AplicarModoZoomAction) ---
        // Se itera sobre todas las acciones y se filtran las que son de tipo AplicarModoZoomAction.
        for (Action action : actionMap.values()) {
            if (action instanceof controlador.actions.zoom.AplicarModoZoomAction) {
                AplicarModoZoomAction zoomModeAction = (AplicarModoZoomAction) action;
                
                // a) Determinar si esta acción representa el modo actualmente activo en el modelo.
                boolean estaAccionDebeEstarSeleccionada = (zoomModeAction.getModoAsociado() == modoActivoDelModelo);
                
                // b) Sincronizar el estado lógico (SELECTED_KEY) de la Action.
                zoomModeAction.putValue(Action.SELECTED_KEY, estaAccionDebeEstarSeleccionada);
                
                // c) Llamar al ConfigApplicationManager para que aplique el aspecto visual.
                configAppManager.actualizarAspectoBotonToggle(zoomModeAction, estaAccionDebeEstarSeleccionada);
            }
        }
        
        // --- 5. SINCRONIZAR EL BOTÓN DE ZOOM AL CURSOR (ToggleZoomToCursorAction) ---
        Action zoomCursorAction = actionMap.get(AppActionCommands.CMD_ZOOM_TOGGLE_TO_CURSOR);
        if (zoomCursorAction != null) {
            // a) Sincronizar el estado lógico de la Action.
            zoomCursorAction.putValue(Action.SELECTED_KEY, zoomAlCursorActivoDelModelo);

            // b) Llamar al ConfigApplicationManager. Aunque este botón no esté en la toolbar principal,
            //    el método encontrará el componente asociado (en el menú, por ejemplo) y lo actualizará si es un JCheckBoxMenuItem.
            //    Si en el futuro lo pones como un JToggleButton en otro sitio, esto ya funcionará.
            configAppManager.actualizarAspectoBotonToggle(zoomCursorAction, zoomAlCursorActivoDelModelo);
        }

        // --- 6. SINCRONIZAR EL BOTÓN DE RESET (ResetZoomAction) ---
        // Este no es un botón de tipo "toggle", solo se habilita o deshabilita.
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetAction != null) {
            // Su estado 'enabled' depende de si el paneo manual está activo.
            resetAction.setEnabled(permisoManualActivoDelModelo);
        }
        
        // --- 7. ACTUALIZAR LAS BARRAS DE INFORMACIÓN (Opcional, pero buena práctica) ---
        // Esto asegura que cualquier texto que muestre el modo de zoom se actualice.
        if (this.infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
        
        logger.debug("[VisorController] Sincronización completa de la UI de Zoom finalizada.");

    } // --- FIN del método sincronizarEstadoVisualBotonesYRadiosZoom ---
    
    
    /**
     * Sincroniza ÚNICAMENTE los componentes que dependen del estado de paneo,
     * como el botón de Reset.
     */
    public void sincronizarEstadoBotonReset() {
        if (actionMap == null || model == null) {
            return;
        }
        
        boolean permisoManualActivo = model.isZoomHabilitado();
        
        Action resetAction = actionMap.get(AppActionCommands.CMD_ZOOM_RESET);
        if (resetAction != null) {
            // Habilita o deshabilita la Action de Reset basándose en si el paneo está activo.
            resetAction.setEnabled(permisoManualActivo);
            logger.debug("[VisorController] Botón Reset " + (permisoManualActivo ? "habilitado." : "deshabilitado."));
        }
    }// --- FIN del metodo sincronizarEstadoBotonReset ---
    
    
    /**
     * Gestiona la lógica cuando el usuario establece un nuevo zoom desde la UI
     * (barra de estado o menú). Activa el modo correcto y aplica el zoom.
     * @param nuevoPorcentaje El nuevo porcentaje de zoom a aplicar.
     */
    public void solicitarZoomPersonalizado(double nuevoPorcentaje) {
        logger.debug("--- [VisorController] INICIO solicitarZoomPersonalizado (Lógica Centralizada): " + nuevoPorcentaje + "% ---");
        if (model == null || configuration == null || actionMap == null) {
            logger.error("  -> ERROR: Dependencias nulas. Abortando.");
            return;
        }

        // --- INICIO DE LA MODIFICACIÓN ---

        // 1. LÓGICA DE NEGOCIO: ACTUALIZAR EL MODELO Y LA CONFIGURACIÓN
        //    Guardamos el "deseo" del usuario en nuestro nuevo campo del modelo.
        model.setZoomCustomPercentage(nuevoPorcentaje);
        
        //    También actualizamos la configuración para que el cambio se guarde al cerrar.
        configuration.setZoomPersonalizadoPorcentaje(nuevoPorcentaje);
        logger.debug("  -> Modelo y Configuración actualizados con el nuevo porcentaje: " + nuevoPorcentaje + "%");

        // 2. ACTIVAR EL MODO DE ZOOM FIJADO
        //    Buscamos la Action correspondiente al modo "Bloqueador" y la ejecutamos.
        //    Esto asegura que se aplique toda la lógica definida en AplicarModoZoomAction,
        //    incluida la sincronización de la UI.
        Action zoomFijadoAction = actionMap.get(AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO);
        if (zoomFijadoAction != null) {
            logger.debug("  -> Disparando Action para aplicar el modo USER_SPECIFIED_PERCENTAGE...");
            // Creamos un ActionEvent simple para disparar la acción.
            zoomFijadoAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, AppActionCommands.CMD_ZOOM_TIPO_ESPECIFICADO));
        } else {
            logger.error("  -> ERROR: No se encontró la Action para CMD_ZOOM_TIPO_ESPECIFICADO.");
        }

        // --- FIN DE LA MODIFICACIÓN ---
        
        // (El código antiguo que tenías aquí ya no es necesario porque la Action se encarga de todo)
        
        logger.debug("--- [VisorController] FIN solicitarZoomPersonalizado ---\n");
        
    } // --- FIN del metodo solicitarZoomPersonalizado ---
    
    
    public void notificarCambioEstadoZoomManual() {
        logger.debug("[VisorController] Notificado cambio de estado de zoom manual. Actualizando barras...");
        
     // << --- ACTUALIZAR BARRAS AL FINAL DE LA LIMPIEZA --- >>  
        if (this.infobarImageManager != null) {
            infobarImageManager.actualizar();
        }
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }
    } // FIN del metodo notificarCambioEstadoZoomManual
    
    
    /**
     * REFACTORIZADO: Configura un listener que se dispara UNA SOLA VEZ, cuando la
     * ventana principal es mostrada y tiene dimensiones válidas por primera vez.
     * Su único propósito es corregir el zoom inicial.
     */
    public void configurarListenerRedimensionVentana() {
        if (view == null) {
            logger.error("ERROR [Controller - configurarListenerRedimensionamiento]: Vista nula.");
            return;
        }
        
        logger.debug("    [Controller] Configurando ComponentListener para el primer arranque...");

        view.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
                
                // Solo actuar si el panel tiene un tamaño válido y hay una imagen cargada.
                if (displayPanel != null && displayPanel.getWidth() > 0 && model != null && model.getCurrentImage() != null) {
                    
                    logger.debug("--- [Listener de Ventana] Primer redimensionado válido detectado. Re-aplicando modo de zoom inicial. ---");
                    
                         aplicarModoDeZoom(model.getCurrentZoomMode());
                    
                    // ¡Importante! Eliminar el listener después de que se haya ejecutado una vez.
                    view.removeComponentListener(this);
                    logger.debug("--- [Listener de Ventana] Tarea completada. Listener eliminado. ---");
                }
            }
        });
    } // --- FIN del metodo configurarListenerRedimensionVentana ---
    
    
    private void sincronizarUiControlesZoom(Action action, boolean isSelected) {
        if (configAppManager != null) {
            // Delega la actualización visual al manager correspondiente.
            configAppManager.actualizarEstadoControlesZoom(isSelected, isSelected);
            configAppManager.actualizarAspectoBotonToggle(action, isSelected);
        } else {
            logger.warn("WARN [sincronizarUiControlesZoom]: configAppManager es nulo.");
        }
    }// --- FIN del metodo sincronizarUiControlesZoom ---
    
    
    
// ***************************************************************************************************************************
//											FIN DE LA LÓGICA DE SINCRONIZACIÓN DE LA UI DE ZOOM.
//***************************************************************************************************************************
    
// ***************************************************************************************************************************
//															GETTERS Y SETTERS
//***************************************************************************************************************************
    
    @Override
    public void setZoomMode(ZoomModeEnum nuevoModo, Runnable onComplete) {
        logger.debug("[ZoomManager] Solicitud para establecer modo (vía setZoomMode): " + nuevoModo);

        // Guarda de seguridad idéntica a la que tenías en la Action
        if (model.getCurrentZoomMode() == nuevoModo && nuevoModo != ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
            logger.debug("  -> El modo ya está activo. No se hace nada.");
            if (onComplete != null) {
                onComplete.run(); // Ejecutamos el callback por si acaso la UI necesita sincronizarse
            }
            return;
        }

        // --- LÓGICA DEL SWITCH MOVIDA DESDE LA ACTION ---
        switch (nuevoModo) {
            case MAINTAIN_CURRENT_ZOOM:
                double currentZoomFactor = model.getZoomFactor();
                model.setZoomCustomPercentage(currentZoomFactor * 100.0);
                
                // LLAMAMOS A TU MÉTODO EXISTENTE, SIN CAMBIOS
                aplicarModoDeZoom(nuevoModo, onComplete);
                break;

            case USER_SPECIFIED_PERCENTAGE:
                model.setCurrentZoomMode(nuevoModo);
                double customPercentage = model.getZoomCustomPercentage();
                model.setZoomFactor(customPercentage / 100.0);
                
                refrescarVistaSincrono();
                if (onComplete != null) {
                    onComplete.run();
                }
                break;

            default: // Para todos los modos automáticos
                // LLAMAMOS A TU MÉTODO EXISTENTE, SIN CAMBIOS
                aplicarModoDeZoom(nuevoModo, onComplete);
                break;
        }
    } //--- FIN del metodo setZoomMode ---
    
    @Override
    public void setStatusBarManager(InfobarStatusManager manager) { this.statusBarManager = manager; }
    @Override
    public void setListCoordinator(ListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model, "VisorModel no puede ser null"); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null"); }
    public void setConfiguration(ConfigurationManager configuration) { this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null"); }
    public void setVisorController(VisorController visorController) {this.visorController = visorController;}
    
    public void setView(VisorView view) { this.view = view; }
    public void setActionMap(Map<String, Action> actionMap) { this.actionMap = actionMap; }
    public void setConfigApplicationManager(ConfigApplicationManager configAppManager) { this.configAppManager = configAppManager; }
    public void setInfobarImageManager(InfobarImageManager infobarImageManager) {this.infobarImageManager = infobarImageManager;}
    
    @Override
    public void setViewManager(IViewManager viewManager) { 
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null en ZoomManager"); 
    } // --- Fin del método setViewManager ---
    
// ***************************************************************************************************************************
//													FIN DE GETTERS Y SETTERS
//***************************************************************************************************************************
    
    
} // --- FIN de la clase ZoomManager ---

