package modelo;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.swing.DefaultListModel;

import servicios.zoom.ZoomModeEnum;

public class VisorModel {

    // --- Enum para definir los modos de trabajo ---
    public enum WorkMode {
        VISUALIZADOR,
        PROYECTO,
        DATOS
    }

    private WorkMode currentWorkMode;
    private ListContext visualizadorListContext;
    private ListContext proyectoListContext;
    private ListContext datosListContext;

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
    private Path carpetaRaizActual = null;
    private boolean navegacionCircularActivada = false;
    private int saltoDeBloque;
    
    private boolean enModoProyecto = false; 

    public VisorModel() {
        this.currentWorkMode = WorkMode.VISUALIZADOR;
        this.visualizadorListContext = new ListContext();
        this.proyectoListContext = new ListContext();
        this.datosListContext = new ListContext();
        this.visualizadorZoomContext = new ZoomContext();
        this.proyectoZoomContext = new ZoomContext();
        this.datosZoomContext = new ZoomContext();
        this.currentImage = null;
        this.saltoDeBloque = 10;
    }

    // Añadimos el nuevo parámetro 'zoomAlCursor' a la firma del método
    public void initializeContexts(boolean mantenerPropInicial, boolean soloCarpetaInicial, ZoomModeEnum modoZoomInicial, boolean zoomManualInicial, boolean navCircularInicial, boolean zoomAlCursor) {
        System.out.println("[VisorModel] Inicializando estado por defecto de todos los contextos...");

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

        this.visualizadorListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.proyectoListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        this.datosListContext.setMostrarSoloCarpetaActual(soloCarpetaInicial);
        
        this.navegacionCircularActivada = navCircularInicial;
    }
    
    public WorkMode getCurrentWorkMode() {
        return this.currentWorkMode;
    }
    
    public void setCurrentWorkMode(WorkMode newMode) {
        if (this.currentWorkMode != newMode) {
            System.out.println("[Model] Cambiando modo de trabajo de " + this.currentWorkMode + " a: " + newMode);
            this.currentWorkMode = newMode;
            this.setCurrentImage(null);
            this.enModoProyecto = (newMode == WorkMode.PROYECTO);
        }
    }

    public ListContext getCurrentListContext() {
        switch (this.currentWorkMode) {
            case PROYECTO: return this.proyectoListContext;
            case DATOS: return this.datosListContext;
            case VISUALIZADOR: default: return this.visualizadorListContext;
        }
    }

    public ZoomContext getCurrentZoomContext() {
        switch (this.currentWorkMode) {
            case PROYECTO: return this.proyectoZoomContext;
            case DATOS: return this.datosZoomContext;
            case VISUALIZADOR: default: return this.visualizadorZoomContext;
        }
    }

    public DefaultListModel<String> getModeloLista() { 
        return getCurrentListContext().getModeloLista(); 
    }
    
    public Map<String, Path> getRutaCompletaMap() { return getCurrentListContext().getRutaCompletaMap(); }
    public Path getRutaCompleta(String key) { return getCurrentListContext().getRutaCompleta(key); }
    public String getSelectedImageKey() { return getCurrentListContext().getSelectedImageKey(); }
    public void setSelectedImageKey(String selectedImageKey) { getCurrentListContext().setSelectedImageKey(selectedImageKey); }
    
    public void actualizarListaCompleta(DefaultListModel<String> nuevoModelo, Map<String, java.nio.file.Path> nuevoMapaRutas) {
        System.out.println("[Model] Actualizando contexto de lista para el modo: " + this.currentWorkMode);
        getCurrentListContext().actualizarContextoCompleto(nuevoModelo, nuevoMapaRutas);
    }

