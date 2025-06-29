package modelo;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultListModel;

import servicios.zoom.ZoomModeEnum;

/**
 * Modelo de datos para el Visor de Imágenes. Contiene el estado de la aplicación,
 * ahora con contextos de trabajo separados para el zoom y las listas de imágenes.
 */
public class VisorModel {

    // --- Enum para definir los modos de trabajo ---
    public enum WorkMode {
        VISUALIZADOR,
        PROYECTO,
        DATOS
    }

    // --- CAMBIO: El estado de la lista y la imagen actual ahora está contextualizado ---
    private WorkMode currentWorkMode;
    private ListContext visualizadorListContext;
    private ListContext proyectoListContext;
    private ListContext datosListContext; // Preparado para el futuro

    private ZoomContext visualizadorZoomContext;
    private ZoomContext proyectoZoomContext;
    private ZoomContext datosZoomContext;
    
    // La imagen actual es global, ya que solo hay un panel de visualización principal a la vez.
    private BufferedImage currentImage;
    
    // --- Estado de Configuración/Comportamiento Global (no cambia) ---
    private int miniaturasAntes;
    private int miniaturasDespues;
    private int miniaturaSelAncho;
    private int miniaturaSelAlto;
    private int miniaturaNormAncho;
    private int miniaturaNormAlto;
    private Path carpetaRaizActual = null;
    private boolean navegacionCircularActivada = false;
    
    // --- CAMBIO: Los campos para guardar/restaurar manualmente ya no son necesarios ---
    // private DefaultListModel<String> modeloListaCarpetaGuardado;
    // private Map<String, Path> mapaRutasCarpetaGuardado; 
    // private String claveSeleccionadaCarpetaGuardada;

    // --- Flag de compatibilidad. Usar preferentemente currentWorkMode ---
    private boolean enModoProyecto = false; 

    // --- Constructor ---
    public VisorModel() {
        // Inicialización de modo y contextos
        this.currentWorkMode = WorkMode.VISUALIZADOR;
        
        // --- CAMBIO: Inicializar los nuevos contextos de lista ---
        this.visualizadorListContext = new ListContext();
        this.proyectoListContext = new ListContext();
        this.datosListContext = new ListContext();

        this.visualizadorZoomContext = new ZoomContext();
        this.proyectoZoomContext = new ZoomContext();
        this.datosZoomContext = new ZoomContext();
        
        this.currentImage = null;
        
    } // --- Fin del método VisorModel (constructor) ---

    // --- GESTIÓN DE MODO Y CONTEXTO ---
    
    /**
     * Inicializa el estado por defecto de TODOS los contextos de trabajo
     * basándose en los valores de configuración pasados.
     * Este método debe ser llamado por el inicializador de la aplicación.
     *
     * @param mantenerPropInicial Estado inicial para 'mantener proporciones'.
     * @param soloCarpetaInicial Estado inicial para 'mostrar solo carpeta actual'.
     * @param modoZoomInicial Estado inicial para el modo de zoom.
     * @param zoomManualInicial Estado inicial para el permiso de zoom manual.
     * @param navCircularInicial Estado inicial para la navegación circular.
     */
    public void initializeContexts(boolean mantenerPropInicial, boolean soloCarpetaInicial, ZoomModeEnum modoZoomInicial, boolean zoomManualInicial, boolean navCircularInicial) {
        System.out.println("[VisorModel] Inicializando estado por defecto de todos los contextos...");

        // Aplicar a contextos de Zoom
        this.visualizadorZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.visualizadorZoomContext.setZoomMode(modoZoomInicial);
        this.visualizadorZoomContext.setZoomHabilitado(zoomManualInicial);
        
        this.proyectoZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.proyectoZoomContext.setZoomMode(modoZoomInicial);
        this.proyectoZoomContext.setZoomHabilitado(zoomManualInicial);
        
        this.datosZoomContext.setMantenerProporcion(mantenerPropInicial);
        this.datosZoomContext.setZoomMode(modoZoomInicial);
        this.datosZoomContext.setZoomHabilitado(zoomManualInicial);

        // Aplicar a contextos de Lista
        this.visualizadorListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.proyectoListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.datosListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        
        // Aplicar a estado global que no es contextual
        this.navegacionCircularActivada = navCircularInicial;
    } // --- FIN del metodo initializeContexts ---
    
