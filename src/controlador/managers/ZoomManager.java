package controlador.managers;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.SwingUtilities;

import controlador.ListCoordinator;
// --- INICIO DE LA MODIFICACIÓN: Importar la interfaz ---
import controlador.managers.interfaces.IZoomManager;
// --- FIN DE LA MODIFICACIÓN ---
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
// --- INICIO DE LA MODIFICACIÓN: Implementar la interfaz ---
public class ZoomManager implements IZoomManager {
// --- FIN DE LA MODIFICACIÓN ---

    // --- INICIO DE LA MODIFICACIÓN: Campos para dependencias ---
	private VisorModel model;
    private ComponentRegistry registry;
    private ConfigurationManager configuration;
    private InfobarStatusManager statusBarManager;
    private ListCoordinator listCoordinator;

    private int lastMouseX, lastMouseY;

    public ZoomManager() {
        // El constructor ahora está vacío.
    } // --- Fin del constructor de ZoomManager ---

    // =================================================================================
    // MÉTODOS PÚBLICOS
    // =================================================================================
    
    @Override
    public void setPermisoManual(boolean activar) {
        if (model.isZoomHabilitado() == activar) return;
        
        model.setZoomHabilitado(activar);
        
        if (statusBarManager != null) statusBarManager.actualizar();
    } // --- FIN del metodo setPermisoManual ---
    
    // Este método delega la llamada a la versión con el callback, pasando null.
    @Override
    public void aplicarModoDeZoom(ZoomModeEnum modo) {
        aplicarModoDeZoom(modo, null);
    } // --- Fin del método aplicarModoDeZoom (sin callback) ---    
    
    @Override
    public void aplicarModoDeZoom(ZoomModeEnum modo, Runnable onComplete) {
        if (model == null || model.getCurrentImage() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel == null || displayPanel.getWidth() <= 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            double factorActual = model.getZoomFactor();
            configuration.setZoomPersonalizadoPorcentaje(factorActual * 100);
            System.out.println("[ZoomManager] Capturado nuevo Zoom Fijo: " + (factorActual * 100) + "%");
        }

        model.setCurrentZoomMode(modo);

        // Se elimina el caso especial para FILL, ya que _calcularFactorDeZoom ahora lo maneja
        // y el refresco es genérico. Si necesitaras una lógica de paneo especial para FILL,
        // se añadiría aquí, pero el escalado ya no.

        double nuevoFactor = _calcularFactorDeZoom(modo);
        model.setZoomFactor(nuevoFactor);
        model.resetPan();
        
        refrescarVistaSincrono(); // <-- Esta llamada ahora es muy ligera.

        // Notificar a la barra de estado del cambio.
        if (statusBarManager != null) {
            statusBarManager.actualizar();
        }

        // Ejecutar el callback al final de toda la operación
        if (onComplete != null) {
            onComplete.run();
        }
    } // --- Fin del método aplicarModoDeZoom (con callback) ---
    
    
//    @Override
//    public void aplicarModoDeZoom(ZoomModeEnum modo) {
//        aplicarModoDeZoom(modo, null);
//    } // --- Fin del método aplicarModoDeZoom (modificado) ---
//
//    // Implementar el nuevo método de la interfaz
//    @Override
//    public void aplicarModoDeZoom(ZoomModeEnum modo, Runnable onComplete) {
//        if (model == null || model.getCurrentImage() == null) {
//            if (onComplete != null) onComplete.run();
//            return;
//        }
//        
//        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
//        if (displayPanel == null || displayPanel.getWidth() <= 0) {
//            if (onComplete != null) onComplete.run();
//            return;
//        }
//
//        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
//            double factorActual = model.getZoomFactor();
//            configuration.setZoomPersonalizadoPorcentaje(factorActual * 100);
//            System.out.println("[ZoomManager] Capturado nuevo Zoom Fijo: " + (factorActual * 100) + "%");
//        }
//
//        model.setCurrentZoomMode(modo);
//
//        if (modo == ZoomModeEnum.FILL) {
//            BufferedImage imgOriginal = model.getCurrentImage();
//            Image imagenEscalada = ImageDisplayUtils.escalar(imgOriginal, displayPanel.getWidth(), displayPanel.getHeight());
//            double factorPromedio = ((double) displayPanel.getWidth() / imgOriginal.getWidth() + (double) displayPanel.getHeight() / imgOriginal.getHeight()) / 2.0;
//            model.setZoomFactor(factorPromedio);
//            model.resetPan();
//            displayPanel.setImagenEscalada(imagenEscalada, model.getImageOffsetX(), model.getImageOffsetY());
//            if (statusBarManager != null) statusBarManager.actualizar();
//            
//            if (onComplete != null) {
//                onComplete.run();
//            }
//            return; 
//        }
//
//        double nuevoFactor = _calcularFactorDeZoom(modo);
//        model.setZoomFactor(nuevoFactor);
//        model.resetPan();
//        
//        refrescarVistaSincrono();
//
//        // Ejecutar el callback al final de toda la operación
//        if (onComplete != null) {
//            onComplete.run();
//        }
//    } // --- Fin del método aplicarModoDeZoom (con callback) ---
    
