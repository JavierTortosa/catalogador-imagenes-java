package modelo;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
// --- INICIO DE MODIFICACIÓN: AÑADIR IMPORTS ---
import java.util.ArrayList;
import java.util.List;
// --- FIN DE MODIFICACIÓN ---
import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicios.zoom.ZoomModeEnum;

public class VisorModel {

	private static final Logger logger = LoggerFactory.getLogger(VisorModel.class);
	
    // --- Enum para definir los modos de trabajo ---
    public enum WorkMode {
        VISUALIZADOR,
        PROYECTO,
        DATOS,
        EDICION,
        CARROUSEL
    }
    
    public enum DisplayMode {
        SINGLE_IMAGE, // Vista de una única imagen
        GRID,         // Vista de cuadrícula de miniaturas
        POLAROID      // Vista de imagen única con estilo "polaroid"
    }
    
    public enum SortDirection {
        NONE,       // Sin orden específico (orden de lectura del disco)
        ASCENDING,  // Orden ascendente (A-Z)
        DESCENDING  // Orden descendente (Z-A)
    }

    private WorkMode currentWorkMode;
    private DisplayMode currentDisplayMode;
    private SortDirection sortDirection = SortDirection.NONE;
    
    private ListContext visualizadorListContext;
    private ListContext proyectoListContext;
    private ListContext datosListContext;
    private ListContext carouselListContext;

    private ZoomContext carouselZoomContext;
    private ZoomContext visualizadorZoomContext;
    private ZoomContext proyectoZoomContext;
    private ZoomContext datosZoomContext;
    
    private BufferedImage currentImage;
    
    private int miniaturasAntes;
    private int miniaturasDespues;
    private int miniaturaSelAncho;
    private int miniaturaSelAlto;
    private int miniaturaNormAncho;
    private int miniaturaNormAlto;
    private int saltoDeBloque;
    private int carouselDelay;
    
    private boolean navegacionCircularActivada = false;
    private boolean enModoProyecto = false;
    private boolean modoPantallaCompletaActivado = false;
    private boolean carouselShuffleEnabled = false;
    private boolean syncVisualizadorCarrusel = false;
    private double zoomCustomPercentage = 100.0;

    private Path ultimaCarpetaCarrusel;
    private String ultimaImagenKeyCarrusel; 
    private boolean liveFilterActive = false;
    
    // =========================================================================
    // === INICIO DE MODIFICACIÓN: AÑADIR SISTEMA DE EVENTOS ===
    // =========================================================================
    
    private final List<MasterListChangeListener> masterListListeners = new ArrayList<>();

    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================
    
    
    public VisorModel() {
        this.currentWorkMode = WorkMode.VISUALIZADOR;
        this.currentDisplayMode = DisplayMode.SINGLE_IMAGE; 
        this.visualizadorListContext = new ListContext();
        this.proyectoListContext = new ListContext();
        this.datosListContext = new ListContext();
        this.carouselListContext = new ListContext();
        this.carouselZoomContext = new ZoomContext();
        this.visualizadorZoomContext = new ZoomContext();
        this.proyectoZoomContext = new ZoomContext();
        this.datosZoomContext = new ZoomContext();
        this.currentImage = null;
        this.saltoDeBloque = 10;
        this.carouselDelay = 3000;
    } // FIN del constructor

    // Añadimos el nuevo parámetro 'zoomAlCursor' a la firma del método
    public void initializeContexts(
    		boolean mantenerPropInicial, 
    		boolean soloCarpetaInicial, 
    		ZoomModeEnum modoZoomInicial, 
    		boolean zoomManualInicial, 
    		boolean navCircularInicial, 
    		boolean zoomAlCursor
    ) {
        logger.info("[VisorModel] Inicializando estado por defecto de todos los contextos...");

        this.visualizadorZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.visualizadorZoomContext.setZoomMode(modoZoomInicial);
        this.visualizadorZoomContext.setZoomHabilitado(zoomManualInicial);
        this.visualizadorZoomContext.setZoomToCursorEnabled(zoomAlCursor); // Asignar el nuevo estado
        
        this.proyectoZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.proyectoZoomContext.setZoomMode(modoZoomInicial);
        this.proyectoZoomContext.setZoomHabilitado(zoomManualInicial);
        this.proyectoZoomContext.setZoomToCursorEnabled(zoomAlCursor); // Asignar el nuevo estado
        
        this.datosZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.datosZoomContext.setZoomMode(modoZoomInicial);
        this.datosZoomContext.setZoomHabilitado(zoomManualInicial);
        this.datosZoomContext.setZoomToCursorEnabled(zoomAlCursor); // Asignar el nuevo estado

        this.carouselZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.carouselZoomContext.setZoomMode(modoZoomInicial);
        this.carouselZoomContext.setZoomHabilitado(zoomManualInicial);
        this.carouselZoomContext.setZoomToCursorEnabled(zoomAlCursor);
        
        this.visualizadorListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.proyectoListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.datosListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        
        this.navegacionCircularActivada = navCircularInicial;
    }
    