    public WorkMode getCurrentWorkMode() {
        return this.currentWorkMode;
    }
    
    public void setCurrentWorkMode(WorkMode newMode) {
        if (this.currentWorkMode != newMode) {
            System.out.println("[Model] Cambiando modo de trabajo de " + this.currentWorkMode + " a: " + newMode);
            this.currentWorkMode = newMode;
            // Al cambiar de modo, es buena práctica limpiar la imagen actual para forzar una recarga.
            this.setCurrentImage(null);
            // Sincronizar el flag booleano por compatibilidad con código antiguo
            this.enModoProyecto = (newMode == WorkMode.PROYECTO);
        }
    } // --- Fin del método setCurrentWorkMode ---

    /**
     * Devuelve el contexto de LISTA ACTIVO, dependiendo del modo de trabajo actual.
     * @return El objeto ListContext correspondiente al modo actual.
     */
    public ListContext getCurrentListContext() {
        switch (this.currentWorkMode) {
            case PROYECTO:
                return this.proyectoListContext;
            case DATOS:
                return this.datosListContext;
            case VISUALIZADOR:
            default:
                return this.visualizadorListContext;
        }
    } // --- Fin del método getCurrentListContext ---

    /**
     * Devuelve el contexto de ZOOM ACTIVO, dependiendo del modo de trabajo actual.
     * @return El objeto ZoomContext correspondiente al modo actual.
     */
    public ZoomContext getCurrentZoomContext() {
        switch (this.currentWorkMode) {
            case PROYECTO:
                return this.proyectoZoomContext;
            case DATOS:
                return this.datosZoomContext;
            case VISUALIZADOR:
            default:
                return this.visualizadorZoomContext;
        }
    } // --- Fin del método getCurrentZoomContext ---

    // --- GETTERS Y SETTERS "DELEGADOS" para la LISTA ---
    // El resto del código no necesita saber de contextos, solo usa estos métodos.
    
    public DefaultListModel<String> getModeloLista() { 
    	
    	ListContext ctx = getCurrentListContext();
    	
//    	// LOG ### DEBUG MODELO: getModeloLista() llamado en modo
//        // --- INICIO CÓDIGO DEPURACIÓN ---
//        System.out.println("### DEBUG MODELO: getModeloLista() llamado en modo " + this.currentWorkMode + 
//                           ". Devolviendo modelo de ListContext@" + Integer.toHexString(ctx.hashCode()) +
//                           " con tamaño: " + ctx.getModeloLista().getSize());
//        // --- FIN CÓDIGO DEPURACIÓN ---
        
        return ctx.getModeloLista(); 
    }
    
    public Map<String, Path> getRutaCompletaMap() { return getCurrentListContext().getRutaCompletaMap(); }
    public Path getRutaCompleta(String key) { return getCurrentListContext().getRutaCompleta(key); }
    public String getSelectedImageKey() { return getCurrentListContext().getSelectedImageKey(); }
    
    public void setSelectedImageKey(String selectedImageKey) 
    { 
    	getCurrentListContext().setSelectedImageKey(selectedImageKey); 
    	
    }
    
    /**
     * Actualiza el contexto de lista COMPLETO para el modo de trabajo ACTUAL.
     * @param nuevoModelo El nuevo modelo de lista.
     * @param nuevoMapaRutas El nuevo mapa de rutas.
     */
    public void actualizarListaCompleta(DefaultListModel<String> nuevoModelo, Map<String, java.nio.file.Path> nuevoMapaRutas) {
        System.out.println("[Model] Actualizando contexto de lista para el modo: " + this.currentWorkMode);
        getCurrentListContext().actualizarContextoCompleto(nuevoModelo, nuevoMapaRutas);
    } // --- Fin del método actualizarListaCompleta ---


    // --- GETTERS Y SETTERS "DELEGADOS" PARA EL ZOOM (sin cambios, ya estaban bien) ---

    public ZoomModeEnum getCurrentZoomMode() { return getCurrentZoomContext().getZoomMode(); }
    public void setCurrentZoomMode(ZoomModeEnum newMode) { getCurrentZoomContext().setZoomMode(newMode); }