    @Override
    public void manejarRuedaInteracciona(java.awt.event.MouseWheelEvent e) {
        if (!model.isZoomHabilitado()) {
            return;
        }

        if (e.isControlDown() && e.isShiftDown()) {
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

        } else if (e.isControlDown()) {
            aplicarPan(-e.getWheelRotation() * 30, 0);

        } else if (e.isShiftDown()) {
            aplicarPan(0, -e.getWheelRotation() * 30);
        }
    } // --- Fin del método manejarRuedaInteracciona ---
    
    @Override
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
        if (model.isZoomHabilitado()) {
            model.addImageOffsetX(deltaX);
            model.addImageOffsetY(deltaY);
            refrescarVistaSincrono();
        }
    } // --- Fin del método aplicarPan ---
    
    
    @Override
    public void refrescarVistaSincrono() {
        if (registry == null) {
            System.err.println("WARN [ZoomManager.refrescarVistaSincrono]: Registry es nulo.");
            return;
        }
        
        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
        if (displayPanel != null) {
            // El panel ya tiene acceso al modelo y sabe cómo pintarse a sí mismo
            // con la transformación correcta. Solo necesita que le digamos CUÁNDO.
            displayPanel.repaint();
        } else {
            System.err.println("WARN [ZoomManager.refrescarVistaSincrono]: 'panel.display.imagen' no encontrado en el registro.");
        }
    } // --- Fin del método refrescarVistaSincrono ---
    
    
//    @Override
//    public void refrescarVistaSincrono() {
//        if (model == null || registry == null || model.getCurrentImage() == null) return;
//        
//        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
//        if (displayPanel == null) return;
//        
//        BufferedImage imgOriginal = model.getCurrentImage();
//        double zoomFactor = model.getZoomFactor();
//        
//        int nuevoAncho = (int)(imgOriginal.getWidth() * zoomFactor);
//        int nuevoAlto = (int)(imgOriginal.getHeight() * zoomFactor);
//        
//        Image imagenEscalada = ImageDisplayUtils.escalar(imgOriginal, nuevoAncho, nuevoAlto);
//        displayPanel.setImagenEscalada(imagenEscalada, model.getImageOffsetX(), model.getImageOffsetY());
//    } // --- FIN del metodo refrescarVistaSincrono ---

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    
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

        boolean modoSeguroActivado = model.isMantenerProporcion();
        double factorIntencional;

        switch (modo) {
            case MAINTAIN_CURRENT_ZOOM:
            case USER_SPECIFIED_PERCENTAGE:
                return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
                
            case FIT_TO_SCREEN:
                return Math.min((double) panelW / imgW, (double) panelH / imgH);
            
            case FILL: // <--- NUEVO CASO MANEJADO AQUÍ
                // Devuelve el factor de escala más GRANDE para que la imagen rellene el panel.
                // Esto ignorará la proporción si es necesario, pero el panel se encargará de estirarla.
                // Si `mantenerProporcion` estuviera activado, este modo no debería ser una opción
                // o debería comportarse como FIT_TO_SCREEN. Por ahora, asumimos que ignora la proporción.
                return Math.max((double) panelW / imgW, (double) panelH / imgH);

            case FIT_TO_WIDTH:
                factorIntencional = (double) panelW / imgW;
                if (modoSeguroActivado) {
                    int altoProyectado = (int) (imgH * factorIntencional);
                    if (altoProyectado > panelH) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
                    }
                }
                return factorIntencional;

            case FIT_TO_HEIGHT:
                factorIntencional = (double) panelH / imgH;
                if (modoSeguroActivado) {
                    int anchoProyectado = (int) (imgW * factorIntencional);
                    if (anchoProyectado > panelW) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
                    }
                }
                return factorIntencional;

            case DISPLAY_ORIGINAL:
                factorIntencional = 1.0;
                if (modoSeguroActivado) {
                    if (imgW > panelW || imgH > panelH) {
                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
                    }
                }
                return factorIntencional;
                
            default:
                return model.getZoomFactor(); 
        }
    } // --- Fin del método _calcularFactorDeZoom ---
    
    
