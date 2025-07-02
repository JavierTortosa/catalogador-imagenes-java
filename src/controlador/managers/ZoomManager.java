package controlador.managers;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.SwingUtilities;

import controlador.ListCoordinator;
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

    private VisorModel model;
    private ComponentRegistry registry;
    private ConfigurationManager configuration;
//    private InfobarImageManager infobarImageManager;
    private InfobarStatusManager statusBarManager;
    private ListCoordinator listCoordinator;

    private int lastMouseX, lastMouseY;

    public ZoomManager() {
        // El constructor ahora está vacío. Las dependencias se inyectan.
    }

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
        if (model == null || model.getCurrentImage() == null) {
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

        // --- LÓGICA DE SINCRONIZACIÓN PARA ZOOM FIJO ---
        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            double factorActual = model.getZoomFactor();
            configuration.setZoomPersonalizadoPorcentaje(factorActual * 100);
        }
        
        model.setCurrentZoomMode(modo);

        double nuevoFactor = _calcularFactorDeZoom(modo);
        model.setZoomFactor(nuevoFactor);
        model.resetPan();
        
        refrescarVistaSincrono();

        // La barra inferior solo se actualiza si el modo es ZOOM_FIJO o USER_SPECIFIED
//        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM || modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
//            if (statusBarManager != null) statusBarManager.actualizar();
//        }

        if (onComplete != null) onComplete.run();
    } // --- Fin del método aplicarModoDeZoom (con callback) ---
    
    
//    @Override
//    public void aplicarModoDeZoom(ZoomModeEnum modo, Runnable onComplete) {
//        if (model == null || model.getCurrentImage() == null) {
//            ImageDisplayPanel panelActivo = getActiveDisplayPanel();
//            if (panelActivo != null) panelActivo.limpiar();
//            if (onComplete != null) onComplete.run();
//            return;
//        }
//        
//        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
//        if (displayPanel == null || !displayPanel.isShowing() || displayPanel.getWidth() <= 0) {
//            System.out.println("[ZoomManager] Panel no listo/visible. Reintentando en EDT...");
//            SwingUtilities.invokeLater(() -> aplicarModoDeZoom(modo, onComplete));
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
//        double nuevoFactor = _calcularFactorDeZoom(modo);
//        model.setZoomFactor(nuevoFactor);
//        model.resetPan();
//        
//        refrescarVistaSincrono();
//
//        if (statusBarManager != null) statusBarManager.actualizar();
//        if (onComplete != null) onComplete.run();
//    } // --- Fin del método aplicarModoDeZoom (con callback) ---

    
    @Override
    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
        if (model == null || !model.isZoomHabilitado() || model.getCurrentImage() == null) {
            return;
        }

        // --- 1. DATOS INICIALES ---
        double zoomActual = model.getZoomFactor();
        double nuevoZoom = (e.getWheelRotation() < 0) ? zoomActual * 1.1 : zoomActual / 1.1;

        // Obtener el panel y las coordenadas del ratón relativas a él.
        ImageDisplayPanel panel = getActiveDisplayPanel();
        if (panel == null) return;
        java.awt.Point puntoRatonEnPanel = e.getPoint();

        // --- 2. LÓGICA DE ZOOM AL CURSOR (SI ESTÁ ACTIVA) ---
        if (model.isZoomToCursorEnabled()) {
            
            // Dimensiones del panel y de la imagen original.
            double panelW = panel.getWidth();
            double panelH = panel.getHeight();
            double imgW = model.getCurrentImage().getWidth();
            double imgH = model.getCurrentImage().getHeight();

            // Posición actual del paneo (offset).
            double offsetXActual = model.getImageOffsetX();
            double offsetYActual = model.getImageOffsetY();
            
            // Coordenadas de la base centrada (antes del paneo).
            double xBaseActual = (panelW - imgW * zoomActual) / 2.0;
            double yBaseActual = (panelH - imgH * zoomActual) / 2.0;

            // a) Coordenadas del punto del ratón relativas a la esquina superior izquierda
            //    de la imagen DIBUJADA ACTUALMENTE en el panel.
            double ratonRelativoAIngenX = puntoRatonEnPanel.x - (xBaseActual + offsetXActual);
            double ratonRelativoAIngenY = puntoRatonEnPanel.y - (yBaseActual + offsetYActual);

            // b) Calcular la posición del punto del ratón como un ratio (0.0 a 1.0)
            //    dentro de la imagen escalada. Esto nos da un "punto de anclaje" independiente de la escala.
            double ratioX = ratonRelativoAIngenX / (imgW * zoomActual);
            double ratioY = ratonRelativoAIngenY / (imgH * zoomActual);

            // c) Calculamos la nueva base centrada con la nueva escala.
            double xBaseNuevo = (panelW - imgW * nuevoZoom) / 2.0;
            double yBaseNuevo = (panelH - imgH * nuevoZoom) / 2.0;

            // d) Calculamos el NUEVO OFFSET de paneo.
            //    La idea es que la esquina de la imagen dibujada se ajuste para que
            //    el punto de anclaje (el ratio) bajo el ratón siga estando bajo el ratón.
            //    Ecuación: puntoRaton = nuevaBase + nuevoOffset + (ratio * nuevaImagenEscalada)
            //    Despejando nuevoOffset:
            //    nuevoOffset = puntoRaton - nuevaBase - (ratio * nuevaImagenEscalada)
            double nuevoOffsetX = puntoRatonEnPanel.x - xBaseNuevo - (ratioX * imgW * nuevoZoom);
            double nuevoOffsetY = puntoRatonEnPanel.y - yBaseNuevo - (ratioY * imgH * nuevoZoom);
            
            // Actualizar el offset en el modelo
            model.setImageOffsetX((int) nuevoOffsetX);
            model.setImageOffsetY((int) nuevoOffsetY);
        }
        
        // --- 3. ACTUALIZAR ZOOM Y REPINTAR (SE HACE SIEMPRE) ---
        model.setZoomFactor(nuevoZoom);
        refrescarVistaSincrono();

        // --- 4. ACTUALIZAR UI (Opcional, pero recomendado) ---
        //    Si el modo de zoom es FIJO, al hacer zoom manual, el porcentaje cambia.
        //    Es buena idea actualizar la barra de estado.
        if (model.getCurrentZoomMode() == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM || model.getCurrentZoomMode() == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
            if (statusBarManager != null) {
                statusBarManager.actualizar();
            }
        }
    }
    
    
