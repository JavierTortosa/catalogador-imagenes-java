package controlador.managers;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.SwingUtilities;

import controlador.ListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigurationManager;
import servicios.zoom.ZoomModeEnum;
import vista.panels.ImageDisplayPanel;
import vista.util.ImageDisplayUtils;

/**
 * Gestiona todas las operaciones de zoom y paneo (arrastre) de la imagen.
 * (Versión con independencia total entre Modos de Zoom y Modo Paneo).
 */
public class ZoomManager {

    private VisorModel model;
    private final ComponentRegistry registry;
    private final ConfigurationManager configuration;
    private InfobarStatusManager statusBarManager;
    private int lastMouseX, lastMouseY;
    private ListCoordinator listCoordinator;

    public ZoomManager(VisorModel model, ComponentRegistry registry, ConfigurationManager configuration) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null");
    }
    // --- FIN del constructor de ZoomManager ---

    // =================================================================================
    // MÉTODOS PÚBLICOS
    // =================================================================================
    
    /**
     * Activa o desactiva el "Modo Paneo". Es un interruptor independiente.
     * Su única función es cambiar el estado en el modelo y notificar a la UI.
     */
    public void setPermisoManual(boolean activar) {
        if (model.isZoomHabilitado() == activar) return;
        
        model.setZoomHabilitado(activar);
        
        // Simplemente notificamos a las barras de estado para que actualicen sus indicadores.
        if (statusBarManager != null) statusBarManager.actualizar();
    }
    // --- FIN del metodo setPermisoManual ---
    
    /**
     * Aplica un modo de zoom. Esta acción calcula y establece un nuevo factor de zoom,
     * pero NO afecta al estado del Modo Paneo.
     * Contiene la lógica especial para "capturar" el zoom en el modo MAINTAIN_CURRENT_ZOOM.
     */
    public void aplicarModoDeZoom(ZoomModeEnum modo) {
        if (model == null || model.getCurrentImage() == null) return;
        
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null || displayPanel.getWidth() <= 0) return;

        // --- INICIO DE LA LÓGICA CORREGIDA ---

        // Lógica de "captura" para el modo Zoom Fijo.
        // Se ejecuta ANTES de cambiar el modo en el modelo.
        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            double factorActual = model.getZoomFactor();
            configuration.setZoomPersonalizadoPorcentaje(factorActual * 100);
            System.out.println("[ZoomManager] Capturado nuevo Zoom Fijo: " + (factorActual * 100) + "%");
        }

        // --- FIN DE LA LÓGICA CORREGIDA ---

        model.setCurrentZoomMode(modo);

        if (modo == ZoomModeEnum.FILL) {
            BufferedImage imgOriginal = model.getCurrentImage();
            Image imagenEscalada = ImageDisplayUtils.escalar(imgOriginal, displayPanel.getWidth(), displayPanel.getHeight());
            double factorPromedio = ((double) displayPanel.getWidth() / imgOriginal.getWidth() + (double) displayPanel.getHeight() / imgOriginal.getHeight()) / 2.0;
            model.setZoomFactor(factorPromedio);
            model.resetPan();
            displayPanel.setImagenEscalada(imagenEscalada, model.getImageOffsetX(), model.getImageOffsetY());
            if (statusBarManager != null) statusBarManager.actualizar();
            return; 
        }

        double nuevoFactor = _calcularFactorDeZoom(modo);
        model.setZoomFactor(nuevoFactor);
        model.resetPan();
        
        refrescarVistaSincrono();
    }
    // --- FIN del metodo aplicarModoDeZoom ---
    
    /**
     * Maneja la interacción de la rueda del ratón cuando el modo paneo está activo.
     * REFACTORIZADO para usar Ctrl+Shift para Zoom y Ctrl/Shift para Paneo.
     *
     * @param e El evento de la rueda del ratón.
     */
    public void manejarRuedaInteracciona(java.awt.event.MouseWheelEvent e) {
        if (!model.isZoomHabilitado()) {
            // Guarda de seguridad, no debería ser llamado si el modo paneo está inactivo.
            return;
        }

        // --- LÓGICA DE DECISIÓN DE ACCIÓN ---

        if (e.isControlDown() && e.isShiftDown()) {
            // CASO 1: ZOOM (Ctrl + Shift + Rueda)
            double zoomActual = model.getZoomFactor();
            double scaleFactor = 1.1; // Factor de escalado
            double nuevoZoom = (e.getWheelRotation() < 0) ? zoomActual * scaleFactor : zoomActual / scaleFactor;
            
            // Limitar el zoom a un rango razonable
            nuevoZoom = Math.max(0.01, Math.min(nuevoZoom, 50.0));
            
            model.setZoomFactor(nuevoZoom);

            // Si el modo actual es "Fijo", actualizamos el valor guardado
            if (model.getCurrentZoomMode() == servicios.zoom.ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
                configuration.setZoomPersonalizadoPorcentaje(nuevoZoom * 100);
                if (statusBarManager != null) statusBarManager.actualizar();
            }
            
            refrescarVistaSincrono();

        } else if (e.isControlDown()) {
            // CASO 2: PANEO HORIZONTAL (Ctrl + Rueda)
            // Un valor negativo en getWheelRotation es "hacia arriba/izquierda"
            aplicarPan(-e.getWheelRotation() * 30, 0);

        } else if (e.isShiftDown()) {
            // CASO 3: PANEO VERTICAL (Shift + Rueda)
            // Un valor negativo en getWheelRotation es "hacia arriba"
            aplicarPan(0, -e.getWheelRotation() * 30);
        }
        
        // NOTA: La rueda sola y Ctrl+Alt se manejan en el VisorController.
        
    } // --- Fin del método manejarRuedaInteracciona ---
    
    /**
     * Realiza una operación de zoom simple. Ya no maneja lógica de teclas.
     * Es llamado por el controlador principal.
     * @param e El evento de la rueda del ratón.
     */
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (!model.isZoomHabilitado()) {
            return;
        }

        double zoomActual = model.getZoomFactor();
        double scaleFactor = 1.1;
        double nuevoZoom = (e.getWheelRotation() < 0) ? zoomActual * scaleFactor : zoomActual / scaleFactor;
        nuevoZoom = Math.max(0.01, Math.min(nuevoZoom, 50.0));
        
        model.setZoomFactor(nuevoZoom);

        if (model.getCurrentZoomMode() == servicios.zoom.ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            configuration.setZoomPersonalizadoPorcentaje(nuevoZoom * 100);
            if (statusBarManager != null) statusBarManager.actualizar();
        }
        
        refrescarVistaSincrono();
    }

    public void iniciarPaneo(MouseEvent e) {
        if (model != null && SwingUtilities.isLeftMouseButton(e) && model.isZoomHabilitado()) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    }
    
    public void continuarPaneo(MouseEvent e) {
        if (model != null && SwingUtilities.isLeftMouseButton(e) && model.isZoomHabilitado()) {
            int deltaX = e.getX() - lastMouseX;
            int deltaY = e.getY() - lastMouseY;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            aplicarPan(deltaX, deltaY);
        }
    }
    
    /**
     * Aplica un desplazamiento a la imagen, solo si el Modo Paneo está activo.
     */
    public void aplicarPan(int deltaX, int deltaY) {
        if (model.isZoomHabilitado()) {
            model.addImageOffsetX(deltaX);
            model.addImageOffsetY(deltaY);
            refrescarVistaSincrono();
        }
    }
    
    /**
     * Refresca la vista. Su única responsabilidad es pintar el estado
     * actual del modelo, sin tomar decisiones lógicas.
     */
    public void refrescarVistaSincrono() {
        if (model == null || registry == null || model.getCurrentImage() == null) return;
        
        // Simplemente lee el estado actual del modelo y lo pinta.
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null) return;
        
        BufferedImage imgOriginal = model.getCurrentImage();
        double zoomFactor = model.getZoomFactor();
        
        int nuevoAncho = (int)(imgOriginal.getWidth() * zoomFactor);
        int nuevoAlto = (int)(imgOriginal.getHeight() * zoomFactor);
        
        Image imagenEscalada = ImageDisplayUtils.escalar(imgOriginal, nuevoAncho, nuevoAlto);
        displayPanel.setImagenEscalada(imagenEscalada, model.getImageOffsetX(), model.getImageOffsetY());
        
