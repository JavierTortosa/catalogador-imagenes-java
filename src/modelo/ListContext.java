package modelo;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controlador.AppInitializer;
import modelo.VisorModel.DisplayMode;


/**
 * Encapsula el estado completo de una lista de imágenes para un modo de trabajo específico.
 * Contiene el modelo de la lista, el mapa de rutas y la selección actual.
 */
public class ListContext {
	
	private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    // --- Campos de Estado del Contexto ---
    private DefaultListModel<String> modeloLista;
    private Path carpetaRaizContexto;
    
    private Map<String, Path> rutaCompletaMap;
    private String selectedImageKey;
    private boolean mostrarSoloCarpetaActual = true;
    private String nombreListaActiva;
    
    // Nuevos campos para recordar la selección de cada lista en el modo proyecto
    private String seleccionListKey; // Última clave seleccionada en la lista "Selección Actual"
    private String descartesListKey;  // Última clave seleccionada en la lista "Descartes"

    private DisplayMode displayMode;
    
    /**
     * Constructor. Inicializa el contexto a un estado vacío y válido.
     */
    public ListContext() {
        this.modeloLista = new DefaultListModel<>();
        this.rutaCompletaMap = new HashMap<>();
        this.selectedImageKey = null;
        this.nombreListaActiva = "seleccion";
        
        this.seleccionListKey = null;
        this.descartesListKey = null;
        
        this.displayMode = DisplayMode.SINGLE_IMAGE;
        
    } // --- Fin del constructor ListContext ---

    
    /**
     * Reemplaza el estado completo de este contexto con nuevos datos.
     * @param nuevoModelo El nuevo modelo de lista.
     * @param nuevoMapaRutas El nuevo mapa de rutas.
     */
    public void actualizarContextoCompleto(DefaultListModel<String> nuevoModelo, Map<String, Path> nuevoMapaRutas) {
        String oldSelectedKey = this.selectedImageKey;

        this.modeloLista = (nuevoModelo != null) ? nuevoModelo : new DefaultListModel<>();
        this.rutaCompletaMap = (nuevoMapaRutas != null) ? nuevoMapaRutas : new HashMap<>();
        
        if (oldSelectedKey != null && this.modeloLista.contains(oldSelectedKey)) {
            this.selectedImageKey = oldSelectedKey;
            logger.debug("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": Restored old selectedKey: '" + oldSelectedKey + "'");
        } else {
            this.selectedImageKey = null;
            logger.debug("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": SelectedKey set to NULL (old key not found or was null).");
        }
        
        logger.debug("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + 
                           " actualizado. Nuevo tamaño: " + this.modeloLista.getSize());
    } // --- Fin del método actualizarContextoCompleto ---
    
    
    /**
     * Clona el estado de otro ListContext en este.
     * Crea copias de los modelos y mapas para asegurar la independencia.
     * @param otroContexto El contexto desde el cual copiar los datos.
     */
    public void clonarDesde(ListContext otroContexto) {
        if (otroContexto == null) {
            logger.warn("WARN [ListContext.clonarDesde]: Se intentó clonar desde un contexto nulo.");
            return;
        }
        
        // 1. Clonar el DefaultListModel
        DefaultListModel<String> nuevoModelo = new DefaultListModel<>();
        if (otroContexto.getModeloLista() != null) {
            for (String item : Collections.list(otroContexto.getModeloLista().elements())) {
                nuevoModelo.addElement(item);
            }
        }
        this.modeloLista = nuevoModelo;
        
        // 2. Clonar el mapa de rutas
        this.rutaCompletaMap = new HashMap<>(otroContexto.getRutaCompletaMap());
        
        // 3. Copiar los valores primitivos/inmutables
        this.selectedImageKey = otroContexto.getSelectedImageKey();
        this.mostrarSoloCarpetaActual = otroContexto.isMostrarSoloCarpetaActual();
        
        // LOG [ListContext.clonarDesde] Clonando 'mostrarSoloCarpetaActual'. Valor:
        logger.debug("    [ListContext.clonarDesde] Clonando 'mostrarSoloCarpetaActual'. Valor: " + this.mostrarSoloCarpetaActual);
        
        this.nombreListaActiva = otroContexto.getNombreListaActiva();
        this.seleccionListKey = otroContexto.getSeleccionListKey();
        this.descartesListKey = otroContexto.getDescartesListKey();
        this.carpetaRaizContexto = otroContexto.getCarpetaRaizContexto();
        
        this.displayMode = otroContexto.getDisplayMode();

    } // --- Fin del método clonarDesde ---
    

    // --- Getters y Setters ---


	public DefaultListModel<String> getModeloLista(){ return modeloLista; }
	public void setModeloLista(DefaultListModel<String> modeloLista){ this.modeloLista = modeloLista; }

	public Map<String, Path> getRutaCompletaMap(){ return rutaCompletaMap; }
	public Path getRutaCompleta(String key)	{return (this.rutaCompletaMap != null) ? this.rutaCompletaMap.get(key) : null;}

	public void setRutaCompletaMap(Map<String, Path> rutaCompletaMap)	{ this.rutaCompletaMap = rutaCompletaMap; }

	public String getSelectedImageKey()	{ return selectedImageKey; }
	public void setSelectedImageKey(String selectedImageKey){this.selectedImageKey = selectedImageKey;}
    
    public boolean isMostrarSoloCarpetaActual() { return mostrarSoloCarpetaActual; } 
    public void setMostrarSoloCarpetaActual(boolean soloCarpeta) { this.mostrarSoloCarpetaActual = soloCarpeta; }
    
	public String getNombreListaActiva(){ return this.nombreListaActiva; } 
	public void setNombreListaActiva(String nombreListaActiva)	{ this.nombreListaActiva = nombreListaActiva; }
	
	public String getSeleccionListKey() {return seleccionListKey;} 
    public void setSeleccionListKey(String seleccionListKey) {this.seleccionListKey = seleccionListKey;} 
    
    public String getDescartesListKey() {return descartesListKey;} 
    public void setDescartesListKey(String descartesListKey) {this.descartesListKey = descartesListKey;}

    public Path getCarpetaRaizContexto() {return this.carpetaRaizContexto;}
    public void setCarpetaRaizContexto(Path carpetaRaiz) {this.carpetaRaizContexto = carpetaRaiz;}
    
    public DisplayMode getDisplayMode() {return this.displayMode;}
    public void setDisplayMode(DisplayMode displayMode) {this.displayMode = displayMode;}
    
} // --- FIN DE LA CLASE ListContext ---