    // =========================================================================
    // === INICIO DE MODIFICACIÓN: AÑADIR MÉTODOS PARA GESTIONAR OYENTES Y EVENTOS ===
    // =========================================================================

    /**
     * Registra un oyente que será notificado de los cambios en la lista maestra.
     * @param listener El oyente a añadir.
     */
    public void addMasterListChangeListener(MasterListChangeListener listener) {
        if (!masterListListeners.contains(listener)) {
            masterListListeners.add(listener);
        }
    } // --- Fin del método addMasterListChangeListener ---

    /**
     * Elimina un oyente de la lista de notificaciones.
     * @param listener El oyente a eliminar.
     */
    public void removeMasterListChangeListener(MasterListChangeListener listener) {
        masterListListeners.remove(listener);
    } // --- Fin del método removeMasterListChangeListener ---
    
    /**
     * Reemplaza la lista maestra y el mapa de rutas para el contexto de trabajo actual,
     * y notifica a todos los oyentes registrados sobre este cambio fundamental.
     * Este es el método PREFERIDO para cambiar la lista de datos.
     * 
     * @param nuevoModelo El nuevo DefaultListModel que actuará como lista maestra.
     * @param nuevoMapaRutas El mapa de rutas correspondiente al nuevo modelo.
     * @param source El objeto controlador que inició el cambio (para depuración).
     */
    public void setMasterListAndNotify(DefaultListModel<String> nuevoModelo, Map<String, Path> nuevoMapaRutas, Object source) {
        logger.debug("[Model] Estableciendo nueva lista maestra para el modo: " + this.currentWorkMode);
        ListContext currentContext = getCurrentListContext();
        if (currentContext != null) {
            currentContext.actualizarContextoCompleto(nuevoModelo, nuevoMapaRutas);
            
            // ¡EL GRITO! Notificamos a todos los oyentes que la lista ha cambiado.
            logger.debug("[Model] Notificando a " + masterListListeners.size() + " oyentes sobre el cambio de la lista maestra.");
            for (MasterListChangeListener listener : masterListListeners) {
                listener.onMasterListChanged(nuevoModelo, source);
            }
        }
    } // --- Fin del método setMasterListAndNotify ---

    // =========================================================================
    // === FIN DE MODIFICACIÓN ===
    // =========================================================================

    public WorkMode getCurrentWorkMode() {return this.currentWorkMode;}    
    public void setCurrentWorkMode(WorkMode newMode) {
        if (this.currentWorkMode != newMode) {
            logger.debug("[Model] Cambiando modo de trabajo de " + this.currentWorkMode + " a: " + newMode);
            this.currentWorkMode = newMode;
            this.setCurrentImage(null);
            this.enModoProyecto = (newMode == WorkMode.PROYECTO);
        }
    }

    
    public DisplayMode getCurrentDisplayMode() {
        ListContext currentContext = getCurrentListContext();
        // Si hay un contexto activo, su displayMode tiene prioridad.
        if (currentContext != null) {
            return currentContext.getDisplayMode();
        }
        // Si no hay contexto (muy raro, pero posible durante la inicialización),
        // devolvemos el valor de fallback.
        logger.warn("[VisorModel] getCurrentDisplayMode() llamado sin un ListContext activo. Devolviendo fallback.");
        return this.currentDisplayMode; // 'this.currentDisplayMode' es el de fallback
    }