//        if (statusBarManager != null) statusBarManager.actualizar();
    }
    // --- FIN del metodo refrescarVistaSincrono ---

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Calcula el factor de zoom "puro" para un modo de zoom base.
     * Es la implementación de la tabla de especificaciones.
     */
    private double _calcularFactorDeZoom(ZoomModeEnum modo) {
        if (modo == null) return 1.0;

        BufferedImage img = model.getCurrentImage();
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        
        if (img == null || displayPanel == null) return 1.0;

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int panelW = displayPanel.getWidth();
        int panelH = displayPanel.getHeight();
        
        if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) return 1.0;

        boolean modoSeguroActivado = model.isMantenerProporcion(); // Botón BMP
        double factorIntencional;

        // --- PASO 1: Determinar el comportamiento según el modo ---
        switch (modo) {
            // --- Modos que ignoran completamente el BMP ---
            case MAINTAIN_CURRENT_ZOOM:
            case USER_SPECIFIED_PERCENTAGE:
                return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
                
            // --- Modos que siempre se ajustan sin salirse ---
            case FIT_TO_SCREEN:
                return Math.min((double) panelW / imgW, (double) panelH / imgH);

            // --- Modos que dependen del estado del BMP ---
            case FIT_TO_WIDTH:
                factorIntencional = (double) panelW / imgW;
                if (modoSeguroActivado) {
                    int altoProyectado = (int) (imgH * factorIntencional);
                    if (altoProyectado > panelH) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH); // Fallback a FIT_TO_SCREEN
                    }
                }
                return factorIntencional;

            case FIT_TO_HEIGHT:
                factorIntencional = (double) panelH / imgH;
                if (modoSeguroActivado) {
                    int anchoProyectado = (int) (imgW * factorIntencional);
                    if (anchoProyectado > panelW) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH); // Fallback a FIT_TO_SCREEN
                    }
                }
                return factorIntencional;

            case DISPLAY_ORIGINAL:
                factorIntencional = 1.0;
                if (modoSeguroActivado) {
                    if (imgW > panelW || imgH > panelH) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH); // Fallback a FIT_TO_SCREEN
                    }
                }
                return factorIntencional;
                
            default:
                // Caso por defecto para FILL (manejado aparte) o cualquier otro enum futuro.
                return model.getZoomFactor(); 
        }
    }
    // --- FIN del metodo _calcularFactorDeZoom ---
    
    public void setStatusBarManager(InfobarStatusManager manager) {this.statusBarManager = manager; }
    public void setListCoordinator(ListCoordinator listCoordinator) {this.listCoordinator = listCoordinator;}
    
}
// --- FIN de la clase ZoomManager ---

