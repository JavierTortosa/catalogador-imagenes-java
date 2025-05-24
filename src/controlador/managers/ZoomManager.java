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

    /**
     * Activa o desactiva el modo de zoom manual en el modelo.
     * Si el estado cambia, también resetea el zoom y paneo actuales del modelo.
     * La actualización de la UI (botones, actions, repintado de imagen) la debe
     * orquestar el llamador (la Action o el VisorController) DESPUÉS de llamar a este método.
     *
     * @param activar true para activar el zoom manual, false para desactivarlo.
     * @return true si el estado del zoom manual en el modelo realmente cambió, false en caso contrario.
     */
    public boolean activarODesactivarZoomManual(boolean activar) {
        if (this.model == null) return false;
        if (this.model.isZoomHabilitado() == activar) return false;
        this.model.setZoomHabilitado(activar);
        this.model.resetZoomState(); 
        return true;
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
            	// ImageDisplayUtils devuelve original.
                // Calculamos factor para que el ancho de la original sea el de la etiqueta.
                if (imgW > 0 && etiquetaAncho > 0) {
                    nuevoFactorCalculado = (double) etiquetaAncho / imgW;
                    if (encajarTotalmente && imgH > 0 && etiquetaAlto > 0) {
                        // Si prop ON, y el alto desborda, necesitamos que el factor final la haga caber.
                        if ((imgH * nuevoFactorCalculado) > etiquetaAlto) {
                            nuevoFactorCalculado = (double) etiquetaAlto / imgH;
                        }
                    }
                }
                break;

            case FIT_TO_HEIGHT:
            	// ImageDisplayUtils devuelve original.
                if (imgH > 0 && etiquetaAlto > 0) {
                    nuevoFactorCalculado = (double) etiquetaAlto / imgH;
                    if (encajarTotalmente && imgW > 0 && etiquetaAncho > 0) {
                        if ((imgW * nuevoFactorCalculado) > etiquetaAncho) {
                            nuevoFactorCalculado = (double) etiquetaAncho / imgW;
                        }
                    }
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
                break;
            case USER_SPECIFIED_PERCENTAGE:
                double pConfig = configuration.getDouble("zoom.personalizado.porcentaje", 100.0);
                nuevoFactorCalculado = pConfig / 100.0;
                break;
        }
        
        model.setCurrentZoomMode(modoDeseado);
        model.setZoomFactor(nuevoFactorCalculado); 
        refrescarVistaPrincipalConEstadoActualDelModelo(); // Llama a ImageDisplayUtils y luego a view.setImagenMostrada
        return modoAnterior != modoDeseado;
    }
    
    
    public boolean aplicarModoDeZoomNEWOLD(ZoomModeEnum modoDeseado) {
        // --- SECCIÓN 1: VALIDACIONES INICIALES Y PREPARACIÓN ---
        if (model == null || view == null || view.getEtiquetaImagen() == null || configuration == null) {
            System.err.println("ERROR [ZoomManager.aplicarModoDeZoom]: Dependencias nulas.");
            return false;
        }
        
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal == null) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] No hay imagen original.");
            if (view != null) view.limpiarImagenMostrada();
            boolean modoRealmenteCambio = model.getCurrentZoomMode() != modoDeseado;
            if (modoRealmenteCambio) model.setCurrentZoomMode(modoDeseado);
            return modoRealmenteCambio;
        }

        int etiquetaAncho = view.getEtiquetaImagen().getWidth();
        int etiquetaAlto = view.getEtiquetaImagen().getHeight();

        // Si la etiqueta no tiene dimensiones, no podemos hacer la mayoría de los cálculos.
        // Excepción para DISPLAY_ORIGINAL, MAINTAIN_CURRENT_ZOOM, USER_SPECIFIED_PERCENTAGE
        if ((etiquetaAncho <= 0 || etiquetaAlto <= 0) && 
            !(modoDeseado == ZoomModeEnum.DISPLAY_ORIGINAL || 
              modoDeseado == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM ||
              modoDeseado == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE)) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] WARN: Etiqueta sin tamaño válido. No se puede aplicar modo: " + modoDeseado);
            return false; 
        }

        ZoomModeEnum modoAnterior = model.getCurrentZoomMode();
        double nuevoFactorCalculado = 1.0; // Default para varios casos
        boolean encajarTotalmente = model.isMantenerProporcion(); // El estado del toggle

        model.resetPan(); // Reseteamos el paneo para la mayoría de los modos automáticos

        // --- SECCIÓN 2: CÁLCULO DEL NUEVO FACTOR DE ZOOM ---
        switch (modoDeseado) {
            case DISPLAY_ORIGINAL:
            	
            	if (encajarTotalmente) { // prop ON
                    if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0 && etiquetaAncho > 0 && etiquetaAlto > 0) {
                      double _w = (double) imgOriginal.getWidth();
                      double _h = (double) imgOriginal.getHeight();
                      double _ew = (double) etiquetaAncho;
                      double _eh = (double) etiquetaAlto;

                      double factorAnchoImg = _ew / _w; // Factor para ajustar al ancho
                      double factorAltoImg = _eh / _h; // Factor para ajustar al alto
                     
                    	
                        double factorParaQueQuepa = Math.min(
                            (double) etiquetaAncho / imgOriginal.getWidth(),
                            (double) etiquetaAlto / imgOriginal.getHeight()
                        );
                        
                      //---- NUEVO LOG DETALLADO ----
                      System.out.println("    [ZM DISPLAY_ORIGINAL prop ON DEBUG]");
                      System.out.println("      imgOriginal: " + imgOriginal.getWidth() + "x" + imgOriginal.getHeight());
                      System.out.println("      etiqueta: " + etiquetaAncho + "x" + etiquetaAlto);
                      System.out.println("      factorAnchoImg: " + factorAnchoImg);
                      System.out.println("      factorAltoImg: " + factorAltoImg);
                      System.out.println("      factorParaQueQuepa: " + factorParaQueQuepa);
                      // ---- FIN NUEVO LOG ----
                        
                        nuevoFactorCalculado = Math.min(1.0, factorParaQueQuepa);
                    } else { nuevoFactorCalculado = 1.0; }
                } else { // prop OFF
                    nuevoFactorCalculado = 1.0; 
                }
                break;
            	
