package controlador.managers;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import controlador.ListCoordinator;
import controlador.managers.interfaces.IViewManager;
import controlador.managers.interfaces.IZoomManager;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.panels.ImageDisplayPanel;

/**
 * Gestiona todas las operaciones de zoom y paneo (arrastre) de la imagen.
 * Delega los cálculos de estado al modelo y la representación a la vista.
 */
public class ZoomManager implements IZoomManager {

    // --- Dependencias ---
    private VisorModel model;
    private ComponentRegistry registry;
    private ConfigurationManager configuration;
    private InfobarStatusManager statusBarManager;
    private ListCoordinator listCoordinator;
    private IViewManager viewManager; // <<< Dependencia clave

    // --- Estado Interno ---
    private int lastMouseX, lastMouseY;
    private ImageDisplayPanel specificPanel = null;

    public ZoomManager() {
        // Constructor vacío. Las dependencias se inyectan.
    } // --- FIN del método ZoomManager (constructor) ---

    // =================================================================================
    // MÉTODOS PÚBLICOS DE LA INTERFAZ IZoomManager
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
        if (model == null || statusBarManager == null || registry == null || viewManager == null) {
            System.err.println("ERROR [aplicarModoDeZoom]: Dependencias nulas.");
            if (onComplete != null) onComplete.run();
            return;
        }

        if (model.getCurrentImage() == null) {
            ImageDisplayPanel panelActivo = getActiveDisplayPanel();
            if (panelActivo != null) panelActivo.limpiar();
            if (onComplete != null) onComplete.run();
            return;
        }
        
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        if (displayPanel == null || !displayPanel.isShowing() || displayPanel.getWidth() <= 0) {
            System.out.println("[ZoomManager] Panel no listo/visible. Reintentando en EDT...");
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
            double porcentajeDelLabel = statusBarManager.getValorActualDelLabelZoom();
            factorDeZoomParaAplicar = porcentajeDelLabel / 100.0;
        } else {
            factorDeZoomParaAplicar = _calcularFactorDeZoom(modo);
        }
        
        model.setCurrentZoomMode(modo);
        model.setZoomFactor(factorDeZoomParaAplicar);
        model.resetPan();
        refrescarVistaSincrono();

        if (onComplete != null) onComplete.run();
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
        System.err.println("ERROR [ZoomManager.getActiveDisplayPanel]: ViewManager es nulo.");
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
            case MAINTAIN_CURRENT_ZOOM: return model.getZoomFactor(); 
            case USER_SPECIFIED_PERCENTAGE: return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
            case FIT_TO_SCREEN: return Math.min((double) panelW / imgW, (double) panelH / imgH);
            case FILL: return Math.max((double) panelW / imgW, (double) panelH / imgH);
            case FIT_TO_WIDTH: return (double) panelW / imgW;
            case FIT_TO_HEIGHT: return (double) panelH / imgH;
            case DISPLAY_ORIGINAL: return 1.0;
            default: return model.getZoomFactor(); 
        }
    } // --- Fin del método _calcularFactorDeZoom ---
    
    // --- SETTERS PARA INYECCIÓN DE DEPENDENCIAS ---

    @Override
    public void setStatusBarManager(InfobarStatusManager manager) { this.statusBarManager = manager; }
    @Override
    public void setListCoordinator(ListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model, "VisorModel no puede ser null"); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null"); }
    public void setConfiguration(ConfigurationManager configuration) { this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null"); }
    
    @Override
    public void setViewManager(IViewManager viewManager) { 
        this.viewManager = Objects.requireNonNull(viewManager, "IViewManager no puede ser null en ZoomManager"); 
    } // --- Fin del método setViewManager ---
    
} // --- FIN de la clase ZoomManager ---