    public void setCurrentDisplayMode(DisplayMode newDisplayMode) {
        ListContext currentContext = getCurrentListContext();
        
        // La acción principal es actualizar el contexto activo.
        if (currentContext != null && currentContext.getDisplayMode() != newDisplayMode) {
            logger.debug("[Model] Cambiando DisplayMode para contexto " + currentWorkMode + " a: " + newDisplayMode);
            currentContext.setDisplayMode(newDisplayMode);
        }
        
        // Además, actualizamos la variable de fallback para mantenerla sincronizada.
        // Esto es útil para cuando se clonan contextos o se inicializa la app.
        this.currentDisplayMode = newDisplayMode;
    }

    
    public ListContext getCurrentListContext() {
        switch (this.currentWorkMode) {
            case PROYECTO: return this.proyectoListContext;
            case DATOS: return this.datosListContext;
            case CARROUSEL: return this.carouselListContext; // <<< AÑADIDO
            case VISUALIZADOR: 
            default: 
                return this.visualizadorListContext;
        }
    } // --- Fin del método getCurrentListContext ---

    public ZoomContext getCurrentZoomContext() {
        switch (this.currentWorkMode) {
            case PROYECTO: return this.proyectoZoomContext;
            case DATOS: return this.datosZoomContext;
            case CARROUSEL: return this.carouselZoomContext; // <<< AÑADIDO
            case VISUALIZADOR: 
            default: 
                return this.visualizadorZoomContext;
        }
    } // --- Fin del método getCurrentZoomContext --

    
    public void actualizarListaCompleta(DefaultListModel<String> nuevoModelo, Map<String, java.nio.file.Path> nuevoMapaRutas) {
        logger.debug("[Model] Actualizando contexto de lista para el modo: " + this.currentWorkMode);
        getCurrentListContext().actualizarContextoCompleto(nuevoModelo, nuevoMapaRutas);
    }

    public String getSelectedImageKey() { return getCurrentListContext().getSelectedImageKey(); }
    public void setSelectedImageKey(String selectedImageKey) { getCurrentListContext().setSelectedImageKey(selectedImageKey); }
    public ZoomModeEnum getCurrentZoomMode() { return getCurrentZoomContext().getZoomMode(); }
    public void setCurrentZoomMode(ZoomModeEnum newMode) { getCurrentZoomContext().setZoomMode(newMode); }
    public int getImageOffsetX() { return getCurrentZoomContext().getImageOffsetX(); }
    public void setImageOffsetX(int x) { getCurrentZoomContext().setImageOffsetX(x); }
    public void addImageOffsetX(int deltaX) { setImageOffsetX(getImageOffsetX() + deltaX); }
    public int getImageOffsetY() { return getCurrentZoomContext().getImageOffsetY(); }
    public void setImageOffsetY(int y) { getCurrentZoomContext().setImageOffsetY(y); }
    public void addImageOffsetY(int deltaY) { setImageOffsetY(getImageOffsetY() + deltaY); }
    public BufferedImage getCurrentImage() { return currentImage; }
    public void setCurrentImage(BufferedImage currentImage) { this.currentImage = currentImage; }
    public int getMiniaturasAntes() { return miniaturasAntes; }
    public void setMiniaturasAntes(int val) { this.miniaturasAntes = val; }
    public int getMiniaturasDespues() { return miniaturasDespues; }
    public void setMiniaturasDespues(int val) { this.miniaturasDespues = val; }
    public int getMiniaturaSelAncho() { return miniaturaSelAncho; }
    public void setMiniaturaSelAncho(int val) { this.miniaturaSelAncho = val; }
    public int getMiniaturaSelAlto() { return miniaturaSelAlto; }
    public void setMiniaturaSelAlto(int val) { this.miniaturaSelAlto = val; }
    public int getMiniaturaNormAncho() { return miniaturaNormAncho; }
    public void setMiniaturaNormAncho(int val) { this.miniaturaNormAncho = val; }
    public int getMiniaturaNormAlto() { return miniaturaNormAlto; }
    public void setMiniaturaNormAlto(int val) { this.miniaturaNormAlto = val; }
    public boolean isEnModoProyecto() { return enModoProyecto; }
    public void setEnModoProyecto(boolean enModoProyecto) { setCurrentWorkMode(enModoProyecto ? WorkMode.PROYECTO : WorkMode.VISUALIZADOR); }
    public int getSaltoDeBloque() { return saltoDeBloque; }
    public void setSaltoDeBloque(int salto) { this.saltoDeBloque = salto; }
    public ListContext getCarouselListContext() {return this.carouselListContext;} 
    public ListContext getVisualizadorListContext() {return this.visualizadorListContext;}
    public DefaultListModel<String> getModeloLista() {return getCurrentListContext().getModeloLista();}
    public Map<String, Path> getRutaCompletaMap() { return getCurrentListContext().getRutaCompletaMap(); }
    public Path getRutaCompleta(String key) { return getCurrentListContext().getRutaCompleta(key); }
    public void setZoomHabilitado(boolean b) { getCurrentZoomContext().setZoomHabilitado(b); }
    public boolean isZoomHabilitado() { return getCurrentZoomContext().isZoomHabilitado(); }
    public void resetPan() { getCurrentZoomContext().resetPan(); }
    public void resetZoomState() {this.setZoomFactor(1.0); this.resetPan();}
    public ListContext getProyectoListContext() { return this.proyectoListContext; }
    