//            	if (encajarTotalmente) { // prop ON
//                    if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0 && etiquetaAncho > 0 && etiquetaAlto > 0) {
//                        double _w = (double) imgOriginal.getWidth();
//                        double _h = (double) imgOriginal.getHeight();
//                        double _ew = (double) etiquetaAncho;
//                        double _eh = (double) etiquetaAlto;
//
//                        double factorAnchoImg = _ew / _w; // Factor para ajustar al ancho
//                        double factorAltoImg = _eh / _h; // Factor para ajustar al alto
//                        
//                        double factorParaQueQuepa = Math.min(factorAnchoImg, factorAltoImg);
//
//                        // ---- NUEVO LOG DETALLADO ----
//                        System.out.println("    [ZM DISPLAY_ORIGINAL prop ON DEBUG]");
//                        System.out.println("      imgOriginal: " + imgOriginal.getWidth() + "x" + imgOriginal.getHeight());
//                        System.out.println("      etiqueta: " + etiquetaAncho + "x" + etiquetaAlto);
//                        System.out.println("      factorAnchoImg: " + factorAnchoImg);
//                        System.out.println("      factorAltoImg: " + factorAltoImg);
//                        System.out.println("      factorParaQueQuepa: " + factorParaQueQuepa);
//                        // ---- FIN NUEVO LOG ----
//
//                        nuevoFactorCalculado = Math.min(1.0, factorParaQueQuepa);
//                    } else { 
//                        System.out.println("    [ZM DISPLAY_ORIGINAL prop ON DEBUG] Dimensiones inválidas para cálculo. Usando factor 1.0");
//                        nuevoFactorCalculado = 1.0; 
//                    }
//                } else { // prop OFF
//                    nuevoFactorCalculado = 1.0;
//                }
//                System.out.println("  [ZoomManager] Modo DISPLAY_ORIGINAL (" + (encajarTotalmente ? "EncajarTotalmente" : "PermitirDesborde") + "): Factor calculado FINAL = " + nuevoFactorCalculado);
//                break;
            	