//    private double _calcularFactorDeZoom(ZoomModeEnum modo) {
//        if (modo == null) return 1.0;
//
//        BufferedImage img = model.getCurrentImage();
//        ImageDisplayPanel displayPanel = registry.get("panel.display.imagen");
//        
//        if (img == null || displayPanel == null) return 1.0;
//
//        int imgW = img.getWidth();
//        int imgH = img.getHeight();
//        int panelW = displayPanel.getWidth();
//        int panelH = displayPanel.getHeight();
//        
//        if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) return 1.0;
//
//        boolean modoSeguroActivado = model.isMantenerProporcion();
//        double factorIntencional;
//
//        switch (modo) {
//            case MAINTAIN_CURRENT_ZOOM:
//            case USER_SPECIFIED_PERCENTAGE:
//                return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
//                
//            case FIT_TO_SCREEN:
//                return Math.min((double) panelW / imgW, (double) panelH / imgH);
//
//            case FIT_TO_WIDTH:
//                factorIntencional = (double) panelW / imgW;
//                if (modoSeguroActivado) {
//                    int altoProyectado = (int) (imgH * factorIntencional);
//                    if (altoProyectado > panelH) {
//                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
//                    }
//                }
//                return factorIntencional;
//
//            case FIT_TO_HEIGHT:
//                factorIntencional = (double) panelH / imgH;
//                if (modoSeguroActivado) {
//                    int anchoProyectado = (int) (imgW * factorIntencional);
//                    if (anchoProyectado > panelW) {
//                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
//                    }
//                }
//                return factorIntencional;
//
//            case DISPLAY_ORIGINAL:
//                factorIntencional = 1.0;
//                if (modoSeguroActivado) {
//                    if (imgW > panelW || imgH > panelH) {
//                        return Math.min((double) panelW / imgW, (double) panelH / imgH);
//                    }
//                }
//                return factorIntencional;
//                
//            default:
//                return model.getZoomFactor(); 
//        }
//    } // --- FIN del metodo _calcularFactorDeZoom ---
    
    // --- INICIO DE LA MODIFICACIÓN: Setters para inyección de dependencias ---
    @Override
    public void setStatusBarManager(InfobarStatusManager manager) {
        this.statusBarManager = manager; 
    } // --- Fin del método setStatusBarManager ---

    @Override
    public void setListCoordinator(ListCoordinator listCoordinator) {
        this.listCoordinator = listCoordinator;
    } // --- Fin del método setListCoordinator ---
    
    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
    } // --- Fin del método setModel ---
    
    public void setRegistry(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
    } // --- Fin del método setRegistry ---
    
    public void setConfiguration(ConfigurationManager configuration) {
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null");
    } // --- Fin del método setConfiguration ---
    // --- FIN DE LA MODIFICACIÓN ---

	
    
} // --- FIN de la clase ZoomManager ---