    public ZoomModeEnum getCurrentZoomMode() { return getCurrentZoomContext().getZoomMode(); }
    public void setCurrentZoomMode(ZoomModeEnum newMode) { getCurrentZoomContext().setZoomMode(newMode); }

    
    public int getImageOffsetX() { return getCurrentZoomContext().getImageOffsetX(); }
    public void setImageOffsetX(int x) { getCurrentZoomContext().setImageOffsetX(x); }
    public void addImageOffsetX(int deltaX) { setImageOffsetX(getImageOffsetX() + deltaX); }
    public int getImageOffsetY() { return getCurrentZoomContext().getImageOffsetY(); }
    public void setImageOffsetY(int y) { getCurrentZoomContext().setImageOffsetY(y); }
    public void addImageOffsetY(int deltaY) { setImageOffsetY(getImageOffsetY() + deltaY); }
    public boolean isZoomHabilitado() { return getCurrentZoomContext().isZoomHabilitado(); }
    public void setZoomHabilitado(boolean b) { getCurrentZoomContext().setZoomHabilitado(b); }
    public void resetPan() { getCurrentZoomContext().resetPan(); }
    public void resetZoomState() {this.setZoomFactor(1.0); this.resetPan();}
    public BufferedImage getCurrentImage() { return currentImage; }
    public void setCurrentImage(BufferedImage currentImage) { this.currentImage = currentImage; }
    public ListContext getProyectoListContext() { return this.proyectoListContext; }
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

    public double getZoomFactor() { return getCurrentZoomContext().getZoomFactor(); }
    public void setZoomFactor(double zoomFactor) {
        double validFactor = Math.max(0.01, Math.min(zoomFactor, 50.0));
        getCurrentZoomContext().setZoomFactor(validFactor);
    }

    public boolean isMostrarSoloCarpetaActual() { return getCurrentListContext().isMostrarSoloCarpetaActual(); }
    public void setMostrarSoloCarpetaActual(boolean mostrarSoloCarpetaActual) {
        if (isMostrarSoloCarpetaActual() != mostrarSoloCarpetaActual) {
            getCurrentListContext().setMostrarSoloCarpetaActual(mostrarSoloCarpetaActual);
            System.out.println("  [Model " + currentWorkMode + "] Estado mostrarSoloCarpetaActual cambiado a: " + mostrarSoloCarpetaActual);
        }
    }

    public boolean isMantenerProporcion() { return getCurrentZoomContext().isMantenerProporcion(); }
    public void setMantenerProporcion(boolean mantenerProporcion) {
        if (isMantenerProporcion() != mantenerProporcion) {
            getCurrentZoomContext().setMantenerProporcion(mantenerProporcion);
            System.out.println("  [Model " + currentWorkMode + "] Estado mantenerProporcion cambiado a: " + mantenerProporcion);
        }
    }

    public Path getCarpetaRaizActual() { return carpetaRaizActual; }
    public void setCarpetaRaizActual(Path carpetaRaizActual) {
        if (!Objects.equals(this.carpetaRaizActual, carpetaRaizActual)) {
            System.out.println("  [VisorModel] carpetaRaizActual cambiada de '" + this.carpetaRaizActual + "' a '" + carpetaRaizActual + "'");
            this.carpetaRaizActual = carpetaRaizActual;
        }
    }
    
    public boolean isNavegacionCircularActivada() { return navegacionCircularActivada; }
    public void setNavegacionCircularActivada(boolean activada) {
        if (this.navegacionCircularActivada != activada) {
            this.navegacionCircularActivada = activada;
            System.out.println("  [VisorModel] Navegación Circular cambiada a: " + activada);
        }
    }
    
    public boolean isZoomToCursorEnabled() { return getCurrentZoomContext().isZoomToCursorEnabled(); }
    public void setZoomToCursorEnabled(boolean enabled) {
        if (isZoomToCursorEnabled() != enabled) {
            getCurrentZoomContext().setZoomToCursorEnabled(enabled);
            System.out.println("  [Model " + currentWorkMode + "] Estado zoomToCursorEnabled cambiado a: " + enabled);
        }
    }
        
} // --- FIN DE LA CLASE VisorModel ---