//            	if (encajarTotalmente) {
//                    // Si queremos que quepa totalmente Y sea lo más cercano al 100%
//                    if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0 && etiquetaAncho > 0 && etiquetaAlto > 0) {
//                        // Factor para que quepa completamente manteniendo proporción
//                        double factorParaQueQuepa = Math.min(
//                            (double) etiquetaAncho / imgOriginal.getWidth(),
//                            (double) etiquetaAlto / imgOriginal.getHeight()
//                        );
//                        // Si 100% (factor 1.0) es más pequeño o igual que el factor para que quepa,
//                        // significa que la imagen al 100% ya cabe o es más pequeña que el visor.
//                        // Entonces, usamos 1.0.
//                        // Si la imagen al 100% es más grande (factor 1.0 > factorParaQueQuepa),
//                        // entonces debemos usar factorParaQueQuepa para reducirla.
//                        nuevoFactorCalculado = Math.min(1.0, factorParaQueQuepa);
//                    } else {
//                        nuevoFactorCalculado = 1.0; // Fallback si dimensiones no válidas
//                    }
//                } else {
//                    // Si encajarTotalmente es false, queremos 100% real, permitiendo desborde.
//                    nuevoFactorCalculado = 1.0;
//                }
//                System.out.println("  [ZoomManager] Modo DISPLAY_ORIGINAL (" + (encajarTotalmente ? "EncajarTotalmente" : "PermitirDesborde") + "): Factor calculado = " + nuevoFactorCalculado);
//                break;

            case FIT_TO_WIDTH:
            	if (imgOriginal.getWidth() > 0 && etiquetaAncho > 0) {
                    nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
                    if (encajarTotalmente && imgOriginal.getHeight() > 0 && etiquetaAlto > 0) {
                        if ((imgOriginal.getHeight() * nuevoFactorCalculado) > etiquetaAlto) {
                            nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
                        }
                    }
                }
                System.out.println("  [ZoomManager] Modo FIT_TO_WIDTH (" + (encajarTotalmente ? "EncajarTotalmente" : "PermitirDesbordeAlto") + "): Factor calculado = " + nuevoFactorCalculado);
                break;

            case FIT_TO_HEIGHT:
            	// (Análogo a FIT_TO_WIDTH)
                if (imgOriginal.getHeight() > 0 && etiquetaAlto > 0) {
                    nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
                    if (encajarTotalmente && imgOriginal.getWidth() > 0 && etiquetaAncho > 0) {
                        if ((imgOriginal.getWidth() * nuevoFactorCalculado) > etiquetaAncho) {
                            nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
                        }
                    }
                }
                System.out.println("  [ZoomManager] Modo FIT_TO_HEIGHT (" + (encajarTotalmente ? "EncajarTotalmente" : "PermitirDesbordeAncho") + "): Factor calculado = " + nuevoFactorCalculado);
                break;

            case FIT_TO_SCREEN: // Tu CMD_ZOOM_TIPO_AJUSTAR
            	if (encajarTotalmente) { // Toggle ON: Encajar manteniendo proporción
                    if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0 && etiquetaAncho > 0 && etiquetaAlto > 0) {
                        double ratioAncho = (double) etiquetaAncho / imgOriginal.getWidth();
                        double ratioAlto = (double) etiquetaAlto / imgOriginal.getHeight();
                        nuevoFactorCalculado = Math.min(ratioAncho, ratioAlto);
                    }
                } else { // Toggle OFF: Estirar para llenar el área
                    nuevoFactorCalculado = 1.0; // ImageDisplayUtils estirará la imagen base
                }
                System.out.println("  [ZoomManager] Modo FIT_TO_SCREEN (" + (encajarTotalmente ? "EncajarTotalmente" : "Estirar") + "): Factor calculado = " + nuevoFactorCalculado);
                break;

            case MAINTAIN_CURRENT_ZOOM:
            	nuevoFactorCalculado = model.getZoomFactor();
                System.out.println("  [ZoomManager] Modo MAINTAIN_CURRENT_ZOOM: Factor se mantiene en " + nuevoFactorCalculado + " (EncajarTotalmente: " + encajarTotalmente + ")");
                break;

            case USER_SPECIFIED_PERCENTAGE:
            	double porcentajeConfigurado = configuration.getDouble("zoom.personalizado.porcentaje", 100.0);
                nuevoFactorCalculado = porcentajeConfigurado / 100.0;
                System.out.println("  [ZoomManager] Modo USER_SPECIFIED_PERCENTAGE: Factor desde config (" + porcentajeConfigurado + "%) = " + nuevoFactorCalculado + " (EncajarTotalmente: " + encajarTotalmente + ")");
                break;
                
            default:
            	System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Modo de zoom no reconocido: " + modoDeseado);
                nuevoFactorCalculado = model.getZoomFactor();
                break;
        }
        
        // --- SECCIÓN 3: APLICAR CAMBIOS AL MODELO ---
        model.setCurrentZoomMode(modoDeseado);
        model.setZoomFactor(nuevoFactorCalculado); 
        System.out.println("  [ZoomManager] Modelo ACTUALIZADO -> Modo: " + model.getCurrentZoomMode() + ", Factor: " + model.getZoomFactor());

        // --- SECCIÓN 4: REFRESCO DE LA VISTA ---
        refrescarVistaPrincipalConEstadoActualDelModelo();
        System.out.println("  [ZoomManager] Vista principal refrescada.");

        // --- SECCIÓN 5: DEVOLVER SI EL MODO CAMBIÓ ---
        return modoAnterior != modoDeseado;
    }
    
    
    public boolean aplicarModoDeZoomOLD(ZoomModeEnum modoDeseado) {
        // --- SECCIÓN 1: VALIDACIONES INICIALES Y PREPARACIÓN ---
        // 1.1. Validar que las dependencias principales (modelo, vista, etiqueta de imagen, configuración) existan.
        //      Si alguna falta, no se puede proceder y se registra un error.
        if (model == null || view == null || view.getEtiquetaImagen() == null || configuration == null) {
            System.err.println("ERROR [ZoomManager.aplicarModoDeZoom]: Dependencias (Modelo, Vista, EtiquetaImagen o Config) nulas. No se puede aplicar modo de zoom.");
            return false; // Indica que no se aplicó ningún cambio.
        }
        
        // 1.2. Obtener la imagen original (BufferedImage) desde el VisorModel.
        BufferedImage imgOriginal = model.getCurrentImage();

        // 1.3. Validar si hay una imagen cargada actualmente en el modelo.
        //      Si no hay imagen, no se puede aplicar un modo de zoom. Se limpia la vista.
        if (imgOriginal == null) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] No hay imagen original en el modelo para aplicar modo de zoom.");
            if (view != null) { // Asegurarse de que la vista exista antes de intentar limpiarla.
                view.limpiarImagenMostrada(); 
            }
            // Si el modo actual era diferente al deseado (aunque no haya imagen), se considera un cambio de "intención".
            // No obstante, es más limpio que el cambio de modo se registre solo si hay imagen.
            // Por ahora, si no hay imagen, no actualizamos el model.currentZoomMode aquí.
            // Si el modo actual del modelo ya era el modoDeseado, devuelve false.
            boolean modoRealmenteCambio = model.getCurrentZoomMode() != modoDeseado;
            if (modoRealmenteCambio) {
                model.setCurrentZoomMode(modoDeseado); // Actualizar la intención del modo
            }
            return modoRealmenteCambio;
        }

        // 1.4. Obtener las dimensiones actuales del componente JLabel donde se muestra la imagen.
        int etiquetaAncho = view.getEtiquetaImagen().getWidth();
        int etiquetaAlto = view.getEtiquetaImagen().getHeight();

        // 1.5. Validar que la etiqueta de imagen tenga dimensiones válidas.
        //      Si la etiqueta aún no se ha renderizado (ancho o alto <= 0), ciertos modos de zoom
        //      (los que se ajustan al tamaño de la etiqueta) no se pueden calcular correctamente.
        //      Se permite continuar solo para modos que no dependen del tamaño de la etiqueta
        //      (como DISPLAY_ORIGINAL, USER_SPECIFIED_PERCENTAGE, MAINTAIN_CURRENT_ZOOM).
        if ((etiquetaAncho <= 0 || etiquetaAlto <= 0) && 
            !(modoDeseado == ZoomModeEnum.DISPLAY_ORIGINAL || 
              modoDeseado == ZoomModeEnum.USER_SPECIFIED_PERCENTAGE ||
              modoDeseado == ZoomModeEnum.MAINTAIN_CURRENT_ZOOM)) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] WARN: EtiquetaImagen sin tamaño válido (" + etiquetaAncho + "x" + etiquetaAlto + 
                               "). No se puede aplicar el modo de zoom de ajuste: " + modoDeseado);
            // No se cambia el modo ni el factor de zoom si no se puede calcular.
            return false; 
        }

        // 1.6. Guardar el modo de zoom anterior para determinar si hubo un cambio real.
        ZoomModeEnum modoAnterior = model.getCurrentZoomMode();

        // 1.7. Variable para el nuevo factor de zoom a calcular. Se inicializa con el factor actual
        //      por si el modo es MAINTAIN_CURRENT_ZOOM o si algún cálculo falla.
        double nuevoFactorCalculado = model.getZoomFactor(); 

        // 1.8. Leer la preferencia de "mantener proporciones" desde el VisorModel.
        boolean mantenerProp = model.isMantenerProporcion();

        // 1.9. Resetear el paneo (desplazamiento X e Y) a (0,0) para la mayoría de los modos de zoom.
        //      Esto centra la imagen después de aplicar el nuevo zoom.
        //      Se podría hacer condicional si algunos modos no deben resetear el pan.
        model.resetPan(); 
        System.out.println("  [ZoomManager] Paneo reseteado a (0,0) para aplicar nuevo modo de zoom.");

        // --- SECCIÓN 2: CÁLCULO DEL NUEVO FACTOR DE ZOOM SEGÚN EL MODO DESEADO ---
        // 2.1. Switch para determinar la lógica de cálculo para cada ZoomModeEnum.
        switch (modoDeseado) {
		    case DISPLAY_ORIGINAL:
		        nuevoFactorCalculado = 1.0;
		        break;
	
		    case FIT_TO_WIDTH:
		        if (imgOriginal.getWidth() > 0) {
		            nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
		            if (mantenerProp && imgOriginal.getHeight() > 0) { // Si "Encajar Siempre" está ON
		                // Y si el alto proporcional se sale de la etiquetaAlto
		                if ((imgOriginal.getHeight() * nuevoFactorCalculado) > etiquetaAlto) {
		                    // Entonces, re-escala para que el alto quepa
		                    nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
		                }
		            }
		            // Si "Encajar Siempre" está OFF (Recortar Permitido), nuevoFactorCalculado se queda 
		            // solo con el ajuste al ancho, permitiendo que el alto se desborde.
		        }
		        break;
	
		    case FIT_TO_HEIGHT:
		        if (imgOriginal.getHeight() > 0) {
		            nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
		            if (mantenerProp && imgOriginal.getWidth() > 0) { // Si "Encajar Siempre" está ON
		                // Y si el ancho proporcional se sale de la etiquetaAncho
		                if ((imgOriginal.getWidth() * nuevoFactorCalculado) > etiquetaAncho) {
		                    // Entonces, re-escala para que el ancho quepa
		                    nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
		                }
		            }
		            // Si "Encajar Siempre" está OFF (Recortar Permitido), nuevoFactorCalculado se queda
		            // solo con el ajuste al alto, permitiendo que el ancho se desborde.
		        }
		        break;
	
		    case FIT_TO_SCREEN: // Este es tu "Ajustar"
		        if (mantenerProp) { // Si el toggle "Encajar Siempre" está ON
		            // Ajustar para que quepa completamente, manteniendo proporción (código actual)
		            if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0) {
		                double ratioAncho = (double) etiquetaAncho / imgOriginal.getWidth();
		                double ratioAlto = (double) etiquetaAlto / imgOriginal.getHeight();
		                nuevoFactorCalculado = Math.min(ratioAncho, ratioAlto);
		            }
		        } else { // Si el toggle "Encajar Siempre" está OFF (es decir, "Recortar Permitido" o "Estirar")
		            // Para el modo "Ajustar", si "Encajar Siempre" está OFF, significa ESTIRAR.
		            // Para estirar, el factor de zoom aplicado a la imagen base (que ya fue ajustada
		            // por ImageDisplayUtils) debe ser 1.0.
		            // ImageDisplayUtils se encargará de estirar si model.isMantenerProporcion() (que es encajarSiempre) es false.
		            nuevoFactorCalculado = 1.0; // El ImageDisplayUtils hará el estiramiento si es necesario.
		        }
		        break;
	
		    // MAINTAIN_CURRENT_ZOOM y USER_SPECIFIED_PERCENTAGE no cambian su lógica de cálculo del factor aquí.
		    // El toggle "Encajar Siempre" / "Recortar Permitido" afectará cómo se renderiza la imagen
		    // final a través de ImageDisplayUtils y el pintado en la vista.
		    case MAINTAIN_CURRENT_ZOOM:
		        // No se cambia nuevoFactorCalculado
		        break;
		    case USER_SPECIFIED_PERCENTAGE:
		        double porcentajeConfigurado = configuration.getDouble("zoom.personalizado.porcentaje", 100.0);
		        nuevoFactorCalculado = porcentajeConfigurado / 100.0;
		        break;
        
