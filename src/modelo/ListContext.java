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

    /**
     * Constructor. Inicializa el contexto a un estado vacío y válido.
     */
    public ListContext() {
        this.modeloLista = new DefaultListModel<>();
        this.rutaCompletaMap = new HashMap<>();
        this.selectedImageKey = null;
        this.nombreListaActiva = "seleccion";
        
    } // --- Fin del constructor ListContext ---

    
    /**
     * Reemplaza el estado completo de este contexto con nuevos datos.
     * @param nuevoModelo El nuevo modelo de lista.
     * @param nuevoMapaRutas El nuevo mapa de rutas.
     */
    public void actualizarContextoCompleto(DefaultListModel<String> nuevoModelo, Map<String, Path> nuevoMapaRutas) {
        // INICIO DEL CAMBIO
        String oldSelectedKey = this.selectedImageKey; // Guardar la clave seleccionada actual
        // FIN DEL CAMBIO

        this.modeloLista = (nuevoModelo != null) ? nuevoModelo : new DefaultListModel<>();
        this.rutaCompletaMap = (nuevoMapaRutas != null) ? nuevoMapaRutas : new HashMap<>();
        
        // INICIO DEL CAMBIO
        // Intentar restaurar la selección antigua si todavía existe en el nuevo modelo
        if (oldSelectedKey != null && this.modeloLista.contains(oldSelectedKey)) {
            this.selectedImageKey = oldSelectedKey;
            System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": Restored old selectedKey: '" + oldSelectedKey + "'");
        } else {
            // Si la clave antigua no existía o no se encontró en el nuevo modelo, resetear la selección
            this.selectedImageKey = null;
            System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + ": SelectedKey set to NULL (old key not found or was null).");
        }
        // FIN DEL CAMBIO
        
        System.out.println("### DEBUG CONTEXT: ListContext@" + Integer.toHexString(hashCode()) + 
                           " actualizado. Nuevo tamaño: " + this.modeloLista.getSize());
        if (this.modeloLista.isEmpty()) {
            // Imprime el stack trace para saber QUIÉN lo está vaciando
            // new Throwable("PILA DE LLAMADAS que vació este contexto").printStackTrace(System.out); // Desactivar si no es útil en este punto
        }
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

} // --- FIN DE LA CLASE ListContext ---