//    @Override
//    public void aplicarZoomConRueda(java.awt.event.MouseWheelEvent e) {
//        if (model == null || !model.isZoomHabilitado()) return;
//
//        double zoomActual = model.getZoomFactor();
//        double scaleFactor = 1.1;
//        double nuevoZoom = (e.getWheelRotation() < 0) ? zoomActual * scaleFactor : zoomActual / scaleFactor;
//        
//        // 1. Actualiza el modelo
//        model.setZoomFactor(nuevoZoom);
//        
//        // 2. Repinta la imagen
//        refrescarVistaSincrono();
//        
//        // NADA MÁS. No hay llamadas a otros managers.
//    } // --- Fin del método aplicarZoomConRueda ---
    

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
            // El panel es "inteligente", solo necesita que le pidamos que se repinte.
            // Leerá el estado actualizado del modelo por sí mismo en su paintComponent.
            displayPanel.repaint();
        }
    } // --- Fin del método refrescarVistaSincrono ---
    
 // En la clase: controlador.managers.ZoomManager

    @Override
    public void manejarRuedaInteracciona(java.awt.event.MouseWheelEvent e) {
        if (model == null || !model.isZoomHabilitado()) {
            return;
        }

        // Esta lógica depende de la implementación en VisorController.
        // Se asume que el VisorController decide qué hacer basándose en las teclas modificadoras.
        // Aquí replicamos una posible lógica:
        if (e.isControlDown() && e.isShiftDown()) {
            // Zoom in/out
            aplicarZoomConRueda(e);
        } else if (e.isShiftDown()) {
            // Paneo Horizontal
            aplicarPan(-e.getWheelRotation() * 30, 0);
        } else if (e.isControlDown()) {
            // Paneo Vertical
            aplicarPan(0, -e.getWheelRotation() * 30);
        } else {
            // Si no hay modificadores, podría ser para navegar entre imágenes.
            // Esto lo maneja el VisorController, así que aquí no hacemos nada.
        }
    } // --- Fin del método manejarRuedaInteracciona ---

    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Obtiene el panel de visualización de imagen activo, dependiendo
     * del modo de trabajo actual del modelo.
     * @return El ImageDisplayPanel correcto (visualizador o proyecto) o null si no se encuentra.
     */
    private ImageDisplayPanel getActiveDisplayPanel() {
        if (registry == null || model == null) {
            return null;
        }
        
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            return registry.get("panel.proyecto.display");
        } else {
            // Para VISUALIZADOR y DATOS (por defecto) usamos el panel principal.
            return registry.get("panel.display.imagen");
        }
    } // --- Fin del método getActiveDisplayPanel ---
    
    /**
     * Calcula el factor de zoom basándose en el modo seleccionado.
     * Lee el estado actual del modelo y las dimensiones del panel activo.
     * @param modo El ZoomModeEnum a aplicar.
     * @return El factor de zoom calculado (ej: 1.0 para 100%).
     */
    private double _calcularFactorDeZoom(ZoomModeEnum modo) {
        if (modo == null) return 1.0;

        BufferedImage img = model.getCurrentImage();
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        
        if (img == null || displayPanel == null) return 1.0;

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int panelW = displayPanel.getWidth();
        int panelH = displayPanel.getHeight();
        
        if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) {
            return model.getZoomFactor();
        }

        boolean modoSeguroActivado = model.isMantenerProporcion();
        double factorIntencional;

        switch (modo) {
            case MAINTAIN_CURRENT_ZOOM:
            case USER_SPECIFIED_PERCENTAGE:
                return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
                
            case FIT_TO_SCREEN:
                return Math.min((double) panelW / imgW, (double) panelH / imgH);
            
            case FILL:
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
    
    // --- SETTERS PARA INYECCIÓN DE DEPENDENCIAS ---

    @Override
    public void setStatusBarManager(InfobarStatusManager manager) { this.statusBarManager = manager; }
    @Override
    public void setListCoordinator(ListCoordinator listCoordinator) { this.listCoordinator = listCoordinator; }
    public void setModel(VisorModel model) { this.model = Objects.requireNonNull(model, "VisorModel no puede ser null"); }
    public void setRegistry(ComponentRegistry registry) { this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null"); }
    public void setConfiguration(ConfigurationManager configuration) { this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null"); }

//    public void setInfobarImageManager(InfobarImageManager manager) { this.infobarImageManager = manager; }
	

} // --- FIN de la clase ZoomManager ---

