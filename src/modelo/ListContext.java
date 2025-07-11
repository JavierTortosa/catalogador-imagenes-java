package modelo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;

/**
 * Encapsula el estado completo de una lista de imágenes para un modo de trabajo específico.
 * Contiene el modelo de la lista, el mapa de rutas y la selección actual.
 */
public class ListContext {

    // --- Campos de Estado del Contexto ---
    private DefaultListModel<String> modeloLista;
    private Map<String, Path> rutaCompletaMap;
    private String selectedImageKey;
    private boolean mostrarSoloCarpetaActual = true;
    private String nombreListaActiva;
    
    // Nuevos campos para recordar la selección de cada lista en el modo proyecto
    private String seleccionListKey; // Última clave seleccionada en la lista "Selección Actual"
    private String descartesListKey;  // Última clave seleccionada en la lista "Descartes"

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
            System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": Restored old selectedKey: '" + oldSelectedKey + "'");
        } else {
            this.selectedImageKey = null;
            System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": SelectedKey set to NULL (old key not found or was null).");
        }
        
        System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + 
                           " actualizado. Nuevo tamaño: " + this.modeloLista.getSize());
    } // --- Fin del método actualizarContextoCompleto ---
    

    // --- Getters y Setters ---


	public DefaultListModel<String> getModeloLista(){ return modeloLista; }
	public void setModeloLista(DefaultListModel<String> modeloLista){ this.modeloLista = modeloLista; }

	public Map<String, Path> getRutaCompletaMap(){ return rutaCompletaMap; }
	public Path getRutaCompleta(String key)	{return (this.rutaCompletaMap != null) ? this.rutaCompletaMap.get(key) : null;}

	public void setRutaCompletaMap(Map<String, Path> rutaCompletaMap)	{ this.rutaCompletaMap = rutaCompletaMap; }

	public String getSelectedImageKey()	{ return selectedImageKey; }
	
	public void setSelectedImageKey(String selectedImageKey)
	{ 
		if (selectedImageKey == null && this.selectedImageKey != null) {
            System.out.println("### DEBUG: ListContext@" + Integer.toHexString(hashCode()) +
                               ": selectedImageKey se está poniendo a NULL. (Contexto Hash: " + System.identityHashCode(this) + ")");
            new Throwable("Pila de llamadas a setSelectedImageKey(null)").printStackTrace(System.out);
        }
		
		this.selectedImageKey = selectedImageKey; 
		
	}
    
    public boolean isMostrarSoloCarpetaActual() { return mostrarSoloCarpetaActual; } 
    public void setMostrarSoloCarpetaActual(boolean soloCarpeta) { this.mostrarSoloCarpetaActual = soloCarpeta; }
	public String getNombreListaActiva(){ return this.nombreListaActiva; } 
	public void setNombreListaActiva(String nombreListaActiva)	{ this.nombreListaActiva = nombreListaActiva; }
	
	public String getSeleccionListKey() {return seleccionListKey;} 
    public void setSeleccionListKey(String seleccionListKey) {this.seleccionListKey = seleccionListKey;} 
    public String getDescartesListKey() {return descartesListKey;} 
    public void setDescartesListKey(String descartesListKey) {this.descartesListKey = descartesListKey;}

} // --- FIN DE LA CLASE ListContext ---