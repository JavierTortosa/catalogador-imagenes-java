package controlador.managers;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JLabel;
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
    private InfobarStatusManager statusBarManager;
    private ListCoordinator listCoordinator;

    private int lastMouseX, lastMouseY;
    
    private ImageDisplayPanel specificPanel = null;

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
        // 1. Validaciones iniciales
        if (model == null || statusBarManager == null || registry == null) {
            System.err.println("ERROR [aplicarModoDeZoom]: Modelo, StatusBarManager o Registry nulos.");
            if (onComplete != null) onComplete.run();
            return;
        }

        if (model.getCurrentImage() == null) {
            ImageDisplayPanel panelActivo = getActiveDisplayPanel();
            if (panelActivo != null) {
                panelActivo.limpiar();
            }
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        ImageDisplayPanel displayPanel = getActiveDisplayPanel();
        if (displayPanel == null || !displayPanel.isShowing() || displayPanel.getWidth() <= 0) {
            System.out.println("[ZoomManager] Panel no listo/visible. Reintentando en EDT...");
            SwingUtilities.invokeLater(() -> aplicarModoDeZoom(modo, onComplete));
            return;
        }

        // --- INICIO DE LA LÓGICA DE ZOOM ---

        double factorDeZoomParaAplicar;
        
        // 2. Lógica de "Captura" para el Modo Fijo (el candado)
        // Si el modo al que estamos cambiando es "Zoom Fijo"...
        if (modo == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
            
            // ...capturamos el factor de zoom REAL que la imagen tiene en este momento.
            factorDeZoomParaAplicar = model.getZoomFactor();
            
            // Actualizamos explícitamente el JLabel de la barra de estado con este nuevo valor.
            JLabel porcentajeLabel = registry.get("label.control.zoomPorcentaje");
            if (porcentajeLabel != null) {
                double porcentaje = factorDeZoomParaAplicar * 100.0;
                porcentajeLabel.setText(String.format("Z: %.0f%%", porcentaje));
                System.out.println("[ZoomManager] Modo Fijo activado. Label de Status Bar actualizado a: " + porcentaje + "%");
            }

        } 
        // 3. Lógica para el Modo Personalizado (el del icono del 7)
        // Si el modo es "Personalizado", usamos el valor que ya está mostrando el JLabel.
        else if (modo == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE) {
            
            double porcentajeDelLabel = statusBarManager.getValorActualDelLabelZoom();
            factorDeZoomParaAplicar = porcentajeDelLabel / 100.0;
            
            System.out.println("[ZoomManager] Modo Personalizado activado. Usando valor del label: " + porcentajeDelLabel + "%");

        } 
        // 4. Lógica para todos los demás modos (Ancho, Alto, etc.)
        else {
            // Para los modos automáticos, simplemente calculamos el factor.
            factorDeZoomParaAplicar = _calcularFactorDeZoom(modo);
        }
        
        // 5. Aplicación final
        model.setCurrentZoomMode(modo);
        model.setZoomFactor(factorDeZoomParaAplicar);
        model.resetPan();
        refrescarVistaSincrono();

        // 6. Sincronización de la UI
        if (onComplete != null) {
            onComplete.run();
        }
        
    } // --- Fin del método aplicarModoDeZoom (con callback) ---
    
    
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

    
    /**
     * Asigna un panel de visualización específico para que este ZoomManager opere sobre él.
     * Si se establece, getActiveDisplayPanel() devolverá este panel en lugar de buscar en el registro.
     * @param panel El ImageDisplayPanel a controlar.
     */
    public void setSpecificPanel(ImageDisplayPanel panel) {
        this.specificPanel = panel;
    } // --- Fin del método setSpecificPanel ---
    
    
    // =================================================================================
    // MÉTODOS PRIVADOS
    // =================================================================================

    /**
     * Obtiene el panel de visualización de imagen activo, dependiendo
     * del modo de trabajo actual del modelo.
     * @return El ImageDisplayPanel correcto (visualizador o proyecto) o null si no se encuentra.
     */
    private ImageDisplayPanel getActiveDisplayPanel() {
        // PRIORIDAD 1: Si se ha establecido un panel específico, lo usamos.
        if (specificPanel != null) {
            return specificPanel;
        }

        // PRIORIDAD 2: Si no, usamos la lógica existente del registro (para la app principal).
        if (registry == null || model == null) {
            return null;
        }
        
        if (model.getCurrentWorkMode() == VisorModel.WorkMode.PROYECTO) {
            return registry.get("panel.proyecto.display");
        } else {
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
        
        // Si las dimensiones no son válidas, devolvemos el zoom actual para no causar errores.
        if (imgW <= 0 || imgH <= 0 || panelW <= 0 || panelH <= 0) {
            return model.getZoomFactor();
        }

        switch (modo) {
        
	        case SMART_FIT:
	            // Calcula la relación de aspecto (ratio ancho/alto) de la imagen y del panel.
	            double imgAspectRatio = (double) imgW / imgH;
	            double panelAspectRatio = (double) panelW / panelH;
	            
	            // Compara las relaciones de aspecto.
	            if (imgAspectRatio > panelAspectRatio) {
	                // Si la imagen es proporcionalmente MÁS ANCHA que el panel,
	                // la limitación es el ANCHO. La ajustamos al ancho del panel.
	                return (double) panelW / imgW;
	            } else {
	                // Si la imagen es proporcionalmente MÁS ALTA (o igual) que el panel,
	                // la limitación es el ALTO. La ajustamos al alto del panel.
	                return (double) panelH / imgH;
	            }
        
            case MAINTAIN_CURRENT_ZOOM:
                // Para el modo fijo, el factor es el que ya está en el modelo.
                return model.getZoomFactor(); 
                
            case USER_SPECIFIED_PERCENTAGE:
                // Para el modo personalizado, leemos el porcentaje de la configuración.
                return configuration.getZoomPersonalizadoPorcentaje() / 100.0;
                
            case FIT_TO_SCREEN:
                return Math.min((double) panelW / imgW, (double) panelH / imgH);
            
            case FILL:
                return Math.max((double) panelW / imgW, (double) panelH / imgH);

            case FIT_TO_WIDTH:
                // Simplemente calcula y devuelve el factor para ajustar al ancho. Sin condiciones.
                return (double) panelW / imgW;

            case FIT_TO_HEIGHT:
                // Simplemente calcula y devuelve el factor para ajustar al alto. Sin condiciones.
                return (double) panelH / imgH;

            case DISPLAY_ORIGINAL:
                // Simplemente devuelve 1.0 (100%). Sin condiciones.
                return 1.0;
                
            default:
                // Como fallback, devuelve el zoom actual.
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

	

} // --- FIN de la clase ZoomManager ---