    public double getZoomFactor() { return getCurrentZoomContext().getZoomFactor(); }
    public void setZoomFactor(double zoomFactor) {
        double validFactor = Math.max(0.01, Math.min(zoomFactor, 50.0));
        getCurrentZoomContext().setZoomFactor(validFactor);
    }
    
    public int getImageOffsetX() { return getCurrentZoomContext().getImageOffsetX(); }
    public void setImageOffsetX(int x) { getCurrentZoomContext().setImageOffsetX(x); }
    public void addImageOffsetX(int deltaX) { setImageOffsetX(getImageOffsetX() + deltaX); }

    public int getImageOffsetY() { return getCurrentZoomContext().getImageOffsetY(); }
    public void setImageOffsetY(int y) { getCurrentZoomContext().setImageOffsetY(y); }
    public void addImageOffsetY(int deltaY) { setImageOffsetY(getImageOffsetY() + deltaY); }
    
    public boolean isZoomHabilitado() { return getCurrentZoomContext().isZoomHabilitado(); }
    public void setZoomHabilitado(boolean b) { getCurrentZoomContext().setZoomHabilitado(b); }
    
    public void resetPan() { getCurrentZoomContext().resetPan(); }
    public void resetZoomState() {
        this.setZoomFactor(1.0);
        this.resetPan();
    } // --- Fin del método resetZoomState ---

    // --- GETTERS Y SETTERS DE ESTADO GLOBAL (IMAGEN ACTUAL) ---

    public BufferedImage getCurrentImage() { return currentImage; }
    public void setCurrentImage(BufferedImage currentImage) { this.currentImage = currentImage; }
    
    // --- MÉTODOS Y GETTERS/SETTERS DE CONFIGURACIÓN GLOBAL (SIN CAMBIOS) ---
    
    public boolean isMostrarSoloCarpetaActual() { 
        return getCurrentListContext().isMostrarSoloCarpetaActual();
    }
    public void setMostrarSoloCarpetaActual(boolean mostrarSoloCarpetaActual) {
        if (isMostrarSoloCarpetaActual() != mostrarSoloCarpetaActual) {
            getCurrentListContext().setMostrarSoloCarpetaActual(mostrarSoloCarpetaActual);
            System.out.println("  [Model " + currentWorkMode + "] Estado mostrarSoloCarpetaActual cambiado a: " 
            		+ mostrarSoloCarpetaActual);
        }
    }

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

    public boolean isMantenerProporcion() { 
        return getCurrentZoomContext().isMantenerProporcion(); 
    }
    
    public void setMantenerProporcion(boolean mantenerProporcion) {
        if (isMantenerProporcion() != mantenerProporcion) {
            getCurrentZoomContext().setMantenerProporcion(mantenerProporcion);
            System.out.println("  [Model " + currentWorkMode + "] Estado mantenerProporcion cambiado a: " + mantenerProporcion);
        }
    } // --- Fin del método setMantenerProporcion ---

    public Path getCarpetaRaizActual() { return carpetaRaizActual; }
    public void setCarpetaRaizActual(Path carpetaRaizActual) {
        if (!Objects.equals(this.carpetaRaizActual, carpetaRaizActual)) {
            System.out.println("  [VisorModel] carpetaRaizActual cambiada de '" + this.carpetaRaizActual + "' a '" + carpetaRaizActual + "'");
            this.carpetaRaizActual = carpetaRaizActual;
        }
    } // --- Fin del método setCarpetaRaizActual ---

    public boolean isNavegacionCircularActivada() { return navegacionCircularActivada; }
    public void setNavegacionCircularActivada(boolean activada) {
        if (this.navegacionCircularActivada != activada) {
            this.navegacionCircularActivada = activada;
            System.out.println("  [VisorModel] Navegación Circular cambiada a: " + activada);
        }
    } // --- Fin del método setNavegacionCircularActivada ---

    public boolean isEnModoProyecto() { return enModoProyecto; }
    public void setEnModoProyecto(boolean enModoProyecto) {
        // Este método ahora solo existe por compatibilidad, la fuente de verdad es setCurrentWorkMode
        setCurrentWorkMode(enModoProyecto ? WorkMode.PROYECTO : WorkMode.VISUALIZADOR);
    } // --- Fin del método setEnModoProyecto ---
    
    public ListContext getProyectoListContext() {
        return this.proyectoListContext;
    }
    
        
} // --- FIN DE LA CLASE VisorModel ---



