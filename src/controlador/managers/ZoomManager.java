package controlador.managers;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
// No necesitamos importar VisorController aquí
import java.util.Objects;

import javax.swing.SwingUtilities;

import modelo.VisorModel;
import servicios.ConfigurationManager; // Para leer el porcentaje de zoom personalizado
import servicios.zoom.ZoomModeEnum;     // La enum que define los tipos de zoom
import vista.VisorView;
import vista.util.ImageDisplayUtils;   // La utilidad para reescalar la imagen base para la vista

public class ZoomManager {
    private VisorModel model;
    private VisorView view;
    private ConfigurationManager configuration;
    private int lastMouseX, lastMouseY; // Para el paneo
    
    private InfoBarManager infoBarManagerRef; 

    // --- SECCIÓN 2: CONSTRUCTOR ---
    /**
     * Constructor para ZoomManager.
     * @param model La instancia de VisorModel.
     * @param view La instancia de VisorView.
     * @param configuration La instancia de ConfigurationManager.
     */
    public ZoomManager(VisorModel model, VisorView view, ConfigurationManager configuration) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null en ZoomManager");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null en ZoomManager");
        this.configuration = Objects.requireNonNull(configuration, "ConfigurationManager no puede ser null en ZoomManager");
    }

    // --- SECCIÓN 3: MÉTODOS PARA GESTIONAR EL MODO DE ZOOM MANUAL ---