    public double getZoomFactor() { return getCurrentZoomContext().getZoomFactor(); }
    public void setZoomFactor(double zoomFactor) {
        double validFactor = Math.max(0.01, Math.min(zoomFactor, 50.0));
        getCurrentZoomContext().setZoomFactor(validFactor);
    } // --- Fin del método setZoomFactor ---
    
    public int getCarouselDelay() {return this.carouselDelay;} 
    public void setCarouselDelay(int delayMs) {
        int sign = (int) Math.signum(delayMs); // Guardamos el signo: 1, -1, o 0
        if (sign == 0) sign = 1; // Si es 0, lo tratamos como positivo

        int absoluteDelay = Math.abs(delayMs); // Trabajamos con el valor absoluto

        // Validamos el valor absoluto para que esté en el rango permitido
        int clampedAbsoluteDelay = Math.max(500, Math.min(absoluteDelay, 30000)); // Límite entre 0.5s y 30s

        // Reaplicamos el signo original al valor validado
        this.carouselDelay = clampedAbsoluteDelay * sign;
        
        logger.debug("[VisorModel] Retardo del carrusel actualizado a: " + this.carouselDelay + "ms");
    } // --- Fin del método setCarouselDelay ---

    public boolean isMostrarSoloCarpetaActual() { return getCurrentListContext().isMostrarSoloCarpetaActual(); }
    public void setMostrarSoloCarpetaActual(boolean mostrarSoloCarpetaActual) {
        if (isMostrarSoloCarpetaActual() != mostrarSoloCarpetaActual) {
            getCurrentListContext().setMostrarSoloCarpetaActual(mostrarSoloCarpetaActual);
            logger.debug("  [Model " + currentWorkMode + "] Estado mostrarSoloCarpetaActual cambiado a: " + mostrarSoloCarpetaActual);
        }
    } // --- Fin del método setMostrarSoloCarpetaActual ---

    public boolean isMantenerProporcion() { return getCurrentZoomContext().isMantenerProporcion(); }
    public void setMantenerProporcion(boolean mantenerProporcion) {
        if (isMantenerProporcion() != mantenerProporcion) {
            getCurrentZoomContext().setMantenerProporcion(mantenerProporcion);
            logger.debug("  [Model " + currentWorkMode + "] Estado mantenerProporcion cambiado a: " + mantenerProporcion);
        }
    } // --- Fin del método setMantenerProporcion ---

    public Path getCarpetaRaizActual() {return getCurrentListContext().getCarpetaRaizContexto();}
    public void setCarpetaRaizActual(Path carpetaRaiz) {
        ListContext currentContext = getCurrentListContext();
        if (currentContext != null) {
            if (!Objects.equals(currentContext.getCarpetaRaizContexto(), carpetaRaiz)) {
                logger.debug("  [VisorModel] Carpeta raíz para contexto " + currentWorkMode + " cambiada a '" + carpetaRaiz + "'");
                currentContext.setCarpetaRaizContexto(carpetaRaiz);
            }
        }
    } // --- Fin del método setCarpetaRaizActual ---
    
    public boolean isNavegacionCircularActivada() { return navegacionCircularActivada; }
    public void setNavegacionCircularActivada(boolean activada) {
        if (this.navegacionCircularActivada != activada) {
            this.navegacionCircularActivada = activada;
            logger.debug("  [VisorModel] Navegación Circular cambiada a: " + activada);
        }
    } // --- Fin del método setNavegacionCircularActivada ---
    