//            case DISPLAY_ORIGINAL: // Mostrar la imagen a su tamaño 100% (sin escalar por ajuste).
//                nuevoFactorCalculado = 1.0;
//                System.out.println("  [ZoomManager] Modo DISPLAY_ORIGINAL: Factor calculado = 1.0");
//                break;
//
//            case FIT_TO_WIDTH: // Ajustar la imagen al ancho del área de visualización.
//                if (imgOriginal.getWidth() > 0) { // Evitar división por cero.
//                    nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
//                    // Si se deben mantener proporciones y el alto calculado excede el alto del área,
//                    // entonces se debe ajustar por alto para que quepa completamente.
//                    if (mantenerProp && imgOriginal.getHeight() > 0) {
//                        if ((imgOriginal.getHeight() * nuevoFactorCalculado) > etiquetaAlto) {
//                            nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
//                        }
//                    }
//                    System.out.println("  [ZoomManager] Modo FIT_TO_WIDTH: Factor calculado = " + nuevoFactorCalculado);
//                } else {
//                    System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Ancho de imagen original es 0 para FIT_TO_WIDTH.");
//                }
//                break;
//
//            case FIT_TO_HEIGHT: // Ajustar la imagen al alto del área de visualización.
//                if (imgOriginal.getHeight() > 0) { // Evitar división por cero.
//                    nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
//                    // Si se deben mantener proporciones y el ancho calculado excede el ancho del área,
//                    // entonces se debe ajustar por ancho para que quepa completamente.
//                    if (mantenerProp && imgOriginal.getWidth() > 0) {
//                        if ((imgOriginal.getWidth() * nuevoFactorCalculado) > etiquetaAncho) {
//                            nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
//                        }
//                    }
//                    System.out.println("  [ZoomManager] Modo FIT_TO_HEIGHT: Factor calculado = " + nuevoFactorCalculado);
//                } else {
//                    System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Alto de imagen original es 0 para FIT_TO_HEIGHT.");
//                }
//                break;
//
//            case FIT_TO_SCREEN: // Ajustar la imagen para que quepa completamente en el área (sin recortes).
//                if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0) { // Evitar división por cero.
//                    double ratioAncho = (double) etiquetaAncho / imgOriginal.getWidth();
//                    double ratioAlto = (double) etiquetaAlto / imgOriginal.getHeight();
//                    nuevoFactorCalculado = Math.min(ratioAncho, ratioAlto); // Usar el ratio menor para asegurar que toda la imagen sea visible.
//                    System.out.println("  [ZoomManager] Modo FIT_TO_SCREEN: Factor calculado = " + nuevoFactorCalculado);
//                } else {
//                    System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Dimensiones de imagen original inválidas para FIT_TO_SCREEN.");
//                }
//                break;
//
//            case MAINTAIN_CURRENT_ZOOM: // Mantener el factor de zoom actual (útil al cambiar de imagen).
//                // No se cambia `nuevoFactorCalculado`, ya que se inicializó con `model.getZoomFactor()`.
//                System.out.println("  [ZoomManager] Modo MAINTAIN_CURRENT_ZOOM: Factor actual (" + model.getZoomFactor() + ") se mantiene.");
//                break;
//
//            case USER_SPECIFIED_PERCENTAGE: // Aplicar un porcentaje de zoom fijo leído de la configuración.
//                double porcentajeConfigurado = configuration.getDouble("zoom.personalizado.porcentaje", 100.0); // Clave de config
//                nuevoFactorCalculado = porcentajeConfigurado / 100.0;
//                System.out.println("  [ZoomManager] Modo USER_SPECIFIED_PERCENTAGE: Factor calculado desde config (" + porcentajeConfigurado + "%) = " + nuevoFactorCalculado);
//                break;
                
            // case FILL_SCREEN: // (Si se implementa en el futuro) Ajustar para que la imagen llene el área, permitiendo recortes.
            //     if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0) {
            //         double ratioAncho = (double) etiquetaAncho / imgOriginal.getWidth();
            //         double ratioAlto = (double) etiquetaAlto / imgOriginal.getHeight();
            //         nuevoFactorCalculado = Math.max(ratioAncho, ratioAlto); // Usar el ratio mayor para llenar.
            //         System.out.println("  [ZoomManager] Modo FILL_SCREEN: Factor calculado = " + nuevoFactorCalculado);
            //     } else {
            //          System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Dimensiones de imagen original inválidas para FILL_SCREEN.");
            //     }
            //     break;

            default:
                System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Modo de zoom no reconocido o no implementado: " + modoDeseado + ". No se cambia el factor de zoom.");
                // No se modifica nuevoFactorCalculado, se usará el que ya tenía el modelo.
                break;
        }
        
        // --- SECCIÓN 3: APLICAR CAMBIOS AL MODELO ---
        // 3.1. Actualizar el modo de zoom actual en el modelo.
        model.setCurrentZoomMode(modoDeseado);
        
        // 3.2. Aplicar el nuevo factor de zoom calculado al modelo.
        //      El método setZoomFactor en VisorModel ya debería aplicar límites (ej. 0.01 a 20.0).
        model.setZoomFactor(nuevoFactorCalculado); 
        System.out.println("  [ZoomManager] Estado del Modelo ACTUALIZADO -> Modo: " + model.getCurrentZoomMode() + ", Factor: " + model.getZoomFactor());

        // --- SECCIÓN 4: REFRESCO DE LA VISTA ---
        // 4.1. Solicitar al propio ZoomManager que refresque la imagen principal en la VisorView.
        //      Este método utiliza ImageDisplayUtils para el reescalado base y luego aplica
        //      el zoomFactor y offsets actuales del modelo para el pintado final en la vista.
        refrescarVistaPrincipalConEstadoActualDelModelo();
        System.out.println("  [ZoomManager] Vista principal refrescada.");

        // --- SECCIÓN 5: DEVOLVER SI EL MODO CAMBIÓ ---
        // 5.1. Comprobar si el modo de zoom en el modelo es diferente del que tenía antes de esta llamada.
        return modoAnterior != modoDeseado;
    }// FIN del metodo aplicarModoDeZoom
    
    
    
    /**
     * Aplica un modo de zoom específico (ej. ajustar a ancho, alto, pantalla).
     * Calcula el factor de zoom necesario, actualiza el modelo y luego
     * solicita un refresco de la vista.
     * @param modoDeseado El tipo de zoom a aplicar, según la enum {@link ZoomModeEnum}.
     */
    public void aplicarModoDeZoomVERYOLD(ZoomModeEnum modoDeseado) {
        // 5.1. Validaciones iniciales.
        if (model == null || view == null || view.getEtiquetaImagen() == null || configuration == null) {
            System.err.println("ERROR [ZoomManager.aplicarModoDeZoom]: Dependencias (Modelo, Vista, EtiquetaImagen o Config) nulas.");
            return;
        }
        
        BufferedImage imgOriginal = model.getCurrentImage();
        if (imgOriginal == null) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] No hay imagen original en el modelo para aplicar modo de zoom.");
            if (view != null) view.limpiarImagenMostrada(); // Limpiar vista si no hay imagen
            return;
        }

        // 5.2. Obtener dimensiones del área de visualización.
        int etiquetaAncho = view.getEtiquetaImagen().getWidth();
        int etiquetaAlto = view.getEtiquetaImagen().getHeight();

        // Si la etiqueta aún no tiene dimensiones (ej. al inicio), no se puede calcular el zoom de ajuste.
        if (etiquetaAncho <= 0 || etiquetaAlto <= 0) {
            System.out.println("[ZoomManager.aplicarModoDeZoom] WARN: EtiquetaImagen sin tamaño válido (" + etiquetaAncho + "x" + etiquetaAlto + "). No se puede aplicar modo de zoom de ajuste.");
            // Podríamos optar por un zoom de 100% como fallback o simplemente no hacer nada.
            // Si se aplica DISPLAY_ORIGINAL o USER_SPECIFIED, estos podrían funcionar.
            if (modoDeseado != ZoomModeEnum.DISPLAY_ORIGINAL && modoDeseado != ZoomModeEnum.USER_SPECIFIED_PERCENTAGE && modoDeseado != ZoomModeEnum.MAINTAIN_CURRENT_ZOOM) {
                 return;
            }
        }

        // 5.3. Variable para el nuevo factor de zoom.
        double nuevoFactorCalculado = model.getZoomFactor(); // Por defecto, no cambiar si el modo no lo especifica.
        // 5.4. Leer la preferencia de mantener proporciones del modelo.
        boolean mantenerProp = model.isMantenerProporcion();

        // 5.5. La mayoría de los modos de zoom predefinidos también resetean el paneo para centrar la imagen.
        model.resetPan(); 

        // 5.6. Calcular el nuevo factor de zoom según el modo deseado.
        switch (modoDeseado) {
            case DISPLAY_ORIGINAL: // Equivalente a "Zoom Automático" si este es 100%
                nuevoFactorCalculado = 1.0;
                break;
            case FIT_TO_WIDTH:
                if (imgOriginal.getWidth() > 0) { // Evitar división por cero
                    nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
                    if (mantenerProp && imgOriginal.getHeight() > 0) {
                        // Si al ajustar al ancho, el alto se sale, entonces ajustar por alto también.
                        if ((imgOriginal.getHeight() * nuevoFactorCalculado) > etiquetaAlto) {
                            nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
                        }
                    }
                }
                break;
            case FIT_TO_HEIGHT:
                if (imgOriginal.getHeight() > 0) { // Evitar división por cero
                    nuevoFactorCalculado = (double) etiquetaAlto / imgOriginal.getHeight();
                    if (mantenerProp && imgOriginal.getWidth() > 0) {
                        // Si al ajustar al alto, el ancho se sale, entonces ajustar por ancho también.
                        if ((imgOriginal.getWidth() * nuevoFactorCalculado) > etiquetaAncho) {
                            nuevoFactorCalculado = (double) etiquetaAncho / imgOriginal.getWidth();
                        }
                    }
                }
                break;
            case FIT_TO_SCREEN: // Ajustar para que la imagen quepa completamente
                if (imgOriginal.getWidth() > 0 && imgOriginal.getHeight() > 0) {
                    double ratioAncho = (double) etiquetaAncho / imgOriginal.getWidth();
                    double ratioAlto = (double) etiquetaAlto / imgOriginal.getHeight();
                    nuevoFactorCalculado = Math.min(ratioAncho, ratioAlto); // Usar el menor para que quepa
                }
                break;
            case MAINTAIN_CURRENT_ZOOM: // "Zoom Fijo" al cambiar de imagen
                // Este modo no cambia el zoom actual, sino que lo preserva para la siguiente imagen.
                // La lógica de aplicación real de este modo ocurre cuando se carga una NUEVA imagen.
                // Al seleccionar este modo para la imagen ACTUAL, simplemente no se cambia el zoom.
                System.out.println("  [ZoomManager] Modo MAINTAIN_CURRENT_ZOOM seleccionado. Factor actual se mantiene.");
                // No se hace nada al factor, se usará el que ya tiene el modelo.
                break;
            case USER_SPECIFIED_PERCENTAGE: // "Zoom Fijado" a un %
                // Leer el porcentaje de la configuración.
                double porcentajeConfigurado = configuration.getDouble("zoom.personalizado.porcentaje", 100.0);
                nuevoFactorCalculado = porcentajeConfigurado / 100.0;
                break;
            default:
                System.err.println("WARN [ZoomManager.aplicarModoDeZoom]: Modo de zoom no reconocido: " + modoDeseado);
                // Mantener el factor actual o volver a 1.0 como fallback.
                // nuevoFactorCalculado = 1.0;
                break;
        }
        
        // 5.7. Aplicar el nuevo factor de zoom (con límites) al modelo.
        model.setZoomFactor(Math.max(0.01, nuevoFactorCalculado)); // Evitar zoom <= 0. VisorModel.setZoomFactor también tiene límites.
        System.out.println("  [ZoomManager] Modo de zoom '" + modoDeseado + "' aplicado. Nuevo factor en modelo: " + model.getZoomFactor());

        // 5.8. Refrescar la vista principal para mostrar los cambios.
        refrescarVistaPrincipalConEstadoActualDelModelo();
       
    }// FIN del metodo aplicarModoDeZoom

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
        if (model == null || view == null) return;
        BufferedImage imgOriginalDelModelo = model.getCurrentImage();
        if (imgOriginalDelModelo == null) {
            view.limpiarImagenMostrada();
            return;
        }
        Image imagenBaseParaVista = ImageDisplayUtils.reescalarImagenParaAjustar(imgOriginalDelModelo, model, view);
        if (imagenBaseParaVista != null) {
            view.setImagenMostrada(imagenBaseParaVista, model.getZoomFactor(), model.getImageOffsetX(), model.getImageOffsetY());
        } else {
            view.limpiarImagenMostrada();
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
    
    
} // --- FIN CLASE ZoomManager ---