//    /**
//     * Activa o desactiva el modo de zoom manual en el modelo.
//     * Si el estado cambia, también resetea el zoom y paneo actuales del modelo.
//     * La actualización de la UI (botones, actions, repintado de imagen) la debe
//     * orquestar el llamador (la Action o el VisorController) DESPUÉS de llamar a este método.
//     *
//     * @param activar true para activar el zoom manual, false para desactivarlo.
//     * @return true si el estado del zoom manual en el modelo realmente cambió, false en caso contrario.
//     */
//    public boolean activarODesactivarZoomManual(boolean activar) {
//        if (this.model == null) return false;
//        if (this.model.isZoomHabilitado() == activar) return false;
//        this.model.setZoomHabilitado(activar);
//        this.model.resetZoomState(); 
//        return true;
//    }
    
    
    public boolean activarODesactivarZoomManual(boolean activar) {
        if (this.model == null) {
            System.err.println("ERROR [ZoomManager.activarODesactivarZoomManual]: Modelo es null.");
            return false;
        }
        if (this.model.isZoomHabilitado() == activar) {
            // System.out.println("[ZoomManager] Zoom manual ya está en estado: " + activar + ". No se realiza cambio.");
            return false; // No hubo cambio
        }

        this.model.setZoomHabilitado(activar); // Actualiza el estado en el modelo
        System.out.println("[ZoomManager] Zoom manual " + (activar ? "ACTIVADO" : "DESACTIVADO") + " en el modelo.");

        if (!activar) { // Si se está desactivando
            this.model.resetZoomState(); // Resetea factor de zoom y offsets en el modelo
            System.out.println("[ZoomManager] Estado de zoom/pan reseteado en el modelo porque el zoom manual se desactivó.");
        }

        // --- ¡AQUÍ! ACTUALIZAR LA CONFIGURACIÓN ---
        if (this.configuration != null) {
            this.configuration.setString(
                ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO,
                String.valueOf(activar) // Guardar el nuevo estado
            );
            System.out.println("  [ZoomManager] Configuración '" + ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_MANUAL_INICIAL_ACTIVO + "' actualizada a: " + activar);
        } else {
            System.err.println("WARN [ZoomManager.activarODesactivarZoomManual]: ConfigurationManager es null. No se pudo guardar el estado del zoom manual en config.");
        }
        // --- FIN ACTUALIZAR CONFIGURACIÓN ---

        return true; // Hubo cambio
    }
    

    /**
     * Devuelve si el modo de zoom manual está actualmente activo según el modelo.
     * @return true si el zoom manual está habilitado, false en caso contrario.
     */
    public boolean isModoZoomManualActivo() {
        return (this.model != null) && this.model.isZoomHabilitado();
    }

    // --- SECCIÓN 4: MÉTODOS PARA APLICAR ZOOM/PAN (TÍPICAMENTE DESDE LISTENERS DE RATÓN) ---

    /**
     * Establece un nuevo factor de zoom en el modelo.
     * Usado por el listener de la rueda del ratón.
     * @param nuevoFactor El nuevo factor de zoom a aplicar.
     * @return true si el factor de zoom en el modelo cambió, false si no.
     */
    public boolean establecerFactorZoom(double nuevoFactor) {
        if (this.model == null) return false;
        if (Math.abs(this.model.getZoomFactor() - nuevoFactor) > 0.0001) {
            this.model.setZoomFactor(nuevoFactor);
            return true;
        }
        return false;
    }

    /**
     * Aplica un delta al paneo (offsets X e Y) en el modelo.
     * Usado por el listener de arrastre del ratón.
     * Solo aplica el paneo si el zoom manual está habilitado en el modelo.
     * @param deltaX El cambio en el eje X.
     * @param deltaY El cambio en el eje Y.
     */
    public void aplicarPan(int deltaX, int deltaY) {
        if (this.model == null || !this.model.isZoomHabilitado()) return;
        this.model.addImageOffsetX(deltaX);
        this.model.addImageOffsetY(deltaY);
    }
    
    
    // --- SECCIÓN 5: MÉTODOS PARA MODOS DE ZOOM ESPECÍFICOS Y RESET ---


    /**
     * Aplica un modo de zoom específico (ej. ajustar a ancho, alto, pantalla).
     * Actualiza el VisorModel con el nuevo modo y el factor de zoom calculado,
     * y luego solicita un refresco de la vista principal.
     *
     * @param modoDeseado El tipo de zoom a aplicar, según la enum {@link ZoomModeEnum}.
     * @return true si el ZoomModeEnum en el modelo realmente cambió, false en caso contrario.
     */
    public boolean aplicarModoDeZoom(ZoomModeEnum modoDeseado) {
        if (model == null || view == null || view.getEtiquetaImagen() == null || configuration == null) { /*...*/ return false; }
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal == null) { /*...*/ return false; } // Asumimos que limpia la vista y actualiza modo si es necesario

        int etiquetaAncho = view.getEtiquetaImagen().getWidth();
        int etiquetaAlto = view.getEtiquetaImagen().getHeight();
        if (etiquetaAncho <= 0 || etiquetaAlto <= 0) {
            // Si no hay dimensiones de etiqueta, solo 100% o % especificado tiene sentido.
             if (!(modoDeseado == ZoomModeEnum.DISPLAY_ORIGINAL || 
                   modoDeseado == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE ||
                   modoDeseado == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM)) {
                System.out.println("[ZoomManager] WARN: Etiqueta sin tamaño, no se puede aplicar modo de ajuste: " + modoDeseado);
                return false;
            }
        }

        ZoomModeEnum modoAnterior = model.getCurrentZoomMode();
        double nuevoFactorCalculado = 1.0; // Default
        boolean encajarTotalmente = model.isMantenerProporcion(); 
        model.resetPan();

        int imgW = imgOriginal.getWidth();
        int imgH = imgOriginal.getHeight();

        switch (modoDeseado) {
	        case DISPLAY_ORIGINAL:
	            if (encajarTotalmente) { // prop ON
	                // ImageDisplayUtils devuelve original. Queremos 100% O que quepa.
	                if (imgW > 0 && imgH > 0 && etiquetaAncho > 0 && etiquetaAlto > 0) {
	                    double factorParaQueQuepa = Math.min((double)etiquetaAncho / imgW, (double)etiquetaAlto / imgH);
	                    nuevoFactorCalculado = Math.min(1.0, factorParaQueQuepa);
	                }
	            } else { // prop OFF
	                nuevoFactorCalculado = 1.0; // ImageDisplayUtils devuelve original. Mostrar al 100% con desborde.
	            }
	            break;

	        case FIT_TO_WIDTH:
	            if (imgW > 0 && etiquetaAncho > 0) {
	                nuevoFactorCalculado = (double) etiquetaAncho / imgW;
	                if (encajarTotalmente && imgH > 0 && etiquetaAlto > 0) { // Si prop ON y el alto se desborda
	                    if ((imgH * nuevoFactorCalculado) > etiquetaAlto) {
	                        nuevoFactorCalculado = (double) etiquetaAlto / imgH; // Ajusta para que quepa
	                    }
	                }
	                // Si prop OFF, el factor se queda para ajustar al ancho, permitiendo desborde alto.
	            }
	            break;

	        case FIT_TO_HEIGHT:
	            if (imgH > 0 && etiquetaAlto > 0) {
	                nuevoFactorCalculado = (double) etiquetaAlto / imgH;
	                if (encajarTotalmente && imgW > 0 && etiquetaAncho > 0) { // Si prop ON y el ancho se desborda
	                    if ((imgW * nuevoFactorCalculado) > etiquetaAncho) {
	                        nuevoFactorCalculado = (double) etiquetaAncho / imgW; // Ajusta para que quepa
	                    }
	                }
	                // Si prop OFF, el factor se queda para ajustar al alto, permitiendo desborde ancho.
	            }
	            break;

            case FIT_TO_SCREEN: // "Ajustar"
            	// ImageDisplayUtils ya ha hecho el trabajo:
                // - Si prop ON: la base está ajustada proporcionalmente para caber.
                // - Si prop OFF: la base está estirada para llenar.
                // En ambos casos, el factor adicional es 1.0 para mostrar esa base tal cual.
                nuevoFactorCalculado = 1.0;
                break;

            case MAINTAIN_CURRENT_ZOOM:
                nuevoFactorCalculado = model.getZoomFactor();
                double porcentajeParaGuardar = nuevoFactorCalculado * 100.0;

             // 3. Guardar este porcentaje en ConfigurationManager (en memoria).
                if (configuration != null) { // Buena práctica verificar
                    configuration.setZoomPersonalizadoPorcentaje(porcentajeParaGuardar);
                    System.out.println("  [ZoomManager] MAINTAIN_CURRENT_ZOOM: config '" +
                                       ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE +
                                       "' actualizado a " + String.format("%.2f", porcentajeParaGuardar) + // Formatear para el log
                                       "% (basado en el factor de zoom actual: " + String.format("%.4f", nuevoFactorCalculado) + ").");
                } else {
                    System.err.println("WARN [ZoomManager MAINTAIN_CURRENT_ZOOM]: ConfigurationManager es null. No se pudo guardar el porcentaje personalizado.");
                }
                
                break;
            case USER_SPECIFIED_PERCENTAGE:
            	double pConfig = configuration.getDouble(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE, 100.0);
                nuevoFactorCalculado = pConfig / 100.0;
                System.out.println("  [ZoomManager] Modo USER_SPECIFIED_PERCENTAGE: Factor desde config (" + pConfig + "%) = " + nuevoFactorCalculado + " (EncajarTotalmente: " + encajarTotalmente + ")");
                break;
            	
//            	double pConfig = configuration.getDouble(ConfigurationManager.KEY_COMPORTAMIENTO_ZOOM_PERSONALIZADO_PORCENTAJE, 100.0);
//            	nuevoFactorCalculado = pConfig / 100.0;
//                break;
            	
            	
        }
        
        model.setCurrentZoomMode(modoDeseado);
        model.setZoomFactor(nuevoFactorCalculado); 
        refrescarVistaPrincipalConEstadoActualDelModelo(); // Llama a ImageDisplayUtils y luego a view.setImagenMostrada
        return modoAnterior != modoDeseado;
    } // FIN metodo aplicarModoDeZoom
    
    
    /**
     * Resetea el zoom y paneo en el modelo y luego solicita a la vista que se refresque.
     * Normalmente llamado por ResetZoomAction.
     */
    public void resetearZoomYPanYRefrescarVista() {
        if (model == null) return;
        model.resetZoomState();
        refrescarVistaPrincipalConEstadoActualDelModelo();
    }

    // --- SECCIÓN 6: MÉTODO PRIVADO DE REFRESCO DE LA VISTA ---

    /**
     * Método PRIVADO para refrescar la imagen principal en la vista.
     * Utiliza ImageDisplayUtils para obtener la imagen base escalada según el ajuste
     * por defecto (respetando proporciones si está activado en el modelo) y luego
     * le dice a la vista que la muestre aplicando el zoomFactor y offsets actuales del modelo.
     */
    public void refrescarVistaPrincipalConEstadoActualDelModelo() {
        if (model == null || view == null) {
            System.err.println("ERROR [ZoomManager.refrescarVista]: Modelo o Vista nulos."); // Añadir log si quieres
            return;
        }
        BufferedImage imgOriginalDelModelo = model.getCurrentImage();

        if (imgOriginalDelModelo == null) {
            view.limpiarImagenMostrada();
        } else {
            Image imagenBaseParaVista = ImageDisplayUtils.reescalarImagenParaAjustar(imgOriginalDelModelo, model, view);
            if (imagenBaseParaVista != null) {
                view.setImagenMostrada(imagenBaseParaVista, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
            } else {
                System.err.println("ERROR [ZoomManager.refrescarVista]: ImageDisplayUtils devolvió null. Limpiando vista."); // Log
                view.limpiarImagenMostrada();
            }
        }

        // --- LLAMADA PARA ACTUALIZAR LAS BARRAS DE INFORMACIÓN ---
        if (infoBarManagerRef != null) {
        	// LOG Refresco vista Completado
//            System.out.println("  [ZoomManager] Refresco de vista completado. Solicitando actualización de InfoBars...");
            infoBarManagerRef.actualizarBarrasDeInfo();
        } else {
            System.out.println("  [ZoomManager] WARN: infoBarManagerRef es null. No se pueden actualizar barras desde ZoomManager.");
        }
    }
    
    
    public void manejarRuedaRaton(MouseWheelEvent e) {
        if (this.model == null || !this.model.isZoomHabilitado()) return;

        int notches = e.getWheelRotation();
        double currentZoomFactor = this.model.getZoomFactor();
        double zoomIncrement = 0.1; // Podría ser configurable
        double newZoomFactor = currentZoomFactor + (notches < 0 ? zoomIncrement : -zoomIncrement);
        newZoomFactor = Math.max(0.01, Math.min(newZoomFactor, 20.0)); // Limitar

        if (Math.abs(newZoomFactor - currentZoomFactor) > 0.001) {
            if (establecerFactorZoom(newZoomFactor)) { // Llama a su propio método que actualiza el modelo
                refrescarVistaPrincipalConEstadoActualDelModelo();
            }
        }
    }

    public void iniciarPaneo(MouseEvent e) {
        if (this.model != null && this.model.isZoomHabilitado() && SwingUtilities.isLeftMouseButton(e)) {
            this.lastMouseX = e.getX();
            this.lastMouseY = e.getY();
        }
    }

    public void continuarPaneo(MouseEvent e) {
        if (this.model != null && this.model.isZoomHabilitado() && SwingUtilities.isLeftMouseButton(e)) {
            int deltaX = e.getX() - this.lastMouseX;
            int deltaY = e.getY() - this.lastMouseY;
            
            aplicarPan(deltaX, deltaY); // Llama a su propio método que actualiza el modelo
            
            this.lastMouseX = e.getX();
            this.lastMouseY = e.getY();
            
            refrescarVistaPrincipalConEstadoActualDelModelo();
        }
    }
    
    
    public void setInfoBarManager(InfoBarManager infoBarManager) {
        this.infoBarManagerRef = infoBarManager;
        if (infoBarManager != null) { // Log opcional
            System.out.println("  [ZoomManager] InfoBarManager inyectado.");
        }
    }
    
    
} // --- FIN CLASE ZoomManager ---