    public boolean isZoomToCursorEnabled() { return getCurrentZoomContext().isZoomToCursorEnabled(); }
    public void setZoomToCursorEnabled(boolean enabled) {
        if (isZoomToCursorEnabled() != enabled) {
            getCurrentZoomContext().setZoomToCursorEnabled(enabled);
            logger.debug("  [Model " + currentWorkMode + "] Estado zoomToCursorEnabled cambiado a: " + enabled);
        }
    } // --- Fin del método setZoomToCursorEnabled ---
    
    
    public boolean isModoPantallaCompletaActivado() {return this.modoPantallaCompletaActivado;}
    public void setModoPantallaCompletaActivado(boolean activado) {
        if (this.modoPantallaCompletaActivado != activado) {
            this.modoPantallaCompletaActivado = activado;
            logger.debug("  [VisorModel] Estado de pantalla completa cambiado a: " + activado);
        }
    } // --- Fin del método setModoPantallaCompletaActivado ---
    
    public Path getCarpetaRaizDelVisualizador() {return (this.visualizadorListContext != null) ? this.visualizadorListContext.getCarpetaRaizContexto() : null;}
    public void setCarpetaRaizInicialParaVisualizador(Path carpetaRaiz) {
        if (this.visualizadorListContext != null) {
            this.visualizadorListContext.setCarpetaRaizContexto(carpetaRaiz);
            logger.debug("  [VisorModel] Carpeta raíz INICIAL para contexto VISUALIZADOR establecida a: " + carpetaRaiz);
        }
    } // --- Fin del método setCarpetaRaizInicialParaVisualizador ---
    
    
    public boolean isCarouselShuffleEnabled() {return this.carouselShuffleEnabled;}
    public void setCarouselShuffleEnabled(boolean enabled) {
        this.carouselShuffleEnabled = enabled;
        logger.debug("[VisorModel] Modo aleatorio del carrusel: " + (enabled ? "ACTIVADO" : "DESACTIVADO"));
    } // --- Fin del método setCarouselShuffleEnabled ---

    
    public boolean isSyncVisualizadorCarrusel() {return this.syncVisualizadorCarrusel;}
    public void setSyncVisualizadorCarrusel(boolean syncActivado) {
        if (this.syncVisualizadorCarrusel != syncActivado) {
            this.syncVisualizadorCarrusel = syncActivado;
            logger.debug("[VisorModel] Sincronización Visor<->Carrusel cambiada a: " + (syncActivado ? "ACTIVADO" : "DESACTIVADO"));
        }
    } // --- Fin del método setSyncVisualizadorCarrusel ---

    public double getZoomCustomPercentage() {return this.zoomCustomPercentage;}
    public void setZoomCustomPercentage(double percentage) {
    	// Podríamos añadir validación aquí si quisiéramos (ej: que no sea negativo)
    	this.zoomCustomPercentage = percentage;
    	logger.debug("  [VisorModel] zoomCustomPercentage actualizado a: " + this.zoomCustomPercentage + "%");
    } // --- Fin del método setZoomCustomPercentage ---
    	
    public void setInitialDisplayMode(DisplayMode initialDisplayMode) {
        logger.debug("[Model] Estableciendo DisplayMode inicial a: " + initialDisplayMode);
        this.currentDisplayMode = initialDisplayMode;
    } // --- FIN del metodo setInitialDisplayMode ---
    
    public Path getUltimaCarpetaCarrusel() {return this.ultimaCarpetaCarrusel;}
    public void setUltimaCarpetaCarrusel(Path path) {this.ultimaCarpetaCarrusel = path;}
    public String getUltimaImagenKeyCarrusel() {return this.ultimaImagenKeyCarrusel;}
    public void setUltimaImagenKeyCarrusel(String key) {this.ultimaImagenKeyCarrusel = key;}
    public boolean isLiveFilterActive() {return this.liveFilterActive;}
    public void setLiveFilterActive(boolean liveFilterActive) {this.liveFilterActive = liveFilterActive;}
    
    
    public SortDirection getSortDirection() {return sortDirection;}
    public void setSortDirection(SortDirection newSortDirection) {
        SortDirection oldSortDirection = this.sortDirection;
        this.sortDirection = newSortDirection;
        // (Opcional pero recomendado) Si usas PropertyChangeSupport para notificar a la UI:
        // firePropertyChange("sortDirection", oldSortDirection, newSortDirection);
    } // --- FIN del metodo setSortDirection ---
    
} // --- FIN DE LA CLASE VisorModel ---
